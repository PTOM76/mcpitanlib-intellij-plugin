package net.pitan76.mpltemplateideplugin

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File

class TemplateConfigurator(private val project: Project) {

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
            renamePackages(basePath, config)
            updateMainClass(basePath, config)
            renameMixins(basePath, config)

            generateLicense(
                config.license,
                config.authors
            )

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
        // プロジェクトのディレクトリを探索し、関連するクラス名を更新
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

    // 他のメソッドは前回と同じ...
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

        file.writeText(newContent)

        val file2 = File(basePath, "neoforge/src/main/resources/META-INF/forge.mods.toml")
        if (!file2.exists()) return

        val content2 = file2.readText()
        val newContent2 = content2
            .replace("authors = \"Pitan\"", "authors = \"${config.authors}\"")
            .replace("logoFile = \"assets/examplemod/icon.png\"", "logoFile = \"assets/${config.modId}/icon.png\"")
            .replace("description = '''\n\n'''", "description = '''\n${config.description}\n'''")
            .replace("config = \"examplemod-common.mixins.json\"", "config = \"${config.modId}-common.mixins.json\"")
            .replace("config = \"examplemod.mixins.json\"", "config = \"${config.modId}.mixins.json\"")

        file.writeText(newContent2)

    }

    private fun updateForgeModsToml(basePath: String, config: ProjectConfig) {
        val file = File(basePath, "forge/src/main/resources/META-INF/mods.toml")
        if (!file.exists()) return

        val content = file.readText()
        val newContent = content
            .replace("authors = \"Pitan\"", "authors = \"${config.authors}\"")
            .replace("logoFile = \"assets/examplemod/icon.png\"", "logoFile = \"assets/${config.modId}/icon.png\"")
            .replace("description = '''\n\n'''", "description = '''\n${config.description}\n'''")

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
                if (file.isFile && file.extension == "java") {
                    val content = file.readText()
                    val newContent = content.replace(oldPackage, newPackage)

                    val relativePath = file.relativeTo(oldPackageDir)
                    val newFile = File(newPackageDir, relativePath.path)
                    newFile.parentFile.mkdirs()
                    newFile.writeText(newContent)
                }
            }

            oldPackageDir.deleteRecursively()
        }
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
}