package com.homebrew.tabletopcompanion

import com.homebrew.tabletopcompanion.utils.GitHubUpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateCheckerTest {
    private fun newer(latest: String, current: String) = GitHubUpdateChecker.isVersionNewer(latest, current)

    @Test fun newerPatchMinorAndMajor() {
        assertTrue(newer("1.6.2", "1.6.1"))
        assertTrue(newer("1.7", "1.6.9"))
        assertTrue(newer("2.0.0", "1.99.99"))
    }

    @Test fun sameOrOlderIsNotNewer() {
        assertFalse(newer("1.6.1", "1.6.1"))
        assertFalse(newer("1.6", "1.6.0"))
        assertFalse(newer("1.5.9", "1.6.0"))
        assertFalse(newer("", "1.0"))
    }

    @Test fun suffixDoesNotDropTheLastNumber() {
        // "1.6.1-beta" used to be read as 1.6 and wrongly looked older than 1.6.0.5
        assertTrue(newer("1.6.2-beta", "1.6.1"))
        assertFalse(newer("1.6.1-beta", "1.6.1"))
    }
}
