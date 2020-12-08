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
    EXPLORE : "Esplora",
    DASHBOARD : "Dashboard",
    DASHBOARD_VIEW_ALL_APPS : "Visualizza tutte le applicazioni",
    DASHBOARD_VIEW_ALL_SERVERS : "Visualizza tutti i server",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Visualizza tutti i cluster",
    DASHBOARD_VIEW_ALL_HOSTS : "Visualizza tutti gli host",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Visualizza tutti i runtime",
    SEARCH : "Ricerca",
    SEARCH_RECENT : "Ricerche recenti ",
    SEARCH_RESOURCES : "Ricerca nelle risorse",
    SEARCH_RESULTS : "Risultati della ricerca",
    SEARCH_NO_RESULTS : "Nessun risultato",
    SEARCH_NO_MATCHES : "Nessuna corrispondenza",
    SEARCH_TEXT_INVALID : "Il testo di ricerca contiene caratteri non validi",
    SEARCH_CRITERIA_INVALID : "I criteri di ricerca non sono validi.",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} non è valido quando specificato con {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "Specificare {0} solo una volta.",
    SEARCH_TEXT_MISSING : "Il testo di ricerca è obbligatorio",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "La ricerca di tag applicazione su un server non è supportata.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "La ricerca di tag applicazione su un cluster non è supportata.",
    SEARCH_UNSUPPORT : "I criteri di ricerca non sono supportati.",
    SEARCH_SWITCH_VIEW : "Cambia vista",
    FILTERS : "Filtri",
    DEPLOY_SERVER_PACKAGE : "Distribuisci package server",
    MEMBER_OF : "Membro di",
    N_CLUSTERS: "{0} cluster ...",

    INSTANCE : "Istanza",
    INSTANCES : "Istanze",
    APPLICATION : "Applicazione",
    APPLICATIONS : "Applicazioni",
    SERVER : "Server",
    SERVERS : "Server",
    CLUSTER : "Cluster",
    CLUSTERS : "Cluster",
    CLUSTER_NAME : "Nome cluster: ",
    CLUSTER_STATUS : "Stato cluster: ",
    APPLICATION_NAME : "Nome applicazione: ",
    APPLICATION_STATE : "Stato applicazione: ",
    HOST : "Host",
    HOSTS : "Host",
    RUNTIME : "Runtime",
    RUNTIMES : "Runtime",
    PATH : "Percorso",
    CONTROLLER : "Controllore",
    CONTROLLERS : "Controllori",
    OVERVIEW : "Panoramica",
    CONFIGURE : "Configura",

    SEARCH_RESOURCE_TYPE: "Tipo", // Search by resource types
    SEARCH_RESOURCE_STATE: "Stato", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Tutto", // Search all resource types
    SEARCH_RESOURCE_NAME: "Nome", // Search by resource name
    SEARCH_RESOURCE_TAG: "Tag", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Contenitore", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Nessuno", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Tipo di runtime", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Proprietario", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Contatto ", // Search by contact
    SEARCH_RESOURCE_NOTE: "Nota ", // Search by note

    GRID_HEADER_USERDIR : "Directory utente",
    GRID_HEADER_NAME : "Nome",
    GRID_LOCATION_NAME : "Ubicazione",
    GRID_ACTIONS : "Azioni griglia",
    GRID_ACTIONS_LABEL : "Azioni griglia {0}",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} su {1} ({2})", // server on host (/path)

    STATS : "Monitor",
    STATS_ALL : "Tutto",
    STATS_VALUE : "Valore: {0}",
    CONNECTION_IN_USE_STATS : "{0} In uso = {1} Gestito - {2} Libero",
    CONNECTION_IN_USE_STATS_VALUE : "Valore: {0} In uso = {1} Gestito - {2} Libero",
    DATA_SOURCE : "Origine dati: {0}",
    STATS_DISPLAY_LEGEND : "Mostra legenda",
    STATS_HIDE_LEGEND : "Nascondi legenda",
    STATS_VIEW_DATA : "Visualizza dati grafico",
    STATS_VIEW_DATA_TIMESTAMP : "Data/ora",
    STATS_ACTION_MENU : "Menu azioni {0}",
    STATS_SHOW_HIDE : "Aggiungi metriche risorsa",
    STATS_SHOW_HIDE_SUMMARY : "Aggiungi metriche per il riepilogo",
    STATS_SHOW_HIDE_TRAFFIC : "Aggiungi metriche per il traffico",
    STATS_SHOW_HIDE_PERFORMANCE : "Aggiungi metriche per le prestazioni",
    STATS_SHOW_HIDE_AVAILABILITY : "Aggiungi metriche per la disponibilità",
    STATS_SHOW_HIDE_ALERT : "Aggiungi metriche per gli avvisi",
    STATS_SHOW_HIDE_LIST_BUTTON : "Mostra o nascondi elenco metriche di risorsa",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Modifica grafici",
    STATS_SHOW_HIDE_CONFIRM : "Salva",
    STATS_SHOW_HIDE_CANCEL : "Annulla",
    STATS_SHOW_HIDE_DONE : "Eseguito",
    STATS_DELETE_GRAPH : "Elimina grafico",
    STATS_ADD_CHART_LABEL : "Aggiungi grafico alla vista",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Aggiungi tutti i grafici JVM alla vista",
    STATS_HEAP_TITLE : "Memoria heap utilizzata",
    STATS_HEAP_USED : "Utilizzata: {0} MB",
    STATS_HEAP_COMMITTED : "Sottoposta a commit: {0} MB",
    STATS_HEAP_MAX : "Max: {0} MB",
    STATS_HEAP_X_TIME : "Tempo",
    STATS_HEAP_Y_MB : "MB utilizzati",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "Classi caricate",
    STATS_CLASSES_LOADED : "Caricate: {0}",
    STATS_CLASSES_UNLOADED : "Scaricate: {0}",
    STATS_CLASSES_TOTAL : "Totali: {0}",
    STATS_CLASSES_Y_TOTAL : "Classi caricate",
    STATS_PROCESSCPU_TITLE : "Utilizzo della CPU",
    STATS_PROCESSCPU_USAGE : "Utilizzo della CPU: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "Percentuale CPU",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Thread JVM attivi",
    STATS_LIVE_MSG_INIT : "Visualizzazione dei dati in tempo reale",
    STATS_LIVE_MSG :"Questo grafico non possiede dati cronologici. Continuerà a mostrare i dati degli ultimi 10 minuti.",
    STATS_THREADS_ACTIVE : "Live: {0}",
    STATS_THREADS_PEAK : "Picco: {0}",
    STATS_THREADS_TOTAL : "Totali: {0}",
    STATS_THREADS_Y_THREADS : "Thread",
    STATS_TP_POOL_SIZE : "Dimensione pool",
    STATS_JAXWS_TITLE : "Servizi Web JAX-WS",
    STATS_JAXWS_BUTTON_LABEL : "Aggiungi tutti i grafici Servizi Web JAX-WS alla vista",
    STATS_JW_AVG_RESP_TIME : "Tempo medio di risposta",
    STATS_JW_AVG_INVCOUNT : "Conteggio richiami medio",
    STATS_JW_TOTAL_FAULTS : "Totale errori di runtime",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Seleziona risorse...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} risorse",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 risorsa",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "È necessario selezionare almeno una risorsa.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "Non ci sono dati disponibili per l'intervallo di tempo selezionato.",
    STATS_ACCESS_LOG_TITLE : "Log accessi",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Aggiungi tutti i grafici Log accessi alla vista",
    STATS_ACCESS_LOG_GRAPH : "Conteggio messaggi log accessi",
    STATS_ACCESS_LOG_SUMMARY : "Riepilogo log accessi",
    STATS_ACCESS_LOG_TABLE : "Elenco messaggi log accessi",
    STATS_MESSAGES_TITLE : "Messaggi e traccia",
    STATS_MESSAGES_BUTTON_LABEL : "Aggiungi tutti i grafici Messaggi e traccia alla vista",
    STATS_MESSAGES_GRAPH : "Conteggio messaggi log",
    STATS_MESSAGES_TABLE : "Elenco messaggi log",
    STATS_FFDC_GRAPH : "Conteggio FFDC",
    STATS_FFDC_TABLE : "Elenco FFDC",
    STATS_TRACE_LOG_GRAPH : "Conteggio messaggi traccia",
    STATS_TRACE_LOG_TABLE : "Elenco messaggi traccia",
    STATS_THREAD_POOL_TITLE : "Pool di thread",
    STATS_THREAD_POOL_BUTTON_LABEL : "Aggiungi tutti i grafici Pool di thread alla vista",
    STATS_THREADPOOL_TITLE : "Thread Liberty attivi",
    STATS_THREADPOOL_SIZE : "Dimensione pool: {0}",
    STATS_THREADPOOL_ACTIVE : "Attivi: {0}",
    STATS_THREADPOOL_TOTAL : "Totali: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Thread attivi",
    STATS_SESSION_MGMT_TITLE : "Sessioni",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Aggiungi tutti i grafici Sessioni alla vista",
    STATS_SESSION_CONFIG_LABEL : "Seleziona sessioni...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} sessioni",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 sessione",
    STATS_SESSION_CONFIG_SELECT_ONE : "È necessario selezionare almeno una sessione.",
    STATS_SESSION_TITLE : "Sessioni attive",
    STATS_SESSION_Y_ACTIVE : "Sessioni attive",
    STATS_SESSION_LIVE_LABEL : "Conteggio live: {0}",
    STATS_SESSION_CREATE_LABEL : "Conteggio creazioni: {0}",
    STATS_SESSION_INV_LABEL : "Conteggio invalidate: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Conteggio invalidate per timeout: {0}",
    STATS_WEBCONTAINER_TITLE : "Applicazioni web",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Aggiungi tutti i grafici Applicazioni Web alla vista",
    STATS_SERVLET_CONFIG_LABEL : "Seleziona servlet...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} servlet",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "È necessario selezionare almeno un servlet.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Conteggio richieste",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Conteggio richieste",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Conteggio risposte",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Conteggio risposte",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Tempo medio di risposta (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Tempo di risposta (ns)",
    STATS_CONN_POOL_TITLE : "Pool di connessioni",
    STATS_CONN_POOL_BUTTON_LABEL : "Aggiungi tutti i grafici Pool di connessioni alla vista",
    STATS_CONN_POOL_CONFIG_LABEL : "Seleziona origini dati...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} origini dati",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 origine dati",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "È necessario selezionare almeno una origine dati.",
    STATS_CONNECT_IN_USE_TITLE : "Connessioni in uso",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Connessioni",
    STATS_CONNECT_IN_USE_LABEL : "In uso: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Utilizzate: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Libere: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Create: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Eliminate: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Tempo medio di attesa (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Tempo di attesa (ms)",
    STATS_TIME_ALL : "Tutto",
    STATS_TIME_1YEAR : "1a",
    STATS_TIME_1MONTH : "1me",
    STATS_TIME_1WEEK : "1s",
    STATS_TIME_1DAY : "1g",
    STATS_TIME_1HOUR : "1h",
    STATS_TIME_10MINUTES : "10m",
    STATS_TIME_5MINUTES : "5m",
    STATS_TIME_1MINUTE : "1m",
    STATS_PERSPECTIVE_SUMMARY : "Riepilogo",
    STATS_PERSPECTIVE_TRAFFIC : "Traffico ",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "Traffico JVM",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Traffico connessione",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Traffico log accessi",
    STATS_PERSPECTIVE_PROBLEM : "Problema",
    STATS_PERSPECTIVE_PERFORMANCE : "Prestazioni",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "Prestazione JVM",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Prestazione connessione",
    STATS_PERSPECTIVE_ALERT : "Anali avviso",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Avviso log accessi",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Avviso log messaggi e di traccia",
    STATS_PERSPECTIVE_AVAILABILITY : "Disponibilità",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Ultimo minuto",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "Ultimi 5 minuti",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "Ultimi 10 minuti",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Ultima ora",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Ultimo giorno",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Ultima settimana",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Ultimo mese",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Ultimo anno",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Ultimo {0}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Ultimo {0}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Ultimo {0}m {1}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Ultima {0}h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Ultima {0}h {1}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Ultimo {0}g",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Ultimo {0}g {1}h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Ultima {0}set",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Ultima {0}set {1}g",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Ultimo {0}mese",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Ultimo {0}mese {1}g",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Ultimo {0}a",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Ultimo {0}a {1}mese",

    STATS_LIVE_UPDATE_LABEL: "Aggiornamento live",
    STATS_TIME_SELECTOR_NOW_LABEL: "Ora",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Messaggi di log",

    AUTOSCALED_APPLICATION : "Applicazione scalata automaticamente",
    AUTOSCALED_SERVER : "Server scalato automaticamente",
    AUTOSCALED_CLUSTER : "Cluster scalato automaticamente",
    AUTOSCALED_POLICY : "Politica di scala automatica",
    AUTOSCALED_POLICY_DISABLED : "La politica di scala automatica è disabilitata",
    AUTOSCALED_NOACTIONS : "Non sono disponibili azioni per risorse scalate automaticamente",

    START : "Avvia",
    START_CLEAN : "Avvia --clean",
    STARTING : "Avvio in corso",
    STARTED : "Avviato",
    RUNNING : "In esecuzione",
    NUM_RUNNING: "{0} In esecuzione",
    PARTIALLY_STARTED : "Parzialmente avviato",
    PARTIALLY_RUNNING : "Parzialmente in esecuzione",
    NOT_STARTED : "Non avviato",
    STOP : "Arresta",
    STOPPING : "Arresto in corso",
    STOPPED : "Arrestato",
    NUM_STOPPED : "{0} Arrestato",
    NOT_RUNNING : "Non in esecuzione",
    RESTART : "Riavvia",
    RESTARTING : "Riavvio in corso",
    RESTARTED : "Riavviato",
    ALERT : "Avviso",
    ALERTS : "Avvisi",
    UNKNOWN : "Sconosciuto",
    NUM_UNKNOWN : "{0} Sconosciuto",
    SELECT : "Seleziona",
    SELECTED : "Selezionato",
    SELECT_ALL : "Seleziona tutto",
    SELECT_NONE : "Nessuna selezione",
    DESELECT: "Deseleziona",
    DESELECT_ALL : "Deseleziona tutto",
    TOTAL : "Totale",
    UTILIZATION : "Utilizzo superiore al {0}%", // percent

    ELLIPSIS_ARIA: "Espandere per vedere altre opzioni. ",
    EXPAND : "Espandi ",
    COLLAPSE: "Comprimi",

    ALL : "Tutto",
    ALL_APPS : "Tutte le applicazioni",
    ALL_SERVERS : "Tutti i server",
    ALL_CLUSTERS : "Tutti i cluster",
    ALL_HOSTS : "Tutti gli host",
    ALL_APP_INSTANCES : "Tutte le istanze applicazione",
    ALL_RUNTIMES : "Tutti i runtime",

    ALL_APPS_RUNNING : "Tutte le applicazioni in esecuzione",
    ALL_SERVER_RUNNING : "Tutti i server in esecuzione",
    ALL_CLUSTERS_RUNNING : "Tutti i cluster in esecuzione",
    ALL_APPS_STOPPED : "Tutte le applicazioni arrestate",
    ALL_SERVER_STOPPED : "Tutti i server arrestati",
    ALL_CLUSTERS_STOPPED : "Tutti i cluster arrestati",
    ALL_SERVERS_UNKNOWN : "Tutti i server sconosciuti",
    SOME_APPS_RUNNING : "Alcune applicazioni in esecuzione",
    SOME_SERVERS_RUNNING : "Alcuni server in esecuzione",
    SOME_CLUSTERS_RUNNING : "Alcuni cluster in esecuzione",
    NO_APPS_RUNNING : "Nessuna applicazione in esecuzione",
    NO_SERVERS_RUNNING : "Nessun server in esecuzione",
    NO_CLUSTERS_RUNNING : "Nessun cluster in esecuzione",

    HOST_WITH_ALL_SERVERS_RUNNING: "Host con tutti i server in esecuzione", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Host con alcuni server in esecuzione",
    HOST_WITH_NO_SERVERS_RUNNING: "Host con nessun server in esecuzione", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Host con tutti i server arrestati",
    HOST_WITH_SERVERS_RUNNING: "Host con server in esecuzione",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Runtime con alcuni server in esecuzione",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Runtime con tutti i server arrestati",
    RUNTIME_WITH_SERVERS_RUNNING: "Runtime con server in esecuzione",

    START_ALL_APPS : "Avviare tutte le applicazioni?",
    START_ALL_INSTANCES : "Avviare tutte le istanze applicazione?",
    START_ALL_SERVERS : "Avviare tutti i server?",
    START_ALL_CLUSTERS : "Avviare tutti i cluster?",
    STOP_ALL_APPS : "Arrestare tutte le applicazioni?",
    STOPE_ALL_INSTANCES : "Arrestare tutte le istanze applicazione?",
    STOP_ALL_SERVERS : "Arrestare tutti i server?",
    STOP_ALL_CLUSTERS : "Arrestare tutti i cluster?",
    RESTART_ALL_APPS : "Riavviare tutte le applicazioni?",
    RESTART_ALL_INSTANCES : "Riavviare tutte le istanze applicazione?",
    RESTART_ALL_SERVERS : "Riavviare tutti i server?",
    RESTART_ALL_CLUSTERS : "Riavviare tutti i cluster?",

    START_INSTANCE : "Avviare l'istanza applicazione?",
    STOP_INSTANCE : "Arrestare l'istanza applicazione?",
    RESTART_INSTANCE : "Riavviare l'istanza applicazione?",

    START_SERVER : "Avviare il server {0}?",
    STOP_SERVER : "Arrestare il server {0}?",
    RESTART_SERVER : "Riavviare il server {0}?",

    START_ALL_INSTS_OF_APP : "Avviare tutte le istanze di {0}?", // application name
    START_APP_ON_SERVER : "Avviare {0} su {1}?", // app name, server name
    START_ALL_APPS_WITHIN : "Avviare tutte le applicazioni in {0}?", // resource
    START_ALL_APP_INSTS_WITHIN : "Avviare tutte le istanze applicazione in {0}?", // resource
    START_ALL_SERVERS_WITHIN : "Avviare tutti i server in {0}?", // resource
    STOP_ALL_INSTS_OF_APP : "Arrestare tutte le istanze di {0}?", // application name
    STOP_APP_ON_SERVER : "Arrestare {0} su {1}?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Arrestare tutte le applicazioni in {0}?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Arrestare tutte le istanze applicazione in {0}?", // resource
    STOP_ALL_SERVERS_WITHIN : "Arrestare tutti i server in {0}?", // resource
    RESTART_ALL_INSTS_OF_APP : "Riavviare tutte le istanze di {0}?", // application name
    RESTART_APP_ON_SERVER : "Riavviare {0} su {1}?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Riavviare tutte le applicazioni in {0}?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Riavviare tutte le istanze applicazione in {0}?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Riavviare tutti i server in esecuzione in {0}?", // resource

    START_SELECTED_APPS : "Avviare tutte le istanze delle applicazioni selezionate?",
    START_SELECTED_INSTANCES : "Avviare le istanze applicazione selezionate?",
    START_SELECTED_SERVERS : "Avviare i server selezionati?",
    START_SELECTED_SERVERS_LABEL : "Avvia server selezionati",
    START_SELECTED_CLUSTERS : "Avviare i cluster selezionati?",
    START_CLEAN_SELECTED_SERVERS : "Avviare --clean dei server selezionati?",
    START_CLEAN_SELECTED_CLUSTERS : "Avviare --clean dei cluster selezionati?",
    STOP_SELECTED_APPS : "Arrestare tutte le istanze delle applicazioni selezionate?",
    STOP_SELECTED_INSTANCES : "Arrestare le istanze applicazione selezionate?",
    STOP_SELECTED_SERVERS : "Arrestare i server selezionati?",
    STOP_SELECTED_CLUSTERS : "Arrestare i cluster selezionati?",
    RESTART_SELECTED_APPS : "Riavviare tutte le istanze delle applicazioni selezionate?",
    RESTART_SELECTED_INSTANCES : "Riavviare le istanze applicazione selezionate?",
    RESTART_SELECTED_SERVERS : "Riavviare i server selezionati?",
    RESTART_SELECTED_CLUSTERS : "Riavviare i cluster selezionati?",

    START_SERVERS_ON_HOSTS : "Avviare tutti i server sugli host selezionati?",
    STOP_SERVERS_ON_HOSTS : "Arrestare tutti i server sugli host selezionati?",
    RESTART_SERVERS_ON_HOSTS : "Riavviare tutti i server in esecuzione  sugli host selezionati?",

    SELECT_APPS_TO_START : "Selezionare le applicazioni arrestate da avviare.",
    SELECT_APPS_TO_STOP : "Selezionare le applicazioni avviate da arrestare.",
    SELECT_APPS_TO_RESTART : "Selezionare le applicazioni avviate da riavviare.",
    SELECT_INSTANCES_TO_START : "Selezionare le istanze applicazione arrestate da avviare.",
    SELECT_INSTANCES_TO_STOP : "Selezionare le istanze applicazione avviate da arrestare.",
    SELECT_INSTANCES_TO_RESTART : "Selezionare le istanze applicazione avviate da riavviare.",
    SELECT_SERVERS_TO_START : "Selezionare i server arrestati da avviare.",
    SELECT_SERVERS_TO_STOP : "Selezionare i server avviati da arrestare.",
    SELECT_SERVERS_TO_RESTART : "Selezionare i server avviati da riavviare.",
    SELECT_CLUSTERS_TO_START : "Selezionare i cluster arrestati da avviare.",
    SELECT_CLUSTERS_TO_STOP : "Selezionare i cluster avviati da arrestare.",
    SELECT_CLUSTERS_TO_RESTART : "Selezionare i cluster avviati da riavviare.",

    STATUS : "Stato",
    STATE : "Stato:",
    NAME : "Nome:",
    DIRECTORY : "Directory",
    INFORMATION : "Informazioni",
    DETAILS : "Dettagli",
    ACTIONS : "Azioni",
    CLOSE : "Chiudi",
    HIDE : "Nascondi",
    SHOW_ACTIONS : "Mostra azioni",
    SHOW_SERVER_ACTIONS_LABEL : "Azioni server {0}",
    SHOW_APP_ACTIONS_LABEL : "Azioni applicazione {0}",
    SHOW_CLUSTER_ACTIONS_LABEL : "Azioni cluster {0}",
    SHOW_HOST_ACTIONS_LABEL : "Azioni host {0}",
    SHOW_RUNTIME_ACTIONS_LABEL : "Azioni runtime {0}",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "Menu azioni server {0}",
    SHOW_APP_ACTIONS_MENU_LABEL : "Menu azioni applicazione {0}",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "Menu azioni cluster {0}",
    SHOW_HOST_ACTIONS_MENU_LABEL : "Menu azioni host {0}",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "Menu azioni runtime {0}",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "Menu azioni runtime su host {0}",
    SHOW_COLLECTION_MENU_LABEL : "Menu azioni stato raccolta {0}",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "Menu azioni stato ricerca {0}",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: stato sconosciuto", // resourceName
    UNKNOWN_STATE_APPS : "{0} applicazioni in stato sconosciuto", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} istanze applicazione in stato sconosciuto", // quantity
    UNKNOWN_STATE_SERVERS : "{0} server in stato sconosciuto", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} cluster in stato sconosciuto", // quantity

    INSTANCES_NOT_RUNNING : "{0} istanze applicazione non in esecuzione", // quantity
    APPS_NOT_RUNNING : "{0} applicazioni non in esecuzione", // quantity
    SERVERS_NOT_RUNNING : "{0} server non in esecuzione", // quantity
    CLUSTERS_NOT_RUNNING : "{0} cluster non in esecuzione", // quantity

    APP_STOPPED_ON_SERVER : "{0} arrestato su server in esecuzione: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} applicazioni arrestate su server in esecuzione: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} applicazioni arrestate su server in esecuzione.", // quantity
    NUMBER_RESOURCES : "{0} risorse", // quantity
    NUMBER_APPS : "{0} applicazioni", // quantity
    NUMBER_SERVERS : "{0} server", // quantity
    NUMBER_CLUSTERS : "{0} cluster", // quantity
    NUMBER_HOSTS : "{0} host", // quantity
    NUMBER_RUNTIMES : "{0} Runtime", // quantity
    SERVERS_INSERT : "server",
    INSERT_STOPPED_ON_INSERT : "{0} arrestato su {1} in esecuzione.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} arrestato sul server in esecuzione {1}", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "{0} sul cluster {1} arrestato sui server in esecuzione: {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} istanze applicazione arrestate su server in esecuzione.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: istanza applicazione non in esecuzione", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: non tutte le applicazioni sono in esecuzione", // serverName[]
    NO_APPS_RUNNING : "{0}: nessuna applicazione in esecuzione", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "Su {0} server non tutte le applicazioni sono in esecuzione", // quantity
    NO_APPS_RUNNING_SERVERS : "Su {0} server non ci sono applicazioni in esecuzione", // quantity

    COUNT_OF_APPS_SELECTED : "{0} applicazioni selezionate",
    RATIO_RUNNING : "{0} in esecuzione", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0} selezionati",

    NO_HOSTS_SELECTED : "Nessun host selezionato",
    NO_DEPLOY_RESOURCE : "Nessuna risorsa per distribuire l'installazione",
    NO_TOPOLOGY : "Nessun {0} presente.",
    COUNT_OF_APPS_STARTED  : "{0} applicazioni avviate",

    APPS_LIST : "{0} applicazioni",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} istanze in esecuzione",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} server in esecuzione",
    RESOURCE_ON_RESOURCE : "{0} su {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} sul server {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} sul cluster {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Il riavvio è disabilitato per questo server perché ospita il centro di gestione",
    ACTION_DISABLED_FOR_USER: "Le azioni sono disabilitate su questa risorsa perché l'utente non è autorizzato ",

    RESTART_AC_TITLE: "Nessun riavvio per il centro di gestione",
    RESTART_AC_DESCRIPTION: "{0} sta fornendo il centro di gestione. Il centro di gestione non può riavviarsi da solo.",
    RESTART_AC_MESSAGE: "Tutti gli altri server selezionati verranno riavviati.",
    RESTART_AC_CLUSTER_MESSAGE: "Tutti gli altri cluster selezionati verranno riavviati.",

    STOP_AC_TITLE: "Arresta centro di gestione",
    STOP_AC_DESCRIPTION: "Il server {0} è un Collective Controller che utilizza il centro di gestione. Il suo arresto può interferire sulle operazioni di gestione collettive di Liberty e rendere il centro di gestione non disponibile.",
    STOP_AC_MESSAGE: "Arrestare questo controllore?",
    STOP_STANDALONE_DESCRIPTION: "Il server {0} sta eseguendo il centro di gestione. Il suo arresto rende indisponibile il centro di gestione.",
    STOP_STANDALONE_MESSAGE: "Arrestare questo server?",

    STOP_CONTROLLER_TITLE: "Arresta controllore",
    STOP_CONTROLLER_DESCRIPTION: "Il server {0} è un Collective Controller. Il suo arresto può interferire sulle operazioni collettive di Liberty.",
    STOP_CONTROLLER_MESSAGE: "Arrestare questo controllore?",

    STOP_AC_CLUSTER_TITLE: "Arresta cluster {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "Il cluster {0} contiene un Collective Controller che utilizza il centro di gestione.  Il suo arresto può interferire sulle operazioni di gestione collettive di Liberty e rendere il centro di gestione non disponibile.",
    STOP_AC_CLUSTER_MESSAGE: "Arrestare questo cluster?",

    INVALID_URL: "La pagina non esiste.",
    INVALID_APPLICATION: "L'applicazione {0} non esiste più nel Collective.", // application name
    INVALID_SERVER: "Il server {0} non esiste più nel Collective.", // server name
    INVALID_CLUSTER: "Il cluster {0} non esiste più nel Collective.", // cluster name
    INVALID_HOST: "L'host {0} non esiste più nel Collective.", // host name
    INVALID_RUNTIME: "Il runtime {0} non esiste più nel Collective.", // runtime name
    INVALID_INSTANCE: "L'istanza applicazione {0} non esiste più nel Collective.", // application instance name
    GO_TO_DASHBOARD: "Vai al dashboard",
    VIEWED_RESOURCE_REMOVED: "Spiacenti. La risorsa è stata rimossa o  non è più disponibile.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "La connessione al server è stata persa. La pagina non mostrerà più le modifiche dinamiche all'ambiente. Aggiornare la pagina per ripristinare la connessione e gli aggiornamenti dinamici.",
    ERROR_MESSAGE: "Connessione interrotta",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Arresta il server',

    // Tags
    RELATED_RESOURCES: "Risorse correlate",
    TAGS : "Tag",
    TAG_BUTTON_LABEL : "Tag {0}",  // tag value
    TAGS_LABEL : "Immettere le tag separate da virgola, spazio, invio o tabulazione.",
    OWNER : "Proprietario",
    OWNER_BUTTON_LABEL : "Proprietario {0}",  // owner value
    CONTACTS : "Contatti",
    CONTACT_BUTTON_LABEL : "Contatto {0}",  // contact value
    PORTS : "Porte",
    CONTEXT_ROOT : "Root di contesto",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Altro",  // alt text for the ... button
    MORE_BUTTON_MENU : "Menu Altro {0}", // alt text for the menu
    NOTES: "Note",
    NOTE_LABEL : "Nota {0}",  // note value
    SET_ATTRIBUTES: "Tag e metadati",
    SETATTR_RUNTIME_NAME: "{0} su {1}",  // runtime, host
    SAVE: "Salva",
    TAGINVALIDCHARS: "I caratteri '/', '<' e '>' non sono validi.",
    ERROR_GET_TAGS_METADATA: "Il prodotto non può richiamare le tag e i metadati correnti per la risorsa.",
    ERROR_SET_TAGS_METADATA: "Un errore ha impedito al prodotto di impostare le tag e i metadati.",
    METADATA_WILLBE_INHERITED: "I metadati sono impostati sull'applicazione e condivisi in tutte le istanze nel cluster.",
    ERROR_ALT: "Errore",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Le statistiche correnti non sono disponibili per questo server perché è arrestato. Avvia il server per iniziare a monitorarlo.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Le statistiche correnti non sono disponibili per questa applicazione perché il server associato è arrestato. Avvia il server per iniziare a monitorare questa applicazione.",
    GRAPH_FEATURES_NOT_CONFIGURED: "Non sono ancora presenti elementi. Monitorare questa risorsa selezionando l'icona Modifica e aggiungendo metriche.",
    NO_GRAPHS_AVAILABLE: "Non sono disponibili metriche da aggiungere. Tentare con l'installazione di altre funzioni di monitoraggio per rendere disponibili più metriche.",
    NO_APPS_GRAPHS_AVAILABLE: "Non sono disponibili metriche da aggiungere. Tentare con l'installazione di altre funzioni di monitoraggio per rendere disponibili più metriche.Inoltre, assicurarsi che l'applicazione sia in uso.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Modifiche non salvate",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "Sono presenti modifiche non salvate. Se si passa a un'altra pagina si perderanno le modifiche.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Salvare le modifiche?",

    NO_CPU_STATS_AVAILABLE : "Le statistiche sull'utilizzo della CPU non sono disponibili per questo server.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "Per abilitare questa vista, installare lo strumento di configurazione server.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Salvare le modifiche a {0} prima di chiudere?",
    SAVE: "Salva",
    DONT_SAVE: "Non salvare",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Abilita modalità manutenzione",
    DISABLE_MAINTENANCE_MODE: "Disabilita modalità manutenzione",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Abilita modalità manutenzione",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Disabilita modalità manutenzione",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Abilita modalità manutenzione sull'host e tutti i server ({0} server)",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Abilita modalità manutenzione sugli host e tutti i server ({0} server)",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Abilita modalità manutenzione sul server",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Abilita modalità manutenzione sui servers",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Disabilita modalità manutenzione sull'host e tutti i server ({0} server)",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Disabilita modalità manutenzione sul server",
    BREAK_AFFINITY_LABEL: "Interrompi affinità con sessioni attive",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Attivazione",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Disabilita",
    MAINTENANCE_MODE: "Modalità manutenzione",
    ENABLING_MAINTENANCE_MODE: "Abilitazione modalità manutenzione",
    MAINTENANCE_MODE_ENABLED: "Modalità manutenzione abilitata",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "La modalità di manutenzione non è stata abilitata perché non sono stati avviati i server alternativi.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Selezionare Forza, per abilitare la modalità di manutenzione senza avviare i server alternativi. L'opzione Forza può interrompere le politiche di scala automatica.",
    MAINTENANCE_MODE_FAILED: "Impossibile abilitare la modalità di manutenzione.",
    MAINTENANCE_MODE_FORCE_LABEL: "Forza",
    MAINTENANCE_MODE_CANCEL_LABEL: "Annulla",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0} server sono attualmente in modalità di manutenzione.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Abilitazione della modalità di manutenzione su tutti i server host.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Abilitazione della modalità di manutenzione su tutti i server host.  Visualizzare la vista Server per lo stato.",

    SERVER_API_DOCMENTATION: "Visualizza definizione API server",

    // objectView title
    TITLE_FOR_CLUSTER: "Cluster {0}", // cluster name
    TITLE_FOR_HOST: "Host {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Collective controller",
    LIBERTY_SERVER : "Server Liberty",
    NODEJS_SERVER : "Server Node.js",
    CONTAINER : "Contenitore",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Liberty Server in in un contenitore Docker",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Server Node.js in un contenitore Docker",
    RUNTIME_LIBERTY : "Runtime Liberty",
    RUNTIME_NODEJS : "Runtime Node.js",
    RUNTIME_DOCKER : "Runtime in un contenitore Docker"

});
