import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.gamehub.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

val GameBoxFontFamily = FontFamily(
    Font(R.font.gamebox_font1) // use your actual font file name here!
)

@RequiresApi(Build.VERSION_CODES.O)
val GameBoxTypography = Typography(
    displayLarge = TextStyle(fontFamily = GameBoxFontFamily),
    displayMedium = TextStyle(fontFamily = GameBoxFontFamily),
    displaySmall = TextStyle(fontFamily = GameBoxFontFamily),
    headlineLarge = TextStyle(fontFamily = GameBoxFontFamily),
    headlineMedium = TextStyle(fontFamily = GameBoxFontFamily),
    headlineSmall = TextStyle(fontFamily = GameBoxFontFamily),
    titleLarge = TextStyle(fontFamily = GameBoxFontFamily),
    titleMedium = TextStyle(fontFamily = GameBoxFontFamily),
    titleSmall = TextStyle(fontFamily = GameBoxFontFamily),
    bodyLarge = TextStyle(fontFamily = GameBoxFontFamily),
    bodyMedium = TextStyle(fontFamily = GameBoxFontFamily),
    bodySmall = TextStyle(fontFamily = GameBoxFontFamily),
    labelLarge = TextStyle(fontFamily = GameBoxFontFamily),
    labelMedium = TextStyle(fontFamily = GameBoxFontFamily),
    labelSmall = TextStyle(fontFamily = GameBoxFontFamily)
)