package com.cardify.app.data.local.entities

enum class BarcodeFormatEnum(val displayName: String, val is2D: Boolean) {
    // 1D Barcodes
    EAN_13("EAN-13", false),
    EAN_8("EAN-8", false),
    CODE_128("Code 128", false),
    CODE_39("Code 39", false),
    CODE_93("Code 93", false),
    UPC_A("UPC-A", false),
    UPC_E("UPC-E", false),
    CODABAR("Codabar", false),
    ITF("ITF (Interleaved 2 of 5)", false),

    // 2D Barcodes
    QR_CODE("QR Code", true),
    DATA_MATRIX("Data Matrix", true),
    AZTEC("Aztec", true),
    PDF_417("PDF 417", true);

    companion object {
        fun fromString(name: String?): BarcodeFormatEnum {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: CODE_128
        }
    }
}
