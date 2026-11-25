package com.example.ui.components.workout

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val HEAD_RADIUS = 0.043f
private const val NECK_LEN = 0.036f
private const val TORSO_LEN = 0.215f
private const val UPPER_ARM_LEN = 0.130f
private const val FOREARM_LEN = 0.120f
private const val HAND_LEN = 0.044f
private const val THIGH_LEN = 0.184f
private const val SHIN_LEN = 0.178f
private const val ANKLE_CLEARANCE = 0.034f
private const val FLOOR_Y = 0.88f

private fun degToRad(deg: Float): Float = (deg * PI / 180.0).toFloat()

private fun smoothCycle(phase: Float): Float {
    // 0 -> 1 -> 0 with gentle dwell at endpoints
    val raw = ((1f - cos(phase * 2f * PI.toFloat())) * 0.5f).coerceIn(0f, 1f)
    return raw * raw * (3f - 2f * raw)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun lerpVec(a: Vec2, b: Vec2, t: Float): Vec2 = Vec2(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

private fun forwardPt(origin: Vec2, angleDegFromRightDown: Float, len: Float): Vec2 {
    val rad = degToRad(angleDegFromRightDown)
    return Vec2(origin.x + cos(rad) * len, origin.y + sin(rad) * len)
}

/**
 * Exact 2-bone inverse kinematics preserving rigid anatomical segment lengths.
 * Screen coordinate system: +x right, +y down.
 */
private fun solveTwoBoneIk(
    root: Vec2,
    target: Vec2,
    len1: Float,
    len2: Float,
    bendSign: Float,
): Pair<Vec2, Vec2> {
    val dx = target.x - root.x
    val dy = target.y - root.y
    val rawDist = hypot(dx, dy).coerceAtLeast(0.001f)
    val minD = kotlin.math.abs(len1 - len2) + 0.004f
    val maxD = (len1 + len2) - 0.002f
    val clampedDist = rawDist.coerceIn(minD, maxD)
    val cosA = ((len1 * len1 + clampedDist * clampedDist - len2 * len2) / (2f * len1 * clampedDist)).coerceIn(-1f, 1f)
    val baseAngle = atan2(dy, dx)
    val jointAngle = baseAngle + bendSign * acos(cosA)
    val joint = Vec2(
        root.x + cos(jointAngle) * len1,
        root.y + sin(jointAngle) * len1,
    )
    val endAngle = atan2(target.y - joint.y, target.x - joint.x)
    val end = Vec2(
        joint.x + cos(endAngle) * len2,
        joint.y + sin(endAngle) * len2,
    )
    return joint to end
}

private fun makeSideFoot(ankle: Vec2, footAngleDeg: Float = 0f): Pair<Vec2, Vec2> {
    val rad = degToRad(footAngleDeg)
    val heel = Vec2(ankle.x - cos(rad) * 0.022f, ankle.y + ANKLE_CLEARANCE - sin(rad) * 0.012f)
    val toe = Vec2(ankle.x + cos(rad) * 0.068f, ankle.y + ANKLE_CLEARANCE + sin(rad) * 0.036f)
    return heel to toe
}

/**
 * Evaluates a smooth, anatomically rigid pose for any exercise at normalized loop phase [0f..1f).
 */
fun evaluateExercisePose(
    type: ExerciseAnimationType,
    phase: Float,
    gender: SilhouetteGender,
): BodySkeletonPose = when (type) {
    ExerciseAnimationType.GENTLE_WALK -> evalWalking(phase, gender, brisk = false)
    ExerciseAnimationType.BRISK_MARCH -> evalWalking(phase, gender, brisk = true)
    ExerciseAnimationType.SEATED_ANKLE_SHOULDER -> evalSeatedMobility(phase, gender)
    ExerciseAnimationType.CHAIR_SIT_TO_STAND -> evalChairSitToStand(phase, gender)
    ExerciseAnimationType.BODYWEIGHT_SQUAT -> evalSquat(phase, gender)
    ExerciseAnimationType.STEP_UPS -> evalStepUps(phase, gender)
    ExerciseAnimationType.PLANK_HOLD -> evalPlank(phase, gender)
    ExerciseAnimationType.PUSH_UPS -> evalPushUps(phase, gender)
    ExerciseAnimationType.WALL_PUSH_UPS -> evalWallPushUps(phase, gender)
    ExerciseAnimationType.PIKE_SHOULDER_PRESS -> evalPikeShoulderPress(phase, gender)
    ExerciseAnimationType.SPLIT_LUNGE -> evalSplitLunge(phase, gender)
    ExerciseAnimationType.GLUTE_BRIDGE -> evalGluteBridge(phase, gender)
    ExerciseAnimationType.BREATH_WIND_DOWN -> evalBreathWindDown(phase, gender)
    ExerciseAnimationType.NECK_BACK_STRETCH -> evalNeckBackStretch(phase, gender)
    ExerciseAnimationType.JUMPING_JACKS -> evalJumpingJacks(phase, gender)
    ExerciseAnimationType.BICEP_CURL -> evalBicepCurl(phase, gender)
    ExerciseAnimationType.SHOULDER_PRESS -> evalShoulderPress(phase, gender)
    ExerciseAnimationType.SIT_UPS -> evalSitUps(phase, gender)
    ExerciseAnimationType.CALF_RAISES -> evalCalfRaises(phase, gender)
}

private fun buildSideSpineAndHead(
    pelvis: Vec2,
    torsoDegFromUpRight: Float, // 0 = straight up, +25 = leaning forward right
    headTiltDeg: Float = 0f,
    breathingExpand: Float = 0f,
): Triple<Vec2, Vec2, Vec2> {
    // Up is -90 deg
    val angle = -90f + torsoDegFromUpRight
    val shoulder = forwardPt(pelvis, angle, TORSO_LEN)
    val neckBase = lerpVec(pelvis, shoulder, 0.96f)
    val headCenter = forwardPt(shoulder, angle + headTiltDeg * 0.5f, NECK_LEN + HEAD_RADIUS * 0.92f)
    return Triple(shoulder, neckBase, headCenter)
}

private fun evalWalking(phase: Float, gender: SilhouetteGender, brisk: Boolean): BodySkeletonPose {
    val rad = phase * 2f * PI.toFloat()
    val swingA = sin(rad)
    val swingB = sin(rad + PI.toFloat())
    val bounce = kotlin.math.abs(cos(rad)) * (if (brisk) 0.014f else 0.008f)

    val hipBaseY = FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN - THIGH_LEN + 0.012f - bounce
    val pelvis = Vec2(0.48f, hipBaseY)
    val lean = if (brisk) 7f else 3f
    val (shoulder, neckBase, headCenter) = buildSideSpineAndHead(pelvis, lean)

    // Legs in side gait
    val strideX = if (brisk) 0.135f else 0.105f
    val liftMax = if (brisk) 0.068f else 0.036f

    fun legTargets(s: Float, cosVal: Float): Triple<Vec2, Vec2, Float> {
        val ankleX = pelvis.x + s * strideX
        val lift = (-cosVal).coerceAtLeast(0f) * liftMax
        val ankleY = (FLOOR_Y - ANKLE_CLEARANCE) - lift
        val footTilt = if (lift > 0.01f) (s * 14f) else (s * 8f).coerceAtLeast(-6f)
        return Triple(Vec2(ankleX, ankleY), Vec2(pelvis.x, pelvis.y), footTilt)
    }

    val nearCos = cos(rad)
    val farCos = cos(rad + PI.toFloat())
    val (nearAnkleT, _, nearFootTilt) = legTargets(swingA, nearCos)
    val (farAnkleT, _, farFootTilt) = legTargets(swingB, farCos)

    val (nearKnee, nearAnkle) = solveTwoBoneIk(pelvis, nearAnkleT, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (farKnee, farAnkle) = solveTwoBoneIk(pelvis, farAnkleT, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (nearHeel, nearToe) = makeSideFoot(nearAnkle, nearFootTilt)
    val (farHeel, farToe) = makeSideFoot(farAnkle, farFootTilt)

    // Arms swing in opposition (near arm swings opposite near leg)
    val armAmp = if (brisk) 34f else 22f
    val nearUpperAngle = 90f + swingB * armAmp
    val nearElbowBend = if (brisk) 82f else 24f + (-swingB).coerceAtLeast(0f) * 18f
    val farUpperAngle = 90f + swingA * armAmp
    val farElbowBend = if (brisk) 82f else 24f + (-swingA).coerceAtLeast(0f) * 18f

    val nearShoulder = Vec2(shoulder.x - 0.004f, shoulder.y + 0.010f)
    val nearElbow = forwardPt(nearShoulder, nearUpperAngle, UPPER_ARM_LEN)
    val nearWrist = forwardPt(nearElbow, nearUpperAngle - nearElbowBend, FOREARM_LEN)
    val nearHandTip = forwardPt(nearWrist, nearUpperAngle - nearElbowBend, HAND_LEN)

    val farShoulder = Vec2(shoulder.x + 0.008f, shoulder.y + 0.008f)
    val farElbow = forwardPt(farShoulder, farUpperAngle, UPPER_ARM_LEN)
    val farWrist = forwardPt(farElbow, farUpperAngle - farElbowBend, FOREARM_LEN)
    val farHandTip = forwardPt(farWrist, farUpperAngle - farElbowBend, HAND_LEN)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.72f),
        waistCenter = lerpVec(pelvis, shoulder, 0.40f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = farShoulder,
        farElbow = farElbow,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = pelvis,
        nearKnee = nearKnee,
        nearAnkle = nearAnkle,
        nearHeel = nearHeel,
        nearToe = nearToe,
        farHip = pelvis,
        farKnee = farKnee,
        farAnkle = farAnkle,
        farHeel = farHeel,
        farToe = farToe,
        floorY = FLOOR_Y,
        torsoLeanDeg = lean,
    )
}

private fun evalSeatedMobility(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val rad = phase * 2f * PI.toFloat()
    val seatY = FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN + 0.010f
    val pelvis = Vec2(0.40f, seatY - 0.020f)
    val rollY = sin(rad) * 0.015f
    val rollX = cos(rad) * 0.013f
    val (shoulderBase, neckBase, headCenter) = buildSideSpineAndHead(pelvis, torsoDegFromUpRight = 2f)
    val nearShoulder = Vec2(shoulderBase.x + rollX, shoulderBase.y + 0.012f + rollY)
    val farShoulder = Vec2(shoulderBase.x - rollX * 0.5f + 0.01f, shoulderBase.y + 0.010f - rollY * 0.5f)

    // Seated thighs horizontal, one foot planted, one ankle extended performing circular rotation
    val farAnkleTarget = Vec2(pelvis.x + THIGH_LEN * 0.98f, FLOOR_Y - ANKLE_CLEARANCE)
    val (farKnee, farAnkle) = solveTwoBoneIk(pelvis, farAnkleTarget, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (farHeel, farToe) = makeSideFoot(farAnkle, 0f)

    val nearAnkleTarget = Vec2(pelvis.x + THIGH_LEN + 0.068f, FLOOR_Y - ANKLE_CLEARANCE - 0.068f)
    val (nearKnee, nearAnkle) = solveTwoBoneIk(pelvis, nearAnkleTarget, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val ankleCircleAngle = sin(rad * 2f) * 26f
    val (nearHeel, nearToe) = makeSideFoot(nearAnkle, ankleCircleAngle)

    // Relaxed arms resting cleanly near thighs while rolling shoulders
    val nearElbow = forwardPt(nearShoulder, 98f, UPPER_ARM_LEN)
    val nearWrist = forwardPt(nearElbow, 28f, FOREARM_LEN)
    val nearHandTip = forwardPt(nearWrist, 24f, HAND_LEN)

    val farElbow = forwardPt(farShoulder, 95f, UPPER_ARM_LEN)
    val farWrist = forwardPt(farElbow, 25f, FOREARM_LEN)
    val farHandTip = forwardPt(farWrist, 22f, HAND_LEN)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulderBase, 0.72f),
        waistCenter = lerpVec(pelvis, shoulderBase, 0.40f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = farShoulder,
        farElbow = farElbow,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = pelvis,
        nearKnee = nearKnee,
        nearAnkle = nearAnkle,
        nearHeel = nearHeel,
        nearToe = nearToe,
        farHip = pelvis,
        farKnee = farKnee,
        farAnkle = farAnkle,
        farHeel = farHeel,
        farToe = farToe,
        propType = SilhouettePropType.CHAIR,
        propPrimaryY = seatY,
        propSecondaryX = 0.31f,
        floorY = FLOOR_Y,
    )
}

private fun evalChairSitToStand(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    // t = 0 seated, t = 1 standing tall
    val t = smoothCycle(phase)
    val seatY = 0.65f
    val ankleX = 0.56f
    val anklePos = Vec2(ankleX, FLOOR_Y - ANKLE_CLEARANCE)
    val (heel, toe) = makeSideFoot(anklePos, 0f)

    val seatedHip = Vec2(0.40f, seatY - 0.025f)
    val standingHip = Vec2(ankleX - 0.015f, FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN - THIGH_LEN + 0.005f)
    val rawHip = lerpVec(seatedHip, standingHip, t)

    // Forward lean peaks during transition (t ~ 0.45) for realistic sit-to-stand biomechanics
    val transitionPeak = sin(t * PI.toFloat())
    val lean = lerp(5f, 2f, t) + transitionPeak * 24f

    val (knee, ankle) = solveTwoBoneIk(rawHip, anklePos, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (shoulder, neckBase, headCenter) = buildSideSpineAndHead(rawHip, lean)

    // Arms reach gently forward during stand-up, lower cleanly to sides when standing
    val armLift = transitionPeak * 58f + (1f - t) * 18f
    val nearShoulder = Vec2(shoulder.x, shoulder.y + 0.010f)
    val nearElbow = forwardPt(nearShoulder, 90f - armLift, UPPER_ARM_LEN)
    val nearWrist = forwardPt(nearElbow, 90f - armLift - 12f, FOREARM_LEN)
    val nearHandTip = forwardPt(nearWrist, 90f - armLift - 12f, HAND_LEN)

    val farShoulder = Vec2(shoulder.x + 0.01f, shoulder.y + 0.008f)
    val farElbow = forwardPt(farShoulder, 90f - armLift * 0.92f, UPPER_ARM_LEN)
    val farWrist = forwardPt(farElbow, 90f - armLift * 0.92f - 10f, FOREARM_LEN)
    val farHandTip = forwardPt(farWrist, 90f - armLift * 0.92f - 10f, HAND_LEN)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(rawHip, shoulder, 0.72f),
        waistCenter = lerpVec(rawHip, shoulder, 0.40f),
        pelvisCenter = rawHip,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = farShoulder,
        farElbow = farElbow,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = rawHip,
        nearKnee = knee,
        nearAnkle = ankle,
        nearHeel = heel,
        nearToe = toe,
        farHip = Vec2(rawHip.x + 0.01f, rawHip.y),
        farKnee = Vec2(knee.x + 0.012f, knee.y),
        farAnkle = Vec2(ankle.x + 0.012f, ankle.y),
        farHeel = Vec2(heel.x + 0.012f, heel.y),
        farToe = Vec2(toe.x + 0.012f, toe.y),
        propType = SilhouettePropType.CHAIR,
        propPrimaryY = seatY,
        propSecondaryX = 0.30f,
        floorY = FLOOR_Y,
        torsoLeanDeg = lean,
    )
}

private fun evalSquat(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 standing, 1 deep squat
    val anklePos = Vec2(0.51f, FLOOR_Y - ANKLE_CLEARANCE)
    val (heel, toe) = makeSideFoot(anklePos, 0f)

    val standHip = Vec2(0.495f, FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN - THIGH_LEN + 0.005f)
    val squatHip = Vec2(0.395f, FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN + 0.015f)
    val pelvis = lerpVec(standHip, squatHip, t)
    val lean = lerp(3f, 29f, t)

    val (knee, ankle) = solveTwoBoneIk(pelvis, anklePos, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (shoulder, neckBase, headCenter) = buildSideSpineAndHead(pelvis, lean, headTiltDeg = -lean * 0.45f)

    // Arms raise smoothly straight forward at bottom for balance
    val armAngle = lerp(92f, 8f, t)
    val elbowBend = lerp(10f, 6f, t)
    val nearShoulder = Vec2(shoulder.x, shoulder.y + 0.010f)
    val nearElbow = forwardPt(nearShoulder, armAngle, UPPER_ARM_LEN)
    val nearWrist = forwardPt(nearElbow, armAngle - elbowBend, FOREARM_LEN)
    val nearHandTip = forwardPt(nearWrist, armAngle - elbowBend, HAND_LEN)

    val farShoulder = Vec2(shoulder.x + 0.010f, shoulder.y + 0.007f)
    val farElbow = forwardPt(farShoulder, armAngle - 3f, UPPER_ARM_LEN)
    val farWrist = forwardPt(farElbow, armAngle - 3f - elbowBend, FOREARM_LEN)
    val farHandTip = forwardPt(farWrist, armAngle - 3f - elbowBend, HAND_LEN)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.72f),
        waistCenter = lerpVec(pelvis, shoulder, 0.40f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = farShoulder,
        farElbow = farElbow,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = pelvis,
        nearKnee = knee,
        nearAnkle = ankle,
        nearHeel = heel,
        nearToe = toe,
        farHip = Vec2(pelvis.x + 0.010f, pelvis.y),
        farKnee = Vec2(knee.x + 0.014f, knee.y),
        farAnkle = Vec2(ankle.x + 0.014f, ankle.y),
        farHeel = Vec2(heel.x + 0.014f, heel.y),
        farToe = Vec2(toe.x + 0.014f, toe.y),
        floorY = FLOOR_Y,
        torsoLeanDeg = lean,
    )
}

private fun evalStepUps(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    // 0..0.35 lift lead foot onto step -> 0.35..0.65 drive body up on step -> 0.65..1.0 step down smoothly
    val stepBoxTopY = 0.76f
    val stepBoxX = 0.52f
    val leadWeight = smoothCycle(phase) // 0 floor, 1 standing on box

    val groundAnkle = Vec2(0.41f, FLOOR_Y - ANKLE_CLEARANCE)
    val boxAnkle = Vec2(0.58f, stepBoxTopY - ANKLE_CLEARANCE)

    // Near foot steps onto box quickly in first half, stays planted while body rises
    val footPlaceT = (phase * 2.4f).coerceIn(0f, 1f)
    val footBackT = ((1f - phase) * 2.4f).coerceIn(0f, 1f)
    val nearAnkleTarget = lerpVec(groundAnkle, boxAnkle, minOf(footPlaceT, footBackT))

    val pelvisStandGround = Vec2(0.42f, FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN - THIGH_LEN + 0.005f)
    val pelvisStandBox = Vec2(0.57f, stepBoxTopY - ANKLE_CLEARANCE - SHIN_LEN - THIGH_LEN + 0.005f)
    val pelvis = lerpVec(pelvisStandGround, pelvisStandBox, leadWeight)

    val farAnkleTarget = lerpVec(
        Vec2(0.40f, FLOOR_Y - ANKLE_CLEARANCE),
        Vec2(0.55f, stepBoxTopY - ANKLE_CLEARANCE - 0.015f),
        leadWeight,
    )

    val (nearKnee, nearAnkle) = solveTwoBoneIk(pelvis, nearAnkleTarget, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (farKnee, farAnkle) = solveTwoBoneIk(pelvis, farAnkleTarget, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (nearHeel, nearToe) = makeSideFoot(nearAnkle, 0f)
    val (farHeel, farToe) = makeSideFoot(farAnkle, 0f)

    val lean = sin(leadWeight * PI.toFloat()) * 12f + 4f
    val (shoulder, neckBase, headCenter) = buildSideSpineAndHead(pelvis, lean)

    val armSwing = sin(phase * 2f * PI.toFloat()) * 24f
    val nearShoulder = Vec2(shoulder.x, shoulder.y + 0.010f)
    val nearElbow = forwardPt(nearShoulder, 90f - armSwing, UPPER_ARM_LEN)
    val nearWrist = forwardPt(nearElbow, 42f - armSwing, FOREARM_LEN)
    val nearHandTip = forwardPt(nearWrist, 40f - armSwing, HAND_LEN)

    val farShoulder = Vec2(shoulder.x + 0.008f, shoulder.y + 0.008f)
    val farElbow = forwardPt(farShoulder, 90f + armSwing, UPPER_ARM_LEN)
    val farWrist = forwardPt(farElbow, 42f + armSwing, FOREARM_LEN)
    val farHandTip = forwardPt(farWrist, 40f + armSwing, HAND_LEN)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.72f),
        waistCenter = lerpVec(pelvis, shoulder, 0.40f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = farShoulder,
        farElbow = farElbow,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = pelvis,
        nearKnee = nearKnee,
        nearAnkle = nearAnkle,
        nearHeel = nearHeel,
        nearToe = nearToe,
        farHip = pelvis,
        farKnee = farKnee,
        farAnkle = farAnkle,
        farHeel = farHeel,
        farToe = farToe,
        propType = SilhouettePropType.STEP_BOX,
        propPrimaryY = stepBoxTopY,
        propSecondaryX = stepBoxX,
        floorY = FLOOR_Y,
        torsoLeanDeg = lean,
    )
}

private fun evalPlank(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    // Static forearm plank with subtle, natural breathing movement
    val breath = sin(phase * 2f * PI.toFloat()) * 0.0045f
    val matY = 0.84f
    val ankle = Vec2(0.185f, matY - 0.032f)
    val heel = Vec2(ankle.x - 0.015f, ankle.y - 0.018f)
    val toe = Vec2(ankle.x + 0.006f, matY)

    val bodyAngleDeg = -14.8f
    val knee = forwardPt(ankle, bodyAngleDeg, SHIN_LEN)
    val pelvis = forwardPt(knee, bodyAngleDeg, THIGH_LEN)
    val shoulder = forwardPt(pelvis, bodyAngleDeg, TORSO_LEN)
    val shoulderWithBreath = Vec2(shoulder.x, shoulder.y - breath)
    val nearShoulder = Vec2(shoulderWithBreath.x, shoulderWithBreath.y + 0.008f)
    val neckBase = lerpVec(pelvis, shoulderWithBreath, 0.96f)
    val headCenter = forwardPt(shoulderWithBreath, bodyAngleDeg + 14f, NECK_LEN + HEAD_RADIUS * 0.88f)

    // Forearms planted neatly under shoulders preserving rigid UPPER_ARM_LEN
    val elbowY = matY - 0.012f
    val dy = (elbowY - nearShoulder.y).coerceIn(-UPPER_ARM_LEN + 0.005f, UPPER_ARM_LEN - 0.005f)
    val dx = kotlin.math.sqrt((UPPER_ARM_LEN * UPPER_ARM_LEN - dy * dy).coerceAtLeast(0f))
    val nearElbowTarget = Vec2(nearShoulder.x + dx * 0.18f, nearShoulder.y + kotlin.math.sqrt((UPPER_ARM_LEN * UPPER_ARM_LEN - (dx * 0.18f) * (dx * 0.18f)).coerceAtLeast(0f)))
    val nearWrist = Vec2(nearElbowTarget.x + FOREARM_LEN * 0.98f, nearElbowTarget.y - 0.004f)
    val nearHandTip = Vec2(nearWrist.x + HAND_LEN * 0.92f, nearWrist.y)

    val farElbowTarget = Vec2(nearElbowTarget.x + 0.020f, nearElbowTarget.y - 0.003f)
    val farWrist = Vec2(farElbowTarget.x + FOREARM_LEN * 0.95f, farElbowTarget.y - 0.004f)
    val farHandTip = Vec2(farWrist.x + HAND_LEN * 0.9f, farWrist.y)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulderWithBreath, 0.72f),
        waistCenter = lerpVec(pelvis, shoulderWithBreath, 0.42f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbowTarget,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = Vec2(shoulderWithBreath.x + 0.012f, shoulderWithBreath.y + 0.004f),
        farElbow = farElbowTarget,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = pelvis,
        nearKnee = knee,
        nearAnkle = ankle,
        nearHeel = heel,
        nearToe = toe,
        farHip = pelvis,
        farKnee = Vec2(knee.x + 0.008f, knee.y - 0.004f),
        farAnkle = Vec2(ankle.x + 0.008f, ankle.y - 0.004f),
        farHeel = Vec2(heel.x + 0.008f, heel.y - 0.004f),
        farToe = Vec2(toe.x + 0.008f, toe.y),
        propType = SilhouettePropType.FLOOR_MAT,
        propPrimaryY = matY,
        floorY = matY,
        breathingExpand = breath,
    )
}

private fun evalPushUps(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 top, 1 lowered chest
    val matY = 0.85f
    val ankle = Vec2(0.18f, matY - 0.032f)
    val heel = Vec2(ankle.x - 0.015f, ankle.y - 0.018f)
    val toe = Vec2(ankle.x + 0.006f, matY)

    val bodyAngleDeg = lerp(-23.5f, -7.5f, t)
    val knee = forwardPt(ankle, bodyAngleDeg, SHIN_LEN)
    val pelvis = forwardPt(knee, bodyAngleDeg, THIGH_LEN)
    val shoulder = forwardPt(pelvis, bodyAngleDeg, TORSO_LEN)
    val neckBase = lerpVec(pelvis, shoulder, 0.96f)
    val headCenter = forwardPt(shoulder, bodyAngleDeg + 16f, NECK_LEN + HEAD_RADIUS * 0.88f)

    val handPlant = Vec2(0.755f, matY - 0.008f)
    val nearShoulder = Vec2(shoulder.x, shoulder.y + 0.008f)
    val (nearElbow, nearWrist) = solveTwoBoneIk(nearShoulder, handPlant, UPPER_ARM_LEN, FOREARM_LEN, bendSign = +1f)
    val nearHandTip = Vec2(nearWrist.x + HAND_LEN * 0.88f, matY - 0.004f)

    val farShoulder = Vec2(shoulder.x + 0.012f, shoulder.y + 0.004f)
    val farHandPlant = Vec2(handPlant.x + 0.018f, handPlant.y - 0.004f)
    val (farElbow, farWrist) = solveTwoBoneIk(farShoulder, farHandPlant, UPPER_ARM_LEN, FOREARM_LEN, bendSign = +1f)
    val farHandTip = Vec2(farWrist.x + HAND_LEN * 0.85f, farHandPlant.y)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.72f),
        waistCenter = lerpVec(pelvis, shoulder, 0.42f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = farShoulder,
        farElbow = farElbow,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = pelvis,
        nearKnee = knee,
        nearAnkle = ankle,
        nearHeel = heel,
        nearToe = toe,
        farHip = pelvis,
        farKnee = Vec2(knee.x + 0.008f, knee.y - 0.004f),
        farAnkle = Vec2(ankle.x + 0.008f, ankle.y - 0.004f),
        farHeel = Vec2(heel.x + 0.008f, heel.y - 0.004f),
        farToe = Vec2(toe.x + 0.008f, toe.y),
        propType = SilhouettePropType.FLOOR_MAT,
        propPrimaryY = matY,
        floorY = matY,
    )
}

private fun evalWallPushUps(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 arms extended, 1 chest closer to wall
    val wallX = 0.78f
    val anklePos = Vec2(0.37f, FLOOR_Y - ANKLE_CLEARANCE)
    val (heel, toe) = makeSideFoot(anklePos, -5f)

    // Body pivots slightly forward around ankles from 11 deg to 24 deg lean toward wall
    val bodyAngleDeg = lerp(-90f + 11f, -90f + 24f, t)
    val knee = forwardPt(anklePos, bodyAngleDeg, SHIN_LEN)
    val pelvis = forwardPt(knee, bodyAngleDeg, THIGH_LEN)
    val shoulder = forwardPt(pelvis, bodyAngleDeg, TORSO_LEN)
    val neckBase = lerpVec(pelvis, shoulder, 0.96f)
    val headCenter = forwardPt(shoulder, bodyAngleDeg + 4f, NECK_LEN + HEAD_RADIUS * 0.92f)

    val wallPalm = Vec2(wallX - 0.01f, shoulder.y + 0.02f)
    val nearShoulder = Vec2(shoulder.x, shoulder.y + 0.010f)
    val (nearElbow, nearWrist) = solveTwoBoneIk(nearShoulder, wallPalm, UPPER_ARM_LEN, FOREARM_LEN, bendSign = +1f)
    val nearHandTip = Vec2(wallX - 0.004f, nearWrist.y - HAND_LEN * 0.82f)

    val farShoulder = Vec2(shoulder.x + 0.008f, shoulder.y + 0.006f)
    val farPalm = Vec2(wallX - 0.006f, wallPalm.y - 0.015f)
    val (farElbow, farWrist) = solveTwoBoneIk(farShoulder, farPalm, UPPER_ARM_LEN, FOREARM_LEN, bendSign = +1f)
    val farHandTip = Vec2(wallX - 0.002f, farWrist.y - HAND_LEN * 0.8f)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.72f),
        waistCenter = lerpVec(pelvis, shoulder, 0.40f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = farShoulder,
        farElbow = farElbow,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = pelvis,
        nearKnee = knee,
        nearAnkle = anklePos,
        nearHeel = heel,
        nearToe = toe,
        farHip = pelvis,
        farKnee = Vec2(knee.x + 0.010f, knee.y),
        farAnkle = Vec2(anklePos.x + 0.010f, anklePos.y),
        farHeel = Vec2(heel.x + 0.010f, heel.y),
        farToe = Vec2(toe.x + 0.010f, toe.y),
        propType = SilhouettePropType.WALL,
        propSecondaryX = wallX,
        floorY = FLOOR_Y,
    )
}

private fun evalPikeShoulderPress(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 high pike, 1 head dips toward floor
    val matY = 0.85f
    val ankle = Vec2(0.24f, matY - 0.025f)
    val heel = Vec2(ankle.x - 0.012f, ankle.y - 0.014f)
    val toe = Vec2(ankle.x + 0.014f, matY)

    // Inverted V: hips elevated cleanly at leg length ~0.358 from ankle
    val legAngle = -56f
    val knee = forwardPt(ankle, legAngle - 3f, SHIN_LEN)
    val pelvis = forwardPt(knee, legAngle + 3f, THIGH_LEN)

    val torsoAngle = lerp(52f, 67f, t)
    val shoulder = forwardPt(pelvis, torsoAngle, TORSO_LEN)
    val neckBase = lerpVec(pelvis, shoulder, 0.96f)
    val headCenter = forwardPt(shoulder, torsoAngle + 15f, NECK_LEN + HEAD_RADIUS * 0.88f)

    val handPlant = Vec2(0.74f, matY - 0.008f)
    val nearShoulder = Vec2(shoulder.x, shoulder.y + 0.006f)
    val (nearElbow, nearWrist) = solveTwoBoneIk(nearShoulder, handPlant, UPPER_ARM_LEN, FOREARM_LEN, bendSign = +1f)
    val nearHandTip = Vec2(nearWrist.x + HAND_LEN * 0.85f, matY - 0.004f)

    val farShoulder = Vec2(shoulder.x + 0.012f, shoulder.y)
    val (farElbow, farWrist) = solveTwoBoneIk(farShoulder, Vec2(handPlant.x + 0.015f, handPlant.y - 0.004f), UPPER_ARM_LEN, FOREARM_LEN, bendSign = +1f)
    val farHandTip = Vec2(farWrist.x + HAND_LEN * 0.85f, matY - 0.006f)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.72f),
        waistCenter = lerpVec(pelvis, shoulder, 0.42f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = farShoulder,
        farElbow = farElbow,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = pelvis,
        nearKnee = knee,
        nearAnkle = ankle,
        nearHeel = heel,
        nearToe = toe,
        farHip = pelvis,
        farKnee = Vec2(knee.x + 0.012f, knee.y),
        farAnkle = Vec2(ankle.x + 0.012f, ankle.y),
        farHeel = Vec2(heel.x + 0.012f, heel.y),
        farToe = Vec2(toe.x + 0.012f, toe.y),
        propType = SilhouettePropType.FLOOR_MAT,
        propPrimaryY = matY,
        floorY = matY,
    )
}

private fun evalSplitLunge(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 top split stance, 1 bottom 90/90 lunge
    val frontAnkle = Vec2(0.64f, FLOOR_Y - ANKLE_CLEARANCE)
    val (frontHeel, frontToe) = makeSideFoot(frontAnkle, 0f)

    val backAnklePos = Vec2(0.30f, FLOOR_Y - ANKLE_CLEARANCE - lerp(0.01f, 0.025f, t))
    val backHeel = Vec2(backAnklePos.x - 0.016f, backAnklePos.y - 0.012f)
    val backToe = Vec2(backAnklePos.x + 0.020f, FLOOR_Y)

    val hipTop = Vec2(0.47f, FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN - THIGH_LEN + 0.048f)
    val hipBottom = Vec2(0.455f, FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN + 0.015f)
    val pelvis = lerpVec(hipTop, hipBottom, t)

    val (nearKnee, nearAnkle) = solveTwoBoneIk(pelvis, frontAnkle, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (farKnee, farAnkle) = solveTwoBoneIk(pelvis, backAnklePos, THIGH_LEN, SHIN_LEN, bendSign = -1f)

    val lean = lerp(4f, 9f, t)
    val (shoulder, neckBase, headCenter) = buildSideSpineAndHead(pelvis, lean)

    // Balanced athletic arms: hands near waist/hips or slight forward poise
    val nearShoulder = Vec2(shoulder.x, shoulder.y + 0.010f)
    val nearElbow = forwardPt(nearShoulder, 114f, UPPER_ARM_LEN)
    val nearWrist = forwardPt(nearElbow, 42f, FOREARM_LEN * 0.92f)
    val nearHandTip = forwardPt(nearWrist, 36f, HAND_LEN)

    val farShoulder = Vec2(shoulder.x + 0.008f, shoulder.y + 0.008f)
    val farElbow = forwardPt(farShoulder, 82f - t * 22f, UPPER_ARM_LEN)
    val farWrist = forwardPt(farElbow, 35f - t * 18f, FOREARM_LEN)
    val farHandTip = forwardPt(farWrist, 30f - t * 18f, HAND_LEN)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.72f),
        waistCenter = lerpVec(pelvis, shoulder, 0.40f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = farShoulder,
        farElbow = farElbow,
        farWrist = farWrist,
        farHandTip = farHandTip,
        nearHip = pelvis,
        nearKnee = nearKnee,
        nearAnkle = nearAnkle,
        nearHeel = frontHeel,
        nearToe = frontToe,
        farHip = pelvis,
        farKnee = farKnee,
        farAnkle = farAnkle,
        farHeel = backHeel,
        farToe = backToe,
        floorY = FLOOR_Y,
        torsoLeanDeg = lean,
    )
}

private fun evalGluteBridge(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 resting flat, 1 full bridge lift
    val matY = 0.85f
    val shoulder = Vec2(0.28f, matY - 0.032f)
    val neckBase = Vec2(shoulder.x - 0.015f, shoulder.y)
    val headCenter = Vec2(shoulder.x - NECK_LEN - HEAD_RADIUS * 0.95f, matY - HEAD_RADIUS * 0.95f)

    val pelvisAngleFromShoulder = lerp(0f, -31f, t)
    val pelvis = forwardPt(shoulder, pelvisAngleFromShoulder, TORSO_LEN)
    val ankleTarget = Vec2(0.755f, matY - ANKLE_CLEARANCE)
    val (knee, ankle) = solveTwoBoneIk(pelvis, ankleTarget, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (heel, toe) = makeSideFoot(ankle, 0f)

    // Arms flat along floor beside body
    val nearShoulder = Vec2(shoulder.x + 0.008f, shoulder.y + 0.006f)
    val nearElbow = Vec2(nearShoulder.x + UPPER_ARM_LEN, matY - 0.018f)
    val nearWrist = Vec2(nearElbow.x + FOREARM_LEN, matY - 0.014f)
    val nearHandTip = Vec2(nearWrist.x + HAND_LEN, matY - 0.012f)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.70f),
        waistCenter = lerpVec(pelvis, shoulder, 0.35f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = shoulder,
        farElbow = nearElbow,
        farWrist = nearWrist,
        farHandTip = nearHandTip,
        nearHip = pelvis,
        nearKnee = knee,
        nearAnkle = ankle,
        nearHeel = heel,
        nearToe = toe,
        farHip = pelvis,
        farKnee = Vec2(knee.x + 0.010f, knee.y + 0.005f),
        farAnkle = Vec2(ankle.x + 0.010f, ankle.y),
        farHeel = Vec2(heel.x + 0.010f, heel.y),
        farToe = Vec2(toe.x + 0.010f, toe.y),
        propType = SilhouettePropType.FLOOR_MAT,
        propPrimaryY = matY,
        floorY = matY,
    )
}

private fun evalSitUps(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 supine, 1 full sit-up
    val matY = 0.85f
    val pelvis = Vec2(0.45f, matY - 0.032f)
    val ankleTarget = Vec2(0.77f, matY - ANKLE_CLEARANCE)
    val (knee, ankle) = solveTwoBoneIk(pelvis, ankleTarget, THIGH_LEN, SHIN_LEN, bendSign = -1f)
    val (heel, toe) = makeSideFoot(ankle, 0f)

    val torsoAngle = lerp(-174f, -112f, t)
    val shoulder = forwardPt(pelvis, torsoAngle, TORSO_LEN)
    val neckBase = lerpVec(pelvis, shoulder, 0.96f)
    val headCenter = forwardPt(shoulder, torsoAngle + 14f, NECK_LEN + HEAD_RADIUS * 0.9f)

    // Hands resting gently near temples
    val nearShoulder = Vec2(shoulder.x, shoulder.y + 0.008f)
    val (nearElbow, nearWrist) = solveTwoBoneIk(
        nearShoulder,
        Vec2(headCenter.x + 0.02f, headCenter.y + 0.01f),
        UPPER_ARM_LEN,
        FOREARM_LEN,
        bendSign = -1f,
    )
    val nearHandTip = lerpVec(nearWrist, headCenter, 0.35f)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.72f),
        waistCenter = lerpVec(pelvis, shoulder, 0.40f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = shoulder,
        farElbow = nearElbow,
        farWrist = nearWrist,
        farHandTip = nearHandTip,
        nearHip = pelvis,
        nearKnee = knee,
        nearAnkle = ankle,
        nearHeel = heel,
        nearToe = toe,
        farHip = pelvis,
        farKnee = Vec2(knee.x + 0.010f, knee.y + 0.004f),
        farAnkle = Vec2(ankle.x + 0.010f, ankle.y),
        farHeel = Vec2(heel.x + 0.010f, heel.y),
        farToe = Vec2(toe.x + 0.010f, toe.y),
        propType = SilhouettePropType.FLOOR_MAT,
        propPrimaryY = matY,
        floorY = matY,
    )
}

private fun evalCalfRaises(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 flat foot, 1 up on balls of feet
    val lift = t * 0.042f
    val toeY = FLOOR_Y
    val ankle = Vec2(0.50f, FLOOR_Y - ANKLE_CLEARANCE - lift)
    val heel = Vec2(ankle.x - 0.022f, FLOOR_Y - lift * 0.95f)
    val toe = Vec2(ankle.x + 0.064f, toeY)

    val knee = Vec2(ankle.x + 0.004f, ankle.y - SHIN_LEN)
    val pelvis = Vec2(knee.x - 0.008f, knee.y - THIGH_LEN)
    val (shoulder, neckBase, headCenter) = buildSideSpineAndHead(pelvis, 2f)

    val nearShoulder = Vec2(shoulder.x, shoulder.y + 0.010f)
    val nearElbow = forwardPt(nearShoulder, 92f, UPPER_ARM_LEN)
    val nearWrist = forwardPt(nearElbow, 84f, FOREARM_LEN)
    val nearHandTip = forwardPt(nearWrist, 84f, HAND_LEN)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.SIDE,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        neckBase = neckBase,
        chestCenter = lerpVec(pelvis, shoulder, 0.72f),
        waistCenter = lerpVec(pelvis, shoulder, 0.40f),
        pelvisCenter = pelvis,
        nearShoulder = nearShoulder,
        nearElbow = nearElbow,
        nearWrist = nearWrist,
        nearHandTip = nearHandTip,
        farShoulder = nearShoulder,
        farElbow = nearElbow,
        farWrist = nearWrist,
        farHandTip = nearHandTip,
        nearHip = pelvis,
        nearKnee = knee,
        nearAnkle = ankle,
        nearHeel = heel,
        nearToe = toe,
        farHip = pelvis,
        farKnee = Vec2(knee.x + 0.010f, knee.y),
        farAnkle = Vec2(ankle.x + 0.010f, ankle.y),
        farHeel = Vec2(heel.x + 0.010f, heel.y),
        farToe = Vec2(toe.x + 0.010f, toe.y),
        floorY = FLOOR_Y,
    )
}

private fun buildFrontStandingBase(
    gender: SilhouetteGender,
    hipY: Float = FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN - THIGH_LEN + 0.006f,
    headTiltDeg: Float = 0f,
    breathingExpand: Float = 0f,
): BodySkeletonPose {
    val cx = 0.50f
    val shoulderHalfW = when (gender) {
        SilhouetteGender.MALE -> 0.095f
        SilhouetteGender.FEMALE -> 0.078f
        SilhouetteGender.NEUTRAL -> 0.086f
    } + breathingExpand * 0.5f
    val hipHalfW = when (gender) {
        SilhouetteGender.MALE -> 0.056f
        SilhouetteGender.FEMALE -> 0.064f
        SilhouetteGender.NEUTRAL -> 0.059f
    }

    val pelvis = Vec2(cx, hipY)
    val shoulderY = hipY - TORSO_LEN
    val chestCenter = Vec2(cx, lerp(hipY, shoulderY, 0.72f))
    val waistCenter = Vec2(cx, lerp(hipY, shoulderY, 0.42f))
    val neckBase = Vec2(cx, shoulderY - 0.008f)
    val headCenter = forwardPt(neckBase, -90f + headTiltDeg, NECK_LEN + HEAD_RADIUS * 0.90f)

    // Front view: near = screen right (+x), far = screen left (-x)
    val rShoulder = Vec2(cx + shoulderHalfW, shoulderY + 0.012f)
    val lShoulder = Vec2(cx - shoulderHalfW, shoulderY + 0.012f)
    val rHip = Vec2(cx + hipHalfW, pelvis.y + 0.01f)
    val lHip = Vec2(cx - hipHalfW, pelvis.y + 0.01f)

    val ankleY = FLOOR_Y - ANKLE_CLEARANCE
    val rKnee = Vec2(rHip.x + 0.004f, pelvis.y + THIGH_LEN)
    val lKnee = Vec2(lHip.x - 0.004f, pelvis.y + THIGH_LEN)
    val rAnkle = Vec2(rKnee.x + 0.004f, ankleY)
    val lAnkle = Vec2(lKnee.x - 0.004f, ankleY)

    return BodySkeletonPose(
        viewMode = SilhouetteViewMode.FRONT,
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        headTiltDeg = headTiltDeg,
        neckBase = neckBase,
        chestCenter = chestCenter,
        waistCenter = waistCenter,
        pelvisCenter = pelvis,
        nearShoulder = rShoulder,
        nearElbow = forwardPt(rShoulder, 82f, UPPER_ARM_LEN),
        nearWrist = forwardPt(forwardPt(rShoulder, 82f, UPPER_ARM_LEN), 86f, FOREARM_LEN),
        nearHandTip = forwardPt(forwardPt(forwardPt(rShoulder, 82f, UPPER_ARM_LEN), 86f, FOREARM_LEN), 86f, HAND_LEN),
        farShoulder = lShoulder,
        farElbow = forwardPt(lShoulder, 98f, UPPER_ARM_LEN),
        farWrist = forwardPt(forwardPt(lShoulder, 98f, UPPER_ARM_LEN), 94f, FOREARM_LEN),
        farHandTip = forwardPt(forwardPt(forwardPt(lShoulder, 98f, UPPER_ARM_LEN), 94f, FOREARM_LEN), 94f, HAND_LEN),
        nearHip = rHip,
        nearKnee = rKnee,
        nearAnkle = rAnkle,
        nearHeel = Vec2(rAnkle.x - 0.010f, FLOOR_Y),
        nearToe = Vec2(rAnkle.x + 0.022f, FLOOR_Y),
        farHip = lHip,
        farKnee = lKnee,
        farAnkle = lAnkle,
        farHeel = Vec2(lAnkle.x + 0.010f, FLOOR_Y),
        farToe = Vec2(lAnkle.x - 0.022f, FLOOR_Y),
        floorY = FLOOR_Y,
        breathingExpand = breathingExpand,
    )
}

private fun evalBreathWindDown(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    // Slow calming inhale/arm-float (0..0.42), calm hold, slow relaxing exhale lower
    val t = smoothCycle(phase)
    val base = buildFrontStandingBase(gender, breathingExpand = t * 0.010f)
    val armLift = t * 112f // floats smoothly up from sides to comfortable V overhead
    val rElbow = forwardPt(base.nearShoulder, 78f - armLift, UPPER_ARM_LEN)
    val rWrist = forwardPt(rElbow, 78f - armLift - 14f, FOREARM_LEN)
    val rHand = forwardPt(rWrist, 78f - armLift - 16f, HAND_LEN)

    val lElbow = forwardPt(base.farShoulder, 102f + armLift, UPPER_ARM_LEN)
    val lWrist = forwardPt(lElbow, 102f + armLift + 14f, FOREARM_LEN)
    val lHand = forwardPt(lWrist, 102f + armLift + 16f, HAND_LEN)

    return base.copy(
        nearElbow = rElbow,
        nearWrist = rWrist,
        nearHandTip = rHand,
        farElbow = lElbow,
        farWrist = lWrist,
        farHandTip = lHand,
    )
}

private fun evalNeckBackStretch(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    // Gentle lateral neck tilt + alternating shoulder/thoracic arm reach
    val rad = phase * 2f * PI.toFloat()
    val sway = sin(rad)
    val tiltDeg = sway * 18f
    val reachT = (1f - cos(rad * 2f)) * 0.5f // two gentle overhead reaches per cycle
    val base = buildFrontStandingBase(gender, headTiltDeg = tiltDeg)

    val rAngle = 76f - reachT * 124f
    val lAngle = 104f + reachT * 124f
    val rElbow = forwardPt(base.nearShoulder, rAngle, UPPER_ARM_LEN)
    val rWrist = forwardPt(rElbow, rAngle - 18f * reachT, FOREARM_LEN)
    val rHand = forwardPt(rWrist, rAngle - 20f * reachT, HAND_LEN)

    val lElbow = forwardPt(base.farShoulder, lAngle, UPPER_ARM_LEN)
    val lWrist = forwardPt(lElbow, lAngle + 18f * reachT, FOREARM_LEN)
    val lHand = forwardPt(lWrist, lAngle + 20f * reachT, HAND_LEN)

    return base.copy(
        nearElbow = rElbow,
        nearWrist = rWrist,
        nearHandTip = rHand,
        farElbow = lElbow,
        farWrist = lWrist,
        farHandTip = lHand,
    )
}

private fun evalJumpingJacks(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 closed stance, 1 wide star
    val jumpArc = sin(t * PI.toFloat()) * 0.022f
    val base = buildFrontStandingBase(
        gender,
        hipY = (FLOOR_Y - ANKLE_CLEARANCE - SHIN_LEN - THIGH_LEN + 0.006f) - jumpArc,
    )

    // Legs abduct smoothly outward
    val legSpreadDeg = t * 18f
    val rKnee = forwardPt(base.nearHip, 90f - legSpreadDeg, THIGH_LEN)
    val rAnkle = forwardPt(rKnee, 90f - legSpreadDeg * 0.9f, SHIN_LEN)
    val lKnee = forwardPt(base.farHip, 90f + legSpreadDeg, THIGH_LEN)
    val lAnkle = forwardPt(lKnee, 90f + legSpreadDeg * 0.9f, SHIN_LEN)

    // Arms sweep overhead into natural wide V
    val armSweep = t * 140f
    val rElbow = forwardPt(base.nearShoulder, 82f - armSweep, UPPER_ARM_LEN)
    val rWrist = forwardPt(rElbow, 82f - armSweep - 10f, FOREARM_LEN)
    val rHand = forwardPt(rWrist, 82f - armSweep - 10f, HAND_LEN)

    val lElbow = forwardPt(base.farShoulder, 98f + armSweep, UPPER_ARM_LEN)
    val lWrist = forwardPt(lElbow, 98f + armSweep + 10f, FOREARM_LEN)
    val lHand = forwardPt(lWrist, 98f + armSweep + 10f, HAND_LEN)

    return base.copy(
        nearElbow = rElbow,
        nearWrist = rWrist,
        nearHandTip = rHand,
        farElbow = lElbow,
        farWrist = lWrist,
        farHandTip = lHand,
        nearKnee = rKnee,
        nearAnkle = rAnkle,
        nearHeel = Vec2(rAnkle.x - 0.010f, rAnkle.y + ANKLE_CLEARANCE),
        nearToe = Vec2(rAnkle.x + 0.022f, rAnkle.y + ANKLE_CLEARANCE),
        farKnee = lKnee,
        farAnkle = lAnkle,
        farHeel = Vec2(lAnkle.x + 0.010f, lAnkle.y + ANKLE_CLEARANCE),
        farToe = Vec2(lAnkle.x - 0.022f, lAnkle.y + ANKLE_CLEARANCE),
    )
}

private fun evalBicepCurl(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 arms lowered, 1 forearms curled to shoulders
    val base = buildFrontStandingBase(gender)
    val rElbow = forwardPt(base.nearShoulder, 82f, UPPER_ARM_LEN)
    val rForearmTarget = lerpVec(
        forwardPt(rElbow, 86f, FOREARM_LEN),
        Vec2(base.nearShoulder.x + 0.008f, base.nearShoulder.y + 0.018f),
        t,
    )
    val rAngle = atan2(rForearmTarget.y - rElbow.y, rForearmTarget.x - rElbow.x) * 180f / PI.toFloat()
    val rWrist = forwardPt(rElbow, rAngle, FOREARM_LEN)
    val rHand = forwardPt(rWrist, rAngle, HAND_LEN)

    val lElbow = forwardPt(base.farShoulder, 98f, UPPER_ARM_LEN)
    val lForearmTarget = lerpVec(
        forwardPt(lElbow, 94f, FOREARM_LEN),
        Vec2(base.farShoulder.x - 0.008f, base.farShoulder.y + 0.018f),
        t,
    )
    val lAngle = atan2(lForearmTarget.y - lElbow.y, lForearmTarget.x - lElbow.x) * 180f / PI.toFloat()
    val lWrist = forwardPt(lElbow, lAngle, FOREARM_LEN)
    val lHand = forwardPt(lWrist, lAngle, HAND_LEN)

    return base.copy(
        nearElbow = rElbow,
        nearWrist = rWrist,
        nearHandTip = rHand,
        farElbow = lElbow,
        farWrist = lWrist,
        farHandTip = lHand,
    )
}

private fun evalShoulderPress(phase: Float, gender: SilhouetteGender): BodySkeletonPose {
    val t = smoothCycle(phase) // 0 rack at shoulders, 1 pressed overhead
    val base = buildFrontStandingBase(gender)
    val rTarget = lerpVec(
        Vec2(base.nearShoulder.x + 0.09f, base.nearShoulder.y - 0.06f),
        Vec2(base.nearShoulder.x + 0.025f, base.nearShoulder.y - (UPPER_ARM_LEN + FOREARM_LEN) * 0.88f),
        t,
    )
    val (rElbow, rWrist) = solveTwoBoneIk(base.nearShoulder, rTarget, UPPER_ARM_LEN, FOREARM_LEN, bendSign = +1f)
    val rHand = forwardPt(rWrist, -90f, HAND_LEN)

    val lTarget = lerpVec(
        Vec2(base.farShoulder.x - 0.09f, base.farShoulder.y - 0.06f),
        Vec2(base.farShoulder.x - 0.025f, base.farShoulder.y - (UPPER_ARM_LEN + FOREARM_LEN) * 0.88f),
        t,
    )
    val (lElbow, lWrist) = solveTwoBoneIk(base.farShoulder, lTarget, UPPER_ARM_LEN, FOREARM_LEN, bendSign = -1f)
    val lHand = forwardPt(lWrist, -90f, HAND_LEN)

    return base.copy(
        nearElbow = rElbow,
        nearWrist = rWrist,
        nearHandTip = rHand,
        farElbow = lElbow,
        farWrist = lWrist,
        farHandTip = lHand,
    )
}
