package net.pitan76.mpltemplateideplugin

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MPLProjectGeneratorPanel(private val builder: MPLProjectModuleBuilder) : ModuleWizardStep() {

    private val modIdField = JTextField().apply {
        columns = 20
    }

    private val modNameField = JTextField().apply {
        columns = 20
    }

    private val modVersionField = JTextField("1.0.0").apply {
        columns = 10
    }

    private val mavenGroupField = JTextField("com.example").apply {
        columns = 30
    }

    private val packageNameField = JTextField("com.example.examplemod").apply {
        columns = 30
    }

    private val classNameField = JTextField("ExampleMod").apply {
        columns = 20
    }

    private val authorsField = JTextField().apply {
        columns = 30
    }

    private val descriptionArea = JTextArea(3, 30)
    private val minecraftVersions = listOf(
        "1.21.5", "1.21.4", "1.21.3", "1.21.1",
        "1.20.4", "1.20.1", "1.19.2", "1.18.2", "1.16.5"
    )
    private val minecraftVersionCombo = ComboBox(minecraftVersions.toTypedArray()).apply {
        selectedItem = "1.20.4"
    }
    private val mcpitanlibVersionCombo = ComboBox<String>().apply {
        addItem("3.2.8")
    }

    private val fabricCheck = JCheckBox("Fabric", true)
    private val forgeCheck = JCheckBox("Forge", true)
    private val neoforgeCheck = JCheckBox("NeoForge", true)

    private val versionFetcher = MPLVersionUtil()
    private val scope = CoroutineScope(Dispatchers.Swing)

    // クラス名がユーザーにより手動変更されたかどうか
    private var isClassNameManuallyChanged = false

    init {
        setupVersionFetching()

        // modIdField の変更を監視してクラス名とパッケージ名を同期
        modIdField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                handleModIdChange()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                handleModIdChange()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                handleModIdChange()
            }
        })

        classNameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                if (isChangingByHandleModIdChange) return
                isClassNameManuallyChanged = true
            }

            override fun removeUpdate(e: DocumentEvent?) {
                if (isChangingByHandleModIdChange) return
                isClassNameManuallyChanged = true
            }

            override fun changedUpdate(e: DocumentEvent?) {
                if (isChangingByHandleModIdChange) return
                isClassNameManuallyChanged = true
            }
        })
    }

    private var isChangingByHandleModIdChange = false
    private fun handleModIdChange() {
        isChangingByHandleModIdChange = true

        val modId = modIdField.text.trim()

        // クラス名を同期 (ただし、手動変更されていない場合だけ)
        if (!isClassNameManuallyChanged && modId.isNotEmpty()) {
            classNameField.text = modId.replaceFirstChar { it.uppercaseChar() }
        }

        // パッケージ名を同期
        val mavenGroup = mavenGroupField.text.trim()
        if (mavenGroup.isNotEmpty() && modId.isNotEmpty()) {
            packageNameField.text = "$mavenGroup.$modId".lowercase()
        }

        isChangingByHandleModIdChange = false
    }

    override fun getComponent(): JComponent {
        return panel {
            group(Lang.get("label.basicsettings")) {
                row("Mod ID:") { cell(modIdField).comment(Lang.get("comment.mod.id")) }
                row("Mod Name:") { cell(modNameField).comment(Lang.get("comment.mod.name")) }
                row("Version:") { cell(modVersionField) }
                row("Maven Group:") { cell(mavenGroupField).comment(Lang.get("comment.maven.group")) }
                row("Package Name:") { cell(packageNameField).comment(Lang.get("comment.package.name")) }
                row("Class Name:") { cell(classNameField).comment(Lang.get("comment.class.name")) }
                row("Authors:") { cell(authorsField).comment(Lang.get("comment.authors")) }
                row("Description:") { scrollCell(descriptionArea) }
            }
            group(Lang.get("label.versionsettings")) {
                row("Minecraft Version:") { cell(minecraftVersionCombo) }
                row("MCPitanLib Version:") { cell(mcpitanlibVersionCombo) }
            }
            group(Lang.get("label.platforms")) {
                row { cell(fabricCheck); cell(forgeCheck); cell(neoforgeCheck) }
            }
        }
    }

    private fun setupVersionFetching() {
        minecraftVersionCombo.addActionListener {
            fetchMCPitanLibVersions()
        }

        fetchMCPitanLibVersions()
    }

    private fun fetchMCPitanLibVersions() {
        val selectedVersion = minecraftVersionCombo.selectedItem as? String ?: return
        mcpitanlibVersionCombo.removeAllItems()
        mcpitanlibVersionCombo.addItem(Lang.get("label.fetching"))

        scope.launch {
            try {
                val versions = versionFetcher.getVersions(selectedVersion)
                mcpitanlibVersionCombo.removeAllItems()
                versions.forEach { mcpitanlibVersionCombo.addItem(it) }
                if (mcpitanlibVersionCombo.itemCount == 0) {
                    mcpitanlibVersionCombo.addItem(Lang.get("label.failedfetching"))
                } else {
                    mcpitanlibVersionCombo.selectedItem = versions.lastOrNull()
                }
            } catch (e: Exception) {
                mcpitanlibVersionCombo.removeAllItems()
                mcpitanlibVersionCombo.addItem(Lang.get("label.failedfetching"))
                e.printStackTrace()
            }
        }
    }

    override fun updateDataModel() {
        val enabledPlatforms = mutableSetOf<String>()
        if (fabricCheck.isSelected) enabledPlatforms.add("fabric")
        if (forgeCheck.isSelected) enabledPlatforms.add("forge")
        if (neoforgeCheck.isSelected) enabledPlatforms.add("neoforge")

        val config = ProjectConfig(
            modId = modIdField.text.trim(),
            modName = modNameField.text.trim(),
            modVersion = modVersionField.text.trim(),
            mavenGroup = mavenGroupField.text.trim(),
            packageName = packageNameField.text.trim(),
            className = classNameField.text.trim(),
            authors = authorsField.text.trim(),
            description = descriptionArea.text.trim(),
            minecraftVersion = minecraftVersionCombo.selectedItem as String,
            mcpitanlibVersion = mcpitanlibVersionCombo.selectedItem as String,
            enabledPlatforms = enabledPlatforms
        )

        builder.setProjectConfig(config)
    }

    override fun validate(): Boolean {
        if (modIdField.text.isBlank()) {
            JOptionPane.showMessageDialog(null, Lang.get("message.mod.id"))
            return false
        }

        if (modNameField.text.isBlank()) {
            JOptionPane.showMessageDialog(null, Lang.get("message.mod.name"))
            return false
        }

        if (modVersionField.text.isBlank()) {
            modVersionField.text = "1.0.0"
        }

        if (mavenGroupField.text.isBlank()) {
            JOptionPane.showMessageDialog(null, Lang.get("message.maven.group"))
            return false
        }

        if (packageNameField.text.isBlank()) {
            JOptionPane.showMessageDialog(null, Lang.get("message.package.name"))
            return false
        }

        if (classNameField.text.isBlank()) {
            JOptionPane.showMessageDialog(null, Lang.get("message.class.name"))
            return false
        }

        if ((mcpitanlibVersionCombo.selectedItem as? String).isNullOrBlank() || mcpitanlibVersionCombo.selectedItem == "取得失敗") {
            JOptionPane.showMessageDialog(null, "MCPitanLibバージョンの取得に失敗しました。再試行してください。")
            return false
        }
        return true
    }
}