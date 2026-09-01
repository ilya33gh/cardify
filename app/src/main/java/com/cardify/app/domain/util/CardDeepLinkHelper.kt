package com.cardify.app.domain.util

import android.net.Uri
import android.util.Base64
import com.cardify.app.domain.model.LoyaltyCard
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

data class SharedCardPayload(
    val title: String,
    val barcodeValue: String,
    val barcodeFormat: String,
    val colorHex: String,
    val notes: String? = null,
    val categoryName: String? = null
)

object CardDeepLinkHelper {

    private const val WEB_SCHEME = "https"
    private const val WEB_HOST = "ilya33gh.github.io"
    private const val WEB_PATH = "/cardify/import"

    private const val CUSTOM_SCHEME = "cardify"
    private const val CUSTOM_HOST = "import"
    private const val PARAM_DATA = "data"

    /**
     * Compactly and securely packs card properties into an opaque HTTPS URL:
     * https://ilya33gh.github.io/cardify/import?data=<Base64Url(Deflate(JSON))>
     */
    fun createDeepLink(card: LoyaltyCard): String {
        return createBundleDeepLink(listOf(card))
    }

    /**
     * Packs multiple cards into a single encrypted & compressed deep link:
     */
    fun createBundleDeepLink(cards: List<LoyaltyCard>): String {
        if (cards.isEmpty()) return ""

        val jsonString = if (cards.size == 1) {
            val card = cards.first()
            JSONObject().apply {
                put("t", card.title)
                put("b", card.barcodeValue)
                put("f", card.barcodeFormat.name)
                put("c", card.colorHex)
                if (!card.notes.isNullOrBlank()) {
                    put("n", card.notes)
                }
                if (!card.categoryName.isNullOrBlank()) {
                    put("cat", card.categoryName)
                }
            }.toString()
        } else {
            val arr = org.json.JSONArray()
            cards.forEach { card ->
                val obj = JSONObject().apply {
                    put("t", card.title)
                    put("b", card.barcodeValue)
                    put("f", card.barcodeFormat.name)
                    put("c", card.colorHex)
                    if (!card.notes.isNullOrBlank()) {
                        put("n", card.notes)
                    }
                    if (!card.categoryName.isNullOrBlank()) {
                        put("cat", card.categoryName)
                    }
                }
                arr.put(obj)
            }
            JSONObject().apply {
                put("cards", arr)
            }.toString()
        }

        val compressedBytes = compress(jsonString.toByteArray(Charsets.UTF_8))
        val base64 = Base64.encodeToString(
            compressedBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        return Uri.Builder()
            .scheme(WEB_SCHEME)
            .authority(WEB_HOST)
            .path(WEB_PATH)
            .appendQueryParameter(PARAM_DATA, base64)
            .build()
            .toString()
    }

    /**
     * Parses an incoming deep link Uri into a list of cards (single card or bundle).
     */
    fun parseDeepLinkCards(uri: Uri): List<SharedCardPayload> {
        val isCustomScheme = uri.scheme.equals(CUSTOM_SCHEME, ignoreCase = true) &&
                uri.host.equals(CUSTOM_HOST, ignoreCase = true)

        val isWebScheme = (uri.scheme.equals("https", ignoreCase = true) || uri.scheme.equals("http", ignoreCase = true)) &&
                uri.host.equals(WEB_HOST, ignoreCase = true) &&
                (uri.path?.contains("import", ignoreCase = true) == true)

        if (!isCustomScheme && !isWebScheme) {
            return emptyList()
        }

        val dataParam = uri.getQueryParameter(PARAM_DATA)
        if (!dataParam.isNullOrBlank()) {
            val list = decodePayloadList(dataParam)
            if (list.isNotEmpty()) return list
        }

        // Fallback for uncompressed direct parameters
        val fallbackTitle = uri.getQueryParameter("title") ?: ""
        val fallbackBarcode = uri.getQueryParameter("barcode") ?: uri.getQueryParameter("barcodeValue") ?: ""
        val fallbackFormat = uri.getQueryParameter("format") ?: uri.getQueryParameter("formatName") ?: "CODE_128"
        val fallbackColor = uri.getQueryParameter("color") ?: uri.getQueryParameter("colorHex") ?: "blue"
        val fallbackNotes = uri.getQueryParameter("notes")
        val fallbackCategory = uri.getQueryParameter("category")

        if (fallbackBarcode.isNotBlank() || fallbackTitle.isNotBlank()) {
            return listOf(
                SharedCardPayload(
                    title = fallbackTitle.ifBlank { "Карта" },
                    barcodeValue = fallbackBarcode,
                    barcodeFormat = fallbackFormat,
                    colorHex = fallbackColor,
                    notes = fallbackNotes,
                    categoryName = fallbackCategory
                )
            )
        }

        return emptyList()
    }

    fun parseDeepLink(uri: Uri): SharedCardPayload? {
        return parseDeepLinkCards(uri).firstOrNull()
    }

    /**
     * Parses raw text or clipboard content (extracting URLs or raw base64 payloads).
     */
    fun parseRawTextCards(rawText: String): List<SharedCardPayload> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 1. Check if contains URL
        val urlRegex = Regex("""(https?://[^\s]+|cardify://[^\s]+)""", RegexOption.IGNORE_CASE)
        val match = urlRegex.find(trimmed)
        if (match != null) {
            try {
                val uri = Uri.parse(match.value)
                val cards = parseDeepLinkCards(uri)
                if (cards.isNotEmpty()) return cards
            } catch (_: Throwable) { }
        }

        // 2. Check if text contains data=...
        if (trimmed.contains("data=")) {
            val dataPart = trimmed.substringAfter("data=").substringBefore("&").substringBefore(" ").trim()
            val list = decodePayloadList(dataPart)
            if (list.isNotEmpty()) return list
        }

        // 3. Try decoding as raw base64 payload
        val list = decodePayloadList(trimmed)
        if (list.isNotEmpty()) return list

        return emptyList()
    }

    fun parseRawText(rawText: String): SharedCardPayload? {
        return parseRawTextCards(rawText).firstOrNull()
    }

    private fun decodePayloadList(base64Data: String): List<SharedCardPayload> {
        return try {
            val decodedBytes = Base64.decode(base64Data, Base64.URL_SAFE or Base64.NO_WRAP)
            val jsonString = String(decompress(decodedBytes), Charsets.UTF_8)

            if (jsonString.startsWith("[")) {
                val arr = org.json.JSONArray(jsonString)
                val result = mutableListOf<SharedCardPayload>()
                for (i in 0 until arr.length()) {
                    parseCardFromJson(arr.getJSONObject(i))?.let { result.add(it) }
                }
                return result
            }

            val json = JSONObject(jsonString)
            if (json.has("cards")) {
                val arr = json.getJSONArray("cards")
                val result = mutableListOf<SharedCardPayload>()
                for (i in 0 until arr.length()) {
                    parseCardFromJson(arr.getJSONObject(i))?.let { result.add(it) }
                }
                return result
            }

            parseCardFromJson(json)?.let { listOf(it) } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parseCardFromJson(json: JSONObject): SharedCardPayload? {
        val title = json.optString("t", "").ifBlank { json.optString("title", "Карта") }
        val barcode = json.optString("b", "").ifBlank { json.optString("barcode", "") }
        val format = json.optString("f", "").ifBlank { json.optString("format", "CODE_128") }
        val color = json.optString("c", "").ifBlank { json.optString("color", "blue") }
        val notes = json.optString("n", "").ifBlank { json.optString("notes", "") }.takeIf { it.isNotBlank() }
        val category = json.optString("cat", "").ifBlank { json.optString("category", "") }.takeIf { it.isNotBlank() }

        return if (barcode.isNotBlank() || title.isNotBlank()) {
            SharedCardPayload(
                title = title,
                barcodeValue = barcode,
                barcodeFormat = format,
                colorHex = color,
                notes = notes,
                categoryName = category
            )
        } else null
    }

    private fun compress(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        val baos = ByteArrayOutputStream()
        DeflaterOutputStream(baos, deflater).use { it.write(input) }
        return baos.toByteArray()
    }

    private fun decompress(input: ByteArray): ByteArray {
        val inflater = Inflater()
        val bais = ByteArrayInputStream(input)
        val baos = ByteArrayOutputStream()
        InflaterInputStream(bais, inflater).use { iis ->
            val buffer = ByteArray(1024)
            var len: Int
            while (iis.read(buffer).also { len = it } > 0) {
                baos.write(buffer, 0, len)
            }
        }
        return baos.toByteArray()
    }
}
