package net.nprod.spinverter

import mmsFileToMnova
import mmsFileToMol2
import net.nprod.spinverter.MMS.*
import java.io.File


fun main(args: Array<String>) {
    val mmsFile = MMSFile()
    val inputStream = File("tests/test_001.mms").inputStream()

    parseMMS(inputStream, mmsFile)
    //mmsFileToMol2(mmsFile)
    mmsFileToMnova(mmsFile, "/tmp/mnova.xml")
}