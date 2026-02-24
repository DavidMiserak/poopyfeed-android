package com.poopyfeed.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Red50
import com.poopyfeed.android.ui.theme.Red500
import com.poopyfeed.android.ui.theme.Red700

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Red50, RoundedCornerShape(8.dp))
                .drawBehind {
                    drawLine(
                        color = Red500,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 8f,
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = Red700,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorBannerPreview() {
    PoopyFeedTheme {
        ErrorBanner(message = "Invalid email or password")
    }
}
