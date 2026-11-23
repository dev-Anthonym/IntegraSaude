/*
 * IntegraSaude — frontend web.
 * Página única com rotas no "#/...". Toda regra de negócio fica no servidor Java;
 * aqui só se chama a API (/api/...) e se desenham as telas.
 */
'use strict';

(function () {
  const app = document.getElementById('app');
  const abas = document.getElementById('abas');
  const aviso = document.getElementById('aviso');

  const ATUALIZAR_A_CADA_MS = 20000;
  const DURACAO_AVISO_MS = 3800;

  let usuario = null;      // { nome, primeiroNome, email, perfil }
  let atual = null;        // { caminho, params, tela }
  let acoes = {};          // ações da tela desenhada (data-acao / data-enviar)
  let pausado = false;     // não atualiza sozinho enquanto há uma confirmação aberta
  let geracao = 0;         // descarta respostas de telas que já foram trocadas
  let timerAviso = null;

  // ---------------------------------------------------------------- Ícones

  const svg = (corpo) => '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" ' +
    'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + corpo + '</svg>';
  const ICONE = {
    agenda: svg('<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M8 3v4M16 3v4M3 10h18"/>'),
    sino: svg('<path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>'),
    remedio: svg('<path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z"/><path d="m8.5 8.5 7 7"/>'),
    consulta: svg('<rect x="3" y="3" width="18" height="18" rx="4"/><path d="M12 8v8M8 12h8"/>'),
    pessoas: svg('<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>' +
      '<path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>'),
    mais: svg('<path d="M12 5v14M5 12h14"/>'),
    voltar: svg('<path d="m15 18-6-6 6-6"/>'),
    lixeira: svg('<path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v6M14 11v6"/>'),
    relogio: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>'),
    info: svg('<circle cx="12" cy="12" r="9"/><path d="M12 16v-5M12 8h.01"/>')
  };

  const ABAS = {
    PACIENTE: [
      { rota: 'paciente/agenda', rotulo: 'Agenda', icone: ICONE.agenda },
      { rota: 'paciente/lembretes', rotulo: 'Lembretes', icone: ICONE.sino },
      { rota: 'paciente/remedios', rotulo: 'Remédios', icone: ICONE.remedio },
      { rota: 'paciente/consultas', rotulo: 'Consultas', icone: ICONE.consulta },
      { rota: 'paciente/responsaveis', rotulo: 'Responsáveis', icone: ICONE.pessoas }
    ],
    RESPONSAVEL: [
      { rota: 'responsavel/pacientes', rotulo: 'Pacientes', icone: ICONE.pessoas },
      { rota: 'responsavel/lembretes', rotulo: 'Lembretes', icone: ICONE.sino },
      { rota: 'responsavel/agenda', rotulo: 'Agenda', icone: ICONE.agenda }
    ]
  };

  const STATUS = {
    CONFIRMADO: ['Confirmado', 'verde'],
    PENDENTE: ['Pendente', 'laranja'],
    PERDIDO: ['Perdido', 'vermelho'],
    EM_BREVE: ['Em breve', 'cinza'],
    AGENDADA: ['Agendada', 'cinza'],
    SEM_REGISTRO: ['Sem lembrete', 'cinza'],
    REALIZADA: ['Realizada', 'verde'],
    CANCELADA: ['Cancelada', 'vermelho']
  };

  // ---------------------------------------------------------------- Utilidades

  const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  const esc = (valor) => String(valor ?? '').replace(/[&<>"']/g, (c) => ESCAPES[c]);
  const plural = (n, um, varios) => n + ' ' + (n === 1 ? um : varios);
  const pad = (n) => String(n).padStart(2, '0');
  const inicial = (nome) => esc((nome || '?').trim().charAt(0).toUpperCase());

  function listarNomes(nomes) {
    if (nomes.length <= 1) return nomes.join('');
    return nomes.slice(0, -1).join(', ') + ' e ' + nomes[nomes.length - 1];
  }

  function chip(status, extra) {
    const [texto, cor] = STATUS[status] || [status, 'cinza'];
    return '<span class="chip ' + cor + (extra ? ' ' + extra : '') + '">' + esc(texto) + '</span>';
  }

  function textoStatus(status) {
    const [texto, cor] = STATUS[status] || [status, 'cinza'];
    return '<span class="st texto-st ' + cor + '">' + esc(texto) + '</span>';
  }

  // Datas chegam do servidor como "aaaa-mm-dd" e são tratadas no fuso local
  const DIAS_CURTOS = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB'];
  const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro',
    'outubro', 'novembro', 'dezembro'];
  const DIAS_LONGOS = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];

  function paraData(iso) {
    const [a, m, d] = iso.split('-').map(Number);
    return new Date(a, m - 1, d);
  }
  function paraIso(data) {
    return data.getFullYear() + '-' + pad(data.getMonth() + 1) + '-' + pad(data.getDate());
  }
  function somarDias(iso, dias) {
    const data = paraData(iso);
    data.setDate(data.getDate() + dias);
    return paraIso(data);
  }
  function porExtenso(iso) {
    const d = paraData(iso);
    return DIAS_LONGOS[d.getDay()] + ', ' + d.getDate() + ' de ' + MESES[d.getMonth()];
  }
  function intervaloSemana(inicioIso) {
    const ini = paraData(inicioIso);
    const fim = paraData(somarDias(inicioIso, 6));
    if (ini.getMonth() === fim.getMonth()) {
      return ini.getDate() + ' a ' + fim.getDate() + ' de ' + MESES[fim.getMonth()];
    }
    return ini.getDate() + ' de ' + MESES[ini.getMonth()] + ' a ' + fim.getDate() + ' de ' + MESES[fim.getMonth()];
  }
  function ddmm(iso) {
    const d = paraData(iso);
    return pad(d.getDate()) + '/' + pad(d.getMonth() + 1);
  }
  function rotuloDia(iso, hoje) {
    if (iso === hoje) return 'Hoje';
    if (iso === somarDias(hoje, -1)) return 'Ontem';
    if (iso === somarDias(hoje, 1)) return 'Amanhã';
    return DIAS_CURTOS[paraData(iso).getDay()] + ', ' + ddmm(iso);
  }
  function hojeLocal() {
    return paraIso(new Date());
  }

  // Agrupa itens já ordenados pela chave "data", preservando a ordem
  function agruparPorData(itens) {
    const grupos = [];
    for (const item of itens) {
      const ultimo = grupos[grupos.length - 1];
      if (ultimo && ultimo.data === item.data) ultimo.itens.push(item);
      else grupos.push({ data: item.data, itens: [item] });
    }
    return grupos;
  }

  function mostrarAviso(texto, tipo) {
    aviso.textContent = texto;
    aviso.className = 'aviso' + (tipo === 'erro' ? ' erro' : '');
    aviso.hidden = false;
    clearTimeout(timerAviso);
    timerAviso = setTimeout(() => { aviso.hidden = true; }, DURACAO_AVISO_MS);
  }

  // ---------------------------------------------------------------- API

  // Erro já tratado (ex.: sessão expirada levou ao login): não mostra mensagem
  class ErroSilencioso extends Error {}

  async function chamar(metodo, url, dados) {
    const opcoes = { method: metodo, credentials: 'same-origin', headers: { 'X-Requested-With': 'fetch' } };
    if (dados) opcoes.body = new URLSearchParams(dados);
    let resposta;
    try {
      resposta = await fetch(url, opcoes);
    } catch (e) {
      throw new Error('Sem conexão com o servidor. Confira se o IntegraSaude ainda está rodando.');
    }
    const corpo = await resposta.json().catch(() => ({}));
    if (resposta.status === 401 && url !== '/api/login') {
      usuario = null;
      ir('login');
      throw new ErroSilencioso();
    }
    if (!resposta.ok) throw new Error(corpo.erro || 'Algo deu errado. Tente novamente.');
    return corpo;
  }
  const get = (url) => chamar('GET', url);
  const post = (url, dados) => chamar('POST', url, dados || {});

  // ---------------------------------------------------------------- Rotas

  function ir(rota) {
    const destino = '#/' + rota;
    if (location.hash === destino) navegar();
    else location.hash = destino;
  }

  function lerRota() {
    const [caminho, consulta] = location.hash.replace(/^#\/?/, '').split('?');
    return { caminho: caminho || '', params: new URLSearchParams(consulta || '') };
  }

  const inicio = () => (usuario && usuario.perfil === 'RESPONSAVEL' ? 'responsavel/pacientes' : 'paciente/agenda');

  async function navegar() {
    const { caminho, params } = lerRota();
    if (!usuario) {
      usuario = await fetch('/api/sessao', { credentials: 'same-origin' })
        .then((r) => (r.ok ? r.json() : null)).catch(() => null);
    }
    const tela = TELAS[caminho];
    if (!usuario) {
      if (!tela || tela.perfil) return ir('login');
    } else if (!tela || !tela.perfil || tela.perfil !== usuario.perfil) {
      return ir(inicio());
    }
    atual = { caminho, params, tela };
    desenharAbas();
    await desenhar(true);
  }

  function desenharAbas() {
    const mostrar = atual.tela.abas && usuario;
    abas.hidden = !mostrar;
    app.classList.toggle('sem-abas', !mostrar);
    if (!mostrar) return;
    const secao = atual.caminho.split('/').slice(0, 2).join('/');
    abas.innerHTML = '<ul>' + ABAS[usuario.perfil].map((a) =>
      '<li><a href="#/' + a.rota + '"' + (a.rota === secao ? ' aria-current="page"' : '') + '>' +
      a.icone + '<span>' + a.rotulo + '</span></a></li>').join('') + '</ul>';
  }

  // Desenha a tela atual. "navegacao" = veio de um clique (rola para o topo e mostra erros na tela)
  async function desenhar(navegacao) {
    const minha = ++geracao;
    const { tela, params } = atual;
    const carregando = navegacao ? setTimeout(() => {
      if (minha === geracao) app.innerHTML = '<p class="carregando">Carregando…</p>';
    }, 250) : null;
    try {
      const resultado = await tela.desenhar(params);
      if (minha !== geracao) return;
      const rolagem = window.scrollY;
      app.innerHTML = resultado.html;
      acoes = resultado.acoes || {};
      pausado = false;
      if (resultado.montar) resultado.montar(app);
      if (navegacao) {
        window.scrollTo(0, 0);
        const titulo = app.querySelector('h1');
        if (titulo) { titulo.tabIndex = -1; titulo.focus({ preventScroll: true }); }
      } else {
        window.scrollTo(0, rolagem);
      }
    } catch (e) {
      if (e instanceof ErroSilencioso || minha !== geracao) return;
      if (navegacao) {
        app.innerHTML = '<div class="erro-form">' + esc(e.message) + '</div>' +
          '<button class="botao contorno" data-acao="tentar">Tentar de novo</button>';
        acoes = { tentar: () => desenhar(true) };
      }
    } finally {
      clearTimeout(carregando);
    }
  }

  const redesenhar = () => desenhar(false);

  // Cliques e envios de formulário passam por aqui (delegação de eventos)
  app.addEventListener('click', (evento) => {
    const alvo = evento.target.closest('[data-ir], [data-acao]');
    if (!alvo || !app.contains(alvo)) return;
    if (alvo.dataset.ir !== undefined) {
      evento.preventDefault();
      ir(alvo.dataset.ir);
      return;
    }
    const acao = acoes[alvo.dataset.acao];
    if (acao) {
      evento.preventDefault();
      executar(acao, alvo);
    }
  });

  app.addEventListener('submit', (evento) => {
    evento.preventDefault();
    const formulario = evento.target;
    const acao = acoes[formulario.dataset.enviar];
    if (acao) enviar(formulario, acao);
  });

  // Executa uma ação de botão, travando o botão até terminar
  async function executar(acao, botao) {
    if (botao.disabled) return;
    botao.disabled = true;
    try {
      await acao(botao);
    } catch (e) {
      if (!(e instanceof ErroSilencioso)) mostrarAviso(e.message, 'erro');
    } finally {
      botao.disabled = false;
    }
  }

  // Envia um formulário; o erro aparece no topo do próprio formulário
  async function enviar(formulario, acao) {
    const botao = formulario.querySelector('button[type="submit"]');
    const erro = formulario.querySelector('.erro-form');
    if (botao && botao.disabled) return;
    if (erro) erro.hidden = true;
    if (botao) botao.disabled = true;
    try {
      await acao(Object.fromEntries(new FormData(formulario)), formulario);
    } catch (e) {
      if (e instanceof ErroSilencioso) return;
      if (erro) {
        erro.textContent = e.message;
        erro.hidden = false;
        erro.scrollIntoView({ block: 'center', behavior: 'smooth' });
      } else {
        mostrarAviso(e.message, 'erro');
      }
    } finally {
      if (botao) botao.disabled = false;
    }
  }

  // Confirmação dentro do cartão (sem janela do navegador)
  function abrirConfirmacao(botao) {
    const cartao = botao.closest('.cartao');
    app.querySelectorAll('[data-confirmacao]').forEach((c) => { c.hidden = true; });
    const caixa = cartao.querySelector('[data-confirmacao]');
    caixa.hidden = false;
    pausado = true;
    caixa.querySelector('button').focus();
  }
  function fecharConfirmacao(botao) {
    botao.closest('[data-confirmacao]').hidden = true;
    pausado = false;
  }

  async function sair() {
    await post('/api/logout').catch(() => null);
    usuario = null;
    ir('login');
  }

  // ---------------------------------------------------------------- Pedaços de tela

  function cabecalho(titulo, opcoes) {
    const o = opcoes || {};
    return '<header class="cabecalho' + (o.sobretitulo ? '' : ' centro') + '"><div>' +
      (o.sobretitulo ? '<p class="sobretitulo">' + esc(o.sobretitulo) + '</p>' : '') +
      '<h1>' + esc(titulo) + '</h1></div>' +
      (o.sair ? '<button type="button" class="sair" data-acao="sair">Sair</button>' : '') +
      (o.mais ? '<button type="button" class="botao-mais" data-ir="' + o.mais + '" aria-label="' +
        esc(o.rotuloMais) + '">' + ICONE.mais + '</button>' : '') +
      '</header>';
  }

  function segmentos(opcoes, ativo) {
    return '<div class="segmentos" role="group">' + opcoes.map(([rotulo, rota], i) =>
      '<button type="button" data-ir="' + rota + '" aria-pressed="' + (i === ativo) + '">' +
      esc(rotulo) + '</button>').join('') + '</div>';
  }

  function voltar(rotulo, rota) {
    return '<button type="button" class="voltar" data-ir="' + rota + '">' + ICONE.voltar + esc(rotulo) + '</button>';
  }

  function lista(itens, vazio) {
    if (!itens.length) return '<p class="vazio-lista">' + esc(vazio) + '</p>';
    return '<div class="lista">' + itens.join('') + '</div>';
  }

  function caixaInfo(icone, texto) {
    return '<div class="info">' + icone + '<p>' + esc(texto) + '</p></div>';
  }

  function campo(rotulo, entrada, dica) {
    return '<label class="campo"><span>' + esc(rotulo) + '</span>' + entrada +
      (dica ? '<small class="dica">' + esc(dica) + '</small>' : '') + '</label>';
  }

  function itemAgenda(item, tituloNormal) {
    return '<div class="cartao item-agenda"><span class="hora">' + esc(item.hora) + '</span><div>' +
      '<div class="titulo' + (tituloNormal ? ' normal' : '') + '">' + esc(item.titulo) + '</div>' +
      (item.subtitulo && !tituloNormal ? '<div class="sub">' + esc(item.subtitulo) + '</div>' : '') +
      '</div>' + chip(item.status) + '</div>';
  }

  function botaoConfirmacao(pergunta, acaoSim, rotuloSim, id) {
    return '<div class="confirmacao" data-confirmacao hidden><span>' + esc(pergunta) + '</span>' +
      '<button type="button" class="botao perigo cheio" data-acao="' + acaoSim + '" data-id="' + esc(id) + '">' +
      esc(rotuloSim) + '</button>' +
      '<button type="button" class="botao texto" data-acao="fechar-confirmacao">Voltar</button></div>';
  }

  // Linha "Previsto 08:00 · confirmado às 08:12" usada nos históricos
  function linhaHistorico(l) {
    const previsto = l.tipo === 'CONSULTA' ? 'Lembrete às ' + l.previsto : 'Previsto ' + l.previsto;
    if (l.status === 'CONFIRMADO') {
      return previsto + (l.confirmadoAs ? ' · confirmado às ' + l.confirmadoAs : ' · confirmado');
    }
    return previsto + ' · prazo encerrado às ' + l.prazo;
  }

  function tituloLembrete(l) {
    return l.tipo === 'CONSULTA' && l.subtitulo ? l.titulo + ' · ' + l.subtitulo : l.titulo;
  }

  function historicoAgrupado(itens, hoje, titulo, destinatarioAlerta) {
    if (!itens.length) {
      return '<p class="vazio-lista">Ainda não há lembretes confirmados ou perdidos.</p>';
    }
    return agruparPorData(itens).map((grupo) =>
      '<h2 class="rotulo-secao">' + esc(rotuloDia(grupo.data, hoje)) + '</h2><div class="lista">' +
      grupo.itens.map((l) =>
        '<article class="cartao"><div class="cartao-topo"><h3>' + esc(titulo(l)) + '</h3>' + chip(l.status) +
        '</div><p class="linha-suave">' + esc(linhaHistorico(l)) + '</p>' +
        (l.alertaAs ? '<p class="linha-alerta">' + ICONE.sino + esc('Alerta enviado a ' + destinatarioAlerta +
          ' às ' + l.alertaAs) + '</p>' : '') +
        '</article>').join('') + '</div>').join('');
  }

  const acoesComuns = {
    sair,
    'abrir-confirmacao': abrirConfirmacao,
    'fechar-confirmacao': fecharConfirmacao
  };

  // ---------------------------------------------------------------- Login e cadastro

  async function telaLogin() {
    const html = '<section class="login">' +
      '<div class="logo">' + ICONE.mais + '</div>' +
      '<h1>IntegraSaude</h1>' +
      '<p class="descricao">Lembretes de remédios e consultas, com quem cuida de você por perto.</p>' +
      '<form data-enviar="entrar" novalidate><div class="erro-form" role="alert" hidden></div>' +
      campo('E-mail', '<input name="email" type="email" autocomplete="username" inputmode="email" ' +
        'placeholder="maria@demo.com" required>') +
      campo('Senha', '<input name="senha" type="password" autocomplete="current-password" required>') +
      '<button type="submit" class="botao primario bloco">Entrar</button></form>' +
      '<p class="criar">Ainda não tem conta? <button type="button" data-ir="cadastro">Criar conta</button></p>' +
      '<div class="perfis-login"><p class="rotulo-secao">Ao criar a conta, escolha o perfil</p><div class="grade">' +
      '<button type="button" data-ir="cadastro?perfil=PACIENTE"><strong>Paciente</strong>' +
      '<small>Recebe e confirma os lembretes</small></button>' +
      '<button type="button" data-ir="cadastro?perfil=RESPONSAVEL"><strong>Responsável</strong>' +
      '<small>Acompanha e recebe alertas</small></button></div></div></section>';
    return {
      html,
      montar: (raiz) => raiz.querySelector('input[name="email"]').focus(),
      acoes: {
        entrar: async (dados) => {
          usuario = await post('/api/login', dados);
          ir(inicio());
        }
      }
    };
  }

  async function telaCadastro(params) {
    const perfil = params.get('perfil') === 'RESPONSAVEL' ? 'RESPONSAVEL' : 'PACIENTE';
    const opcao = (valor, titulo, texto) =>
      '<label class="opcao-perfil"><input type="radio" name="perfil" value="' + valor + '"' +
      (perfil === valor ? ' checked' : '') + '><span><strong>' + titulo + '</strong><small>' + texto +
      '</small></span></label>';
    const html = voltar('Voltar', 'login') +
      '<h1>Criar conta</h1><p class="descricao">Leva menos de um minuto.</p>' +
      '<form class="formulario" data-enviar="criar" novalidate><div class="erro-form" role="alert" hidden></div>' +
      campo('Nome completo', '<input name="nome" autocomplete="name" required>') +
      campo('E-mail', '<input name="email" type="email" autocomplete="email" inputmode="email" required>') +
      campo('Senha', '<input name="senha" type="password" autocomplete="new-password" minlength="8" required>',
        'Mínimo de 8 caracteres, com letra e número. A senha é guardada de forma protegida, nunca em texto aberto.') +
      '<fieldset class="campo"><legend class="rotulo-campo">Seu perfil</legend><div class="opcoes-perfil">' +
      opcao('PACIENTE', 'Paciente', 'Recebe e confirma os lembretes') +
      opcao('RESPONSAVEL', 'Responsável', 'Acompanha pacientes e recebe alertas') +
      '</div></fieldset>' +
      '<div class="acoes-rodape"><button type="submit" class="botao primario bloco">Criar conta</button></div></form>';
    return {
      html,
      montar: (raiz) => raiz.querySelector('input[name="nome"]').focus(),
      acoes: {
        criar: async (dados) => {
          usuario = await post('/api/cadastro', dados);
          mostrarAviso('Conta criada. Bem-vindo(a), ' + usuario.primeiroNome + '!');
          ir(inicio());
        }
      }
    };
  }

  // ---------------------------------------------------------------- Paciente: Agenda

  async function confirmarLembrete(botao) {
    await post('/api/paciente/lembretes/confirmar', { id: botao.dataset.id });
    mostrarAviso('Confirmado! Registrado no histórico.');
    await redesenhar();
  }

  async function pacienteAgendaDia(params) {
    const dia = params.get('dia');
    const d = await get('/api/paciente/agenda' + (dia ? '?dia=' + encodeURIComponent(dia) : ''));
    const agora = d.agora
      ? '<div class="cartao alerta agora"><div><span class="etiqueta">Agora · prazo ' + esc(d.agora.prazo) +
        '</span><div class="nome">' + esc(d.agora.titulo) + '</div></div>' +
        '<button type="button" class="botao primario" data-acao="confirmar" data-id="' + esc(d.agora.id) +
        '">Confirmar</button></div>'
      : '';
    const html = cabecalho('Agenda', { sobretitulo: porExtenso(d.dia), sair: true }) +
      segmentos([['Dia', 'paciente/agenda'], ['Semana', 'paciente/agenda/semana']], 0) +
      (d.dia !== d.hoje ? '<p class="nota"><button type="button" class="sair" data-ir="paciente/agenda">' +
        '← Voltar para hoje</button></p>' : '') +
      agora +
      lista(d.itens.map((i) => itemAgenda(i)), 'Nada na agenda deste dia.');
    return { html, acoes: { ...acoesComuns, confirmar: confirmarLembrete } };
  }

  async function pacienteAgendaSemana() {
    const d = await get('/api/paciente/semana');
    const dias = d.dias.map((dia) => {
      const linhas = [];
      if (dia.horariosRemedios.length) linhas.push(dia.horariosRemedios.join(' · ') + ' Remédios');
      dia.consultas.forEach((c) => linhas.push(c.hora + ' Consulta · ' + c.titulo));
      return cartaoDiaSemana(dia.data, d.hoje, linhas, 'paciente/agenda');
    });
    const html = cabecalho('Agenda', { sobretitulo: intervaloSemana(d.inicio), sair: true }) +
      segmentos([['Dia', 'paciente/agenda'], ['Semana', 'paciente/agenda/semana']], 1) +
      '<div class="lista">' + dias.join('') + '</div>';
    return { html, acoes: acoesComuns };
  }

  function cartaoDiaSemana(dataIso, hoje, linhas, rotaDia) {
    return '<button type="button" class="cartao clicavel dia-semana' + (dataIso === hoje ? ' destaque' : '') +
      '" data-ir="' + rotaDia + '?dia=' + esc(dataIso) + '" aria-label="Ver ' + esc(porExtenso(dataIso)) + '">' +
      '<span class="data-dia"><span class="sigla">' + DIAS_CURTOS[paraData(dataIso).getDay()] + '</span>' +
      '<span class="numero">' + paraData(dataIso).getDate() + '</span></span>' +
      '<span class="resumo">' + (linhas.length
        ? linhas.map((l) => '<span class="linha">' + esc(l) + '</span>').join('')
        : '<span class="vazio">Nada agendado</span>') + '</span></button>';
  }

  // ---------------------------------------------------------------- Paciente: Lembretes

  async function pacienteLembretes() {
    const d = await get('/api/paciente/lembretes');
    const cartoes = d.pendentes.map((l) => {
      const ehRemedio = l.tipo === 'MEDICAMENTO';
      const detalhe = ehRemedio
        ? 'horário previsto ' + l.previsto
        : (l.subtitulo ? l.subtitulo + ' · ' : '') + 'consulta às ' + (l.consultaAs || l.previsto);
      return '<article class="cartao' + (l.devido ? ' alerta' : '') + '">' +
        '<div class="cartao-topo"><span class="etiqueta">' + (ehRemedio ? 'Medicamento' : 'Consulta') + '</span>' +
        '<span class="chip ' + (l.devido ? 'branco' : '') + '">Prazo ' + esc(l.prazo) + '</span></div>' +
        '<div class="titulo-grande">' + esc(l.titulo) + '</div>' +
        '<p class="linha-suave">' + esc(detalhe) + '</p>' +
        '<button type="button" class="botao ' + (l.devido ? 'primario' : 'contorno') + ' bloco espaco" ' +
        'data-acao="confirmar" data-id="' + esc(l.id) + '">' +
        (ehRemedio ? 'Confirmar que tomei' : 'Confirmar presença') + '</button></article>';
    });
    const html = cabecalho('Lembretes') +
      segmentos([['Pendentes', 'paciente/lembretes'], ['Histórico', 'paciente/lembretes/historico']], 0) +
      lista(cartoes, 'Nenhum lembrete pendente. Eles aparecem aqui no horário de cada remédio e antes de cada consulta.') +
      caixaInfo(ICONE.relogio, 'Você tem ' + plural(d.tempoLimite, 'minuto', 'minutos') +
        ' depois do horário para confirmar. ' +
        'Passado o prazo, o lembrete vira Perdido e seus responsáveis recebem um aviso.');
    return { html, acoes: { ...acoesComuns, confirmar: confirmarLembrete } };
  }

  async function pacienteHistorico() {
    const d = await get('/api/paciente/historico');
    const destinatario = d.responsaveis.length ? listarNomes(d.responsaveis) : 'seus responsáveis';
    const html = cabecalho('Lembretes') +
      segmentos([['Pendentes', 'paciente/lembretes'], ['Histórico', 'paciente/lembretes/historico']], 1) +
      historicoAgrupado(d.itens, d.hoje, tituloLembrete, destinatario) +
      '<p class="nota">Cada lembrete guarda os eventos: gerado, confirmado, perdido e alerta enviado.</p>';
    return { html, acoes: acoesComuns };
  }

  // ---------------------------------------------------------------- Paciente: Remédios

  async function pacienteRemedios() {
    const remedios = await get('/api/paciente/medicamentos');
    const cartoes = remedios.map((m) =>
      '<article class="cartao"><div class="cartao-topo"><div>' +
      '<h2>' + esc(m.nome + ' ' + m.dosagem) + '</h2>' +
      '<p class="linha-suave">A cada ' + esc(m.frequencia) + ' h · primeiro horário ' + esc(m.primeiroHorario) + '</p>' +
      '<p class="linha-suave menor">' + (m.encerrado ? 'Tratamento encerrado em ' + ddmm(m.fim)
        : 'De ' + ddmm(m.inicio) + ' a ' + ddmm(m.fim)) + '</p></div>' +
      '<button type="button" class="lixeira" data-acao="abrir-confirmacao" aria-label="Remover ' +
      esc(m.nome) + '">' + ICONE.lixeira + '</button></div>' +
      (m.horarios.length ? '<div class="chips">' + m.horarios.map((h) => '<span class="chip hora">' + esc(h) +
        '</span>').join('') + '</div>' : '') +
      botaoConfirmacao('Remover este medicamento?', 'remover', 'Remover', m.id) + '</article>');
    const html = cabecalho('Medicamentos', { mais: 'paciente/remedios/novo', rotuloMais: 'Novo medicamento' }) +
      lista(cartoes, 'Nenhum medicamento cadastrado. Toque em + para adicionar.') +
      '<p class="nota">Os horários são calculados a partir do primeiro horário e da frequência.</p>';
    return {
      html,
      acoes: {
        ...acoesComuns,
        remover: async (botao) => {
          await post('/api/paciente/medicamentos/remover', { id: botao.dataset.id });
          mostrarAviso('Medicamento removido.');
          await redesenhar();
        }
      }
    };
  }

  // Prévia dos horários (a conta oficial é a do servidor). Se 24 é múltiplo da frequência, todo dia
  // repete o mesmo ciclo; senão, mostra o primeiro dia: do primeiro horário até a meia-noite
  function calcularHorarios(frequencia, primeiro) {
    const horas = Number(frequencia);
    if (!Number.isInteger(horas) || horas < 1 || horas > 24 || !/^\d{2}:\d{2}$/.test(primeiro || '')) return [];
    const [h, m] = primeiro.split(':').map(Number);
    const dia = 24 * 60;
    const passo = horas * 60;
    const minutos = [];
    if (dia % passo === 0) {
      for (let k = 0; k < dia / passo; k++) minutos.push((h * 60 + m + k * passo) % dia);
      minutos.sort((a, b) => a - b);
    } else {
      for (let t = h * 60 + m; t < dia; t += passo) minutos.push(t);
    }
    return minutos.map((t) => pad(Math.floor(t / 60)) + ':' + pad(t % 60));
  }

  async function pacienteNovoRemedio() {
    const hoje = hojeLocal();
    const html = voltar('Medicamentos', 'paciente/remedios') + '<h1>Novo medicamento</h1>' +
      '<form class="formulario espaco-titulo" data-enviar="salvar" novalidate>' +
      '<div class="erro-form" role="alert" hidden></div>' +
      campo('Nome', '<input name="nome" placeholder="Ex.: Metformina" required>') +
      campo('Dose', '<input name="dosagem" placeholder="Ex.: 850 mg" required>') +
      '<div class="dupla">' +
      campo('A cada (horas)', '<input name="frequencia" type="number" min="1" max="24" step="1" ' +
        'inputmode="numeric" value="12" required>') +
      campo('Primeiro horário', '<input name="primeiroHorario" type="time" value="08:00" required>') +
      '</div><div class="dupla">' +
      campo('Início', '<input name="inicio" type="date" value="' + hoje + '" required>') +
      campo('Fim', '<input name="fim" type="date" min="' + hoje + '" value="' + somarDias(hoje, 30) + '" required>') +
      '</div>' +
      '<div class="horarios-calculados" aria-live="polite"><span class="etiqueta">Horários calculados</span>' +
      '<div class="chips" data-horarios></div><p data-explicacao></p></div>' +
      '<div class="acoes-rodape"><button type="submit" class="botao primario bloco">Salvar medicamento</button>' +
      '<button type="button" class="botao texto bloco" data-ir="paciente/remedios">Cancelar</button></div></form>';
    return {
      html,
      montar: (raiz) => {
        const form = raiz.querySelector('form');
        const atualizar = () => {
          const frequencia = form.frequencia.value;
          const horarios = calcularHorarios(frequencia, form.primeiroHorario.value);
          raiz.querySelector('[data-horarios]').innerHTML = horarios.length
            ? horarios.map((h) => '<span class="chip">' + h + '</span>').join('')
            : '<span class="linha-suave">Informe a frequência (1 a 24 h) e o primeiro horário.</span>';
          raiz.querySelector('[data-explicacao]').textContent = horarios.length && 24 % Number(frequencia) !== 0
            ? 'Como 24 não é múltiplo de ' + frequencia + ', os horários mudam de um dia para o outro. ' +
              'Estes são os do primeiro dia; os lembretes seguem a sequência sozinhos.'
            : 'Os lembretes são gerados sozinhos nesses horários.';
        };
        form.addEventListener('input', atualizar);
        atualizar();
        form.nome.focus();
      },
      acoes: {
        salvar: async (dados) => {
          await post('/api/paciente/medicamentos', dados);
          mostrarAviso('Medicamento salvo. Os lembretes serão gerados nos horários.');
          ir('paciente/remedios');
        }
      }
    };
  }

  // ---------------------------------------------------------------- Paciente: Consultas

  async function pacienteConsultas() {
    const d = await get('/api/paciente/consultas');
    const quando = (c) => {
      const rotulo = rotuloDia(c.data, d.hoje);
      const dia = rotulo === 'Hoje' || rotulo === 'Amanhã' ? rotulo + ', ' + ddmm(c.data) : ddmm(c.data);
      return dia + ' às ' + c.hora;
    };
    const cartoes = d.consultas.map((c) =>
      '<article class="cartao"><div class="cartao-topo"><h2>' + esc(c.especialidade) + '</h2>' + chip(c.status) +
      '</div><p class="linha-suave">' + esc(quando(c) + ' · ' + c.medico) + '</p>' +
      '<p class="linha-suave menor">' + esc(c.local) + '</p>' +
      (c.podeCancelar ? '<div class="espaco"><button type="button" class="botao perigo" ' +
        'data-acao="abrir-confirmacao">Cancelar consulta</button></div>' +
        botaoConfirmacao('Cancelar esta consulta?', 'cancelar', 'Sim, cancelar', c.id) : '') +
      '</article>');
    const html = cabecalho('Consultas', { mais: 'paciente/consultas/nova', rotuloMais: 'Nova consulta' }) +
      lista(cartoes, 'Nenhuma consulta cadastrada. Toque em + para agendar.');
    return {
      html,
      acoes: {
        ...acoesComuns,
        cancelar: async (botao) => {
          await post('/api/paciente/consultas/cancelar', { id: botao.dataset.id });
          mostrarAviso('Consulta cancelada. Ela continua no histórico.');
          await redesenhar();
        }
      }
    };
  }

  async function pacienteNovaConsulta() {
    const especialidades = await get('/api/especialidades');
    const hoje = hojeLocal();
    const html = voltar('Consultas', 'paciente/consultas') + '<h1>Nova consulta</h1>' +
      '<form class="formulario espaco-titulo" data-enviar="agendar" novalidate>' +
      '<div class="erro-form" role="alert" hidden></div>' +
      campo('Especialidade', '<select name="especialidade" required><option value="">Escolha…</option>' +
        especialidades.map((e) => '<option value="' + esc(e.id) + '">' + esc(e.nome) + '</option>').join('') +
        '<option value="novo">Outra especialidade…</option></select>') +
      '<div data-nova-especialidade hidden>' +
      campo('Nome da especialidade', '<input name="novaEspecialidade" placeholder="Ex.: Clínico geral">') + '</div>' +
      '<div data-bloco-medico>' + campo('Médico', '<select name="medico"></select>') + '</div>' +
      '<div data-novo-medico hidden>' +
      campo('Nome do médico', '<input name="novoMedico" placeholder="Ex.: Dra. Ana Reis">') + '</div>' +
      '<div class="dupla">' +
      campo('Data', '<input name="data" type="date" min="' + hoje + '" required>') +
      campo('Horário', '<input name="hora" type="time" required>') +
      '</div><p class="dica-solta">Não é possível marcar duas consultas no mesmo horário.</p>' +
      '<div class="campo"><span>CEP da clínica</span><div class="com-botao">' +
      '<input name="cep" inputmode="numeric" autocomplete="postal-code" placeholder="00000-000" aria-label="CEP da clínica">' +
      '<button type="button" class="botao contorno" data-acao="buscar-cep">Buscar</button></div></div>' +
      campo('Endereço', '<input name="endereco" autocomplete="street-address" placeholder="Rua, número · bairro">') +
      campo('Cidade · UF', '<input name="cidade" placeholder="Ex.: Contagem · MG">',
        'O endereço vem do ViaCEP. Se a busca falhar, digite à mão.') +
      '<div class="acoes-rodape"><button type="submit" class="botao primario bloco">Agendar consulta</button></div>' +
      '</form>';

    return {
      html,
      montar: (raiz) => {
        const form = raiz.querySelector('form');
        const blocoNovaEsp = raiz.querySelector('[data-nova-especialidade]');
        const blocoMedico = raiz.querySelector('[data-bloco-medico]');
        const blocoNovoMedico = raiz.querySelector('[data-novo-medico]');
        const trocarEspecialidade = () => {
          const valor = form.especialidade.value;
          const nova = valor === 'novo';
          blocoNovaEsp.hidden = !nova;
          blocoMedico.hidden = nova || !valor;
          const escolhida = especialidades.find((e) => String(e.id) === valor);
          const medicos = escolhida ? escolhida.medicos : [];
          form.medico.innerHTML = (valor && !nova ? '<option value="">Escolha…</option>' : '') +
            medicos.map((m) => '<option value="' + esc(m.id) + '">' + esc(m.nome) + '</option>').join('') +
            '<option value="novo"' + (nova ? ' selected' : '') + '>Outro médico…</option>';
          if (escolhida && medicos.length === 1) form.medico.value = String(medicos[0].id);
          if (escolhida && !medicos.length) form.medico.value = 'novo';
          trocarMedico();
          if (nova) form.novaEspecialidade.focus();
        };
        const trocarMedico = () => {
          blocoNovoMedico.hidden = !(form.especialidade.value && form.medico.value === 'novo');
        };
        form.especialidade.addEventListener('change', trocarEspecialidade);
        form.medico.addEventListener('change', () => {
          trocarMedico();
          if (form.medico.value === 'novo') form.novoMedico.focus();
        });
        form.cep.addEventListener('keydown', (e) => {
          if (e.key === 'Enter') { e.preventDefault(); raiz.querySelector('[data-acao="buscar-cep"]').click(); }
        });
        trocarEspecialidade();
      },
      acoes: {
        'buscar-cep': async () => {
          const form = app.querySelector('form');
          try {
            const e = await get('/api/cep?cep=' + encodeURIComponent(form.cep.value));
            form.endereco.value = [e.logradouro, e.bairro].filter(Boolean).join(' · ');
            form.cidade.value = [e.cidade, e.uf].filter(Boolean).join(' · ');
            mostrarAviso('Endereço encontrado. Complete com o número.');
            form.endereco.focus();
            const posicao = (e.logradouro || '').length;
            form.endereco.setSelectionRange(posicao, posicao);
          } catch (erro) {
            if (erro instanceof ErroSilencioso) return;
            mostrarAviso('Não foi possível usar o CEP (' + erro.message + '). Digite o endereço.', 'erro');
            form.endereco.focus();
          }
        },
        agendar: async (dados) => {
          const local = [dados.endereco, dados.cidade].map((t) => (t || '').trim()).filter(Boolean).join(', ');
          await post('/api/paciente/consultas', {
            especialidade: dados.especialidade || '',
            novaEspecialidade: dados.novaEspecialidade || '',
            medico: dados.medico || '',
            novoMedico: dados.novoMedico || '',
            data: dados.data || '',
            hora: dados.hora || '',
            local
          });
          mostrarAviso('Consulta agendada.');
          ir('paciente/consultas');
        }
      }
    };
  }

  // ---------------------------------------------------------------- Paciente: Responsáveis

  async function pacienteResponsaveis() {
    const responsaveis = await get('/api/paciente/responsaveis');
    const cartoes = responsaveis.map((r) =>
      '<article class="cartao"><div class="pessoa"><div class="avatar" aria-hidden="true">' + inicial(r.nome) + '</div>' +
      '<div class="dados"><div class="nome">' + esc(r.nome) + '</div><div class="email">' + esc(r.email) + '</div></div>' +
      '<button type="button" class="link-perigo" data-acao="abrir-confirmacao">Desvincular</button></div>' +
      botaoConfirmacao('Desvincular ' + r.nome + '?', 'desvincular', 'Desvincular', r.email) + '</article>');
    const html = cabecalho('Meus responsáveis', { mais: 'paciente/responsaveis/novo', rotuloMais: 'Adicionar responsável' }) +
      '<p class="descricao">Estas pessoas recebem um alerta se você não confirmar um lembrete dentro do prazo.</p>' +
      lista(cartoes, 'Você ainda não tem responsáveis. Toque em + para adicionar alguém.');
    return {
      html,
      acoes: {
        ...acoesComuns,
        desvincular: async (botao) => {
          await post('/api/paciente/responsaveis/remover', { email: botao.dataset.id });
          mostrarAviso('Responsável desvinculado.');
          await redesenhar();
        }
      }
    };
  }

  async function pacienteNovoResponsavel() {
    const html = voltar('Meus responsáveis', 'paciente/responsaveis') + '<h1>Adicionar responsável</h1>' +
      '<p class="descricao">Informe o e-mail de quem vai acompanhar seus lembretes.</p>' +
      '<form class="formulario" data-enviar="vincular" novalidate><div class="erro-form" role="alert" hidden></div>' +
      campo('E-mail do responsável', '<input name="email" type="email" inputmode="email" autocomplete="off" ' +
        'placeholder="joao@demo.com" required>') +
      caixaInfo(ICONE.info, 'A pessoa precisa ter uma conta com o perfil Responsável. Ela passa a receber um aviso, ' +
        'uma única vez, quando você deixar de confirmar um lembrete no prazo.') +
      '<div class="acoes-rodape"><button type="submit" class="botao primario bloco">Vincular responsável</button>' +
      '<button type="button" class="botao texto bloco" data-ir="paciente/responsaveis">Cancelar</button></div></form>';
    return {
      html,
      montar: (raiz) => raiz.querySelector('input[name="email"]').focus(),
      acoes: {
        vincular: async (dados) => {
          const r = await post('/api/paciente/responsaveis', dados);
          mostrarAviso(r.nome + ' agora acompanha seus lembretes.');
          ir('paciente/responsaveis');
        }
      }
    };
  }

  // ---------------------------------------------------------------- Responsável

  async function responsavelPacientes() {
    const d = await get('/api/responsavel/pacientes');
    let alerta = '';
    if (d.alerta) {
      const a = d.alerta;
      const oque = a.tipo === 'CONSULTA' ? 'o lembrete da consulta de ' + a.titulo : a.titulo;
      alerta = '<div class="cartao alerta aviso-alerta espaco-baixo">' + ICONE.sino + '<div>' +
        '<span class="etiqueta">Alerta · enviado uma vez</span><p>' + esc(a.primeiroNome + ' não confirmou ' + oque +
          ' das ' + a.previsto + ' dentro do prazo de ' + plural(d.tempoLimite, 'minuto', 'minutos') + '.') +
          '</p></div></div>';
    }
    const cartoes = d.pacientes.map((p) =>
      '<article class="cartao"><div class="pessoa"><div class="avatar" aria-hidden="true">' + inicial(p.nome) + '</div>' +
      '<div class="dados"><div class="nome">' + esc(p.nome) + '</div><div class="email">' + esc(p.email) + '</div></div>' +
      (p.perdidosHoje > 0 ? '<span class="chip vermelho">' + plural(p.perdidosHoje, 'perdido', 'perdidos') + '</span>'
        : '<span class="chip verde">Em dia</span>') + '</div>' +
      (p.itens.length ? '<ul class="linhas-status">' + p.itens.map((i) =>
        '<li><span>' + esc(i.hora + ' ' + i.nome) + '</span>' + textoStatus(i.status) + '</li>').join('') + '</ul>'
        : '<p class="linha-suave">Nada na agenda de hoje.</p>') +
      '</article>');
    const html = cabecalho('Meus pacientes', { sobretitulo: 'Perfil responsável', sair: true }) + alerta +
      lista(cartoes, 'Você ainda não acompanha nenhum paciente.') +
      '<p class="nota">' + esc('Para acompanhar um novo paciente, peça que ele adicione o seu e-mail (' +
        usuario.email + ') em "Meus responsáveis".') + '</p>';
    return { html, acoes: acoesComuns };
  }

  async function responsavelLembretes() {
    const d = await get('/api/responsavel/lembretes');
    const cartoes = d.itens.map((l) => {
      const etiqueta = l.paciente + ' · ' + (l.tipo === 'CONSULTA' ? 'Consulta' : 'Medicamento');
      const linhas = [];
      if (l.status === 'PERDIDO') {
        linhas.push('<p class="linha-suave">' + esc('Previsto ' + l.previsto + ' · prazo encerrado às ' + l.prazo) + '</p>');
        linhas.push('<p class="linha-alerta">' + esc(l.alertaAs ? 'Alerta enviado a você às ' + l.alertaAs
          : 'Alerta sendo enviado') + '</p>');
      } else if (l.status === 'PENDENTE') {
        const base = l.tipo === 'CONSULTA'
          ? (l.subtitulo ? l.subtitulo + ' · ' : '') + 'consulta às ' + (l.consultaAs || l.previsto)
          : 'Previsto ' + l.previsto;
        linhas.push('<p class="linha-suave">' + esc(base + ' · prazo ' + l.prazo) + '</p>');
        linhas.push('<p class="linha-suave menor">' + esc('Aguardando a confirmação de ' + l.primeiroNome) + '</p>');
      } else {
        linhas.push('<p class="linha-suave">' + esc('Previsto ' + l.previsto) + '</p>');
      }
      return '<article class="cartao' + (l.status === 'PERDIDO' ? ' alerta' : '') + '">' +
        '<div class="cartao-topo"><span class="etiqueta">' + esc(etiqueta) + '</span>' + chip(l.status) + '</div>' +
        '<div class="titulo-grande">' + esc(l.titulo) + '</div>' + linhas.join('') + '</article>';
    });
    const html = cabecalho('Lembretes', { sair: true }) +
      segmentos([['Em aberto', 'responsavel/lembretes'], ['Histórico', 'responsavel/lembretes/historico']], 0) +
      lista(cartoes, 'Nada em aberto agora. Tudo em dia!') +
      '<p class="nota">Quem confirma é o paciente. Você só acompanha e recebe o alerta quando o prazo passa.</p>';
    return { html, acoes: acoesComuns };
  }

  async function responsavelHistorico() {
    const d = await get('/api/responsavel/historico');
    const html = cabecalho('Lembretes', { sair: true }) +
      segmentos([['Em aberto', 'responsavel/lembretes'], ['Histórico', 'responsavel/lembretes/historico']], 1) +
      historicoAgrupado(d.itens, d.hoje, (l) => l.primeiroNome + ' · ' + tituloLembrete(l), 'você');
    return { html, acoes: acoesComuns };
  }

  async function responsavelAgendaDia(params) {
    const dia = params.get('dia');
    const d = await get('/api/responsavel/agenda' + (dia ? '?dia=' + encodeURIComponent(dia) : ''));
    const secoes = d.pacientes.map((p) =>
      '<h2 class="rotulo-secao forte">' + esc(p.nome) + '</h2>' +
      lista(p.itens.map((i) => itemAgenda({
        hora: i.hora, status: i.status,
        titulo: i.tipo === 'CONSULTA' ? 'Consulta · ' + i.titulo : i.titulo
      }, true)), 'Nada na agenda deste dia.'));
    const html = cabecalho('Agenda', { sobretitulo: porExtenso(d.dia), sair: true }) +
      segmentos([['Dia', 'responsavel/agenda'], ['Semana', 'responsavel/agenda/semana']], 0) +
      (d.dia !== d.hoje ? '<p class="nota"><button type="button" class="sair" data-ir="responsavel/agenda">' +
        '← Voltar para hoje</button></p>' : '') +
      (secoes.length ? secoes.join('') : '<p class="vazio-lista">Você ainda não acompanha nenhum paciente.</p>');
    return { html, acoes: acoesComuns };
  }

  async function responsavelAgendaSemana() {
    const d = await get('/api/responsavel/semana');
    const dias = d.dias.map((dia) => cartaoDiaSemana(dia.data, d.hoje, dia.linhas.map((l) => {
      const partes = [];
      if (l.doses) partes.push(plural(l.doses, 'remédio', 'remédios'));
      if (l.consultas) partes.push(plural(l.consultas, 'consulta', 'consultas'));
      return l.paciente + ': ' + (partes.length ? partes.join(' · ') : 'nada agendado');
    }), 'responsavel/agenda'));
    const html = cabecalho('Agenda', { sobretitulo: intervaloSemana(d.inicio), sair: true }) +
      segmentos([['Dia', 'responsavel/agenda'], ['Semana', 'responsavel/agenda/semana']], 1) +
      '<div class="lista">' + dias.join('') + '</div>';
    return { html, acoes: acoesComuns };
  }

  // ---------------------------------------------------------------- Tabela de telas

  // perfil: quem pode abrir (null = só sem login) · abas: mostra a barra · atualizar: recarrega sozinha
  const TELAS = {
    'login': { perfil: null, desenhar: telaLogin },
    'cadastro': { perfil: null, desenhar: telaCadastro },
    'paciente/agenda': { perfil: 'PACIENTE', abas: true, atualizar: true, desenhar: pacienteAgendaDia },
    'paciente/agenda/semana': { perfil: 'PACIENTE', abas: true, atualizar: true, desenhar: pacienteAgendaSemana },
    'paciente/lembretes': { perfil: 'PACIENTE', abas: true, atualizar: true, desenhar: pacienteLembretes },
    'paciente/lembretes/historico': { perfil: 'PACIENTE', abas: true, atualizar: true, desenhar: pacienteHistorico },
    'paciente/remedios': { perfil: 'PACIENTE', abas: true, desenhar: pacienteRemedios },
    'paciente/remedios/novo': { perfil: 'PACIENTE', desenhar: pacienteNovoRemedio },
    'paciente/consultas': { perfil: 'PACIENTE', abas: true, desenhar: pacienteConsultas },
    'paciente/consultas/nova': { perfil: 'PACIENTE', desenhar: pacienteNovaConsulta },
    'paciente/responsaveis': { perfil: 'PACIENTE', abas: true, desenhar: pacienteResponsaveis },
    'paciente/responsaveis/novo': { perfil: 'PACIENTE', desenhar: pacienteNovoResponsavel },
    'responsavel/pacientes': { perfil: 'RESPONSAVEL', abas: true, atualizar: true, desenhar: responsavelPacientes },
    'responsavel/lembretes': { perfil: 'RESPONSAVEL', abas: true, atualizar: true, desenhar: responsavelLembretes },
    'responsavel/lembretes/historico': {
      perfil: 'RESPONSAVEL', abas: true, atualizar: true, desenhar: responsavelHistorico
    },
    'responsavel/agenda': { perfil: 'RESPONSAVEL', abas: true, atualizar: true, desenhar: responsavelAgendaDia },
    'responsavel/agenda/semana': {
      perfil: 'RESPONSAVEL', abas: true, atualizar: true, desenhar: responsavelAgendaSemana
    }
  };

  // Telas de acompanhamento se atualizam sozinhas (o agendador muda os status no servidor)
  function atualizarSePreciso() {
    if (document.hidden || pausado || !atual || !atual.tela.atualizar || !usuario) return;
    redesenhar();
  }
  setInterval(atualizarSePreciso, ATUALIZAR_A_CADA_MS);
  document.addEventListener('visibilitychange', atualizarSePreciso);

  window.addEventListener('hashchange', navegar);
  navegar();
})();
