@file:Suppress("DEPRECATION")

package com.saku.v2.data.sync

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.saku.v2.data.DriveSyncContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Synchronizes `saku_v2_sync.zip` with the user's hidden Google Drive `appDataFolder`
 * using Google Sign-In (`play-services-auth`) and Drive REST API v3 over OkHttp.
 */
class DriveSyncManager(
    context: Context,
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DriveSyncContract {

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val OAUTH2_TOKEN_SCOPE = "oauth2:$DRIVE_APPDATA_SCOPE"
        const val SYNC_FILE_NAME = "saku_v2_sync.zip"
        private const val FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_ENDPOINT = "https://www.googleapis.com/upload/drive/v3/files"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()
        private val ZIP_MEDIA_TYPE = "application/zip".toMediaType()
        private val MULTIPART_RELATED_TYPE = "multipart/related".toMediaType()

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .build()
        }
    }

    @Serializable
    private data class DriveFileListResponse(
        val files: List<DriveFileItem> = emptyList()
    )

    @Serializable
    private data class DriveFileItem(
        val id: String,
        val name: String = "",
        val modifiedTime: String? = null
    )

    @Serializable
    private data class DriveFileCreateMetadata(
        val name: String,
        val parents: List<String>
    )

    @Serializable
    private data class DriveFileUpdateMetadata(
        val name: String
    )

    private class UnauthorizedTokenException : IOException("Token akses Google Drive kedaluwarsa")

    private val appContext: Context = context.applicationContext ?: context
    private val driveScope = Scope(DRIVE_APPDATA_SCOPE)

    private val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
        GoogleSignIn.getClient(appContext, options)
    }

    private val _signedInEmail = MutableStateFlow(readValidSignedInAccount()?.email)
    override val signedInEmail: StateFlow<String?> = _signedInEmail.asStateFlow()

    /**
     * Creates the Google Sign-In intent requesting email + `drive.appdata` permission.
     */
    fun createSignInIntent(): Intent = signInClient.signInIntent

    /**
     * Parses the activity result returned from [createSignInIntent] and updates [signedInEmail].
     */
    fun handleSignInResult(data: Intent?): Result<String> {
        if (data == null) {
            return Result.failure(IllegalStateException("Masuk ke Google dibatalkan."))
        }
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val email = account.email
            when {
                email.isNullOrBlank() -> Result.failure(
                    IllegalStateException("Akun Google tidak menyertakan alamat email.")
                )
                !GoogleSignIn.hasPermissions(account, driveScope) -> Result.failure(
                    IllegalStateException(
                        "Izin penyimpanan Google Drive appData belum disetujui. Silakan masuk ulang dan centang izin akses."
                    )
                )
                else -> {
                    _signedInEmail.value = email
                    Result.success(email)
                }
            }
        } catch (e: ApiException) {
            Result.failure(IllegalStateException(mapSignInError(e.statusCode), e))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun getConnectedAccountEmail(): String? {
        return withContext(ioDispatcher) {
            val email = readValidSignedInAccount()?.email
            _signedInEmail.value = email
            email
        }
    }

    override suspend fun uploadZip(zipFile: File): Result<Long> = withContext(ioDispatcher) {
        if (!zipFile.exists() || !zipFile.isFile) {
            return@withContext Result.failure(
                FileNotFoundException("File arsip backup tidak ditemukan untuk diunggah.")
            )
        }

        executeAuthenticated { accessToken ->
            val existing = findRemoteSyncFile(accessToken)
            val metadataJson = if (existing == null) {
                json.encodeToString(
                    DriveFileCreateMetadata.serializer(),
                    DriveFileCreateMetadata(
                        name = SYNC_FILE_NAME,
                        parents = listOf("appDataFolder")
                    )
                )
            } else {
                json.encodeToString(
                    DriveFileUpdateMetadata.serializer(),
                    DriveFileUpdateMetadata(name = SYNC_FILE_NAME)
                )
            }

            val targetUrl = if (existing == null) {
                UPLOAD_ENDPOINT.toHttpUrl().newBuilder()
                    .addQueryParameter("uploadType", "multipart")
                    .addQueryParameter("fields", "id, name, modifiedTime")
                    .build()
            } else {
                "$UPLOAD_ENDPOINT/${existing.id}".toHttpUrl().newBuilder()
                    .addQueryParameter("uploadType", "multipart")
                    .addQueryParameter("fields", "id, name, modifiedTime")
                    .build()
            }

            val multipartBody = MultipartBody.Builder()
                .setType(MULTIPART_RELATED_TYPE)
                .addPart(metadataJson.toRequestBody(JSON_MEDIA_TYPE))
                .addPart(zipFile.asRequestBody(ZIP_MEDIA_TYPE))
                .build()

            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .header("Authorization", "Bearer $accessToken")

            val request = if (existing == null) {
                requestBuilder.post(multipartBody).build()
            } else {
                requestBuilder.patch(multipartBody).build()
            }

            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body.string()
                if (response.code == 401) {
                    throw UnauthorizedTokenException()
                }
                if (!response.isSuccessful) {
                    throw IOException(mapHttpError(response.code, rawBody))
                }
                val uploaded = json.decodeFromString(DriveFileItem.serializer(), rawBody)
                parseModifiedEpochMs(uploaded.modifiedTime) ?: System.currentTimeMillis()
            }
        }
    }

    override suspend fun downloadZip(destFile: File): Result<Unit> = withContext(ioDispatcher) {
        executeAuthenticated { accessToken ->
            val remoteFile = findRemoteSyncFile(accessToken)
                ?: throw FileNotFoundException(
                    "Belum ada cadangan Saku ($SYNC_FILE_NAME) di Google Drive Anda."
                )

            val downloadUrl = "$FILES_ENDPOINT/${remoteFile.id}".toHttpUrl().newBuilder()
                .addQueryParameter("alt", "media")
                .build()

            val request = Request.Builder()
                .url(downloadUrl)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val parent = destFile.absoluteFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val tempDownload = File(
                parent ?: appContext.cacheDir,
                "${destFile.name}.dl.tmp"
            )

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 401) {
                        throw UnauthorizedTokenException()
                    }
                    if (!response.isSuccessful) {
                        val err = response.body.string()
                        throw IOException(mapHttpError(response.code, err))
                    }
                    FileOutputStream(tempDownload).use { out ->
                        response.body.byteStream().use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                    }
                }
                if (destFile.exists()) {
                    destFile.delete()
                }
                if (!tempDownload.renameTo(destFile)) {
                    tempDownload.copyTo(destFile, overwrite = true)
                    tempDownload.delete()
                }
            } catch (t: Throwable) {
                tempDownload.delete()
                throw t
            }
        }
    }

    override suspend fun fetchRemoteModifiedTime(): Result<Long?> = withContext(ioDispatcher) {
        executeAuthenticated { accessToken ->
            val remoteFile = findRemoteSyncFile(accessToken)
            parseModifiedEpochMs(remoteFile?.modifiedTime)
        }
    }

    override suspend fun signOut() {
        withContext(ioDispatcher) {
            runCatching { signInClient.signOut().awaitCompletion() }
            _signedInEmail.value = null
        }
    }

    private fun readValidSignedInAccount(): GoogleSignInAccount? {
        val account = runCatching { GoogleSignIn.getLastSignedInAccount(appContext) }.getOrNull()
            ?: return null
        val hasDriveScope = runCatching {
            GoogleSignIn.hasPermissions(account, driveScope)
        }.getOrDefault(false)
        return if (hasDriveScope) account else null
    }

    private suspend fun <T> executeAuthenticated(block: (accessToken: String) -> T): Result<T> {
        return try {
            ensurePlayServicesAvailable()
            val androidAccount = resolveAndroidAccount()

            var token = acquireAccessToken(androidAccount)
            try {
                Result.success(block(token))
            } catch (_: UnauthorizedTokenException) {
                runCatching { GoogleAuthUtil.clearToken(appContext, token) }
                token = acquireAccessToken(androidAccount)
                Result.success(block(token))
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun ensurePlayServicesAvailable() {
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext)
        if (status != ConnectionResult.SUCCESS) {
            throw IllegalStateException(
                "Google Play Services tidak tersedia atau perlu diperbarui di perangkat ini."
            )
        }
    }

    private fun resolveAndroidAccount(): Account {
        val signInAccount = GoogleSignIn.getLastSignedInAccount(appContext)
            ?: throw IllegalStateException(
                "Akun Google belum terhubung. Silakan hubungkan akun terlebih dahulu di Pengaturan."
            )
        if (!GoogleSignIn.hasPermissions(signInAccount, driveScope)) {
            throw IllegalStateException(
                "Izin Google Drive appData belum diberikan. Silakan hubungkan ulang akun Google Anda."
            )
        }
        return signInAccount.account
            ?: signInAccount.email?.takeIf { it.isNotBlank() }?.let { Account(it, "com.google") }
            ?: throw IllegalStateException(
                "Informasi akun Google tidak lengkap. Silakan masuk ulang."
            )
    }

    private fun acquireAccessToken(account: Account): String {
        return try {
            GoogleAuthUtil.getToken(appContext, account, OAUTH2_TOKEN_SCOPE)
        } catch (e: UserRecoverableAuthException) {
            throw IllegalStateException(
                "Persetujuan akses Google Drive diperlukan. Hubungkan ulang akun Google Anda.",
                e
            )
        } catch (e: GoogleAuthException) {
            throw IllegalStateException(
                "Autentikasi Google Drive gagal. Pastikan OAuth Client ID dan SHA-1 aplikasi sudah dikonfigurasi.",
                e
            )
        } catch (e: IOException) {
            throw IOException("Gagal menghubungi layanan Google. Periksa koneksi internet Anda.", e)
        }
    }

    private fun findRemoteSyncFile(accessToken: String): DriveFileItem? {
        val listUrl = FILES_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name = '$SYNC_FILE_NAME' and trashed = false")
            .addQueryParameter("fields", "files(id, name, modifiedTime)")
            .addQueryParameter("orderBy", "modifiedTime desc")
            .addQueryParameter("pageSize", "5")
            .build()

        val request = Request.Builder()
            .url(listUrl)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val rawBody = response.body.string()
            if (response.code == 401) {
                throw UnauthorizedTokenException()
            }
            if (!response.isSuccessful) {
                throw IOException(mapHttpError(response.code, rawBody))
            }
            val listResponse = json.decodeFromString(DriveFileListResponse.serializer(), rawBody)
            listResponse.files.firstOrNull()
        }
    }

    private fun parseModifiedEpochMs(modifiedTime: String?): Long? {
        if (modifiedTime.isNullOrBlank()) return null
        return runCatching { Instant.parse(modifiedTime).toEpochMilli() }.getOrNull()
    }

    private fun mapSignInError(statusCode: Int): String {
        return when (statusCode) {
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                "Proses masuk Google dibatalkan."
            CommonStatusCodes.DEVELOPER_ERROR,
            GoogleSignInStatusCodes.SIGN_IN_FAILED ->
                "OAuth Client ID Google Drive belum terkonfigurasi untuk package com.saku.v2 (kode $statusCode). Buka Panduan Setup OAuth di Pengaturan."
            CommonStatusCodes.NETWORK_ERROR ->
                "Gagal terhubung ke jaringan saat masuk ke akun Google."
            else ->
                "Gagal masuk ke akun Google (kode $statusCode)."
        }
    }

    private fun mapHttpError(statusCode: Int, responseBody: String): String {
        return when (statusCode) {
            401 -> "Sesi Google Drive kedaluwarsa (401). Silakan hubungkan ulang akun Google Anda."
            403 -> "Akses Google Drive ditolak (403). Periksa izin Drive API & OAuth Client ID Anda."
            404 -> "File sinkronisasi tidak ditemukan di Google Drive (404)."
            429 -> "Terlalu banyak permintaan ke Google Drive (429). Coba beberapa saat lagi."
            in 500..599 -> "Server Google Drive sedang bermasalah ($statusCode). Coba lagi nanti."
            else -> "Kesalahan Google Drive ($statusCode): ${responseBody.take(160)}"
        }
    }

    private suspend fun Task<Void>.awaitCompletion() {
        suspendCancellableCoroutine<Unit> { cont ->
            addOnCompleteListener(Executor { it.run() }) { task ->
                when {
                    task.isCanceled -> cont.cancel()
                    task.exception != null -> cont.resumeWithException(task.exception!!)
                    else -> cont.resume(Unit)
                }
            }
        }
    }
}
