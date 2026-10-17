package com.ytlibre.app.ui.main

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.ytlibre.app.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de fumée : l'application démarre et affiche la navigation principale
 * même sans clé API configurée (état d'erreur explicite).
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @Test
    fun bottomNavigation_isDisplayedOnLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
        }
    }
}
