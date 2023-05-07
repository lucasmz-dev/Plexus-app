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

package tech.techlore.plexus.repositories.database

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import tech.techlore.plexus.appmanager.ApplicationManager
import tech.techlore.plexus.dao.MainDataDao
import tech.techlore.plexus.models.main.MainData
import tech.techlore.plexus.utils.ListUtils.Companion.scannedInstalledAppsList

class MainDataRepository(private val mainDataDao: MainDataDao) {
    
    suspend fun plexusDataIntoDB(context: Context) {
        withContext(Dispatchers.IO) {
            val apiRepository = (context.applicationContext as ApplicationManager).apiRepository
            val appsCall = apiRepository.getAppsWithScores()
            val appsResponse = appsCall.awaitResponse()
            
            if (appsResponse.isSuccessful) {
                appsResponse.body()?.let { root ->
                    val requestManager = Glide.with(context)
                    for (appData in root.appData) {
    
                        appData.iconUrl?.let {
                            // Preload icon into cache
                            requestManager
                                .load(appData.iconUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache strategy
                                .preload()
                        }
    
                        // de-Googled score
                        // 1 decimal place without rounding off
                        val dgScoreString = appData.scores[1].score.toString()
                        val truncatedDgScore = dgScoreString.substring(0, dgScoreString.indexOf(".") + 2).toFloat()
    
                        // microG score
                        // 1 decimal place without rounding off
                        val mgScoreString = appData.scores[0].score.toString()
                        val truncatedMgScore = mgScoreString.substring(0, mgScoreString.indexOf(".") + 2).toFloat()
    
                        mainDataDao
                            .insertOrUpdatePlexusData(MainData(name = appData.name,
                                                               packageName = appData.packageName,
                                                               iconUrl = appData.iconUrl ?: "",
                                                               dgScore = truncatedDgScore,
                                                               totalDgRatings = appData.scores[1].totalRatings,
                                                               mgScore = truncatedMgScore,
                                                               totalMgRatings = appData.scores[0].totalRatings))
                    }
                }
            }
        }
    }
    
    suspend fun installedAppsIntoDB(context: Context) {
        withContext(Dispatchers.IO) {
        
            val installedApps = scannedInstalledAppsList(context)
            val databaseApps = mainDataDao.getInstalledApps() as ArrayList<MainData>
        
            // Find uninstalled apps
            val uninstalledApps = databaseApps.filterNot { databaseApp ->
                installedApps.any { installedApp ->
                    installedApp.packageName == databaseApp.packageName
                }
            }
    
            // Delete uninstalled apps from db
            uninstalledApps.forEach {
                if (! it.isInPlexusData) {
                    mainDataDao.delete(it)
                }
                else {
                    it.isInstalled = false
                    it.installedVersion = ""
                    it.installedFrom = ""
                    mainDataDao.update(it)
                }
            }
        
            // Insert/update new data
            installedApps.forEach {
                mainDataDao.insertOrUpdateInstalledApps(it)
            }
        }
    }
    
    suspend fun getAppByPackage(packageName: String): MainData? {
        return withContext(Dispatchers.IO){
            mainDataDao.getAppByPackage (packageName)
        }
    }
    
    suspend fun updateIsInPlexusData(mainData: MainData) {
        return withContext(Dispatchers.IO) {
            mainDataDao.updateIsInPlexusData(mainData)
        }
    }
}