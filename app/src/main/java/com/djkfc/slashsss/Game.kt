package com.djkfc.slashsss

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.random.Random

class Btn(val id: Int, val label: String) {
    var x = 0f; var y = 0f; var r = 0f
    var held = false; var pid = -1; var press = 0f
}

class UiBtn(val id: String, val rect: RectF)

class Game(val ctx: Context) {
    enum class St { MENU, PLAY, PAUSE, PERK, OVER, MOVES }

    companion object {
        const val B_ATK = 0; const val B_HVY = 1; const val B_JUMP = 2; const val B_DASH = 3
        const val B_SW = 4; const val B_RAGE = 5; const val B_PAUSE = 6
        val RANK_T = floatArrayOf(0f, 110f, 260f, 450f, 680f, 950f, 1280f)
        const val STYLE_MAX = 1550f
        val RANK_L = arrayOf("D", "C", "B", "A", "S", "SS", "SSS")
        val RANK_N = arrayOf("DULL", "COOL", "BRUTAL", "ATOMIC", "SAVAGE", "SSADISTIC", "SLASHSSS!")
        val RANK_C = intArrayOf(
            0xFF9AA0B5.toInt(), 0xFF4DA3FF.toInt(), 0xFF4DFF9A.toInt(), 0xFFFFD84D.toInt(),
            0xFFFF8A3D.toInt(), 0xFFFF3D5A.toInt(), 0xFFFF4DE1.toInt()
        )
        val REFLECT = Move("rf", "REFLECT", "", 0f, 0f, 0f, 0f, 0f, 0f, 0f, 30f, 450f, -250f, style = 40f, stop = 0.08f, shake = 8f, heavy = true)
    }

    val sfx = Sfx(ctx)
    private val prefs = ctx.getSharedPreferences("slashsss", Context.MODE_PRIVATE)
    @Suppress("DEPRECATION")
    private val vib: Vibrator? = try { ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator } catch (e: Exception) { null }
    val rnd = Random(System.nanoTime())
    val render = Renderer(this)

    var W = 1f; var H = 1f; var s = 1f; var viewW = 1f
    var state = St.MENU
    var movesFrom = St.MENU
    var movesTab = 0
    var time = 0f
    var gt = 0f

    var player = Player()
    val enemies = ArrayList<Enemy>()
    val projs = ArrayList<Proj>()
    val parts = ArrayList<Particle>()
    val slashes = ArrayList<SlashFx>()
    val texts = ArrayList<FloatText>()
    val ghosts = ArrayList<Ghost>()

    var camX = 0f
    var shake = 0f
    var hitstop = 0f
    var slowT = 0f
    var flashT = 0f

    var style = 0f
    var rank = 0
    var rankPop = 0f
    var styleIdle = 0f
    val recent = ArrayDeque<String>()
    var bestRank = 0
    var combo = 0
    var comboT = 0f
    var comboPop = 0f
    var maxCombo = 0
    var hitsTotal = 0
    var rage = 0f
    var rageT = 0f
    var score = 0
    var kills = 0
    var wave = 0
    var waveState = 0
    var waveT = 0f
    var toSpawn = 0
    var spawnT = 0f
    var maxAlive = 4
    var eliteLeft = 0
    var switchBonus = 0f
    var moveName = ""
    var moveNameT = 0f
    var ghostT = 0f

    // перки
    var dmgMul = 1f; var rageMul = 1f; var decayMul = 1f; var lifesteal = 0f; var atkSpeed = 1f
    var crit = 0f; var orbMul = 1f; var dashIframe = 0f; var dashCdMul = 1f; var airDmg = 0f; var styleMul = 1f
    var perkChoices: List<Perk> = emptyList()
    val taken = ArrayList<String>()

    var bestWave = prefs.getInt("bestWave", 0)
    var bestScore = prefs.getInt("bestScore", 0)
    var bestCombo = prefs.getInt("bestCombo", 0)
    var newRecord = false

    // ввод
    val btns = arrayOf(
        Btn(B_ATK, "АТК"), Btn(B_HVY, "СИЛ"), Btn(B_JUMP, "ПРЫЖОК"), Btn(B_DASH, "РЫВОК"),
        Btn(B_SW, "СМЕНА"), Btn(B_RAGE, "ЯРОСТЬ"), Btn(B_PAUSE, "II")
    )
    var stickPid = -1
    var stickOx = 0f; var stickOy = 0f; var stickCx = 0f; var stickCy = 0f
    var sx = 0f; var sy = 0f
    val events = ArrayList<Int>()
    val ui = ArrayList<UiBtn>()
    val weaponRects = Array(5) { RectF() }

    var bufKind = 0
    var bufDir = ""
    var bufFace = 0
    var bufT = 0f

    fun resize(w: Int, h: Int) {
        W = w.toFloat(); H = h.toFloat()
        s = H / VIEW_H
        viewW = W / s
        val u = s
        fun place(b: Btn, x: Float, y: Float, r: Float) { b.x = x; b.y = y; b.r = r }
        place(btns[B_ATK], W - 150f * u, H - 135f * u, 80f * u)
        place(btns[B_HVY], W - 310f * u, H - 105f * u, 64f * u)
        place(btns[B_JUMP], W - 125f * u, H - 310f * u, 62f * u)
        place(btns[B_DASH], W - 285f * u, H - 265f * u, 56f * u)
        place(btns[B_SW], W - 440f * u, H - 220f * u, 48f * u)
        place(btns[B_RAGE], W - 460f * u, H - 85f * u, 48f * u)
        place(btns[B_PAUSE], W - 46f * u, 44f * u, 30f * u)
        render.resize()
    }

    fun onPause() {
        if (state == St.PLAY) state = St.PAUSE
        releaseAll()
    }

    fun onBack(): Boolean {
        return when (state) {
            St.PLAY -> { state = St.PAUSE; releaseAll(); true }
            St.PAUSE -> { state = St.PLAY; true }
            St.MOVES -> { state = movesFrom; true }
            St.PERK -> true
            St.OVER -> { state = St.MENU; true }
            St.MENU -> false
        }
    }

    // ============================== ВВОД ==============================

    private fun releaseAll() {
        for (b in btns) { b.held = false; b.pid = -1 }
        stickPid = -1; sx = 0f; sy = 0f
    }

    fun touch(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val x = e.getX(i); val y = e.getY(i); val pid = e.getPointerId(i)
                if (state != St.PLAY) { uiTap(x, y); return }
                for (k in 0 until 5) if (weaponRects[k].contains(x, y)) { switchWeapon(k); return }
                var best: Btn? = null
                var bd = Float.MAX_VALUE
                for (b in btns) {
                    val dx = x - b.x; val dy = y - b.y
                    val d = sqrt(dx * dx + dy * dy)
                    if (d < b.r * 1.25f && d < bd) { bd = d; best = b }
                }
                if (best != null) {
                    best.held = true; best.pid = pid; best.press = 1f
                    events.add(best.id)
                    return
                }
                if (x < W * 0.5f && stickPid == -1) {
                    stickPid = pid; stickOx = x; stickOy = y; stickCx = x; stickCy = y; sx = 0f; sy = 0f
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    if (e.getPointerId(i) == stickPid) {
                        val rmax = 85f * s
                        var dx = e.getX(i) - stickOx; var dy = e.getY(i) - stickOy
                        val d = sqrt(dx * dx + dy * dy)
                        if (d > rmax) {
                            // плавающий джойстик тянется за пальцем
                            stickOx += dx * (1f - rmax / d); stickOy += dy * (1f - rmax / d)
                            dx = e.getX(i) - stickOx; dy = e.getY(i) - stickOy
                        }
                        stickCx = e.getX(i); stickCy = e.getY(i)
                        sx = (dx / rmax).coerceIn(-1f, 1f); sy = (dy / rmax).coerceIn(-1f, 1f)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pid = e.getPointerId(e.actionIndex)
                if (pid == stickPid) { stickPid = -1; sx = 0f; sy = 0f }
                for (b in btns) if (b.pid == pid) { b.held = false; b.pid = -1 }
            }
            MotionEvent.ACTION_CANCEL -> releaseAll()
        }
    }

    private fun uiTap(x: Float, y: Float) {
        for (b in ui) if (b.rect.contains(x, y)) { onUi(b.id); return }
        if (state == St.MENU) onUi("play")
    }

    private fun onUi(id: String) {
        sfx.play("orb", 0.5f, 1.3f)
        when {
            id == "play" || id == "retry" -> newGame()
            id == "moves" -> { movesFrom = state; state = St.MOVES }
            id == "resume" -> state = St.PLAY
            id == "menu" -> state = St.MENU
            id == "back" -> state = movesFrom
            id == "sound" -> sfx.enabled = !sfx.enabled
            id.startsWith("tab") -> movesTab = id.substring(3).toInt()
            id.startsWith("perk") -> {
                val p = perkChoices[id.substring(4).toInt()]
                p.apply(this); taken.add(p.name)
                state = St.PLAY
                startWave()
            }
        }
    }

    private fun dirToken(): String {
        if (abs(sy) > 0.55f && abs(sy) >= abs(sx) * 0.9f) return if (sy < 0f) "u" else "d"
        if (abs(sx) > 0.5f) return "f"
        return ""
    }

    private fun buffer(kind: Int) {
        bufKind = kind; bufT = 0.32f
        bufDir = dirToken()
        bufFace = if (abs(sx) > 0.5f && bufDir == "f") sign(sx).toInt() else 0
    }

    // ============================== ИГРА ==============================

    fun newGame() {
        player = Player()
        enemies.clear(); projs.clear(); parts.clear(); slashes.clear(); texts.clear(); ghosts.clear()
        style = 0f; rank = 0; bestRank = 0; combo = 0; comboT = 0f; maxCombo = 0; hitsTotal = 0
        rage = 0f; rageT = 0f; score = 0; kills = 0; wave = 0; recent.clear()
        dmgMul = 1f; rageMul = 1f; decayMul = 1f; lifesteal = 0f; atkSpeed = 1f
        crit = 0f; orbMul = 1f; dashIframe = 0f; dashCdMul = 1f; airDmg = 0f; styleMul = 1f
        taken.clear(); newRecord = false
        camX = player.x - viewW / 2f
        hitstop = 0f; slowT = 0f; shake = 0f
        releaseAll(); events.clear(); bufKind = 0
        state = St.PLAY
        startWave()
    }

    private fun startWave() {
        wave++
        waveState = 0; waveT = 2.2f
        toSpawn = min(5 + wave * 2, 70)
        maxAlive = min(4 + wave / 2, 12)
        spawnT = 0.3f
        eliteLeft = if (wave % 5 == 0) wave / 5 else 0
        sfx.play("wave", 0.8f)
    }

    fun update(dtRaw: Float) {
        val dt = min(dtRaw, 0.05f)
        time += dt
        for (b in btns) b.press = max(0f, b.press - dt * 5f)
        rankPop = max(0f, rankPop - dt * 2.5f)
        comboPop = max(0f, comboPop - dt * 4f)
        if (state == St.MENU || state == St.OVER) {
            updateParticles(dt * 0.5f)
            return
        }
        if (state != St.PLAY) return

        if (hitstop > 0f) {
            hitstop -= dt
            shake = max(0f, shake - shake * min(1f, dt * 8f))
            return
        }
        val ts = if (slowT > 0f) 0.3f else 1f
        slowT -= dt
        flashT = max(0f, flashT - dt * 3f)
        val d = dt * ts
        gt += d

        updatePlayer(d)
        updateEnemies(d)
        updateProjs(d)
        updateParticles(d)
        updateStyle(d)
        updateWave(d)

        val p = player
        var target = p.x - viewW / 2f + p.face * 90f
        target = if (viewW >= ARENA_W) (ARENA_W - viewW) / 2f else target.coerceIn(0f, ARENA_W - viewW)
        camX += (target - camX) * min(1f, dt * 5f)
        shake = max(0f, shake - shake * min(1f, dt * 8f))
        moveNameT -= dt
    }

    // ---------------- игрок ----------------

    private fun updatePlayer(d: Float) {
        val p = player
        for (ev in events) when (ev) {
            B_ATK -> buffer(1)
            B_HVY -> buffer(2)
            B_JUMP -> buffer(3)
            B_DASH -> buffer(4)
            B_SW -> switchWeapon((p.weapon + 1) % 5)
            B_RAGE -> activateRage()
            B_PAUSE -> { state = St.PAUSE; releaseAll() }
        }
        events.clear()
        if (state != St.PLAY) return

        p.dashCd -= d; p.invuln -= d; p.hurtT -= d; bufT -= d
        p.squash = max(0f, p.squash - d * 5f)
        if (rageT > 0f) {
            rageT -= d
            if (rageT <= 0f) addText("ЯРОСТЬ ВСЁ", p.x, p.cy - 90f, 0xFFFFFFFF.toInt(), 30f, 1f)
        }
        switchBonus -= d

        // буфер ввода
        if (bufT > 0f && bufKind != 0 && p.hurtT <= 0f) {
            val m = p.move
            when (bufKind) {
                1, 2 -> {
                    val free = (m == null || p.mt >= m.cancelT) && p.dashT < 0.17f
                    if (free) { tryAttack(); bufKind = 0 }
                }
                3 -> if (m == null || p.mt >= m.cancelT || (m.fx == Fx.DIVE && p.mt < m.a0)) { doJump(); bufKind = 0 }
                4 -> if (p.dashCd <= 0f && (m == null || p.mt >= m.a1 * 0.8f)) { doDash(); bufKind = 0 }
            }
        }
        if (bufT <= 0f) bufKind = 0

        var gravMul = 1f
        val m = p.move
        if (m != null) {
            val sp = atkSpeed * (if (rageT > 0f) 1.2f else 1f)
            p.mt += d * sp
            val inActive = p.mt >= m.a0 && p.mt <= m.a1
            if (!p.fired && p.mt >= m.a0) { p.fired = true; onActiveStart(m) }
            val diving = m.fx == Fx.DIVE || m.fx == Fx.BOUNCE
            if (diving) {
                if (p.fired && !p.onGround) {
                    if (p.mt > m.a1 - 0.002f) p.mt = m.a1 - 0.002f
                    p.vy = if (m.lungeY > 0f) m.lungeY else 2100f
                    p.vx = p.face * m.lungeX
                } else if (p.fired && p.onGround && !p.landed) {
                    p.landed = true
                    if (m.fx == Fx.DIVE) landingImpact(m)
                    p.mt = max(p.mt, m.a1)
                    p.vx = 0f
                } else if (!p.fired) {
                    p.vy = min(p.vy, 0f) * 0.5f
                    gravMul = 0.1f
                }
            } else {
                if (m.lungeX != 0f && inActive) {
                    val k = (p.mt - m.a0) / max(0.01f, m.a1 - m.a0)
                    p.vx = p.face * m.lungeX * (1f - 0.6f * k)
                } else {
                    p.vx *= max(0f, 1f - d * 12f)
                }
                if (m.lungeY != 0f && p.mt >= m.a0 && !p.lungeYDone) {
                    p.lungeYDone = true; p.vy = m.lungeY; p.onGround = false
                }
                if (!p.onGround && m.lungeY >= 0f) {
                    gravMul = 0.2f
                    if (p.vy > 180f) p.vy = 180f
                }
            }
            if (inActive && p.move === m) doHits(m)
            if (p.move === m && m.shape == 4 && inActive) {
                val idx = ((p.mt - m.a0) / m.interval).toInt()
                if (idx != p.lastFlurry) {
                    p.lastFlurry = idx
                    val a = rnd.nextFloat() * 360f
                    slashes.add(SlashFx(p.x + p.face * (m.hx + m.hw * 0.5f), p.cy + m.hy + m.hh * 0.5f + rnd.nextFloat() * 30f - 15f,
                        m.rad * (0.5f + rnd.nextFloat() * 0.4f), a, 110f, wColor(), 0.14f, 0, p.face, 0f))
                    if (idx % 2 == 0) sfx.play("swoosh", 0.35f, 1.3f + rnd.nextFloat() * 0.4f)
                }
            }
            if (p.move === m && p.mt >= m.dur) endMove()
        }

        if (p.dashT > 0f) {
            p.dashT -= d
            p.vy = 0f
            gravMul = 0f
            if (p.dashT <= 0f) p.vx *= 0.35f
        } else if (p.move == null && p.hurtT <= 0f) {
            val target = if (abs(sx) > 0.2f) sx * 430f else 0f
            p.vx += (target - p.vx) * min(1f, d * 14f)
            if (abs(sx) > 0.25f) p.face = if (sx > 0f) 1 else -1
        } else if (p.hurtT > 0f) {
            p.vx *= max(0f, 1f - d * 3f)
        }

        p.vy += GRAV * gravMul * d
        p.x += p.vx * d
        p.y += p.vy * d
        if (p.y >= GROUND) {
            if (!p.onGround) {
                p.jumps = p.maxJumps; p.airDash = true; p.squash = 1f
                dust(p.x, GROUND, 6)
            }
            p.y = GROUND; p.vy = 0f; p.onGround = true
        } else {
            p.onGround = false
        }
        p.x = p.x.coerceIn(30f, ARENA_W - 30f)
        p.anim += d * (if (p.onGround) abs(p.vx) / 60f else 4f)

        // шарф
        p.scarfX[0] = p.x - p.face * 6f; p.scarfY[0] = p.y - 82f
        for (i in 1 until p.scarfX.size) {
            val tx = p.scarfX[i - 1] - p.face * 5f - p.vx * 0.012f
            val ty = p.scarfY[i - 1] + 1.5f - p.vy * 0.006f + kotlin.math.sin(time * 9f + i) * 1.6f
            p.scarfX[i] += (tx - p.scarfX[i]) * min(1f, d * 30f)
            p.scarfY[i] += (ty - p.scarfY[i]) * min(1f, d * 30f)
        }

        ghostT -= d
        if ((p.dashT > 0f || rageT > 0f || (m != null && m.lungeX > 800f)) && ghostT <= 0f) {
            ghostT = 0.035f
            ghosts.add(Ghost(p.x, p.y, p.face, if (rageT > 0f) 0xFFFF3D5A.toInt() else wColor(), 0.25f))
        }
    }

    fun wColor(): Int = Weapons.all[player.weapon].color

    private fun endMove() {
        val p = player
        p.lastMove = p.move; p.lastEnd = gt; p.move = null
    }

    private fun tryAttack() {
        val p = player
        val w = Weapons.all[p.weapon]
        val air = !p.onGround
        val btn = if (bufKind == 1) "L" else "H"
        var dir = bufDir
        if (p.dashT > 0f && dir == "") dir = "f"
        val pre = if (air) "a" else ""
        val tok = pre + dir + btn
        val neutral = pre + btn
        var prev: String? = null
        var pause = false
        val m = p.move
        if (m != null) prev = m.id
        else {
            val lm = p.lastMove
            if (lm != null) {
                val since = gt - p.lastEnd
                if (since < 0.17f) prev = lm.id
                else if (since < 0.85f) { prev = lm.id; pause = true }
            }
        }
        val mv = pick(w, prev, pause, tok, neutral) ?: return
        if (bufFace != 0) p.face = bufFace
        if (m != null) { p.lastMove = m; p.lastEnd = gt }
        startMove(mv)
    }

    private fun pick(w: Weapon, prev: String?, pause: Boolean, tok: String, neutral: String): Move? {
        val keys = ArrayList<String>(6)
        if (prev != null && pause) keys.add("$prev~$tok")
        if (prev != null) keys.add("$prev>$tok")
        keys.add(">$tok")
        if (prev != null && pause) keys.add("$prev~$neutral")
        if (prev != null) keys.add("$prev>$neutral")
        keys.add(">$neutral")
        for (k in keys) {
            val id = w.links[k] ?: continue
            return w.byId[id]
        }
        return null
    }

    private fun startMove(m: Move) {
        val p = player
        p.move = m; p.moveW = p.weapon; p.mt = 0f; p.fired = false; p.lungeYDone = false; p.landed = false
        p.hitSet.clear(); p.lastFlurry = -1
        p.dashT = 0f
        val rep = recent.count { it == m.id }
        p.varMul = 1f / (1f + rep * 0.6f)
        recent.addLast(m.id)
        if (recent.size > 8) recent.removeFirst()
        moveName = m.name; moveNameT = 1.1f
        if (!p.onGround && m.fx != Fx.DIVE && m.fx != Fx.BOUNCE) p.vy = min(p.vy, 0f) * 0.3f
    }

    private fun onActiveStart(m: Move) {
        val p = player
        val col = wColor()
        sfx.play(if (m.heavy) "heavy" else "swoosh", 0.7f, 0.9f + rnd.nextFloat() * 0.3f)
        when (m.shape) {
            0 -> slashes.add(SlashFx(p.x, p.cy, m.rad, m.arc0, m.arc1 - m.arc0, col, 0.2f, 0, p.face, 0f))
            1 -> slashes.add(SlashFx(p.x + p.face * m.hx, p.cy + m.hy + m.hh / 2f, 0f, 0f, 0f, col, 0.18f, 1, p.face, m.hw))
            2 -> {
                val cx = p.x + p.face * (if (m.both) 0f else m.hx + m.hw / 2f)
                slashes.add(SlashFx(cx, p.cy + m.hy + m.hh / 2f, m.rad, 0f, 360f, col, max(0.25f, m.a1 - m.a0), 2, p.face, 0f))
            }
            3 -> if (m.fx != Fx.DIVE) {
                slashes.add(SlashFx(p.x, p.cy, m.rad, m.arc0, m.arc1 - m.arc0, col, 0.22f, 0, p.face, 0f))
                slashes.add(SlashFx(p.x + p.face * m.hw * 0.4f, GROUND, m.rad, 0f, 0f, col, 0.35f, 3, p.face, 0f))
                shake += m.shake * 0.5f
                dust(p.x + p.face * 60f, GROUND, 10)
            } else {
                slashes.add(SlashFx(p.x, p.cy, m.rad * 0.8f, m.arc0, m.arc1 - m.arc0, col, 0.2f, 0, p.face, 0f))
            }
        }
        when (m.fx) {
            Fx.WAVE -> projs.add(Proj(p.x + p.face * 60f, GROUND - 45f, p.face * 900f, 0f, 70f, 90f, m.dmg, true, 1, 1.1f, m, col))
            Fx.BOOMERANG -> projs.add(Proj(p.x + p.face * 40f, p.cy - 10f, p.face * 1350f, 0f, 70f, 70f, m.dmg, true, 2, 2.5f, m, col))
            else -> {}
        }
    }

    private fun landingImpact(m: Move) {
        val p = player
        shake += m.shake
        hitstop = max(hitstop, 0.06f)
        slashes.add(SlashFx(p.x, GROUND, m.rad * 1.3f, 0f, 0f, wColor(), 0.4f, 3, p.face, 0f))
        dust(p.x, GROUND, 18)
        sfx.play("bighit", 0.8f, 0.8f)
        val r = m.rad * 1.1f
        for (e in enemies) {
            if (e.dead) continue
            if (abs(e.x - p.x) < r + e.w / 2f && e.y > GROUND - 160f && !p.hitSet.containsKey(e)) {
                p.hitSet[e] = p.mt
                val mm = Move("land", m.name, "", 0f, 0f, 0f, 0f, 0f, 0f, 0f, m.dmg * 0.7f, 260f, -520f, style = m.style * 0.6f, stun = 0.6f, stop = 0.03f)
                hitEnemy(e, mm, if (e.x >= p.x) 1 else -1)
            }
        }
    }

    private fun doJump() {
        val p = player
        val m = p.move
        if (m != null) { p.lastMove = m; p.lastEnd = gt; p.move = null }
        p.dashT = 0f
        if (p.onGround) {
            p.vy = -1080f; p.onGround = false
        } else if (p.jumps > 0) {
            p.jumps--; p.vy = -980f
            ring(p.x, p.y, 0xFFFFFFFF.toInt())
        } else {
            // шаг по врагу
            var stepped: Enemy? = null
            for (e in enemies) if (!e.dead && abs(e.x - p.x) < e.w / 2f + 50f && abs(e.cy - p.cy) < e.h / 2f + 70f) { stepped = e; break }
            if (stepped == null) return
            p.vy = -980f
            stepped.stun = max(stepped.stun, 0.3f)
            if (stepped.air) stepped.vy = 350f
            addStyle(28f, "step")
            addText("ENEMY STEP", p.x, p.cy - 80f, 0xFFFFFFFF.toInt(), 26f, 0.8f)
            ring(p.x, p.y, wColor())
        }
        if (abs(sx) > 0.25f) p.vx = sx * 430f
        sfx.play("jump", 0.5f)
    }

    private fun doDash() {
        val p = player
        val dir = if (abs(sx) > 0.3f) (if (sx > 0f) 1 else -1) else p.face
        if (!p.onGround) {
            if (!p.airDash) return
            p.airDash = false
        }
        val m = p.move
        if (m != null) { p.lastMove = m; p.lastEnd = gt; p.move = null }
        p.face = dir
        p.dashT = 0.2f
        p.invuln = max(p.invuln, 0.22f + dashIframe)
        p.vx = dir * 1500f; p.vy = 0f
        p.dashCd = 0.38f * dashCdMul
        sfx.play("dash", 0.6f)
        dust(p.x, p.y, 5)
    }

    fun switchWeapon(k: Int) {
        val p = player
        if (k == p.weapon) return
        p.weapon = k
        if (combo > 0) switchBonus = 1.2f
        val w = Weapons.all[k]
        addText(w.ru.uppercase(), p.x, p.cy - 100f, w.color, 24f, 0.6f)
        ring(p.x, p.cy, w.color)
        sfx.play("orb", 0.4f, 0.8f)
    }

    private fun activateRage() {
        if (rage < 100f || rageT > 0f) return
        rage = 0f; rageT = 10f
        shake += 14f; flashT = 1f
        sfx.play("rage", 1f)
        addText("SSS RAGE", player.x, player.cy - 110f, 0xFFFF3D5A.toInt(), 44f, 1.4f)
        for (i in 0 until 30) spark(player.x, player.cy, 0xFFFF3D5A.toInt(), 600f)
        // ударная волна ярости
        for (e in enemies) if (!e.dead && abs(e.x - player.x) < 260f) {
            e.vx = sign(e.x - player.x) * 700f; e.vy = -500f; e.air = true; e.stun = 0.8f
        }
    }

    // ---------------- попадания ----------------

    private fun hitRectHit(m: Move, e: Enemy): Boolean {
        val p = player
        val top = p.cy + m.hy
        val bot = top + m.hh
        val el = e.x - e.w / 2f; val er = e.x + e.w / 2f
        val et = e.y - e.h; val eb = e.y
        if (eb < top || et > bot) return false
        val l1: Float; val r1: Float
        if (p.face > 0) { l1 = p.x + m.hx; r1 = l1 + m.hw } else { r1 = p.x - m.hx; l1 = r1 - m.hw }
        if (er > l1 && el < r1) return true
        if (m.both) {
            val l2: Float; val r2: Float
            if (p.face > 0) { r2 = p.x - m.hx; l2 = r2 - m.hw } else { l2 = p.x + m.hx; r2 = l2 + m.hw }
            if (er > l2 && el < r2) return true
        }
        return false
    }

    private fun doHits(m: Move) {
        val p = player
        for (e in enemies) {
            if (e.dead) continue
            if (!hitRectHit(m, e)) continue
            val last = p.hitSet[e]
            if (last == null || p.mt - last >= m.interval * 0.95f) {
                p.hitSet[e] = p.mt
                val dir = if (m.both || m.fx == Fx.PULL) (if (e.x >= p.x) 1 else -1) else p.face
                hitEnemy(e, m, dir)
                if (p.move !== m) return
            }
        }
        // отражение пуль
        val top = p.cy + m.hy; val bot = top + m.hh
        for (pr in projs) {
            if (pr.friendly || pr.kind != 0 || pr.dead) continue
            val l: Float; val r: Float
            if (p.face > 0) { l = p.x + m.hx - 30f; r = l + m.hw + 60f } else { r = p.x - m.hx + 30f; l = r - m.hw - 60f }
            if (pr.x > l && pr.x < r && pr.y > top - 20f && pr.y < bot + 20f) {
                pr.friendly = true; pr.vx = -pr.vx * 1.6f; pr.dmg = 30f * dmgMul; pr.life = 2f; pr.face = if (pr.vx > 0) 1 else -1
                addStyle(40f, "reflect")
                addText("REFLECT!", pr.x, pr.y - 40f, 0xFFFFFFFF.toInt(), 28f, 0.8f)
                hitstop = max(hitstop, 0.07f)
                sfx.play("block", 0.8f, 1.4f)
            }
        }
    }

    fun hitEnemy(e: Enemy, m: Move, dir: Int, dmgBase: Float = m.dmg, styleScale: Float = player.varMul, fromProj: Boolean = false) {
        val p = player
        var dmg = dmgBase * dmgMul * (if (rageT > 0f) 1.5f else 1f)
        if (e.air) dmg *= (1f + airDmg)
        val isCrit = rnd.nextFloat() < crit
        if (isCrit) dmg *= 2f
        val col = if (fromProj) (0xFFFFFFFF.toInt()) else wColor()

        // щит
        if (e.type == EType.SHIELD && e.guardBroken <= 0f && e.face == -dir && e.stun <= 0f) {
            if (m.heavy || m.launch || m.slam) {
                e.guardBroken = 4.5f; e.guard = 0f
                addText("GUARD BREAK", e.x, e.y - e.h - 30f, 0xFF4DA3FF.toInt(), 28f, 0.9f)
                addStyle(30f, "gb")
                sfx.play("block", 1f, 0.7f)
                shake += 6f
            } else {
                e.guard -= dmg
                e.vx = dir * 120f
                e.flash = 0.06f
                sfx.play("block", 0.7f, 1f + rnd.nextFloat() * 0.3f)
                for (i in 0 until 5) spark(e.x - dir * e.w * 0.5f, e.cy, 0xFF4DA3FF.toInt(), 300f)
                hitstop = max(hitstop, 0.025f)
                if (e.guard <= 0f) {
                    e.guardBroken = 4.5f
                    addText("GUARD BREAK", e.x, e.y - e.h - 30f, 0xFF4DA3FF.toInt(), 28f, 0.9f)
                    addStyle(20f, "gb")
                } else {
                    addText("BLOCK", e.x, e.y - e.h - 20f, 0xFF9AA0B5.toInt(), 20f, 0.4f)
                }
                return
            }
        }

        e.hp -= dmg
        e.flash = 0.1f
        val armored = e.type == EType.BRUTE && e.armor > 0f && !(m.heavy || m.launch || m.slam)
        if (e.type == EType.BRUTE && e.armor > 0f) {
            e.armor -= if (armored) dmg else dmg * 2.5f
            if (e.armor <= 0f) {
                e.armorT = 4f; e.stun = max(e.stun, 1.2f); e.state = 0
                addText("ARMOR BREAK", e.x, e.y - e.h - 30f, 0xFFFF8A3D.toInt(), 30f, 1f)
                addStyle(35f, "ab")
                shake += 8f
            }
        }
        if (!armored && !(e.type == EType.BRUTE && e.armor > 0f)) {
            val heavyMul = if (e.type == EType.BRUTE) 0.45f else 1f
            e.vx = dir * m.kbx * heavyMul
            when {
                m.fx == Fx.HOOK -> {
                    e.x = (p.x + p.face * 80f).coerceIn(20f, ARENA_W - 20f)
                    e.vx = 0f
                    if (!p.onGround) { e.y = min(GROUND, p.y); e.vy = -150f; e.air = true }
                }
                m.launch && !e.air -> { e.vy = m.kby * (if (e.type == EType.BRUTE) 0.6f else 1f); e.air = true; e.y -= 2f }
                m.launch && e.air -> e.vy = min(m.kby * 0.6f, -520f)
                m.slam && e.air -> { e.vy = 1700f; e.slammed = true }
                e.air -> e.vy = min(e.vy, if (m.kby < -260f) m.kby else -260f)
                m.kby < -400f -> { e.vy = m.kby * heavyMul; e.air = true; e.y -= 2f }
                else -> if (e.type != EType.FLYER) e.vy = 0f else e.vy = m.kby * 0.5f
            }
            e.stun = max(e.stun, m.stun)
            e.state = 0
            if (e.type == EType.FLYER) e.air = true
        } else {
            e.stun = max(e.stun, 0.05f)
        }
        if (e.air) e.juggle++

        hitstop = max(hitstop, m.stop * (if (isCrit) 1.6f else 1f))
        shake += m.shake + (if (isCrit) 4f else 0f)
        combo++; comboT = 2.6f; comboPop = 1f; hitsTotal++
        if (combo > maxCombo) maxCombo = combo
        val per = m.style * styleScale / sqrt(m.hits.toFloat()) * (if (e.air) 1.25f else 1f)
        addStyle(per, null)
        if (switchBonus > 0f) {
            switchBonus = 0f
            addStyle(18f, "sw")
            addText("SWITCH!", p.x, p.cy - 120f, wColor(), 24f, 0.7f)
        }
        if (lifesteal > 0f || rageT > 0f) p.hp = min(p.maxHp, p.hp + lifesteal + (if (rageT > 0f) 0.4f else 0f))

        val hx = e.x - dir * e.w * 0.25f
        val hy = e.cy + rnd.nextFloat() * 30f - 15f
        for (i in 0 until (if (m.heavy) 12 else 7)) spark(hx, hy, if (i % 3 == 0) 0xFFFFFFFF.toInt() else col, if (m.heavy) 700f else 480f)
        blood(hx, hy, dir, if (m.heavy) 8 else 4)
        addText(
            if (isCrit) "${dmg.toInt()}!" else "${dmg.toInt()}",
            e.x + rnd.nextFloat() * 30f - 15f, e.y - e.h - 10f,
            if (isCrit) 0xFFFFD84D.toInt() else 0xFFFFFFFF.toInt(), if (isCrit) 30f else 20f, 0.6f
        )
        if (m.heavy || m.slam) {
            sfx.play("bighit", 0.85f, 0.9f + rnd.nextFloat() * 0.2f); buzz(18)
        } else {
            sfx.play("hit", 0.7f, 0.85f + rnd.nextFloat() * 0.4f + p.weapon * 0.05f)
        }

        if (!fromProj && !p.onGround && m.fx != Fx.DIVE && m.fx != Fx.BOUNCE && m.lungeY >= 0f) p.vy = min(p.vy, -60f)
        if (!fromProj && m.fx == Fx.BOUNCE && p.move === m) {
            p.vy = -820f; p.lastMove = m; p.lastEnd = gt; p.move = null; p.jumps = max(p.jumps, 1); p.airDash = true
            ring(p.x, p.y, col)
        }
        if (e.hp <= 0f) kill(e)
    }

    private fun kill(e: Enemy) {
        if (e.dead) return
        e.dead = true
        kills++
        val mul = 1f + rank * 0.5f
        score += (e.score * mul).toInt()
        addStyle(18f + (if (e.air) 14f else 0f) + (if (e.elite) 80f else 0f), null)
        if (e.air) addText("AIR KILL", e.x, e.y - e.h - 40f, 0xFF4DE8FF.toInt(), 22f, 0.7f)
        for (i in 0 until 26) spark(e.x, e.cy, if (i % 2 == 0) 0xFF1A0A10.toInt() else 0xFFB0103A.toInt(), 650f)
        blood(e.x, e.cy, if (e.x > player.x) 1 else -1, 14)
        for (i in 0 until 6) spark(e.x, e.cy, wColor(), 400f)
        sfx.play("kill", 0.8f, 0.8f + rnd.nextFloat() * 0.3f)
        rage = min(100f, rage + 3f * rageMul)
        val orbChance = (if (e.elite) 1f else 0.13f) * orbMul
        var orbs = if (rnd.nextFloat() < orbChance) 1 else 0
        if (e.elite) orbs = 4
        for (i in 0 until orbs) {
            val o = Particle()
            o.x = e.x; o.y = e.cy; o.vx = rnd.nextFloat() * 400f - 200f; o.vy = -300f - rnd.nextFloat() * 200f
            o.life = 6f; o.max = 6f; o.size = 9f; o.color = 0xFF4DFF9A.toInt(); o.kind = 2
            parts.add(o)
        }
        if (toSpawn == 0 && eliteLeft == 0 && enemies.count { !it.dead } == 0) {
            slowT = 0.9f
            addText("WAVE CLEAR", player.x, player.cy - 150f, 0xFFFFFFFF.toInt(), 46f, 1.6f)
        }
    }

    private fun hurtPlayer(dmgIn: Float, fromX: Float, e: Enemy?): Boolean {
        val p = player
        if (state != St.PLAY) return false
        if (p.invuln > 0f) {
            if (p.dashT > 0f && e != null && !e.dodged) {
                e.dodged = true
                addStyle(45f, "dodge")
                addText("JUST DODGE!", p.x, p.cy - 110f, 0xFF4DE8FF.toInt(), 30f, 0.9f)
                slowT = max(slowT, 0.45f)
                sfx.play("rank", 0.5f, 1.5f)
            }
            return false
        }
        val dmgScale = 1f + 0.06f * (wave - 1)
        val dmg = dmgIn * dmgScale * (if (rageT > 0f) 0.6f else 1f)
        p.hp -= dmg
        p.invuln = 1.0f; p.hurtT = 0.35f
        val m = p.move
        if (m != null) { p.lastMove = null; p.move = null }
        p.dashT = 0f
        val dir = if (p.x >= fromX) 1 else -1
        p.vx = dir * 450f; p.vy = -420f; p.onGround = false
        style = max(0f, style - (230f + rank * 60f))
        if (combo > 5) addText("COMBO LOST", p.x, p.cy - 120f, 0xFF9AA0B5.toInt(), 22f, 0.8f)
        combo = 0
        shake += 12f; flashT = 0.5f
        sfx.play("hurt", 0.9f)
        buzz(60)
        for (i in 0 until 10) spark(p.x, p.cy, 0xFFFF3D5A.toInt(), 400f)
        if (p.hp <= 0f) gameOver()
        return true
    }

    private fun gameOver() {
        player.hp = 0f
        state = St.OVER
        slowT = 0f
        if (wave > bestWave) { bestWave = wave; newRecord = true }
        if (score > bestScore) { bestScore = score; newRecord = true }
        if (maxCombo > bestCombo) bestCombo = maxCombo
        prefs.edit().putInt("bestWave", bestWave).putInt("bestScore", bestScore).putInt("bestCombo", bestCombo).apply()
        for (i in 0 until 40) spark(player.x, player.cy, 0xFFFF3D5A.toInt(), 700f)
    }

    private fun buzz(ms: Long) {
        try {
            val v = vib ?: return
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, 120))
            else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (e: Exception) {}
    }

    // ---------------- стиль ----------------

    fun addStyle(base: Float, key: String?) {
        var p = base * styleMul
        if (key != null) {
            val rep = recent.count { it == key }
            p /= (1f + rep * 0.6f)
            recent.addLast(key)
            if (recent.size > 8) recent.removeFirst()
        }
        style = min(STYLE_MAX, style + p)
        styleIdle = 0f
        if (rageT <= 0f) rage = min(100f, rage + p * 0.11f * rageMul)
    }

    private fun updateStyle(d: Float) {
        styleIdle += d
        val decay = (14f + rank * 13f) * decayMul * (if (styleIdle > 1.6f) 2.6f else 1f)
        style = max(0f, style - decay * d)
        var r = 0
        for (i in RANK_T.indices) if (style >= RANK_T[i]) r = i
        if (r > rank) {
            rankPop = 1f
            sfx.play("rank", 0.6f, 0.9f + r * 0.08f)
            if (r >= 5) shake += 5f
        }
        rank = r
        if (rank > bestRank) bestRank = rank
        comboT -= d
        if (comboT <= 0f) combo = 0
    }

    // ---------------- волны и враги ----------------

    private fun updateWave(d: Float) {
        when (waveState) {
            0 -> {
                waveT -= d
                if (waveT <= 0f) {
                    waveState = 1
                    while (eliteLeft > 0) { spawn(EType.BRUTE, true); eliteLeft-- }
                }
            }
            1 -> {
                spawnT -= d
                val alive = enemies.count { !it.dead }
                if (toSpawn > 0 && spawnT <= 0f && alive < maxAlive) {
                    spawn(pickType(), false)
                    toSpawn--
                    spawnT = max(0.22f, 0.9f - wave * 0.04f)
                }
                if (toSpawn == 0 && alive == 0) {
                    waveState = 2; waveT = 2.6f
                    val bonus = wave * 150
                    score += bonus
                    player.hp = min(player.maxHp, player.hp + 15f)
                    addText("+$bonus", player.x, player.cy - 100f, 0xFFFFD84D.toInt(), 30f, 1.4f)
                }
            }
            2 -> {
                waveT -= d
                if (waveT <= 0f) {
                    if (wave % 3 == 0) {
                        val pool = Perks.all.shuffled(rnd)
                        perkChoices = pool.take(3)
                        state = St.PERK
                        releaseAll()
                    } else startWave()
                }
            }
        }
        enemies.removeAll { it.dead }
    }

    private fun pickType(): EType {
        val w = wave
        val types = ArrayList<EType>(); val weights = ArrayList<Float>()
        fun add(t: EType, wt: Float) { types.add(t); weights.add(wt) }
        add(EType.GRUNT, 10f)
        if (w >= 2) add(EType.RUNNER, 6f)
        if (w >= 3) add(EType.SHIELD, 4f + w / 3f)
        if (w >= 4) add(EType.FLYER, 4f)
        if (w >= 5) add(EType.GUNNER, 4f)
        if (w >= 6) add(EType.BRUTE, 1f + w / 6f)
        var r = rnd.nextFloat() * weights.sum()
        for (i in types.indices) { r -= weights[i]; if (r <= 0f) return types[i] }
        return EType.GRUNT
    }

    private fun spawn(t: EType, elite: Boolean) {
        val side = if (rnd.nextBoolean()) -1 else 1
        val vl = camX; val vr = camX + viewW
        var x = if (side < 0) vl - 120f else vr + 120f
        if (x < 40f) x = vr + 120f
        if (x > ARENA_W - 40f) x = vl - 120f
        x = x.coerceIn(40f, ARENA_W - 40f)
        val e = Enemy(t, x, GROUND, elite)
        val hpMul = 1f + 0.09f * (wave - 1)
        when (t) {
            EType.GRUNT -> { e.hp = 45f; e.dmg = 10f; e.speed = 150f; e.range = 85f; e.windup = 0.55f; e.active = 0.16f; e.w = 44f; e.h = 96f; e.score = 100 }
            EType.RUNNER -> { e.hp = 28f; e.dmg = 8f; e.speed = 320f; e.range = 130f; e.windup = 0.38f; e.active = 0.22f; e.lunge = 650f; e.w = 36f; e.h = 84f; e.score = 120 }
            EType.SHIELD -> { e.hp = 60f; e.guardMax = 45f; e.dmg = 13f; e.speed = 105f; e.range = 95f; e.windup = 0.7f; e.active = 0.18f; e.w = 52f; e.h = 100f; e.score = 180 }
            EType.FLYER -> { e.hp = 30f; e.dmg = 9f; e.speed = 240f; e.range = 420f; e.windup = 0.55f; e.active = 0.55f; e.w = 54f; e.h = 44f; e.score = 150; e.y = GROUND - 330f; e.hoverY = GROUND - 250f - rnd.nextFloat() * 90f }
            EType.GUNNER -> { e.hp = 35f; e.dmg = 10f; e.speed = 140f; e.range = 520f; e.windup = 0.65f; e.active = 0.1f; e.w = 40f; e.h = 92f; e.score = 160 }
            EType.BRUTE -> { e.hp = 220f; e.armorMax = 60f; e.dmg = 24f; e.speed = 85f; e.range = 165f; e.windup = 0.9f; e.active = 0.22f; e.w = 80f; e.h = 140f; e.score = 500 }
        }
        e.hp *= hpMul
        e.armorMax *= hpMul
        if (elite) {
            e.hp *= 2.5f; e.armorMax *= 2f; e.dmg *= 1.3f; e.w *= 1.2f; e.h *= 1.2f; e.score *= 4; e.range *= 1.15f
            addText("ELITE", x.coerceIn(camX + 80f, camX + viewW - 80f), GROUND - 250f, 0xFFFFD84D.toInt(), 36f, 1.5f)
        }
        e.maxHp = e.hp
        e.guard = e.guardMax
        e.armor = e.armorMax
        e.atkCd = 0.8f + rnd.nextFloat()
        e.face = if (player.x > e.x) 1 else -1
        enemies.add(e)
    }

    private fun updateEnemies(d: Float) {
        val p = player
        for (e in enemies) {
            if (e.dead) continue
            e.anim += d
            e.flash -= d
            e.guardBroken -= d
            if (e.type == EType.BRUTE && e.armor <= 0f) {
                e.armorT -= d
                if (e.armorT <= 0f) e.armor = e.armorMax
            }
            if (e.type == EType.SHIELD && e.guardBroken <= 0f && e.guard <= 0f) e.guard = e.guardMax

            val hover = e.type == EType.FLYER && e.stun <= 0f
            if (!hover) e.vy += GRAV * (if (e.stun > 0f && e.y < GROUND - 1f) 0.55f else 1f) * d
            e.x += e.vx * d
            e.y += e.vy * d
            if (e.y >= GROUND) {
                e.y = GROUND
                if (e.slammed && e.vy > 500f) {
                    e.vy = -380f; e.slammed = false
                    e.hp -= 5f; shake += 5f; dust(e.x, GROUND, 8)
                    e.stun = max(e.stun, 0.5f)
                    if (e.hp <= 0f) { kill(e); continue }
                } else {
                    e.vy = 0f; e.air = false; e.juggle = 0; e.slammed = false
                }
            } else if (e.type != EType.FLYER && e.y < GROUND - 2f) {
                e.air = true
            }
            if (!e.air || e.type == EType.FLYER) {
                if (e.stun > 0f) e.vx *= max(0f, 1f - d * 7f)
            }
            if (e.x < 20f) { e.x = 20f; if (e.vx < -300f) { e.vx = -e.vx * 0.4f; shake += 3f } }
            if (e.x > ARENA_W - 20f) { e.x = ARENA_W - 20f; if (e.vx > 300f) { e.vx = -e.vx * 0.4f; shake += 3f } }

            if (e.stun > 0f) {
                e.stun -= d
                if (e.type == EType.FLYER && e.stun <= 0f) e.air = false
                continue
            }
            when (e.type) {
                EType.GUNNER -> aiGunner(e, d)
                EType.FLYER -> aiFlyer(e, d)
                else -> aiMelee(e, d)
            }
        }
        // раздвигаем врагов
        for (i in enemies.indices) {
            val a = enemies[i]
            if (a.dead) continue
            for (j in i + 1 until enemies.size) {
                val b = enemies[j]
                if (b.dead || a.type == EType.FLYER || b.type == EType.FLYER) continue
                val dx = b.x - a.x
                val minD = (a.w + b.w) * 0.4f
                if (abs(dx) < minD && abs(a.y - b.y) < 60f) {
                    val push = (minD - abs(dx)) * 0.5f * (if (dx >= 0f) 1f else -1f)
                    a.x -= push * min(1f, d * 10f); b.x += push * min(1f, d * 10f)
                }
            }
        }
        // контакт с игроком не наносит урон — только атаки
        if (p.hp <= 0f) return
    }

    private fun aiMelee(e: Enemy, d: Float) {
        val p = player
        val dx = p.x - e.x
        val dist = abs(dx)
        when (e.state) {
            0 -> {
                e.face = if (dx >= 0f) 1 else -1
                e.atkCd -= d
                if (dist > e.range * 0.8f) e.vx = e.face * e.speed else e.vx *= 0.8f
                if (e.air) e.vx = e.vx * 0.98f
                if (dist < e.range && e.atkCd <= 0f && abs(p.y - e.y) < 170f && !e.air) {
                    e.state = 1; e.st = e.windup; e.vx = 0f
                }
            }
            1 -> {
                e.st -= d; e.vx *= 0.8f
                if (e.st <= 0f) {
                    e.state = 2; e.st = e.active; e.hitPlayer = false; e.dodged = false
                    if (e.lunge > 0f) e.vx = e.face * e.lunge
                    if (e.type == EType.BRUTE) { shake += 8f; dust(e.x + e.face * 80f, GROUND, 10); sfx.play("bighit", 0.5f, 0.6f) }
                    else sfx.play("swoosh", 0.35f, 0.7f)
                }
            }
            2 -> {
                e.st -= d
                if (!e.hitPlayer && enemyAttackHits(e)) {
                    e.hitPlayer = true
                    hurtPlayer(e.dmg, e.x, e)
                }
                if (e.st <= 0f) { e.state = 3; e.st = if (e.type == EType.BRUTE) 0.7f else 0.45f; if (e.lunge > 0f) e.vx *= 0.3f }
            }
            3 -> {
                e.st -= d; e.vx *= 0.85f
                if (e.st <= 0f) { e.state = 0; e.atkCd = (0.5f + rnd.nextFloat() * 1.3f) * max(0.45f, 1f - wave * 0.03f) }
            }
        }
    }

    fun attackRect(e: Enemy, out: RectF) {
        val top = e.y - e.h
        if (e.type == EType.BRUTE) out.set(e.x - e.range, top, e.x + e.range, e.y)
        else if (e.face > 0) out.set(e.x, top, e.x + e.range + 10f, e.y)
        else out.set(e.x - e.range - 10f, top, e.x, e.y)
    }

    private val tmpR = RectF()
    private fun enemyAttackHits(e: Enemy): Boolean {
        val p = player
        attackRect(e, tmpR)
        return p.x + 18f > tmpR.left && p.x - 18f < tmpR.right && p.y > tmpR.top && p.y - 96f < tmpR.bottom
    }

    private fun aiGunner(e: Enemy, d: Float) {
        val p = player
        val dx = p.x - e.x
        val dist = abs(dx)
        e.face = if (dx >= 0f) 1 else -1
        when (e.state) {
            0 -> {
                e.atkCd -= d
                e.vx = when {
                    dist < 300f -> -e.face * e.speed
                    dist > 520f -> e.face * e.speed
                    else -> e.vx * 0.8f
                }
                if (e.atkCd <= 0f && dist < 750f) { e.state = 1; e.st = e.windup; e.vx = 0f }
            }
            1 -> {
                e.st -= d; e.vx *= 0.8f
                if (e.st <= 0f) {
                    projs.add(Proj(e.x + e.face * 30f, e.cy - 10f, e.face * 640f, 0f, 24f, 12f, e.dmg, false, 0, 3f, null, 0xFFFF5A3D.toInt()))
                    sfx.play("shot", 0.5f)
                    e.state = 3; e.st = 0.4f
                }
            }
            else -> {
                e.st -= d
                if (e.st <= 0f) { e.state = 0; e.atkCd = 1.6f + rnd.nextFloat() * 1.2f }
            }
        }
    }

    private fun aiFlyer(e: Enemy, d: Float) {
        val p = player
        val dx = p.x - e.x
        when (e.state) {
            0 -> {
                e.face = if (dx >= 0f) 1 else -1
                val tx = p.x - e.face * 170f
                val ty = e.hoverY + kotlin.math.sin(e.anim * 3f) * 22f
                e.vx = ((tx - e.x) * 2.2f).coerceIn(-e.speed, e.speed)
                e.vy = (ty - e.y) * 3f
                e.atkCd -= d
                if (e.atkCd <= 0f && abs(dx) < 420f) { e.state = 1; e.st = e.windup; e.vx = 0f; e.vy = 0f }
            }
            1 -> {
                e.st -= d
                e.vx = 0f; e.vy = -30f
                if (e.st <= 0f) {
                    e.state = 2; e.st = e.active; e.hitPlayer = false; e.dodged = false
                    val tx = p.x - e.x; val ty = p.cy - e.cy
                    val l = max(1f, sqrt(tx * tx + ty * ty))
                    e.vx = tx / l * 760f; e.vy = ty / l * 760f
                    sfx.play("swoosh", 0.4f, 1.4f)
                }
            }
            2 -> {
                e.st -= d
                if (e.y >= GROUND - 5f) e.vy = 0f
                if (!e.hitPlayer && abs(p.x - e.x) < e.w / 2f + 22f && abs(p.cy - e.cy) < e.h / 2f + 45f) {
                    e.hitPlayer = true
                    hurtPlayer(e.dmg, e.x, e)
                }
                if (e.st <= 0f) { e.state = 3; e.st = 0.7f }
            }
            else -> {
                e.st -= d
                e.vy = (e.hoverY - e.y) * 3f
                e.vx *= 0.9f
                if (e.st <= 0f) { e.state = 0; e.atkCd = 1.4f + rnd.nextFloat() * 1.4f }
            }
        }
    }

    // ---------------- снаряды и частицы ----------------

    private fun updateProjs(d: Float) {
        val p = player
        for (pr in projs) {
            if (pr.dead) continue
            pr.t += d; pr.life -= d
            when (pr.kind) {
                2 -> {
                    if (pr.t < 0.42f) {
                        pr.vx = pr.face * 1350f * (1f - pr.t / 0.5f)
                    } else {
                        if (!pr.returned) { pr.returned = true; pr.hitSet.clear() }
                        val dx = p.x - pr.x; val dy = p.cy - pr.y
                        val l = max(1f, sqrt(dx * dx + dy * dy))
                        pr.vx = dx / l * 1400f; pr.vy = dy / l * 1400f
                        if (l < 50f) pr.dead = true
                    }
                }
            }
            pr.x += pr.vx * d; pr.y += pr.vy * d
            if (pr.life <= 0f || pr.x < -100f || pr.x > ARENA_W + 100f) { pr.dead = true; continue }
            if (pr.friendly) {
                for (e in enemies) {
                    if (e.dead || pr.hitSet.contains(e)) continue
                    if (abs(e.x - pr.x) < e.w / 2f + pr.w / 2f && abs(e.cy - pr.y) < e.h / 2f + pr.h / 2f) {
                        pr.hitSet.add(e)
                        val mv = pr.move ?: REFLECT
                        hitEnemy(e, mv, if (pr.vx >= 0f) 1 else -1, pr.dmg, 1f, true)
                        if (pr.kind == 0) { pr.dead = true; break }
                    }
                }
            } else {
                if (abs(p.x - pr.x) < 20f + pr.w / 2f && abs(p.cy - pr.y) < 48f + pr.h / 2f) {
                    if (p.invuln > 0f) continue
                    hurtPlayer(pr.dmg, pr.x, null)
                    pr.dead = true
                }
            }
        }
        projs.removeAll { it.dead }
    }

    private fun updateParticles(d: Float) {
        val p = player
        val it = parts.iterator()
        while (it.hasNext()) {
            val q = it.next()
            q.life -= d
            if (q.kind == 2) {
                // сфера лечения
                if (q.life < q.max - 0.5f) {
                    val dx = p.x - q.x; val dy = p.cy - q.y
                    val l = max(1f, sqrt(dx * dx + dy * dy))
                    q.vx += dx / l * 3000f * d; q.vy += dy / l * 3000f * d
                    q.vx *= 0.94f; q.vy *= 0.94f
                    if (l < 40f) {
                        p.hp = min(p.maxHp, p.hp + 10f)
                        sfx.play("orb", 0.6f)
                        addText("+10", p.x, p.cy - 70f, 0xFF4DFF9A.toInt(), 22f, 0.6f)
                        it.remove(); continue
                    }
                } else {
                    q.vy += 900f * d
                }
            } else {
                q.vy += q.grav * d
                q.vx *= max(0f, 1f - d * 3f)
            }
            q.x += q.vx * d; q.y += q.vy * d
            if (q.kind != 2 && q.y > GROUND && q.grav > 0f) { q.y = GROUND; q.vy *= -0.3f; q.vx *= 0.6f }
            if (q.life <= 0f) it.remove()
        }
        for (sl in slashes) sl.life -= d
        slashes.removeAll { it.life <= 0f }
        for (t in texts) { t.life -= d; t.y -= 40f * d }
        texts.removeAll { it.life <= 0f }
        for (g in ghosts) g.life -= d
        ghosts.removeAll { it.life <= 0f }
        while (parts.size > 700) parts.removeAt(0)
    }

    fun spark(x: Float, y: Float, col: Int, speed: Float) {
        val q = Particle()
        val a = rnd.nextFloat() * 6.2832f
        val v = speed * (0.3f + rnd.nextFloat() * 0.7f)
        q.x = x; q.y = y; q.vx = kotlin.math.cos(a) * v; q.vy = kotlin.math.sin(a) * v
        q.life = 0.25f + rnd.nextFloat() * 0.3f; q.max = q.life
        q.size = 2f + rnd.nextFloat() * 3f; q.color = col; q.kind = 0; q.grav = 600f
        parts.add(q)
    }

    private fun blood(x: Float, y: Float, dir: Int, n: Int) {
        for (i in 0 until n) {
            val q = Particle()
            q.x = x; q.y = y
            q.vx = dir * (150f + rnd.nextFloat() * 450f); q.vy = -100f - rnd.nextFloat() * 400f
            q.life = 0.5f + rnd.nextFloat() * 0.5f; q.max = q.life
            q.size = 3f + rnd.nextFloat() * 4f; q.color = 0xFFB0103A.toInt(); q.kind = 1; q.grav = 1800f
            parts.add(q)
        }
    }

    fun dust(x: Float, y: Float, n: Int) {
        for (i in 0 until n) {
            val q = Particle()
            q.x = x + rnd.nextFloat() * 40f - 20f; q.y = y - 4f
            q.vx = (rnd.nextFloat() - 0.5f) * 500f; q.vy = -rnd.nextFloat() * 160f
            q.life = 0.4f + rnd.nextFloat() * 0.3f; q.max = q.life
            q.size = 6f + rnd.nextFloat() * 8f; q.color = 0xFF6A6478.toInt(); q.kind = 3; q.grav = 0f
            parts.add(q)
        }
    }

    private fun ring(x: Float, y: Float, col: Int) {
        slashes.add(SlashFx(x, y, 50f, 0f, 360f, col, 0.22f, 5, 1, 0f))
    }

    fun addText(t: String, x: Float, y: Float, col: Int, size: Float, life: Float) {
        texts.add(FloatText(t, x, y, col, size, life))
        if (texts.size > 40) texts.removeAt(0)
    }

    fun draw(c: Canvas) {
        render.draw(c)
    }

    @Suppress("unused")
    private fun expDecay(v: Float, k: Float, d: Float) = v * exp(-k * d)
}
