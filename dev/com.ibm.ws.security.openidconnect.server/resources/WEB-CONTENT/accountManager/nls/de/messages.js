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
    "CLEAR": "Sucheingabe löschen",
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
    "TABLE_FIELD_SORT_ASC": "Die Tabelle ist nach {0} in aufsteigender Reihenfolge sortiert. ",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Die Tabelle ist nach {0} in absteigender Reihenfolge sortiert. ", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Vorgang wiederholen...",
    "UPDATE": "Aktualisieren",

    // Common Column Names
    "EXPIRES_COL": "Ablaufdatum",
    "ISSUED_COL": "Austelldatum",
    "NAME_COL": "Name",
    "TYPE_COL": "Typ",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Persönliche Token verwalten",
    "ACCT_MGR_DESC": "Erstellen Sie app-passwords und app-tokens, löschen Sie sie und generieren Sie sie neu. ",
    "ADD_NEW_AUTHENTICATION": "Neues app-password oder app-token hinzufügen",
    "NAME_IDENTIFIER": "Name: {0}",
    "ADD_NEW_TITLE": "Neue Authentifizierung registrieren",
    "NOT_GENERATED_PLACEHOLDER": "Nicht generiert",
    "AUTHENTICAION_GENERATED": "Generierte Authentifizierung",
    "GENERATED_APP_PASSWORD": "Generiertes app-password",
    "GENERATED_APP_TOKEN": "Generiertes app-token",
    "COPY_APP_PASSWORD": "app-password in die Zwischenablage kopieren",
    "COPY_APP_TOKEN": "app-token in Zwischenablage kopieren",
    "REGENERATE_APP_PASSWORD": "App-Password neu generieren",
    "REGENERATE_PW_WARNING": "Diese Aktion überschreibt das aktuelle app-password.",
    "REGENERATE_PW_PLACEHOLDER": "Das Kennwort wurde zuvor am {0} generiert.",        // 0 - date
    "REGENERATE_APP_TOKEN": "App-Token neu generieren",
    "REGENERATE_TOKEN_WARNING": "Diese Aktion überschreibt das aktuelle app-token.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Das Token wurde zuvor am {0} generiert.",        // 0 - date
    "DELETE_PW": "Dieses app-password löschen",
    "DELETE_TOKEN": "Dieses app-token löschen",
    "DELETE_WARNING_PW": "Diese Aktion entfernt das momentan zugeordnete app-password.",
    "DELETE_WARNING_TOKEN": "Diese Aktion entfernt das momentan zugeordnete app-token.",
    "REGENERATE_ARIA": "{0} für {1} neu generieren",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "{0} mit dem Namen {1} löschen",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Fehler beim Generieren von {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "Es ist ein Fehler aufgetreten, als ein neues {0} mit dem Namen {1} generiert werden sollte.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "Der Name ist bereits einem {0} zugeordnet oder es ist zu lang.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Fehler beim Löschen von {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Es ist ein Fehler aufgetreten, als {0} mit dem Namen {1} gelöscht werden sollte.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Fehler beim Neugenerieren von {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "Es ist ein Fehler aufgetreten, als {0} mit dem Namen {1} neu generiert werden sollte.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "Es ist ein Fehler aufgetreten, als {0} mit dem Namen {1} neu generiert werden sollte. Das {0} wurde gelöscht, es konnte jedoch nicht erneut erstellt werden. ", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Fehler beim Abrufen von Authentifizierungen",
    "GENERIC_FETCH_FAIL_MSG": "Die Liste mit den aktuellen app-passwords oder app-tokens konnte nicht abgerufen werden.",
    "GENERIC_NOT_CONFIGURED": "Client nicht konfiguriert",
    "GENERIC_NOT_CONFIGURED_MSG": "Die Clientattribute appPasswordAllowed und appTokenAllowed sind nicht konfiguriert. Es können keine Daten abgerufen werden.",
    "APP_PASSWORD_NOT_CONFIGURED": "Das Clientattribut appPasswordAllowed ist nicht konfiguriert. ",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "Das Clientattribut appTokenAllowed ist nicht konfiguriert. "         // 'appTokenAllowed' is a config option.  Do not translate.
};
