package com.ervinzhang.englishreader.feature.vocabulary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class WordUi(val word: String, val meaning: String)

private val sampleWords = listOf(
    WordUi("apple", "苹果"),
    WordUi("dad", "爸爸"),
)

@Composable
fun VocabularyScreen(
    onBack: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<WordUi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生词本") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sampleWords) { item ->
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = item.word)
                        Text(text = item.meaning)
                        Button(onClick = { pendingDelete = item }) {
                            Text("删除")
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("确认删除") },
            text = { Text("要删除 ${pendingDelete?.word} 吗？") },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }
}
