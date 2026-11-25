package com.example

import com.example.data.local.BundledDataLoader
import com.example.data.model.HealthProfileCatalog
import com.example.data.model.PROFILE_OPTION_NONE
import com.example.data.model.PROFILE_OPTION_NOT_SURE
import com.example.data.model.PatientProfile
import com.example.data.model.RelationshipType
import com.example.data.model.StructuredAssessmentInput
import com.example.data.model.WellnessGoal
import com.example.data.model.clinicalAllergies
import com.example.data.model.clinicalChronicConditions
import com.example.engine.LocalMedicalGuidanceEngine
import com.example.engine.RemediesAndPlans
import com.example.engine.TriageEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HealthProfileCatalogTest {

    private fun createLoader(): BundledDataLoader {
        val assetsRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
        ).first { it.exists() }
        return BundledDataLoader(assetOpener = { path -> File(assetsRoot, path).readText() })
    }

    @Test
    fun chronicCatalog_containsAtLeast25DistinctConditionsWithoutDuplicates() {
        val conditions = HealthProfileCatalog.chronicConditions
        assertTrue("Expected >= 25 conditions, found ${conditions.size}", conditions.size >= 25)
        assertEquals(conditions.size, conditions.map { it.id.lowercase() }.toSet().size)
        assertEquals(conditions.size, conditions.map { it.labelEn.lowercase() }.toSet().size)
        assertTrue(conditions.all { it.labelEn.isNotBlank() && it.labelUr.isNotBlank() && it.categoryId.isNotBlank() })
    }

    @Test
    fun allergyCatalog_containsComprehensiveUniqueCategories() {
        val allergies = HealthProfileCatalog.allergies
        assertTrue("Expected >= 16 allergy categories, found ${allergies.size}", allergies.size >= 16)
        assertEquals(allergies.size, allergies.map { it.id.lowercase() }.toSet().size)
        assertTrue(allergies.any { it.id.contains("Peanut", ignoreCase = true) })
        assertTrue(allergies.any { it.id.contains("Antibiotic", ignoreCase = true) })
        assertTrue(allergies.any { it.id.contains("Pollen", ignoreCase = true) })
    }

    @Test
    fun sentinelOptions_excludedFromClinicalListsAndEnginePrecautions() {
        val patient = PatientProfile(
            id = "pat_none",
            accountId = "acc",
            name = "Amina",
            ageMonths = 300,
            sex = "F",
            relationship = RelationshipType.Self,
            chronicConditions = listOf(PROFILE_OPTION_NONE),
            allergies = listOf(PROFILE_OPTION_NOT_SURE),
        )
        assertTrue(patient.clinicalChronicConditions().isEmpty())
        assertTrue(patient.clinicalAllergies().isEmpty())
        val (en, ur) = HealthProfileCatalog.buildProfilePrecautions(patient)
        assertTrue(en.isEmpty())
        assertTrue(ur.isEmpty())

        val plan = RemediesAndPlans.buildPersonalizedPlanOffline(patient, WellnessGoal.BALANCED)
        assertFalse(plan.avoidNotes_en.any { it.contains("NONE_OF_THESE") || it.contains("NOT_SURE") })
    }

    @Test
    fun expandedConditionsAndAllergies_generatePrecautionsAndDietSafeguards() {
        val patient = PatientProfile(
            id = "pat_profiled",
            accountId = "acc",
            name = "Bilal",
            ageMonths = 480,
            sex = "M",
            relationship = RelationshipType.Self,
            chronicConditions = listOf("Type 2 Diabetes", "Hypertension", "Chronic Kidney Disease"),
            allergies = listOf("Antibiotic Allergies", "Peanut Allergy"),
        )
        val (en, ur) = HealthProfileCatalog.buildProfilePrecautions(patient)
        assertTrue(en.any { it.contains("Diabetes", ignoreCase = true) })
        assertTrue(en.any { it.contains("NSAID", ignoreCase = true) })
        assertTrue(en.any { it.contains("Antibiotic", ignoreCase = true) })
        assertTrue(ur.isNotEmpty())

        val loader = createLoader()
        val triageEngine = TriageEngine(loader)
        val guidanceEngine = LocalMedicalGuidanceEngine(loader, triageEngine)

        val assessment = guidanceEngine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = patient,
                categoryId = "cat_respiratory",
                selectedSymptomIds = setOf("sym_sore_throat", "sym_runny_nose"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_duration" to setOf("opt_dur_1_2d"),
                    "q_severity" to setOf("opt_sev_mild"),
                ),
            )
        )
        assertTrue(assessment.profilePrecautionsEn.isNotEmpty())
        assertTrue(assessment.profilePrecautionsUr.isNotEmpty())

        val plan = RemediesAndPlans.buildPersonalizedPlanOffline(patient, WellnessGoal.BALANCED)
        assertTrue(plan.avoidNotes_en.any { it.contains("sugary", ignoreCase = true) })
        assertTrue(plan.avoidNotes_en.any { it.contains("salt", ignoreCase = true) })
        assertTrue(plan.avoidNotes_en.any { it.contains("Peanut", ignoreCase = true) })
    }
}
