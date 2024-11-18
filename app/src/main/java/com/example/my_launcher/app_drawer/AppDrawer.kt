package com.example.my_launcher.app_drawer

import android.R.attr.maxHeight
import android.R.attr.maxWidth
import android.R.attr.minHeight
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateListOf

class AppDrawer(
    private val activity: Activity,
    private val applicationContext: Context,
    private val packageManager: PackageManager,
    private val requestWidgetPermissionsLauncher: ActivityResultLauncher<Intent>
) {
    class ApplicationInformation {
        var label: String? = null
        var packageName: String? = null
        var icon: Drawable? = null
        var hidden: Boolean? = null
    }

    var apps = mutableStateListOf<ApplicationInformation>()
    var alphabet = mutableStateListOf<String>()

    lateinit var widgetHost: AppWidgetHost
    lateinit var widgetManager: AppWidgetManager
    var widgetId: Int = 0
    lateinit var duoWidget: AppWidgetProviderInfo
    lateinit var options: Bundle
    lateinit var hostView: AppWidgetHostView

    fun createDuolingoWidget() {
        widgetHost = AppWidgetHost(applicationContext, 0)
        widgetHost.startListening()
        widgetManager = AppWidgetManager.getInstance(applicationContext)
        duoWidget = widgetManager.installedProviders.find {
            it.activityInfo.name.contains("com.duolingo.streak.streakWidget.MediumStreakWidgetProvider")
        }!!
        widgetId = widgetHost.allocateAppWidgetId()

        options = Bundle()
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, maxWidth)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight)

        if (!widgetManager.bindAppWidgetIdIfAllowed(widgetId, duoWidget.provider)) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, duoWidget.provider)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)

            requestWidgetPermissionsLauncher.launch(intent)
        }

        hostView = widgetHost.createView(applicationContext, widgetId, duoWidget)
        hostView.setAppWidget(widgetId, duoWidget)
    }

    fun deleteWidget() {
        widgetHost.stopListening()
        widgetHost.deleteAppWidgetId(widgetId)
    }

    fun launchApp(packageName: String?) {
        if (packageName == null)
            return

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        activity.startActivity(launchIntent)
    }

    fun hideApp(packageName: String?) {
        val app = apps.find { it.packageName?.lowercase() == packageName?.lowercase() }
        apps.find { it.packageName?.lowercase() == packageName?.lowercase() }?.hidden = !app?.hidden!!
    }

    fun uninstallApp(packageName: String?) {
        if (packageName == null)
            return

        val packageIntent = Intent(Intent.ACTION_MAIN, null)
        packageIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages: List<ResolveInfo> = packageManager.queryIntentActivities(packageIntent, PackageManager.GET_META_DATA)
        val app = packages.find { it.activityInfo.packageName.lowercase() == packageName.lowercase() }

        if (app == null)
            return

        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:${app.activityInfo.packageName}")
        activity.startActivity(intent)
    }

    fun getPackages(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
    }

    fun createAppList() {
        val packages = getPackages()

        val appList = mutableListOf<ApplicationInformation>()
        packages.forEach { app ->
            val appInfo = ApplicationInformation()
            appInfo.label = app.loadLabel(packageManager).toString()
            appInfo.packageName = app.activityInfo.packageName
            appInfo.icon = app.loadIcon(packageManager)
            val previousApp = apps.find { it.packageName?.lowercase() == appInfo.packageName!!.lowercase() }
            if (previousApp != null)
                appInfo.hidden = previousApp.hidden
            else
                appInfo.hidden = false

            appList.add(appInfo)
        }
        appList.sortWith { a, b ->
            a.label?.uppercase()!!.compareTo(b.label?.uppercase()!!)
        }

        apps.clear()
        appList.forEach {
            apps.add(it)
        }

        createAlphabetList(apps)
    }

    fun createAlphabetList(apps: List<ApplicationInformation>) {
        val tempAlphabet = "1234567890qwertyuiopasdfghjklzxcvbnm".split("").dropLast(1).toMutableList()
        val alphabetList = tempAlphabet.subList(1, tempAlphabet.size)
        alphabetList.sortWith { a, b ->
            a.compareTo(b)
        }
        alphabetList.add("å")
        alphabetList.add("ä")
        alphabetList.add("ö")

        val removeLetters = mutableListOf<String>()
        alphabetList.forEach { letter ->
            var result = false
            apps.forEach { app ->
                if (!result) {
                    if (app.label != null && app.label!![0].uppercaseChar() == letter.toCharArray()[0].uppercaseChar()) {
                        result = true
                    }
                }
            }

            if (!result) {
                removeLetters.add(letter)
            }
        }

        removeLetters.forEach { letter ->
            alphabetList.remove(letter)
        }

        alphabet.clear()
        alphabetList.forEach {
            alphabet.add(it)
        }
    }
}