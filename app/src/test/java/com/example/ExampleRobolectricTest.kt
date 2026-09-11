package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.backend.FirestoreSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Egle11", appName)
  }

  @Test
  fun `verify restricted states compliance`() {
    assertTrue(FirestoreSchema.isStateRestricted("Assam"))
    assertTrue(FirestoreSchema.isStateRestricted("Telangana"))
    assertTrue(FirestoreSchema.isStateRestricted("Andhra Pradesh"))
    assertTrue(FirestoreSchema.isStateRestricted("Tamil Nadu"))
    assertFalse(FirestoreSchema.isStateRestricted("Maharashtra"))
    assertFalse(FirestoreSchema.isStateRestricted("Karnataka"))
    assertFalse(FirestoreSchema.isStateRestricted("Delhi"))
  }
}
