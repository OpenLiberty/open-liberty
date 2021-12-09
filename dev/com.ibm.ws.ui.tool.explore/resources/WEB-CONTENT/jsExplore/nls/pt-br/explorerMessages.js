/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    EXPLORER : "Explorer",
    EXPLORE : "Explorar",
    DASHBOARD : "Painel",
    DASHBOARD_VIEW_ALL_APPS : "Visualizar todos os aplicativos",
    DASHBOARD_VIEW_ALL_SERVERS : "Visualizar todos os servidores",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Visualizar todos os clusters",
    DASHBOARD_VIEW_ALL_HOSTS : "Visualizar todos os hosts",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Visualizar todos os tempos de execução",
    SEARCH : "Procurar",
    SEARCH_RECENT : "Procuras Recentes",
    SEARCH_RESOURCES : "Recursos de procura",
    SEARCH_RESULTS : "Resultados da Procura",
    SEARCH_NO_RESULTS : "Nenhum resultado",
    SEARCH_NO_MATCHES : "Nenhuma correspondência",
    SEARCH_TEXT_INVALID : "O texto da procura inclui caracteres inválidos",
    SEARCH_CRITERIA_INVALID : "Os critérios de procura não são válidos.",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} não é válido quando for especificado com {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "Especifique {0} somente uma vez.",
    SEARCH_TEXT_MISSING : "O texto da procura é necessário",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "A procura por tags de aplicativos em um servidor não é suportada.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "A procura por tags de aplicativo em um cluster não é suportada.",
    SEARCH_UNSUPPORT : "Os critérios de procura não são suportados.",
    SEARCH_SWITCH_VIEW : "Alternar visualização",
    FILTERS : "Filtros",
    DEPLOY_SERVER_PACKAGE : "Implementar pacote do servidor",
    MEMBER_OF : "Membro de",
    N_CLUSTERS: "{0} Clusters ...",

    INSTANCE : "Instância",
    INSTANCES : "Instâncias",
    APPLICATION : "Aplicativo",
    APPLICATIONS : "Aplicativos",
    SERVER : "Servidor",
    SERVERS : "Servidores",
    CLUSTER : "Cluster",
    CLUSTERS : "Clusters",
    CLUSTER_NAME : "Nome do cluster: ",
    CLUSTER_STATUS : "Status do cluster: ",
    APPLICATION_NAME : "Nome do aplicativo: ",
    APPLICATION_STATE : "Estado do aplicativo: ",
    HOST : "Host",
    HOSTS : "Hosts",
    RUNTIME : "Tempo de execução",
    RUNTIMES : "Tempos de execução",
    PATH : "Caminho",
    CONTROLLER : "Controlador",
    CONTROLLERS : "Controladores",
    OVERVIEW : "Visão Geral",
    CONFIGURE : "Configurar",

    SEARCH_RESOURCE_TYPE: "Tipo", // Search by resource types
    SEARCH_RESOURCE_STATE: "Estado", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Tudo", // Search all resource types
    SEARCH_RESOURCE_NAME: "Nome", // Search by resource name
    SEARCH_RESOURCE_TAG: "Tag", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Contêiner", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Nenhum(a)", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Tipo de Tempo de Execução", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Proprietário", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Contact", // Search by contact
    SEARCH_RESOURCE_NOTE: "Observação", // Search by note

    GRID_HEADER_USERDIR : "Diretório do usuário",
    GRID_HEADER_NAME : "Nome",
    GRID_LOCATION_NAME : "Local",
    GRID_ACTIONS : "Ações da Grade",
    GRID_ACTIONS_LABEL : "Ações da grade {0}",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} em {1} ({2})", // server on host (/path)

    STATS : "Monitor",
    STATS_ALL : "Tudo",
    STATS_VALUE : "Valor: {0}",
    CONNECTION_IN_USE_STATS : "{0} Em uso = {1} Gerenciado(s) - {2} Livre(s)",
    CONNECTION_IN_USE_STATS_VALUE : "Valor: {0} Em uso = {1} Gerenciado - {2} Livre",
    DATA_SOURCE : "Origem de Dados: {0}",
    STATS_DISPLAY_LEGEND : "Mostrar legenda",
    STATS_HIDE_LEGEND : "Ocultar legenda",
    STATS_VIEW_DATA : "Visualizar dados de diagrama",
    STATS_VIEW_DATA_TIMESTAMP : "Registro de data e hora",
    STATS_ACTION_MENU : "{0} menu de ação",
    STATS_SHOW_HIDE : "Incluir métrica de recurso",
    STATS_SHOW_HIDE_SUMMARY : "Incluir Métricar para Sumarização",
    STATS_SHOW_HIDE_TRAFFIC : "Incluir Métricas para Tráfego",
    STATS_SHOW_HIDE_PERFORMANCE : "Incluir Métricas para Desempenho",
    STATS_SHOW_HIDE_AVAILABILITY : "Incluir Métricas para Disponibilidade",
    STATS_SHOW_HIDE_ALERT : "Incluir Métricas para Alerta",
    STATS_SHOW_HIDE_LIST_BUTTON : "Mostrar ou ocultar a lista de métricas de recurso",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Editar diagramas",
    STATS_SHOW_HIDE_CONFIRM : "Salvar",
    STATS_SHOW_HIDE_CANCEL : "Cancelar",
    STATS_SHOW_HIDE_DONE : "Pronto",
    STATS_DELETE_GRAPH : "Excluir diagrama",
    STATS_ADD_CHART_LABEL : "Incluir diagrama na visualização",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Incluir todos os diagramas JVM na visualização",
    STATS_HEAP_TITLE : "Memória heap usada",
    STATS_HEAP_USED : "Usado: {0} MB",
    STATS_HEAP_COMMITTED : "Confirmado: {0} MB",
    STATS_HEAP_MAX : "Máx: {0} MB",
    STATS_HEAP_X_TIME : "Horário",
    STATS_HEAP_Y_MB : "MB usado",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "Classes carregadas",
    STATS_CLASSES_LOADED : "Carregado: {0}",
    STATS_CLASSES_UNLOADED : "Descarregado: {0}",
    STATS_CLASSES_TOTAL : "Total: {0}",
    STATS_CLASSES_Y_TOTAL : "Classes carregadas",
    STATS_PROCESSCPU_TITLE : "Uso de CPU",
    STATS_PROCESSCPU_USAGE : "Uso de CPU: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "Porcentagem de CPU",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Encadeamentos JVM ativos",
    STATS_LIVE_MSG_INIT : "Mostrando dados ativos",
    STATS_LIVE_MSG :"Este gráfico não tem dados históricos. Continuará mostrando os 10 minutos mais recentes de dados.",
    STATS_THREADS_ACTIVE : "Em tempo real: {0}",
    STATS_THREADS_PEAK : "Pico: {0}",
    STATS_THREADS_TOTAL : "Total: {0}",
    STATS_THREADS_Y_THREADS : "Encadeamentos",
    STATS_TP_POOL_SIZE : "Tamanho do conjunto",
    STATS_JAXWS_TITLE : "Serviços da web JAX-WS",
    STATS_JAXWS_BUTTON_LABEL : "Incluir todos os diagramas de serviços da web JAX-WS para visualização",
    STATS_JW_AVG_RESP_TIME : "Tempo médio de resposta",
    STATS_JW_AVG_INVCOUNT : "Contagem média de chamada",
    STATS_JW_TOTAL_FAULTS : "Falhas de tempo de execução totais",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Selecionar recursos...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} recursos",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 recurso",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "Deve-se selecionar ao menos um recurso.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "Nenhum recurso disponível para o intervalo de tempo selecionado.",
    STATS_ACCESS_LOG_TITLE : "Log de acesso",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Incluir todos os diagramas de Log de acesso para visualização",
    STATS_ACCESS_LOG_GRAPH : "Contagem de mensagens do log de acesso",
    STATS_ACCESS_LOG_SUMMARY : "Resumo do log de acesso",
    STATS_ACCESS_LOG_TABLE : "Acessar Lista de Mensagens de Log",
    STATS_MESSAGES_TITLE : "Mensagens e rastreio",
    STATS_MESSAGES_BUTTON_LABEL : "Incluir todos os diagramas Mensagens e Rastreio para visualização",
    STATS_MESSAGES_GRAPH : "Contagem da mensagem de log",
    STATS_MESSAGES_TABLE : "Lista de mensagem de log",
    STATS_FFDC_GRAPH : "Contagem FFDC",
    STATS_FFDC_TABLE : "Lista FFDC",
    STATS_TRACE_LOG_GRAPH : "Contagem de mensagem de rastreio",
    STATS_TRACE_LOG_TABLE : "Lista de mensagem de rastreio",
    STATS_THREAD_POOL_TITLE : "Conjunto de encadeamentos",
    STATS_THREAD_POOL_BUTTON_LABEL : "Incluir todos os diagramas Conjunto de encadeamentos para visualização",
    STATS_THREADPOOL_TITLE : "Encadeamentos Liberty ativos",
    STATS_THREADPOOL_SIZE : "Tamanho do conjunto: {0}",
    STATS_THREADPOOL_ACTIVE : "Ativo: {0}",
    STATS_THREADPOOL_TOTAL : "Total: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Encadeamentos ativos",
    STATS_SESSION_MGMT_TITLE : "Sessões",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Incluir todos os diagramas Sessões para visualização",
    STATS_SESSION_CONFIG_LABEL : "Selecionar sessões...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} sessões",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 sessão",
    STATS_SESSION_CONFIG_SELECT_ONE : "Deve-se selecionar ao menos uma sessão.",
    STATS_SESSION_TITLE : "Sessões ativas",
    STATS_SESSION_Y_ACTIVE : "Sessões ativas",
    STATS_SESSION_LIVE_LABEL : "Contagem em tempo real: {0}",
    STATS_SESSION_CREATE_LABEL : "Criar contagem: {0}",
    STATS_SESSION_INV_LABEL : "Contagem invalidada: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Contagem invalidada por tempo limite: {0}",
    STATS_WEBCONTAINER_TITLE : "Aplicativos da Web",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Incluir todos os diagramas Aplicativos da web para visualização",
    STATS_SERVLET_CONFIG_LABEL : "Selecionar servlets...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} servlets",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "Deve-se selecionar ao menos um servlet.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Contagem de solicitações",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Contagem de solicitações",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Contagem de resposta",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Contagem de resposta",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Tempo médio de resposta (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Tempo de resposta (ns)",
    STATS_CONN_POOL_TITLE : "Conjunto de conexões",
    STATS_CONN_POOL_BUTTON_LABEL : "Incluir todos os diagramas conjunto de conexões para visualização",
    STATS_CONN_POOL_CONFIG_LABEL : "Selecionar origem de dados...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} origens de dados",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 origem de dados",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "Deve-se selecionar ao menos uma origem de dados.",
    STATS_CONNECT_IN_USE_TITLE : "Conexões em uso",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Conexões",
    STATS_CONNECT_IN_USE_LABEL : "Em uso: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Usado: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Livre: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Criado: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Destruído: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Média de Tempo de Espera (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Tempo de espera (ms)",
    STATS_TIME_ALL : "Tudo",
    STATS_TIME_1YEAR : "1a",
    STATS_TIME_1MONTH : "1 mês",
    STATS_TIME_1WEEK : "1s",
    STATS_TIME_1DAY : "1d",
    STATS_TIME_1HOUR : "1h",
    STATS_TIME_10MINUTES : "10m",
    STATS_TIME_5MINUTES : "5m",
    STATS_TIME_1MINUTE : "1m",
    STATS_PERSPECTIVE_SUMMARY : "Resumo",
    STATS_PERSPECTIVE_TRAFFIC : "Tráfego",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "Tráfego JVM",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Tráfego de Conexão",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Acessar Tráfego de Log",
    STATS_PERSPECTIVE_PROBLEM : "Problema",
    STATS_PERSPECTIVE_PERFORMANCE : "Desempenho",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "Desempenho JVM",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Desempenho da Conexão",
    STATS_PERSPECTIVE_ALERT : "Análise de Alerta",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Acessar Alerta de Log",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Alerta de Mensagens e Log de Rastreio",
    STATS_PERSPECTIVE_AVAILABILITY : "Disponibilidade",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Último minuto",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "Últimos 5 minutos",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "Últimos 10 minutos",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Última hora",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Último dia",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Semana passada",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Mês passado",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Ano passado",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Últimos {0}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Últimos {0}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Últimos {0}m {1}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Última {0}h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Últimos {0}h {1}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Último {0}d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Últimos {0}d {1}h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Última {0}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Últimos {0}s {1}d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Último {0} mês",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Últimos {0}mês {1}d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Último {0}a",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Últimos {0}a {1} mês",

    STATS_LIVE_UPDATE_LABEL: "Atualização em tempo real",
    STATS_TIME_SELECTOR_NOW_LABEL: "Agora",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Mensagens de log",

    AUTOSCALED_APPLICATION : "Aplicativo escalado automático",
    AUTOSCALED_SERVER : "Servidor escalado automático",
    AUTOSCALED_CLUSTER : "Cluster escalado automático",
    AUTOSCALED_POLICY : "Política de ajuste de escala automática",
    AUTOSCALED_POLICY_DISABLED : "Política de ajuste de escala automática está desativada",
    AUTOSCALED_NOACTIONS : "As ações não estão disponíveis para recursos escalados automáticos",

    START : "Iniciar",
    START_CLEAN : "Iniciar --limpar",
    STARTING : "Iniciando",
    STARTED : "Iniciado",
    RUNNING : "Em execução",
    NUM_RUNNING: "{0} Em Execução",
    PARTIALLY_STARTED : "Parcialmente iniciado",
    PARTIALLY_RUNNING : "Parcialmente em execução",
    NOT_STARTED : "Não iniciado",
    STOP : "Parar",
    STOPPING : "Interrompendo",
    STOPPED : "Interrompido",
    NUM_STOPPED : "{0} foi interrompido",
    NOT_RUNNING : "Não em execução",
    RESTART : "Reiniciar",
    RESTARTING : "Reiniciando",
    RESTARTED : "Reiniciado",
    ALERT : "Alerta",
    ALERTS : "Alertas",
    UNKNOWN : "Desconhecido",
    NUM_UNKNOWN : "{0} Desconhecido",
    SELECT : "Selecionar",
    SELECTED : "Selecionado",
    SELECT_ALL : "Selecionar tudo",
    SELECT_NONE : "Selecionar nenhum",
    DESELECT: "Cancelar seleção",
    DESELECT_ALL : "Cancelar seleção de todos",
    TOTAL : "Total",
    UTILIZATION : "Utilização acima de {0}%", // percent

    ELLIPSIS_ARIA: "Expandir para obter mais opções.",
    EXPAND : "Expandir",
    COLLAPSE: "Reduzir",

    ALL : "Tudo",
    ALL_APPS : "Todos os aplicativos",
    ALL_SERVERS : "Todos os servidores",
    ALL_CLUSTERS : "Todos os clusters",
    ALL_HOSTS : "Todos os Hosts",
    ALL_APP_INSTANCES : "Todas as instâncias do aplicativo",
    ALL_RUNTIMES : "Todos os tempos de execução",

    ALL_APPS_RUNNING : "Todos os aplicativos em execução",
    ALL_SERVER_RUNNING : "Todos os servidores em execução",
    ALL_CLUSTERS_RUNNING : "Todos os clusters em execução",
    ALL_APPS_STOPPED : "Todos os aplicativos interrompidos",
    ALL_SERVER_STOPPED : "Todos os servidores interrompidos",
    ALL_CLUSTERS_STOPPED : "Todos os clusters interrompidos",
    ALL_SERVERS_UNKNOWN : "Todos os servidores desconhecidos",
    SOME_APPS_RUNNING : "Alguns aplicativos em execução",
    SOME_SERVERS_RUNNING : "Alguns servidores em execução",
    SOME_CLUSTERS_RUNNING : "Alguns clusters em execução",
    NO_APPS_RUNNING : "Nenhum aplicativo em execução",
    NO_SERVERS_RUNNING : "Nenhum servidor em execução",
    NO_CLUSTERS_RUNNING : "Nenhum cluster em execução",

    HOST_WITH_ALL_SERVERS_RUNNING: "Hosts com todos os servidores em execução", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Hosts com alguns servidores em execução",
    HOST_WITH_NO_SERVERS_RUNNING: "Hosts sem servidores em execução", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Hosts com todos os servidores parados",
    HOST_WITH_SERVERS_RUNNING: "Hosts com os servidores em execução",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Tempos de execução com alguns servidores em execução",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Tempos de execução com todos os servidores interrompidos",
    RUNTIME_WITH_SERVERS_RUNNING: "Tempos de execução com servidores em execução",

    START_ALL_APPS : "Iniciar todos os aplicativos?",
    START_ALL_INSTANCES : "Iniciar todas as instâncias do aplicativo?",
    START_ALL_SERVERS : "Iniciar todos os servidores?",
    START_ALL_CLUSTERS : "Iniciar todos os clusters?",
    STOP_ALL_APPS : "Parar todos os aplicativos?",
    STOPE_ALL_INSTANCES : "Parar todas as instâncias do aplicativo?",
    STOP_ALL_SERVERS : "Parar todos os servidores?",
    STOP_ALL_CLUSTERS : "Parar todos os clusters?",
    RESTART_ALL_APPS : "Reiniciar todos os aplicativos?",
    RESTART_ALL_INSTANCES : "Reiniciar todas as instâncias do aplicativo?",
    RESTART_ALL_SERVERS : "Reiniciar todos os servidores?",
    RESTART_ALL_CLUSTERS : "Reiniciar todos os clusters?",

    START_INSTANCE : "Iniciar instância do aplicativo?",
    STOP_INSTANCE : "Parar instância do aplicativo?",
    RESTART_INSTANCE : "Reiniciar instância do aplicativo?",

    START_SERVER : "Iniciar servidor {0}?",
    STOP_SERVER : "Parar servidor {0}?",
    RESTART_SERVER : "Reiniciar servidor {0}?",

    START_ALL_INSTS_OF_APP : "Iniciar todas as instâncias de {0}?", // application name
    START_APP_ON_SERVER : "Iniciar {0} em {1}?", // app name, server name
    START_ALL_APPS_WITHIN : "Iniciar todos os aplicativos dentro de {0}?", // resource
    START_ALL_APP_INSTS_WITHIN : "Iniciar todas as instâncias do aplicativo em {0}?", // resource
    START_ALL_SERVERS_WITHIN : "Iniciar todos os servidores dentro de {0}?", // resource
    STOP_ALL_INSTS_OF_APP : "Parar todas as instâncias de {0}?", // application name
    STOP_APP_ON_SERVER : "Parar {0} em {1}?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Parar todos os aplicativos dentro de {0}?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Parar todas as instâncias do aplicativo em {0}?", // resource
    STOP_ALL_SERVERS_WITHIN : "Parar todos os servidores dentro de {0}?", // resource
    RESTART_ALL_INSTS_OF_APP : "Reiniciar todas as instâncias de {0}?", // application name
    RESTART_APP_ON_SERVER : "Reiniciar {0} em {1}?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Reiniciar todos os aplicativos dentro de {0}?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Reiniciar todas as instâncias do aplicativo em {0}?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Reiniciar todos os servidores em execução dentro de {0}?", // resource

    START_SELECTED_APPS : "Iniciar todas as instâncias de aplicativos selecionados?",
    START_SELECTED_INSTANCES : "Iniciar as instâncias do aplicativo selecionadas?",
    START_SELECTED_SERVERS : "Iniciar os servidores selecionados?",
    START_SELECTED_SERVERS_LABEL : "Iniciar os servidores selecionados",
    START_SELECTED_CLUSTERS : "Iniciar os clusters selecionados?",
    START_CLEAN_SELECTED_SERVERS : "Iniciar servidores selecionados --clean?",
    START_CLEAN_SELECTED_CLUSTERS : "Iniciar clusters selecionados --clean?",
    STOP_SELECTED_APPS : "Parar todas as instâncias de aplicativos selecionados?",
    STOP_SELECTED_INSTANCES : "Parar as instâncias do aplicativo selecionadas?",
    STOP_SELECTED_SERVERS : "Parar os servidores selecionados?",
    STOP_SELECTED_CLUSTERS : "Parar os clusters selecionados?",
    RESTART_SELECTED_APPS : "Reiniciar todas as instâncias de aplicativos selecionados?",
    RESTART_SELECTED_INSTANCES : "Reiniciar as instâncias do aplicativo selecionadas?",
    RESTART_SELECTED_SERVERS : "Reiniciar os servidores selecionados?",
    RESTART_SELECTED_CLUSTERS : "Reiniciar os clusters selecionados?",

    START_SERVERS_ON_HOSTS : "Iniciar todos os servidores nos hosts selecionados?",
    STOP_SERVERS_ON_HOSTS : "Parar todos os servidores nos hosts selecionados?",
    RESTART_SERVERS_ON_HOSTS : "Reiniciar todos os servidores em execução nos hosts selecionados?",

    SELECT_APPS_TO_START : "Selecione os aplicativos interrompidos a serem iniciados.",
    SELECT_APPS_TO_STOP : "Selecione os aplicativos iniciados a serem parados.",
    SELECT_APPS_TO_RESTART : "Selecione os aplicativos iniciados a serem reiniciados.",
    SELECT_INSTANCES_TO_START : "Selecione as instâncias do aplicativo interrompidas a serem iniciadas.",
    SELECT_INSTANCES_TO_STOP : "Selecione as instâncias do aplicativo iniciadas a serem paradas.",
    SELECT_INSTANCES_TO_RESTART : "Selecione as instâncias do aplicativo iniciadas a serem reiniciadas.",
    SELECT_SERVERS_TO_START : "Selecione os servidores interrompidos a serem iniciados.",
    SELECT_SERVERS_TO_STOP : "Selecione os servidores iniciados a serem parados.",
    SELECT_SERVERS_TO_RESTART : "Selecione os servidores iniciados a serem reiniciados.",
    SELECT_CLUSTERS_TO_START : "Selecione os clusters interrompidos a serem iniciados.",
    SELECT_CLUSTERS_TO_STOP : "Selecione os clusters iniciados a serem parados.",
    SELECT_CLUSTERS_TO_RESTART : "Selecione os clusters iniciados a serem reiniciados.",

    STATUS : "Status",
    STATE : "Estado:",
    NAME : "Nome:",
    DIRECTORY : "Diretório",
    INFORMATION : "Informações",
    DETAILS : "Detalhes",
    ACTIONS : "Ações",
    CLOSE : "Fechar",
    HIDE : "Ocultar",
    SHOW_ACTIONS : "Mostrar ações",
    SHOW_SERVER_ACTIONS_LABEL : "Ações do servidor {0}",
    SHOW_APP_ACTIONS_LABEL : "Ações do aplicativo {0}",
    SHOW_CLUSTER_ACTIONS_LABEL : "Ações do cluster {0}",
    SHOW_HOST_ACTIONS_LABEL : "Ações do host {0}",
    SHOW_RUNTIME_ACTIONS_LABEL : "{0} ações de tempo de execução",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "{0} menu de ações do servidor",
    SHOW_APP_ACTIONS_MENU_LABEL : "{0} menu de ações do aplicativo",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "{0} menu de ações do cluster",
    SHOW_HOST_ACTIONS_MENU_LABEL : "{0} menu de ações do host",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "{0} menu de ações de tempo de execução",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "{0} menu de ações de tempo de execução no host",
    SHOW_COLLECTION_MENU_LABEL : "{0} menu de ações de estado da coleção",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "{0} menu de ações de estado da procura",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: estado desconhecido", // resourceName
    UNKNOWN_STATE_APPS : "{0} aplicativos no estado desconhecido", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} instâncias do aplicativo no estado desconhecido", // quantity
    UNKNOWN_STATE_SERVERS : "{0} servidores no estado desconhecido", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} clusters no estado desconhecido", // quantity

    INSTANCES_NOT_RUNNING : "{0} instâncias do aplicativo não em execução", // quantity
    APPS_NOT_RUNNING : "{0} aplicativos não em execução", // quantity
    SERVERS_NOT_RUNNING : "{0} servidores não em execução", // quantity
    CLUSTERS_NOT_RUNNING : "{0} clusters não em execução", // quantity

    APP_STOPPED_ON_SERVER : "{0} interrompidos nos servidores em execução: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} aplicativos interrompidos nos servidores em execução: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} aplicativos interrompidos nos servidores em execução.", // quantity
    NUMBER_RESOURCES : "{0} recursos", // quantity
    NUMBER_APPS : "{0} aplicativos", // quantity
    NUMBER_SERVERS : "{0} servidores", // quantity
    NUMBER_CLUSTERS : "{0} clusters", // quantity
    NUMBER_HOSTS : "{0} hosts", // quantity
    NUMBER_RUNTIMES : "{0} Tempos de execução", // quantity
    SERVERS_INSERT : "servidores",
    INSERT_STOPPED_ON_INSERT : "{0} interrompido(s) ao executar {1}.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} parou no servidor em execução {1}", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "{0} no cluster {1} parou nos servidores em execução: {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} instâncias do aplicativo interrompidas nos servidores em execução.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: instância do aplicativo não em execução", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: nem todos os aplicativos em execução", // serverName[]
    NO_APPS_RUNNING : "{0}: nenhum aplicativo em execução", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} servidores com nem todos os aplicativos em execução", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} servidores sem nenhum aplicativo em execução", // quantity

    COUNT_OF_APPS_SELECTED : "{0} aplicativos selecionados",
    RATIO_RUNNING : "{0} em execução", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0} selecionado(s)",

    NO_HOSTS_SELECTED : "Nenhum host selecionado",
    NO_DEPLOY_RESOURCE : "Nenhum recurso para instalação de implementação",
    NO_TOPOLOGY : "Não há nenhum {0}.",
    COUNT_OF_APPS_STARTED  : "{0} aplicativos iniciados",

    APPS_LIST : "{0} aplicativos",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} instâncias em execução",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} servidores em execução",
    RESOURCE_ON_RESOURCE : "{0} em {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} no servidor {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} no cluster {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "A reinicialização está desativada para este servidor porque ele está hospedando o Admin Center",
    ACTION_DISABLED_FOR_USER: "As ações estão desativadas neste recurso porque o usuário não está autorizado",

    RESTART_AC_TITLE: "Sem reinicialização para o Admin Center",
    RESTART_AC_DESCRIPTION: "{0} está fornecendo o Admin Center. O Admin Center não pode ser reiniciado sozinho.",
    RESTART_AC_MESSAGE: "Todos os outros servidores selecionados serão reiniciados.",
    RESTART_AC_CLUSTER_MESSAGE: "Todos os outros clusters selecionados serão reiniciados.",

    STOP_AC_TITLE: "Parar o Admin Center",
    STOP_AC_DESCRIPTION: "O servidor {0} é um controlador coletivo que executa o Admin Center. Interrompê-lo poderia afetar as operações de gerenciamento coletivas do Liberty e indisponibilizar o Admin Center.",
    STOP_AC_MESSAGE: "Deseja parar esse controlador?",
    STOP_STANDALONE_DESCRIPTION: "O servidor {0} executa o Admin Center. Pará-lo tornará o Admin Center indisponível.",
    STOP_STANDALONE_MESSAGE: "Deseja parar este servidor?",

    STOP_CONTROLLER_TITLE: "Parar Controlador",
    STOP_CONTROLLER_DESCRIPTION: "O servidor {0} é um controlador coletivo. Pará-lo pode impactar as operações coletivas do Liberty.",
    STOP_CONTROLLER_MESSAGE: "Deseja parar esse controlador?",

    STOP_AC_CLUSTER_TITLE: "Para cluster {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "O cluster {0} contém um controlador coletivo que executa o Admin Center.  Pará-lo pode afetar as operações de gerenciamento coletivas do Liberty e tornar o Admin Center indisponível.",
    STOP_AC_CLUSTER_MESSAGE: "Deseja parar este cluster?",

    INVALID_URL: "A página não existe.",
    INVALID_APPLICATION: "O aplicativo {0} não existe mais no coletivo.", // application name
    INVALID_SERVER: "O servidor {0} não existe mais no coletivo.", // server name
    INVALID_CLUSTER: "O cluster {0} não existe mais no coletivo.", // cluster name
    INVALID_HOST: "O host {0} não existe mais no coletivo.", // host name
    INVALID_RUNTIME: "O tempo de execução {0} não existe mais no coletivo.", // runtime name
    INVALID_INSTANCE: "A instância do aplicativo {0} não existe mais no coletivo.", // application instance name
    GO_TO_DASHBOARD: "Acesse o Painel",
    VIEWED_RESOURCE_REMOVED: "Ops! O recurso foi removido ou não está mais disponível.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "A conexão com o servidor foi perdida. A página não mostrará mais as mudanças dinâmicas no ambiente. Atualize a página para restaurar as atualizações de conexão e dinâmica.",
    ERROR_MESSAGE: "Conexão interrompida",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Encerrar Servidor',

    // Tags
    RELATED_RESOURCES: "Recursos relacionados",
    TAGS : "Tags",
    TAG_BUTTON_LABEL : "Tag {0}",  // tag value
    TAGS_LABEL : "Insira as tags separadas com vírgula, espaço, enter ou tab.",
    OWNER : "Proprietário",
    OWNER_BUTTON_LABEL : "Proprietário {0}",  // owner value
    CONTACTS : "Contatos",
    CONTACT_BUTTON_LABEL : "Contato {0}",  // contact value
    PORTS : "Portas",
    CONTEXT_ROOT : "Raiz de Contexto",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Mais",  // alt text for the ... button
    MORE_BUTTON_MENU : "{0} Mais menu", // alt text for the menu
    NOTES: "Notas",
    NOTE_LABEL : "Nota {0}",  // note value
    SET_ATTRIBUTES: "Tags e metadados",
    SETATTR_RUNTIME_NAME: "{0} em {1}",  // runtime, host
    SAVE: "Salvar",
    TAGINVALIDCHARS: "Caracteres '/', '<' e '>' não são válidos.",
    ERROR_GET_TAGS_METADATA: "O produto não pode obter tags e metadados atuais para o recurso.",
    ERROR_SET_TAGS_METADATA: "Um erro evitou que o produto configurasse as tags e os metadados.",
    METADATA_WILLBE_INHERITED: "Metadados são configurados no aplicativo e compartilhados entre todas as instâncias no cluster.",
    ERROR_ALT: "Erro",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "As estatísticas atuais não estão disponíveis para este servidor, pois ele está parado. Inicie o servidor para começar a monitorá-lo.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "As estatísticas atuais não estão disponíveis para este aplicativo, pois seu servidor associado está parado. Inicie o servidor para começar a monitorar este aplicativo.",
    GRAPH_FEATURES_NOT_CONFIGURED: "Ainda não há nada aqui! Monitore este recurso selecionando o ícone Editar e incluindo a métrica.",
    NO_GRAPHS_AVAILABLE: "Não há métricas para incluir. Tente instalar recursos de monitoramento adicionais para tornar mais métricas disponíveis.",
    NO_APPS_GRAPHS_AVAILABLE: "Não há métricas para incluir. Tente instalar recursos de monitoramento adicionais para tornar mais métricas disponíveis. Além disso, assegure-se de que o aplicativo está em uso.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Mudanças não salvas",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "Você possui mudanças não salvas. Se for para outra página, você perderá as mudanças.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Deseja salvar as suas mudanças?",

    NO_CPU_STATS_AVAILABLE : "As estatísticas Uso de CPU não estão disponíveis para este servidor.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "Para ativar esta visualização, instale a ferramenta Configuração do servidor.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Salvar as mudanças em {0} antes de fechar?",
    SAVE: "Salvar",
    DONT_SAVE: "Não salvar",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Ativar o modo de manutenção",
    DISABLE_MAINTENANCE_MODE: "Desativar o modo de manutenção",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Ativar o modo de manutenção",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Desativar o modo de manutenção",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Ativar o modo de manutenção no host e todos os seus servidores ({0} servidores)",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Ativar o modo de manutenção nos hosts e todos os seus servidores ({0} servidores)",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Ativar o modo de manutenção no servidor",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Ativar o modo de manutenção nos servidores",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Desativar o modo de manutenção no host e todos os seus servidores ({0} servidores)",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Desativar o modo de manutenção no servidor",
    BREAK_AFFINITY_LABEL: "Dividir afinidade com as sessões ativas",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Ativar",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Desativar",
    MAINTENANCE_MODE: "Modo de Manutenção",
    ENABLING_MAINTENANCE_MODE: "Desativando o modo de manutenção",
    MAINTENANCE_MODE_ENABLED: "Modo de manutenção ativado",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "O modo de manutenção não foi ativado, pois os servidores alternativos não foram iniciados.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Selecione Forçar para ativar o modo de manutenção sem iniciar os servidores alternativos. Forçar pode dividir as políticas de ajuste de escala automáticas.",
    MAINTENANCE_MODE_FAILED: "O modo de manutenção não pode ser ativado.",
    MAINTENANCE_MODE_FORCE_LABEL: "Forçar",
    MAINTENANCE_MODE_CANCEL_LABEL: "Cancelar",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0} servidores estão atualmente no modo de manutenção.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Ativando o modo de manutenção em todos os servidores host.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Ativando o modo de manutenção em todos os servidores host.  Exibir a visualização Servidores para status.",

    SERVER_API_DOCMENTATION: "Visualizar definição de API do servidor",

    // objectView title
    TITLE_FOR_CLUSTER: "Cluster {0}", // cluster name
    TITLE_FOR_HOST: "Host {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Controlador coletivo",
    LIBERTY_SERVER : "Servidor Liberty",
    NODEJS_SERVER : "Servidor Node.js",
    CONTAINER : "Contêiner",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Servidor Liberty em um contêiner Docker",
    NODEJS_IN_DOCKER_DESCRIPTOR : "O servidor Node.js em um contêiner Docker",
    RUNTIME_LIBERTY : "Tempo de execução Liberty",
    RUNTIME_NODEJS : "Tempo de execução do Node.js",
    RUNTIME_DOCKER : "Tempo de execução em um contêiner Docker"

});
