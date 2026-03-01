# Browse Page Pagination Design

## Overview
Implement Paging3-based image browsing functionality for the browse page, displaying media items from the backend API with 5-column grid layout.

## Requirements
- Use Paging3 framework for lazy loading
- Display images in a 5-column grid layout
- Show all albums (albumId = null)
- No click interaction (display only)
- Pagination only, no pull-to-refresh
- Display "暂无图片" (No images) when empty

## Architecture

### Component Relationship
```
BrowseViewModel
  └─ PagingSource (MediaPagingSource)
      └─ ApiService.listMedia()
          └─ PageData<MediaListItem>

BrowseScreen
  └─ LazyVerticalGrid (5 columns)
      └─ AsyncImage (Coil)
```

### Data Flow
```
User scroll → PagingSource.load() → API request → PagingData update → Compose redraw → Display images
```

## Components

### 1. MediaPagingSource
**Location:** `data/paging/MediaPagingSource.kt`

**Responsibilities:**
- Implement PagingSource<Int, MediaListItem>
- Call ApiService.listMedia() with pagination params
- Handle success/error states
- Return LoadResult.Page or LoadResult.Error

**Configuration:**
- Initial page: 1
- Page size: 20
- Prefetch distance: 5 pages

### 2. BrowseViewModel (Modified)
**Location:** `viewmodel/BrowseViewModel.kt`

**Responsibilities:**
- Create Pager with PagingConfig
- Expose Flow<PagingData<MediaListItem>>
- Cache paging data in viewModelScope

**Configuration:**
- pageSize: 20
- enablePlaceholders: false
- prefetchDistance: 5

### 3. BrowseScreen (Modified)
**Location:** `ui/BrowseScreen.kt`

**Responsibilities:**
- Collect lazyPagingItems from ViewModel
- Render images using LazyVerticalGrid with 5 columns
- Display loading/error states
- Show empty state message

**Layout:**
- GridCells.Fixed(5)
- Aspect ratio: 1.0 (square)
- Item spacing: 4dp
- Content scaling: Crop

## Error Handling

### API Errors
- Catch exceptions in PagingSource
- Return LoadResult.Error
- Display error message in UI

### Empty State
- Check loadState.refresh == NotLoading
- Check loadState.append.endOfPaginationReached == true
- Check itemCount == 0
- Display "暂无图片"

## Dependencies

### New Dependencies
```kotlin
implementation("androidx.paging:paging-runtime-ktx:3.3.2")
implementation("androidx.paging:paging-compose:3.3.2")
```

### Existing Dependencies (Reuse)
- Retrofit 3.0.0
- Coil 3.4.0
- Hilt 2.57
- Navigation Compose 2.9.7
- Material 3 (via BOM)

## File Changes

### New Files
- `data/paging/MediaPagingSource.kt`

### Modified Files
- `viewmodel/BrowseViewModel.kt`
- `ui/BrowseScreen.kt`
- `app/build.gradle.kts`

## Implementation Notes

### PagingSource Key Strategy
- Key type: Int (page number)
- Null prevKey for page 1
- Null nextKey when hasNext == false

### Refresh Key
- Use anchorPosition to find current page
- Return nearest page's prevKey + 1

### Grid Configuration
- 5 columns fixed
- Square aspect ratio for images
- Content scale: Crop to fill each cell
- Rounded corners (4dp) for images

## Testing Considerations

### Manual Testing
- Scroll to trigger pagination
- Verify empty state
- Test API error handling
- Check image loading

### Future Enhancements
- Add pull-to-refresh if needed
- Implement click interactions
- Add image details view
- Consider RemoteMediator for offline support
