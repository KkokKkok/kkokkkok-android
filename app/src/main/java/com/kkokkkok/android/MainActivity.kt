package com.kkokkkok.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KkokKkokApp() }
    }
}

// Bootstrap host only. Figma home UI will be added in a separate PR.
@Composable
internal fun KkokKkokApp() {
    Box(Modifier.fillMaxSize().safeDrawingPadding())
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    KkokKkokApp()
}
