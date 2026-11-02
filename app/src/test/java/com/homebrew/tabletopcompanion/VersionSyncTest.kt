package com.homebrew.tabletopcompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

/** The app version is written in two files. A forgotten bump in one of them fails this test. */
class VersionSyncTest {
    @Test fun appAndWebVersionsMatch() {
        // Gradle runs unit tests with the app/ folder as working directory.
        val gradle = File("build.gradle").readText()
        val sw = File("../docs/sw.js").readText()
        val appVersion = Regex("""versionName\s+"([^"]+)"""").find(gradle)?.groupValues?.get(1)
        val webVersion = Regex("""SW_VERSION\s*=\s*'v([^']+)'""").find(sw)?.groupValues?.get(1)
        assertNotNull("versionName not found in app/build.gradle", appVersion)
        assertNotNull("SW_VERSION not found in docs/sw.js", webVersion)
        assertEquals("app/build.gradle versionName vs docs/sw.js SW_VERSION", appVersion, webVersion)
    }
}
