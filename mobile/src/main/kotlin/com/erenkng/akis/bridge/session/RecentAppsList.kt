package com.erenkng.akis.bridge.session

import kotlinx.serialization.Serializable

@Serializable
data class RecentApp(
    val packageName: String,
    val appLabel: String,
    val lastTitle: String = "",
)

/** "Son kullanılanlar" listesinin saf mantığı (BRIEF Bölüm 7.2, madde 3). */
object RecentAppsList {
    /** Saatte gösterilen sayı. */
    const val VISIBLE = 5

    /** Saklanan sayı; aktif olanlar çıkarıldıktan sonra da 5 satır kalsın diye fazlası tutulur. */
    const val STORED = 10

    fun push(list: List<RecentApp>, app: RecentApp, max: Int = STORED): List<RecentApp> =
        (listOf(app) + list.filterNot { it.packageName == app.packageName }).take(max)

    /** Oturumu hâlâ açık olan uygulamalar "Şu an aktif" altında zaten görünür. */
    fun visible(list: List<RecentApp>, activePackages: Set<String>, max: Int = VISIBLE): List<RecentApp> =
        list.filterNot { it.packageName in activePackages }.take(max)
}
