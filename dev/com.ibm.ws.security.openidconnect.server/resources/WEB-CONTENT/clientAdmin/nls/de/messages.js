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
    "GENERATE": "Generieren",
    "LOADING": "Wird geladen",
    "LOGOUT": "Anmelden",
    "NEXT_PAGE": "Nächste Seite",
    "NO_RESULTS_FOUND": "Keine Ergebnisse gefunden",
    "PAGES": "{0} von {1} Seiten",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Seitennummer zur Ansicht auswählen",
    "PREVIOUS_PAGE": "Vorherige Seite",
    "PROCESSING": "Wird verarbeitet",
    "REGENERATE": "Neu generieren",
    "REGISTER": "Registrieren",
    "TRY_AGAIN": "Wiederholen Sie den Vorgang...",
    "UPDATE": "Aktualisieren",

    // Common Column Names
    "CLIENT_NAME_COL": "Clientname",
    "EXPIRES_COL": "Ablaufzeit",
    "ISSUED_COL": "Ausgegeben am",
    "NAME_COL": "Name",
    "TYPE_COL": "Typ",

    // Client Admin
    "CLIENT_ADMIN_TITLE": "OAuth-Clients verwalten",
    "CLIENT_ADMIN_DESC": "Verwenden Sie dieses Tool, um Clients hinzuzufügen und zu bearbeiten, und um geheime Clientschlüssel neu zu generieren.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Nach OAuth-Clientnamen filtern",
    "ADD_NEW_CLIENT": "Neuen OAuth-Client hinzufügen",
    "CLIENT_NAME": "Clientname",
    "CLIENT_ID": "Client-ID",
    "EDIT_ARIA": "OAuth-Client {0} bearbeiten",      // {0} - name
    "DELETE_ARIA": "OAuth-Client {0} löschen",  // {0} - name
    "CLIENT_SECRET": "Geheimer Clientschlüssel",
    "GRANT_TYPES": "Granttypen",
    "SCOPE": "Geltungsbereich",
    "PREAUTHORIZED_SCOPE": "Vorab autorisierter Geltungsbereich (optional)",
    "REDIRECT_URLS": "URLs umleiten (optional)",
    "ADDITIONAL_PROPS": "Weitere Eigenschaften",
    "ADDITIONAL_PROPS_OPTIONAL": "Weitere Eigenschaften (optional)",
    "CLIENT_SECRET_CHECKBOX": "Geheimen Clientschlüssel neu generieren",
    "PROPERTY_PLACEHOLDER": "Eigenschaft",
    "VALUE_PLACEHOLDER": "Wert",
    "GRANT_TYPES_SELECTED": "Anzahl der ausgewählten Granttypen",
    "GRANT_TYPES_NONE_SELECTED": "Keine ausgewählt",
    "MODAL_EDIT_TITLE": "OAuth-Client bearbeiten",
    "MODAL_REGISTER_TITLE": "Neuen OAuth-Client registrieren",
    "MODAL_SECRET_REGISTER_TITLE": "OAuth-Registrierung gespeichert",
    "MODAL_SECRET_UPDATED_TITLE": "OAuth-Registrierung aktualisiert",
    "MODAL_DELETE_CLIENT_TITLE": "Diesen OAuth-Client löschen",
    "VALUE_COL": "Wert",
    "ADD": "Hinzufügen",
    "DELETE_PROP": "Angepasste Eigenschaft löschen",
    "RESET_GRANT_TYPE": "Alle ausgewählten Granttypen abwählen",
    "SELECT_ONE_GRANT_TYPE": "Mindestens einen Granttypen auswählen",
    "OPEN_GRANT_TYPE": "Liste mit Granttypen öffnen",
    "CLOSE_GRANT_TYPE": "Liste mit Granttypen schließen",
    "SPACE_HELPER_TEXT": "Durch Leerschritte getrennte Werte",
    "REDIRECT_URL_HELPER_TEXT": "Absolute Umleitungs-URLs durch Leerschritte voneinander getrennt.",
    "DELETE_OAUTH_CLIENT_DESC": "Diese Operation löscht den registrierten Client aus dem Clientregistrierungsservice.",
    "REGISTRATION_SAVED": "Es wurden eine Client-ID und ein geheimer Clientschlüssel generiert und zugeordnet.",
    "REGISTRATION_UPDATED": "Es wurde ein neuer geheimer Clientschlüssel für diesen Client generiert und ihm zugeordnet. ",
    "REGISTRATION_UPDATED_NOSECRET": "Der OAuth-Client {0} wurde aktualisiert.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "Mindestens ein Granttyp muss ausgewählt werden. ",
    "ERR_REDIRECT_URIS": "Werte müssen absolute URIs sein.",
    "GENERIC_REGISTER_FAIL": "Fehler beim Registrieren des OAuth-Clients",
    "GENERIC_UPDATE_FAIL": "Fehler beim Aktualisieren des OAuth-Clients",
    "GENERIC_DELETE_FAIL": "Fehler beim Löschen des OAuth-Clients",
    "GENERIC_MISSING_CLIENT": "Fehler beim Abrufen des OAuth-Clients",
    "GENERIC_REGISTER_FAIL_MSG": "Beim Registrieren des OAuth-Clients {0} ist ein Fehler aufgetreten.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "Beim Aktualisieren des OAuth-Clients {0} ist ein Fehler aufgetreten.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "Beim Löschen des OAuth-Clients {0} ist ein Fehler aufgetreten.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "Der OAuth-Client {0} mit der ID {1} wurde nicht gefunden.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "Beim Abrufen von Informationen zum OAuth-Client {0} ist ein Fehler aufgetreten.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Fehler beim Abrufen von OAuth-Clients",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Beim Abrufen der Liste mit den OAuth-Clients ist ein Fehler aufgetreten."
};
