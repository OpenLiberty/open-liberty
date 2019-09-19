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

    // Client Admin
    "CLIENT_ADMIN_TITLE": "Gestisci client OAuth",
    "CLIENT_ADMIN_DESC": "Utilizzare questo strumento per aggiungere e modificare i client e rigenerare i segreti client.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Filtra per nome client OAuth",
    "ADD_NEW_CLIENT": "Aggiungi nuovo client OAuth.",
    "CLIENT_NAME": "Nome client",
    "CLIENT_ID": "ID Client",
    "EDIT_ARIA": "Modifica il client OAuth {0}",      // {0} - name
    "DELETE_ARIA": "Elimina il client OAuth {0}",  // {0} - name
    "CLIENT_SECRET": "Segreto client",
    "GRANT_TYPES": "Tipi di concessione",
    "SCOPE": "Ambito",
    "PREAUTHORIZED_SCOPE": "Ambito pre-autorizzato (facoltativo)",
    "REDIRECT_URLS": "URL di reindirizzamento (facoltativo)",
    "CLIENT_SECRET_CHECKBOX": "Rigenera segreto client",
    "NONE_SELECTED": "Nessuno selezionato",
    "MODAL_EDIT_TITLE": "Modifica client OAuth",
    "MODAL_REGISTER_TITLE": "Registra nuovo client OAuth",
    "MODAL_SECRET_REGISTER_TITLE": "Registrazione OAuth salvata",
    "MODAL_SECRET_UPDATED_TITLE": "Registrazione OAuth aggiornata",
    "MODAL_DELETE_CLIENT_TITLE": "Elimina questo client OAuth",
    "RESET_GRANT_TYPE": "Deseleziona tutti i tipi di concessione selezionati.",
    "SELECT_ONE_GRANT_TYPE": "Seleziona almeno un tipo di concessione",
    "SPACE_HELPER_TEXT": "Valori separati da spazio",
    "REDIRECT_URL_HELPER_TEXT": "URL di reindirizzamento assoluti separati da spazio",
    "DELETE_OAUTH_CLIENT_DESC": "Questa operazione elimina il client registrato dal servizio di registrazione client.",
    "REGISTRATION_SAVED": "Un ID client e segreto client sono stati generati e assegnati.",
    "REGISTRATION_UPDATED": "Un nuovo segreto client è stato generato e assegnato per questo client.",
    "COPY_CLIENT_ID": "Copia ID client negli appunti",
    "COPY_CLIENT_SECRET": "Copia segreto client negli appunti",
    "REGISTRATION_UPDATED_NOSECRET": "Il client OAuth {0} è aggiornato.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "È necessario selezionare almeno un tipo di concessione.",
    "ERR_REDIRECT_URIS": "I valori devono essere URI assoluti.",
    "GENERIC_REGISTER_FAIL": "Errore durante la registrazione del client OAuth",
    "GENERIC_UPDATE_FAIL": "Errore durante l'aggiornamento del client OAuth",
    "GENERIC_DELETE_FAIL": "Errore durante l'eliminazione del client OAuth",
    "GENERIC_MISSING_CLIENT": "Errore durante il richiamo del client OAuth",
    "GENERIC_REGISTER_FAIL_MSG": "Si è verificato un errore durante la registrazione del client OAuth {0}.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "Si è verificato un errore durante l'aggiornamento del client OAuth {0}.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "Si è verificato un errore durante l'eliminazione del client OAuth {0}.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "Il client OAuth {0} con ID {1} non è stato trovato.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "Si è verificato un errore durante il richiamo delle informazioni sul client OAuth {0}.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Errore durante il richiamo dei client OAuth",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Si è verificato un errore durante il richiamo dell'elenco dei client OAuth.",

    "RESET_SELECTION": "Deseleziona tutti i {0} selezionati",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "Numero di {0} selezionati",     // {0} - field name
    "OPEN_LIST": "Aprire elenco {0}.",                   // {0} - field name
    "CLOSE_LIST": "Chiudere elenco {0}.",                 // {0} - field name
    "ENTER_PLACEHOLDER": "Immetti valore ",
    "ADD_VALUE": "Aggiungi elemento",
    "REMOVE_VALUE": "Rimuovi elemento",
    "REGENERATE_CLIENT_SECRET": "'*' preserva il valore esistente. Un valore vuoto genera un nuovo segreto client. Un valore parametro non vuoto sovrascrive il valore esistente con il nuovo valore specificato. ",
    "ALL_OPTIONAL": "Tutti i campi sono facoltativi. "
};
