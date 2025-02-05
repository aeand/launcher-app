package com.example.my_launcher.app_drawer

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppDrawer(
    private val activity: Activity,
    private val packageManager: PackageManager,
    private val customScope: CoroutineScope,
) {
    class ApplicationInformation {
        var label: String? = null
        var packageName: String? = null
        var icon: Drawable? = null
        var hidden: Boolean? = null
    }

    var packages: List<ResolveInfo> = listOf()
    var apps = mutableStateListOf<ApplicationInformation>()
    var alphabet = mutableStateListOf<String>()
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

    fun hideApp(packageName: String?) {
        val app = apps.find { it.packageName?.lowercase() == packageName?.lowercase() }
        apps.find { it.packageName?.lowercase() == packageName?.lowercase() }?.hidden =
            !app?.hidden!!

        createAlphabetList(apps)
    }

    fun uninstallApp(packageName: String?) {
        if (packageName == null)
            return

        val packageIntent = Intent(Intent.ACTION_MAIN, null)
        packageIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages: List<ResolveInfo> =
            packageManager.queryIntentActivities(packageIntent, PackageManager.GET_META_DATA)
        val app =
            packages.find { it.activityInfo.packageName.lowercase() == packageName.lowercase() }

        if (app == null)
            return

        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:${app.activityInfo.packageName}")
        activity.startActivity(intent)

        customScope.launch {
            delay(2500)
            createAppList()
        }
    }

    fun scrollToFirstItem(letter: String, lazyScroll: LazyListState) {
        customScope.launch {

            apps.forEachIndexed { index, app ->
                if (app.label!![0].uppercaseChar() == letter.toCharArray()[0].uppercaseChar()) {
                    lazyScroll.animateScrollToItem(index)
                    return@launch
                }
            }
        }
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

        createAlphabetList(apps)
    }

    private fun getPackages() {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        packages = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
    }

    private fun createAlphabetList(apps: List<ApplicationInformation>) {
        val letters = "1234567890qwertyuiopasdfghjklzxcvbnm"
            .split("")
            .dropLast(1)
            .drop(1)
            .toMutableList()

        letters.sortWith { a, b ->
            a.compareTo(b)
        }

        letters.add("å")
        letters.add("ä")
        letters.add("ö")

        val filteredLetters = letters.filter { letter ->
            apps.find { app -> app.label != null && app.label!![0].uppercaseChar() == letter.toCharArray()[0].uppercaseChar() && (app.hidden == false || showAllApps.value) } != null
        }

        alphabet.clear()
        filteredLetters.forEach {
            alphabet.add(it)
        }
    }
}