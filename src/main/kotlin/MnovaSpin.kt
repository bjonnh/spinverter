import net.nprod.spinverter.MMS.MMSFile
import java.io.File
import java.io.StringWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

fun XMLStreamWriter.document(init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    this.writeStartDocument("UTF-8", "1.0")
    this.init()
    this.writeEndDocument()
    return this
}

fun XMLStreamWriter.element(name: String, init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    this.writeStartElement(name)
    this.init()
    this.writeEndElement()
    return this
}

fun XMLStreamWriter.element(name: String, content: String) {
    element(name) {
        writeCharacters(content)
    }
}


fun XMLStreamWriter.attribute(name: String, value: String) = writeAttribute(name, value)


fun XMLStreamWriter.group(
    name: String, spinByTwo: String, lineWidth: Float, number: Int,
    init: XMLStreamWriter.() -> Unit
): XMLStreamWriter {
    element("group") {
        attribute("name", name)
        attribute("spinByTwo", spinByTwo)
        attribute("lineWidth", "%.1f".format(lineWidth))
        attribute("number", number.toString())
        this.init()
    }
    return this
}

fun XMLStreamWriter.shift(shift: Float): XMLStreamWriter {
    element("shift", shift.toString())
    return this
}


fun XMLStreamWriter.qConst(qConst: Float): XMLStreamWriter {
    element("qConst", qConst.toString())
    return this
}

fun XMLStreamWriter.jCoupling(with: String, value: Float): XMLStreamWriter {
    element("jCoupling") {
        attribute("name", with)
        this.writeCharacters("%.2f".format(value))
    }
    return this
}

fun XMLStreamWriter.dCoupling(with: String, value: Float): XMLStreamWriter {
    element("dCoupling") {
        attribute("name", with)
        this.writeCharacters("%.2f".format(value))
    }
    return this
}

fun XMLStreamWriter.spectrum(init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    element("spectrum") {
        this.init()
    }
    return this
}

fun XMLStreamWriter.frequency(frequency: Float): XMLStreamWriter {
    element("frequency") {
        this.writeCharacters(frequency.toString())
    }
    return this
}

fun XMLStreamWriter.points(points: Int): XMLStreamWriter {
    element("points") {
        this.writeCharacters(points.toString())
    }
    return this
}


fun XMLStreamWriter.from_ppm(ppm: Float): XMLStreamWriter {
    element("from") {
        this.writeCharacters(ppm.toString())
    }
    return this
}

fun XMLStreamWriter.to_ppm(ppm: Float): XMLStreamWriter {
    element("to") {
        this.writeCharacters(ppm.toString())
    }
    return this
}

fun XMLStreamWriter.population(population: Float): XMLStreamWriter {
    element("population") {
        this.writeCharacters(population.toString())
    }
    return this
}

fun XMLStreamWriter.mnovaspinsim(init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    element("mnova-spinsim") {
        this.init()
    }
    return this
}

fun XMLStreamWriter.spinsystem(init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    element("spin-system") {
        this.init()
    }
    return this
}

fun XMLStreamWriter.summary(init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    element("summary") {
        this.init()
    }
    return this
}


fun mmsFileToMnova(mmsFile: MMSFile, fileName: String) {
    // Only supports 1H for now

    val f = XMLOutputFactory.newInstance()

    val sw = StringWriter()
    val writer = f.createXMLStreamWriter(sw)
    writer.document {
        mnovaspinsim {
            spinsystem {
                summary { }
                population(1f)
                // Get the spin groups corresponding to that type of atom
                mmsFile.spinGroups.filter { sg -> mmsFile.atomNames[sg.magneticGroups[0][0].atom_number].atomic_number == 1 }
                    .mapIndexed { sg_index, sg ->
                        // We have X*Y where X is magnetically equivalent Y chemically equivalent
                        sg.magneticGroups[0].forEachIndexed { index, mgg ->
                                group(
                                    name = "H" + sg.index + "000" + index.toString(), spinByTwo = "1",
                                    lineWidth = sg.lineWidth, number = sg.numMagneticGroups * sg.magneticGroups[0].size
                                ) {
                                    shift(sg.observedShift ?: sg.predictedShift ?: 0.0f)
                                    qConst(0f)
                                    mmsFile.couplingGroups.filter { it.type == 1 }.map { cg -> // We only manage direct bonds here
                                        when {
                                            (cg.mg1 == sg.index) ->
                                                mmsFile.spinGroups.find { it.index == cg.mg2 }?.let {
                                                    jCoupling("H" + cg.mg2 + "000"+ cg.mg2_index, cg.observed_coupling)
                                                }
                                            (cg.mg2 == sg.index) ->
                                                mmsFile.spinGroups.find { it.index == cg.mg1 }?.let {
                                                    jCoupling("H" + cg.mg1 + "000" + cg.mg1_index, cg.observed_coupling)
                                                }
                                            else -> null
                                        }
                                    }

                            }
                        }

                    }
            }
            spectrum {
                frequency(499.82867f)
                points(65536)
                from_ppm(-2f)
                to_ppm(14f)
            }

        }
    }
    writer.flush()
    sw.flush()
    println(sw.toString())
    File(fileName).writeText(sw.toString())

}
