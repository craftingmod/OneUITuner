package tk.zwander.oneuituner.fragments

import android.os.Bundle
import tk.zwander.oneuituner.R

class Camera : Base() {
    override val title = R.string.camera_title

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.camera, rootKey)
    }
}