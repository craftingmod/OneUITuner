package tk.zwander.oneuituner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import info.build.DummyActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import tk.zwander.oneuituner.util.*
import java.io.File

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, (File) -> Unit {
    private val currentFrag: NavDestination?
        get() = navController.currentDestination
    private val overlayReceiver = OverlayReceiver()

    private val backButton by lazy { createBackButton() }
    private val workaround by lazy { WorkaroundInstaller(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayReceiver.register()

        root.layoutTransition = LayoutTransition()
            .apply {
                enableTransitionType(LayoutTransition.CHANGING)
            }

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        updateFABs()

        navController.addOnDestinationChangedListener(this)

        apply.setOnClickListener {
            progress_apply.visibility = View.VISIBLE
            install(currentFrag?.label.toString(), this)
        }

        remove.setOnClickListener {
            uninstall(currentFrag?.label.toString())
        }

        val animDuration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

        title_switcher.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in).apply { duration =  animDuration}
        title_switcher.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out).apply { duration = animDuration }
        title_switcher.setFactory {
            AppCompatTextView(this).apply {
                setTextAppearance(android.R.style.TextAppearance_Material_Widget_ActionBar_Title)
                setTextColor(Color.WHITE)
            }
        }
        this.findViewById<Button>(R.id.build_info_btn).setOnClickListener {
            startActivity(Intent(this, DummyActivity::class.java))
        }

//        val ldClass = Class.forName("libcore.icu.LocaleData")
//        val get = ldClass.getMethod("get", Locale::class.java)
//        val d = get.invoke(null, resources.configuration.locale)
//
//        val hms = ldClass.getDeclaredField("timeFormat_hms")
//            .apply { isAccessible = true }
//            .get(d)
//
//        Log.e("OneUITuner", hms.toString())
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        when (intent?.action) {
            WorkaroundInstaller.ACTION_FINISHED -> {
                val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -100)
                val message: String? = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

                progress_apply.visibility = View.GONE

                Toast.makeText(this, message.toString(), Toast.LENGTH_SHORT).show()

                when (status) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val confirmIntent = intent.extras?.get(Intent.EXTRA_INTENT) as Intent?
                        startActivity(confirmIntent)
                    }
                }
            }
        }
    }

    override fun invoke(apk: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.apkprovider",
            apk
        )

//        val installIntent = Intent(Intent.ACTION_VIEW)
//        installIntent.setDataAndType(
//            FileProvider.getUriForFile(this,
//                "$packageName.apkprovider", apk),
//            "application/vnd.android.package-archive")
//        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//        startActivity(installIntent)

        workaround.installPackage(uri, apk.name)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        updateFABs()
    }

    override fun setTitle(title: CharSequence?) {
        title_switcher.setText(title)
        super.setTitle(null)
    }

    override fun setTitle(titleId: Int) {
        title = getText(titleId)
    }

    override fun onDestroy() {
        super.onDestroy()

        overlayReceiver.unregister()
        navController.removeOnDestinationChangedListener(this)
    }

    override fun onResume() {
        super.onResume()

        updateFABs()
    }

    fun updateFABs() {
        val id = currentFrag?.id
        val enabled = id != R.id.main

        setBackClickable(enabled)

        apply_wrapper.animatedVisibility = if (enabled) View.VISIBLE else View.GONE

        when (id) {
            R.id.main -> {
                remove_wrapper.animatedVisibility = View.GONE
            }

            R.id.clock -> {
                remove_wrapper.animatedVisibility = if (isInstalled(Keys.clockPkg)) View.VISIBLE else View.GONE
            }

            R.id.qs -> {
                remove_wrapper.animatedVisibility = if (isInstalled(Keys.qsPkg)) View.VISIBLE else View.GONE
            }

            R.id.misc -> {
                remove_wrapper.animatedVisibility = if (isInstalled(Keys.miscPkg)) View.VISIBLE else View.GONE
            }

            R.id.statusBar -> {
                remove_wrapper.animatedVisibility = if (isInstalled(Keys.statusBarPkg)) View.VISIBLE else View.GONE
            }
        }
    }

    private var RelativeLayout.animatedVisibility: Int
        get() = visibility
        set(value) {
            val hide = value != View.VISIBLE

            if (!hide) visibility = value

            animate()
                .scaleX(if (hide) 0f else 1f)
                .scaleY(if (hide) 0f else 1f)
                .setDuration(resources.getInteger(android.R.integer.config_mediumAnimTime).toLong())
                .setInterpolator(if (hide) AnticipateInterpolator() else OvershootInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        if (hide) visibility = value
                    }
                })
        }

    private fun setBackClickable(clickable: Boolean) {
        backButton.isClickable = clickable

        backButton.animate()
            .scaleX(if (clickable) 1f else 0f)
            .scaleY(if (clickable) 1f else 0f)
            .setInterpolator(if (clickable) OvershootInterpolator() else AnticipateInterpolator())
            .setDuration(resources.getInteger(android.R.integer.config_mediumAnimTime).toLong())
            .start()
    }

    private fun createBackButton(): ImageButton {
        val mNavButtonView = toolbar::class.java.getDeclaredField("mNavButtonView")
        mNavButtonView.isAccessible = true

        return mNavButtonView.get(toolbar) as ImageButton
    }

    inner class OverlayReceiver : BroadcastReceiver() {
        fun register() {
            val remFilter = IntentFilter()
            remFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
            remFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
            remFilter.addDataScheme("package")

            registerReceiver(this, remFilter)
        }

        fun unregister() {
            unregisterReceiver(this)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REMOVED -> updateFABs()
            }
        }
    }
}
