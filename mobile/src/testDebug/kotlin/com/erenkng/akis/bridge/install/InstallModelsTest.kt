package com.erenkng.akis.bridge.install

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstallModelsTest {

    @Test
    fun parsesHostAndPort() {
        assertEquals(AdbEndpoint("manual", "192.168.1.23", 37415), AdbEndpoint.parse(" 192.168.1.23:37415 "))
        assertEquals("fe80::1", AdbEndpoint.parse("[fe80::1]:5555")?.host)
        assertNull(AdbEndpoint.parse(""))
        assertNull(AdbEndpoint.parse("192.168.1.23"))
        assertNull(AdbEndpoint.parse("192.168.1.23:"))
        assertNull(AdbEndpoint.parse(":5555"))
        assertNull(AdbEndpoint.parse("192.168.1.23:70000"))
        assertNull(AdbEndpoint.parse("192.168.1.23:abc"))
        assertEquals("192.168.1.23:37415", AdbEndpoint.parse("192.168.1.23:37415")?.label)
    }

    @Test
    fun recognisesInstallSuccess() {
        assertEquals(InstallOutcome.Success, InstallOutcome.parse("Success\n"))
        assertEquals(InstallOutcome.Success, InstallOutcome.parse("Performing Streamed Install\r\nSuccess\r\n"))
    }

    @Test
    fun extractsFailureCode() {
        val signature = InstallOutcome.parse(
            "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.erenkng.akis signatures do not match]",
        )
        assertEquals("INSTALL_FAILED_UPDATE_INCOMPATIBLE", (signature as InstallOutcome.Failure).code)

        val bare = InstallOutcome.parse("Error: INSTALL_FAILED_INSUFFICIENT_STORAGE")
        assertEquals("INSTALL_FAILED_INSUFFICIENT_STORAGE", (bare as InstallOutcome.Failure).code)

        val unknown = InstallOutcome.parse("cmd: Can't find service: package")
        assertEquals("UNKNOWN", (unknown as InstallOutcome.Failure).code)
        assertEquals("cmd: Can't find service: package", unknown.output)

        // "Success" bir kelimenin parçasıysa başarı sayılmaz.
        assertTrue(InstallOutcome.parse("NotSuccessful") is InstallOutcome.Failure)
    }

    @Test
    fun normalisesPairingCode() {
        assertEquals("123456", PairingCode.normalize(" 123 456 "))
        assertEquals("123456", PairingCode.normalize("1234567"))
        assertTrue(PairingCode.isValid("123456"))
        assertFalse(PairingCode.isValid("12345"))
        assertFalse(PairingCode.isValid("12a456"))
    }
}
