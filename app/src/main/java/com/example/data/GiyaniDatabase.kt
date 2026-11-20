package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tab_groups")
data class TabGroupEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val colorHex: Long,
    val isExpanded: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "browser_tabs")
data class BrowserTabEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val groupId: String?, // null = Ungrouped
    val title: String,
    val url: String,
    val isPrivate: Boolean = false,
    val isActive: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val localPath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String, // QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELED
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = -1L,
    val supportsRange: Boolean = true,
    val eTag: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "browser_profiles")
data class BrowserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String = "",
    val countryCode: String,
    val cityName: String,
    val timezoneId: String,
    val languageTag: String,
    val devicePresetId: String,
    val preferredVpnServerId: String?,
    val blockThirdPartyCookies: Boolean = true,
    val blockTrackers: Boolean = true,
    val canvasProtection: Boolean = true,
    val webRtcProtection: Boolean = true,
    val geolocationPolicy: String = "ASK", // ASK, SIMULATED_TESTING, BLOCKED
    val simulatedLat: Double = 40.7128,
    val simulatedLon: Double = -74.0060,
    val keepLoginSession: Boolean = true, // Preserve cookies, localStorage & WebView session when switching profiles
    val isActive: Boolean = false, // Currently opened alongside other active profiles in the same Country/VPN
    val isFocused: Boolean = false // Currently focused profile in the main Chrome viewport
)

@Entity(tableName = "vpn_endpoints")
data class VpnEndpointEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val cityName: String,
    val endpointHost: String,
    val endpointPort: Int,
    val tunnelAddressIpv4: String,
    val tunnelAddressIpv6: String?,
    val dnsServers: String,
    val publicKey: String,
    val encryptedPrivateKey: String, // Encrypted via Android Keystore AES-GCM
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val persistentKeepalive: Int = 25,
    val browserOnlyRouting: Boolean = true,
    val failClosedKillSwitch: Boolean = true
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history_items")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Dao
interface GiyaniDao {
    // Tab Groups
    @Query("SELECT * FROM tab_groups WHERE profileId = :profileId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeTabGroups(profileId: String): Flow<List<TabGroupEntity>>

    @Query("SELECT * FROM tab_groups ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAllTabGroups(): Flow<List<TabGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabGroup(group: TabGroupEntity)

    @Update
    suspend fun updateTabGroup(group: TabGroupEntity)

    @Query("DELETE FROM tab_groups WHERE id = :groupId")
    suspend fun deleteTabGroup(groupId: String)

    @Query("UPDATE browser_tabs SET groupId = NULL WHERE groupId = :groupId")
    suspend fun ungroupTabsInGroup(groupId: String)

    @Query("DELETE FROM browser_tabs WHERE groupId = :groupId")
    suspend fun deleteTabsInGroup(groupId: String)

    // Browser Tabs
    @Query("SELECT * FROM browser_tabs WHERE profileId = :profileId ORDER BY lastAccessed DESC")
    fun observeTabsForProfile(profileId: String): Flow<List<BrowserTabEntity>>

    @Query("SELECT * FROM browser_tabs WHERE profileId = :profileId ORDER BY lastAccessed DESC")
    suspend fun getTabsForProfileSnapshot(profileId: String): List<BrowserTabEntity>

    @Query("SELECT * FROM browser_tabs ORDER BY lastAccessed ASC")
    fun observeAllTabs(): Flow<List<BrowserTabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: BrowserTabEntity)

    @Update
    suspend fun updateTab(tab: BrowserTabEntity)

    @Query("UPDATE browser_tabs SET groupId = :targetGroupId WHERE id = :tabId")
    suspend fun moveTabToGroup(tabId: String, targetGroupId: String?)

    @Query("UPDATE browser_tabs SET isActive = (id = :activeTabId) WHERE profileId = :profileId")
    suspend fun setActiveTab(profileId: String, activeTabId: String)

    @Query("DELETE FROM browser_tabs WHERE id = :tabId")
    suspend fun deleteTab(tabId: String)

    // Downloads
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)

    // Profiles
    @Query("SELECT * FROM browser_profiles ORDER BY isFocused DESC, isActive DESC, name ASC")
    fun observeProfiles(): Flow<List<BrowserProfileEntity>>

    @Query("SELECT * FROM browser_profiles")
    suspend fun getAllProfilesSnapshot(): List<BrowserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BrowserProfileEntity)

    @Update
    suspend fun updateProfile(profile: BrowserProfileEntity)

    @Query("UPDATE browser_profiles SET isFocused = (id = :profileId)")
    suspend fun setFocusedProfile(profileId: String)

    @Query("UPDATE browser_profiles SET isActive = :active WHERE id = :profileId")
    suspend fun setProfileActiveState(profileId: String, active: Boolean)

    @Query("UPDATE browser_profiles SET keepLoginSession = :keepLogin WHERE id = :profileId")
    suspend fun setProfileKeepLoginSession(profileId: String, keepLogin: Boolean)

    @Query("UPDATE browser_profiles SET isActive = 0, isFocused = 0 WHERE countryCode != :allowedCountryCode")
    suspend fun deactivateProfilesOutsideCountry(allowedCountryCode: String)

    @Query("DELETE FROM browser_profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: String)

    // VPN Endpoints
    @Query("SELECT * FROM vpn_endpoints ORDER BY countryCode ASC, cityName ASC")
    fun observeVpnEndpoints(): Flow<List<VpnEndpointEntity>>

    @Query("SELECT * FROM vpn_endpoints")
    suspend fun getAllVpnEndpointsSnapshot(): List<VpnEndpointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVpnEndpoint(endpoint: VpnEndpointEntity)

    @Update
    suspend fun updateVpnEndpoint(endpoint: VpnEndpointEntity)

    @Query("DELETE FROM vpn_endpoints WHERE id = :id")
    suspend fun deleteVpnEndpoint(id: String)

    @Query("DELETE FROM vpn_endpoints")
    suspend fun deleteAllVpnEndpoints()

    // Bookmarks & History
    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeBookmarks(profileId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)

    @Query("SELECT * FROM history_items WHERE profileId = :profileId ORDER BY visitedAt DESC LIMIT 100")
    fun observeHistory(profileId: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_items ORDER BY visitedAt DESC LIMIT 100")
    fun observeAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity)

    @Query("DELETE FROM history_items WHERE profileId = :profileId")
    suspend fun clearHistoryForProfile(profileId: String)
}

@Database(
    entities = [
        TabGroupEntity::class,
        BrowserTabEntity::class,
        DownloadEntity::class,
        BrowserProfileEntity::class,
        VpnEndpointEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GiyaniDatabase : RoomDatabase() {
    abstract fun giyaniDao(): GiyaniDao

    companion object {
        @Volatile
        private var INSTANCE: GiyaniDatabase? = null

        fun getInstance(context: Context): GiyaniDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GiyaniDatabase::class.java,
                    "giyani_browser.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
