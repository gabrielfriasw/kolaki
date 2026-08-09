package com.kolvexo.kolaki

import android.app.Application
import com.kolvexo.kolaki.data.AppPreferences
import com.kolvexo.kolaki.data.PackRepository

class KolakiApplication : Application() {
    val repository by lazy { PackRepository(this) }
    val preferences by lazy { AppPreferences(this) }
}
