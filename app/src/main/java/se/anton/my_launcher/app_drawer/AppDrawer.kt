package se.anton.my_launcher.app_drawer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import se.anton.my_launcher.FileManager

class ApplicationInformation {
    var label: String? = null
    var packageName: String? = null
    var icon: Drawable? = null
    var hidden: Boolean? = null
}

class AppDrawer(
    private val activity: Activity,
    private val packageManager: PackageManager,
    context: Context
) {
    var packages: List<ResolveInfo> = listOf()
    var apps = mutableStateListOf<ApplicationInformation>()
    var showAllApps = mutableStateOf(false)

    private val fileManager = FileManager(context)

    init {
        createAppList()
    }

    fun launchApp(packageName: String?) {
        if (packageName == null)
            return

        if (packages.find { it.activityInfo.packageName == packageName } == null) {
            return
        }

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            activity.startActivity(launchIntent)
        } catch (e: Exception) {
            createAppList()
        }
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

        if (packageName != null && !app?.hidden!!) {
            fileManager.hidePackage(packageName)

            apps.find { it.packageName?.lowercase() == packageName.lowercase() }?.hidden = true
        } else if (packageName != null && app?.hidden!!) {
            fileManager.showPackage(packageName)

            apps.find { it.packageName?.lowercase() == packageName.lowercase() }?.hidden = false
        }
    }

    fun createAppList() {
        getPackages()
        fileManager.validateHiddenPackages(packages.map { it.activityInfo.packageName })
        val appList = packages
            .map { app ->
                ApplicationInformation()
                    .apply {
                        label = app.loadLabel(packageManager).toString()
                        packageName = app.activityInfo.packageName
                        icon = app.loadIcon(packageManager)
                        hidden = fileManager.getHiddenPackages()
                            .contains(app.activityInfo.packageName)
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