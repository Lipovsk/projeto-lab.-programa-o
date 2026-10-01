package br.edu.unit.chemest.model;
public record OrbitalResult(double homoHartree, double lumoHartree) {
    public static final double HARTREE_TO_EV = 27.211386245988;
    public OrbitalResult {
        if (!Double.isFinite(homoHartree) || !Double.isFinite(lumoHartree)
                || !Double.isFinite((lumoHartree - homoHartree) * HARTREE_TO_EV))
            throw new IllegalArgumentException("Energias devem ser números finitos.");
    }
    public double gapHartree() { return lumoHartree - homoHartree; }
    public double gapEv() { return gapHartree() * HARTREE_TO_EV; }
}
