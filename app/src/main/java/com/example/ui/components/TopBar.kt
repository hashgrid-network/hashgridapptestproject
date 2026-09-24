package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray

@Composable
fun TopBar(
    unreadNotificationCount: Int = 3,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Logo: Isometric Cube + "HASHGRID"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("brand_header")
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianNavy)
                    .border(1.2.dp, GoldBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⬡",
                    fontSize = 24.sp,
                    color = GoldGradientMid,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HASHGRID",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 2.sp,
                        color = ObsidianNavy
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldGradientEnd.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "INSTITUTIONAL",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Text(
                    text = "Arctic Geothermal Infrastructure",
                    fontSize = 10.sp,
                    color = SlateGray,
                    letterSpacing = 0.2.sp
                )
            }
        }

        // Notification Bell with dynamic unread badge
        IconButton(
            onClick = onNotificationClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CardWhite)
                .border(1.dp, GoldBorderSubtle, CircleShape)
                .testTag("notifications_button")
        ) {
            if (unreadNotificationCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = CrimsonRed,
                            contentColor = Color.White
                        ) {
                            Text("$unreadNotificationCount", fontSize = 9.sp)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = ObsidianNavy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = ObsidianNavy,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
