package br.edu.unit.chemest.service;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import java.awt.image.BufferedImage;
public final class SmilesService {
    public IAtomContainer parse(String smiles) {
        if (smiles == null || smiles.isBlank()) throw new InvalidSmilesException("Informe um SMILES válido.");
        try {
            IAtomContainer molecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles.trim());
            if (molecule.getAtomCount() == 0) throw new InvalidSmilesException("Informe um SMILES válido.");
            return molecule;
        } catch (CDKException | IllegalArgumentException e) {
            throw new InvalidSmilesException("Informe um SMILES válido. Verifique átomos, ligações e parênteses.");
        }
    }
    public BufferedImage depict(String smiles) {
        try { return new DepictionGenerator().withSize(560,320).withAtomColors().depict(parse(smiles)).toImg(); }
        catch (CDKException e) { throw new InvalidSmilesException("Não foi possível desenhar a molécula."); }
    }
}
