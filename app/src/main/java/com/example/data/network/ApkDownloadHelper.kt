package com.example.data.network

import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.data.model.AppUpdateConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit

object ApkDownloadHelper {

    private val driveFileRegex = Regex("/file/d/([a-zA-Z0-9_-]+)")
    private val queryIdRegex = Regex("[?&]id=([a-zA-Z0-9_-]+)")
    private val githubBlobRegex = Regex("^https?://github\\.com/([^/]+)/([^/]+)/blob/(.+)$")

    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Converts common cloud/web share links (Google Drive, Dropbox, OneDrive, GitHub blob)
     * into direct binary download URLs so clicking/downloading fetches the .apk directly.
     */
    fun toDirectDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        val noFragment = trimmed.substringBefore('#')
        val uri = runCatching { URI(noFragment) }.getOrNull() ?: return noFragment
        val host = uri.host?.lowercase(Locale.US).orEmpty()

        // 1. Google Drive / Docs -> drive.usercontent direct binary endpoint
        if (host.contains("drive.google.com") || host.contains("docs.google.com") || host.contains("drive.usercontent.google.com")) {
            val idFromPath = driveFileRegex.find(noFragment)?.groupValues?.getOrNull(1)
            val idFromQuery = queryIdRegex.find(noFragment)?.groupValues?.getOrNull(1)
            val fileId = idFromPath ?: idFromQuery
            if (!fileId.isNullOrBlank()) {
                return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
            }
        }

        // 2. Dropbox -> force dl=1
        if (host.contains("dropbox.com")) {
            return when {
                noFragment.contains("dl=0") -> noFragment.replace("dl=0", "dl=1")
                noFragment.contains("dl=1") -> noFragment
                noFragment.contains("?") -> "$noFragment&dl=1"
                else -> "$noFragment?dl=1"
            }
        }

        // 3. OneDrive -> force download=1
        if (host.contains("onedrive.live.com")) {
            return when {
                noFragment.contains("download=1") -> noFragment
                noFragment.contains("?") -> "$noFragment&download=1"
                else -> "$noFragment?download=1"
            }
        }

        // 4. GitHub blob page -> raw binary download URL (leave /releases/download/ untouched)
        githubBlobRegex.find(noFragment)?.let { match ->
            val owner = match.groupValues[1]
            val repo = match.groupValues[2]
            val tail = match.groupValues[3]
            return "https://raw.githubusercontent.com/$owner/$repo/$tail"
        }

        return noFragment
    }

    const val APP_LINK_SCHEME = "lotaya"
    const val ADMIN_PORTAL_DEEP_LINK = "lotaya://admin"
    const val CUSTOMER_SHARE_DEEP_LINK = "lotaya://share"

    sealed interface ParsedAppLink {
        data object AdminPortal : ParsedAppLink
        data class AdminInvite(val code: String) : ParsedAppLink
        data object CustomerShare : ParsedAppLink
    }

    fun buildAdminPortalLink(): String = ADMIN_PORTAL_DEEP_LINK

    fun buildAdminInviteLink(code: String): String {
        val cleanCode = code.trim().uppercase(Locale.US)
        return if (cleanCode.isBlank()) ADMIN_PORTAL_DEEP_LINK
        else "$ADMIN_PORTAL_DEEP_LINK?invite=$cleanCode"
    }

    fun buildCustomerShareLink(rawUrl: String): String {
        val direct = toDirectDownloadUrl(rawUrl.ifBlank { AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL })
        return direct.ifBlank { AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL }
    }

    fun buildApkShareText(
        appName: String,
        rawUrl: String,
        versionLabel: String = ""
    ): String {
        val cleanName = appName.trim().ifBlank { "လိုတရ" }
        val directUrl = buildCustomerShareLink(rawUrl)
        val versionPart = if (versionLabel.isNotBlank()) " ($versionLabel)" else ""
        return buildString {
            append("$cleanName APK Share Link$versionPart\n")
            append("တိုက်ရိုက်ဒေါင်းလုဒ်လင့်: $directUrl\n")
            append("App Link: $CUSTOMER_SHARE_DEEP_LINK\n")
            append("မှတ်ချက်: လင့်ကိုနှိပ်၍ APK ဖိုင်ဒေါင်းပြီးပါက ဖွင့်၍ Install လုပ်ပါ။")
        }
    }

    fun buildAdminInviteShareText(
        appName: String,
        code: String,
        label: String,
        rawApkUrl: String = AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL
    ): String {
        val cleanName = appName.trim().ifBlank { "လိုတရ" }
        val cleanCode = code.trim().uppercase(Locale.US)
        val inviteDeepLink = buildAdminInviteLink(cleanCode)
        val directApkUrl = buildCustomerShareLink(rawApkUrl)
        val labelPart = if (label.isNotBlank()) " ($label)" else ""
        return buildString {
            append("$cleanName Admin Link$labelPart\n")
            append("1-Tap Admin Link: $inviteDeepLink\n")
            append("Invite Code: $cleanCode\n")
            append("APK Download: $directApkUrl\n")
            append("မှတ်ချက်: Admin Link ကို နှိပ်၍ ဝင်ရောက်နိုင်သလို App ထဲရှိ 'Admin Key' တွင် Code ထည့်၍လည်း ဝင်နိုင်ပါသည်။")
        }
    }

    fun parseIncomingAppLink(uriString: String?): ParsedAppLink? {
        val raw = uriString?.trim().orEmpty()
        if (raw.isBlank()) return null
        val parsed = runCatching { URI(raw) }.getOrNull() ?: return null
        val scheme = parsed.scheme?.lowercase(Locale.US).orEmpty()
        if (scheme != APP_LINK_SCHEME) return null
        val host = parsed.host?.lowercase(Locale.US).orEmpty()
        return when (host) {
            "admin" -> {
                val query = parsed.rawQuery.orEmpty()
                val inviteParam = query.split("&")
                    .mapNotNull { part ->
                        val eq = part.indexOf('=')
                        if (eq <= 0) null
                        else part.substring(0, eq) to part.substring(eq + 1)
                    }
                    .firstOrNull { (k, _) -> k.equals("invite", ignoreCase = true) || k.equals("code", ignoreCase = true) }
                    ?.second
                    ?.trim()
                    ?.uppercase(Locale.US)
                    .orEmpty()
                if (inviteParam.length >= 4) ParsedAppLink.AdminInvite(inviteParam)
                else ParsedAppLink.AdminPortal
            }
            "share" -> ParsedAppLink.CustomerShare
            else -> null
        }
    }

    suspend fun downloadApkToCache(
        context: Context,
        rawUrl: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val directUrl = toDirectDownloadUrl(rawUrl)
            require(directUrl.startsWith("https://") || directUrl.startsWith("http://")) {
                "APK Download Link သည် https:// ဖြင့် စတင်ရပါမည်"
            }

            val updatesDir = File(context.cacheDir, "apk_updates").apply { mkdirs() }
            val partFile = File(updatesDir, "LoTaYa-update.apk.part")
            val finalFile = File(updatesDir, "LoTaYa-update.apk")
            if (partFile.exists()) partFile.delete()

            val request = Request.Builder()
                .url(directUrl)
                .header("User-Agent", "LoTaYa-Android/APK-Updater")
                .build()

            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = when (response.code) {
                        404 -> "APK ဖိုင်တိုက်ရိုက်မတွေ့ပါ (404 Not Found) — Link ကို စစ်ဆေးပါ သို့မဟုတ် 'APK ဖိုင်တိုက်ရိုက်ပို့မည်' ဖြင့် ရှယ်ပါ"
                        403, 401 -> "Server မှ ခွင့်ပြုချက်မပေးပါ (HTTP ${response.code})"
                        else -> "Download မအောင်မြင်ပါ (HTTP ${response.code})"
                    }
                    return@withContext Result.failure(IllegalStateException(msg))
                }

                val contentType = response.header("Content-Type")?.lowercase(Locale.US).orEmpty()
                if (contentType.contains("text/html") || contentType.contains("application/json")) {
                    return@withContext Result.failure(
                        IllegalStateException("ဤ Link သည် တိုက်ရိုက် APK ဖိုင်မဟုတ်ဘဲ Webpage (HTML) ပြနေပါသည် — Direct APK Link ပြောင်းထည့်ပါ")
                    )
                }

                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("APK ဖိုင် Data မရရှိပါ"))

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                var lastReportedPct = -1

                body.byteStream().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    val firstRead = input.read(buffer)
                    if (firstRead < 4) {
                        return@withContext Result.failure(IllegalStateException("APK ဖိုင်အရွယ်အစား မပြည့်စုံပါ"))
                    }
                    // APK files are ZIP archives starting with 'PK' (0x50, 0x4B)
                    if (buffer[0] != 0x50.toByte() || buffer[1] != 0x4B.toByte()) {
                        return@withContext Result.failure(
                            IllegalStateException("ဒေါင်းလုဒ်ရလာသောဖိုင်သည် APK ဖိုင်အစစ်မဟုတ်ပါ (HTML/Error Page ဖြစ်နေနိုင်ပါသည်)")
                        )
                    }

                    FileOutputStream(partFile).use { output ->
                        output.write(buffer, 0, firstRead)
                        downloadedBytes += firstRead
                        if (totalBytes > 0) {
                            val pct = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            lastReportedPct = pct
                            onProgress(pct)
                        }
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0) {
                                val pct = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                if (pct != lastReportedPct) {
                                    lastReportedPct = pct
                                    onProgress(pct)
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }
            onProgress(100)
            Result.success(finalFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun enqueueSystemDownload(
        context: Context,
        rawUrl: String,
        title: String = "LoTaYa Update APK"
    ): Result<Long> {
        return try {
            val directUrl = toDirectDownloadUrl(rawUrl)
            require(directUrl.startsWith("https://") || directUrl.startsWith("http://")) {
                "APK Download Link သည် https:// ဖြင့် စတင်ရပါမည်"
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return Result.failure(IllegalStateException("DownloadManager အသုံးပြု၍မရပါ"))

            val request = DownloadManager.Request(Uri.parse(directUrl))
                .setTitle(title)
                .setDescription("လိုတရ APK တိုက်ရိုက်ဒေါင်းလုဒ်")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "LoTaYa-latest.apk")

            val id = dm.enqueue(request)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches Android's PackageInstaller for [apkFile].
     * Returns true if PackageInstaller launched, false if user was directed to Unknown Sources settings.
     */
    fun installApkFile(context: Context, apkFile: File): Result<Boolean> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permIntent)
                return Result.success(false)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports the currently installed APK binary directly via FileProvider so Admins/Customers
     * can share the real .apk file over Telegram, Messenger, ShareIt, etc. even without a server URL.
     */
    suspend fun shareInstalledApkFile(
        context: Context,
        appName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sourcePath = context.applicationInfo.publicSourceDir ?: context.applicationInfo.sourceDir
            val sourceFile = File(sourcePath)
            require(sourceFile.exists() && sourceFile.canRead()) {
                "လက်ရှိ Install လုပ်ထားသော APK ဖိုင်ကို ဖတ်၍မရပါ"
            }

            val shareDir = File(context.cacheDir, "shared_apk").apply { mkdirs() }
            val destFile = File(shareDir, "LoTaYa.apk")
            sourceFile.copyTo(destFile, overwrite = true)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$appName APK")
                putExtra(Intent.EXTRA_TEXT, "$appName Android APK ဖိုင် (Install လုပ်၍ တိုက်ရိုက်အသုံးပြုနိုင်ပါသည်)")
                clipData = ClipData.newUri(context.contentResolver, "$appName APK", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "$appName APK ဖိုင်တိုက်ရိုက်ပို့မည်").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(chooser)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
