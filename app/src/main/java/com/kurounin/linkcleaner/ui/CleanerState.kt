package com.kurounin.linkcleaner.ui

import com.kurounin.linkcleaner.logic.CleanResult

data class CleanerState(
    val input: String = "",
    val isCleaning: Boolean = false,
    val result: CleanResult? = null
)
