package com.example.gamehub.ui.components

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gamehub.R

@Composable
fun NinePatchBorder(
    modifier: Modifier = Modifier,
    drawableRes: Int = R.drawable.border, // Use your actual resource name (without .png)
    contentDescription: String? = null
) {
    val context = LocalContext.current
    AndroidView(
        factory = {
            ImageView(it).apply {
                setImageResource(drawableRes)
                scaleType = ImageView.ScaleType.FIT_XY // makes 9-patch work for any size
                contentDescription?.let { desc -> this.contentDescription = desc }
            }
        },
        modifier = modifier
    )
}
