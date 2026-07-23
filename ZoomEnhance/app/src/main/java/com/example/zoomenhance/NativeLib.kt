package com.example.zoomenhance

import android.graphics.Bitmap

object NativeLib {
    init { System.loadLibrary("native-lib") }
    external fun initModel(modelPath: String, taesdPath: String): Boolean
    external fun enhanceImage(bitmap: Bitmap): Bitmap?
    external fun releaseModel()
}
