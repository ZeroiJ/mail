package com.example.mail.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mail.data.local.EmailMessage
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.Geist
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.NothingTheme
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onEmailClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = Geist,
                            fontSize = 15.sp,
                            color = PureWhite
                        ),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "SEARCH MAIL",
                                    fontFamily = NDot,
                                    fontSize = 13.sp,
                                    color = MutedGray
                                )
                            }
                            inner()
                        }
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear",
                            tint = MutedGray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(onClick = viewModel::clear)
                        )
                    }
                }
            }
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MutedGray
                )
            }
        }

        if (results.isEmpty() && !isSearching && query.isNotBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO RESULTS",
                    fontFamily = NDot,
                    fontSize = 14.sp,
                    color = MutedGray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(results, key = { it.id }) { email ->
                    SearchResultRow(
                        email = email,
                        onClick = { onEmailClick(email.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    email: EmailMessage,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(com.example.mail.ui.theme.SurfaceDark)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            text = email.sender,
            fontFamily = NDot,
            fontSize = 13.sp,
            color = PureWhite
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = email.subject,
            fontFamily = NDot,
            fontSize = 15.sp,
            color = PureWhite,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = email.summary.ifBlank { email.snippet },
            fontFamily = Geist,
            fontSize = 13.sp,
            color = MutedGray,
            maxLines = 2
        )
    }
}

@Preview("Search Screen", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchScreenPreview() {
    NothingTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OLEDBlack)
        ) {
            Text(text = "SEARCH", fontFamily = NDot, color = PureWhite)
        }
    }
}
