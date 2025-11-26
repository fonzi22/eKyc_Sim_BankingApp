package com.example.ekycsimulate.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel

import com.example.ekycsimulate.ui.auth.IdCardInfo

class EkycViewModel : ViewModel() {
    var croppedImage: Bitmap? = null
    var idCardInfo: IdCardInfo? = null
}
