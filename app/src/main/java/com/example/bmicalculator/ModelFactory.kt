package com.example.bmicalculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Supplies each screen's inputs when Android creates its ViewModel. */
fun modelFactory(create: () -> ViewModel) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
