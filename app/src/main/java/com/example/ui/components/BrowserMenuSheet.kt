package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.models.BrowserTab
import com.example.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onTabSelected: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    isIncognito: Boolean,
    activeIdentity: com.example.domain.models.BrowserIdentity,
    identities: List<com.example.domain.models.BrowserIdentity>,
    onSwitchIdentity: (String) -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isIncognito) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                // Actions Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = DesignTokens.Spacing16, vertical = DesignTokens.Spacing8),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing16),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item { MenuAction(Icons.Default.Add, "New Tab", onNewTab) }
                    item { MenuAction(Icons.Default.VpnKey, "Incognito", onNewIncognitoTab) }
                    item { MenuAction(Icons.Default.Bookmarks, "Bookmarks", onNavigateToBookmarks) }
                    item { MenuAction(Icons.Default.History, "History", onNavigateToHistory) }
                    item { MenuAction(Icons.Default.Download, "Downloads", { /* TODO Navigate to downloads */ }) }
                    item { MenuAction(Icons.Default.DesktopMac, "Desktop", { /* TODO toggle desktop */ }) }
                    item { MenuAction(Icons.Default.FindInPage, "Find", { /* TODO */ }) }
                    item { MenuAction(Icons.Default.Settings, "Settings", onNavigateToSettings) }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing16))

                // Identity switch row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing16)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(DesignTokens.CornerRadiusMedium))
                        .padding(DesignTokens.Spacing12),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showIdentityDropdown by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Identity")
                        Spacer(modifier = Modifier.width(DesignTokens.Spacing8))
                        Text(activeIdentity.name, style = MaterialTheme.typography.bodyMedium)
                    }
                    Box {
                        TextButton(onClick = { showIdentityDropdown = true }) {
                            Text("Switch")
                        }
                        DropdownMenu(
                            expanded = showIdentityDropdown,
                            onDismissRequest = { showIdentityDropdown = false }
                        ) {
                            identities.forEach { identity ->
                                DropdownMenuItem(
                                    text = { Text(identity.name) },
                                    onClick = {
                                        showIdentityDropdown = false
                                        if (identity.id != activeIdentity.id) onSwitchIdentity(identity.id)
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(DesignTokens.Spacing16))
            }
        }
    }
}

@Composable
fun MenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(DesignTokens.CornerRadiusMedium))
            .clickable(onClick = onClick)
            .padding(DesignTokens.Spacing8)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(DesignTokens.Spacing4))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
