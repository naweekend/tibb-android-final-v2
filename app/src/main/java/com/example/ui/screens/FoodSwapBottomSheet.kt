package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.DietMealSlot
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.HealthProfileCatalog
import com.example.data.model.NutritionDataSource
import com.example.data.model.PatientProfile
import com.example.data.model.WellnessGoal
import com.example.data.model.clinicalAllergies
import com.example.data.model.clinicalChronicConditions
import com.example.engine.FoodAlternativeMatch
import com.example.engine.FoodMatchingEngine
import com.example.engine.SuitabilityStatus
import com.example.ui.components.TibbStatusBadge
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodSwapBottomSheet(
    locale: AppLocale,
    mealSlot: DietMealSlot,
    foodCatalog: List<FoodItem>,
    patient: PatientProfile,
    goal: WellnessGoal,
    onDismiss: () -> Unit,
    onSelectFood: (FoodItem) -> Unit,
    onRestoreOriginal: () -> Unit,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FoodCategory?>(null) }
    var withinCalorieTargetOnly by remember { mutableStateOf(true) }
    var hideAllergens by remember { mutableStateOf(true) }

    val targetMacros = remember(mealSlot, foodCatalog) {
        FoodMatchingEngine.resolveMealMacros(mealSlot, foodCatalog)
    }

    val matches = remember(
        mealSlot,
        foodCatalog,
        patient,
        goal,
        searchQuery,
        selectedCategory,
        withinCalorieTargetOnly,
        hideAllergens,
    ) {
        FoodMatchingEngine.findAlternatives(
            originalMeal = mealSlot,
            catalog = foodCatalog,
            patient = patient,
            goal = goal,
            query = searchQuery,
            categoryFilter = selectedCategory,
            hideAllergens = hideAllergens,
            withinCalorieTargetOnly = withinCalorieTargetOnly,
        )
    }

    val activeAllergies = remember(patient) { patient.clinicalAllergies() }
    val activeConditions = remember(patient) { patient.clinicalChronicConditions() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.bgTop,
        contentColor = c.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUr) "${mealSlot.slot} کا متبادل منتخب کریں" else "Choose ${mealSlot.slot} Alternative",
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                        color = c.ink,
                    )
                    Text(
                        text = if (isUr) {
                            "${foodCatalog.size} مستند مقامی و بین الاقوامی غذائیں (آف لائن)"
                        } else {
                            "Ranked by nutritional match from ${foodCatalog.size} offline foods"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("food_swap_close_btn"),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = if (isUr) "بند کریں" else "Close food selector",
                        tint = c.muted,
                    )
                }
            }

            // Current / Original Meal Comparison Header
            val referenceMeal = mealSlot.originalMealSnapshot
            TibbGlassCard(padding = 12.dp, borderColor = c.accent.copy(alpha = 0.35f)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TibbStatusBadge(
                            text = if (mealSlot.isSwapped) {
                                if (isUr) "موجودہ منتخب غذا" else "Current Selection"
                            } else {
                                if (isUr) "اصل کھانا" else "Original Meal"
                            },
                            color = c.accent,
                            softBg = c.accentSoft,
                        )
                        if (mealSlot.isSwapped && referenceMeal != null) {
                            TibbSecondaryPillButton(
                                onClick = onRestoreOriginal,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("food_swap_restore_btn"),
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isUr) "اصل بحال کریں" else "Restore Original",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isUr) mealSlot.title_ur else mealSlot.title_en,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleSmall) else MaterialTheme.typography.titleSmall,
                        color = c.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${targetMacros.caloriesKcal} kcal",
                            style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                            color = c.accent,
                        )
                        Text(
                            text = "P ${targetMacros.proteinG}g · C ${targetMacros.carbsG}g · F ${targetMacros.fatG}g · Fib ${targetMacros.fiberG}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (mealSlot.isSwapped && referenceMeal != null) {
                        Text(
                            text = if (isUr) {
                                "اصل: ${referenceMeal.title_ur} (${referenceMeal.caloriesApprox} kcal)"
                            } else {
                                "Original: ${referenceMeal.title_en} (${referenceMeal.caloriesApprox} kcal)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = c.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Personalization Safety Summary Banner
            if (activeAllergies.isNotEmpty() || activeConditions.isNotEmpty() || patient.dietaryPreferences.isNotEmpty()) {
                val filterItems = buildList {
                    if (activeAllergies.isNotEmpty()) {
                        add(
                            if (isUr) "الرجی تحفظ: ${activeAllergies.size}" else "Allergy Filter (${activeAllergies.size})",
                        )
                    }
                    if (activeConditions.isNotEmpty()) {
                        add(
                            if (isUr) "طبی شرائط: ${activeConditions.size}" else "Health Profile (${activeConditions.size})",
                        )
                    }
                    if (patient.dietaryPreferences.isNotEmpty()) {
                        add(patient.dietaryPreferences.joinToString(", "))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.wellnessBlueSoft.copy(alpha = 0.7f))
                        .border(1.dp, c.wellnessBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.HealthAndSafety,
                        contentDescription = null,
                        tint = c.wellnessBlue,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = filterItems.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isUr) "کھانا تلاش کریں (مثلاً دال، چکن، چپاتی، دہی)" else "Search foods (e.g., Daal, Chicken, Chapati, Yogurt)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = c.muted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search", tint = c.muted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.border,
                    focusedContainerColor = c.surface,
                    unfocusedContainerColor = c.surface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("food_swap_search_input"),
            )

            // Calorie Match & Allergen Filter Toggles
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterToggleChip(
                    label = if (isUr) "±15% کیلوریز میچ" else "±15% Calorie Match",
                    selected = withinCalorieTargetOnly,
                    accentColor = c.safeGreen,
                    onClick = { withinCalorieTargetOnly = !withinCalorieTargetOnly },
                    testTag = "food_swap_calorie_filter_chip",
                )
                if (activeAllergies.isNotEmpty()) {
                    FilterToggleChip(
                        label = if (isUr) "الرجی والی غذائیں چھپائیں" else "Hide Allergens",
                        selected = hideAllergens,
                        accentColor = c.wellnessBlue,
                        onClick = { hideAllergens = !hideAllergens },
                        testTag = "food_swap_allergen_toggle",
                    )
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CategoryPillChip(
                    label = if (isUr) "تمام (${foodCatalog.size})" else "All (${foodCatalog.size})",
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    testTag = "food_swap_cat_all",
                )
                FoodCategory.entries.forEach { cat ->
                    CategoryPillChip(
                        label = if (isUr) cat.labelUr else cat.labelEn,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        testTag = "food_swap_cat_${cat.name.lowercase()}",
                    )
                }
            }

            // Results Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isUr) "${matches.size} متبادل دستیاب ہیں" else "${matches.size} alternatives shown",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted,
                )
                if (withinCalorieTargetOnly && matches.isEmpty()) {
                    Text(
                        text = if (isUr) "تمام کیلوریز دکھائیں" else "Show all calories",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = c.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { withinCalorieTargetOnly = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Alternatives List
            if (matches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = if (isUr) "اس فلٹر میں کوئی غذا نہیں ملی۔" else "No foods matched the active filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.muted,
                        )
                        TibbSecondaryPillButton(
                            onClick = {
                                searchQuery = ""
                                selectedCategory = null
                                withinCalorieTargetOnly = false
                            },
                        ) {
                            Text(if (isUr) "فلٹرز صاف کریں" else "Broaden Filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(matches, key = { it.food.id }) { match ->
                        FoodAlternativeCard(
                            locale = locale,
                            match = match,
                            targetCalories = targetMacros.caloriesKcal,
                            onSelect = { onSelectFood(match.food) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterToggleChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accentColor.copy(alpha = 0.14f) else c.surface)
            .border(1.dp, if (selected) accentColor else c.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = if (selected) accentColor else c.ink,
        )
    }
}

@Composable
private fun CategoryPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) c.ctaBg else c.surfaceMuted)
            .border(1.dp, if (selected) c.ctaBg else c.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) c.ctaInk else c.ink,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodAlternativeCard(
    locale: AppLocale,
    match: FoodAlternativeMatch,
    targetCalories: Int,
    onSelect: () -> Unit,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val food = match.food

    val statusColor = when (match.suitability) {
        SuitabilityStatus.RECOMMENDED -> c.safeGreen
        SuitabilityStatus.SUITABLE -> c.wellnessBlue
        SuitabilityStatus.CAUTION -> c.amber
        SuitabilityStatus.EXCLUDED_ALLERGEN -> c.dangerRed
    }
    val statusBg = when (match.suitability) {
        SuitabilityStatus.RECOMMENDED -> c.safeGreenSoft
        SuitabilityStatus.SUITABLE -> c.wellnessBlueSoft
        SuitabilityStatus.CAUTION -> c.amberSoft
        SuitabilityStatus.EXCLUDED_ALLERGEN -> c.dangerSoft
    }
    val statusText = when (match.suitability) {
        SuitabilityStatus.RECOMMENDED -> if (isUr) "بہترین میچ (${match.similarityScore}%)" else "Best Match · ${match.similarityScore}%"
        SuitabilityStatus.SUITABLE -> if (isUr) "موزوں (${match.similarityScore}%)" else "Suitable · ${match.similarityScore}%"
        SuitabilityStatus.CAUTION -> if (isUr) "احتیاط" else "Caution"
        SuitabilityStatus.EXCLUDED_ALLERGEN -> if (isUr) "الرجی انتباہ" else "Allergen Risk"
    }

    val borderCol = when (match.suitability) {
        SuitabilityStatus.EXCLUDED_ALLERGEN -> c.dangerRed.copy(alpha = 0.65f)
        SuitabilityStatus.CAUTION -> c.amber.copy(alpha = 0.55f)
        SuitabilityStatus.RECOMMENDED -> c.safeGreen.copy(alpha = 0.45f)
        SuitabilityStatus.SUITABLE -> c.border
    }

    TibbGlassCard(
        padding = 14.dp,
        borderColor = borderCol,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isUr) food.nameUr else food.nameEn,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                        color = c.ink,
                    )
                    Text(
                        text = "${if (isUr) food.category.labelUr else food.category.labelEn} · ${if (isUr) food.servingSizeUr else food.servingSizeEn}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted,
                    )
                }
                Spacer(Modifier.width(8.dp))
                TibbStatusBadge(
                    text = statusText,
                    color = statusColor,
                    softBg = statusBg,
                )
            }

            // Calorie Comparison Row vs Original Meal
            val calDeltaSign = if (match.calorieDelta > 0) "+${match.calorieDelta}" else "${match.calorieDelta}"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.surfaceMuted.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${food.caloriesKcal} kcal",
                        style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                        color = c.ink,
                    )
                    Text(
                        text = "($calDeltaSign vs $targetCalories kcal)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (match.isWithinCalorieBand) c.safeGreen else c.muted,
                    )
                }
                val sourceLabel = when (food.nutritionSource) {
                    NutritionDataSource.MEASURED -> if (isUr) "ناپا گیا" else "Measured"
                    NutritionDataSource.ESTIMATED -> if (isUr) "تخمینی" else "Estimated"
                    NutritionDataSource.UNAVAILABLE -> if (isUr) "محدود" else "Limited"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = if (food.nutritionSource == NutritionDataSource.MEASURED) c.safeGreen else c.muted,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted,
                    )
                }
            }

            // Macro Breakdown Strip
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MacroChip("Protein", "${food.proteinG}g", match.proteinDeltaG)
                MacroChip("Carbs", "${food.carbsG}g", match.carbsDeltaG)
                MacroChip("Fat", "${food.fatG}g", match.fatDeltaG)
                MacroChip("Fiber", "${food.fiberG}g", match.fiberDeltaG)
                val sugarText = if (food.sugarG != null) "${food.sugarG}g" else "N/A"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.surface)
                        .border(1.dp, c.border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Sugar $sugarText",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted,
                    )
                }
            }

            // Benefits text
            Text(
                text = if (isUr) food.benefitsUr else food.benefitsEn,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                color = c.muted,
            )

            // Suitability / Caution Notes
            val notes = if (isUr) match.suitabilityNotesUr else match.suitabilityNotesEn
            if (notes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusBg.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    notes.forEach { note ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp).padding(top = 2.dp),
                            )
                            Text(
                                text = note,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.ink,
                            )
                        }
                    }
                }
            }

            // Dietary Tags + Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    food.dietaryTags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(c.surfaceMuted)
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.muted,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                TibbPrimaryPillButton(
                    onClick = onSelect,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("food_swap_item_${food.id}"),
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isUr) "منتخب کریں" else "Select")
                }
            }
        }
    }
}

@Composable
private fun MacroChip(
    label: String,
    value: String,
    delta: Double,
) {
    val c = LocalTibbColors.current
    val roundedDelta = (delta * 10).roundToInt() / 10.0
    val deltaText = when {
        roundedDelta > 0.4 -> " (+$roundedDelta)"
        roundedDelta < -0.4 -> " ($roundedDelta)"
        else -> ""
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$label $value$deltaText",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = c.ink,
        )
    }
}
