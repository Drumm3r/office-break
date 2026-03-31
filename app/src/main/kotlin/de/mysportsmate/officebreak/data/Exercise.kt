package de.mysportsmate.officebreak.data

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    @SerialName("name")
    val name: String,
    @SerialName("isEnabled")
    val isEnabled: Boolean = true,
    @SerialName("nameResKey")
    val nameResKey: String? = null,
) {

    fun displayName(context: Context): String {
        val resId = nameResKey?.let { ExerciseConfig.resKeyToId[it] }

        return if (resId != null) context.getString(resId) else name
    }
}
