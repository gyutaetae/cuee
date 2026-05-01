package com.cuee.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.cuee.data.UserSettings
import com.cuee.data.UserSettingsStore
import com.cuee.overlay.CUE_GREEN
import com.cuee.overlay.circle
import com.cuee.overlay.rounded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)
    private lateinit var settingsStore: UserSettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = UserSettingsStore(applicationContext)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10) showTutorialStep2()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun render() {
        scope.launch {
            val settings = settingsStore.settings.first()
            when {
                !settings.onboardingCompleted -> showOnboarding()
                !isCueAccessibilityEnabled() -> showAccessibilitySetup()
                !settings.tutorialCompleted -> showTutorialStep1()
                else -> showHome(settings)
            }
        }
    }

    private fun showOnboarding() {
        setContentView(baseColumn {
            title("큐가 옆에서 알려드릴게요")
            body("휴대폰 화면에서 어디를 눌러야 할지 알려드려요.")
            body("큐는 대신 누르지 않아요.")
            body("결제와 송금은 대신 하지 않아요.")
            body("개인정보는 휴대폰 안에만 있어요.")
            primaryButton("시작하기") {
                scope.launch {
                    settingsStore.acceptOnboarding()
                    showAccessibilitySetup()
                }
            }
            link("약관 보기") { showTerms() }
        })
    }

    private fun showAccessibilitySetup() {
        setContentView(baseColumn {
            title("화면 위에서 알려드리려면 설정이 필요해요")
            body("큐는 화면의 글자와 버튼 위치를 보고 길을 알려드려요.")
            body("이 권한으로 결제하거나 송금하지 않아요.")
            body("계좌번호, 카드번호, 비밀번호, 인증번호는 저장하지 않아요.")
            spacer(18)
            step("1. 설정에서 큐를 누르세요")
            step("2. 사용을 켜세요")
            primaryButton("설정 열기") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            link("약관 보기") { showTerms() }
        })
    }

    private fun showTutorialStep1() {
        setContentView(baseColumn {
            title("연습해볼게요")
            body("동그라미를 눌러보세요.")
            val bubble = TextView(this@MainActivity).apply {
                text = "큐"
                textSize = 22f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = circle(CUE_GREEN)
                setOnClickListener { showTutorialStep2() }
            }
            addView(bubble, LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(22)
                bottomMargin = dp(22)
            })
        })
    }

    private fun showTutorialStep2() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            setContentView(baseColumn {
                title("말로 도움을 받을 수 있어요")
                body("큐가 말을 듣고 필요한 곳을 찾아드려요.")
                body("들은 말은 길을 찾는 데만 사용해요.")
                primaryButton("마이크 켜기") {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
                }
            })
            return
        }
        setContentView(baseColumn {
            title("무슨 도움이 필요한지 말해보세요")
            body("예시처럼 말하면 큐가 눌러야 할 곳을 찾아드려요.")
            step("물티슈 찾아줘")
            primaryButton("다음") { showTutorialMask() }
        })
    }

    private fun showTutorialMask() {
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.WHITE)
        val target = TextView(this).apply {
            text = "검색창"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = rounded(0xFFEFEFEF.toInt(), dp(8))
            setOnClickListener {
                scope.launch {
                    settingsStore.completeTutorial()
                    showHome(settingsStore.settings.first())
                }
            }
        }
        root.addView(target, FrameLayout.LayoutParams(dp(220), dp(64)).apply {
            gravity = Gravity.CENTER
        })
        val back = TextView(this).apply {
            text = "<"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = circle(Color.WHITE)
            setOnClickListener { showTutorialStep1() }
        }
        root.addView(back, FrameLayout.LayoutParams(dp(52), dp(52)).apply {
            leftMargin = dp(12)
            topMargin = dp(20)
        })
        setContentView(root)
    }

    private fun showHome(settings: UserSettings) {
        setContentView(baseColumn {
            if (!isCueAccessibilityEnabled()) {
                title("큐를 사용하려면 설정을 켜야 해요")
                primaryButton("설정 열기") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                return@baseColumn
            }
            title(if (settings.bubbleEnabled) "큐가 켜져 있어요" else "큐 동그라미가 꺼져 있어요")
            body("화면 옆 동그라미를 누르고 말해보세요.")
            spacer(14)
            step("쿠팡: 상품 찾기, 장바구니 보기")
            step("카카오톡: 메시지 입력, 사진 버튼 찾기")
            step("코레일톡: 예매한 표 보기, 승차권 예매 찾기")
            if (!settings.bubbleEnabled) {
                primaryButton("다시 켜기") {
                    scope.launch { settingsStore.setBubbleEnabled(true) }
                }
            }
            secondaryButton("튜토리얼 다시 보기") { showTutorialStep1() }
            link("약관 보기") { showTerms() }
        })
    }

    private fun showTerms() {
        setContentView(baseColumn {
            title("약관과 안내")
            body("큐는 사용자가 직접 누를 수 있도록 위치를 안내합니다.")
            body("큐는 결제, 송금, 주문 확정, 구매 확정 버튼을 안내하지 않습니다.")
            body("큐는 계좌번호, 카드번호, 비밀번호, 인증번호를 저장하지 않습니다.")
            body("큐는 사용자가 입력한 민감정보를 대신 입력하지 않습니다.")
            body("큐는 화면의 글자, 버튼 이름, 위치를 기기 안에서 분석합니다.")
            body("지원 앱과 화면에 따라 동작하지 않을 수 있습니다.")
            primaryButton("돌아가기") { render() }
        })
    }

    private fun baseColumn(content: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
            setBackgroundColor(Color.WHITE)
            content()
        }
    }

    private fun LinearLayout.title(textValue: String) {
        addView(TextView(this@MainActivity).apply {
            text = textValue
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(22)
        })
    }

    private fun LinearLayout.body(textValue: String) {
        addView(TextView(this@MainActivity).apply {
            text = textValue
            textSize = 19f
            setTextColor(0xFF222222.toInt())
            setLineSpacing(0f, 1.12f)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        })
    }

    private fun LinearLayout.step(textValue: String) {
        addView(TextView(this@MainActivity).apply {
            text = textValue
            textSize = 20f
            setTextColor(Color.BLACK)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFF3F3F3.toInt(), dp(8))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        })
    }

    private fun LinearLayout.primaryButton(textValue: String, onClick: () -> Unit) {
        addView(TextView(this@MainActivity).apply {
            text = textValue
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(CUE_GREEN, dp(8))
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(20)
        })
    }

    private fun LinearLayout.secondaryButton(textValue: String, onClick: () -> Unit) {
        addView(TextView(this@MainActivity).apply {
            text = textValue
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = rounded(0xFFEDEDED.toInt(), dp(8))
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        })
    }

    private fun LinearLayout.link(textValue: String, onClick: () -> Unit) {
        addView(TextView(this@MainActivity).apply {
            text = textValue
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(CUE_GREEN)
            setPadding(dp(8), dp(18), dp(8), dp(8))
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun LinearLayout.spacer(height: Int) {
        addView(View(this@MainActivity), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun isCueAccessibilityEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val services = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return services.any { service ->
            val info = service.resolveInfo.serviceInfo
            info.packageName == packageName && info.name == "com.cuee.service.CueAccessibilityService"
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
