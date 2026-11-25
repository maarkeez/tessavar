package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.BarView
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import korlibs.math.geom.Size

class ManaBarView(
    size: Size,
    showValue: Boolean = true,
) : BarView(size, HudStyle.MANA, showValue)
