package com.megakotl.web

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.megakotl.workspace.WorkspaceScreen
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val container = document.getElementById("app") ?: error("HTML root #app is missing")
    ComposeViewport(container) {
        MaterialTheme { WorkspaceScreen() }
    }
}
