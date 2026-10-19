package com.example.core.data.mediastore.scan

/**
 * Path filtering rules for library scan (blacklist & whitelist).
 * Pure functions, no state. Extracted from MediaStoreScanner.
 */
object ScanPathFilter {

    fun isPathBlacklisted(dataPath: String, relativePath: String, blacklist: Set<String>): Boolean {
        val normData = dataPath.replace('\\', '/').lowercase()
        val normRel = relativePath.replace('\\', '/').lowercase()

        // System & app-private folders are always excluded
        if (listOf("android/data", "android/obb", ".thumbnails").any { folder ->
                normData.contains("/$folder") || normRel.contains(folder)
            }) return true

        return blacklist.any { entry ->
            val clean = entry.trim().replace('\\', '/').trim('/').lowercase()
            clean.isNotEmpty() &&
                (normData.contains("/$clean") || normRel.startsWith(clean) ||
                    normRel.contains("/$clean") || normRel.contains("$clean/"))
        }
    }

    fun isPathIncluded(dataPath: String, relativePath: String, includedFolders: Set<String>): Boolean {
        if (includedFolders.isEmpty()) return true

        val normData = dataPath.replace('\\', '/').lowercase()
        val normRel = relativePath.replace('\\', '/').trim('/').lowercase()

        for (folder in includedFolders) {
            val raw = folder.trim().replace('\\', '/')
            if (raw.isBlank()) continue

            val normFolder = raw.lowercase().trimEnd('/')
            if (normFolder.isEmpty()) continue

            // Direct path match
            if (normData.startsWith("$normFolder/") || normData == normFolder) {
                return true
            }

            // Handle alias /sdcard/ <=> /storage/emulated/0/
            if (normFolder.contains("/sdcard/")) {
                val emu = normFolder.replace("/sdcard/", "/storage/emulated/0/")
                if (normData.startsWith("$emu/") || normData == emu) return true
            }
            if (normFolder.contains("/storage/emulated/0/")) {
                val sdc = normFolder.replace("/storage/emulated/0/", "/sdcard/")
                if (normData.startsWith("$sdc/") || normData == sdc) return true
            }

            // Relative path match (e.g., "Music", "Download", "Audio")
            val cleanRel = raw
                .replace("/storage/emulated/0/", "")
                .replace("/sdcard/", "")
                .trim('/')
                .lowercase()
            if (cleanRel.isNotEmpty()) {
                if (normRel.startsWith(cleanRel) || normRel == cleanRel ||
                    normRel.contains("$cleanRel/") || normData.contains("/$cleanRel/")) {
                    return true
                }
            }
        }
        return false
    }
}
