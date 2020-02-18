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
      ACCOUNTING_STRING : "Łańcuch rozliczeniowy",
      SEARCH_RESOURCE_TYPE_ALL : "Wszystkie",
      SEARCH : "Szukaj",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Wprowadź kryteria wyszukiwania, klikając przycisk Dodaj kryteria wyszukiwania i podając wartość",
      SUBMITTED : "Wprowadzone",
      JMS_QUEUED : "W kolejce JMS",
      JMS_CONSUMED : "Pobrane z kolejki JMS",
      JOB_PARAMETER : "Parametr zadania",
      DISPATCHED : "Rozesłane",
      FAILED : "Zakończone niepowodzeniem",
      STOPPED : "Zatrzymane",
      COMPLETED : "Wykonane",
      ABANDONED : "Porzucone",
      STARTED : "Uruchomione",
      STARTING : "Uruchamianie",
      STOPPING : "Zatrzymywanie",
      REFRESH : "Odśwież",
      INSTANCE_STATE : "Stan instancji",
      APPLICATION_NAME : "Nazwa aplikacji",
      APPLICATION: "Aplikacja",
      INSTANCE_ID : "Identyfikator instancji",
      LAST_UPDATE : "Ostatnia aktualizacja",
      LAST_UPDATE_RANGE : "Ostatnia aktualizacja (zakres)",
      LAST_UPDATED_TIME : "Godzina ostatniej aktualizacji",
      DASHBOARD_VIEW : "Widok panelu kontrolnego",
      HOMEPAGE : "Strona główna",
      JOBLOGS : "Dzienniki zadań",
      QUEUED : "W kolejce",
      ENDED : "Zakończone",
      ERROR : "Błąd",
      CLOSE : "Zamknij",
      WARNING : "Ostrzeżenie",
      GO_TO_DASHBOARD: "Idź do panelu kontrolnego",
      DASHBOARD : "Panel kontrolny",
      BATCH_JOB_NAME: "Nazwa zadania wsadowego",
      SUBMITTER: "Użytkownik wprowadzający",
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
      VIEW_LOG_FILE: "Wyświetl pliki dziennika",
      STEP_NAME: "Nazwa kroku",
      ID: "Identyfikator",
      PARTITION_ID: "Partycja {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Wyświetl szczegóły wykonania zadania {0}",    // Job Execution ID number
      PARENT_DETAILS: "Szczegóły informacji nadrzędnej",
      TIMES: "Czasy",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Status",
      SEARCH_ON: "Wybierz, aby przeprowadzić filtrowanie według wartości i nazwy kolumny ({1}, {0})",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Wprowadź kryteria wyszukiwania.",
      BREADCRUMB_JOB_INSTANCE : "Instancja zadania {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Wykonanie zadania {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Dziennik zadania {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "Kryteria wyszukiwania są niepoprawne.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "Kryteria wyszukiwania nie mogą zawierać wielu parametrów filtrowania: {0}.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Tabela instancji zadań",
      EXECUTIONS_TABLE_IDENTIFIER: "Tabela wykonań zadań",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Tabela szczegółów kroków",
      LOADING_VIEW : "Trwa ładowanie informacji na stronie",
      LOADING_VIEW_TITLE : "Ładowanie widoku",
      LOADING_GRID : "Oczekiwanie na zwrócenie wyników wyszukiwania przez serwer",
      PAGENUMBER : "Numer strony",
      SELECT_QUERY_SIZE: "Wybierz wielkość zapytania",
      LINK_EXPLORE_HOST: "Wybierz, aby wyświetlić szczegółowe informacje dotyczące hosta {0} w narzędziu eksploracji.",      // Host name
      LINK_EXPLORE_SERVER: "Wybierz, aby wyświetlić szczegółowe informacje dotyczące serwera {0} w narzędziu eksploracji.",  // Server name

      //ACTIONS
      RESTART: "Restartuj",
      STOP: "Zatrzymaj",
      PURGE: "Wyczyść",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Działania dla instancji zadania {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Menu działań instancji zdania",

      RESTART_INSTANCE_MESSAGE: "Czy zrestartować ostatnie wykonanie zadania powiązane z instancją zadania {0}?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Czy zatrzymać ostatnie wykonanie zadania powiązane z instancją zadania {0}?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Czy chcesz wyczyścić wszystkie wpisy w bazie danych i dzienniki zadań powiązane z instancją zadania {0}?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Wyczyść tylko składnicę zadania",

      RESTART_INST_ERROR_MESSAGE: "Żądanie zrestartowania nie powiodło się.",
      STOP_INST_ERROR_MESSAGE: "Żądanie zatrzymania nie powiodło się.",
      PURGE_INST_ERROR_MESSAGE: "Żądanie wyczyszczenia nie powiodło się.",
      ACTION_REQUEST_ERROR_MESSAGE: "Żądanie działanie nie powiodło się i został zwrócony kod statusu: {0}.  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Użyj ponownie parametrów z poprzedniego wykonania",
      JOB_PARAMETERS_EMPTY: "Jeśli nie wybrano opcji {0}, wprowadź parametry zadania w tym obszarze.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Nazwa parametru",
      JOB_PARAMETER_VALUE: "Wartość parametru",
      PARM_NAME_COLUMN_HEADER: "Parametr",
      PARM_VALUE_COLUMN_HEADER: "Wartość",
      PARM_ADD_ICON_TITLE: "Dodaj parametr",
      PARM_REMOVE_ICON_TITLE: "Usuń parametr",
      PARMS_ENTRY_ERROR: "Nazwa parametru jest wymagana.",
      JOB_PARAMETER_CREATE: "Wybierz opcję {0}, aby dodać parametry do następnego wykonania tej instancji zadania.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Przycisk dodawania parametru w nagłówku tabeli.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Treść dziennika zadania",
      FILE_DOWNLOAD : "Pobieranie pliku",
      DOWNLOAD_DIALOG_DESCRIPTION : "Czy pobrać pliki dziennika?",
      INCLUDE_ALL_LOGS : "Uwzględnij wszystkie pliki dziennika podczas wykonywania zadania.",
      LOGS_NAVIGATION_BAR : "Pasek nawigacyjny dzienników zadań",
      DOWNLOAD : "Pobierz",
      LOG_TOP : "Początek dzienników",
      LOG_END : "Koniec dzienników",
      PREVIOUS_PAGE : "Poprzednia strona",
      NEXT_PAGE : "Następna strona",
      DOWNLOAD_ARIA : "Pobierz plik",

      //Error messages for popups
      REST_CALL_FAILED : "Wywołanie pobrania danych nie powiodło się.",
      NO_JOB_EXECUTION_URL : "Nie podano numeru wykonania zadania w adresie URL lub ta instancja nie ma żadnych dzienników wykonania zadania do wyświetlenia.",
      NO_VIEW : "Błąd adresu URL: brak takiego widoku.",
      WRONG_TOOL_ID : "Łańcuch zapytania adresu URL rozpoczęto przy użyciu narzędzia {1}, a nie przy użyciu narzędzia o identyfikatorze {0}",   // {0} and {1} are both Strings
      URL_NO_LOGS : "Błąd adresu URL: brak dzienników.",
      NOT_A_NUMBER : "Błąd adresu URL: w polu {0} należy podać liczbę.",                                                // {0} is a field name
      PARAMETER_REPETITION : "Błąd adresu URL: pole {0} może występować tylko raz w parametrach.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "Błąd adresu URL: parametr strony jest poza zakresem.",
      INVALID_PARAMETER : "Błąd adresu URL: {0} nie jest poprawnym parametrem.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "Błąd adresu URL: adres URL może zawierać wykonanie zadania lub instancję zadania, lecz nie może zawierać obu tych elementów jednocześnie.",
      MISSING_EXECUTION_ID_PARAM : "Brak wymaganego parametru identyfikatora wykonania.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Aby móc używać narzędzia Java Batch, należy skonfigurować trwałą bazę danych zadań wsadowych Java.",
      IGNORED_SEARCH_CRITERIA : "W wynikach zignorowano następujące kryteria filtrowania: {0}",

      GRIDX_SUMMARY_TEXT : "Wyświetlanie ostatnich ${0} instancji zadań"

});

