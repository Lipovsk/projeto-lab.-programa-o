package br.edu.unit.chemest.model;
import java.util.List;
public record MoleculeRecord(String id, String name, String smiles, List<Atom3D> atoms) {
    public MoleculeRecord {
        if (id == null || id.isBlank() || name == null || name.isBlank() || smiles == null || smiles.isBlank())
            throw new IllegalArgumentException("Informe identificação, nome e SMILES.");
        if ((id + name + smiles).contains("\n") || (id + name + smiles).contains("\r"))
            throw new IllegalArgumentException("Os campos da molécula devem ocupar uma linha.");
        atoms = List.copyOf(atoms);
    }
}
