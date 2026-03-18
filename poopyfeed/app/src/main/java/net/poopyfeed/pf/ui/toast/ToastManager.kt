package net.poopyfeed.pf.ui.toast

interface ToastManager {
  fun showSuccess(message: String)

  fun showError(message: String)

  fun showInfo(message: String)
}
