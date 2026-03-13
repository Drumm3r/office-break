package com.drumm3r.officebreak.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    @SerialName("name")
    val name: String,
    @SerialName("isEnabled")
    val isEnabled: Boolean = true,
)
