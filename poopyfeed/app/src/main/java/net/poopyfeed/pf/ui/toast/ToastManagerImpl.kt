package net.poopyfeed.pf.ui.toast

import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToastManagerImpl @Inject constructor(@ApplicationContext private val context: Context) :
    ToastManager {

  override fun showSuccess(message: String) {
    showToast(message, Toast.LENGTH_SHORT)
  }

  override fun showError(message: String) {
    showToast(message, Toast.LENGTH_LONG)
  }

  override fun showInfo(message: String) {
    showToast(message, Toast.LENGTH_SHORT)
  }

  private fun showToast(message: String, duration: Int) {
    Toast.makeText(context, message, duration).show()
  }
}
