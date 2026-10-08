package com.example.asitjournal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface InjectionDao {
    @Query("SELECT * FROM injections ORDER BY plannedDate ASC")
    suspend fun getAll(): List<Injection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Injection>)

    @Update
    suspend fun update(injection: Injection)

    @Query("DELETE FROM injections")
    suspend fun deleteAll()
}

@Dao
interface VialSettingsDao {
    @Query("SELECT * FROM vial_settings")
    suspend fun getAll(): List<VialSettings>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<VialSettings>)
}

@Database(entities = [Injection::class, VialSettings::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun injectionDao(): InjectionDao
    abstract fun vialSettingsDao(): VialSettingsDao
}