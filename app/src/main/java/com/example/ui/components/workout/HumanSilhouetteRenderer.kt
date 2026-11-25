package com.example.ui.components.workout

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Reusable pre-allocated Path pool so per-frame silhouette rendering creates zero Path garbage.
 */
class SilhouettePathPool {
    val torsoPath = Path()
    val headPath = Path()
    val farArmPath = Path()
    val nearArmPath = Path()
    val farLegPath = Path()
    val nearLegPath = Path()
    val farFootPath = Path()
    val nearFootPath = Path()
    val propPath = Path()
}

private data class GenderMorph(
    val chestForward: Float,
    val backCurve: Float,
    val waistFront: Float,
    val waistBack: Float,
    val hipForward: Float,
    val gluteCurve: Float,
    val shoulderFrontWidth: Float,
    val waistFrontWidth: Float,
    val hipFrontWidth: Float,
    val limbScale: Float,
    val headWidthScale: Float,
)

private fun genderMorphFor(gender: SilhouetteGender): GenderMorph = when (gender) {
    SilhouetteGender.MALE -> GenderMorph(
        chestForward = 0.044f,
        backCurve = 0.040f,
        waistFront = 0.032f,
        waistBack = 0.029f,
        hipForward = 0.035f,
        gluteCurve = 0.040f,
        shoulderFrontWidth = 0.025f,
        waistFrontWidth = 0.056f,
        hipFrontWidth = 0.064f,
        limbScale = 1.06f,
        headWidthScale = 0.90f,
    )
    SilhouetteGender.FEMALE -> GenderMorph(
        chestForward = 0.043f,
        backCurve = 0.034f,
        waistFront = 0.026f,
        waistBack = 0.024f,
        hipForward = 0.036f,
        gluteCurve = 0.047f,
        shoulderFrontWidth = 0.019f,
        waistFrontWidth = 0.045f,
        hipFrontWidth = 0.068f,
        limbScale = 0.93f,
        headWidthScale = 0.84f,
    )
    SilhouetteGender.NEUTRAL -> GenderMorph(
        chestForward = 0.041f,
        backCurve = 0.037f,
        waistFront = 0.029f,
        waistBack = 0.027f,
        hipForward = 0.035f,
        gluteCurve = 0.043f,
        shoulderFrontWidth = 0.022f,
        waistFrontWidth = 0.051f,
        hipFrontWidth = 0.066f,
        limbScale = 0.99f,
        headWidthScale = 0.87f,
    )
}

/**
 * Renders a complete, seamless, solid human silhouette (and minimal floor/prop reference)
 * using zero-stroke overlapping organic anatomical Bezier shapes.
 */
fun DrawScope.drawSolidHumanSilhouette(
    pose: BodySkeletonPose,
    gender: SilhouetteGender,
    silhouetteColor: Color,
    propColor: Color,
    floorColor: Color,
    paths: SilhouettePathPool,
    isThumbnail: Boolean = false,
) {
    val scale = minOf(size.width / 0.86f, size.height / 0.92f)
    val offsetX = size.width * 0.50f - 0.50f * scale
    val offsetY = size.height * 0.50f - 0.475f * scale

    fun px(nx: Float): Float = offsetX + nx * scale
    fun py(ny: Float): Float = offsetY + ny * scale
    fun p(v: Vec2): Offset = Offset(px(v.x), py(v.y))

    val morph = genderMorphFor(gender)

    // 1. Floor baseline & minimal exercise prop (behind silhouette)
    if (!isThumbnail) {
        val floorLineY = py(pose.floorY)
        drawLine(
            color = floorColor,
            start = Offset(px(0.08f), floorLineY),
            end = Offset(px(0.92f), floorLineY),
            strokeWidth = (1.5f * (scale / 220f)).coerceIn(1f, 2.5f),
            cap = StrokeCap.Round,
        )
    }

    drawMinimalProp(
        pose = pose,
        propColor = propColor,
        px = ::px,
        py = ::py,
        scale = scale,
        propPath = paths.propPath,
    )

    // Subtle ground shadow directly beneath the body
    if (!isThumbnail) {
        val minFootX = minOf(pose.nearToe.x, pose.nearHeel.x, pose.farToe.x, pose.farHeel.x, pose.pelvisCenter.x)
        val maxFootX = maxOf(pose.nearToe.x, pose.nearHeel.x, pose.farToe.x, pose.farHeel.x, pose.nearHandTip.x)
        val shadowCenterX = ((minFootX + maxFootX) * 0.5f).coerceIn(0.20f, 0.80f)
        val shadowSpan = (maxFootX - minFootX + 0.14f).coerceIn(0.20f, 0.68f)
        val shadowY = py(pose.floorY)
        drawOval(
            color = silhouetteColor.copy(alpha = 0.08f),
            topLeft = Offset(px(shadowCenterX - shadowSpan * 0.5f), shadowY - scale * 0.010f),
            size = Size(shadowSpan * scale, scale * 0.022f),
        )
    }

    // 2. Render unified solid human silhouette
    if (pose.viewMode == SilhouetteViewMode.SIDE) {
        drawSideSilhouette(
            pose = pose,
            morph = morph,
            silhouetteColor = silhouetteColor,
            p = ::p,
            scale = scale,
            paths = paths,
        )
    } else {
        drawFrontSilhouette(
            pose = pose,
            morph = morph,
            silhouetteColor = silhouetteColor,
            p = ::p,
            scale = scale,
            paths = paths,
        )
    }
}

private fun DrawScope.drawMinimalProp(
    pose: BodySkeletonPose,
    propColor: Color,
    px: (Float) -> Float,
    py: (Float) -> Float,
    scale: Float,
    propPath: Path,
) {
    when (pose.propType) {
        SilhouettePropType.NONE -> Unit
        SilhouettePropType.FLOOR_MAT -> {
            val matTop = py(pose.propPrimaryY)
            drawRoundRect(
                color = propColor,
                topLeft = Offset(px(0.11f), matTop),
                size = Size(px(0.89f) - px(0.11f), scale * 0.020f),
                cornerRadius = CornerRadius(scale * 0.010f, scale * 0.010f),
            )
        }
        SilhouettePropType.STEP_BOX -> {
            val topY = py(pose.propPrimaryY)
            val floorY = py(pose.floorY)
            val leftX = px(pose.propSecondaryX)
            val rightX = px(0.75f)
            drawRoundRect(
                color = propColor,
                topLeft = Offset(leftX, topY),
                size = Size(rightX - leftX, floorY - topY),
                cornerRadius = CornerRadius(scale * 0.014f, scale * 0.014f),
            )
        }
        SilhouettePropType.WALL -> {
            val wallX = px(pose.propSecondaryX)
            val floorY = py(pose.floorY)
            val topY = py(0.12f)
            drawRoundRect(
                color = propColor,
                topLeft = Offset(wallX, topY),
                size = Size(scale * 0.036f, floorY - topY),
                cornerRadius = CornerRadius(scale * 0.008f, scale * 0.008f),
            )
        }
        SilhouettePropType.CHAIR -> {
            val seatY = py(pose.propPrimaryY)
            val floorY = py(pose.floorY)
            val backX = px(pose.propSecondaryX)
            val frontX = px(pose.propSecondaryX + 0.165f)
            val strokeW = (scale * 0.018f).coerceAtLeast(2f)

            propPath.reset()
            // Backrest + rear leg
            propPath.moveTo(backX - scale * 0.018f, py(pose.propPrimaryY - 0.22f))
            propPath.lineTo(backX, seatY)
            propPath.lineTo(backX - scale * 0.008f, floorY)
            // Seat
            propPath.moveTo(backX - scale * 0.008f, seatY)
            propPath.lineTo(frontX, seatY)
            // Front leg
            propPath.moveTo(frontX - scale * 0.012f, seatY)
            propPath.lineTo(frontX - scale * 0.006f, floorY)

            drawPath(
                path = propPath,
                color = propColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
            // Seat cushion highlight
            drawRoundRect(
                color = propColor,
                topLeft = Offset(backX - scale * 0.008f, seatY - scale * 0.012f),
                size = Size((frontX - backX) + scale * 0.014f, scale * 0.024f),
                cornerRadius = CornerRadius(scale * 0.010f, scale * 0.010f),
            )
        }
    }
}

private fun DrawScope.drawSideSilhouette(
    pose: BodySkeletonPose,
    morph: GenderMorph,
    silhouetteColor: Color,
    p: (Vec2) -> Offset,
    scale: Float,
    paths: SilhouettePathPool,
) {
    val ls = morph.limbScale
    val pelvisP = p(pose.pelvisCenter)
    val neckP = p(pose.neckBase)
    val headP = p(pose.headCenter)

    // Spine unit up vector & anatomical front normal (rotated +90° clockwise from up)
    val sdx = neckP.x - pelvisP.x
    val sdy = neckP.y - pelvisP.y
    val sLen = hypot(sdx, sdy).coerceAtLeast(1f)
    val up = Offset(sdx / sLen, sdy / sLen)
    val front = Offset(-up.y, up.x)

    // 1. Far limbs (drawn seamlessly underneath)
    buildOrganicArmPath(
        path = paths.farArmPath,
        shoulder = p(pose.farShoulder),
        elbow = p(pose.farElbow),
        wrist = p(pose.farWrist),
        handTip = p(pose.farHandTip),
        scale = scale * 0.96f * ls,
    )
    drawPath(paths.farArmPath, color = silhouetteColor)

    buildSideLegPath(
        path = paths.farLegPath,
        hip = p(pose.farHip),
        knee = p(pose.farKnee),
        ankle = p(pose.farAnkle),
        frontDir = front,
        scale = scale * 0.97f * ls,
    )
    drawPath(paths.farLegPath, color = silhouetteColor)

    buildSideFootPath(
        path = paths.farFootPath,
        ankle = p(pose.farAnkle),
        heel = p(pose.farHeel),
        toe = p(pose.farToe),
        scale = scale * 0.97f,
    )
    drawPath(paths.farFootPath, color = silhouetteColor)

    // 2. Sculpted side torso & pelvis
    buildSideTorsoPath(
        path = paths.torsoPath,
        pelvis = pelvisP,
        neckBase = neckP,
        up = up,
        front = front,
        morph = morph,
        breathingExpand = pose.breathingExpand,
        scale = scale,
    )
    drawPath(paths.torsoPath, color = silhouetteColor)

    // 3. Sculpted side head & neck
    buildSideHeadAndNeckPath(
        path = paths.headPath,
        neckBase = neckP,
        headCenter = headP,
        headRadius = pose.headRadius * scale,
        torsoUp = up,
        front = front,
        scale = scale,
    )
    drawPath(paths.headPath, color = silhouetteColor)

    // 4. Near leg & foot
    buildSideLegPath(
        path = paths.nearLegPath,
        hip = p(pose.nearHip),
        knee = p(pose.nearKnee),
        ankle = p(pose.nearAnkle),
        frontDir = front,
        scale = scale * ls,
    )
    drawPath(paths.nearLegPath, color = silhouetteColor)

    buildSideFootPath(
        path = paths.nearFootPath,
        ankle = p(pose.nearAnkle),
        heel = p(pose.nearHeel),
        toe = p(pose.nearToe),
        scale = scale,
    )
    drawPath(paths.nearFootPath, color = silhouetteColor)

    // 5. Near arm
    buildOrganicArmPath(
        path = paths.nearArmPath,
        shoulder = p(pose.nearShoulder),
        elbow = p(pose.nearElbow),
        wrist = p(pose.nearWrist),
        handTip = p(pose.nearHandTip),
        scale = scale * ls,
    )
    drawPath(paths.nearArmPath, color = silhouetteColor)
}

private fun DrawScope.drawFrontSilhouette(
    pose: BodySkeletonPose,
    morph: GenderMorph,
    silhouetteColor: Color,
    p: (Vec2) -> Offset,
    scale: Float,
    paths: SilhouettePathPool,
) {
    val ls = morph.limbScale

    // 1. Torso connecting shoulders, chest, waist, hips
    buildFrontTorsoPath(
        path = paths.torsoPath,
        neckBase = p(pose.neckBase),
        rShoulder = p(pose.nearShoulder),
        lShoulder = p(pose.farShoulder),
        waistCenter = p(pose.waistCenter),
        rHip = p(pose.nearHip),
        lHip = p(pose.farHip),
        pelvisCenter = p(pose.pelvisCenter),
        morph = morph,
        breathingExpand = pose.breathingExpand,
        scale = scale,
    )
    drawPath(paths.torsoPath, color = silhouetteColor)

    // 2. Head & Neck
    val headCenterPx = p(pose.headCenter)
    val neckBasePx = p(pose.neckBase)
    val headAngleDeg = buildFrontNeckPath(
        path = paths.headPath,
        neckBase = neckBasePx,
        headCenter = headCenterPx,
        headRadius = pose.headRadius * scale,
        scale = scale,
    )
    drawPath(paths.headPath, color = silhouetteColor)
    val rx = pose.headRadius * scale * morph.headWidthScale
    val ry = pose.headRadius * scale * 1.12f
    rotate(degrees = headAngleDeg, pivot = headCenterPx) {
        drawOval(
            color = silhouetteColor,
            topLeft = Offset(headCenterPx.x - rx, headCenterPx.y - ry),
            size = Size(rx * 2f, ry * 2f),
        )
    }

    // 3. Left & Right Legs + Feet
    buildFrontLegPath(
        path = paths.farLegPath,
        hip = p(pose.farHip),
        knee = p(pose.farKnee),
        ankle = p(pose.farAnkle),
        outwardSign = -1f,
        scale = scale * ls,
    )
    drawPath(paths.farLegPath, color = silhouetteColor)

    buildFrontLegPath(
        path = paths.nearLegPath,
        hip = p(pose.nearHip),
        knee = p(pose.nearKnee),
        ankle = p(pose.nearAnkle),
        outwardSign = +1f,
        scale = scale * ls,
    )
    drawPath(paths.nearLegPath, color = silhouetteColor)

    buildFrontFootPath(
        path = paths.farFootPath,
        ankle = p(pose.farAnkle),
        heel = p(pose.farHeel),
        toe = p(pose.farToe),
        scale = scale,
    )
    drawPath(paths.farFootPath, color = silhouetteColor)

    buildFrontFootPath(
        path = paths.nearFootPath,
        ankle = p(pose.nearAnkle),
        heel = p(pose.nearHeel),
        toe = p(pose.nearToe),
        scale = scale,
    )
    drawPath(paths.nearFootPath, color = silhouetteColor)

    // 4. Left & Right Arms + Hands
    buildOrganicArmPath(
        path = paths.farArmPath,
        shoulder = p(pose.farShoulder),
        elbow = p(pose.farElbow),
        wrist = p(pose.farWrist),
        handTip = p(pose.farHandTip),
        scale = scale * ls,
    )
    drawPath(paths.farArmPath, color = silhouetteColor)

    buildOrganicArmPath(
        path = paths.nearArmPath,
        shoulder = p(pose.nearShoulder),
        elbow = p(pose.nearElbow),
        wrist = p(pose.nearWrist),
        handTip = p(pose.nearHandTip),
        scale = scale * ls,
    )
    drawPath(paths.nearArmPath, color = silhouetteColor)
}

private fun offsetAlong(origin: Offset, up: Offset, front: Offset, u: Float, f: Float, scale: Float): Offset =
    Offset(
        origin.x + (up.x * u + front.x * f) * scale,
        origin.y + (up.y * u + front.y * f) * scale,
    )

private fun buildSideTorsoPath(
    path: Path,
    pelvis: Offset,
    neckBase: Offset,
    up: Offset,
    front: Offset,
    morph: GenderMorph,
    breathingExpand: Float,
    scale: Float,
) {
    path.reset()
    val spineLen = hypot(neckBase.x - pelvis.x, neckBase.y - pelvis.y) / scale
    val chestF = morph.chestForward + breathingExpand

    val pTrapBack = offsetAlong(pelvis, up, front, spineLen * 1.03f, -0.018f, scale)
    val pUpperBack = offsetAlong(pelvis, up, front, spineLen * 0.76f, -morph.backCurve, scale)
    val pLumbar = offsetAlong(pelvis, up, front, spineLen * 0.36f, -morph.waistBack, scale)
    val pGlute = offsetAlong(pelvis, up, front, spineLen * 0.04f, -morph.gluteCurve, scale)
    val pPelvisBottom = offsetAlong(pelvis, up, front, -0.052f, -0.006f, scale)
    val pHipFront = offsetAlong(pelvis, up, front, spineLen * 0.03f, morph.hipForward, scale)
    val pWaistFront = offsetAlong(pelvis, up, front, spineLen * 0.40f, morph.waistFront, scale)
    val pChestFront = offsetAlong(pelvis, up, front, spineLen * 0.76f, chestF, scale)
    val pClavicle = offsetAlong(pelvis, up, front, spineLen * 1.01f, 0.019f, scale)

    path.moveTo(pClavicle.x, pClavicle.y)
    path.cubicTo(
        pClavicle.x + front.x * 0.014f * scale,
        pClavicle.y + front.y * 0.014f * scale,
        pChestFront.x + up.x * 0.025f * scale,
        pChestFront.y + up.y * 0.025f * scale,
        pChestFront.x,
        pChestFront.y,
    )
    path.cubicTo(
        pChestFront.x - up.x * 0.036f * scale,
        pChestFront.y - up.y * 0.036f * scale,
        pWaistFront.x + up.x * 0.030f * scale,
        pWaistFront.y + up.y * 0.030f * scale,
        pWaistFront.x,
        pWaistFront.y,
    )
    path.cubicTo(
        pWaistFront.x - up.x * 0.032f * scale,
        pWaistFront.y - up.y * 0.032f * scale,
        pHipFront.x + up.x * 0.030f * scale,
        pHipFront.y + up.y * 0.030f * scale,
        pHipFront.x,
        pHipFront.y,
    )
    path.cubicTo(
        pHipFront.x - up.x * 0.032f * scale,
        pHipFront.y - up.y * 0.032f * scale,
        pPelvisBottom.x + front.x * 0.026f * scale,
        pPelvisBottom.y + front.y * 0.026f * scale,
        pPelvisBottom.x,
        pPelvisBottom.y,
    )
    path.cubicTo(
        pPelvisBottom.x - front.x * 0.028f * scale,
        pPelvisBottom.y - front.y * 0.028f * scale,
        pGlute.x - up.x * 0.030f * scale,
        pGlute.y - up.y * 0.030f * scale,
        pGlute.x,
        pGlute.y,
    )
    path.cubicTo(
        pGlute.x + up.x * 0.035f * scale,
        pGlute.y + up.y * 0.035f * scale,
        pLumbar.x - up.x * 0.030f * scale,
        pLumbar.y - up.y * 0.030f * scale,
        pLumbar.x,
        pLumbar.y,
    )
    path.cubicTo(
        pLumbar.x + up.x * 0.036f * scale,
        pLumbar.y + up.y * 0.036f * scale,
        pUpperBack.x - up.x * 0.034f * scale,
        pUpperBack.y - up.y * 0.034f * scale,
        pUpperBack.x,
        pUpperBack.y,
    )
    path.cubicTo(
        pUpperBack.x + up.x * 0.035f * scale,
        pUpperBack.y + up.y * 0.035f * scale,
        pTrapBack.x - up.x * 0.015f * scale,
        pTrapBack.y - up.y * 0.015f * scale,
        pTrapBack.x,
        pTrapBack.y,
    )
    path.close()
}

private fun buildSideHeadAndNeckPath(
    path: Path,
    neckBase: Offset,
    headCenter: Offset,
    headRadius: Float,
    torsoUp: Offset,
    front: Offset,
    scale: Float,
) {
    path.reset()
    val hdx = headCenter.x - neckBase.x
    val hdy = headCenter.y - neckBase.y
    val hLen = hypot(hdx, hdy).coerceAtLeast(1f)
    val hUp = Offset(hdx / hLen, hdy / hLen)
    // Align front relative to head direction so head profile always faces same side as chest
    val cand = Offset(-hUp.y, hUp.x)
    val hFront = if (cand.x * front.x + cand.y * front.y >= 0f) cand else Offset(-cand.x, -cand.y)

    val r = headRadius
    val neckFront = Offset(
        neckBase.x + hFront.x * 0.017f * scale - torsoUp.x * 0.010f * scale,
        neckBase.y + hFront.y * 0.017f * scale - torsoUp.y * 0.010f * scale,
    )
    val neckBack = Offset(
        neckBase.x - hFront.x * 0.020f * scale - torsoUp.x * 0.010f * scale,
        neckBase.y - hFront.y * 0.020f * scale - torsoUp.y * 0.010f * scale,
    )
    val throat = Offset(
        headCenter.x - hUp.x * r * 0.85f + hFront.x * r * 0.34f,
        headCenter.y - hUp.y * r * 0.85f + hFront.y * r * 0.34f,
    )
    val chin = Offset(
        headCenter.x - hUp.x * r * 0.72f + hFront.x * r * 0.92f,
        headCenter.y - hUp.y * r * 0.72f + hFront.y * r * 0.92f,
    )
    val nose = Offset(
        headCenter.x - hUp.x * r * 0.04f + hFront.x * r * 1.03f,
        headCenter.y - hUp.y * r * 0.04f + hFront.y * r * 1.03f,
    )
    val brow = Offset(
        headCenter.x + hUp.x * r * 0.42f + hFront.x * r * 0.90f,
        headCenter.y + hUp.y * r * 0.42f + hFront.y * r * 0.90f,
    )
    val crown = Offset(
        headCenter.x + hUp.x * r * 1.08f,
        headCenter.y + hUp.y * r * 1.08f,
    )
    val occiput = Offset(
        headCenter.x - hUp.x * r * 0.05f - hFront.x * r * 0.98f,
        headCenter.y - hUp.y * r * 0.05f - hFront.y * r * 0.98f,
    )
    val skullBase = Offset(
        headCenter.x - hUp.x * r * 0.72f - hFront.x * r * 0.56f,
        headCenter.y - hUp.y * r * 0.72f - hFront.y * r * 0.56f,
    )

    path.moveTo(neckFront.x, neckFront.y)
    path.quadraticTo(
        (neckFront.x + throat.x) * 0.5f,
        (neckFront.y + throat.y) * 0.5f,
        throat.x,
        throat.y,
    )
    path.quadraticTo(
        chin.x - hFront.x * r * 0.10f,
        chin.y - hFront.y * r * 0.10f,
        chin.x,
        chin.y,
    )
    path.cubicTo(
        chin.x + hUp.x * r * 0.30f + hFront.x * r * 0.08f,
        chin.y + hUp.y * r * 0.30f + hFront.y * r * 0.08f,
        nose.x - hUp.x * r * 0.18f + hFront.x * r * 0.04f,
        nose.y - hUp.y * r * 0.18f + hFront.y * r * 0.04f,
        nose.x,
        nose.y,
    )
    path.cubicTo(
        nose.x + hUp.x * r * 0.22f,
        nose.y + hUp.y * r * 0.22f,
        brow.x - hUp.x * r * 0.16f + hFront.x * r * 0.05f,
        brow.y - hUp.y * r * 0.16f + hFront.y * r * 0.05f,
        brow.x,
        brow.y,
    )
    path.cubicTo(
        brow.x + hUp.x * r * 0.52f,
        brow.y + hUp.y * r * 0.52f,
        crown.x + hFront.x * r * 0.58f,
        crown.y + hFront.y * r * 0.58f,
        crown.x,
        crown.y,
    )
    path.cubicTo(
        crown.x - hFront.x * r * 0.65f,
        crown.y - hFront.y * r * 0.65f,
        occiput.x + hUp.x * r * 0.72f,
        occiput.y + hUp.y * r * 0.72f,
        occiput.x,
        occiput.y,
    )
    path.cubicTo(
        occiput.x - hUp.x * r * 0.40f,
        occiput.y - hUp.y * r * 0.40f,
        skullBase.x - hFront.x * r * 0.15f,
        skullBase.y - hFront.y * r * 0.15f,
        skullBase.x,
        skullBase.y,
    )
    path.quadraticTo(
        (skullBase.x + neckBack.x) * 0.5f,
        (skullBase.y + neckBack.y) * 0.5f,
        neckBack.x,
        neckBack.y,
    )
    path.close()
}

private fun buildSideLegPath(
    path: Path,
    hip: Offset,
    knee: Offset,
    ankle: Offset,
    frontDir: Offset,
    scale: Float,
) {
    path.reset()
    val tDx = knee.x - hip.x
    val tDy = knee.y - hip.y
    val tLen = hypot(tDx, tDy).coerceAtLeast(1f)
    val tAxis = Offset(tDx / tLen, tDy / tLen)
    val tCand = Offset(-tAxis.y, tAxis.x)
    val tFront = if (tCand.x * frontDir.x + tCand.y * frontDir.y >= 0f) tCand else Offset(-tCand.x, -tCand.y)

    val sDx = ankle.x - knee.x
    val sDy = ankle.y - knee.y
    val sLen = hypot(sDx, sDy).coerceAtLeast(1f)
    val sAxis = Offset(sDx / sLen, sDy / sLen)
    val sCand = Offset(-sAxis.y, sAxis.x)
    val sFront = if (sCand.x * frontDir.x + sCand.y * frontDir.y >= 0f) sCand else Offset(-sCand.x, -sCand.y)

    val hipTop = Offset(hip.x - tAxis.x * 0.024f * scale, hip.y - tAxis.y * 0.024f * scale)
    val hipF = Offset(hip.x + tFront.x * 0.036f * scale, hip.y + tFront.y * 0.036f * scale)
    val hipB = Offset(hip.x - tFront.x * 0.038f * scale, hip.y - tFront.y * 0.038f * scale)

    val thighMid = Offset((hip.x + knee.x) * 0.5f, (hip.y + knee.y) * 0.5f)
    val quadPeak = Offset(thighMid.x + tFront.x * 0.036f * scale, thighMid.y + tFront.y * 0.036f * scale)
    val hamPeak = Offset(thighMid.x - tFront.x * 0.032f * scale, thighMid.y - tFront.y * 0.032f * scale)

    val kneeF = Offset(knee.x + tFront.x * 0.025f * scale, knee.y + tFront.y * 0.025f * scale)
    val kneeB = Offset(knee.x - tFront.x * 0.022f * scale, knee.y - tFront.y * 0.022f * scale)

    val shinUpper = Offset(knee.x + sAxis.x * sLen * 0.34f, knee.y + sAxis.y * sLen * 0.34f)
    val shinF = Offset(shinUpper.x + sFront.x * 0.020f * scale, shinUpper.y + sFront.y * 0.020f * scale)
    val calfPeak = Offset(shinUpper.x - sFront.x * 0.032f * scale, shinUpper.y - sFront.y * 0.032f * scale)

    val ankleF = Offset(ankle.x + sFront.x * 0.015f * scale, ankle.y + sFront.y * 0.015f * scale)
    val ankleB = Offset(ankle.x - sFront.x * 0.015f * scale, ankle.y - sFront.y * 0.015f * scale)
    val ankleBot = Offset(ankle.x + sAxis.x * 0.014f * scale, ankle.y + sAxis.y * 0.014f * scale)

    path.moveTo(hipTop.x, hipTop.y)
    path.quadraticTo(hipF.x - tAxis.x * 0.015f * scale, hipF.y - tAxis.y * 0.015f * scale, hipF.x, hipF.y)
    path.quadraticTo(quadPeak.x, quadPeak.y, kneeF.x, kneeF.y)
    path.cubicTo(
        kneeF.x + sAxis.x * 0.028f * scale,
        kneeF.y + sAxis.y * 0.028f * scale,
        shinF.x - sAxis.x * 0.012f * scale,
        shinF.y - sAxis.y * 0.012f * scale,
        shinF.x,
        shinF.y,
    )
    path.quadraticTo(
        (shinF.x + ankleF.x) * 0.5f - sFront.x * 0.004f * scale,
        (shinF.y + ankleF.y) * 0.5f - sFront.y * 0.004f * scale,
        ankleF.x,
        ankleF.y,
    )
    path.quadraticTo(ankleBot.x + sFront.x * 0.01f * scale, ankleBot.y + sFront.y * 0.01f * scale, ankleBot.x, ankleBot.y)
    path.quadraticTo(ankleBot.x - sFront.x * 0.01f * scale, ankleBot.y - sFront.y * 0.01f * scale, ankleB.x, ankleB.y)
    path.cubicTo(
        ankleB.x - sAxis.x * 0.036f * scale - sFront.x * 0.004f * scale,
        ankleB.y - sAxis.y * 0.036f * scale - sFront.y * 0.004f * scale,
        calfPeak.x + sAxis.x * 0.042f * scale,
        calfPeak.y + sAxis.y * 0.042f * scale,
        calfPeak.x,
        calfPeak.y,
    )
    path.quadraticTo(
        calfPeak.x - sAxis.x * 0.035f * scale,
        calfPeak.y - sAxis.y * 0.035f * scale,
        kneeB.x,
        kneeB.y,
    )
    path.quadraticTo(hamPeak.x, hamPeak.y, hipB.x, hipB.y)
    path.quadraticTo(hipB.x - tAxis.x * 0.018f * scale, hipB.y - tAxis.y * 0.018f * scale, hipTop.x, hipTop.y)
    path.close()
}

private fun buildSideFootPath(
    path: Path,
    ankle: Offset,
    heel: Offset,
    toe: Offset,
    scale: Float,
) {
    path.reset()
    val fDx = toe.x - heel.x
    val fDy = toe.y - heel.y
    val fLen = hypot(fDx, fDy).coerceAtLeast(1f)
    val fwd = Offset(fDx / fLen, fDy / fLen)
    // Upward perpendicular from heel->toe
    val cand = Offset(fwd.y, -fwd.x)
    val up = if (cand.y <= 0f) cand else Offset(-cand.x, -cand.y)

    val heelBack = Offset(heel.x - fwd.x * 0.011f * scale, heel.y - fwd.y * 0.011f * scale)
    val heelBot = Offset(heel.x + up.x * 0.001f * scale, heel.y + up.y * 0.001f * scale)
    val archMid = Offset((heel.x + toe.x) * 0.48f + up.x * 0.006f * scale, (heel.y + toe.y) * 0.48f + up.y * 0.006f * scale)
    val ball = Offset(toe.x - fwd.x * 0.018f * scale, toe.y - fwd.y * 0.018f * scale)
    val toeTip = Offset(toe.x + fwd.x * 0.008f * scale + up.x * 0.007f * scale, toe.y + fwd.y * 0.008f * scale + up.y * 0.007f * scale)
    val toeTop = Offset(toe.x - fwd.x * 0.012f * scale + up.x * 0.017f * scale, toe.y - fwd.y * 0.012f * scale + up.y * 0.017f * scale)
    val instep = Offset(ankle.x + fwd.x * 0.022f * scale - up.x * 0.004f * scale, ankle.y + fwd.y * 0.022f * scale - up.y * 0.004f * scale)
    val ankleTopFront = Offset(ankle.x + fwd.x * 0.013f * scale + up.x * 0.012f * scale, ankle.y + fwd.y * 0.013f * scale + up.y * 0.012f * scale)
    val ankleTopBack = Offset(ankle.x - fwd.x * 0.014f * scale + up.x * 0.012f * scale, ankle.y - fwd.y * 0.014f * scale + up.y * 0.012f * scale)

    path.moveTo(ankleTopBack.x, ankleTopBack.y)
    path.lineTo(ankleTopFront.x, ankleTopFront.y)
    path.quadraticTo(instep.x, instep.y, toeTop.x, toeTop.y)
    path.cubicTo(
        toeTop.x + fwd.x * 0.014f * scale,
        toeTop.y + fwd.y * 0.014f * scale,
        toeTip.x + up.x * 0.006f * scale,
        toeTip.y + up.y * 0.006f * scale,
        toeTip.x,
        toeTip.y,
    )
    path.quadraticTo(toeTip.x - up.x * 0.006f * scale, toeTip.y - up.y * 0.006f * scale, ball.x, ball.y)
    path.quadraticTo(archMid.x, archMid.y, heelBot.x, heelBot.y)
    path.cubicTo(
        heelBack.x,
        heelBack.y - up.y * 0.008f * scale,
        heelBack.x + up.x * 0.016f * scale,
        heelBack.y + up.y * 0.016f * scale,
        ankleTopBack.x,
        ankleTopBack.y,
    )
    path.close()
}

private fun buildOrganicArmPath(
    path: Path,
    shoulder: Offset,
    elbow: Offset,
    wrist: Offset,
    handTip: Offset,
    scale: Float,
) {
    path.reset()
    fun perp(a: Offset, b: Offset): Pair<Offset, Offset> {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy).coerceAtLeast(1f)
        val ax = Offset(dx / len, dy / len)
        return ax to Offset(-ax.y, ax.x)
    }

    val (uAxis, uPerp) = perp(shoulder, elbow)
    val (fAxis, fPerp) = perp(elbow, wrist)
    // Keep uPerp and fPerp on the same side so there is zero twist at the elbow
    val fPerpAligned = if (uPerp.x * fPerp.x + uPerp.y * fPerp.y >= 0f) fPerp else Offset(-fPerp.x, -fPerp.y)
    val (hAxis, hPerp) = perp(wrist, handTip)
    val hPerpAligned = if (fPerpAligned.x * hPerp.x + fPerpAligned.y * hPerp.y >= 0f) hPerp else Offset(-hPerp.x, -hPerp.y)

    val shoulderCap = Offset(shoulder.x - uAxis.x * 0.024f * scale, shoulder.y - uAxis.y * 0.024f * scale)
    val shA = Offset(shoulder.x + uPerp.x * 0.025f * scale, shoulder.y + uPerp.y * 0.025f * scale)
    val shB = Offset(shoulder.x - uPerp.x * 0.025f * scale, shoulder.y - uPerp.y * 0.025f * scale)

    val upMid = Offset((shoulder.x + elbow.x) * 0.5f, (shoulder.y + elbow.y) * 0.5f)
    val bicepA = Offset(upMid.x + uPerp.x * 0.023f * scale, upMid.y + uPerp.y * 0.023f * scale)
    val bicepB = Offset(upMid.x - uPerp.x * 0.022f * scale, upMid.y - uPerp.y * 0.022f * scale)

    val elbAvgPerp = Offset((uPerp.x + fPerpAligned.x) * 0.5f, (uPerp.y + fPerpAligned.y) * 0.5f).let {
        val l = hypot(it.x, it.y).coerceAtLeast(0.001f)
        Offset(it.x / l, it.y / l)
    }
    val elbA = Offset(elbow.x + elbAvgPerp.x * 0.0175f * scale, elbow.y + elbAvgPerp.y * 0.0175f * scale)
    val elbB = Offset(elbow.x - elbAvgPerp.x * 0.0175f * scale, elbow.y - elbAvgPerp.y * 0.0175f * scale)

    val foreUpper = Offset(elbow.x + fAxis.x * hypot(wrist.x - elbow.x, wrist.y - elbow.y) * 0.34f, elbow.y + fAxis.y * hypot(wrist.x - elbow.x, wrist.y - elbow.y) * 0.34f)
    val foreA = Offset(foreUpper.x + fPerpAligned.x * 0.0195f * scale, foreUpper.y + fPerpAligned.y * 0.0195f * scale)
    val foreB = Offset(foreUpper.x - fPerpAligned.x * 0.0185f * scale, foreUpper.y - fPerpAligned.y * 0.0185f * scale)

    val wrA = Offset(wrist.x + fPerpAligned.x * 0.0115f * scale, wrist.y + fPerpAligned.y * 0.0115f * scale)
    val wrB = Offset(wrist.x - fPerpAligned.x * 0.0115f * scale, wrist.y - fPerpAligned.y * 0.0115f * scale)

    val palmMid = Offset((wrist.x + handTip.x) * 0.5f, (wrist.y + handTip.y) * 0.5f)
    val palmA = Offset(palmMid.x + hPerpAligned.x * 0.0135f * scale, palmMid.y + hPerpAligned.y * 0.0135f * scale)
    val palmB = Offset(palmMid.x - hPerpAligned.x * 0.0115f * scale, palmMid.y - hPerpAligned.y * 0.0115f * scale)
    val fingerTip = Offset(handTip.x + hAxis.x * 0.004f * scale, handTip.y + hAxis.y * 0.004f * scale)

    path.moveTo(shoulderCap.x, shoulderCap.y)
    path.quadraticTo(shA.x - uAxis.x * 0.014f * scale, shA.y - uAxis.y * 0.014f * scale, shA.x, shA.y)
    path.quadraticTo(bicepA.x, bicepA.y, elbA.x, elbA.y)
    path.quadraticTo(foreA.x, foreA.y, wrA.x, wrA.y)
    path.quadraticTo(palmA.x, palmA.y, fingerTip.x, fingerTip.y)
    path.quadraticTo(palmB.x, palmB.y, wrB.x, wrB.y)
    path.quadraticTo(foreB.x, foreB.y, elbB.x, elbB.y)
    path.quadraticTo(bicepB.x, bicepB.y, shB.x, shB.y)
    path.quadraticTo(shB.x - uAxis.x * 0.014f * scale, shB.y - uAxis.y * 0.014f * scale, shoulderCap.x, shoulderCap.y)
    path.close()
}

private fun buildFrontTorsoPath(
    path: Path,
    neckBase: Offset,
    rShoulder: Offset,
    lShoulder: Offset,
    waistCenter: Offset,
    rHip: Offset,
    lHip: Offset,
    pelvisCenter: Offset,
    morph: GenderMorph,
    breathingExpand: Float,
    scale: Float,
) {
    path.reset()
    val wHalf = (morph.waistFrontWidth + breathingExpand * 0.35f) * scale
    val hHalf = morph.hipFrontWidth * scale
    val shCapPad = morph.shoulderFrontWidth * scale

    val rShOuter = Offset(rShoulder.x + shCapPad, rShoulder.y - 0.004f * scale)
    val lShOuter = Offset(lShoulder.x - shCapPad, lShoulder.y - 0.004f * scale)
    val rTrap = Offset(neckBase.x + 0.024f * scale, neckBase.y + 0.004f * scale)
    val lTrap = Offset(neckBase.x - 0.024f * scale, neckBase.y + 0.004f * scale)

    val rWaist = Offset(waistCenter.x + wHalf, waistCenter.y)
    val lWaist = Offset(waistCenter.x - wHalf, waistCenter.y)
    val rPelvisOuter = Offset(pelvisCenter.x + hHalf, pelvisCenter.y + 0.006f * scale)
    val lPelvisOuter = Offset(pelvisCenter.x - hHalf, pelvisCenter.y + 0.006f * scale)
    val groin = Offset(pelvisCenter.x, pelvisCenter.y + 0.052f * scale)

    val rRib = Offset(rShoulder.x + 0.004f * scale, rShoulder.y + 0.056f * scale)
    val lRib = Offset(lShoulder.x - 0.004f * scale, lShoulder.y + 0.056f * scale)

    path.moveTo(neckBase.x, neckBase.y)
    path.lineTo(rTrap.x, rTrap.y)
    path.quadraticTo(
        (rTrap.x + rShOuter.x) * 0.5f,
        rTrap.y + 0.006f * scale,
        rShOuter.x,
        rShOuter.y,
    )
    path.cubicTo(
        rShOuter.x + 0.004f * scale,
        rShOuter.y + 0.028f * scale,
        rRib.x + 0.004f * scale,
        rRib.y - 0.015f * scale,
        rRib.x,
        rRib.y,
    )
    path.cubicTo(
        rRib.x - 0.006f * scale,
        rRib.y + 0.032f * scale,
        rWaist.x,
        rWaist.y - 0.026f * scale,
        rWaist.x,
        rWaist.y,
    )
    path.cubicTo(
        rWaist.x + 0.002f * scale,
        rWaist.y + 0.034f * scale,
        rPelvisOuter.x + 0.004f * scale,
        rPelvisOuter.y - 0.032f * scale,
        rPelvisOuter.x,
        rPelvisOuter.y,
    )
    path.quadraticTo(
        rHip.x + 0.008f * scale,
        groin.y - 0.004f * scale,
        groin.x,
        groin.y,
    )
    path.quadraticTo(
        lHip.x - 0.008f * scale,
        groin.y - 0.004f * scale,
        lPelvisOuter.x,
        lPelvisOuter.y,
    )
    path.cubicTo(
        lPelvisOuter.x - 0.004f * scale,
        lPelvisOuter.y - 0.032f * scale,
        lWaist.x - 0.002f * scale,
        lWaist.y + 0.034f * scale,
        lWaist.x,
        lWaist.y,
    )
    path.cubicTo(
        lWaist.x,
        lWaist.y - 0.026f * scale,
        lRib.x + 0.006f * scale,
        lRib.y + 0.032f * scale,
        lRib.x,
        lRib.y,
    )
    path.cubicTo(
        lRib.x - 0.004f * scale,
        lRib.y - 0.015f * scale,
        lShOuter.x - 0.004f * scale,
        lShOuter.y + 0.028f * scale,
        lShOuter.x,
        lShOuter.y,
    )
    path.quadraticTo(
        (lTrap.x + lShOuter.x) * 0.5f,
        lTrap.y + 0.006f * scale,
        lTrap.x,
        lTrap.y,
    )
    path.close()
}

private fun buildFrontNeckPath(
    path: Path,
    neckBase: Offset,
    headCenter: Offset,
    headRadius: Float,
    scale: Float,
): Float {
    path.reset()
    val dx = headCenter.x - neckBase.x
    val dy = headCenter.y - neckBase.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val up = Offset(dx / len, dy / len)
    val right = Offset(-up.y, up.x)

    val neckHalfW = 0.0185f * scale
    val nR = Offset(neckBase.x + right.x * neckHalfW - up.x * 0.014f * scale, neckBase.y + right.y * neckHalfW - up.y * 0.014f * scale)
    val nL = Offset(neckBase.x - right.x * neckHalfW - up.x * 0.014f * scale, neckBase.y - right.y * neckHalfW - up.y * 0.014f * scale)
    val jR = Offset(headCenter.x + right.x * neckHalfW * 0.96f - up.x * headRadius * 0.55f, headCenter.y + right.y * neckHalfW * 0.96f - up.y * headRadius * 0.55f)
    val jL = Offset(headCenter.x - right.x * neckHalfW * 0.96f - up.x * headRadius * 0.55f, headCenter.y - right.y * neckHalfW * 0.96f - up.y * headRadius * 0.55f)

    path.moveTo(nL.x, nL.y)
    path.lineTo(nR.x, nR.y)
    path.lineTo(jR.x, jR.y)
    path.lineTo(jL.x, jL.y)
    path.close()

    return (atan2(dy, dx) * 180.0 / PI + 90.0).toFloat()
}

private fun buildFrontLegPath(
    path: Path,
    hip: Offset,
    knee: Offset,
    ankle: Offset,
    outwardSign: Float,
    scale: Float,
) {
    path.reset()
    fun perp(a: Offset, b: Offset): Pair<Offset, Offset> {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy).coerceAtLeast(1f)
        val ax = Offset(dx / len, dy / len)
        val cand = Offset(-ax.y, ax.x)
        // Orient outward vector toward outwardSign (+x for right screen leg, -x for left screen leg)
        val out = if (cand.x * outwardSign >= 0f) cand else Offset(-cand.x, -cand.y)
        return ax to out
    }

    val (tAxis, tOut) = perp(hip, knee)
    val (sAxis, sOut) = perp(knee, ankle)

    val hipTop = Offset(hip.x - tAxis.x * 0.028f * scale, hip.y - tAxis.y * 0.028f * scale)
    val hipOuter = Offset(hip.x + tOut.x * 0.036f * scale, hip.y + tOut.y * 0.036f * scale)
    val hipInner = Offset(hip.x - tOut.x * 0.032f * scale, hip.y - tOut.y * 0.032f * scale)

    val thighMid = Offset((hip.x + knee.x) * 0.5f, (hip.y + knee.y) * 0.5f)
    val thighOuter = Offset(thighMid.x + tOut.x * 0.035f * scale, thighMid.y + tOut.y * 0.035f * scale)
    val thighInner = Offset(thighMid.x - tOut.x * 0.028f * scale, thighMid.y - tOut.y * 0.028f * scale)

    val kneeOuter = Offset(knee.x + tOut.x * 0.022f * scale, knee.y + tOut.y * 0.022f * scale)
    val kneeInner = Offset(knee.x - tOut.x * 0.021f * scale, knee.y - tOut.y * 0.021f * scale)

    val calfLevel = Offset(knee.x + sAxis.x * hypot(ankle.x - knee.x, ankle.y - knee.y) * 0.34f, knee.y + sAxis.y * hypot(ankle.x - knee.x, ankle.y - knee.y) * 0.34f)
    val calfOuter = Offset(calfLevel.x + sOut.x * 0.027f * scale, calfLevel.y + sOut.y * 0.027f * scale)
    val calfInner = Offset(calfLevel.x - sOut.x * 0.024f * scale, calfLevel.y - sOut.y * 0.024f * scale)

    val ankleOuter = Offset(ankle.x + sOut.x * 0.013f * scale, ankle.y + sOut.y * 0.013f * scale)
    val ankleInner = Offset(ankle.x - sOut.x * 0.013f * scale, ankle.y - sOut.y * 0.013f * scale)
    val ankleBot = Offset(ankle.x + sAxis.x * 0.016f * scale, ankle.y + sAxis.y * 0.016f * scale)

    path.moveTo(hipTop.x, hipTop.y)
    path.quadraticTo(hipOuter.x - tAxis.x * 0.018f * scale, hipOuter.y - tAxis.y * 0.018f * scale, hipOuter.x, hipOuter.y)
    path.quadraticTo(thighOuter.x, thighOuter.y, kneeOuter.x, kneeOuter.y)
    path.quadraticTo(calfOuter.x, calfOuter.y, ankleOuter.x, ankleOuter.y)
    path.lineTo(ankleBot.x, ankleBot.y)
    path.lineTo(ankleInner.x, ankleInner.y)
    path.quadraticTo(calfInner.x, calfInner.y, kneeInner.x, kneeInner.y)
    path.quadraticTo(thighInner.x, thighInner.y, hipInner.x, hipInner.y)
    path.quadraticTo(hipInner.x - tAxis.x * 0.018f * scale, hipInner.y - tAxis.y * 0.018f * scale, hipTop.x, hipTop.y)
    path.close()
}

private fun buildFrontFootPath(
    path: Path,
    ankle: Offset,
    heel: Offset,
    toe: Offset,
    scale: Float,
) {
    path.reset()
    val midX = (heel.x + toe.x) * 0.5f
    val floorY = maxOf(heel.y, toe.y)
    val halfSpan = maxOf(kotlin.math.abs(toe.x - heel.x) * 0.55f, 0.018f * scale)
    val ankleTop = Offset(ankle.x, ankle.y - 0.004f * scale)

    path.moveTo(ankleTop.x - 0.013f * scale, ankleTop.y)
    path.lineTo(ankleTop.x + 0.013f * scale, ankleTop.y)
    path.cubicTo(
        midX + halfSpan * 1.05f,
        floorY - 0.016f * scale,
        midX + halfSpan,
        floorY,
        midX + halfSpan * 0.70f,
        floorY,
    )
    path.lineTo(midX - halfSpan * 0.70f, floorY)
    path.cubicTo(
        midX - halfSpan,
        floorY,
        midX - halfSpan * 1.05f,
        floorY - 0.016f * scale,
        ankleTop.x - 0.013f * scale,
        ankleTop.y,
    )
    path.close()
}
