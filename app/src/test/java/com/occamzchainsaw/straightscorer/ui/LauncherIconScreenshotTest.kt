package com.occamzchainsaw.straightscorer.ui

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.occamzchainsaw.straightscorer.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Preview of the adaptive launcher icon under common masks, plus the themed (monochrome) variant. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class LauncherIconScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun launcherIcon() {
        composeRule.setContent {
            Row(
                modifier = Modifier
                    .background(Color(0xFFECEFF1))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(CircleShape)
                Icon(RoundedCornerShape(30))
                Icon(RoundedCornerShape(8.dp))
                Icon(CircleShape, themed = true)
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/icon/launcher_icon.png")
    }

    /** Adaptive icons are 108dp, of which the mask shows the middle 72dp; drawn here at 64dp. */
    @Composable
    private fun Icon(shape: Shape, themed: Boolean = false) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(shape)
                .background(if (themed) Color(0xFF2E3A3B) else Color(0xFF00838F))
        ) {
            Image(
                painter = painterResource(if (themed) R.drawable.ic_launcher_monochrome else R.drawable.ic_launcher_foreground),
                contentDescription = null,
                colorFilter = if (themed) ColorFilter.tint(Color(0xFFB2EBF2)) else null,
                modifier = Modifier.requiredSize(96.dp)
            )
        }
    }
}
