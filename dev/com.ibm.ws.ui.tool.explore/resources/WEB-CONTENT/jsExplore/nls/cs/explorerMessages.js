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
    EXPLORER : "Průzkumník",
    EXPLORE : "Prozkoumat",
    DASHBOARD : "Panel dashboard",
    DASHBOARD_VIEW_ALL_APPS : "Zobrazit všechny aplikace",
    DASHBOARD_VIEW_ALL_SERVERS : "Zobrazit všechny servery",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Zobrazit všechny klastry",
    DASHBOARD_VIEW_ALL_HOSTS : "Zobrazit všechny hostitele",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Zobrazit všechna běhová prostředí",
    SEARCH : "Hledat",
    SEARCH_RECENT : "Poslední hledání",
    SEARCH_RESOURCES : "Prohledat prostředky",
    SEARCH_RESULTS : "Výsledky vyhledávání",
    SEARCH_NO_RESULTS : "Žádné výsledky",
    SEARCH_NO_MATCHES : "Žádné shody",
    SEARCH_TEXT_INVALID : "Hledaný text zahrnuje neplatné znaky",
    SEARCH_CRITERIA_INVALID : "Vyhledávací kritéria nejsou platná.",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} není platné při určení pomocí {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "{0} určete pouze jednou.",
    SEARCH_TEXT_MISSING : "Hledaný text je povinný",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "Hledání značek aplikací na serveru není podporováno.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "Hledání značek aplikací v klastru není podporováno.",
    SEARCH_UNSUPPORT : "Vyhledávací kritéria nejsou podporována.",
    SEARCH_SWITCH_VIEW : "Přepnout zobrazení",
    FILTERS : "Filtry",
    DEPLOY_SERVER_PACKAGE : "Implementovat balík serveru",
    MEMBER_OF : "Člen",
    N_CLUSTERS: "{0} klastrů...",

    INSTANCE : "Instance",
    INSTANCES : "Instance",
    APPLICATION : "Aplikace",
    APPLICATIONS : "Aplikace",
    SERVER : "Server",
    SERVERS : "Servery",
    CLUSTER : "Klastr",
    CLUSTERS : "Klastry",
    CLUSTER_NAME : "Název klastru: ",
    CLUSTER_STATUS : "Stav klastru: ",
    APPLICATION_NAME : "Název aplikace: ",
    APPLICATION_STATE : "Stav aplikace: ",
    HOST : "Hostitel",
    HOSTS : "Hostitelé",
    RUNTIME : "Běhové prostředí",
    RUNTIMES : "Běhová prostředí",
    PATH : "Cesta",
    CONTROLLER : "Řadič",
    CONTROLLERS : "Řadiče",
    OVERVIEW : "Přehled",
    CONFIGURE : "Konfigurování",

    SEARCH_RESOURCE_TYPE: "Typ", // Search by resource types
    SEARCH_RESOURCE_STATE: "Stav", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Vše", // Search all resource types
    SEARCH_RESOURCE_NAME: "Název", // Search by resource name
    SEARCH_RESOURCE_TAG: "Značka", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Kontejner", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Není", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Běhový typ", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Vlastník", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Kontakt", // Search by contact
    SEARCH_RESOURCE_NOTE: "Poznámka", // Search by note

    GRID_HEADER_USERDIR : "Adresář uživatelů",
    GRID_HEADER_NAME : "Název",
    GRID_LOCATION_NAME : "Umístění",
    GRID_ACTIONS : "Akce mřížky",
    GRID_ACTIONS_LABEL : "Akcí mřížky: {0}",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} na {1} ({2})", // server on host (/path)

    STATS : "Monitor",
    STATS_ALL : "Vše",
    STATS_VALUE : "Hodnota: {0}",
    CONNECTION_IN_USE_STATS : "{0} Používá se = {1} Spravováno - {2} Nevyužitá",
    CONNECTION_IN_USE_STATS_VALUE : "Hodnota: {0} Používá se = {1} Spravováno - {2} Nevyužitá",
    DATA_SOURCE : "Zdroj dat: {0}",
    STATS_DISPLAY_LEGEND : "Zobrazit legendu",
    STATS_HIDE_LEGEND : "Skrýt legendu",
    STATS_VIEW_DATA : "Zobrazit data grafu",
    STATS_VIEW_DATA_TIMESTAMP : "Časové razítko",
    STATS_ACTION_MENU : "Nabídka akcí {0}",
    STATS_SHOW_HIDE : "Přidat metriky prostředků",
    STATS_SHOW_HIDE_SUMMARY : "Přidat metriky pro souhrn",
    STATS_SHOW_HIDE_TRAFFIC : "Přidat metriky pro provoz",
    STATS_SHOW_HIDE_PERFORMANCE : "Přidat metriky pro výkon",
    STATS_SHOW_HIDE_AVAILABILITY : "Přidat metriky pro dostupnost",
    STATS_SHOW_HIDE_ALERT : "Přidat metriky pro výstrahu",
    STATS_SHOW_HIDE_LIST_BUTTON : "Zobrazit nebo skrýt seznam metrik prostředků",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Upravit grafy",
    STATS_SHOW_HIDE_CONFIRM : "Uložit",
    STATS_SHOW_HIDE_CANCEL : "Storno",
    STATS_SHOW_HIDE_DONE : "Hotovo",
    STATS_DELETE_GRAPH : "Odstranit graf",
    STATS_ADD_CHART_LABEL : "Přidat graf pro zobrazení",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Přidat všechny grafy JVM do zobrazení",
    STATS_HEAP_TITLE : "Používat paměť haldy",
    STATS_HEAP_USED : "Využito: {0} MB",
    STATS_HEAP_COMMITTED : "Potvrzeno: {0} MB",
    STATS_HEAP_MAX : "Maximum: {0} MB",
    STATS_HEAP_X_TIME : "Čas",
    STATS_HEAP_Y_MB : "MB využito",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "Načtené třídy",
    STATS_CLASSES_LOADED : "Načteno: {0}",
    STATS_CLASSES_UNLOADED : "Uvolněno: {0}",
    STATS_CLASSES_TOTAL : "Celkem: {0}",
    STATS_CLASSES_Y_TOTAL : "Načtené třídy",
    STATS_PROCESSCPU_TITLE : "Využití procesoru",
    STATS_PROCESSCPU_USAGE : "Využití procesoru: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "Procentní využití procesoru",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Aktivní podprocesy prostředí JVM",
    STATS_LIVE_MSG_INIT : "Ukázka živých dat",
    STATS_LIVE_MSG :"Tento graf nemá žádná historická data. Dále se bude zobrazovat posledních 10 minut dat.",
    STATS_THREADS_ACTIVE : "Aktivní: {0}",
    STATS_THREADS_PEAK : "Ve špičce: {0}",
    STATS_THREADS_TOTAL : "Celkem: {0}",
    STATS_THREADS_Y_THREADS : "Podprocesy",
    STATS_TP_POOL_SIZE : "Velikost fondu",
    STATS_JAXWS_TITLE : "Webové služby JAX-WS",
    STATS_JAXWS_BUTTON_LABEL : "Přidat všechny grafy webových služeb JAX-WS do zobrazení",
    STATS_JW_AVG_RESP_TIME : "Průměrná doba odezvy",
    STATS_JW_AVG_INVCOUNT : "Průměrný počet vyvolání",
    STATS_JW_TOTAL_FAULTS : "Celkový počet běhových poruch",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Vybrat prostředky...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} prostředků",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 prostředek",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "Musíte vybrat alespoň jeden prostředek.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "Pro vybraný rozsah času nejsou dostupná žádná data.",
    STATS_ACCESS_LOG_TITLE : "Protokol přístupu",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Přidat všechny grafy protokolu přístupu do zobrazení",
    STATS_ACCESS_LOG_GRAPH : "Počet zpráv protokolu přístupu",
    STATS_ACCESS_LOG_SUMMARY : "Souhrn protokolu přístupu",
    STATS_ACCESS_LOG_TABLE : "Seznam zpráv protokolu přístupu",
    STATS_MESSAGES_TITLE : "Zprávy a trasování",
    STATS_MESSAGES_BUTTON_LABEL : "Přidat všechny grafy zpráv a trasování do zobrazení",
    STATS_MESSAGES_GRAPH : "Počet zpráv protokolu",
    STATS_MESSAGES_TABLE : "Seznam zpráv protokolu",
    STATS_FFDC_GRAPH : "Počet FFDC",
    STATS_FFDC_TABLE : "Seznam FFDC",
    STATS_TRACE_LOG_GRAPH : "Počet zpráv trasování",
    STATS_TRACE_LOG_TABLE : "Seznam zpráv trasování",
    STATS_THREAD_POOL_TITLE : "Fond podprocesů",
    STATS_THREAD_POOL_BUTTON_LABEL : "Přidat všechny grafy fondů podprocesů do zobrazení",
    STATS_THREADPOOL_TITLE : "Aktivní podprocesy Liberty",
    STATS_THREADPOOL_SIZE : "Velikost fondu: {0}",
    STATS_THREADPOOL_ACTIVE : "Aktivní: {0}",
    STATS_THREADPOOL_TOTAL : "Celkem: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Aktivní podprocesy",
    STATS_SESSION_MGMT_TITLE : "Relace",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Přidat všechny grafy relací do zobrazení",
    STATS_SESSION_CONFIG_LABEL : "Vybrat relace...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} relací",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 relace",
    STATS_SESSION_CONFIG_SELECT_ONE : "Je nutné vybrat alespoň jednu relaci.",
    STATS_SESSION_TITLE : "Aktivní relace",
    STATS_SESSION_Y_ACTIVE : "Aktivní relace",
    STATS_SESSION_LIVE_LABEL : "Počet aktivních: {0}",
    STATS_SESSION_CREATE_LABEL : "Počet vytvořených: {0}",
    STATS_SESSION_INV_LABEL : "Počet zneplatněných: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Počet zneplatněných podle časového limitu: {0}",
    STATS_WEBCONTAINER_TITLE : "Webové aplikace",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Přidat všechny grafy webových aplikací do zobrazení",
    STATS_SERVLET_CONFIG_LABEL : "Vybrat servlety...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} servletů",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "Je třeba vybrat alespoň jeden servlet.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Počet požadavků",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Počet požadavků",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Počet odezev",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Počet odezev",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Průměrná doba odezvy (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Doba odezvy (v ns)",
    STATS_CONN_POOL_TITLE : "Fond připojení",
    STATS_CONN_POOL_BUTTON_LABEL : "Přidat všechny grafy fondů připojení do zobrazení",
    STATS_CONN_POOL_CONFIG_LABEL : "Vybrat zdroje dat...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} zdrojů dat",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 zdroj dat",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "Je nutné vybrat alespoň jeden zdroj dat.",
    STATS_CONNECT_IN_USE_TITLE : "Používaná připojení",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Připojení",
    STATS_CONNECT_IN_USE_LABEL : "Používá se: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Využitá: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Nevyužitá: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Vytvořená: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Zlikvidovaná: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Průměrná doba čekání (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Doba čekání (v ms)",
    STATS_TIME_ALL : "Vše",
    STATS_TIME_1YEAR : "1 rok",
    STATS_TIME_1MONTH : "1 měsíc",
    STATS_TIME_1WEEK : "1 týden",
    STATS_TIME_1DAY : "1 den",
    STATS_TIME_1HOUR : "1 hod",
    STATS_TIME_10MINUTES : "10 min",
    STATS_TIME_5MINUTES : "5 min",
    STATS_TIME_1MINUTE : "1 min",
    STATS_PERSPECTIVE_SUMMARY : "Souhrn",
    STATS_PERSPECTIVE_TRAFFIC : "Provoz",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "Provoz prostředí JVM",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Provoz připojení",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Provoz protokolu přístupu",
    STATS_PERSPECTIVE_PROBLEM : "Problém",
    STATS_PERSPECTIVE_PERFORMANCE : "Výkon",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "Výkon prostředí JVM",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Výkon připojení",
    STATS_PERSPECTIVE_ALERT : "Analýza výstrahy",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Výstraha protokolu přístupu",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Výstraha protokolu zpráv a trasování",
    STATS_PERSPECTIVE_AVAILABILITY : "Dostupnost",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Poslední minuta",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "Posledních 5 minut",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "Posledních 10 minut",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Poslední hodina",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Poslední den",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Minulý týden",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Minulý měsíc",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Loňský rok",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Poslední {0} s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Poslední {0} min",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Poslední {0} min, {1} s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Poslední {0} hod",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Poslední {0} hod, {1} min",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Poslední den: {0}",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Poslední den: {0}, {1} hod",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Minulý týden: {0}",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Minulý týden: {0}, den: {1}",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Minulý měsíc: {0}",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Minulý měsíc: {0}, den: {1}",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Loňský rok {0}",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Loňský rok: {0}, měsíc: {1}",

    STATS_LIVE_UPDATE_LABEL: "Živá aktualizace",
    STATS_TIME_SELECTOR_NOW_LABEL: "Nyní",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Zprávy protokolu",

    AUTOSCALED_APPLICATION : "Automaticky přizpůsobitelná aplikace",
    AUTOSCALED_SERVER : "Automaticky přizpůsobitelný Server",
    AUTOSCALED_CLUSTER : "Automaticky přizpůsobitelný klastr",
    AUTOSCALED_POLICY : "Automaticky přizpůsobitelná zásada",
    AUTOSCALED_POLICY_DISABLED : "Automaticky přizpůsobitelná zásada je zakázána",
    AUTOSCALED_NOACTIONS : "Akce nejsou dostupné pro automaticky přizpůsobitelné prostředky",

    START : "Spuštění",
    START_CLEAN : "Spustit --clean",
    STARTING : "Spouštění",
    STARTED : "Spuštěno",
    RUNNING : "Spuštěno",
    NUM_RUNNING: "Spuštěno: {0}",
    PARTIALLY_STARTED : "Částečně spuštěno",
    PARTIALLY_RUNNING : "Částečně spuštěno",
    NOT_STARTED : "Nezahájeno",
    STOP : "Zastavení",
    STOPPING : "Zastavování",
    STOPPED : "Zastaveno",
    NUM_STOPPED : "Zastaveno {0}",
    NOT_RUNNING : "Nespuštěno",
    RESTART : "Restartovat",
    RESTARTING : "Restartuje se",
    RESTARTED : "Restartováno",
    ALERT : "Výstraha",
    ALERTS : "Výstrahy",
    UNKNOWN : "Neznámý",
    NUM_UNKNOWN : "Neznámý {0}",
    SELECT : "Vybrat",
    SELECTED : "Vybráno",
    SELECT_ALL : "Vybrat vše",
    SELECT_NONE : "Nevybrat nic",
    DESELECT: "Zrušit výběr",
    DESELECT_ALL : "Zrušit veškerý výběr",
    TOTAL : "Celkem",
    UTILIZATION : "Využití přesahuje {0}%", // percent

    ELLIPSIS_ARIA: "Rozbalte pro další volby.",
    EXPAND : "Rozbalit",
    COLLAPSE: "Sbalit",

    ALL : "Vše",
    ALL_APPS : "Všechny aplikace",
    ALL_SERVERS : "Všechny servery",
    ALL_CLUSTERS : "Všechny klastry",
    ALL_HOSTS : "Všichni hostitelé",
    ALL_APP_INSTANCES : "Všechny instance aplikace",
    ALL_RUNTIMES : "Všechna běhová prostředí",

    ALL_APPS_RUNNING : "Všechny aplikace spuštěné",
    ALL_SERVER_RUNNING : "Všechny servery spuštěné",
    ALL_CLUSTERS_RUNNING : "Všechny klastry spuštěné",
    ALL_APPS_STOPPED : "Všechny aplikace zastavené",
    ALL_SERVER_STOPPED : "Všechny servery zastavené",
    ALL_CLUSTERS_STOPPED : "Všechny klastry zastavené",
    ALL_SERVERS_UNKNOWN : "Všechny servery neznámé",
    SOME_APPS_RUNNING : "Některé aplikace jsou spuštěné",
    SOME_SERVERS_RUNNING : "Některé servery jsou spuštěné",
    SOME_CLUSTERS_RUNNING : "Některé klastry jsou spuštěné",
    NO_APPS_RUNNING : "Žádné aplikace nejsou spuštěné",
    NO_SERVERS_RUNNING : "Žádné servery nejsou spuštěné",
    NO_CLUSTERS_RUNNING : "Žádné klastry nejsou spuštěné",

    HOST_WITH_ALL_SERVERS_RUNNING: "Hostitelé se všemi servery spuštěnými", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Hostitelé s některými servery spuštěnými",
    HOST_WITH_NO_SERVERS_RUNNING: "Hostitelé bez spuštěných serverů", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Hostitelé se všemi servery zastavenými",
    HOST_WITH_SERVERS_RUNNING: "Hostitelé se servery spuštěnými",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Běhová prostředí s některými servery spuštěnými",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Běhová prostředí se všemi servery zastavenými",
    RUNTIME_WITH_SERVERS_RUNNING: "Běhová prostředí se servery spuštěnými",

    START_ALL_APPS : "Spustit všechny aplikace?",
    START_ALL_INSTANCES : "Spustit všechny instance aplikace?",
    START_ALL_SERVERS : "Spustit všechny servery?",
    START_ALL_CLUSTERS : "Spustit všechny klastry?",
    STOP_ALL_APPS : "Zastavit všechny aplikace?",
    STOPE_ALL_INSTANCES : "Zastavit všechny instance aplikace?",
    STOP_ALL_SERVERS : "Zastavit všechny servery?",
    STOP_ALL_CLUSTERS : "Zastavit všechny klastry?",
    RESTART_ALL_APPS : "Restartovat všechny aplikace?",
    RESTART_ALL_INSTANCES : "Restartovat všechny instance aplikace?",
    RESTART_ALL_SERVERS : "Restartovat všechny servery?",
    RESTART_ALL_CLUSTERS : "Restartovat všechny klastry?",

    START_INSTANCE : "Spustit instanci aplikace?",
    STOP_INSTANCE : "Zastavit instanci aplikace?",
    RESTART_INSTANCE : "Restartovat instanci aplikace?",

    START_SERVER : "Spustit server {0}?",
    STOP_SERVER : "Zastavit server {0}?",
    RESTART_SERVER : "Restartovat server {0}?",

    START_ALL_INSTS_OF_APP : "Spustit všechny instance aplikace {0}?", // application name
    START_APP_ON_SERVER : "Spustit {0} na serveru {1}?", // app name, server name
    START_ALL_APPS_WITHIN : "Spustit všechny aplikace v rámci prostředku {0}?", // resource
    START_ALL_APP_INSTS_WITHIN : "Spustit všechny instance aplikace v rámci {0}?", // resource
    START_ALL_SERVERS_WITHIN : "Spustit všechny servery v rámci prostředku {0}?", // resource
    STOP_ALL_INSTS_OF_APP : "Zastavit všechny instance aplikace {0}?", // application name
    STOP_APP_ON_SERVER : "Zastavit {0} na serveru {1}?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Zastavit všechny aplikace v rámci prostředku {0}?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Zastavit všechny instance aplikace v rámci {0}?", // resource
    STOP_ALL_SERVERS_WITHIN : "Zastavit všechny servery v rámci prostředku {0}?", // resource
    RESTART_ALL_INSTS_OF_APP : "Restartovat všechny instance aplikace {0}?", // application name
    RESTART_APP_ON_SERVER : "Restartovat {0} na serveru {1}?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Restartovat všechny aplikace v rámci prostředku {0}?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Restartovat všechny instance aplikace v rámci {0}?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Restartovat všechny spuštěné servery v rámci prostředku {0}?", // resource

    START_SELECTED_APPS : "Spustit všechny instance vybraných aplikací?",
    START_SELECTED_INSTANCES : "Spustit vybrané instance aplikace?",
    START_SELECTED_SERVERS : "Spustit vybrané servery?",
    START_SELECTED_SERVERS_LABEL : "Spustit vybrané servery",
    START_SELECTED_CLUSTERS : "Spustit vybrané klastry?",
    START_CLEAN_SELECTED_SERVERS : "Spustit čištění vybraných serverů?",
    START_CLEAN_SELECTED_CLUSTERS : "Spustit čištění vybraných klastrů?",
    STOP_SELECTED_APPS : "Zastavit všechny instance vybraných aplikací?",
    STOP_SELECTED_INSTANCES : "Zastavit vybrané instance aplikace?",
    STOP_SELECTED_SERVERS : "Zastavit vybrané servery?",
    STOP_SELECTED_CLUSTERS : "Zastavit vybrané klastry?",
    RESTART_SELECTED_APPS : "Restartovat všechny instance vybraných aplikací?",
    RESTART_SELECTED_INSTANCES : "Restartovat vybrané instance aplikace?",
    RESTART_SELECTED_SERVERS : "Restartovat vybrané servery?",
    RESTART_SELECTED_CLUSTERS : "Restartovat vybrané klastry?",

    START_SERVERS_ON_HOSTS : "Spustit všechny servery na vybraných hostitelích?",
    STOP_SERVERS_ON_HOSTS : "Zastavit všechny servery na vybraných hostitelích?",
    RESTART_SERVERS_ON_HOSTS : "Restartovat všechny spuštěné servery na vybraných hostitelích?",

    SELECT_APPS_TO_START : "Vyberte, které zastavené aplikace se mají spustit.",
    SELECT_APPS_TO_STOP : "Vyberte, které spuštěné aplikace se mají zastavit.",
    SELECT_APPS_TO_RESTART : "Vyberte, které spuštěné aplikace se mají restartovat.",
    SELECT_INSTANCES_TO_START : "Vyberte, které zastavené instance aplikace se mají spustit.",
    SELECT_INSTANCES_TO_STOP : "Vyberte, které spuštěné instance aplikace se mají zastavit.",
    SELECT_INSTANCES_TO_RESTART : "Vyberte, které spuštěné instance aplikace se mají restartovat.",
    SELECT_SERVERS_TO_START : "Vyberte, které zastavené servery se mají spustit.",
    SELECT_SERVERS_TO_STOP : "Vyberte, které spuštěné servery se mají zastavit.",
    SELECT_SERVERS_TO_RESTART : "Vyberte, které spuštěné servery se mají restartovat.",
    SELECT_CLUSTERS_TO_START : "Vyberte, které zastavené klastry se mají spustit.",
    SELECT_CLUSTERS_TO_STOP : "Vyberte, které spuštěné klastry se mají zastavit.",
    SELECT_CLUSTERS_TO_RESTART : "Vyberte, které spuštěné klastry se mají restartovat.",

    STATUS : "Stav",
    STATE : "Stav:",
    NAME : "Název:",
    DIRECTORY : "Adresář",
    INFORMATION : "Informace",
    DETAILS : "Podrobnosti",
    ACTIONS : "Akce",
    CLOSE : "Zavřít",
    HIDE : "Skrýt",
    SHOW_ACTIONS : "Zobrazit akce",
    SHOW_SERVER_ACTIONS_LABEL : "Akcí serveru: {0}",
    SHOW_APP_ACTIONS_LABEL : "Akcí aplikace: {0}",
    SHOW_CLUSTER_ACTIONS_LABEL : "Akcí klastru: {0}",
    SHOW_HOST_ACTIONS_LABEL : "Akcí hostitele: {0}",
    SHOW_RUNTIME_ACTIONS_LABEL : "Akcí běhového prostředí: {0}",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "Nabídek akcí serveru: {0}",
    SHOW_APP_ACTIONS_MENU_LABEL : "Nabídek akcí aplikace: {0}",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "Nabídek akcí klastru: {0}",
    SHOW_HOST_ACTIONS_MENU_LABEL : "Nabídek akcí hostitele: {0}",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "Nabídek akcí běhového prostředí: {0}",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "Nabídek akcí běhového prostředí na hostiteli: {0}",
    SHOW_COLLECTION_MENU_LABEL : "Nabídek akcí stavu kolekce: {0}",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "Nabídek akcí stavu hledání: {0}",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: neznámý stav", // resourceName
    UNKNOWN_STATE_APPS : "{0} aplikací v neznámém stavu", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} instancí aplikace v neznámém stavu", // quantity
    UNKNOWN_STATE_SERVERS : "{0} serverů v neznámém stavu", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} klastrů v neznámém stavu", // quantity

    INSTANCES_NOT_RUNNING : "{0} instancí aplikace není spuštěno", // quantity
    APPS_NOT_RUNNING : "{0} aplikací není spuštěno", // quantity
    SERVERS_NOT_RUNNING : "{0} serverů není spuštěno", // quantity
    CLUSTERS_NOT_RUNNING : "{0} klastrů není spuštěno", // quantity

    APP_STOPPED_ON_SERVER : "Aplikace {0} zastavena na spuštěných serverech: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} aplikací zastaveno na spuštěných serverech: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} aplikací zastaveno na spuštěných serverech.", // quantity
    NUMBER_RESOURCES : "{0} prostředků", // quantity
    NUMBER_APPS : "{0} Aplikace", // quantity
    NUMBER_SERVERS : "{0} serverů", // quantity
    NUMBER_CLUSTERS : "{0} klastrů", // quantity
    NUMBER_HOSTS : "{0} hostitelů", // quantity
    NUMBER_RUNTIMES : "{0} běhových prostředí", // quantity
    SERVERS_INSERT : "servery",
    INSERT_STOPPED_ON_INSERT : "{0} zastaveno během spuštění {1}.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} zastavené během spuštění serveru {1}", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "{0} na klastru {1} zastaveno během spuštění serverů: {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} instancí aplikace zastaveno na spuštěných serverech.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: není spuštěna instance aplikace", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: nejsou spuštěny všechny aplikace", // serverName[]
    NO_APPS_RUNNING : "{0}: nejsou spuštěny žádné aplikace", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} serverů, kde nejsou spuštěny všechny aplikace", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} serverů, kde nejsou spuštěny žádné aplikace", // quantity

    COUNT_OF_APPS_SELECTED : "Vybraných aplikací: {0}",
    RATIO_RUNNING : "Spuštěno: {0}", // ratio ex. 1/2

    RESOURCES_SELECTED : "Vybráno: {0}",

    NO_HOSTS_SELECTED : "Nejsou vybráni žádní hostitelé",
    NO_DEPLOY_RESOURCE : "Pro instalaci implementace není žádný prostředek",
    NO_TOPOLOGY : "Nejsou žádné {0}.",
    COUNT_OF_APPS_STARTED  : "Spuštěných aplikací: {0}",

    APPS_LIST : "{0} Aplikace",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} spuštěných instancí",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} spuštěných serverů",
    RESOURCE_ON_RESOURCE : "{0} pro {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} na serveru {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} na klastru {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Restart je pro tento server zakázán, protože je hostitelem administračního centra",
    ACTION_DISABLED_FOR_USER: "Akce jsou na tomto prostředku zakázány, protože uživatel není autorizován",

    RESTART_AC_TITLE: "Administrační centrum bez restartu",
    RESTART_AC_DESCRIPTION: "{0} zajišťuje administrační centrum. Administrační centrum se nemůže restartovat.",
    RESTART_AC_MESSAGE: "Všechny ostatní vybrané servery budou restartovány.",
    RESTART_AC_CLUSTER_MESSAGE: "Všechny ostatní vybrané klastry budou restartovány.",

    STOP_AC_TITLE: "Zastavit administrační centrum",
    STOP_AC_DESCRIPTION: "Server {0} je kolektivní řadič, který provozuje administrační centrum. Jeho zastavení může ovlivnit operace kolektivní správy Liberty a znepřístupnit administrační centrum.",
    STOP_AC_MESSAGE: "Chcete tento řadič zastavit?",
    STOP_STANDALONE_DESCRIPTION: "Na serveru {0} je spuštěno administrační centrum. Po jeho zastavení bude administrační centrum nedostupné.",
    STOP_STANDALONE_MESSAGE: "Chcete tento server zastavit?",

    STOP_CONTROLLER_TITLE: "Zastavit řadič",
    STOP_CONTROLLER_DESCRIPTION: "Server {0} je kolektivní řadič. Jeho zastavení může mít vliv na kolektivní operace Liberty.",
    STOP_CONTROLLER_MESSAGE: "Chcete tento řadič zastavit?",

    STOP_AC_CLUSTER_TITLE: "Zastavit klastr {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "Klastr {0} obsahuje kolektivní řadič, který spouští administrační centrum. Jeho zastavení může ovlivnit operace kolektivní správy Liberty a znepřístupnit administrační centrum.",
    STOP_AC_CLUSTER_MESSAGE: "Chcete tento klastr zastavit?",

    INVALID_URL: "Stránka neexistuje.",
    INVALID_APPLICATION: "Aplikace {0}, která již v kolektivu neexistuje.", // application name
    INVALID_SERVER: "Server {0}, který již v kolektivu neexistuje.", // server name
    INVALID_CLUSTER: "Klastr {0}, který již v kolektivu neexistuje.", // cluster name
    INVALID_HOST: "Hostitel {0}, který již v kolektivu neexistuje.", // host name
    INVALID_RUNTIME: "Běhové prostředí {0}, které již v kolektivu neexistuje.", // runtime name
    INVALID_INSTANCE: "Instance aplikace {0}, která již v kolektivu neexistuje.", // application instance name
    GO_TO_DASHBOARD: "Přejít na panel dashboard",
    VIEWED_RESOURCE_REMOVED: "Problém! Prostředek byl odebrán nebo již není dostupný.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "Připojení k serveru bylo přerušeno. Stránka nebude již zobrazovat dynamické změny v prostředí. Aktualizací stránky obnovte připojení a dynamické aktualizace.",
    ERROR_MESSAGE: "Připojení bylo přerušeno",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Zastavit server',

    // Tags
    RELATED_RESOURCES: "Související prostředky",
    TAGS : "Značky",
    TAG_BUTTON_LABEL : "Značka {0}",  // tag value
    TAGS_LABEL : "Zadejte značky oddělené čárkou, mezerou, zalomením řádku (klávesou Enter) nebo tabulátorem.",
    OWNER : "Vlastník",
    OWNER_BUTTON_LABEL : "Vlastník {0}",  // owner value
    CONTACTS : "Kontakty",
    CONTACT_BUTTON_LABEL : "Kontakt {0}",  // contact value
    PORTS : "Porty",
    CONTEXT_ROOT : "Kontextový kořenový adresář",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Více",  // alt text for the ... button
    MORE_BUTTON_MENU : "Více nabídek: {0}", // alt text for the menu
    NOTES: "Poznámky",
    NOTE_LABEL : "Poznámka {0}",  // note value
    SET_ATTRIBUTES: "Značky a metadata",
    SETATTR_RUNTIME_NAME: "{0} pro {1}",  // runtime, host
    SAVE: "Uložit",
    TAGINVALIDCHARS: "Znaky '/', '<' a '>' nejsou platné.",
    ERROR_GET_TAGS_METADATA: "Produkt nemůže získat aktuální značky a metadata pro prostředek.",
    ERROR_SET_TAGS_METADATA: "Chyba zabránila produktu v nastavení značek a metadat.",
    METADATA_WILLBE_INHERITED: "Metadata jsou nastavena na aplikaci a sdílena přes všechny instance v klastru.",
    ERROR_ALT: "Chyba",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Pro tento server není k dispozici aktuální statistika, protože je zastaven. Chcete-li tento server začít monitorovat, spusťte jej.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Pro tuto aplikaci není k dispozici aktuální statistika, protože je zastaven přidružený server. Chcete-li tuto aplikaci začít monitorovat, spusťte server.",
    GRAPH_FEATURES_NOT_CONFIGURED: "Tady zatím nic není! Chcete-li tento prostředek monitorovat, vyberte ikonu Upravit a přidejte metriky.",
    NO_GRAPHS_AVAILABLE: "Nejsou k dispozici žádné metriky, které by bylo možné přidat. Zkuste nainstalovat další monitorovací funkce, které zpřístupní další metriky.",
    NO_APPS_GRAPHS_AVAILABLE: "Nejsou k dispozici žádné metriky, které by bylo možné přidat. Zkuste nainstalovat další monitorovací funkce, které zpřístupní další metriky. Rovněž se ujistěte, že se používá aplikace.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Neuložené změny",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "Máte neuložené změny. Když se přesunete na jinou stránku, provedené změny se ztratí.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Chcete uložit změny?",

    NO_CPU_STATS_AVAILABLE : "Pro tento server není k dispozici statistika využití procesoru.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "Chcete-li povolit tento pohled, nainstalujte nástroj pro konfiguraci serveru.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Uložit změny {0} před zavřením?",
    SAVE: "Uložit",
    DONT_SAVE: "Neukládat",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Povolit režim údržby",
    DISABLE_MAINTENANCE_MODE: "Zakázat režim údržby",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Povolit režim údržby",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Zakázat režim údržby",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Povolit režim údržby na hostiteli a všech jeho serverech ({0} serverů)",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Povolit režim údržby na hostitelích a všech jejich serverech ({0} serverů)",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Povolit režim údržby na serveru",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Povolit režim údržby na serverech",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Zakázat režim údržby na hostiteli a všech jeho serverech ({0} serverů)",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Zakázat režim údržby na serveru",
    BREAK_AFFINITY_LABEL: "Přerušit afinitu s aktivními relacemi",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Povolit",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Zakázat",
    MAINTENANCE_MODE: "Režim údržby",
    ENABLING_MAINTENANCE_MODE: "Povolení režimu údržby",
    MAINTENANCE_MODE_ENABLED: "Režim údržby povolen",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "Režim údržby nebyl zapnut, protože nebyly spuštěny alternativní servery.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Chcete-li režim údržby zapnout i bez spuštění alternativních serverů, vyberte volbu Vynutit. Vynucení může narušit zásady automatického škálování.",
    MAINTENANCE_MODE_FAILED: "Nelze zapnout režim údržby.",
    MAINTENANCE_MODE_FORCE_LABEL: "Vynutit",
    MAINTENANCE_MODE_CANCEL_LABEL: "Storno",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0} serverů se v současnosti nachází v režimu údržby.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Povolení režimu údržby na všech hostitelských serverech.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Povolení režimu údržby na všech hostitelských serverech. Zobrazte pohled Servery pro stav.",

    SERVER_API_DOCMENTATION: "Zobrazit definici rozhraní API serveru",

    // objectView title
    TITLE_FOR_CLUSTER: "Klastr {0}", // cluster name
    TITLE_FOR_HOST: "Hostitel {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Kolektivní řadič",
    LIBERTY_SERVER : "Server Liberty",
    NODEJS_SERVER : "Server Node.js",
    CONTAINER : "Kontejner",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Server Liberty v kontejneru Docker",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Server Node.js v kontejneru Docker",
    RUNTIME_LIBERTY : "Běhové prostředí Liberty",
    RUNTIME_NODEJS : "Běhové prostředí Node.js",
    RUNTIME_DOCKER : "Běhové prostředí v kontejneru Docker"

});
