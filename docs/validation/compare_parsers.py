"""Referência independente equivalente ao roteiro; não é cópia autenticada do artigo."""
import csv
import hashlib
import json
import math
import platform
import re
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
SAMPLE = ROOT/"data/exemplos/gaussian_sample.out"

def extract_HOMO_LUMO(path):
    # Referência em memória: blocos de linhas ocupadas seguidos da primeira virtual.
    content = path.read_text(encoding="latin-1")
    pattern = r"(?m)^(?:[^\S\n]*Alpha  occ\. eigenvalues --[^\r\n]*(?:\r?\n))+[^\S\n]*Alpha virt\. eigenvalues --[^\r\n]*"
    blocks = re.findall(pattern, content)
    if not blocks:
        raise ValueError("Par Alpha completo ausente")
    lines = blocks[-1].splitlines()
    occupied = [line for line in lines if "Alpha  occ. eigenvalues --" in line]
    virtual = next(line for line in lines if "Alpha virt. eigenvalues --" in line)
    homo = float(occupied[-1].split("--",1)[1].split()[-1].replace("D","E"))
    lumo = float(virtual.split("--",1)[1].split()[0].replace("D","E"))
    return homo, lumo, lumo-homo

digest = hashlib.sha256(SAMPLE.read_bytes()).hexdigest()
assert digest in (ROOT/"docs/validation/geracao.txt").read_text(encoding="utf-8"), "Amostra mudou depois da execução Java"
with (ROOT/"resultados/resultados.csv").open(encoding="utf-8",newline="") as stream:
    java_rows = list(csv.DictReader(stream))
assert len(java_rows)==1
row = java_rows[0]
assert Path(row["arquivo"]).as_posix() == "data/exemplos/gaussian_sample.out"
python_values = extract_HOMO_LUMO(SAMPLE)
java_values = tuple(float(row[k]) for k in ("HOMO","LUMO","gapHartree"))
diffs = tuple(abs(p-j) for p,j in zip(python_values,java_values))
ok = all(math.isfinite(d) and d<=1e-8 for d in diffs)
header = ["arquivo","homo_python","lumo_python","gap_python","homo_java","lumo_java","gap_java","diferenca_homo","diferenca_lumo","diferenca_gap","status"]
with (ROOT/"docs/comparacao_python_java.csv").open("w",encoding="utf-8",newline="") as stream:
    writer=csv.writer(stream)
    writer.writerow(header)
    writer.writerow(["data/exemplos/gaussian_sample.out",*python_values,*java_values,*diffs,"OK" if ok else "FALHA"])
proof={"python":platform.python_version(),"sample_sha256":digest,"tolerance_hartree":1e-8,
       "python_values":python_values,"java_values":java_values,"absolute_differences":diffs,
       "status":"OK" if ok else "FALHA","scope":"Referencia independente equivalente ao roteiro, em uma amostra sintetica; nao autentica todo o codigo do artigo."}
(ROOT/"docs/validation/comparacao.json").write_text(json.dumps(proof,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(json.dumps(proof,ensure_ascii=False,indent=2))
if not ok: raise SystemExit(1)

