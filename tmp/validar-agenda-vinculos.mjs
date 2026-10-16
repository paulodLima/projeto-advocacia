// Validação visual da Agenda com APIs simuladas; não altera dados reais.
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';
import assert from 'node:assert/strict';
const profile=await fs.mkdtemp(path.join(os.tmpdir(),'adv-agenda-'));
const chrome=spawn('C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--remote-debugging-port=9247','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:'ignore'});
let ws,seq=0,modo='master',conflito=false;const pending=new Map(),errors=[];const usuario='00000000-0000-0000-0000-000000000001',assistente='00000000-0000-0000-0000-000000000002',empresa='00000000-0000-0000-0000-000000000003';
const data=date=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
const hoje=data(new Date());const futuro=data(new Date(Date.now()+3*86400000));
const pessoas=[{id:usuario,nome:'Responsável do escritório',ativo:true},{id:assistente,nome:'Assistente',ativo:true}];
let atividades=[{id:'00000000-0000-0000-0000-000000000011',versao:0,dados:{tipo:'REUNIAO',titulo:'Reunião de planejamento',tipoTarefaId:null,data:'2026-09-10',dataFim:null,hora:'09:00:00',horaFim:'10:00:00',prazoFatal:null,diaInteiro:false,modalidade:'PRESENCIAL',local:'Sala de reuniões',descricao:'Planejamento da semana.',responsaveis:[usuario]},situacao:'PENDENTE',concluidaEm:null,criadoEm:new Date().toISOString(),atualizadoEm:new Date().toISOString(),historico:[]}];

atividades.push({...atividades[0],id:'00000000-0000-0000-0000-000000000012',dados:{...atividades[0].dados,tipo:'PRAZO',titulo:'Prazo de revisão',data:'2026-09-11',prazoFatal:'2026-09-11',diaInteiro:true,hora:null,horaFim:null,modalidade:null}});
let preferencias={ativo:true,antecedenciaMinutos:1440},falhaLeitura=false;const lidos=new Set();
function avisos(){return preferencias.ativo?atividades.filter(a=>a.situacao==='PENDENTE').map(a=>({atividadeId:a.id,versao:a.versao,titulo:a.dados.titulo,tipo:a.dados.tipo,marco:a.dados.prazoFatal?'PRAZO_FATAL':'ATIVIDADE',data:a.dados.prazoFatal||a.dados.data,hora:'09:00:00'})).filter(l=>!lidos.has(l.atividadeId+':'+l.versao+':'+l.marco)):[];}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
function request(method,params={}){const id=++seq;return new Promise((resolve,reject)=>{const timeout=setTimeout(()=>{pending.delete(id);reject(new Error(method+' timeout'));},10000);pending.set(id,{resolve,reject,timeout});ws.send(JSON.stringify({id,method,params}));});}
function corresponde(a,u){const d=a.dados,ini=u.searchParams.get('inicio'),fim=u.searchParams.get('fim');const ocorre=!ini || (d.data && d.data<=fim && (d.dataFim || d.data)>=ini) || d.prazoFatal && d.prazoFatal>=ini && d.prazoFatal<=fim || !d.data && u.searchParams.get('incluirSemData')==='true';return ocorre && (!u.searchParams.get('situacao') || a.situacao===u.searchParams.get('situacao')) && (!u.searchParams.get('tipo') || d.tipo===u.searchParams.get('tipo')) && (!u.searchParams.get('responsavel') || d.responsaveis.includes(u.searchParams.get('responsavel'))) && (!u.searchParams.get('busca') || d.titulo.toLowerCase().includes(u.searchParams.get('busca').toLowerCase()));}
async function fulfill(e){
  const u=new URL(e.request.url),p=u.pathname;let body={},status=200;
  if(e.request.method==='OPTIONS') body={};
  else if(p==='/api/auth/me')body={id:usuario,nome:modo==='master'?'Responsável do escritório':'Financeiro',email:'demo@example.test',status:'ATIVO'};
  else if(p==='/api/plataforma/acesso')body={podeAdministrar:false};
  else if(p==='/api/auth/csrf')body={token:'test',headerName:'X-CSRF-TOKEN'};
  else if(p==='/api/empresas/minha')body={id:empresa,papel:modo==='master'?'MASTER':'MEMBRO',dados:{nome_fantasia:'Escritório de demonstração'},podeCadastrar:false};
  else if(p==='/api/usuarios/me/perfil')body={usuarioId:usuario,telefone:'',emailPessoal:'',endereco:'',foto:''};
  else if(p==='/api/usuarios/me/tema')body={usuarioId:usuario,tema:'verde'};
  else if(p==='/api/empresas/minha/identidade')body={empresaId:empresa,nome:'Escritório de demonstração',identidade:{imagens:{},usos:{},zoom:1,posX:50,posY:50}};
  else if(p==='/api/equipe/me')body={empresaId:empresa,papel:modo==='master'?'MASTER':'MEMBRO',perfil:modo==='master'?'ADMINISTRADOR':'FINANCEIRO',modulos:['inicio','config','agenda','contatos','parceiros','equipe','casos','crm'],enviaDocumento:false};
  else if(p==='/api/agenda/lembretes' && e.request.method==='GET'){const itens=avisos(),pagina=Number(u.searchParams.get('pagina')||0),tamanho=Number(u.searchParams.get('tamanho')||20);body={preferencias,itens:itens.slice(pagina*tamanho,(pagina+1)*tamanho),total:itens.length,pagina,tamanho,consultadoEm:new Date().toISOString(),fusoHorario:'America/Sao_Paulo'};}
  else if(p==='/api/agenda/lembretes/preferencias' && e.request.method==='PUT'){preferencias=JSON.parse(e.request.postData);body=preferencias;}
  else if(p.startsWith('/api/agenda/lembretes/') && e.request.method==='PATCH'){
    if(falhaLeitura){falhaLeitura=false;status=404;body={detail:'O aviso foi alterado. Atualize o painel.'};}
    else{const dados=JSON.parse(e.request.postData),id=p.split('/')[4];lidos.add(id+':'+dados.versao+':'+dados.marco);status=204;body=null;}
  }
  else if(p==='/api/crm/notificacoes')body=[];
  else if(p==='/api/agenda/opcoes')body={podeEditar:modo==='master',podeVincularContatos:true,responsaveis:pessoas,tiposTarefa:[{id:'tipo',nome:'Petição inicial',ativo:true}],fusoHorario:'America/Sao_Paulo'};
  else if(p==='/api/agenda/workflows' && e.request.method==='GET')body=[{id:'modelo',nome:'Preparar petição',versao:1,buffer:1,quantidadeEtapas:2}];
  else if(p==='/api/agenda/workflows/previsao'){
    const c=JSON.parse(e.request.postData);body={...c,assinatura:'a'.repeat(64),nome:'Preparar petição',buffer:1,etapas:[{ordem:1,nome:'Redigir',responsavel:usuario,responsavelNome:'Responsável',dias:2,inicio:hoje,fim:hoje},{ordem:2,nome:'Protocolar',responsavel:null,responsavelNome:'',dias:1,inicio:hoje,fim:hoje}]};
  }
  else if(p==='/api/agenda/workflows/execucoes'){
    const c=JSON.parse(e.request.postData);assert(c.requisicaoId);assert.equal(c.assinaturaPlano,'a'.repeat(64));
    const ids=[crypto.randomUUID(),crypto.randomUUID()];
    ids.forEach((id,i)=>atividades.push({...atividades[0],id,dados:{...atividades[0].dados,tipo:'TAREFA',titulo:i?'Protocolar':'Redigir',data:hoje,diaInteiro:true,hora:null,horaFim:null},workflow:{execucaoId:'execucao',nome:'Preparar petição',ordem:i+1,totalEtapas:2,prazoFatal:c.prazoFatal,inicioPlanejado:hoje,fimPlanejado:hoje}}));
    body={id:'execucao',plano:{},atividades:ids};
  }
  else if(p==='/api/agenda/contatos')body=[{id:'cliente-maria',nome:'Maria Silva',ativo:true},{id:'advogado-joao',nome:'João Oliveira',ativo:true}].filter(c=>u.searchParams.has('ids')?u.searchParams.get('ids').split(',').includes(c.id):c.nome.toLowerCase().includes((u.searchParams.get('busca')||'').toLowerCase()));
  else if(p==='/api/agenda/vinculos')body={itens:[
    {id:'consulta',leadId:'00000000-0000-0000-0000-000000000021',tarefaId:null,casoId:null,titulo:'Consulta — Maria Silva',tipo:'REUNIAO',data:hoje,dataFim:null,hora:'14:30',horaFim:null,prazoFatal:null,diaInteiro:false,local:'',situacao:'PENDENTE',responsaveis:[]},
    {id:'tarefa',leadId:null,tarefaId:'00000000-0000-0000-0000-000000000022',casoId:'00000000-0000-0000-0000-000000000023',titulo:'Revisar contrato do caso',tipo:'TAREFA',data:hoje,dataFim:null,hora:null,horaFim:null,prazoFatal:futuro,diaInteiro:true,local:'Escritório',situacao:'PENDENTE',responsaveis:[pessoas[0]]}
  ],total:2,pagina:0,tamanho:20};
  else if(p==='/api/casos/opcoes')body={podeEditar:true,podeCriarCliente:true,podeVerFinanceiro:false,clientes:[],grupos:[],membros:pessoas};
  else if(p==='/api/casos/00000000-0000-0000-0000-000000000023')body={caso:{id:'00000000-0000-0000-0000-000000000023',versao:0,titulo:'Consultoria contratual',status:'ativo',clienteId:null,cliente:null,grupoId:null,grupo:null,responsavelId:null,responsavel:null,partesAdversas:'',observacoes:'',valorProposta:null,propostaEm:null,desfecho:'',valorFechado:null,motivoPerda:'',fechadoEm:null,leadId:null,criadoEm:new Date().toISOString(),atualizadoEm:new Date().toISOString()},anotacoes:[],historicoLead:{}};
  else if(p.endsWith('/tarefas/opcoes'))body={podeEditar:true,tipos:[],membros:pessoas};
  else if(p.endsWith('/tarefas'))body=[{id:'00000000-0000-0000-0000-000000000022',versao:0,status:'pendente',dados:{tipoId:null,titulo:'Revisar contrato do caso',data:hoje,dataFim:null,hora:null,horaFim:null,prazoFatal:futuro,local:'Escritório',descricao:'Tarefa vinculada à Agenda.',mostrarAgenda:true,diaInteiro:true,repeticao:'nunca',flags:[],responsaveis:[usuario]},tipo:null,responsaveis:pessoas.slice(0,1),concluidaPor:null,concluidaEm:null,comentarios:[]}];

  else if(p==='/api/agenda/calendario'){
    body=[];for(let d=new Date(u.searchParams.get('inicio')+'T12:00:00');data(d)<=u.searchParams.get('fim');d.setDate(d.getDate()+1)){const dia=data(d),itens=atividades.filter(a=>corresponde(a,u) && (a.dados.data && a.dados.data<=dia && (a.dados.dataFim || a.dados.data)>=dia || a.dados.prazoFatal===dia));body.push({data:dia,total:itens.length,pendentes:itens.filter(a=>a.situacao==='PENDENTE').length,prazosFatais:itens.filter(a=>a.situacao==='PENDENTE'&&a.dados.prazoFatal===dia).length});}
  } else if(p==='/api/agenda' && e.request.method==='GET'){const itens=atividades.filter(a=>corresponde(a,u));body={itens,total:itens.length,pagina:0,tamanho:50};}
  else if(p.startsWith('/api/agenda')){
    const id=p.split('/')[3];let atual=atividades.find(a=>a.id===id);
    if(e.request.method==='GET')body=atual;
    else if(e.request.method==='POST'){const d=JSON.parse(e.request.postData);const {versao,...dados}=d;body={id:crypto.randomUUID(),versao:0,dados,situacao:'PENDENTE',concluidaEm:null,criadoEm:new Date().toISOString(),atualizadoEm:new Date().toISOString(),historico:[{id:crypto.randomUUID(),autor:'Responsável',acao:'CRIADA',observacao:'',criadoEm:new Date().toISOString()}]};atividades.push(body);}
    else if(e.request.method==='PUT' && conflito){conflito=false;atual.versao++;status=409;body={detail:'A atividade foi alterada em outra sessão. Recarregue a ficha antes de salvar.'};}
    else if(e.request.method==='PUT'){const {versao,...dados}=JSON.parse(e.request.postData);assert.equal(versao,atual.versao);body={...atual,dados,versao:versao+1,historico:[...atual.historico,{id:crypto.randomUUID(),autor:'Responsável',acao:'EDITADA',observacao:'',criadoEm:new Date().toISOString()}]};atividades=atividades.map(a=>a.id===id?body:a);}
    else if(e.request.method==='PATCH'){const d=JSON.parse(e.request.postData);assert.equal(d.versao,atual.versao);body={...atual,situacao:d.situacao,versao:d.versao+1,concluidaEm:d.situacao==='CONCLUIDA'?new Date().toISOString():null,historico:[...atual.historico,{id:crypto.randomUUID(),autor:'Responsável',acao:d.situacao==='PENDENTE'?'REABERTA':d.situacao,observacao:d.observacao,criadoEm:new Date().toISOString()}]};atividades=atividades.map(a=>a.id===id?body:a);
      if(d.situacao==='CONCLUIDA' && atual.dados.repeticao==='SEMANAL' && !atividades.some(a=>a.anteriorId===id)){
        const dataNova=new Date(atual.dados.data+'T12:00:00');dataNova.setDate(dataNova.getDate()+7);
        atividades.push({...body,id:crypto.randomUUID(),anteriorId:id,versao:0,situacao:'PENDENTE',concluidaEm:null,dados:{...atual.dados,data:data(dataNova),prazoFatal:null},historico:[]});
      }
    }
    else {status=403;body={detail:'Fora do cenário'};}
  }else {status=403;body={detail:'Fora do cenário visual'};}
  await request('Fetch.fulfillRequest',{requestId:e.requestId,responseCode:status,responseHeaders:[{name:'Content-Type',value:'application/json'},{name:'Access-Control-Allow-Origin',value:'http://localhost:4200'},{name:'Access-Control-Allow-Credentials',value:'true'},{name:'Access-Control-Allow-Headers',value:'Content-Type,X-CSRF-TOKEN'},{name:'Access-Control-Allow-Methods',value:'GET,POST,PUT,PATCH,OPTIONS'}],body:Buffer.from(JSON.stringify(body)).toString('base64')});
}
async function evaluate(expression){const r=await request('Runtime.evaluate',{expression,returnByValue:true});if(r.exceptionDetails)throw new Error(JSON.stringify(r.exceptionDetails));return r.result.value;}
async function waitFor(expression){for(let i=0;i<120;i++){if(await evaluate(expression))return;await sleep(100);}throw new Error(expression+' não ocorreu; '+await evaluate('document.body.innerText'));}
const click=text=>evaluate(`Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()===${JSON.stringify(text)}).click()`);
const fill=(name,value)=>evaluate(`(()=>{const e=document.querySelector('dialog[open] [name="${name}"]') || document.querySelector('[name="${name}"]');e.value=${JSON.stringify(value)};e.dispatchEvent(new Event('input',{bubbles:true}));})()`);
const select=(name,value)=>evaluate(`(()=>{const e=document.querySelector('dialog[open] [name="${name}"]') || document.querySelector('[name="${name}"]');e.value=${JSON.stringify(value)};e.dispatchEvent(new Event('change',{bubbles:true}));})()`);
async function screenshot(name){const r=await request('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});await fs.writeFile('docs/'+name,Buffer.from(r.data,'base64'));}

try {
  let page;for(let i=0;i<100;i++){try{page=(await(await fetch('http://127.0.0.1:9247/json')).json()).find(p=>p.type==='page');if(page)break;}catch{}await sleep(100);}assert(page);
  ws=new WebSocket(page.webSocketDebuggerUrl);await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject;});
  ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);if(p){clearTimeout(p.timeout);pending.delete(m.id);m.error?p.reject(new Error(JSON.stringify(m.error))):p.resolve(m.result);}}else if(m.method==='Fetch.requestPaused')fulfill(m.params).catch(e=>errors.push(e.message));else if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.text);};
  await request('Page.enable');await request('Runtime.enable');await request('Fetch.enable',{patterns:[{urlPattern:'*/api/*'}]});await request('Emulation.setDeviceMetricsOverride',{width:1440,height:1080,deviceScaleFactor:1,mobile:false});
  await request('Page.navigate',{url:'http://localhost:4200/agenda'});await waitFor("document.body.innerText.includes('Nenhuma atividade')");
  await waitFor("document.querySelector('app-vinculos-agenda')?.innerText.includes('Consulta — Maria Silva')");
  assert(await evaluate("!!document.querySelector('app-vinculos-agenda a[href*=lead]')"));
  assert(await evaluate("!!document.querySelector('app-vinculos-agenda a[href*=tarefa]')"));
  await screenshot('agenda-vinculos-desktop.png');
  await request('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});
  await evaluate("document.querySelector('app-vinculos-agenda').scrollIntoView()");
  assert(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'));
  await screenshot('agenda-vinculos-mobile.png');
  await evaluate("document.querySelector('app-vinculos-agenda a[href*=tarefa]').click()");
  await waitFor("document.querySelector('app-tarefas-caso dialog[open]')?.innerText.includes('Revisar contrato do caso')");
  assert(await evaluate("location.search.includes('aba=tarefas') && location.search.includes('tarefa=')"));
  assert(await evaluate("document.querySelector('app-tarefas-caso dialog[open]').innerText.includes('Tarefa vinculada à Agenda.')"));
  await screenshot('agenda-vinculos-tarefa-mobile.png');
  assert.deepEqual(errors,[]);console.log('Chrome desktop/celular: consultas e tarefas na Agenda, links para a origem e abertura da tarefa exata com suas permissões; sem overflow ou exceções JavaScript. APIs simuladas.');
}finally{if(ws)ws.close();chrome.kill();}
