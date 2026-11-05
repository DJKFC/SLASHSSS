package com.djkfc.slashsss

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Renderer(private val g: Game) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sky = Paint()
    private val vignette = Paint()
    private val path = Path()
    private val r1 = RectF()
    private val r2 = RectF()
    private val fStyle: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
    private val fBold: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val rnd = Random(1)

    private class Bld(val x: Float, val w: Float, val h: Float, val win: Int)
    private val layers = Array(3) { ArrayList<Bld>() }
    private val layerF = floatArrayOf(0.2f, 0.45f, 0.7f)
    private val layerC = intArrayOf(0xFF2B2244.toInt(), 0xFF1F1832.toInt(), 0xFF150F22.toInt())
    private val layerH = floatArrayOf(330f, 260f, 190f)

    init {
        val r = Random(42)
        for (li in 0 until 3) {
            var x = -200f
            while (x < 5200f) {
                val w = 60f + r.nextFloat() * 140f
                val h = layerH[li] * (0.4f + r.nextFloat() * 0.8f)
                layers[li].add(Bld(x, w, h, r.nextInt()))
                x += w + r.nextFloat() * 40f
            }
        }
    }

    fun resize() {
        sky.shader = LinearGradient(0f, 0f, 0f, g.H, intArrayOf(0xFF07040D.toInt(), 0xFF1A0F2A.toInt(), 0xFF3A1730.toInt()), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        vignette.shader = RadialGradient(g.W / 2f, g.H / 2f, max(g.W, g.H) * 0.7f, intArrayOf(0x00000000, 0x00000000, 0xCCFF0030.toInt()), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
    }

    private fun alpha(c: Int, a: Float): Int = Color.argb((Color.alpha(c) * a.coerceIn(0f, 1f)).toInt(), Color.red(c), Color.green(c), Color.blue(c))

    private fun mix(a: Int, b: Int, k: Float): Int {
        val t = k.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * t).toInt(),
            (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        )
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int, align: Paint.Align = Paint.Align.CENTER, tf: Typeface = fStyle, outline: Float = 0.16f) {
        txt.typeface = tf
        txt.textSize = size
        txt.textAlign = align
        if (outline > 0f) {
            txt.style = Paint.Style.STROKE
            txt.strokeWidth = size * outline
            txt.strokeJoin = Paint.Join.ROUND
            txt.color = alpha(0xFF000000.toInt(), Color.alpha(col) / 255f)
            c.drawText(s, x, y, txt)
        }
        txt.style = Paint.Style.FILL
        txt.color = col
        c.drawText(s, x, y, txt)
    }

    private fun button(c: Canvas, id: String, cx: Float, cy: Float, w: Float, h: Float, label: String, col: Int, size: Float = 0f) {
        val r = RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        g.ui.add(UiBtn(id, r))
        fill.color = 0xCC140C1C.toInt()
        c.drawRoundRect(r, h * 0.2f, h * 0.2f, fill)
        stroke.color = col; stroke.strokeWidth = 3f * g.s
        c.drawRoundRect(r, h * 0.2f, h * 0.2f, stroke)
        val ts = if (size > 0f) size else h * 0.42f
        text(c, label, cx, cy + ts * 0.36f, ts, col, Paint.Align.CENTER, fStyle, 0f)
    }

    // ================================================================

    fun draw(c: Canvas) {
        g.ui.clear()
        for (r in g.weaponRects) r.setEmpty()
        c.drawColor(0xFF07040D.toInt())
        when (g.state) {
            Game.St.MENU -> { drawWorld(c, false); drawMenu(c) }
            Game.St.MOVES -> drawMoves(c)
            else -> {
                drawWorld(c, true)
                drawHud(c)
                when (g.state) {
                    Game.St.PLAY -> drawControls(c)
                    Game.St.PAUSE -> drawPause(c)
                    Game.St.PERK -> drawPerk(c)
                    Game.St.OVER -> drawOver(c)
                    else -> {}
                }
            }
        }
    }

    private fun drawWorld(c: Canvas, entities: Boolean) {
        val s = g.s
        c.drawRect(0f, 0f, g.W, g.H, sky)
        // оттенок ранга
        if (entities && g.rank > 0) {
            fill.color = alpha(Game.RANK_C[g.rank], 0.04f + g.rank * 0.025f)
            c.drawRect(0f, 0f, g.W, g.H, fill)
        }
        // луна
        val camX = if (entities) g.camX else (ARENA_W - g.viewW) / 2f + sin(g.time * 0.15f) * 400f
        val mx = g.W * 0.78f - camX * 0.03f * s; val my = g.H * 0.2f
        fill.color = 0x33FF4D80; c.drawCircle(mx, my, 95f * s, fill)
        fill.color = 0xFFF5D7E0.toInt(); c.drawCircle(mx, my, 60f * s, fill)
        fill.color = 0xFFE6B8C8.toInt(); c.drawCircle(mx - 18f * s, my - 10f * s, 12f * s, fill); c.drawCircle(mx + 15f * s, my + 18f * s, 8f * s, fill)

        c.save()
        val sh = g.shake
        val shx = if (sh > 0.2f) (rnd.nextFloat() - 0.5f) * sh else 0f
        val shy = if (sh > 0.2f) (rnd.nextFloat() - 0.5f) * sh else 0f
        c.scale(s, s)
        c.translate(-camX + shx, shy)

        // город
        for (li in 0 until 3) {
            val f = layerF[li]
            val off = camX * (1f - f)
            fill.color = layerC[li]
            for (b in layers[li]) {
                val bx = b.x + off
                if (bx + b.w < camX - 50f || bx > camX + g.viewW + 50f) continue
                c.drawRect(bx, GROUND - b.h, bx + b.w, GROUND + 10f, fill)
            }
            if (li == 1) {
                fill.color = alpha(Game.RANK_C[g.rank], 0.35f)
                for (b in layers[li]) {
                    val bx = b.x + off
                    if (bx + b.w < camX - 50f || bx > camX + g.viewW + 50f) continue
                    var k = 0
                    var wy = GROUND - b.h + 18f
                    while (wy < GROUND - 20f) {
                        var wx = bx + 10f
                        while (wx < bx + b.w - 14f) {
                            if (((b.win shr (k % 31)) and 1) == 1 && k % 3 != 0) c.drawRect(wx, wy, wx + 7f, wy + 10f, fill)
                            wx += 18f; k++
                        }
                        wy += 24f
                    }
                    fill.color = alpha(Game.RANK_C[g.rank], 0.35f)
                }
            }
        }
        // земля
        val gl = camX - 100f; val gr = camX + g.viewW + 100f
        fill.color = 0xFF0E0A14.toInt()
        c.drawRect(gl, GROUND, gr, VIEW_H + 200f, fill)
        stroke.color = alpha(Game.RANK_C[g.rank], 0.8f); stroke.strokeWidth = 3f
        c.drawLine(gl, GROUND, gr, GROUND, stroke)
        stroke.color = 0x22FFFFFF; stroke.strokeWidth = 2f
        var lx = (camX / 140f).toInt() * 140f - 140f
        while (lx < gr) {
            c.drawLine(lx, GROUND + 4f, lx + (lx - camX - g.viewW / 2f) * 0.35f, VIEW_H + 40f, stroke)
            lx += 140f
        }
        // стены арены
        fill.color = 0xFF050308.toInt()
        c.drawRect(-400f, 0f, 0f, VIEW_H + 200f, fill)
        c.drawRect(ARENA_W, 0f, ARENA_W + 400f, VIEW_H + 200f, fill)
        stroke.color = 0x66FF3D5A; stroke.strokeWidth = 4f
        c.drawLine(0f, 0f, 0f, GROUND, stroke); c.drawLine(ARENA_W, 0f, ARENA_W, GROUND, stroke)

        if (entities) {
            drawTelegraphs(c)
            for (gh in g.ghosts) drawGhost(c, gh)
            for (e in g.enemies) if (!e.dead) drawEnemy(c, e)
            for (pr in g.projs) drawProj(c, pr)
            if (g.state != Game.St.OVER || g.player.hp > 0f) drawPlayer(c)
            for (sl in g.slashes) drawSlash(c, sl)
            drawParticles(c)
            for (t in g.texts) {
                val k = t.life / t.max
                val sc = if (k > 0.85f) 1f + (k - 0.85f) * 3f else 1f
                text(c, t.text, t.x, t.y, t.size * sc, alpha(t.color, min(1f, k * 2.5f)))
            }
        }
        c.restore()
    }

    private fun drawTelegraphs(c: Canvas) {
        for (e in g.enemies) {
            if (e.dead || e.state != 1 || e.stun > 0f) continue
            if (e.type == EType.GUNNER || e.type == EType.FLYER) continue
            g.attackRect(e, r1)
            val pulse = 0.25f + 0.2f * sin(g.time * 30f)
            fill.color = alpha(0xFFFF2040.toInt(), pulse)
            c.drawRect(r1.left, GROUND - 8f, r1.right, GROUND + 6f, fill)
            fill.color = alpha(0xFFFF2040.toInt(), pulse * 0.35f)
            c.drawRect(r1, fill)
        }
    }

    private fun drawGhost(c: Canvas, gh: Ghost) {
        val k = gh.life / 0.25f
        fill.color = alpha(gh.color, k * 0.45f)
        c.drawRoundRect(gh.x - 12f, gh.y - 84f, gh.x + 12f, gh.y - 4f, 10f, 10f, fill)
        c.drawCircle(gh.x + gh.face * 3f, gh.y - 94f, 13f, fill)
    }

    // ---------------- игрок ----------------

    private fun drawPlayer(c: Canvas) {
        val p = g.player
        if (p.invuln > 0f && p.dashT <= 0f && ((g.time * 20f).toInt() % 2 == 0) && p.hurtT < 0.3f && g.state == Game.St.PLAY) return
        val w = Weapons.all[p.weapon]
        val wc = w.color
        val rage = g.rageT > 0f

        // шарф
        stroke.color = if (rage) 0xFFFF3D5A.toInt() else wc
        for (i in 1 until p.scarfX.size) {
            stroke.strokeWidth = 9f - i * 0.7f
            c.drawLine(p.scarfX[i - 1], p.scarfY[i - 1], p.scarfX[i], p.scarfY[i], stroke)
        }
        if (rage) {
            fill.color = alpha(0xFFFF3D5A.toInt(), 0.25f + 0.1f * sin(g.time * 12f))
            c.drawCircle(p.x, p.cy, 80f, fill)
        }

        c.save()
        c.translate(p.x, p.y)
        val sq = p.squash
        c.scale(p.face.toFloat() * (1f + sq * 0.15f), 1f - sq * 0.15f)

        val body = if (p.hurtT > 0f) 0xFFFFFFFF.toInt() else 0xFF0C0810.toInt()
        val m = p.move
        val running = p.onGround && abs(p.vx) > 40f && m == null
        val lean = if (m != null || running || p.dashT > 0f) 9f else 2f

        // ноги
        stroke.color = body; stroke.strokeWidth = 9f
        if (p.onGround) {
            val ph = p.anim
            val a1 = if (running) sin(ph) * 0.7f else 0.25f
            val a2 = if (running) -sin(ph) * 0.7f else -0.25f
            c.drawLine(0f, -42f, sin(a1) * 38f, -42f + cos(a1) * 42f, stroke)
            c.drawLine(0f, -42f, sin(a2) * 38f, -42f + cos(a2) * 42f, stroke)
        } else {
            c.drawLine(0f, -42f, -12f, -10f, stroke)
            c.drawLine(0f, -42f, 16f, -20f, stroke)
        }
        // торс
        stroke.strokeWidth = 16f
        c.drawLine(0f, -42f, lean, -78f, stroke)
        // плащ
        fill.color = 0xFF1A1020.toInt()
        path.reset()
        path.moveTo(lean - 6f, -78f)
        path.lineTo(-14f - abs(p.vx) * 0.02f, -30f + sin(g.time * 8f) * 3f)
        path.lineTo(2f, -36f)
        path.close()
        c.drawPath(path, fill)
        // голова
        fill.color = body
        c.drawCircle(lean + 3f, -94f, 13f, fill)
        fill.color = if (rage) 0xFFFF3D5A.toInt() else wc
        c.drawRect(lean + 6f, -98f, lean + 15f, -94f, fill)

        // оружие
        var ang: Float
        var reach = 0f
        if (m != null) {
            val k = ((p.mt - m.a0) / max(0.01f, m.a1 - m.a0)).coerceIn(0f, 1f)
            val e = 1f - (1f - k) * (1f - k)
            ang = when {
                p.mt < m.a0 -> m.arc0 - 25f * (p.mt / max(0.01f, m.a0))
                m.shape == 1 -> 0f
                m.shape == 2 -> m.arc0 + e * 720f
                m.shape == 4 -> (g.time * 2400f) % 360f
                else -> m.arc0 + (m.arc1 - m.arc0) * e
            }
            if (m.shape == 1 && m.hy + m.hh / 2f > 0f) ang = 30f
            if (m.fx == Fx.DIVE) ang = if (p.fired && !p.landed) 80f else ang
            reach = if (p.mt >= m.a0 && p.mt <= m.a1 + 0.05f) (m.hx + m.hw) * (if (m.shape == 1) min(1f, k * 3f) else 1f) else 0f
        } else if (!p.onGround) ang = -25f
        else if (running) ang = 55f
        else ang = 38f + sin(g.time * 2.5f) * 4f
        val ar = Math.toRadians(ang.toDouble())
        val ca = cos(ar).toFloat(); val sa = sin(ar).toFloat()
        val shx = lean + 2f; val shy = -76f
        val hx = shx + ca * 26f; val hy = shy + sa * 26f
        stroke.color = body; stroke.strokeWidth = 7f
        c.drawLine(shx, shy, hx, hy, stroke)
        drawWeapon(c, p.weapon, hx, hy, ca, sa, reach, wc, m != null && p.mt >= m.a0 && p.mt <= m.a1)
        c.restore()
    }

    private fun drawWeapon(c: Canvas, wi: Int, hx: Float, hy: Float, ca: Float, sa: Float, reach: Float, wc: Int, active: Boolean) {
        when (wi) {
            0 -> { // катана
                val l = 82f
                stroke.color = alpha(wc, if (active) 0.7f else 0.35f); stroke.strokeWidth = 9f
                c.drawLine(hx, hy, hx + ca * l, hy + sa * l, stroke)
                stroke.color = 0xFFEAF6FF.toInt(); stroke.strokeWidth = 3.5f
                c.drawLine(hx, hy, hx + ca * l, hy + sa * l, stroke)
                stroke.color = 0xFF2A2030.toInt(); stroke.strokeWidth = 6f
                c.drawLine(hx - ca * 12f, hy - sa * 12f, hx + ca * 2f, hy + sa * 2f, stroke)
                stroke.color = wc; stroke.strokeWidth = 4f
                c.drawLine(hx + ca * 3f - sa * 7f, hy + sa * 3f + ca * 7f, hx + ca * 3f + sa * 7f, hy + sa * 3f - ca * 7f, stroke)
            }
            1 -> { // двуручник
                val l = 104f
                path.reset()
                val nx = -sa; val ny = ca
                path.moveTo(hx + nx * 9f, hy + ny * 9f)
                path.lineTo(hx + ca * l + nx * 7f, hy + sa * l + ny * 7f)
                path.lineTo(hx + ca * (l + 16f), hy + sa * (l + 16f))
                path.lineTo(hx + ca * l - nx * 9f, hy + sa * l - ny * 9f)
                path.lineTo(hx - nx * 9f, hy - ny * 9f)
                path.close()
                fill.color = 0xFF9C948C.toInt(); c.drawPath(path, fill)
                stroke.color = alpha(wc, if (active) 1f else 0.6f); stroke.strokeWidth = 3f
                c.drawPath(path, stroke)
                stroke.color = 0xFF2A2030.toInt(); stroke.strokeWidth = 7f
                c.drawLine(hx - ca * 18f, hy - sa * 18f, hx, hy, stroke)
                stroke.color = wc; stroke.strokeWidth = 6f
                c.drawLine(hx - sa * 14f, hy + ca * 14f, hx + sa * 14f, hy - ca * 14f, stroke)
            }
            2 -> { // кастеты
                fill.color = alpha(wc, if (active) 0.5f else 0.2f)
                c.drawCircle(hx + ca * 6f, hy + sa * 6f, if (active) 20f else 14f, fill)
                fill.color = wc
                c.drawCircle(hx + ca * 6f, hy + sa * 6f, 10f, fill)
                fill.color = 0xFF2A2030.toInt()
                c.drawRect(hx + ca * 6f - 6f, hy + sa * 6f - 2f, hx + ca * 6f + 6f, hy + sa * 6f + 2f, fill)
                fill.color = alpha(wc, 0.8f)
                c.drawCircle(-4f, -60f, 8f, fill)
            }
            3 -> { // коса
                val l = 92f
                stroke.color = 0xFF3A2A40.toInt(); stroke.strokeWidth = 6f
                val bx = hx + ca * l; val by = hy + sa * l
                c.drawLine(hx - ca * 30f, hy - sa * 30f, bx, by, stroke)
                // лезвие
                val nx = -sa; val ny = ca
                path.reset()
                path.moveTo(bx, by)
                path.quadTo(bx + nx * 40f + ca * 20f, by + ny * 40f + sa * 20f, bx + nx * 72f - ca * 18f, by + ny * 72f - sa * 18f)
                path.quadTo(bx + nx * 36f + ca * 2f, by + ny * 36f + sa * 2f, bx - ca * 8f, by - sa * 8f)
                path.close()
                fill.color = 0xFFD8CCE6.toInt(); c.drawPath(path, fill)
                stroke.color = alpha(wc, if (active) 1f else 0.6f); stroke.strokeWidth = 3f
                c.drawPath(path, stroke)
            }
            else -> { // цепь
                val l = if (reach > 0f) reach else 46f
                val segs = (l / 13f).toInt().coerceAtLeast(3)
                val sag = if (reach > 0f) 0f else 14f
                stroke.color = 0xFF8A8296.toInt(); stroke.strokeWidth = 3f
                var px = hx; var py = hy
                for (i in 1..segs) {
                    val t = i.toFloat() / segs
                    val x = hx + ca * l * t
                    val y = hy + sa * l * t + sin(t * 3.14f) * sag + sin(g.time * 10f + t * 6f) * (if (reach > 0f) 3f else 1f)
                    c.drawLine(px, py, x, y, stroke)
                    px = x; py = y
                }
                fill.color = wc
                path.reset()
                path.moveTo(px + ca * 16f, py + sa * 16f)
                path.lineTo(px - sa * 8f, py + ca * 8f)
                path.lineTo(px + sa * 8f, py - ca * 8f)
                path.close()
                c.drawPath(path, fill)
                fill.color = alpha(wc, 0.5f)
                c.drawCircle(hx, hy, 6f, fill)
            }
        }
    }

    // ---------------- враги ----------------

    private fun drawEnemy(c: Canvas, e: Enemy) {
        val flash = e.flash > 0f
        val wind = e.state == 1 && e.stun <= 0f
        val base = when (e.type) {
            EType.GRUNT -> 0xFF3A2430.toInt()
            EType.RUNNER -> 0xFF40281C.toInt()
            EType.SHIELD -> 0xFF26304A.toInt()
            EType.FLYER -> 0xFF35203F.toInt()
            EType.GUNNER -> 0xFF2C3A2A.toInt()
            EType.BRUTE -> if (e.elite) 0xFF4A3A14.toInt() else 0xFF3A1C1C.toInt()
        }
        val body = if (flash) 0xFFFFFFFF.toInt() else if (wind && (g.time * 16f).toInt() % 2 == 0) mix(base, 0xFFFF2040.toInt(), 0.5f) else base
        val eye = if (e.elite) 0xFFFFD84D.toInt() else if (wind) 0xFFFFFFFF.toInt() else 0xFFFF2A40.toInt()

        c.save()
        c.translate(e.x, e.y)
        c.scale(e.face.toFloat(), 1f)
        val w = e.w; val h = e.h
        val bob = sin(e.anim * (if (abs(e.vx) > 30f) 12f else 3f)) * 3f
        when (e.type) {
            EType.FLYER -> {
                val fl = sin(e.anim * 18f) * 18f
                fill.color = body
                path.reset()
                path.moveTo(-6f, -h / 2f); path.lineTo(-w * 0.9f, -h / 2f - 10f - fl); path.lineTo(-w * 0.4f, -h / 2f + 8f); path.close()
                c.drawPath(path, fill)
                path.reset()
                path.moveTo(6f, -h / 2f); path.lineTo(w * 0.9f, -h / 2f - 10f - fl); path.lineTo(w * 0.4f, -h / 2f + 8f); path.close()
                c.drawPath(path, fill)
                r1.set(-w * 0.35f, -h, w * 0.35f, 0f)
                c.drawOval(r1, fill)
                fill.color = eye
                c.drawCircle(w * 0.15f, -h * 0.6f, 4f, fill)
                stroke.color = body; stroke.strokeWidth = 4f
                c.drawLine(w * 0.2f, -h * 0.25f, w * 0.45f, -h * 0.1f, stroke)
            }
            else -> {
                // ноги
                stroke.color = body; stroke.strokeWidth = w * 0.22f
                val ph = e.anim * 10f
                val mv = abs(e.vx) > 30f && !e.air
                val legH = h * 0.42f
                c.drawLine(-w * 0.15f, -legH, -w * 0.15f + (if (mv) sin(ph) * 14f else -4f), 0f, stroke)
                c.drawLine(w * 0.15f, -legH, w * 0.15f + (if (mv) -sin(ph) * 14f else 6f), 0f, stroke)
                // тело
                fill.color = body
                val lean = if (wind) -8f else if (e.state == 2) 10f else 0f
                r1.set(-w / 2f + lean, -h + 22f + bob, w / 2f + lean, -legH + 6f)
                c.drawRoundRect(r1, w * 0.3f, w * 0.3f, fill)
                if (e.type == EType.BRUTE) {
                    r2.set(-w * 0.75f + lean, -h + 26f + bob, w * 0.75f + lean, -h + 26f + h * 0.22f + bob)
                    c.drawRoundRect(r2, 14f, 14f, fill)
                    // рога
                    path.reset()
                    path.moveTo(lean - 14f, -h + 14f); path.lineTo(lean - 28f, -h - 18f); path.lineTo(lean - 4f, -h + 8f); path.close()
                    c.drawPath(path, fill)
                    path.reset()
                    path.moveTo(lean + 14f, -h + 14f); path.lineTo(lean + 32f, -h - 18f); path.lineTo(lean + 4f, -h + 8f); path.close()
                    c.drawPath(path, fill)
                    if (e.elite) {
                        fill.color = 0xFFFFD84D.toInt()
                        path.reset()
                        path.moveTo(lean - 16f, -h + 4f); path.lineTo(lean - 16f, -h - 14f); path.lineTo(lean - 6f, -h - 4f)
                        path.lineTo(lean, -h - 18f); path.lineTo(lean + 6f, -h - 4f); path.lineTo(lean + 16f, -h - 14f); path.lineTo(lean + 16f, -h + 4f); path.close()
                        c.drawPath(path, fill)
                    }
                    if (e.armor > 0f) {
                        stroke.color = alpha(0xFFFF8A3D.toInt(), 0.5f + 0.2f * sin(g.time * 6f)); stroke.strokeWidth = 3f
                        r2.set(-w * 0.8f, -h - 6f, w * 0.8f, 4f)
                        c.drawRoundRect(r2, 30f, 30f, stroke)
                    }
                }
                // голова
                fill.color = body
                c.drawCircle(lean + 4f, -h + 12f + bob, w * (if (e.type == EType.BRUTE) 0.26f else 0.32f), fill)
                fill.color = eye
                c.drawRect(lean + 6f, -h + 8f + bob, lean + 16f, -h + 13f + bob, fill)
                // оружие врага
                val armA = when (e.state) { 1 -> -2.2f; 2 -> 0.4f; else -> 0.9f }
                val ax = lean + w * 0.3f; val ay = -h * 0.65f + bob
                stroke.color = body; stroke.strokeWidth = 6f
                val ex = ax + cos(armA) * 26f; val ey = ay + sin(armA) * 26f
                c.drawLine(ax, ay, ex, ey, stroke)
                when (e.type) {
                    EType.GUNNER -> {
                        stroke.color = 0xFF555F55.toInt(); stroke.strokeWidth = 8f
                        c.drawLine(ax, ay, ax + 46f, ay, stroke)
                        if (e.state == 1) { fill.color = alpha(0xFFFF5A3D.toInt(), 0.6f + 0.4f * sin(g.time * 30f)); c.drawCircle(ax + 50f, ay, 7f, fill) }
                    }
                    EType.SHIELD -> {
                        val broken = e.guardBroken > 0f
                        if (!broken) {
                            fill.color = if (flash) 0xFFFFFFFF.toInt() else 0xFF3E5A8A.toInt()
                            r2.set(w * 0.35f, -h * 0.85f, w * 0.35f + 16f, -h * 0.2f)
                            c.drawRoundRect(r2, 6f, 6f, fill)
                            stroke.color = 0xFF8AB4FF.toInt(); stroke.strokeWidth = 2.5f
                            c.drawRoundRect(r2, 6f, 6f, stroke)
                        }
                        stroke.color = 0xFF9A9AA8.toInt(); stroke.strokeWidth = 4f
                        c.drawLine(ex, ey, ex + cos(armA) * 40f, ey + sin(armA) * 40f, stroke)
                    }
                    EType.BRUTE -> {
                        stroke.color = 0xFF5A4A4A.toInt(); stroke.strokeWidth = 12f
                        c.drawLine(ex, ey, ex + cos(armA) * 70f, ey + sin(armA) * 70f, stroke)
                        fill.color = 0xFF6A5858.toInt()
                        c.drawCircle(ex + cos(armA) * 76f, ey + sin(armA) * 76f, 18f, fill)
                    }
                    else -> {
                        stroke.color = 0xFF9A9AA8.toInt(); stroke.strokeWidth = if (e.type == EType.RUNNER) 3f else 5f
                        c.drawLine(ex, ey, ex + cos(armA) * 42f, ey + sin(armA) * 42f, stroke)
                    }
                }
            }
        }
        c.restore()
        if (wind) text(c, "!", e.x, e.y - e.h - 22f, 34f, 0xFFFF3D5A.toInt())
        if (e.stun > 0f && e.air) {
            stroke.color = 0x88FFFFFF.toInt(); stroke.strokeWidth = 2f
            for (i in 0 until 3) {
                val a = g.time * 6f + i * 2.1f
                c.drawCircle(e.x + cos(a) * 22f, e.y - e.h - 8f + sin(a) * 6f, 3f, stroke)
            }
        }
        if (e.hp < e.maxHp) {
            val bw = max(50f, e.w * 1.2f)
            val top = e.y - e.h - 12f
            fill.color = 0xAA000000.toInt()
            c.drawRect(e.x - bw / 2f, top, e.x + bw / 2f, top + 6f, fill)
            fill.color = if (e.elite) 0xFFFFD84D.toInt() else 0xFFFF3D5A.toInt()
            c.drawRect(e.x - bw / 2f, top, e.x - bw / 2f + bw * (e.hp / e.maxHp).coerceIn(0f, 1f), top + 6f, fill)
            if (e.guardMax > 0f && e.guardBroken <= 0f) {
                fill.color = 0xFF4DA3FF.toInt()
                c.drawRect(e.x - bw / 2f, top + 7f, e.x - bw / 2f + bw * (e.guard / e.guardMax).coerceIn(0f, 1f), top + 10f, fill)
            }
        }
    }

    private fun drawProj(c: Canvas, pr: Proj) {
        when (pr.kind) {
            0 -> {
                val col = if (pr.friendly) 0xFFFFFFFF.toInt() else pr.color
                fill.color = alpha(col, 0.35f)
                r1.set(pr.x - pr.w, pr.y - pr.h, pr.x + pr.w, pr.y + pr.h)
                c.drawOval(r1, fill)
                fill.color = col
                r1.set(pr.x - pr.w / 2f, pr.y - pr.h / 2f, pr.x + pr.w / 2f, pr.y + pr.h / 2f)
                c.drawOval(r1, fill)
            }
            1 -> {
                val k = min(1f, pr.life * 2f)
                fill.color = alpha(pr.color, 0.55f * k)
                path.reset()
                val f = pr.face.toFloat()
                path.moveTo(pr.x - f * 20f, GROUND)
                path.quadTo(pr.x + f * 50f, GROUND - pr.h * 0.7f, pr.x - f * 10f, GROUND - pr.h * 1.3f)
                path.quadTo(pr.x + f * 20f, GROUND - pr.h * 0.6f, pr.x - f * 40f, GROUND)
                path.close()
                c.drawPath(path, fill)
                stroke.color = alpha(0xFFFFFFFF.toInt(), k); stroke.strokeWidth = 3f
                c.drawPath(path, stroke)
            }
            else -> {
                c.save()
                c.translate(pr.x, pr.y)
                c.rotate(pr.t * 1400f)
                stroke.color = alpha(pr.color, 0.5f); stroke.strokeWidth = 12f
                r1.set(-30f, -30f, 30f, 30f)
                c.drawArc(r1, 0f, 220f, false, stroke)
                stroke.color = 0xFFE8DDF4.toInt(); stroke.strokeWidth = 5f
                c.drawArc(r1, 0f, 200f, false, stroke)
                c.restore()
            }
        }
    }

    private fun drawSlash(c: Canvas, sl: SlashFx) {
        val k = (sl.life / sl.max).coerceIn(0f, 1f)
        when (sl.shape) {
            0 -> {
                val start: Float; val sweep: Float
                if (sl.face > 0) { start = sl.a0; sweep = sl.sweep } else { start = 180f - sl.a0; sweep = -sl.sweep }
                for (i in 0 until 3) {
                    val rr = sl.r - i * 10f
                    r1.set(sl.x - rr, sl.y - rr, sl.x + rr, sl.y + rr)
                    stroke.color = alpha(sl.color, k * (0.75f - i * 0.2f))
                    stroke.strokeWidth = (20f - i * 5f) * k + 2f
                    c.drawArc(r1, start, sweep, false, stroke)
                }
                r1.set(sl.x - sl.r, sl.y - sl.r, sl.x + sl.r, sl.y + sl.r)
                stroke.color = alpha(0xFFFFFFFF.toInt(), k); stroke.strokeWidth = 3f * k + 1f
                c.drawArc(r1, start + sweep * 0.15f, sweep * 0.8f, false, stroke)
            }
            1 -> {
                val x2 = sl.x + sl.face * sl.len
                stroke.color = alpha(sl.color, k * 0.7f); stroke.strokeWidth = 22f * k + 2f
                c.drawLine(sl.x, sl.y, x2, sl.y, stroke)
                stroke.color = alpha(0xFFFFFFFF.toInt(), k); stroke.strokeWidth = 4f * k + 1f
                c.drawLine(sl.x, sl.y, x2 + sl.face * 20f, sl.y, stroke)
                stroke.color = alpha(sl.color, k * 0.5f); stroke.strokeWidth = 2f
                c.drawLine(sl.x, sl.y - 16f, x2 - sl.face * 30f, sl.y - 16f, stroke)
                c.drawLine(sl.x, sl.y + 16f, x2 - sl.face * 40f, sl.y + 16f, stroke)
            }
            2 -> {
                val rot = g.time * 900f
                r1.set(sl.x - sl.r, sl.y - sl.r, sl.x + sl.r, sl.y + sl.r)
                for (i in 0 until 3) {
                    stroke.color = alpha(sl.color, k * 0.7f); stroke.strokeWidth = 14f * k + 3f
                    c.drawArc(r1, rot + i * 120f, 80f, false, stroke)
                    stroke.color = alpha(0xFFFFFFFF.toInt(), k * 0.9f); stroke.strokeWidth = 2.5f
                    c.drawArc(r1, rot + i * 120f + 10f, 60f, false, stroke)
                }
                r2.set(sl.x - sl.r * 0.7f, sl.y - sl.r * 0.7f, sl.x + sl.r * 0.7f, sl.y + sl.r * 0.7f)
                stroke.color = alpha(sl.color, k * 0.4f); stroke.strokeWidth = 6f
                c.drawArc(r2, -rot * 1.3f, 140f, false, stroke)
            }
            3 -> {
                val rr = sl.r * (1.3f - k * 0.7f)
                r1.set(sl.x - rr, sl.y - rr * 0.16f, sl.x + rr, sl.y + rr * 0.16f)
                stroke.color = alpha(sl.color, k); stroke.strokeWidth = 8f * k + 2f
                c.drawOval(r1, stroke)
                stroke.color = alpha(0xFFFFFFFF.toInt(), k * 0.8f); stroke.strokeWidth = 3f
                for (i in -3..3) {
                    val x = sl.x + i * rr * 0.28f
                    val hh = (40f + (3 - abs(i)) * 25f) * k
                    c.drawLine(x, sl.y, x + i * 4f, sl.y - hh, stroke)
                }
            }
            else -> {
                val rr = sl.r * (1.6f - k)
                stroke.color = alpha(sl.color, k); stroke.strokeWidth = 5f * k + 1f
                c.drawCircle(sl.x, sl.y, rr, stroke)
            }
        }
    }

    private fun drawParticles(c: Canvas) {
        for (q in g.parts) {
            val k = (q.life / q.max).coerceIn(0f, 1f)
            when (q.kind) {
                0 -> {
                    stroke.color = alpha(q.color, k); stroke.strokeWidth = q.size
                    c.drawLine(q.x, q.y, q.x - q.vx * 0.03f, q.y - q.vy * 0.03f, stroke)
                }
                1 -> { fill.color = alpha(q.color, k); c.drawCircle(q.x, q.y, q.size * (0.5f + k * 0.5f), fill) }
                2 -> {
                    fill.color = alpha(q.color, 0.3f); c.drawCircle(q.x, q.y, q.size * 2.2f, fill)
                    fill.color = q.color; c.drawCircle(q.x, q.y, q.size, fill)
                    fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(q.x - 2f, q.y - 2f, q.size * 0.35f, fill)
                }
                else -> { fill.color = alpha(q.color, k * 0.4f); c.drawCircle(q.x, q.y, q.size * (1.6f - k * 0.6f), fill) }
            }
        }
    }

    // ---------------- HUD ----------------

    private fun drawHud(c: Canvas) {
        val s = g.s
        val p = g.player
        // вспышки
        if (g.flashT > 0f) {
            fill.color = alpha(if (g.rageT > 0f && p.hurtT <= 0f) 0xFFFF3D5A.toInt() else 0xFFFF0030.toInt(), g.flashT * 0.35f)
            c.drawRect(0f, 0f, g.W, g.H, fill)
        }
        if (g.rageT > 0f || p.hp < p.maxHp * 0.25f) {
            vignette.alpha = if (g.rageT > 0f) (150 + 60 * sin(g.time * 8f)).toInt() else (90 + 80 * sin(g.time * 5f)).toInt()
            c.drawRect(0f, 0f, g.W, g.H, vignette)
        }

        // HP
        val x0 = 22f * s; val y0 = 20f * s; val bw = 320f * s
        fill.color = 0xAA000000.toInt()
        c.drawRoundRect(x0 - 4f * s, y0 - 4f * s, x0 + bw + 4f * s, y0 + 42f * s, 6f * s, 6f * s, fill)
        fill.color = 0xFF3A0A14.toInt()
        c.drawRect(x0, y0, x0 + bw, y0 + 20f * s, fill)
        fill.color = if (p.hp < p.maxHp * 0.3f) mix(0xFFFF3D5A.toInt(), 0xFFFFFFFF.toInt(), 0.5f + 0.5f * sin(g.time * 10f)) else 0xFFFF3D5A.toInt()
        c.drawRect(x0, y0, x0 + bw * (p.hp / p.maxHp).coerceIn(0f, 1f), y0 + 20f * s, fill)
        text(c, "${p.hp.toInt()} / ${p.maxHp.toInt()}", x0 + 8f * s, y0 + 16f * s, 15f * s, 0xFFFFFFFF.toInt(), Paint.Align.LEFT, fBold, 0.18f)
        // ярость
        val ry = y0 + 26f * s
        fill.color = 0xFF2A0A20.toInt()
        c.drawRect(x0, ry, x0 + bw, ry + 12f * s, fill)
        val rk = if (g.rageT > 0f) g.rageT / 10f else g.rage / 100f
        fill.color = if (g.rageT > 0f) 0xFFFFFFFF.toInt() else if (g.rage >= 100f) mix(0xFFFF4DE1.toInt(), 0xFFFFFFFF.toInt(), 0.5f + 0.5f * sin(g.time * 10f)) else 0xFFFF4DE1.toInt()
        c.drawRect(x0, ry, x0 + bw * rk.coerceIn(0f, 1f), ry + 12f * s, fill)
        if (g.rage >= 100f && g.rageT <= 0f) text(c, "ЯРОСТЬ ГОТОВА!", x0 + bw + 14f * s, ry + 12f * s, 18f * s, 0xFFFF4DE1.toInt(), Paint.Align.LEFT)

        // волна / счёт
        val cx = g.W / 2f
        val left = g.toSpawn + g.enemies.count { !it.dead }
        text(c, "ВОЛНА ${g.wave}", cx, 40f * s, 30f * s, 0xFFFFFFFF.toInt())
        text(c, "врагов: $left   очки: ${g.score}", cx, 64f * s, 16f * s, 0xFFB8B0C8.toInt(), Paint.Align.CENTER, fBold, 0.2f)

        // оружие
        val iw = 58f * s; val gap = 8f * s
        val total = iw * 5 + gap * 4
        var ix = cx - total / 2f
        val iy = 78f * s
        for (i in 0 until 5) {
            val w = Weapons.all[i]
            val r = g.weaponRects[i]
            r.set(ix, iy, ix + iw, iy + 42f * s)
            val sel = i == p.weapon
            fill.color = if (sel) alpha(w.color, 0.35f) else 0x99000000.toInt()
            c.drawRoundRect(r, 8f * s, 8f * s, fill)
            stroke.color = if (sel) w.color else alpha(w.color, 0.4f); stroke.strokeWidth = (if (sel) 3f else 1.5f) * s
            c.drawRoundRect(r, 8f * s, 8f * s, stroke)
            text(c, w.ru.take(4).uppercase(), ix + iw / 2f, iy + 27f * s, 15f * s, if (sel) 0xFFFFFFFF.toInt() else alpha(w.color, 0.8f), Paint.Align.CENTER, fBold, 0f)
            ix += iw + gap
        }

        // стиль
        if (g.style > 4f) {
            val rk2 = g.rank
            val col = if (rk2 == 6) Color.HSVToColor(floatArrayOf((g.time * 240f) % 360f, 0.55f, 1f)) else Game.RANK_C[rk2]
            val bx = g.W - 190f * s; val by = 175f * s
            val pop = 1f + g.rankPop * 0.6f
            c.save()
            c.translate(bx, by)
            c.rotate(-10f)
            c.scale(pop, pop)
            val big = 120f * s
            if (rk2 >= 5) {
                val j = 4f * s
                text(c, Game.RANK_L[rk2], -j + (rnd.nextFloat() - 0.5f) * j, (rnd.nextFloat() - 0.5f) * j, big, alpha(0xFF4DE8FF.toInt(), 0.6f), Paint.Align.CENTER, fStyle, 0f)
                text(c, Game.RANK_L[rk2], j, 0f, big, alpha(0xFFFF3D5A.toInt(), 0.6f), Paint.Align.CENTER, fStyle, 0f)
            }
            text(c, Game.RANK_L[rk2], 0f, 0f, big, col, Paint.Align.CENTER, fStyle, 0.08f)
            text(c, Game.RANK_N[rk2], 0f, 34f * s, 26f * s, col)
            // полоска до следующего ранга
            val lo = Game.RANK_T[rk2]
            val hi = if (rk2 < 6) Game.RANK_T[rk2 + 1] else Game.STYLE_MAX
            val prog = ((g.style - lo) / max(1f, hi - lo)).coerceIn(0f, 1f)
            fill.color = 0x88000000.toInt()
            c.drawRect(-80f * s, 46f * s, 80f * s, 54f * s, fill)
            fill.color = col
            c.drawRect(-80f * s, 46f * s, -80f * s + 160f * s * prog, 54f * s, fill)
            c.restore()
        }
        // комбо
        if (g.combo >= 2) {
            val sc = 1f + g.comboPop * 0.35f
            val col = when {
                g.combo >= 100 -> 0xFFFF4DE1.toInt()
                g.combo >= 50 -> 0xFFFF3D5A.toInt()
                g.combo >= 20 -> 0xFFFFD84D.toInt()
                else -> 0xFFFFFFFF.toInt()
            }
            val y = 300f * s
            text(c, "${g.combo}", g.W - 120f * s, y, 64f * s * sc, col, Paint.Align.RIGHT)
            text(c, "HITS", g.W - 112f * s, y, 24f * s, col, Paint.Align.LEFT)
            fill.color = alpha(col, 0.7f)
            c.drawRect(g.W - 260f * s, y + 10f * s, g.W - 260f * s + 200f * s * (g.comboT / 2.6f).coerceIn(0f, 1f), y + 14f * s, fill)
        }
        // название приёма
        if (g.moveNameT > 0f && g.state == Game.St.PLAY) {
            val k = min(1f, g.moveNameT * 2f)
            text(c, g.moveName, g.W * 0.36f, g.H - 30f * s, 26f * s, alpha(g.wColor(), k))
        }
        // элитный враг
        val elite = g.enemies.firstOrNull { it.elite && !it.dead }
        if (elite != null) {
            val ew = g.W * 0.4f; val ex = g.W / 2f - ew / 2f; val ey = 136f * s
            text(c, "ЭЛИТНЫЙ ГРОМИЛА", g.W / 2f, ey - 6f * s, 18f * s, 0xFFFFD84D.toInt())
            fill.color = 0xAA000000.toInt(); c.drawRect(ex, ey, ex + ew, ey + 12f * s, fill)
            fill.color = 0xFFFFD84D.toInt(); c.drawRect(ex, ey, ex + ew * (elite.hp / elite.maxHp).coerceIn(0f, 1f), ey + 12f * s, fill)
        }
        // заставка волны
        if (g.waveState == 0 && g.state == Game.St.PLAY) {
            val k = (g.waveT / 2.2f).coerceIn(0f, 1f)
            val a = if (k > 0.85f) (1f - k) / 0.15f else min(1f, k * 3f)
            val sc = 1f + (1f - k) * 0.15f
            fill.color = alpha(0xFF000000.toInt(), a * 0.5f)
            c.drawRect(0f, g.H * 0.4f, g.W, g.H * 0.6f, fill)
            text(c, "ВОЛНА ${g.wave}", g.W / 2f, g.H * 0.52f, 80f * s * sc, alpha(0xFFFFFFFF.toInt(), a))
            if (g.wave % 5 == 0) text(c, "ЭЛИТА!", g.W / 2f, g.H * 0.59f, 30f * s, alpha(0xFFFFD84D.toInt(), a))
        }
    }

    private fun drawControls(c: Canvas) {
        val s = g.s
        // джойстик
        if (g.stickPid != -1) {
            fill.color = 0x22FFFFFF
            c.drawCircle(g.stickOx, g.stickOy, 85f * s, fill)
            stroke.color = 0x55FFFFFF; stroke.strokeWidth = 2f * s
            c.drawCircle(g.stickOx, g.stickOy, 85f * s, stroke)
            fill.color = 0x88FFFFFF.toInt()
            c.drawCircle(g.stickOx + g.sx * 85f * s, g.stickOy + g.sy * 85f * s, 36f * s, fill)
        } else {
            stroke.color = 0x33FFFFFF; stroke.strokeWidth = 2f * s
            c.drawCircle(170f * s, g.H - 170f * s, 85f * s, stroke)
            text(c, "двигай", 170f * s, g.H - 165f * s, 18f * s, 0x55FFFFFF, Paint.Align.CENTER, fBold, 0f)
        }
        for (b in g.btns) {
            if (b.id == Game.B_PAUSE) {
                fill.color = 0x55FFFFFF
                c.drawRect(b.x - 10f * s, b.y - 14f * s, b.x - 3f * s, b.y + 14f * s, fill)
                c.drawRect(b.x + 3f * s, b.y - 14f * s, b.x + 10f * s, b.y + 14f * s, fill)
                continue
            }
            var col = 0xFFFFFFFF.toInt()
            if (b.id == Game.B_ATK) col = g.wColor()
            if (b.id == Game.B_HVY) col = mix(g.wColor(), 0xFFFFFFFF.toInt(), 0.4f)
            if (b.id == Game.B_SW) col = Weapons.all[(g.player.weapon + 1) % 5].color
            var a = if (b.held) 0.45f else 0.18f
            if (b.id == Game.B_RAGE) {
                col = 0xFFFF4DE1.toInt()
                a = if (g.rage >= 100f) 0.35f + 0.25f * sin(g.time * 10f) else 0.08f
            }
            val rr = b.r * (1f + b.press * 0.12f)
            fill.color = alpha(col, a)
            c.drawCircle(b.x, b.y, rr, fill)
            stroke.color = alpha(col, if (b.id == Game.B_RAGE && g.rage < 100f) 0.3f else 0.75f); stroke.strokeWidth = 3f * s
            c.drawCircle(b.x, b.y, rr, stroke)
            if (b.id == Game.B_RAGE && g.rage < 100f && g.rageT <= 0f) {
                r1.set(b.x - rr, b.y - rr, b.x + rr, b.y + rr)
                stroke.color = col; stroke.strokeWidth = 5f * s
                c.drawArc(r1, -90f, 360f * g.rage / 100f, false, stroke)
            }
            val ts = if (b.label.length > 4) b.r * 0.32f else b.r * 0.42f
            text(c, if (b.id == Game.B_SW) Weapons.all[(g.player.weapon + 1) % 5].ru.take(4).uppercase() else b.label,
                b.x, b.y + ts * 0.36f, ts, alpha(0xFFFFFFFF.toInt(), 0.85f), Paint.Align.CENTER, fBold, 0f)
        }
    }

    // ---------------- экраны ----------------

    private fun dim(c: Canvas, col: Int = 0xCC07040D.toInt()) {
        fill.color = col
        c.drawRect(0f, 0f, g.W, g.H, fill)
    }

    private fun title(c: Canvas, y: Float, size: Float) {
        val s = g.s
        val j = 5f * s
        val tx = g.W / 2f
        text(c, "SLASHSSS", tx - j, y, size, alpha(0xFF4DE8FF.toInt(), 0.7f), Paint.Align.CENTER, fStyle, 0f)
        text(c, "SLASHSSS", tx + j, y + (rnd.nextFloat() - 0.5f) * 3f * s, size, alpha(0xFFFF3D5A.toInt(), 0.7f), Paint.Align.CENTER, fStyle, 0f)
        text(c, "SLASHSSS", tx, y, size, 0xFFFFFFFF.toInt(), Paint.Align.CENTER, fStyle, 0.05f)
        stroke.color = 0xFFFF3D5A.toInt(); stroke.strokeWidth = 6f * s
        val k = (g.time * 0.7f) % 1f
        val sw = g.W * 0.5f
        c.drawLine(tx - sw / 2f, y - size * 0.15f + 30f * s, tx - sw / 2f + sw * min(1f, k * 3f), y - size * 0.15f - 30f * s * min(1f, k * 3f) + 30f * s, stroke)
    }

    private fun drawMenu(c: Canvas) {
        val s = g.s
        dim(c, 0x88000000.toInt())
        title(c, g.H * 0.32f, 140f * s)
        text(c, "бесконечный 2D-слэшер • 5 оружий • 80 приёмов", g.W / 2f, g.H * 0.32f + 46f * s, 22f * s, 0xFFB8B0C8.toInt(), Paint.Align.CENTER, fBold, 0f)
        button(c, "play", g.W / 2f, g.H * 0.56f, 320f * s, 74f * s, "ИГРАТЬ", 0xFFFF3D5A.toInt())
        button(c, "moves", g.W / 2f - 90f * s, g.H * 0.70f, 160f * s, 56f * s, "ПРИЁМЫ", 0xFF4DE8FF.toInt())
        button(c, "sound", g.W / 2f + 90f * s, g.H * 0.70f, 160f * s, 56f * s, if (g.sfx.enabled) "ЗВУК: ДА" else "ЗВУК: НЕТ", 0xFFFFD84D.toInt(), 22f * s)
        if (g.bestWave > 0) {
            text(c, "рекорд: волна ${g.bestWave} • ${g.bestScore} очков • комбо ${g.bestCombo}", g.W / 2f, g.H * 0.84f, 20f * s, 0xFFFFD84D.toInt(), Paint.Align.CENTER, fBold, 0f)
        }
        text(c, "Слева — движение. Направление + АТК/СИЛ = разные приёмы. Меняй оружие посреди комбо!", g.W / 2f, g.H * 0.93f, 17f * s, 0xFF8A8296.toInt(), Paint.Align.CENTER, fBold, 0f)
    }

    private fun drawPause(c: Canvas) {
        val s = g.s
        dim(c)
        text(c, "ПАУЗА", g.W / 2f, g.H * 0.26f, 80f * s, 0xFFFFFFFF.toInt())
        button(c, "resume", g.W / 2f, g.H * 0.42f, 320f * s, 64f * s, "ПРОДОЛЖИТЬ", 0xFF4DFF9A.toInt())
        button(c, "moves", g.W / 2f, g.H * 0.55f, 320f * s, 64f * s, "ПРИЁМЫ", 0xFF4DE8FF.toInt())
        button(c, "sound", g.W / 2f, g.H * 0.68f, 320f * s, 64f * s, if (g.sfx.enabled) "ЗВУК: ДА" else "ЗВУК: НЕТ", 0xFFFFD84D.toInt())
        button(c, "menu", g.W / 2f, g.H * 0.81f, 320f * s, 64f * s, "В МЕНЮ", 0xFFFF3D5A.toInt())
        if (g.taken.isNotEmpty()) text(c, "усиления: " + g.taken.joinToString(", "), g.W / 2f, g.H * 0.93f, 16f * s, 0xFF8A8296.toInt(), Paint.Align.CENTER, fBold, 0f)
    }

    private fun drawPerk(c: Canvas) {
        val s = g.s
        dim(c)
        text(c, "ВЫБЕРИ УСИЛЕНИЕ", g.W / 2f, g.H * 0.2f, 54f * s, 0xFFFFD84D.toInt())
        val cw = min(300f * s, g.W * 0.28f); val ch = 260f * s
        val gap = 30f * s
        val n = g.perkChoices.size
        var x = g.W / 2f - (cw * n + gap * (n - 1)) / 2f
        val y = g.H * 0.3f
        for (i in 0 until n) {
            val pk = g.perkChoices[i]
            val r = RectF(x, y, x + cw, y + ch)
            g.ui.add(UiBtn("perk$i", r))
            fill.color = 0xEE1A1024.toInt()
            c.drawRoundRect(r, 18f * s, 18f * s, fill)
            stroke.color = Game.RANK_C[2 + i * 2]; stroke.strokeWidth = 3f * s
            c.drawRoundRect(r, 18f * s, 18f * s, stroke)
            text(c, pk.name, x + cw / 2f, y + 70f * s, 30f * s, Game.RANK_C[2 + i * 2])
            wrap(c, pk.desc, x + cw / 2f, y + 130f * s, cw - 30f * s, 20f * s, 0xFFE0D8F0.toInt())
            text(c, "ВЗЯТЬ", x + cw / 2f, y + ch - 30f * s, 22f * s, 0xFFFFFFFF.toInt())
            x += cw + gap
        }
    }

    private fun wrap(c: Canvas, s: String, cx: Float, y0: Float, maxW: Float, size: Float, col: Int) {
        txt.typeface = fBold; txt.textSize = size
        val words = s.split(" ")
        var line = ""
        var y = y0
        for (w in words) {
            val t = if (line.isEmpty()) w else "$line $w"
            if (txt.measureText(t) > maxW && line.isNotEmpty()) {
                text(c, line, cx, y, size, col, Paint.Align.CENTER, fBold, 0f)
                y += size * 1.3f; line = w
            } else line = t
        }
        if (line.isNotEmpty()) text(c, line, cx, y, size, col, Paint.Align.CENTER, fBold, 0f)
    }

    private fun drawOver(c: Canvas) {
        val s = g.s
        dim(c, 0xDD12040A.toInt())
        text(c, "ТЫ ПАЛ", g.W / 2f, g.H * 0.2f, 90f * s, 0xFFFF3D5A.toInt())
        val lines = listOf(
            "Волна" to "${g.wave}",
            "Очки" to "${g.score}",
            "Макс. комбо" to "${g.maxCombo}",
            "Лучший ранг" to Game.RANK_L[g.bestRank],
            "Убийств" to "${g.kills}",
            "Ударов" to "${g.hitsTotal}",
        )
        var y = g.H * 0.32f
        for ((k, v) in lines) {
            text(c, k, g.W / 2f - 20f * s, y, 26f * s, 0xFFB8B0C8.toInt(), Paint.Align.RIGHT, fBold, 0f)
            text(c, v, g.W / 2f + 20f * s, y, 28f * s, if (k == "Лучший ранг") Game.RANK_C[g.bestRank] else 0xFFFFFFFF.toInt(), Paint.Align.LEFT)
            y += 38f * s
        }
        if (g.newRecord) text(c, "НОВЫЙ РЕКОРД!", g.W / 2f, y + 8f * s, 30f * s, 0xFFFFD84D.toInt())
        button(c, "retry", g.W / 2f - 130f * s, g.H * 0.88f, 230f * s, 64f * s, "ЕЩЁ РАЗ", 0xFF4DFF9A.toInt())
        button(c, "menu", g.W / 2f + 130f * s, g.H * 0.88f, 230f * s, 64f * s, "МЕНЮ", 0xFFFF3D5A.toInt())
    }

    private fun drawMoves(c: Canvas) {
        val s = g.s
        c.drawRect(0f, 0f, g.W, g.H, sky)
        dim(c, 0xAA000000.toInt())
        // вкладки
        val tw = min(170f * s, (g.W - 160f * s) / 5f - 8f * s)
        var x = 140f * s
        for (i in 0 until 5) {
            val w = Weapons.all[i]
            val r = RectF(x, 16f * s, x + tw, 64f * s)
            g.ui.add(UiBtn("tab$i", r))
            val sel = i == g.movesTab
            fill.color = if (sel) alpha(w.color, 0.3f) else 0x88000000.toInt()
            c.drawRoundRect(r, 10f * s, 10f * s, fill)
            stroke.color = if (sel) w.color else alpha(w.color, 0.4f); stroke.strokeWidth = 2.5f * s
            c.drawRoundRect(r, 10f * s, 10f * s, stroke)
            text(c, w.ru.uppercase(), x + tw / 2f, 48f * s, 22f * s, if (sel) 0xFFFFFFFF.toInt() else w.color, Paint.Align.CENTER, fStyle, 0f)
            x += tw + 8f * s
        }
        button(c, "back", 66f * s, 40f * s, 100f * s, 48f * s, "НАЗАД", 0xFFFF3D5A.toInt(), 20f * s)

        val w = Weapons.all[g.movesTab]
        val moves = w.moves
        val half = (moves.size + 1) / 2
        val rowH = min(36f * s, (g.H - 210f * s) / half)
        val colW = (g.W - 80f * s) / 2f
        for (i in moves.indices) {
            val m = moves[i]
            val col = i / half; val row = i % half
            val bx = 40f * s + col * colW
            val by = 100f * s + row * rowH
            if (row % 2 == 0) { fill.color = 0x22FFFFFF; c.drawRect(bx, by - rowH * 0.7f, bx + colW - 20f * s, by + rowH * 0.3f, fill) }
            text(c, m.name, bx + 10f * s, by, rowH * 0.55f, w.color, Paint.Align.LEFT, fStyle, 0f)
            text(c, m.how, bx + colW - 30f * s, by, rowH * 0.5f, 0xFFE0D8F0.toInt(), Paint.Align.RIGHT, fBold, 0f)
        }
        val ty = g.H - 82f * s
        val hint = 0xFF9A90B0.toInt()
        val hs = 15f * s
        text(c, "АТК / СИЛ — лёгкая и сильная атака.  → ↑ ↓ — наклон джойстика в момент удара.  «…» — сделай паузу ~0.3с в цепочке.", g.W / 2f, ty, hs, hint, Paint.Align.CENTER, fBold, 0f)
        text(c, "ПРЫЖОК ×2 (у врага — ещё шаг по врагу).  РЫВОК — неуязвимость, увернись в момент удара = JUST DODGE.  Рывок + АТК = приём «→».", g.W / 2f, ty + 22f * s, hs, hint, Paint.Align.CENTER, fBold, 0f)
        text(c, "СМЕНА оружия посреди комбо = бонус стиля.  Бей по пулям — отражение.  Повторяй один приём — стиль растёт хуже.  ЯРОСТЬ: урон ×1.5.", g.W / 2f, ty + 44f * s, hs, hint, Paint.Align.CENTER, fBold, 0f)
    }
}
