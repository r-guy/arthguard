package com.example.arthguard.core.util.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.util.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    content: @Composable () -> Unit,
    bottomPadding: Dp = 32.dp,
    horizontalPadding: Dp = 20.dp,
    isKeyBoardEnabled: Boolean = false,
    backgroundColor: Color = AppColors.bgSecondary,
    closeIconBgColor: Color = AppColors.bgSecondary,
    closeIconColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = AppColors.transparent,
        dragHandle = {
            Icon(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .background(
                        color = closeIconBgColor,
                        shape = RoundedCornerShape(size = 32.dp)
                    )
                    .clip(shape = RoundedCornerShape(size = 32.dp))
                    .clickable(onClick = onDismissRequest)
                    .padding(all = 10.dp)
                    .size(12.dp),
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = closeIconColor
            )
        },
        modifier = if (isKeyBoardEnabled) Modifier.imePadding() else Modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(size = 32.dp,))
                .background(backgroundColor)
                .navigationBarsPadding()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = bottomPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 24.dp)
                    .height(4.dp)
                    .width(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(size = 16.dp)
                    )
            )
            content()
        }
    }
}
