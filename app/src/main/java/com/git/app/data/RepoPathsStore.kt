package com.git.app.data

import android.content.Context
import android.content.SharedPreferences

object RepoPathsStore {
    private const val PREFS_NAME = "repo_paths"
    private const val KEY = "paths"

    fun getPaths(context: Context): Set<String> {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getStringSet(KEY, emptySet()) ?: emptySet()
    }

    fun addPath(context: Context, path: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(path)
        sp.edit().putStringSet(KEY, set).apply()
    }

    fun removePath(context: Context, path: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.remove(path)
        sp.edit().putStringSet(KEY, set).apply()
    }
}
