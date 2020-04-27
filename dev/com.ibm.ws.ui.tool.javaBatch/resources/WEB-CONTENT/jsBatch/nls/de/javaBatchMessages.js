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
      ACCOUNTING_STRING : "Abrechnungszeichenfolge",
      SEARCH_RESOURCE_TYPE_ALL : "Alle",
      SEARCH : "Suchen",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Suchkriterien durch Auswahl der Schaltfläche Suchkriterien hinzufügen und anschließende Angabe eines Werts eingeben",
      SUBMITTED : "Übergeben",
      JMS_QUEUED : "In JMS-Warteschlange eingereiht",
      JMS_CONSUMED : "Aus JMS-Warteschlange konsumiert",
      JOB_PARAMETER : "Jobparameter",
      DISPATCHED : "Zugeteilt",
      FAILED : "Fehlgeschlagen",
      STOPPED : "Gestoppt",
      COMPLETED : "Abgeschlossen",
      ABANDONED : "Abgebrochen",
      STARTED : "Gestartet",
      STARTING : "Wird gestartet",
      STOPPING : "Wird gestoppt",
      REFRESH : "Aktualisieren",
      INSTANCE_STATE : "Instanzstatus",
      APPLICATION_NAME : "Anwendungsname",
      APPLICATION: "Anwendung",
      INSTANCE_ID : "Instanz-ID",
      LAST_UPDATE : "Letzte Aktualisierung",
      LAST_UPDATE_RANGE : "Letzter Aktualisierungsbereich",
      LAST_UPDATED_TIME : "Zeitpunkt der letzten Aktualisierung",
      DASHBOARD_VIEW : "Dashboardansicht",
      HOMEPAGE : "Homepage",
      JOBLOGS : "Jobprotokolle",
      QUEUED : "In Warteschlange eingereiht",
      ENDED : "Beendet",
      ERROR : "Fehler",
      CLOSE : "Schließen",
      WARNING : "Warnung",
      GO_TO_DASHBOARD: "Wechseln zu Dashboard",
      DASHBOARD : "Dashboard",
      BATCH_JOB_NAME: "Name des Stapeljobs",
      SUBMITTER: "Übergebender",
      BATCH_STATUS: "Stapelstatus",
      EXECUTION_ID: "Jobausführungs-ID",
      EXIT_STATUS: "Exitstatus",
      CREATE_TIME: "Erstellungszeit",
      START_TIME: "Startzeit",
      END_TIME: "Endzeit",
      SERVER: "Server",
      SERVER_NAME: "Servername",
      SERVER_USER_DIRECTORY: "Benutzerverzeichnis",
      SERVERS_USER_DIRECTORY: "Serverbenutzerverzeichnis",
      HOST: "Host",
      NAME: "Name",
      JOB_PARAMETERS: "Jobparameter",
      JES_JOB_NAME: "Name des JES-Jobs",
      JES_JOB_ID: "ID des JES-Jobs",
      ACTIONS: "Aktionen",
      VIEW_LOG_FILE: "Protokolldatei anzeigen",
      STEP_NAME: "Schrittname",
      ID: "ID",
      PARTITION_ID: "Partition {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Details der Jobausführung {0} anzeigen",    // Job Execution ID number
      PARENT_DETAILS: "Details des übergeordneten Elements",
      TIMES: "Zeiten",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Status",
      SEARCH_ON: "Auswahl zum Filtern nach {1} {0} treffen",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Geben Sie Suchkriterien ein. ",
      BREADCRUMB_JOB_INSTANCE : "Jobinstanz {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Jobausführung {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Jobprotokoll {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "Die Suchkriterien sind nicht gültig.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "Die Suchkriterien können nicht mehrfache Filter mit folgenden Parameter haben: {0} ", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Tabelle mit Jobinstanzen",
      EXECUTIONS_TABLE_IDENTIFIER: "Tabelle mit Jobausführungen",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Tabelle mit Schrittdetails",
      LOADING_VIEW : "Auf dieser Seite werden momentan Informationen geladen.",
      LOADING_VIEW_TITLE : "Ladeansicht",
      LOADING_GRID : "Es wird auf die Rückgabe der Suchergebnisse des Servers gewartet.",
      PAGENUMBER : "Seitennummer",
      SELECT_QUERY_SIZE: "Abfragegröße auswählen",
      LINK_EXPLORE_HOST: "Auswählen, um Details zum Host {0} im Explore-Tool anzuzeigen.",      // Host name
      LINK_EXPLORE_SERVER: "Auswählen, um Details zum Server {0} im Explore-Tool anzuzeigen.",  // Server name

      //ACTIONS
      RESTART: "Erneut starten",
      STOP: "Stoppen",
      PURGE: "Löschen",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Aktionen für Jobinstanz {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Aktionsmenü für Jobinstanz",

      RESTART_INSTANCE_MESSAGE: "Möchten Sie die aktuellste Jobausführung, die der Jobinstanz {0} zugeordnet ist, erneut starten?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Möchten Sie die aktuellste Jobausführung, die der Jobinstanz {0} zugeordnet ist, stoppen?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Möchten Sie alle Datenbankeinträge und Jobprotokolle, die der Jobinstanz {0} zugeordnet sind, löschen?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Nur Jobspeicher löschen",

      RESTART_INST_ERROR_MESSAGE: "Die Anforderung für den Neustart ist fehlgeschlagen.",
      STOP_INST_ERROR_MESSAGE: "Die Stoppanforderung ist fehlgeschlagen.",
      PURGE_INST_ERROR_MESSAGE: "Löschanforderung fehlgeschlagen.",
      ACTION_REQUEST_ERROR_MESSAGE: "Die Aktionsanforderung ist mit dem folgenden Statuscode fehlgeschlagen: {0}. URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Parameter der vorherigen Ausführung wiederverwenden",
      JOB_PARAMETERS_EMPTY: "Wenn '{0}' nicht ausgewählt ist, geben Sie in diesem Bereich die Jobparameter an.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Parametername",
      JOB_PARAMETER_VALUE: "Parameterwert",
      PARM_NAME_COLUMN_HEADER: "Parameter",
      PARM_VALUE_COLUMN_HEADER: "Wert",
      PARM_ADD_ICON_TITLE: "Parameter hinzufügen",
      PARM_REMOVE_ICON_TITLE: "Parameter entfernen",
      PARMS_ENTRY_ERROR: "Der Parametername ist erforderlich.",
      JOB_PARAMETER_CREATE: "Wählen Sie {0} aus, um Parameter für die nächste Ausführung dieser Jobinstanz hinzuzufügen.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Schaltfläche Parameter hinzufügen in der Tabellenüberschrift",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Inhalt des Jobprotokolls",
      FILE_DOWNLOAD : "Dateidownload",
      DOWNLOAD_DIALOG_DESCRIPTION : "Möchten Sie die Protokolldatei herunterladen?",
      INCLUDE_ALL_LOGS : "Alle Protokolldateien für Jobausführung einschließen",
      LOGS_NAVIGATION_BAR : "Navigationsleiste für Jobprotokolle",
      DOWNLOAD : "Herunterladen",
      LOG_TOP : "Anfang der Protokolle",
      LOG_END : "Ende der Protokolle",
      PREVIOUS_PAGE : "Vorherige Seite",
      NEXT_PAGE : "Nächste Seite",
      DOWNLOAD_ARIA : "Datei herunterladen",

      //Error messages for popups
      REST_CALL_FAILED : "Der Aufruf zum Abrufen der Daten ist fehlgeschlagen.",
      NO_JOB_EXECUTION_URL : "Entweder wurde in der URL keine Jobausführungsnummer angegeben oder diese Instanz hat keine anzuzeigenden Jobausführungsprotokolle.",
      NO_VIEW : "URL-Fehler: Diese Ansicht ist nicht vorhanden.",
      WRONG_TOOL_ID : "Die Abfragezeichenfolge der URL beginnt nicht mit der Tool-ID {0}, sondern stattdessen mit {1}.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "URL-Fehler: Keine Protokolle vorhanden.",
      NOT_A_NUMBER : "URL-Fehler: {0} muss eine Zahl sein.",                                                // {0} is a field name
      PARAMETER_REPETITION : "URL-Fehler: {0} darf nur ein einziges Mal in den Parametern vorkommen.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "URL-Fehler: Der Seitenparameter liegt außerhalb des gültigen Bereichs.",
      INVALID_PARAMETER : "URL-Fehler: {0} ist kein gültiger Parameter.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "URL-Fehler: Die URL kann entweder eine Jobausführung oder eine Jobinstanz angeben, aber nicht beides.",
      MISSING_EXECUTION_ID_PARAM : "Der erforderliche Parameter für die Ausführungs-ID fehlt.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Es ist eine persistente Datenbankkonfiguration für Java Batch erforderlich, um das Tool Java Batch verwenden zu können.",
      IGNORED_SEARCH_CRITERIA : "Die folgenden Filterkriterien wurden in den Ergebnissen ignoriert: {0}",

      GRIDX_SUMMARY_TEXT : "Es werden die ${0} aktuellsten Jobinstanzen angezeigt."

});

