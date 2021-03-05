package de.markus_thielker.streamplus.ui.views.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun settingsView() {

    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Text("This is settingsView")
    }
}