"""Converte o Markdown entregue em PDF; dependências documentais, fora do Maven."""
from pathlib import Path
import os, re
from xml.sax.saxutils import escape
from reportlab.pdfgen import canvas
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.colors import HexColor, white
from reportlab.lib.enums import TA_LEFT
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[2]
fonts=Path(os.environ.get("WINDIR","C:/Windows"))/"Fonts"
for name,file in [("Delivery","arial.ttf"),("DeliveryBold","arialbd.ttf")]:
    pdfmetrics.registerFont(TTFont(name,str(fonts/file)))
pdfmetrics.registerFontFamily("Delivery",normal="Delivery",bold="DeliveryBold",italic="Delivery",boldItalic="DeliveryBold")
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name="BodyDelivery",fontName="Delivery",fontSize=9.8,leading=13.3,spaceAfter=8,textColor=HexColor("#203040")))
styles.add(ParagraphStyle(name="TitleDelivery",fontName="DeliveryBold",fontSize=29,leading=34,spaceAfter=12,textColor=HexColor("#146b78")))
styles.add(ParagraphStyle(name="HeadingDelivery",fontName="DeliveryBold",fontSize=14,leading=18,spaceBefore=14,spaceAfter=8,keepWithNext=True,textColor=HexColor("#146b78")))
styles.add(ParagraphStyle(name="CellDelivery",fontName="Delivery",fontSize=8.4,leading=10.4))
styles.add(ParagraphStyle(name="SmallDelivery",fontName="Delivery",fontSize=9,leading=12,spaceAfter=12,textColor=HexColor("#526272")))
def text(value):
    return escape(value).replace("**","").replace("`","")
def footer(c,doc):
    c.setStrokeColor(HexColor("#bdd3d7"));c.line(44,40,551,40)
    c.setFont("Delivery",8);c.setFillColor(HexColor("#526272"))
    c.drawString(44,27,"ChemEST Java | Relatório técnico | Amostra sintética")
    c.drawRightString(551,27,str(doc.page))
class NumberedCanvas(canvas.Canvas):
    def __init__(self,*args,**kwargs):
        super().__init__(*args,**kwargs)
        self.setTitle("ChemEST Java - Relatório técnico")
        self.setAuthor("Projeto acadêmico ChemEST Java")
lines=(ROOT/"docs/relatorio_tecnico.md").read_text(encoding="utf-8").splitlines()
story=[];i=0
while i<len(lines):
    line=lines[i].strip()
    if not line:i+=1;continue
    if line.startswith("# "):
        story.append(Paragraph(text(line[2:]),styles["TitleDelivery"]));i+=1;continue
    if line.startswith("## "):
        if re.match(r"## (4|6|8|10)\.",line):story.append(PageBreak())
        story.append(Paragraph(text(line[3:]),styles["HeadingDelivery"]));i+=1;continue
    if line.startswith("|"):
        rows=[]
        while i<len(lines) and lines[i].startswith("|"):
            cells=[c.strip() for c in lines[i].strip().strip("|").split("|")]
            if not all(re.fullmatch(r"[-: ]+",c) for c in cells):
                rows.append([Paragraph(text(c),styles["CellDelivery"]) for c in cells])
            i+=1
        widths=[125,382] if len(rows[0])==2 else [143,112,112,140]
        table=Table(rows,colWidths=widths,repeatRows=1,hAlign="LEFT")
        table.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,0),HexColor("#dcecef")),("VALIGN",(0,0),(-1,-1),"TOP"),
            ("BOX",(0,0),(-1,-1),.5,HexColor("#bacdd2")),("INNERGRID",(0,0),(-1,-1),.3,HexColor("#cddce0")),
            ("LEFTPADDING",(0,0),(-1,-1),7),("RIGHTPADDING",(0,0),(-1,-1),7),("TOPPADDING",(0,0),(-1,-1),4),("BOTTOMPADDING",(0,0),(-1,-1),4)]))
        story.extend([table,Spacer(1,10)]);continue
    if line.startswith("- "):
        story.append(Paragraph(text(line[2:]),styles["BodyDelivery"],bulletText="•"));i+=1;continue
    para=[line];i+=1
    while i<len(lines) and lines[i].strip() and not lines[i].startswith(("#","|","- ")):
        para.append(lines[i].strip());i+=1
    story.append(Paragraph(text(" ".join(para)),styles["BodyDelivery"]))
output=ROOT/"docs/relatorio_tecnico.pdf"
doc=SimpleDocTemplate(str(output),pagesize=(595.28,841.89),leftMargin=44,rightMargin=44,topMargin=38,bottomMargin=56)
doc.build(story,onFirstPage=footer,onLaterPages=footer,canvasmaker=NumberedCanvas)
# Renderização com MuPDF, alternativa disponível ao Poppler.
import pymupdf
pdf=pymupdf.open(output)
preview=ROOT/"target/pdf-preview";preview.mkdir(parents=True,exist_ok=True)
for i,page in enumerate(pdf):
    page.get_pixmap(matrix=pymupdf.Matrix(1.15,1.15)).save(preview/f"page-{i+1}.png")
content="\n".join(page.get_text() for page in pdf)
for required in ["ChemEST Java","1. Introdução","8. Validação","12. Referência","5.4422772491976"]:
    assert required in content, required
assert len(pdf)>0
print(f"PDF gerado e renderizado: {len(pdf)} páginas; {len(content)} caracteres extraídos.")

