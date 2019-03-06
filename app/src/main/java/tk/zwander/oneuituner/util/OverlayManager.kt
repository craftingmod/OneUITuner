package tk.zwander.oneuituner.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.android.apksig.ApkSigner
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tk.zwander.oneuituner.data.OverlayInfo
import tk.zwander.oneuituner.data.ResourceData
import tk.zwander.oneuituner.data.ResourceFileData
import tk.zwander.oneuituner.data.ResourceImageData
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

fun Context.install(which: String, listener: ((apk: File) -> Unit)?) {
    GlobalScope.launch {
        val data = when (which) {
            Keys.clock -> {
                OverlayInfo(
                    Keys.systemuiPkg,
                    Keys.clockPkg,
                    mutableListOf<ResourceFileData>().apply {
                        val clockFormat = prefs.clockFormat
                        val qsDateFormat = prefs.qsDateFormat

                        if (prefs.customClock && clockFormat.isValidClockFormat) {
                            add(
                                ResourceFileData(
                                    "qs_status_bar_clock.xml",
                                    "layout",
                                    getResourceXmlFromAsset(
                                        "clock/layout",
                                        "qs_status_bar_clock_custom.xml"
                                    ).replace("h:mm a", clockFormat)
                                )
                            )
                        }

                        if (prefs.customQsDateFormat && qsDateFormat.isValidClockFormat) {
                            add(
                                ResourceFileData(
                                    "strings.xml",
                                    "values",
                                    makeResourceXml(
                                        ResourceData(
                                            "string",
                                            "system_ui_quick_panel_date_pattern",
                                            qsDateFormat
                                        )
                                    )
                                )
                            )
                        }
                    }
                )
            }
            Keys.qs -> {
                OverlayInfo(
                    Keys.systemuiPkg,
                    Keys.qsPkg,
                    arrayListOf(
                        ResourceFileData(
                            "integers.xml",
                            "values",
                            makeResourceXml(
                                ResourceData(
                                    "integer",
                                    "quick_qs_tile_num",
                                    prefs.headerCountPortrait.toString()
                                ),
                                ResourceData(
                                    "integer",
                                    "quick_qs_tile_min_num",
                                    "2"
                                ),
                                ResourceData(
                                    "integer",
                                    "qspanel_screen_grid_columns_5",
                                    prefs.qsColCountPortrait.toString()
                                ),
                                ResourceData(
                                    "integer",
                                    "qspanel_screen_grid_rows",
                                    prefs.qsRowCountPortrait.toString()
                                )
                            )
                        ),
                        ResourceFileData(
                            "integers.xml",
                            "values-land",
                            makeResourceXml(
                                ResourceData(
                                    "integer",
                                    "quick_qs_tile_num",
                                    prefs.headerCountLandscape.toString()
                                ),
                                ResourceData(
                                    "integer",
                                    "qspanel_screen_grid_columns_5",
                                    prefs.qsColCountLandscape.toString()
                                ),
                                ResourceData(
                                    "integer",
                                    "qspanel_screen_grid_rows",
                                    prefs.qsRowCountLandscape.toString()
                                )
                            )
                        ),
                        ResourceFileData(
                            "dimens.xml",
                            "values",
                            makeResourceXml(
                                ResourceData(
                                    "dimen",
                                    "qs_tile_height_5x3_ratio",
                                    if (prefs.qsRowCountPortrait > 4) "9.0" else "7.1"
                                )
                            )
                        )
                    ).apply {
                        if (prefs.hideQsTileBackground) {
                            add(
                                ResourceFileData(
                                    "colors.xml",
                                    "values",
                                    makeResourceXml(
                                        ResourceData(
                                            "color",
                                            "qs_tile_round_background_on",
                                            "@android:color/transparent"
                                        )
                                    )
                                )
                            )
                        }
                    }
                )
            }
            Keys.misc -> {
                OverlayInfo(
                    Keys.androidPkg,
                    Keys.miscPkg,
                    arrayListOf(
                        ResourceFileData(
                            "config.xml",
                            "values",
                            makeResourceXml(
                                mutableListOf(
                                    ResourceData(
                                        "dimen",
                                        "navigation_bar_height",
                                        "${prefs.navHeight}dp"
                                    ),
                                    ResourceData(
                                        "dimen",
                                        "navigation_bar_width",
                                        "${prefs.navHeight}dp"
                                    ),
                                    ResourceData(
                                        "dimen",
                                        "status_bar_height_portrait",
                                        "${prefs.statusBarHeight}dp"
                                    )
                                ).apply {
                                    if (prefs.oldRecents) {
                                        add(
                                            ResourceData(
                                                "string",
                                                "config_recentsComponentName",
                                                "com.android.systemui/.recents.RecentsActivity",
                                                "translatable=\"false\""
                                            )
                                        )
                                    }
                                }
                            )
                        )
                    )
                )
            }
            Keys.statusBar -> {
                OverlayInfo(
                    Keys.systemuiPkg,
                    Keys.statusBarPkg,
                    ArrayList<ResourceFileData>()
                        .apply {
                            if (prefs.leftSystemIcons) {
                                add(
                                    ResourceFileData(
                                        "status_bar.xml",
                                        "layout",
                                        getResourceXmlFromAsset("statusbar/layout", "status_bar.xml")
                                    )
                                )
                            }
                        if (prefs.hideStatusBarCarrier) {
                            add(
                                ResourceFileData(
                                    "bools.xml",
                                    "values",
                                    makeResourceXml(
                                        arrayListOf(
                                            ResourceData(
                                                "bool",
                                                "config_showOperatorNameInStatusBar",
                                                "${!prefs.hideStatusBarCarrier}"
                                            )
                                        )
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "dimens.xml",
                                    "values",
                                    makeResourceXml(
                                        arrayListOf(
                                            ResourceData(
                                                "dimen",
                                                "status_bar_carrier_text_size",
                                                "0.0dip"
                                            )
                                        )
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "stat_notify_operator_logo_skt.xml",
                                    "drawable",
                                    getResourceXmlFromAsset("statusbar/drawable", "stat_notify_operator_logo_skt.xml")
                                )
                            )
                            add(
                                ResourceFileData(
                                    "stat_notify_operator_logo_kt.xml",
                                    "drawable",
                                    getResourceXmlFromAsset("statusbar/drawable", "stat_notify_operator_logo_kt.xml")
                                )
                            )
                            add(
                                ResourceFileData(
                                    "stat_notify_operator_logo_lgt.xml",
                                    "drawable",
                                    getResourceXmlFromAsset("statusbar/drawable", "stat_notify_operator_logo_lgt.xml")
                                )
                            )
                            add(
                                ResourceFileData(
                                    "stat_notify_operator_logo_org.xml",
                                    "drawable",
                                    getResourceXmlFromAsset("statusbar/drawable", "stat_notify_operator_logo_org.xml")
                                )
                            )
                        }
                     }
                )
            }
            Keys.lockScreen -> {
                OverlayInfo(
                    Keys.systemuiPkg,
                    Keys.lockScreenPkg,
                    ArrayList<ResourceFileData>()
                        .apply {
                            add(
                                ResourceFileData(
                                    "bools.xml",
                                    "values",
                                    makeResourceXml(
                                        ResourceData(
                                            "bool",
                                            "config_enableLockScreenRotation",
                                            "${prefs.lockScreenRotation}"
                                        )
                                    )
                                )
                            )
                        }
                )
            }
            Keys.ota -> {
                OverlayInfo(
                    Keys.otaPkg,
                    Keys.otaUpdatePkg,
                    mutableListOf<ResourceFileData>().apply {
                        if (prefs.disableOTAUpdate) {
                            add(
                                ResourceFileData(
                                    "stat_fota.xml",
                                    "drawable",
                                    getResourceXmlFromAsset(
                                        "ota/drawable",
                                        "stat_fota.xml"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "stat_fota_completion.xml",
                                    "drawable",
                                    getResourceXmlFromAsset(
                                        "ota/drawable",
                                        "stat_fota_completion.xml"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "stat_fota_fail.xml",
                                    "drawable",
                                    getResourceXmlFromAsset(
                                        "ota/drawable",
                                        "stat_fota_fail.xml"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "stat_fota_message_phone.xml",
                                    "drawable",
                                    getResourceXmlFromAsset(
                                        "ota/drawable",
                                        "stat_fota_message_phone.xml"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "stat_fota_postpone.xml",
                                    "drawable",
                                    getResourceXmlFromAsset(
                                        "ota/drawable",
                                        "stat_fota_postpone.xml"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "download_progress_content.xml",
                                    "layout",
                                    getResourceXmlFromAsset(
                                        "ota/layout",
                                        "download_progress_content.xml"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "software_update_information_content.xml",
                                    "layout",
                                    getResourceXmlFromAsset(
                                        "ota/layout",
                                        "software_update_information_content.xml"
                                    )
                                )
                            )
                        }
                    }
                )
            }
            Keys.camera -> {
                OverlayInfo(
                    Keys.cameraPkg,
                    Keys.cameraMutePkg,
                    mutableListOf<ResourceFileData>().apply {
                        // If I know Kotlin well, I'll use for loop.. Hu
                        if (prefs.muteCameraSound) {
                            add(
                                ResourceFileData(
                                    "cam_start.ogg",
                                    "raw",
                                    getResourceXmlFromAsset(
                                        "camera/raw",
                                        "cam_start.ogg"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "cam_stop.ogg",
                                    "raw",
                                    getResourceXmlFromAsset(
                                        "camera/raw",
                                        "cam_stop.ogg"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "panorama_start.ogg",
                                    "raw",
                                    getResourceXmlFromAsset(
                                        "camera/raw",
                                        "panorama_start.ogg"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "panorama_stop.ogg",
                                    "raw",
                                    getResourceXmlFromAsset(
                                        "camera/raw",
                                        "panorama_stop.ogg"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "panorama_warning.ogg",
                                    "raw",
                                    getResourceXmlFromAsset(
                                        "camera/raw",
                                        "panorama_warning.ogg"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "shutter_100ms.ogg",
                                    "raw",
                                    getResourceXmlFromAsset(
                                        "camera/raw",
                                        "shutter_100ms.ogg"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "shutter_close.ogg",
                                    "raw",
                                    getResourceXmlFromAsset(
                                        "camera/raw",
                                        "shutter_close.ogg"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "shutter_open.ogg",
                                    "raw",
                                    getResourceXmlFromAsset(
                                        "camera/raw",
                                        "shutter_open.ogg"
                                    )
                                )
                            )
                            add(
                                ResourceFileData(
                                    "shutter.ogg",
                                    "raw",
                                    getResourceXmlFromAsset(
                                        "camera/raw",
                                        "shutter.ogg"
                                    )
                                )
                            )
                        }
                    }
                )
            }
            else -> return@launch
        }

        doCompileAlignAndSign(
            data,
            listener
        )
    }
}

fun Context.uninstall(which: String) {
    val pkg = when (which) {
        Keys.clock -> Keys.clockPkg
        Keys.qs -> Keys.qsPkg
        Keys.misc -> Keys.miscPkg
        Keys.statusBar -> Keys.statusBarPkg
        Keys.ota -> Keys.otaUpdatePkg
        Keys.camera -> Keys.cameraMutePkg
        else -> return
    }

    if (Shell.SU.available()) {
        app.ipcReceiver.postIPCAction { it.uninstallPkg(pkg) }
    } else {
        workaroundInstaller.uninstallPackage(pkg)
    }
}

fun Context.doCompileAlignAndSign(
    overlayInfo: OverlayInfo,
    listener: ((apk: File) -> Unit)? = null
) {
    val base = makeBaseDir(overlayInfo.overlayPkg)
    val manifest = getManifest(base, overlayInfo.targetPkg, overlayInfo.overlayPkg)
    val unsignedUnaligned = makeOverlayFile(base, overlayInfo.overlayPkg, OverlayType.UNSIGNED_UNALIGNED)
    val unsigned = makeOverlayFile(base, overlayInfo.overlayPkg, OverlayType.UNSIGNED)
    val signed = makeOverlayFile(base, overlayInfo.overlayPkg, OverlayType.SIGNED)
    val resDir = makeResDir(base)

    overlayInfo.data.forEach {
        val dir = File(resDir, it.fileDirectory)

        dir.mkdirs()
        dir.mkdir()

        val resFile = File(dir, it.filename)
        if (resFile.exists()) resFile.delete()

        if (it is ResourceImageData) {
            it.image?.let {
                FileOutputStream(resFile).use { stream ->
                    it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
        } else {
            OutputStreamWriter(resFile.outputStream()).use { writer ->
                writer.write(it.contents)
                writer.write("\n")
            }
        }
    }

    compileOverlay(manifest, unsignedUnaligned, resDir, overlayInfo.targetPkg)
    alignOverlay(unsignedUnaligned, unsigned)
    signOverlay(unsigned, signed)

    Shell.run("sh", arrayOf("cp ${signed.absolutePath} ${signed.absolutePath}"), null, true)

    listener?.invoke(signed)
}

fun Context.compileOverlay(manifest: File, overlayFile: File, resFile: File, targetPackage: String) {
    if (overlayFile.exists()) {
        overlayFile.delete()
    }

    val aaptCmd = StringBuilder()
        .append(aapt)
        .append(" p")
        .append(" -M ")
        .append(manifest)
        .append(" -I ")
        .append("/system/framework/framework-res.apk")
        .apply {
            if (targetPackage != "android") {
                append(" -I ")
                append(packageManager.getApplicationInfo(targetPackage, 0).sourceDir)
            }
        }
        .append(" -S ")
        .append(resFile)
        .append(" -F ")
        .append(overlayFile)
        .toString()

    Shell.run("sh", arrayOf(aaptCmd), null, true)
        .apply { Log.e("OneUITuner", toString()) }
    Shell.SH.run("chmod 777 ${overlayFile.absolutePath}")
}

fun Context.alignOverlay(overlayFile: File, alignedOverlayFile: File) {
    if (alignedOverlayFile.exists()) alignedOverlayFile.delete()

    val zipalignCmd = StringBuilder()
        .append(zipalign)
        .append(" 4 ")
        .append(overlayFile.absolutePath)
        .append(" ")
        .append(alignedOverlayFile.absolutePath)
        .toString()

    Shell.run("sh", arrayOf(zipalignCmd), null, true)

    Shell.SH.run("chmod 777 ${alignedOverlayFile.absolutePath}")
}

fun Context.signOverlay(overlayFile: File, signed: File) {
    Shell.SH.run("chmod 777 ${overlayFile.absolutePath}")

    val key = File(cacheDir, "/signing-key-new")
    val pass = "overlay".toCharArray()

    if (key.exists()) key.delete()

    val store = KeyStore.getInstance(KeyStore.getDefaultType())
    store.load(assets.open("signing-key-new"), pass)

    val privKey = store.getKey("key", pass) as PrivateKey
    val certs = ArrayList<X509Certificate>()

    certs.add(store.getCertificateChain("key")[0] as X509Certificate)

    val signConfig = ApkSigner.SignerConfig.Builder("overlay", privKey, certs).build()
    val signConfigs = ArrayList<ApkSigner.SignerConfig>()

    signConfigs.add(signConfig)

    val signer = ApkSigner.Builder(signConfigs)
    signer.setV1SigningEnabled(true)
        .setV2SigningEnabled(true)
        .setInputApk(overlayFile)
        .setOutputApk(signed)
        .setMinSdkVersion(Build.VERSION.SDK_INT)
        .build()
        .sign()

    Shell.SH.run("chmod 777 ${signed.absolutePath}")
}