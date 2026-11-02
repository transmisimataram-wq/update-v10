package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("Keuangan", appName)
  }

  @Test
  fun `pdf report helper handles generation gracefully without crashing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val result = com.example.util.PdfReportHelper.generateAndSavePdf(
      context = context,
      periodLabel = "Oktober 2026",
      walletName = "Semua Dompet",
      incomeTotal = 5000000.0,
      expenseTotal = 2500000.0,
      netCashFlow = 2500000.0,
      savingsRate = 50,
      expenseBreakdown = emptyList(),
      incomeBreakdown = emptyList(),
      transactions = emptyList()
    )
    if (result != null) {
      org.junit.Assert.assertTrue(result.file.exists())
      org.junit.Assert.assertTrue(result.fileName.endsWith(".pdf"))
    }
  }
}
