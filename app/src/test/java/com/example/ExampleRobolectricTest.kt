package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PreloadedCharacters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("গল্পAI", appName)
  }

  @Test
  fun preloadedCharactersContainBengaliPersonas() {
    val characters = PreloadedCharacters.characters
    assertTrue(characters.isNotEmpty())
    val ariya = characters.firstOrNull { it.id == "char_ariya" }
    assertEquals("আরিয়া", ariya?.name)
  }
}
