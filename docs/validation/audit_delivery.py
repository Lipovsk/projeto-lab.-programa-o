"""Auditoria de artefatos existentes; não executa cálculos quânticos."""
from pathlib import Path
import csv, hashlib, json, re, zipfile, subprocess
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
required=["README.md","pom.xml","src/test/resources/gaussian_sample.out",
"data/exemplos/etanol.xyz","data/exemplos/1.xyz","data/exemplos/moleculas.csv","data/exemplos/gaussian_sample.out",
"resultados/exemplo.gjf","resultados/exemplo.bcf","resultados/resultados.csv","resultados/relatorio.html",
"docs/MIGRATION.md","docs/relatorio_tecnico.md","docs/relatorio_tecnico.pdf","docs/comparacao_python_java.csv"]
assert all((ROOT/p).is_file() and (ROOT/p).stat().st_size>0 for p in required)
assert (ROOT/"data/exemplos/etanol.xyz").read_bytes()==(ROOT/"data/exemplos/1.xyz").read_bytes()
orbitals=lambda p:[l for l in p.read_text(encoding="latin-1").splitlines() if l.startswith("Alpha")]
assert orbitals(ROOT/required[2])==orbitals(ROOT/"data/exemplos/gaussian_sample.out")
with (ROOT/"resultados/resultados.csv").open(encoding="utf-8",newline="") as f:
    row=next(csv.DictReader(f))
for k,v in {"HOMO":-.25,"LUMO":-.05,"gapHartree":.2,"gapEv":5.4422772491976}.items():
    assert abs(float(row[k])-v)<1e-8
bcf=(ROOT/"resultados/exemplo.bcf").read_text(encoding="utf-8")
assert bcf=="!\n!batch file\n!start=1\n!\nexemplo.gjf, exemplo.out\n"
html=(ROOT/"resultados/relatorio.html").read_text(encoding="utf-8")
assert "SINTÉTICA" in html and "5.4422772492" in html and "CalculationConfig" in html
suites=[]
for p in sorted((ROOT/"target/surefire-reports").glob("TEST-*.xml")):
    s=ET.parse(p).getroot()
    suites.append({"name":s.attrib["name"],**{k:int(s.attrib[k]) for k in ("tests","failures","errors","skipped")}})
totals={k:sum(s[k] for s in suites) for k in ("tests","failures","errors","skipped")}
assert suites and totals["tests"]>=29 and totals["failures"]==0 and totals["errors"]==0
assert totals["skipped"]==0, "Auditoria de entrega requer executar também o teste gráfico."
jar=ROOT/"target/chemest-java-1.0-SNAPSHOT.jar"
with zipfile.ZipFile(jar) as z:
    assert "br/edu/unit/chemest/App.class" in z.namelist()
    assert "br/edu/unit/chemest/io/GaussianOutputParser.class" in z.namelist()
for folder in ("data","resultados","docs/validation"):
    for p in (ROOT/folder).rglob("*"):
        if p.is_file() and p.suffix not in (".pdf",".png",".pyc"):
            value=p.read_text(encoding="latin-1")
            assert not re.search(r"[A-Za-z]:[\\/](?:Users|home)[\\/]",value),str(p)
            assert not re.search(r"(?i)(?:api_key|password|token)\s*[:=]\s*['\"][A-Za-z0-9]{16,}",value),str(p)
changed_files=subprocess.check_output(["git","diff","--name-only"],cwd=ROOT,text=True).splitlines()
assert not subprocess.check_output(["git","ls-files","--","target",".idea"],cwd=ROOT,text=True).strip()
for path in ("target/chemest-java-1.0-SNAPSHOT.jar",".idea/workspace.xml"):
    subprocess.run(["git","check-ignore","--quiet",path],cwd=ROOT,check=True)
result={"status":"OK","validation":"Relatórios Surefire e JAR existentes inspecionados; este script não executa Maven",
"tests":totals,"suites":suites,"jar":"target/chemest-java-1.0-SNAPSHOT.jar",
"jar_sha256":hashlib.sha256(jar.read_bytes()).hexdigest(),
"changed_files":changed_files,
"scope":"Varredura de padrões de segredos e caminhos pessoais nas novas entradas, saídas e evidências; não é auditoria de segurança exaustiva.",
"files":{p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in required}}
(ROOT/"docs/validation/auditoria.json").write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(json.dumps({"status":"OK","tests":totals,"required_files":len(required),"jar":result["jar"]},ensure_ascii=False))

