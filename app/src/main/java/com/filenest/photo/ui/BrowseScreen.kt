package com.filenest.photo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.filenest.photo.data.model.MediaListItem
import com.filenest.photo.viewmodel.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    navController: NavHostController,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val pagingItems: LazyPagingItems<MediaListItem> = viewModel.mediaList.collectAsLazyPagingItems()

    Scaffold(
        topBar = { TopAppBar(title = { Text("浏览") }) },
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                pagingItems.loadState.refresh is LoadState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                pagingItems.loadState.refresh is LoadState.Error -> {
                    val error = pagingItems.loadState.refresh as LoadState.Error
                    Text(
                        text = error.error.localizedMessage ?: "加载失败",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                pagingItems.itemCount == 0 -> {
                    Text(
                        text = "暂无数据",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(1.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = { index -> "${pagingItems[index]?.id}_$index" }
                        ) { index ->
                            val media = pagingItems[index]
                            media?.let {
                                MediaGridItem(
                                    media = it,
                                    onClick = {
                                        navController.navigate(Screen.Detail.createRoute(it.id))
                                    }
                                )
                            }
                        }

                        if (pagingItems.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaGridItem(
    media: MediaListItem,
    onClick: () -> Unit
) {
    val thumbnailUrl = "http://172.25.20.10:8916/api/preview/media/${media.thumbnailPath}"
    AsyncImage(
        model = thumbnailUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    )
}