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
    val pH: Float,
    val ionic_strength: Float,
    val concentration: Float
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
    val coupling_data: List<Float> // Doesn't seem useful, it is redundant with data later in the file
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
    val predicted_shift: Float?,
    val predicted_range: Float?
)

data class SpinGroupField(
    val name: String,
    val observed_shift: Float?,
    val predicted_shift: Float?,
    val line_width: Float,
    val decoupling: Int,
    val suppressed: Int,
    val reliability: Float,
    val range: Float,
    val range_lock: Int,
    val group_weight: Int,
    val num_magnetic_groups: Int,
    val magnetic_groups: List<List<MagneticGroup>>
)

fun chemicalShiftCleaner(str: String): Float? {
    return when {
        str =="-1e+012" -> null
        else -> str.toFloat()
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

fun spinGroupFromString(line: String): SpinGroupField {
    val splitLine = line.split(" ")
    return SpinGroupField(
        name = splitLine[2].removeSurrounding("|"),
        observed_shift = chemicalShiftCleaner(splitLine[3]),
        predicted_shift = chemicalShiftCleaner(splitLine[4]),
        line_width = splitLine[5].toFloat(),
        decoupling = splitLine[6].toInt(),
        suppressed = splitLine[7].toInt(),
        reliability = splitLine[8].toFloat(),
        range = splitLine[9].toFloat(),
        range_lock = splitLine[10].toInt(),
        group_weight = splitLine[11].toInt(),
        num_magnetic_groups = splitLine[12].toInt(),
        magnetic_groups = parseMagneticGroups(splitLine.subList(12, splitLine.size).joinToString(" "))
    )
}

data class CouplingGroupField(
    val type: Int,
    val mg1_parent: Int,
    val mg1: Int,
    val mg2_parent: Int,
    val mg2: Int,
    val group_id: Int,
    val bond_length: Int,
    val observed_coupling: Float,
    val predicted_coupling: Float,
    val prediction_quality: Int
)

fun couplingGroupFromString(line: String): CouplingGroupField {
    val splitLine = line.split(" ")
    return CouplingGroupField(
        type = splitLine[2].toInt(),
        mg1_parent = splitLine[3].toInt(),
        mg1 = splitLine[4].toInt(),
        mg2_parent = splitLine[5].toInt(),
        mg2 = splitLine[6].toInt(),
        group_id = splitLine[7].toInt(),
        bond_length = splitLine[8].toInt(),
        observed_coupling = splitLine[9].toFloat(),
        predicted_coupling = splitLine[10].toFloat(),
        prediction_quality = splitLine[11].toInt()
    )
}

data class AtomField(
    val atomic_number: Int,
    val charge: Float,
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
    val x: Float,
    val y: Float,
    val z: Float
)

fun atomFromString(line: String): AtomField {
    val splitLine = line.split(" ")
    return AtomField(
        atomic_number = splitLine[1].toInt(),
        charge = splitLine[2].toFloat(),
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
        x = splitLine[13].toFloat(),
        y = splitLine[14].toFloat(),
        z = splitLine[15].toFloat()
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


fun parseMMS(inputStream: FileInputStream, mmsFile: MMSFile) {
    inputStream.bufferedReader().useLines { lines ->
        lines.forEach {
            when {
                it.startsWith('V') -> mmsFile.vField = vFieldFromString(it)
                it.startsWith("I A NAME") -> mmsFile.atomNames.add(atomNameFromString(it))
                it.startsWith("N G") -> mmsFile.spinGroups.add(spinGroupFromString(it))
                it.startsWith("N C") -> mmsFile.couplingGroups.add(couplingGroupFromString(it))
                it.startsWith("A") -> mmsFile.atoms.add(atomFromString(it))
                it.startsWith("B") -> mmsFile.bonds.add(bondFromString(it))
            }
        }
    }
}