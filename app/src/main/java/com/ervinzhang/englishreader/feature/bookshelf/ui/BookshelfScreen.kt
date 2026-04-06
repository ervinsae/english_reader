package com.ervinzhang.englishreader.feature.bookshelf.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ervinzhang.englishreader.core.model.Book

private val sampleBooks = listOf(
    Book("oxford-tree-01", "The Apple", "L1", "", 12),
    Book("oxford-tree-02", "At the Park", "L1", "", 10),
    Book("oxford-tree-03", "Big and Little", "L2", "", 14),
)

@Composable
fun BookshelfScreen(
    onOpenBook: (String) -> Unit,
    onOpenProfile: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书架") },
                actions = {
                    TextButton(onClick = onOpenProfile) {
                        Text("我的")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sampleBooks) { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBook(book.id) },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = book.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = "分级：${book.level}")
                        Text(text = "页数：${book.pageCount}")
                    }
                }
            }
        }
    }
}
