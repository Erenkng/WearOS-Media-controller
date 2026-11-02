package com.erenkng.akis.bridge.install

/** Ağda bulunan ya da elle girilen bir ADB servisi. */
data class AdbEndpoint(
    val serviceName: String,
    val host: String,
    val port: Int,
) {
    val label: String get() = "$host:$port"

    companion object {
        /** "192.168.1.23:37415" → uç nokta. Boşluklar yok sayılır; geçersizse null. */
        fun parse(text: String): AdbEndpoint? {
            val trimmed = text.trim()
            val separator = trimmed.lastIndexOf(':')
            if (separator <= 0 || separator == trimmed.lastIndex) return null
            val host = trimmed.substring(0, separator).trim().removePrefix("[").removeSuffix("]")
            val port = trimmed.substring(separator + 1).trim().toIntOrNull() ?: return null
            if (host.isEmpty() || port !in 1..65535) return null
            return AdbEndpoint(serviceName = "manual", host = host, port = port)
        }
    }
}

/** `cmd package install` çıktısının yorumu. */
sealed interface InstallOutcome {
    data object Success : InstallOutcome

    /** @property code ör. `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; tanınmıyorsa `UNKNOWN`. */
    data class Failure(val code: String, val output: String) : InstallOutcome

    companion object {
        private val FAILURE_CODE = Regex("""Failure \[([A-Z0-9_]+)""")
        private val ANY_INSTALL_CODE = Regex("""\b(INSTALL_[A-Z0-9_]+)\b""")

        fun parse(output: String): InstallOutcome {
            val text = output.trim()
            if (text.lineSequence().any { it.trim() == "Success" }) return Success
            val code = FAILURE_CODE.find(text)?.groupValues?.get(1)
                ?: ANY_INSTALL_CODE.find(text)?.groupValues?.get(1)
                ?: "UNKNOWN"
            return Failure(code, text)
        }
    }
}

/** Eşleştirme kodu: saatte gösterilen 6 rakam. */
object PairingCode {
    fun normalize(input: String): String = input.filter { it.isDigit() }.take(6)

    fun isValid(code: String): Boolean = code.length == 6 && code.all { it.isDigit() }
}
