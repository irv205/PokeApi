package com.irv205.pokeapi.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.irv205.pokeapi.core.common.toDisplayName
import com.irv205.pokeapi.domain.model.EvolutionChain
import com.irv205.pokeapi.domain.model.EvolutionNode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvolutionChainContent(
    chain: EvolutionChain,
    currentPokemonId: Int?,
    onPokemonClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stages = remember(chain) { chain.stages() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        stages.forEachIndexed { index, stage ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 12.dp,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 3,
            ) {
                stage.forEach { node ->
                    EvolutionNodeItem(
                        node = node,
                        isCurrent = node.speciesId == currentPokemonId,
                        onClick = { onPokemonClick(node.speciesId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EvolutionNodeItem(
    node: EvolutionNode,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = CircleShape,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
            border = if (isCurrent) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
        ) {
            PokemonImage(
                imageUrl = node.imageUrl,
                contentDescription = node.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
            )
        }
        Text(
            text = node.name.toDisplayName(),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        node.evolutionRequirement?.let { requirement ->
            Text(
                text = requirement,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
