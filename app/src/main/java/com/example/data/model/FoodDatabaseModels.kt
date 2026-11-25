package com.example.data.model

import com.squareup.moshi.JsonClass

enum class NutritionDataSource(val labelEn: String, val labelUr: String) {
    MEASURED("Measured (Ref.)", "پیمائش شدہ (حوالہ)"),
    ESTIMATED("Estimated Recipe", "تخمینی ترکیب"),
    UNAVAILABLE("Unavailable", "دستیاب نہیں"),
}

enum class FoodCategory(val labelEn: String, val labelUr: String) {
    PAKISTANI_TRADITIONAL("Pakistani", "پاکستانی"),
    SOUTH_ASIAN("South Asian", "جنوبی ایشیائی"),
    INTERNATIONAL("International", "بین الاقوامی"),
    WHOLE_GRAINS("Whole Grains", "اناج اور روٹی"),
    LEGUMES_PULSES("Legumes & Daal", "دالیں اور چنے"),
    PROTEIN_RICH("Protein-Rich", "پروٹین"),
    DAIRY_ALTERNATIVES("Dairy & Alt.", "دودھ اور دہی"),
    FRUITS("Fruits", "پھل"),
    VEGETABLES("Vegetables", "سبزیاں"),
    HEALTHY_SNACKS("Healthy Snacks", "صحت مند اسنیکس"),
    LOW_GLYCEMIC("Low-GI", "لو گلائسیمک"),
}

enum class MealSlotType(val slotName: String, val labelUr: String) {
    BREAKFAST("Breakfast", "ناشتہ"),
    MID_DAY_SNACK("Mid-Day Snack", "درمیانی اسنیک"),
    LUNCH("Lunch", "دوپہر کا کھانا"),
    DINNER("Dinner", "رات کا کھانا"),
    ;

    companion object {
        fun fromSlotName(slot: String): MealSlotType {
            val s = slot.lowercase()
            return when {
                s.contains("breakfast") -> BREAKFAST
                s.contains("snack") -> MID_DAY_SNACK
                s.contains("lunch") -> LUNCH
                else -> DINNER
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class FoodItem(
    val id: String,
    val nameEn: String,
    val nameUr: String,
    val category: FoodCategory,
    val servingSizeEn: String,
    val servingSizeUr: String,
    val servingGrams: Double,
    val caloriesKcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sugarG: Double? = null,
    val nutritionSource: NutritionDataSource = NutritionDataSource.MEASURED,
    val sugarSource: NutritionDataSource = NutritionDataSource.MEASURED,
    val glycemicIndexTier: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val sodiumLevel: String = "MODERATE", // LOW, MODERATE, HIGH
    val benefitsEn: String,
    val benefitsUr: String,
    val dietaryTags: List<String> = emptyList(),
    val suitableMeals: List<MealSlotType> = emptyList(),
    val allergens: List<String> = emptyList(),
    val conditionFlags: List<String> = emptyList(),
)
