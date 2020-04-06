/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
var messages = {
    // Common Strings
    "ADD_NEW": "Neu hinzufügen",
    "CANCEL": "Abbrechen",
    "CLEAR_SEARCH": "Sucheingabe löschen",
    "CLEAR_FILTER": "Filter löschen",
    "CLICK_TO_SORT": "Zum Sortieren der Spalte klicken",
    "CLOSE": "Schließen",
    "COPY_TO_CLIPBOARD": "In die Zwischenablage kopieren",
    "COPIED_TO_CLIPBOARD": "In die Zwischenablage kopiert",
    "DELETE": "Löschen",
    "DONE": "Fertig",
    "EDIT": "Bearbeiten",
    "FALSE": "False",
    "GENERATE": "Generieren",
    "LOADING": "Wird geladen",
    "LOGOUT": "Abmelden",
    "NEXT_PAGE": "Nächste Seite",
    "NO_RESULTS_FOUND": "Keine Ergebnisse gefunden",
    "PAGES": "{0} von {1} Seiten",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Seitennummer zur Ansicht auswählen",
    "PREVIOUS_PAGE": "Vorherige Seite",
    "PROCESSING": "Wird verarbeitet",
    "REGENERATE": "Neu generieren",
    "REGISTER": "Registrieren",
    "TABLE_BATCH_BAR": "Tabellenaktionsleiste",
    "TABLE_FIELD_SORT_ASC": "Die Tabelle ist nach {0} in aufsteigender Reihenfolge sortiert. ",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Die Tabelle ist nach {0} in absteigender Reihenfolge sortiert. ", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Vorgang wiederholen...",
    "UPDATE": "Aktualisieren",

    // Common Column Names
    "CLIENT_NAME_COL": "Clientname",
    "EXPIRES_COL": "Ablaufdatum",
    "ISSUED_COL": "Austelldatum",
    "NAME_COL": "Name",
    "TYPE_COL": "Typ",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Token löschen",
    "TOKEN_MGR_DESC": "Löscht die app-passwords und app-tokens für einen angegebenen Benutzer. ",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Benutzer-ID eingeben",
    "TABLE_FILLED_WITH": "Die Tabelle wurde aktualisiert, um {0} Authentifizierungen anzuzeigen, die zur Benutzer-ID {1} gehören.",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "Ausgewählte app-passwords und app-tokens löschen",
    "DELETE_ARIA": "{0} mit dem Namen {1} löschen",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Dieses app-password löschen",
    "DELETE_TOKEN": "Dieses app-token löschen",
    "DELETE_FOR_USERID": "{0} für {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "Diese Aktion entfernt das momentan zugeordnete app-password.",
    "DELETE_WARNING_TOKEN": "Diese Aktion entfernt das momentan zugeordnete app-token.",
    "DELETE_MANY": "App-passwords/app-tokens löschen",
    "DELETE_MANY_FOR": "Zugeordnet zu {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "Diese Aktion löscht das ausgewählte app-password/app-token.",
    "DELETE_MANY_MESSAGE": "Diese Aktion löscht {0} ausgewählte app-passwords/app-tokens.",  // 0 - number
    "DELETE_ALL_MESSAGE": "Diese Aktion löscht alle app-passwords/app-tokens, die {0} zugeordnet sind.", // 0 - user id
    "DELETE_NONE": "Zum Löschen auswählen",
    "DELETE_NONE_MESSAGE": "Wählen Sie ein Kontrollkästchen aus, um anzugeben, welche app-passwords bzw. app-tokens gelöscht werden sollen.",
    "SINGLE_ITEM_SELECTED": "1 Element ausgewählt",
    "ITEMS_SELECTED": "{0} Elemente ausgewählt",            // 0 - number
    "SELECT_ALL_AUTHS": "Alle app-passwords und app-tokens für diesen Benutzer auswählen",
    "SELECT_SPECIFIC": "Wählen Sie das {0} mit dem Namen {1} aus, das gelöscht werden soll.",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Suchen Sie etwas Bestimmtes? Geben Sie eine Benutzer-ID ein, um die zugehörigen app-passwords und app-tokens anzuzeigen.",
    "GENERIC_FETCH_FAIL": "Fehler beim Abrufen von {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Die Liste mit {0}, die zu {1} gehört, konnte nicht abgerufen werden.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Fehler beim Löschen von {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Es ist ein Fehler aufgetreten, als {0} mit dem Namen {1} gelöscht werden sollte.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "Es ist ein Fehler aufgetreten, als {0} für {1} gelöscht werden sollte.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Fehler beim Löschen",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "Beim Löschen des folgenden app-passwords bzw. app-tokens ist ein Fehler aufgetreten:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "Beim Löschen der folgenden {0} app-passwords und app-tokens ist ein Fehler aufgetreten: ",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Fehler beim Abrufen von Authentifizierungen",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Die Liste mit app-passwords und app-tokens, die zu {0} gehören, konnte nicht abgerufen werden.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Client nicht konfiguriert",
    "GENERIC_NOT_CONFIGURED_MSG": "Die Clientattribute appPasswordAllowed und appTokenAllowed sind nicht konfiguriert. Es können keine Daten abgerufen werden."
};
