/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import ua.acclorite.book_story.presentation.main.ExternalPdfEffect
import ua.acclorite.book_story.presentation.reader.ReaderScreen
import ua.acclorite.book_story.ui.navigator.LocalNavigator

@Composable
fun ExternalPdfEffects(effects: Flow<ExternalPdfEffect>) {
    val navigator = LocalNavigator.current
    LaunchedEffect(effects, navigator) {
        effects.collect { effect ->
            when (effect) {
                is ExternalPdfEffect.OpenBook -> {
                    navigator.push(ReaderScreen(effect.bookId))
                }
            }
        }
    }
}