/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
      ACCOUNTING_STRING : "Łańcuch rozliczeniowy",
      SEARCH_RESOURCE_TYPE_ALL : "Wszystkie",
      SEARCH : "Szukaj",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Wprowadź kryteria wyszukiwania, wybierając przycisk Dodaj kryteria wyszukiwania, a następnie określając wartość",
      SUBMITTED : "Wprowadzono",
      JMS_QUEUED : "W kolejce JMS",
      JMS_CONSUMED : "Skonsumowano w JMS",
      JOB_PARAMETER : "Parametr zadania",
      DISPATCHED : "Rozesłano",
      FAILED : "Niepowodzenie",
      STOPPED : "Zatrzymane",
      COMPLETED : "Zakończone",
      ABANDONED : "Porzucono",
      STARTED : "Uruchomiono",
      STARTING : "Uruchamianie",
      STOPPING : "Zatrzymywanie",
      REFRESH : "Odśwież",
      INSTANCE_STATE : "Stan instancji",
      APPLICATION_NAME : "Nazwa aplikacji",
      APPLICATION: "Aplikacja",
      INSTANCE_ID : "Identyfikator instancji",
      LAST_UPDATE : "Ostatnia aktualizacja",
      LAST_UPDATE_RANGE : "Zakres ostatniej aktualizacji",
      LAST_UPDATED_TIME : "Czas ostatniej aktualizacji",
      DASHBOARD_VIEW : "Widok panelu kontrolnego",
      HOMEPAGE : "Strona główna",
      JOBLOGS : "Dzienniki zadań",
      QUEUED : "W kolejce",
      ENDED : "Zakończono",
      ERROR : "Błąd",
      CLOSE : "Zamknij",
      WARNING : "Ostrzeżenie",
      GO_TO_DASHBOARD: "Idź do panelu kontrolnego",
      DASHBOARD : "Panel kontrolny",
      BATCH_JOB_NAME: "Nazwa zadania wsadowego",
      SUBMITTER: "Wprowadzający",
      BATCH_STATUS: "Status zadania wsadowego",
      EXECUTION_ID: "Identyfikator wykonania zadania",
      EXIT_STATUS: "Status wyjścia",
      CREATE_TIME: "Czas utworzenia",
      START_TIME: "Czas rozpoczęcia",
      END_TIME: "Czas zakończenia",
      SERVER: "Serwer",
      SERVER_NAME: "Nazwa serwera",
      SERVER_USER_DIRECTORY: "Katalog użytkowników",
      SERVERS_USER_DIRECTORY: "Katalog użytkowników serwera",
      HOST: "Host",
      NAME: "Nazwa",
      JOB_PARAMETERS: "Parametry zadania",
      JES_JOB_NAME: "Nazwa zadania JES",
      JES_JOB_ID: "Identyfikator zadania JES",
      ACTIONS: "Działania",
      VIEW_LOG_FILE: "Wyświetl plik dziennika",
      STEP_NAME: "Nazwa kroku",
      ID: "Identyfikator",
      PARTITION_ID: "Partycja {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Wyświetl szczegóły wykonania zadania {0}",    // Job Execution ID number
      PARENT_DETAILS: "Szczegóły informacji nadrzędnej",
      TIMES: "Czasy",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Status",
      SEARCH_ON: "Wybierz, aby filtrować {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Wprowadź kryteria wyszukiwania. ",
      BREADCRUMB_JOB_INSTANCE : "Instancja zadania {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Wykonanie zadania {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Dziennik zadania {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "Kryteria wyszukiwania nie są poprawne.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "Kryteria wyszukiwania nie mogą obejmować więcej niż jednego parametru Filtruj wg {0}.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Tabela instancji zadań",
      EXECUTIONS_TABLE_IDENTIFIER: "Tabela wykonań zadań",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Tabela szczegółów kroków",
      LOADING_VIEW : "Trwa ładowanie informacji na stronę",
      LOADING_VIEW_TITLE : "Ładowanie widoku",
      LOADING_GRID : "Oczekiwanie na zwrócenie wyników wyszukiwania z serwera",
      PAGENUMBER : "Numer strony",
      SELECT_QUERY_SIZE: "Wybierz wielkość zapytania",
      LINK_EXPLORE_HOST: "Wybierz, aby wyświetlić szczegóły hosta {0} w narzędziu eksploracji.",      // Host name
      LINK_EXPLORE_SERVER: "Wybierz, aby wyświetlić szczegóły serwera {0} w narzędziu eksploracji.",  // Server name

      //ACTIONS
      RESTART: "Restartuj",
      STOP: "Zatrzymaj",
      PURGE: "Wyczyść",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Akcje dla instancji zadania {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Menu akcji instancji zadania",

      RESTART_INSTANCE_MESSAGE: "Czy zrestartować ostatnie wykonanie zadania powiązane z instancją zadania {0}?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Czy zatrzymać ostatnie wykonanie zadania powiązane z instancją zadania {0}?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Czy wyczyścić wszystkie pozycje bazy danych i dzienniki zadań powiązane z instancją zadania {0}?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Wyczyść tylko składnicę zadań",

      RESTART_INST_ERROR_MESSAGE: "Żądanie restartu nie powiodło się.",
      STOP_INST_ERROR_MESSAGE: "Żądanie zatrzymania nie powiodło się.",
      PURGE_INST_ERROR_MESSAGE: "Żądanie czyszczenia nie powiodło się.",
      ACTION_REQUEST_ERROR_MESSAGE: "Żądanie wykonania akcji nie powiodło się. Kod statusu: {0}.  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Ponownie wykorzystaj parametry z poprzedniego wykonania",
      JOB_PARAMETERS_EMPTY: "Jeśli opcja '{0}' nie jest wybrana, użyj tego obszaru w celu wprowadzenia parametrów zadania.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Nazwa parametru",
      JOB_PARAMETER_VALUE: "Wartość parametru",
      PARM_NAME_COLUMN_HEADER: "Parametr",
      PARM_VALUE_COLUMN_HEADER: "Wartość",
      PARM_ADD_ICON_TITLE: "Dodaj parametr",
      PARM_REMOVE_ICON_TITLE: "Usuń parametr",
      PARMS_ENTRY_ERROR: "Nazwa parametru jest wymagana.",
      JOB_PARAMETER_CREATE: "Wybierz {0}, aby dodać parametry do następnego wykonania tej instancji zadania.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Przycisk Dodaj parametr w nagłówku tabeli.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Zawartość dziennika zadań",
      FILE_DOWNLOAD : "Pobieranie pliku",
      DOWNLOAD_DIALOG_DESCRIPTION : "Czy pobrać pliki dziennika?",
      INCLUDE_ALL_LOGS : "Uwzględnij wszystkie pliki dzienników wykonania zadań",
      LOGS_NAVIGATION_BAR : "Pasek nawigacyjny dzienników zadań",
      DOWNLOAD : "Pobierz",
      LOG_TOP : "Początek dzienników",
      LOG_END : "Koniec dzienników",
      PREVIOUS_PAGE : "Poprzednia strona",
      NEXT_PAGE : "Następna strona",
      DOWNLOAD_ARIA : "Pobierz plik",

      //Error messages for popups
      REST_CALL_FAILED : "Wywołanie pobrania danych nie powiodło się.",
      NO_JOB_EXECUTION_URL : "W adresie URL nie podano numeru wykonania zadania lub instancja nie zawiera żadnych dzienników wykonania zadań do wyświetlenia.",
      NO_VIEW : "Błąd adresu URL: taki widok nie istnieje.",
      WRONG_TOOL_ID : "Łańcuch zapytania w adresie URL nie rozpoczyna się od identyfikatora narzędzia {0}, lecz od: {1}.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "Błąd adresu URL: nie istnieją żadne dzienniki.",
      NOT_A_NUMBER : "Błąd adresu URL: {0} musi być liczbą.",                                                // {0} is a field name
      PARAMETER_REPETITION : "Błąd adresu URL: {0} może występować tylko jeden raz w parametrach.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "Błąd adresu URL: parametr page jest spoza zakresu.",
      INVALID_PARAMETER : "Błąd adresu URL: {0} nie jest poprawnym parametrem.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "Błąd adresu URL: adres URL może określać albo wykonanie zadania, albo instancję zadania, ale nie oba te elementy jednocześnie.",
      MISSING_EXECUTION_ID_PARAM : "Brak wymaganego parametru identyfikatora wykonania.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Do korzystania z narzędzia Java Batch wymagana jest konfiguracja trwałej bazy danych przetwarzania zadań wsadowych języka Java.",
      IGNORED_SEARCH_CRITERIA : "Następujące kryteria filtrowania zostały zignorowane w wynikach: {0}",

      GRIDX_SUMMARY_TEXT : "Wyświetlanie ${0} najnowszych instancji zadań"

});
