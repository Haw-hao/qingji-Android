package ink.qingji.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun Schedule() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("计划与提醒", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("日程功能即将上线", color = Teal)
        Text("这里会记录账单日、固定支出和周期提醒。", color = Color.Gray)
    }
}
