package com.occamzchainsaw.straightscorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.occamzchainsaw.straightscorer.data.settings.AppSettings
import com.occamzchainsaw.straightscorer.data.settings.ThemeMode
import com.occamzchainsaw.straightscorer.ui.navigation.StraightScorerApp
import com.occamzchainsaw.straightscorer.ui.theme.StraightScorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = (application as StraightScorerApplication).container.settingsRepository
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
            StraightScorerTheme(
                darkTheme = when (settings.themeMode) {
                    ThemeMode.System -> isSystemInDarkTheme()
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                },
                dynamicColor = settings.dynamicColor
            ) {
                StraightScorerApp()
            }
        }
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    StraightScorerTheme {
//        Greeting("Android")
//    }
//}