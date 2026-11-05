package com.djkfc.slashsss

// Особые эффекты приёмов
enum class Fx { NONE, WAVE, BOOMERANG, PULL, HOOK, DIVE, BOUNCE }

/**
 * Приём. Все хитбоксы описаны относительно центра игрока, лицом вправо.
 * hx/hy — левый-верхний угол хитбокса, hw/hh — размер.
 */
class Move(
    val id: String,
    val name: String,
    val how: String,
    val dur: Float,
    val a0: Float,
    val a1: Float,
    val hx: Float, val hy: Float, val hw: Float, val hh: Float,
    val dmg: Float,
    val kbx: Float = 150f,
    val kby: Float = -120f,
    val hits: Int = 1,
    val style: Float = 14f,
    val cancel: Float = -1f,
    val lungeX: Float = 0f,
    val lungeY: Float = 0f,
    val launch: Boolean = false,
    val slam: Boolean = false,
    val heavy: Boolean = false,
    val stun: Float = 0.4f,
    val fx: Fx = Fx.NONE,
    val arc0: Float = -70f,
    val arc1: Float = 50f,
    val rad: Float = 120f,
    val shape: Int = 0, // 0 дуга, 1 выпад, 2 круг, 3 удар в землю, 4 шквал
    val shake: Float = 0f,
    val stop: Float = 0.045f,
    val both: Boolean = false,
) {
    val cancelT: Float get() = if (cancel < 0f) a1 + 0.02f else cancel
    val interval: Float get() = if (hits > 1) (a1 - a0) / hits else 999f
}

class Weapon(
    val name: String,
    val ru: String,
    val color: Int,
    val moves: List<Move>,
    val links: Map<String, String>,
) {
    val byId: Map<String, Move> = moves.associateBy { it.id }
}

object Weapons {
    // ---------------- КАТАНА ----------------
    private val katana = Weapon(
        "KATANA", "Катана", 0xFF4DE8FF.toInt(),
        listOf(
            Move("k1", "SLASH", "АТК", 0.30f, 0.06f, 0.14f, 0f, -70f, 125f, 100f, 9f, 120f, -60f, style = 12f, arc0 = -80f, arc1 = 45f),
            Move("k2", "CROSS", "АТК ×2", 0.30f, 0.06f, 0.14f, 0f, -70f, 125f, 100f, 9f, 120f, -60f, style = 12f, arc0 = 50f, arc1 = -70f),
            Move("k3", "RISING CUT", "АТК ×3", 0.36f, 0.07f, 0.16f, 0f, -110f, 120f, 140f, 11f, 80f, -380f, style = 16f, arc0 = 70f, arc1 = -110f),
            Move("k4", "TEMPEST", "АТК ×4", 0.58f, 0.08f, 0.40f, -40f, -95f, 200f, 150f, 6f, 280f, -220f, hits = 4, style = 30f, shape = 2, rad = 120f, both = true, shake = 4f),
            Move("kp", "THOUSAND CUTS", "АТК АТК … АТК", 0.85f, 0.10f, 0.70f, 10f, -90f, 180f, 130f, 4f, 30f, -60f, hits = 9, style = 42f, shape = 4, stun = 0.5f, stop = 0.03f),
            Move("kf", "STINGER", "→ АТК", 0.46f, 0.05f, 0.30f, 0f, -60f, 120f, 70f, 15f, 520f, -120f, style = 28f, lungeX = 1500f, shape = 1, cancel = 0.32f, shake = 5f),
            Move("ku", "HIGH TIME", "↑ АТК (подброс)", 0.46f, 0.08f, 0.18f, 0f, -150f, 120f, 190f, 10f, 40f, -1150f, style = 24f, launch = true, arc0 = 70f, arc1 = -115f),
            Move("kh", "IAI", "СИЛ", 0.62f, 0.24f, 0.32f, 0f, -75f, 200f, 110f, 28f, 320f, -100f, style = 30f, heavy = true, shape = 1, stop = 0.1f, shake = 10f),
            Move("kfh", "JUDGEMENT CUT", "→ СИЛ", 0.78f, 0.30f, 0.62f, 230f, -150f, 240f, 220f, 7f, 20f, -200f, hits = 5, style = 45f, heavy = true, shape = 2, rad = 120f, stop = 0.05f, shake = 6f),
            Move("kuh", "RISING DRAGON", "↑ СИЛ", 0.55f, 0.05f, 0.42f, -10f, -170f, 130f, 210f, 6f, 30f, -900f, hits = 4, style = 34f, lungeY = -950f, launch = true, arc0 = 80f, arc1 = -100f, shape = 2, rad = 90f),
            Move("kdh", "SHEATHE SPLIT", "↓ СИЛ", 0.70f, 0.32f, 0.40f, -10f, -60f, 160f, 110f, 22f, 200f, -500f, style = 30f, heavy = true, slam = true, shape = 3, both = true, shake = 9f, stop = 0.08f),
            Move("ka1", "AERIAL", "в возд. АТК", 0.30f, 0.06f, 0.14f, 0f, -80f, 120f, 120f, 8f, 70f, -280f, style = 12f),
            Move("ka2", "AERIAL II", "в возд. АТК ×2", 0.30f, 0.06f, 0.14f, 0f, -80f, 120f, 120f, 8f, 70f, -280f, style = 12f, arc0 = 50f, arc1 = -70f),
            Move("ka3", "AERIAL RAVE", "в возд. АТК ×3", 0.42f, 0.08f, 0.18f, 0f, -80f, 130f, 130f, 12f, 330f, -120f, style = 20f, arc0 = -100f, arc1 = 80f, shake = 4f),
            Move("kad", "HELM BREAKER", "в возд. ↓ АТК", 0.55f, 0.10f, 0.30f, -10f, -60f, 110f, 140f, 20f, 100f, 0f, style = 30f, slam = true, fx = Fx.DIVE, shape = 3, shake = 12f, stop = 0.08f, arc0 = -100f, arc1 = 90f),
            Move("kah", "AIR TEMPEST", "в возд. СИЛ", 0.60f, 0.08f, 0.48f, -40f, -100f, 200f, 160f, 6f, 120f, -260f, hits = 5, style = 32f, shape = 2, both = true),
            Move("kau", "SKY SLICE", "в возд. ↑ АТК", 0.40f, 0.06f, 0.16f, 0f, -160f, 120f, 180f, 10f, 40f, -700f, style = 18f, lungeY = -500f, arc0 = 80f, arc1 = -110f),
        ),
        mapOf(
            ">L" to "k1", "k1>L" to "k2", "k2>L" to "k3", "k3>L" to "k4", "k2~L" to "kp",
            ">fL" to "kf", "kf>L" to "k3", ">uL" to "ku", "ku>L" to "k1",
            ">H" to "kh", ">fH" to "kfh", ">uH" to "kuh", ">dH" to "kdh", "k4>H" to "kh",
            ">aL" to "ka1", "ka1>aL" to "ka2", "ka2>aL" to "ka3", ">adL" to "kad", ">adH" to "kad",
            ">aH" to "kah", ">auL" to "kau", "kau>aL" to "ka1",
        )
    )

    // ---------------- ДВУРУЧНИК ----------------
    private val sword = Weapon(
        "GREATSWORD", "Двуручник", 0xFFFF8A3D.toInt(),
        listOf(
            Move("g1", "CLEAVE", "АТК", 0.50f, 0.16f, 0.26f, 0f, -90f, 170f, 130f, 18f, 260f, -120f, style = 15f, heavy = true, rad = 160f, shake = 5f, stop = 0.07f),
            Move("g2", "BACKSWING", "АТК ×2", 0.50f, 0.16f, 0.26f, -60f, -90f, 230f, 130f, 18f, 260f, -160f, style = 15f, heavy = true, rad = 160f, arc0 = 60f, arc1 = -90f, shake = 5f, stop = 0.07f),
            Move("g3", "SPLITTER", "АТК ×3", 0.68f, 0.26f, 0.36f, 10f, -130f, 180f, 180f, 32f, 200f, -60f, style = 30f, heavy = true, slam = true, rad = 170f, arc0 = -100f, arc1 = 90f, shape = 3, shake = 13f, stop = 0.11f),
            Move("gp", "DRIVE", "АТК АТК … АТК", 0.62f, 0.20f, 0.30f, 20f, -90f, 140f, 130f, 20f, 380f, -150f, style = 36f, heavy = true, fx = Fx.WAVE, rad = 160f, shake = 6f),
            Move("gf", "RAM", "→ АТК", 0.52f, 0.05f, 0.32f, 0f, -90f, 110f, 120f, 16f, 720f, -200f, style = 26f, heavy = true, lungeX = 1100f, shape = 1, shake = 6f),
            Move("gu", "TITAN UPPER", "↑ АТК (подброс)", 0.58f, 0.16f, 0.26f, 0f, -170f, 160f, 220f, 18f, 60f, -1150f, style = 26f, heavy = true, launch = true, arc0 = 80f, arc1 = -120f, rad = 160f, shake = 5f),
            Move("gh", "EARTHQUAKE", "СИЛ", 0.95f, 0.46f, 0.56f, 0f, -110f, 250f, 150f, 40f, 300f, -450f, style = 40f, heavy = true, slam = true, both = true, shape = 3, rad = 180f, shake = 20f, stop = 0.13f, arc0 = -110f, arc1 = 90f),
            Move("gfh", "WHIRLWIND", "→ СИЛ", 1.05f, 0.15f, 0.88f, 0f, -110f, 160f, 160f, 8f, 160f, -150f, hits = 7, style = 44f, lungeX = 330f, shape = 2, both = true, rad = 150f, heavy = true),
            Move("guh", "SKY RENDER", "↑ СИЛ", 0.72f, 0.22f, 0.36f, 0f, -230f, 170f, 270f, 25f, 80f, -1300f, style = 34f, heavy = true, launch = true, arc0 = 90f, arc1 = -130f, rad = 190f, shake = 8f, stop = 0.09f),
            Move("gdh", "GRAVE DIGGER", "↓ СИЛ", 0.80f, 0.30f, 0.60f, 0f, -50f, 200f, 90f, 9f, 60f, -650f, hits = 4, style = 34f, heavy = true, both = true, shape = 3, rad = 140f, shake = 7f),
            Move("ga1", "AIR CLEAVE", "в возд. АТК", 0.46f, 0.14f, 0.24f, 0f, -100f, 160f, 150f, 16f, 160f, -300f, style = 16f, heavy = true, rad = 150f),
            Move("ga2", "SKY CRUSH", "в возд. АТК ×2", 0.50f, 0.16f, 0.26f, 0f, -80f, 160f, 160f, 20f, 160f, 0f, style = 22f, heavy = true, slam = true, arc0 = -110f, arc1 = 90f, rad = 150f, shake = 8f),
            Move("gad", "METEOR", "в возд. ↓ АТК", 0.70f, 0.12f, 0.36f, -30f, -60f, 160f, 160f, 32f, 300f, -500f, style = 36f, heavy = true, slam = true, fx = Fx.DIVE, shape = 3, rad = 200f, shake = 22f, stop = 0.12f, both = true),
            Move("gah", "HELICOPTER", "в возд. СИЛ", 0.95f, 0.10f, 0.85f, 0f, -110f, 160f, 170f, 8f, 160f, -240f, hits = 6, style = 36f, shape = 2, both = true, rad = 150f),
        ),
        mapOf(
            ">L" to "g1", "g1>L" to "g2", "g2>L" to "g3", "g2~L" to "gp",
            ">fL" to "gf", "gf>L" to "g2", ">uL" to "gu", "gu>L" to "g1",
            ">H" to "gh", ">fH" to "gfh", ">uH" to "guh", ">dH" to "gdh", "g3>H" to "gh",
            ">aL" to "ga1", "ga1>aL" to "ga2", ">adL" to "gad", ">adH" to "gad", ">aH" to "gah",
        )
    )

    // ---------------- КАСТЕТЫ ----------------
    private val fists = Weapon(
        "GAUNTLETS", "Кастеты", 0xFFFFE14D.toInt(),
        listOf(
            Move("f1", "JAB", "АТК", 0.20f, 0.04f, 0.10f, 10f, -80f, 85f, 70f, 5f, 60f, -40f, style = 8f, shape = 1, rad = 70f, stop = 0.03f),
            Move("f2", "JAB II", "АТК ×2", 0.20f, 0.04f, 0.10f, 10f, -80f, 85f, 70f, 5f, 60f, -40f, style = 8f, shape = 1, rad = 70f, stop = 0.03f),
            Move("f3", "HOOK", "АТК ×3", 0.26f, 0.06f, 0.13f, 0f, -90f, 95f, 90f, 7f, 100f, -80f, style = 10f, rad = 80f, arc0 = 60f, arc1 = -40f),
            Move("f4", "ROUNDHOUSE", "АТК ×4", 0.34f, 0.08f, 0.17f, 0f, -80f, 120f, 100f, 9f, 220f, -160f, style = 14f, rad = 100f, arc0 = 40f, arc1 = -60f),
            Move("f5", "FINISHER", "АТК ×5", 0.46f, 0.12f, 0.20f, 0f, -80f, 120f, 90f, 16f, 820f, -250f, style = 26f, shape = 1, shake = 9f, stop = 0.09f),
            Move("fp", "RAPID FIRE", "АТК ×3 … АТК", 0.95f, 0.08f, 0.82f, 10f, -95f, 120f, 110f, 3f, 30f, -30f, hits = 14, style = 46f, shape = 4, stun = 0.45f, stop = 0.02f, rad = 80f),
            Move("ff", "ROCKET", "→ АТК", 0.42f, 0.05f, 0.28f, 0f, -80f, 90f, 80f, 14f, 620f, -150f, style = 24f, lungeX = 1600f, shape = 1, shake = 5f),
            Move("fu", "SHORYU", "↑ АТК (подброс)", 0.52f, 0.04f, 0.30f, 0f, -160f, 90f, 190f, 7f, 40f, -1100f, hits = 2, style = 26f, lungeY = -1250f, lungeX = 150f, launch = true, arc0 = 60f, arc1 = -90f, rad = 80f),
            Move("fh", "INFERNO", "СИЛ", 0.72f, 0.35f, 0.42f, 0f, -85f, 130f, 100f, 35f, 1150f, -300f, style = 36f, heavy = true, shape = 1, shake = 15f, stop = 0.14f),
            Move("ffh", "STORM KICKS", "→ СИЛ", 0.72f, 0.10f, 0.62f, 0f, -90f, 120f, 110f, 5f, 90f, -80f, hits = 7, style = 38f, lungeX = 520f, shape = 4, rad = 90f),
            Move("fuh", "TWIN DRAGON", "↑ СИЛ", 0.75f, 0.10f, 0.50f, -20f, -200f, 130f, 240f, 7f, 30f, -900f, hits = 4, style = 36f, lungeY = -1100f, launch = true, shape = 2, rad = 80f),
            Move("fdh", "GROUND POUND", "↓ СИЛ", 0.62f, 0.30f, 0.38f, 0f, -50f, 180f, 90f, 18f, 150f, -650f, style = 30f, heavy = true, slam = true, launch = true, both = true, shape = 3, rad = 160f, shake = 12f),
            Move("fa1", "AIR JAB", "в возд. АТК", 0.22f, 0.04f, 0.11f, 10f, -90f, 90f, 90f, 6f, 50f, -240f, style = 8f, shape = 1, rad = 70f),
            Move("fa2", "AIR HOOK", "в возд. АТК ×2", 0.24f, 0.05f, 0.12f, 0f, -90f, 100f, 100f, 6f, 50f, -240f, style = 9f, rad = 80f, arc0 = 60f, arc1 = -40f),
            Move("fa3", "AIR STRAIGHT", "в возд. АТК ×3", 0.36f, 0.08f, 0.16f, 0f, -85f, 120f, 90f, 11f, 500f, -150f, style = 16f, shape = 1, shake = 5f),
            Move("fad", "DIVE KICK", "в возд. ↓ АТК (отскок)", 0.60f, 0.06f, 0.50f, 0f, -20f, 80f, 90f, 13f, 200f, -300f, style = 24f, lungeX = 750f, lungeY = 1400f, fx = Fx.BOUNCE, shape = 1),
            Move("fah", "HAMMER", "в возд. СИЛ", 0.50f, 0.18f, 0.28f, 0f, -80f, 120f, 150f, 18f, 80f, 0f, style = 24f, heavy = true, slam = true, arc0 = -110f, arc1 = 90f, rad = 90f, shake = 9f, stop = 0.09f),
        ),
        mapOf(
            ">L" to "f1", "f1>L" to "f2", "f2>L" to "f3", "f3>L" to "f4", "f4>L" to "f5", "f3~L" to "fp",
            ">fL" to "ff", "ff>L" to "f4", ">uL" to "fu", "fu>L" to "f1",
            ">H" to "fh", ">fH" to "ffh", ">uH" to "fuh", ">dH" to "fdh", "f5>H" to "fh",
            ">aL" to "fa1", "fa1>aL" to "fa2", "fa2>aL" to "fa3", ">adL" to "fad", ">aH" to "fah", ">adH" to "fah",
        )
    )

    // ---------------- КОСА ----------------
    private val scythe = Weapon(
        "SCYTHE", "Коса", 0xFFB45CFF.toInt(),
        listOf(
            Move("s1", "REAP", "АТК", 0.38f, 0.10f, 0.20f, 0f, -100f, 175f, 140f, 12f, 140f, -80f, style = 13f, rad = 160f, arc0 = -100f, arc1 = 30f),
            Move("s2", "REVERSE REAP", "АТК ×2", 0.40f, 0.10f, 0.20f, 0f, -100f, 175f, 140f, 12f, 160f, -100f, style = 15f, rad = 160f, arc0 = 40f, arc1 = -110f, both = true),
            Move("s3", "DEATH SPIN", "АТК ×3", 0.62f, 0.10f, 0.46f, 0f, -110f, 180f, 160f, 7f, 220f, -220f, hits = 4, style = 30f, shape = 2, both = true, rad = 160f, shake = 4f),
            Move("sp", "HARVEST", "АТК АТК … АТК (притяг.)", 0.60f, 0.12f, 0.40f, 0f, -100f, 280f, 140f, 8f, -480f, -60f, hits = 3, style = 38f, fx = Fx.PULL, rad = 260f, arc0 = -60f, arc1 = 40f),
            Move("sf", "SICKLE", "→ АТК (бумеранг)", 0.42f, 0.12f, 0.14f, 0f, -80f, 10f, 10f, 9f, 120f, -150f, style = 26f, fx = Fx.BOOMERANG, cancel = 0.25f),
            Move("su", "HARVEST MOON", "↑ АТК (подброс)", 0.50f, 0.10f, 0.22f, -40f, -180f, 200f, 220f, 14f, 40f, -1150f, style = 24f, launch = true, rad = 170f, arc0 = 100f, arc1 = -130f),
            Move("sh", "DEATH CIRCLE", "СИЛ", 0.92f, 0.30f, 0.80f, 0f, -140f, 240f, 210f, 7f, 120f, -200f, hits = 6, style = 42f, heavy = true, shape = 2, both = true, rad = 230f, shake = 5f),
            Move("sfh", "GRIM DRAG", "→ СИЛ (притяг.)", 0.62f, 0.20f, 0.30f, 40f, -100f, 340f, 140f, 15f, -950f, -120f, style = 30f, heavy = true, fx = Fx.PULL, rad = 300f, arc0 = -20f, arc1 = 60f, shake = 6f),
            Move("suh", "SOUL RISE", "↑ СИЛ", 0.70f, 0.08f, 0.50f, -30f, -200f, 180f, 240f, 6f, 20f, -800f, hits = 5, style = 36f, lungeY = -850f, launch = true, shape = 2, both = true, rad = 140f),
            Move("sdh", "GRAVE SWEEP", "↓ СИЛ", 0.60f, 0.22f, 0.32f, 0f, -50f, 260f, 80f, 18f, 120f, -550f, style = 28f, heavy = true, both = true, rad = 220f, arc0 = 30f, arc1 = 30f, shape = 3, shake = 6f),
            Move("sa1", "AIR REAP", "в возд. АТК", 0.38f, 0.08f, 0.18f, 0f, -110f, 170f, 160f, 11f, 80f, -280f, style = 13f, rad = 150f, arc0 = -100f, arc1 = 40f),
            Move("sa2", "AIR REVERSE", "в возд. АТК ×2", 0.40f, 0.08f, 0.18f, 0f, -110f, 170f, 160f, 11f, 80f, -280f, style = 14f, rad = 150f, arc0 = 40f, arc1 = -110f, both = true),
            Move("sad", "REAPER DROP", "в возд. ↓ АТК", 0.62f, 0.10f, 0.34f, -20f, -70f, 150f, 160f, 24f, 200f, -400f, style = 30f, slam = true, fx = Fx.DIVE, both = true, shape = 3, rad = 180f, shake = 13f, stop = 0.09f),
            Move("sah", "LUNAR", "в возд. СИЛ", 0.80f, 0.08f, 0.70f, 0f, -120f, 190f, 190f, 7f, 120f, -260f, hits = 6, style = 36f, shape = 2, both = true, rad = 170f),
        ),
        mapOf(
            ">L" to "s1", "s1>L" to "s2", "s2>L" to "s3", "s2~L" to "sp",
            ">fL" to "sf", "sf>L" to "s1", ">uL" to "su", "su>L" to "s1",
            ">H" to "sh", ">fH" to "sfh", ">uH" to "suh", ">dH" to "sdh", "sfh>L" to "s3", "sp>L" to "s3",
            ">aL" to "sa1", "sa1>aL" to "sa2", ">adL" to "sad", ">adH" to "sad", ">aH" to "sah",
        )
    )

    // ---------------- ЦЕПЬ ----------------
    private val chain = Weapon(
        "CHAINS", "Цепь", 0xFFFF3D5A.toInt(),
        listOf(
            Move("c1", "LASH", "АТК", 0.32f, 0.10f, 0.17f, 20f, -80f, 240f, 60f, 8f, 150f, -60f, style = 11f, shape = 1),
            Move("c2", "LASH II", "АТК ×2", 0.32f, 0.10f, 0.17f, 20f, -80f, 240f, 60f, 8f, 150f, -60f, style = 11f, shape = 1),
            Move("c3", "CRACK", "АТК ×3", 0.42f, 0.13f, 0.21f, 20f, -85f, 310f, 70f, 12f, 260f, -120f, style = 16f, shape = 1, shake = 4f, stop = 0.06f),
            Move("c4", "TWIRL", "АТК ×4", 0.64f, 0.10f, 0.50f, 0f, -110f, 220f, 150f, 6f, 200f, -200f, hits = 5, style = 32f, shape = 2, both = true, rad = 220f),
            Move("cp", "SNAKE BITE", "АТК … АТК", 0.80f, 0.10f, 0.68f, 20f, -80f, 320f, 60f, 5f, 40f, -40f, hits = 6, style = 40f, shape = 1, stop = 0.025f),
            Move("cf", "HOOK", "→ АТК (притянуть)", 0.50f, 0.12f, 0.22f, 20f, -85f, 400f, 70f, 6f, 0f, 0f, style = 26f, fx = Fx.HOOK, shape = 1, stun = 0.9f),
            Move("cu", "WHIP LIFT", "↑ АТК (подброс)", 0.46f, 0.10f, 0.20f, 0f, -200f, 200f, 230f, 10f, -80f, -1150f, style = 24f, launch = true, rad = 200f, arc0 = 70f, arc1 = -120f),
            Move("ch", "FLAIL STORM", "СИЛ", 0.95f, 0.20f, 0.85f, 0f, -150f, 270f, 220f, 6f, 180f, -180f, hits = 7, style = 44f, heavy = true, shape = 2, both = true, rad = 260f, shake = 4f),
            Move("cfh", "LONG LASH", "→ СИЛ", 0.70f, 0.30f, 0.38f, 20f, -85f, 480f, 70f, 22f, 450f, -160f, style = 30f, heavy = true, shape = 1, shake = 8f, stop = 0.09f),
            Move("cuh", "SKY CHAIN", "↑ СИЛ", 0.70f, 0.15f, 0.50f, -60f, -330f, 240f, 340f, 6f, 0f, -650f, hits = 5, style = 36f, launch = true, shape = 2, rad = 180f),
            Move("cdh", "SNAKE NET", "↓ СИЛ", 0.66f, 0.25f, 0.45f, 0f, -45f, 300f, 70f, 8f, -200f, -600f, hits = 3, style = 34f, heavy = true, both = true, launch = true, shape = 3, rad = 280f, shake = 6f),
            Move("ca1", "AIR LASH", "в возд. АТК", 0.32f, 0.08f, 0.16f, 20f, -85f, 230f, 70f, 8f, 80f, -260f, style = 11f, shape = 1),
            Move("ca2", "AIR CRACK", "в возд. АТК ×2", 0.38f, 0.10f, 0.18f, 20f, -85f, 280f, 70f, 11f, 300f, -120f, style = 15f, shape = 1),
            Move("cad", "ANCHOR", "в возд. ↓ АТК", 0.60f, 0.10f, 0.34f, -20f, -60f, 150f, 160f, 22f, 200f, -450f, style = 30f, slam = true, fx = Fx.DIVE, both = true, shape = 3, rad = 200f, shake = 12f),
            Move("cah", "AIR STORM", "в возд. СИЛ", 0.85f, 0.10f, 0.75f, 0f, -140f, 240f, 220f, 6f, 160f, -260f, hits = 6, style = 38f, shape = 2, both = true, rad = 230f),
            Move("cau", "SKY HOOK", "в возд. → АТК", 0.50f, 0.12f, 0.22f, 20f, -110f, 380f, 120f, 6f, 0f, 0f, style = 26f, fx = Fx.HOOK, shape = 1, stun = 0.9f),
        ),
        mapOf(
            ">L" to "c1", "c1>L" to "c2", "c2>L" to "c3", "c3>L" to "c4", "c1~L" to "cp",
            ">fL" to "cf", "cf>L" to "c3", ">uL" to "cu", "cu>L" to "c1",
            ">H" to "ch", ">fH" to "cfh", ">uH" to "cuh", ">dH" to "cdh", "c4>H" to "cfh",
            ">aL" to "ca1", "ca1>aL" to "ca2", ">adL" to "cad", ">adH" to "cad", ">aH" to "cah", ">afL" to "cau",
        )
    )

    val all: List<Weapon> = listOf(katana, sword, fists, scythe, chain)
}

enum class EType { GRUNT, RUNNER, SHIELD, FLYER, GUNNER, BRUTE }

class Perk(val name: String, val desc: String, val apply: (Game) -> Unit)

object Perks {
    val all: List<Perk> = listOf(
        Perk("ОСТРОТА", "+15% урона") { it.dmgMul += 0.15f },
        Perk("ЖИВУЧЕСТЬ", "+25 к макс. HP и полное лечение") { it.player.maxHp += 25f; it.player.hp = it.player.maxHp },
        Perk("БЕШЕНСТВО", "Ярость копится на 35% быстрее") { it.rageMul += 0.35f },
        Perk("ПОКАЗУХА", "Стиль падает на 25% медленнее") { it.decayMul *= 0.75f },
        Perk("ВАМПИРИЗМ", "+0.5 HP за каждый удар") { it.lifesteal += 0.5f },
        Perk("КРЫЛЬЯ", "Ещё один прыжок в воздухе") { it.player.maxJumps += 1 },
        Perk("ТЕМП", "Атаки на 10% быстрее") { it.atkSpeed += 0.10f },
        Perk("КРИТ", "+10% шанс крита ×2") { it.crit += 0.10f },
        Perk("СФЕРЫ", "Чаще падают сферы лечения") { it.orbMul += 0.6f },
        Perk("ПРИЗРАК", "Рывок длиннее неуязвим, кд меньше") { it.dashIframe += 0.08f; it.dashCdMul *= 0.8f },
        Perk("КАЗНЬ", "+40% урона по подброшенным") { it.airDmg += 0.4f },
        Perk("ЭГО", "+20% очков стиля") { it.styleMul += 0.2f },
    )
}
