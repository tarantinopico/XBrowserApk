package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.models.BrowserTab
import com.example.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTabSwitcher(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onTabSelected: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    isIncognito: Boolean
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text("Tabs") },
                        actions = {
                            IconButton(onClick = onNewTab) { Icon(Icons.Default.Add, contentDescription = "New Tab") }
                            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (isIncognito) Color.DarkGray else MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(
                        start = DesignTokens.Spacing16,
                        end = DesignTokens.Spacing16,
                        top = innerPadding.calculateTopPadding() + DesignTokens.Spacing16,
                        bottom = innerPadding.calculateBottomPadding() + DesignTokens.Spacing16
                    ),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing12),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing12),
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    items(tabs) { tab ->
                        TabCard(
                            tab = tab,
                            isActive = tab.id == activeTabId,
                            onClick = { onTabSelected(tab.id) },
                            onClose = { onCloseTab(tab.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabCard(tab: BrowserTab, isActive: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.CornerRadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive && tab.state.isIncognito) Color.DarkGray
            else if (isActive) MaterialTheme.colorScheme.primaryContainer
            else if (tab.state.isIncognito) Color.Gray
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) DesignTokens.ElevationHigh else DesignTokens.ElevationLow)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing8, vertical = DesignTokens.Spacing4),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.state.title,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider()
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (tab.state.thumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = tab.state.thumbnail!!.asImageBitmap(),
                        contentDescription = "Thumbnail for ${tab.state.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = tab.state.url.takeIf { it.isNotEmpty() && it != "about:blank" } ?: "New Tab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(DesignTokens.Spacing8)
                    )
                }
            }
        }
    }
}
