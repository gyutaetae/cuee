package com.cuee.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.cuee.data.AppCapabilityRepository
import com.cuee.data.UserSettingsStore
import com.cuee.engine.CueEngine
import com.cuee.engine.CueEngineHost
import com.cuee.engine.FailureReason
import com.cuee.engine.TargetCandidate
import com.cuee.launcher.AppLauncher
import com.cuee.overlay.BubbleOverlayController
import com.cuee.overlay.MaskOverlayController
import com.cuee.overlay.ScrollHintController
import com.cuee.overlay.SpeechPanelController
import com.cuee.overlay.screenBounds
import com.cuee.voice.SpeechController
import com.cuee.voice.TtsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CueAccessibilityService : AccessibilityService(), CueEngineHost {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private lateinit var settingsStore: UserSettingsStore
    private lateinit var engine: CueEngine
    private lateinit var tts: TtsController
    private lateinit var speech: SpeechController
    private lateinit var bubble: BubbleOverlayController
    private lateinit var speechPanel: SpeechPanelController
    private lateinit var mask: MaskOverlayController
    private lateinit var scrollHint: ScrollHintController
    private var lastEventAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsStore = UserSettingsStore(applicationContext)
        val capabilities = AppCapabilityRepository(applicationContext)
        tts = TtsController(applicationContext)
        engine = CueEngine(capabilities, AppLauncher(applicationContext), this)
        speech = SpeechController(
            applicationContext,
            onResult = { text ->
                speechPanel.showThinking()
                engine.handleVoiceText(text)
            },
            onFailure = {
                speechPanel.showFailure("잘 못 들었어요. 다시 말씀해 주세요.")
                speak("잘 못 들었어요. 다시 말씀해 주세요.")
            }
        )
        bubble = BubbleOverlayController(
            context = applicationContext,
            windowManager = windowManager,
            onTap = {
                Log.i(TAG, "Bubble tapped")
                startListening()
            },
            onDismissed = { scope.launch { settingsStore.setBubbleEnabled(false) } },
            onPositionSaved = { side, ratio -> scope.launch { settingsStore.saveBubblePosition(side, ratio) } }
        )
        speechPanel = SpeechPanelController(applicationContext, windowManager) { startListening() }
        mask = MaskOverlayController(applicationContext, windowManager) { engine.cancel() }
        scrollHint = ScrollHintController(applicationContext, windowManager)
        scope.launch {
            settingsStore.settings.collect { settings ->
                if (settings.bubbleEnabled) {
                    bubble.show(settings.bubbleSide, settings.bubbleYRatio)
                } else {
                    bubble.hide()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType ?: return
        if (event.packageName?.toString() == packageName) return
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            type == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            val now = System.currentTimeMillis()
            if (now - lastEventAt < 800) return
            lastEventAt = now
            handler.postDelayed({ engine.onScreenChanged() }, 220)
        }
    }

    override fun onInterrupt() {
        speech.stop()
        clearGuidance()
    }

    override fun onDestroy() {
        super.onDestroy()
        speech.stop()
        tts.shutdown()
        bubble.hide()
        clearGuidance()
        job.cancel()
    }

    private fun startListening() {
        Log.i(TAG, "Start listening requested")
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Record audio permission is not granted")
            speechPanel.showFailure("마이크 권한을 켜주세요.")
            speak("마이크 권한을 켜주세요.")
            return
        }
        Log.i(TAG, "Record audio permission is granted")
        clearGuidance()
        speechPanel.showListening()
        // Avoid playing TTS over the microphone while speech recognition is starting.
        speech.start()
    }

    override fun getRootNode(): AccessibilityNodeInfo? = rootInActiveWindow

    override fun getScreenBounds(): Rect = windowManager.screenBounds()

    override fun showGuidance(candidates: List<TargetCandidate>) {
        speechPanel.hide()
        scrollHint.hide()
        mask.show(candidates)
        scheduleAutoDismiss()
    }

    override fun showScrollHint() {
        speechPanel.hide()
        mask.hide()
        scrollHint.show()
    }

    override fun showFailure(reason: FailureReason, message: String) {
        mask.hide()
        scrollHint.hide()
        speechPanel.showFailure(message)
        speak(message)
    }

    override fun showSensitivePause() {
        mask.hide()
        scrollHint.hide()
        val message = "개인정보가 있는 화면이에요. 직접 확인해 주세요."
        speechPanel.showFailure(message)
        speak("직접 확인해 주세요.")
    }

    override fun clearGuidance() {
        mask.hide()
        scrollHint.hide()
        speechPanel.hide()
        handler.removeCallbacksAndMessages(null)
    }

    override fun speak(text: String) {
        tts.speak(text)
    }

    private fun scheduleAutoDismiss() {
        handler.postDelayed({ speak("눌러주세요.") }, 6_000)
        handler.postDelayed({
            speak("나중에 다시 도와드릴게요.")
            engine.cancel()
        }, 12_000)
    }

    private companion object {
        const val TAG = "CueAccessibilityService"
    }
}
