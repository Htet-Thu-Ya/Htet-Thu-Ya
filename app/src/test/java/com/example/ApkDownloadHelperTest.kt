package com.example

import com.example.data.network.ApkDownloadHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkDownloadHelperTest {

    @Test
    fun googleDriveShareLink_convertsToDirectDownload() {
        val raw = "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz/view?usp=sharing"
        val expected = "https://drive.usercontent.google.com/download?id=1AbCdEfGhIjKlMnOpQrStUvWxYz&export=download&confirm=t"
        assertEquals(expected, ApkDownloadHelper.toDirectDownloadUrl(raw))
    }

    @Test
    fun googleDriveOpenQueryLink_convertsToDirectDownload() {
        val raw = "https://drive.google.com/open?id=98765_abc-XYZ"
        val expected = "https://drive.usercontent.google.com/download?id=98765_abc-XYZ&export=download&confirm=t"
        assertEquals(expected, ApkDownloadHelper.toDirectDownloadUrl(raw))
    }

    @Test
    fun dropboxLinks_forceDl1() {
        assertEquals(
            "https://www.dropbox.com/scl/fi/abc123/LoTaYa.apk?rlkey=xyz&dl=1",
            ApkDownloadHelper.toDirectDownloadUrl("https://www.dropbox.com/scl/fi/abc123/LoTaYa.apk?rlkey=xyz&dl=0")
        )
        assertEquals(
            "https://www.dropbox.com/s/abc123/LoTaYa.apk?dl=1",
            ApkDownloadHelper.toDirectDownloadUrl("https://www.dropbox.com/s/abc123/LoTaYa.apk")
        )
    }

    @Test
    fun onedriveLink_appendsDownload1() {
        assertEquals(
            "https://onedrive.live.com/embed?cid=123&resid=456&download=1",
            ApkDownloadHelper.toDirectDownloadUrl("https://onedrive.live.com/embed?cid=123&resid=456")
        )
    }

    @Test
    fun githubBlobLink_convertsToRaw_andReleaseApkPreserved() {
        assertEquals(
            "https://raw.githubusercontent.com/user/repo/main/releases/LoTaYa.apk",
            ApkDownloadHelper.toDirectDownloadUrl("https://github.com/user/repo/blob/main/releases/LoTaYa.apk")
        )
        val releaseUrl = "https://github.com/Htet-Thu-Ya/Htet-Thu-Ya/releases/download/latest/LoTaYa.apk"
        assertEquals(releaseUrl, ApkDownloadHelper.toDirectDownloadUrl(releaseUrl))
    }

    @Test
    fun buildApkShareText_normalizesUrlAndIncludesVersion() {
        val text = ApkDownloadHelper.buildApkShareText(
            appName = "လိုတရ",
            rawUrl = "https://drive.google.com/file/d/FILE_ID_123/view?usp=sharing",
            versionLabel = "v2.5"
        )
        assertTrue(text.contains("လိုတရ"))
        assertTrue(text.contains("(v2.5)"))
        assertTrue(text.contains("https://drive.usercontent.google.com/download?id=FILE_ID_123&export=download&confirm=t"))
    }
}
