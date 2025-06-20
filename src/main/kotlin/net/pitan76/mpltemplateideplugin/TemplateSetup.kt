package net.pitan76.mpltemplateideplugin

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.intellij.openapi.util.io.FileUtil
import net.pitan76.mpltemplateideplugin.util.Lang
import net.pitan76.mpltemplateideplugin.util.ProjectConfig
import net.pitan76.mpltemplateideplugin.util.generateLicense
import java.io.File

class TemplateSetup(private val project: Project) {

    fun configure(config: ProjectConfig) {
        ApplicationManager.getApplication().runWriteAction {
            configureFromPath(project.basePath!!, config)
        }
    }

    fun configureFromPath(basePath: String, config: ProjectConfig) {
        try {
            updateGradleProperties(basePath, config)
            updateFabricModJson(basePath, config)
            updateNeoForgeModsToml(basePath, config)
            updateForgeModsToml(basePath, config)
            updateForgeBuildGradle(basePath, config)
            renamePackages(basePath, config)
            updateMainClass(basePath, config)
            renameMixins(basePath, config)
            renameAssetsAndData(basePath, config)
            updatePackMcmeta(basePath, config)

            generateLicense(
                basePath,
                config.license,
                config.authors
            )

            val gitDir = File(basePath, ".git")
            if (!gitDir.exists()) {
                ProcessBuilder("git", "init")
                    .directory(File(basePath))
                    .inheritIO()
                    .start()
                    .waitFor()
            }

            Notification(
                "TemplateMod",
                "MCPitanLib",
                Lang.get("notification.success"),
                NotificationType.INFORMATION
            ).notify(project)

        } catch (e: Exception) {
            Notification(
                "TemplateMod",
                "MCPitanLib",
                Lang.get("notification.error", e.message ?: "Unknown error"),
                NotificationType.ERROR
            ).notify(project)
        }
    }

    private fun updateMainClass(basePath: String, config: ProjectConfig) {
        val platforms = listOf("common", "fabric", "forge", "neoforge")
        platforms.forEach { platform ->
            val srcDir = File(basePath, "$platform/src/main/java")
            if (srcDir.exists()) {
                srcDir.walkTopDown().forEach { file ->
                    if (file.isFile && file.extension == "java") {
                        var content = file.readText()

                        content = content.replace("ExampleMod", config.className)
                            .replace("ExampleModFabric", "${config.className}Fabric")
                            .replace("ExampleModForge", "${config.className}Forge")
                            .replace("ExampleModNeoForge", "${config.className}NeoForge")
                            .replace("\"examplemod\"", "\"${config.modId}\"")
                            .replace("\"Example Mod\"", "\"${config.modName}\"")

                        val newFileName = when {
                            file.name == "ExampleMod.java" -> "${config.className}.java"
                            file.name == "ExampleModFabric.java" -> "${config.className}Fabric.java"
                            file.name == "ExampleModForge.java" -> "${config.className}Forge.java"
                            file.name == "ExampleModNeoForge.java" -> "${config.className}NeoForge.java"
                            else -> file.name
                        }

                        val newFile = File(file.parentFile, newFileName)
                        file.delete()
                        newFile.writeText(content)
                    }
                }
            }
        }
    }

    private fun updateGradleProperties(basePath: String, config: ProjectConfig) {
        val file = File(basePath, "gradle.properties")
        if (!file.exists()) return

        val content = file.readText()
        val newContent = content
            .replace("archives_base_name=examplemod", "archives_base_name=${config.modId}")
            .replace("mod_id=examplemod", "mod_id=${config.modId}")
            .replace("mod_name=ExampleMod", "mod_name=${config.modName}")
            .replace("mod_version=1.0.0", "mod_version=${config.modVersion}")
            .replace("maven_group=net.pitan76", "maven_group=${config.mavenGroup}")

        file.writeText(newContent)
    }

    private fun updateFabricModJson(basePath: String, config: ProjectConfig) {
        val file = File(basePath, "fabric/src/main/resources/fabric.mod.json")
        if (!file.exists()) return

        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonObject = gson.fromJson(file.readText(), JsonObject::class.java)

        jsonObject.addProperty("description", config.description)
        jsonObject.add("authors", gson.toJsonTree(config.authors.split(",")))
        jsonObject.addProperty("icon", "assets/${config.modId}/icon.png")
        jsonObject.addProperty("license", config.license)

        // entrypoints.main[] の更新
        val mainEntrypoints = jsonObject.getAsJsonObject("entrypoints").getAsJsonArray("main")

        for (i in mainEntrypoints.size() - 1 downTo 0) {
            val entry = mainEntrypoints[i].asString
            if (entry.startsWith("net.pitan76.examplemod.fabric.")) {
                mainEntrypoints.remove(i)
            }
        }

        mainEntrypoints.add(config.packageName + ".fabric." + config.className + "Fabric")

        // mixins[] の更新
        val mixins = jsonObject.getAsJsonArray("mixins")
        for (i in mixins.size() - 1 downTo 0) {
            val mixin = mixins[i].asString
            if (mixin.startsWith("examplemod")) {
                mixins.remove(i)
            }
        }
        mixins.add("${config.modId}.mixins.json")
        mixins.add("${config.modId}-common.mixins.json")

        file.writeText(gson.toJson(jsonObject))
    }

    private fun updateNeoForgeModsToml(basePath: String, config: ProjectConfig) {
        val file = File(basePath, "neoforge/src/main/resources/META-INF/neoforge.mods.toml")
        if (!file.exists()) return

        val content = file.readText()
        val newContent = content
            .replace("authors = \"Pitan\"", "authors = \"${config.authors}\"")
            .replace("logoFile = \"assets/examplemod/icon.png\"", "logoFile = \"assets/${config.modId}/icon.png\"")
            .replace("description = '''\n\n'''", "description = '''\n${config.description}\n'''")
            .replace("config = \"examplemod-common.mixins.json\"", "config = \"${config.modId}-common.mixins.json\"")
            .replace("config = \"examplemod.mixins.json\"", "config = \"${config.modId}.mixins.json\"")
            .replace("license = \"MIT\"", "license = \"${config.license}\"")

        file.writeText(newContent)

        val file2 = File(basePath, "neoforge/src/main/resources/META-INF/mods.toml")
        if (!file2.exists()) return

        val content2 = file2.readText()
        val newContent2 = content2
            .replace("authors = \"Pitan\"", "authors = \"${config.authors}\"")
            .replace("logoFile = \"assets/examplemod/icon.png\"", "logoFile = \"assets/${config.modId}/icon.png\"")
            .replace("description = '''\n\n'''", "description = '''\n${config.description}\n'''")
            .replace("config = \"examplemod-common.mixins.json\"", "config = \"${config.modId}-common.mixins.json\"")
            .replace("config = \"examplemod.mixins.json\"", "config = \"${config.modId}.mixins.json\"")
            .replace("license = \"MIT\"", "license = \"${config.license}\"")

        file2.writeText(newContent2)

    }

    private fun updateForgeModsToml(basePath: String, config: ProjectConfig) {
        val file = File(basePath, "forge/src/main/resources/META-INF/mods.toml")
        if (!file.exists()) return

        val content = file.readText()
        val newContent = content
            .replace("authors = \"Pitan\"", "authors = \"${config.authors}\"")
            .replace("logoFile = \"assets/examplemod/icon.png\"", "logoFile = \"assets/${config.modId}/icon.png\"")
            .replace("description = '''\n\n'''", "description = '''\n${config.description}\n'''")
            .replace("license = \"MIT\"", "license = \"${config.license}\"")

        file.writeText(newContent)
    }

    private fun updateForgeBuildGradle(basePath: String, config: ProjectConfig) {
        val file = File(basePath, "forge/build.gradle")
        if (!file.exists()) return

        val content = file.readText()
        val newContent = content
            .replace("mixinConfig \"examplemod-common.mixins.json\"", "mixinConfig \"${config.modId}-common.mixins.json\"")
            .replace("mixinConfig \"examplemod.mixins.json\"", "mixinConfig \"${config.modId}.mixins.json\"")

        file.writeText(newContent)
    }

    private fun renamePackages(basePath: String, config: ProjectConfig) {
        val oldPackage = "net.pitan76.examplemod"
        val newPackage = config.packageName

        val platforms = listOf("common", "fabric", "forge", "neoforge")

        platforms.forEach { platform ->
            val srcDir = File(basePath, "$platform/src/main/java")
            if (srcDir.exists()) {
                renamePackageDirectory(srcDir, oldPackage, newPackage)
            }
        }
    }

    private fun renamePackageDirectory(srcDir: File, oldPackage: String, newPackage: String) {
        val oldPath = oldPackage.replace(".", "/")
        val newPath = newPackage.replace(".", "/")

        val oldPackageDir = File(srcDir, oldPath)
        val newPackageDir = File(srcDir, newPath)

        if (oldPackageDir.exists()) {
            newPackageDir.parentFile.mkdirs()

            oldPackageDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val content = file.readText()
                    val newContent = content.replace(oldPackage, newPackage)

                    val relativePath = file.relativeTo(oldPackageDir)
                    val newFile = File(newPackageDir, relativePath.path)
                    newFile.parentFile.mkdirs()
                    newFile.writeText(newContent)
                }
            }

            oldPackageDir.deleteRecursively()
            removeEmptyDirs(srcDir, oldPackageDir.parentFile)
        }
    }

    private fun removeEmptyDirs(baseDir: File, dir: File) {
        var current = dir

        while (!FileUtil.filesEqual(current, baseDir) && current.delete())
            current = current.parentFile
    }

    private fun renameMixins(basePath: String, config: ProjectConfig) {
        val platforms = listOf("common", "fabric", "forge", "neoforge")
        platforms.forEach { platform ->
            val mixinFile = File(basePath, "$platform/src/main/resources/examplemod.mixins.json")
            val newMixinFile = File(basePath, "$platform/src/main/resources/${config.modId}.mixins.json")

            if (mixinFile.exists()) {
                val content = mixinFile.readText()
                val newContent = content.replace("net.pitan76.examplemod", config.packageName)

                mixinFile.delete()
                newMixinFile.writeText(newContent)
            }

            val commonMixinFile = File(basePath, "$platform/src/main/resources/examplemod-common.mixins.json")
            val newCommonMixinFile = File(basePath, "$platform/src/main/resources/${config.modId}-common.mixins.json")
            if (commonMixinFile.exists()) {
                val content = commonMixinFile.readText()
                val newContent = content.replace("net.pitan76.examplemod", config.packageName)

                commonMixinFile.delete()
                newCommonMixinFile.writeText(newContent)
            }
        }
    }

    private fun renameAssetsAndData(basePath: String, config: ProjectConfig) {
        val commonResources = File(basePath, "common/src/main/resources")
        if (commonResources.exists()) {
            val assetsDir = File(commonResources, "assets/examplemod")
            val dataDir = File(commonResources, "data/examplemod")

            if (assetsDir.exists()) {
                assetsDir.renameTo(File(commonResources, "assets/${config.modId}"))
            }
            if (dataDir.exists()) {
                dataDir.renameTo(File(commonResources, "data/${config.modId}"))
            }
        }
    }

    private fun updatePackMcmeta(basePath: String, config: ProjectConfig) {
        val platforms = listOf("forge", "neoforge")
        platforms.forEach { platform ->
            val file = File(basePath, "$platform/src/main/resources/pack.mcmeta")
            if (file.exists()) {
                val content = file.readText()
                val newContent = content
                    .replace("examplemod", config.modId)
                    .replace("Example Mod", config.modName)

                file.writeText(newContent)
            }
        }
    }
}