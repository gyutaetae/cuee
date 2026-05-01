package com.cuee.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore("user_settings")

enum class BubbleSide {
    LEFT,
    RIGHT
}

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val tutorialCompleted: Boolean = false,
    val consentVersion: Int = 1,
    val consentAcceptedAt: Long = 0L,
    val bubbleEnabled: Boolean = true,
    val bubbleSide: BubbleSide = BubbleSide.RIGHT,
    val bubbleYRatio: Float = 0.55f,
    val voiceGuideEnabled: Boolean = true,
    val ttsRate: Float = 1.0f
)

class UserSettingsStore(private val context: Context) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val tutorialCompleted = booleanPreferencesKey("tutorial_completed")
        val consentVersion = intPreferencesKey("consent_version")
        val consentAcceptedAt = longPreferencesKey("consent_accepted_at")
        val bubbleEnabled = booleanPreferencesKey("bubble_enabled")
        val bubbleSide = intPreferencesKey("bubble_side")
        val bubbleYRatio = floatPreferencesKey("bubble_y_ratio")
        val voiceGuideEnabled = booleanPreferencesKey("voice_guide_enabled")
        val ttsRate = floatPreferencesKey("tts_rate")
    }

    val settings: Flow<UserSettings> = context.userSettingsDataStore.data.map { prefs ->
        UserSettings(
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false,
            tutorialCompleted = prefs[Keys.tutorialCompleted] ?: false,
            consentVersion = prefs[Keys.consentVersion] ?: 1,
            consentAcceptedAt = prefs[Keys.consentAcceptedAt] ?: 0L,
            bubbleEnabled = prefs[Keys.bubbleEnabled] ?: true,
            bubbleSide = if ((prefs[Keys.bubbleSide] ?: 1) == 0) BubbleSide.LEFT else BubbleSide.RIGHT,
            bubbleYRatio = prefs[Keys.bubbleYRatio] ?: 0.55f,
            voiceGuideEnabled = prefs[Keys.voiceGuideEnabled] ?: true,
            ttsRate = prefs[Keys.ttsRate] ?: 1.0f
        )
    }

    suspend fun acceptOnboarding() {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.onboardingCompleted] = true
            prefs[Keys.consentVersion] = 1
            prefs[Keys.consentAcceptedAt] = System.currentTimeMillis()
            prefs[Keys.voiceGuideEnabled] = true
            prefs[Keys.bubbleEnabled] = true
        }
    }

    suspend fun completeTutorial() {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.tutorialCompleted] = true
            prefs[Keys.bubbleEnabled] = true
        }
    }

    suspend fun setBubbleEnabled(enabled: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.bubbleEnabled] = enabled
        }
    }

    suspend fun saveBubblePosition(side: BubbleSide, yRatio: Float) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.bubbleSide] = if (side == BubbleSide.LEFT) 0 else 1
            prefs[Keys.bubbleYRatio] = yRatio.coerceIn(0.08f, 0.92f)
        }
    }
}

