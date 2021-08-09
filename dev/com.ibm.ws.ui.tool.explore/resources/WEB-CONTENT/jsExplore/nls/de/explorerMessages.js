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
    EXPLORE : "Untersuchen",
    DASHBOARD : "Dashboard",
    DASHBOARD_VIEW_ALL_APPS : "Alle Anwendungen anzeigen",
    DASHBOARD_VIEW_ALL_SERVERS : "Alle Server anzeigen",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Alle Cluster anzeigen",
    DASHBOARD_VIEW_ALL_HOSTS : "Alle Hosts anzeigen",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Alle Laufzeitumgebungen anzeigen",
    SEARCH : "Suchen",
    SEARCH_RECENT : "Letzte Suchoperationen",
    SEARCH_RESOURCES : "Ressourcen suchen",
    SEARCH_RESULTS : "Suchergebnisse",
    SEARCH_NO_RESULTS : "Keine Ergebnisse",
    SEARCH_NO_MATCHES : "Keine Übereinstimmungen",
    SEARCH_TEXT_INVALID : "Der Suchbegriff enthält ungültige Zeichen.",
    SEARCH_CRITERIA_INVALID : "Die Suchkriterien sind nicht gültig.",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} ist nicht gültig, wenn {1} angegeben ist.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "Geben Sie {0} nur ein einziges Mal an.",
    SEARCH_TEXT_MISSING : "Der Suchbegriff ist erforderlich.",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "Das Suchen von Anwendungstags auf einem Server wird nicht unterstützt.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "Das Suchen von Anwendungstags in einem Cluster wird nicht unterstützt.",
    SEARCH_UNSUPPORT : "Die Suchkriterien werden nicht unterstützt.",
    SEARCH_SWITCH_VIEW : "Ansicht wechseln",
    FILTERS : "Filter",
    DEPLOY_SERVER_PACKAGE : "Serverpaket implementieren",
    MEMBER_OF : "Member von",
    N_CLUSTERS: "{0} Cluster...",

    INSTANCE : "Instanz",
    INSTANCES : "Instanzen",
    APPLICATION : "Anwendung",
    APPLICATIONS : "Anwendungen",
    SERVER : "Server",
    SERVERS : "Server",
    CLUSTER : "Cluster",
    CLUSTERS : "Cluster",
    CLUSTER_NAME : "Clustername: ",
    CLUSTER_STATUS : "Clusterstatus: ",
    APPLICATION_NAME : "Anwendungsname: ",
    APPLICATION_STATE : "Anwendungsstatus: ",
    HOST : "Host",
    HOSTS : "Hosts",
    RUNTIME : "Laufzeitumgebung",
    RUNTIMES : "Laufzeitumgebungen",
    PATH : "Pfad",
    CONTROLLER : "Controller",
    CONTROLLERS : "Controller",
    OVERVIEW : "Übersicht",
    CONFIGURE : "Konfigurieren",

    SEARCH_RESOURCE_TYPE: "Typ", // Search by resource types
    SEARCH_RESOURCE_STATE: "Status", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Alle", // Search all resource types
    SEARCH_RESOURCE_NAME: "Name", // Search by resource name
    SEARCH_RESOURCE_TAG: "Tag", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Container", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Keine", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Laufzeittyp", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Eigner", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Ansprechpartner", // Search by contact
    SEARCH_RESOURCE_NOTE: "Anmerkung", // Search by note

    GRID_HEADER_USERDIR : "Benutzerverzeichnis",
    GRID_HEADER_NAME : "Name",
    GRID_LOCATION_NAME : "Position",
    GRID_ACTIONS : "Gridaktionen",
    GRID_ACTIONS_LABEL : "Aktionen für Grid {0}",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} auf {1} ({2})", // server on host (/path)

    STATS : "Überwachen",
    STATS_ALL : "Alle",
    STATS_VALUE : "Wert: {0}",
    CONNECTION_IN_USE_STATS : "{0} Im Gebrauch = {1} Verwaltet - {2} Frei",
    CONNECTION_IN_USE_STATS_VALUE : "Wert: {0} Im Gebrauch = {1} Verwaltet - {2} Frei",
    DATA_SOURCE : "Datenquelle: {0}",
    STATS_DISPLAY_LEGEND : "Legende einblenden",
    STATS_HIDE_LEGEND : "Legende ausblenden",
    STATS_VIEW_DATA : "Diagrammdaten anzeigen",
    STATS_VIEW_DATA_TIMESTAMP : "Zeitmarke",
    STATS_ACTION_MENU : "Aktionsmenü für {0}",
    STATS_SHOW_HIDE : "Ressourcenmetriken hinzufügen",
    STATS_SHOW_HIDE_SUMMARY : "Metriken für Zusammenfassung hinzufügen",
    STATS_SHOW_HIDE_TRAFFIC : "Metriken für Datenverkehr hinzufügen",
    STATS_SHOW_HIDE_PERFORMANCE : "Metriken für Leistung hinzufügen",
    STATS_SHOW_HIDE_AVAILABILITY : "Metriken für Verfügbarkeit hinzufügen",
    STATS_SHOW_HIDE_ALERT : "Metriken für Alert hinzufügen",
    STATS_SHOW_HIDE_LIST_BUTTON : "Ressourcenmetrikliste einblenden oder ausblenden",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Diagramme bearbeiten",
    STATS_SHOW_HIDE_CONFIRM : "Speichern",
    STATS_SHOW_HIDE_CANCEL : "Abbrechen",
    STATS_SHOW_HIDE_DONE : "Fertig",
    STATS_DELETE_GRAPH : "Diagramm löschen",
    STATS_ADD_CHART_LABEL : "Diagramm zur Ansicht hinzufügen",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Alle JVM-Diagramme zur Ansicht hinzufügen",
    STATS_HEAP_TITLE : "Belegter Heapspeicher",
    STATS_HEAP_USED : "Belegt: {0} MB",
    STATS_HEAP_COMMITTED : "Festgeschrieben: {0} MB",
    STATS_HEAP_MAX : "Maximal: {0} MB",
    STATS_HEAP_X_TIME : "Zeit",
    STATS_HEAP_Y_MB : "MB belegt",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "Geladene Klassen",
    STATS_CLASSES_LOADED : "Geladen: {0}",
    STATS_CLASSES_UNLOADED : "Entladen: {0}",
    STATS_CLASSES_TOTAL : "Insgesamt: {0}",
    STATS_CLASSES_Y_TOTAL : "Geladene Klassen",
    STATS_PROCESSCPU_TITLE : "CPU-Auslastung",
    STATS_PROCESSCPU_USAGE : "CPU-Auslastung: {0} %",
    STATS_PROCESSCPU_Y_PERCENT : "CPU in Prozent",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Aktive JVM-Threads",
    STATS_LIVE_MSG_INIT : "Livedaten anzeigen",
    STATS_LIVE_MSG :"Dieses Diagramm enthält keine Langzeitdaten. Im Diagramm werden weiterhin nur die Daten der letzten 10 Minuten angezeigt.",
    STATS_THREADS_ACTIVE : "Live: {0}",
    STATS_THREADS_PEAK : "Spitzenwert: {0}",
    STATS_THREADS_TOTAL : "Insgesamt: {0}",
    STATS_THREADS_Y_THREADS : "Threads",
    STATS_TP_POOL_SIZE : "Poolgröße",
    STATS_JAXWS_TITLE : "JAX-WS-Web-Services",
    STATS_JAXWS_BUTTON_LABEL : "Alle JAX-WS-Web-Service-Diagramme zur Ansicht hinzufügen",
    STATS_JW_AVG_RESP_TIME : "Durchschnittliche Antwortzeit",
    STATS_JW_AVG_INVCOUNT : "Durchschnittliche Aufrufanzahl",
    STATS_JW_TOTAL_FAULTS : "Gesamtanzahl der Laufzeitfehler",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Ressourcen auswählen...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} Ressourcen",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 Ressource",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "Sie müssen mindestens eine Ressource auswählen.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "Es sind keine Daten für den ausgewählten Zeitraum verfügbar.",
    STATS_ACCESS_LOG_TITLE : "Zugriffsprotokoll",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Alle Zugriffsprotokolldiagramme zur Ansicht hinzufügen",
    STATS_ACCESS_LOG_GRAPH : "Anzahl der Zugriffsprotokollnachrichten",
    STATS_ACCESS_LOG_SUMMARY : "Zusammenfassung des Zugriffsprotokolls",
    STATS_ACCESS_LOG_TABLE : "Nachrichtenliste des Zugriffsprotokolls",
    STATS_MESSAGES_TITLE : "Nachrichten und Trace",
    STATS_MESSAGES_BUTTON_LABEL : "Alle Nachrichten- und Tracediagramme zur Ansicht hinzufügen",
    STATS_MESSAGES_GRAPH : "Anzahl der Protokollnachrichten",
    STATS_MESSAGES_TABLE : "Liste der Protokollnachrichten",
    STATS_FFDC_GRAPH : "FFDC-Anzahl",
    STATS_FFDC_TABLE : "FFDC-Liste",
    STATS_TRACE_LOG_GRAPH : "Anzahl der Tracenachrichten",
    STATS_TRACE_LOG_TABLE : "Liste der Tracenachrichten",
    STATS_THREAD_POOL_TITLE : "Thread-Pool",
    STATS_THREAD_POOL_BUTTON_LABEL : "Alle Thread-Pool-Diagramme zur Ansicht hinzufügen",
    STATS_THREADPOOL_TITLE : "Aktive Liberty-Threads",
    STATS_THREADPOOL_SIZE : "Poolgröße: {0}",
    STATS_THREADPOOL_ACTIVE : "Aktiv: {0}",
    STATS_THREADPOOL_TOTAL : "Insgesamt: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Aktive Threads",
    STATS_SESSION_MGMT_TITLE : "Sitzungen",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Alle Sitzungsdiagramme zur Ansicht hinzufügen",
    STATS_SESSION_CONFIG_LABEL : "Sitzungen auswählen...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} Sitzungen",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 Sitzung",
    STATS_SESSION_CONFIG_SELECT_ONE : "Sie müssen mindestens eine Sitzung auswählen.",
    STATS_SESSION_TITLE : "Aktive Sitzungen",
    STATS_SESSION_Y_ACTIVE : "Aktive Sitzungen",
    STATS_SESSION_LIVE_LABEL : "Anzahl der Livesitzungen: {0}",
    STATS_SESSION_CREATE_LABEL : "Anzahl erstellter Sitzungen: {0}",
    STATS_SESSION_INV_LABEL : "Anzahl inaktivierter Sitzungen: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Anzahl der durch Zeitlimitüberschreitung inaktivierten Sitzungen: {0}",
    STATS_WEBCONTAINER_TITLE : "Webanwendungen",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Alle Webanwendungsdiagramme zur Ansicht hinzufügen",
    STATS_SERVLET_CONFIG_LABEL : "Servlets auswählen...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} Servlets",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 Servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "Sie müssen mindestens ein Servlet auswählen.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Anzahl der Anforderungen",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Anzahl der Anforderungen",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Anzahl der Antworten",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Anzahl der Antworten",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Durchschnittliche Antwortzeit (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Antwortzeit (ns)",
    STATS_CONN_POOL_TITLE : "Verbindungspool",
    STATS_CONN_POOL_BUTTON_LABEL : "Alle Verbindungspooldiagramme zur Ansicht hinzufügen",
    STATS_CONN_POOL_CONFIG_LABEL : "Datenquellen auswählen...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} Datenquellen",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 Datenquelle",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "Sie müssen mindestens eine Datenquelle auswählen.",
    STATS_CONNECT_IN_USE_TITLE : "Verbindungen im Gebrauch",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Verbindungen",
    STATS_CONNECT_IN_USE_LABEL : "Im Gebrauch: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Verwendet: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Frei: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Erstellt: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Gelöscht: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Durchschnittliche Wartezeit (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Wartezeit (ms)",
    STATS_TIME_ALL : "Alle",
    STATS_TIME_1YEAR : "1 Jahr",
    STATS_TIME_1MONTH : "1 Monat",
    STATS_TIME_1WEEK : "1 Woche",
    STATS_TIME_1DAY : "1 Tag",
    STATS_TIME_1HOUR : "1 Stunde",
    STATS_TIME_10MINUTES : "10 Minuten",
    STATS_TIME_5MINUTES : "5 Minuten",
    STATS_TIME_1MINUTE : "1 Minute",
    STATS_PERSPECTIVE_SUMMARY : "Zusammenfassung",
    STATS_PERSPECTIVE_TRAFFIC : "Datenverkehr",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "JVM-Datenverkehr",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Datenverkehr bei Verbindungen",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Datenverkehr bei Zugriffsprotokollen",
    STATS_PERSPECTIVE_PROBLEM : "Problem",
    STATS_PERSPECTIVE_PERFORMANCE : "Leistung",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "JVM-Leistung",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Verbindungsleistung",
    STATS_PERSPECTIVE_ALERT : "Alertanalyse",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Alerts bei Zugriffsprotokollen",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Alerts bei Nachrichten- und Traceprotokollen",
    STATS_PERSPECTIVE_AVAILABILITY : "Verfügbarkeit",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Letzte Minute",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "Letzte 5 Minuten",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "Letzte 10 Minuten",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Letzte Stunde",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Letzter Tag",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Letzte Woche",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Letzter Monat",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Letztes Jahr",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Letzte {0} Sekunden",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Letzte {0} Minuten",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Letzte {0} Minuten(n) {1} Sekunde(n)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Letzte {0} Stunden",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Letzte {0} Stunde(n) {1} Minute(n)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Letzte {0} Tage",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Letzter/Letzte {0} Tag(e) {1} Stunde(n)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Letzte {0} Woche(n)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Letzte {0} Woche(n) {1} Tag(e)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Letzter/Letzte {0} Monat(e)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Letzter/Letzte {0} Monat(e) {1} Tag(e)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Letztes/Letzte Last {0} Jahr(e)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Letztes/Letzte {0} Jahr(e) {1} Monat(e)",

    STATS_LIVE_UPDATE_LABEL: "Liveaktualisierung",
    STATS_TIME_SELECTOR_NOW_LABEL: "Sofort",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Protokollnachrichten",

    AUTOSCALED_APPLICATION : "Automatisch skalierte Anwendung",
    AUTOSCALED_SERVER : "Automatisch skalierter Server",
    AUTOSCALED_CLUSTER : "Automatisch skalierter Cluster",
    AUTOSCALED_POLICY : "Richtlinie für automatische Skalierung",
    AUTOSCALED_POLICY_DISABLED : "Die Richtlinie für automatische Skalierung ist inaktiviert.",
    AUTOSCALED_NOACTIONS : "Es sind keine Aktionen für automatisch skalierte Ressourcen verfügbar.",

    START : "Starten",
    START_CLEAN : "--clean starten",
    STARTING : "Wird gestartet",
    STARTED : "Gestartet",
    RUNNING : "Aktiv",
    NUM_RUNNING: "{0} aktiv",
    PARTIALLY_STARTED : "Teilweise gestartet",
    PARTIALLY_RUNNING : "Teilweise aktiv",
    NOT_STARTED : "Nicht gestartet",
    STOP : "Stoppen",
    STOPPING : "Wird gestoppt",
    STOPPED : "Gestoppt",
    NUM_STOPPED : "{0} gestoppt",
    NOT_RUNNING : "Nicht aktiv",
    RESTART : "Erneut starten",
    RESTARTING : "Wird erneut gestartet",
    RESTARTED : "Erneut gestartet",
    ALERT : "Alert",
    ALERTS : "Alerts",
    UNKNOWN : "Unbekannt",
    NUM_UNKNOWN : "{0} unbekannt",
    SELECT : "Auswählen",
    SELECTED : "Ausgewählt",
    SELECT_ALL : "Alles auswählen",
    SELECT_NONE : "Nichts auswählen",
    DESELECT: "Abwählen",
    DESELECT_ALL : "Alles abwählen",
    TOTAL : "Insgesamt",
    UTILIZATION : "Über {0} % Auslastung", // percent

    ELLIPSIS_ARIA: "Für weitere Optionen einblenden.",
    EXPAND : "Einblenden",
    COLLAPSE: "Ausblenden",

    ALL : "Alle",
    ALL_APPS : "Alle Anwendungen",
    ALL_SERVERS : "Alle Server",
    ALL_CLUSTERS : "Alle Cluster",
    ALL_HOSTS : "Alle Hosts",
    ALL_APP_INSTANCES : "Alle Anwendungsinstanzen",
    ALL_RUNTIMES : "Alle Laufzeitumgebungen",

    ALL_APPS_RUNNING : "Alle Anwendungen aktiv",
    ALL_SERVER_RUNNING : "Alle Server aktiv",
    ALL_CLUSTERS_RUNNING : "Alle Cluster aktiv",
    ALL_APPS_STOPPED : "Alle Anwendungen gestoppt",
    ALL_SERVER_STOPPED : "Alle Server gestoppt",
    ALL_CLUSTERS_STOPPED : "Alle Cluster gestoppt",
    ALL_SERVERS_UNKNOWN : "Alle Server unbekannt",
    SOME_APPS_RUNNING : "Einige Anwendungen aktiv",
    SOME_SERVERS_RUNNING : "Einige Server aktiv",
    SOME_CLUSTERS_RUNNING : "Einige Cluster aktiv",
    NO_APPS_RUNNING : "Keine Anwendungen aktiv",
    NO_SERVERS_RUNNING : "Keine Server aktiv",
    NO_CLUSTERS_RUNNING : "Keine Cluster aktiv",

    HOST_WITH_ALL_SERVERS_RUNNING: "Hosts, auf denen alle Server aktiv sind", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Hosts, auf denen einige Server aktiv sind",
    HOST_WITH_NO_SERVERS_RUNNING: "Hosts ohne aktive Server", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Hosts, auf denen alle Server gestoppt sind",
    HOST_WITH_SERVERS_RUNNING: "Hosts mit aktiven Servern",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Laufzeitumgebungen, in denen einige Server aktiv sind",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Laufzeitumgebungen, in denen alle Server gestoppt sind",
    RUNTIME_WITH_SERVERS_RUNNING: "Laufzeitumgebungen mit aktiven Servern",

    START_ALL_APPS : "Alle Anwendungen starten?",
    START_ALL_INSTANCES : "Alle Anwendungsinstanzen starten?",
    START_ALL_SERVERS : "Alle Server starten?",
    START_ALL_CLUSTERS : "Alle Cluster starten?",
    STOP_ALL_APPS : "Alle Anwendungen stoppen?",
    STOPE_ALL_INSTANCES : "Alle Anwendungsinstanzen stoppen?",
    STOP_ALL_SERVERS : "Alle Server stoppen?",
    STOP_ALL_CLUSTERS : "Alle Cluster stoppen?",
    RESTART_ALL_APPS : "Alle Anwendungen erneut starten?",
    RESTART_ALL_INSTANCES : "Alle Anwendungsinstanzen erneut starten?",
    RESTART_ALL_SERVERS : "Alle Server erneut starten?",
    RESTART_ALL_CLUSTERS : "Alle Cluster erneut starten?",

    START_INSTANCE : "Anwendungsinstanz starten?",
    STOP_INSTANCE : "Anwendungsinstanz stoppen?",
    RESTART_INSTANCE : "Anwendungsinstanz erneut starten?",

    START_SERVER : "Server {0} starten?",
    STOP_SERVER : "Server {0} stoppen?",
    RESTART_SERVER : "Server {0} erneut starten?",

    START_ALL_INSTS_OF_APP : "Alle Instanzen von {0} starten?", // application name
    START_APP_ON_SERVER : "{0} auf {1} starten?", // app name, server name
    START_ALL_APPS_WITHIN : "Alle Anwendungen in {0} starten?", // resource
    START_ALL_APP_INSTS_WITHIN : "Alle Anwendungsinstanzen in {0} starten?", // resource
    START_ALL_SERVERS_WITHIN : "Alle Server in {0} starten?", // resource
    STOP_ALL_INSTS_OF_APP : "Alle Instanzen von {0} stoppen?", // application name
    STOP_APP_ON_SERVER : "{0} auf {1} stoppen?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Alle Anwendungen in {0} stoppen?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Alle Anwendungsinstanzen in {0} stoppen?", // resource
    STOP_ALL_SERVERS_WITHIN : "Alle Server in {0} stoppen?", // resource
    RESTART_ALL_INSTS_OF_APP : "Alle Instanzen von {0} erneut starten?", // application name
    RESTART_APP_ON_SERVER : "{0} auf {1} erneut starten?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Alle Anwendungen in {0} erneut starten?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Alle Anwendungsinstanzen in {0} erneut starten?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Alle aktiven Server in {0} erneut starten?", // resource

    START_SELECTED_APPS : "Alle Instanzen der ausgewählten Anwendungen starten?",
    START_SELECTED_INSTANCES : "Ausgewählte Anwendungsinstanzen starten?",
    START_SELECTED_SERVERS : "Ausgewählte Server starten?",
    START_SELECTED_SERVERS_LABEL : "Ausgewählte Server starten",
    START_SELECTED_CLUSTERS : "Ausgewählte Cluster starten?",
    START_CLEAN_SELECTED_SERVERS : "Ausgewählte Server mit --clean starten?",
    START_CLEAN_SELECTED_CLUSTERS : "Ausgewählte Cluster mit --clean starten?",
    STOP_SELECTED_APPS : "Alle Instanzen der ausgewählten Anwendungen stoppen?",
    STOP_SELECTED_INSTANCES : "Ausgewählte Anwendungsinstanzen stoppen?",
    STOP_SELECTED_SERVERS : "Ausgewählte Server stoppen?",
    STOP_SELECTED_CLUSTERS : "Ausgewählte Cluster stoppen?",
    RESTART_SELECTED_APPS : "Alle Instanzen der ausgewählten Anwendungen erneut starten?",
    RESTART_SELECTED_INSTANCES : "Ausgewählte Anwendungsinstanzen erneut starten?",
    RESTART_SELECTED_SERVERS : "Ausgewählte Server erneut starten?",
    RESTART_SELECTED_CLUSTERS : "Ausgewählte Cluster erneut starten?",

    START_SERVERS_ON_HOSTS : "Alle Server auf ausgewählten Hosts starten?",
    STOP_SERVERS_ON_HOSTS : "Alle Server auf ausgewählten Hosts stoppen?",
    RESTART_SERVERS_ON_HOSTS : "Alle aktiven Server auf ausgewählten Hosts erneut starten?",

    SELECT_APPS_TO_START : "Wählen Sie die zu startenden gestoppten Anwendungen aus.",
    SELECT_APPS_TO_STOP : "Wählen Sie die zu stoppenden gestarteten Anwendungen aus.",
    SELECT_APPS_TO_RESTART : "Wählen Sie die erneut zu startenden gestarteten Anwendungen aus.",
    SELECT_INSTANCES_TO_START : "Wählen Sie die zu startenden gestoppten Anwendungsinstanzen aus.",
    SELECT_INSTANCES_TO_STOP : "Wählen Sie die zu stoppenden gestarteten Anwendungsinstanzen aus.",
    SELECT_INSTANCES_TO_RESTART : "Wählen Sie die erneut zu startenden gestarteten Anwendungsinstanzen aus.",
    SELECT_SERVERS_TO_START : "Wählen Sie die zu startenden gestoppten Server aus.",
    SELECT_SERVERS_TO_STOP : "Wählen Sie die zu stoppenden gestarteten Server aus.",
    SELECT_SERVERS_TO_RESTART : "Wählen Sie die erneut zu startenden gestarteten Server aus.",
    SELECT_CLUSTERS_TO_START : "Wählen Sie die zu startenden gestoppten Cluster aus.",
    SELECT_CLUSTERS_TO_STOP : "Wählen Sie die zu stoppenden gestarteten Cluster aus.",
    SELECT_CLUSTERS_TO_RESTART : "Wählen Sie die erneut zu startenden gestarteten Cluster aus.",

    STATUS : "Status",
    STATE : "Zustand:",
    NAME : "Name:",
    DIRECTORY : "Verzeichnis",
    INFORMATION : "Informationen",
    DETAILS : "Details",
    ACTIONS : "Aktionen",
    CLOSE : "Schließen",
    HIDE : "Ausblenden",
    SHOW_ACTIONS : "Aktionen anzeigen",
    SHOW_SERVER_ACTIONS_LABEL : "Aktionen für Server {0}",
    SHOW_APP_ACTIONS_LABEL : "Aktionen für Anwendung {0}",
    SHOW_CLUSTER_ACTIONS_LABEL : "Aktionen für Cluster {0}",
    SHOW_HOST_ACTIONS_LABEL : "Aktionen für Host {0}",
    SHOW_RUNTIME_ACTIONS_LABEL : "Aktionen für Laufzeitumgebung {0}",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "Aktionsmenü für Server {0} ",
    SHOW_APP_ACTIONS_MENU_LABEL : "Aktionsmenü für Anwendung {0}",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "Aktionsmenü für Cluster {0}",
    SHOW_HOST_ACTIONS_MENU_LABEL : "Aktionsmenü für Host {0}",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "Aktionsmenü für Laufzeitumgebung {0}",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "Aktionsmenü für Laufzeitumgebung auf Host {0}",
    SHOW_COLLECTION_MENU_LABEL : "Aktionsmenü für Erfassungsstatus von {0}",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "Aktionsmenü für Suchstatus von {0}",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: Unbekannter Zustand", // resourceName
    UNKNOWN_STATE_APPS : "{0} Anwendungen mit unbekanntem Zustand", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} Anwendungsinstanzen mit unbekanntem Zustand", // quantity
    UNKNOWN_STATE_SERVERS : "{0} Server mit unbekanntem Zustand", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} Cluster mit unbekanntem Zustand", // quantity

    INSTANCES_NOT_RUNNING : "{0} Anwendungsinstanzen nicht aktiv", // quantity
    APPS_NOT_RUNNING : "{0} Anwendungen nicht aktiv", // quantity
    SERVERS_NOT_RUNNING : "{0} Server nicht aktiv", // quantity
    CLUSTERS_NOT_RUNNING : "{0} Cluster nicht aktiv", // quantity

    APP_STOPPED_ON_SERVER : "{0} wurde auf folgenden aktiven Servern gestoppt: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} Anwendungen wurden auf folgenden aktiven Servern gestoppt: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} Anwendungen wurden auf aktiven Servern gestoppt.", // quantity
    NUMBER_RESOURCES : "{0} Ressourcen", // quantity
    NUMBER_APPS : "{0} Anwendungen", // quantity
    NUMBER_SERVERS : "{0} Server", // quantity
    NUMBER_CLUSTERS : "{0} Cluster", // quantity
    NUMBER_HOSTS : "{0} Hosts", // quantity
    NUMBER_RUNTIMES : "{0} Laufzeitumgebungen", // quantity
    SERVERS_INSERT : "Server",
    INSERT_STOPPED_ON_INSERT : "{0} wurde auf aktiven Servern {1} gestoppt.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} wurde auf dem aktiven Server {1} gestoppt.", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "{0} im Cluster {1} wurde auf den aktiven Servern {2} gestoppt.",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} Anwendungsinstanzen wurden auf aktiven Servern gestoppt.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: Anwendungsinstanz nicht aktiv", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: nicht alle Anwendungen aktiv", // serverName[]
    NO_APPS_RUNNING : "{0}: keine Anwendung aktiv", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} Server, auf denen nicht alle Anwendungen aktiv sind", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} Server, auf denen keine Anwendung aktiv ist", // quantity

    COUNT_OF_APPS_SELECTED : "{0} Anwendungen ausgewählt",
    RATIO_RUNNING : "{0} aktiv", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0} ausgewählt",

    NO_HOSTS_SELECTED : "Es wurden keine Hosts ausgewählt.",
    NO_DEPLOY_RESOURCE : "Es ist keine Ressource zum Implementieren der Installation verfügbar.",
    NO_TOPOLOGY : "Keine {0}.",
    COUNT_OF_APPS_STARTED  : "{0} Anwendungen gestartet",

    APPS_LIST : "{0} Anwendungen",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} Instanzen aktiv",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} Server aktiv",
    RESOURCE_ON_RESOURCE : "{0} auf {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} auf Server {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} im Cluster {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Der Neustart ist für diesen Server inaktiviert, weil dieser Server das Admin Center hostet.",
    ACTION_DISABLED_FOR_USER: "Aktionen sind bei dieser Ressource inaktiviert, da der Benutzer nicht berechtigt ist. ",

    RESTART_AC_TITLE: "Keinen Neustart für das Admin Center",
    RESTART_AC_DESCRIPTION: "{0} stellt das Admin Center bereit. Das Admin Center kann sich nicht selbst erneut starten.",
    RESTART_AC_MESSAGE: "Alle anderen ausgewählten Server werden erneut gestartet.",
    RESTART_AC_CLUSTER_MESSAGE: "Alle anderen ausgewählten Cluster werden erneut gestartet.",

    STOP_AC_TITLE: "Admin Center stoppen",
    STOP_AC_DESCRIPTION: "Der Server {0} ist ein Verbundcontroller, der das Admin Center ausführt. Wenn Sie ihn stoppen, kann sich dies auf die Verbundoperationen für die Liberty-Verwaltung auswirken und das Admin Center ist dann nicht mehr verfügbar. ",
    STOP_AC_MESSAGE: "Möchten Sie diesen Controller stoppen?",
    STOP_STANDALONE_DESCRIPTION: "Auf dem Server {0} wird das Admin Center ausgeführt. Wenn dieser Server gestoppt wird, ist das Admin Center nicht verfügbar.",
    STOP_STANDALONE_MESSAGE: "Möchten Sie diesen Server stoppen?",

    STOP_CONTROLLER_TITLE: "Controller stoppen",
    STOP_CONTROLLER_DESCRIPTION: "Der Server {0} ist ein Verbundcontroller. Wenn Sie diesen Server stoppen, kann dies Auswirkungen auf die Operationen im Liberty-Verbund haben.",
    STOP_CONTROLLER_MESSAGE: "Möchten Sie diesen Controller stoppen?",

    STOP_AC_CLUSTER_TITLE: "Cluster {0} stoppen",
    STOP_AC_CLUSTER_DESCRIPTION: "Der Cluster {0} enthält einen Verbundcontroller, auf dem das Admin Center ausgeführt wird. Wenn Sie den Cluster stoppen, kann sich dies auf die Verbundoperationen für die Liberty-Verwaltung auswirken und das Admin Center ist dann nicht mehr verfügbar. ",
    STOP_AC_CLUSTER_MESSAGE: "Möchten Sie diesen Cluster stoppen?",

    INVALID_URL: "Die Seite ist nicht vorhanden.",
    INVALID_APPLICATION: "Die Anwendung {0} ist nicht mehr im Verbund vorhanden.", // application name
    INVALID_SERVER: "Der Server {0} ist nicht mehr im Verbund vorhanden.", // server name
    INVALID_CLUSTER: "Der Cluster {0} ist nicht mehr im Verbund vorhanden.", // cluster name
    INVALID_HOST: "Der Host {0} ist nicht mehr im Verbund vorhanden.", // host name
    INVALID_RUNTIME: "Die Laufzeitumgebung {0} ist nicht mehr im Verbund vorhanden.", // runtime name
    INVALID_INSTANCE: "Die Anwendungsinstanz {0} ist nicht mehr im Verbund vorhanden.", // application instance name
    GO_TO_DASHBOARD: "In das Dashboard wechseln",
    VIEWED_RESOURCE_REMOVED: "Die Ressource wurde entfernt oder sie ist nicht mehr verfügbar.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "Die Verbindung zum Server ist unterbrochen. Auf der Seite werden keine dynamischen Änderungen in der Umgebung mehr angezeigt. Aktualisieren Sie die Seite, um die Verbindung für dynamische Aktualisierungen wiederherzustellen. ",
    ERROR_MESSAGE: "Verbindung unterbrochen",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Server stoppen',

    // Tags
    RELATED_RESOURCES: "Zugehörige Ressourcen",
    TAGS : "Tags",
    TAG_BUTTON_LABEL : "Tag {0}",  // tag value
    TAGS_LABEL : "Geben Sie eine durch Kommas, Leerzeichen, Zeilenumbrüche oder Tabulatoren getrennte Liste mit Tags ein.",
    OWNER : "Eigner",
    OWNER_BUTTON_LABEL : "Eigner {0}",  // owner value
    CONTACTS : "Ansprechpartner",
    CONTACT_BUTTON_LABEL : "Ansprechpartner {0}",  // contact value
    PORTS : "Ports",
    CONTEXT_ROOT : "Kontextstammverzeichnis",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Mehr",  // alt text for the ... button
    MORE_BUTTON_MENU : "Menü Mehr {0} ", // alt text for the menu
    NOTES: "Anmerkungen",
    NOTE_LABEL : "Anmerkung {0}",  // note value
    SET_ATTRIBUTES: "Tags und Metadaten",
    SETATTR_RUNTIME_NAME: "{0} auf {1}",  // runtime, host
    SAVE: "Speichern",
    TAGINVALIDCHARS: "Die Zeichen '/', '<' und '>' sind nicht gültig.",
    ERROR_GET_TAGS_METADATA: "Das Produkt kann die aktuellen Tags und Metadaten für die Ressource nicht abrufen.",
    ERROR_SET_TAGS_METADATA: "Ein Fehler verhindert die Festlegung der Tags und Metadaten durch das Produkt.",
    METADATA_WILLBE_INHERITED: "Metadaten werden für die Anwendung festgelegt und über alle Instanzen im Cluster verteilt.",
    ERROR_ALT: "Fehler",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Es sind keine aktuellen Statistiken für diesen Server verfügbar, weil der Server gestoppt ist. Starten Sie den Server, um mit der Überwachung des Servers zu beginnen.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Es sind keine aktuellen Statistiken für diese Anwendung verfügbar, weil die Anwendung einem Server zugeordnet ist, der gestoppt ist. Starten Sie den Server, um mit der Überwachung dieser Anwendung zu beginnen.",
    GRAPH_FEATURES_NOT_CONFIGURED: "Es sind noch keine Daten verfügbar! Starten Sie die Überwachung dieser Ressource, indem Sie das Symbol Bearbeiten auswählen und Metriken hinzufügen.",
    NO_GRAPHS_AVAILABLE: "Es sind keine Metriken verfügbar, die hinzugefügt werden könnten. Versuchen Sie, weitere Überwachungsfeatures hinzuzufügen, um weitere Metriken verfügbar zu machen. ",
    NO_APPS_GRAPHS_AVAILABLE: "Es sind keine Metriken verfügbar, die hinzugefügt werden könnten. Versuchen Sie, weitere Überwachungsfeatures hinzuzufügen, um weitere Metriken verfügbar zu machen. Stellen Sie außerdem sicher, dass die Anwendung im Gebrauch ist.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Nicht gespeicherte Änderungen",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "Es sind nicht gespeicherte Änderungen vorhanden. Wenn Sie eine andere Seite aufrufen, gehen diese Änderungen verloren.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Möchten Sie Ihre Änderungen speichern?",

    NO_CPU_STATS_AVAILABLE : "Es sind keine Statistiken zur CPU-Auslastung für diesen Server verfügbar.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "Zum Aktivieren dieser Ansicht installieren Sie das Serverkonfigurationstool.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Möchten Sie Ihre Änderungen in {0} vor dem Schließen der Seite speichern?",
    SAVE: "Speichern",
    DONT_SAVE: "Nicht speichern",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Wartungsmodus aktivieren",
    DISABLE_MAINTENANCE_MODE: "Wartungsmodus inaktivieren",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Wartungsmodus aktivieren",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Wartungsmodus inaktivieren",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Wartungsmodus auf dem Host und allen Servern des Hosts ({0} Server) aktivieren",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Wartungsmodus auf den Hosts und allen Servern der Hosts ({0} Server) aktivieren",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Wartungsmodus auf dem Server aktivieren",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Wartungsmodus auf den Servern aktivieren",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Wartungsmodus auf dem Host und allen Servern des Hosts ({0} Server) inaktivieren",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Wartungsmodus auf dem Server inaktivieren",
    BREAK_AFFINITY_LABEL: "Affinität zu aktiven Sitzungen aufheben",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Aktivieren",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Inaktivieren",
    MAINTENANCE_MODE: "Wartungsmodus",
    ENABLING_MAINTENANCE_MODE: "Wartungsmodus wird aktiviert",
    MAINTENANCE_MODE_ENABLED: "Wartungsmodus aktiviert",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "Der Wartungsmodus wurde nicht aktiviert, weil keine alternativen Server gestartet wurden.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Wählen Sie Erzwingen aus, um den Wartungsmodus zu aktivieren, ohne alternative Server zu starten. Durch das Erzwingen der Aktivierung werden die Richtlinien für die automatische Skalierung möglicherweise nicht mehr eingehalten.",
    MAINTENANCE_MODE_FAILED: "Der Wartungsmodus kann nicht aktiviert werden.",
    MAINTENANCE_MODE_FORCE_LABEL: "Erzwingen",
    MAINTENANCE_MODE_CANCEL_LABEL: "Abbrechen",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0} Server befinden sich momentan im Wartungsmodus.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Der Wartungsmodus wird auf allen Host-Servern aktiviert.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Der Wartungsmodus wird auf allen Host-Servern aktiviert. Den Status können Sie der Serveransicht entnehmen.",

    SERVER_API_DOCMENTATION: "Server-API-Definition anzeigen",

    // objectView title
    TITLE_FOR_CLUSTER: "Cluster {0}", // cluster name
    TITLE_FOR_HOST: "Host {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Verbundcontroller",
    LIBERTY_SERVER : "Liberty-Server",
    NODEJS_SERVER : "Node.js-Server",
    CONTAINER : "Container",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Liberty-Server in einem Docker-Container",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Node.js-Server in einem Docker-Container",
    RUNTIME_LIBERTY : "Liberty-Laufzeitumgebung",
    RUNTIME_NODEJS : "Node.js-Laufzeitumgebung",
    RUNTIME_DOCKER : "Laufzeit in einem Docker-Container"

});
