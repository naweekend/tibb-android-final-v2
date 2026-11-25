package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AllergyCategory
import com.example.data.model.AppLocale
import com.example.data.model.ChronicConditionCategory
import com.example.data.model.HealthProfileCatalog
import com.example.data.model.HealthProfileOption
import com.example.data.model.PROFILE_OPTION_NONE
import com.example.data.model.PROFILE_OPTION_NOT_SURE
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingMedicalProfileStep(
    locale: AppLocale,
    sex: String,
    ageMonths: Int,
    chronic: SnapshotStateList<String>,
    isPregnantOrLactating: Boolean,
    onTogglePregnant: (Boolean) -> Unit,
    allergies: SnapshotStateList<String>,
    customChronic: String,
    onCustomChronicChange: (String) -> Unit,
    customAllergy: String,
    onCustomAllergyChange: (String) -> Unit,
    dailyMedicines: String,
    onDailyMedicinesChange: (String) -> Unit,
    priorLabNotes: String,
    onPriorLabNotesChange: (String) -> Unit,
    emergencyContact: String,
    onEmergencyContactChange: (String) -> Unit,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR

    var chronicQuery by remember { mutableStateOf("") }
    var allergyQuery by remember { mutableStateOf("") }

    val expandedChronicCats = remember {
        mutableStateMapOf(
            ChronicConditionCategory.CARDIOMETABOLIC.id to true,
            ChronicConditionCategory.RESPIRATORY.id to true,
        )
    }
    val expandedAllergyCats = remember {
        mutableStateMapOf(
            AllergyCategory.FOOD.id to true,
            AllergyCategory.MEDICATION.id to true,
        )
    }

    fun toggleListSelection(list: SnapshotStateList<String>, id: String) {
        if (id == PROFILE_OPTION_NONE || id == PROFILE_OPTION_NOT_SURE) {
            val already = list.contains(id)
            list.clear()
            if (!already) list.add(id)
        } else {
            list.remove(PROFILE_OPTION_NONE)
            list.remove(PROFILE_OPTION_NOT_SURE)
            if (list.contains(id)) list.remove(id) else list.add(id)
        }
    }

    fun isOptionSelected(list: List<String>, opt: HealthProfileOption): Boolean =
        list.any { stored ->
            stored.equals(opt.id, ignoreCase = true) ||
                opt.aliases.any { it.equals(stored, ignoreCase = true) }
        }

    fun toggleOption(list: SnapshotStateList<String>, opt: HealthProfileOption) {
        val existing = list.firstOrNull { stored ->
            stored.equals(opt.id, ignoreCase = true) ||
                opt.aliases.any { it.equals(stored, ignoreCase = true) }
        }
        if (existing != null) {
            list.remove(existing)
        } else {
            list.remove(PROFILE_OPTION_NONE)
            list.remove(PROFILE_OPTION_NOT_SURE)
            list.add(opt.id)
        }
    }

    // Non-diagnostic personalization disclaimer
    TibbGlassCard(padding = 12.dp, borderColor = c.accent.copy(alpha = 0.45f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
            Text(
                text = if (isUr) {
                    "یہ معلومات صرف حفاظتی انتباہات اور خوراک/علاج کی رہنمائی بہتر بنانے کے لیے ہیں — یہ طبی تشخیص نہیں اور آپ چاہیں تو اسے خالی بھی چھوڑ سکتے ہیں۔"
                } else {
                    "Optional health profile: selections personalize safety warnings and precautions only. They are not a medical diagnosis."
                },
                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                color = c.ink,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // Selected Profile Review & Quick Edit Summary
    if (chronic.isNotEmpty() || allergies.isNotEmpty()) {
        TibbGlassCard(padding = 12.dp, borderColor = c.border) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isUr) "منتخب کردہ خلاصہ (ترمیم کے لیے ٹیپ کریں)" else "Selected Summary (tap × to remove)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = c.accent,
                )
                if (chronic.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        chronic.forEach { entry ->
                            SelectedSummaryChip(
                                label = HealthProfileCatalog.conditionDisplayLabel(entry, isUr),
                                isAllergy = false,
                                onRemove = { chronic.remove(entry) },
                            )
                        }
                    }
                }
                if (allergies.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        allergies.forEach { entry ->
                            SelectedSummaryChip(
                                label = HealthProfileCatalog.allergyDisplayLabel(entry, isUr),
                                isAllergy = true,
                                onRemove = { allergies.remove(entry) },
                            )
                        }
                    }
                }
            }
        }
    }

    // SECTION 1: CHRONIC CONDITIONS
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (isUr) "دائمی طبی کیفیات (اختیاری)" else "Chronic Medical Conditions (Optional)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink,
        )

        // None / Not Sure quick cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusChoicePill(
                label = if (isUr) "ان میں سے کوئی نہیں" else "None of these",
                selected = chronic.contains(PROFILE_OPTION_NONE),
                onClick = { toggleListSelection(chronic, PROFILE_OPTION_NONE) },
                modifier = Modifier.weight(1f).testTag("chronic_none_btn"),
            )
            StatusChoicePill(
                label = if (isUr) "معلوم نہیں" else "I'm not sure",
                selected = chronic.contains(PROFILE_OPTION_NOT_SURE),
                onClick = { toggleListSelection(chronic, PROFILE_OPTION_NOT_SURE) },
                modifier = Modifier.weight(1f).testTag("chronic_not_sure_btn"),
            )
        }

        OutlinedTextField(
            value = chronicQuery,
            onValueChange = { chronicQuery = it },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (chronicQuery.isNotEmpty()) {
                    IconButton(onClick = { chronicQuery = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear condition search")
                    }
                }
            },
            placeholder = {
                Text(
                    text = if (isUr) "30+ بیماریاں تلاش کریں (مثلاً شوگر، دمہ، تھائیرائیڈ)..." else "Search 30+ conditions (e.g. Diabetes, GERD, PCOS)...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("chronic_search_input"),
        )

        val chronicSearchTrim = chronicQuery.trim().lowercase()
        val isSearchingChronic = chronicSearchTrim.isNotEmpty()

        ChronicConditionCategory.entries.forEach { category ->
            val itemsInCat = remember(category, chronicSearchTrim) {
                HealthProfileCatalog.chronicConditions.filter { opt ->
                    opt.categoryId == category.id && (
                        chronicSearchTrim.isEmpty() ||
                            opt.labelEn.lowercase().contains(chronicSearchTrim) ||
                            opt.labelUr.contains(chronicSearchTrim) ||
                            opt.id.lowercase().contains(chronicSearchTrim) ||
                            opt.aliases.any { it.lowercase().contains(chronicSearchTrim) }
                        )
                }
            }
            if (itemsInCat.isNotEmpty()) {
                val isExpanded = isSearchingChronic || (expandedChronicCats[category.id] == true)
                val selectedCountInCat = itemsInCat.count { isOptionSelected(chronic, it) }

                CategoryAccordionHeader(
                    title = if (isUr) category.titleUr else category.titleEn,
                    countBadge = if (selectedCountInCat > 0) "$selectedCountInCat" else "${itemsInCat.size}",
                    hasActiveSelection = selectedCountInCat > 0,
                    expanded = isExpanded,
                    onToggle = { expandedChronicCats[category.id] = !isExpanded },
                )

                AnimatedVisibility(visible = isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsInCat.forEach { opt ->
                            val selected = isOptionSelected(chronic, opt)
                            MultiSelectProfileRow(
                                label = if (isUr) opt.labelUr else opt.labelEn,
                                selected = selected,
                                accentColor = c.accent,
                                selectedBg = c.accentSoft,
                                testTag = "chronic_opt_${opt.id.lowercase().replace(' ', '_')}",
                                onClick = { toggleOption(chronic, opt) },
                            )
                        }
                    }
                }
            }
        }

        // Custom Chronic Condition Entry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = customChronic,
                onValueChange = onCustomChronicChange,
                label = { Text(if (isUr) "دیگر بیماری شامل کریں" else "Add other condition") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("custom_chronic_input"),
            )
            TibbSecondaryPillButton(
                onClick = {
                    val clean = customChronic.trim()
                    if (clean.isNotEmpty()) {
                        chronic.remove(PROFILE_OPTION_NONE)
                        chronic.remove(PROFILE_OPTION_NOT_SURE)
                        if (chronic.none { it.equals(clean, ignoreCase = true) }) {
                            chronic.add(clean)
                        }
                        onCustomChronicChange("")
                    }
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.heightIn(min = 48.dp).testTag("add_custom_chronic_btn"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Condition")
            }
        }
    }

    if (sex == "F" && ageMonths >= 12 * 12) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surfaceMuted)
                .clickable { onTogglePregnant(!isPregnantOrLactating) }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isPregnantOrLactating, onCheckedChange = onTogglePregnant)
            Text(
                text = if (isUr) "حاملہ یا دودھ پلانے والی ماں (اضافی زچگی حفاظتی سوالات فعال ہوں گے)"
                else "Currently Pregnant or Breastfeeding (enables WHO maternal safety checks)",
                style = MaterialTheme.typography.bodySmall,
                color = c.ink,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // SECTION 2: ALLERGIES
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (isUr) "الرجی کی اقسام (اختیاری)" else "Allergies & Sensitivities (Optional)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusChoicePill(
                label = if (isUr) "ان میں سے کوئی نہیں" else "None of these",
                selected = allergies.contains(PROFILE_OPTION_NONE),
                onClick = { toggleListSelection(allergies, PROFILE_OPTION_NONE) },
                modifier = Modifier.weight(1f).testTag("allergy_none_btn"),
            )
            StatusChoicePill(
                label = if (isUr) "معلوم نہیں" else "I'm not sure",
                selected = allergies.contains(PROFILE_OPTION_NOT_SURE),
                onClick = { toggleListSelection(allergies, PROFILE_OPTION_NOT_SURE) },
                modifier = Modifier.weight(1f).testTag("allergy_not_sure_btn"),
            )
        }

        OutlinedTextField(
            value = allergyQuery,
            onValueChange = { allergyQuery = it },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (allergyQuery.isNotEmpty()) {
                    IconButton(onClick = { allergyQuery = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear allergy search")
                    }
                }
            },
            placeholder = {
                Text(
                    text = if (isUr) "خوراک، پولن یا ادویات کی الرجی تلاش کریں..." else "Search food, pollen, or medication allergies...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("allergy_search_input"),
        )

        val allergySearchTrim = allergyQuery.trim().lowercase()
        val isSearchingAllergy = allergySearchTrim.isNotEmpty()

        AllergyCategory.entries.forEach { category ->
            val itemsInCat = remember(category, allergySearchTrim) {
                HealthProfileCatalog.allergies.filter { opt ->
                    opt.categoryId == category.id && (
                        allergySearchTrim.isEmpty() ||
                            opt.labelEn.lowercase().contains(allergySearchTrim) ||
                            opt.labelUr.contains(allergySearchTrim) ||
                            opt.id.lowercase().contains(allergySearchTrim) ||
                            opt.aliases.any { it.lowercase().contains(allergySearchTrim) }
                        )
                }
            }
            if (itemsInCat.isNotEmpty()) {
                val isExpanded = isSearchingAllergy || (expandedAllergyCats[category.id] == true)
                val selectedCountInCat = itemsInCat.count { isOptionSelected(allergies, it) }

                CategoryAccordionHeader(
                    title = if (isUr) category.titleUr else category.titleEn,
                    countBadge = if (selectedCountInCat > 0) "$selectedCountInCat" else "${itemsInCat.size}",
                    hasActiveSelection = selectedCountInCat > 0,
                    expanded = isExpanded,
                    onToggle = { expandedAllergyCats[category.id] = !isExpanded },
                )

                AnimatedVisibility(visible = isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsInCat.forEach { opt ->
                            val selected = isOptionSelected(allergies, opt)
                            MultiSelectProfileRow(
                                label = if (isUr) opt.labelUr else opt.labelEn,
                                selected = selected,
                                accentColor = c.dangerRed,
                                selectedBg = c.dangerSoft,
                                testTag = "allergy_opt_${opt.id.lowercase().replace(' ', '_')}",
                                onClick = { toggleOption(allergies, opt) },
                            )
                        }
                    }
                }
            }
        }

        // Custom / Specific Allergy Entry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = customAllergy,
                onValueChange = onCustomAllergyChange,
                label = { Text(if (isUr) "مخصوص الرجی لکھیں (اختیاری)" else "Specify or add custom allergy") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("custom_allergy_input"),
            )
            TibbSecondaryPillButton(
                onClick = {
                    val clean = customAllergy.trim()
                    if (clean.isNotEmpty()) {
                        allergies.remove(PROFILE_OPTION_NONE)
                        allergies.remove(PROFILE_OPTION_NOT_SURE)
                        if (allergies.none { it.equals(clean, ignoreCase = true) }) {
                            allergies.add(clean)
                        }
                        onCustomAllergyChange("")
                    }
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.heightIn(min = 48.dp).testTag("add_custom_allergy_btn"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Allergy")
            }
        }
    }

    OutlinedTextField(
        value = dailyMedicines,
        onValueChange = onDailyMedicinesChange,
        label = { Text(if (isUr) "روزانہ کی دوائیں (اختیاری)" else "Daily Medicines Note (Optional)") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = priorLabNotes,
        onValueChange = onPriorLabNotesChange,
        label = { Text(if (isUr) "پچھلی لیب رپورٹ / نوٹس" else "Prior Lab Notes (e.g. HbA1c, CBC, BP readings)") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = emergencyContact,
        onValueChange = onEmergencyContactChange,
        label = { Text(if (isUr) "ہنگامی رابطہ نمبر (ICE)" else "Emergency Contact (ICE)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StatusChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.ctaBg else c.surfaceMuted)
            .border(1.dp, if (selected) c.ctaBg else c.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = if (selected) c.ctaInk else c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CategoryAccordionHeader(
    title: String,
    countBadge: String,
    hasActiveSelection: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasActiveSelection) c.accentSoft.copy(alpha = 0.55f) else c.surfaceMuted.copy(alpha = 0.7f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (hasActiveSelection) c.accent else c.surface)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = countBadge,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (hasActiveSelection) Color.White else c.muted,
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            tint = c.muted,
        )
    }
}

@Composable
private fun MultiSelectProfileRow(
    label: String,
    selected: Boolean,
    accentColor: Color,
    selectedBg: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) selectedBg else c.surface)
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                color = if (selected) accentColor else c.border,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onClick() },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = c.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SelectedSummaryChip(
    label: String,
    isAllergy: Boolean,
    onRemove: () -> Unit,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isAllergy) c.dangerSoft else c.accentSoft)
            .border(1.dp, if (isAllergy) c.dangerRed.copy(alpha = 0.65f) else c.accent.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
            .clickable(onClick = onRemove)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Remove $label",
            tint = if (isAllergy) c.dangerRed else c.accent,
            modifier = Modifier.size(14.dp),
        )
    }
}
