/*
Copyright 2022 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package dev.deeds.irfilegenerator

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.refresh
import com.github.mvysny.kaributools.setPrimary
import com.github.mvysny.kaributools.tooltip
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dependency.CssImport
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.editor.Editor
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.PWA
import java.io.File;
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.demo.helloworld.MainView.DataManager.readFile

open class IrDataManager {
    data class IrCommand(val commandName: String, val protocol: String, val deviceId: Int, val functionId: Int) {
        constructor(fieldMap: Map<String, String>) : this(
            fieldMap["functionname"] ?: "UNKNOWN",
            fieldMap["protocol"] ?: "UNKNOWN",
            (fieldMap["device"] ?: "0").toInt(),
            (fieldMap["function"] ?: "0").toInt())
    }

    data class IrEntry(val indexedPath: String) {
        val dataPath = Path("cache", indexedPath)
        val vendor = indexedPath.substringBefore("/")
        val deviceName = indexedPath.substringAfter("/")
        val irCommands = mutableListOf<IrCommand>()

        fun loadData() {
            // Hitachi VCR 96,-1 has a function named '0 ?!"' which is wacky and breaks the CSV library because of the single double-quote
            val rows = csvReader() {
                escapeChar = Char(0)
            }.readAllWithHeader(dataPath.toFile())
            rows.forEach{ row -> irCommands.add(IrCommand(row)) }
        }
    }
    var indexEntries = mutableListOf<IrEntry>()
    var vendorList : SortedSet<String> = sortedSetOf<String>()
    var selectedVendor : String? = null
    val guessedIrCommands = mutableListOf<IrCommand>()

    init {
        println("Initializing IrDataManager")
        fetchFile("index")
        val indexContents = readFile("index").trim()
        indexContents.split("\n").forEach { e -> indexEntries.add(IrEntry(e)) }
        println("Index contains ${indexEntries.size} entries")
//        val downloadPool = ForkJoinPool(32)
//        indexEntries = downloadPool.submit {  ->
//            indexEntries.parallelStream().filter{ e -> fetchFile(e.indexedPath) }.collect(Collectors.toList())}.get() as MutableList<IrEntry>
        indexEntries = indexEntries.filter { e -> fetchFile(e.indexedPath) }.toMutableList()
        println("After filtering out empty entries, down to ${indexEntries.size} entries")
        vendorList = indexEntries.map { e -> e.vendor }.toSortedSet()

        indexEntries.forEach{ e -> e.loadData() }
    }


    fun getDeviceDataProvider(): ListDataProvider<IrEntry> {
        var dataProvider = ListDataProvider<IrEntry>(indexEntries)
        dataProvider.setFilter { entry ->
            if (selectedVendor != null && entry.vendor != selectedVendor) return@setFilter false
            guessedIrCommands.forEach{ guess ->
                if (!entry.irCommands.any { other ->
                    (guess.protocol == other.protocol
                        && guess.deviceId == other.deviceId
                        && guess.functionId == other.functionId)
                }) return@setFilter false
            }
            return@setFilter true
        }
        return dataProvider
    }

    fun getUrl(filename: String) : URL {
        val escapedUri = URI(
            "https", "cdn.jsdelivr.net", "/gh/probonopd/irdb@master/codes/$filename", null)
        return escapedUri.toURL()
    }

    fun fetchFile(name: String): Boolean {
        val desiredPath = Path("cache", name)
        if (!desiredPath.exists(LinkOption.NOFOLLOW_LINKS)) {
            val fileUrl = getUrl(name)
            println("Downloading $fileUrl")
            desiredPath.parent.toFile().mkdirs()
            try {
                Files.copy(fileUrl.openStream(), desiredPath)
            } catch (e: Exception) {
                return false
            }
        }
        return desiredPath.exists(LinkOption.NOFOLLOW_LINKS)
    }

    fun readFile(name: String): String {
        val desiredPath = File(File("cache"), name).toPath()
        return Files.readString(desiredPath)
    }

}


@Route("")
@CssImport("./styles/shared-styles.css")
@CssImport("./styles/vaadin-text-field-styles.css", themeFor = "vaadin-text-field")
class MainView : KComposite() {
    private lateinit var vendorField: TextField
    private lateinit var greetButton: Button
    private lateinit var vendorSelector: ComboBox<String?>
    private lateinit var addGuessButton: Button
    private lateinit var guessedCommandGrid: Grid<IrDataManager.IrCommand>
    private lateinit var deviceIdColumn: Grid.Column<IrDataManager.IrCommand>
    private lateinit var deviceList: Grid<IrDataManager.IrEntry>

    private lateinit var protocolInput: TextField
    private lateinit var deviceIdInput: TextField
    private lateinit var functionIdInput: TextField

    private lateinit var remainingPossibilities: H3
    companion object DataManager : IrDataManager()

    private val root = ui {
        verticalLayout(/*classNames = "centered-content"*/) {

            horizontalLayout {

            }

            horizontalLayout {

                text("Use your flipper to determine the protocol, device, and function values for a button. Try it with more buttons to narrow down the results")
            }

            formLayout {

                vendorSelector = comboBox("Optional: Manufacturer") {
                    setItems(vendorList)
                }

                protocolInput = textField("Protocol e.g. 'NEC'") {
                    width = "50px"
                    flexGrow = 0.0
                }
                deviceIdInput = textField("Device ID (enter the part after 'A: 0x')") {
                    prefixComponent = Span("0x")
                    width = "50px"
                    flexGrow = 0.0
                }
                functionIdInput = textField("Function ID (enter the part after 'C: 0x')") {
                    prefixComponent = Span("0x")
                    width = "50px"
                    flexGrow = 0.0
                }

//                text("(Optional) Choose the manufacturer of your remote: ")


                addGuessButton = button("Add to table") {
                    tooltip = "Add current input to the list of guesses"
                }

                button("Clear table") {
                    addClickListener { e -> guessedIrCommands.clear(); guessedCommandGrid.refresh() }
                    addThemeVariants(ButtonVariant.LUMO_ERROR)
                }
                setResponsiveSteps(
                    FormLayout.ResponsiveStep("0", 1),
                    FormLayout.ResponsiveStep("550px", 3)
                )
            }

            guessedCommandGrid = grid {
                setItems(guessedIrCommands)
                addColumn(IrDataManager.IrCommand::protocol).setHeader("Command protocol")
                deviceIdColumn = addColumn(IrDataManager.IrCommand::deviceId).setHeader("Device ID (base 10)")
                addColumn(IrDataManager.IrCommand::functionId).setHeader("Function ID (base 10)")
                setMaxHeight("200px")
            }

            remainingPossibilities = h3("Remaining possibilities:")
            deviceList = grid {
                setItems(getDeviceDataProvider())
                addColumn(IrDataManager.IrEntry::vendor).setHeader("Device manufacturer")
                addColumn(IrDataManager.IrEntry::deviceName).setHeader("Device name")
            }



            button() {
                className = "overlay-button"
                icon = Icon(VaadinIcon.QUESTION_CIRCLE)
                addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE)
                onLeftClick { InfoDialog().open() }
            }
            setWidthFull()
        }
    }

    init {
        addGuessButton.onLeftClick {
            guessedIrCommands.add(
                IrDataManager.IrCommand("", protocolInput.value, deviceIdInput.value.toInt(16), functionIdInput.value.toInt(16)))
            deviceList.dataProvider.refreshAll()
            guessedCommandGrid.refresh()
        }
        vendorSelector.addValueChangeListener { event ->
            selectedVendor = event.value
            deviceList.dataProvider.refreshAll()
            Notification.show("Filtering to show $event.val")
        }

        deviceList.listDataView.addItemCountChangeListener { e ->
            remainingPossibilities.setText("Remaining possibilities: ${e.itemCount}")
        }

    }
}


@PWA(name = "IR File Generator", shortName = "IR File Generator")
class AppShell: AppShellConfigurator
