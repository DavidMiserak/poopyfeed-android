package com.poopyfeed.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poopyfeed.android.ui.theme.Amber400
import com.poopyfeed.android.ui.theme.AppShapes
import com.poopyfeed.android.ui.theme.Orange400
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Rose400
import com.poopyfeed.android.ui.theme.White

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp),
        enabled = enabled && !isLoading,
        shape = AppShapes.small,
        contentPadding = PaddingValues(0.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors =
                                    if (enabled && !isLoading) {
                                        listOf(Rose400, Orange400, Amber400)
                                    } else {
                                        listOf(
                                            Rose400.copy(alpha = 0.5f),
                                            Orange400.copy(alpha = 0.5f),
                                            Amber400.copy(alpha = 0.5f),
                                        )
                                    },
                            ),
                        shape = AppShapes.small,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = text,
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GradientButtonPreview() {
    PoopyFeedTheme {
        GradientButton(text = "Get Started", onClick = {})
    }
}
