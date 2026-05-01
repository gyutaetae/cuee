package com.cuee.data

import android.content.Context
import com.cuee.engine.ActionId
import com.cuee.engine.ActionSpec
import com.cuee.engine.AppCapabilitySpec
import com.cuee.engine.AutoInputPolicy
import com.cuee.engine.StopPolicy
import com.cuee.engine.SupportedApp
import com.cuee.engine.TargetType
import org.json.JSONArray

class AppCapabilityRepository(context: Context) {
    private val specs: List<AppCapabilitySpec> = loadSpecs(context)

    fun all(): List<AppCapabilitySpec> = specs

    fun findByPackage(packageName: String?): AppCapabilitySpec? {
        if (packageName.isNullOrBlank()) return null
        return specs.firstOrNull { spec -> spec.packageNames.any { it == packageName } }
    }

    fun findByApp(app: SupportedApp): AppCapabilitySpec? = specs.firstOrNull { it.app == app }

    fun isSupportedPackage(packageName: String?): Boolean = findByPackage(packageName) != null

    private fun loadSpecs(context: Context): List<AppCapabilitySpec> {
        val json = context.assets.open("capabilities/apps.json")
            .bufferedReader()
            .use { it.readText() }
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val app = SupportedApp.valueOf(obj.getString("app"))
            val actionsArray = obj.getJSONArray("supportedActions")
            val actions = (0 until actionsArray.length()).map { index ->
                val action = actionsArray.getJSONObject(index)
                ActionSpec(
                    id = ActionId.valueOf(action.getString("id")),
                    app = app,
                    triggerKeywords = action.getJSONArray("triggerKeywords").toStringList(),
                    targetSequence = action.getJSONArray("targetSequence").toStringList().map { TargetType.valueOf(it) },
                    autoInputPolicy = AutoInputPolicy.valueOf(action.getString("autoInputPolicy")),
                    stopPolicy = StopPolicy.valueOf(action.getString("stopPolicy")),
                    maxSteps = action.optInt("maxSteps", 5)
                )
            }
            AppCapabilitySpec(
                app = app,
                packageNames = obj.getJSONArray("packageNames").toStringList(),
                displayName = obj.getString("displayName"),
                supportedActions = actions,
                blockedKeywords = obj.getJSONArray("blockedKeywords").toStringList(),
                sensitiveKeywords = obj.getJSONArray("sensitiveKeywords").toStringList()
            )
        }
    }
}

private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }

