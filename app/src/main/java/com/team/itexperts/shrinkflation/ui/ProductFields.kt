package com.team.itexperts.shrinkflation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.data.ProductCard

@Composable
fun ProductFields(
    name: String,
    brand: String,
    spec: String,
    suggestions: List<ProductCard>,
    searchField: String?,
    onNameChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onSpecChange: (String) -> Unit,
    onSelect: (ProductCard, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.field_product)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = brand,
                onValueChange = onBrandChange,
                label = { Text(stringResource(R.string.field_brand)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = spec,
                onValueChange = onSpecChange,
                label = { Text(stringResource(R.string.field_spec)) },
                modifier = Modifier.weight(1f)
            )
        }
        if (suggestions.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    suggestions.forEach { p ->
                        val label = ProductSearch.label(p, searchField)
                        TextButton(onClick = { onSelect(p, searchField) }, modifier = Modifier.fillMaxWidth()) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}
