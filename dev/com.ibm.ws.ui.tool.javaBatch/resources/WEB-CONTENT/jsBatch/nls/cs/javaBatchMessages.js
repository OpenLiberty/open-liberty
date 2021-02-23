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
      ACCOUNTING_STRING : "Účetní řetězec",
      SEARCH_RESOURCE_TYPE_ALL : "Vše",
      SEARCH : "Hledat",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Zadejte vyhledávací kritéria výběrem tlačítka Přidat vyhledávací kritéria a následným určením hodnoty",
      SUBMITTED : "Odesláno",
      JMS_QUEUED : "JMS ve frontě",
      JMS_CONSUMED : "JMS využito",
      JOB_PARAMETER : "Parametr úlohy",
      DISPATCHED : "Odbaveno",
      FAILED : "Nezdařilo se",
      STOPPED : "Zastaveno",
      COMPLETED : "Dokončeno",
      ABANDONED : "Opuštěno",
      STARTED : "Spuštěno",
      STARTING : "Spouštění",
      STOPPING : "Zastavení",
      REFRESH : "Aktualizovat",
      INSTANCE_STATE : "Stav instance",
      APPLICATION_NAME : "Název aplikace",
      APPLICATION: "Aplikace",
      INSTANCE_ID : "ID instance",
      LAST_UPDATE : "Poslední aktualizace",
      LAST_UPDATE_RANGE : "Rozsah poslední aktualizace",
      LAST_UPDATED_TIME : "Čas poslední aktualizace",
      DASHBOARD_VIEW : "Pohled panelu dashboard",
      HOMEPAGE : "Domovská stránka",
      JOBLOGS : "Protokoly úlohy",
      QUEUED : "Zařazeno do fronty",
      ENDED : "Ukončeno",
      ERROR : "Chyba",
      CLOSE : "Zavřít",
      WARNING : "Varování",
      GO_TO_DASHBOARD: "Přejít na panel dashboard",
      DASHBOARD : "Panel dashboard",
      BATCH_JOB_NAME: "Název dávkové úlohy",
      SUBMITTER: "Odeslal",
      BATCH_STATUS: "Stav dávky",
      EXECUTION_ID: "ID provedení úlohy",
      EXIT_STATUS: "Stav ukončení",
      CREATE_TIME: "Čas vytvoření",
      START_TIME: "Čas zahájení",
      END_TIME: "Čas ukončení",
      SERVER: "Server",
      SERVER_NAME: "Název serveru",
      SERVER_USER_DIRECTORY: "Uživatelský adresář",
      SERVERS_USER_DIRECTORY: "Uživatelský adresář serveru",
      HOST: "Hostitel",
      NAME: "Název",
      JOB_PARAMETERS: "Parametry úlohy",
      JES_JOB_NAME: "Název úlohy JES",
      JES_JOB_ID: "ID úlohy JES",
      ACTIONS: "Akce",
      VIEW_LOG_FILE: "Zobrazit soubor protokolu",
      STEP_NAME: "Název kroku",
      ID: "ID",
      PARTITION_ID: "Oblast {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Zobrazit podrobnosti o provedení úlohy {0}",    // Job Execution ID number
      PARENT_DETAILS: "Podrobnosti o nadřízených informacích",
      TIMES: "Časy",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Stav",
      SEARCH_ON: "Vybrat k filtraci pro {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Zadejte vyhledávací kritéria.",
      BREADCRUMB_JOB_INSTANCE : "Instance úlohy {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Provedení úlohy {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Protokol úlohy {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "Vyhledávací kritéria nejsou platná.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "Vyhledávací kritéria nemohou mít více filtrů podle parametrů {0}. ", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Tabulka instancí úlohy",
      EXECUTIONS_TABLE_IDENTIFIER: "Tabulka provedení úlohy",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Tabulka podrobností kroku",
      LOADING_VIEW : "Stránka aktuálně načítá informace",
      LOADING_VIEW_TITLE : "Načítání pohledu",
      LOADING_GRID : "Čekání na výsledky hledání pro návrat ze serveru",
      PAGENUMBER : "Číslo stránky",
      SELECT_QUERY_SIZE: "Vybrat velikost dotazu",
      LINK_EXPLORE_HOST: "Tuto položku vyberte, chcete-li zobrazit podrobnosti na hostiteli {0} v nástroji pro prozkoumání. ",      // Host name
      LINK_EXPLORE_SERVER: "Tuto položku vyberte, chcete-li zobrazit podrobnosti na serveru {0} v nástroji pro prozkoumání. ",  // Server name

      //ACTIONS
      RESTART: "Restartovat",
      STOP: "Zastavit",
      PURGE: "Vyprázdnit",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Akce pro instanci úlohy {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Nabídka akcí instance úlohy.",

      RESTART_INSTANCE_MESSAGE: "Chcete restartovat poslední provedení úlohy přidružené k instanci úlohy {0}?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Chcete zastavit poslední provedení úlohy přidružené k instanci úlohy {0}?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Chcete vyprázdnit všechny položky databáze a protokoly úlohy přidružené k instanci úlohy {0}?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Vyprázdnit pouze úložiště úloh",

      RESTART_INST_ERROR_MESSAGE: "Požadavek na restart se nezdařil.",
      STOP_INST_ERROR_MESSAGE: "Požadavek na zastavení se nezdařil.",
      PURGE_INST_ERROR_MESSAGE: "Požadavek na vyprázdnění se nezdařil.",
      ACTION_REQUEST_ERROR_MESSAGE: "Požadavek na akci se nezdařil se stavovým kódem: {0}. URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Znovu použít parametry z předchozího provedení",
      JOB_PARAMETERS_EMPTY: "Jestliže možnost '{0}' není vybrána, použijte k zadání parametrů úlohy tuto oblast.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Název parametru",
      JOB_PARAMETER_VALUE: "Hodnota parametru",
      PARM_NAME_COLUMN_HEADER: "Parametr",
      PARM_VALUE_COLUMN_HEADER: "Hodnota",
      PARM_ADD_ICON_TITLE: "Přidat parametr",
      PARM_REMOVE_ICON_TITLE: "Odebrat parametr",
      PARMS_ENTRY_ERROR: "Název parametru je povinný.",
      JOB_PARAMETER_CREATE: "Výběrem {0} přidáte parametry do dalšího provedení této instance úlohy.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Přidejte tlačítko parametru do záhlaví tabulky.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Obsah protokolu úlohy",
      FILE_DOWNLOAD : "Stáhnout soubor",
      DOWNLOAD_DIALOG_DESCRIPTION : "Chcete stáhnout soubor protokolu?",
      INCLUDE_ALL_LOGS : "Zahrnout všechny soubory protokolu pro provedení úlohy",
      LOGS_NAVIGATION_BAR : "Navigační panel protokolů úlohy",
      DOWNLOAD : "Stáhnout",
      LOG_TOP : "Začátek protokolů",
      LOG_END : "Konec protokolů",
      PREVIOUS_PAGE : "Předchozí stránka",
      NEXT_PAGE : "Další stránka",
      DOWNLOAD_ARIA : "Stáhnout soubor",

      //Error messages for popups
      REST_CALL_FAILED : "Nezdařilo se volání pro načtení dat.",
      NO_JOB_EXECUTION_URL : "Buď nebylo v adrese URL poskytnuto číslo provedení úlohy, nebo instance nemá žádné protokoly provedení úlohy k zobrazení.",
      NO_VIEW : "Chyba adresy URL: Takový pohled neexistuje.",
      WRONG_TOOL_ID : "Řetězec dotazu adresy URL se nespustil s ID nástroje {0}, ale {1}.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "Chyba adresy URL: Protokoly neexistují.",
      NOT_A_NUMBER : "Chyba adresy URL: {0} musí být číslo.",                                                // {0} is a field name
      PARAMETER_REPETITION : "Chyba adresy URL: {0} může v parametrech existovat jen jednou.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "Chyba adresy URL: Parametr stránky je mimo rozsah.",
      INVALID_PARAMETER : "Chyba adresy URL: {0} není platný parametr.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "Chyba adresy URL: Adresa URL může mít buď provedení úlohy, nebo instanci úlohy, nikoli obojí.",
      MISSING_EXECUTION_ID_PARAM : "Chybí povinný parametr ID provedení.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Chcete-li použít nástroj pro dávkové úlohy Java, je nezbytná konfigurace trvalé databáze dávky Java.",
      IGNORED_SEARCH_CRITERIA : "Ve výsledcích byla ignorována následující kritéria filtru: {0}",

      GRIDX_SUMMARY_TEXT : "Zobrazení nejnovějších instancí úlohy: ${0}"

});

