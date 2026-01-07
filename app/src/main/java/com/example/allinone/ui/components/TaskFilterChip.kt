package com.example.allinone.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.allinone.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFilterChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color? = null,
    selectedLabelColor: Color? = null,
    containerColor: Color? = null,
    labelColor: Color? = null,
    onClick: () -> Unit
)
{

    val selBg = (selectedContainerColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.18f)
    val selFg = (selectedLabelColor ?: (selectedContainerColor ?: MaterialTheme.colorScheme.primary))
    val unselBg = containerColor ?: MaterialTheme.colorScheme.surface
    val unselFg = labelColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) (selectedContainerColor ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    FilterChip(
        modifier = modifier,
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        // 關鍵修正：強制使用圓形形狀以符合「藥丸型」草圖
        shape = CircleShape,
        // 商業配色：使用主題色定義選中與未選中的色彩
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selBg,
            selectedLabelColor = selFg,
            containerColor = unselBg,
            labelColor = unselFg
        )
    )
}

@Preview(showBackground = true)
@Composable
fun TaskFilterChipPreview() {
    AppTheme {
        Column(Modifier.padding(16.dp)) {
            TaskFilterChip(label = "Selected", isSelected = true, onClick = {})
            TaskFilterChip(label = "Unselected", isSelected = false, onClick = {})
        }
    }
}