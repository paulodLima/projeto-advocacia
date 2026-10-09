from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'output/pdf/compartilhamento-de-causas-entre-parceiros.pdf'
OUT.parent.mkdir(parents=True, exist_ok=True)
for name, font, fallback in [('Body','GOTHIC.TTF','arial.ttf'),('Bold','GOTHICB.TTF','arialbd.ttf'),('Title','BKANT.TTF','georgia.ttf')]:
    path = Path('C:/Windows/Fonts') / font
    pdfmetrics.registerFont(TTFont(name, str(path if path.exists() else path.with_name(fallback))))
pdfmetrics.registerFontFamily('Body', normal='Body', bold='Bold')
W,H = A4
c = canvas.Canvas(str(OUT), pagesize=A4)
c.setTitle('Parcerias que conectam escritórios | Gestão Advocacia')
c.setAuthor('Gestão Advocacia')
GREEN = HexColor('#4f5a49'); GOLD = HexColor('#b8935a'); LIGHT = HexColor('#afb695')
BG = HexColor('#f7f4ef'); INK = HexColor('#2a2723'); MUTED = HexColor('#7d6c5e')
def box(x,y,w,h,color,r=14):
    c.setFillColor(color); c.roundRect(x,y,w,h,r,stroke=0,fill=1)
def text(s,x,y,size=10,color=INK,font='Body'):
    c.setFillColor(color); c.setFont(font,size); c.drawString(x,y,s)
def para(s,x,top,w,size=10.2,color=INK,font='Body',leading=15):
    p=Paragraph(s,ParagraphStyle('p',fontName=font,fontSize=size,leading=leading,textColor=color))
    _,h=p.wrap(w,1000); p.drawOn(c,x,top-h); return h
def logo(x,y,scale=.45):
    c.saveState(); c.translate(x,y); c.scale(scale,-scale); c.setStrokeColor(white); c.setLineWidth(2.4)
    p=c.beginPath(); p.moveTo(83,23); p.curveTo(76,11,64,5,51,5); p.curveTo(28,5,12,23,12,47); p.curveTo(12,72,28,89,51,89); p.curveTo(66,89,78,82,85,71); p.lineTo(85,48); p.lineTo(58,48); c.drawPath(p)
    for pts in [[(85,48),(74,48),(74,73)],[(27,100),(56,25),(86,100)],[(39,69),(73,69)],[(20,100),(39,100)],[(76,100),(94,100)]]:
        p=c.beginPath(); p.moveTo(*pts[0])
        for pt in pts[1:]: p.lineTo(*pt)
        c.drawPath(p)
    c.restoreState()

c.setFillColor(BG); c.rect(0,0,W,H,stroke=0,fill=1)
box(20,24,12,H-48,GREEN,6)
box(48,H-111,W-76,83,GREEN,18)
logo(65,H-44)
text('GESTÃO ADVOCACIA',124,H-65,16,white,'Bold')
text('Parceiros  /  Colaboração entre escritórios',124,H-87,9.5,HexColor('#e4e8db'))
text('PROPOSTA DE EVOLUÇÃO',50,H-141,8.5,GOLD,'Bold')
text('Parcerias que conectam',50,H-177,26,GREEN,'Title')
text('escritórios e causas',50,H-208,26,GREEN,'Title')
para('Dois escritórios no Gestão Advocacia podem atuar na mesma causa, com acesso apenas às informações autorizadas. Cada empresa mantém seus demais dados separados.',50,H-229,W-100,11,leading=16)

box(48,424,W-76,123,white)
text('Por que vale a pena',66,524,14,GREEN,'Bold')
benefits=[('Informação organizada','Documentos e atualizações no mesmo lugar.'),('Atuação complementar','Especialidades e apoio regional em parceria.'),('Mais controle','Responsáveis definidos e histórico das ações.')]
for i,(title,desc) in enumerate(benefits):
    y=501-i*27
    c.setFillColor(GOLD); c.circle(69,y+3,2.5,stroke=0,fill=1)
    text(title,80,y,10,GREEN,'Bold'); text(desc,80,y-12,9.1,MUTED)

text('Como funcionaria',50,396,14,GREEN,'Bold')
steps=[('01','Selecionar','A empresa responsável escolhe a causa e o escritório parceiro.'),('02','Autorizar','Define documentos, participantes e ações permitidas.'),('03','Colaborar','Após aceitar o convite, o parceiro acessa a causa em sua conta.')]
for i,(n,title,desc) in enumerate(steps):
    y=325-i*54
    box(48,y,W-76,47,HexColor('#e9ecdf'),10)
    box(59,y+10,28,28,GREEN,9); text(n,65,y+19,9,white,'Bold')
    text(title,100,y+29,10,GREEN,'Bold'); para(desc,100,y+22,W-144,9,leading=12)

box(48,111,W-76,89,white)
text('Acesso limitado à parceria',66,178,12,GREEN,'Bold')
para('A causa continua pertencendo ao escritório de origem. Outros clientes, causas e dados internos permanecem privados. O acesso pode ser revogado; arquivos já baixados não podem ser recolhidos.',66,163,W-112,9.4,leading=14)
para('<b>Próxima etapa:</b> após o módulo de Causas, criar vínculo entre empresas, convites, permissões e histórico de acesso.',50,91,W-100,9.3,color=GREEN,leading=13)
c.setStrokeColor(LIGHT); c.line(50,45,W-28,45)
text('Ideia futura: o cadastro de parceiros já existe; o compartilhamento ainda será implementado.',50,31,7.2,MUTED)
c.save()
print(OUT)
