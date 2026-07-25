package com.example.ui.screens

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.URLUtil
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import com.example.domain.models.BrowserTab
import com.example.ui.browser.BrowserViewModel
import com.example.ui.theme.DesignTokens
import kotlinx.coroutines.launch

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.MoreVert
import com.example.ui.components.BrowserMenuSheet
import com.example.ui.components.BrowserTabSwitcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onNavigateToTabs: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var isOmniboxFocused by remember { mutableStateOf(false) }
    
    val tabs by viewModel.tabManager.tabs.collectAsState()
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }
    val customView by viewModel.tabManager.customView.collectAsState()
    val isIncognitoSession by viewModel.tabManager.isIncognitoSession.collectAsState()
    val searchEngineUrl by viewModel.searchEngine.collectAsState()
    val newTabPage by viewModel.newTabPage.collectAsState()
    val homepageUrl by viewModel.homepageUrl.collectAsState()
    
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    val identities by viewModel.identities.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var showMenuSheet by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }

    // File chooser launcher
    val fileChooserLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val result = if (uris.isNotEmpty()) uris.toTypedArray() else null
        viewModel.tabManager.fileChooserCallback?.onReceiveValue(result)
        viewModel.tabManager.fileChooserCallback = null
    }

    DisposableEffect(context) {
        viewModel.tabManager.attachActivityContext(context)
        viewModel.tabManager.setFileLaunchHandler { callback, params ->
            val acceptTypes = params?.acceptTypes?.joinToString(",") ?: "*/*"
            val type = if (acceptTypes.isEmpty()) "*/*" else acceptTypes
            fileChooserLauncher.launch(type)
        }
        onDispose {
            viewModel.tabManager.detachActivityContext()
            viewModel.tabManager.setFileLaunchHandler { _, _ -> }
        }
    }

    LaunchedEffect(activeTab?.state?.url) {
        if (!isOmniboxFocused) {
            urlInput = activeTab?.state?.url?.let { if (it == "about:blank") "" else it } ?: ""
        }
    }

    LaunchedEffect(activeTab?.state?.isLoading) {
        if (activeTab?.state?.isLoading == false) {
            isRefreshing = false
        }
    }
    
    BackHandler(enabled = showMenuSheet || showTabSwitcher) {
        if (showMenuSheet) showMenuSheet = false
        else if (showTabSwitcher) showTabSwitcher = false
    }

    if (customView != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { customView!! }, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = { viewModel.tabManager.hideCustomView() },
                modifier = Modifier.align(Alignment.TopEnd).padding(DesignTokens.Spacing16).statusBarsPadding()
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Fullscreen", tint = Color.White)
            }
        }
        return
    }

    val onUrlSubmit: (String) -> Unit = { targetUrl ->
        var cleanUrl = targetUrl.trim()
        if (cleanUrl.isNotEmpty()) {
            if (URLUtil.isValidUrl(cleanUrl)) {
                activeTab?.webView?.loadUrl(cleanUrl)
            } else if (cleanUrl.contains(".") && !cleanUrl.contains(" ")) {
                activeTab?.webView?.loadUrl("https://$cleanUrl")
            } else {
                activeTab?.webView?.loadUrl(searchEngineUrl + cleanUrl)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.statusBarsPadding()
                ) {
                    BrowserTopBar(
                        urlInput = urlInput,
                        onUrlInputChange = { urlInput = it },
                        onUrlSubmit = onUrlSubmit,
                        activeTab = activeTab,
                        isIncognito = isIncognitoSession,
                        onGoBack = { activeTab?.webView?.goBack() },
                        onReloadStop = {
                            if (activeTab?.state?.isLoading == true) activeTab.webView?.stopLoading()
                            else activeTab?.webView?.reload()
                        },
                        isOmniboxFocused = isOmniboxFocused,
                        onOmniboxFocusChanged = { isOmniboxFocused = it },
                        onMenuClick = { showMenuSheet = true },
                        onTabsClick = { showTabSwitcher = true },
                        tabCount = tabs.size
                    )
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    val currentUrl = activeTab?.state?.url ?: ""
                    if (currentUrl == "about:blank" || currentUrl.isEmpty()) {
                        if (newTabPage == "speed_dial") {
                            SpeedDialContent(onUrlSubmit = { url -> activeTab?.webView?.loadUrl(url) })
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                        }
                    } else {
                        PullToRefreshBox(
                            modifier = Modifier.fillMaxSize(),
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            onRefresh = { 
                                isRefreshing = true
                                activeTab?.webView?.reload() 
                            }
                        ) {
                            if (activeTab?.webView != null) {
                                val isSystemInDarkTheme = isSystemInDarkTheme()
                                key(activeTabId) {
                                    AndroidView(
                                        factory = { activeTab.webView!! },
                                        modifier = Modifier.fillMaxSize(),
                                        update = { view ->
                                            val url = activeTab.state.url
                                            if (url != "about:blank" && url.isNotEmpty()) {
                                                viewModel.addHistory(url, activeTab.state.title, null)
                                            }
                                            viewModel.tabManager.applyDarkTheme(activeTabId ?: "", isSystemInDarkTheme)
                                        }
                                    )
                                }
                            } else {
                                Text("No active tab", modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
            
            BrowserMenuSheet(
                isVisible = showMenuSheet,
                onDismiss = { showMenuSheet = false },
                tabs = tabs,
                activeTabId = activeTabId,
                onTabSelected = { 
                    viewModel.tabManager.selectTab(it)
                    showMenuSheet = false
                },
                onCloseTab = { viewModel.tabManager.closeTab(it) },
                onNewTab = { 
                    viewModel.tabManager.addNewTab()
                    showMenuSheet = false
                },
                onNewIncognitoTab = {
                    viewModel.tabManager.addNewTab(isIncognito = true)
                    showMenuSheet = false
                },
                onNavigateToSettings = {
                    showMenuSheet = false
                    onNavigateToSettings()
                },
                onNavigateToBookmarks = {
                    showMenuSheet = false
                    onNavigateToBookmarks()
                },
                onNavigateToHistory = {
                    showMenuSheet = false
                    onNavigateToHistory()
                },
                isIncognito = isIncognitoSession,
                activeIdentity = activeIdentity,
                identities = identities,
                onSwitchIdentity = { viewModel.switchIdentity(it) }
            )

            BrowserTabSwitcher(
                isVisible = showTabSwitcher,
                onDismiss = { showTabSwitcher = false },
                tabs = tabs,
                activeTabId = activeTabId,
                onTabSelected = { 
                    viewModel.tabManager.selectTab(it)
                    showTabSwitcher = false
                },
                onCloseTab = { viewModel.tabManager.closeTab(it) },
                onNewTab = { 
                    viewModel.tabManager.addNewTab()
                    showTabSwitcher = false
                },
                isIncognito = isIncognitoSession
            )
        }
    }
}

@Composable
fun SpeedDialContent(onUrlSubmit: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text("Good Morning", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(DesignTokens.Spacing24))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing12),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing12),
            modifier = Modifier.fillMaxWidth()
        ) {
            val topSites = listOf("google.com", "youtube.com", "wikipedia.org", "reddit.com", "amazon.com", "twitter.com", "github.com", "stackoverflow.com")
            items(topSites.size) { i ->
                val site = topSites[i]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onUrlSubmit("https://www.$site") }.padding(DesignTokens.Spacing4)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(DesignTokens.CornerRadiusMedium)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(site.first().uppercase(), style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing4))
                    Text(site.substringBefore("."), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(modifier = Modifier.weight(2f))
    }
}

@Composable
fun BrowserTopBar(
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onUrlSubmit: (String) -> Unit,
    activeTab: BrowserTab?,
    isIncognito: Boolean,
    onGoBack: () -> Unit,
    onReloadStop: () -> Unit,
    isOmniboxFocused: Boolean,
    onOmniboxFocusChanged: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onTabsClick: () -> Unit,
    tabCount: Int,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing12, vertical = DesignTokens.Spacing8),
        shape = RoundedCornerShape(DesignTokens.CornerRadiusExtraLarge),
        color = if (isIncognito) Color.DarkGray else MaterialTheme.colorScheme.surfaceColorAtElevation(DesignTokens.ElevationLow),
        tonalElevation = DesignTokens.ElevationLow,
        shadowElevation = DesignTokens.ElevationLow
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing8, vertical = DesignTokens.Spacing8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(visible = !isOmniboxFocused) {
                    IconButton(onClick = onGoBack, enabled = activeTab?.state?.canGoBack == true, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(DesignTokens.CornerRadiusLarge))
                        .background(if (isIncognito) Color.Gray.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = DesignTokens.Spacing12),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val isSecure = activeTab?.state?.isSecure == true
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = !isOmniboxFocused) {
                            Row {
                                Icon(
                                    imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Info,
                                    contentDescription = "Security Status",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(DesignTokens.Spacing8))
                            }
                        }
                        
                        BasicTextField(
                            value = urlInput,
                            onValueChange = onUrlInputChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { onOmniboxFocusChanged(it.isFocused) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                focusManager.clearFocus()
                                onUrlSubmit(urlInput)
                            }),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (urlInput.isEmpty()) {
                                        Text("Search or type URL", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        if (urlInput.isNotEmpty() && isOmniboxFocused) {
                            IconButton(onClick = { onUrlInputChange("") }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear text", modifier = Modifier.size(16.dp))
                            }
                        } else if (!isOmniboxFocused) {
                            val isLoading = activeTab?.state?.isLoading == true
                            IconButton(onClick = onReloadStop, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                    contentDescription = if (isLoading) "Stop loading" else "Reload page",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                AnimatedVisibility(visible = !isOmniboxFocused) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(DesignTokens.Spacing8))
                        
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(DesignTokens.CornerRadiusSmall))
                                .background(Color.Transparent)
                                .clickable(onClick = onTabsClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$tabCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(DesignTokens.Spacing4))

                        IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            
            val isLoading = activeTab?.state?.isLoading == true
            val progress = activeTab?.state?.loadingProgress ?: 0
            
            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

// Bottom bar removed for minimalist UI

