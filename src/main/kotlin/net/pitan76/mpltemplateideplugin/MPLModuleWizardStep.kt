package net.pitan76.mpltemplateideplugin

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.pitan76.mpltemplateideplugin.util.Lang
import net.pitan76.mpltemplateideplugin.util.MPLVersionUtil
import net.pitan76.mpltemplateideplugin.util.ProjectConfig
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MPLModuleWizardStep(private val builder: MPLModuleBuilder) : ModuleWizardStep() {

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

    private val licenseCombo = ComboBox<String>().apply {
        addItem("MIT")
        addItem("Apache-2.0")
        addItem("GPL-3.0")
        addItem("LGPL-3.0")
        addItem("MPL-2.0")
        addItem("CC0-1.0")
        addItem("All Rights Reserved")
        addItem("Unlicense")
        addItem("Custom")
        selectedItem = "MIT"
    }

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

    // クラス名がユーザーにより手動変更されたかどうか
    private var isClassNameManuallyChanged = false

    init {
        setupVersionFetching()

        modIdField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                handleChange()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                handleChange()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                handleChange()
            }
        })

        mavenGroupField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                if (isHandleChanging) return
                handleChange()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                if (isHandleChanging) return
                handleChange()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                if (isHandleChanging) return
                handleChange()
            }
        })

        classNameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                if (isHandleChanging) return
                isClassNameManuallyChanged = true
            }

            override fun removeUpdate(e: DocumentEvent?) {
                if (isHandleChanging) return
                isClassNameManuallyChanged = true
            }

            override fun changedUpdate(e: DocumentEvent?) {
                if (isHandleChanging) return
                isClassNameManuallyChanged = true
            }
        })
    }

    private var isHandleChanging = false
    private fun handleChange() {
        isHandleChanging = true

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

        isHandleChanging = false
    }

    override fun getComponent(): JComponent {
        return panel {
            group(Lang.get("label.basicsettings")) {
                row("Mod ID:") { cell(modIdField) }
                row("Mod Name:") { cell(modNameField) }
                row("Version:") { cell(modVersionField) }
                row("Maven Group:") { cell(mavenGroupField) }
                row("Package Name:") { cell(packageNameField) }
                row("Class Name:") { cell(classNameField) }
                row("Authors:") { cell(authorsField) }
                row("Description:") { scrollCell(descriptionArea) }
                row("License:") { cell(licenseCombo) }
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val versions = versionFetcher.getVersions(selectedVersion)

                SwingUtilities.invokeLater {
                    mcpitanlibVersionCombo.removeAllItems()
                    versions.forEach { mcpitanlibVersionCombo.addItem(it) }

                    if (mcpitanlibVersionCombo.itemCount == 0) {
                        mcpitanlibVersionCombo.addItem(Lang.get("label.failedfetching"))
                        return@invokeLater
                    }
                    mcpitanlibVersionCombo.selectedItem = versions.lastOrNull()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    mcpitanlibVersionCombo.removeAllItems()
                    mcpitanlibVersionCombo.addItem(Lang.get("label.failedfetching"))
                }
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
            license = licenseCombo.selectedItem as String,
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

        if ((mcpitanlibVersionCombo.selectedItem as? String).isNullOrBlank() || mcpitanlibVersionCombo.selectedItem == Lang.get("label.failedfetching")) {
            JOptionPane.showMessageDialog(null, Lang.get("message.failedfetchmcpitanlib"))
            return false
        }
        return true
    }
}