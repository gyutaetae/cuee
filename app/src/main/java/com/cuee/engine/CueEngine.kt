package com.cuee.engine

import android.graphics.Rect
import android.os.Handler
import android.os.Bundle
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import com.cuee.data.AppCapabilityRepository
import com.cuee.launcher.AppLauncher

interface CueEngineHost {
    fun getRootNode(): AccessibilityNodeInfo?
    fun getScreenBounds(): Rect
    fun showGuidance(candidates: List<TargetCandidate>)
    fun showScrollHint()
    fun showFailure(reason: FailureReason, message: String)
    fun showSensitivePause()
    fun clearGuidance()
    fun speak(text: String)
}

class CueEngine(
    private val capabilities: AppCapabilityRepository,
    private val appLauncher: AppLauncher,
    private val host: CueEngineHost
) {
    private val parser = IntentParser()
    private val nodeReader = NodeTreeReader()
    private val safetyGuard = SafetyGuard()
    private val contextDetector = ScreenContextDetector()
    private val nodeMatcher = NodeMatcher(safetyGuard)
    private val retryHandler = Handler(Looper.getMainLooper())
    private var session: RuntimeSession? = null

    fun handleVoiceText(text: String) {
        host.clearGuidance()
        val nodes = nodeReader.read(host.getRootNode())
        val currentSpec = currentSpec(nodes)
        val currentApp = currentSpec?.app
        val parsed = parser.parse(text, currentApp)

        if (parsed.actionId == ActionId.OPEN_APP && parsed.app != null) {
            if (appLauncher.launch(parsed.app)) {
                host.speak("${displayName(parsed.app)}을 열게요.")
            } else {
                host.showFailure(FailureReason.APP_NOT_INSTALLED, "${displayName(parsed.app)} 앱이 보이지 않아요.")
            }
            return
        }

        val app = parsed.app ?: currentApp
        if (app == null || capabilities.findByApp(app) == null) {
            host.showFailure(FailureReason.UNSUPPORTED_APP, "지금은 쿠팡, 카카오톡, 코레일톡에서 도와드릴 수 있어요.")
            return
        }

        val action = parsed.actionId
        if (action == null || parsed.confidence < 0.75f) {
            host.showFailure(FailureReason.UNSUPPORTED_REQUEST, "아직 이 도움은 준비 중이에요.")
            return
        }

        val spec = capabilities.findByApp(app)
        if (safetyGuard.isSensitiveScreen(nodes, spec)) {
            host.showSensitivePause()
            return
        }

        session = RuntimeSession(
            originalCommand = text,
            app = app,
            actionId = action,
            extractedText = parsed.extractedText,
            waitingForChatRoom = action == ActionId.KAKAO_COMPOSE_MESSAGE &&
                contextDetector.detect(app, nodes, false) != ScreenContext.KAKAO_CHAT_ROOM
        )

        if (session?.waitingForChatRoom == true) {
            host.speak("채팅방을 직접 찾아주세요. 찾으면 다음을 알려드릴게요.")
            return
        }

        guideNext()
    }

    fun onScreenChanged() {
        val current = session ?: return
        if (System.currentTimeMillis() - current.startedAt > 60_000 && current.waitingForChatRoom) {
            finish()
            return
        }
        if (current.waitingForChatRoom) {
            val nodes = nodeReader.read(host.getRootNode())
            val context = contextDetector.detect(current.app, nodes, false)
            if (context == ScreenContext.KAKAO_CHAT_ROOM) {
                current.waitingForChatRoom = false
                guideNext()
            }
            return
        }

        val pending = current.pendingTarget ?: return
        when (pending) {
            TargetType.SEARCH_FIELD -> {
                if (tryAutoInput(current, AutoInputPolicy.SEARCH_QUERY_ONLY)) {
                    current.autoInputAttempts = 0
                    current.stepCount += 1
                    current.pendingTarget = null
                    guideNext()
                } else {
                    retryAutoInputOrFail(current)
                }
            }
            TargetType.MESSAGE_INPUT -> {
                if (tryAutoInput(current, AutoInputPolicy.KAKAO_MESSAGE_ONLY)) {
                    current.autoInputAttempts = 0
                    host.speak("보내기 전 내용을 확인해 주세요.")
                    finish(clearOverlay = true)
                } else {
                    retryAutoInputOrFail(current)
                }
            }
            else -> {
                if (current.actionId == ActionId.COUPANG_SEARCH && pending == TargetType.SEARCH_BUTTON) {
                    host.speak("상품은 직접 골라주세요.")
                }
                finish(clearOverlay = true)
            }
        }
    }

    fun cancel() {
        finish(clearOverlay = true)
    }

    private fun guideNext() {
        val current = session ?: return
        if (current.stepCount >= 5) {
            host.speak("여기까지 도와드렸어요. 다음은 직접 확인해 주세요.")
            finish(clearOverlay = true)
            return
        }
        val spec = capabilities.findByApp(current.app)
        val action = spec?.supportedActions?.firstOrNull { it.id == current.actionId }
        if (action == null) {
            host.showFailure(FailureReason.UNSUPPORTED_REQUEST, "아직 이 도움은 준비 중이에요.")
            return
        }
        val target = action.targetSequence.getOrNull(current.stepCount)
        if (target == null) {
            if (current.actionId == ActionId.COUPANG_SEARCH) host.speak("상품은 직접 골라주세요.")
            finish(clearOverlay = true)
            return
        }

        val root = host.getRootNode()
        val nodes = nodeReader.read(root)
        if (safetyGuard.isSensitiveScreen(nodes, spec)) {
            host.showSensitivePause()
            finish(clearOverlay = false)
            return
        }
        val context = contextDetector.detect(current.app, nodes, false)
        val result = nodeMatcher.findCandidates(target, context, nodes, spec, host.getScreenBounds())
        when (result) {
            is CandidateResult.Found -> {
                if (result.candidates.isEmpty()) {
                    host.showFailure(FailureReason.TARGET_NOT_FOUND, "지금 화면에서는 찾지 못했어요.")
                    return
                }
                current.pendingTarget = target
                host.showGuidance(result.candidates)
                host.speak("눌러주세요.")
            }
            CandidateResult.ScrollPossible -> {
                if (current.scrollAttempts < 3) {
                    current.scrollAttempts += 1
                    host.showScrollHint()
                    host.speak("아래로 밀어주세요.")
                } else {
                    host.showFailure(FailureReason.TARGET_NOT_FOUND, "지금 화면에서는 찾지 못했어요.")
                }
            }
            CandidateResult.TooMany -> host.showFailure(FailureReason.TOO_MANY_CANDIDATES, "비슷한 곳이 많아서 정확히 고르기 어려워요.")
            CandidateResult.NotFound -> host.showFailure(FailureReason.TARGET_NOT_FOUND, "지금 화면에서는 찾지 못했어요.")
        }
    }

    private fun tryAutoInput(current: RuntimeSession, requiredPolicy: AutoInputPolicy): Boolean {
        val text = current.extractedText
        if (!safetyGuard.canAutoInput(requiredPolicy, text)) return false
        val node = nodeReader.findFocusedEditable(host.getRootNode()) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        node.recycle()
        return ok
    }

    private fun retryAutoInputOrFail(current: RuntimeSession) {
        current.autoInputAttempts += 1
        if (current.autoInputAttempts < 3) {
            retryHandler.postDelayed({ onScreenChanged() }, 350)
        } else {
            host.showFailure(FailureReason.AUTO_INPUT_FAILED, "직접 입력해 주세요.")
        }
    }

    private fun currentSpec(nodes: List<ScreenNode>): AppCapabilitySpec? {
        val packageName = nodes.firstOrNull { !it.packageName.isNullOrBlank() }?.packageName
        return capabilities.findByPackage(packageName)
    }

    private fun displayName(app: SupportedApp): String = capabilities.findByApp(app)?.displayName ?: when (app) {
        SupportedApp.COUPANG -> "쿠팡"
        SupportedApp.KAKAO -> "카카오톡"
        SupportedApp.KORAIL -> "코레일톡"
    }

    private fun finish(clearOverlay: Boolean = false) {
        retryHandler.removeCallbacksAndMessages(null)
        session = null
        if (clearOverlay) host.clearGuidance()
    }
}
