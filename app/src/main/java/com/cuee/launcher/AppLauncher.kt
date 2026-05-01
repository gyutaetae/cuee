package com.cuee.launcher

import android.content.Context
import com.cuee.engine.SupportedApp

class AppLauncher(private val context: Context) {
    private val packages = mapOf(
        SupportedApp.COUPANG to "com.coupang.mobile",
        SupportedApp.KAKAO to "com.kakao.talk",
        SupportedApp.KORAIL to "com.korail.talk"
    )

    fun launch(app: SupportedApp): Boolean {
        val packageName = packages[app] ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}

