package net.pitan76.mpltemplateideplugin

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import net.pitan76.mpltemplateideplugin.util.Lang
import net.pitan76.mpltemplateideplugin.util.ProjectConfig
import net.pitan76.mpltemplateideplugin.util.TemplateDownloader
import javax.swing.Icon

class MPLModuleBuilder : ModuleBuilder() {

    private val repoName = "Pitan76/TemplateMod-for-MCPitanLib"

    private var projectConfig: ProjectConfig? = null

    override fun getModuleType(): ModuleType<*> = StdModuleTypes.JAVA

    override fun getPresentableName(): String = "MCPitanLib"

    override fun getDescription(): String = Lang.get("message.modulebuilder.description")

    override fun getGroupName(): String = "Minecraft"

    override fun getBuilderId(): String = "MCPitanLib"

    override fun getNodeIcon(): Icon? {
        return IconLoader.getIcon("/icons/mcpitanlib.png", MPLModuleBuilder::class.java)
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val contentEntry = doAddContentEntry(modifiableRootModel) ?: return
        val root = contentEntry.file ?: return

        // プロジェクトのテンプレートを生成
        val config = projectConfig ?: return
        generateProjectFromTemplate(root, config, modifiableRootModel.project)
    }

    override fun getCustomOptionsStep(context: WizardContext, parentDisposable: Disposable): ModuleWizardStep? {
        return MPLModuleWizardStep(this)
    }

    fun setProjectConfig(config: ProjectConfig) {
        this.projectConfig = config
    }

    private fun generateProjectFromTemplate(root: VirtualFile, config: ProjectConfig, project: com.intellij.openapi.project.Project) {
        try {
            val templateDownloader = TemplateDownloader()
            templateDownloader.downloadTemplate(root.path, repoName)

            val configurator = TemplateSetup(project)
            configurator.configureFromPath(root.path, config)
        } catch (e: Exception) {
            throw RuntimeException("${Lang.get("message.failedgenerate")}: ${e.message}", e)
        }
    }
}