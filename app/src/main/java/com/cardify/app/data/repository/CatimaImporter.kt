package com.cardify.app.data.repository

import androidx.compose.ui.graphics.Color
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.cardify.app.data.local.entities.CardEntity
import com.cardify.app.data.local.entities.CategoryEntity
import com.cardify.app.domain.model.CardColorPalette
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class CatimaImportResult(
    val cards: List<CardEntity>,
    val categories: List<CategoryEntity>
)

object CatimaImporter {

    fun parse(inputStream: InputStream, existingCategories: List<CategoryEntity>): CatimaImportResult {
        val allBytes = inputStream.readBytes()
        if (allBytes.isEmpty()) {
            return CatimaImportResult(emptyList(), emptyList())
        }

        // 1. Try parsing as ZIP archive
        try {
            val zipResult = parseZipBytes(allBytes, existingCategories)
            if (zipResult.cards.isNotEmpty()) {
                return zipResult
            }
        } catch (_: Throwable) {
            // Not a zip or corrupted zip entry, fallback to plain text
        }

        // 2. Try parsing as UTF-8 text
        try {
            val textUtf8 = String(allBytes, Charsets.UTF_8)
            val textResult = parseText(textUtf8, existingCategories)
            if (textResult.cards.isNotEmpty()) {
                return textResult
            }
        } catch (_: Throwable) { }

        // 3. Try parsing with ISO-8859-1 fallback
        try {
            val textIso = String(allBytes, Charsets.ISO_8859_1)
            val textResult = parseText(textIso, existingCategories)
            if (textResult.cards.isNotEmpty()) {
                return textResult
            }
        } catch (_: Throwable) { }

        return CatimaImportResult(emptyList(), emptyList())
    }

    private fun parseZipBytes(bytes: ByteArray, existingCategories: List<CategoryEntity>): CatimaImportResult {
        val files = mutableMapOf<String, String>()
        val zip = ZipInputStream(ByteArrayInputStream(bytes))
        var entry = zip.nextEntry
        val buffer = ByteArray(8192)

        while (entry != null) {
            if (!entry.isDirectory) {
                val baos = ByteArrayOutputStream()
                var len: Int
                while (zip.read(buffer).also { len = it } > 0) {
                    baos.write(buffer, 0, len)
                }
                val entryBytes = baos.toByteArray()
                val text = try {
                    String(entryBytes, Charsets.UTF_8)
                } catch (_: Throwable) {
                    String(entryBytes, Charsets.ISO_8859_1)
                }
                files[entry.name.lowercase().trim()] = text
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        if (files.isEmpty()) {
            return CatimaImportResult(emptyList(), emptyList())
        }

        // Look for cards.csv, groups.csv, card_groups.csv
        var cardsCsv = files.entries.find { it.key.endsWith("cards.csv") || it.key == "cards.csv" }?.value
        val groupsCsv = files.entries.find { it.key.endsWith("groups.csv") || it.key == "groups.csv" }?.value ?: ""
        val cardGroupsCsv = files.entries.find { it.key.endsWith("card_groups.csv") || it.key == "card_groups.csv" }?.value ?: ""

        // If cards.csv not found by name, search contents of any text file
        if (cardsCsv == null) {
            for ((_, content) in files) {
                if (content.contains("store", ignoreCase = true) && (content.contains("cardid", ignoreCase = true) || content.contains("barcodetype", ignoreCase = true) || content.contains("_id", ignoreCase = true))) {
                    val parsed = parseText(content, existingCategories)
                    if (parsed.cards.isNotEmpty()) {
                        return parsed
                    }
                    cardsCsv = content
                    break
                }
            }
        }

        if (cardsCsv.isNullOrBlank() && files.size == 1) {
            val singleContent = files.values.first()
            return parseText(singleContent, existingCategories)
        }

        if (cardsCsv.isNullOrBlank()) {
            return CatimaImportResult(emptyList(), emptyList())
        }

        return processCsvTables(cardsCsv, groupsCsv, cardGroupsCsv, existingCategories)
    }

    private fun parseText(fullText: String, existingCategories: List<CategoryEntity>): CatimaImportResult {
        // Handle Catima multi-table text/csv format
        val lines = fullText.lines()
        val cardsLines = mutableListOf<String>()
        val groupsLines = mutableListOf<String>()
        val cardGroupsLines = mutableListOf<String>()

        var currentTarget: MutableList<String>? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if ((line.contains("store", ignoreCase = true) && line.contains("cardid", ignoreCase = true)) || line.contains("_id,store", ignoreCase = true)) {
                currentTarget = cardsLines
                currentTarget.add(rawLine)
                continue
            }

            if (line.startsWith("cardId,groupId", ignoreCase = true) || line.startsWith("card_id,group_id", ignoreCase = true)) {
                currentTarget = cardGroupsLines
                currentTarget.add(rawLine)
                continue
            }

            if (line.startsWith("_id,name", ignoreCase = true) || line.startsWith("id,name", ignoreCase = true) || line.startsWith("groupId,name", ignoreCase = true)) {
                currentTarget = groupsLines
                currentTarget.add(rawLine)
                continue
            }

            currentTarget?.add(rawLine)
        }

        // If no sections recognized, but there is text, try treating the whole text as cards CSV
        val finalCardsCsv = if (cardsLines.isEmpty() && fullText.contains(",")) fullText else cardsLines.joinToString("\n")

        return processCsvTables(
            cardsCsv = finalCardsCsv,
            groupsCsv = groupsLines.joinToString("\n"),
            cardGroupsCsv = cardGroupsLines.joinToString("\n"),
            existingCategories = existingCategories
        )
    }

    private fun processCsvTables(
        cardsCsv: String,
        groupsCsv: String,
        cardGroupsCsv: String,
        existingCategories: List<CategoryEntity>
    ): CatimaImportResult {
        // 1. Parse Groups / Categories if present
        val catimaGroupIdToName = mutableMapOf<String, String>()
        if (groupsCsv.isNotBlank()) {
            val groupRows = parseCsvRows(groupsCsv)
            if (groupRows.isNotEmpty()) {
                val header = groupRows.first().map { it.lowercase().trim() }
                val idIdx = header.indexOfFirst { it == "_id" || it == "id" || it == "groupid" }
                val nameIdx = header.indexOfFirst { it == "name" || it == "title" }
                if (idIdx >= 0 && nameIdx >= 0) {
                    for (i in 1 until groupRows.size) {
                        val row = groupRows[i]
                        if (row.size > maxOf(idIdx, nameIdx)) {
                            val id = row[idIdx].trim()
                            val name = row[nameIdx].trim()
                            if (id.isNotBlank() && name.isNotBlank()) {
                                catimaGroupIdToName[id] = name
                            }
                        }
                    }
                }
            }
        }

        // 2. Parse Card-to-Group mappings
        val cardIdToGroupName = mutableMapOf<String, String>()
        if (cardGroupsCsv.isNotBlank()) {
            val mappingRows = parseCsvRows(cardGroupsCsv)
            if (mappingRows.isNotEmpty()) {
                val header = mappingRows.first().map { it.lowercase().trim() }
                val cardIdIdx = header.indexOfFirst { it == "cardid" || it == "card_id" }
                val groupIdIdx = header.indexOfFirst { it == "groupid" || it == "group_id" }
                if (cardIdIdx >= 0 && groupIdIdx >= 0) {
                    for (i in 1 until mappingRows.size) {
                        val row = mappingRows[i]
                        if (row.size > maxOf(cardIdIdx, groupIdIdx)) {
                            val cId = row[cardIdIdx].trim()
                            val gId = row[groupIdIdx].trim()
                            val gName = catimaGroupIdToName[gId]
                            if (cId.isNotBlank() && gName != null) {
                                cardIdToGroupName[cId] = gName
                            }
                        }
                    }
                }
            }
        }

        // 3. Parse Cards table
        val cardRows = parseCsvRows(cardsCsv)
        if (cardRows.isEmpty()) {
            return CatimaImportResult(emptyList(), emptyList())
        }

        val header = cardRows.first().map { it.lowercase().trim() }
        val idIdx = header.indexOfFirst { it == "_id" || it == "id" }
        val storeIdx = header.indexOfFirst { it == "store" || it == "title" || it == "name" }
        val cardIdIdx = header.indexOfFirst { it == "cardid" || it == "card_id" }
        val barcodeIdIdx = header.indexOfFirst { it == "barcodeid" || it == "barcode_id" }
        val typeIdx = header.indexOfFirst { it == "barcodetype" || it == "barcode_type" || it == "type" }
        val colorIdx = header.indexOfFirst { it == "headercolor" || it == "header_color" || it == "color" }
        val starIdx = header.indexOfFirst { it == "starstatus" || it == "star_status" || it == "favorite" }
        val noteIdx = header.indexOfFirst { it == "note" || it == "notes" }
        val lastUsedIdx = header.indexOfFirst { it == "lastused" || it == "last_used" }

        val newCategoriesToCreate = mutableListOf<CategoryEntity>()
        val categoryNameToEntityMap = existingCategories.associateBy { it.name.lowercase().trim() }.toMutableMap()

        val parsedCards = mutableListOf<CardEntity>()

        for (i in 1 until cardRows.size) {
            val row = cardRows[i]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            fun getCol(idx: Int): String = if (idx >= 0 && idx < row.size) row[idx].trim() else ""

            val rawStore = getCol(storeIdx)
            val rawCardId = getCol(cardIdIdx)
            val rawBarcodeId = getCol(barcodeIdIdx)
            val rawType = getCol(typeIdx)
            val rawColor = getCol(colorIdx)
            val rawStar = getCol(starIdx)
            val rawNote = getCol(noteIdx)
            val rawLastUsed = getCol(lastUsedIdx)
            val rawInternalId = getCol(idIdx)

            val title = rawStore.ifBlank { "Карта" }
            val barcodeValue = if (rawCardId.isNotBlank()) rawCardId else rawBarcodeId
            if (barcodeValue.isBlank() && rawStore.isBlank()) continue

            val barcodeFormat = mapCatimaBarcodeType(rawType)
            val colorKey = mapCatimaColor(rawColor)
            val isFavorite = rawStar == "1" || rawStar.equals("true", ignoreCase = true)
            val notes = rawNote

            val lastUsedAt = rawLastUsed.toLongOrNull()?.let { ts ->
                if (ts in 1L..99999999999L) ts * 1000L else if (ts > 0L) ts else System.currentTimeMillis()
            } ?: System.currentTimeMillis()

            // Resolve Category if linked via cardIdToGroupName
            val groupName = cardIdToGroupName[rawInternalId]
            var categoryId: Long? = null
            if (!groupName.isNullOrBlank()) {
                val normalizedGroupName = groupName.lowercase().trim()
                var cat = categoryNameToEntityMap[normalizedGroupName]
                if (cat == null) {
                    val newCat = CategoryEntity(
                        name = groupName,
                        iconName = "category",
                        colorHex = colorKey,
                        orderIndex = categoryNameToEntityMap.size
                    )
                    newCategoriesToCreate.add(newCat)
                    categoryNameToEntityMap[normalizedGroupName] = newCat
                    cat = newCat
                }
                categoryId = if (cat.id > 0) cat.id else null
            }

            parsedCards.add(
                CardEntity(
                    id = 0L,
                    title = title,
                    barcodeValue = barcodeValue.ifBlank { "000000000000" },
                    barcodeFormat = barcodeFormat,
                    categoryId = categoryId,
                    colorHex = colorKey,
                    notes = notes,
                    isFavorite = isFavorite,
                    useCount = 0,
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = lastUsedAt
                )
            )
        }

        return CatimaImportResult(parsedCards, newCategoriesToCreate)
    }

    private fun mapCatimaBarcodeType(rawType: String): BarcodeFormatEnum {
        val clean = rawType.trim().uppercase()
        return when {
            clean.contains("QR") -> BarcodeFormatEnum.QR_CODE
            clean.contains("EAN_13") || clean == "EAN13" -> BarcodeFormatEnum.EAN_13
            clean.contains("EAN_8") || clean == "EAN8" -> BarcodeFormatEnum.EAN_8
            clean.contains("128") -> BarcodeFormatEnum.CODE_128
            clean.contains("CODE_39") || clean == "CODE39" -> BarcodeFormatEnum.CODE_39
            clean.contains("CODE_93") || clean == "CODE93" -> BarcodeFormatEnum.CODE_93
            clean.contains("DATA_MATRIX") || clean.contains("DATAMATRIX") -> BarcodeFormatEnum.DATA_MATRIX
            clean.contains("PDF") || clean.contains("PDF_417") -> BarcodeFormatEnum.PDF_417
            clean.contains("AZTEC") -> BarcodeFormatEnum.AZTEC
            clean.contains("UPC_A") || clean == "UPCA" -> BarcodeFormatEnum.UPC_A
            clean.contains("UPC_E") || clean == "UPCE" -> BarcodeFormatEnum.UPC_E
            clean.contains("CODABAR") -> BarcodeFormatEnum.CODABAR
            clean.contains("ITF") -> BarcodeFormatEnum.ITF
            else -> BarcodeFormatEnum.fromString(clean)
        }
    }

    private fun mapCatimaColor(rawColor: String): String {
        if (rawColor.isBlank()) return "blue"

        // 1. Check if it's signed 32-bit integer ARGB (e.g. -769226, -11751600)
        rawColor.toIntOrNull()?.let { intArgb ->
            val color = Color(intArgb)
            return CardColorPalette.findClosestOption(color).id
        }

        // 2. Check if it's a known hex
        CardColorPalette.findOption(rawColor)?.let { return it.id }

        // 3. Parse arbitrary hex
        return try {
            val parsed = CardColorPalette.parseColorSafe(rawColor)
            CardColorPalette.findClosestOption(parsed).id
        } catch (e: Exception) {
            "blue"
        }
    }

    /**
     * Standard RFC 4180 CSV line parser supporting quoted strings and commas.
     */
    private fun parseCsvRows(csvText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        if (csvText.isBlank()) return rows

        val lines = csvText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val row = mutableListOf<String>()
            val sb = StringBuilder()
            var inQuotes = false
            var i = 0

            while (i < line.length) {
                val c = line[i]
                when {
                    c == '\"' -> {
                        if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                            sb.append('\"')
                            i++
                        } else {
                            inQuotes = !inQuotes
                        }
                    }
                    c == ',' && !inQuotes -> {
                        row.add(sb.toString().trim())
                        sb.clear()
                    }
                    else -> {
                        sb.append(c)
                    }
                }
                i++
            }
            row.add(sb.toString().trim())
            rows.add(row)
        }
        return rows
    }
}
