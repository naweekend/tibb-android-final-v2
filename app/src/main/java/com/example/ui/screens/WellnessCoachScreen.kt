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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.DailyWellnessLog
import com.example.data.model.FoodItem
import com.example.data.model.PatientProfile
import com.example.data.model.PersonalizedWellnessPlan
import com.example.data.model.TriageTier
import com.example.data.model.WellnessGoal
import com.example.engine.FoodMatchingEngine
import com.example.engine.HelpTopic
import com.example.engine.RemediesAndPlans
import com.example.engine.TrilingualStrings
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.TibbCircularTimerRing
import com.example.ui.components.TibbClinicalCallout
import com.example.ui.components.TibbDistributionSlice
import com.example.ui.components.TibbDonutGauge
import com.example.ui.components.TibbLinearRangeBar
import com.example.ui.components.TibbMedicalInfoBlock
import com.example.ui.components.TibbRangeZone
import com.example.ui.components.TibbSectionHeader
import com.example.ui.components.TibbSegmentedDistributionBar
import com.example.ui.components.TibbStatPill
import com.example.ui.components.TibbStatusBadge
import com.example.ui.components.workout.SilhouetteGender
import com.example.ui.components.workout.WorkoutExerciseCard
import com.example.ui.theme.InterHeroStatStyle
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbExpandableSection
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.TibbSegmentedTabs
import com.example.ui.theme.urduTextStyle
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WellnessCoachScreen(
    locale: AppLocale,
    activePerson: PatientProfile?,
    todayLog: DailyWellnessLog?,
    foodCatalog: List<FoodItem> = emptyList(),
    onGeneratePlan: (WellnessGoal, Int) -> Unit,
    onUpdatePlan: (PersonalizedWellnessPlan) -> Unit,
    onUpdateWellness: (DailyWellnessLog) -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    if (activePerson == null || todayLog == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(s.onboardingSubtitle, color = c.muted)
        }
        return
    }

    var subTab by remember { mutableIntStateOf(0) } // 0 = Diet Plan, 1 = Workout, 2 = Remedies
    var selectedGoal by remember(activePerson.id) { mutableStateOf(activePerson.preferredGoal) }
    var variationSeed by remember { mutableIntStateOf(0) }
    var remedyFilter by remember { mutableStateOf("ALL") }
    var expandedRemedyId by remember { mutableStateOf<String?>(null) }
    var activeSwapSlotIdx by remember { mutableStateOf<Int?>(null) }

    var workoutSec by remember { mutableIntStateOf(0) }
    var workoutRunning by remember { mutableStateOf(false) }
    LaunchedEffect(workoutRunning) {
        while (workoutRunning) {
            delay(1000L)
            workoutSec++
        }
    }

    val bmi = remember(activePerson) {
        RemediesAndPlans.computeBmiAssessment(activePerson.weightKg, activePerson.heightCm, activePerson.ageMonths)
    }
    val plan = todayLog.plan ?: remember(activePerson.id, selectedGoal, variationSeed) {
        RemediesAndPlans.buildPersonalizedPlanOffline(activePerson, selectedGoal, variationSeed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Compact BMI & BLUE (Wellness) Classification Header
        TibbGlassCard(padding = 16.dp, borderColor = c.wellnessBlue.copy(alpha = 0.42f)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TibbStatusBadge(
                        text = s.blueWellnessLabel,
                        color = c.wellnessBlue,
                        softBg = c.wellnessBlueSoft,
                        modifier = Modifier.testTag("coach_blue_wellness_badge"),
                    )
                    SectionHelpButton(topic = HelpTopic.COACH_BMI, locale = locale)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isUr) "جنوبی ایشیائی بی ایم آئی" else "South Asian BMI",
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                            color = c.ink,
                        )
                        Text(
                            text = if (isUr) bmi.labelUr else bmi.labelEn,
                            style = MaterialTheme.typography.labelMedium,
                            color = c.accent,
                        )
                    }
                    if (bmi.bmi != null) {
                        Text(
                            text = "${bmi.bmi}",
                            style = InterHeroStatStyle,
                            color = c.accent,
                        )
                    }
                }
                val bmiVal = bmi.bmi
                if (bmiVal != null) {
                    val bmiFloat = bmiVal.toFloat()
                    val markerFrac = when {
                        bmiFloat < 18.5f -> ((bmiFloat - 13f) / 5.5f).coerceIn(0.02f, 0.98f) * 0.22f
                        bmiFloat < 23f -> 0.22f + ((bmiFloat - 18.5f) / 4.5f).coerceIn(0f, 1f) * 0.27f
                        bmiFloat < 25f -> 0.49f + ((bmiFloat - 23f) / 2f).coerceIn(0f, 1f) * 0.23f
                        else -> 0.72f + ((bmiFloat - 25f) / 10f).coerceIn(0f, 1f) * 0.26f
                    }
                    val markerCol = when {
                        bmiFloat < 18.5f -> c.wellnessBlue
                        bmiFloat < 23f -> c.safeGreen
                        bmiFloat < 25f -> c.amber
                        else -> c.dangerRed
                    }
                    TibbLinearRangeBar(
                        zones = listOf(
                            TibbRangeZone("<18.5", 2.2f, c.wellnessBlue),
                            TibbRangeZone("18.5–22.9 Healthy", 2.7f, c.safeGreen),
                            TibbRangeZone("23–24.9 Over", 2.3f, c.amber),
                            TibbRangeZone("≥25 High", 2.8f, c.dangerRed),
                        ),
                        markerFraction = markerFrac,
                        markerColor = markerCol,
                        accessibilityLabel = "South Asian BMI range indicator",
                    )
                }
                Text(if (isUr) bmi.adviceUr else bmi.adviceEn, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }

        // Unified Subtabs
        TibbSegmentedTabs(
            options = listOf(
                0 to (if (isUr) "غذا پلان" else "Diet Plan"),
                1 to (if (isUr) "ورزش" else "Workout"),
                2 to (if (isUr) "گھریلو علاج" else "Remedies"),
            ),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "coach_subtab",
        )

        when (subTab) {
            0 -> {
                // Collapsed-by-default Goal Picker
                TibbExpandableSection(
                    title = if (isUr) "صحت کا ہدف تبدیل کریں" else "Wellness Goal",
                    summary = if (isUr) selectedGoal.titleUr else selectedGoal.titleEn,
                    icon = Icons.Rounded.Flag,
                    initiallyExpanded = false,
                    headerTestTag = "coach_goal_expand_btn",
                    trailingHeaderContent = {
                        SectionHelpButton(topic = HelpTopic.COACH_DIET, locale = locale)
                    },
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WellnessGoal.entries.forEach { g ->
                            val sel = selectedGoal == g
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (sel) c.ctaBg else c.surfaceMuted)
                                    .border(1.dp, if (sel) c.ctaBg else c.border, RoundedCornerShape(999.dp))
                                    .clickable {
                                        selectedGoal = g
                                        onGeneratePlan(g, variationSeed)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(if (isUr) g.titleUr else g.titleEn, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                            }
                        }
                    }
                }

                // Single Primary Action CTA
                TibbPrimaryPillButton(
                    onClick = {
                        variationSeed++
                        onGeneratePlan(selectedGoal, variationSeed)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("coach_rotate_plan_btn"),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isUr) "نیا دیسی مینو بنائیں" else "Generate / Rotate Desi Menu")
                }

                // Daily Meal Progress & Calorie Share Visualization
                val totalMeals = plan.dietMeals.size.coerceAtLeast(1)
                val eatenMealsCount = plan.dietMeals.count { todayLog.mealsChecked.contains(it.slot) }
                val dailyMacros = remember(plan.dietMeals, todayLog.mealsChecked, foodCatalog) {
                    FoodMatchingEngine.computeDailyMacros(plan.dietMeals, todayLog.mealsChecked, foodCatalog)
                }
                val totalKcal = dailyMacros.totalKcal
                val eatenKcal = dailyMacros.loggedKcal
                val slotPalette = listOf(c.accent, c.wellnessBlue, c.amber, c.safeGreen)

                TibbGlassCard(padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TibbSectionHeader(
                            eyebrow = if (isUr) "غذائی توازن" else "NUTRITION OVERVIEW",
                            title = if (isUr) "آج کے کھانوں کا خلاصہ" else "Daily Diet Balance",
                            isUr = isUr,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TibbDonutGauge(
                                progress = eatenMealsCount.toFloat() / totalMeals.toFloat(),
                                centerValue = "$eatenMealsCount/$totalMeals",
                                centerCaption = if (isUr) "کھانے" else "Meals",
                                size = 66.dp,
                                strokeWidth = 6.5.dp,
                                activeColor = c.safeGreen,
                                accessibilityLabel = "$eatenMealsCount of $totalMeals meals eaten today",
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TibbStatPill(
                                    label = if (isUr) "مکمل ہدف" else "Plan Total",
                                    value = "$totalKcal",
                                    unit = "kcal",
                                    accentColor = c.accent,
                                    modifier = Modifier.weight(1f),
                                )
                                TibbStatPill(
                                    label = if (isUr) "کھایا گیا" else "Logged",
                                    value = "$eatenKcal",
                                    unit = "kcal",
                                    accentColor = c.safeGreen,
                                    containerColor = c.safeGreenSoft.copy(alpha = 0.72f),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val macroSummaries = listOf(
                                Triple(if (isUr) "پروٹین" else "Protein", "${dailyMacros.totalProteinG}g", c.wellnessBlue),
                                Triple(if (isUr) "کاربس" else "Carbs", "${dailyMacros.totalCarbsG}g", c.accent),
                                Triple(if (isUr) "چکنائی" else "Fat", "${dailyMacros.totalFatG}g", c.amber),
                                Triple(if (isUr) "فائبر" else "Fiber", "${dailyMacros.totalFiberG}g", c.safeGreen),
                            )
                            macroSummaries.forEach { (label, valText, tint) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(tint.copy(alpha = 0.10f))
                                        .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 9.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = "$label $valText",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = c.ink,
                                    )
                                }
                            }
                        }
                        if (plan.dietMeals.isNotEmpty()) {
                            TibbSegmentedDistributionBar(
                                slices = plan.dietMeals.mapIndexed { i, meal ->
                                    TibbDistributionSlice(
                                        label = "${meal.slot} kcal",
                                        count = meal.caloriesApprox,
                                        color = slotPalette[i % slotPalette.size],
                                    )
                                },
                                accessibilityLabel = "Calorie distribution across meals",
                            )
                        }
                    }
                }

                // Clean Meal Cards with Searchable Nutritional Swapping
                plan.dietMeals.forEachIndexed { slotIdx, meal ->
                    val eaten = todayLog.mealsChecked.contains(meal.slot)
                    val slotKey = meal.slot.lowercase().replace(' ', '_')
                    val resolvedMacros = remember(meal, foodCatalog) {
                        FoodMatchingEngine.resolveMealMacros(meal, foodCatalog)
                    }
                    TibbGlassCard(padding = 14.dp, borderColor = if (eaten) c.safeGreen else c.border) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "${meal.slot} · ~${resolvedMacros.caloriesKcal} kcal",
                                        style = TabularNumberStyle.copy(fontWeight = FontWeight.SemiBold),
                                        color = c.accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (meal.isSwapped) {
                                        TibbStatusBadge(
                                            text = if (isUr) "تبدیل شدہ" else "Swapped",
                                            color = c.wellnessBlue,
                                            softBg = c.wellnessBlueSoft,
                                        )
                                    }
                                }
                                TibbSecondaryPillButton(
                                    onClick = { activeSwapSlotIdx = slotIdx },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("meal_swap_$slotKey"),
                                ) {
                                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isUr) "تبدیل" else "Swap",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                    )
                                }
                            }

                            Text(
                                text = if (isUr) meal.title_ur else meal.title_en,
                                style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                                color = c.ink,
                            )
                            Text(
                                text = if (isUr) meal.portions_ur else meal.portions_en,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.muted,
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val mealMacroTags = listOf(
                                    "P ${resolvedMacros.proteinG}g",
                                    "C ${resolvedMacros.carbsG}g",
                                    "F ${resolvedMacros.fatG}g",
                                    "Fib ${resolvedMacros.fiberG}g",
                                )
                                mealMacroTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(c.surfaceMuted)
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = c.muted,
                                        )
                                    }
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TibbSecondaryPillButton(
                                    onClick = {
                                        val nextChecked = if (eaten) todayLog.mealsChecked - meal.slot else todayLog.mealsChecked + meal.slot
                                        onUpdateWellness(todayLog.copy(mealsChecked = nextChecked))
                                    },
                                    borderColor = if (eaten) c.safeGreen else c.border,
                                    contentColor = if (eaten) c.safeGreen else c.ink,
                                    modifier = Modifier.testTag("meal_eaten_$slotKey"),
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (eaten) (if (isUr) "آج کھایا گیا" else "Eaten Today") else (if (isUr) "کھایا گیا نشان لگائیں" else "Mark Eaten"))
                                }
                                if (meal.isSwapped && meal.originalMealSnapshot != null) {
                                    TibbSecondaryPillButton(
                                        onClick = {
                                            val updatedMeals = plan.dietMeals.toMutableList()
                                            updatedMeals[slotIdx] = FoodMatchingEngine.restoreOriginalMeal(meal)
                                            onUpdatePlan(plan.copy(dietMeals = updatedMeals))
                                        },
                                        modifier = Modifier.testTag("meal_restore_$slotKey"),
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isUr) "اصل بحال" else "Restore")
                                    }
                                }
                            }
                        }
                    }
                }

                // Collapsed-by-default Condition & Allergy Safeguards
                val notes = if (isUr) plan.avoidNotes_ur else plan.avoidNotes_en
                if (notes.isNotEmpty()) {
                    TibbExpandableSection(
                        title = if (isUr) "احتیاطی غذائی اصول" else "Condition & Allergy Safeguards",
                        summary = "${notes.size} safety notes",
                        icon = Icons.Rounded.Warning,
                        accentColor = c.amber,
                        initiallyExpanded = false,
                        headerTestTag = "coach_safeguards_expand_btn",
                    ) {
                        notes.forEach { note ->
                            Text("• $note", style = MaterialTheme.typography.bodySmall, color = c.ink)
                        }
                    }
                }
            }

            1 -> {
                TibbGlassCard(padding = 18.dp, borderColor = c.accent.copy(alpha = 0.4f)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = c.accent)
                                Text(
                                    text = if (isUr) "ورزش کا ٹائمر" else "Workout Timer",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = c.ink,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                TibbStatusBadge(
                                    text = if (todayLog.workoutDone) (if (isUr) "مکمل" else "Completed") else (if (isUr) "30 منٹ ہدف" else "30m Goal"),
                                    color = if (todayLog.workoutDone) c.safeGreen else c.accent,
                                    softBg = if (todayLog.workoutDone) c.safeGreenSoft else c.accentSoft,
                                )
                                SectionHelpButton(topic = HelpTopic.COACH_WORKOUT, locale = locale)
                            }
                        }

                        val mm = workoutSec / 60
                        val ss = workoutSec % 60
                        val timerProgress = if (todayLog.workoutDone) 1f else ((workoutSec % 1800) / 1800f).coerceIn(0f, 1f)

                        TibbCircularTimerRing(
                            progress = timerProgress,
                            size = 152.dp,
                            strokeWidth = 9.dp,
                            activeColor = if (todayLog.workoutDone) c.safeGreen else c.accent,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${mm.toString().padStart(2, '0')}:${ss.toString().padStart(2, '0')}",
                                    style = InterHeroStatStyle.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize),
                                    color = c.ink,
                                )
                                Text(
                                    text = if (workoutRunning) (if (isUr) "جاری ہے" else "Active") else (if (isUr) "تیار" else "Ready"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.muted,
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TibbPrimaryPillButton(
                                onClick = { workoutRunning = !workoutRunning },
                                modifier = Modifier.testTag("workout_timer_toggle"),
                            ) {
                                Icon(if (workoutRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (workoutRunning) "Pause" else "Start Session")
                            }
                            TibbSecondaryPillButton(
                                onClick = {
                                    workoutRunning = false
                                    workoutSec = 0
                                },
                            ) { Text("Reset") }
                            TibbSecondaryPillButton(
                                onClick = { onUpdateWellness(todayLog.copy(workoutDone = !todayLog.workoutDone)) },
                                borderColor = if (todayLog.workoutDone) c.safeGreen else c.border,
                                contentColor = if (todayLog.workoutDone) c.safeGreen else c.ink,
                                modifier = Modifier.testTag("workout_mark_done_btn"),
                            ) {
                                Text(if (todayLog.workoutDone) "Done Today" else "Mark Done")
                            }
                        }
                    }
                }

                // Exercise Routine List with animated full-body human silhouette demonstrations
                val gender = remember(activePerson.sex) { SilhouetteGender.fromSexCode(activePerson.sex) }
                var expandedExerciseIdx by remember(plan.id) { mutableIntStateOf(0) }

                TibbExpandableSection(
                    title = if (isUr) "آج کی ورزش کے مراحل" else "Daily Exercise Routine",
                    summary = "${plan.workoutRoutine.size} exercises",
                    icon = Icons.Rounded.FitnessCenter,
                    initiallyExpanded = true,
                    headerTestTag = "coach_workout_routine_expand",
                ) {
                    plan.workoutRoutine.forEachIndexed { idx, ex ->
                        WorkoutExerciseCard(
                            index = idx,
                            exercise = ex,
                            gender = gender,
                            locale = locale,
                            isExpanded = expandedExerciseIdx == idx,
                            onToggleExpand = {
                                expandedExerciseIdx = if (expandedExerciseIdx == idx) -1 else idx
                            },
                        )
                    }
                }
            }

            2 -> {
                val categories = listOf(
                    "ALL" to "All",
                    "FEVER_THROAT" to "Fever & Throat",
                    "STOMACH_HYDRATION" to "Stomach & ORS",
                    "HEAT_SKIN" to "Heat & Skin",
                    "PAIN_RECOVERY" to "Pain & Sprains",
                    "STRESS_SLEEP" to "Sleep & Stress",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isUr) "مستند گھریلو علاج" else "Verified Home Remedies",
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                        color = c.ink,
                        modifier = Modifier.weight(1f),
                    )
                    SectionHelpButton(topic = HelpTopic.COACH_REMEDIES, locale = locale)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { (key, lbl) ->
                        val sel = remedyFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) c.ctaBg else c.surface)
                                .border(1.dp, if (sel) c.ctaBg else c.border, RoundedCornerShape(999.dp))
                                .clickable { remedyFilter = key }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                        }
                    }
                }

                val list = remember(remedyFilter) {
                    if (remedyFilter == "ALL") RemediesAndPlans.HOME_REMEDIES_PAKISTAN
                    else RemediesAndPlans.HOME_REMEDIES_PAKISTAN.filter { it.category == remedyFilter }
                }

                // Progressive disclosure: each remedy collapsed by default into an expandable card
                list.forEach { rem ->
                    val title = if (isUr) rem.title_ur else rem.title_en
                    val ingredients = if (isUr) rem.ingredients_ur else rem.ingredients_en
                    TibbExpandableSection(
                        title = title,
                        summary = ingredients,
                        icon = Icons.Rounded.Spa,
                        expanded = expandedRemedyId == rem.title_en,
                        onExpandedChange = { open -> expandedRemedyId = if (open) rem.title_en else null },
                    ) {
                        val steps = if (isUr) rem.steps_ur else rem.steps_en
                        TibbMedicalInfoBlock(
                            title = if (isUr) "تیاری کا طریقہ" else "Preparation Steps",
                            icon = Icons.Rounded.Spa,
                            accentColor = c.safeGreen,
                            containerColor = c.safeGreenSoft.copy(alpha = 0.55f),
                            items = steps,
                            numbered = true,
                            isUr = isUr,
                        )
                        TibbClinicalCallout(
                            title = if (isUr) "احتیاط" else "Safety Caution",
                            icon = Icons.Rounded.Warning,
                            accentColor = c.amber,
                            containerColor = c.amberSoft,
                            isUr = isUr,
                        ) {
                            Text(
                                text = if (isUr) rem.caution_ur else rem.caution_en,
                                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                                color = c.ink,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    val swapIdx = activeSwapSlotIdx
    if (swapIdx != null && swapIdx in plan.dietMeals.indices) {
        val mealToSwap = plan.dietMeals[swapIdx]
        FoodSwapBottomSheet(
            locale = locale,
            mealSlot = mealToSwap,
            foodCatalog = foodCatalog,
            patient = activePerson,
            goal = selectedGoal,
            onDismiss = { activeSwapSlotIdx = null },
            onSelectFood = { chosenFood ->
                val updatedMeals = plan.dietMeals.toMutableList()
                updatedMeals[swapIdx] = FoodMatchingEngine.applyFoodSwap(mealToSwap, chosenFood, foodCatalog)
                onUpdatePlan(plan.copy(dietMeals = updatedMeals))
                activeSwapSlotIdx = null
            },
            onRestoreOriginal = {
                val updatedMeals = plan.dietMeals.toMutableList()
                updatedMeals[swapIdx] = FoodMatchingEngine.restoreOriginalMeal(mealToSwap)
                onUpdatePlan(plan.copy(dietMeals = updatedMeals))
                activeSwapSlotIdx = null
            },
        )
    }
}
