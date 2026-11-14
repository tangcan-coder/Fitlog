package com.fitlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.FitLogApp
import com.fitlog.app.ui.theme.FitLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitLogTheme {
                val vm: AppViewModel = viewModel(factory = AppViewModel.factory(applicationContext))
                FitLogApp(vm)
            }
        }
    }
}
