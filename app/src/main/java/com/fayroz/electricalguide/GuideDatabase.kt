package com.fayroz.electricalguide

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val emoji: String,
    val readSpecs: String,
    val commonCheck: String,
    val glossary: String
)

@Entity(tableName = "materials", indices = [Index("category")])
data class MaterialEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val en: String = "",
    val sub: String = "",
    val description: String = "",
    val types: String = "",
    val choose: String = "",
    val install: String = "",
    val caution: String = "",
    val deepSpec: String = "",
    val deepCompare: String = "",
    val deepInstall: String = "",
    val deepFault: String = "",
    val favorite: Boolean = false,
    val note: String = "",
    val isCustom: Boolean = false,
    val updatedAt: Long = 0L
)

@Entity(tableName = "brands", indices = [Index("materialId")])
data class BrandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: String,
    val name: String,
    val website: String = "",
    val evidence: String = "",
    val quality: Int = 0,
    val popularity: Int = 0
)

@Entity(tableName = "offers", indices = [Index("materialId")])
data class OfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: String,
    val brand: String = "",
    val model: String = "",
    val variant: String = "",
    val price: Double = 0.0,
    val currency: String = "EGP",
    val unit: String = "قطعة",
    val supplier: String = "",
    val source: String = "",
    val checked: String = "",
    val kind: String = "",
    val status: String = "",
    val taxStatus: String = "",
    val isCustom: Boolean = false
)

@Entity(tableName = "photos", indices = [Index("materialId")])
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: String,
    val location: String, // Remote HTTPS link or local filename as local:<id>.jpg
    val caption: String = "",
    val credit: String = "",
    val isPrimary: Boolean = false
)

@Dao
interface GuideDao {
    @Query("SELECT * FROM categories ORDER BY title")
    fun categories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM materials ORDER BY title COLLATE NOCASE")
    fun materials(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM offers ORDER BY brand, model")
    fun offers(): Flow<List<OfferEntity>>

    @Query("SELECT * FROM brands ORDER BY name")
    fun brands(): Flow<List<BrandEntity>>

    @Query("SELECT * FROM photos ORDER BY isPrimary DESC, id")
    fun photos(): Flow<List<PhotoEntity>>

    @Query("SELECT COUNT(*) FROM materials")
    suspend fun countMaterials(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(rows: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(rows: List<MaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMaterial(row: MaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrands(rows: List<BrandEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putBrand(row: BrandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffers(rows: List<OfferEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putOffer(row: OfferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(rows: List<PhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPhoto(row: PhotoEntity)

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun deleteMaterial(id: String)

    @Query("DELETE FROM offers WHERE id = :id")
    suspend fun deleteOffer(id: Long)

    @Query("DELETE FROM brands WHERE id = :id")
    suspend fun deleteBrand(id: Long)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("UPDATE photos SET isPrimary = 0 WHERE materialId = :id")
    suspend fun clearPrimary(id: String)

    @Query("DELETE FROM offers WHERE materialId = :id")
    suspend fun deleteOffersOf(id: String)

    @Query("DELETE FROM brands WHERE materialId = :id")
    suspend fun deleteBrandsOf(id: String)

    @Query("DELETE FROM photos WHERE materialId = :id")
    suspend fun deletePhotosOf(id: String)

    @Query("DELETE FROM photos")
    suspend fun clearPhotos()

    @Query("DELETE FROM offers")
    suspend fun clearOffers()

    @Query("DELETE FROM brands")
    suspend fun clearBrands()

    @Query("DELETE FROM materials")
    suspend fun clearMaterials()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}

@Database(
    entities = [CategoryEntity::class, MaterialEntity::class, BrandEntity::class, OfferEntity::class, PhotoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GuideDatabase : RoomDatabase() {
    abstract fun guideDao(): GuideDao
}
