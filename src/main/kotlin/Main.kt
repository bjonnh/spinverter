package net.nprod.spinverter

import mmsFileToMnova
import mmsFileToMol2
import net.nprod.spinverter.MMS.*
import net.nprod.spinverter.PMS.parsePMS
import pmsFileToMnova
import java.io.File


fun main(args: Array<String>) {
/*    val mmsFile = MMSFile()
    val inputStream = File("tests/test_001.mms").inputStream()

    parseMMS(inputStream, mmsFile)
    //mmsFileToMol2(mmsFile)
    mmsFileToMnova(mmsFile, "/tmp/mnova.xml")*/

    val pmsInputStream = File("tests/test_003.pms").inputStream()
    val pmsData = parsePMS(pmsInputStream)
    pmsFileToMnova(pmsData, "/tmp/mnova.xml")

}
