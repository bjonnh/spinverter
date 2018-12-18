import net.nprod.spinverter.MMS.MMSFile
import net.nprod.spinverter.PMS.PMSData
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
    name: String, spinByTwo: Int, lineWidth: Double, number: Int,
    init: XMLStreamWriter.() -> Unit
): XMLStreamWriter {
    element("group") {
        attribute("name", name)
        attribute("spinByTwo", spinByTwo.toString())
        attribute("lineWidth", "%.1f".format(lineWidth))
        attribute("number", number.toString())
        this.init()
    }
    return this
}

fun XMLStreamWriter.shift(shift: Double): XMLStreamWriter {
    element("shift", shift.toString())
    return this
}


fun XMLStreamWriter.qConst(qConst: Double): XMLStreamWriter {
    element("qConst", qConst.toString())
    return this
}

fun XMLStreamWriter.jCoupling(with: String, value: Double): XMLStreamWriter {
    element("jCoupling") {
        attribute("name", with)
        this.writeCharacters("%.2f".format(value))
    }
    return this
}

fun XMLStreamWriter.dCoupling(with: String, value: Double): XMLStreamWriter {
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

fun XMLStreamWriter.frequency(frequency: Double): XMLStreamWriter {
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


fun XMLStreamWriter.from_ppm(ppm: Double): XMLStreamWriter {
    element("from") {
        this.writeCharacters(ppm.toString())
    }
    return this
}

fun XMLStreamWriter.to_ppm(ppm: Double): XMLStreamWriter {
    element("to") {
        this.writeCharacters(ppm.toString())
    }
    return this
}

fun XMLStreamWriter.population(population: Double): XMLStreamWriter {
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
                population(1.0)
                // Get the spin groups corresponding to that type of atom
                mmsFile.spinGroups.filter { sg -> mmsFile.atomNames[sg.magneticGroups[0][0].atom_number].atomic_number == 1 }
                    .mapIndexed { sgIndex, sg ->
                        // We have X*Y where X is magnetically equivalent Y chemically equivalent
                        sg.magneticGroups[0].forEachIndexed { index, mgg ->
                                group(
                                    name = "H" + sg.index + "000" + index.toString(), spinByTwo = 1,
                                    lineWidth = sg.lineWidth, number = sg.numMagneticGroups * sg.magneticGroups[0].size
                                ) {
                                    shift(sg.observedShift ?: sg.predictedShift ?: 0.0)
                                    qConst(0.0)
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
                frequency(499.82867)
                points(65536)
                from_ppm(-2.0)
                to_ppm(14.0)
            }

        }
    }
    writer.flush()
    sw.flush()
    File(fileName).writeText(sw.toString())

}

fun pmsFileToMnova(pmsFile: PMSData, fileName: String) {
    // Only supports 1H for now

    val f = XMLOutputFactory.newInstance()

    val sw = StringWriter()
    val writer = f.createXMLStreamWriter(sw)
    writer.document {
        mnovaspinsim {
            spinsystem {
                summary { }
                population(1.0)

                pmsFile.spinGroups.filter { it.species == "1H" }.map { sg ->

                    sg.spins.map { s ->
                        (1..(s.magneticEquivalence?:1)).map {magneticEquivalent ->
                            val newName = (s.name + "-" + magneticEquivalent)
                            group(
                                name = newName,
                                spinByTwo = sg.twospin ?: 1,
                                lineWidth = s.width ?: 1.0,
                                number = s.chemicalEquivalence ?: 1
                            ) {

                                shift(s.chemicalShift ?: s.predictedShift ?: 0.0)
                                qConst(0.0)
                                val cpls = pmsFile.couplings.filter { (it.sg1 == s.name) }
                                cpls.map { c ->

                                    val other = when {
                                        (c.sg1 == s.name) -> c.sg2
                                        (c.sg2 == s.name) -> c.sg1
                                        else -> "UNK"
                                    }

                                    // We have to handle the cases of multiple magnetically equivalent nuclei
                                    val cplList = sg.spins.filter { it.name == other }.map {
                                        println(it.name)
                                    }
                                }
                            }
                        }
                    }
                }
                // Get the spin groups corresponding to that type of atom
                 /*mmsFile.spinGroups.filter { sg -> mmsFile.atomNames[sg.magneticGroups[0][0].atom_number].atomic_number == 1 }
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

                    }*/
            }
            spectrum {
                frequency(pmsFile.frequency?.toDouble() ?: 400.0)
                points(65536)
                from_ppm(pmsFile.rightPPM?.toDouble() ?: 0.0)
                to_ppm(pmsFile.leftPPM?.toDouble() ?: 20.0)
            }

        }
    }
    writer.flush()
    sw.flush()
    File(fileName).writeText(sw.toString())

}
