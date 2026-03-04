package com.poopyfeed.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poopyfeed.android.ui.theme.Amber400
import com.poopyfeed.android.ui.theme.Orange400
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Rose400
import com.poopyfeed.android.ui.theme.Slate800

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\uD83C\uDF7C",
            fontSize = 40.sp,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = "Poopy",
            style =
                TextStyle(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(Rose400, Orange400, Amber400),
                        ),
                ),
        )
        Text(
            text = "Feed",
            style =
                TextStyle(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrandLogoPreview() {
    PoopyFeedTheme {
        BrandLogo()
    }
}
