package com.fayroz.electricalguide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import java.util.UUID

@Composable
private fun Input(label: String, value: String, onChange: (String) -> Unit,
                  lines: Int = 1, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = lines == 1, maxLines = lines.coerceAtLeast(1),
        modifier = modifier.fillMaxWidth())
}

@Composable
fun MaterialEditor(initial: MaterialEntity?, categories: List<CategoryEntity>, defaultCategory: String?,
                   onDismiss: () -> Unit, onSave: (MaterialEntity) -> Unit,
                   onDelete: (MaterialEntity) -> Unit) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var en by remember(initial?.id) { mutableStateOf(initial?.en.orEmpty()) }
    var sub by remember(initial?.id) { mutableStateOf(initial?.sub.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var types by remember(initial?.id) { mutableStateOf(initial?.types.orEmpty()) }
    var choose by remember(initial?.id) { mutableStateOf(initial?.choose.orEmpty()) }
    var install by remember(initial?.id) { mutableStateOf(initial?.install.orEmpty()) }
    var caution by remember(initial?.id) { mutableStateOf(initial?.caution.orEmpty()) }
    var spec by remember(initial?.id) { mutableStateOf(initial?.deepSpec.orEmpty()) }
    var compare by remember(initial?.id) { mutableStateOf(initial?.deepCompare.orEmpty()) }
    var details by remember(initial?.id) { mutableStateOf(initial?.deepInstall.orEmpty()) }
    var faults by remember(initial?.id) { mutableStateOf(initial?.deepFault.orEmpty()) }
    var category by remember(initial?.id) {
        mutableStateOf(initial?.category ?: defaultCategory ?: categories.firstOrNull()?.id.orEmpty())
    }
    var categoryOpen by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة خامة جديدة" else "تعديل بيانات الخامة") },
        text = {
            Column(Modifier.heightIn(max = 460.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = page == 0, onClick = { page = 0 }, label = { Text("التعريف") })
                    FilterChip(selected = page == 1, onClick = { page = 1 }, label = { Text("المعلومات الفنية") })
                }
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (page == 0) {
                        Input("اسم القطعة *", title, { title = it })
                        Input("الاسم الإنجليزي", en, { en = it })
                        Input("القسم الفرعي", sub, { sub = it })
                        Box {
                            OutlinedButton(onClick = { categoryOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("القسم: ${categories.firstOrNull { it.id == category }?.title ?: category}")
                            }
                            DropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                                categories.forEach { item ->
                                    DropdownMenuItem(text = { Text(item.title) }, onClick = {
                                        category = item.id; categoryOpen = false
                                    })
                                }
                            }
                        }
                        Input("بتستخدم في إيه؟", description, { description = it }, lines = 4)
                        Input("الأنواع", types, { types = it }, lines = 4)
                    } else {
                        Input("طريقة الاختيار", choose, { choose = it }, lines = 4)
                        Input("ملاحظات التركيب", install, { install = it }, lines = 4)
                        Input("تحذيرات", caution, { caution = it }, lines = 4)
                        Input("المواصفات الفنية", spec, { spec = it }, lines = 5)
                        Input("الفرق عن البدائل", compare, { compare = it }, lines = 4)
                        Input("تفاصيل التركيب", details, { details = it }, lines = 5)
                        Input("الأعطال الشائعة", faults, { faults = it }, lines = 4)
                    }
                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) { error = "اكتب اسم القطعة"; page = 0 }
                else if (category.isBlank()) { error = "اختار القسم"; page = 0 }
                else {
                    onSave(MaterialEntity(
                        id = initial?.id ?: "local_${UUID.randomUUID()}", category = category,
                        title = title.trim(), en = en.trim(), sub = sub.trim(),
                        description = description.trim(), types = types.trim(), choose = choose.trim(),
                        install = install.trim(), caution = caution.trim(), deepSpec = spec.trim(),
                        deepCompare = compare.trim(), deepInstall = details.trim(), deepFault = faults.trim(),
                        favorite = initial?.favorite ?: false, note = initial?.note.orEmpty(),
                        isCustom = initial?.isCustom ?: true
                    ))
                }
            }) { Text("حفظ") }
        },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { onDelete(initial) }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            }
        }
    )
}

@Composable
fun OfferEditor(existing: OfferEntity?, materialId: String,
                onDismiss: () -> Unit, onSave: (OfferEntity) -> Unit) {
    var brand by remember(existing?.id) { mutableStateOf(existing?.brand.orEmpty()) }
    var model by remember(existing?.id) { mutableStateOf(existing?.model.orEmpty()) }
    var variant by remember(existing?.id) { mutableStateOf(existing?.variant.orEmpty()) }
    var amount by remember(existing?.id) { mutableStateOf(existing?.price?.toString().orEmpty()) }
    var currency by remember(existing?.id) { mutableStateOf(existing?.currency ?: "EGP") }
    var unit by remember(existing?.id) { mutableStateOf(existing?.unit ?: "قطعة") }
    var supplier by remember(existing?.id) { mutableStateOf(existing?.supplier.orEmpty()) }
    var source by remember(existing?.id) { mutableStateOf(existing?.source.orEmpty()) }
    var checked by remember(existing?.id) { mutableStateOf(existing?.checked.orEmpty()) }
    var status by remember(existing?.id) { mutableStateOf(existing?.status.orEmpty()) }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("${if (existing == null) "إضافة" else "تعديل"} سعر") },
        text = {
            Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Input("الشركة *", brand, { brand = it })
                Input("الموديل / الكود", model, { model = it })
                Input("المواصفة والمقاس", variant, { variant = it }, lines = 3)
                Input("السعر *", amount, { amount = it })
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("EGP", "USD", "EUR").forEach { c ->
                        FilterChip(selected = currency == c, onClick = { currency = c }, label = { Text(c) })
                    }
                }
                Input("الوحدة (قطعة / متر / لفة ...)", unit, { unit = it })
                Input("المورد / المحل", supplier, { supplier = it })
                Input("رابط المصدر", source, { source = it })
                Input("تاريخ السعر YYYY-MM-DD", checked, { checked = it })
                Input("ملاحظات حالة السعر والضريبة", status, { status = it }, lines = 3)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amount.trim().replace(",", "").toDoubleOrNull()
                if (brand.isBlank() || value == null || value < 0 || !value.isFinite()) {
                    error = "اكتب الشركة وسعرًا رقميًا صحيحًا"; return@TextButton
                }
                onSave(OfferEntity(
                    id = existing?.id ?: 0L, materialId = materialId,
                    brand = brand.trim(), model = model.trim(), variant = variant.trim(),
                    price = value, currency = currency, unit = unit.trim(), supplier = supplier.trim(),
                    source = source.trim(), checked = checked.trim(), kind = existing?.kind ?: "سعر أدخله المستخدم",
                    status = status.trim(), taxStatus = existing?.taxStatus.orEmpty(), isCustom = true
                ))
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
fun BrandEditor(existing: BrandEntity?, materialId: String,
                onDismiss: () -> Unit, onSave: (BrandEntity) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var website by remember(existing?.id) { mutableStateOf(existing?.website.orEmpty()) }
    var evidence by remember(existing?.id) { mutableStateOf(existing?.evidence.orEmpty()) }
    var quality by remember(existing?.id) { mutableFloatStateOf((existing?.quality ?: 0).toFloat()) }
    var popularity by remember(existing?.id) { mutableFloatStateOf((existing?.popularity ?: 0).toFloat()) }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("بيانات الشركة وتقييمك") },
        text = {
            Column(Modifier.heightIn(max = 445.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Input("اسم الشركة *", name, { name = it })
                Input("موقع الشركة", website, { website = it })
                Input("ملاحظة عن توفر منتجات الشركة", evidence, { evidence = it }, lines = 3)
                Text("الجودة من وجهة نظرك: ${quality.roundToInt()}/5")
                Slider(value = quality, onValueChange = { quality = it }, valueRange = 0f..5f, steps = 4)
                Text("الشهرة من وجهة نظرك: ${popularity.roundToInt()}/5")
                Slider(value = popularity, onValueChange = { popularity = it }, valueRange = 0f..5f, steps = 4)
                Text("صفر يعني بدون تقييم، وليس جودة صفرية.", style = MaterialTheme.typography.labelSmall)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            if (name.isBlank()) { error = "اسم الشركة مطلوب"; return@TextButton }
            onSave(BrandEntity(
                id = existing?.id ?: 0L, materialId = materialId, name = name.trim(),
                website = website.trim(), evidence = evidence.trim(),
                quality = quality.roundToInt(), popularity = popularity.roundToInt()
            ))
        }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}
