# Browse Page Pagination Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement Paging3-based image browsing with 5-column grid layout using Retrofit API

**Architecture:** Standard Paging3 pattern with PagingSource calling ApiService.listMedia(), ViewModel exposing Flow<PagingData<MediaListItem>>, and Compose LazyVerticalGrid rendering AsyncImage items

**Tech Stack:** Paging 3.3.2, Retrofit 3.0.0, Coil 3.4.0, Hilt 2.57, Jetpack Compose, Material 3

---

### Task 1: Add Paging3 Dependencies

**Files:**
- Modify: `app/build.gradle.kts:120`

**Step 1: Add Paging3 dependencies to build.gradle.kts**

Find the Retrofit section and add Paging3 dependencies after line 120:

```kotlin
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    // Gson 解析
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    // OkHttp 日志拦截器（调试用）
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Paging 3
    implementation("androidx.paging:paging-runtime-ktx:3.3.2")
    implementation("androidx.paging:paging-compose:3.3.2")
```

**Step 2: Sync Gradle to download dependencies**

Run: `./gradlew --refresh-dependencies build --dry-run`
Expected: Gradle syncs successfully without errors

**Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "feat: add Paging3 dependencies"
```

---

### Task 2: Create MediaPagingSource

**Files:**
- Create: `app/src/main/java/com/filenest/photo/data/paging/MediaPagingSource.kt`

**Step 1: Create paging package and MediaPagingSource class**

```kotlin
package com.filenest.photo.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.filenest.photo.data.api.ApiService
import com.filenest.photo.data.model.MediaListItem

class MediaPagingSource(
    private val apiService: ApiService,
) : PagingSource<Int, MediaListItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaListItem> {
        val page = params.key ?: 1
        return try {
            val response = apiService.listMedia(
                albumId = null,
                pageNum = page,
                pageSize = params.loadSize,
            )

            if (response.isRetOk() && response.data != null) {
                LoadResult.Page(
                    data = response.data.list,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (response.data.hasNext) page + 1 else null,
                )
            } else {
                LoadResult.Error(Exception(response.retMsg()))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaListItem>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/filenest/photo/data/paging/MediaPagingSource.kt
git commit -m "feat: create MediaPagingSource for pagination"
```

---

### Task 3: Update BrowseViewModel

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/viewmodel/BrowseViewModel.kt`

**Step 1: Update BrowseViewModel with Pager implementation**

Replace entire file content with:

```kotlin
package com.filenest.photo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.model.MediaListItem
import com.filenest.photo.data.paging.MediaPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    val mediaPagingFlow = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
            prefetchDistance = 5,
        ),
        pagingSourceFactory = {
            MediaPagingSource(retrofitClient.getApiService())
        },
    ).flow.cachedIn(viewModelScope)
}
```

**Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/filenest/photo/viewmodel/BrowseViewModel.kt
git commit -m "feat: implement Pager in BrowseViewModel"
```

---

### Task 4: Update BrowseScreen with Paging3

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/ui/BrowseScreen.kt`

**Step 1: Update BrowseScreen to render paging data**

Replace entire file content with:

```kotlin
package com.filenest.photo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.filenest.photo.data.model.MediaListItem
import com.filenest.photo.viewmodel.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(navController: NavHostController) {
    val viewModel: BrowseViewModel = hiltViewModel()
    val lazyPagingItems: LazyPagingItems<MediaListItem> = viewModel.mediaPagingFlow.collectAsLazyPagingItems()

    Scaffold(
        topBar = { TopAppBar(title = { Text("浏览") }) },
        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id },
            ) { index ->
                lazyPagingItems[index]?.let { item ->
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = null,
                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            lazyPagingItems.apply {
                when (loadState.append) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("加载失败，重试中...")
                            }
                        }
                    }
                    else -> {}
                }
            }

            if (loadState.refresh is LoadState.NotLoading &&
                loadState.append.endOfPaginationReached &&
                itemCount == 0
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("暂无图片")
                    }
                }
            }
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/filenest/photo/ui/BrowseScreen.kt
git commit -m "feat: implement LazyVerticalGrid with Paging3 in BrowseScreen"
```

---

### Task 5: Build and Test

**Files:**
- None

**Step 1: Build complete project**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, APK generated at `app/build/outputs/apk/debug/app-debug.apk`

**Step 2: Manual testing checklist**

Test on emulator or device:
1. [ ] Navigate to browse tab
2. [ ] Verify images load in 5-column grid
3. [ ] Scroll down to trigger pagination
4. [ ] Check loading indicator appears
5. [ ] Verify images display correctly with crop
6. [ ] Test empty state (no images scenario)
7. [ ] Verify "暂无图片" displays when empty

**Step 3: Commit if no issues found**

```bash
git commit --allow-empty -m "feat: complete browse page pagination implementation"
```

---

## Summary

This plan implements:
1. ✅ Paging3 dependencies
2. ✅ MediaPagingSource for API integration
3. ✅ BrowseViewModel with Pager
4. ✅ BrowseScreen with 5-column LazyVerticalGrid
5. ✅ Empty state and loading handling
6. ✅ Error handling and display

Total tasks: 5
Estimated time: 15-20 minutes
