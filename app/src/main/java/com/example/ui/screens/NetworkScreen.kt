package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.model.BountyTask
import com.example.model.TeamMember

/**
 * NetworkScreen represents the Network / Syndicate / Growth screen.
 * Displays live stats bound to Firestore:
 * - Current User document: "teamCount" -> "$teamCount Joined", "extraHashrate" -> "+extraHashrate GH/s"
 * - Subcollection "users/{currentUid}/team": "TEAM SYNDICATE DIRECTORY (X ACTIVE)"
 * - Automatically triggers retroactive referral recovery and reconciliation on screen launch.
 */
@Composable
fun NetworkScreen(
    referralCode: String,
    teamCount: Long = 0L,
    extraHashrate: Double = 0.0,
    syndicateTier: String = "NOVICE",
    totalReferralRewardsUsdt: Double = 0.0,
    teamMembers: List<TeamMember> = emptyList(),
    referralRewards: List<com.example.model.ReferralReward> = emptyList(),
    bountyTasks: List<BountyTask> = emptyList(),
    freeAdCooldownHours: Int = 0,
    onClaimFreeAdSession: () -> Pair<Boolean, String> = { Pair(false, "") },
    onOpenBountyModal: (BountyTask) -> Unit = {},
    onOpenCreatorMilestoneModal: () -> Unit = {},
    onOpenSyndicateTerms: () -> Unit = {},
    onReconcile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onReconcile()
    }

    GrowthScreen(
        referralCode = referralCode,
        referralCount = teamCount,
        teamCount = teamCount,
        bonusHashrate = extraHashrate,
        extraHashrate = extraHashrate,
        syndicateTier = syndicateTier,
        totalReferralRewardsUsdt = totalReferralRewardsUsdt,
        teamMembers = teamMembers,
        referralRewards = referralRewards,
        bountyTasks = bountyTasks,
        freeAdCooldownHours = freeAdCooldownHours,
        onClaimFreeAdSession = onClaimFreeAdSession,
        onOpenBountyModal = onOpenBountyModal,
        onOpenCreatorMilestoneModal = onOpenCreatorMilestoneModal,
        onOpenSyndicateTerms = onOpenSyndicateTerms,
        modifier = modifier
    )
}
