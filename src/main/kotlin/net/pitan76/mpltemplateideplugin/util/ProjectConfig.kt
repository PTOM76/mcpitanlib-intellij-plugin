package net.pitan76.mpltemplateideplugin.util

data class ProjectConfig(
    val modId: String,
    val modName: String,
    val modVersion: String,
    val mavenGroup: String,
    val className: String,
    val packageName: String,
    val authors: String,
    val description: String,
    val license: String,
    val minecraftVersion: String,
    val mcpitanlibVersion: String,
    val enabledPlatforms: Set<String> = emptySet(),
)