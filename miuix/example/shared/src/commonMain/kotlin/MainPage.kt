// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalScrollBarApi::class)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import component.arrowSection
import component.badgeSection
import component.basicComponentSection
import component.blurSection
import component.bottomSheetSection
import component.breadcrumbBarSection
import component.buttonSection
import component.cardSection
import component.checkboxSection
import component.colorPickerSection
import component.dialogSection
import component.dropdownSection
import component.numberPickerSection
import component.otherPageSection
import component.progressIndicatorSection
import component.radioButtonSection
import component.sliderSection
import component.snackbarSection
import component.spinnerSection
import component.switchSection
import component.tabRowSection
import component.textFieldSection
import component.tooltipSection
import i18n.LocalStrings
import i18n.Str
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.menu.OverlayIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import utils.AdaptiveTopAppBar
import utils.BlurredBar
import utils.pageContentPadding
import utils.pageScrollModifiers
import utils.rememberBlurBackdrop

// Multi-select entries, grouped exactly like the dropdown shows them.
private val MultiSelectKeys = listOf(
    listOf(Str.MultiSelectionA1, Str.MultiSelectionA2),
    listOf(Str.MultiSelectionB1, Str.MultiSelectionB2, Str.MultiSelectionB3),
)

@Composable
fun MainPage(
    snackbarHostState: SnackbarHostState,
    padding: PaddingValues,
) {
    val s = LocalStrings.current
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    var searchValue by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val notExpanded by remember { derivedStateOf { !expanded } }
    val onCancelSearch = remember {
        {
            expanded = false
            searchValue = ""
        }
    }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    var selectedIndex1 by remember { mutableIntStateOf(0) }
    var selectedIndex2 by remember { mutableIntStateOf(1) }
    var selectedIndex3 by remember { mutableIntStateOf(2) }
    val optionItems = remember(
        selectedIndex1,
        selectedIndex2,
        selectedIndex3,
        s,
    ) {
        listOf(
            DropdownEntry(
                items = listOf(s[Str.SelectionA1], s[Str.SelectionA2])
                    .mapIndexed { index, text ->
                        DropdownItem(
                            text = text,
                            selected = selectedIndex1 == index,
                            onClick = { selectedIndex1 = index },
                        )
                    },
            ),
            DropdownEntry(
                items = listOf(s[Str.SelectionB1], s[Str.SelectionB2], s[Str.SelectionB3])
                    .mapIndexed { index, text ->
                        DropdownItem(
                            text = text,
                            selected = selectedIndex2 == index,
                            onClick = { selectedIndex2 = index },
                        )
                    },
            ),
            DropdownEntry(
                items = listOf(s[Str.SelectionC1], s[Str.SelectionC2], s[Str.SelectionC3], s[Str.SelectionC4])
                    .mapIndexed { index, text ->
                        DropdownItem(
                            text = text,
                            selected = selectedIndex3 == index,
                            onClick = { selectedIndex3 = index },
                        )
                    },
            ),
        )
    }
    var cascadingSortIndex by remember { mutableIntStateOf(0) }
    var cascadingViewIndex by remember { mutableIntStateOf(0) }
    var cascadingFilterIndex by remember { mutableIntStateOf(0) }
    var cascadingCollapseOnSelection by remember { mutableStateOf(false) }
    val cascadingEntries = remember(
        cascadingSortIndex,
        cascadingViewIndex,
        cascadingFilterIndex,
        cascadingCollapseOnSelection,
        s,
    ) {
        val sortLabels = listOf(s[Str.SortByCaptureDate], s[Str.SortByDateAdded])
        val viewLabels = listOf(s[Str.GroupByDate], s[Str.Compact])
        val filterLabels = listOf(s[Str.AllItems], s[Str.CameraAlbum])
        listOf(
            DropdownEntry(
                items = sortLabels.mapIndexed { idx, label ->
                    DropdownItem(
                        text = label,
                        selected = cascadingSortIndex == idx,
                        onClick = { cascadingSortIndex = idx },
                    )
                },
            ),
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = s[Str.CollapseOnSelection],
                        selected = cascadingCollapseOnSelection,
                        onClick = {
                            cascadingCollapseOnSelection = !cascadingCollapseOnSelection
                        },
                    ),
                    DropdownItem(
                        text = s[Str.ViewMode],
                        children = viewLabels.mapIndexed { idx, label ->
                            DropdownItem(
                                text = label,
                                selected = cascadingViewIndex == idx,
                                onClick = { cascadingViewIndex = idx },
                            )
                        },
                    ),
                    DropdownItem(
                        text = s[Str.Filter],
                        children = filterLabels.mapIndexed { idx, label ->
                            DropdownItem(
                                text = label,
                                selected = cascadingFilterIndex == idx,
                                onClick = { cascadingFilterIndex = idx },
                            )
                        },
                    ),
                ),
            ),
        )
    }
    // The selection is tracked by flat index instead of by label: labels are translated, so a
    // language switch must not lose which entries are selected. Indices 0/3/4 = A-1, B-2, B-3.
    var multiSelectedIndices by remember { mutableStateOf(setOf(0, 3, 4)) }
    val multiSelectItems = remember(multiSelectedIndices, s) {
        var flatIndex = 0
        MultiSelectKeys.map { group ->
            DropdownEntry(
                items = group.map { key ->
                    val index = flatIndex++
                    DropdownItem(
                        text = s[key],
                        selected = index in multiSelectedIndices,
                        onClick = {
                            multiSelectedIndices =
                                if (index in multiSelectedIndices) {
                                    multiSelectedIndices - index
                                } else {
                                    multiSelectedIndices + index
                                }
                        },
                    )
                },
            )
        }
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive, topAppBarScrollBehavior) {
                AdaptiveTopAppBar(
                    title = s[Str.Home],
                    showTopAppBar = appState.showTopAppBar,
                    isWideScreen = isWideScreen,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    actions = {
                        TooltipBox(text = s[Str.Options]) {
                            OverlayIconCascadingDropdownMenu(
                                entries = cascadingEntries,
                                collapseOnSelection = cascadingCollapseOnSelection,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Tune,
                                    contentDescription = s[Str.Tune],
                                )
                            }
                        }
                        TooltipBox(text = s[Str.Sort]) {
                            OverlayIconDropdownMenu(
                                entries = optionItems,
                                collapseOnSelection = false,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Sort,
                                    contentDescription = s[Str.Sort],
                                )
                            }
                        }
                        TooltipBox(text = s[Str.SelectAll]) {
                            OverlayIconDropdownMenu(
                                entries = multiSelectItems,
                                collapseOnSelection = false,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.SelectAll,
                                    contentDescription = s[Str.SelectAll2],
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        val contentPadding = pageContentPadding(innerPadding, padding, isWideScreen)
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.pageScrollModifiers(
                    appState.enableScrollEndHaptic,
                    appState.showTopAppBar,
                    topAppBarScrollBehavior,
                ),
                contentPadding = contentPadding,
            ) {
                item(key = "searchbar") {
                    SmallTitle(text = "SearchBar")
                    SearchBar(
                        modifier = Modifier.padding(bottom = 12.dp),
                        inputField = {
                            InputField(
                                query = searchValue,
                                onQueryChange = { searchValue = it },
                                onSearch = { expanded = false },
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                label = s[Str.Search],
                            )
                        },
                        outsideEndAction = {
                            Text(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                        onClick = onCancelSearch,
                                    ),
                                text = s[Str.Cancel],
                                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                                color = MiuixTheme.colorScheme.primary,
                            )
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        Column {
                            repeat(4) { idx ->
                                val resultText = s.format(Str.SuggestionIndex, idx)
                                BasicComponent(
                                    title = resultText,
                                    onClick = {
                                        searchValue = resultText
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                if (notExpanded) {
                    basicComponentSection()
                    switchSection()
                    checkboxSection()
                    radioButtonSection()
                    buttonSection()
                    tabRowSection()
                    breadcrumbBarSection()
                    arrowSection()
                    dialogSection()
                    bottomSheetSection()
                    dropdownSection()
                    spinnerSection()
                    snackbarSection(snackbarHostState)
                    textFieldSection()
                    progressIndicatorSection()
                    sliderSection()
                    cardSection()
                    tooltipSection()
                    badgeSection()
                    numberPickerSection()
                    colorPickerSection()
                    blurSection()
                    otherPageSection()
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(lazyListState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                trackPadding = contentPadding,
            )
        }
    }
}
