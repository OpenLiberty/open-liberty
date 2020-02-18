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
    EXPLORER : "Böngésző",
    EXPLORE : "Böngészés",
    DASHBOARD : "Műszerfal",
    DASHBOARD_VIEW_ALL_APPS : "Összes alkalmazás megtekintése",
    DASHBOARD_VIEW_ALL_SERVERS : "Összes kiszolgáló megtekintése",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Összes fürt megtekintése",
    DASHBOARD_VIEW_ALL_HOSTS : "Összes hoszt megtekintése",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Összes futási környezet megtekintése",
    SEARCH : "Keresés",
    SEARCH_RECENT : "Legutóbbi keresések",
    SEARCH_RESOURCES : "Erőforrások keresése",
    SEARCH_RESULTS : "Keresés eredményei",
    SEARCH_NO_RESULTS : "Nincs eredmény",
    SEARCH_NO_MATCHES : "Nincs egyezés",
    SEARCH_TEXT_INVALID : "A keresési szöveg érvénytelen karaktereket tartalmaz",
    SEARCH_CRITERIA_INVALID : "A keresési feltételek érvénytelenek.",
    SEARCH_CRITERIA_INVALID_COMBO :"A(z) {0} nem érvényes, ha együtt szerepel a következővel: {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "A(z) {0} csak egyszer adható meg.",
    SEARCH_TEXT_MISSING : "A keresési szöveg kötelező",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "Az alkalmazáscímkék keresése a kiszolgálón nem támogatott.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "Az alkalmazáscímkék keresése a fürtön nem támogatott.",
    SEARCH_UNSUPPORT : "A keresési feltételek nem támogatottak.",
    SEARCH_SWITCH_VIEW : "Nézetváltás",
    FILTERS : "Szűrők",
    DEPLOY_SERVER_PACKAGE : "Kiszolgálócsomag telepítése",
    MEMBER_OF : "A következő tagja:",
    N_CLUSTERS: "{0} fürt ...",

    INSTANCE : "Példány",
    INSTANCES : "Példány",
    APPLICATION : "Alkalmazás",
    APPLICATIONS : "Alkalmazás",
    SERVER : "Kiszolgáló",
    SERVERS : "Kiszolgáló",
    CLUSTER : "Fürt",
    CLUSTERS : "Fürt",
    CLUSTER_NAME : "Fürt neve: ",
    CLUSTER_STATUS : "Fürt állapota: ",
    APPLICATION_NAME : "Alkalmazásnév: ",
    APPLICATION_STATE : "Alkalmazás állapota: ",
    HOST : "Hoszt",
    HOSTS : "Hoszt",
    RUNTIME : "Futási",
    RUNTIMES : "Futási környezetek",
    PATH : "Útvonal",
    CONTROLLER : "Vezérlő",
    CONTROLLERS : "Vezérlő",
    OVERVIEW : "Áttekintés",
    CONFIGURE : "Beállítás",

    SEARCH_RESOURCE_TYPE: "Típus", // Search by resource types
    SEARCH_RESOURCE_STATE: "Állapot", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Minden", // Search all resource types
    SEARCH_RESOURCE_NAME: "Név", // Search by resource name
    SEARCH_RESOURCE_TAG: "Címke", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Tároló", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Nincs", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Futási környezet típusa", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Tulajdonos", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Kapcsolattartó", // Search by contact
    SEARCH_RESOURCE_NOTE: "Megjegyzés", // Search by note

    GRID_HEADER_USERDIR : "Felhasználói könyvtár",
    GRID_HEADER_NAME : "Név",
    GRID_LOCATION_NAME : "Hely",
    GRID_ACTIONS : "Rácsműveletek",
    GRID_ACTIONS_LABEL : "{0} rácsműveletek",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} szerver a(z) {1} hoszton ({2})", // server on host (/path)

    STATS : "Megfigyelés",
    STATS_ALL : "Minden",
    STATS_VALUE : "Érték: {0}",
    CONNECTION_IN_USE_STATS : "{0} Használatban = {1} Felügyelt - {2} Szabad",
    CONNECTION_IN_USE_STATS_VALUE : "Érték: {0} Használatban = {1} Felügyelt - {2} Szabad",
    DATA_SOURCE : "Adatforrás: {0}",
    STATS_DISPLAY_LEGEND : "Jelmagyarázat megjelenítése",
    STATS_HIDE_LEGEND : "Jelmagyarázat elrejtése",
    STATS_VIEW_DATA : "Diagramadatok megjelenítése",
    STATS_VIEW_DATA_TIMESTAMP : "Időpecsét",
    STATS_ACTION_MENU : "{0} műveleti menü",
    STATS_SHOW_HIDE : "Erőforrás-mérőszámok hozzáadása",
    STATS_SHOW_HIDE_SUMMARY : "Összegzés mérőszámainak hozzáadása",
    STATS_SHOW_HIDE_TRAFFIC : "Forgalom mérőszámainak hozzáadása",
    STATS_SHOW_HIDE_PERFORMANCE : "Teljesítmény mérőszámainak hozzáadása",
    STATS_SHOW_HIDE_AVAILABILITY : "Rendelkezésre állás mérőszámainak hozzáadása",
    STATS_SHOW_HIDE_ALERT : "Riasztás mérőszámainak hozzáadása",
    STATS_SHOW_HIDE_LIST_BUTTON : "Erőforrás-mérőszámok listájának megjelenítése vagy elrejtése",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Diagramok szerkesztése",
    STATS_SHOW_HIDE_CONFIRM : "Mentés",
    STATS_SHOW_HIDE_CANCEL : "Mégse",
    STATS_SHOW_HIDE_DONE : "Kész",
    STATS_DELETE_GRAPH : "Diagram törlése",
    STATS_ADD_CHART_LABEL : "Diagram hozzáadása a nézethez",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Összes JVM diagram hozzáadása a nézethez",
    STATS_HEAP_TITLE : "Felhasznált kupacmemória",
    STATS_HEAP_USED : "Felhasznált: {0} MB",
    STATS_HEAP_COMMITTED : "Véglegesített: {0} MB",
    STATS_HEAP_MAX : "Maximális: {0} MB",
    STATS_HEAP_X_TIME : "Idő",
    STATS_HEAP_Y_MB : "Felhasznált MB",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "Betöltött osztályok",
    STATS_CLASSES_LOADED : "Betöltve: {0}",
    STATS_CLASSES_UNLOADED : "Kiürítve: {0}",
    STATS_CLASSES_TOTAL : "Összesen: {0}",
    STATS_CLASSES_Y_TOTAL : "Betöltött osztályok",
    STATS_PROCESSCPU_TITLE : "CPU felhasználás",
    STATS_PROCESSCPU_USAGE : "CPU felhasználás: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "CPU százalék",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Aktív JVM szálak",
    STATS_LIVE_MSG_INIT : "Éles adatok megjelenítése",
    STATS_LIVE_MSG :"Ez a diagram nem rendelkezik előzményadatokkal. Továbbra is a legutóbbi 10 perc adatait jeleníti meg.",
    STATS_THREADS_ACTIVE : "Élő: {0}",
    STATS_THREADS_PEAK : "Csúcs: {0}",
    STATS_THREADS_TOTAL : "Összesen: {0}",
    STATS_THREADS_Y_THREADS : "Szálak",
    STATS_TP_POOL_SIZE : "Készlet mérete",
    STATS_JAXWS_TITLE : "JAX-WS webszolgáltatások",
    STATS_JAXWS_BUTTON_LABEL : "JAX-WS webszolgáltatások összes diagramjának hozzáadása a nézethez",
    STATS_JW_AVG_RESP_TIME : "Átlagos válaszidő",
    STATS_JW_AVG_INVCOUNT : "Átlagos meghívásszám",
    STATS_JW_TOTAL_FAULTS : "Futási hibák összesen",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Erőforrások kiválasztása...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} erőforrás",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 erőforrás",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "Legalább egy erőforrást ki kell választania.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "A kiválasztott időtartományban nincsenek elérhető adatok.",
    STATS_ACCESS_LOG_TITLE : "Hozzáférési napló",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Hozzáférési napló összes diagramjának hozzáadása a nézethez",
    STATS_ACCESS_LOG_GRAPH : "Hozzáférési napló üzeneteinek száma",
    STATS_ACCESS_LOG_SUMMARY : "Hozzáférési napló összegzése",
    STATS_ACCESS_LOG_TABLE : "Hozzáférési napló üzeneteinek listája",
    STATS_MESSAGES_TITLE : "Üzenetek és nyomkövetés",
    STATS_MESSAGES_BUTTON_LABEL : "Üzenetek és nyomkövetések összes diagramjának hozzáadása a nézethez",
    STATS_MESSAGES_GRAPH : "Napló üzeneteinek száma",
    STATS_MESSAGES_TABLE : "Napló üzeneteinek listája",
    STATS_FFDC_GRAPH : "FFDC szám",
    STATS_FFDC_TABLE : "FFDC lista",
    STATS_TRACE_LOG_GRAPH : "Nyomkövetési üzenetek száma",
    STATS_TRACE_LOG_TABLE : "Nyomkövetési üzenetek listája",
    STATS_THREAD_POOL_TITLE : "Szálkészlet",
    STATS_THREAD_POOL_BUTTON_LABEL : "Összes szálkészletdiagram hozzáadása a nézethez",
    STATS_THREADPOOL_TITLE : "Aktív Liberty szálak",
    STATS_THREADPOOL_SIZE : "Készletméret: {0}",
    STATS_THREADPOOL_ACTIVE : "Aktív: {0}",
    STATS_THREADPOOL_TOTAL : "Összesen: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Aktív szálak",
    STATS_SESSION_MGMT_TITLE : "Munkamenetek",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Munkamenetek összes diagramjának hozzáadása a nézethez",
    STATS_SESSION_CONFIG_LABEL : "Munkamenetek kiválasztása...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} munkamenet",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 munkamenet",
    STATS_SESSION_CONFIG_SELECT_ONE : "Legalább egy munkamenetet ki kell választania.",
    STATS_SESSION_TITLE : "Aktív munkamenetek",
    STATS_SESSION_Y_ACTIVE : "Aktív munkamenetek",
    STATS_SESSION_LIVE_LABEL : "Élők száma: {0}",
    STATS_SESSION_CREATE_LABEL : "Létrehozottak száma: {0}",
    STATS_SESSION_INV_LABEL : "Érvénytelenítettek száma: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Érvénytelenítettek száma időtúllépés szerint: {0}",
    STATS_WEBCONTAINER_TITLE : "Webalkalmazások",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Webalkalmazások összes diagramjának hozzáadása a nézethez",
    STATS_SERVLET_CONFIG_LABEL : "Kiszolgáló kisalkalmazások kiválasztása...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} kiszolgáló kisalkalmazás",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 kiszolgáló kisalkalmazás",
    STATS_SERVLET_CONFIG_SELECT_ONE : "Legalább egy kiszolgáló kisalkalmazást ki kell választania.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Kérések száma",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Kérések száma",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Válaszok száma",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Válaszok száma",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Átlagos válaszidő (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Válaszidő (ns)",
    STATS_CONN_POOL_TITLE : "Kapcsolattár",
    STATS_CONN_POOL_BUTTON_LABEL : "Összes kapcsolattár-diagram hozzáadása a nézethez",
    STATS_CONN_POOL_CONFIG_LABEL : "Adatforrások kiválasztása...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} adatforrás",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 adatforrás",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "Legalább egy adatforrást ki kell választania.",
    STATS_CONNECT_IN_USE_TITLE : "Használatban lévő kapcsolatok",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Kapcsolatok",
    STATS_CONNECT_IN_USE_LABEL : "Használatban: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Használt: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Szabad: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Létrehozott: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Megsemmisített: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Átlagos várakozási idő (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Várakozási idő (ms)",
    STATS_TIME_ALL : "Minden",
    STATS_TIME_1YEAR : "1é",
    STATS_TIME_1MONTH : "1hó",
    STATS_TIME_1WEEK : "1hé",
    STATS_TIME_1DAY : "1n",
    STATS_TIME_1HOUR : "1ó",
    STATS_TIME_10MINUTES : "10p",
    STATS_TIME_5MINUTES : "5p",
    STATS_TIME_1MINUTE : "1p",
    STATS_PERSPECTIVE_SUMMARY : "Összegzés",
    STATS_PERSPECTIVE_TRAFFIC : "Forgalom",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "JVM forgalom",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Kapcsolat forgalom",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Hozzáférési napló forgalom",
    STATS_PERSPECTIVE_PROBLEM : "Probléma",
    STATS_PERSPECTIVE_PERFORMANCE : "Teljesítmény",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "JVM teljesítmény",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Kapcsolat teljesítmény",
    STATS_PERSPECTIVE_ALERT : "Riasztáselemzés",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Hozzáférési napló riasztás",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Üzenet- és nyomkövetésnapló riasztás",
    STATS_PERSPECTIVE_AVAILABILITY : "Rendelkezésre állás",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Utolsó perc",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "Utolsó 5 perc",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "Utolsó 10 perc",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Elmúlt óra",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Előző nap",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Múlt hét",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Múlt hónap",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Múlt év",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Utolsó {0} mp",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Utolsó {0} p",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Utolsó {0} p {1} mp",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Utolsó {0} ó",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Utolsó {0} ó {1} p",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Utolsó {0} n",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Utolsó {0} n {1} ó",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Utolsó {0} hé",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Utolsó {0} hé {1} n",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Utolsó {0} hó",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Utolsó {0} hó {1} n",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Utolsó {0} év",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Utolsó {0} év {1} hó",

    STATS_LIVE_UPDATE_LABEL: "Élő frissítés",
    STATS_TIME_SELECTOR_NOW_LABEL: "Most",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Naplóüzenetek",

    AUTOSCALED_APPLICATION : "Automatikusan méretezett alkalmazás",
    AUTOSCALED_SERVER : "Automatikusan méretezett kiszolgáló",
    AUTOSCALED_CLUSTER : "Automatikusan méretezett fürt",
    AUTOSCALED_POLICY : "Automatikus méretezés irányelve",
    AUTOSCALED_POLICY_DISABLED : "Automatikus méretezés irányelve tiltott",
    AUTOSCALED_NOACTIONS : "Nem állnak rendelkezésre műveletek az automatikusan méretezett erőforrásokhoz",

    START : "Indítás",
    START_CLEAN : "Indítás --clean",
    STARTING : "Indítás",
    STARTED : "Elindítva",
    RUNNING : "Fut",
    NUM_RUNNING: "{0} fut",
    PARTIALLY_STARTED : "Részlegesen elindítva",
    PARTIALLY_RUNNING : "Részlegesen fut",
    NOT_STARTED : "Nincs elindítva",
    STOP : "Leállítás",
    STOPPING : "Leállítás",
    STOPPED : "Leállítva",
    NUM_STOPPED : "{0} leállítva",
    NOT_RUNNING : "Nem fut",
    RESTART : "Újraindítás",
    RESTARTING : "Újraindítás",
    RESTARTED : "Újraindítva",
    ALERT : "Riasztás",
    ALERTS : "Riasztások",
    UNKNOWN : "Ismeretlen",
    NUM_UNKNOWN : "{0} ismeretlen",
    SELECT : "Kiválasztás",
    SELECTED : "Kiválasztott",
    SELECT_ALL : "Összes kiválasztása",
    SELECT_NONE : "Nincs kijelölés",
    DESELECT: "Kijelölés megszüntetése",
    DESELECT_ALL : "Kijelölések megszüntetése",
    TOTAL : "Összesen",
    UTILIZATION : "Több, mint {0}% kihasználtság", // percent

    ELLIPSIS_ARIA: "További beállítások kibontása",
    EXPAND : "Kibontás",
    COLLAPSE: "Összehúzás",

    ALL : "Minden",
    ALL_APPS : "Minden alkalmazás",
    ALL_SERVERS : "Minden kiszolgáló",
    ALL_CLUSTERS : "Minden fürt",
    ALL_HOSTS : "Minden hoszt",
    ALL_APP_INSTANCES : "Minden alkalmazáspéldány",
    ALL_RUNTIMES : "Minden futási környezet",

    ALL_APPS_RUNNING : "Minden alkalmazás fut",
    ALL_SERVER_RUNNING : "Minden kiszolgáló fut",
    ALL_CLUSTERS_RUNNING : "Minden fürt fut",
    ALL_APPS_STOPPED : "Minden alkalmazás leállítva",
    ALL_SERVER_STOPPED : "Minden kiszolgáló leállítva",
    ALL_CLUSTERS_STOPPED : "Minden fürt leállítva",
    ALL_SERVERS_UNKNOWN : "Minden kiszolgáló ismeretlen",
    SOME_APPS_RUNNING : "Néhány alkalmazás fut",
    SOME_SERVERS_RUNNING : "Néhány kiszolgáló fut",
    SOME_CLUSTERS_RUNNING : "Néhány fürt fut",
    NO_APPS_RUNNING : "Nincs futó alkalmazás",
    NO_SERVERS_RUNNING : "Nincs futó kiszolgáló",
    NO_CLUSTERS_RUNNING : "Nincs futó fürt",

    HOST_WITH_ALL_SERVERS_RUNNING: "Hosztok, amelyeken az összes kiszolgáló fut", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Hosztok, amelyeken a kiszolgálók egy része fut",
    HOST_WITH_NO_SERVERS_RUNNING: "Hosztok, amelyeken egy kiszolgáló sem fut", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Hosztok, amelyeken minden kiszolgáló leállt",
    HOST_WITH_SERVERS_RUNNING: "Hosztok, amelyeken kiszolgálók futnak",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Futási környezetek, amelyekben a kiszolgálók egy része fut",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Futási környezetek, amelyekben az összes kiszolgáló le van állítva",
    RUNTIME_WITH_SERVERS_RUNNING: "Futási környezetek, amelyekben a kiszolgálók futnak",

    START_ALL_APPS : "Minden alkalmazás indítása?",
    START_ALL_INSTANCES : "Minden alkalmazáspéldány indítása?",
    START_ALL_SERVERS : "Minden kiszolgáló indítása?",
    START_ALL_CLUSTERS : "Minden fürt indítása?",
    STOP_ALL_APPS : "Minden alkalmazás leállítása?",
    STOPE_ALL_INSTANCES : "Minden alkalmazáspéldány leállítása?",
    STOP_ALL_SERVERS : "Minden kiszolgáló leállítása?",
    STOP_ALL_CLUSTERS : "Minden fürt leállítása?",
    RESTART_ALL_APPS : "Minden alkalmazás újraindítása?",
    RESTART_ALL_INSTANCES : "Minden alkalmazáspéldány újraindítása?",
    RESTART_ALL_SERVERS : "Minden kiszolgáló újraindítása?",
    RESTART_ALL_CLUSTERS : "Minden fürt újraindítása?",

    START_INSTANCE : "Alkalmazáspéldány indítása?",
    STOP_INSTANCE : "Alkalmazáspéldány leállítása?",
    RESTART_INSTANCE : "Alkalmazáspéldány újraindítása?",

    START_SERVER : "{0} kiszolgáló indítása?",
    STOP_SERVER : "{0} kiszolgáló leállítása?",
    RESTART_SERVER : "{0} kiszolgáló újraindítása?",

    START_ALL_INSTS_OF_APP : "{0} minden példányának indítása?", // application name
    START_APP_ON_SERVER : "{0} indítása a következőn: {1}?", // app name, server name
    START_ALL_APPS_WITHIN : "Minden alkalmazás indítása a következőn belül: {0}?", // resource
    START_ALL_APP_INSTS_WITHIN : "Minden alkalmazáspéldány indítása a következőn belül: {0}?", // resource
    START_ALL_SERVERS_WITHIN : "Minden kiszolgáló indítása a következőn belül: {0}?", // resource
    STOP_ALL_INSTS_OF_APP : "{0} minden példányának leállítása?", // application name
    STOP_APP_ON_SERVER : "{0} leállítása a következőn: {1}?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Minden alkalmazás leállítása a következőn belül: {0}?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Minden alkalmazáspéldány leállítása a következőn belül: {0}?", // resource
    STOP_ALL_SERVERS_WITHIN : "Minden kiszolgáló leállítása a következőn belül: {0}?", // resource
    RESTART_ALL_INSTS_OF_APP : "{0} minden példányának újraindítása?", // application name
    RESTART_APP_ON_SERVER : "{0} újraindítása a következőn: {1}?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Minden alkalmazás újraindítása a következőn belül: {0}?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Minden alkalmazáspéldány újraindítása a következőn belül: {0}?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Minden futó kiszolgáló újraindítása a következőn belül: {0}?", // resource

    START_SELECTED_APPS : "Kijelölt alkalmazások minden példányának indítása?",
    START_SELECTED_INSTANCES : "Kijelölt alkalmazáspéldányok indítása?",
    START_SELECTED_SERVERS : "Kijelölt kiszolgálók indítása?",
    START_SELECTED_SERVERS_LABEL : "Kijelölt kiszolgálók indítása",
    START_SELECTED_CLUSTERS : "Kijelölt fürtök indítása?",
    START_CLEAN_SELECTED_SERVERS : "Elindítja a kiválasztott kiszolgálók takarítását?",
    START_CLEAN_SELECTED_CLUSTERS : "Elindítja a kiválasztott fürtök takarítását?",
    STOP_SELECTED_APPS : "Kijelölt alkalmazások minden példányának leállítása?",
    STOP_SELECTED_INSTANCES : "Kijelölt alkalmazáspéldányok leállítása?",
    STOP_SELECTED_SERVERS : "Kijelölt kiszolgálók leállítása?",
    STOP_SELECTED_CLUSTERS : "Kijelölt fürtök leállítása?",
    RESTART_SELECTED_APPS : "Kijelölt alkalmazások minden példányának újraindítása?",
    RESTART_SELECTED_INSTANCES : "Kijelölt alkalmazáspéldányok újraindítása?",
    RESTART_SELECTED_SERVERS : "Kijelölt kiszolgálók újraindítása?",
    RESTART_SELECTED_CLUSTERS : "Kijelölt fürtök újraindítása?",

    START_SERVERS_ON_HOSTS : "Minden kiszolgáló indítása a kijelölt hoszton?",
    STOP_SERVERS_ON_HOSTS : "Minden kiszolgáló leállítása a kijelölt hoszton?",
    RESTART_SERVERS_ON_HOSTS : "Minden futó kiszolgáló újraindítása a kijelölt hoszton?",

    SELECT_APPS_TO_START : "Leállított alkalmazások kijelölése indításra.",
    SELECT_APPS_TO_STOP : "Elindított alkalmazások kijelölése leállításra.",
    SELECT_APPS_TO_RESTART : "Elindított alkalmazások kijelölése újraindításra.",
    SELECT_INSTANCES_TO_START : "Leállított alkalmazáspéldányok kijelölése indításra.",
    SELECT_INSTANCES_TO_STOP : "Elindított alkalmazáspéldányok kijelölése leállításra.",
    SELECT_INSTANCES_TO_RESTART : "Elindított alkalmazáspéldányok kijelölése újraindításra.",
    SELECT_SERVERS_TO_START : "Leállított kiszolgálók kijelölése indításra.",
    SELECT_SERVERS_TO_STOP : "Elindított kiszolgálók kijelölése leállításra.",
    SELECT_SERVERS_TO_RESTART : "Elindított kiszolgálók kijelölése újraindításra.",
    SELECT_CLUSTERS_TO_START : "Leállított fürtök kijelölése indításra.",
    SELECT_CLUSTERS_TO_STOP : "Elindított fürtök kijelölése leállításra.",
    SELECT_CLUSTERS_TO_RESTART : "Elindított fürtök kijelölése újraindításra.",

    STATUS : "Állapot",
    STATE : "Állapot:",
    NAME : "Név: ",
    DIRECTORY : "Könyvtár",
    INFORMATION : "Információk",
    DETAILS : "Részletek",
    ACTIONS : "Műveletek",
    CLOSE : "Bezárás",
    HIDE : "Elrejtés",
    SHOW_ACTIONS : "Műveletek megjelenítése",
    SHOW_SERVER_ACTIONS_LABEL : "{0} kiszolgáló műveletei",
    SHOW_APP_ACTIONS_LABEL : "{0} alkalmazás műveletei",
    SHOW_CLUSTER_ACTIONS_LABEL : "{0} fürt műveletei",
    SHOW_HOST_ACTIONS_LABEL : "{0} hoszt műveletei",
    SHOW_RUNTIME_ACTIONS_LABEL : "{0} futási környezet műveletei",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "{0} kiszolgáló műveleti menüje",
    SHOW_APP_ACTIONS_MENU_LABEL : "{0} alkalmazás műveleti menüje",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "{0} fürt műveleti menüje",
    SHOW_HOST_ACTIONS_MENU_LABEL : "{0} hoszt műveleti menüje",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "{0} futási környezet műveleti menüje",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "A(z) {0} hoszton lévő futási környezet műveleti menüje",
    SHOW_COLLECTION_MENU_LABEL : "{0} gyűjtemény állapotműveleti menüje",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "{0} keresés állapotműveleti menüje",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: ismeretlen állapot", // resourceName
    UNKNOWN_STATE_APPS : "{0} alkalmazás ismeretlen állapotban", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} alkalmazáspéldány ismeretlen állapotban", // quantity
    UNKNOWN_STATE_SERVERS : "{0} kiszolgáló ismeretlen állapotban", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} fürt ismeretlen állapotban", // quantity

    INSTANCES_NOT_RUNNING : "{0} alkalmazáspéldány nem fut", // quantity
    APPS_NOT_RUNNING : "{0} alkalmazás nem fut", // quantity
    SERVERS_NOT_RUNNING : "{0} kiszolgáló nem fut", // quantity
    CLUSTERS_NOT_RUNNING : "{0} fürt nem fut", // quantity

    APP_STOPPED_ON_SERVER : "{0} leállítva futó kiszolgálókon: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} alkalmazás leállítva futó kiszolgálókon: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} alkalmazás leállítva futó kiszolgálókon.", // quantity
    NUMBER_RESOURCES : "{0} erőforrás", // quantity
    NUMBER_APPS : "{0} alkalmazás", // quantity
    NUMBER_SERVERS : "{0} kiszolgáló", // quantity
    NUMBER_CLUSTERS : "{0} fürt", // quantity
    NUMBER_HOSTS : "{0} hoszt", // quantity
    NUMBER_RUNTIMES : "{0} futási környezet", // quantity
    SERVERS_INSERT : "kiszolgálókon",
    INSERT_STOPPED_ON_INSERT : "{0} leállítva futó {1}.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} leállítva a futtató kiszolgálón: {1}", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "{0} a(z) {1} fürtön leállítva a futtató kiszolgálókon: {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} alkalmazáspéldány leállítva futó kiszolgálókon.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: alkalmazáspéldány nem fut", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: nem minden alkalmazás fut", // serverName[]
    NO_APPS_RUNNING : "{0}: nincs futó alkalmazás", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} kiszolgáló, ahol nem minden alkalmazás fut", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} kiszolgáló futó alkalmazás nélkül", // quantity

    COUNT_OF_APPS_SELECTED : "{0} alkalmazás kijelölve",
    RATIO_RUNNING : "{0} fut", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0} kijelölve",

    NO_HOSTS_SELECTED : "Nincs kijelölt hoszt",
    NO_DEPLOY_RESOURCE : "Nincs erőforrás telepítés rendszerbe állításához",
    NO_TOPOLOGY : "Nincsen {0}.",
    COUNT_OF_APPS_STARTED  : "{0} alkalmazás elindítva",

    APPS_LIST : "{0} alkalmazás",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} példány fut",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} kiszolgáló fut",
    RESOURCE_ON_RESOURCE : "{0} a következőn: {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} a(z) {1} kiszolgálón", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} a(z) {1} fürtön", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Az újraindítás ezen a kiszolgálón le van tiltva, mert ezen fut az Admin Center.",
    ACTION_DISABLED_FOR_USER: "A műveletek le vannak tiltva ezen az erőforráson, mert a felhasználó nem rendelkezik jogosultsággal",

    RESTART_AC_TITLE: "Admin Center esetén nincs újraindítás",
    RESTART_AC_DESCRIPTION: "A(z) {0} kiszolgálón fut az Admin Center. Az Admin Center nem tudja újraindítani önmagát.",
    RESTART_AC_MESSAGE: "A többi kijelölt kiszolgáló újra lesz indítva.",
    RESTART_AC_CLUSTER_MESSAGE: "Az összes többi kiválasztott fürt újra lesz indítva.",

    STOP_AC_TITLE: "Admin Center leállítása",
    STOP_AC_DESCRIPTION: "A(z) {0} kiszolgáló egy együttes vezérlő, amely az Admin Center programot futtatja. A leállítása hatással lehet a Liberty együttes felügyeleti műveleteire, és elérhetetlenné teheti az Admin Center programot.",
    STOP_AC_MESSAGE: "Leállítja ezt a vezérlőt?",
    STOP_STANDALONE_DESCRIPTION: "A(z) {0} kiszolgáló futtatja az adminisztrációs központot. A leállítása elérhetetlenné teheti az adminisztrációs központot.",
    STOP_STANDALONE_MESSAGE: "Leállítja ezt a kiszolgálót?",

    STOP_CONTROLLER_TITLE: "Vezérlő leállítása",
    STOP_CONTROLLER_DESCRIPTION: "A(z) {0} kiszolgáló egy együttes vezérlő. A leállítás hatással lehet a Liberty együttes műveleteire.",
    STOP_CONTROLLER_MESSAGE: "Leállítja ezt a vezérlőt?",

    STOP_AC_CLUSTER_TITLE: "A(z) {0} fürt leállítása",
    STOP_AC_CLUSTER_DESCRIPTION: "A(z) {0} fürt tartalmazza az adminisztrációs központot futtató együttes vezérlőt.  A leállítása hatással lehet a Liberty kollektív felügyeleti műveleteire, és elérhetetlenné teheti az adminisztrációs központot.",
    STOP_AC_CLUSTER_MESSAGE: "Leállítja ezt a fürtöt?",

    INVALID_URL: "Az oldal nem létezik.",
    INVALID_APPLICATION: "A(z) {0} alkalmazás már nem létezik az együttesben.", // application name
    INVALID_SERVER: "A(z) {0} kiszolgáló már nem létezik az együttesben.", // server name
    INVALID_CLUSTER: "A(z) {0} fürt már nem létezik az együttesben.", // cluster name
    INVALID_HOST: "A(z) {0} hoszt már nem létezik az együttesben.", // host name
    INVALID_RUNTIME: "A(z) {0} futási környezet már nem létezik az együttesben.", // runtime name
    INVALID_INSTANCE: "A(z) {0} alkalmazáspéldány már nem létezik az együttesben.", // application instance name
    GO_TO_DASHBOARD: "Ugrás a műszerfalra",
    VIEWED_RESOURCE_REMOVED: "Hoppá! Az erőforrás eltávolításra került, vagy már nem érhető el.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "Megszakadt a kapcsolat a kiszolgálóval. Az oldal már nem jeleníti meg a környezet dinamikus változásait. Frissítse az oldalt a kapcsolat és a dinamikus frissítések helyreállításához.",
    ERROR_MESSAGE: "A kapcsolat megszakadt",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Kiszolgáló leállítása',

    // Tags
    RELATED_RESOURCES: "Kapcsolódó erőforrások",
    TAGS : "Címkék",
    TAG_BUTTON_LABEL : "{0} címke",  // tag value
    TAGS_LABEL : "Adja meg a címkéket vesszővel, szóközzel, Enterrel vagy tabulátorral elválasztva.",
    OWNER : "Tulajdonos",
    OWNER_BUTTON_LABEL : "{0} tulajdonos",  // owner value
    CONTACTS : "Kapcsolattartók",
    CONTACT_BUTTON_LABEL : "{0} kapcsolattartó",  // contact value
    PORTS : "Portok",
    CONTEXT_ROOT : "Környezet gyökere",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Részletek",  // alt text for the ... button
    MORE_BUTTON_MENU : "{0} további menü", // alt text for the menu
    NOTES: "Megjegyzések",
    NOTE_LABEL : "{0} megjegyzés",  // note value
    SET_ATTRIBUTES: "Címkék és metaadatok",
    SETATTR_RUNTIME_NAME: "{0} a következőn: {1}",  // runtime, host
    SAVE: "Mentés",
    TAGINVALIDCHARS: "A '/', '<' és '>' karakterek nem érvényesek.",
    ERROR_GET_TAGS_METADATA: "A termék nem tudja beolvasni az erőforrás aktuális címkéit és metaadatait.",
    ERROR_SET_TAGS_METADATA: "A termék egy hiba miatt nem tudta beállítani a címkéket és metaadatokat.",
    METADATA_WILLBE_INHERITED: "A metaadatok beállításra kerülnek az alkalmazáson, és a fürtben található minden példányon meg lesznek osztva.",
    ERROR_ALT: "Hiba",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Ehhez a kiszolgálóhoz nem érhetők el aktuális statisztikák, mivel le van állítva. Indítsa el a kiszolgálót, hogy elkezdje a megfigyelését.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Ehhez az alkalmazáshoz nem érhetők el aktuális statisztikák, mivel a társított kiszolgálója le van állítva. Indítsa el a kiszolgálót, hogy elkezdje az alkalmazás megfigyelését.",
    GRAPH_FEATURES_NOT_CONFIGURED: "Még nincs itt semmi! Kattintson a Szerkesztés ikonra és adjon hozzá méréseket az erőforrás megfigyeléséhez.",
    NO_GRAPHS_AVAILABLE: "Nem érhetők el hozzáadható mérőszámok. Próbáljon meg további megfigyelési szolgáltatásokat telepíteni, hogy több mérés legyen elérhető.",
    NO_APPS_GRAPHS_AVAILABLE: "Nem érhetők el hozzáadható mérőszámok. Próbáljon meg további megfigyelési szolgáltatásokat telepíteni, hogy több mérés legyen elérhető.Győződjön meg arról is, hogy az alkalmazás használatban van.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Nem mentett módosítások",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "Nem mentett módosításokkal rendelkezik. Ha átlép egy másik oldalra, akkor elveszíti a módosításokat.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Kívánja menteni a módosításait?",

    NO_CPU_STATS_AVAILABLE : "Ehhez a kiszolgálóhoz nem érhetők el CPU használati statisztikák.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "A nézet engedélyezéséhez telepítse a Kiszolgálókonfigurációs eszközt.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Bezárás előtt menti a(z) {0} módosításait?",
    SAVE: "Mentés",
    DONT_SAVE: "Nincs mentés",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Karbantartási mód engedélyezése",
    DISABLE_MAINTENANCE_MODE: "Karbantartási mód letiltása",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Karbantartási mód engedélyezése",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Karbantartási mód letiltása",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Engedélyezheti a karbantartási módot a hoszton és az összes kiszolgálóján ({0} kiszolgáló)",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Engedélyezheti a karbantartási módot a hosztokon és az összes kiszolgálóikon ({0} kiszolgáló)",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Karbantartási mód engedélyezése a kiszolgálón",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Karbantartási mód engedélyezése a kiszolgálókon",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Karbantartási mód letiltása a hoszton és az összes kiszolgálóján ({0} kiszolgáló)",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Karbantartási mód letiltása a kiszolgálón",
    BREAK_AFFINITY_LABEL: "Állandóság megszakítása az aktív munkamenetekkel",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Engedélyezés",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Letiltás",
    MAINTENANCE_MODE: "Karbantartási mód",
    ENABLING_MAINTENANCE_MODE: "Karbantartási mód engedélyezése",
    MAINTENANCE_MODE_ENABLED: "Karbantartási mód engedélyezve",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "A karbantartási mód nem lett engedélyezve, mert az alternatív kiszolgálók nem indultak el.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Válassza a Kényszerítés lehetőséget, ha a karbantartási módot az alternatív kiszolgálók elindítása nélkül is engedélyezni szeretné. A kényszerítés megszakíthatja az automatikus méretezési irányelveket.",
    MAINTENANCE_MODE_FAILED: "A karbantartási mód nem engedélyezhető.",
    MAINTENANCE_MODE_FORCE_LABEL: "Kényszerítés",
    MAINTENANCE_MODE_CANCEL_LABEL: "Mégse",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "Jelenleg {0} kiszolgáló van karbantartási módban.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Karbantartási mód engedélyezése az összes hosztkiszolgálón.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Karbantartási mód engedélyezése az összes hosztkiszolgálón.  Az állapotot a Kiszolgálók nézetben jelenítheti meg.",

    SERVER_API_DOCMENTATION: "Kiszolgáló API meghatározásának megjelenítése",

    // objectView title
    TITLE_FOR_CLUSTER: "{0} fürt", // cluster name
    TITLE_FOR_HOST: "{0} hoszt", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Kollektív vezérlő",
    LIBERTY_SERVER : "Liberty kiszolgáló",
    NODEJS_SERVER : "Node.js kiszolgáló",
    CONTAINER : "Tároló",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Liberty kiszolgáló Docker tárolóban",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Node.js kiszolgáló Docker tárolóban",
    RUNTIME_LIBERTY : "Liberty futási környezet",
    RUNTIME_NODEJS : "Node.js futási környezet",
    RUNTIME_DOCKER : "Futási környezet Docker tárolóban"

});
