/*
 * Copyright (c) 2022-present Techlore
 *
 *  This file is part of Plexus.
 *
 *  Plexus is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plexus is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Plexus.  If not, see <https://www.gnu.org/licenses/>.
 */

package tech.techlore.plexus.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import tech.techlore.plexus.models.main.MainData

@Dao
interface MainDataDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(mainData: MainData)
    
    @Update
    suspend fun update(mainData: MainData)
    
    @Delete
    suspend fun delete(mainData: MainData)
    
    @Transaction
    suspend fun insertOrUpdatePlexusData(mainData: MainData) {
        
        val existingData = getAppByPackage(mainData.packageName)
        
        if (existingData == null) {
            insert(mainData)
        }
        else {
            existingData.name = mainData.name
            existingData.packageName = mainData.packageName
            existingData.dgScore.dgScore = mainData.dgScore.dgScore
            existingData.mgScore.mgScore = mainData.mgScore.mgScore
            existingData.ratingsList = mainData.ratingsList
            update(existingData)
        }
    }
    
    @Transaction
    suspend fun insertOrUpdateInstalledApps(mainData: MainData) {
        
        val existingApp = getAppByPackage(mainData.packageName)
        
        if (existingApp == null) {
            mainData.isInPlexusData = false
            insert(mainData)
        }
        else {
            existingApp.installedVersion = mainData.installedVersion
            existingApp.installedBuild = mainData.installedBuild
            existingApp.isInstalled = mainData.isInstalled
            existingApp.installedFrom = mainData.installedFrom
            update(existingApp)
        }
    }
    
    @Transaction
    suspend fun updateFavApps(mainData: MainData) {
        
        val existingApps = getAppByPackage(mainData.packageName)
        
        if (existingApps != null) {
            existingApps.isFav = mainData.isFav
            update(existingApps)
        }
    }
    
    @Query("SELECT * FROM main_table WHERE packageName = :packageName")
    fun getAppByPackage(packageName: String): MainData?
    
    @Query("SELECT * FROM main_table WHERE packageName = :packageName AND NOT isInstalled")
    fun getNotInstalledAppByPackage(packageName: String): MainData?
    
    @Query("SELECT * FROM main_table WHERE packageName = :packageName AND isInstalled")
    fun getInstalledAppByPackage(packageName: String): MainData?
    
    @Query("SELECT * FROM main_table WHERE packageName = :packageName AND isFav")
    fun getFavoriteAppByPackage(packageName: String): MainData?
    
    @Query("SELECT * FROM main_table WHERE NOT isInstalled")
    suspend fun getNotInstalledApps(): List<MainData>
    
    @Query("SELECT * FROM main_table WHERE isInstalled")
    suspend fun getInstalledApps(): List<MainData>
    
    @Query("SELECT * FROM main_table WHERE isFav")
    suspend fun getFavApps(): List<MainData>
    
    @Query("""
        SELECT * FROM main_table
        WHERE NOT isInstalled
        AND ((dgScore >= :dgScoreFrom AND dgScore <= :dgScoreTo) OR (:dgScoreFrom = -1 AND :dgScoreTo = -1))
        AND ((mgScore >= :mgScoreFrom AND mgScore <= :mgScoreTo) OR (:mgScoreFrom = -1 AND :mgScoreTo = -1))
        ORDER BY
        CASE WHEN :isAsc = 1 THEN name END ASC,
        CASE WHEN :isAsc = 0 THEN name END DESC
    """)
    suspend fun getSortedNotInstalledApps(dgScoreFrom: Float,
                                          dgScoreTo: Float,
                                          mgScoreFrom: Float,
                                          mgScoreTo: Float,
                                          isAsc: Boolean): List<MainData>
    // -1 is for ignoring the score when required,
    // so it doesn't include it while filtering
    
    @Query("""
        SELECT * FROM main_table
        WHERE isInstalled
        AND (installedFrom = :installedFrom OR :installedFrom = '')
        AND ((dgScore >= :dgScoreFrom AND dgScore <= :dgScoreTo) OR (:dgScoreFrom = -1 AND :dgScoreTo = -1))
        AND ((mgScore >= :mgScoreFrom AND mgScore <= :mgScoreTo) OR (:mgScoreFrom = -1 AND :mgScoreTo = -1))
        ORDER BY
        CASE WHEN :isAsc = 1 THEN name END ASC,
        CASE WHEN :isAsc = 0 THEN name END DESC
    """)
    suspend fun getSortedInstalledApps(installedFrom: String,
                                       dgScoreFrom: Float,
                                       dgScoreTo: Float,
                                       mgScoreFrom: Float,
                                       mgScoreTo: Float,
                                       isAsc: Boolean): List<MainData>
    // -1 is for ignoring the score when required,
    // so it doesn't include it while filtering
    
    /*@Query("SELECT * FROM main_table")
    suspend fun getAll(): List<PlexusData>*/
    
    /*@Query("SELECT * FROM main_table WHERE installedFrom = :installedFrom")
    fun getInstalledAppsByInstaller(installers: List<String>): List<MainData>
    
    @Query("SELECT * FROM main_table WHERE dgStatus = :dgStatus")
    fun getInstalledAppsByDGRating(): List<MainData>
    
    @Query("SELECT * FROM main_table ORDER BY name DESC")
    fun getInstalledAppsByMGRating(): List<MainData>*/
}