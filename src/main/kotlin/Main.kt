package net.nprod.spinverter

import mmsFileToMol2
import net.nprod.spinverter.MMS.*
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLStreamWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun mmsFileToMnova(mmsFile: MMSFile) {
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder  = dbFactory.newDocumentBuilder()
    val doc = dBuilder.newDocument()
    val root = doc.createElement("mnova-spinsim")

    doc.appendChild(root)
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    transformer.transform(DOMSource(doc), StreamResult(File("/tmp/mnova.xml")))

}

fun main(args: Array<String>) {
    val mmsFile = MMSFile()
    val inputStream = File("tests/test_001.mms").inputStream()

    parseMMS(inputStream, mmsFile)
    mmsFileToMol2(mmsFile)
    mmsFileToMnova(mmsFile)
}