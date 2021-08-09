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
    "ADD_NEW": "Adăugare nou",
    "CANCEL": "Anulare",
    "CLEAR_SEARCH": "Curăţare intrare de căutare",
    "CLEAR_FILTER": "Curăţare filtru",
    "CLICK_TO_SORT": "Faceţi clic pentru sortarea coloanei",
    "CLOSE": "Închidere",
    "COPY_TO_CLIPBOARD": "Copiere în clipboard",
    "COPIED_TO_CLIPBOARD": "Copiat în clipboard",
    "DELETE": "Ştergere",
    "DONE": "Gata",
    "EDIT": "Editare",
    "FALSE": "Fals",
    "GENERATE": "Generare",
    "LOADING": "Încărcare",
    "LOGOUT": "Delogare",
    "NEXT_PAGE": "Pagina următoare",
    "NO_RESULTS_FOUND": "Nu au fost găsite rezultate",
    "PAGES": "{0} din {1} pagini",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Selectare număr pagină de vizualizat",
    "PREVIOUS_PAGE": "Pagina anterioară",
    "PROCESSING": "Procesare",
    "REGENERATE": "Regenerare",
    "REGISTER": "Înregistrare",
    "TABLE_FIELD_SORT_ASC": "Tabelul este sortat după {0} în ordine crescătoare.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Tabelul este sortat după {0} în ordine descrescătoare.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "Adevărat",
    "TRY_AGAIN": "Reîncercare...",
    "UPDATE": "Actualizare",

    // Common Column Names
    "CLIENT_NAME_COL": "Nume client",
    "EXPIRES_COL": "Expiră pe",
    "ISSUED_COL": "Emis pe",
    "NAME_COL": "Nume",
    "TYPE_COL": "Tip",

    // Client Admin
    "CLIENT_ADMIN_TITLE": "Gestionare clienţi OAuth",
    "CLIENT_ADMIN_DESC": "Folosiţi această unealtă pentru a adăuga şi edita clienţi, şi pentru a regenera secretele de client.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Filtru pe numele de client OAuth",
    "ADD_NEW_CLIENT": "Adăugare client OAuth nou",
    "CLIENT_NAME": "Nume client",
    "CLIENT_ID": "ID client",
    "EDIT_ARIA": "Editare client OAuth {0}",      // {0} - name
    "DELETE_ARIA": "Ştergere client OAuth {0}",  // {0} - name
    "CLIENT_SECRET": "Secret client",
    "GRANT_TYPES": "Tipuri de acordare",
    "SCOPE": "Domeniu",
    "PREAUTHORIZED_SCOPE": "Domeniu pre-autorizat (opţional)",
    "REDIRECT_URLS": "URL-uri redirectare (opţional)",
    "CLIENT_SECRET_CHECKBOX": "Regenerare secret client",
    "NONE_SELECTED": "Niciunul selectat",
    "MODAL_EDIT_TITLE": "Editare client OAuth",
    "MODAL_REGISTER_TITLE": "Înregistrare client OAuth nou",
    "MODAL_SECRET_REGISTER_TITLE": "Înregistrare OAuth salvată",
    "MODAL_SECRET_UPDATED_TITLE": "Înregistrare OAuth actualizată",
    "MODAL_DELETE_CLIENT_TITLE": "Ştergere acest client OAuth",
    "RESET_GRANT_TYPE": "Curăţaţi toate tipurile de acordare selectate",
    "SELECT_ONE_GRANT_TYPE": "Selectaţi cel puţin un tip de acordare",
    "SPACE_HELPER_TEXT": "Valori separate prin spaţiu",
    "REDIRECT_URL_HELPER_TEXT": "URL-uri de redirectare absolute separate prin spaţiu",
    "DELETE_OAUTH_CLIENT_DESC": "Această operaţie şterge clientul înregistrat de serviciul de înregistrare clienţi.",
    "REGISTRATION_SAVED": "Au fost generate şi alocate un ID de client şi secret de client.",
    "REGISTRATION_UPDATED": "A fost generat şi alocat un nou Secret client pentru acest client.",
    "COPY_CLIENT_ID": "Copiere ID client la clipboard",
    "COPY_CLIENT_SECRET": "Copiere secret client la clipboard",
    "REGISTRATION_UPDATED_NOSECRET": "Clientul OAuth {0} este actualizat.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "Trebuie selectat cel puţin un tip de acordare.",
    "ERR_REDIRECT_URIS": "Valorile trebuie să fie URL-uri absolute.",
    "GENERIC_REGISTER_FAIL": "Eroare la înregistrarea clientului OAuth",
    "GENERIC_UPDATE_FAIL": "Eroare la actualizarea clientului OAuth",
    "GENERIC_DELETE_FAIL": "Eroare la ştergerea clientului OAuth",
    "GENERIC_MISSING_CLIENT": "Eroare la extragerea clientului OAuth",
    "GENERIC_REGISTER_FAIL_MSG": "A apărut o eroarea la înregistrarea clientului OAuth {0}.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "A apărut o eroarea la actualizarea clientului OAuth {0}.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "A apărut o eroarea la ştergerea clientului OAuth {0}.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "Nu a fost găsit clientul OAuth {0} cu ID-ul {1}.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "A apărut o eroarea la extragerea informaţiilor clientului OAuth {0}.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Eroare la extragerea clienţilor OAuth",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "A apărut o eroarea la extragerea listei de clienţi OAuth.",

    "RESET_SELECTION": "Curăţare toate {0} selectate",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "Număr de {0} selectate",     // {0} - field name
    "OPEN_LIST": "Deschideţi lista {0}.",                   // {0} - field name
    "CLOSE_LIST": "Închideţi lista {0}.",                 // {0} - field name
    "ENTER_PLACEHOLDER": "Introduceţi valoarea",
    "ADD_VALUE": "Adăugare element",
    "REMOVE_VALUE": "Înlăturare element",
    "REGENERATE_CLIENT_SECRET": "'*' păstrează valoarea existentă. O valoare blanc generează un nou secret client. Un parametru care nu este blanc înlocuieşte valoarea existentă cu valoarea nou specificată. ",
    "ALL_OPTIONAL": "Toate câmpurile sunt opţionale. "
};
