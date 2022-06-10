package com.example.agatepedia.data.local.entity

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agate")
class AgateEntity (
    @NonNull
    @field:PrimaryKey
    @field:ColumnInfo(name = "type")
    var type: String,
    @field:ColumnInfo(name = "price")
    var price: String,
    @field:ColumnInfo(name = "image")
    var image: String
    )