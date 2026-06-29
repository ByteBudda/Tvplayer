package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.IptvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("IPTV Player", appName)
  }

  @Test
  fun `test xmltv parsing with url tags`() {
    val xmlContent = """
      <?xml version="1.0" encoding="utf-8"?>
      <tv>
        <channel id="1">
          <display-name>Первый Канал</display-name>
          <url>http://1tv.ru</url>
        </channel>
        <channel id="2">
          <display-name>Россия 1</display-name>
        </channel>
        <programme start="20260629120000 +0300" stop="20260629130000 +0300" channel="1">
          <title>Новости</title>
        </programme>
      </tv>
    """.trimIndent()

    val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
    val result = IptvParser.parseXml(0L, inputStream)
    
    assertNotNull(result)
    // If the state leak bug exists, "Россия 1" (id="2") will NOT be parsed because chUrl is not cleared!
    assertEquals("Первый Канал", result.channelIdToName["1"])
    assertEquals("Россия 1", result.channelIdToName["2"])
  }
}
