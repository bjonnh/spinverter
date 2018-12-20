/*
    Spinverter - Converting and working with various spin-formats
    Copyright (C) 2018 Jonathan Bisson

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.nprod.spinverter

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import javafx.stage.Stage
import net.nprod.spinverter.PMS.*
import pmsFileToMnova
import java.io.File

import tornadofx.*

class Main : App(MainView::class) {
    override fun start(stage: Stage) {
        super.start(stage)
        stage.minWidth = 1024.0
        stage.minHeight = 700.0
    }
}

class PMSController : Controller() {
    var pmsData: PMSData = PMSData()
    var status: SimpleStringProperty? = null
    var update = SimpleBooleanProperty()
    var loaded = SimpleBooleanProperty(false)

    fun load(file: File) {
        runLater { status?.set("Loading file...") }
        try {
            pmsData = parsePMS(file.inputStream())
            runLater { status?.set("Successfully loaded...") }
            update.set(true)
            loaded.set(true)
        } catch (e: Exception) {
            runLater { status?.set("Failed loading file...") }
        }
    }

    fun saveToMnova(file: File) {
        pmsFileToMnova(pmsData, file)
    }
}


class StatsView : View() {
    var spinGroups = mutableListOf<PMSSpinGroup>().observable()
    var couplings = mutableListOf<PMSCoupling>().observable()

    override val root = splitpane {
        orientation = Orientation.VERTICAL
        vbox {
            tableview(spinGroups) {
                readonlyColumn("Name", PMSSpinGroup::name)
                readonlyColumn("2*SPIN", PMSSpinGroup::twospin)
                readonlyColumn("species", PMSSpinGroup::species)
                readonlyColumn("spins", PMSSpinGroup::numSpins)
                rowExpander(expandOnDoubleClick = true) {
                    paddingLeft = expanderColumn.width

                    tableview(it.spins.observable()) {
                        readonlyColumn("Name", PMSSpin::name)
                        readonlyColumn("Chemical Shift", PMSSpin::chemicalShift)
                        readonlyColumn("Magnetic equivalent", PMSSpin::magneticEquivalence)
                        readonlyColumn("Chemical equivalent", PMSSpin::chemicalEquivalence)
                        readonlyColumn("Predicted Shift", PMSSpin::predictedShift)
                        readonlyColumn("Line Width", PMSSpin::width)
                        readonlyColumn("Response", PMSSpin::response)

                    }
                }
            }
            tableview(couplings) {
                readonlyColumn("Name", PMSCoupling::name)
                readonlyColumn("Constant", PMSCoupling::constant)
                readonlyColumn("Type", PMSCoupling::type)
                readonlyColumn("Spin 1", PMSCoupling::sg1)
                readonlyColumn("Spin 1", PMSCoupling::sg2)
                readonlyColumn("Predicted coupling", PMSCoupling::predicted)
            }
        }
    }

}

class StatusBar : View() {

    val lastMessage = SimpleStringProperty()

    override val root = hbox {
        label(lastMessage)

        padding = Insets(4.0)
        vgrow = Priority.NEVER
    }
}


class MainView : View() {
    override val root = BorderPane()

    // private val salesPeopleView: SalesPeopleView by inject()
    // private val companyClientView: CustomerView by inject()
    // private val appliedCustomerView: AppliedCustomerView by inject()

    // private val controller: EventController by inject()
    var statsView: StatsView by singleAssign()
    val pmsController: PMSController by inject()
    var statusBar: StatusBar by singleAssign()


    init {
        title = "Spinverter"

        with(root) {
            top = hbox {
                imageview {
                    image = Image(resources["/logo.png"])
                }
                label {
                    text = "Spinverter"
                    style {
                        fontSize = 6.em
                    }
                }
            }
            center = splitpane() {
                orientation = Orientation.HORIZONTAL
                vbox {
                    maxWidth = 150.0
                    button {
                        text = "Load PMS File"
                        action {
                            val fileChooser = FileChooser()
                            fileChooser.extensionFilters.addAll(
                                FileChooser.ExtensionFilter("PMS Files", "*.pms")
                            )

                            val file = fileChooser.showOpenDialog(null)
                            if (file != null) {
                                pmsController.load(file)
                            }
                        }
                    }
                    button {
                        text = "Write Mnova File"
                        enableWhen(pmsController.loaded)
                        action {
                            val fileChooser = FileChooser()
                            fileChooser.initialFileName = "spinverter.xml"
                            fileChooser.title = "Select the Mnova spinsim output file"
                            fileChooser.extensionFilters.addAll(
                                FileChooser.ExtensionFilter("XML Files", "*.xml")
                            )
                            val file = fileChooser.showSaveDialog(null)
                            if (file != null) {
                                pmsController.saveToMnova(file)
                            }
                        }
                    }
                }
                statsView = StatsView()
                pmsController.update.onChange {
                    statsView.spinGroups.removeAll()
                    statsView.spinGroups.addAll(pmsController.pmsData.spinGroups)
                    statsView.couplings.removeAll()
                    statsView.couplings.addAll(pmsController.pmsData.couplings)
                }
                add(statsView)
                setDividerPositions(0.3)
            }
            statusBar = StatusBar()
            pmsController.status = statusBar.lastMessage
            bottom = statusBar.root


            /*
                top =
                center = splitpane {
                    orientation = Orientation.HORIZONTAL
                    splitpane {
                        orientation = Orientation.VERTICAL
                        this += button { "Load a PMS file" }
                        this += button { "Something else"}
                    }
                }
                */
        }
    }

}
