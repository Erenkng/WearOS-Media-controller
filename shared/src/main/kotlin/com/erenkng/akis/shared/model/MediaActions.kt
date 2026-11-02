package com.erenkng.akis.shared.model

/**
 * `NowPlayingState.actions` bit maskesi.
 *
 * Değerler `android.media.session.PlaybackState.ACTION_*` sabitlerinin birebir kopyasıdır;
 * `:shared` Android'e bağımlı olmadığı için burada tekrar tanımlanır.
 */
object MediaActions {
    const val STOP = 1L shl 0
    const val PAUSE = 1L shl 1
    const val PLAY = 1L shl 2
    const val REWIND = 1L shl 3
    const val SKIP_TO_PREVIOUS = 1L shl 4
    const val SKIP_TO_NEXT = 1L shl 5
    const val FAST_FORWARD = 1L shl 6
    const val SET_RATING = 1L shl 7
    const val SEEK_TO = 1L shl 8
    const val PLAY_PAUSE = 1L shl 9
    const val PLAY_FROM_MEDIA_ID = 1L shl 10
    const val PLAY_FROM_SEARCH = 1L shl 11
    const val SKIP_TO_QUEUE_ITEM = 1L shl 12
    const val PLAY_FROM_URI = 1L shl 13
    const val PREPARE = 1L shl 14
    const val SET_REPEAT_MODE = 1L shl 18
    const val SET_SHUFFLE_MODE = 1L shl 21
    const val SET_PLAYBACK_SPEED = 1L shl 22

    fun has(actions: Long, action: Long): Boolean = actions and action != 0L

    /**
     * Bazı uygulamalar (özellikle tarayıcılar) eylem maskesini hiç doldurmaz (0).
     * Bu durumda oynat/duraklat'ı kapatmak kullanıcıyı kilitler; o yüzden açık sayılır.
     */
    fun canPlay(actions: Long): Boolean = actions == 0L || has(actions, PLAY) || has(actions, PLAY_PAUSE)

    fun canPause(actions: Long): Boolean = actions == 0L || has(actions, PAUSE) || has(actions, PLAY_PAUSE)

    fun canSkipNext(actions: Long): Boolean = has(actions, SKIP_TO_NEXT)

    fun canSkipPrevious(actions: Long): Boolean = has(actions, SKIP_TO_PREVIOUS)

    fun canSeek(actions: Long): Boolean = has(actions, SEEK_TO)
}
