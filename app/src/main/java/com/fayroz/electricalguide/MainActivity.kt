package com.fayroz.electricalguide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = GuideRepository(applicationContext)
        setContent { ElectricalGuideApp(repository) }
    }
}

private val DeepNavy = androidx.compose.ui.graphics.Color(0xFF17324D)
private val Teal = androidx.compose.ui.graphics.Color(0xFF008E91)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricalGuideApp(repo: GuideRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items by repo.materials.collectAsState(initial = emptyList())
    val cats by repo.categories.collectAsState(initial = emptyList())
    val prices by repo.offers.collectAsState(initial = emptyList())
    val brands by repo.brands.collectAsState(initial = emptyList())
    val photos by repo.photos.collectAsState(initial = emptyList())
    var dark by remember { mutableStateOf(repo.darkMode()) }
    var page by remember { mutableStateOf("home") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }
    var detailTab by remember { mutableIntStateOf(0) }
    var editMaterial by remember { mutableStateOf<MaterialEntity?>(null) }
    var materialEditorOpen by remember { mutableStateOf(false) }
    var offerEditor by remember { mutableStateOf<OfferEntity?>(null) }
    var offerEditorOpen by remember { mutableStateOf(false) }
    var brandEditor by remember { mutableStateOf<BrandEntity?>(null) }
    var brandEditorOpen by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<MaterialEntity?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val selected = items.firstOrNull { it.id == selectedId }

    LaunchedEffect(Unit) {
        try { repo.ensureSeeded() } catch (e: Exception) { message = "تعذر تحميل المكتبة: ${e.message}" }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && selectedId != null) scope.launch {
            try { repo.addPhoto(selectedId!!, uri); message = "تمت إضافة الصورة" }
            catch (e: Exception) { message = "تعذر إضافة الصورة: ${e.message}" }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingCameraFile
        if (success && file != null && selectedId != null) scope.launch {
            try { repo.addPhoto(selectedId!!, Uri.fromFile(file)); message = "تم حفظ الصورة" }
            catch (e: Exception) { message = "تعذر حفظ الصورة: ${e.message}" }
            finally { file.delete() }
        } else file?.delete()
    }
    val backupExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                repo.exportBackup(uri, GuideBackup(
                    categories = cats, materials = items, brands = brands, offers = prices, photos = photos
                ))
                message = "تم تصدير المكتبة والصور والأسعار"
            } catch (e: Exception) { message = "خطأ تصدير النسخة: ${e.message}" }
        }
    }
    val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try { repo.restoreBackup(uri); selectedId = null; page = "home"; message = "تم استرجاع البيانات والصور" }
            catch (e: Exception) { message = "تعذر الاسترجاع: ${e.message}" }
        }
    }
    val isDetail = selected != null
    val listed = remember(items, prices, brands, category, query, page) {
        val q = query.trim()
        items.filter { m ->
            (page != "favorites" || m.favorite) &&
            (category == "all" || m.category == category) &&
            (q.isBlank() || listOf(m.title, m.en, m.sub, m.description, m.types, m.choose)
                .any { it.contains(q, ignoreCase = true) } ||
                prices.any { it.materialId == m.id &&
                    listOf(it.brand, it.model, it.variant, it.supplier).any { s -> s.contains(q, true) } } ||
                brands.any { it.materialId == m.id && it.name.contains(q, true) })
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF45CFCA))
                                    else lightColorScheme(primary = Teal, secondary = DeepNavy)) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(if (isDetail) selected!!.title else "دليل خامات الكهرباء", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (isDetail) selected!!.sub else "${items.size} قطعة • ${prices.size} عرض سعر",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        navigationIcon = {
                            if (isDetail) IconButton(onClick = { selectedId = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "رجوع")
                            }
                        },
                        actions = {
                            if (isDetail) {
                                IconButton(onClick = {
                                    editMaterial = selected; materialEditorOpen = true
                                }) { Icon(Icons.Default.Edit, "تعديل بيانات القطعة") }
                            } else {
                                IconButton(onClick = { dark = !dark; repo.setDarkMode(dark) }) {
                                    Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "الوضع الليلي")
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isDetail) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            listOf(
                                Triple("home", "الأقسام", Icons.Default.GridView),
                                Triple("search", "بحث", Icons.Default.Search),
                                Triple("favorites", "المفضلة", Icons.Default.FavoriteBorder),
                                Triple("admin", "الإدارة", Icons.Default.Settings)
                            ).forEach { (dest, label, icon) ->
                                NavigationBarItem(selected = page == dest,
                                    onClick = { page = dest; if (dest == "favorites") category = "all" },
                                    icon = { Icon(icon, label) }, label = { Text(label, fontSize = 11.sp) })
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!isDetail && page != "admin") {
                        SmallFloatingActionButton(onClick = { editMaterial = null; materialEditorOpen = true }) {
                            Icon(Icons.Default.Add, "إضافة قطعة")
                        }
                    }
                }
            ) { inset ->
                Box(Modifier.fillMaxSize().padding(inset)) {
                    if (isDetail) {
                        MaterialDetail(
                            material = selected!!, offers = prices.filter { it.materialId == selected!!.id },
                            brands = brands.filter { it.materialId == selected!!.id },
                            images = photos.filter { it.materialId == selected!!.id },
                            category = cats.firstOrNull { it.id == selected!!.category },
                            repository = repo,
                            activeTab = detailTab, onTabChange = { detailTab = it },
                            onGallery = { gallery.launch("image/*") },
                            onCamera = {
                                try {
                                    val file = File.createTempFile("picture_", ".jpg", context.cacheDir)
                                    pendingCameraFile = file
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                                    camera.launch(uri)
                                } catch (e: Exception) { message = "تعذر فتح الكاميرا: ${e.message}" }
                            },
                            onPrimary = { scope.launch { repo.selectPrimary(it) } },
                            onDeletePhoto = { scope.launch { repo.removePhoto(it) } },
                            onEditOffer = { offerEditor = it; offerEditorOpen = true },
                            onAddOffer = { offerEditor = null; offerEditorOpen = true },
                            onRemoveOffer = { scope.launch { repo.removeOffer(it) } },
                            onEditBrand = { brandEditor = it; brandEditorOpen = true },
                            onAddBrand = { brandEditor = null; brandEditorOpen = true },
                            onDeleteBrand = { scope.launch { repo.removeBrand(it) } },
                            onUpdateNote = { value -> scope.launch { repo.saveMaterial(selected!!.copy(note = value)) } }
                        )
                    } else if (page == "admin") {
                        AdminPage(items.size, prices.size, photos.size,
                            onAdd = { editMaterial = null; materialEditorOpen = true },
                            onExport = { backupExport.launch("Fayroz_Electrical_Guide_backup.zip") },
                            onImport = { backupImport.launch(arrayOf("application/zip", "application/octet-stream")) },
                            onDark = { dark = !dark; repo.setDarkMode(dark) }, dark = dark,
                            onDelete = { material -> toDelete = material }, customRows = items.filter { it.isCustom })
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            OutlinedTextField(value = query, onValueChange = { query = it },
                                label = { Text("ابحث باسم القطعة أو الشركة أو الموديل") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح") } },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                                singleLine = true, shape = RoundedCornerShape(12.dp))
                            if (page != "favorites") {
                                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        FilterChip(selected = category == "all", onClick = { category = "all" },
                                            label = { Text("الكل") })
                                    }
                                    items(cats, key = { it.id }) { cat ->
                                        FilterChip(selected = category == cat.id,
                                            onClick = { category = cat.id }, label = { Text("${cat.emoji} ${cat.title}") })
                                    }
                                }
                            }
                            Text("${listed.size} قطعة", modifier = Modifier.padding(horizontal = 15.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyColumn(modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 85.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                items(listed, key = { it.id }) { mat ->
                                    CompactMaterialRow(mat, photos.firstOrNull { it.materialId == mat.id }, repo,
                                        onClick = { selectedId = mat.id; detailTab = 0 },
                                        onFavorite = { scope.launch { repo.saveMaterial(mat.copy(favorite = !mat.favorite)) } })
                                }
                            }
                        }
                    }
                }
            }
            if (materialEditorOpen) MaterialEditor(
                initial = editMaterial, categories = cats, defaultCategory = category.takeUnless { it == "all" },
                onDismiss = { materialEditorOpen = false }, onSave = { row ->
                    scope.launch { repo.saveMaterial(row); selectedId = row.id; materialEditorOpen = false }
                }, onDelete = { row -> materialEditorOpen = false; toDelete = row }
            )
            if (offerEditorOpen && selected != null) OfferEditor(
                existing = offerEditor, materialId = selected.id,
                onDismiss = { offerEditorOpen = false }, onSave = { row ->
                    scope.launch { repo.saveOffer(row); offerEditorOpen = false }
                })
            if (brandEditorOpen && selected != null) BrandEditor(
                existing = brandEditor, materialId = selected.id,
                onDismiss = { brandEditorOpen = false }, onSave = { row ->
                    scope.launch { repo.saveBrand(row); brandEditorOpen = false }
                })
            toDelete?.let { mat ->
                AlertDialog(onDismissRequest = { toDelete = null },
                    title = { Text("حذف الصنف؟") },
                    text = { Text("سيُحذف الصنف وكل عروض أسعاره وصوره من التطبيق. لا يمكن التراجع إلا باستعادة نسخة احتياطية.") },
                    confirmButton = { TextButton(onClick = {
                        scope.launch { repo.deleteMaterial(mat, photos); if (selectedId == mat.id) selectedId = null; toDelete = null }
                    }) { Text("حذف نهائيًا") } },
                    dismissButton = { TextButton(onClick = { toDelete = null }) { Text("إلغاء") } })
            }
            message?.let { msg ->
                AlertDialog(onDismissRequest = { message = null }, title = { Text("دليل خامات الكهرباء") },
                    text = { Text(msg) }, confirmButton = { TextButton(onClick = { message = null }) { Text("حسنًا") } })
            }
        }
    }
}

@Composable
private fun CompactMaterialRow(material: MaterialEntity, picture: PhotoEntity?, repo: GuideRepository,
                               onClick: () -> Unit, onFavorite: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 66.dp).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            CompactPhoto(picture, repo, Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {
                Text(material.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(material.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(modifier = Modifier.size(38.dp), onClick = onFavorite) {
                Icon(if (material.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "المفضلة", tint = if (material.favorite) Teal else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
internal fun CompactPhoto(photo: PhotoEntity?, repo: GuideRepository, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center) {
        if (photo != null) AsyncImage(model = repo.imageModel(photo.location), contentDescription = photo.caption,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Default.ElectricalServices, "صورة غير متاحة", tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AdminPage(count: Int, prices: Int, photos: Int, onAdd: () -> Unit, onExport: () -> Unit,
                      onImport: () -> Unit, onDark: () -> Unit, dark: Boolean,
                      onDelete: (MaterialEntity) -> Unit, customRows: List<MaterialEntity>) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)) {
        item { Text("إدارة المكتبة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { Text("$count قطعة • $prices عرض سعر • $photos صورة مرتبطة", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("إضافة خامة") } }
        item { OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Upload, null); Spacer(Modifier.width(8.dp)); Text("تصدير نسخة احتياطية تشمل الصور والأسعار") } }
        item { OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("استرجاع نسخة احتياطية ZIP") } }
        item { OutlinedButton(onClick = onDark, modifier = Modifier.fillMaxWidth()) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, null); Spacer(Modifier.width(8.dp)); Text("تغيير الوضع الفاتح / الداكن") } }
        item { Text("الأسعار المنشورة في المكتبة استرشادية؛ راجع تاريخ العرض والموديل قبل الشراء.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (customRows.isNotEmpty()) item { Text("الأصناف التي أضفتها", fontWeight = FontWeight.Bold) }
        items(customRows, key = { it.id }) { m ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(m.title, modifier = Modifier.weight(1f))
                IconButton(onClick = { onDelete(m) }) { Icon(Icons.Default.Delete, "حذف") }
            }
        }
    }
}

internal fun priceText(row: OfferEntity): String =
    "${DecimalFormat("#,##0.##").format(row.price)} ${when(row.currency.uppercase()) {
        "EGP" -> "ج.م"; "USD" -> "دولار"; "EUR" -> "يورو"; else -> row.currency
    }} / ${row.unit}"

internal fun openWeb(context: android.content.Context, url: String) {
    if (url.startsWith("https://") || url.startsWith("http://")) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }
}
