package com.example.ui.components.workout

import androidx.compose.runtime.Immutable
import com.example.data.model.WorkoutExerciseSlot

enum class SilhouetteGender {
    MALE,
    FEMALE,
    NEUTRAL,
    ;

    companion object {
        fun fromSexCode(sex: String?): SilhouetteGender = when (sex?.trim()?.uppercase()) {
            "M", "MALE" -> MALE
            "F", "FEMALE" -> FEMALE
            else -> NEUTRAL
        }
    }
}

enum class SilhouetteViewMode {
    SIDE,
    FRONT,
}

enum class SilhouettePropType {
    NONE,
    FLOOR_MAT,
    CHAIR,
    STEP_BOX,
    WALL,
}

enum class ExerciseAnimationType(
    val viewMode: SilhouetteViewMode,
    val cycleDurationMs: Int,
    val isStaticHold: Boolean = false,
) {
    GENTLE_WALK(SilhouetteViewMode.SIDE, 1700),
    BRISK_MARCH(SilhouetteViewMode.SIDE, 1250),
    SEATED_ANKLE_SHOULDER(SilhouetteViewMode.SIDE, 2600),
    CHAIR_SIT_TO_STAND(SilhouetteViewMode.SIDE, 3200),
    BODYWEIGHT_SQUAT(SilhouetteViewMode.SIDE, 2800),
    STEP_UPS(SilhouetteViewMode.SIDE, 2600),
    PLANK_HOLD(SilhouetteViewMode.SIDE, 3600, isStaticHold = true),
    PUSH_UPS(SilhouetteViewMode.SIDE, 2600),
    WALL_PUSH_UPS(SilhouetteViewMode.SIDE, 2600),
    PIKE_SHOULDER_PRESS(SilhouetteViewMode.SIDE, 2800),
    SPLIT_LUNGE(SilhouetteViewMode.SIDE, 2900),
    GLUTE_BRIDGE(SilhouetteViewMode.SIDE, 2900),
    BREATH_WIND_DOWN(SilhouetteViewMode.FRONT, 4600),
    NECK_BACK_STRETCH(SilhouetteViewMode.FRONT, 4000),
    JUMPING_JACKS(SilhouetteViewMode.FRONT, 1500),
    BICEP_CURL(SilhouetteViewMode.FRONT, 2400),
    SHOULDER_PRESS(SilhouetteViewMode.FRONT, 2500),
    SIT_UPS(SilhouetteViewMode.SIDE, 2900),
    CALF_RAISES(SilhouetteViewMode.SIDE, 2200),
}

@Immutable
data class ExerciseMovementSpec(
    val type: ExerciseAnimationType,
    val labelEn: String,
    val labelUr: String,
)

@Immutable
data class Vec2(val x: Float, val y: Float)

/**
 * Normalized pose skeleton in [0f..1f] canvas coordinates (1:1 aspect stage).
 * For SIDE view, `near*` is the foreground limb and `far*` is the background limb.
 * For FRONT view, `near*` corresponds to screen-right (anatomical left) and `far*` to screen-left (anatomical right).
 */
@Immutable
data class BodySkeletonPose(
    val viewMode: SilhouetteViewMode,
    val headCenter: Vec2,
    val headRadius: Float,
    val headTiltDeg: Float = 0f,
    val neckBase: Vec2,
    val chestCenter: Vec2,
    val waistCenter: Vec2,
    val pelvisCenter: Vec2,
    val nearShoulder: Vec2,
    val nearElbow: Vec2,
    val nearWrist: Vec2,
    val nearHandTip: Vec2,
    val farShoulder: Vec2,
    val farElbow: Vec2,
    val farWrist: Vec2,
    val farHandTip: Vec2,
    val nearHip: Vec2,
    val nearKnee: Vec2,
    val nearAnkle: Vec2,
    val nearHeel: Vec2,
    val nearToe: Vec2,
    val farHip: Vec2,
    val farKnee: Vec2,
    val farAnkle: Vec2,
    val farHeel: Vec2,
    val farToe: Vec2,
    val propType: SilhouettePropType = SilhouettePropType.NONE,
    val propPrimaryY: Float = 0.88f,
    val propSecondaryX: Float = 0.78f,
    val floorY: Float = 0.88f,
    val torsoLeanDeg: Float = 0f,
    val breathingExpand: Float = 0f,
)

/**
 * Maps every WorkoutExerciseSlot in the app's workout database into one or more
 * distinct, dedicated exercise animations.
 */
fun resolveExerciseMovements(slot: WorkoutExerciseSlot): List<ExerciseMovementSpec> {
    val name = slot.name_en.lowercase()
    val list = mutableListOf<ExerciseMovementSpec>()

    fun addIfMissing(type: ExerciseAnimationType, en: String, ur: String) {
        if (list.none { it.type == type }) {
            list += ExerciseMovementSpec(type, en, ur)
        }
    }

    if ("sit-to-stand" in name || "sit to stand" in name) {
        addIfMissing(ExerciseAnimationType.CHAIR_SIT_TO_STAND, "Chair Sit-to-Stand", "کرسی سے اٹھنا بیٹھنا")
    }
    if ("ankle circle" in name || "shoulder roll" in name || "seated" in name) {
        addIfMissing(ExerciseAnimationType.SEATED_ANKLE_SHOULDER, "Ankle & Shoulder Rolls", "ٹخنوں اور کندھوں کی گردش")
    }
    if ("wall push" in name) {
        addIfMissing(ExerciseAnimationType.WALL_PUSH_UPS, "Wall Push-Ups", "دیوار پش اپس")
    } else if ("push-up" in name || "push up" in name || "pushup" in name) {
        addIfMissing(ExerciseAnimationType.PUSH_UPS, "Push-Ups", "پش اپس")
    }
    if ("pike" in name) {
        addIfMissing(ExerciseAnimationType.PIKE_SHOULDER_PRESS, "Pike Shoulder Press", "پائیک شولڈر پریس")
    } else if ("shoulder press" in name) {
        addIfMissing(ExerciseAnimationType.SHOULDER_PRESS, "Shoulder Press", "شولڈر پریس")
    }
    if ("lunge" in name) {
        addIfMissing(ExerciseAnimationType.SPLIT_LUNGE, "Split Lunges", "لنجز")
    }
    if ("squat" in name) {
        addIfMissing(ExerciseAnimationType.BODYWEIGHT_SQUAT, "Bodyweight Squats", "اسکواٹس")
    }
    if ("step-up" in name || "step up" in name) {
        addIfMissing(ExerciseAnimationType.STEP_UPS, "Step-Ups", "اسٹیپ اپس")
    }
    if ("glute bridge" in name || "bridge" in name) {
        addIfMissing(ExerciseAnimationType.GLUTE_BRIDGE, "Glute Bridges", "گلوٹ برجز")
    }
    if ("plank" in name) {
        addIfMissing(ExerciseAnimationType.PLANK_HOLD, "Plank Hold", "پلینک ہولڈ")
    }
    if ("marching" in name || "brisk" in name) {
        addIfMissing(ExerciseAnimationType.BRISK_MARCH, "Brisk March / Walk", "تیز چہل قدمی")
    }
    if ("walk" in name || "stroll" in name) {
        if ("marching" !in name && "brisk" !in name) {
            addIfMissing(ExerciseAnimationType.GENTLE_WALK, "Gentle Walk", "ہلکی چہل قدمی")
        } else if (list.any { it.type != ExerciseAnimationType.BRISK_MARCH }) {
            // Compound slot mentioning evening walk alongside bridges/plank
            addIfMissing(ExerciseAnimationType.GENTLE_WALK, "Evening Walk", "شام کی چہل قدمی")
        }
    }
    if ("cyclic sighing" in name || "breath" in name) {
        addIfMissing(ExerciseAnimationType.BREATH_WIND_DOWN, "Breath Wind-Down", "سانس کی مشق")
    }
    if ("neck" in name || "stretch" in name || "mobility" in name) {
        if ("ankle" !in name) {
            addIfMissing(ExerciseAnimationType.NECK_BACK_STRETCH, "Neck & Back Stretch", "گردن و کمر اسٹریچ")
        }
    }
    if ("jumping jack" in name) {
        addIfMissing(ExerciseAnimationType.JUMPING_JACKS, "Jumping Jacks", "جمپنگ جیکس")
    }
    if ("bicep" in name || "curl" in name) {
        addIfMissing(ExerciseAnimationType.BICEP_CURL, "Bicep Curls", "بائیسپ کرل")
    }
    if ("sit-up" in name || "crunch" in name) {
        addIfMissing(ExerciseAnimationType.SIT_UPS, "Sit-Ups", "سِٹ اپس")
    }
    if ("calf raise" in name) {
        addIfMissing(ExerciseAnimationType.CALF_RAISES, "Calf Raises", "کاف ریزز")
    }

    if (list.isEmpty()) {
        list += ExerciseMovementSpec(ExerciseAnimationType.GENTLE_WALK, slot.name_en, slot.name_ur)
    }
    return list
}
