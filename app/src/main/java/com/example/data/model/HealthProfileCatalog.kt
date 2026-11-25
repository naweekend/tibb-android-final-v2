package com.example.data.model

const val PROFILE_OPTION_NONE = "NONE_OF_THESE"
const val PROFILE_OPTION_NOT_SURE = "NOT_SURE"

enum class ChronicConditionCategory(
    val id: String,
    val titleEn: String,
    val titleUr: String,
) {
    CARDIOMETABOLIC("cardiometabolic", "Cardiometabolic & Blood Pressure", "دل، شوگر اور بلڈ پریشر"),
    RESPIRATORY("respiratory", "Respiratory & Airways", "سانس اور پھیپھڑوں کے امراض"),
    RENAL_HEPATIC("renal_hepatic", "Kidney & Liver Health", "گردے اور جگر کے امراض"),
    NEUROLOGICAL("neurological", "Neurological", "اعصابی امراض"),
    ENDOCRINE("endocrine", "Thyroid & Hormones", "تھائیرائیڈ اور ہارمونز"),
    MUSCULOSKELETAL("musculoskeletal", "Joints, Bone & Pain", "جوڑ، ہڈیاں اور دائمی درد"),
    GASTROINTESTINAL("gi", "Digestive & Gut", "معدے اور آنتوں کے امراض"),
    HEMATOLOGIC_OTHER("other_chronic", "Blood, Women's & Mental Health", "خون، خواتین کی صحت اور ذہنی صحت"),
}

enum class AllergyCategory(
    val id: String,
    val titleEn: String,
    val titleUr: String,
) {
    FOOD("food", "Food Allergies", "خوراک سے الرجی"),
    ENVIRONMENTAL("environmental", "Environmental & Seasonal", "ماحولیاتی اور موسمی الرجی"),
    MEDICATION("medication", "Medication & Drug Allergies", "ادویات سے الرجی"),
    CONTACT_INSECT("contact_insect", "Insect & Contact Allergies", "کیڑوں کے ڈنک اور رابطے سے الرجی"),
}

data class HealthProfileOption(
    val id: String,
    val labelEn: String,
    val labelUr: String,
    val categoryId: String,
    val aliases: List<String> = emptyList(),
)

object HealthProfileCatalog {

    val chronicConditions: List<HealthProfileOption> = listOf(
        // Cardiometabolic (6)
        HealthProfileOption(
            id = "Type 1 Diabetes",
            labelEn = "Type 1 Diabetes",
            labelUr = "ذیابیطس ٹائپ 1 (انسولین)",
            categoryId = ChronicConditionCategory.CARDIOMETABOLIC.id,
            aliases = listOf("T1D", "insulin dependent diabetes", "sugar"),
        ),
        HealthProfileOption(
            id = "Type 2 Diabetes",
            labelEn = "Type 2 Diabetes",
            labelUr = "ذیابیطس ٹائپ 2 (شوگر)",
            categoryId = ChronicConditionCategory.CARDIOMETABOLIC.id,
            aliases = listOf("DIABETES", "T2D", "sugar", "blood sugar"),
        ),
        HealthProfileOption(
            id = "Hypertension",
            labelEn = "Hypertension (High BP)",
            labelUr = "بلند فشارِ خون (ہائی بلڈ پریشر)",
            categoryId = ChronicConditionCategory.CARDIOMETABOLIC.id,
            aliases = listOf("HYPERTENSION", "high bp", "high blood pressure"),
        ),
        HealthProfileOption(
            id = "Hypotension",
            labelEn = "Hypotension (Low BP)",
            labelUr = "کم فشارِ خون (لو بلڈ پریشر)",
            categoryId = ChronicConditionCategory.CARDIOMETABOLIC.id,
            aliases = listOf("low bp", "low blood pressure"),
        ),
        HealthProfileOption(
            id = "Heart Disease",
            labelEn = "Heart Disease",
            labelUr = "دل کی بیماری",
            categoryId = ChronicConditionCategory.CARDIOMETABOLIC.id,
            aliases = listOf("HEART_DISEASE", "cardiac", "heart failure", "cardiomyopathy"),
        ),
        HealthProfileOption(
            id = "Coronary Artery Disease",
            labelEn = "Coronary Artery Disease",
            labelUr = "دل کی شریانوں کی بیماری",
            categoryId = ChronicConditionCategory.CARDIOMETABOLIC.id,
            aliases = listOf("CAD", "angina", "ischemic heart"),
        ),

        // Respiratory (2)
        HealthProfileOption(
            id = "Asthma",
            labelEn = "Asthma",
            labelUr = "دمہ (سانس کی تنگی)",
            categoryId = ChronicConditionCategory.RESPIRATORY.id,
            aliases = listOf("ASTHMA", "wheezing", "bronchial asthma"),
        ),
        HealthProfileOption(
            id = "COPD",
            labelEn = "COPD",
            labelUr = "پھیپھڑوں کی دائمی بیماری (COPD)",
            categoryId = ChronicConditionCategory.RESPIRATORY.id,
            aliases = listOf("chronic bronchitis", "emphysema", "chronic lung"),
        ),

        // Renal & Hepatic (2)
        HealthProfileOption(
            id = "Chronic Kidney Disease",
            labelEn = "Chronic Kidney Disease",
            labelUr = "گردوں کی دائمی بیماری",
            categoryId = ChronicConditionCategory.RENAL_HEPATIC.id,
            aliases = listOf("KIDNEY", "CKD", "renal failure", "kidney condition"),
        ),
        HealthProfileOption(
            id = "Chronic Liver Disease",
            labelEn = "Chronic Liver Disease",
            labelUr = "جگر کی دائمی بیماری",
            categoryId = ChronicConditionCategory.RENAL_HEPATIC.id,
            aliases = listOf("CLD", "hepatitis", "fatty liver", "cirrhosis"),
        ),

        // Neurological (2)
        HealthProfileOption(
            id = "Epilepsy",
            labelEn = "Epilepsy",
            labelUr = "مرگی (دورے پڑنا)",
            categoryId = ChronicConditionCategory.NEUROLOGICAL.id,
            aliases = listOf("seizures", "fits", "convulsions"),
        ),
        HealthProfileOption(
            id = "Migraine",
            labelEn = "Migraine",
            labelUr = "آدھے سر کا درد (مائیگرین)",
            categoryId = ChronicConditionCategory.NEUROLOGICAL.id,
            aliases = listOf("chronic headache", "aura headache"),
        ),

        // Endocrine & Thyroid (3)
        HealthProfileOption(
            id = "Thyroid Disorders",
            labelEn = "Thyroid Disorders",
            labelUr = "تھائیرائیڈ کے امراض",
            categoryId = ChronicConditionCategory.ENDOCRINE.id,
            aliases = listOf("THYROID", "goiter"),
        ),
        HealthProfileOption(
            id = "Hypothyroidism",
            labelEn = "Hypothyroidism",
            labelUr = "کم تھائیرائیڈ (ہائپو تھائیرائیڈزم)",
            categoryId = ChronicConditionCategory.ENDOCRINE.id,
            aliases = listOf("underactive thyroid"),
        ),
        HealthProfileOption(
            id = "Hyperthyroidism",
            labelEn = "Hyperthyroidism",
            labelUr = "زیادہ تھائیرائیڈ (ہائپر تھائیرائیڈزم)",
            categoryId = ChronicConditionCategory.ENDOCRINE.id,
            aliases = listOf("overactive thyroid", "graves"),
        ),

        // Musculoskeletal & Bone (4)
        HealthProfileOption(
            id = "Arthritis",
            labelEn = "Arthritis",
            labelUr = "جوڑوں کا درد (آرتھرائٹس)",
            categoryId = ChronicConditionCategory.MUSCULOSKELETAL.id,
            aliases = listOf("osteoarthritis", "joint pain"),
        ),
        HealthProfileOption(
            id = "Rheumatoid Arthritis",
            labelEn = "Rheumatoid Arthritis",
            labelUr = "گٹھیا (ریمیٹائڈ آرتھرائٹس)",
            categoryId = ChronicConditionCategory.MUSCULOSKELETAL.id,
            aliases = listOf("RA", "autoimmune arthritis"),
        ),
        HealthProfileOption(
            id = "Osteoporosis",
            labelEn = "Osteoporosis",
            labelUr = "ہڈیوں کی کمزوری",
            categoryId = ChronicConditionCategory.MUSCULOSKELETAL.id,
            aliases = listOf("low bone density", "weak bones"),
        ),
        HealthProfileOption(
            id = "Chronic Back Pain",
            labelEn = "Chronic Back Pain",
            labelUr = "کمر کا دائمی درد",
            categoryId = ChronicConditionCategory.MUSCULOSKELETAL.id,
            aliases = listOf("sciatica", "lumbar pain", "disc pain"),
        ),

        // Gastrointestinal (5)
        HealthProfileOption(
            id = "Chronic Gastritis",
            labelEn = "Chronic Gastritis",
            labelUr = "معدے کی دائمی سوزش",
            categoryId = ChronicConditionCategory.GASTROINTESTINAL.id,
            aliases = listOf("stomach inflammation", "ulcer", "acidity"),
        ),
        HealthProfileOption(
            id = "GERD",
            labelEn = "GERD (Acid Reflux)",
            labelUr = "تیزابیت اور جلن (GERD)",
            categoryId = ChronicConditionCategory.GASTROINTESTINAL.id,
            aliases = listOf("acid reflux", "heartburn"),
        ),
        HealthProfileOption(
            id = "Inflammatory Bowel Disease",
            labelEn = "Inflammatory Bowel Disease",
            labelUr = "آنتوں کی دائمی سوزش (IBD)",
            categoryId = ChronicConditionCategory.GASTROINTESTINAL.id,
            aliases = listOf("IBD", "bowel inflammation"),
        ),
        HealthProfileOption(
            id = "Crohn's Disease",
            labelEn = "Crohn's Disease",
            labelUr = "کرونز بیماری",
            categoryId = ChronicConditionCategory.GASTROINTESTINAL.id,
            aliases = listOf("crohns"),
        ),
        HealthProfileOption(
            id = "Ulcerative Colitis",
            labelEn = "Ulcerative Colitis",
            labelUr = "السرٹیو کولائٹس",
            categoryId = ChronicConditionCategory.GASTROINTESTINAL.id,
            aliases = listOf("colitis"),
        ),

        // Hematologic, Women's & Mental Health (6)
        HealthProfileOption(
            id = "Anemia",
            labelEn = "Anemia",
            labelUr = "خون کی کمی (انیمیا)",
            categoryId = ChronicConditionCategory.HEMATOLOGIC_OTHER.id,
            aliases = listOf("low hemoglobin", "iron deficiency"),
        ),
        HealthProfileOption(
            id = "PCOS",
            labelEn = "PCOS",
            labelUr = "پی سی او ایس (PCOS)",
            categoryId = ChronicConditionCategory.HEMATOLOGIC_OTHER.id,
            aliases = listOf("polycystic ovary syndrome"),
        ),
        HealthProfileOption(
            id = "Endometriosis",
            labelEn = "Endometriosis",
            labelUr = "اینڈومیٹریوسس",
            categoryId = ChronicConditionCategory.HEMATOLOGIC_OTHER.id,
            aliases = listOf("pelvic pain"),
        ),
        HealthProfileOption(
            id = "Depression",
            labelEn = "Depression",
            labelUr = "ڈپریشن",
            categoryId = ChronicConditionCategory.HEMATOLOGIC_OTHER.id,
            aliases = listOf("mood disorder", "low mood"),
        ),
        HealthProfileOption(
            id = "Anxiety Disorders",
            labelEn = "Anxiety Disorders",
            labelUr = "بے چینی اور گھبراہٹ کے امراض",
            categoryId = ChronicConditionCategory.HEMATOLOGIC_OTHER.id,
            aliases = listOf("panic disorder", "GAD"),
        ),
        HealthProfileOption(
            id = "Gout",
            labelEn = "Gout (High Uric Acid)",
            labelUr = "یورک ایسڈ / گاؤٹ",
            categoryId = ChronicConditionCategory.HEMATOLOGIC_OTHER.id,
            aliases = listOf("uric acid", "hyperuricemia"),
        ),
    )

    val allergies: List<HealthProfileOption> = listOf(
        // Food Allergies (8)
        HealthProfileOption(
            id = "Peanut Allergy",
            labelEn = "Peanut Allergy",
            labelUr = "مونگ پھلی سے الرجی",
            categoryId = AllergyCategory.FOOD.id,
            aliases = listOf("Peanuts", "groundnut"),
        ),
        HealthProfileOption(
            id = "Tree Nut Allergy",
            labelEn = "Tree Nut Allergy",
            labelUr = "خشک میوہ جات (بادام/اخروٹ) سے الرجی",
            categoryId = AllergyCategory.FOOD.id,
            aliases = listOf("nuts", "almonds", "walnuts", "cashew", "pistachio"),
        ),
        HealthProfileOption(
            id = "Milk Allergy",
            labelEn = "Milk / Dairy Allergy",
            labelUr = "دودھ / ڈیری سے الرجی",
            categoryId = AllergyCategory.FOOD.id,
            aliases = listOf("dairy", "lactose", "casein"),
        ),
        HealthProfileOption(
            id = "Egg Allergy",
            labelEn = "Egg Allergy",
            labelUr = "انڈے سے الرجی",
            categoryId = AllergyCategory.FOOD.id,
            aliases = listOf("eggs"),
        ),
        HealthProfileOption(
            id = "Wheat Allergy",
            labelEn = "Wheat / Gluten Allergy",
            labelUr = "گندم / گلوٹین سے الرجی",
            categoryId = AllergyCategory.FOOD.id,
            aliases = listOf("gluten", "celiac"),
        ),
        HealthProfileOption(
            id = "Soy Allergy",
            labelEn = "Soy Allergy",
            labelUr = "سویا سے الرجی",
            categoryId = AllergyCategory.FOOD.id,
            aliases = listOf("soybean", "tofu"),
        ),
        HealthProfileOption(
            id = "Fish Allergy",
            labelEn = "Fish Allergy",
            labelUr = "مچھلی سے الرجی",
            categoryId = AllergyCategory.FOOD.id,
            aliases = listOf("seafood fish"),
        ),
        HealthProfileOption(
            id = "Shellfish Allergy",
            labelEn = "Shellfish / Seafood Allergy",
            labelUr = "جھینگا / سی فوڈ سے الرجی",
            categoryId = AllergyCategory.FOOD.id,
            aliases = listOf("Seafood", "shrimp", "prawn", "crab"),
        ),

        // Environmental (5)
        HealthProfileOption(
            id = "Pollen Allergy",
            labelEn = "Pollen Allergy",
            labelUr = "پولن سے الرجی",
            categoryId = AllergyCategory.ENVIRONMENTAL.id,
            aliases = listOf("hay fever", "tree pollen", "paper mulberry"),
        ),
        HealthProfileOption(
            id = "Seasonal Allergies",
            labelEn = "Seasonal Allergies",
            labelUr = "موسمی الرجی",
            categoryId = AllergyCategory.ENVIRONMENTAL.id,
            aliases = listOf("allergic rhinitis", "spring allergy"),
        ),
        HealthProfileOption(
            id = "Dust Mite Allergy",
            labelEn = "Dust Mite Allergy",
            labelUr = "دھول مٹی سے الرجی",
            categoryId = AllergyCategory.ENVIRONMENTAL.id,
            aliases = listOf("house dust", "dust"),
        ),
        HealthProfileOption(
            id = "Mold Allergy",
            labelEn = "Mold Allergy",
            labelUr = "پھپھوندی / نمی سے الرجی",
            categoryId = AllergyCategory.ENVIRONMENTAL.id,
            aliases = listOf("fungus", "damp"),
        ),
        HealthProfileOption(
            id = "Pet Dander Allergy",
            labelEn = "Pet Dander Allergy",
            labelUr = "پالتو جانوروں کے بالوں سے الرجی",
            categoryId = AllergyCategory.ENVIRONMENTAL.id,
            aliases = listOf("cat", "dog", "fur"),
        ),

        // Medication (3)
        HealthProfileOption(
            id = "Medication Allergies",
            labelEn = "Medication Allergies",
            labelUr = "ادویات سے عمومی الرجی",
            categoryId = AllergyCategory.MEDICATION.id,
            aliases = listOf("drug allergy", "medicine reaction"),
        ),
        HealthProfileOption(
            id = "Antibiotic Allergies",
            labelEn = "Antibiotic Allergies (Penicillin / Sulfa)",
            labelUr = "اینٹی بائیوٹک (پینسلین / سلفا) سے الرجی",
            categoryId = AllergyCategory.MEDICATION.id,
            aliases = listOf("Penicillin", "Sulfa Drugs", "amoxicillin", "cephalosporin"),
        ),
        HealthProfileOption(
            id = "NSAIDs / Painkiller Allergies",
            labelEn = "NSAIDs / Painkiller Allergies",
            labelUr = "درد کش ادویات (اسپرین / آئیبوپروفین) سے الرجی",
            categoryId = AllergyCategory.MEDICATION.id,
            aliases = listOf("NSAIDs / Aspirin", "aspirin", "ibuprofen", "diclofenac"),
        ),

        // Insect & Contact (2)
        HealthProfileOption(
            id = "Insect Sting Allergy",
            labelEn = "Insect Sting Allergy",
            labelUr = "کیڑے مکوڑوں کے ڈنک سے الرجی",
            categoryId = AllergyCategory.CONTACT_INSECT.id,
            aliases = listOf("bee sting", "wasp", "ant bite"),
        ),
        HealthProfileOption(
            id = "Latex Allergy",
            labelEn = "Latex Allergy",
            labelUr = "لیٹکس / ربڑ سے الرجی",
            categoryId = AllergyCategory.CONTACT_INSECT.id,
            aliases = listOf("rubber", "latex gloves"),
        ),
    )

    private val conditionLookup: Map<String, HealthProfileOption> = buildMap {
        chronicConditions.forEach { opt ->
            put(opt.id.lowercase(), opt)
            opt.aliases.forEach { alias -> put(alias.lowercase(), opt) }
        }
    }

    private val allergyLookup: Map<String, HealthProfileOption> = buildMap {
        allergies.forEach { opt ->
            put(opt.id.lowercase(), opt)
            opt.aliases.forEach { alias -> put(alias.lowercase(), opt) }
        }
    }

    fun isSentinelOption(value: String): Boolean {
        val norm = value.trim().uppercase()
        return norm.isEmpty() ||
            norm == PROFILE_OPTION_NONE ||
            norm == PROFILE_OPTION_NOT_SURE ||
            norm == "NONE" ||
            norm == "NONE OF THESE" ||
            norm == "I'M NOT SURE" ||
            norm == "NOT SURE"
    }

    fun conditionDisplayLabel(raw: String, isUr: Boolean): String {
        val norm = raw.trim().uppercase()
        if (norm == PROFILE_OPTION_NONE) return if (isUr) "ان میں سے کوئی نہیں" else "None of these"
        if (norm == PROFILE_OPTION_NOT_SURE) return if (isUr) "یقین نہیں / معلوم نہیں" else "I'm not sure"
        val match = conditionLookup[raw.trim().lowercase()]
        return if (match != null) (if (isUr) match.labelUr else match.labelEn) else raw
    }

    fun allergyDisplayLabel(raw: String, isUr: Boolean): String {
        val norm = raw.trim().uppercase()
        if (norm == PROFILE_OPTION_NONE) return if (isUr) "ان میں سے کوئی نہیں" else "None of these"
        if (norm == PROFILE_OPTION_NOT_SURE) return if (isUr) "یقین نہیں / معلوم نہیں" else "I'm not sure"
        val match = allergyLookup[raw.trim().lowercase()]
        return if (match != null) (if (isUr) match.labelUr else match.labelEn) else raw
    }

    fun matchesCondition(stored: List<String>, vararg keywords: String): Boolean {
        val clinical = stored.filterNot(::isSentinelOption)
        return clinical.any { item ->
            val low = item.lowercase()
            val mapped = conditionLookup[low]
            keywords.any { kw ->
                val k = kw.lowercase()
                low.contains(k) ||
                    mapped?.id?.lowercase()?.contains(k) == true ||
                    mapped?.aliases?.any { it.lowercase() == k } == true
            }
        }
    }

    fun matchesAllergy(stored: List<String>, vararg keywords: String): Boolean {
        val clinical = stored.filterNot(::isSentinelOption)
        return clinical.any { item ->
            val low = item.lowercase()
            val mapped = allergyLookup[low]
            keywords.any { kw ->
                val k = kw.lowercase()
                low.contains(k) ||
                    mapped?.id?.lowercase()?.contains(k) == true ||
                    mapped?.aliases?.any { it.lowercase() == k } == true
            }
        }
    }

    fun buildProfilePrecautions(patient: PatientProfile): Pair<List<String>, List<String>> {
        val conditions = patient.clinicalChronicConditions()
        val allergies = patient.clinicalAllergies()
        val en = mutableListOf<String>()
        val ur = mutableListOf<String>()

        if (allergies.isNotEmpty()) {
            val labelsEn = allergies.joinToString(", ") { allergyDisplayLabel(it, isUr = false) }
            val labelsUr = allergies.joinToString("، ") { allergyDisplayLabel(it, isUr = true) }
            en.add("Recorded allergy alert ($labelsEn): always confirm medications, syrups, and home-remedy ingredients before taking them.")
            ur.add("الرجی انتباہ ($labelsUr): کوئی بھی دوا، شربت یا گھریلو نسخہ استعمال کرنے سے پہلے اجزاء کی تصدیق ضرور کریں۔")
        }

        if (matchesAllergy(allergies, "antibiotic", "penicillin", "sulfa", "medication")) {
            en.add("Medication/antibiotic allergy on file: inform the doctor or pharmacist before any prescription or injection.")
            ur.add("ادویات یا اینٹی بائیوٹک سے الرجی درج ہے: کسی بھی نسخے یا انجیکشن سے پہلے ڈاکٹر کو لازمی بتائیں۔")
        }

        if (
            matchesAllergy(allergies, "nsaid", "aspirin", "ibuprofen", "painkiller") ||
            matchesCondition(conditions, "kidney", "gastritis", "gerd", "ulcerative colitis", "crohn", "asthma")
        ) {
            en.add("Caution with OTC NSAID painkillers (ibuprofen, diclofenac, aspirin): verify safe fever/pain medicine with a clinician.")
            ur.add("درد کش ادویات (آئیبوپروفین، ڈیکلوفینک، اسپرین) میں احتیاط کریں؛ ڈاکٹر کے مشورے کے بغیر استعمال نہ کریں۔")
        }

        if (matchesCondition(conditions, "diabetes")) {
            en.add("Diabetes on profile: monitor blood glucose closely during illness and avoid high-sugar syrups without clinician advice.")
            ur.add("ذیابیطس کی صورت میں بیماری کے دوران شوگر چیک کرتے رہیں اور میٹھے شربت ڈاکٹر کے مشورے سے لیں۔")
        }

        if (matchesCondition(conditions, "hypertension", "heart", "coronary", "hyperthyroidism")) {
            en.add("Cardiovascular/thyroid profile note: avoid OTC oral decongestants (e.g., pseudoephedrine) that can raise blood pressure or heart rate.")
            ur.add("دل، ہائی بلڈ پریشر یا تھائیرائیڈ کی صورت میں نزلہ زکام کی بعض ادویات (ڈیکونجسٹنٹ) بلڈ پریشر بڑھا سکتی ہیں؛ معالج سے پوچھیں۔")
        }

        if (matchesCondition(conditions, "hypotension")) {
            en.add("Low BP note: maintain steady oral fluids if unwell and rise slowly from sitting or lying positions.")
            ur.add("کم بلڈ پریشر کی صورت میں پانی اور نمکیات کا توازن رکھیں اور بیٹھنے یا لیٹنے کے بعد آہستہ اٹھیں۔")
        }

        if (matchesCondition(conditions, "liver")) {
            en.add("Chronic liver condition note: confirm safe paracetamol/acetaminophen dosing with a physician.")
            ur.add("جگر کی بیماری کی صورت میں بخار یا درد کی دوا (پیراسیٹامول) کی محفوظ مقدار ڈاکٹر سے معلوم کریں۔")
        }

        if (matchesCondition(conditions, "asthma", "copd")) {
            en.add("Airway profile note: keep prescribed inhalers accessible and seek prompt care if wheezing or breathlessness worsens.")
            ur.add("دمہ یا سانس کی بیماری میں اپنا انہیلر ساتھ رکھیں اور سانس پھولنے کی صورت میں فوری ڈاکٹر سے رجوع کریں۔")
        }

        if (matchesCondition(conditions, "epilepsy")) {
            en.add("Epilepsy note: continue prescribed anti-seizure medicines on time and seek care if vomiting prevents keeping doses down.")
            ur.add("مرگی کی صورت میں باقاعدہ دوائیں وقت پر لیں؛ قے کی وجہ سے دوا نہ رکنے پر فوراً معالج سے رابطہ کریں۔")
        }

        return en to ur
    }
}

fun PatientProfile.clinicalChronicConditions(): List<String> =
    chronicConditions.filterNot { HealthProfileCatalog.isSentinelOption(it) }

fun PatientProfile.clinicalAllergies(): List<String> =
    allergies.filterNot { HealthProfileCatalog.isSentinelOption(it) }
