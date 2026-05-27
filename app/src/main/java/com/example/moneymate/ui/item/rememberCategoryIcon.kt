
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@Composable
fun rememberCategoryIcon(iconResName: String): Painter {
    val context = LocalContext.current

    // Tìm ID nguyên bản (Int) của file ảnh trong thư mục drawable dựa trên chuỗi tên mã hóa trong DB
    val resId = remember(iconResName) {
        context.resources.getIdentifier(iconResName, "drawable", context.packageName)
    }

    return if (resId != 0) {
        painterResource(id = resId)
    } else {
        // Trả về icon mặc định phòng trường hợp chuỗi tên từ DB bị lỗi hoặc file ảnh bị xóa
        painterResource(id = android.R.drawable.ic_menu_help)
    }
}