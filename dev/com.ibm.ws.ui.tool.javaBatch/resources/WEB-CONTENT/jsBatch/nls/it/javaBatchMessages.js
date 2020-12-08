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
      ACCOUNTING_STRING : "Stringa account",
      SEARCH_RESOURCE_TYPE_ALL : "Tutto",
      SEARCH : "Ricerca",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Immettere i criteri di ricerca selezionando il pulsante Aggiungi criteri di ricerca e specificare un valore",
      SUBMITTED : "Inoltrato",
      JMS_QUEUED : "JMS accodato",
      JMS_CONSUMED : "JMS utilizzato",
      JOB_PARAMETER : "Parametro lavoro",
      DISPATCHED : "Distribuito",
      FAILED : "Non riuscito",
      STOPPED : "Arrestato",
      COMPLETED : "Completato",
      ABANDONED : "Abbandonato",
      STARTED : "Avviato",
      STARTING : "Avvio in corso",
      STOPPING : "Arresto in corso",
      REFRESH : "Aggiorna",
      INSTANCE_STATE : "Stato istanza",
      APPLICATION_NAME : "Nome applicazione",
      APPLICATION: "Applicazione",
      INSTANCE_ID : "ID istanza",
      LAST_UPDATE : "Ultimo aggiornamento",
      LAST_UPDATE_RANGE : "Intervallo ultimo aggiornamento",
      LAST_UPDATED_TIME : "Data/ora ultimo aggiornamento",
      DASHBOARD_VIEW : "Vista dashboard",
      HOMEPAGE : "Home Page",
      JOBLOGS : "Log lavori",
      QUEUED : "Accodato",
      ENDED : "Terminato",
      ERROR : "Errore",
      CLOSE : "Chiudi",
      WARNING : "Avvertenza",
      GO_TO_DASHBOARD: "Vai al dashboard",
      DASHBOARD : "Dashboard",
      BATCH_JOB_NAME: "Nome lavoro batch",
      SUBMITTER: "Inoltratore",
      BATCH_STATUS: "Stato batch",
      EXECUTION_ID: "ID di esecuzione lavoro",
      EXIT_STATUS: "Stato di uscita",
      CREATE_TIME: "Data/ora di creazione",
      START_TIME: "Data/ora di avvio",
      END_TIME: "Data/ora di fine",
      SERVER: "Server",
      SERVER_NAME: "Nome server",
      SERVER_USER_DIRECTORY: "Directory utente",
      SERVERS_USER_DIRECTORY: "Directory utente del server",
      HOST: "Host",
      NAME: "Nome",
      JOB_PARAMETERS: "Parametri lavoro",
      JES_JOB_NAME: "Nome lavoro JES",
      JES_JOB_ID: "ID lavoro JES",
      ACTIONS: "Azioni",
      VIEW_LOG_FILE: "Visualizza file di log",
      STEP_NAME: "Nome passo",
      ID: "ID",
      PARTITION_ID: "Partizione {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Visualizza dettagli esecuzione lavoro {0}",    // Job Execution ID number
      PARENT_DETAILS: "Dettagli informazioni parent",
      TIMES: "Date/ore",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Stato",
      SEARCH_ON: "Selezionare per filtrare in base a {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Immettere i criteri di ricerca.",
      BREADCRUMB_JOB_INSTANCE : "Istanza lavoro {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Esecuzione lavoro {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Log lavoro {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "I criteri di ricerca non sono validi.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "I criteri di ricerca non possono avere più filtri per i parametri {0}. ", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Tabella istanze lavoro",
      EXECUTIONS_TABLE_IDENTIFIER: "Tabella esecuzioni lavoro",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Tabella dettagli passi",
      LOADING_VIEW : "La pagina sta caricando le informazioni",
      LOADING_VIEW_TITLE : "Caricamento vista",
      LOADING_GRID : "In attesa della restituzione dei risultati della ricerca dal  server",
      PAGENUMBER : "Numero di pagina",
      SELECT_QUERY_SIZE: "Seleziona dimensione query",
      LINK_EXPLORE_HOST: "Selezionare per visualizzare i dettagli sull'host {0} nello strumento Esplora. ",      // Host name
      LINK_EXPLORE_SERVER: "Selezionare per visualizzare i dettagli sul server {0} nello strumento Esplora. ",  // Server name

      //ACTIONS
      RESTART: "Riavvia",
      STOP: "Arresta",
      PURGE: "Cancella",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Azioni per istanza lavoro {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Menu Azioni istanza lavoro",

      RESTART_INSTANCE_MESSAGE: "Riavviare l'esecuzione lavoro più recente associata all'istanza lavoro {0}?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Arrestare l'esecuzione lavoro più recente associata all'istanza lavoro {0}?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Cancellare tutte le voci di database e i log lavori associati all'istanza lavoro? {0}?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Cancella solo archivio lavoro",

      RESTART_INST_ERROR_MESSAGE: "Richiesta di riavvio non riuscita.",
      STOP_INST_ERROR_MESSAGE: "Richiesta di arresto non riuscita.",
      PURGE_INST_ERROR_MESSAGE: "Richiesta di cancellazione non riuscita.",
      ACTION_REQUEST_ERROR_MESSAGE: "Richiesta di azione non riuscita con il codice di stato: {0}.  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Riutilizza parametri da esecuzione precedente",
      JOB_PARAMETERS_EMPTY: "Quando '{0}' non è selezionato, utilizzare questa area per immettere i parametri lavoro.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Nome parametro",
      JOB_PARAMETER_VALUE: "Valore parametro",
      PARM_NAME_COLUMN_HEADER: "Parametro",
      PARM_VALUE_COLUMN_HEADER: "Valore",
      PARM_ADD_ICON_TITLE: "Aggiungi parametro",
      PARM_REMOVE_ICON_TITLE: "Rimuovi parametro",
      PARMS_ENTRY_ERROR: "Nome parametro obbligatorio.",
      JOB_PARAMETER_CREATE: "Selezionare {0} per aggiungere i parametri alla successiva esecuzione di questa istanza lavoro.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Pulsante Aggiungi parametro nell'intestazione tabella.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Contenuto log lavoro",
      FILE_DOWNLOAD : "Download del file",
      DOWNLOAD_DIALOG_DESCRIPTION : "Scaricare il file di log?",
      INCLUDE_ALL_LOGS : "Includi tutti i file di log per l'esecuzione lavoro",
      LOGS_NAVIGATION_BAR : "Barra di navigazione dei log lavori",
      DOWNLOAD : "Download",
      LOG_TOP : "Inizio dei log",
      LOG_END : "Fine dei log",
      PREVIOUS_PAGE : "Pagina precedente",
      NEXT_PAGE : "Pagina successiva",
      DOWNLOAD_ARIA : "Scarica file",

      //Error messages for popups
      REST_CALL_FAILED : "La chiamata per acquisire i dati non è riuscita.",
      NO_JOB_EXECUTION_URL : "Non è stato fornito un numero di esecuzione lavoro nell'URL o l'istanza non ha alcun log di esecuzione lavoro da visualizzare.",
      NO_VIEW : "Errore URL: non esistono viste simili.",
      WRONG_TOOL_ID : "La stringa di query dell'URL non inizia con l'ID strumento {0}, ma con {1}.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "Errore URL: Nessun log esistente.",
      NOT_A_NUMBER : "Errore URL: {0} deve essere un numero. ",                                                // {0} is a field name
      PARAMETER_REPETITION : "Errore URL: {0} può esistere una sola volta nei parametri.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "Errore URL: il parametro pagina è fuori dall'intervallo.",
      INVALID_PARAMETER : "Errore URL: {0} non è un parametro valido.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "Errore URL: l'URL può specificare l'esecuzione lavoro o l'istanza lavoro, ma non entrambe.",
      MISSING_EXECUTION_ID_PARAM : "Manca il parametro ID di esecuzione necessario.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Una configurazione database persistente batch Java è richiesta per poter utilizzare lo strumento Batch Java.",
      IGNORED_SEARCH_CRITERIA : "I seguenti criteri di filtro sono stati ignorati nei risultati: {0}",

      GRIDX_SUMMARY_TEXT : "Visualizzazione delle ultime ${0} istanze lavoro"

});

