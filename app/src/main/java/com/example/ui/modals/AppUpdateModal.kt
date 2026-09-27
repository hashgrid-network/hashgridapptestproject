package com.example.ui.modals

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.service.AppUpdateInfo
import com.example.service.AppUpdateManager
import com.example.service.UpdateStatus
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldBrush
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import java.io.File

@Composable
fun AppUpdateModal(
    status: UpdateStatus,
    onDismiss: () -> Unit,
    onUpdateNow: (AppUpdateInfo) -> Unit,
    onInstallNow: (File) -> Unit
) {
    val context = LocalContext.current

    val updateInfo = when (status) {
        is UpdateStatus.UpdateAvailable -> status.info
        is UpdateStatus.Downloading -> status.info
        is UpdateStatus.ReadyToInstall -> status.info
        else -> null
    } ?: return

    val isMandatory = updateInfo.isMandatory

    Dialog(
        onDismissRequest = {
            if (!isMandatory && status !is UpdateStatus.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory && status !is UpdateStatus.Downloading,
            dismissOnClickOutside = !isMandatory && status !is UpdateStatus.Downloading
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(1.2.dp, GoldBorderSubtle, RoundedCornerShape(26.dp))
                .testTag("app_update_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header: Icon + Close (if not mandatory)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Update Available",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "New Version Released",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldGradientEnd
                            )
                        }
                    }

                    if (!isMandatory && status !is UpdateStatus.Downloading) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F3F5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = SlateGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alert prompt text
                Text(
                    text = "A new version of HashGrid is available. Please update to continue.",
                    fontSize = 13.sp,
                    color = ObsidianNavy,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Version Comparison Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F9FA))
                        .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT VERSION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SlateNavy
                        )
                    }

                    Text(
                        text = "→",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldGradientEnd
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LATEST VERSION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "v${updateInfo.versionName} (${updateInfo.versionCode})",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = MintDark
                        )
                    }
                }

                // Release Notes (if provided)
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldLight.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "WHAT'S NEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = GoldGradientEnd
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateInfo.releaseNotes,
                            fontSize = 11.sp,
                            color = ObsidianNavy,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dynamic Status & Action Area
                val officialReleasesUrl = "https://github.com/hashgrid-network/hashgridapptestproject/releases"
                val openWebpage = {
                    try {
                        val webpageIntent = Intent(Intent.ACTION_VIEW, Uri.parse(officialReleasesUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webpageIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening download webpage...", Toast.LENGTH_SHORT).show()
                    }
                }

                when (status) {
                    is UpdateStatus.Downloading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading APK in background...",
                                    fontSize = 11.sp,
                                    color = SlateGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${status.progressPercent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = GoldGradientEnd
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { status.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = GoldGradientEnd,
                                trackColor = GoldLight
                            )
                        }
                    }

                    is UpdateStatus.ReadyToInstall -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE6FBF2))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MintDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Latest HashGrid Release Available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintDark
                                )
                            }

                            Button(
                                onClick = {
                                    openWebpage()
                                    onInstallNow(status.apkFile)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .testTag("install_now_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(GoldBrush),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = ObsidianNavy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "UPDATE",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = ObsidianNavy
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // UpdateAvailable State
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!isMandatory) {
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F3F5))
                                ) {
                                    Text(
                                        text = "Later",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SlateGray
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    openWebpage()
                                    onUpdateNow(updateInfo)
                                },
                                modifier = Modifier
                                    .weight(if (isMandatory) 1f else 1.5f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .testTag("update_now_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(GoldBrush),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = ObsidianNavy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "UPDATE",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = ObsidianNavy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
