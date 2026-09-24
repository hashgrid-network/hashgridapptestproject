package com.example.ui.modals

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.example.model.BountyTask
import com.example.model.BountyType
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import java.util.Locale

@Composable
fun BountySubmissionModal(
    task: BountyTask,
    userId: String,
    referralCode: String,
    onDismiss: () -> Unit,
    onSubmitWhatsApp: (String, String) -> String?,
    onSubmitTelegram: (String) -> String?
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // WhatsApp state
    var whatsappViews by remember { mutableStateOf("") }
    var whatsappTime by remember { mutableStateOf("Today, posted 3 hours ago") }
    var mockScreenshotUploaded by remember { mutableStateOf(false) }

    // Telegram state
    var telegramUsername by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(1.2.dp, GoldBorder, RoundedCornerShape(26.dp))
                .testTag("bounty_submission_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = task.title.uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = ObsidianNavy
                        )
                        Text(
                            text = "Reward: $${String.format(Locale.US, "%.2f", task.rewardUsdt)} USDT Bonus Hash Voucher",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // WHATSAPP STATUS TASK ($0.20 USDT)
                // ==========================================
                if (task.type == BountyType.WHATSAPP) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF9F7F3))
                            .border(1.dp, GoldBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "DAILY SCREENSHOT VERIFICATION RULE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Broadcast Arctic PPA proof with your referral link on WhatsApp Status. Attach a screenshot showing contact views count and timestamp.",
                                fontSize = 10.sp,
                                color = SlateGray,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = whatsappViews,
                        onValueChange = { whatsappViews = it; errorMessage = null },
                        label = { Text("Views Count shown in Screenshot", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 52 views", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldGradientEnd,
                            unfocusedBorderColor = GoldBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mock Screenshot Attachment Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (mockScreenshotUploaded) MintGreen.copy(alpha = 0.1f) else Color(0xFFF1ECE4))
                            .border(
                                1.dp,
                                if (mockScreenshotUploaded) MintDark else GoldBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                mockScreenshotUploaded = true
                                Toast.makeText(context, "Screenshot attached successfully!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (mockScreenshotUploaded) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = if (mockScreenshotUploaded) MintDark else GoldGradientEnd,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (mockScreenshotUploaded) "whatsapp_status_proof_2026.png (Attached)" else "Tap to Select & Attach Screenshot",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (mockScreenshotUploaded) MintDark else ObsidianNavy
                            )
                        }
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!mockScreenshotUploaded) {
                                errorMessage = "Please tap to attach your WhatsApp status screenshot."
                                return@Button
                            }
                            val err = onSubmitWhatsApp(whatsappViews, whatsappTime)
                            if (err != null) {
                                errorMessage = err
                            } else {
                                Toast.makeText(context, "WhatsApp proof submitted for Admin Review!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Text(
                            text = "SUBMIT PROOF ($0.20 USDT)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // ==========================================
                // TELEGRAM TASK ($0.20 USDT)
                // ==========================================
                if (task.type == BountyType.TELEGRAM) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF9F7F3))
                            .border(1.dp, GoldBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Enter your active Telegram handle to verify membership in @HashGridOfficial Arctic Node community.",
                            fontSize = 10.sp,
                            color = SlateGray,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = telegramUsername,
                        onValueChange = { telegramUsername = it; errorMessage = null },
                        label = { Text("Telegram Username", fontSize = 11.sp) },
                        placeholder = { Text("@alex_crypto", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldGradientEnd,
                            unfocusedBorderColor = GoldBorder
                        ),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 11.sp,
                            color = CrimsonRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val err = onSubmitTelegram(telegramUsername)
                            if (err != null) {
                                errorMessage = err
                            } else {
                                Toast.makeText(context, "Telegram handle submitted for verification!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Text(
                            text = "SUBMIT TELEGRAM HANDLE ($0.20 USDT)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
