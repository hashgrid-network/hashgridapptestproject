package com.example.ui.modals

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy

@Composable
fun CreatorMilestoneModal(
    userId: String,
    referralCode: String,
    onDismiss: () -> Unit,
    onSubmit: (channelUrl: String, videoUrl: String, contactTelegram: String) -> String?
) {
    val context = LocalContext.current
    var channelUrl by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var contactTelegram by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(1.2.dp, GoldBorder, RoundedCornerShape(26.dp))
                .testTag("claim_creator_milestone_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldLight)
                                .border(1.dp, GoldBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "CLAIM CREATOR MILESTONE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "50,000+ Verified Organic Views",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldGradientEnd
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Rules Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF9F7F3))
                        .border(1.dp, GoldBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = MintDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MANDATORY QUALIFICATION RULES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = ObsidianNavy,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val rules = listOf(
                            "1. Authentic video review or showcase of HashGrid (Minimum 2+ minutes duration).",
                            "2. Your User ID ($userId) and referral link (https://hashgrid.io/join?ref=$referralCode) must be written in the video description.",
                            "3. Video must achieve 50,000+ verified organic views without botting or artificial manipulation."
                        )

                        rules.forEach { rule ->
                            Text(
                                text = rule,
                                fontSize = 10.sp,
                                color = SlateGray,
                                modifier = Modifier.padding(vertical = 2.dp),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Form Inputs
                OutlinedTextField(
                    value = channelUrl,
                    onValueChange = { channelUrl = it; errorMessage = null },
                    label = { Text("Channel Profile URL", fontSize = 11.sp) },
                    placeholder = { Text("https://youtube.com/@yourchannel", fontSize = 10.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("creator_channel_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldGradientEnd,
                        unfocusedBorderColor = GoldBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it; errorMessage = null },
                    label = { Text("50,000+ Views Video Link", fontSize = 11.sp) },
                    placeholder = { Text("https://youtube.com/watch?v=...", fontSize = 10.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("creator_video_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldGradientEnd,
                        unfocusedBorderColor = GoldBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = contactTelegram,
                    onValueChange = { contactTelegram = it; errorMessage = null },
                    label = { Text("Executive Contact Telegram Handle", fontSize = 11.sp) },
                    placeholder = { Text("@your_telegram", fontSize = 10.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("creator_telegram_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldGradientEnd,
                        unfocusedBorderColor = GoldBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Discretion Terms Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(0.8.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "⚖️ Discretion Terms: All creator milestone gifts are audited for organic engagement and disbursed at the sole discretion of HashGrid Executive Administration.",
                        fontSize = 9.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 13.sp
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 11.sp,
                        color = CrimsonRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val err = onSubmit(channelUrl, videoUrl, contactTelegram)
                        if (err != null) {
                            errorMessage = err
                        } else {
                            Toast.makeText(
                                context,
                                "Milestone dossier submitted for Executive Audit!",
                                Toast.LENGTH_LONG
                            ).show()
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("submit_creator_milestone_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = GoldGradientMid,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SUBMIT FOR EXECUTIVE AUDIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
