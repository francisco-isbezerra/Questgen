package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class DashboardChallengesResponse(
    val status: String,
    val message: String?,
    @SerializedName("active_challenge") val activeChallenge: Challenge?,
    @SerializedName("pending_challenges") val pendingChallenges: List<Challenge>?,
    val updated_user: User? = null
)
