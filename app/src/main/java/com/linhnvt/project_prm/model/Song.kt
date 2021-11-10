package com.linhnvt.project_prm.model

import android.net.Uri
import com.linhnvt.project_prm.utils.Constant

data class Song(
    val id: String,
    val name: String = Constant.STRING_EMPTY,
    var songUri: Uri = Uri.EMPTY,
    var imageUri: Uri = Uri.EMPTY,
    val duration: Int = 0,
    val fileName: String = Constant.STRING_EMPTY,
    val artis: String = Constant.STRING_EMPTY,
    val description: String = "",
    var isLoaded: Boolean = false,
    var isUriLoaded: Boolean = false
){
    constructor() : this(id="-1")
}