package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.PreviewBarView
import korlibs.image.color.RGBA
import korlibs.math.geom.Size

class ManaBarPreviewView(
    size: Size,
) : PreviewBarView(
        size = size,
        filledColor = HudStyle.MANA,
        previewColor = RGBA(0x96, 0xCD, 0xFF),
    )
