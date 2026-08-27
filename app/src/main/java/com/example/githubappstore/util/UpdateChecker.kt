package com.example.githubappstore.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.example.githubappstore.data.fdroid.FdroidIndexRepository
import com.example.githubappstore.data.model.GhRelease
import com.example.githubappstore.domain.AppItem

/** Installed app metadata for update comparison. */
data class InstalledAppInfo(
    val packageName: String, val appName: String,
    val versionName: String?, val versionCode: Long, val sourceDir: String
)

/**
 * Candidate app that is both installed locally and tracked as a GitHub open-source
 * Android project. [hasUpdate] uses [VersionComparator] (semver4j) for robust
 * comparison of the latest release tag against the installed version.
 */
data class UpdateCandidate(
    val installed: InstalledAppInfo, val appItem: AppItem,
    val latestRelease: GhRelease?, val latestVersionTag: String?
) {
    val hasUpdate: Boolean
        get() = VersionComparator.isUpdateAvailable(latestVersionTag, installed.versionName)
    val diff: VersionComparator.UpdateDiff
        get() = VersionComparator.diff(latestVersionTag, installed.versionName)
}

/** Lists installed apps present in [knownPackages] (package -> owner/repo). */
fun listInstalledGithubApps(context: Context, knownPackages: Map<String, String>): List<InstalledAppInfo> {
    val pm = context.packageManager
    return knownPackages.keys.mapNotNull { pkg ->
        runCatching {
            val pi: PackageInfo = pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
            InstalledAppInfo(
                pkg,
                pi.applicationInfo?.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg },
                pi.versionName, pi.longVersionCode,
                pi.applicationInfo?.sourceDir.orEmpty()
            )
        }.getOrNull()
    }
}

/**
 * Returns the effective package->repo map for update detection: the curated
 * built-in table merged with dynamically discovered GitHub repos from the
 * F-Droid index (when available). F-Droid entries win on conflict because they
 * are package-authoritative.
 */
suspend fun effectiveGithubPackages(context: Context): Map<String, String> {
    val froid = runCatching { FdroidIndexRepository(context).githubPackages() }.getOrDefault(emptyMap())
    // F-Droid entries win on conflict because they are package-authoritative.
    return LinkedHashMap(KnownGithubAndroidApps.PACKAGE_TO_REPO).apply { putAll(froid) }
}

/** Curated map of well-known open-source Android apps (package -> owner/repo). */
object KnownGithubAndroidApps {
    val PACKAGE_TO_REPO: Map<String, String> = linkedMapOf(
        "com.aurora.store" to "aurora-store/AuroraStore",
        "org.fossify.filemanager" to "FossifyOrg/File-Manager",
        "org.fossify.gallery" to "FossifyOrg/Gallery",
        "org.fossify.calendar" to "FossifyOrg/Calendar",
        "org.fossify.contacts" to "FossifyOrg/Contacts",
        "org.fossify.clock" to "FossifyOrg/Clock",
        "org.fossify.notes" to "FossifyOrg/Notes",
        "org.fossify.camera" to "FossifyOrg/Camera",
        "org.fossify.messages" to "FossifyOrg/Messages",
        "org.fossify.phone" to "FossifyOrg/Phone",
        "org.fossify.recorder" to "FossifyOrg/Recorder",
        "org.fossify.voicerecorder" to "FossifyOrg/Voice-Recorder",
        "org.schabi.newpipe" to "TeamNewPipe/NewPipe",
        "org.schabi.newpipelegacy" to "TeamNewPipe/NewPipe-Legacy",
        "com.github.catfriend1.syncthingandroid" to "Catfriend1/syncthing-android",
        "com.nutomic.syncthingandroid" to "syncthing/syncthing-android",
        "net.osmand" to "osmandapp/OsmAnd",
        "net.osmand.plus" to "osmandapp/OsmAnd",
        "com.nextcloud.client" to "nextcloud/android",
        "com.nextcloud.talk2" to "nextcloud/talk-android",
        "org.mozilla.fenix" to "mozilla-mobile/fenix",
        "org.mozilla.focus" to "mozilla-mobile/focus-android",
        "com.brave.browser" to "brave/brave-browser",
        "com.cromite.cromite" to "uazo/cromite",
        "com.duckduckgo.mobile.android" to "duckduckgo/Android",
        "com.kunzisoft.keepass.free" to "Kunzisoft/KeePassDX",
        "com.kunzisoft.keepass.libre" to "Kunzisoft/KeePassDX",
        "org.wikipedia" to "wikipedia/org.wikipedia",
        "com.secuso.privacyfriendlypasswordgenerator" to "SECUSO-Research/PasswordGenerator",
        "com.secuso.privacyfriendlytodolist" to "SECUSO-Research/ToDoList",
        "com.secuso.privacyfriendlynotes" to "SECUSO-Research/Notes",
        "com.secuso.privacyfriendlyweather" to "SECUSO-Research/Weather",
        "org.torproject.android" to "guardianproject/orbot-android",
        "org.thoughtcrime.securesms" to "signalapp/Signal-Android",
        "org.fdroid.fdroid" to "F-Droid/F-Droid"
    )
}
