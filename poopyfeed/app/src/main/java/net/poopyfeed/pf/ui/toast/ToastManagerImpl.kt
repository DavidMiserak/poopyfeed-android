package net.poopyfeed.pf.ui.toast

import android.content.Context
import android.widget.Toast
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToastManagerImpl @Inject constructor(private val context: Context) : ToastManager {

    override fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun showInfo(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
