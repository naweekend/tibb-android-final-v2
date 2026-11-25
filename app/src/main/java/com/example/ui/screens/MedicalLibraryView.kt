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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalInformation
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.ConditionSeverity
import com.example.data.model.MedicalConditionEntry
import com.example.data.model.SymptomCategoryNode
import com.example.engine.HelpTopic
import com.example.engine.LocalMedicalGuidanceEngine
import com.example.engine.TrilingualStrings
import com.example.ui.components.OfflineStatusBadge
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.TibbClinicalCallout
import com.example.ui.components.TibbMedicalInfoBlock
import com.example.ui.components.UrgencyClassificationsPillStrip
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TibbExpandableSection
import com.example.ui.theme.urduTextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicalLibraryView(
    locale: AppLocale,
    guidanceEngine: LocalMedicalGuidanceEngine,
    categories: List<SymptomCategoryNode>,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR
    val meta = remember { guidanceEngine.getKnowledgeMetadata() }

    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(query, selectedCategoryId) {
        guidanceEngine.searchConditions(query, selectedCategoryId)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OfflineStatusBadge(
                label = s.activeEngineOffline,
                modifier = Modifier.weight(1f, fill = false),
            )
            SectionHelpButton(topic = HelpTopic.GUIDED_CHECKUP, locale = locale)
        }

        UrgencyClassificationsPillStrip(locale = locale)

        // System Scope, Authoritative Sources & Limitations (Collapsed by default)
        TibbExpandableSection(
            title = if (isUr) meta.titleUr else meta.titleEn,
            summary = if (isUr) "دائرہ کار، ذرائع اور آخری جائزہ: ${meta.lastReviewedDate}" else "Scope, sources & limits · Reviewed ${meta.lastReviewedDate}",
            icon = Icons.Rounded.Info,
            expanded = expandedId == "_scope_",
            onExpandedChange = { open -> expandedId = if (open) "_scope_" else null },
            headerTestTag = "library_scope_expand_btn",
        ) {
            Text(
                text = if (isUr) meta.medicalDisclaimerUr else meta.medicalDisclaimerEn,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                color = c.urgentYellow,
            )
            Text(
                text = if (isUr) "شامل طبی شعبے:" else "Covered Medical Domains:",
                style = MaterialTheme.typography.labelLarge,
                color = c.ink,
            )
            val domains = if (isUr) meta.coveredDomainsUr else meta.coveredDomainsEn
            domains.forEach { d ->
                Text("• $d", style = MaterialTheme.typography.bodySmall, color = c.ink)
            }
            Text(
                text = if (isUr) "طبی ذرائع:" else "Reference Guidelines:",
                style = MaterialTheme.typography.labelLarge,
                color = c.ink,
            )
            meta.authoritativeSources.forEach { src ->
                Text("• $src", style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Text(
                text = if (isUr) "اہم حدود:" else "Important Limitations:",
                style = MaterialTheme.typography.labelLarge,
                color = c.dangerRed,
            )
            val limits = if (isUr) meta.limitationsUr else meta.limitationsEn
            limits.forEach { lim ->
                Text("• $lim", style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = if (isUr) "صاف کریں" else "Clear search")
                    }
                }
            },
            placeholder = {
                Text(if (isUr) "بیماری یا علامت تلاش کریں (مثلاً ڈینگی، دمہ)..." else "Search offline medical guides (e.g. fever, asthma, bite)...")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("library_search_input"),
        )

        // Category Filter Strip
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryFilterChip(
                label = if (isUr) "تمام (${guidanceEngine.getAllConditions().size})" else "All (${guidanceEngine.getAllConditions().size})",
                selected = selectedCategoryId == null,
                onClick = { selectedCategoryId = null },
            )
            categories.forEach { cat ->
                LibraryFilterChip(
                    label = if (isUr) cat.titleUr else cat.titleEn,
                    selected = selectedCategoryId == cat.id,
                    onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id },
                )
            }
        }

        Text(
            text = if (isUr) "${filtered.size} مستند آف لائن طبی رہنما" else "${filtered.size} offline reference guide(s)",
            style = MaterialTheme.typography.labelMedium,
            color = c.muted,
        )

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surfaceMuted)
                    .padding(16.dp),
            ) {
                Text(
                    text = if (isUr) "کوئی نتیجہ نہیں ملا۔ کوئی اور لفظ تلاش کریں۔" else "No matching offline condition guides found. Try another keyword.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
            }
        } else {
            filtered.groupBy { cond ->
                if (isUr) cond.subcategoryUr.ifBlank { cond.categoryLabelUr }
                else cond.subcategoryEn.ifBlank { cond.categoryLabelEn }
            }.forEach { (subcatLabel, groupList) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = subcatLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = c.accent,
                    )
                    Text(
                        text = "${groupList.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted,
                    )
                }
                groupList.forEach { cond ->
                    ConditionLibraryItem(
                        cond = cond,
                        isUr = isUr,
                        expanded = expandedId == cond.id,
                        onToggle = { open -> expandedId = if (open) cond.id else null },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) c.ctaBg else c.surfaceMuted)
            .border(1.dp, if (selected) c.ctaBg else c.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
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
private fun ConditionLibraryItem(
    cond: MedicalConditionEntry,
    isUr: Boolean,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val c = LocalTibbColors.current
    val sevColor = when (cond.possibleSeverity) {
        ConditionSeverity.EMERGENCY -> c.dangerRed
        ConditionSeverity.URGENT, ConditionSeverity.MODERATE -> c.urgentYellow
        ConditionSeverity.MILD -> c.safeGreen
    }
    val subLabel = if (isUr) cond.subcategoryUr.ifBlank { cond.categoryLabelUr } else cond.subcategoryEn.ifBlank { cond.categoryLabelEn }
    TibbExpandableSection(
        title = if (isUr) cond.nameUr else cond.nameEn,
        summary = "$subLabel · ${cond.lastReviewedDate}",
        icon = Icons.Rounded.MedicalInformation,
        accentColor = sevColor,
        expanded = expanded,
        onExpandedChange = onToggle,
        headerTestTag = "lib_cond_${cond.id}",
    ) {
        Text(
            text = if (isUr) cond.shortDescriptionUr else cond.shortDescriptionEn,
            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
            color = c.ink,
        )

        val common = if (isUr) cond.commonSymptomsUr else cond.commonSymptomsEn
        if (common.isNotEmpty()) {
            Text(
                text = if (isUr) "عام علامات:" else "Common Symptoms:",
                style = MaterialTheme.typography.labelLarge,
                color = c.accent,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                common.forEach { sym ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.surfaceMuted)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(sym, style = MaterialTheme.typography.labelSmall, color = c.ink)
                    }
                }
            }
        }

        val flags = if (isUr) cond.emergencyWarningSignsUr else cond.emergencyWarningSignsEn
        if (flags.isNotEmpty()) {
            TibbMedicalInfoBlock(
                title = if (isUr) "خطرناک علامات (فوری 1122 / ایمرجنسی):" else "Emergency Warning Signs",
                icon = Icons.Rounded.Warning,
                accentColor = c.dangerRed,
                containerColor = c.dangerSoft.copy(alpha = 0.72f),
                items = flags,
                numbered = false,
                isUr = isUr,
            )
        }

        val firstAid = if (isUr) cond.appropriateFirstAidUr else cond.appropriateFirstAidEn
        if (firstAid.isNotEmpty()) {
            TibbMedicalInfoBlock(
                title = if (isUr) "فوری ابتدائی امداد:" else "Appropriate First Aid",
                icon = Icons.Rounded.MedicalServices,
                accentColor = c.accent,
                containerColor = c.accentSoft.copy(alpha = 0.55f),
                items = firstAid,
                numbered = true,
                isUr = isUr,
            )
        }

        val noHome = if (isUr) cond.noHomeCareReasonUr else cond.noHomeCareReasonEn
        if (!noHome.isNullOrBlank()) {
            TibbClinicalCallout(
                title = if (isUr) "گھریلو علاج مناسب نہیں" else "Why Home Remedies Are Unsafe",
                icon = Icons.Rounded.Block,
                accentColor = c.dangerRed,
                containerColor = c.dangerSoft,
                isUr = isUr,
            ) {
                Text(
                    text = noHome,
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                    color = c.ink,
                )
            }
        } else {
            val homeCare = if (isUr) cond.safeHomeCareUr else cond.safeHomeCareEn
            if (homeCare.isNotEmpty()) {
                TibbMedicalInfoBlock(
                    title = if (isUr) "محفوظ گھریلو دیکھ بھال:" else "Safe Home Care",
                    icon = Icons.Rounded.Healing,
                    accentColor = c.safeGreen,
                    containerColor = c.safeGreenSoft.copy(alpha = 0.65f),
                    items = homeCare,
                    numbered = true,
                    isUr = isUr,
                )
            }
        }

        val avoid = if (isUr) cond.thingsToAvoidUr else cond.thingsToAvoidEn
        if (avoid.isNotEmpty()) {
            TibbMedicalInfoBlock(
                title = if (isUr) "پرہیز:" else "Things to Avoid",
                icon = Icons.Rounded.Block,
                accentColor = c.urgentYellow,
                containerColor = c.urgentYellowSoft.copy(alpha = 0.72f),
                items = avoid,
                numbered = false,
                isUr = isUr,
            )
        }

        TibbClinicalCallout(
            title = if (isUr) "ڈاکٹر کب دکھائیں" else "When to See a Doctor",
            icon = Icons.Rounded.LocalHospital,
            accentColor = c.wellnessBlue,
            containerColor = c.wellnessBlueSoft.copy(alpha = 0.65f),
            isUr = isUr,
        ) {
            Text(
                text = if (isUr) cond.whenToConsultDoctorUr else cond.whenToConsultDoctorEn,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                color = c.ink,
            )
        }
        Text(
            text = "${cond.sources.joinToString("; ")} · Last reviewed ${cond.lastReviewedDate}",
            style = MaterialTheme.typography.labelSmall,
            color = c.muted,
        )
    }
}
