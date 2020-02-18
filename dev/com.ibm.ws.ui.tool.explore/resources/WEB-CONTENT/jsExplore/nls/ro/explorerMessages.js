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
    EXPLORE : "Explorare",
    DASHBOARD : "Tablou de bord",
    DASHBOARD_VIEW_ALL_APPS : "Vizualizare toate aplicaţiile",
    DASHBOARD_VIEW_ALL_SERVERS : "Vizualizare toate serverele",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Vizualizare toate cluster-ele",
    DASHBOARD_VIEW_ALL_HOSTS : "Vizualizare toate gazdele",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Vizualizare toate runtime-urile",
    SEARCH : "Căutare",
    SEARCH_RECENT : "Căutări recente",
    SEARCH_RESOURCES : "Căutare resurse",
    SEARCH_RESULTS : "Căutare rezultate",
    SEARCH_NO_RESULTS : "Niciun rezultat",
    SEARCH_NO_MATCHES : "Nicio potrivire",
    SEARCH_TEXT_INVALID : "Textul de căutare include caractere invalide",
    SEARCH_CRITERIA_INVALID : "Criteriile de căutare nu sunt valide.",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} nu este valid când este specificat cu {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "Specificaţi {0} numai o dată.",
    SEARCH_TEXT_MISSING : "Textul de căutare este necesar",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "Nu este suportată căutarea tagurilor de aplicaţii pe un server.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "Nu este suportată căutarea tagurilor de aplicaţii pe un cluster.",
    SEARCH_UNSUPPORT : "Criteriile de căutare nu sunt suportate.",
    SEARCH_SWITCH_VIEW : "Comutare vizualizare",
    FILTERS : "Filtre",
    DEPLOY_SERVER_PACKAGE : "Implementare pachet servere",
    MEMBER_OF : "Membru al",
    N_CLUSTERS: "{0} cluster-e ...",

    INSTANCE : "Instanţă",
    INSTANCES : "Instanţe",
    APPLICATION : "Aplicaţie",
    APPLICATIONS : "Aplicaţii",
    SERVER : "Server",
    SERVERS : "Servere",
    CLUSTER : "Cluster",
    CLUSTERS : "Cluster-e",
    CLUSTER_NAME : "Nume cluster: ",
    CLUSTER_STATUS : "Stare cluster: ",
    APPLICATION_NAME : "Nume aplicaţie: ",
    APPLICATION_STATE : "Stare aplicaţie: ",
    HOST : "Gazdă",
    HOSTS : "Gazde",
    RUNTIME : "Runtime",
    RUNTIMES : "Runtime-uri",
    PATH : "Cale",
    CONTROLLER : "Controler",
    CONTROLLERS : "Controlere",
    OVERVIEW : "Privire generală",
    CONFIGURE : "Configurare",

    SEARCH_RESOURCE_TYPE: "Tip", // Search by resource types
    SEARCH_RESOURCE_STATE: "Stat", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Toate", // Search all resource types
    SEARCH_RESOURCE_NAME: "Nume", // Search by resource name
    SEARCH_RESOURCE_TAG: "Tag", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Container", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Fără", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Tip de runtime", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Proprietar", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Contact", // Search by contact
    SEARCH_RESOURCE_NOTE: "Notă", // Search by note

    GRID_HEADER_USERDIR : "Director de utilizator",
    GRID_HEADER_NAME : "Nume",
    GRID_LOCATION_NAME : "Locaţie",
    GRID_ACTIONS : "Acţiuni grilă",
    GRID_ACTIONS_LABEL : "{0} acţiuni grilă",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} pe {1} ({2})", // server on host (/path)

    STATS : "Monitor",
    STATS_ALL : "Toate",
    STATS_VALUE : "Valoare: {0}",
    CONNECTION_IN_USE_STATS : "{0} în uz = {1} gestionate - {2} libere",
    CONNECTION_IN_USE_STATS_VALUE : "Valoare: {0} în uz = {1} gestionate - {2} libere",
    DATA_SOURCE : "Susă de date: {0}",
    STATS_DISPLAY_LEGEND : "Afişare legendă",
    STATS_HIDE_LEGEND : "Ascundere legendă",
    STATS_VIEW_DATA : "Vizualizare diagramă de date",
    STATS_VIEW_DATA_TIMESTAMP : "Amprentă de timp",
    STATS_ACTION_MENU : "Meniu acţiune {0}",
    STATS_SHOW_HIDE : "Adăugare indici de măsurare resurse",
    STATS_SHOW_HIDE_SUMMARY : "Adăugare indici de măsurare pentru Sumar",
    STATS_SHOW_HIDE_TRAFFIC : "Adăugare indici de măsurare pentru Trafic",
    STATS_SHOW_HIDE_PERFORMANCE : "Adăugare indici de măsurare pentru Performanţă",
    STATS_SHOW_HIDE_AVAILABILITY : "Adăugare indici de măsurare pentru Disponibilitate",
    STATS_SHOW_HIDE_ALERT : "Adăugare indici de măsurare pentru Alertă",
    STATS_SHOW_HIDE_LIST_BUTTON : "Afişare sau ascundere listă de indici de măsurare resurse",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Editare diagrame",
    STATS_SHOW_HIDE_CONFIRM : "Salvare",
    STATS_SHOW_HIDE_CANCEL : "Anulare",
    STATS_SHOW_HIDE_DONE : "Gata",
    STATS_DELETE_GRAPH : "Ştergere diagramă",
    STATS_ADD_CHART_LABEL : "Adăugare diagramă la vizualizare",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Adăugarea tuturor diagramelor JVM la vizualizare",
    STATS_HEAP_TITLE : "Memorie Heap utilizată",
    STATS_HEAP_USED : "Utilizată: {0} MB",
    STATS_HEAP_COMMITTED : "Comisă: {0} MB",
    STATS_HEAP_MAX : "Max: {0} MB",
    STATS_HEAP_X_TIME : "Timp",
    STATS_HEAP_Y_MB : "MB utilizaţi",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "Clase încărcate",
    STATS_CLASSES_LOADED : "Încărcate: {0}",
    STATS_CLASSES_UNLOADED : "Descărcate: {0}",
    STATS_CLASSES_TOTAL : "Total: {0}",
    STATS_CLASSES_Y_TOTAL : "Clase încărcate",
    STATS_PROCESSCPU_TITLE : "Utilizare CPU",
    STATS_PROCESSCPU_USAGE : "Utilizare CPU: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "Procentaj CPU",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Fire de execuţie JVM active",
    STATS_LIVE_MSG_INIT : "Afişare date live",
    STATS_LIVE_MSG :"Această diagramă nu are date istorice. Ea va continua să arate ultimele 10 minute de date. ",
    STATS_THREADS_ACTIVE : "Live: {0}",
    STATS_THREADS_PEAK : "De vârf: {0}",
    STATS_THREADS_TOTAL : "Total: {0}",
    STATS_THREADS_Y_THREADS : "Fire de execuţie",
    STATS_TP_POOL_SIZE : "Dimensiune pool",
    STATS_JAXWS_TITLE : "Servicii web JAX-WS",
    STATS_JAXWS_BUTTON_LABEL : "Adăugarea tuturor diagramelor de servicii web JAX-WS la vizualizare",
    STATS_JW_AVG_RESP_TIME : "Timp mediu de răspuns",
    STATS_JW_AVG_INVCOUNT : "Număr mediu de invocări",
    STATS_JW_TOTAL_FAULTS : "Total erori de runtime",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Selectare resurse...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} resurse",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 resursă",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "Trebuie să selectaţi cel puţin o resursă.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "Nu este disponibilă nicio dată pentru intervalul de timp selectat.",
    STATS_ACCESS_LOG_TITLE : "Istoric de acces",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Adăugarea tuturor diagramelor de Istoric de acces la vizualizare",
    STATS_ACCESS_LOG_GRAPH : "Numărul de mesaje din istoricul de acces",
    STATS_ACCESS_LOG_SUMMARY : "Rezumat istoric de acces",
    STATS_ACCESS_LOG_TABLE : "Listă de mesaje istoric de acces",
    STATS_MESSAGES_TITLE : "Mesaje şi urmărire",
    STATS_MESSAGES_BUTTON_LABEL : "Adăugarea tuturor diagramelor de Mesaje şi urmărire la vizualizare",
    STATS_MESSAGES_GRAPH : "Numărul de mesaje din istoric",
    STATS_MESSAGES_TABLE : "Lista de mesaje din istoric",
    STATS_FFDC_GRAPH : "Număr FFDC",
    STATS_FFDC_TABLE : "Listă FFDC",
    STATS_TRACE_LOG_GRAPH : "Număr mesaje de urmărire",
    STATS_TRACE_LOG_TABLE : "Listă de mesaje de urmărire",
    STATS_THREAD_POOL_TITLE : "Pool fire de execuţie",
    STATS_THREAD_POOL_BUTTON_LABEL : "Adăugarea tuturor diagramelor Pool fire de execuţie la vizualizare",
    STATS_THREADPOOL_TITLE : "Fire de execuţie Liberty active",
    STATS_THREADPOOL_SIZE : "Dimensiune pool {0} :",
    STATS_THREADPOOL_ACTIVE : "Active: {0}",
    STATS_THREADPOOL_TOTAL : "Total: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Fire de execuţie active",
    STATS_SESSION_MGMT_TITLE : "Sesiuni",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Adăugarea tuturor diagramelor de sesiuni la vizualizare",
    STATS_SESSION_CONFIG_LABEL : "Selectare sesiuni...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} sesiuni",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 sesiune",
    STATS_SESSION_CONFIG_SELECT_ONE : "Trebuie să selectaţi cel puţin o sesiune.",
    STATS_SESSION_TITLE : "Sesiuni active",
    STATS_SESSION_Y_ACTIVE : "Sesiuni active",
    STATS_SESSION_LIVE_LABEL : "Număr de sesiuni live: {0}",
    STATS_SESSION_CREATE_LABEL : "Creare număr: {0}",
    STATS_SESSION_INV_LABEL : "Număr invalidat: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Număr invalidat după timeout: {0}",
    STATS_WEBCONTAINER_TITLE : "Aplicaţii Web",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Adăugarea tuturor diagramelor de Aplicaţii Web la vizualizare",
    STATS_SERVLET_CONFIG_LABEL : "Selectare servleturi...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} servleturi",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "Trebuie să selectaţi cel puţin un servlet.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Număr de răspunsuri",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Număr de răspunsuri",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Număr de răspunsuri",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Număr de răspunsuri",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Timp de răspuns mediu (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Timp de răspuns (ns)",
    STATS_CONN_POOL_TITLE : "Pool de conexiuni",
    STATS_CONN_POOL_BUTTON_LABEL : "Adăugarea tuturor diagramelor Pool de conexiuni la vizualizare",
    STATS_CONN_POOL_CONFIG_LABEL : "Selectare surse de date...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} surse de date",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 sursă de date",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "Trebuie să selectaţi cel puţin o sursă de date.",
    STATS_CONNECT_IN_USE_TITLE : "Conexiuni în uz",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Conexiuni",
    STATS_CONNECT_IN_USE_LABEL : "În uz: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Utilizate: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Libere: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Create: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Distruse: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Timp de aşteptare mediu (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Timp de aşteptare (ms)",
    STATS_TIME_ALL : "Toate",
    STATS_TIME_1YEAR : "1a",
    STATS_TIME_1MONTH : "1l",
    STATS_TIME_1WEEK : "1s",
    STATS_TIME_1DAY : "1z",
    STATS_TIME_1HOUR : "1o",
    STATS_TIME_10MINUTES : "10m",
    STATS_TIME_5MINUTES : "5m",
    STATS_TIME_1MINUTE : "1m",
    STATS_PERSPECTIVE_SUMMARY : "Sumar",
    STATS_PERSPECTIVE_TRAFFIC : "Trafic",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "Trafic JVM",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Trafic conexiune",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Trafic istoric de acces",
    STATS_PERSPECTIVE_PROBLEM : "Problemă",
    STATS_PERSPECTIVE_PERFORMANCE : "Performanţă",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "Performanţă JVM",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Performanţă conexiune",
    STATS_PERSPECTIVE_ALERT : "Analiză alerte",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Alertă istoric de acces",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Alertă istoric de mesaje şi urmărire",
    STATS_PERSPECTIVE_AVAILABILITY : "Disponibilitate",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Ultimul minut",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "Ultimele 5 minute",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "Ultimele 10 minute",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Ultima oră",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Ultima zi",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Ultima săptămână",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Ultima lună",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Ultimul an",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Ultimele {0}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Ultimele {0}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Ultimele {0}m {1}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Ultimele {0} ore",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Ultimele {0}h {1}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Ultimele {0} zile",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Ultimele {0}d {1}h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Ultimele {0} săptămâni",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Ultimele {0} săptămâni {1} zile",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Ultimele {0} luni",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Ultimele {0} luni {1} zile",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Ultimii {0} ani",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Ultimii {0} ani {1} luni",

    STATS_LIVE_UPDATE_LABEL: "Actualizare live",
    STATS_TIME_SELECTOR_NOW_LABEL: "Acum",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Înregistrare în istoric mesaje",

    AUTOSCALED_APPLICATION : "Aplicaţii auto-scalate",
    AUTOSCALED_SERVER : "Server auto-scalat",
    AUTOSCALED_CLUSTER : "Cluster auto-scalat",
    AUTOSCALED_POLICY : "Politică auto-scalată",
    AUTOSCALED_POLICY_DISABLED : "Politica auto-scalată este dezactivată",
    AUTOSCALED_NOACTIONS : "Acţiunile nu sunt disponibile pentru resursele auto-scalate",

    START : "Pornire",
    START_CLEAN : "Pornire --curăţare",
    STARTING : "În pornire",
    STARTED : "Pornit",
    RUNNING : "În rulare",
    NUM_RUNNING: "{0} în rulare",
    PARTIALLY_STARTED : "Pornit parţial",
    PARTIALLY_RUNNING : "Rulare parţială",
    NOT_STARTED : "Nepornit",
    STOP : "Oprire",
    STOPPING : "În oprire",
    STOPPED : "Oprit",
    NUM_STOPPED : "{0} oprite",
    NOT_RUNNING : "Nu rulează",
    RESTART : "Repornire",
    RESTARTING : "Repornire",
    RESTARTED : "Repornit",
    ALERT : "Alertă",
    ALERTS : "Alerte",
    UNKNOWN : "Necunoscut",
    NUM_UNKNOWN : "{0} necunoscute",
    SELECT : "Selectare",
    SELECTED : "Selectat",
    SELECT_ALL : "Selectare toate",
    SELECT_NONE : "Selectare niciuna",
    DESELECT: "Deselectare",
    DESELECT_ALL : "Deselectare toate",
    TOTAL : "Total",
    UTILIZATION : "Peste {0}% utilizare", // percent

    ELLIPSIS_ARIA: "Expandare pentru mai multe opţiuni",
    EXPAND : "Expandare",
    COLLAPSE: "Restrângere",

    ALL : "Toate",
    ALL_APPS : "Toate aplicaţiile",
    ALL_SERVERS : "Toate serverele",
    ALL_CLUSTERS : "Toate cluster-ele",
    ALL_HOSTS : "Toate gazdele",
    ALL_APP_INSTANCES : "Toate instanţele de aplicaţii",
    ALL_RUNTIMES : "Toate runtime-urile",

    ALL_APPS_RUNNING : "Toate aplicaţiile care rulează",
    ALL_SERVER_RUNNING : "Toate serverele care rulează",
    ALL_CLUSTERS_RUNNING : "Toate cluster-ele care rulează",
    ALL_APPS_STOPPED : "Toate aplicaţiile oprite",
    ALL_SERVER_STOPPED : "Toate serverele oprite",
    ALL_CLUSTERS_STOPPED : "Toate cluster-ele oprite",
    ALL_SERVERS_UNKNOWN : "Toate serverele necunoscute",
    SOME_APPS_RUNNING : "Unele aplicaţii care rulează",
    SOME_SERVERS_RUNNING : "Unele servere care rulează",
    SOME_CLUSTERS_RUNNING : "Unele cluster-e care rulează",
    NO_APPS_RUNNING : "Nicio aplicaţie care rulează",
    NO_SERVERS_RUNNING : "Niciun server care rulează",
    NO_CLUSTERS_RUNNING : "Niciun cluster care rulează",

    HOST_WITH_ALL_SERVERS_RUNNING: "Gazde cu toate serverele în rulare", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Gazde cu câteva servere în rulare",
    HOST_WITH_NO_SERVERS_RUNNING: "Gazde cu niciun server în rulare", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Gazde cu toate serverele oprite",
    HOST_WITH_SERVERS_RUNNING: "Gazde cu servere în rulare",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Runtime-uri cu unele servere în rulare",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Runtime-uri cu toate serverele oprite",
    RUNTIME_WITH_SERVERS_RUNNING: "Runtime-uri cu servere în rulare",

    START_ALL_APPS : "Pornesc toate aplicaţiile?",
    START_ALL_INSTANCES : "Pornesc toate instanţele de aplicaţii?",
    START_ALL_SERVERS : "Pornesc toate serverele?",
    START_ALL_CLUSTERS : "Pornesc toate cluster-ele?",
    STOP_ALL_APPS : "Se opresc toate aplicaţiile?",
    STOPE_ALL_INSTANCES : "Se opresc toate instanţele de aplicaţii?",
    STOP_ALL_SERVERS : "Se opresc toate serverele?",
    STOP_ALL_CLUSTERS : "Se opresc toate cluster-ele?",
    RESTART_ALL_APPS : "Repornesc toate aplicaţiile?",
    RESTART_ALL_INSTANCES : "Repornesc toate instanţele de aplicaţii?",
    RESTART_ALL_SERVERS : "Repornesc toate serverele?",
    RESTART_ALL_CLUSTERS : "Repornesc toate cluster-ele?",

    START_INSTANCE : "Porneşte instanţa de aplicaţie?",
    STOP_INSTANCE : "Se opreşte instanţa de aplicaţie?",
    RESTART_INSTANCE : "Reporneşte instanţa de aplicaţie?",

    START_SERVER : "Porneşte serverul {0}?",
    STOP_SERVER : "Se opreşte serverul {0}?",
    RESTART_SERVER : "Reporneşte serverul {0}?",

    START_ALL_INSTS_OF_APP : "Se opresc toate instanţele de {0}?", // application name
    START_APP_ON_SERVER : "Porneşte {0} pe {1}?", // app name, server name
    START_ALL_APPS_WITHIN : "Pornesc toate aplicaţiile în {0}?", // resource
    START_ALL_APP_INSTS_WITHIN : "Pornesc toate instanţele de aplicaţii în {0}?", // resource
    START_ALL_SERVERS_WITHIN : "Pornesc toate serverele în {0}?", // resource
    STOP_ALL_INSTS_OF_APP : "Se opresc toate instanţele de {0}?", // application name
    STOP_APP_ON_SERVER : "Se opreşte {0} pe {1}?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Se opresc toate aplicaţiile în {0}?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Se opresc toate instanţele de aplicaţii în {0}?", // resource
    STOP_ALL_SERVERS_WITHIN : "Se opresc toate serverele în {0}?", // resource
    RESTART_ALL_INSTS_OF_APP : "Repornesc toate instanţele de {0}?", // application name
    RESTART_APP_ON_SERVER : "Reporneşte {0} pe {1}?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Repornesc toate aplicaţiile în {0}?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Repornesc toate instanţele de aplicaţii în {0}?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Repornesc toate serverele în rulare în {0}?", // resource

    START_SELECTED_APPS : "Pornesc toate instanţele aplicaţiilor selectate?",
    START_SELECTED_INSTANCES : "Pornesc instanţele de aplicaţii selectate?",
    START_SELECTED_SERVERS : "Pornesc serverele selectate?",
    START_SELECTED_SERVERS_LABEL : "Pornesc serverele selectate",
    START_SELECTED_CLUSTERS : "Pornesc cluster-ele selectate?",
    START_CLEAN_SELECTED_SERVERS : "Pornire --curăţare servere selectate?",
    START_CLEAN_SELECTED_CLUSTERS : "Pornire --curăţare cluster-e selectate?",
    STOP_SELECTED_APPS : "Se opresc toate instanţele aplicaţiilor selectate?",
    STOP_SELECTED_INSTANCES : "Se opresc instanţele de aplicaţii selectate?",
    STOP_SELECTED_SERVERS : "Se opresc serverele selectate?",
    STOP_SELECTED_CLUSTERS : "Se opresc cluster-ele selectate?",
    RESTART_SELECTED_APPS : "Repornesc toate instanţele aplicaţiilor selectate?",
    RESTART_SELECTED_INSTANCES : "Repornesc instanţele de aplicaţii selectate?",
    RESTART_SELECTED_SERVERS : "Repornesc serverele selectate?",
    RESTART_SELECTED_CLUSTERS : "Repornesc cluster-ele selectate?",

    START_SERVERS_ON_HOSTS : "Pornesc toate serverele pe gazdele selectate?",
    STOP_SERVERS_ON_HOSTS : "Se opresc toate serverele pe gazdele selectate?",
    RESTART_SERVERS_ON_HOSTS : "Repornesc toate serverele în rulare pe gazdele selectate?",

    SELECT_APPS_TO_START : "Selectaţi aplicaţiile oprite pentru pornire.",
    SELECT_APPS_TO_STOP : "Selectaţi aplicaţiile pornite pentru oprire.",
    SELECT_APPS_TO_RESTART : "Selectaţi aplicaţiile pornite pentru repornire.",
    SELECT_INSTANCES_TO_START : "Selectaţi instanţele de aplicaţii oprite pentru pornire.",
    SELECT_INSTANCES_TO_STOP : "Selectaţi instanţele de aplicaţii pornite pentru oprire.",
    SELECT_INSTANCES_TO_RESTART : "Selectaţi instanţele de aplicaţii pornite pentru repornire.",
    SELECT_SERVERS_TO_START : "Selectaţi serverele oprite pentru pornire.",
    SELECT_SERVERS_TO_STOP : "Selectaţi serverele pornite pentru oprire.",
    SELECT_SERVERS_TO_RESTART : "Selectaţi serverele pornite pentru repornire.",
    SELECT_CLUSTERS_TO_START : "Selectaţi cluster-ele oprite pentru pornire.",
    SELECT_CLUSTERS_TO_STOP : "Selectaţi cluster-ele pornite pentru oprire.",
    SELECT_CLUSTERS_TO_RESTART : "Selectaţi cluster-ele pornite pentru repornire.",

    STATUS : "Stare",
    STATE : "Stare:",
    NAME : "Nume:",
    DIRECTORY : "Director",
    INFORMATION : "Informaţii",
    DETAILS : "Detalii",
    ACTIONS : "Acţiuni",
    CLOSE : "Închidere",
    HIDE : "Ascundere",
    SHOW_ACTIONS : "Afişare acţiuni",
    SHOW_SERVER_ACTIONS_LABEL : "{0} acţiuni server",
    SHOW_APP_ACTIONS_LABEL : "{0} acţiuni aplicaţie",
    SHOW_CLUSTER_ACTIONS_LABEL : "{0} acţiuni cluster",
    SHOW_HOST_ACTIONS_LABEL : "{0} acţiuni gazdă",
    SHOW_RUNTIME_ACTIONS_LABEL : "{0} acţiuni runtime",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "Meniu acţiuni server {0}",
    SHOW_APP_ACTIONS_MENU_LABEL : "Meniu acţiuni aplicaţie {0}",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "Meniu acţiuni cluster {0}",
    SHOW_HOST_ACTIONS_MENU_LABEL : "Meniu acţiuni gazdă {0}",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "Meniu acţiuni runtime {0}",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "Meniu acţiuni runtime pe gazda {0}",
    SHOW_COLLECTION_MENU_LABEL : "Meniu acţiuni stare colecţie {0}",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "Meniu acţiuni stare căutare {0}",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: stare necunoscută", // resourceName
    UNKNOWN_STATE_APPS : "{0} aplicaţii într-o stare necunoscută", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} instanţe de aplicaţii într-o stare necunoscută", // quantity
    UNKNOWN_STATE_SERVERS : "{0} servere într-o stare necunoscută", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} cluster-e într-o stare necunoscută", // quantity

    INSTANCES_NOT_RUNNING : "{0} instanţe de aplicaţii nu rulează", // quantity
    APPS_NOT_RUNNING : "{0} aplicaţii nu rulează", // quantity
    SERVERS_NOT_RUNNING : "{0} servere nu rulează", // quantity
    CLUSTERS_NOT_RUNNING : "{0} cluster-e nu rulează", // quantity

    APP_STOPPED_ON_SERVER : "{0} oprite pe servere în rulare: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} aplicaţii oprite pe servere în rulare: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} aplicaţii oprite pe servere în rulare.", // quantity
    NUMBER_RESOURCES : "{0} resurse", // quantity
    NUMBER_APPS : "{0} aplicaţii", // quantity
    NUMBER_SERVERS : "{0} servere", // quantity
    NUMBER_CLUSTERS : "{0} cluster-e", // quantity
    NUMBER_HOSTS : "{0} gazde", // quantity
    NUMBER_RUNTIMES : "{0} runtime-uri", // quantity
    SERVERS_INSERT : "servere",
    INSERT_STOPPED_ON_INSERT : "{0} oprite pe {1} în rulare.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} oprit pe serverul în rulare {1}", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "{0} de pe cluster-ul {1} oprit pe serverele în rulare: {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} instanţe de aplicaţii oprite pe servere în rulare.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: instanţa de aplicaţii nu rulează", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: nu toate aplicaţiile rulează", // serverName[]
    NO_APPS_RUNNING : "{0}: nicio aplicaţie nu rulează", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} servere nu cu toate aplicaţiile care rulează", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} servere cu nicio aplicaţie care rulează", // quantity

    COUNT_OF_APPS_SELECTED : "{0} aplicaţii selectate",
    RATIO_RUNNING : "{0} rulează", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0} selectate",

    NO_HOSTS_SELECTED : "Nu este selectat nicio gazdă",
    NO_DEPLOY_RESOURCE : "Nicio resursă pentru implementarea instalării",
    NO_TOPOLOGY : "Nu există niciun {0}.",
    COUNT_OF_APPS_STARTED  : "{0} aplicaţii pornite",

    APPS_LIST : "{0} aplicaţii",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} instanţe care rulează",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} servere care rulează",
    RESOURCE_ON_RESOURCE : "{0} pe {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} pe serverul {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} pe cluster-ul {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Repornirea este dezactivată pentru acest server deoarece este gazda Admin Center",
    ACTION_DISABLED_FOR_USER: "Acţiunile sunt dezactivate pe această resursă deoarece utilizatorul nu este autorizat",

    RESTART_AC_TITLE: "Nicio repornire pentru Admin Center",
    RESTART_AC_DESCRIPTION: "{0} asigură Admin Center. Admin Center nu se poate reporni singur.",
    RESTART_AC_MESSAGE: "Toate celelalte servere selectate vor fi repornite.",
    RESTART_AC_CLUSTER_MESSAGE: "Toate celelalte cluster-e selectate vor fi repornite.",

    STOP_AC_TITLE: "Oprire Admin Center",
    STOP_AC_DESCRIPTION: "Serverul {0} este un controler colectiv care rulează Admin Center. Oprirea lui ar avea impact asupra operaţiilor de gestionare colectivă Liberty şi ar face indisponibil Admin Center.",
    STOP_AC_MESSAGE: "Doriţi să opriţi acest controler?",
    STOP_STANDALONE_DESCRIPTION: "Serverul {0} rulează Admin Center. Oprirea lui va face Admin Center indisponibil.",
    STOP_STANDALONE_MESSAGE: "Doriţi să opriţi acest server?",

    STOP_CONTROLLER_TITLE: "Oprire controler",
    STOP_CONTROLLER_DESCRIPTION: "Serverul {0} este un controler colectiv. Oprirea ar putea avea impact asupra operaţiilor colective Liberty.",
    STOP_CONTROLLER_MESSAGE: "Doriţi să opriţi acest controler?",

    STOP_AC_CLUSTER_TITLE: "Opriţi cluster-ul {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "Cluster-ul {0} nu conţine un controler colectiv care rulează Admin Center.  Oprirea lui ar putea afecta operaţiile Liberty de gestionare colectivă şi pot face Admin Center indisponibil.",
    STOP_AC_CLUSTER_MESSAGE: "Doriţi să opriţi acest cluster?",

    INVALID_URL: "Pagina nu există.",
    INVALID_APPLICATION: "Aplicaţia {0} nu mai există în colectiv.", // application name
    INVALID_SERVER: "Serverul {0} nu mai există în colectiv.", // server name
    INVALID_CLUSTER: "Cluster-ul {0} nu mai există în colectiv.", // cluster name
    INVALID_HOST: "Gazda {0} nu mai există în colectiv.", // host name
    INVALID_RUNTIME: "Runtime-ul {0} nu mai există în colectiv.", // runtime name
    INVALID_INSTANCE: "Instanţa de aplicaţie {0} nu mai există în colectiv.", // application instance name
    GO_TO_DASHBOARD: "Deplasaţi-vă la tabloul de bord",
    VIEWED_RESOURCE_REMOVED: "Au! Resursa a fost înlăturată sau nu mai este disponibilă.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "Conexiunea la server a fost pierdută. Pagina nu va mai afişa modificările dinamice în mediu. Reîmprospătaţi pagina pentru a restaura conexiunea şi actualizările dinamice.",
    ERROR_MESSAGE: "Conexiune întreruptă",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Oprire server',

    // Tags
    RELATED_RESOURCES: "Resurse conexe",
    TAGS : "Taguri",
    TAG_BUTTON_LABEL : "Tagul {0}",  // tag value
    TAGS_LABEL : "Introduceţi taguri separate prin virgulă, spaţiu, Enter sau Tab.",
    OWNER : "Proprietar",
    OWNER_BUTTON_LABEL : "Proprietarul {0}",  // owner value
    CONTACTS : "Contacte",
    CONTACT_BUTTON_LABEL : "Contactul {0}",  // contact value
    PORTS : "Porturi",
    CONTEXT_ROOT : "Rădăcină de context",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Mai multe",  // alt text for the ... button
    MORE_BUTTON_MENU : "Meniu Mai multe {0}", // alt text for the menu
    NOTES: "Notă",
    NOTE_LABEL : "Nota {0}",  // note value
    SET_ATTRIBUTES: "Taguri şi metadate",
    SETATTR_RUNTIME_NAME: "{0} pe {1}",  // runtime, host
    SAVE: "Salvare",
    TAGINVALIDCHARS: "Caracterele '/', '<', şi '>' nu sunt valide.",
    ERROR_GET_TAGS_METADATA: "Produsul nu poate obţine curent taguri şi metadate pentru resursă.",
    ERROR_SET_TAGS_METADATA: "O eroare a împiedicat produsul de la setarea tagurilor şi metadatelor.",
    METADATA_WILLBE_INHERITED: "Metadatele sunt setate pe aplicaţie şi partajate pe toate instanţele din cluster.",
    ERROR_ALT: "Eroare",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Statisticile curente nu sunt disponibile pentru acest server deoarece este oprit. Porniţi serverul pentru a începe monitorizarea.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Statisticile curente nu sunt disponibile pentru această aplicaţie deoarece serverul său asociat este oprit. Porniţi serverul pentru a începe monitorizarea acestei aplicaţii.",
    GRAPH_FEATURES_NOT_CONFIGURED: "Nu există încă nimic aici! Monitorizaţi această resursă selectând pictograma Editare şi adăugând indicii de măsurare.",
    NO_GRAPHS_AVAILABLE: "Nu există indici de măsurare disponibili de adăugat. Încercaţi să instalaţi mai multe caracteristici de monitorizare pentru a face disponibili mai mulţi indici de măsurare. ",
    NO_APPS_GRAPHS_AVAILABLE: "Nu există indici de măsurare disponibili de adăugat. Încercaţi să instalaţi mai multe caracteristici de monitorizare pentru a face disponibili mai mulţi indici de măsurare. De asemenea, asiguraţi-vă că aplicaţia este în uz.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Modificări nesalvate",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "Aveţi modificări nesalvate. Dacă vă mutaţi la altă pagină, veţi pierde modificările.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Doriţi să vă salvaţi modificările?",

    NO_CPU_STATS_AVAILABLE : "Statisticile de utilizare CPU nu sunt disponibile pentru acest server.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "Pentru a activa această vizualizare, instalaţi unealta Server Config.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Salvaţi modificările la {0} înainte de a închide?",
    SAVE: "Salvare",
    DONT_SAVE: "Nu salvaţi",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Activare mod de întreţinere",
    DISABLE_MAINTENANCE_MODE: "Dezactivare mod de întreţinere",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Activare mod de întreţinere",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Dezactivare mod de întreţinere",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Activare mod întreţinere pe gazdă şi pe toate serverele sale ({0} servere)",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Activare mod întreţinere pe gazde şi toate serverele sale ( {0} servere )",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Activare mod de întreţinere pe server",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Activare mod întreţinere pe servere",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Dezactivare mod întreţinere pe gazde şi toate serverele sale ({0} servere)",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Dezactivare mod întreţinere pe server",
    BREAK_AFFINITY_LABEL: "Întrerupere afinitate cu sesiunile active",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Activat",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Dezactivare",
    MAINTENANCE_MODE: "Mod întreţinere",
    ENABLING_MAINTENANCE_MODE: "Activare mod de întreţinere",
    MAINTENANCE_MODE_ENABLED: "Mod de întreţinere activat",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "Modul de întreţinere nu a fost activat deoarece nu au pornit serverele alternative.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Selectaţi Forţare pentru a activa  modul de întreţinere fără pornirea serverelor alternative. Forţarea ar putea întrerupe politicile de auto-scalare.",
    MAINTENANCE_MODE_FAILED: "Modul de întreţinere nu poate fi activat.",
    MAINTENANCE_MODE_FORCE_LABEL: "Forţare",
    MAINTENANCE_MODE_CANCEL_LABEL: "Anulare",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0} servere sunt momentan în modul de întreţinere.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Activarea modului de întreţinere pe toate serverele gazdă.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Activarea modului de întreţinere pe toate serverele gazdă.  Afişarea vizualizării Servere pentru stare.",

    SERVER_API_DOCMENTATION: "Vizualizare definiţie API server",

    // objectView title
    TITLE_FOR_CLUSTER: "Cluster-ul {0}", // cluster name
    TITLE_FOR_HOST: "Gazda {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Controler de colectiv",
    LIBERTY_SERVER : "Server Liberty",
    NODEJS_SERVER : "Server Node.js",
    CONTAINER : "Container",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Server Liberty într-un container Docker",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Server Node.js într-un container Docker",
    RUNTIME_LIBERTY : "Runtime Liberty",
    RUNTIME_NODEJS : "Runtime Node.js",
    RUNTIME_DOCKER : "Runtime într-un container Docker"

});
