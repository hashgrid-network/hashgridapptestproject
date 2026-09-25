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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy

@Composable
fun KycSubmissionModal(
    currentStatus: String,
    onDismiss: () -> Unit,
    onSubmitKyc: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var selectedIdTypeIndex by remember { mutableIntStateOf(0) }
    val idTypes = listOf("Passport", "National ID", "Driver's License")
    var isDocumentAttached by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.2.dp, GoldBorderSubtle, RoundedCornerShape(28.dp))
                .testTag("kyc_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
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
                                .background(GoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "INSTITUTIONAL KYC",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "Identity & Compliance Verification",
                                fontSize = 11.sp,
                                color = SlateGray
                            )
                        }
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

                // Current Status Banner
                val statusColor = when (currentStatus.uppercase()) {
                    "VERIFIED" -> MintDark
                    "PENDING REVIEW" -> Color(0xFFD97706)
                    else -> SlateGray
                }
                val statusBg = when (currentStatus.uppercase()) {
                    "VERIFIED" -> MintGreen.copy(alpha = 0.2f)
                    "PENDING REVIEW" -> Color(0xFFFEF3C7)
                    else -> Color(0xFFF3F4F6)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Status:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianNavy
                    )
                    Text(
                        text = currentStatus.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (currentStatus.equals("VERIFIED", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MintGreen.copy(alpha = 0.15f))
                            .border(1.dp, MintGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MintDark,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "TIER-2 INSTITUTIONAL KYC VERIFIED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your account has unrestricted access to full hashpower allocation, cold storage audits, and unlimited withdrawals.",
                                fontSize = 10.sp,
                                color = SlateGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Text("CLOSE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else if (currentStatus.equals("PENDING REVIEW", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFFD97706),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "APPLICATION UNDER REVIEW",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your submitted government ID proof is being processed by the institutional compliance desk. Verification typically completes within 1-2 business hours.",
                                fontSize = 10.sp,
                                color = SlateGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    // UNVERIFIED FORM
                    Text(
                        text = "Full Legal Name",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMessage = null },
                        placeholder = { Text("e.g. Alexander Vance", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldGradientEnd,
                            unfocusedBorderColor = GoldBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Document Type",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TabRow(
                        selectedTabIndex = selectedIdTypeIndex,
                        containerColor = Color(0xFFF1ECE4),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedIdTypeIndex]),
                                color = GoldGradientEnd
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        idTypes.forEachIndexed { index, type ->
                            Tab(
                                selected = selectedIdTypeIndex == index,
                                onClick = { selectedIdTypeIndex = index },
                                text = {
                                    Text(
                                        text = type,
                                        fontSize = 10.sp,
                                        fontWeight = if (selectedIdTypeIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedIdTypeIndex == index) ObsidianNavy else SlateGray
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Document / Identification Number",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = idNumber,
                        onValueChange = { idNumber = it; errorMessage = null },
                        placeholder = { Text("e.g. A92837418", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldGradientEnd,
                            unfocusedBorderColor = GoldBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ID Upload Box
                    Text(
                        text = "Upload Document Photo / Scan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDocumentAttached) MintGreen.copy(alpha = 0.15f) else Color(0xFFF9F7F3))
                            .border(
                                1.dp,
                                if (isDocumentAttached) MintDark else GoldBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                isDocumentAttached = true
                                Toast.makeText(context, "ID photo attached successfully.", Toast.LENGTH_SHORT).show()
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDocumentAttached) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = if (isDocumentAttached) MintDark else GoldGradientEnd,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDocumentAttached) "document_scan_front.jpg attached (Verified)" else "Tap to attach ID photo or PDF scan",
                                fontSize = 11.sp,
                                fontWeight = if (isDocumentAttached) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDocumentAttached) MintDark else SlateNavy
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

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (fullName.trim().length < 2) {
                                errorMessage = "Please enter your full legal name."
                                return@Button
                            }
                            if (idNumber.trim().length < 3) {
                                errorMessage = "Please enter a valid document ID number."
                                return@Button
                            }
                            if (!isDocumentAttached) {
                                errorMessage = "Please attach your ID document photo/scan."
                                return@Button
                            }

                            isSubmitting = true
                            onSubmitKyc(fullName.trim(), idTypes[selectedIdTypeIndex], idNumber.trim())
                            isSubmitting = false
                            Toast.makeText(context, "KYC documents submitted for compliance review.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = "SUBMIT FOR COMPLIANCE VERIFICATION",
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
}
