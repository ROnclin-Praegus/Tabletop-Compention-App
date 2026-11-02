package com.homebrew.tabletopcompanion

import com.homebrew.tabletopcompanion.model.DamageType
import com.homebrew.tabletopcompanion.model.Vitals
import com.homebrew.tabletopcompanion.model.absorbDamage
import org.junit.Assert.assertEquals
import org.junit.Test

// The main damage cases are in docs/rule-cases.json (see RuleCasesTest), shared with the web app.
class DamageResolverTest {
    @Test fun zeroOrNegativeDamageChangesNothing() {
        val start = Vitals(20, 0, 5, 3, 0, 0)
        assertEquals(start, absorbDamage(start, 0, DamageType.PHYSICAL))
        assertEquals(start, absorbDamage(start, -4, DamageType.PHYSICAL))
    }
}
