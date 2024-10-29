package com.example.my_launcher

import android.R.attr.maxHeight
import android.R.attr.maxWidth
import android.R.attr.minHeight
import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Start
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/* TODO
- duo widget doesn't always get the update connection. Or doesn't update on new images and such
- refresh app list after uninstall and install
- Fix app select background (currently grey)
- set text color dynamically depending on background color
- blur background when list is open
- make home button open the wallpaper view
- add a notes feature on swipe right
- make it swipeable to open the status bar by using permission EXPAND_STATUS_BAR (use setExpandNotificationDrawer(true))
- Handle back button event, BackHandler { }
*/

/* Inspiration
https://www.youtube.com/watch?v=aVg3RkfNtqE
https://medium.com/@muhammadzaeemkhan/top-9-open-source-android-launchers-you-need-to-try-56c5f975e2f8
*/

class MainActivity : ComponentActivity() {
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)
    private lateinit var receiver: BroadcastReceiver

    private var date: String = ""
    private var apps: MutableList<ApplicationInformation>? = null
    private lateinit var lazyScroll: LazyListState
    private lateinit var batteryManager: BatteryManager
    private lateinit var isCharging: MutableState<Boolean>
    private lateinit var batteryTextColor: MutableState<Color>

    private lateinit var widgetHost: AppWidgetHost
    private lateinit var widgetManager: AppWidgetManager
    private var widgetId: Int = 0
    private lateinit var duoWidget: AppWidgetProviderInfo
    private lateinit var options: Bundle
    private lateinit var hostView: AppWidgetHostView

    private var requestWidgetPermissionsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        println(result)
        if (result.resultCode == RESULT_OK) {
            println("onActivityResult: ${widgetManager.bindAppWidgetIdIfAllowed(widgetId, duoWidget.provider, options)}")
            if (widgetManager.bindAppWidgetIdIfAllowed(widgetId, duoWidget.provider, options)) {
                hostView = widgetHost.createView(applicationContext, widgetId, duoWidget)
                hostView.setAppWidget(widgetId, duoWidget)
            }
        }
    }

    class ApplicationInformation {
        var label: String? = null
        var packageName: String? = null
        var icon: Drawable? = null
        var hidden: Boolean? = null
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val filters = IntentFilter()
        filters.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        filters.addAction(Intent.ACTION_DATE_CHANGED)
        filters.addAction(Intent.ACTION_PACKAGE_ADDED)
        filters.addAction(Intent.ACTION_UNINSTALL_PACKAGE)
        filters.addAction(Intent.ACTION_INSTALL_PACKAGE)
        filters.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filters.addAction(Intent.ACTION_PACKAGE_REPLACED)
        filters.addAction(Intent.ACTION_PACKAGE_INSTALL)
        filters.addAction(Intent.ACTION_BATTERY_CHANGED)
        filters.addAction(Intent.ACTION_BATTERY_LOW)
        filters.addAction(Intent.ACTION_BATTERY_OKAY)

        receiver = object:BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    // TODO go through this list of intents to find useful intents for this project
                    // https://developer.android.com/reference/kotlin/android/content/Intent#action_airplane_mode_changed

                    Intent.ACTION_APPLICATION_LOCALE_CHANGED -> { // broadcast some app locale has changed
                        println("ACTION_APPLICATION_LOCALE_CHANGED")
                        createAppList()
                    }

                    Intent.ACTION_APPLICATION_PREFERENCES -> { // intent for adjusting app preferences. recommended for all apps with settings
                        println("ACTION_APPLICATION_PREFERENCES")
                    }

                    Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED -> { // broadcast app restrictions changed
                        println("ACTION_APPLICATION_RESTRICTIONS_CHANGED")
                    }

                    Intent.ACTION_APP_ERROR -> { // user pressed report after app crash
                        println("ACTION_APP_ERROR")
                    }

                    Intent.ACTION_ATTACH_DATA -> { // some data should be attached somewhere else. Like image for contact
                        println("ACTION_ATTACH_DATA")
                    }

                    Intent.ACTION_AUTO_REVOKE_PERMISSIONS -> { // Launch UI to manage auto-revoke state
                        println("ACTION_AUTO_REVOKE_PERMISSIONS")
                    }

                    Intent.ACTION_BATTERY_CHANGED -> { // sticky broadcast Has charging state, level, and more info
                        println("ACTION_BATTERY_CHANGED")
                        isCharging.value = batteryManager.isCharging
                    }

                    Intent.ACTION_BATTERY_LOW -> { // broadcast battery is low
                        println("ACTION_BATTERY_LOW")
                        batteryTextColor.value = Color.Red
                    }

                    Intent.ACTION_BATTERY_OKAY -> { // broadcast battery is okay after being low
                        println("ACTION_BATTERY_OKAY")
                        batteryTextColor.value = Color.White
                    }

                    Intent.ACTION_BOOT_COMPLETED -> { // broadcast permissions only once after the user has finished booting
                        println("ACTION_BOOT_COMPLETED")
                    }

                    Intent.ACTION_BUG_REPORT -> { // Show activity for reporting a bug
                        println("ACTION_BUG_REPORT")
                    }

                    Intent.ACTION_CAMERA_BUTTON -> { // broadcast camera button is pressed. Broadcast includes EXTRA_KEY_EVENT
                        println("ACTION_CAMERA_BUTTON")
                    }

                    Intent.ACTION_CHOOSER -> { // opens an activity picker. Alternative to the standard picker.
                        println("ACTION_CHOOSER")
                    }

                    Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> { // broadcast user action to dismiss temporary system dialog.
                        println("ACTION_CLOSE_SYSTEM_DIALOGS")
                        customScope.launch {
                            lazyScroll.scrollToItem(0)
                            //TODO also make this scroll the anchorDraggable back to start
                        }
                    }

                    Intent.ACTION_CONFIGURATION_CHANGED -> { // broadcast orientation, locale, etc has changed. UI will be rebuilt aka. system will stop and start app
                        println("ACTION_CONFIGURATION_CHANGED")
                    }

                    Intent.ACTION_CREATE_DOCUMENT -> { // creates new document with system picker. Lots more info
                        println("ACTION_CREATE_DOCUMENT")
                    }

                    Intent.ACTION_CREATE_NOTE -> { // start note-taking activity. can run in lockscreen
                        println("ACTION_CREATE_NOTE")
                    }

                    Intent.ACTION_CREATE_REMINDER -> { // EXTRA_TITLE, EXTRA_TEXT, EXTRA_TIME
                        //TODO implement this! Seems like a great way to remind me of stuff without using alarms all the time
                        println("ACTION_CREATE_REMINDER")
                    }

                    Intent.ACTION_CREATE_SHORTCUT -> { // create shortcut. SHORTCUT_NAME, SHORTCUT_ICON, SHORTCUT_ICON_RESOURCE
                        println("ACTION_CREATE_SHORTCUT")
                    }

                    Intent.ACTION_DATE_CHANGED -> { // broadcast date has changed
                        println("ACTION_DATE_CHANGED")
                        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
                    }

                    Intent.ACTION_DEFAULT -> { // same as ACTION_VIEW
                        println("ACTION_DEFAULT")
                    }

                    Intent.ACTION_DEFINE -> { // define meaning of word(s)
                        println("ACTION_DEFINE")
                    }

                    Intent.ACTION_DELETE -> { // Delete the given data from its container
                        println("ACTION_DELETE")
                    }

                    Intent.ACTION_DEVICE_STORAGE_LOW -> { // sticky broadcast deprecated indicates low storage on device
                        println("ACTION_DEVICE_STORAGE_LOW")
                    }

                    Intent.ACTION_DEVICE_STORAGE_OK -> { // sticky broadcast deprecated indicates ok storage after low storage on device
                        println("ACTION_DEVICE_STORAGE_OK")
                    }

                    Intent.ACTION_DOCK_EVENT -> { // sticky broadcast changes in physical docking state
                        println("ACTION_DOCK_EVENT")
                    }

                    // dreams are interactive screen savers. can be custom
                    Intent.ACTION_DREAMING_STARTED -> { // broadcast system started dreaming
                        println("ACTION_DREAMING_STARTED")
                    }

                    Intent.ACTION_DREAMING_STOPPED -> { // broadcast system stopped dreaming
                        println("ACTION_DREAMING_STOPPED")
                    }

                    Intent.ACTION_EDIT -> { // give edit access to data
                        println("ACTION_EDIT")
                    }

                    Intent.ACTION_GET_CONTENT -> { // let user pick some data to send to your app
                        println("ACTION_GET_CONTENT")
                    }

                    Intent.ACTION_HEADSET_PLUG -> { // broadcast wired headset plugged in or out
                        println("ACTION_HEADSET_PLUG")
                    }

                    Intent.ACTION_INPUT_METHOD_CHANGED -> { // broadcast input method changed
                        println("ACTION_INPUT_METHOD_CHANGED")
                    }

                    Intent.ACTION_INSTALL_FAILURE -> { // activity to handle split installation fails
                        println("ACTION_INSTALL_FAILURE")
                    }

                    Intent.ACTION_INSTALL_PACKAGE -> { // deprecated launch app installer
                        println("ACTION_INSTALL_PACKAGE")
                    }

                    //TODO look into, seems cool
                    Intent.ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE -> { // Use with startActivityForResult to start a system activity that captures content on the screen to take a screenshot and present it to the user for editing
                        println("ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE")
                    }

                    Intent.ACTION_LOCALE_CHANGED -> { // broadcast device locale changed
                        println("ACTION_LOCALE_CHANGED")
                    }

                    Intent.ACTION_LOCKED_BOOT_COMPLETED -> { // broadcast when device has booted but still is in locked state
                        println("ACTION_LOCKED_BOOT_COMPLETED")
                    }

                    Intent.ACTION_MAIN -> { // Start as a main entry point, does not expect to receive data
                        println("ACTION_MAIN")
                    }

                    Intent.ACTION_MANAGED_PROFILE_UNLOCKED -> { // broadcast received by primary user when a managed profile is unlocked. There are more profile related actions
                        println("ACTION_MANAGED_PROFILE_UNLOCKED")
                    }

                    Intent.ACTION_MANAGE_NETWORK_USAGE -> { // shows settings for managing network data usage
                        println("ACTION_MANAGE_NETWORK_USAGE")
                    }

                    Intent.ACTION_MANAGE_PACKAGE_STORAGE -> { // broadcast Indicates low memory condition notification acknowledged by user
                        println("ACTION_MANAGE_PACKAGE_STORAGE")
                    }

                    Intent.ACTION_MANAGE_UNUSED_APPS -> { // opens UI to handle unused apps
                        println("ACTION_MANAGE_UNUSED_APPS")
                    }

                    Intent.ACTION_MEDIA_BAD_REMOVAL -> { // broadcast SD card removed from slot but mount point was not unmounted
                        println("ACTION_MEDIA_BAD_REMOVAL")
                    }

                    // TODO check what buttons are media buttons
                    Intent.ACTION_MEDIA_BUTTON -> { // media button was pressed. contains EXTRA_KEY_EVENT
                        println("ACTION_MEDIA_BUTTON")
                    }

                    Intent.ACTION_MEDIA_CHECKING -> { // broadcast external media is present
                        println("ACTION_MEDIA_CHECKING")
                    }

                    Intent.ACTION_MEDIA_EJECT -> { // broadcast user desires to remove media. app must obey
                        println("ACTION_MEDIA_EJECT")
                    }

                    Intent.ACTION_MEDIA_MOUNTED -> { // broadcast External media is present and mounted at its mount point
                        println("ACTION_MEDIA_MOUNTED")
                    }

                    Intent.ACTION_MEDIA_NOFS -> { // broadcast External media is present, but is using an incompatible fs (probably means filesystem)
                        println("ACTION_MEDIA_NOFS")
                    }

                    Intent.ACTION_MEDIA_REMOVED -> { // broadcast External media has been removed
                        println("ACTION_MEDIA_REMOVED")
                    }

                    Intent.ACTION_MEDIA_SCANNER_FINISHED -> { // broadcast The media scanner has finished scanning a directory
                        println("ACTION_MEDIA_SCANNER_FINISHED")
                    }

                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE -> { // broadcast request scan of file and add to DB
                        println("ACTION_MEDIA_SCANNER_SCAN_FILE")
                    }

                    Intent.ACTION_MEDIA_SCANNER_STARTED -> { // broadcast media scanner started
                        println("ACTION_MEDIA_SCANNER_STARTED")
                    }

                    Intent.ACTION_MEDIA_SHARED -> { // broadcast external media is unmounted because it is being shared via USB mass storage
                        println("ACTION_MEDIA_SHARED")
                    }

                    Intent.ACTION_MEDIA_UNMOUNTABLE -> { // broadcast External media is present but cannot be mounted
                        println("ACTION_MEDIA_UNMOUNTABLE")
                    }

                    Intent.ACTION_MEDIA_UNMOUNTED -> { // broadcast External media is present, but not mounted at its mount point
                        println("ACTION_MEDIA_UNMOUNTED")
                    }

                    //TODO this seems important
                    Intent.ACTION_MY_PACKAGE_REPLACED -> { // broadcast a new version of your app has been installed over an existing one
                        println("ACTION_MY_PACKAGE_REPLACED")
                    }

                    Intent.ACTION_MY_PACKAGE_SUSPENDED -> { // broadcast Sent to a package that has been suspended by the system
                        println("ACTION_MY_PACKAGE_SUSPENDED")
                    }

                    Intent.ACTION_MY_PACKAGE_UNSUSPENDED -> { // broadcast Sent to a package that has been unsuspended
                        println("ACTION_MY_PACKAGE_UNSUSPENDED")
                    }

                    Intent.ACTION_OPEN_DOCUMENT -> { // allows user to pick doc(s) via system UI
                        println("ACTION_OPEN_DOCUMENT")
                    }

                    Intent.ACTION_OPEN_DOCUMENT_TREE -> { // Allow the user to pick a directory subtree
                        println("ACTION_OPEN_DOCUMENT_TREE")
                    }

                    Intent.ACTION_PACKAGES_SUSPENDED -> { // broadcast packages have been suspended
                        println("ACTION_PACKAGES_SUSPENDED")
                    }

                    Intent.ACTION_PACKAGES_UNSUSPENDED -> { // broadcast Packages have been unsuspended
                        println("ACTION_PACKAGES_UNSUSPENDED")
                    }

                    Intent.ACTION_PACKAGE_ADDED -> { // broadcast new package installed. contains data with name
                        println("ACTION_PACKAGE_ADDED")
                        createAppList()
                    }

                    Intent.ACTION_PACKAGE_CHANGED -> { // broadcast a package has been changed
                        println("ACTION_PACKAGE_CHANGED")
                    }

                    Intent.ACTION_PACKAGE_DATA_CLEARED -> { // broadcast user has cleared the data of a package
                        println("ACTION_PACKAGE_DATA_CLEARED")
                    }

                    Intent.ACTION_PACKAGE_FIRST_LAUNCH -> { // broadcast app launched for the first time
                        println("ACTION_PACKAGE_FIRST_LAUNCH")
                    }

                    Intent.ACTION_PACKAGE_FULLY_REMOVED -> { // broadcast package has been completely removed from device
                        println("ACTION_PACKAGE_FULLY_REMOVED")
                        createAppList()
                    }

                    Intent.ACTION_PACKAGE_INSTALL -> { // deprecated broadcast trigger download and install of package
                        println("ACTION_PACKAGE_INSTALL")
                    }

                    Intent.ACTION_PACKAGE_NEEDS_VERIFICATION -> { // broadcast to the system package verifier when a package needs to be verified
                        println("ACTION_PACKAGE_NEEDS_VERIFICATION")
                    }

                    Intent.ACTION_PACKAGE_REMOVED -> { // broadcast app has been removed
                        println("ACTION_PACKAGE_REMOVED")
                        createAppList()
                    }

                    Intent.ACTION_PACKAGE_REPLACED -> { // broadcast new version of an application package has been installed
                        println("ACTION_PACKAGE_REPLACED")
                        createAppList()
                    }

                    Intent.ACTION_PACKAGE_RESTARTED -> { // broadcast user has restarted a package
                        println("ACTION_PACKAGE_RESTARTED")
                    }

                    /*Intent.ACTION_PACKAGE_UNSTOPPED -> { // apparently doesn't exist
                        println("ACTION_PACKAGE_UNSTOPPED")
                    }*/

                    Intent.ACTION_PACKAGE_VERIFIED -> { // broadcast Sent to the system package verifier when a package is verified
                        println("ACTION_PACKAGE_VERIFIED")
                    }

                    Intent.ACTION_PASTE -> { // create item in given container, initializing it from the current clipboard
                        println("ACTION_PASTE")
                    }

                    Intent.ACTION_POWER_CONNECTED -> { // broadcast External power has been connected to the device. will wake device
                        println("ACTION_POWER_CONNECTED")
                    }

                    Intent.ACTION_POWER_DISCONNECTED -> { // broadcast External power has been removed from device
                        println("ACTION_POWER_DISCONNECTED")
                        isCharging.value = false
                    }

                    Intent.ACTION_POWER_USAGE_SUMMARY -> { // shows power usage info to user
                        println("ACTION_POWER_USAGE_SUMMARY")
                    }

                    Intent.ACTION_PROCESS_TEXT -> { // process a piece of text. don't know more than that
                        println("ACTION_PROCESS_TEXT")
                    }

                    Intent.ACTION_PROVIDER_CHANGED -> { // broadcast providers content changed
                        println("ACTION_PROVIDER_CHANGED")
                    }

                    Intent.ACTION_QUICK_CLOCK -> { // sent when the user taps on the clock widget in the system's "quick settings" area
                        println("ACTION_QUICK_CLOCK")
                    }

                    Intent.ACTION_QUICK_VIEW -> { // Quick view the data. Launches a quick viewer for a URI or a list of URIs
                        println("ACTION_QUICK_VIEW")
                    }

                    Intent.ACTION_REBOOT -> { // broadcast have the device reboot. Only for system code
                        println("ACTION_REBOOT")
                    }

                    Intent.ACTION_RUN -> { // "Run the data, whatever that means." real documentation xD
                        println("ACTION_RUN")
                    }

                    Intent.ACTION_SAFETY_CENTER -> { // launch UI for safety center
                        println("ACTION_SAFETY_CENTER")
                    }

                    Intent.ACTION_SCREEN_OFF -> { // broadcast device goes to sleep and becomes non-interactive
                        println("ACTION_SCREEN_OFF")
                    }

                    Intent.ACTION_SCREEN_ON -> { // broadcast device wakes up and becomes interactive
                        println("ACTION_SCREEN_ON")
                    }

                    //TODO could be useful for a system search
                    Intent.ACTION_SEARCH -> { // perform a search
                        println("ACTION_SEARCH")
                    }

                    Intent.ACTION_SEARCH_LONG_PRESS -> { // start action associated with long pressing on a search key
                        println("ACTION_SEARCH_LONG_PRESS")
                    }

                    Intent.ACTION_SEND -> { // Deliver some data to someone else. Who the data is being delivered to is not specified; it is up to the receiver of this action to ask the user where the data should be sent
                        println("ACTION_SEND")
                    }

                    Intent.ACTION_SENDTO -> { // send a message to someone specified by the data
                        println("ACTION_SENDTO")
                    }

                    Intent.ACTION_SEND_MULTIPLE -> { // deliver multiple data to someone else
                        println("ACTION_SEND_MULTIPLE")
                    }

                    //TODO look into
                    Intent.ACTION_SET_WALLPAPER -> { // show settings for choosing wallpaper
                        println("ACTION_SET_WALLPAPER")
                    }

                    Intent.ACTION_SHOW_APP_INFO -> { // will show app information 
                        println("ACTION_SHOW_APP_INFO")
                    }

                    Intent.ACTION_SHOW_WORK_APPS -> { // shows all work apps in the launcher
                        println("ACTION_SHOW_WORK_APPS")
                    }

                    //TODO could be useful
                    Intent.ACTION_SHUTDOWN -> { // broadcast device is shutting down
                        println("ACTION_SHUTDOWN")
                    }

                    Intent.ACTION_SYNC -> { // Perform a data synchronization
                        println("ACTION_SYNC")
                    }

                    Intent.ACTION_SYSTEM_TUTORIAL -> { // start the platform-defined tutorial
                        println("ACTION_SYSTEM_TUTORIAL")
                    }

                    Intent.ACTION_TIMEZONE_CHANGED -> { // broadcast timezone has changed. includes EXTRA_TIMEZONE
                        println("ACTION_TIMEZONE_CHANGED")
                    }

                    Intent.ACTION_TIME_CHANGED -> { // broadcast The time was set
                        println("ACTION_TIME_CHANGED")
                    }

                    Intent.ACTION_TIME_TICK -> { // broadcast time has changed. sent every minute. only receiver
                        println("ACTION_TIME_TICK")
                    }

                    Intent.ACTION_TRANSLATE -> { // perform text translation
                        println("ACTION_TRANSLATE")
                    }

                    Intent.ACTION_UID_REMOVED -> { // broadcast a uid has been removed from the system. includes EXTRA_UID and EXTRA_REPLACING
                        println("ACTION_UID_REMOVED")
                    }

                    Intent.ACTION_UMS_CONNECTED -> { // deprecated broadcast the device has entered USB mass storage mode
                        println("ACTION_UMS_CONNECTED")
                    }

                    Intent.ACTION_UMS_DISCONNECTED -> { // deprecated broadcast the device has exited USB mass storage mode
                        println("ACTION_UMS_DISCONNECTED")
                    }

                    /*Intent.ACTION_UNARCHIVE_PACKAGE -> { // broadcast sent to the responsible installer. archived package when unarchival is requested
                        println("ACTION_UNARCHIVE_PACKAGE")
                    }*/

                    Intent.ACTION_UNINSTALL_PACKAGE -> { // deprecated broadcast launch app uninstaller
                        println("ACTION_UNINSTALL_PACKAGE")
                    }

                    Intent.ACTION_USER_BACKGROUND -> { // sent after a user switch is complete
                        println("ACTION_USER_BACKGROUND")
                    }

                    Intent.ACTION_USER_FOREGROUND -> { // sent after a user switch is complete
                        println("ACTION_USER_FOREGROUND")
                    }

                    Intent.ACTION_USER_INITIALIZE -> { // broadcast? sent the first time a user is starting
                        println("ACTION_USER_INITIALIZE")
                    }

                    Intent.ACTION_USER_PRESENT -> { // broadcast sent when the user is present after device wakes up
                        println("ACTION_USER_PRESENT")
                    }

                    Intent.ACTION_USER_UNLOCKED -> { // broadcast sent when the credential-encrypted private storage has become unlocked for the target user.
                        println("ACTION_USER_UNLOCKED")
                    }

                    Intent.ACTION_VIEW -> { // display data to the user
                        println("ACTION_VIEW")
                    }

                    Intent.ACTION_VIEW_LOCUS -> { // display an activity state associated with an unique LocusId
                        println("ACTION_VIEW_LOCUS")
                    }

                    //TODO could be nice
                    Intent.ACTION_VIEW_PERMISSION_USAGE -> { // launch UI to show information about the usage of a given permission group
                        println("ACTION_VIEW_PERMISSION_USAGE")
                    }

                    Intent.ACTION_VIEW_PERMISSION_USAGE_FOR_PERIOD -> { // launch UI to show info about the usage of a given permission group in a given period
                        println("ACTION_VIEW_PERMISSION_USAGE_FOR_PERIOD")
                    }

                    Intent.ACTION_VOICE_COMMAND -> { // start voice command
                        println("ACTION_VOICE_COMMAND")
                    }

                    Intent.ACTION_WALLPAPER_CHANGED -> { // deprecated broadcast the current system wallpaper has changed
                        println("ACTION_WALLPAPER_CHANGED")
                    }

                    Intent.ACTION_WEB_SEARCH -> { // perform a web search
                        println("ACTION_WEB_SEARCH")
                    }

                    /*Intent.CAPTURE_CONTENT_FOR_NOTE_BLOCKED_BY_ADMIN -> { // a response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE
                        println("CAPTURE_CONTENT_FOR_NOTE_BLOCKED_BY_ADMIN")
                    }*/

                    /*Intent.CAPTURE_CONTENT_FOR_NOTE_FAILED -> { // A response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE to indicate that something went wrong
                        println("CAPTURE_CONTENT_FOR_NOTE_FAILED")
                    }*/

                    /*Intent.CAPTURE_CONTENT_FOR_NOTE_SUCCESS -> { // A response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE to indicate that the request was a success
                        println("CAPTURE_CONTENT_FOR_NOTE_SUCCESS")
                    }*/

                    /*Intent.CAPTURE_CONTENT_FOR_NOTE_USER_CANCELED -> { // A response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE to indicate that user canceled the content capture flow
                        println("CAPTURE_CONTENT_FOR_NOTE_USER_CANCELED")
                    }*/

                    /*Intent.CAPTURE_CONTENT_FOR_NOTE_WINDOW_MODE_UNSUPPORTED -> { // A response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE to indicate that the intent action ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE was started by an activity that is running in a non-supported window mode
                        println("CAPTURE_CONTENT_FOR_NOTE_WINDOW_MODE_UNSUPPORTED")
                    }*/

                    Intent.CATEGORY_ACCESSIBILITY_SHORTCUT_TARGET -> { // the accessibility shortcut is global gesture for users with disabilities to trigger an important for them accessibility
                        println("CATEGORY_ACCESSIBILITY_SHORTCUT_TARGET")
                    }

                    Intent.CATEGORY_ALTERNATIVE -> { // Set if the activity should be considered as an alternative action to the data the user is currently viewing
                        println("CATEGORY_ALTERNATIVE")
                    }

                    //TODO launches browser. Do I need it? maybe for a search
                    Intent.CATEGORY_APP_BROWSER -> { // Used with ACTION_MAIN to launch the browser application
                        println("CATEGORY_APP_BROWSER")
                    }

                    Intent.CATEGORY_APP_CALCULATOR -> { // Used with ACTION_MAIN to launch the calculator application
                        println("CATEGORY_APP_CALCULATOR")
                    }

                    Intent.CATEGORY_APP_CALENDAR -> { // Used with ACTION_MAIN to launch the calendar application
                        println("CATEGORY_APP_CALENDAR")
                    }

                    Intent.CATEGORY_APP_CONTACTS -> { // Used with ACTION_MAIN to launch the contacts application
                        println("CATEGORY_APP_CONTACTS")
                    }

                    Intent.CATEGORY_APP_EMAIL -> { // Used with ACTION_MAIN to launch the email application
                        println("CATEGORY_APP_EMAIL")
                    }

                    Intent.CATEGORY_APP_FILES -> { // Used with ACTION_MAIN to launch the files application
                        println("CATEGORY_APP_FILES")
                    }

                    Intent.CATEGORY_APP_FITNESS -> { // Used with ACTION_MAIN to launch the fitness application
                        println("CATEGORY_APP_FITNESS")
                    }

                    Intent.CATEGORY_APP_GALLERY -> { // Used with ACTION_MAIN to launch the gallery application
                        println("CATEGORY_APP_GALLERY")
                    }

                    Intent.CATEGORY_APP_MAPS -> { // Used with ACTION_MAIN to launch the maps application
                        println("CATEGORY_APP_MAPS")
                    }

                    Intent.CATEGORY_APP_MARKET -> { // This activity allows the user to browse and download new applications
                        println("CATEGORY_APP_MARKET")
                    }

                    Intent.CATEGORY_APP_MESSAGING -> { // Used with ACTION_MAIN to launch the messaging application
                        println("CATEGORY_APP_MESSAGING")
                    }

                    Intent.CATEGORY_APP_MUSIC -> { // Used with ACTION_MAIN to launch the music application
                        println("CATEGORY_APP_MUSIC")
                    }

                    Intent.CATEGORY_APP_WEATHER -> { // Used with ACTION_MAIN to launch the weather application
                        println("CATEGORY_APP_WEATHER")
                    }

                    Intent.CATEGORY_BROWSABLE -> { // Activities that can be safely invoked from a browser must support this category
                        println("CATEGORY_BROWSABLE")
                    }

                    //TODO really useful for drivvler
                    Intent.CATEGORY_CAR_DOCK -> { // An activity to run when device is inserted into a car dock
                        println("CATEGORY_CAR_DOCK")
                    }

                    //TODO really useful for drivvler
                    Intent.CATEGORY_CAR_MODE -> { // Used to indicate that the activity can be used in a car environment
                        println("CATEGORY_CAR_MODE")
                    }

                    Intent.CATEGORY_DEFAULT -> { // Set if the activity should be an option for the default action to perform on a piece of data
                        println("CATEGORY_DEFAULT")
                    }

                    Intent.CATEGORY_DEVELOPMENT_PREFERENCE -> { // This activity is a development preference panel
                        println("CATEGORY_DEVELOPMENT_PREFERENCE")
                    }

                    //TODO very useful i think
                    Intent.CATEGORY_HOME -> { // This is the home activity, that is the first activity that is displayed when the device boots
                        println("CATEGORY_HOME")
                    }

                    //TODO :eyes:
                    Intent.CATEGORY_INFO -> { // Provides information about the package it is in; typically used if a package does not contain a CATEGORY_LAUNCHER to provide a front-door to the user without having to be shown in the all apps list.
                        println("CATEGORY_INFO")
                    }

                    //TODO this might be even more interesting
                    Intent.CATEGORY_LAUNCHER -> { // Should be displayed in the top-level launcher
                        println("CATEGORY_LAUNCHER")
                    }

                    //TODO :eyes:
                    Intent.CATEGORY_LEANBACK_LAUNCHER -> { // Indicates an activity optimized for Leanback mode, and that should be displayed in the Leanback launcher.
                        println("CATEGORY_LEANBACK_LAUNCHER")
                    }

                    //TODO I want to have a preference/settings place
                    Intent.CATEGORY_PREFERENCE -> { // This activity is a preference panel
                        println("CATEGORY_PREFERENCE")
                    }

                    //TODO what does this do?
                    Intent.CATEGORY_SAMPLE_CODE -> { // To be used as a sample code example (not part of the normal user experience).
                        println("CATEGORY_SAMPLE_CODE")
                    }

                    //TODO interesting
                    Intent.CATEGORY_SECONDARY_HOME -> { // The home activity shown on secondary displays that support showing home activities
                        println("CATEGORY_SECONDARY_HOME")
                    }

                    // Skipped out on the EXTRA_

                    //TODO very interested with clipboard stuff
                    /*Intent.FILL_IN_CLIP_DATA -> { // Use with fillIn to allow the current ClipData to be overwritten, even if it is already set.
                        println("FILL_IN_CLIP_DATA")
                    }*/

                    //TODO useful for drivvler
                    Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT.toString() -> { // This flag is not normally set by application code, but set for you by the system as described in the launchMode documentation for the singleTask mode
                        println("FLAG_ACTIVITY_BROUGHT_TO_FRONT")
                    }

                    //TODO is this set automatically with manifest changes?
                    Intent.FLAG_ACTIVITY_CLEAR_TOP.toString() -> { // too long
                        println("FLAG_ACTIVITY_CLEAR_TOP")
                    }
                }
            }
        }

        registerReceiver(receiver, filters, RECEIVER_NOT_EXPORTED)

        val textColor = Color.White
        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
        val packageIntent = Intent(Intent.ACTION_MAIN, null)
        packageIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        var packages: List<ResolveInfo> = packageManager.queryIntentActivities(packageIntent, PackageManager.GET_META_DATA)
        createAppList()
        var alphabet = createAlphabetList(apps!!)
        createDuolingoWidget()

        batteryTextColor = mutableStateOf(Color.White)
        batteryManager = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batLevel: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        isCharging = mutableStateOf(false)
        isCharging.value = batteryManager.isCharging

        setContent {
            val isDarkMode = isSystemInDarkTheme()
            val context = LocalContext.current as ComponentActivity
            DisposableEffect(isDarkMode) {
                context.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
                    navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
                )

                onDispose { }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 19.dp, top = 30.dp),
                    text = date,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight(600)
                )

                Text(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 19.dp, top = 30.dp),
                    text = batLevel.toString(),
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight(600)
                )

                if (isCharging.value) {
                    Icon(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(50.dp)
                            .padding(end = 40.dp, top = 30.dp),
                        painter = painterResource(id = R.drawable.lightning),
                        contentDescription = null,
                        tint = textColor
                    )
                }
            }

            val screenWidth = 1080f
            val screenHeight = 2340f

            val decayAnimationSpec = rememberSplineBasedDecay<Float>()
            val dragState = remember {
                AnchoredDraggableState(
                    initialValue = Start,
                    anchors = DraggableAnchors {
                        Start at 0f
                        End at -screenWidth
                    },
                    positionalThreshold = { d -> d * 0.9f},
                    velocityThreshold = { Float.POSITIVE_INFINITY },
                    snapAnimationSpec = tween(),
                    decayAnimationSpec = decayAnimationSpec
                )
            }
            val dragState2 = remember {
                AnchoredDraggableState(
                    initialValue = Start,
                    anchors = DraggableAnchors {
                        Start at 0f
                        End at -screenHeight
                    },
                    positionalThreshold = { d -> d * 0.9f},
                    velocityThreshold = { Float.POSITIVE_INFINITY },
                    snapAnimationSpec = tween(),
                    decayAnimationSpec = decayAnimationSpec
                )
            }

            val appDrawerClosed = dragState2.requireOffset().roundToInt() == 0

            Box(
                modifier = Modifier
                    .anchoredDraggable(
                        state = dragState,
                        enabled = appDrawerClosed,
                        orientation = Orientation.Horizontal
                    )
                    .anchoredDraggable(
                        state = dragState2,
                        enabled = true,
                        orientation = Orientation.Vertical
                    )
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            dragState
                                .requireOffset()
                                .roundToInt(),
                            dragState2
                                .requireOffset()
                                .roundToInt()
                        )
                    }
            )

            if (dragState2.requireOffset().roundToInt() == -screenHeight.roundToInt()) {
                val i = Intent(Intent.ACTION_MAIN, null)
                i.addCategory(Intent.CATEGORY_LAUNCHER)
                val pk: List<ResolveInfo> = packageManager.queryIntentActivities(i, PackageManager.GET_META_DATA)
                if (packages.size == pk.size && packages.toSet() == pk.toSet()) {
                    createAppList()
                    alphabet = createAlphabetList(apps!!)
                    packages = pk
                }
            }

            lazyScroll = rememberLazyListState()

            AppDrawer(
                modifier = Modifier
                    .offset { IntOffset(0, dragState2.requireOffset().roundToInt() + screenHeight.roundToInt()) },
                lazyScroll = lazyScroll,
                hostView = hostView,
                alphabet = alphabet,
                apps = apps!!,
                customScope = customScope,
                launchApp = ::launchApp,
                hideApp = ::hideApp,
                uninstallApp = ::uninstallApp,
                textColor = textColor,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            dragState
                                .requireOffset()
                                .roundToInt() + screenWidth.roundToInt(),
                            dragState2
                                .requireOffset()
                                .roundToInt()
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(100.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color.Cyan
                                )
                            )
                        )
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.Center),
                    text = "Reminder page!"
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        widgetHost.stopListening()
        widgetHost.deleteAppWidgetId(widgetId)
    }

    private fun launchApp(packageName: String?) {
        if (packageName == null)
            return

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(launchIntent)
    }

    private fun hideApp(packageName: String?) {
        val app = apps?.find { it.packageName?.lowercase() == packageName?.lowercase() }
        apps?.find { it.packageName?.lowercase() == packageName?.lowercase() }?.hidden = !app?.hidden!!
    }

    private fun uninstallApp(packageName: String?) {
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
        startActivity(intent)
    }

    @SuppressLint("WrongConstant")
    private fun setExpandNotificationDrawer(expand: Boolean) {
        try {
            val statusBarService = applicationContext.getSystemService("statusbar")
            val methodName = if (expand) "expandNotificationsPanel" else "collapsePanels"
            val statusBarManager: Class<*> = Class.forName("android.app.StatusBarManager")
            val method: Method = statusBarManager.getMethod(methodName)
            method.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createAppList() {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)

        val appList = mutableListOf<ApplicationInformation>()
        packages.forEach { app ->
            val appInfo = ApplicationInformation()
            appInfo.label = app.loadLabel(packageManager).toString()
            appInfo.packageName = app.activityInfo.packageName
            appInfo.icon = app.loadIcon(packageManager)
            val previousApp = apps?.find { it.packageName?.lowercase() == appInfo.packageName!!.lowercase() }
            if (previousApp != null)
                appInfo.hidden = previousApp.hidden
            else
                appInfo.hidden = false

            appList.add(appInfo)
        }
        appList.sortWith { a, b ->
            a.label?.uppercase()!!.compareTo(b.label?.uppercase()!!)
        }

        apps = appList
    }

    private fun createAlphabetList(apps: MutableList<ApplicationInformation>): MutableList<String> {
        val tempAlphabet = "1234567890qwertyuiopasdfghjklzxcvbnm".split("").dropLast(1).toMutableList()
        val alphabet = tempAlphabet.subList(1, tempAlphabet.size)
        alphabet.sortWith { a, b ->
            a.compareTo(b)
        }
        alphabet.add("å")
        alphabet.add("ä")
        alphabet.add("ö")

        val removeLetters = mutableListOf<String>()
        alphabet.forEach { letter ->
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
            alphabet.remove(letter)
        }

        return alphabet
    }

    private fun createDuolingoWidget() {
        widgetHost = AppWidgetHost(applicationContext, 0)
        widgetHost.startListening()
        widgetManager = AppWidgetManager.getInstance(applicationContext)
        duoWidget = widgetManager.installedProviders.find { it.activityInfo.name.contains("com.duolingo.streak.streakWidget.MediumStreakWidgetProvider") }!!
        widgetId = widgetHost.allocateAppWidgetId()

        options = Bundle()
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, maxWidth)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight)

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, duoWidget.provider)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)

        if (!widgetManager.bindAppWidgetIdIfAllowed(widgetId, duoWidget.provider)) {
            println("invalid")
            requestWidgetPermissionsLauncher.launch(intent)
        }

        hostView = widgetHost.createView(applicationContext, widgetId, duoWidget)
        hostView.setAppWidget(widgetId, duoWidget)
    }
}

@SuppressLint("UseOfNonLambdaOffsetOverload")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    modifier: Modifier,
    lazyScroll: LazyListState,
    hostView: AppWidgetHostView?,
    customScope: CoroutineScope,
    textColor: Color,
    apps: MutableList<MainActivity.ApplicationInformation>,
    launchApp: (String?) -> Unit,
    hideApp: (String?) -> Unit,
    uninstallApp: (String?) -> Unit,
    alphabet: MutableList<String>,
) {
    val showAllApps = remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (showAllApps.value) 30.dp else 20.dp)
                    .offset(
                        x = if (showAllApps.value) (-35).dp else (-40).dp,
                        y = if (showAllApps.value) (-1).dp else (-5).dp
                    )
                    .clickable {
                        showAllApps.value = !showAllApps.value
                    },
                painter = painterResource(id = if (showAllApps.value) R.drawable.eye_cross else R.drawable.eye),
                contentDescription = null,
                tint = textColor
            )

            AndroidView(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(0.dp, (-50).dp)
                    .width(350.dp)
                    .height(200.dp),
                factory = { hostView!! }
            )

            Icon(
                modifier = Modifier
                    .width(36.dp)
                    .height(30.dp)
                    .align(Alignment.BottomEnd)
                    .clickable {
                        customScope.launch {
                            lazyScroll.animateScrollToItem(0)
                        }
                    },
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = textColor
            )
        }

        Row(
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomEnd)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.End
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(end = 20.dp),
                state = lazyScroll,
                horizontalAlignment = Alignment.End
            ) {
                apps.forEach { app ->
                    item {
                        if (showAllApps.value || !app.hidden!!) {
                            val showOptions = remember { mutableStateOf(false) }
                            val firstAppWithLetter = apps.find { it.label?.uppercase()?.startsWith(app.label?.uppercase()!![0])!! }!!

                            if (app.label?.uppercase() == firstAppWithLetter.label?.uppercase()) {
                                Row(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(1.dp)
                                            .padding(end = 10.dp)
                                            .offset(0.dp, 2.dp)
                                            .background(Color.White)
                                    )
                                    Text(
                                        modifier = Modifier
                                            .padding(end = 10.dp),
                                        text = app.label?.first()?.uppercaseChar().toString(),
                                        color = textColor,
                                        fontSize = 20.sp
                                    )
                                }
                            }

                            if (showOptions.value) {
                                AlertDialog(
                                    //icon = {  },
                                    title = { Text(text = "ACTION") },
                                    text = { Text(text = "What to do with ${app.label}?") },
                                    onDismissRequest = {
                                        showOptions.value = false
                                    },
                                    confirmButton = {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 10.dp)
                                                .width(100.dp)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.DarkGray)
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .clickable {
                                                        uninstallApp(app.packageName)
                                                        showOptions.value = false
                                                    },
                                                text = "uninstall",
                                                color = textColor
                                            )
                                        }
                                    },
                                    dismissButton = {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 10.dp)
                                                .width(100.dp)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.DarkGray)
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .clickable {
                                                        showOptions.value = false
                                                        hideApp(app.packageName)
                                                    },
                                                text = if (app.hidden != null && app.hidden!!) "show" else "hide",
                                                color = textColor
                                            )
                                        }
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .padding(bottom = 20.dp)
                                    .combinedClickable(
                                        onClick = {
                                            launchApp(app.packageName)
                                        },
                                        onLongClick = {
                                            showOptions.value = true
                                        },
                                    )
                            ) {
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(end = 10.dp)
                                        .width(300.dp),
                                    text = "${app.label}",
                                    color = textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight(weight = 700),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    textAlign = TextAlign.End
                                )

                                Image(
                                    modifier = Modifier
                                        .size(50.dp),
                                    painter = rememberDrawablePainter(drawable = app.icon),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }

            var offsetY by remember { mutableFloatStateOf(0f) }
            var selectedLetter by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                alphabet.forEach { letter ->
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        try {
                                            selectedLetter = letter
                                            offsetY = -150f
                                            awaitRelease()
                                        } finally {
                                            customScope.launch {
                                                var i = 0
                                                var found = false

                                                apps.forEachIndexed { index, app ->
                                                    if (!found && app.label != null && app.label!![0].uppercaseChar() == letter.toCharArray()[0].uppercaseChar()) {
                                                        i = index
                                                        found = true
                                                    }
                                                }

                                                lazyScroll.animateScrollToItem(i)
                                            }

                                            offsetY = 0f
                                            selectedLetter = ""
                                        }
                                    },
                                )
                            }
                            .offset {
                                if (selectedLetter == letter) IntOffset(
                                    0,
                                    offsetY.roundToInt()
                                ) else IntOffset(0, 0)
                            }
                            .drawBehind {
                                if (selectedLetter == letter)
                                    drawCircle(
                                        radius = 80f,
                                        color = Color.Black
                                    )
                            },
                        text = letter.uppercase(),
                        color = textColor,
                        fontSize = if (selectedLetter == letter) 40.sp else 16.sp,
                        fontWeight = FontWeight(600)
                    )
                }
            }
        }
    }
}