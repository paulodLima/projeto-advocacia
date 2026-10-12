// Validação visual com APIs simuladas; não modifica empresas reais.
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';
import assert from 'node:assert/strict';
const profile = await fs.mkdtemp(path.join(os.tmpdir(), 'adv-contratos-'));
const chrome = spawn('C:/Program Files/Google/Chrome/Application/chrome.exe', ['--headless=new','--disable-gpu','--no-first-run','--remote-debugging-port=9241','--user-data-dir='+profile,'about:blank'], { windowsHide: true, stdio: 'ignore' });
let ws, seq = 0, saves = 0; const pending = new Map(); const errors = [];
let empresa = { id: 'empresa-teste', nome: 'Escritório de demonstração', cnpj: '11222333000181', responsavel: 'Responsável de demonstração', emailResponsavel: 'dono@example.test', usuariosAtivos: 3, situacao: 'ATIVA', financeiro: 'REGULAR', inicio: '2026-10-01', versao: 0, modulos: ['inicio','config','contatos','parceiros','equipe','financeiro'].map(c => ({ codigo:c, nome:c, liberado:true, situacao:'DISPONIVEL' })), historico: [] };
const sleep = ms => new Promise(r => setTimeout(r,ms));
function request(method,params={}) {
  const id=++seq;
  return new Promise((resolve,reject) => { const timeout=setTimeout(()=>{pending.delete(id);reject(new Error(method+' timeout'));},10000);pending.set(id,{resolve,reject,timeout});ws.send(JSON.stringify({id,method,params})); });
}
async function fulfill(e) {
  const p = new URL(e.request.url).pathname; let body={},status=200;
  if (p==='/api/auth/me') body={ id:'00000000-0000-0000-0000-000000000001',nome:'Administrador',email:'admin@example.test',status:'ATIVO' };
  else if(p==='/api/plataforma/acesso') body={podeAdministrar:true};
  else if(p==='/api/auth/csrf') body={token:'test',headerName:'X-CSRF-TOKEN'};
  else if(p==='/api/plataforma/admin/convites') body=[];
  else if(p==='/api/plataforma/admin/gestao-empresas') body=[empresa];
  else if(p==='/api/plataforma/admin/gestao-empresas/empresa-teste') {
    if(e.request.method==='PUT') {
      const d=JSON.parse(e.request.postData);assert.equal(d.versao,empresa.versao);assert(d.motivo.trim());
      empresa={...empresa,...d,modulos:empresa.modulos.map(m=>({...m,liberado:d.modulos.includes(m.codigo)})),versao:empresa.versao+1,historico:[{id:crypto.randomUUID(),autor:'Administrador',acao:'EMPRESA_SUSPENSA',anterior:empresa.situacao,atual:d.situacao,motivo:d.motivo,criadoEm:new Date().toISOString()}]};saves++;
    }
    body=empresa;
  } else { status=403;body={detail:'Fora do cenário visual'}; }
  await request('Fetch.fulfillRequest',{requestId:e.requestId,responseCode:status,responseHeaders:[{name:'Content-Type',value:'application/json'},{name:'Access-Control-Allow-Origin',value:'http://localhost:4200'},{name:'Access-Control-Allow-Credentials',value:'true'},{name:'Access-Control-Allow-Headers',value:'Content-Type,X-CSRF-TOKEN'},{name:'Access-Control-Allow-Methods',value:'GET,PUT,OPTIONS'}],body:Buffer.from(JSON.stringify(body)).toString('base64')});
}
async function evaluate(expression) { const r=await request('Runtime.evaluate',{expression,returnByValue:true});if(r.exceptionDetails)throw new Error(JSON.stringify(r.exceptionDetails));return r.result.value; }
async function waitFor(expression) { for(let i=0;i<100;i++){if(await evaluate(expression))return;await sleep(100);}throw new Error('Condição não atingida: '+expression+' '+await evaluate('document.body.innerText')); }
const click = text => evaluate(`Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()===${JSON.stringify(text)}).click()`);
async function screenshot(name) { const r=await request('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});await fs.writeFile('docs/'+name,Buffer.from(r.data,'base64')); }
try {
  let page;for(let i=0;i<100;i++){try{page=(await(await fetch('http://127.0.0.1:9241/json')).json()).find(p=>p.type==='page');if(page)break;}catch{}await sleep(100);}assert(page);
  ws=new WebSocket(page.webSocketDebuggerUrl);await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject;});
  ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);if(p){clearTimeout(p.timeout);pending.delete(m.id);m.error?p.reject(new Error(JSON.stringify(m.error))):p.resolve(m.result);}}else if(m.method==='Fetch.requestPaused')fulfill(m.params).catch(e=>errors.push(e.message));else if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.text);};
  await request('Page.enable');await request('Runtime.enable');await request('Fetch.enable',{patterns:[{urlPattern:'*/api/*'}]});
  await request('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
  await request('Page.navigate',{url:'http://localhost:4200/administracao'});await waitFor("document.body.innerText.includes('Empresas e contratos')");
  await click('Empresas e contratos');await waitFor("document.body.innerText.includes('Escritório de demonstração')");await click('Gerenciar');await waitFor("document.querySelector('[name=situacao]')");
  await evaluate("(()=>{const s=document.querySelector('[name=situacao]');s.value='SUSPENSA';s.dispatchEvent(new Event('change',{bubbles:true}));})()");
  await evaluate("(()=>{const s=document.querySelector('[name=financeiro]');s.value='INADIMPLENTE';s.dispatchEvent(new Event('change',{bubbles:true}));})()");
  await evaluate("(()=>{const t=document.querySelector('[name=motivo]');t.value='Inadimplência — cenário fictício';t.dispatchEvent(new Event('input',{bubbles:true}));})()");
  assert(await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()==='Salvar medidas administrativas').disabled"));
  await evaluate("document.querySelector('[name=confirmado]').click()");await click('Salvar medidas administrativas');await waitFor("document.body.innerText.includes('Contrato atualizado e medida registrada')");
  assert.equal(saves,1);assert.equal(empresa.situacao,'SUSPENSA');assert.equal(empresa.financeiro,'INADIMPLENTE');
  await screenshot('gestao-empresas-desktop.png');
  await request('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});await sleep(300);
  assert(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'),'Overflow horizontal no celular');await screenshot('gestao-empresas-mobile.png');
  assert.deepEqual(errors,[]);console.log('Chrome desktop/celular: filtro/ficha, confirmação, suspensão, financeiro e histórico passaram; APIs simuladas; nenhuma empresa real alterada.');
} finally {if(ws)ws.close();chrome.kill();}
