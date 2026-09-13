package com.example.mail.ui.screens.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mail.data.local.Label
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.Geist
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import com.example.mail.ui.theme.StarkRed
import com.example.mail.ui.theme.SurfaceDark

@Composable
fun LabelManagerScreen(
    onBack: () -> Unit,
    viewModel: LabelViewModel = hiltViewModel()
) {
    val labels by viewModel.labels.collectAsState()
    var newName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                    .clickable(onClick = onBack)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "LABELS",
                fontFamily = NDot,
                fontSize = 20.sp,
                color = PureWhite
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = Geist,
                        fontSize = 15.sp,
                        color = PureWhite
                    ),
                    decorationBox = { inner ->
                        if (newName.isEmpty()) {
                            Text(
                                text = "New label name…",
                                fontFamily = Geist,
                                fontSize = 15.sp,
                                color = MutedGray
                            )
                        }
                        inner()
                    }
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (newName.isBlank()) BorderGray else PureWhite)
                    .clickable(
                        enabled = newName.isNotBlank(),
                        onClick = { viewModel.create(newName) { newName = "" } }
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ADD",
                    fontFamily = NDot,
                    fontSize = 13.sp,
                    color = OLEDBlack
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(labels, key = { it.id }) { label ->
                LabelRow(
                    label = label,
                    onRename = { viewModel.rename(label.id, it) },
                    onDelete = { viewModel.delete(label) }
                )
            }
        }
    }
}

@Composable
private fun LabelRow(
    label: Label,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(label.name) }
    val isSystem = label.type == "system"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (editing) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = Geist,
                        fontSize = 15.sp,
                        color = PureWhite
                    )
                )
            } else {
                Text(
                    text = label.name,
                    fontFamily = NDot,
                    fontSize = 15.sp,
                    color = PureWhite
                )
            }
            Text(
                text = if (isSystem) "SYSTEM" else "CUSTOM",
                fontFamily = NDot,
                fontSize = 10.sp,
                color = MutedGray
            )
        }
        if (isSystem) return@Row
        if (editing) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Save",
                tint = PureWhite,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        onRename(draft)
                        editing = false
                    }
            )
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Cancel",
                tint = MutedGray,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { editing = false }
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Rename",
                tint = MutedGray,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        draft = label.name
                        editing = true
                    }
            )
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = StarkRed,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onDelete)
            )
        }
    }
}
