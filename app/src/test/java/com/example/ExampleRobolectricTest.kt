package com.example

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Krunker", appName)
    val gameUrl = context.getString(R.string.game_url)
    assertEquals("https://krunker.io", gameUrl)
  }

  @Test
  fun `main activity launches and applies notch bypass and keep screen on`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)

    // Verify KEEP_SCREEN_ON flag is set for FPS gaming
    val flags = activity.window.attributes.flags
    val isKeepScreenOn = (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    assertTrue("FLAG_KEEP_SCREEN_ON must be enabled", isKeepScreenOn)

    // Verify notch / display cutout mode is set to bypass notch (ALWAYS on R+, SHORT_EDGES on P+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val cutoutMode = activity.window.attributes.layoutInDisplayCutoutMode
      val isBypassed = cutoutMode == WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES ||
              (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && cutoutMode == WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
      assertTrue("layoutInDisplayCutoutMode must bypass notch padding", isBypassed)
    }
  }

  @Test
  fun `krunker application environment initializes without exception`() {
    val app = ApplicationProvider.getApplicationContext<KrunkerApp>()
    assertNotNull(app)
  }
}
