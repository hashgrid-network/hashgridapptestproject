package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.util.TickerEngine
import kotlinx.coroutines.delay

@Composable
fun TopBar(
    unreadNotificationCount: Int = 3,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentBroadcast by remember {
        mutableStateOf(TickerEngine.getRecentBroadcasts().firstOrNull() ?: TickerEngine.generateNextBroadcast())
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(TickerEngine.nextDelay()) // 20s to 25s delay
            currentBroadcast = TickerEngine.generateNextBroadcast()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasBackground)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Logo & Profile Avatar trigger
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onProfileClick() }
                .padding(2.dp)
                .testTag("brand_header")
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianNavy)
                    .border(1.2.dp, GoldBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⬡",
                    fontSize = 22.sp,
                    color = GoldGradientMid,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HASHGRID",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.5.sp,
                        color = ObsidianNavy
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldGradientEnd.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
                Text(
                    text = "Profile & Account Settings",
                    fontSize = 9.sp,
                    color = SlateGray,
                    letterSpacing = 0.1.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dynamic Live Network Broadcast Pill
        Card(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onNotificationClick() }
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(20.dp))
                .testTag("header_live_ticker_pill"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121722))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "LIVE",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E676),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                AnimatedContent(
                    targetState = currentBroadcast,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    },
                    label = "HeaderTickerAnimation"
                ) { item ->
                    Text(
                        text = "${item.icon} ${item.message}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
