package com.erenkng.akis.bridge.command

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.VolumeProvider
import android.media.session.MediaController
import android.os.Build
import android.util.Log
import com.erenkng.akis.bridge.R
import com.erenkng.akis.bridge.session.OutputNames

/** Ses okuma/yazma ve çıkış etiketi (BRIEF Bölüm 10, "Ses"). */
class VolumeController(private val context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    data class VolumeInfo(val volume: Int, val max: Int, val isRemote: Boolean)

    fun read(controller: MediaController?): VolumeInfo {
        val info = controller?.playbackInfo
        if (info != null && info.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
            val fixed = info.volumeControl == VolumeProvider.VOLUME_CONTROL_FIXED
            return VolumeInfo(info.currentVolume, if (fixed) 0 else info.maxVolume, isRemote = true)
        }
        val max = if (audioManager.isVolumeFixed) 0 else audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return VolumeInfo(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), max, isRemote = false)
    }

    /**
     * Göreli değişiklik telefonda **o anki gerçek değere** uygulanır ve sonuç yeniden ölçülür.
     * Saat birikmiş deltayı yollar, mutlak değer yollamaz; böylece iki taraf farklı değerlere
     * inanıyor olsa bile ses zıplamaz (Samsung'un sorunu, BRIEF Bölüm 3).
     *
     * @return ölçülen gerçek ses, değiştirilemiyorsa null.
     */
    fun adjust(controller: MediaController?, delta: Int): Int? {
        val current = read(controller)
        if (current.max <= 0) return null
        return set(controller, current.volume + delta)
    }

    fun set(controller: MediaController?, target: Int): Int? {
        val current = read(controller)
        if (current.max <= 0) return null
        val clamped = target.coerceIn(0, current.max)
        return try {
            if (current.isRemote && controller != null) {
                controller.setVolumeTo(clamped, 0)
                // Uzak cihaz değeri gecikmeli bildirir; istenen değer en iyi tahmindir.
                clamped
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Ses değiştirilemedi", e)
            null
        }
    }

    /** "Sony WH-1000XM5 · Bluetooth", "Telefon hoparlörü" … */
    fun outputLabel(isRemote: Boolean): String {
        if (isRemote) return context.getString(R.string.output_remote)
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        outputs.firstOrNull { it.type in BLUETOOTH_TYPES }?.let { device ->
            // "Eren Doğu adlı kişiye ait Buds3 FE" → "Buds3 FE"
            val name = device.productName?.toString()?.let(OutputNames::shorten)?.takeIf { it.isNotBlank() }
            return if (name != null) {
                context.getString(R.string.output_bluetooth_suffix, name)
            } else {
                context.getString(R.string.output_bluetooth_generic)
            }
        }
        if (outputs.any { it.type in WIRED_TYPES }) return context.getString(R.string.output_wired)
        return context.getString(R.string.output_phone_speaker)
    }

    private companion object {
        const val TAG = "VolumeController"

        val BLUETOOTH_TYPES: Set<Int> = buildSet {
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_HEARING_AID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(AudioDeviceInfo.TYPE_BLE_HEADSET)
                add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
            }
        }

        val WIRED_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}
