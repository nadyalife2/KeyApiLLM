package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FreeLlmHubRoot
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FreeLlmHubViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: FreeLlmHubViewModel = viewModel()
                    FreeLlmHubRoot(viewModel)
                }
            }
        }
    }
}

/**
 * Compatibility signature for Greeting screenshot tests.
 * Renders a secure welcome screen using modern Material 3 tokens.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    com.example.ui.EmptyListState(
        title = "Sandbox: Hello $name!",
        body = "Secure Local KeyStore vault successfully running on your device."
    )
}
