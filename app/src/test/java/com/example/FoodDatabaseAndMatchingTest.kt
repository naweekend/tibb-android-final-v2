package com.example

import com.example.data.local.BundledDataLoader
import com.example.data.model.DietMealSlot
import com.example.data.model.FoodCategory
import com.example.data.model.NutritionDataSource
import com.example.data.model.PatientProfile
import com.example.data.model.PersonalizedWellnessPlan
import com.example.data.model.RelationshipType
import com.example.data.model.WellnessGoal
import com.example.engine.FoodMatchingEngine
import com.example.engine.NutritionalMatchConfig
import com.example.engine.RemediesAndPlans
import com.example.engine.SuitabilityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FoodDatabaseAndMatchingTest {

    private fun createLoader(): BundledDataLoader {
        val assetsRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
        ).first { it.exists() }
        return BundledDataLoader(assetOpener = { path -> File(assetsRoot, path).readText() })
    }

    private fun baselinePatient(
        conditions: List<String> = emptyList(),
        allergies: List<String> = emptyList(),
        dietPrefs: List<String> = emptyList(),
    ) = PatientProfile(
        id = "pat_test",
        accountId = "acc_test",
        name = "Test Patient",
        ageMonths = 360,
        sex = "M",
        relationship = RelationshipType.Self,
        weightKg = 72.0,
        heightCm = 172.0,
        chronicConditions = conditions,
        allergies = allergies,
        dietaryPreferences = dietPrefs,
        preferredGoal = WellnessGoal.BALANCED,
    )

    @Test
    fun foodDatabase_exceedsFiveTimesOriginalSizeWithoutDuplicatesOrInvalidServings() {
        val catalog = createLoader().foodCatalog
        // Original plan had 36 hardcoded options; >= 5x = >= 180 foods
        assertTrue("Expected >= 180 foods (5x original 36), found ${catalog.size}", catalog.size >= 180)
        assertTrue("Expected ~250+ foods, found ${catalog.size}", catalog.size >= 240)

        // Zero duplicate IDs or English names
        assertEquals(catalog.size, catalog.map { it.id.trim().lowercase() }.toSet().size)
        assertEquals(catalog.size, catalog.map { it.nameEn.trim().lowercase() }.toSet().size)

        // Every food has consistent serving units, valid macros, and explicit provenance
        catalog.forEach { food ->
            assertTrue("Blank nameEn on ${food.id}", food.nameEn.isNotBlank())
            assertTrue("Blank nameUr on ${food.id}", food.nameUr.isNotBlank())
            assertTrue("Blank servingSizeEn on ${food.id}", food.servingSizeEn.isNotBlank())
            assertTrue("Blank servingSizeUr on ${food.id}", food.servingSizeUr.isNotBlank())
            assertTrue("Invalid servingGrams on ${food.id}", food.servingGrams > 0.0)
            assertTrue("Invalid calories on ${food.id}", food.caloriesKcal > 0)
            assertTrue("Negative protein on ${food.id}", food.proteinG >= 0.0)
            assertTrue("Negative carbs on ${food.id}", food.carbsG >= 0.0)
            assertTrue("Negative fat on ${food.id}", food.fatG >= 0.0)
            assertTrue("Negative fiber on ${food.id}", food.fiberG >= 0.0)
            assertTrue(
                "Invalid nutritionSource on ${food.id}",
                food.nutritionSource in NutritionDataSource.entries,
            )
            assertTrue("Missing benefitsEn on ${food.id}", food.benefitsEn.isNotBlank())
            assertTrue("Missing suitableMeals on ${food.id}", food.suitableMeals.isNotEmpty())
        }

        // All categories are well represented
        FoodCategory.entries.forEach { cat ->
            val count = catalog.count { it.category == cat }
            assertTrue("Category $cat has too few entries: $count", count >= 15)
        }

        // Required Desi & staple items present
        val namesJoined = catalog.joinToString(" | ") { it.nameEn.lowercase() }
        listOf(
            "daal", "chana", "chapati", "brown basmati rice", "karahi",
            "grilled chicken", "pulao", "dahi", "guava", "palak",
        ).forEach { keyword ->
            assertTrue("Expected staple food matching '$keyword'", namesJoined.contains(keyword))
        }
    }

    @Test
    fun searchAndCategoryFilter_returnAccurateOfflineMatches() {
        val catalog = createLoader().foodCatalog
        val patient = baselinePatient()
        val dummyMeal = DietMealSlot(
            slot = "Lunch",
            title_en = "Grilled Chicken + 1 Roti",
            title_ur = "گرلڈ چکن",
            portions_en = "1 serving",
            portions_ur = "1 حصہ",
            caloriesApprox = 350,
            proteinG = 28.0,
            carbsG = 30.0,
            fatG = 11.0,
            fiberG = 5.0,
        )

        val daalResults = FoodMatchingEngine.findAlternatives(
            originalMeal = dummyMeal,
            catalog = catalog,
            patient = patient,
            goal = WellnessGoal.BALANCED,
            query = "daal",
            withinCalorieTargetOnly = false,
        )
        assertTrue(daalResults.isNotEmpty())
        assertTrue(
            daalResults.all {
                it.food.nameEn.contains("daal", ignoreCase = true) ||
                    it.food.benefitsEn.contains("daal", ignoreCase = true) ||
                    it.food.category.labelEn.contains("daal", ignoreCase = true)
            },
        )

        val fruitsOnly = FoodMatchingEngine.findAlternatives(
            originalMeal = dummyMeal,
            catalog = catalog,
            patient = patient,
            goal = WellnessGoal.BALANCED,
            categoryFilter = FoodCategory.FRUITS,
            withinCalorieTargetOnly = false,
        )
        assertTrue(fruitsOnly.size >= 15)
        assertTrue(fruitsOnly.all { it.food.category == FoodCategory.FRUITS })
    }

    @Test
    fun nutritionalMatching_prioritizesComparableCaloriesAndProteinWithinConfigurableTolerance() {
        val catalog = createLoader().foodCatalog
        val patient = baselinePatient()
        val targetMeal = DietMealSlot(
            slot = "Dinner",
            title_en = "Chicken Tikka + Salad",
            title_ur = "چکن تکہ",
            portions_en = "1 plate",
            portions_ur = "1 پلیٹ",
            caloriesApprox = 300,
            proteinG = 30.0,
            carbsG = 12.0,
            fatG = 12.0,
            fiberG = 4.0,
        )

        val strictMatches = FoodMatchingEngine.findAlternatives(
            originalMeal = targetMeal,
            catalog = catalog,
            patient = patient,
            goal = WellnessGoal.WEIGHT_LOSS,
            withinCalorieTargetOnly = true,
            config = NutritionalMatchConfig(calorieTolerancePct = 0.15),
        )
        assertTrue("Expected rich matches within ±15% of 300 kcal", strictMatches.size >= 20)
        assertTrue(strictMatches.all { it.food.caloriesKcal in 255..345 })
        // Top match should have high similarity and strong protein parity
        val top = strictMatches.first()
        assertTrue("Top similarity should be >= 75, was ${top.similarityScore}", top.similarityScore >= 75)
        assertEquals(SuitabilityStatus.RECOMMENDED, top.suitability)
    }

    @Test
    fun personalizedAllergyAndConditionRules_hideAllergensAndFlagUnsafeFoods() {
        val catalog = createLoader().foodCatalog
        val diabeticNutAllergicPatient = baselinePatient(
            conditions = listOf("Type 2 Diabetes", "Hypertension (High BP)"),
            allergies = listOf("Peanut Allergy", "Milk Allergy"),
        )
        val snackMeal = DietMealSlot(
            slot = "Mid-Day Snack",
            title_en = "Roasted Chickpeas",
            title_ur = "بھنے چنے",
            portions_en = "1 serving",
            portions_ur = "1 حصہ",
            caloriesApprox = 140,
            proteinG = 7.0,
            carbsG = 20.0,
            fatG = 3.0,
            fiberG = 5.0,
        )

        // When hideAllergens=true, peanut & dairy foods are excluded
        val safeMatches = FoodMatchingEngine.findAlternatives(
            originalMeal = snackMeal,
            catalog = catalog,
            patient = diabeticNutAllergicPatient,
            goal = WellnessGoal.DIABETES_CONTROL,
            hideAllergens = true,
            withinCalorieTargetOnly = false,
        )
        assertFalse(safeMatches.any { it.food.allergens.any { a -> a.contains("Peanut", ignoreCase = true) } })
        assertFalse(safeMatches.any { it.food.allergens.any { a -> a.contains("Milk", ignoreCase = true) } })

        // When hideAllergens=false and searching peanut, allergen risk is explicitly flagged
        val peanutCheck = FoodMatchingEngine.findAlternatives(
            originalMeal = snackMeal,
            catalog = catalog,
            patient = diabeticNutAllergicPatient,
            goal = WellnessGoal.DIABETES_CONTROL,
            query = "peanut",
            hideAllergens = false,
            withinCalorieTargetOnly = false,
        )
        assertTrue(peanutCheck.isNotEmpty())
        assertTrue(peanutCheck.any { it.suitability == SuitabilityStatus.EXCLUDED_ALLERGEN })

        // High natural sugar / honey food is flagged as CAUTION for Diabetes
        val mangoCheck = FoodMatchingEngine.findAlternatives(
            originalMeal = snackMeal,
            catalog = catalog,
            patient = diabeticNutAllergicPatient,
            goal = WellnessGoal.DIABETES_CONTROL,
            query = "mango",
            hideAllergens = true,
            withinCalorieTargetOnly = false,
        )
        assertTrue(mangoCheck.isNotEmpty())
        assertEquals(SuitabilityStatus.CAUTION, mangoCheck.first().suitability)
        assertTrue(mangoCheck.first().suitabilityNotesEn.isNotEmpty())
    }

    @Test
    fun swappingAndRestoringMeal_updatesPlanTotalsMacrosAndPersistsCleanlyViaMoshi() {
        val loader = createLoader()
        val catalog = loader.foodCatalog
        val patient = baselinePatient()
        val plan = RemediesAndPlans.buildPersonalizedPlanOffline(patient, WellnessGoal.BALANCED, variationSeed = 0)

        val initialTotals = FoodMatchingEngine.computeDailyMacros(
            meals = plan.dietMeals,
            mealsChecked = listOf("Breakfast", "Lunch"),
            catalog = catalog,
        )
        assertTrue(initialTotals.totalKcal > 0)
        assertTrue(initialTotals.totalProteinG > 0.0)

        val lunchIdx = plan.dietMeals.indexOfFirst { it.slot == "Lunch" }
        assertTrue(lunchIdx >= 0)
        val originalLunch = plan.dietMeals[lunchIdx]

        // Choose a noticeably different calorie/protein alternative so totals shift deterministically
        val replacement = catalog.first { it.id == "food_pk_chicken_tikka_boti" }
        val swappedLunch = FoodMatchingEngine.applyFoodSwap(originalLunch, replacement, catalog)

        assertTrue(swappedLunch.isSwapped)
        assertNotNull(swappedLunch.originalMealSnapshot)
        assertEquals(replacement.nameEn, swappedLunch.title_en)
        assertEquals(replacement.caloriesKcal, swappedLunch.caloriesApprox)
        assertEquals(replacement.proteinG, swappedLunch.proteinG!!, 0.01)

        val updatedMeals = plan.dietMeals.toMutableList().apply { set(lunchIdx, swappedLunch) }
        val updatedTotals = FoodMatchingEngine.computeDailyMacros(
            meals = updatedMeals,
            mealsChecked = listOf("Breakfast", "Lunch"),
            catalog = catalog,
        )
        val expectedKcalDelta = replacement.caloriesKcal - originalLunch.caloriesApprox
        assertEquals(initialTotals.totalKcal + expectedKcalDelta, updatedTotals.totalKcal)
        assertEquals(initialTotals.loggedKcal + expectedKcalDelta, updatedTotals.loggedKcal)

        // Verify Moshi serialization round-trip preserves swapped state & original snapshot (Room Vault offline persistence)
        val updatedPlan = plan.copy(dietMeals = updatedMeals)
        val adapter = loader.moshi.adapter(PersonalizedWellnessPlan::class.java)
        val json = adapter.toJson(updatedPlan)
        val restoredPlan = adapter.fromJson(json)!!
        assertEquals(replacement.nameEn, restoredPlan.dietMeals[lunchIdx].title_en)
        assertTrue(restoredPlan.dietMeals[lunchIdx].isSwapped)
        assertEquals(originalLunch.title_en, restoredPlan.dietMeals[lunchIdx].originalMealSnapshot?.title_en)

        // Restore original meal reverts everything cleanly
        val revertedLunch = FoodMatchingEngine.restoreOriginalMeal(restoredPlan.dietMeals[lunchIdx])
        assertFalse(revertedLunch.isSwapped)
        assertEquals(originalLunch.title_en, revertedLunch.title_en)
        assertEquals(originalLunch.caloriesApprox, revertedLunch.caloriesApprox)
    }
}
