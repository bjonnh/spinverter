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
                    .map { sg ->
                        // We have X*Y where X is magnetically equivalent Y chemically equivalent
                        sg.magneticGroups[0].forEachIndexed { index, _ ->
                            group(
                                name = "H" + sg.index + "-" + index.toString(), spinByTwo = 1,
                                lineWidth = sg.lineWidth, number = sg.numMagneticGroups * sg.magneticGroups[0].size
                            ) {
                                shift(sg.observedShift ?: sg.predictedShift ?: 0.0)
                                qConst(0.0)
                                mmsFile.couplingGroups.filter { it.type == 1 }.map { cg ->
                                    // We only manage direct bonds here
                                    when {
                                        (cg.mg1 == sg.index) ->
                                            mmsFile.spinGroups.find { it.index == cg.mg2 }?.let {
                                                jCoupling("H" + cg.mg2 + "-" + cg.mg2_index, cg.observed_coupling)
                                            }
                                        (cg.mg2 == sg.index) ->
                                            mmsFile.spinGroups.find { it.index == cg.mg1 }?.let {
                                                jCoupling("H" + cg.mg1 + "-" + cg.mg1_index, cg.observed_coupling)
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

fun pmsFileToMnova(pmsFile: PMSData, file: File) {
    // Only supports 1H for now

    val f = XMLOutputFactory.newInstance()

    val sw = StringWriter()
    val writer = f.createXMLStreamWriter(sw)
    writer.document {
        mnovaspinsim {

                pmsFile.spinGroups.filter { it.species == "1H" }.map { sg ->
                    spinsystem {
                        summary { }
                        population(sg.population?:1.0)

                    sg.spins.map { s ->
                        (1..(s.magneticEquivalence ?: 1)).map { magneticEquivalent ->
                            val newName = (s.name + "-" + magneticEquivalent)
                            group(
                                name = newName,
                                spinByTwo = sg.twospin ?: 1,
                                lineWidth = s.width ?: 1.0,
                                number = s.chemicalEquivalence ?: 1
                            ) {

                                shift(s.chemicalShift ?: s.predictedShift ?: 0.0)
                                qConst(0.0)
                                val cpls = pmsFile.couplings.filter { (it.sg1 == s.name) || (it.sg2 == s.name) }


                                // Grab all couplings that are with the magnetic equivalent
                                println(newName)
                                data class Couple(
                                    val name: String,
                                    val group: Int,
                                    val constant: Double
                                )

                                val coupleList: MutableList<Couple> = mutableListOf()

                                cpls.map { c ->
                                    when {
                                        (c.sg1 == c.sg2) -> {
                                            jCoupling(
                                                s.name + "-" + (3 - magneticEquivalent),
                                                c.constant
                                            )
                                            null
                                        }
                                        else -> {
                                            val other = when {
                                                (c.sg1 == s.name) -> c.sg2
                                                else -> c.sg1
                                            }
                                            // Select the coupling partners that are ME=1
                                            sg.spins.filter { it.name == other && it.magneticEquivalence == 1 }.map {
                                                jCoupling(it.name + "-1", c.constant)
                                            }

                                            // If we are ME=1 ourselve
                                            if (s.magneticEquivalence == 1) {
                                                sg.spins.filter { it.name == other }.map { sp2 ->
                                                    if (sp2.magneticEquivalence == 2) {
                                                        (1..(sp2.magneticEquivalence ?: 1)).map {
                                                            jCoupling(sp2.name + "-$it", c.constant)
                                                        }
                                                    }
                                                }
                                            } else {
                                                sg.spins.filter { it.name == other && it.magneticEquivalence == 2 }
                                                    .map { sp2 ->
                                                        (1..(sp2.magneticEquivalence ?: 1)).map {
                                                            coupleList.add(Couple(sp2.name ?: "", it, c.constant))
                                                        }
                                                    }
                                            }
                                        }
                                    }
                                }

                                coupleList.groupBy { it.name + "-" + it.group }.map { couple ->
                                    // We sort the list by descending order of coupling constants

                                    val sortedList = couple.value.sortedBy { it.constant }.reversed()
                                    if (sortedList.size == 1) {
                                        // If the two couplings are equal, they are more than ME, so we just take the 1-1 couplings
                                        val filtered = coupleList.filter { it.name == sortedList[0].name}
                                        if ((filtered[0].constant != filtered[1].constant) || sortedList[0].group == magneticEquivalent) {
                                            jCoupling(
                                                "${sortedList[0].name}-${sortedList[0].group}",
                                                sortedList[0].constant
                                            )
                                        } else { null }
                                    } else {
                                        // This part is a bit complex
                                        // We take the first value of the group if ME=1
                                        // But we take the second value of the group if ME=2
                                        // This is assuming that the constants are sorted by decreasing order
                                        // Which is done in the beginning of the groupby

                                        val idx = if (magneticEquivalent == 1) {
                                            sortedList[0].group - 1
                                        } else {
                                            2 - sortedList[0].group
                                        }
                                        jCoupling(
                                            "${sortedList[idx].name}-${sortedList[idx].group}",
                                            sortedList[idx].constant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            }
            spectrum {
                frequency(pmsFile.frequency ?: 400.0)
                points(65536)
                from_ppm(pmsFile.rightPPM ?: 0.0)
                to_ppm(pmsFile.leftPPM ?: 20.0)
            }

        }
    }
    writer.flush()
    sw.flush()
    file.writeText(sw.toString())
}
