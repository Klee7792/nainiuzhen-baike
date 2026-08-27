package com.harvesttown.encyclopedia.ui.npc

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.data.model.NpcInfo
import com.harvesttown.encyclopedia.ui.components.MenuCard
import com.harvesttown.encyclopedia.ui.components.NpcPortraitImage
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.Route
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * NPC 资料列表：每项 [MenuCard] 展示立绘 / 名称 / 住址·生日。点击弹出 [NpcDetailScreen] 详情。
 */
@Composable
fun NpcListScreen() {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val scrollBehavior = MiuixScrollBehavior()
    var selected by remember { mutableStateOf<NpcInfo?>(null) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "NPC 资料",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 12.dp),
        ) {
            items(data.npcs, key = { it.id }) { npc ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    MenuCard(
                        title = npc.name,
                        summary = "${npc.address} · ${npc.birthday}",
                        startContent = {
                            NpcPortraitImage(npcId = npc.id, modifier = Modifier.size(48.dp))
                        },
                        onClick = { selected = npc },
                    )
                }
            }
        }
        NpcDetailScreen(npc = selected, onDismissRequest = { selected = null })
    }
}
