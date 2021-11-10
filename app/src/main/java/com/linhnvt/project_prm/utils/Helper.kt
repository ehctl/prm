package com.linhnvt.project_prm.utils

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import com.linhnvt.project_prm.R
import kotlin.math.floor


object Helper {
    private val colorList = arrayOf(
        R.color.item_song_bg_color_1,
        R.color.item_song_bg_color_2,
        R.color.item_song_bg_color_3,
        R.color.item_song_bg_color_4,
        R.color.item_song_bg_color_5,
        R.color.item_song_bg_color_6,
        R.color.item_song_bg_color_7,
        R.color.item_song_bg_color_8,
        R.color.item_song_bg_color_9,
        R.color.item_song_bg_color_10,
        R.color.item_song_bg_color_11,
        R.color.item_song_bg_color_12,
        R.color.item_song_bg_color_13,
        R.color.item_song_bg_color_14,
    )

    fun convertTime(duration: Int): String{
        val hour = (floor(duration.toFloat()  / 3600)).toInt()
        val minute = (floor(duration.toFloat() % 3600 / 60)).toInt()
        val second = duration % 60

        var secondStr = "$second"
        var minuteStr = "$minute"

        if (second < 10 ) secondStr = "0$secondStr"
        if (minute < 10 ) minuteStr = "0$minuteStr"
        var dur = if (hour == 0) "" else "$hour:"
        dur += "$minuteStr:$secondStr"
        return dur
    }

    fun randomColor(): Int{
        return colorList[colorList.indices.random()]

    }

}