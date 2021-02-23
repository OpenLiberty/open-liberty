/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
      ACCOUNTING_STRING : "Sequência de Caracteres para Contabilização",
      SEARCH_RESOURCE_TYPE_ALL : "Todos",
      SEARCH : "Procurar",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Insira os critérios de procura selecionando o botão Incluir critério de procura e, em seguida, especificando um valor",
      SUBMITTED : "Enviado",
      JMS_QUEUED : "Enfileirado pelo JMS",
      JMS_CONSUMED : "Consumido pelo JMS",
      JOB_PARAMETER : "Parâmetro da Tarefa",
      DISPATCHED : "Despachado",
      FAILED : "Com falha",
      STOPPED : "Interrompido",
      COMPLETED : "Concluído",
      ABANDONED : "Abandonado",
      STARTED : "Iniciado",
      STARTING : "Iniciando",
      STOPPING : "Interrompendo",
      REFRESH : "Atualizar",
      INSTANCE_STATE : "Estado da Instância",
      APPLICATION_NAME : "Nome do Aplicativo",
      APPLICATION: "Aplicativo",
      INSTANCE_ID : "ID da Instância",
      LAST_UPDATE : "Última Atualização",
      LAST_UPDATE_RANGE : "Intervalo da última atualização",
      LAST_UPDATED_TIME : "Horário da Última Atualização",
      DASHBOARD_VIEW : "Visualização de painel",
      HOMEPAGE : "Página Inicial",
      JOBLOGS : "Logs de Tarefa",
      QUEUED : "Enfileirado",
      ENDED : "Terminado",
      ERROR : "Erro",
      CLOSE : "Fechar",
      WARNING : "Aviso",
      GO_TO_DASHBOARD: "Acessar o Painel",
      DASHBOARD : "Painel",
      BATCH_JOB_NAME: "Nome da Tarefa em Lote",
      SUBMITTER: "Requisitante",
      BATCH_STATUS: "Status do Lote",
      EXECUTION_ID: "ID de Execução da Tarefa",
      EXIT_STATUS: "Status de Saída",
      CREATE_TIME: "Horário de Criação",
      START_TIME: "Horário de Início",
      END_TIME: "Horário de Encerramento",
      SERVER: "Servidor",
      SERVER_NAME: "Nome do Servidor",
      SERVER_USER_DIRECTORY: "Diretório do usuário",
      SERVERS_USER_DIRECTORY: "Diretório do usuário do servidor",
      HOST: "Host",
      NAME: "Nome",
      JOB_PARAMETERS: "Parâmetros da tarefa",
      JES_JOB_NAME: "Nome da Tarefa JES",
      JES_JOB_ID: "ID da Tarefa JES",
      ACTIONS: "Ações",
      VIEW_LOG_FILE: "Visualizar arquivo de log",
      STEP_NAME: "Nome da etapa",
      ID: "ID",
      PARTITION_ID: "Partição {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Visualizar detalhes de execução da tarefa {0}",    // Job Execution ID number
      PARENT_DETAILS: "Detalhes das Informações Pai",
      TIMES: "Horários",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Status",
      SEARCH_ON: "Selecione para filtrar em {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Inserir critérios de procura.",
      BREADCRUMB_JOB_INSTANCE : "Instância da tarefa {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Execução da tarefa {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Log da tarefa {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "Os critérios de procura não são válidos.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "O critério de procura não pode ter múltiplos filtros pelos parâmetros {0}.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Tabela de Instâncias da Tarefa",
      EXECUTIONS_TABLE_IDENTIFIER: "Tabela de Execuções da Tarefa",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Tabela de Detalhe das Etapas",
      LOADING_VIEW : "A página está atualmente carregando informações",
      LOADING_VIEW_TITLE : "Visualização de Carregamento",
      LOADING_GRID : "Aguardando os resultados da procura retornarem do servidor",
      PAGENUMBER : "Número de Página",
      SELECT_QUERY_SIZE: "Selecionar tamanho de consulta",
      LINK_EXPLORE_HOST: "Selecionar para visualizar os detalhes sobre o Host {0} na ferramenta Explore.",      // Host name
      LINK_EXPLORE_SERVER: "Selecionar para visualizar os detalhes sobre o Servidor {0} na ferramenta Explore.",  // Server name

      //ACTIONS
      RESTART: "Reiniciar",
      STOP: "Parar",
      PURGE: "Limpar",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Ações para instância da tarefa {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Menu de ações de instância da tarefa",

      RESTART_INSTANCE_MESSAGE: "Deseja reiniciar a execução da tarefa mais recente associada à instância da tarefa {0}?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Deseja parar a execução da tarefa mais recente associada à instância da tarefa {0}?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Deseja limpar todas as entradas do banco de dados e os logs da tarefa associados à instância da tarefa {0}?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Limpar apenas o armazenamento de tarefa",

      RESTART_INST_ERROR_MESSAGE: "Solicitação de reinicialização com falha.",
      STOP_INST_ERROR_MESSAGE: "Solicitação de parada com falha.",
      PURGE_INST_ERROR_MESSAGE: "Solicitação de limpeza com falha.",
      ACTION_REQUEST_ERROR_MESSAGE: "A solicitação de ação falhou com o código de status: {0}.  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Reutilizar os parâmetros da execução anterior",
      JOB_PARAMETERS_EMPTY: "Caso '{0}' não esteja selecionado, use essa área para inserir os parâmetros da tarefa.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Nome do parâmetro",
      JOB_PARAMETER_VALUE: "Valor do parâmetro",
      PARM_NAME_COLUMN_HEADER: "Parâmetro",
      PARM_VALUE_COLUMN_HEADER: "Valor",
      PARM_ADD_ICON_TITLE: "Incluir parâmetro",
      PARM_REMOVE_ICON_TITLE: "Remover parâmetro",
      PARMS_ENTRY_ERROR: "O nome do parâmetro é necessário.",
      JOB_PARAMETER_CREATE: "Selecione {0} para incluir os parâmetros à próxima execução desta instância da tarefa.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Incluir botão do parâmetro no cabeçalho da tabela.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Conteúdo do log da tarefa",
      FILE_DOWNLOAD : "Download de Arquivo",
      DOWNLOAD_DIALOG_DESCRIPTION : "Deseja fazer download do arquivo de log?",
      INCLUDE_ALL_LOGS : "Incluir todos os arquivos de log para a execução da tarefa",
      LOGS_NAVIGATION_BAR : "Barra de navegação de logs da tarefa",
      DOWNLOAD : "Fazer download",
      LOG_TOP : "Parte Superior dos Sogs",
      LOG_END : "Término dos Logs",
      PREVIOUS_PAGE : "Página Anterior",
      NEXT_PAGE : "Próxima Página",
      DOWNLOAD_ARIA : "Fazer o download do arquivo",

      //Error messages for popups
      REST_CALL_FAILED : "A chamada para buscar dados falhou.",
      NO_JOB_EXECUTION_URL : "Nenhum número de execução da tarefa foi fornecido na URL ou a instância não possui logs de execução da tarefa para exibição.",
      NO_VIEW : "Erro de URL: não existe tal visualização.",
      WRONG_TOOL_ID : "A sequência de consultas da URL não iniciou o ID da ferramenta {0}, mas iniciou {1} em vez disso.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "Erro de URL: não existem logs.",
      NOT_A_NUMBER : "Erro de URL: {0} deve ser um número.",                                                // {0} is a field name
      PARAMETER_REPETITION : "Erro de URL: {0} pode existir apenas uma vez nos parâmetros.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "Erro de URL: o parâmetro da página está fora do intervalo.",
      INVALID_PARAMETER : "Erro de URL: {0} não é um parâmetro válido.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "Erro de URL: a URL pode especificar a execução da tarefa ou a instância da tarefa, mas não as duas.",
      MISSING_EXECUTION_ID_PARAM : "O parâmetro do ID de execução necessário está ausente.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Uma configuração do banco de dados persistente do Java Batch é obrigatória para usar a ferramenta Java Batch.",
      IGNORED_SEARCH_CRITERIA : "Os critérios de filtro a seguir foram ignorados nos resultados: {0}",

      GRIDX_SUMMARY_TEXT : "Mostrando as ${0} instâncias de tarefa mais recentes"

});

