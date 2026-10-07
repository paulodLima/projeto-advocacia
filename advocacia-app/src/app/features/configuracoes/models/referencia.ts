// Campos e textos extraídos de docs/index.html.
export const EMPRESA_SECOES: { titulo: string; dica?: string; campos: { k: string; r: string; larga?: boolean; obrigatorio?: boolean; tipo?: string }[] }[] = [
  {
    "titulo": "A sociedade",
    "campos": [
      {
        "k": "razao_social",
        "r": "Razão social",
        "larga": true,
        "obrigatorio": true
      },
      {
        "k": "nome_fantasia",
        "r": "Nome fantasia"
      },
      {
        "k": "cnpj",
        "r": "CNPJ",
        "obrigatorio": true
      },
      {
        "k": "oab_sociedade",
        "r": "Registro da sociedade na OAB"
      },
      {
        "k": "inscricao_municipal",
        "r": "Inscrição municipal"
      },
      {
        "k": "cnae",
        "r": "CNAE (ex.: 6911-7/01)"
      },
      {
        "k": "constituida_em",
        "r": "Constituída em",
        "tipo": "date"
      }
    ]
  },
  {
    "titulo": "Advogada titular",
    "dica": "Sociedade individual de advocacia: é o nome que assina os documentos.",
    "campos": [
      {
        "k": "titular_nome",
        "r": "Nome completo",
        "larga": true
      },
      {
        "k": "titular_cpf",
        "r": "CPF"
      },
      {
        "k": "oab",
        "r": "OAB (ex.: OAB/DF 55.941)"
      }
    ]
  },
  {
    "titulo": "Endereço",
    "campos": [
      {
        "k": "endereco",
        "r": "Logradouro, número e complemento",
        "larga": true,
        "obrigatorio": true
      },
      {
        "k": "bairro",
        "r": "Bairro"
      },
      {
        "k": "cidade",
        "r": "Cidade",
        "obrigatorio": true
      },
      {
        "k": "uf",
        "r": "UF"
      },
      {
        "k": "cep",
        "r": "CEP"
      }
    ]
  },
  {
    "titulo": "Contato",
    "campos": [
      {
        "k": "telefone",
        "r": "Telefone"
      },
      {
        "k": "whatsapp",
        "r": "WhatsApp"
      },
      {
        "k": "email",
        "r": "E-mail"
      },
      {
        "k": "site",
        "r": "Site"
      }
    ]
  },
  {
    "titulo": "Tributário",
    "dica": "Usado no rodapé dos documentos e, mais à frente, na emissão da nota fiscal.",
    "campos": [
      {
        "k": "regime",
        "r": "Regime (Simples Nacional, Lucro Presumido…)"
      },
      {
        "k": "codigo_servico",
        "r": "Código do serviço (ISS)"
      },
      {
        "k": "iss_aliquota",
        "r": "Alíquota de ISS (%)"
      },
      {
        "k": "observacao_fiscal",
        "r": "Observação fiscal que sai nos documentos",
        "larga": true
      }
    ]
  },
  {
    "titulo": "Recebimento",
    "dica": "Aparece no rodapé da fatura e do demonstrativo.",
    "campos": [
      {
        "k": "banco_dados",
        "r": "Banco, agência, conta e chave PIX",
        "larga": true
      }
    ]
  }
];
export const PERSONALIDADES = [
  {
    "id": "executiva",
    "nome": "Executiva",
    "resumo": "Direta, um passo à frente, sem rodeio.",
    "exemplo": "São 33 processos ativos. Dois com prazo vencendo esta semana — quer a lista?"
  },
  {
    "id": "didatica",
    "nome": "Didática",
    "resumo": "Explica o caminho junto com a resposta. Boa para quem está aprendendo o procedimento.",
    "exemplo": "São 33 ativos. Contei os que estão com status \"ativo\" na Gestão Processual — os arquivados ficam de fora dessa conta."
  },
  {
    "id": "formal",
    "nome": "Formal",
    "resumo": "Linguagem cuidadosa, para quando a resposta pode virar texto de e-mail ou de peça.",
    "exemplo": "Informo que constam 33 processos ativos, dos quais 2 com prazo a vencer na presente semana."
  },
  {
    "id": "cordial",
    "nome": "Cordial",
    "resumo": "Mesmo conteúdo, tom mais caloroso. Boa para equipe nova.",
    "exemplo": "São 33 ativos! Fique de olho em dois deles, que têm prazo esta semana — quer que eu liste?"
  }
];
export const VARIAVEIS_DOCUMENTO = [
  {
    "v": "cliente",
    "r": "Nome do cliente",
    "ex": "Flávio Gonçalves Borges Junior"
  },
  {
    "v": "qualificacao",
    "r": "CPF/CNPJ do cliente (frase inteira)",
    "ex": ", inscrito(a) no CPF/CNPJ sob o nº 049.288.211-81"
  },
  {
    "v": "cpf_cnpj",
    "r": "CPF/CNPJ (só o número)",
    "ex": "049.288.211-81"
  },
  {
    "v": "valor",
    "r": "Valor",
    "ex": "R$ 2.142,90"
  },
  {
    "v": "valor_extenso",
    "r": "Valor por extenso",
    "ex": "dois mil, cento e quarenta e dois reais e noventa centavos"
  },
  {
    "v": "descricao",
    "r": "Do que se trata",
    "ex": "Honorários contratuais"
  },
  {
    "v": "processo",
    "r": "Processo (frase inteira)",
    "ex": ", no processo nº 0713359-93.2026.8.07.0001"
  },
  {
    "v": "numero_processo",
    "r": "Processo (só o número)",
    "ex": "0713359-93.2026.8.07.0001"
  },
  {
    "v": "contrato",
    "r": "Contrato (frase inteira)",
    "ex": ", na forma do contrato nº 014/2026, firmado em 18/02/2026"
  },
  {
    "v": "contrato_numero",
    "r": "Contrato (só o número)",
    "ex": "014/2026"
  },
  {
    "v": "forma_pagamento",
    "r": "Forma de pagamento",
    "ex": "Boleto"
  },
  {
    "v": "data_pagamento",
    "r": "Data do pagamento",
    "ex": "09/09/2026"
  },
  {
    "v": "cidade",
    "r": "Cidade do escritório",
    "ex": "Brasília/DF"
  },
  {
    "v": "data_extenso",
    "r": "Data de hoje por extenso",
    "ex": "12 de agosto de 2026"
  },
  {
    "v": "escritorio",
    "r": "Nome do escritório",
    "ex": "Andressa Borges Sociedade Individual de Advocacia"
  }
];
export const MODELOS_DOCUMENTO = [
  {
    "chave": "recibo",
    "titulo": "Recibo de honorários",
    "trechos": [
      {
        "k": "corpo",
        "r": "Corpo do recibo",
        "dica": "Sai justificado, logo abaixo do valor.",
        "padrao": "<p>Recebi de <b>{{cliente}}</b>{{qualificacao}}, a importância acima descrita, referente a <b>{{descricao}}</b>{{processo}}{{contrato}}.</p><p>Para clareza, firmo o presente recibo, dando plena e geral quitação do valor recebido.</p>"
      },
      {
        "k": "rodape",
        "r": "Observação do rodapé",
        "dica": "Linha miúda antes da assinatura. Deixe em branco para não sair nada.",
        "padrao": ""
      }
    ]
  },
  {
    "chave": "fatura",
    "titulo": "Fatura de honorários",
    "trechos": [
      {
        "k": "abertura",
        "r": "Texto de abertura",
        "dica": "Vem antes da tabela de parcelas. Em branco, a tabela começa direto.",
        "padrao": ""
      },
      {
        "k": "rodape",
        "r": "Observação do rodapé",
        "padrao": "Este documento é um demonstrativo de honorários e não substitui a nota fiscal de serviço."
      }
    ]
  },
  {
    "chave": "demonstrativo",
    "titulo": "Demonstrativo de despesas",
    "trechos": [
      {
        "k": "abertura_reembolsar",
        "r": "Abertura — só o que falta reembolsar",
        "padrao": "<p>Segue o demonstrativo das despesas processuais suportadas por este escritório e ainda pendentes de reembolso, com a data do efetivo desembolso de cada uma.</p>"
      },
      {
        "k": "abertura_completo",
        "r": "Abertura — demonstrativo completo",
        "padrao": "<p>Segue o demonstrativo das despesas processuais registradas neste processo, com a data do efetivo desembolso e a indicação de quem as suportou.</p>"
      },
      {
        "k": "rodape",
        "r": "Observação do rodapé",
        "padrao": "Este documento é um demonstrativo de despesas e não substitui a nota fiscal de serviço."
      }
    ]
  },
  {
    "chave": "prestacao",
    "titulo": "Prestação de contas",
    "trechos": [
      {
        "k": "abertura",
        "r": "Texto de abertura",
        "dica": "Vem depois da identificação, antes da primeira tabela.",
        "padrao": "<p>Em cumprimento ao dever de prestar contas (art. 668 do Código Civil e art. 34, XXI, da Lei nº 8.906/94), apresento abaixo a movimentação dos valores recebidos em nome de <b>{{cliente}}</b> neste processo, com as deduções contratadas e o valor líquido apurado.</p>"
      },
      {
        "k": "rodape",
        "r": "Observação do rodapé",
        "dica": "Linha miúda antes da assinatura. Deixe em branco para não sair nada.",
        "padrao": ""
      }
    ]
  }
];
export const TAMANHOS_RESPOSTA = [
  {
    "id": "curta",
    "nome": "Curta",
    "resumo": "Duas ou três frases. Quem quiser mais, pergunta de novo."
  },
  {
    "id": "media",
    "nome": "Média",
    "resumo": "Um parágrafo, com o porquê junto."
  }
];
