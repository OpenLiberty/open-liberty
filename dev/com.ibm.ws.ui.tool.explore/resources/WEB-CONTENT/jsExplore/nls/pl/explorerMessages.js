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
    EXPLORER : "Eksplorator",
    EXPLORE : "Eksploracja",
    DASHBOARD : "Panel kontrolny",
    DASHBOARD_VIEW_ALL_APPS : "Wyświetl wszystkie aplikacje",
    DASHBOARD_VIEW_ALL_SERVERS : "Wyświetl wszystkie serwery",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Wyświetl wszystkie klastry",
    DASHBOARD_VIEW_ALL_HOSTS : "Wyświetl wszystkie hosty",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Wyświetl wszystkie środowiska wykonawcze",
    SEARCH : "Szukaj",
    SEARCH_RECENT : "Ostatnie operacje wyszukiwania",
    SEARCH_RESOURCES : "Wyszukaj zasoby",
    SEARCH_RESULTS : "Wyniki wyszukiwania",
    SEARCH_NO_RESULTS : "Brak wyników",
    SEARCH_NO_MATCHES : "Brak dopasowań",
    SEARCH_TEXT_INVALID : "Tekst wyszukiwany zawiera niepoprawne znaki",
    SEARCH_CRITERIA_INVALID : "Kryteria wyszukiwania nie są poprawne.",
    SEARCH_CRITERIA_INVALID_COMBO :"Wartość {0} nie jest poprawna, jeśli jest podawana z wartością {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "Podaj wartość {0} tylko raz.",
    SEARCH_TEXT_MISSING : "Wymagany jest tekst wyszukiwany",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "Wyszukiwanie znaczników aplikacji na serwerze nie jest obsługiwanie.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "Wyszukiwanie znaczników aplikacji w klastrze nie jest obsługiwanie.",
    SEARCH_UNSUPPORT : "Kryteria wyszukiwania nie są obsługiwane.",
    SEARCH_SWITCH_VIEW : "Przełącz widok",
    FILTERS : "Filtry",
    DEPLOY_SERVER_PACKAGE : "Wdróż pakiet serwera",
    MEMBER_OF : "Element klastra",
    N_CLUSTERS: "Klastry: {0}",

    INSTANCE : "Instancja",
    INSTANCES : "Instancje",
    APPLICATION : "Aplikacja",
    APPLICATIONS : "Aplikacje",
    SERVER : "Serwer",
    SERVERS : "Serwery",
    CLUSTER : "Klaster",
    CLUSTERS : "Klastry",
    CLUSTER_NAME : "Nazwa klastra: ",
    CLUSTER_STATUS : "Status klastra: ",
    APPLICATION_NAME : "Nazwa aplikacji: ",
    APPLICATION_STATE : "Stan aplikacji: ",
    HOST : "Host",
    HOSTS : "Hosty",
    RUNTIME : "Środowisko wykonawcze",
    RUNTIMES : "Środowiska wykonawcze",
    PATH : "Ścieżka",
    CONTROLLER : "Kontroler",
    CONTROLLERS : "Kontrolery",
    OVERVIEW : "Przegląd",
    CONFIGURE : "Konfiguruj",

    SEARCH_RESOURCE_TYPE: "Typ", // Search by resource types
    SEARCH_RESOURCE_STATE: "Województwo", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Wszystkie", // Search all resource types
    SEARCH_RESOURCE_NAME: "Nazwa", // Search by resource name
    SEARCH_RESOURCE_TAG: "Znacznik", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Kontener", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Brak", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Typ środowiska wykonawczego", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Właściciel", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Kontakt", // Search by contact
    SEARCH_RESOURCE_NOTE: "Uwaga", // Search by note

    GRID_HEADER_USERDIR : "Katalog użytkowników",
    GRID_HEADER_NAME : "Nazwa",
    GRID_LOCATION_NAME : "Położenie",
    GRID_ACTIONS : "Działania siatki",
    GRID_ACTIONS_LABEL : "Działania siatki {0}",  // name of the grid
    APPONSERVER_LOCATION_NAME : "Serwer {0} na hoście {1} ({2})", // server on host (/path)

    STATS : "Monitorujący",
    STATS_ALL : "Wszystkie",
    STATS_VALUE : "Wartość: {0}",
    CONNECTION_IN_USE_STATS : "{0} używane = {1}, zarządzane - {2} wolne",
    CONNECTION_IN_USE_STATS_VALUE : "Wartość: {0} używane = {1}, zarządzane - {2} wolne",
    DATA_SOURCE : "Źródło danych: {0}",
    STATS_DISPLAY_LEGEND : "Pokaż legendę",
    STATS_HIDE_LEGEND : "Ukryj legendę",
    STATS_VIEW_DATA : "Wyświetl dane wykresu",
    STATS_VIEW_DATA_TIMESTAMP : "Znacznik czasu",
    STATS_ACTION_MENU : "Menu działań {0}",
    STATS_SHOW_HIDE : "Dodaj pomiary zasobów",
    STATS_SHOW_HIDE_SUMMARY : "Dodaj pomiary podsumowania",
    STATS_SHOW_HIDE_TRAFFIC : "Dodaj pomiary ruchu",
    STATS_SHOW_HIDE_PERFORMANCE : "Dodaj pomiary wydajności",
    STATS_SHOW_HIDE_AVAILABILITY : "Dodaj pomiary dostępności",
    STATS_SHOW_HIDE_ALERT : "Dodaj pomiary alertów",
    STATS_SHOW_HIDE_LIST_BUTTON : "Pokaż lub ukryj listę pomiarów zasobów",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Edytuj wykresy",
    STATS_SHOW_HIDE_CONFIRM : "Zapisz",
    STATS_SHOW_HIDE_CANCEL : "Anuluj",
    STATS_SHOW_HIDE_DONE : "Gotowe",
    STATS_DELETE_GRAPH : "Usuń wykres",
    STATS_ADD_CHART_LABEL : "Dodaj wykres do widoku",
    STATS_JVM_TITLE : "Maszyna JVM",
    STATS_JVM_BUTTON_LABEL : "Dodaj wszystkie wykresy maszyny JVM do widoku",
    STATS_HEAP_TITLE : "Użyta pamięć sterty",
    STATS_HEAP_USED : "Użyto: {0} MB",
    STATS_HEAP_COMMITTED : "Zatwierdzono: {0} MB",
    STATS_HEAP_MAX : "Maksymalnie: {0} MB",
    STATS_HEAP_X_TIME : "Czas",
    STATS_HEAP_Y_MB : "Użyta ilość MB",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "Załadowane klasy",
    STATS_CLASSES_LOADED : "Załadowano: {0}",
    STATS_CLASSES_UNLOADED : "Usunięto z pamięci: {0}",
    STATS_CLASSES_TOTAL : "Łącznie: {0}",
    STATS_CLASSES_Y_TOTAL : "Załadowane klasy",
    STATS_PROCESSCPU_TITLE : "Użycie procesora",
    STATS_PROCESSCPU_USAGE : "Użycie procesora: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "Procent użycia procesora",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Aktywne wątki maszyny JVM",
    STATS_LIVE_MSG_INIT : "Wyświetlanie aktualnych danych",
    STATS_LIVE_MSG :"Ten wykres nie zawiera danych historycznych. Nadal będą na nim prezentowane dane z ostatnich 10 minut.",
    STATS_THREADS_ACTIVE : "Działające: {0}",
    STATS_THREADS_PEAK : "Szczyt: {0}",
    STATS_THREADS_TOTAL : "Łącznie: {0}",
    STATS_THREADS_Y_THREADS : "Wątki",
    STATS_TP_POOL_SIZE : "Wielkość puli",
    STATS_JAXWS_TITLE : "Usługi WWW JAX-WS",
    STATS_JAXWS_BUTTON_LABEL : "Dodaj wszystkie wykresy usług WWW JAX-WS do widoku",
    STATS_JW_AVG_RESP_TIME : "Średni czas odpowiedzi",
    STATS_JW_AVG_INVCOUNT : "Średnia liczba wywołań",
    STATS_JW_TOTAL_FAULTS : "Łączna liczba błędów wykonywania",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Wybierz zasoby...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "Zasoby: {0}",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 zasób",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "Należy wybrać co najmniej jeden zasób.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "Brak dostępnych danych dla wybranego okresu.",
    STATS_ACCESS_LOG_TITLE : "Dziennik dostępu",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Dodaj wszystkie wykresy dziennika dostępu do widoku",
    STATS_ACCESS_LOG_GRAPH : "Licznik komunikatów dziennika dostępu",
    STATS_ACCESS_LOG_SUMMARY : "Podsumowanie dziennika dostępu",
    STATS_ACCESS_LOG_TABLE : "Lista komunikatów dziennika dostępu",
    STATS_MESSAGES_TITLE : "Komunikaty i śledzenie",
    STATS_MESSAGES_BUTTON_LABEL : "Dodaj wszystkie wykresy komunikatów i śledzenia do widoku",
    STATS_MESSAGES_GRAPH : "Licznik komunikatów dziennika",
    STATS_MESSAGES_TABLE : "Lista komunikatów dziennika",
    STATS_FFDC_GRAPH : "Liczba FFDC",
    STATS_FFDC_TABLE : "Lista FFDC",
    STATS_TRACE_LOG_GRAPH : "Licznik komunikatów śledzenia",
    STATS_TRACE_LOG_TABLE : "Lista komunikatów śledzenia",
    STATS_THREAD_POOL_TITLE : "Pula wątków",
    STATS_THREAD_POOL_BUTTON_LABEL : "Dodaj wszystkie wykresy puli wątków do widoku",
    STATS_THREADPOOL_TITLE : "Aktywne wątki profilu Liberty",
    STATS_THREADPOOL_SIZE : "Wielkość puli: {0}",
    STATS_THREADPOOL_ACTIVE : "Aktywne: {0}",
    STATS_THREADPOOL_TOTAL : "Łącznie: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Aktywne wątki",
    STATS_SESSION_MGMT_TITLE : "Sesje",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Dodaj wszystkie wykresy sesji do widoku",
    STATS_SESSION_CONFIG_LABEL : "Wybierz sesje...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "Liczba sesji: {0}",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 sesja",
    STATS_SESSION_CONFIG_SELECT_ONE : "Należy wybrać co najmniej jedną sesję.",
    STATS_SESSION_TITLE : "Aktywne sesje",
    STATS_SESSION_Y_ACTIVE : "Aktywne sesje",
    STATS_SESSION_LIVE_LABEL : "Liczba działających: {0}",
    STATS_SESSION_CREATE_LABEL : "Liczba utworzonych: {0}",
    STATS_SESSION_INV_LABEL : "Liczba unieważnionych: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Liczba unieważnionych wg limitu czasu: {0}",
    STATS_WEBCONTAINER_TITLE : "Aplikacje WWW",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Dodaj wszystkie wykresy aplikacji WWW do widoku",
    STATS_SERVLET_CONFIG_LABEL : "Wybierz serwlety...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "Liczba serwletów: {0}",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 serwlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "Należy wybrać co najmniej jeden serwlet.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Liczba żądań",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Liczba żądań",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Liczba odpowiedzi",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Liczba odpowiedzi",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Średni czas odpowiedzi (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Czas odpowiedzi (ns)",
    STATS_CONN_POOL_TITLE : "Pula połączeń",
    STATS_CONN_POOL_BUTTON_LABEL : "Dodaj wszystkie wykresy puli połączeń do widoku",
    STATS_CONN_POOL_CONFIG_LABEL : "Wybierz źródła danych...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "Liczba źródeł danych: {0}",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 źródło danych",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "Należy wybrać co najmniej jedno źródło danych.",
    STATS_CONNECT_IN_USE_TITLE : "Używane połączenia",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Połączenia",
    STATS_CONNECT_IN_USE_LABEL : "Używane: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Użyte: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Wolne: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Utworzone: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Zniszczone: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Średni czas oczekiwania (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Czas oczekiwania (ms)",
    STATS_TIME_ALL : "Wszystkie",
    STATS_TIME_1YEAR : "1r",
    STATS_TIME_1MONTH : "1ms",
    STATS_TIME_1WEEK : "1t",
    STATS_TIME_1DAY : "1d",
    STATS_TIME_1HOUR : "1g",
    STATS_TIME_10MINUTES : "10m",
    STATS_TIME_5MINUTES : "5m",
    STATS_TIME_1MINUTE : "1m",
    STATS_PERSPECTIVE_SUMMARY : "Podsumowanie",
    STATS_PERSPECTIVE_TRAFFIC : "Ruch",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "Ruch związany z maszyną JVM",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Ruch związany z połączeniem",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Ruch związany z dziennikiem dostępu",
    STATS_PERSPECTIVE_PROBLEM : "Problem",
    STATS_PERSPECTIVE_PERFORMANCE : "Wydajność",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "Wydajność maszyny JVM",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Wydajność połączenia",
    STATS_PERSPECTIVE_ALERT : "Analiza alertów",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Alert w dzienniku dostępu",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Alert w dzienniku śledzenia i komunikatów",
    STATS_PERSPECTIVE_AVAILABILITY : "Dostępność",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Ostatnia minuta",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "Ostatnie 5 minut",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "Ostatnie 10 minut",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Ostatnia godzina",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Ostatni dzień",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Ostatni tydzień",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Ostatni miesiąc",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Ostatni rok",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Ostatnie {0} s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Ostatnie {0} min",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Ostatnie {0} min {1} s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Ostatnie {0} godz.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Ostatnie {0} godz. {1} min",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Ostatnie {0} d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Ostatnie {0} d {1} godz.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Ostatnie {0} tyg.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Ostatnie {0} tyg. {1} d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Ostatnie {0} mies.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Ostatnie {0} mies. {1} d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Ostanie {0} lat",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Ostatnie {0} lat {1} mies.",

    STATS_LIVE_UPDATE_LABEL: "Aktualizowanie aktywne",
    STATS_TIME_SELECTOR_NOW_LABEL: "Teraz",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Komunikaty dziennika",

    AUTOSCALED_APPLICATION : "Aplikacja skalowana automatycznie",
    AUTOSCALED_SERVER : "Serwer skalowany automatycznie",
    AUTOSCALED_CLUSTER : "Klaster skalowany automatycznie",
    AUTOSCALED_POLICY : "Strategia automatycznego skalowania",
    AUTOSCALED_POLICY_DISABLED : "Strategia automatycznego skalowania została wyłączona",
    AUTOSCALED_NOACTIONS : "Brak dostępnych działań dla zasobów skalowanych automatycznie",

    START : "Uruchom",
    START_CLEAN : "Uruchom z parametrem --clean",
    STARTING : "Uruchamianie",
    STARTED : "Uruchomiono",
    RUNNING : "Uruchomione",
    NUM_RUNNING: "Liczba działających: {0}",
    PARTIALLY_STARTED : "Częściowo uruchomiony",
    PARTIALLY_RUNNING : "Częściowo uruchomione",
    NOT_STARTED : "Nieuruchomiony",
    STOP : "Zatrzymaj",
    STOPPING : "Zatrzymywanie",
    STOPPED : "Zatrzymane",
    NUM_STOPPED : "Liczba zatrzymanych {0}",
    NOT_RUNNING : "Nieuruchomione",
    RESTART : "Restartuj",
    RESTARTING : "Restartowanie",
    RESTARTED : "Zrestartowany",
    ALERT : "Alert",
    ALERTS : "Alerty",
    UNKNOWN : "Nieznany",
    NUM_UNKNOWN : "Liczba nieznanych: {0}",
    SELECT : "Wybierz",
    SELECTED : "Wybrano",
    SELECT_ALL : "Wybierz wszystko",
    SELECT_NONE : "Anuluj wybór wszystkiego",
    DESELECT: "Anuluj wybór",
    DESELECT_ALL : "Anuluj wybór wszystkiego",
    TOTAL : "Łącznie",
    UTILIZATION : "Wykorzystanie przekracza {0}%", // percent

    ELLIPSIS_ARIA: "Rozwiń, aby wyświetlić więcej opcji.",
    EXPAND : "Rozwiń",
    COLLAPSE: "Zwiń",

    ALL : "Wszystkie",
    ALL_APPS : "Wszystkie aplikacje",
    ALL_SERVERS : "Wszystkie serwery",
    ALL_CLUSTERS : "Wszystkie klastry",
    ALL_HOSTS : "Wszystkie hosty",
    ALL_APP_INSTANCES : "Wszystkie instancje aplikacji",
    ALL_RUNTIMES : "Wszystkie środowiska wykonawcze",

    ALL_APPS_RUNNING : "Wszystkie aplikacje działają",
    ALL_SERVER_RUNNING : "Wszystkie serwery działają",
    ALL_CLUSTERS_RUNNING : "Wszystkie klastry działają",
    ALL_APPS_STOPPED : "Wszystkie aplikacje zostały zatrzymane",
    ALL_SERVER_STOPPED : "Wszystkie serwery zostały zatrzymane",
    ALL_CLUSTERS_STOPPED : "Wszystkie klastry zostały zatrzymane",
    ALL_SERVERS_UNKNOWN : "Wszystkie serwery mają nieznany stan",
    SOME_APPS_RUNNING : "Niektóre aplikacje działają",
    SOME_SERVERS_RUNNING : "Niektóre serwery działają",
    SOME_CLUSTERS_RUNNING : "Niektóre klastry działają",
    NO_APPS_RUNNING : "Nie działają żadne aplikacje",
    NO_SERVERS_RUNNING : "Nie działają żadne serwery",
    NO_CLUSTERS_RUNNING : "Nie działają żadne klastry",

    HOST_WITH_ALL_SERVERS_RUNNING: "Hosty z uruchomionymi wszystkimi serwerami", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Hosty z uruchomionymi niektórymi serwerami",
    HOST_WITH_NO_SERVERS_RUNNING: "Hosty bez uruchomionych serwerów", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Hosty, na których wszystkie serwery zostały zatrzymane",
    HOST_WITH_SERVERS_RUNNING: "Hosty, na których serwery działają",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Środowiska wykonawcze, w których uruchomiono niektóre serwery",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Środowiska wykonawcze, w których wszystkie serwery zostały zatrzymane",
    RUNTIME_WITH_SERVERS_RUNNING: "Środowiska wykonawcze, w których uruchomiono serwery",

    START_ALL_APPS : "Czy uruchomić wszystkie aplikacje?",
    START_ALL_INSTANCES : "Czy uruchomić wszystkie instancje aplikacji?",
    START_ALL_SERVERS : "Czy uruchomić wszystkie serwery?",
    START_ALL_CLUSTERS : "Czy uruchomić wszystkie klastry?",
    STOP_ALL_APPS : "Czy zatrzymać wszystkie aplikacje?",
    STOPE_ALL_INSTANCES : "Czy zatrzymać wszystkie instancje aplikacji?",
    STOP_ALL_SERVERS : "Czy zatrzymać wszystkie serwery?",
    STOP_ALL_CLUSTERS : "Czy zatrzymać wszystkie klastry?",
    RESTART_ALL_APPS : "Czy zrestartować wszystkie aplikacje?",
    RESTART_ALL_INSTANCES : "Czy zrestartować wszystkie instancje aplikacji?",
    RESTART_ALL_SERVERS : "Czy zrestartować wszystkie serwery?",
    RESTART_ALL_CLUSTERS : "Czy zrestartować wszystkie klastry?",

    START_INSTANCE : "Czy uruchomić instancję aplikacji?",
    STOP_INSTANCE : "Czy zatrzymać instancję aplikacji?",
    RESTART_INSTANCE : "Czy zrestartować instancję aplikacji?",

    START_SERVER : "Czy uruchomić serwer {0}?",
    STOP_SERVER : "Czy zatrzymać serwer {0}?",
    RESTART_SERVER : "Czy zrestartować serwer {0}?",

    START_ALL_INSTS_OF_APP : "Czy uruchomić wszystkie instancje aplikacji {0}?", // application name
    START_APP_ON_SERVER : "Czy uruchomić aplikację {0} na serwerze {1}?", // app name, server name
    START_ALL_APPS_WITHIN : "Czy uruchomić wszystkie aplikacje w zasobie {0}?", // resource
    START_ALL_APP_INSTS_WITHIN : "Czy uruchomić wszystkie instancje aplikacji w zasobie {0}?", // resource
    START_ALL_SERVERS_WITHIN : "Czy uruchomić wszystkie serwery w zasobie {0}?", // resource
    STOP_ALL_INSTS_OF_APP : "Czy zatrzymać wszystkie instancje aplikacji {0}?", // application name
    STOP_APP_ON_SERVER : "Czy zatrzymać aplikację {0} na serwerze {1}?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Czy zatrzymać wszystkie aplikacje w zasobie {0}?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Czy zatrzymać wszystkie instancje aplikacji w zasobie {0}?", // resource
    STOP_ALL_SERVERS_WITHIN : "Czy zatrzymać wszystkie serwery w zasobie {0}?", // resource
    RESTART_ALL_INSTS_OF_APP : "Czy zrestartować wszystkie instancje aplikacji {0}?", // application name
    RESTART_APP_ON_SERVER : "Czy zrestartować aplikację {0} na serwerze {1}?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Czy zrestartować wszystkie aplikacje w zasobie {0}?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Czy zrestartować wszystkie instancje aplikacji w zasobie {0}?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Czy zrestartować wszystkie działające serwery w zasobie {0}?", // resource

    START_SELECTED_APPS : "Czy uruchomić wszystkie instancje wybranych aplikacji?",
    START_SELECTED_INSTANCES : "Czy uruchomić wybrane instancje aplikacji?",
    START_SELECTED_SERVERS : "Czy uruchomić wybrane serwery?",
    START_SELECTED_SERVERS_LABEL : "Uruchom wybrane serwery",
    START_SELECTED_CLUSTERS : "Czy uruchomić wybrane klastry?",
    START_CLEAN_SELECTED_SERVERS : "Czy wykonać komendę start --clean dla wybranych serwerów?",
    START_CLEAN_SELECTED_CLUSTERS : "Czy wykonać komendę start --clean dla wybranych klastrów?",
    STOP_SELECTED_APPS : "Czy zatrzymać wszystkie instancje wybranych aplikacji?",
    STOP_SELECTED_INSTANCES : "Czy zatrzymać wybrane instancje aplikacji?",
    STOP_SELECTED_SERVERS : "Czy zatrzymać wybrane serwery?",
    STOP_SELECTED_CLUSTERS : "Czy zatrzymać wybrane klastry?",
    RESTART_SELECTED_APPS : "Czy zrestartować wszystkie instancje wybranych aplikacji?",
    RESTART_SELECTED_INSTANCES : "Czy zrestartować wybrane instancje aplikacji?",
    RESTART_SELECTED_SERVERS : "Czy zrestartować wybrane serwery?",
    RESTART_SELECTED_CLUSTERS : "Czy zrestartować wybrane klastry?",

    START_SERVERS_ON_HOSTS : "Czy uruchomić wszystkie serwery na wybranych hostach?",
    STOP_SERVERS_ON_HOSTS : "Czy zatrzymać wszystkie serwery na wybranych hostach?",
    RESTART_SERVERS_ON_HOSTS : "Czy zrestartować wszystkie uruchomione serwery na wybranych hostach?",

    SELECT_APPS_TO_START : "Wybierz zatrzymane aplikacje do uruchomienia.",
    SELECT_APPS_TO_STOP : "Wybierz uruchomione aplikacje do zatrzymania.",
    SELECT_APPS_TO_RESTART : "Wybierz uruchomione aplikacje do zrestartowania.",
    SELECT_INSTANCES_TO_START : "Wybierz zatrzymane instancje aplikacji do uruchomienia.",
    SELECT_INSTANCES_TO_STOP : "Wybierz uruchomione instancje aplikacji do zatrzymania.",
    SELECT_INSTANCES_TO_RESTART : "Wybierz uruchomione instancje aplikacji do zrestartowania.",
    SELECT_SERVERS_TO_START : "Wybierz zatrzymane serwery do uruchomienia.",
    SELECT_SERVERS_TO_STOP : "Wybierz uruchomione serwery do zatrzymania.",
    SELECT_SERVERS_TO_RESTART : "Wybierz uruchomione serwery do zrestartowania.",
    SELECT_CLUSTERS_TO_START : "Wybierz zatrzymane klastry do uruchomienia.",
    SELECT_CLUSTERS_TO_STOP : "Wybierz uruchomione klastry do zatrzymania.",
    SELECT_CLUSTERS_TO_RESTART : "Wybierz uruchomione klastry do zrestartowania.",

    STATUS : "Status",
    STATE : "Stan:",
    NAME : "Nazwa:",
    DIRECTORY : "Katalog",
    INFORMATION : "Informacja",
    DETAILS : "Szczegóły",
    ACTIONS : "Działania",
    CLOSE : "Zamknij",
    HIDE : "Ukryj",
    SHOW_ACTIONS : "Wyświetl działania",
    SHOW_SERVER_ACTIONS_LABEL : "Działania serwera: {0}",
    SHOW_APP_ACTIONS_LABEL : "Działania aplikacji: {0}",
    SHOW_CLUSTER_ACTIONS_LABEL : "Działania klastra: {0}",
    SHOW_HOST_ACTIONS_LABEL : "Działania hosta: {0}",
    SHOW_RUNTIME_ACTIONS_LABEL : "Działania środowiska wykonawczego {0}",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "Menu działań serwera {0}",
    SHOW_APP_ACTIONS_MENU_LABEL : "Menu działań aplikacji {0}",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "Menu działań klastra {0}",
    SHOW_HOST_ACTIONS_MENU_LABEL : "Menu działań hosta {0}",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "Menu działań środowiska wykonawczego {0}",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "Menu działań środowiska wykonawczego na hoście {0}",
    SHOW_COLLECTION_MENU_LABEL : "Menu działań dotyczących stanu dla kolekcji {0}",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "Menu działań dotyczących stanu dla wyszukiwania {0}",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: nieznany stan", // resourceName
    UNKNOWN_STATE_APPS : "Liczba aplikacji w nieznanym stanie: {0}", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "Liczba instancji aplikacji w nieznanym stanie: {0}", // quantity
    UNKNOWN_STATE_SERVERS : "Liczba serwerów w nieznanym stanie: {0}", // quantity
    UNKNOWN_STATE_CLUSTERS : "Liczba klastrów w nieznanym stanie: {0}", // quantity

    INSTANCES_NOT_RUNNING : "Liczba niedziałających instancji aplikacji: {0}", // quantity
    APPS_NOT_RUNNING : "Liczba niedziałających aplikacji: {0}", // quantity
    SERVERS_NOT_RUNNING : "Liczba niedziałających serwerów: {0}", // quantity
    CLUSTERS_NOT_RUNNING : "Liczba niedziałających klastrów: {0}", // quantity

    APP_STOPPED_ON_SERVER : "Aplikacja {0} została zatrzymana na działających serwerach: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "Aplikacje ({0}) zostały zatrzymane na działających serwerach: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "Liczba aplikacji zatrzymanych na działających serwerach: {0}", // quantity
    NUMBER_RESOURCES : "Zasoby: {0}", // quantity
    NUMBER_APPS : "Aplikacje: {0}", // quantity
    NUMBER_SERVERS : "Serwery: {0}", // quantity
    NUMBER_CLUSTERS : "Klastry: {0}", // quantity
    NUMBER_HOSTS : "Hosty: {0}", // quantity
    NUMBER_RUNTIMES : "Liczba środowisk wykonawczych: {0}", // quantity
    SERVERS_INSERT : "serwerach",
    INSERT_STOPPED_ON_INSERT : "{0} - zatrzymane na działających {1}", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "Serwer aplikacji {0} zatrzymano na działającym serwerze {1}", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "Serwer aplikacji {0} w klastrze {1} zatrzymano na działających serwerach: {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "Liczba instancji aplikacji zatrzymanych na działających serwerach: {0}", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: instancja aplikacji nie działa", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: nie wszystkie aplikacje działają", // serverName[]
    NO_APPS_RUNNING : "{0}: nie działają żadne aplikacje", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "Liczba serwerów, na których działają nie wszystkie aplikacje: {0}", // quantity
    NO_APPS_RUNNING_SERVERS : "Liczba serwerów, na których nie działają żadne aplikacje: {0}", // quantity

    COUNT_OF_APPS_SELECTED : "Liczba wybranych aplikacji: {0}",
    RATIO_RUNNING : "Współczynnik działających: {0}", // ratio ex. 1/2

    RESOURCES_SELECTED : "Liczba wybranych: {0}",

    NO_HOSTS_SELECTED : "Nie wybrano żadnych hostów",
    NO_DEPLOY_RESOURCE : "Brak zasobu do zainstalowania wdrożenia",
    NO_TOPOLOGY : "Brak {0}.",
    COUNT_OF_APPS_STARTED  : "Liczba uruchomionych aplikacji: {0}",

    APPS_LIST : "Aplikacje: {0}",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "Liczba działających instancji: {0}/{1}",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "Liczba działających serwerów: {0}/{1}",
    RESOURCE_ON_RESOURCE : "Zasób {0} w zasobie {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} na serwerze {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} w klastrze {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Restartowanie jest wyłączone w przypadku tego serwera, ponieważ znajduje się na nim centrum administracyjne",
    ACTION_DISABLED_FOR_USER: "Działania są wyłączone dla tego zasobu, ponieważ użytkownik nie jest autoryzowany",

    RESTART_AC_TITLE: "Brak restartowania w przypadku centrum administracyjnego",
    RESTART_AC_DESCRIPTION: "Serwer {0} udostępnia centrum administracyjne. Centrum administracyjne nie może zrestartować samo siebie.",
    RESTART_AC_MESSAGE: "Wszystkie pozostałe wybrane serwery zostaną zrestartowane.",
    RESTART_AC_CLUSTER_MESSAGE: "Wszystkie pozostałe wybrane klastry zostaną zrestartowane.",

    STOP_AC_TITLE: "Zatrzymywanie centrum administracyjnego",
    STOP_AC_DESCRIPTION: "Serwer {0} jest kontrolerem kolektywu, na którym uruchomione jest centrum administracyjne. Jego zatrzymanie może mieć wpływ na operacje zarządzania kolektywem na serwerze Liberty i spowodować niedostępność centrum administracyjnego.",
    STOP_AC_MESSAGE: "Czy zatrzymać ten kontroler?",
    STOP_STANDALONE_DESCRIPTION: "Na serwerze {0} działa Centrum administracyjne. Zatrzymanie go spowoduje, że Centrum administracyjne będzie niedostępne.",
    STOP_STANDALONE_MESSAGE: "Czy zatrzymać ten serwer?",

    STOP_CONTROLLER_TITLE: "Zatrzymywanie kontrolera",
    STOP_CONTROLLER_DESCRIPTION: "Serwer {0} jest kontrolerem kolektywu. Jego zatrzymanie może mieć wpływ na operacje kolektywu na serwerze Liberty.",
    STOP_CONTROLLER_MESSAGE: "Czy zatrzymać ten kontroler?",

    STOP_AC_CLUSTER_TITLE: "Zatrzymaj klaster {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "Klaster {0} zawiera kontroler zbiorczy, na którym działa Centrum administracyjne.  Zatrzymanie tego serwera może mieć wpływ na operacje zarządzania zbiorczego profilu Liberty i spowodować niedostępność Centrum administracyjnego.",
    STOP_AC_CLUSTER_MESSAGE: "Czy zatrzymać ten klaster?",

    INVALID_URL: "Strona nie istnieje.",
    INVALID_APPLICATION: "Aplikacja {0} nie istnieje w elemencie zbiorczym.", // application name
    INVALID_SERVER: "Serwer {0} nie istnieje w elemencie zbiorczym.", // server name
    INVALID_CLUSTER: "Klaster {0} nie istnieje w elemencie zbiorczym.", // cluster name
    INVALID_HOST: "Host {0} nie istnieje w elemencie zbiorczym.", // host name
    INVALID_RUNTIME: "Środowisko wykonawcze {0} nie istnieje w elemencie zbiorczym.", // runtime name
    INVALID_INSTANCE: "Instancja aplikacji {0} nie istnieje w elemencie zbiorczym.", // application instance name
    GO_TO_DASHBOARD: "Idź do panelu kontrolnego",
    VIEWED_RESOURCE_REMOVED: "Oj! Zasób został usunięty lub nie jest już dostępny.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "Połączenie z serwerem zostało utracone. Na tej stronie nie będą już wyświetlane dynamiczne zmiany środowiska. Odśwież stronę, aby przywrócić połączenie i aktualizacje dynamiczne.",
    ERROR_MESSAGE: "Połączenie zostało przerwane",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Zatrzymaj serwer',

    // Tags
    RELATED_RESOURCES: "Zasoby pokrewne",
    TAGS : "Znaczniki",
    TAG_BUTTON_LABEL : "Znacznik ({0})",  // tag value
    TAGS_LABEL : "Wprowadź znaczniki rozdzielone przecinkami, spacjami, znakami Enter lub Tab.",
    OWNER : "Właściciel",
    OWNER_BUTTON_LABEL : "Właściciel ({0})",  // owner value
    CONTACTS : "Osoby kontaktowe",
    CONTACT_BUTTON_LABEL : "Osoba kontaktowa ({0})",  // contact value
    PORTS : "Porty",
    CONTEXT_ROOT : "Kontekstowy katalog główny",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Więcej",  // alt text for the ... button
    MORE_BUTTON_MENU : "Menu przycisku Więcej {0}", // alt text for the menu
    NOTES: "Uwagi",
    NOTE_LABEL : "Uwagi ({0})",  // note value
    SET_ATTRIBUTES: "Znaczniki i metadane",
    SETATTR_RUNTIME_NAME: "Zasób {0} w zasobie {1}",  // runtime, host
    SAVE: "Zapisz",
    TAGINVALIDCHARS: "Znaki '/', '<' i '>' nie są poprawne.",
    ERROR_GET_TAGS_METADATA: "Produkt nie może pobrać bieżących znaczników i metadanych dla tego zasobu.",
    ERROR_SET_TAGS_METADATA: "Błąd uniemożliwił produktowi ustawienie znaczników i metadanych.",
    METADATA_WILLBE_INHERITED: "Metadane są ustawiane w aplikacji i udostępniane do współużytkowania we wszystkich instancjach w klastrze.",
    ERROR_ALT: "Błąd",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Aktualne statystyki nie są dostępne dla tego serwera, ponieważ został on zatrzymany. Uruchom serwer, aby rozpocząć jego monitorowanie.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Aktualne statystyki nie są dostępne dla tej aplikacji, ponieważ powiązany z nią serwer został zatrzymany. Uruchom serwer, aby rozpocząć monitorowanie aplikacji.",
    GRAPH_FEATURES_NOT_CONFIGURED: "W tym miejscu jeszcze nic nie ma! Aby monitorować ten zasób, kliknij ikonę Edytuj i dodaj pomiary.",
    NO_GRAPHS_AVAILABLE: "Brak dostępnych pomiarów do dodania. Spróbuj zainstalować dodatkowe funkcje monitorowania, aby udostępnić więcej pomiarów.",
    NO_APPS_GRAPHS_AVAILABLE: "Brak dostępnych pomiarów do dodania. Spróbuj zainstalować dodatkowe funkcje monitorowania, aby udostępnić więcej pomiarów. Sprawdź także, czy aplikacja jest używana.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Niezapisane zmiany",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "Istnieją niezapisane zmiany. Jeśli przejdziesz na inną stronę, utracisz zmiany.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Czy zapisać wprowadzone zmiany?",

    NO_CPU_STATS_AVAILABLE : "Statystyki użycia procesora nie są dostępne dla tego serwera.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "Aby włączyć ten widok, zainstaluj narzędzie do konfigurowania serwera.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Czy zapisać zmiany w pliku {0} przed zamknięciem?",
    SAVE: "Zapisz",
    DONT_SAVE: "Nie zapisuj",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Włącz tryb konserwacji",
    DISABLE_MAINTENANCE_MODE: "Wyłącz tryb konserwacji",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Włączanie trybu konserwacji",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Wyłączanie tryb konserwacji",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Włącz tryb konserwacji na hoście i wszystkich jego serwerach (liczba serwerów: {0})",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Włącz tryb konserwacji na hostach i wszystkich jego serwerach (liczba serwerów: {0})",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Włącz tryb konserwacji na tym serwerze",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Włącz tryb konserwacji na serwerach",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Wyłącz tryb konserwacji na hoście i wszystkich jego serwerach (liczba serwerów: {0})",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Wyłącz tryb konserwacji na tym serwerze",
    BREAK_AFFINITY_LABEL: "Przerwij powinowactwo z aktywnymi sesjami",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Włącz",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Wyłącz",
    MAINTENANCE_MODE: "Tryb konserwacji",
    ENABLING_MAINTENANCE_MODE: "Włączanie trybu konserwacji",
    MAINTENANCE_MODE_ENABLED: "Tryb konserwacji został włączony",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "Tryb konserwacji nie został włączony, ponieważ serwery alternatywne nie zostały uruchomione.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Wybierz opcję Wymuś, aby włączyć tryb konserwacji bez włączania serwerów alternatywnych. Wymuszenie może naruszyć strategie automatycznego skalowania.",
    MAINTENANCE_MODE_FAILED: "Nie można włączyć trybu konserwacji.",
    MAINTENANCE_MODE_FORCE_LABEL: "Wymuś",
    MAINTENANCE_MODE_CANCEL_LABEL: "Anuluj",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "Liczba serwerów działających aktualnie w trybie konserwacji: {0}.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Włączanie trybu konserwacji na wszystkich serwerach hosta.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Włączanie trybu konserwacji na wszystkich serwerach hosta.  Sprawdź status, wyświetlając widok Serwery.",

    SERVER_API_DOCMENTATION: "Wyświetl definicję interfejsu API serwera",

    // objectView title
    TITLE_FOR_CLUSTER: "Klaster {0}", // cluster name
    TITLE_FOR_HOST: "Host {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Kontroler kolektywu",
    LIBERTY_SERVER : "Serwer Liberty",
    NODEJS_SERVER : "Serwer Node.js",
    CONTAINER : "Kontener",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Serwer Liberty w kontenerze Docker",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Serwer Node.js w kontenerze Docker",
    RUNTIME_LIBERTY : "Środowisko wykonawcze Liberty",
    RUNTIME_NODEJS : "Środowisko wykonawcze Node.js",
    RUNTIME_DOCKER : "Środowisko wykonawcze w kontenerze Docker"

});
