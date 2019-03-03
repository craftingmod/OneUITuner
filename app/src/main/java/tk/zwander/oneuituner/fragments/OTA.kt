package tk.zwander.oneuituner.fragments

import android.os.Bundle
import tk.zwander.oneuituner.R

class OTA : Base() {
    override val title = R.string.ota_title

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.ota, rootKey)
    }
}