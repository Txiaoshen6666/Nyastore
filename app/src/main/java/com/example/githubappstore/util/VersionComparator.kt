package com.example.githubappstore.util

import org.semver4j.Semver

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
    fun diff(latestTag: String?, installedVersion: String?): UpdateDiff {
        val tag = latestTag?.trim()?.removePrefix("v")?.trim().orEmpty()
        val installed = installedVersion?.trim()?.removePrefix("v")?.trim().orEmpty()
        if (tag.isEmpty() || installed.isEmpty()) return UpdateDiff.Unknown
        val t = Semver.parse(tag) ?: return UpdateDiff.Unknown
        val i = Semver.parse(installed) ?: return UpdateDiff.Unknown
        return when (t.diff(i)) {
            org.semver4j.VersionDiff.MAJOR -> UpdateDiff.Major
            org.semver4j.VersionDiff.MINOR -> UpdateDiff.Minor
            org.semver4j.VersionDiff.PATCH -> UpdateDiff.Patch
            org.semver4j.VersionDiff.PRE_RELEASE -> UpdateDiff.PreRelease
            org.semver4j.VersionDiff.BUILD -> UpdateDiff.Build
            else -> UpdateDiff.None
        }
    }

    enum class UpdateDiff { None, Major, Minor, Patch, PreRelease, Build, Unknown }
}

sealed class VersionDiff {
    object Major : VersionDiff()
    object Minor : VersionDiff()
    object Patch : VersionDiff()
    object None : VersionDiff()
    object Unknown : VersionDiff()
}
