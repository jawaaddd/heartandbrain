package com.heartandbrain.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun clipTypeToString(value: ClipType): String = value.name
    @TypeConverter fun stringToClipType(value: String): ClipType = ClipType.valueOf(value)

    @TypeConverter fun categoryToString(value: Category): String = value.name
    @TypeConverter fun stringToCategory(value: String): Category = Category.valueOf(value)
}
