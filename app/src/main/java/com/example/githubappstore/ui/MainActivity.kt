package com.example.githubappstore.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.githubappstore.GitHubAppStoreApp
import com.example.githubappstore.ui.onboarding.OnboardingWizardRoute
import com.example.githubappstore.ui.theme.GitStoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = GitHubAppStoreApp.container.settings
        setContent {
            val pureBlack by settings.pureBlackDarkMode.collectAsState(initial = false)
            val dynamic by settings.dynamicColor.collectAsState(initial = true)
            var wizardDone by remember { mutableStateOf(false) }
            val persistedDone by settings.wizardCompleted.collectAsState(initial = false)
            val showWizard = !wizardDone && !persistedDone
            GitStoreTheme(dynamicColor = dynamic, pureBlackDark = pureBlack) {
                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    if (showWizard) OnboardingWizardRoute(onFinished = { wizardDone = true })
                    else GitStoreApp()
                }
            }
        }
    }
}
