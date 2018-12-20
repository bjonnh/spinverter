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

package net.nprod.spinverter.MMS

import java.io.FileInputStream


data class VField(
    // Looks like a header for the file
    val type: String,
    val keys: List<Int>, // four keys (license?)
    val atom_vector: Int,
    val bond_vector: Int,
    val cg_vector: Int,
    val cp_vector: Int
)

fun vFieldFromString(line: String): VField {
    val splitLine = line.split(" ")
    return VField(
        splitLine[0],
        splitLine.subList(2,5).map { it.toInt() },
        splitLine[6].toInt(),
        splitLine[7].toInt(),
        splitLine[8].toInt(),
        splitLine[9].toInt())
}

data class DollarField(
    val num: Int
)

data class CField(
    val ext: String
)

data class FField(
    val ext: String
)

data class SField(
    val type: Int,
    val pH: Double,
    val ionic_strength: Double,
    val concentration: Double
)

data class IField(
    val ext: String
)

data class RField(
    val ext: String
)

data class TField(
    val ext: String
)

data class IACPField(
    val atom_number: Int,
    val atomic_number: Int,
    val coupling_data: List<Double> // Doesn't seem useful, it is redundant with data later in the file
)

data class AtomNameField(
    val atom_number: Int,
    val atomic_number: Int,
    val name: String
)

fun atomNameFromString(line: String): AtomNameField{
    val splitLine = line.split(" ")
    return AtomNameField(
        splitLine[3].toInt(),
        splitLine[4].toInt(),
        splitLine[5].removeSurrounding("|")
    )
}

data class MagneticGroup(
    val atom_number: Int,
    val predictedShift: Double?,
    val predictedRange: Double?
)

data class SpinGroupField(
    val name: String,
    val observedShift: Double?,
    val predictedShift: Double?,
    val lineWidth: Double,
    val decoupling: Int,
    val suppressed: Int,
    val reliability: Double,
    val range: Double,
    val rangeLock: Int,
    val groupWeight: Int,
    val numMagneticGroups: Int,
    val magneticGroups: List<List<MagneticGroup>>,
    val index: Int
)

fun chemicalShiftCleaner(str: String): Double? {
    return when {
        str =="-1e+012" -> null
        else -> str.toDouble()
    }
}

fun parseMagneticGroups(residualLine: String): List<List<MagneticGroup>> {
    // Lines are of the format
    // n [ m [ a b c a' b' c' … ] m' [ a b c …] ]

    val cleanedLine = residualLine.replace("\\s+".toRegex(), " ")

    return cleanedLine.split(" ").let {elements ->
        val numGroups = elements[0].toInt()
        var index = 2  // This is the index in the
        numGroups.downTo(1).map {
            val subGroups = elements[index].toInt()
            index += 2
            subGroups.downTo(1).map {
                MagneticGroup(elements[index].toInt(),
                    chemicalShiftCleaner(elements[index+1]),
                    chemicalShiftCleaner(elements[index+2])).also { index += 3}
            }.also { index += 1}
        }
    }
}

fun spinGroupFromString(line: String, index: Int): SpinGroupField {
    val splitLine = line.split(" ")
    return SpinGroupField(
        name = splitLine[2].removeSurrounding("|"),
        observedShift = chemicalShiftCleaner(splitLine[3]),
        predictedShift = chemicalShiftCleaner(splitLine[4]),
        lineWidth = splitLine[5].toDouble(),
        decoupling = splitLine[6].toInt(),
        suppressed = splitLine[7].toInt(),
        reliability = splitLine[8].toDouble(),
        range = splitLine[9].toDouble(),
        rangeLock = splitLine[10].toInt(),
        groupWeight = splitLine[11].toInt(),
        numMagneticGroups = splitLine[12].toInt(),
        magneticGroups = parseMagneticGroups(splitLine.subList(12, splitLine.size).joinToString(" ")),
        index = index
    )
}

data class CouplingGroupField(
    val type: Int,
    val mg1: Int,
    val mg1_index: Int,
    val mg2: Int,
    val mg2_index: Int,
    val group_id: Int,
    val bond_length: Int,
    val observed_coupling: Double,
    val predicted_coupling: Double,
    val prediction_quality: Int
)

fun couplingGroupFromString(line: String): CouplingGroupField {
    val splitLine = line.split(" ")
    return CouplingGroupField(
        type = splitLine[2].toInt(),
        mg1 = splitLine[3].toInt(),
        mg1_index = splitLine[4].toInt(),
        mg2 = splitLine[5].toInt(),
        mg2_index = splitLine[6].toInt(),
        group_id = splitLine[7].toInt(),
        bond_length = splitLine[8].toInt(),
        observed_coupling = splitLine[9].toDouble(),
        predicted_coupling = splitLine[10].toDouble(),
        prediction_quality = splitLine[11].toInt()
    )
}

data class AtomField(
    val atomic_number: Int,
    val charge: Double,
    val formal_charge: Int,
    val t1: Int,
    val t2: Int,
    val selected: Int,
    val ch2: Int,
    val u1: Int,
    val u2: Int,
    val u3: Int,
    val charge_lock: Int,
    val position_lock: Int,
    val x: Double,
    val y: Double,
    val z: Double
)

fun atomFromString(line: String): AtomField {
    val splitLine = line.split(" ")
    return AtomField(
        atomic_number = splitLine[1].toInt(),
        charge = splitLine[2].toDouble(),
        formal_charge = splitLine[3].toInt(),
        t1 = splitLine[4].toInt(),
        t2 = splitLine[5].toInt(),
        selected = splitLine[6].toInt(),
        ch2 = splitLine[7].toInt(),
        u1 = splitLine[8].toInt(),
        u2 = splitLine[9].toInt(),
        u3 = splitLine[10].toInt(),
        charge_lock = splitLine[11].toInt(),
        position_lock = splitLine[12].toInt(),
        x = splitLine[13].toDouble(),
        y = splitLine[14].toDouble(),
        z = splitLine[15].toDouble()
    )
}

data class BondField(
    val a1: Int,
    val a2: Int,
    val type: String,
    val v1: Int  // Seems to always be 0?
)

fun bondFromString(line: String): BondField {
    val splitLine = line.split(" ")
    return BondField(
        a1 = splitLine[1].toInt(),
        a2 = splitLine[2].toInt(),
        type = splitLine[3],
        v1 = splitLine[4].toInt()
    )
}

data class MMSFile(
    var vField: VField? = null,  // handled
    var dollarField: DollarField? = null,
    var cField: CField? = null,
    var fField: FField? = null,
    var sField: SField? = null,
    var iField: IField? = null,
    var rField: RField? = null,
    var tField: TField? = null,
    var atoms: MutableList<AtomField> = mutableListOf(),
    var bonds: MutableList<BondField> = mutableListOf(),
    val atomNames: MutableList<AtomNameField> = mutableListOf(),  // handled
    var iacps: MutableList<IACPField> = mutableListOf(),
    var spinGroups: MutableList<SpinGroupField> = mutableListOf(), // handled
    var couplingGroups: MutableList<CouplingGroupField> = mutableListOf() // handled
)


fun parseMMS(inputStream: FileInputStream): MMSFile {
    val mmsFile = MMSFile()
    inputStream.bufferedReader().useLines { lines ->
        lines.forEach {
            when {
                it.startsWith('V') -> mmsFile.vField = vFieldFromString(it)
                it.startsWith("I A NAME") -> mmsFile.atomNames.add(atomNameFromString(it))
                it.startsWith("N G") -> mmsFile.spinGroups.add(spinGroupFromString(it, index=mmsFile.spinGroups.size))
                it.startsWith("N C") -> mmsFile.couplingGroups.add(couplingGroupFromString(it))
                it.startsWith("A") -> mmsFile.atoms.add(atomFromString(it))
                it.startsWith("B") -> mmsFile.bonds.add(bondFromString(it))
            }
        }
    }
    return mmsFile
}