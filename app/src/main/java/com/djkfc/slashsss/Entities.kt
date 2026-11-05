package com.djkfc.slashsss

const val GROUND = 620f
const val VIEW_H = 720f
const val ARENA_W = 2800f
const val GRAV = 2700f

class Player {
    var x = ARENA_W / 2f
    var y = GROUND
    var vx = 0f
    var vy = 0f
    var face = 1
    var onGround = true
    var hp = 100f
    var maxHp = 100f
    var weapon = 0

    var move: Move? = null
    var moveW = 0
    var mt = 0f
    var fired = false
    var lungeYDone = false
    var landed = false
    var varMul = 1f
    var lastFlurry = -1
    val hitSet = HashMap<Enemy, Float>()

    var jumps = 1
    var maxJumps = 1
    var airDash = true
    var dashT = 0f
    var dashCd = 0f
    var invuln = 0f
    var hurtT = 0f
    var lastMove: Move? = null
    var lastEnd = -10f
    var anim = 0f
    var squash = 0f

    val scarfX = FloatArray(10)
    val scarfY = FloatArray(10)

    val cy: Float get() = y - 50f
}

class Enemy(val type: EType, var x: Float, var y: Float, val elite: Boolean) {
    var vx = 0f
    var vy = 0f
    var hp = 40f
    var maxHp = 40f
    var face = -1
    var air = false
    var stun = 0f
    var state = 0
    var st = 0f
    var atkCd = 1f
    var guard = 0f
    var guardMax = 0f
    var guardBroken = 0f
    var armor = 0f
    var armorMax = 0f
    var armorT = 0f
    var flash = 0f
    var dead = false
    var w = 44f
    var h = 96f
    var dmg = 10f
    var speed = 150f
    var range = 90f
    var windup = 0.55f
    var active = 0.16f
    var lunge = 0f
    var hitPlayer = false
    var dodged = false
    var slammed = false
    var anim = (Math.random() * 10).toFloat()
    var hoverY = GROUND - 280f
    var juggle = 0
    var score = 100

    val cy: Float get() = y - h / 2f
}

class Proj(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val w: Float, val h: Float, var dmg: Float, var friendly: Boolean, val kind: Int, var life: Float,
    val move: Move?, val color: Int,
) {
    var t = 0f
    var face = if (vx >= 0f) 1 else -1
    var returned = false
    val hitSet = HashSet<Enemy>()
    var dead = false
}

class Particle {
    var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var life = 0f; var max = 1f; var size = 4f; var color = 0; var kind = 0; var grav = 0f
}

class SlashFx(
    val x: Float, val y: Float, val r: Float, val a0: Float, val sweep: Float,
    val color: Int, var life: Float, val shape: Int, val face: Int, val len: Float,
) {
    val max = life
}

class FloatText(val text: String, var x: Float, var y: Float, val color: Int, val size: Float, var life: Float) {
    val max = life
}

class Ghost(val x: Float, val y: Float, val face: Int, val color: Int, var life: Float)
