package com.example.gamehub.features.JorisJump.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.gamehub.R
import com.example.gamehub.features.JorisJump.model.PLAYER_WIDTH_DP
import com.example.gamehub.features.JorisJump.model.PLAYER_HEIGHT_DP
import com.example.gamehub.features.JorisJump.model.PLATFORM_WIDTH_DP
import com.example.gamehub.features.JorisJump.model.PLATFORM_HEIGHT_DP
import com.example.gamehub.features.JorisJump.model.SPRING_VISUAL_WIDTH_FACTOR
import com.example.gamehub.features.JorisJump.model.SPRING_VISUAL_HEIGHT_FACTOR

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JorisJumpScreenPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) { // Main container for all preview elements

            // Background Image
            Image(
                painter = painterResource(id = R.drawable.background_doodle),
                contentDescription = "Preview Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )

            // Preview Doodler
            // Encapsulate Doodler in its own Box for alignment and offset
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align this Box
                    .offset(y = (-70).dp)          // Then offset it
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.joris_doodler),
                    contentDescription = "Preview Doodler",
                    modifier = Modifier.fillMaxSize() // Image fills its parent Box
                )
            }

            // Preview Cloud Platform
            val previewCloudVisualFactor = 4f
            val pvlW = PLATFORM_WIDTH_DP
            val pvlH = PLATFORM_HEIGHT_DP
            Box( // Encapsulate Cloud in its own Box
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align this Box
                    // Offset for the cloud's visual center to be roughly where a logical platform might be
                    .offset(y = (-70 + PLAYER_HEIGHT_DP + 5 - (pvlH * (previewCloudVisualFactor - 1) / 2)).dp)
                    .size(pvlW.dp * previewCloudVisualFactor, pvlH.dp * previewCloudVisualFactor)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cloud_platform),
                    contentDescription = "Preview Cloud",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Preview Spring Mushroom
            // Positioned relative to where the cloud is, approximately
            val springPreviewYOffset = (-70 + PLAYER_HEIGHT_DP + 5 - (pvlH * (previewCloudVisualFactor - 1) / 2) - (pvlH * SPRING_VISUAL_HEIGHT_FACTOR * previewCloudVisualFactor * 0.7f)).dp
            val springPreviewXOffset = ((pvlW * previewCloudVisualFactor / 2f) - (pvlW * SPRING_VISUAL_WIDTH_FACTOR * previewCloudVisualFactor / 2f)).dp
            Box( // Encapsulate Spring in its own Box
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align this Box
                    .offset(x = springPreviewXOffset, y = springPreviewYOffset) // Then offset it
                    .size(
                        (PLATFORM_WIDTH_DP * SPRING_VISUAL_WIDTH_FACTOR * previewCloudVisualFactor).dp,
                        (PLATFORM_HEIGHT_DP * SPRING_VISUAL_HEIGHT_FACTOR * previewCloudVisualFactor).dp
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.spring_mushroom),
                    contentDescription = "Preview Spring",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Preview Enemy - THIS IS NOW A DIRECT CHILD OF THE MAIN BOX, or in its own positioning Box
            Image(
                painter = painterResource(id = R.drawable.saibaman_enemy),
                contentDescription = "Preview Enemy",
                modifier = Modifier
                    .align(Alignment.Center) // Example alignment
                    .offset(x = 50.dp, y = -50.dp) // Example offset
                    .size(40.dp, 40.dp)
            )

            // Score Text
            Text(
                "Score: 0",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // Preview Helper Text
            Text(
                "Preview (No Dynamic Logic)",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                color = Color.Black
            )
        }
    }
} 