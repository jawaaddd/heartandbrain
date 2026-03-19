package com.heartandbrain.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heartandbrain.app.data.Clip
import com.heartandbrain.app.ui.theme.Primary
import com.heartandbrain.app.ui.theme.Subtle

@Composable
fun QuoteCard(clip: Clip, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16132B), // deep purple-dark tint
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .padding(bottom = 6.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(Primary, Color.Transparent)),
                            shape = RoundedCornerShape(2.dp),
                        )
                        .padding(vertical = 1.5.dp),
                )
            }

            Text(
                text = "\u201C${clip.quoteText ?: clip.title}\u201D",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                lineHeight = 20.sp,
            )

            Text(
                text = clip.category.name.lowercase().replaceFirstChar { it.uppercase() },
                color = Subtle,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
