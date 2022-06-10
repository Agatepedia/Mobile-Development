package com.example.agatepedia.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.agatepedia.data.local.entity.AgateEntity

@Database(entities = [AgateEntity::class], version = 1, exportSchema = false)
abstract class AgateRoomDatabase: RoomDatabase(){
    abstract fun agateDao(): AgateDao

    companion object{
        @Volatile
        private var INSTANCE: AgateRoomDatabase? = null

        @JvmStatic
        fun getDatabase(context: Context): AgateRoomDatabase{
            if(INSTANCE == null){
                synchronized(AgateRoomDatabase::class.java){
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        AgateRoomDatabase::class.java, "agate_database")
                        .build()
                }
            }

            return INSTANCE as AgateRoomDatabase
        }
    }
}