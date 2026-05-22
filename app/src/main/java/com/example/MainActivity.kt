package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.AppNavigationWrapper
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Collect and display native toasts
                LaunchedEffect(Unit) {
                    viewModel.toastEvent.collect { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
                AppNavigationWrapper(viewModel = viewModel)
            }
        }
    }
}
