package ai.dadacast.app.ui

import ai.dadacast.app.R
import androidx.annotation.DrawableRes

/** One opaque art tile per series, shared by our screens and the media app. */
@DrawableRes
fun artRes(category: String?): Int = when (category) {
    "interview_rehearsal" -> R.drawable.art_interview
    "my_material_rehearsal" -> R.drawable.art_material
    else -> R.drawable.art_english
}

fun artUri(category: String?): String {
    val name = when (category) {
        "interview_rehearsal" -> "art_interview"
        "my_material_rehearsal" -> "art_material"
        else -> "art_english"
    }
    return "android.resource://ai.dadacast.app/drawable/$name"
}
