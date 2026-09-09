package com.orderflow.autoresponder.presentation.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.orderflow.autoresponder.presentation.theme.BrandAccent
import com.orderflow.autoresponder.presentation.theme.BrandCardDark
import com.orderflow.autoresponder.presentation.theme.BrandDarkBackground
import com.orderflow.autoresponder.presentation.theme.BrandGreen
import com.orderflow.autoresponder.presentation.theme.BrandTextPrimary
import com.orderflow.autoresponder.presentation.theme.BrandTextSecondary

private const val META_GRAPH_EXPLORER_URL =
    "https://developers.facebook.com/tools/explorer/"

/**
 * A ModalBottomSheet that embeds Meta's Graph API Explorer in a WebView.
 *
 * The user navigates to the explorer, generates a token, then pastes it
 * into the token field below and taps "Use This Token".
 *
 * @param onTokenConfirmed Called with the token string when the user confirms.
 * @param onDismiss        Called when the sheet is dismissed without a token.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaTokenWebViewSheet(
    onTokenConfirmed: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val clipboardManager = LocalClipboardManager.current
    var tokenInput by remember { mutableStateOf("") }
    var isPageLoading by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandDarkBackground,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandCardDark)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Meta Graph API Explorer",
                        color = BrandTextPrimary,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = "Generate your access token, then paste it below",
                        color = BrandTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BrandTextSecondary
                    )
                }
            }

            if (isPageLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandGreen,
                    trackColor = BrandCardDark
                )
            }

            // ── WebView ────────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                MetaWebView(
                    url = META_GRAPH_EXPLORER_URL,
                    onPageStarted = { isPageLoading = true },
                    onPageFinished = { isPageLoading = false }
                )
            }

            // ── Token paste panel ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandCardDark)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Paste your generated access token here:",
                    color = BrandTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = {
                            Text(
                                "EAA...",
                                color = BrandTextSecondary.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = BrandTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        colors = customWebViewTextFieldColors(),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    // Paste from clipboard
                                    val clipboard = clipboardManager.getText()?.text ?: ""
                                    if (clipboard.isNotBlank()) tokenInput = clipboard
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Paste from clipboard",
                                    tint = BrandAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { if (tokenInput.isNotBlank()) onTokenConfirmed(tokenInput.trim()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = tokenInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Use This Token ✓", color = BrandTextPrimary)
                }
            }
        }
    }
}

// ── Internal WebView composable ───────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MetaWebView(
    url: String,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString =
                        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onPageStarted()
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageFinished()
                    }
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun customWebViewTextFieldColors() =
    androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BrandAccent,
        unfocusedBorderColor = BrandTextSecondary.copy(alpha = 0.4f),
        focusedLabelColor = BrandAccent,
        unfocusedLabelColor = BrandTextSecondary,
        focusedTextColor = BrandTextPrimary,
        unfocusedTextColor = BrandTextPrimary,
        cursorColor = BrandAccent
    )
