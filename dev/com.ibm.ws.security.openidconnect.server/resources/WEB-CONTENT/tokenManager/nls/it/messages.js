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
    "ADD_NEW": "Aggiungi nuovo",
    "CANCEL": "Annulla",
    "CLEAR_SEARCH": "Cancella input di ricerca",
    "CLEAR_FILTER": "Cancella filtro",
    "CLICK_TO_SORT": "Fare clic per ordinare la colonna",
    "CLOSE": "Chiudi",
    "COPY_TO_CLIPBOARD": "Copia negli appunti",
    "COPIED_TO_CLIPBOARD": "Copiato negli appunti",
    "DELETE": "Elimina",
    "DONE": "Eseguito",
    "EDIT": "Modifica",
    "FALSE": "False",
    "GENERATE": "Genera",
    "LOADING": "Caricamento",
    "LOGOUT": "Logout",
    "NEXT_PAGE": "Pagina successiva",
    "NO_RESULTS_FOUND": "Nessun risultato trovato",
    "PAGES": "{0} di {1} pagine",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Seleziona il numero pagina da visualizzare",
    "PREVIOUS_PAGE": "Pagina precedente",
    "PROCESSING": "In elaborazione",
    "REGENERATE": "Rigenera",
    "REGISTER": "Registra",
    "TABLE_BATCH_BAR": "Barra azioni tabella",
    "TABLE_FIELD_SORT_ASC": "La tabella è ordinata per {0} in sequenza crescente. ",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "La tabella è ordinata per {0} in sequenza decrescente. ", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Riprova...",
    "UPDATE": "Aggiorna",

    // Common Column Names
    "CLIENT_NAME_COL": "Nome client",
    "EXPIRES_COL": "Data scadenza",
    "ISSUED_COL": "Data emissione",
    "NAME_COL": "Nome",
    "TYPE_COL": "Tipo",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Elimina token",
    "TOKEN_MGR_DESC": "Elimina app-password e app-token per l'utente specificato.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Immetti ID utente",
    "TABLE_FILLED_WITH": "La tabella è stata aggiornata per mostrare {0} autenticazioni appartenenti a {1}.",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "Elimina app-password e app-token selezionati.",
    "DELETE_ARIA": "Elimina {0} denominato {1}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Elimina questa app-password",
    "DELETE_TOKEN": "Elimina questo app_token",
    "DELETE_FOR_USERID": "{0} per {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "Questa azione rimuoverà la app_password attualmente assegnata.",
    "DELETE_WARNING_TOKEN": "Questa azione rimuoverà l'app_token attualmente assegnato.",
    "DELETE_MANY": "Elimina App-Password/App-Token",
    "DELETE_MANY_FOR": "Assegnato a {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "Questa azione eliminerà l'app-password/app-token selezionati.",
    "DELETE_MANY_MESSAGE": "Questa azione eliminerà {0}  app-password/app-token selezionati.",  // 0 - number
    "DELETE_ALL_MESSAGE": "Questa azione eliminerà tutte gli app-password/app-token che appartengono a {0}.", // 0 - user id
    "DELETE_NONE": "Seleziona per eliminazione",
    "DELETE_NONE_MESSAGE": "Selezionare una casella di spunta per indicare quali app-password o app-token devono essere eliminati.",
    "SINGLE_ITEM_SELECTED": "1 elemento selezionato",
    "ITEMS_SELECTED": "{0} elementi selezionati",            // 0 - number
    "SELECT_ALL_AUTHS": "Selezionare tutte le app-password e gli app-token per questo utente.",
    "SELECT_SPECIFIC": "Selezionare {0} denominato {1} per l'eliminazione.",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Cerchi qualcosa? Immetti un ID utente per visualizzare le app-password e gli app-token.",
    "GENERIC_FETCH_FAIL": "Errore durante il richiamo di {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Impossibile ottenere l'elenco di {0} che appartengono a {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Errore durante l'eliminazione di {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Si è verificato un errore durante l'eliminazione di {0} denominato {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "Si è verificato un errore durante l'eliminazione di {0} per {1}.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Errore durante l'eliminazione",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "Si è verificato un errore durante l'eliminazione della seguente app-password o app-token:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "Si è verificato un errore durante l'eliminazione delle seguenti {0} app-password e app-token:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Errore durante il richiamo delle autenticazioni",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Impossibile ottenere l'elenco di app-password e app-token che appartengono a {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Client non configurato",
    "GENERIC_NOT_CONFIGURED_MSG": "Gli attributi client appPasswordAllowed e appTokenAllowed non sono configurati.  Non è possibile richiamare alcun dato."
};
