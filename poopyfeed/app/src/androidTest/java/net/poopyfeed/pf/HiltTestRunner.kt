package net.poopyfeed.pf

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class HiltTestRunner : AndroidJUnitRunner() {
  override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
    return super.newApplication(
        cl, PoopyFeedHiltTestApplication_Application::class.java.name, context)
  }
}
