package com.mkz.tessavar.screen.adapters.presentation

fun EffectInfoView.title(): String = nameText.text

fun EffectInfoView.sentence(): String = sentenceWords.joinToString(" ") { word -> word.text }

fun EffectInfoView.remainingDurations(): String? = remainingDurationsText?.text
