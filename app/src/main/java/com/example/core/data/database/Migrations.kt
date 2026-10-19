package com.example.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Tabel baru dari T5
        db.execSQL("CREATE TABLE IF NOT EXISTS `recent_plays` (`songId` INTEGER NOT NULL, `playedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))")
        // Index SongEntity dihapus di T1-D tanpa bump versi — drop idempoten
        // agar migrasi valid baik dari v4 ber-index maupun v4 tanpa index
        db.execSQL("DROP INDEX IF EXISTS `index_songs_artist`")
        db.execSQL("DROP INDEX IF EXISTS `index_songs_album`")
        db.execSQL("DROP INDEX IF EXISTS `index_songs_title`")
        db.execSQL("DROP INDEX IF EXISTS `index_songs_dateAdded`")
        db.execSQL("DROP INDEX IF EXISTS `index_songs_genre`")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `songs_new` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `albumId` INTEGER NOT NULL, `contentUriString` TEXT NOT NULL, `artworkUriString` TEXT NOT NULL, `trackNumber` INTEGER, `year` INTEGER, `size` INTEGER NOT NULL, `displayName` TEXT NOT NULL, `relativePath` TEXT NOT NULL, `dataPath` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `dateModified` INTEGER NOT NULL, `genre` TEXT, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO `songs_new` SELECT * FROM `songs`")
        db.execSQL("DROP TABLE `songs`")
        db.execSQL("ALTER TABLE `songs_new` RENAME TO `songs`")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `songs_new` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `albumId` INTEGER NOT NULL, `contentUriString` TEXT NOT NULL, `artworkUriString` TEXT NOT NULL, `trackNumber` INTEGER, `year` INTEGER, `size` INTEGER, `displayName` TEXT, `relativePath` TEXT, `dataPath` TEXT, `dateAdded` INTEGER, `dateModified` INTEGER, `genre` TEXT, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO `songs_new` SELECT * FROM `songs`")
        db.execSQL("DROP TABLE `songs`")
        db.execSQL("ALTER TABLE `songs_new` RENAME TO `songs`")
    }
}
