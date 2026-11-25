package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLocale
import com.example.data.model.FrequentPlace
import com.example.data.model.GeoCoords
import com.example.data.model.PakistanLocationRecord
import com.example.data.model.SelectedLocationState
import com.example.engine.TriageEngine
import com.example.ui.components.PakistanLocationPickerDialog
import com.example.ui.components.PakistanOfflineMapCanvas
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingStep3Medical(
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
    OnboardingMedicalProfileStep(
        locale = locale,
        sex = sex,
        ageMonths = ageMonths,
        chronic = chronic,
        isPregnantOrLactating = isPregnantOrLactating,
        onTogglePregnant = onTogglePregnant,
        allergies = allergies,
        customChronic = customChronic,
        onCustomChronicChange = onCustomChronicChange,
        customAllergy = customAllergy,
        onCustomAllergyChange = onCustomAllergyChange,
        dailyMedicines = dailyMedicines,
        onDailyMedicinesChange = onDailyMedicinesChange,
        priorLabNotes = priorLabNotes,
        onPriorLabNotesChange = onPriorLabNotesChange,
        emergencyContact = emergencyContact,
        onEmergencyContactChange = onEmergencyContactChange,
    )
}

@Composable
fun OnboardingStep4Places(
    locale: AppLocale,
    triageEngine: TriageEngine,
    places: SnapshotStateList<FrequentPlace>,
    userGpsCoords: GeoCoords?,
    gpsLocating: Boolean,
    onRequestGps: () -> Unit,
    onPendingSelectionChange: (SelectedLocationState?) -> Unit = {},
) {
    val context = LocalContext.current
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR

    val allLocations = remember { triageEngine.getPakistanLocations() }
    var searchQuery by remember { mutableStateOf("") }
    var placeLabel by remember { mutableStateOf("Home") }
    var customCityMode by remember { mutableStateOf(false) }
    var customDistrict by remember { mutableStateOf("") }
    var customLandmark by remember { mutableStateOf("") }
    var showFullMapDialog by remember { mutableStateOf(false) }

    val initialPin = remember(places.firstOrNull()?.id) {
        places.firstOrNull()?.let { p ->
            if (p.lat != null && p.lng != null) {
                SelectedLocationState(
                    name = p.areaOrLandmark.ifBlank { p.district },
                    district = p.district,
                    province = p.province,
                    lat = p.lat,
                    lng = p.lng,
                )
            } else null
        }
    }
    var pendingSelection by remember { mutableStateOf(initialPin) }
    LaunchedEffect(pendingSelection) {
        onPendingSelectionChange(pendingSelection)
    }
    var mapZoom by remember { mutableFloatStateOf(if (initialPin != null) 1.25f else 1.0f) }
    var mapPan by remember { mutableStateOf(Offset.Zero) }
    var waitingForGpsSnap by remember { mutableStateOf(false) }

    LaunchedEffect(userGpsCoords, waitingForGpsSnap) {
        if (waitingForGpsSnap && userGpsCoords != null) {
            val nearest = triageEngine.findNearestPakistanLocation(userGpsCoords.lat, userGpsCoords.lng)
            pendingSelection = SelectedLocationState(
                name = nearest?.let { "GPS near ${it.nameEn}" } ?: "GPS Location",
                district = nearest?.district ?: "Pakistan",
                province = nearest?.province?.displayNameEn ?: "",
                lat = userGpsCoords.lat,
                lng = userGpsCoords.lng,
                isCustomPin = true,
            )
            waitingForGpsSnap = false
        }
    }

    val filteredLocations = remember(searchQuery) {
        triageEngine.searchPakistanLocations(searchQuery)
    }

    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            waitingForGpsSnap = true
            onRequestGps()
        }
    }

    fun selectNamedPlace(loc: PakistanLocationRecord) {
        pendingSelection = SelectedLocationState(
            name = if (isUr) loc.nameUr else loc.nameEn,
            district = loc.district,
            province = loc.province.displayNameEn,
            lat = loc.lat,
            lng = loc.lng,
            isCustomPin = false,
        )
    }

    fun pinAtCoords(lat: Double, lng: Double) {
        val clampedLat = lat.coerceIn(23.5, 37.2)
        val clampedLng = lng.coerceIn(60.8, 77.9)
        val nearest = triageEngine.findNearestPakistanLocation(clampedLat, clampedLng)
        pendingSelection = SelectedLocationState(
            name = nearest?.let { if (isUr) "پن (قریب ${it.nameUr})" else "Pinned near ${it.nameEn}" } ?: "Custom Pin",
            district = nearest?.district ?: "Pakistan",
            province = nearest?.province?.displayNameEn ?: "",
            lat = clampedLat,
            lng = clampedLng,
            isCustomPin = true,
        )
    }

    fun saveSelection(sel: SelectedLocationState) {
        if (places.size >= 3) return
        val labelToSave = placeLabel.ifBlank { "Home" }.trim()
        val duplicateIdx = places.indexOfFirst {
            it.label.equals(labelToSave, ignoreCase = true) &&
                it.district.equals(sel.district, ignoreCase = true) &&
                it.areaOrLandmark.equals(sel.name, ignoreCase = true)
        }
        val record = FrequentPlace(
            id = "plc_${UUID.randomUUID().toString().take(8)}",
            label = labelToSave,
            district = sel.district,
            areaOrLandmark = sel.name,
            province = sel.province,
            lat = sel.lat,
            lng = sel.lng,
        )
        if (duplicateIdx >= 0) {
            places[duplicateIdx] = record
        } else {
            places.add(record)
        }
    }

    Text(
        text = if (isUr) "روزمرہ مقامات (پاکستان بھر سے 1 تا 3 مقامات منتخب کریں)"
        else "Pakistan Location (Tap map or search to save 1–3 areas)",
        style = MaterialTheme.typography.titleMedium,
        color = c.ink,
    )

    // Search Bar + GPS Snap Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = {
                Text(
                    text = if (isUr) "شہر، ضلع، قصبہ یا محلہ تلاش کریں..." else "Search city, district, town...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("onboard_district_search"),
        )
        Box(
            modifier = Modifier
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.accentSoft)
                .border(1.dp, c.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .clickable {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fine || coarse) {
                        waitingForGpsSnap = true
                        onRequestGps()
                    } else {
                        locPermLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            )
                        )
                    }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("onboard_gps_btn"),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.MyLocation, contentDescription = "Use GPS", tint = c.accent, modifier = Modifier.size(18.dp))
                Text(if (gpsLocating) "..." else "GPS", style = MaterialTheme.typography.labelMedium, color = c.ink)
            }
        }
    }

    // Immediate Search Suggestions when typing
    if (searchQuery.isNotBlank() && !customCityMode) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            filteredLocations.take(6).forEach { loc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surfaceMuted)
                        .clickable {
                            selectNamedPlace(loc)
                            mapZoom = mapZoom.coerceAtLeast(1.8f)
                            mapPan = Offset.Zero
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("onboard_pk_loc_${loc.id}"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUr) loc.nameUr else loc.nameEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.ink,
                        )
                        Text(
                            text = "${loc.district} · ${loc.province.shortCode} · ${if (isUr) loc.category.labelUr else loc.category.labelEn}",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.muted,
                        )
                    }
                    Icon(Icons.Rounded.LocationOn, contentDescription = "Pin ${loc.nameEn}", tint = c.accent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Primary Interactive Offline Map (Displayed Immediately by Default)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(275.dp),
    ) {
        PakistanOfflineMapCanvas(
            allLocations = allLocations,
            filteredLocations = filteredLocations,
            selectedLat = pendingSelection?.lat,
            selectedLng = pendingSelection?.lng,
            selectedProvince = null,
            zoom = mapZoom,
            pan = mapPan,
            onTransformChange = { z, p ->
                mapZoom = z
                mapPan = p
            },
            onSelectExistingLocation = { loc -> selectNamedPlace(loc) },
            onPinTappedAt = { lat, lng -> pinAtCoords(lat, lng) },
            modifier = Modifier.fillMaxSize(),
        )

        // Zoom + Expand Controls (Top Right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface.copy(alpha = 0.90f))
                .border(1.dp, c.border, RoundedCornerShape(14.dp)),
        ) {
            IconButton(
                onClick = { mapZoom = (mapZoom * 1.45f).coerceIn(1f, 14f) },
                modifier = Modifier.size(44.dp).testTag("onboard_map_zoom_in"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Zoom In", tint = c.ink, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = {
                    val nextZ = (mapZoom / 1.45f).coerceIn(1f, 14f)
                    mapZoom = nextZ
                    if (nextZ <= 1.05f) mapPan = Offset.Zero
                },
                modifier = Modifier.size(44.dp).testTag("onboard_map_zoom_out"),
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out", tint = c.ink, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = {
                    mapZoom = 1.0f
                    mapPan = Offset.Zero
                },
                modifier = Modifier.size(44.dp).testTag("onboard_map_reset"),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reset Map", tint = c.accent, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { showFullMapDialog = true },
                modifier = Modifier.size(44.dp).testTag("onboard_open_map_btn"),
            ) {
                Icon(Icons.Rounded.OpenInFull, contentDescription = "Expand Map", tint = c.accent, modifier = Modifier.size(18.dp))
            }
        }

        // Helper hint pill (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(c.surface.copy(alpha = 0.88f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = if (isUr) "نقشے پر ٹیپ یا زوم کریں" else String.format(Locale.US, "%.1fx · Tap map to pin", mapZoom),
                style = MaterialTheme.typography.labelSmall,
                color = c.muted,
            )
        }
    }

    // Selected Pin Summary + Confirmation Action
    val currentPin = pendingSelection
    TibbGlassCard(padding = 12.dp, borderColor = if (currentPin != null) c.accent.copy(alpha = 0.55f) else c.border) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = if (currentPin != null) c.dangerRed else c.muted,
                    modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentPin?.let { "${it.name} · ${it.district}" }
                            ?: if (isUr) "ابھی کوئی مقام منتخب نہیں — نقشے پر ٹیپ کریں" else "No pin selected yet — tap anywhere on the map",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (currentPin != null) {
                        Text(
                            text = buildString {
                                if (currentPin.province.isNotBlank()) append("${currentPin.province} · ")
                                append(String.format(Locale.US, "%.4f°N, %.4f°E", currentPin.lat, currentPin.lng))
                            },
                            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                            color = c.muted,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = placeLabel,
                onValueChange = { placeLabel = it },
                label = { Text(if (isUr) "لیبل (مثلاً گھر، اسکول، دفتر)" else "Label (e.g. Home, School, Office)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onboard_place_label_input"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TibbPrimaryPillButton(
                    onClick = { currentPin?.let(::saveSelection) },
                    enabled = currentPin != null && places.size < 3,
                    modifier = Modifier.weight(1.4f).heightIn(min = 48.dp).testTag("onboard_save_location_btn"),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isUr) "منتخب مقام محفوظ کریں" else "Save Selected Location",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                TibbSecondaryPillButton(
                    onClick = { customCityMode = !customCityMode },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("onboard_custom_city_toggle"),
                ) {
                    Text(
                        text = if (customCityMode) "Hide Manual" else "+ Custom Town",
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (customCityMode) {
        OutlinedTextField(
            value = customDistrict,
            onValueChange = { customDistrict = it },
            label = { Text("City / District") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = customLandmark,
            onValueChange = { customLandmark = it },
            label = { Text("Area / Tehsil / Landmark") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TibbSecondaryPillButton(
            enabled = customDistrict.isNotBlank() && places.size < 3,
            onClick = {
                places.add(
                    FrequentPlace(
                        id = "plc_${UUID.randomUUID().toString().take(8)}",
                        label = placeLabel.ifBlank { "Home" },
                        district = customDistrict.trim(),
                        areaOrLandmark = customLandmark.trim(),
                        lat = userGpsCoords?.lat,
                        lng = userGpsCoords?.lng,
                    )
                )
                customDistrict = ""
                customLandmark = ""
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("onboard_save_custom_place_btn"),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Save Place")
        }
    }

    if (places.isNotEmpty()) {
        Text("Saved Locations (${places.size}/3)", style = MaterialTheme.typography.labelLarge, color = c.accent)
        places.forEach { plc ->
            TibbGlassCard(padding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${plc.label} · ${plc.district}", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        val sub = buildString {
                            if (plc.areaOrLandmark.isNotBlank()) append(plc.areaOrLandmark)
                            if (plc.province.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(plc.province)
                            }
                        }
                        if (sub.isNotBlank()) {
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }
                    }
                    IconButton(onClick = { places.remove(plc) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remove ${plc.label}", tint = c.dangerRed)
                    }
                }
            }
        }
    }

    if (showFullMapDialog) {
        PakistanLocationPickerDialog(
            locale = locale,
            triageEngine = triageEngine,
            initialSelection = pendingSelection,
            userGpsCoords = userGpsCoords,
            gpsLocating = gpsLocating,
            onRequestGps = onRequestGps,
            onConfirm = { picked ->
                pendingSelection = picked
                saveSelection(picked)
                showFullMapDialog = false
            },
            onDismiss = { showFullMapDialog = false },
        )
    }
}
