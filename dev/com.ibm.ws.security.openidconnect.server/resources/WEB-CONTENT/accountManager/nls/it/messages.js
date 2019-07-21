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
    "CLEAR": "Cancella input di ricerca",
    "CLICK_TO_SORT": "Fare clic per ordinare la colonna",
    "CLOSE": "Chiudi",
    "COPY_TO_CLIPBOARD": "Copia negli appunti",
    "COPIED_TO_CLIPBOARD": "Copiato negli appunti",
    "DELETE": "Elimina",
    "DONE": "Eseguito",
    "EDIT": "Modifica",
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
    "TRY_AGAIN": "Riprova...",
    "UPDATE": "Aggiorna",

    // Common Column Names
    "EXPIRES_COL": "Data di scadenza",
    "ISSUED_COL": "Data di emissione",
    "NAME_COL": "Nome",
    "TYPE_COL": "Tipo",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Gestisci token personali",
    "ACCT_MGR_DESC": "Crea, elimina e rigenera password e token app.",
    "ADD_NEW_AUTHENTICATION": "Aggiungi nuova password o token app",
    "NAME_IDENTIFIER": "Nome: {0}",
    "ADD_NEW_TITLE": "Registra nuova autenticazione",
    "NOT_GENERATED_PLACEHOLDER": "Non generato",
    "REGENERATE_APP_PASSWORD": "Rigenera password app",
    "REGENERATE_PW_WARNING": "Questa azione sovrascriverà la password app corrente.",
    "REGENERATE_PW_PLACEHOLDER": "Password precedentemente generata il {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "Rigenera token app",
    "REGENERATE_TOKEN_WARNING": "Questa azione sovrascriverà il token app corrente.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Token precedentemente generato il {0}",        // 0 - date
    "DELETE_PW": "Elimina questa password app",
    "DELETE_TOKEN": "Elimina questo token app",
    "DELETE_WARNING_PW": "Questa azione rimuoverà la password app attualmente assegnata.",
    "DELETE_WARNING_TOKEN": "Questa azione rimuoverà il token app attualmente assegnato.",
    "REGENERATE_ARIA": "Rigenera {0} per {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Elimina {0} denominato {1}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Errore durante la generazione di {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "Si è verificato un errore durante la generazione di un nuovo {0} con il nome {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "Il nome è già associato a un {0}, oppure è troppo lungo.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Errore durante l'eliminazione di {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Si è verificato un errore durante l'eliminazione di {0} denominato {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Errore durante la rigenerazione di {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "Si è verificato un errore durante la rigenerazione del {0} denominato {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "Si è verificato un errore durante la rigenerazione del {0} denominato {1}. {0} è stato eliminato ma non è stato possibile ricrearlo.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Errore durante il richiamo delle autenticazioni",
    "GENERIC_FETCH_FAIL_MSG": "Impossibile ottenere l'elenco corrente di password o token app.",
    "GENERIC_NOT_CONFIGURED": "Client non configurato",
    "GENERIC_NOT_CONFIGURED_MSG": "Gli attributi client appPasswordAllowed e appTokenAllowed non sono configurati.  Non è possibile richiamare alcun dato.",
    "APP_PASSWORD_NOT_CONFIGURED": "L'attributo client appPasswordAllowed non è configurato.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "L'attributo client appTokenAllowed non è configurato."         // 'appTokenAllowed' is a config option.  Do not translate.
};
