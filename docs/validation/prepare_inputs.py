"""Prepara entradas didáticas; não executa química quântica nem otimização."""
from pathlib import Path
from math import sqrt, dist
from collections import Counter

ROOT = Path(__file__).resolve().parents[2]
out = ROOT / "data/exemplos"
out.mkdir(parents=True, exist_ok=True)
# Modelo idealizado: carbonos tetraédricos; comprimentos em angstroms.
atoms = [("C", (0., 0., 0.)), ("C", (1.52, 0., 0.))]
oxygen = (1.52 + 1.43/3, 1.43*sqrt(8)/3, 0.)
atoms.append(("O", oxygen))
for direction in [(-1/3, sqrt(8)/3, 0), (-1/3, -sqrt(2)/3, sqrt(2/3)), (-1/3, -sqrt(2)/3, -sqrt(2/3))]:
    atoms.append(("H", tuple(1.09*v for v in direction)))
for direction in [(1/3, -sqrt(2)/3, sqrt(2/3)), (1/3, -sqrt(2)/3, -sqrt(2/3))]:
    atoms.append(("H", tuple(c + 1.09*v for c, v in zip(atoms[1][1], direction))))
atoms.append(("H", (oxygen[0]+0.96, oxygen[1], oxygen[2])))
assert Counter(s for s, _ in atoms) == {"C": 2, "H": 6, "O": 1}
for i, j, length in [(0,1,1.52),(1,2,1.43),(0,3,1.09),(0,4,1.09),(0,5,1.09),(1,6,1.09),(1,7,1.09),(2,8,.96)]:
    assert abs(dist(atoms[i][1], atoms[j][1])-length) < 1e-10
xyz = "9\nEtanol C2H6O: geometria idealizada de demonstracao em angstroms; sem otimizacao Gaussian.\n"
xyz += "".join(f"{s} {x:.8f} {y:.8f} {z:.8f}\n" for s,(x,y,z) in atoms)
(out/"etanol.xyz").write_text(xyz, encoding="utf-8")
(out/"1.xyz").write_text(xyz, encoding="utf-8")
(out/"moleculas.csv").write_text("NUM,SMILES\n1,CCO\n2,c1ccccc1\n",encoding="utf-8")
original = (ROOT/"src/test/resources/gaussian_sample.out").read_text(encoding="latin-1")
sample = "AMOSTRA SINTÉTICA PARA TESTES - não é uma execução real do Gaussian.\n" + original
(out/"gaussian_sample.out").write_text(sample,encoding="latin-1")
print("Entradas criadas: etanol e alias 1.xyz (9 átomos); CSV com 2 registros; OUT sintético.")

