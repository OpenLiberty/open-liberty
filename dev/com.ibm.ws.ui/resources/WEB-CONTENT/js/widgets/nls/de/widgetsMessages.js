/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
    LIBERTY_HEADER_TITLE: "Liberty Admin Center",
    LIBERTY_HEADER_PROFILE: "Vorgaben",
    LIBERTY_HEADER_LOGOUT: "Abmelden",
    LIBERTY_HEADER_LOGOUT_USERNAME: "{0} abmelden",
    TOOLBOX_BANNER_LABEL: "{0}-Banner",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Toolbox",
    TOOLBOX_TITLE_LOADING_TOOL: "Tool wird geladen...",
    TOOLBOX_TITLE_EDIT: "Toolbox bearbeiten",
    TOOLBOX_EDIT: "Bearbeiten",
    TOOLBOX_DONE: "Fertig",
    TOOLBOX_SEARCH: "Filter",
    TOOLBOX_CLEAR_SEARCH: "Filterkriterien löschen",
    TOOLBOX_END_SEARCH: "Filter beenden",
    TOOLBOX_ADD_TOOL: "Tool hinzufügen",
    TOOLBOX_ADD_CATALOG_TOOL: "Tool hinzufügen",
    TOOLBOX_ADD_BOOKMARK: "Lesezeichen hinzufügen",
    TOOLBOX_REMOVE_TITLE: "Tool {0} entfernen",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Tool entfernen",
    TOOLBOX_REMOVE_MESSAGE: "Möchten Sie {0} wirklich entfernen?",
    TOOLBOX_BUTTON_REMOVE: "Entfernen",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Wechseln zu Toolbox",
    TOOLBOX_BUTTON_CANCEL: "Abbrechen",
    TOOLBOX_BUTTON_BGTASK: "Hintergrundtasks",
    TOOLBOX_BUTTON_BACK: "Zurück",
    TOOLBOX_BUTTON_USER: "Benutzer",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "Beim Hinzufügen des Tools {0} ist ein Fehler aufgetreten: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "Beim Entfernen des Tools {0} ist ein Fehler aufgetreten: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "Beim Abrufen der Tools in der Toolbox ist ein Fehler aufgetreten: {0}",
    TOOLCATALOG_TITLE: "Toolkatalog",
    TOOLCATALOG_ADDTOOL_TITLE: "Tool hinzufügen",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Möchten Sie das Tool {0} wirklich zu Ihrer Toolbox hinzufügen?",
    TOOLCATALOG_BUTTON_ADD: "Hinzufügen",
    TOOL_FRAME_TITLE: "Tool-Frame",
    TOOL_DELETE_TITLE: "{0} löschen",
    TOOL_ADD_TITLE: "{0} hinzufügen",
    TOOL_ADDED_TITLE: "{0} wurde bereits hinzugefügt",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Tool nicht gefunden",
    TOOL_LAUNCH_ERROR_MESSAGE: "Das angeforderte Tool wurde nicht gestartet, weil sich das Tool nicht im Katalog gefindet.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Fehler",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Warnung",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Informationen",
    LIBERTY_UI_CATALOG_GET_ERROR: "Beim Abrufen des Katalogs ist ein Fehler aufgetreten: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "Beim Abrufen des Tools {0} aus dem Katalog ist ein Fehler aufgetreten: {1}",
    PREFERENCES_TITLE: "Vorgaben",
    PREFERENCES_SECTION_TITLE: "Vorgaben",
    PREFERENCES_ENABLE_BIDI: "Bidirektionale Unterstützung aktivieren",
    PREFERENCES_BIDI_TEXTDIR: "Textrichtung",
    PREFERENCES_BIDI_TEXTDIR_LTR: "Links nach rechts",
    PREFERENCES_BIDI_TEXTDIR_RTL: "Rechts nach links",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Kontextuell",
    PREFERENCES_SET_ERROR_MESSAGE: "Beim Einstellen der Benutzervorgabe in der Toolbox ist ein Fehler aufgetreten: {0}",
    BGTASKS_PAGE_LABEL: "Hintergrundtasks",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Installation {0} implementieren", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Installation {0} - {1} implementieren", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "Aktiv",
    BGTASKS_STATUS_FAILED: "Fehlgeschlagen",
    BGTASKS_STATUS_SUCCEEDED: "Fertiggestellt", 
    BGTASKS_STATUS_WARNING: "Partiell erfolgreich",
    BGTASKS_STATUS_PENDING: "Anstehend",
    BGTASKS_INFO_DIALOG_TITLE: "Details",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Standardausgabe:",
    BGTASKS_INFO_DIALOG_STDERR: "Standardfehler:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Ausnahme:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Ergebnis:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Servername:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Benutzerverzeichnis:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Aktive Hintergrundtasks",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Keine",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "Keine aktiven Hintergrundtasks",
    BGTASKS_DISPLAY_BUTTON: "Taskdetails und -verlauf",
    BGTASKS_EXPAND: "Abschnitt einblenden",
    BGTASKS_COLLAPSE: "Abschnitt ausblenden",
    PROFILE_MENU_HELP_TITLE: "Hilfe",
    DETAILS_DESCRIPTION: "Beschreibung",
    DETAILS_OVERVIEW: "Übersicht",
    DETAILS_OTHERVERSIONS: "Andere Versionen",
    DETAILS_VERSION: "Version: {0}",
    DETAILS_UPDATED: "Aktualisiert: {0}",
    DETAILS_NOTOPTIMIZED: "Für die aktuelle Einheit nicht optimiert.",
    DETAILS_ADDBUTTON: "Zu 'Eigene Toolbox' hinzufügen",
    DETAILS_OPEN: "Öffnen",
    DETAILS_CATEGORY: "Kategorie {0}",
    DETAILS_ADDCONFIRM: "Das Tool {0} wurde der Toolbox erfolgreich hinzugefügt.",
    CONFIRM_DIALOG_HELP: "Hilfe",
    YES_BUTTON_LABEL: "{0} ja",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} nein",  // insert is dialog title

    YES: "Ja",
    NO: "Nein",

    TOOL_OIDC_ACCESS_DENIED: "Der Benutzer hat nicht die Rolle, die für die Ausführung dieser Anforderung berechtigt ist.",
    TOOL_OIDC_GENERIC_ERROR: "Es ist ein Fehler aufgetreten. Suchen Sie im Protokoll nach weiteren Informationen zum Fehler.",
    TOOL_DISABLE: "Der Benutzer ist nicht berechtigt, dieses Tool zu verwenden. Nur Benutzer mit der Administratorrolle sind berechtigt, dieses Tool zu verwenden." 
});
