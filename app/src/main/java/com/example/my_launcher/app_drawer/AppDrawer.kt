package com.example.my_launcher.app_drawer

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

class ApplicationInformation {
    var label: String? = null
    var packageName: String? = null
    var icon: Drawable? = null
    var hidden: Boolean? = null
}

class AppDrawer(
    private val activity: Activity,
    private val packageManager: PackageManager,
) {
    var packages: List<ResolveInfo> = listOf()
    var apps = mutableStateListOf<ApplicationInformation>()

    var showAllApps = mutableStateOf(false)

    init {
        createAppList()
    }

    fun launchApp(packageName: String?) {
        if (packageName == null)
            return

        if (packages.find { it.activityInfo.packageName == packageName } == null) {
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        activity.startActivity(launchIntent)
    }

    fun openAppSettings(packageName: String?) {
        if (packageName == null)
            return

        if (packages.find { it.activityInfo.packageName == packageName } == null) {
            return
        }

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.setData(Uri.parse("package:$packageName"))
        activity.startActivity(intent)
    }

    fun hideApp(packageName: String?) {
        val app = apps.find { it.packageName?.lowercase() == packageName?.lowercase() }
        apps.find { it.packageName?.lowercase() == packageName?.lowercase() }?.hidden =
            !app?.hidden!!
    }

    fun createAppList() {
        getPackages()
        val appList = packages
            .map { app ->
                ApplicationInformation()
                    .apply {
                        label = app.loadLabel(packageManager).toString()
                        packageName = app.activityInfo.packageName
                        icon = app.loadIcon(packageManager)
                        val previousApp =
                            apps.find { it.packageName?.lowercase() == packageName!!.lowercase() }
                        hidden = if (previousApp != null) previousApp.hidden else false
                    }
            }
            .sortedWith { a, b ->
                a.label?.uppercase()!!.compareTo(b.label?.uppercase()!!)
            }

        apps.clear()
        appList.forEach {
            apps.add(it)
        }
    }

    private fun getPackages() {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        packages = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
    }
}