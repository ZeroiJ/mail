package com.example.mail.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.mail.ui.screens.labels.LabelViewModel
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.Geist
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import com.example.mail.ui.theme.SurfaceDark

@Composable
fun LabelSection(
    messageId: String,
    onManageLabels: () -> Unit,
    viewModel: LabelViewModel = hiltViewModel()
) {
    val labels by viewModel.labels.collectAsState()
    val appliedIds by viewModel.appliedIds.collectAsState()
    var pickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(messageId) {
        viewModel.loadApplied(messageId)
    }

    val applied = labels.filter { it.id in appliedIds }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "LABELS",
            fontFamily = NDot,
            fontSize = 11.sp,
            color = MutedGray
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            applied.forEach { label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SurfaceDark)
                        .border(1.dp, BorderGray, RoundedCornerShape(50))
                        .clickable { viewModel.toggle(messageId, label.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label.name.uppercase(),
                        fontFamily = NDot,
                        fontSize = 11.sp,
                        color = PureWhite
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                    .clickable { pickerOpen = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add label",
                        tint = MutedGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "ADD",
                        fontFamily = NDot,
                        fontSize = 11.sp,
                        color = MutedGray
                    )
                }
            }
        }
    }

    if (pickerOpen) {
        LabelPickerDialog(
            labels = labels,
            appliedIds = appliedIds,
            onToggle = { viewModel.toggle(messageId, it) },
            onCreate = { viewModel.create(it) },
            onManage = {
                pickerOpen = false
                onManageLabels()
            },
            onDismiss = { pickerOpen = false }
        )
    }
}

@Composable
private fun LabelPickerDialog(
    labels: List<Label>,
    appliedIds: Set<String>,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OLEDBlack,
        title = {
            Text(
                text = "APPLY LABELS",
                fontFamily = NDot,
                fontSize = 16.sp,
                color = PureWhite
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(labels, key = { it.id }) { label ->
                        val applied = label.id in appliedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (applied) SurfaceDark
                                    else OLEDBlack
                                )
                                .clickable { onToggle(label.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label.name,
                                fontFamily = Geist,
                                fontSize = 14.sp,
                                color = if (applied) PureWhite else MutedGray
                            )
                            if (applied) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Applied",
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = Geist,
                                fontSize = 14.sp,
                                color = PureWhite
                            ),
                            decorationBox = { inner ->
                                if (newName.isEmpty()) {
                                    Text(
                                        text = "New label…",
                                        fontFamily = Geist,
                                        fontSize = 14.sp,
                                        color = MutedGray
                                    )
                                }
                                inner()
                            }
                        )
                    }
                    Text(
                        text = "ADD",
                        fontFamily = NDot,
                        fontSize = 12.sp,
                        color = PureWhite,
                        modifier = Modifier.clickable(
                            enabled = newName.isNotBlank(),
                            onClick = {
                                onCreate(newName)
                                newName = ""
                            }
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onManage) {
                Text(
                    text = "MANAGE",
                    fontFamily = NDot,
                    fontSize = 12.sp,
                    color = MutedGray
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "DONE",
                    fontFamily = NDot,
                    fontSize = 12.sp,
                    color = PureWhite
                )
            }
        }
    )
}
