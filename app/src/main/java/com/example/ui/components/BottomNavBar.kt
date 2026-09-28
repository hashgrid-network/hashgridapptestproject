package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray

data class BottomBarTab(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isMiningActive: Boolean = true,
    onOpenMiningSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val leftTabs = listOf(
        BottomBarTab(0, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        BottomBarTab(1, "Rigs Store", Icons.Filled.Memory, Icons.Outlined.Memory, "nav_plans")
    )
    val rightTabs = listOf(
        BottomBarTab(2, "Cloud Miner", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_wallet"),

        BottomBarTab(3, "Network", Icons.Filled.Share, Icons.Outlined.Share, "nav_growth")
    )

    // Pulse animation for inactive node
    val infiniteTransition = rememberInfiniteTransition(label = "HeroFabAnim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Obsidian Bar Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 18.dp, spotColor = Color.Black)
                .background(
                    color = Color(0xFF0F131D),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .border(
                    width = 1.dp,
                    color = GoldBorderSubtle,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 2 Slots
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    leftTabs.forEach { tab ->
                        NavTabItem(
                            tab = tab,
                            isSelected = selectedTab == tab.index,
                            onClick = { onTabSelected(tab.index) }
                        )
                    }
                }

                // Space for Center Elevated FAB
                Spacer(modifier = Modifier.size(68.dp))

                // Right 2 Slots
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rightTabs.forEach { tab ->
                        NavTabItem(
                            tab = tab,
                            isSelected = selectedTab == tab.index,
                            onClick = { onTabSelected(tab.index) }
                        )
                    }
                }
            }
        }

        // ========================================================
        // CENTER SLOT: ELEVATED FLOATING ACTION BUTTON (20dp ABOVE)
        // ========================================================
        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Dual-ring metallic gold gradient outer ring
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .then(
                        if (!isMiningActive) Modifier.scale(pulseScale)
                        else Modifier
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                GoldLight,
                                GoldGradientMid,
                                GoldGradientEnd,
                                GoldLight
                            )
                        )
                    )
                    .padding(2.5.dp) // Outer ring thickness
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = GoldGradientEnd),
                contentAlignment = Alignment.Center
            ) {
                // Inner Obsidian Disc with radar arc or pulse
                Box(
                    modifier = Modifier
                        .size(59.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F1422))
                        .then(
                            if (isMiningActive) {
                                Modifier.border(
                                    width = 2.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF00E676),
                                            Color(0xFF00C853).copy(alpha = 0.2f),
                                            Color(0xFF00E676)
                                        )
                                    ),
                                    shape = CircleShape
                                ).rotate(radarRotation)
                            } else {
                                Modifier.border(1.dp, GoldBorderSubtle, CircleShape)
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 30.dp)
                        ) { onOpenMiningSheet() }
                        .testTag("center_elevated_mining_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Mining Hub",
                            tint = if (isMiningActive) Color(0xFF00E676) else GoldGradientEnd,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isMiningActive) "24H ACTIVE" else "TAP TO MINE",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.3.sp,
                            color = if (isMiningActive) Color(0xFF00E676) else GoldGradientEnd
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTabItem(
    tab: BottomBarTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 22.dp)
            ) { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(tab.tag)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) GoldGradientEnd.copy(alpha = 0.18f) else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label,
                tint = if (isSelected) GoldGradientEnd else SlateGray,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = tab.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else SlateGray
        )

        // Active indicator dot
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(3.5.dp)
                .clip(CircleShape)
                .background(if (isSelected) GoldGradientEnd else Color.Transparent)
        )
    }
}
