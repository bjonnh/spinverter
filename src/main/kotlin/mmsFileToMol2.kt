import net.nprod.spinverter.MMS.MMSFile
import org.openscience.cdk.Atom
import org.openscience.cdk.AtomContainer
import org.openscience.cdk.Bond
import org.openscience.cdk.aromaticity.Kekulization
import org.openscience.cdk.config.Isotopes
import org.openscience.cdk.depict.DepictionGenerator
import org.openscience.cdk.interfaces.IBond
import org.openscience.cdk.io.CMLWriter
import org.openscience.cdk.smiles.SmilesGenerator
import java.io.FileWriter
import javax.vecmath.Point3d
import org.openscience.cdk.interfaces.IAtomContainer
import org.openscience.cdk.aromaticity.Aromaticity
import org.openscience.cdk.graph.Cycles
import org.openscience.cdk.graph.CycleFinder
import org.openscience.cdk.aromaticity.ElectronDonation



fun mmsFileToMol2(mmsFile: MMSFile) {
    val molecule = AtomContainer()
    val atomList: MutableList<Atom> = mutableListOf()
    val isotopes = Isotopes.getInstance()
    mmsFile.atoms.map {
        val atom = Atom(
            isotopes.getElementSymbol(it.atomic_number),
            Point3d(it.x.toDouble()*10, it.y.toDouble()*10, it.z.toDouble()*10)
        )
        atomList.add(atom)
        atom.implicitHydrogenCount = 0
        molecule.addAtom(
            atom
        )
    }

    mmsFile.bonds.map {
        val bond = Bond(
            atomList[it.a1], atomList[it.a2], when (it.type) {
                "S" -> IBond.Order.SINGLE
                "D" -> IBond.Order.DOUBLE
                "T" -> IBond.Order.TRIPLE
                else -> IBond.Order.DOUBLE
            }
        )
        if (it.type == "C") bond.setIsAromatic(true)
        molecule.addBond(bond)
    }
/*
    val model = ElectronDonation.daylight()
    val cycles = Cycles.or(Cycles.all(), Cycles.all(6))
    val aromaticity = Aromaticity(model, cycles)

    // apply our configured model to each molecule
    //Kekulization.kekulize(molecule)
    aromaticity.apply(molecule)
*/

    val generator = SmilesGenerator.generic()
    val smiles = generator.create(molecule)
    println(smiles)

    val dptgen = DepictionGenerator()
    dptgen.depict(molecule).writeTo("/tmp/argh.png")

    val output = FileWriter("/tmp/molecule.cml")
    val cmlwriter = CMLWriter(output)
    cmlwriter.write(molecule)
    cmlwriter.close()

}