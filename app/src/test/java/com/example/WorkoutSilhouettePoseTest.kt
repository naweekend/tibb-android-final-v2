package com.example

import com.example.data.model.PatientProfile
import com.example.data.model.WellnessGoal
import com.example.engine.RemediesAndPlans
import com.example.ui.components.workout.ExerciseAnimationType
import com.example.ui.components.workout.SilhouetteGender
import com.example.ui.components.workout.SilhouetteViewMode
import com.example.ui.components.workout.Vec2
import com.example.ui.components.workout.evaluateExercisePose
import com.example.ui.components.workout.resolveExerciseMovements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class WorkoutSilhouettePoseTest {

    private fun dist(a: Vec2, b: Vec2): Float = hypot(b.x - a.x, b.y - a.y)

    @Test
    fun allWorkoutDatabaseSlotsResolveToDedicatedAnimations() {
        val profiles = listOf(
            PatientProfile(id = "senior", name = "Senior", ageMonths = 68 * 12, sex = "M", preferredGoal = WellnessGoal.SENIOR_MOBILITY),
            PatientProfile(id = "wl", name = "WeightLoss", ageMonths = 34 * 12, sex = "F", preferredGoal = WellnessGoal.WEIGHT_LOSS),
            PatientProfile(id = "mg", name = "MuscleGain", ageMonths = 27 * 12, sex = "M", preferredGoal = WellnessGoal.MUSCLE_GAIN),
            PatientProfile(id = "bal", name = "Balanced", ageMonths = 38 * 12, sex = "O", preferredGoal = WellnessGoal.BALANCED),
        )
        profiles.forEach { profile ->
            val plan = RemediesAndPlans.buildPersonalizedPlanOffline(profile, profile.preferredGoal, variationSeed = 0)
            assertTrue("Expected non-empty routine for ${profile.id}", plan.workoutRoutine.isNotEmpty())
            plan.workoutRoutine.forEach { slot ->
                val specs = resolveExerciseMovements(slot)
                assertTrue("Expected resolved movements for ${slot.name_en}", specs.isNotEmpty())
                if ("squat" in slot.name_en.lowercase()) {
                    assertTrue(specs.any { it.type == ExerciseAnimationType.BODYWEIGHT_SQUAT })
                }
                if ("plank" in slot.name_en.lowercase()) {
                    assertTrue(specs.any { it.type == ExerciseAnimationType.PLANK_HOLD && it.type.isStaticHold })
                }
                if ("lunge" in slot.name_en.lowercase()) {
                    assertTrue(specs.any { it.type == ExerciseAnimationType.SPLIT_LUNGE })
                }
                if ("wall push" in slot.name_en.lowercase()) {
                    assertTrue(specs.any { it.type == ExerciseAnimationType.WALL_PUSH_UPS })
                } else if ("push-up" in slot.name_en.lowercase()) {
                    assertTrue(specs.any { it.type == ExerciseAnimationType.PUSH_UPS })
                }
            }
        }
    }

    @Test
    fun genderMappingRespectsProfileSex() {
        assertEquals(SilhouetteGender.MALE, SilhouetteGender.fromSexCode("M"))
        assertEquals(SilhouetteGender.FEMALE, SilhouetteGender.fromSexCode("F"))
        assertEquals(SilhouetteGender.NEUTRAL, SilhouetteGender.fromSexCode("O"))
        assertEquals(SilhouetteGender.NEUTRAL, SilhouetteGender.fromSexCode(null))
        assertEquals(SilhouetteGender.NEUTRAL, SilhouetteGender.fromSexCode(""))
    }

    @Test
    fun allExercisePosesMaintainRigidSegmentLengthsAcrossCycle() {
        val phases = listOf(0f, 0.25f, 0.5f, 0.75f)
        ExerciseAnimationType.entries.forEach { type ->
            SilhouetteGender.entries.forEach { gender ->
                phases.forEach { phase ->
                    val pose = evaluateExercisePose(type, phase, gender)
                    val points = listOf(
                        pose.headCenter,
                        pose.neckBase,
                        pose.chestCenter,
                        pose.waistCenter,
                        pose.pelvisCenter,
                        pose.nearShoulder,
                        pose.nearElbow,
                        pose.nearWrist,
                        pose.nearHandTip,
                        pose.farShoulder,
                        pose.farElbow,
                        pose.farWrist,
                        pose.farHandTip,
                        pose.nearHip,
                        pose.nearKnee,
                        pose.nearAnkle,
                        pose.nearHeel,
                        pose.nearToe,
                        pose.farHip,
                        pose.farKnee,
                        pose.farAnkle,
                        pose.farHeel,
                        pose.farToe,
                    )
                    points.forEach { pt ->
                        assertFalse("NaN x in $type", pt.x.isNaN())
                        assertFalse("NaN y in $type", pt.y.isNaN())
                        assertTrue("Out-of-bounds x=${pt.x} in $type", pt.x in 0.02f..0.98f)
                        assertTrue("Out-of-bounds y=${pt.y} in $type", pt.y in 0.02f..0.98f)
                    }

                    val upperArm = dist(pose.nearShoulder, pose.nearElbow)
                    val forearm = dist(pose.nearElbow, pose.nearWrist)
                    val thigh = dist(pose.nearHip, pose.nearKnee)
                    val shin = dist(pose.nearKnee, pose.nearAnkle)
                    assertTrue("Upper arm collapsed in $type: $upperArm", upperArm in 0.11f..0.145f)
                    assertTrue("Forearm collapsed in $type: $forearm", forearm in 0.105f..0.135f)
                    assertTrue("Thigh collapsed in $type: $thigh", thigh in 0.165f..0.195f)
                    assertTrue("Shin collapsed in $type: $shin", shin in 0.160f..0.190f)
                }
            }
        }
    }

    @Test
    fun genderProportionsDifferSubtlyInFrontView() {
        val malePose = evaluateExercisePose(ExerciseAnimationType.BREATH_WIND_DOWN, 0f, SilhouetteGender.MALE)
        val femalePose = evaluateExercisePose(ExerciseAnimationType.BREATH_WIND_DOWN, 0f, SilhouetteGender.FEMALE)
        assertEquals(SilhouetteViewMode.FRONT, malePose.viewMode)

        val maleShoulderW = abs(malePose.nearShoulder.x - malePose.farShoulder.x)
        val femaleShoulderW = abs(femalePose.nearShoulder.x - femalePose.farShoulder.x)
        assertTrue("Expected realistic shoulder width variation", maleShoulderW > femaleShoulderW)

        val maleHipW = abs(malePose.nearHip.x - malePose.farHip.x)
        val femaleHipW = abs(femalePose.nearHip.x - femalePose.farHip.x)
        assertTrue("Expected realistic hip width variation", femaleHipW > maleHipW)
    }
}
