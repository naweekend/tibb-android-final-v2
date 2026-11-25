package com.example.engine

import com.example.data.model.DietMealSlot
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.HealthProfileCatalog
import com.example.data.model.MealOptionItem
import com.example.data.model.MealSlotType
import com.example.data.model.NutritionDataSource
import com.example.data.model.PatientProfile
import com.example.data.model.WellnessGoal
import com.example.data.model.clinicalAllergies
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class NutritionalMatchConfig(
    val calorieTolerancePct: Double = 0.15,
    val calorieWeight: Double = 0.42,
    val proteinWeight: Double = 0.24,
    val carbsFatWeight: Double = 0.18,
    val fiberWeight: Double = 0.08,
    val mealSlotBonusWeight: Double = 0.08,
)

enum class SuitabilityStatus {
    RECOMMENDED,
    SUITABLE,
    CAUTION,
    EXCLUDED_ALLERGEN,
}

data class ResolvedMealMacros(
    val caloriesKcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sugarG: Double?,
    val nutritionSource: NutritionDataSource,
    val servingSizeEn: String,
    val servingSizeUr: String,
)

data class DailyNutritionTotals(
    val totalKcal: Int,
    val loggedKcal: Int,
    val totalProteinG: Double,
    val loggedProteinG: Double,
    val totalCarbsG: Double,
    val loggedCarbsG: Double,
    val totalFatG: Double,
    val loggedFatG: Double,
    val totalFiberG: Double,
    val loggedFiberG: Double,
)

data class FoodAlternativeMatch(
    val food: FoodItem,
    val similarityScore: Int,
    val isWithinCalorieBand: Boolean,
    val calorieDelta: Int,
    val proteinDeltaG: Double,
    val carbsDeltaG: Double,
    val fatDeltaG: Double,
    val fiberDeltaG: Double,
    val suitability: SuitabilityStatus,
    val matchedAllergens: List<String>,
    val suitabilityNotesEn: List<String>,
    val suitabilityNotesUr: List<String>,
    val comparisonSummaryEn: String,
    val comparisonSummaryUr: String,
)

object FoodMatchingEngine {

    fun resolveMealMacros(
        meal: DietMealSlot,
        catalog: List<FoodItem> = emptyList(),
    ): ResolvedMealMacros {
        val matchedCatalog = meal.foodId?.let { id -> catalog.firstOrNull { it.id == id } }
        if (meal.proteinG != null && meal.carbsG != null && meal.fatG != null && meal.fiberG != null) {
            return ResolvedMealMacros(
                caloriesKcal = meal.caloriesApprox,
                proteinG = meal.proteinG,
                carbsG = meal.carbsG,
                fatG = meal.fatG,
                fiberG = meal.fiberG,
                sugarG = meal.sugarG,
                nutritionSource = parseSource(meal.nutritionSource),
                servingSizeEn = meal.servingSize_en.ifBlank { "1 standard serving" },
                servingSizeUr = meal.servingSize_ur.ifBlank { "1 معیاری حصہ" },
            )
        }
        if (matchedCatalog != null) {
            return ResolvedMealMacros(
                caloriesKcal = matchedCatalog.caloriesKcal,
                proteinG = matchedCatalog.proteinG,
                carbsG = matchedCatalog.carbsG,
                fatG = matchedCatalog.fatG,
                fiberG = matchedCatalog.fiberG,
                sugarG = matchedCatalog.sugarG,
                nutritionSource = matchedCatalog.nutritionSource,
                servingSizeEn = matchedCatalog.servingSizeEn,
                servingSizeUr = matchedCatalog.servingSizeUr,
            )
        }
        val activeAlt = meal.alternatives.getOrNull(meal.selectedOptionIndex)
        if (activeAlt?.proteinG != null && activeAlt.carbsG != null && activeAlt.fatG != null && activeAlt.fiberG != null) {
            return ResolvedMealMacros(
                caloriesKcal = activeAlt.caloriesApprox,
                proteinG = activeAlt.proteinG,
                carbsG = activeAlt.carbsG,
                fatG = activeAlt.fatG,
                fiberG = activeAlt.fiberG,
                sugarG = activeAlt.sugarG,
                nutritionSource = parseSource(activeAlt.nutritionSource),
                servingSizeEn = activeAlt.servingSize_en.ifBlank { "1 standard serving" },
                servingSizeUr = activeAlt.servingSize_ur.ifBlank { "1 معیاری حصہ" },
            )
        }

        val lower = meal.title_en.lowercase()
        val kcal = meal.caloriesApprox.coerceAtLeast(50)
        val proteinShare = when {
            lower.contains("chicken") || lower.contains("fish") || lower.contains("egg") ||
                lower.contains("paneer") || lower.contains("keema") || lower.contains("tikka") ||
                lower.contains("kabab") -> 0.28
            lower.contains("daal") || lower.contains("lentil") || lower.contains("chana") ||
                lower.contains("rajma") || lower.contains("lobia") || lower.contains("dahi") -> 0.19
            lower.contains("salad") || lower.contains("fruit") || lower.contains("guava") ||
                lower.contains("apple") -> 0.08
            else -> 0.16
        }
        val fatShare = when {
            lower.contains("almond") || lower.contains("walnut") || lower.contains("peanut") ||
                lower.contains("paratha") || lower.contains("paneer") || lower.contains("karahi") -> 0.34
            lower.contains("soup") || lower.contains("yakhni") || lower.contains("fruit") ||
                lower.contains("chaach") || lower.contains("guava") -> 0.18
            else -> 0.26
        }
        val carbShare = (1.0 - proteinShare - fatShare).coerceIn(0.15, 0.72)
        val pG = ((kcal * proteinShare) / 4.0 * 10.0).roundToInt() / 10.0
        val cG = ((kcal * carbShare) / 4.0 * 10.0).roundToInt() / 10.0
        val fG = ((kcal * fatShare) / 9.0 * 10.0).roundToInt() / 10.0
        val fibG = when {
            lower.contains("chana") || lower.contains("daal") || lower.contains("oats") ||
                lower.contains("bran") || lower.contains("rajma") || lower.contains("guava") -> 6.2
            lower.contains("salad") || lower.contains("roti") || lower.contains("veg") ||
                lower.contains("sabzi") || lower.contains("khichdi") -> 4.2
            else -> 2.4
        }
        return ResolvedMealMacros(
            caloriesKcal = kcal,
            proteinG = pG,
            carbsG = cG,
            fatG = fG,
            fiberG = fibG,
            sugarG = null,
            nutritionSource = NutritionDataSource.ESTIMATED,
            servingSizeEn = meal.servingSize_en.ifBlank { "1 standard serving" },
            servingSizeUr = meal.servingSize_ur.ifBlank { "1 معیاری حصہ" },
        )
    }

    private fun parseSource(raw: String): NutritionDataSource =
        runCatching { NutritionDataSource.valueOf(raw) }.getOrDefault(NutritionDataSource.ESTIMATED)

    fun computeDailyMacros(
        meals: List<DietMealSlot>,
        mealsChecked: List<String>,
        catalog: List<FoodItem> = emptyList(),
    ): DailyNutritionTotals {
        var totalKcal = 0
        var loggedKcal = 0
        var totalP = 0.0
        var loggedP = 0.0
        var totalC = 0.0
        var loggedC = 0.0
        var totalF = 0.0
        var loggedF = 0.0
        var totalFib = 0.0
        var loggedFib = 0.0

        meals.forEach { meal ->
            val m = resolveMealMacros(meal, catalog)
            val eaten = mealsChecked.contains(meal.slot)
            totalKcal += m.caloriesKcal
            totalP += m.proteinG
            totalC += m.carbsG
            totalF += m.fatG
            totalFib += m.fiberG
            if (eaten) {
                loggedKcal += m.caloriesKcal
                loggedP += m.proteinG
                loggedC += m.carbsG
                loggedF += m.fatG
                loggedFib += m.fiberG
            }
        }
        return DailyNutritionTotals(
            totalKcal = totalKcal,
            loggedKcal = loggedKcal,
            totalProteinG = (totalP * 10).roundToInt() / 10.0,
            loggedProteinG = (loggedP * 10).roundToInt() / 10.0,
            totalCarbsG = (totalC * 10).roundToInt() / 10.0,
            loggedCarbsG = (loggedC * 10).roundToInt() / 10.0,
            totalFatG = (totalF * 10).roundToInt() / 10.0,
            loggedFatG = (loggedF * 10).roundToInt() / 10.0,
            totalFiberG = (totalFib * 10).roundToInt() / 10.0,
            loggedFiberG = (loggedFib * 10).roundToInt() / 10.0,
        )
    }

    fun applyFoodSwap(
        originalMeal: DietMealSlot,
        chosen: FoodItem,
        catalog: List<FoodItem> = emptyList(),
    ): DietMealSlot {
        val currentSnapshot = originalMeal.originalMealSnapshot ?: run {
            val resolved = resolveMealMacros(originalMeal, catalog)
            MealOptionItem(
                title_en = originalMeal.title_en,
                title_ur = originalMeal.title_ur,
                portions_en = originalMeal.portions_en,
                portions_ur = originalMeal.portions_ur,
                caloriesApprox = resolved.caloriesKcal,
                foodId = originalMeal.foodId,
                servingSize_en = resolved.servingSizeEn,
                servingSize_ur = resolved.servingSizeUr,
                proteinG = resolved.proteinG,
                carbsG = resolved.carbsG,
                fatG = resolved.fatG,
                fiberG = resolved.fiberG,
                sugarG = resolved.sugarG,
                nutritionSource = resolved.nutritionSource.name,
                dietaryTags = originalMeal.dietaryTags,
            )
        }
        return originalMeal.copy(
            title_en = chosen.nameEn,
            title_ur = chosen.nameUr,
            portions_en = "${chosen.servingSizeEn} · ${chosen.benefitsEn}",
            portions_ur = "${chosen.servingSizeUr} · ${chosen.benefitsUr}",
            caloriesApprox = chosen.caloriesKcal,
            foodId = chosen.id,
            servingSize_en = chosen.servingSizeEn,
            servingSize_ur = chosen.servingSizeUr,
            proteinG = chosen.proteinG,
            carbsG = chosen.carbsG,
            fatG = chosen.fatG,
            fiberG = chosen.fiberG,
            sugarG = chosen.sugarG,
            nutritionSource = chosen.nutritionSource.name,
            dietaryTags = chosen.dietaryTags,
            originalMealSnapshot = currentSnapshot,
            isSwapped = true,
        )
    }

    fun restoreOriginalMeal(meal: DietMealSlot): DietMealSlot {
        val orig = meal.originalMealSnapshot ?: return meal.copy(isSwapped = false)
        return meal.copy(
            title_en = orig.title_en,
            title_ur = orig.title_ur,
            portions_en = orig.portions_en,
            portions_ur = orig.portions_ur,
            caloriesApprox = orig.caloriesApprox,
            foodId = orig.foodId,
            servingSize_en = orig.servingSize_en,
            servingSize_ur = orig.servingSize_ur,
            proteinG = orig.proteinG,
            carbsG = orig.carbsG,
            fatG = orig.fatG,
            fiberG = orig.fiberG,
            sugarG = orig.sugarG,
            nutritionSource = orig.nutritionSource,
            dietaryTags = orig.dietaryTags,
            originalMealSnapshot = orig,
            isSwapped = false,
        )
    }

    fun findAlternatives(
        originalMeal: DietMealSlot,
        catalog: List<FoodItem>,
        patient: PatientProfile,
        goal: WellnessGoal,
        query: String = "",
        categoryFilter: FoodCategory? = null,
        hideAllergens: Boolean = true,
        withinCalorieTargetOnly: Boolean = false,
        config: NutritionalMatchConfig = NutritionalMatchConfig(),
    ): List<FoodAlternativeMatch> {
        val targetMacros = resolveMealMacros(originalMeal, catalog)
        val slotType = MealSlotType.fromSlotName(originalMeal.slot)
        val trimmedQuery = query.trim().lowercase()

        val activeAllergies = patient.clinicalAllergies()
        val hasDiabetes = HealthProfileCatalog.matchesCondition(patient.chronicConditions, "diabetes") ||
            goal == WellnessGoal.DIABETES_CONTROL
        val hasBp = HealthProfileCatalog.matchesCondition(patient.chronicConditions, "hypertension") ||
            goal == WellnessGoal.BP_FRIENDLY
        val hasHeartOrCholesterol = HealthProfileCatalog.matchesCondition(patient.chronicConditions, "heart") ||
            HealthProfileCatalog.matchesCondition(patient.chronicConditions, "cholesterol") ||
            HealthProfileCatalog.matchesCondition(patient.chronicConditions, "liver")
        val hasKidney = HealthProfileCatalog.matchesCondition(patient.chronicConditions, "kidney")
        val hasGout = HealthProfileCatalog.matchesCondition(patient.chronicConditions, "gout")
        val hasGerd = HealthProfileCatalog.matchesCondition(patient.chronicConditions, "gerd") ||
            HealthProfileCatalog.matchesCondition(patient.chronicConditions, "gastritis")

        val prefVeg = patient.dietaryPreferences.any { it.contains("veg", ignoreCase = true) }
        val prefVegan = patient.dietaryPreferences.any { it.contains("vegan", ignoreCase = true) }
        val prefLowSalt = patient.dietaryPreferences.any { it.contains("salt", ignoreCase = true) || it.contains("sodium", ignoreCase = true) }
        val prefSugarFree = patient.dietaryPreferences.any { it.contains("sugar", ignoreCase = true) }
        val prefDairyFree = patient.dietaryPreferences.any { it.contains("dairy", ignoreCase = true) || it.contains("lactose", ignoreCase = true) }
        val prefGlutenFree = patient.dietaryPreferences.any { it.contains("gluten", ignoreCase = true) }
        val prefHighProtein = patient.dietaryPreferences.any { it.contains("protein", ignoreCase = true) }

        return catalog.asSequence()
            .filter { food ->
                categoryFilter == null || food.category == categoryFilter
            }
            .filter { food ->
                if (trimmedQuery.isEmpty()) true
                else {
                    food.nameEn.lowercase().contains(trimmedQuery) ||
                        food.nameUr.lowercase().contains(trimmedQuery) ||
                        food.benefitsEn.lowercase().contains(trimmedQuery) ||
                        food.category.labelEn.lowercase().contains(trimmedQuery) ||
                        food.dietaryTags.any { it.lowercase().contains(trimmedQuery) }
                }
            }
            .mapNotNull { food ->
                val matchedAllergens = detectAllergens(activeAllergies, food.allergens, food.nameEn)
                if (hideAllergens && matchedAllergens.isNotEmpty()) {
                    return@mapNotNull null
                }

                val calDelta = food.caloriesKcal - targetMacros.caloriesKcal
                val calTolerance = max(25.0, targetMacros.caloriesKcal * config.calorieTolerancePct)
                val withinBand = abs(calDelta) <= calTolerance
                if (withinCalorieTargetOnly && !withinBand) {
                    return@mapNotNull null
                }

                val calRelErr = abs(calDelta) / max(80.0, targetMacros.caloriesKcal.toDouble())
                val calScore = (1.0 - (calRelErr / 0.55)).coerceIn(0.0, 1.0)

                val pDelta = ((food.proteinG - targetMacros.proteinG) * 10).roundToInt() / 10.0
                val pDenom = max(8.0, targetMacros.proteinG)
                val pScore = (1.0 - (abs(pDelta) / (pDenom * 1.3))).coerceIn(0.0, 1.0)

                val cDelta = ((food.carbsG - targetMacros.carbsG) * 10).roundToInt() / 10.0
                val fDelta = ((food.fatG - targetMacros.fatG) * 10).roundToInt() / 10.0
                val fibDelta = ((food.fiberG - targetMacros.fiberG) * 10).roundToInt() / 10.0

                val cDenom = max(15.0, targetMacros.carbsG)
                val fDenom = max(6.0, targetMacros.fatG)
                val cScore = (1.0 - (abs(cDelta) / (cDenom * 1.4))).coerceIn(0.0, 1.0)
                val fScore = (1.0 - (abs(fDelta) / (fDenom * 1.5))).coerceIn(0.0, 1.0)
                val cfScore = (cScore + fScore) / 2.0

                val fibScore = if (targetMacros.fiberG <= 1.0) 1.0
                else (1.0 - (max(0.0, targetMacros.fiberG - food.fiberG) / max(4.0, targetMacros.fiberG))).coerceIn(0.0, 1.0)

                val slotScore = if (food.suitableMeals.contains(slotType)) 1.0 else 0.35

                val rawScore = (calScore * config.calorieWeight +
                    pScore * config.proteinWeight +
                    cfScore * config.carbsFatWeight +
                    fibScore * config.fiberWeight +
                    slotScore * config.mealSlotBonusWeight) * 100.0

                val notesEn = mutableListOf<String>()
                val notesUr = mutableListOf<String>()
                var isCaution = false
                var bonusPoints = 0

                if (matchedAllergens.isNotEmpty()) {
                    notesEn += "Contains recorded allergen: ${matchedAllergens.joinToString(", ")}"
                    notesUr += "ریکارڈ شدہ الرجی شامل ہے: ${matchedAllergens.joinToString("، ")}"
                }

                if (hasDiabetes || prefSugarFree) {
                    val sugarVal = food.sugarG ?: 0.0
                    val highGlycemic = food.glycemicIndexTier.equals("HIGH", ignoreCase = true) ||
                        food.conditionFlags.contains("ADDED_SUGAR_HONEY") ||
                        food.conditionFlags.contains("HIGH_NATURAL_SUGAR") ||
                        sugarVal >= 18.0
                    if (highGlycemic) {
                        isCaution = true
                        notesEn += "Higher natural/added sugar or glycemic load — monitor portion for glucose control"
                        notesUr += "شوگر یا گلائسیمک اثر زیادہ ہے — مقدار کا خاص خیال رکھیں"
                    } else if (food.glycemicIndexTier.equals("LOW", ignoreCase = true) &&
                        (food.dietaryTags.contains("Diabetes-Friendly") || food.dietaryTags.contains("Low-GI"))
                    ) {
                        bonusPoints += 4
                        notesEn += "Low-GI glucose-steady choice"
                        notesUr += "لو گلائسیمک اور شوگر کے لیے متوازن انتخاب"
                    }
                }

                if (hasBp || prefLowSalt) {
                    if (food.sodiumLevel.equals("HIGH", ignoreCase = true) || food.conditionFlags.contains("HIGH_SODIUM")) {
                        isCaution = true
                        notesEn += "Higher sodium — prepare with reduced salt for blood pressure safety"
                        notesUr += "نمک زیادہ ہو سکتا ہے — بلڈ پریشر کے لیے کم نمک استعمال کریں"
                    } else if (food.sodiumLevel.equals("LOW", ignoreCase = true)) {
                        bonusPoints += 3
                    }
                }

                if (hasHeartOrCholesterol && food.conditionFlags.contains("HIGH_FAT")) {
                    isCaution = true
                    notesEn += "Higher saturated fat — trim fat or keep portion small"
                    notesUr += "چکنائی زیادہ ہے — مقدار محدود رکھیں"
                }

                if (hasKidney && (food.conditionFlags.contains("HIGH_POTASSIUM") ||
                        food.conditionFlags.contains("HIGH_POTASSIUM_PHOSPHORUS"))
                ) {
                    isCaution = true
                    notesEn += "Notable potassium/phosphorus — verify against kidney diet limits"
                    notesUr += "پوٹاشیم/فاسفورس زیادہ ہے — گردے کی خوراک کے مطابق احتیاط کریں"
                }

                if (hasGout && food.conditionFlags.contains("HIGH_PURINE")) {
                    isCaution = true
                    notesEn += "Purine-rich meat/seafood — limit intake if managing gout or uric acid"
                    notesUr += "یورک ایسڈ یا گاؤٹ میں مقدار محدود رکھیں"
                }

                if (hasGerd && food.conditionFlags.contains("SPICY")) {
                    isCaution = true
                    notesEn += "Moderately spiced — prepare mild for reflux/gastritis comfort"
                    notesUr += "تیزابیت یا معدے کی جلن میں مرچ مصالحہ کم رکھیں"
                }

                if (prefVeg && !food.dietaryTags.any { it.equals("Vegetarian", ignoreCase = true) }) {
                    isCaution = true
                    notesEn += "Contains meat/poultry/fish (outside Vegetarian preference)"
                    notesUr += "گوشت یا مچھلی شامل ہے (سبزی خور ترجیح سے مختلف)"
                }
                if (prefVegan && !food.dietaryTags.any { it.equals("Vegan", ignoreCase = true) }) {
                    isCaution = true
                    notesEn += "Contains animal-derived ingredients (outside Vegan preference)"
                    notesUr += "حیوانی اجزاء شامل ہیں"
                }
                if (prefDairyFree && food.allergens.any { it.contains("Milk", ignoreCase = true) || it.contains("Dairy", ignoreCase = true) }) {
                    isCaution = true
                    notesEn += "Contains dairy"
                    notesUr += "دودھ یا ڈیری شامل ہے"
                }
                if (prefGlutenFree && food.allergens.any { it.contains("Wheat", ignoreCase = true) || it.contains("Gluten", ignoreCase = true) }) {
                    isCaution = true
                    notesEn += "Contains wheat/gluten"
                    notesUr += "گندم یا گلوٹین شامل ہے"
                }
                if (prefHighProtein && food.proteinG >= 18.0) {
                    bonusPoints += 3
                }

                val finalScore = (rawScore + bonusPoints - if (isCaution) 12 else 0).roundToInt().coerceIn(1, 99)

                val status = when {
                    matchedAllergens.isNotEmpty() -> SuitabilityStatus.EXCLUDED_ALLERGEN
                    isCaution -> SuitabilityStatus.CAUTION
                    withinBand && finalScore >= 72 -> SuitabilityStatus.RECOMMENDED
                    else -> SuitabilityStatus.SUITABLE
                }

                val signCal = if (calDelta > 0) "+$calDelta" else "$calDelta"
                val signProt = if (pDelta > 0) "+$pDelta" else "$pDelta"
                val summaryEn = "${food.caloriesKcal} kcal ($signCal vs meal) · ${food.proteinG}g protein ($signProt g)"
                val summaryUr = "${food.caloriesKcal} کیلوریز ($signCal) · ${food.proteinG} گرام پروٹین"

                FoodAlternativeMatch(
                    food = food,
                    similarityScore = finalScore,
                    isWithinCalorieBand = withinBand,
                    calorieDelta = calDelta,
                    proteinDeltaG = pDelta,
                    carbsDeltaG = cDelta,
                    fatDeltaG = fDelta,
                    fiberDeltaG = fibDelta,
                    suitability = status,
                    matchedAllergens = matchedAllergens,
                    suitabilityNotesEn = notesEn,
                    suitabilityNotesUr = notesUr,
                    comparisonSummaryEn = summaryEn,
                    comparisonSummaryUr = summaryUr,
                )
            }
            .sortedWith(
                compareBy<FoodAlternativeMatch> { match ->
                    when (match.suitability) {
                        SuitabilityStatus.RECOMMENDED -> 0
                        SuitabilityStatus.SUITABLE -> 1
                        SuitabilityStatus.CAUTION -> 2
                        SuitabilityStatus.EXCLUDED_ALLERGEN -> 3
                    }
                }.thenByDescending { it.isWithinCalorieBand }
                    .thenByDescending { it.similarityScore }
            )
            .toList()
    }

    private fun detectAllergens(
        profileAllergies: List<String>,
        foodAllergens: List<String>,
        foodNameEn: String,
    ): List<String> {
        if (profileAllergies.isEmpty()) return emptyList()
        val matched = mutableListOf<String>()
        profileAllergies.forEach { profileAllergy ->
            val token = profileAllergy.lowercase()
                .removeSuffix("allergy")
                .trim()
            val directHit = foodAllergens.any { fa ->
                val faLower = fa.lowercase()
                when {
                    token.contains("peanut") -> faLower.contains("peanut")
                    token.contains("tree nut") || token.contains("almond") || token.contains("walnut") || token.contains("nut") ->
                        faLower.contains("tree nut")
                    token.contains("milk") || token.contains("dairy") || token.contains("lactose") ->
                        faLower.contains("milk") || faLower.contains("dairy")
                    token.contains("egg") -> faLower.contains("egg")
                    token.contains("wheat") || token.contains("gluten") ->
                        faLower.contains("wheat") || faLower.contains("gluten")
                    token.contains("soy") -> faLower.contains("soy")
                    token.contains("fish") -> faLower == "fish" || faLower.contains("fish") && !faLower.contains("shellfish")
                    token.contains("shellfish") || token.contains("prawn") || token.contains("shrimp") ->
                        faLower.contains("shellfish")
                    token.contains("sesame") -> faLower.contains("sesame")
                    else -> token.length >= 3 && (faLower.contains(token) || foodNameEn.lowercase().contains(token))
                }
            }
            val nameHit = token.length >= 3 && when {
                token.contains("milk") || token.contains("dairy") -> false // covered via foodAllergens
                token.contains("tree nut") -> false
                token.contains("fish") && !token.contains("shellfish") -> false
                else -> foodNameEn.lowercase().contains(token)
            }
            if (directHit || nameHit) {
                matched += HealthProfileCatalog.allergyDisplayLabel(profileAllergy, isUr = false)
            }
        }
        return matched.distinct()
    }
}
