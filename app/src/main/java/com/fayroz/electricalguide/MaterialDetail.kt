package com.fayroz.electricalguide

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun MaterialDetail(material: MaterialEntity, offers: List<OfferEntity>, brands: List<BrandEntity>,
                   images: List<PhotoEntity>, category: CategoryEntity?, repository: GuideRepository,
                   activeTab: Int, onTabChange: (Int) -> Unit,
                   onGallery: () -> Unit, onCamera: () -> Unit,
                   onPrimary: (PhotoEntity) -> Unit, onDeletePhoto: (PhotoEntity) -> Unit,
                   onEditOffer: (OfferEntity) -> Unit, onAddOffer: () -> Unit,
                   onRemoveOffer: (OfferEntity) -> Unit, onEditBrand: (BrandEntity) -> Unit,
                   onAddBrand: () -> Unit, onDeleteBrand: (BrandEntity) -> Unit,
                   onUpdateNote: (String) -> Unit) {
    val tabs = listOf("التعريف", "المواصفات", "التركيب", "الشركات والأسعار", "ملاحظاتي")
    var editNote by remember(material.id, material.note) { mutableStateOf(material.note) }
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = activeTab, edgePadding = 8.dp, divider = {}) {
            tabs.forEachIndexed { index, label ->
                Tab(selected = activeTab == index, onClick = { onTabChange(index) },
                    text = { Text(label, maxLines = 1, fontSize = 12.sp) })
            }
        }
        LazyColumn(Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            when (activeTab) {
                0 -> {
                    item {
                        PhotoStrip(images, repository, onGallery, onCamera, onPrimary, onDeletePhoto)
                    }
                    item { InformationTile("بتستخدم في إيه؟", material.description) }
                    item { InformationTile("الأنواع المتاحة", material.types) }
                    item { InformationTile("مهم تعرف", material.caution, important = true) }
                    item { Text("الاسم الفني: ${material.en}\nالتصنيف: ${category?.title.orEmpty()} • ${material.sub}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                1 -> {
                    item { InformationTile("أختارها إزاي؟", material.choose) }
                    item { InformationTile("المواصفات الفنية", material.deepSpec) }
                    item { InformationTile("الفرق بينها وبين البدائل", material.deepCompare) }
                    if (!category?.glossary.isNullOrBlank()) {
                        item { InformationTile("معاني بعض المصطلحات", category!!.glossary) }
                    }
                }
                2 -> {
                    item { InformationTile("ملاحظات التركيب", material.install) }
                    item { InformationTile("تفاصيل مهمة عند التركيب", material.deepInstall) }
                    item { InformationTile("أعطال وأخطاء متوقعة", material.deepFault) }
                    item { InformationTile("تحذير", material.caution, important = true) }
                    item { Text("تنبيه: توصيل وفحص دوائر الكهرباء يجب أن يتم بواسطة شخص مؤهل وبعد اتباع إجراءات فصل وتأمين التغذية.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                3 -> {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${offers.size} عرض سعر", fontWeight = FontWeight.Bold)
                            TextButton(onClick = onAddOffer) { Icon(Icons.Default.Add, null); Text("إضافة سعر") }
                        }
                    }
                    if (offers.isEmpty()) item { InformationTile("لا يوجد سعر", "يمكنك إضافة سعر بالمقاس والماركة واسم المورد.") }
                    items(offers, key = { it.id }) { offer ->
                        OfferTile(offer, onEditOffer, onRemoveOffer)
                    }
                    item {
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الشركات المرتبطة بالصنف", fontWeight = FontWeight.Bold)
                            TextButton(onClick = onAddBrand) { Icon(Icons.Default.Add, null); Text("إضافة شركة") }
                        }
                    }
                    item { Text("تقييم الجودة والشهرة من إدخالك أنت، وليس ترتيبًا رسميًا أو معتمدًا للسوق.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(brands.sortedWith(compareByDescending<BrandEntity> { it.quality + it.popularity }.thenBy { it.name }),
                        key = { it.id }) { brand -> BrandTile(brand, onEditBrand, onDeleteBrand) }
                }
                4 -> {
                    item { InformationTile("ملاحظاتي على القطعة", "اكتب هنا خبرات التنفيذ، ملاحظات الشراء، أو بيانات إضافية؛ هتتحفظ على جهازك.") }
                    item { OutlinedTextField(value = editNote, onValueChange = { editNote = it },
                        label = { Text("الملاحظات") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 3, maxLines = 8) }
                    item { Button(onClick = { onUpdateNote(editNote) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("حفظ الملاحظات")
                    } }
                }
            }
        }
    }
}

@Composable
private fun InformationTile(title: String, body: String, important: Boolean = false) {
    if (body.isBlank()) return
    Surface(shape = RoundedCornerShape(12.dp),
        color = if (important) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface,
        tonalElevation = if (important) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(body, fontSize = 13.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PhotoStrip(photos: List<PhotoEntity>, repository: GuideRepository,
                       onGallery: () -> Unit, onCamera: () -> Unit,
                       onPrimary: (PhotoEntity) -> Unit, onDelete: (PhotoEntity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("الصور", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onGallery, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(17.dp));
                Spacer(Modifier.width(4.dp)); Text("المعرض", fontSize = 12.sp)
            }
            TextButton(onClick = onCamera, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(17.dp));
                Spacer(Modifier.width(4.dp)); Text("الكاميرا", fontSize = 12.sp)
            }
        }
        if (photos.isEmpty()) {
            Text("مافيش صورة محفوظة للقطعة. تقدر تضيف صورتها من الموبايل.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(photos.sortedWith(compareByDescending<PhotoEntity> { it.isPrimary }.thenBy { it.id }),
                    key = { it.id }) { photo ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            AsyncImage(model = repository.imageModel(photo.location),
                                contentDescription = photo.caption, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(104.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onPrimary(photo) }, modifier = Modifier.size(34.dp)) {
                                Icon(if (photo.isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                                    "اختيار الرئيسية", modifier = Modifier.size(19.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDelete(photo) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.DeleteOutline, "حذف الصورة", modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }
        }
        Text("النجمة لتعيين الصورة الرئيسية. الصور اللي تضيفها تتحفظ داخل التطبيق وتدخل في النسخة الاحتياطية.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OfferTile(offer: OfferEntity, onEdit: (OfferEntity) -> Unit,
                      onDelete: (OfferEntity) -> Unit) {
    val context = LocalContext.current
    Surface(tonalElevation = 1.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(offer.brand.ifBlank { "غير محدد" }, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(priceText(offer), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (offer.model.isNotBlank()) Text(offer.model, fontSize = 12.sp)
            if (offer.variant.isNotBlank()) Text(offer.variant, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("المورد: ${offer.supplier.ifBlank { "غير مذكور" }} • ${offer.checked.ifBlank { "تاريخ غير محدد" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            if (offer.currency != "EGP") Text("سعر بعملة أجنبية؛ ليس سعر توريد بالجنيه المصري",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            if (offer.taxStatus.isNotBlank()) Text(offer.taxStatus,
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                if (offer.source.isNotBlank()) TextButton(onClick = { openWeb(context, offer.source) }) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp)); Text("المصدر")
                }
                IconButton(onClick = { onEdit(offer) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "تعديل العرض", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDelete(offer) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, "حذف السعر", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun BrandTile(brand: BrandEntity, onEdit: (BrandEntity) -> Unit,
                      onDelete: (BrandEntity) -> Unit) {
    val context = LocalContext.current
    Surface(tonalElevation = 1.dp, shape = RoundedCornerShape(11.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(brand.name, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("الجودة: ${if (brand.quality == 0) "بدون تقييم" else "${brand.quality}/5"} • " +
                    "الشهرة: ${if (brand.popularity == 0) "بدون تقييم" else "${brand.popularity}/5"}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (brand.website.isNotBlank()) IconButton(onClick = { openWeb(context, brand.website) },
                modifier = Modifier.size(33.dp)) { Icon(Icons.Default.OpenInNew, "موقع الشركة", modifier = Modifier.size(17.dp)) }
            IconButton(onClick = { onEdit(brand) }, modifier = Modifier.size(33.dp)) {
                Icon(Icons.Default.Edit, "تعديل الشركة", modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = { onDelete(brand) }, modifier = Modifier.size(33.dp)) {
                Icon(Icons.Default.DeleteOutline, "حذف الشركة", modifier = Modifier.size(17.dp))
            }
        }
    }
}
