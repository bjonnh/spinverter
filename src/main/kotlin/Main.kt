package net.nprod.spinverter

import net.nprod.spinverter.MMS.*
import java.io.File



fun mmsFileToMol2(mmsFile: MMSFile) {
    val sb = StringBuilder()
    sb.appendln("# Converted by Spinverter")
    sb.appendln("# ")
    sb.appendln( "")
    println(sb.toString())
}

fun main(args: Array<String>) {
    val mmsFile = MMSFile()
    val inputStream= File("tests/test_002.mms").inputStream()

    inputStream.bufferedReader().useLines { lines -> lines.forEach {
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
    mmsFileToMol2(mmsFile)
}