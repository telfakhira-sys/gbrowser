package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BrowserTabEntity
import com.example.data.TabGroupEntity
import com.example.viewmodel.GiyaniDestination
import com.example.viewmodel.GiyaniUiState
import com.example.viewmodel.GiyaniViewModel

private val GROUP_PALETTE = listOf(
    0xFF10B981L to "Emerald",
    0xFF06B6D4L to "Cyan",
    0xFFF59E0BL to "Amber",
    0xFF8B5CF6L to "Violet",
    0xFFF43F5EL to "Rose",
    0xFF3B82F6L to "Cobalt"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabManagerScreen(
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(GiyaniDestination.HOME_HUB)
    }

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<TabGroupEntity?>(null) }
    var deletingGroup by remember { mutableStateOf<TabGroupEntity?>(null) }
    var movingTab by remember { mutableStateOf<BrowserTabEntity?>(null) }

    val ungroupedTabs = remember(uiState.tabs) {
        uiState.tabs.filter { it.groupId == null }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("tab_manager_screen")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tab Groups & Workspace",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${uiState.tabs.size} open tabs across ${uiState.tabGroups.size} groups • ${uiState.activeProfile.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { viewModel.expandOrCollapseAllGroups(true) },
                    modifier = Modifier.testTag("expand_all_groups_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Expand All Groups",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.expandOrCollapseAllGroups(false) },
                    modifier = Modifier.testTag("collapse_all_groups_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldLess,
                        contentDescription = "Collapse All Groups",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Bar: New Group, New Tab, New Private Tab
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showCreateGroupDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("create_tab_group_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = "New Group",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "New Group", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = {
                    viewModel.createNewTab(
                        url = "https://www.google.com",
                        title = "Google Search",
                        groupId = null,
                        isPrivate = false,
                        openImmediately = true
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("new_ungrouped_tab_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "New Tab", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = {
                    viewModel.createNewTab(
                        url = "https://www.google.com",
                        title = "Private Stealth Tab",
                        groupId = null,
                        isPrivate = true,
                        openImmediately = true
                    )
                },
                modifier = Modifier.testTag("new_private_tab_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Private Tab",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Stealth",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tab_groups_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Render Tab Groups
            items(uiState.tabGroups, key = { it.id }) { group ->
                val tabsInGroup = uiState.tabs.filter { it.groupId == group.id }
                val groupColor = Color(group.colorHex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = groupColor.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("tab_group_card_${group.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Group Header Row (Click to Expand / Collapse)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(groupColor.copy(alpha = 0.14f))
                                .clickable { viewModel.toggleTabGroupExpanded(group) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("tab_group_header_${group.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(groupColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${tabsInGroup.size} ${if (tabsInGroup.size == 1) "tab" else "tabs"} • ${if (group.isExpanded) "Expanded" else "Collapsed"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = groupColor
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Add tab directly inside this group
                                IconButton(
                                    onClick = {
                                        viewModel.createNewTab(
                                            url = "https://www.google.com",
                                            title = "Google (${group.name})",
                                            groupId = group.id,
                                            isPrivate = false,
                                            openImmediately = false
                                        )
                                    },
                                    modifier = Modifier.testTag("add_tab_to_group_${group.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add tab to ${group.name}",
                                        tint = groupColor
                                    )
                                }

                                // Edit group name & color
                                IconButton(
                                    onClick = { editingGroup = group },
                                    modifier = Modifier.testTag("edit_tab_group_${group.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename or recolor ${group.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete or ungroup
                                IconButton(
                                    onClick = { deletingGroup = group },
                                    modifier = Modifier.testTag("delete_tab_group_${group.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete group ${group.name}",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Expand / Collapse indicator
                                Icon(
                                    imageVector = if (group.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (group.isExpanded) "Collapse group" else "Expand group",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Expandable Tab List inside this Group
                        AnimatedVisibility(
                            visible = group.isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (tabsInGroup.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "No tabs in this group yet. Tap + or move a tab here.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    tabsInGroup.forEach { tab ->
                                        TabItemRow(
                                            tab = tab,
                                            accentColor = groupColor,
                                            onOpenTab = { viewModel.switchToTab(tab) },
                                            onMoveTab = { movingTab = tab },
                                            onCloseTab = { viewModel.closeTab(tab) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ungrouped Tabs Section
            item(key = "ungrouped_section") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("ungrouped_tabs_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FolderOff,
                                    contentDescription = "Ungrouped Tabs",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ungrouped Tabs (${ungroupedTabs.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            TextButton(
                                onClick = {
                                    viewModel.createNewTab(
                                        url = "https://www.google.com",
                                        title = "Google Search",
                                        groupId = null,
                                        isPrivate = false,
                                        openImmediately = false
                                    )
                                },
                                modifier = Modifier.testTag("quick_add_ungrouped_tab_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add ungrouped tab",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Tab")
                            }
                        }

                        if (ungroupedTabs.isEmpty()) {
                            Text(
                                text = "All open tabs are organized into groups.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            ungroupedTabs.forEach { tab ->
                                TabItemRow(
                                    tab = tab,
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    onOpenTab = { viewModel.switchToTab(tab) },
                                    onMoveTab = { movingTab = tab },
                                    onCloseTab = { viewModel.closeTab(tab) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Create New Tab Group Dialog
    if (showCreateGroupDialog) {
        TabGroupEditorDialog(
            title = "Create Tab Group",
            initialName = "",
            initialColorHex = GROUP_PALETTE.first().first,
            confirmLabel = "Create Group",
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.createTabGroup(name, colorHex)
                showCreateGroupDialog = false
            }
        )
    }

    // 2. Rename / Recolor Existing Tab Group Dialog
    editingGroup?.let { group ->
        TabGroupEditorDialog(
            title = "Edit Tab Group",
            initialName = group.name,
            initialColorHex = group.colorHex,
            confirmLabel = "Save Changes",
            onDismiss = { editingGroup = null },
            onConfirm = { newName, newColorHex ->
                viewModel.renameOrRecolorTabGroup(group, newName, newColorHex)
                editingGroup = null
            }
        )
    }

    // 3. Delete or Ungroup Tab Group Dialog
    deletingGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { deletingGroup = null },
            title = { Text("Manage Group \"${group.name}\"") },
            text = {
                Text(
                    "Would you like to keep the tabs inside this group (move them to Ungrouped) or close all tabs in this group?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTabGroup(group.id, closeTabsInside = false)
                        deletingGroup = null
                    },
                    modifier = Modifier.testTag("confirm_ungroup_delete_button")
                ) {
                    Text("Ungroup & Keep Tabs")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.deleteTabGroup(group.id, closeTabsInside = true)
                            deletingGroup = null
                        },
                        modifier = Modifier.testTag("confirm_close_all_tabs_in_group_button")
                    ) {
                        Text("Close Group & Tabs", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { deletingGroup = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // 4. Move Tab Between Groups Dialog
    movingTab?.let { tab ->
        AlertDialog(
            onDismissRequest = { movingTab = null },
            title = { Text("Move Tab to Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tab: ${tab.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Option: Ungrouped
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.moveTabToGroup(tab.id, null)
                                movingTab = null
                            }
                            .testTag("move_tab_to_ungrouped"),
                        color = if (tab.groupId == null) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Ungrouped Tabs",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Options: Each available Tab Group
                    uiState.tabGroups.forEach { targetGroup ->
                        val isCurrent = tab.groupId == targetGroup.id
                        val groupColor = Color(targetGroup.colorHex)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.moveTabToGroup(tab.id, targetGroup.id)
                                    movingTab = null
                                }
                                .testTag("move_tab_to_group_${targetGroup.id}"),
                            color = if (isCurrent) {
                                groupColor.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(groupColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = targetGroup.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (isCurrent) {
                                    Text(
                                        text = "Current",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = groupColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { movingTab = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun TabItemRow(
    tab: BrowserTabEntity,
    accentColor: Color,
    onOpenTab: () -> Unit,
    onMoveTab: () -> Unit,
    onCloseTab: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (tab.isActive) 1.5.dp else 1.dp,
                color = if (tab.isActive) accentColor else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onOpenTab() }
            .testTag("tab_item_card_${tab.id}"),
        color = if (tab.isActive) {
            accentColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (tab.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                    contentDescription = if (tab.isPrivate) "Private Tab" else "Standard Tab",
                    tint = if (tab.isPrivate) MaterialTheme.colorScheme.tertiary else accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tab.title.ifBlank { tab.url },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (tab.isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = accentColor.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = tab.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveTab,
                    modifier = Modifier.testTag("move_tab_button_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMove,
                        contentDescription = "Move tab to another group",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenTab,
                    modifier = Modifier.testTag("open_tab_button_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Switch to tab",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onCloseTab,
                    modifier = Modifier.testTag("close_tab_button_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close tab",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabGroupEditorDialog(
    title: String,
    initialName: String,
    initialColorHex: Long,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var groupName by remember { mutableStateOf(initialName) }
    var selectedColorHex by remember { mutableLongStateOf(initialColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name (e.g., Japan Research)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tab_group_name_input")
                )

                Text(
                    text = "Group Accent Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GROUP_PALETTE.forEach { (hex, label) ->
                        val color = Color(hex)
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                                .testTag("group_color_option_$label")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(groupName, selectedColorHex) },
                enabled = groupName.isNotBlank(),
                modifier = Modifier.testTag("confirm_save_tab_group_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
