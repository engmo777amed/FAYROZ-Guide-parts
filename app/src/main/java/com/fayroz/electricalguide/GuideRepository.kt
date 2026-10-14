package com.fayroz.electricalguide

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private fun JsonObject.text(key: String): String =
    get(key)?.takeUnless { it.isJsonNull }?.asString ?: ""
private fun JsonObject.number(key: String): Double =
    get(key)?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0

// Export schema is app-controlled JSON, not a Java-serialized database.
data class GuideBackup(
    val version: Int = 1,
    val categories: List<CategoryEntity>,
    val materials: List<MaterialEntity>,
    val brands: List<BrandEntity>,
    val offers: List<OfferEntity>,
    val photos: List<PhotoEntity>
)

class GuideRepository(private val context: Context) {
    val db: GuideDatabase = Room.databaseBuilder(
        context.applicationContext, GuideDatabase::class.java, "electrical_guide.db"
    ).build()
    val dao = db.guideDao()
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    val materials = dao.materials()
    val categories = dao.categories()
    val offers = dao.offers()
    val brands = dao.brands()
    val photos = dao.photos()

    fun darkMode(): Boolean = prefs.getBoolean("dark_mode", false)
    fun setDarkMode(value: Boolean) { prefs.edit().putBoolean("dark_mode", value).apply() }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (prefs.getBoolean("seed_complete", false)) return@withContext
        // Prevent overwrite of a pre-existing database, including after interrupted setup.
        if (dao.countMaterials() != 0) {
            prefs.edit().putBoolean("seed_complete", true).apply()
            return@withContext
        }
        val raw = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        val json = JsonParser.parseString(raw).asJsonObject
        val cats = json.getAsJsonArray("categories").map { elem ->
            val x = elem.asJsonObject
            val r = x.getAsJsonObject("reference")
            CategoryEntity(x.text("id"), x.text("title"), x.text("emoji"),
                r?.text("read_specs") ?: "", r?.text("common_check") ?: "", r?.text("glossary") ?: "")
        }
        val mats = mutableListOf<MaterialEntity>()
        val prices = mutableListOf<OfferEntity>()
        val brandRows = mutableListOf<BrandEntity>()
        val photoRows = mutableListOf<PhotoEntity>()
        for (elem in json.getAsJsonArray("items")) {
            val x = elem.asJsonObject
            val id = x.text("id")
            mats += MaterialEntity(
                id = id, category = x.text("category"), title = x.text("title"),
                en = x.text("en"), sub = x.text("sub"), description = x.text("use"),
                types = x.text("types"), choose = x.text("choose"), install = x.text("install"),
                caution = x.text("caution"), deepSpec = x.text("deep_spec"),
                deepCompare = x.text("deep_compare"), deepInstall = x.text("deep_install"),
                deepFault = x.text("deep_fault")
            )
            x.getAsJsonArray("brand_candidates")?.forEach { item ->
                val b = item.asJsonObject
                brandRows += BrandEntity(materialId = id, name = b.text("name"),
                    website = b.text("website"), evidence = b.text("evidence"))
            }
            x.getAsJsonArray("verified_offers")?.forEach { item ->
                val o = item.asJsonObject
                prices += OfferEntity(
                    materialId = id, brand = o.text("brand"), model = o.text("model"),
                    variant = o.text("variant"), price = o.number("price"),
                    currency = o.text("currency").ifBlank { "EGP" },
                    unit = o.text("unit").ifBlank { "قطعة" }, supplier = o.text("supplier"),
                    source = o.text("source"), checked = o.text("checked"),
                    kind = o.text("kind"), status = o.text("status"), taxStatus = o.text("tax_status")
                )
            }
            x.getAsJsonObject("photo")?.let { photo ->
                val file = photo.text("file")
                if (file.isNotBlank()) {
                    photoRows += PhotoEntity(materialId = id,
                        location = "https://commons.wikimedia.org/wiki/Special:FilePath/${Uri.encode(file)}?width=640",
                        caption = photo.text("caption"), credit = photo.text("author"), isPrimary = true)
                }
            }
        }
        require(mats.size == 181) { "Source library contains unexpected count: ${mats.size}" }
        require(prices.size == 403) { "Source price list changed: ${prices.size}" }
        db.withTransaction {
            dao.insertCategories(cats)
            dao.insertMaterials(mats)
            dao.insertBrands(brandRows)
            dao.insertOffers(prices)
            dao.insertPhotos(photoRows)
        }
        prefs.edit().putBoolean("seed_complete", true).apply()
    }

    suspend fun saveMaterial(row: MaterialEntity) = withContext(Dispatchers.IO) {
        dao.putMaterial(row.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteMaterial(row: MaterialEntity, pictures: List<PhotoEntity>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            dao.deletePhotosOf(row.id)
            dao.deleteOffersOf(row.id)
            dao.deleteBrandsOf(row.id)
            dao.deleteMaterial(row.id)
        }
        pictures.filter { it.materialId == row.id }.forEach { deleteLocalFile(it.location) }
    }

    private fun photosDir() = File(context.filesDir, "photos").also { it.mkdirs() }
    private fun deleteLocalFile(location: String) {
        if (location.startsWith("local:")) File(photosDir(), location.removePrefix("local:")).delete()
    }

    fun imageModel(location: String): Any {
        return if (location.startsWith("local:")) File(photosDir(), location.removePrefix("local:"))
               else location
    }

    suspend fun addPhoto(materialId: String, uri: Uri) = withContext(Dispatchers.IO) {
        val name = UUID.randomUUID().toString() + ".jpg"
        val target = File(photosDir(), name)
        try {
            context.contentResolver.openInputStream(uri)?.use { source ->
                target.outputStream().use { sink -> source.copyTo(sink) }
            } ?: error("تعذر قراءة الصورة")
            db.withTransaction {
                dao.clearPrimary(materialId)
                dao.putPhoto(PhotoEntity(materialId = materialId, location = "local:$name", isPrimary = true))
            }
        } catch (e: Exception) { target.delete(); throw e }
    }

    suspend fun selectPrimary(row: PhotoEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            dao.clearPrimary(row.materialId)
            dao.putPhoto(row.copy(isPrimary = true))
        }
    }

    suspend fun removePhoto(row: PhotoEntity) = withContext(Dispatchers.IO) {
        dao.deletePhoto(row.id)
        deleteLocalFile(row.location)
    }

    suspend fun saveOffer(row: OfferEntity) = withContext(Dispatchers.IO) { dao.putOffer(row) }
    suspend fun removeOffer(row: OfferEntity) = withContext(Dispatchers.IO) { dao.deleteOffer(row.id) }
    suspend fun saveBrand(row: BrandEntity) = withContext(Dispatchers.IO) { dao.putBrand(row) }
    suspend fun removeBrand(row: BrandEntity) = withContext(Dispatchers.IO) { dao.deleteBrand(row.id) }

    suspend fun exportBackup(destination: Uri, backup: GuideBackup) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(destination)?.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("guide.json"))
                zip.write(gson.toJson(backup).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                backup.photos.forEach { pic ->
                    if (pic.location.startsWith("local:")) {
                        val name = pic.location.removePrefix("local:")
                        if (name.matches(Regex("[A-Za-z0-9._-]+"))) {
                            val f = File(photosDir(), name)
                            if (f.isFile) {
                                zip.putNextEntry(ZipEntry("photos/$name"))
                                f.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            }
        } ?: error("تعذر فتح ملف النسخة الاحتياطية")
    }

    suspend fun restoreBackup(uri: Uri) = withContext(Dispatchers.IO) {
        val staged = File(context.cacheDir, "restore_${UUID.randomUUID()}")
        staged.mkdirs()
        try {
            var data: String? = null
            var total = 0L
            context.contentResolver.openInputStream(uri)?.use { source ->
                ZipInputStream(source).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.name == "guide.json") {
                            data = zip.readBytesLimit(12_000_000).toString(Charsets.UTF_8)
                        } else if (entry.name.startsWith("photos/")) {
                            val name = entry.name.removePrefix("photos/")
                            require(name.matches(Regex("[A-Za-z0-9._-]+"))) { "اسم ملف غير صالح" }
                            File(staged, name).outputStream().use { out ->
                                val bytes = zip.readBytesLimit(12_000_000)
                                total += bytes.size
                                require(total < 200_000_000) { "نسخة الصور كبيرة جدًا" }
                                out.write(bytes)
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: error("تعذر قراءة النسخة الاحتياطية")
            val backup = gson.fromJson(data ?: error("ملف البيانات غير موجود"), GuideBackup::class.java)
            require(backup.version == 1 && backup.materials.isNotEmpty()) { "نسخة غير متوافقة" }
            val localNames = backup.photos.filter { it.location.startsWith("local:") }
                .map { it.location.removePrefix("local:") }
            require(localNames.all { File(staged, it).isFile }) { "بعض الصور غير موجودة في النسخة" }
            // Move photos into app storage before replacing DB; replacement is a single Room transaction.
            val folder = photosDir()
            localNames.forEach { name -> File(staged, name).copyTo(File(folder, name), overwrite = true) }
            db.withTransaction {
                dao.clearPhotos(); dao.clearOffers(); dao.clearBrands(); dao.clearMaterials(); dao.clearCategories()
                dao.insertCategories(backup.categories)
                dao.insertMaterials(backup.materials)
                dao.insertBrands(backup.brands)
                dao.insertOffers(backup.offers)
                dao.insertPhotos(backup.photos)
            }
            prefs.edit().putBoolean("seed_complete", true).apply()
        } finally { staged.deleteRecursively() }
    }
}

private fun ZipInputStream.readBytesLimit(maxBytes: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val n = read(buffer)
        if (n < 0) break
        total += n
        require(total <= maxBytes) { "حجم ملف غير مسموح" }
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}
