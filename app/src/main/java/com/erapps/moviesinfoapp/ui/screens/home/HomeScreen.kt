package com.erapps.moviesinfoapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.erapps.moviesinfoapp.R
import com.erapps.moviesinfoapp.data.api.models.FilterBySelection
import com.erapps.moviesinfoapp.data.api.models.TvShow
import com.erapps.moviesinfoapp.data.api.models.getAllFilters
import com.erapps.moviesinfoapp.data.api.models.getFilter
import com.erapps.moviesinfoapp.ui.shared.*
import com.erapps.moviesinfoapp.ui.theme.dimen
import com.erapps.moviesinfoapp.utils.getImageByPath
import com.google.accompanist.pager.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import java.math.RoundingMode

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = hiltViewModel(),
    onFavsClick: () -> Unit,
    onCardClick: (Int) -> Unit
) {
    val tvShows = viewModel.tvShows.collectAsLazyPagingItems()
    val netWorkStatus = getNetworkStatus()

    val uiState = when {
        tvShows.loadState.source.refresh == LoadState.Loading -> {
            UiState.Loading
        }
        tvShows.itemCount == 0 -> {
            if (!netWorkStatus) {
                viewModel.getLocalListOfTvShows()
                UiState.Success(tvShows)
            }
            UiState.Empty
        }
        else -> {
            UiState.Success(tvShows)
        }
    }

    HomeScreen(
        uiState = uiState,
        onEmptyButtonClick = { viewModel.getFilteredTvShows(FilterBySelection.Popular.filter) },
        onFavsClick = onFavsClick,
        onCardClick = onCardClick,
        onRefresh = { viewModel.getFilteredTvShows(getAllFilters()[0].filter) },
        onCache = { viewModel.cacheTvShows(it) },
        onFilterSelected = { viewModel.getFilteredTvShows(it) }
    )
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    uiState: UiState?,
    onFavsClick: () -> Unit,
    onEmptyButtonClick: () -> Unit,
    onCardClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    onCache: (TvShow) -> Unit,
    onFilterSelected: (String) -> Unit,
) {

    val windowSize = rememberWindowSize()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppBar(windowSize, onFavsClick) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ListAndFilter(
                onFilterSelected,
                uiState,
                windowSize,
                onEmptyButtonClick,
                onCardClick,
                onRefresh,
                onCache
            )
        }
    }
}

@Composable
private fun ListAndFilter(
    onFilterSelected: (String) -> Unit,
    uiState: UiState?,
    windowSizeClass: WindowSizeClass,
    onEmptyButtonClick: () -> Unit,
    onCardClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    onCache: (TvShow) -> Unit
) {

    Column {
        FilterChipGroup(
            onFilterSelected = onFilterSelected,
            windowSize = windowSizeClass
        )
        HomeScreenContent(
            uiState = uiState,
            onEmptyButtonClick = onEmptyButtonClick,
            onRefresh = onRefresh,
            onCache = onCache,
            onCardClick = onCardClick
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FilterChipGroup(
    modifier: Modifier = Modifier,
    filters: List<FilterBySelection> = getAllFilters(),
    onFilterSelected: (String) -> Unit,
    windowSize: WindowSizeClass
) {

    val defaultFilter = stringResource(id = R.string.default_filter)
    val selectedFilter = rememberSaveable { mutableStateOf(getFilter(defaultFilter)) }

    val windowSizeCondition = windowSize.screenWidthInfo is WindowSizeClass.WindowType.Compact
    val fontSize =
        if (windowSizeCondition) MaterialTheme.typography.subtitle1.fontSize else MaterialTheme.typography.h5.fontSize

    Column(modifier = modifier.padding(MaterialTheme.dimen.small)) {
        LazyRow {
            items(filters) { filter ->
                FilterChip(
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimen.extraSmall),
                    selected = selectedFilter.value == filter,
                    onClick = {
                        selectedFilter.value = getFilter(filter.filter)
                        onFilterSelected(filter.filter)
                    },
                    shape = CircleShape,
                    colors = ChipDefaults.filterChipColors(
                        selectedBackgroundColor = MaterialTheme.colors.primary,
                        selectedContentColor = Color.White
                    )
                ) {
                    Text(
                        modifier = Modifier.padding(MaterialTheme.dimen.small),
                        text = filter.filter.capitalize(Locale.current).replace("_", " "),
                        fontSize = fontSize
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreenContent(
    uiState: UiState?,
    onEmptyButtonClick: () -> Unit,
    onCardClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    onCache: (TvShow) -> Unit
) {
    val context = LocalContext.current
    val status = getNetworkStatus()

    PageWithState<LazyPagingItems<TvShow>>(
        uiState = uiState,
        onClick = onEmptyButtonClick
    ) {
        TvShowList(it, onRefresh, onCache) { id ->
            //only can go to details if internet is available
            if (status) {
                onCardClick(id)
                return@TvShowList
            }
            Toast.makeText(
                context,
                context.getString(R.string.cant_see_details_without_internet_text),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
private fun TvShowList(
    tvShows: LazyPagingItems<TvShow>,
    onRefresh: () -> Unit,
    onCache: (TvShow) -> Unit,
    onCardClick: (Int) -> Unit
) {

    val windowSize = rememberWindowSize()
    var refreshing by remember { mutableStateOf(false) }
    val amountOfGrids =
        if (
            windowSize.screenWidthInfo is WindowSizeClass.WindowType.Compact ||
            windowSize.screenHeightInfo is WindowSizeClass.WindowType.Medium
        ) 2 else 4

    LaunchedEffect(refreshing) {
        if (refreshing) {
            delay(2000)
            refreshing = false
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(refreshing),
        onRefresh = {
            onRefresh()
            refreshing = true
        },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(amountOfGrids),
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center
        ) {
            items(tvShows.itemCount) { i ->
                onCache(tvShows[i]!!)
                TvShowListItem(tvShow = tvShows[i]!!, onCardClick = onCardClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun TvShowListItem(
    modifier: Modifier = Modifier,
    tvShow: TvShow,
    onCardClick: (Int) -> Unit
) {

    val windowSize = rememberWindowSize()
    val paddingValue =
        if (
            windowSize.screenWidthInfo is WindowSizeClass.WindowType.Compact ||
            windowSize.screenHeightInfo is WindowSizeClass.WindowType.Medium
        ) MaterialTheme.dimen.small else MaterialTheme.dimen.medium

    Card(
        modifier = modifier
            .padding(paddingValue)
            .wrapContentSize(align = Alignment.Center),
        shape = RoundedCornerShape(MaterialTheme.dimen.borderRounded),
        elevation = MaterialTheme.dimen.elevationNormal,
        onClick = { onCardClick(tvShow.id) }
    ) {
        Column {
            ImageSection(imageUrl = tvShow.poster_path)
            Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))
            Column(
                modifier = Modifier.padding(MaterialTheme.dimen.small)
            ) {
                TitleSection(tvShowName = tvShow.name, windowSize = windowSize)
                Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))
                RatingSection(tvShowRating = tvShow.vote_average)
                Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))
            }
        }
    }
}

@Composable
fun RatingSection(
    modifier: Modifier = Modifier,
    tvShowRating: Double
) {

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RatingBar(rating = (tvShowRating / 2), starsColor = MaterialTheme.colors.secondary)
        Text(
            text = (tvShowRating / 2).toBigDecimal()
                .setScale(1, RoundingMode.UP)
                .toDouble()
                .toString()
        )
    }
}

@Composable
fun TitleSection(
    modifier: Modifier = Modifier,
    windowSize: WindowSizeClass,
    tvShowName: String
) {

    val windowSizeCondition = windowSize.screenWidthInfo is WindowSizeClass.WindowType.Compact
    val fontWeight =
        if (windowSizeCondition) FontWeight.Normal else FontWeight.Bold
    val fontTitleSize =
        if (windowSizeCondition) MaterialTheme.typography.subtitle1.fontSize else MaterialTheme.typography.h5.fontSize

    Text(
        modifier = modifier.fillMaxWidth(),
        text = tvShowName.capitalize(Locale.current),
        fontWeight = fontWeight,
        color = MaterialTheme.colors.onBackground,
        textAlign = TextAlign.Start,
        fontSize = fontTitleSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun ImageSection(
    modifier: Modifier = Modifier,
    imageUrl: String
) {
    val context = LocalContext.current

    SubcomposeAsyncImage(
        modifier = modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimen.imageLarge),
        model = ImageRequest.Builder(context)
            .data(imageUrl.getImageByPath())
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error_placeholder)
            .crossfade(true)
            .build(),
        contentDescription = null,
        loading = { LinearProgressIndicator() },
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun AppBar(windowSize: WindowSizeClass, onFavsClick: () -> Unit) {

    val windowSizeCondition = windowSize.screenWidthInfo is WindowSizeClass.WindowType.Compact
    val fontSize =
        if (windowSizeCondition) MaterialTheme.typography.h6.fontSize else MaterialTheme.typography.h4.fontSize
    val appBarHeight =
        if (windowSizeCondition) MaterialTheme.dimen.appBarNormal else MaterialTheme.dimen.appBarLarge
    val iconButtonSize =
        if (windowSizeCondition) MaterialTheme.dimen.extraLarge else MaterialTheme.dimen.extraExtraLarge
    val iconSize =
        if (windowSizeCondition) MaterialTheme.dimen.large else MaterialTheme.dimen.extraLarge

    TopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(appBarHeight),
        title = {
            Text(
                text = stringResource(id = R.string.tv_shows_title),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = fontSize
            )
        },
        actions = {
            IconButton(
                modifier = Modifier.size(iconButtonSize),
                onClick = onFavsClick
            ) {
                Icon(
                    modifier = Modifier.size(iconSize),
                    imageVector = Icons.Default.AccountCircle,
                    tint = Color.White,
                    contentDescription = null
                )
            }
        },
        backgroundColor = MaterialTheme.colors.primary
    )
}