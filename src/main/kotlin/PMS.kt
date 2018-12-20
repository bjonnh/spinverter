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

package net.nprod.spinverter.PMS

import java.io.FileInputStream

fun String.deduplicateSpace(): String {
    return this.replace("\\s+".toRegex(), " ")
}

class PMSSpin {
    var name: String? = null
    var specie: Int? = null
    var chemicalShift: Double? = null
    var magneticEquivalence: Int? = null
    var chemicalEquivalence: Int? = null
    var locked: Boolean? = null
    var predictedShift: Double? = null
    var predictedRange: Double? = null
    var widthLocked: Boolean? = null
    var width: Double? = null
    var responseLocked: Boolean? = null
    var response: Double? = null
}

class PMSSpinGroup {
    var name: String? = null
    var twospin: Int? = null
    var species: String? = null
    var populationLocked: Boolean? = null
    var population: Double? = null
    var spins: MutableList<PMSSpin> = mutableListOf()
    val numSpins: Int get() = this.spins.size

    fun addSpinFromText(str: String) {
        val spin = PMSSpin()
        val elements = str.replace('/', ' ').deduplicateSpace().split(" ")

        spin.name = elements[1]
        spin.specie = elements[2].toInt()
        spin.chemicalShift = elements[3].toDouble()

        val spindesc = elements[4].split('*')
        spin.magneticEquivalence = spindesc[1].toInt()
        spin.chemicalEquivalence = spindesc[2].toInt()

        spin.locked = elements[5] == "STAT=N"

        spin.predictedShift = elements[7].toDouble()

        spin.predictedRange = elements[9].toDouble()

        spin.widthLocked = elements[10] == "WIDTH(N)="

        spin.width = elements[11].toDouble()

        spin.responseLocked = elements[12] == "RESP(N)="

        spin.response = elements[13].toDouble()

        this.spins.add(spin)
    }
}

enum class CouplingType {
    J, D
}

class PMSCoupling(
    val name: String,
    val constant: Double,
    val type: CouplingType,
    val sg1: String,
    val sg2: String,
    val locked: Boolean = false,
    val predicted: Double? = null,
    val range: Double? = null
) {}

class PMSData {
    private var activeSpecies: String = ""
    var spinGroups: MutableList<PMSSpinGroup> = mutableListOf()
    private var currentSpinGroup: PMSSpinGroup? = null
    val couplings: MutableList<PMSCoupling> = mutableListOf()
    var frequency: Double? = null
    var leftPPM: Double? = null
    var rightPPM: Double? = null

    fun parseActiveSpecies(str: String) {
        this.activeSpecies = str.split(":")[1]
    }

    fun finishSpinGroup() {
        if (this.currentSpinGroup != null) {
            this.spinGroups.add(this.currentSpinGroup!!)
            this.currentSpinGroup = null
        }

    }

    fun newSpinGroupFromText(str: String) {
        val sg = PMSSpinGroup()
        val elements = str.deduplicateSpace().split(" ")
        sg.name = elements[0]

        if (elements[1] == "2*SPIN=") {
            sg.twospin = elements[2].toInt()
        } else {
            throw Exception("PMS-SpinGroup: Invalid Spin format missing 2*SPIN=")
        }

        if (elements[3].startsWith("SPECIES=")) {
            sg.species = elements[3].split('=')[1]
        } else {
            throw Exception("PMS-SpinGroup: Invalid Species definition")
        }

        if (elements[4].startsWith("POPULATION(")) {
            sg.populationLocked = elements[4] != "POPULATION(Y)="   // Pretty rough
            sg.population = elements[5].toDouble()
        } else {
            throw Exception("PMS-SpingGroup: Invalid Population definition")
        }

        this.currentSpinGroup = sg
    }

    fun addSpinFromText(str: String) {
        currentSpinGroup?.addSpinFromText(str)
    }

    fun newCouplingFromText(str: String) {

        val elements = str.deduplicateSpace().split(" ")

        this.couplings.add(
            PMSCoupling(
                name = elements[1],
                constant = elements[2].toDouble(),
                type = when (elements[3]) {
                    "J" -> CouplingType.J
                    "D" -> CouplingType.D
                    else -> throw Exception("PMS-Coupling: Unknown coupling type")
                },
                sg1 = elements[4],
                sg2 = elements[5],
                locked = elements[6] == "STAT=N",
                predicted = elements.elementAtOrNull(8)?.toDouble(),
                range = elements.elementAtOrNull(10)?.toDouble()
            )
        )
    }

    fun newParameterFromText(str: String) {

        val elements = str.deduplicateSpace().split(" ")

        when {
            elements[3].contains("FIELD") -> this.frequency = elements[1].toDouble()
            elements[3].contains("Left freq") -> this.leftPPM = elements[1].toDouble()
            elements[3].contains("Right freq") -> this.rightPPM = elements[1].toDouble()
        }
    }
}

enum class BlockType {
    Spin, Coupling, ControlParameter
}

fun parsePMS(file: FileInputStream): PMSData {
    val data = PMSData()
    var mode: BlockType? = null

    file.bufferedReader().readLines().map {
        when {
            it.startsWith("*") -> null
            it.startsWith("ACTIVE SPECIES:") -> data.parseActiveSpecies(it)
            it.startsWith("CHEMICAL SHIFTS(PPM):") -> mode = BlockType.Spin
            it.startsWith("COUPLING CONSTANTS(HZ):") -> mode = BlockType.Coupling
            it.startsWith("CONTROL PARAMETERS:") -> mode = BlockType.ControlParameter

            mode == BlockType.Spin -> when {
                it.isBlank() -> {
                    mode = null
                    data.finishSpinGroup()
                }
                it[0] == ' ' -> data.addSpinFromText(it)
                else -> {
                    data.finishSpinGroup()
                    data.newSpinGroupFromText(it)
                }
            }

            mode == BlockType.Coupling -> when {
                it.isBlank() -> {
                    mode = null
                }
                else -> data.newCouplingFromText(it)
            }

            mode == BlockType.ControlParameter -> when {
                it.isEmpty() -> {
                    mode = null
                }
                else -> data.newParameterFromText(it)
            }
            else -> null
        }
    }
    return data
}