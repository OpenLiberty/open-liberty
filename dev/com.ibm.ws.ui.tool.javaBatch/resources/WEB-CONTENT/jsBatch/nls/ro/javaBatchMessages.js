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
      ACCOUNTING_STRING : "Şir de contabilizare",
      SEARCH_RESOURCE_TYPE_ALL : "Toate",
      SEARCH : "Căutare",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Introduceţi criteriile de căutare selectând butonul Adăugare criterii de căutare şi apoi specificaţi o valoare.",
      SUBMITTED : "Lansat",
      JMS_QUEUED : "Pus în coadă JMS",
      JMS_CONSUMED : "Consumat JMS",
      JOB_PARAMETER : "Parametru job",
      DISPATCHED : "Dispecerizat",
      FAILED : "Eşuat",
      STOPPED : "Oprit",
      COMPLETED : "Finalizat",
      ABANDONED : "Abandonat",
      STARTED : "Pornit",
      STARTING : "În pornire",
      STOPPING : "În oprire",
      REFRESH : "Reîmprospătare",
      INSTANCE_STATE : "Stare instanţă",
      APPLICATION_NAME : "Nume de aplicaţie",
      APPLICATION: "Aplicaţie",
      INSTANCE_ID : "ID instanţă",
      LAST_UPDATE : "Ultima actualizare",
      LAST_UPDATE_RANGE : "Interval ultima actualizare",
      LAST_UPDATED_TIME : "Ora ultimei actualizări",
      DASHBOARD_VIEW : "Vizualizare tablou de bord",
      HOMEPAGE : "Pagina Acasă",
      JOBLOGS : "Istorice de joburi",
      QUEUED : "Pus în coadă",
      ENDED : "Terminat",
      ERROR : "Eroare",
      CLOSE : "Închidere",
      WARNING : "Avertisment",
      GO_TO_DASHBOARD: "Deplasare la Tablou de bord",
      DASHBOARD : "Tablou de bord",
      BATCH_JOB_NAME: "Nume job batch",
      SUBMITTER: "Trimis de",
      BATCH_STATUS: "Stare batch",
      EXECUTION_ID: "ID execuţie job",
      EXIT_STATUS: "Stare la ieşire",
      CREATE_TIME: "Oră creare",
      START_TIME: "Oră de început",
      END_TIME: "Oră de sfârşit",
      SERVER: "Server",
      SERVER_NAME: "Nume server",
      SERVER_USER_DIRECTORY: "Director utilizator",
      SERVERS_USER_DIRECTORY: "Directorul de utilizator al serverului",
      HOST: "Gazdă",
      NAME: "Nume",
      JOB_PARAMETERS: "Parametri de job",
      JES_JOB_NAME: "Nume de job JES",
      JES_JOB_ID: "ID job JES",
      ACTIONS: "Acţiuni",
      VIEW_LOG_FILE: "Vizualizare fişier istoric",
      STEP_NAME: "Nume de pas",
      ID: "ID",
      PARTITION_ID: "Partiţia {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Vizualizare detalii execuţie job {0}",    // Job Execution ID number
      PARENT_DETAILS: "Detalii info părinte",
      TIMES: "Timp",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Stare",
      SEARCH_ON: "Selectaţi filtrarea pe {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Introduceţi criteriile de căutare.",
      BREADCRUMB_JOB_INSTANCE : "Instanţă job {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Execuţie job {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Istoric job {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "Criteriile de căutare nu sunt valide.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "Criteriile de căutare nu pot avea mai multe filtrări după parametri {0}.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Tabel de instanţe de joburi",
      EXECUTIONS_TABLE_IDENTIFIER: "Tabel de execuţii de joburi",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Tabel de detalii paşi",
      LOADING_VIEW : "Pagina încarcă în prezent informaţii",
      LOADING_VIEW_TITLE : "Încărcare vizualizare",
      LOADING_GRID : "Se aşteaptă să returnarea rezultatelor căutării de la server ",
      PAGENUMBER : "Număr pagină",
      SELECT_QUERY_SIZE: "Selectare dimensiune interogare",
      LINK_EXPLORE_HOST: "Selectaţi pentru a vizualiza detaliile pe Gazda {0} în unealta Explorare.",      // Host name
      LINK_EXPLORE_SERVER: "Selectaţi pentru a vizualiza detaliile pe Serverul {0} în unealta Explorare.",  // Server name

      //ACTIONS
      RESTART: "Repornire",
      STOP: "Oprire",
      PURGE: "Epurare",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Acţiuni pentru instanţa de job {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Meniu de acţiuni pentru instanţa de job",

      RESTART_INSTANCE_MESSAGE: "Doriţi să reporniţi cea mai recentă execuţie a jobului asociat cu instanţa de job {0}?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Doriţi să opriţi cea mai recentă execuţie a jobului asociat cu instanţa de job {0}?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Doriţi să epuraţi toate intrările bazei de date şi istoricele de job asociate cu instanţa de job {0}?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Epurare doar depozit de joburi",

      RESTART_INST_ERROR_MESSAGE: "Cererea de repornire a eşuat.",
      STOP_INST_ERROR_MESSAGE: "Cererea de oprire a eşuat.",
      PURGE_INST_ERROR_MESSAGE: "Cererea de epurare a eşuat.",
      ACTION_REQUEST_ERROR_MESSAGE: "Cererea de acţiune a eşuat cu codul de stare: {0}.  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Reutilizare parametri de la cererea anterioară",
      JOB_PARAMETERS_EMPTY: "Când nu este selectat '{0}', utilizaţi această zonă pentru a introduce parametrii jobului.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Nume parametru",
      JOB_PARAMETER_VALUE: "Valoare parametru",
      PARM_NAME_COLUMN_HEADER: "Parametru",
      PARM_VALUE_COLUMN_HEADER: "Valoare",
      PARM_ADD_ICON_TITLE: "Adăugare parametru",
      PARM_REMOVE_ICON_TITLE: "Înlăturare parametru",
      PARMS_ENTRY_ERROR: "Numele de parametru este necesar.",
      JOB_PARAMETER_CREATE: "Selectaţi {0} pentru a adăuga parametri la următoarea execuţie a acestei instanţe de job.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Butonul Adăugare parametru din antetul tabelului.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Conţinut istoric de job",
      FILE_DOWNLOAD : "Descărcare fişier",
      DOWNLOAD_DIALOG_DESCRIPTION : "Doriţi să descărcaţi fişierul istoric?",
      INCLUDE_ALL_LOGS : "Includere toate fişierele istoric pentru execuţia jobului",
      LOGS_NAVIGATION_BAR : "Bara de navigare a istoricelor de joburi",
      DOWNLOAD : "Descărcare",
      LOG_TOP : "Începutul istoricelor",
      LOG_END : "Terminarea istoricelor",
      PREVIOUS_PAGE : "Pagina anterioară",
      NEXT_PAGE : "Pagina următoare",
      DOWNLOAD_ARIA : "Descărcare fişier",

      //Error messages for popups
      REST_CALL_FAILED : "Apelul de aducere date a eşuat.",
      NO_JOB_EXECUTION_URL : "Fie nu a fost furnizat niciun număr de execuţie job în URL, fie instanţa nu are niciun istoric de execuţie job de arătat.",
      NO_VIEW : "Eroare URL: Nu există o asemenea vizualizare.",
      WRONG_TOOL_ID : "Şirul de interogare al URL-ului nu începe cu ID-ul de unealtă {0}, ci cu {1}.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "Eroare URL: Nu există istorice.",
      NOT_A_NUMBER : "Eroare URL: {0} trebuie să fie un număr.",                                                // {0} is a field name
      PARAMETER_REPETITION : "Eroare URL: {0} poate apărea o singură dată în parametri.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "Eroare URL: Parametrul de pagină este în afara intervalului.",
      INVALID_PARAMETER : "Eroare URL: {0} nu este un parametru valid.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "Eroare URL: URL-ul poate specifica execuţia jobului sau instanţa jobului, dar nu pe amândouă.,",
      MISSING_EXECUTION_ID_PARAM : "Parametrul ID de execuţie lipseşte.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Pentru a utiliza unealta Java Batch este necesară o configuraţie de bază de date persistentă pentru batch Java.",
      IGNORED_SEARCH_CRITERIA : "Următoarele criterii de căutare din filtru au fost ignorate în rezultate: {0}",

      GRIDX_SUMMARY_TEXT : "Afişare ${0} cele mai recente instanţe de job"

});

