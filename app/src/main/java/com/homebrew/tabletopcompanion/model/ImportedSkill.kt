package com.homebrew.tabletopcompanion.model

data class ImportedSkill(
    val name: String,
    val category: String,
    val description: String,
    val reqText: String = "",
    val requiredSkills: List<String> = emptyList(),
    val requiredStats: Map<String, Int> = emptyMap(),
    val specialReq: String = ""
)
