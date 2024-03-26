package de.chagemann.darfichkiffen.map

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.chagemann.darfichkiffen.R

@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    expanded: MutableState<Boolean> = remember {
        mutableStateOf(false)
    }
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, shape = MaterialTheme.shapes.medium)
            .background(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium)
            .clickable {
                expanded.value = !expanded.value
            }
            .padding(8.dp)
            .animateContentSize()
    ) {
        val maxLines = if (!expanded.value) {
            2
        } else {
            Int.MAX_VALUE
        }
        Text(
            text = text,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Preview(showBackground = true)
@Composable
private fun InfoBannerPreview() {
    MaterialTheme {
        InfoBanner(
            text = stringResource(id = R.string.lorem_ipsum_short),
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Preview(showBackground = true)
@Composable
private fun InfoBannerExpandedPreview() {
    MaterialTheme {
        InfoBanner(
            text = stringResource(id = R.string.lorem_ipsum_short),
            modifier = Modifier.padding(8.dp),
            expanded = remember { mutableStateOf(true) }
        )
    }
}

@Preview
@Composable
private fun InfoBannerShortPreview() {
    MaterialTheme {
        InfoBanner(
            text = "Short",
            modifier = Modifier.padding(8.dp)
        )
    }
}