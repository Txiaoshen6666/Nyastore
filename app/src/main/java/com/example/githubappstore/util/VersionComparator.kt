package com.example.githubappstore.util

import org.semver4j.Semver
import org.semver4j.Semver.VersionDiff as SemverDiff

/**
 * Version comparison backed by [semver4j]. Normalises common Android/GitHub tag
 * shapes (`v1.2.3`, `1.2.3-beta.1`, `2024.12.1`, `1.2`) into Semver objects and
 * compares precedence. Falls back to string inequality when neither side parses,
 * so non-semver tags still produce a deterministic (if approximate) result.
 */
object VersionComparator {
    private fun String.toSemver(): Semver? {
        val cleaned = this.trim().removePrefix("v").trim()
        if (cleaned.isEmpty()) return null
        return Semver.parse(cleaned)
    }

    /**
     * Returns true when [latestTag] (a GitHub release tag, e.g. `v1.10.0`) is
     * strictly newer than the version installed on device. [installedVersion] is
     * typically `PackageInfo.versionName` (free-form, may be null/empty).
     */
    fun isUpdateAvailable(latestTag: String?, installedVersion: String?): Boolean {
        val tag = latestTag?.trim()?.lowercase() ?: return false
        val installed = installedVersion?.trim()?.lowercase().orEmpty()
        if (installed.isEmpty()) return true // unknown installed -> assume update available
        val tagSem = tag.toSemver()
        val insSem = installed.toSemver()
        return when {
            tagSem != null && insSem != null -> tagSem.isGreaterThan(insSem)
            tagSem != null && insSem == null -> {
                // Installed not semver (e.g. "r2024"); treat tag as newer only if clearly distinct.
                tag != installed
            }
            else -> tag != installed
        }
    }

    /** Convenience: diff category between latest tag and installed version. */
    fun diff(latestTag: String?, installedVersion: String?): VersionDiff {
        val tag = latestTag?.trim()?.removePrefix("v")?.trim().orEmpty()
        val installed = installedVersion?.trim()?.removePrefix("v")?.trim().orEmpty()
        if (tag.isEmpty() || installed.isEmpty()) return VersionDiff.Unknown
        val t = Semver.parse(tag) ?: return VersionDiff.Unknown
        val i = Semver.parse(installed) ?: return VersionDiff.Unknown
        return when (t.diff(i)) {
            SemverDiff.MAJOR -> VersionDiff.Major
            SemverDiff.MINOR -> VersionDiff.Minor
            SemverDiff.PATCH -> VersionDiff.Patch
            SemverDiff.PRE_RELEASE -> VersionDiff.PreRelease
            SemverDiff.BUILD -> VersionDiff.Build
            else -> VersionDiff.None
        }
    }
}

sealed class VersionDiff {
    data object Major : VersionDiff()
    data object Minor : VersionDiff()
    data object Patch : VersionDiff()
    data object None : VersionDiff()
    data object Unknown : VersionDiff()
    data object PreRelease : VersionDiff()
    data object Build : VersionDiff()
}
