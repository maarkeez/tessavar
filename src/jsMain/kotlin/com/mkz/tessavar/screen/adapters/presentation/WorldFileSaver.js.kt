package com.mkz.tessavar.screen.adapters.presentation

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

actual fun downloadFile(
    fileName: String,
    text: String,
): Boolean {
    val url = URL.createObjectURL(Blob(arrayOf(text), BlobPropertyBag(type = "application/toml")))
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = url
    anchor.download = fileName
    document.body?.appendChild(anchor)
    anchor.click()
    anchor.remove()
    window.setTimeout({ URL.revokeObjectURL(url) }, 0)
    return true
}
