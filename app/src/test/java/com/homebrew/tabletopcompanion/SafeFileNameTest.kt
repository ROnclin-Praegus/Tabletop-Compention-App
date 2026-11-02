package com.homebrew.tabletopcompanion

import com.homebrew.tabletopcompanion.ui.screens.safeFileName
import org.junit.Assert.assertEquals
import org.junit.Test

class SafeFileNameTest {
    @Test fun slashesAndOtherForbiddenCharactersBecomeUnderscores() {
        assertEquals("Sir A_B", safeFileName("Sir A/B"))
        assertEquals("a_b_c", safeFileName("a:b*c"))
    }

    @Test fun normalNamesStayTheSame() = assertEquals("Ignis Firebrand", safeFileName("Ignis Firebrand"))

    @Test fun blankNameGetsADefault() = assertEquals("hero", safeFileName("   "))
}
