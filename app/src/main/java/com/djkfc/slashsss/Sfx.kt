package com.djkfc.slashsss

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Процедурные звуки: генерируем WAV в кэш и грузим в SoundPool. */
class Sfx(ctx: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val ids = HashMap<String, Int>()
    private val rate = 22050
    private val rnd = Random(7)
    var enabled = true

    init {
        try {
            val dir = File(ctx.cacheDir, "sfx").apply { mkdirs() }
            gen(dir, "swoosh", 0.17f) { t, d, s -> noiseLp(s, 0.55f - 0.45f * (t / d)) * env(t, d) * 0.55f }
            gen(dir, "heavy", 0.30f) { t, d, s -> noiseLp(s, 0.25f - 0.2f * (t / d)) * env(t, d) * 0.8f }
            gen(dir, "hit", 0.13f) { t, d, s ->
                (noiseLp(s, 0.7f) * exp(-t * 40f) * 0.7f + sin(2 * PI * 140 * t).toFloat() * exp(-t * 25f) * 0.7f)
            }
            gen(dir, "bighit", 0.30f) { t, d, s ->
                (noiseLp(s, 0.35f) * exp(-t * 14f) * 0.8f + sin(2 * PI * (90 - 40 * t / d) * t).toFloat() * exp(-t * 9f))
            }
            gen(dir, "kill", 0.32f) { t, d, s ->
                (noiseLp(s, 0.5f) * exp(-t * 10f) * 0.6f + sq(2 * PI * (420 - 300 * t / d) * t) * exp(-t * 8f) * 0.3f)
            }
            gen(dir, "jump", 0.09f) { t, d, _ -> sin(2 * PI * (320 + 900 * t) * t).toFloat() * env(t, d) * 0.3f }
            gen(dir, "dash", 0.16f) { t, d, s -> noiseLp(s, 0.15f + 0.4f * (t / d)) * env(t, d) * 0.6f }
            gen(dir, "hurt", 0.26f) { t, d, _ -> sq(2 * PI * (190 - 120 * t / d) * t) * exp(-t * 9f) * 0.4f }
            gen(dir, "rank", 0.40f) { t, _, _ ->
                val f = if (t < 0.1f) 523.0 else if (t < 0.2f) 659.0 else 784.0
                sq(2 * PI * f * t) * exp(-(t % 0.1f) * 12f) * 0.25f
            }
            gen(dir, "block", 0.22f) { t, _, _ ->
                ((sin(2 * PI * 1250 * t) + sin(2 * PI * 1870 * t) * 0.7 + sin(2 * PI * 2630 * t) * 0.5).toFloat()) * exp(-t * 18f) * 0.3f
            }
            gen(dir, "wave", 1.1f) { t, _, _ ->
                ((sin(2 * PI * 98 * t) + sin(2 * PI * 147 * t) * 0.6 + sin(2 * PI * 196 * t) * 0.3).toFloat()) * exp(-t * 3f) * 0.45f
            }
            gen(dir, "shot", 0.12f) { t, d, s -> (sq(2 * PI * (900 - 600 * t / d) * t) * 0.25f + noiseLp(s, 0.6f) * 0.2f) * env(t, d) }
            gen(dir, "rage", 0.7f) { t, d, s ->
                (sq(2 * PI * (55 + 40 * t) * t) * 0.35f + noiseLp(s, 0.2f) * 0.4f) * env(t, d)
            }
            gen(dir, "orb", 0.18f) { t, d, _ -> sin(2 * PI * (880 + 600 * t / d) * t).toFloat() * env(t, d) * 0.25f }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var lp = 0f
    private fun noiseLp(s: Int, a: Float): Float {
        if (s == 0) lp = 0f
        val n = rnd.nextFloat() * 2f - 1f
        lp += a.coerceIn(0.02f, 1f) * (n - lp)
        return lp * 1.6f
    }

    private fun env(t: Float, d: Float): Float {
        val k = (t / d).coerceIn(0f, 1f)
        return sin(PI * k).toFloat()
    }

    private fun sq(ph: Double): Float = if (sin(ph) >= 0) 1f else -1f

    private fun gen(dir: File, name: String, dur: Float, fn: (Float, Float, Int) -> Float) {
        val n = (dur * rate).toInt()
        val data = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()); data.putInt(36 + n * 2); data.put("WAVE".toByteArray())
        data.put("fmt ".toByteArray()); data.putInt(16); data.putShort(1); data.putShort(1)
        data.putInt(rate); data.putInt(rate * 2); data.putShort(2); data.putShort(16)
        data.put("data".toByteArray()); data.putInt(n * 2)
        for (i in 0 until n) {
            val t = i.toFloat() / rate
            val v = (fn(t, dur, i) * 32000f).coerceIn(-32767f, 32767f)
            data.putShort(v.toInt().toShort())
        }
        val f = File(dir, "$name.wav")
        FileOutputStream(f).use { it.write(data.array()) }
        ids[name] = pool.load(f.absolutePath, 1)
    }

    fun play(name: String, vol: Float = 1f, pitch: Float = 1f) {
        if (!enabled) return
        val id = ids[name] ?: return
        pool.play(id, vol, vol, 1, 0, pitch.coerceIn(0.5f, 2f))
    }
}
