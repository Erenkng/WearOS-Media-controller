package com.erenkng.akis.wear.player

import com.erenkng.akis.shared.model.CustomActionInfo

/** Uygulamanın "beğen / favorilere ekle" özel eylemi ve şu anki durumu. */
data class LikeAction(val actionId: String, val liked: Boolean)

/**
 * Uygulamalar beğenme düğmesini standart bir alanla değil, özel eylem (`CustomAction`) olarak sunar;
 * bu yüzden etiket ve kimlikten tanınır. Çoğu uygulama, beğenildikten sonra eylemi "Beğenilenlerden
 * kaldır" gibi bir eylemle değiştirir: durum buradan çıkarılır.
 */
object LikeActionPolicy {
    private val LIKE = Regex(
        """(?i)(like|love|heart|favou?rite|favori|beğen|begen|kaydet|save|thumbs?[ _-]?up|add[ _-]?to[ _-]?(collection|library))""",
    )

    /** "Beğenme" / "thumbs down" beğenmenin tersi; beğen düğmesi sayılmaz. */
    private val DISLIKE = Regex("""(?i)(thumbs?[ _-]?down|dislike|beğenme(dim)?\b|begenme(dim)?\b|hoşlanma)""")

    /** Eylem geri almaysa şarkı şu an beğenilmiş demektir. */
    private val UNDO = Regex(
        """(?i)(remove|unlike|un-?love|un-?favou?rite|unsave|kaldır|kaldir|vazgeç|vazgec|çıkar|cikar|beğenmekten|begenmekten)""",
    )

    fun find(actions: List<CustomActionInfo>): LikeAction? {
        val action = actions.firstOrNull { candidate ->
            LIKE.containsMatchIn(candidate.text()) && !DISLIKE.containsMatchIn(candidate.label)
        } ?: return null
        return LikeAction(actionId = action.id, liked = UNDO.containsMatchIn(action.text()))
    }

    private fun CustomActionInfo.text(): String = "$label $id"
}
