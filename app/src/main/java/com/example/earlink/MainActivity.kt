package com.example.earlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.earlink.theme.EarLinkTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      EarLinkTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
