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
    "ADDITIONAL_PROPS": "Proprietăţi suplimentare",
    "ADDITIONAL_PROPS_OPTIONAL": "Proprietăţi suplimentare (opţional)",
    "CLIENT_SECRET_CHECKBOX": "Regenerare secret client",
    "PROPERTY_PLACEHOLDER": "Proprietate",
    "VALUE_PLACEHOLDER": "Valoare",
    "GRANT_TYPES_SELECTED": "Număr de tipuri de acordare selectate",
    "GRANT_TYPES_NONE_SELECTED": "Niciunul selectat",
    "MODAL_EDIT_TITLE": "Editare client OAuth",
    "MODAL_REGISTER_TITLE": "Înregistrare client OAuth nou",
    "MODAL_SECRET_REGISTER_TITLE": "Înregistrare OAuth salvată",
    "MODAL_SECRET_UPDATED_TITLE": "Înregistrare OAuth actualizată",
    "MODAL_DELETE_CLIENT_TITLE": "Ştergere acest client OAuth",
    "VALUE_COL": "Valoare",
    "ADD": "Adăugare",
    "DELETE_PROP": "Ştergere proprietate personalizată",
    "RESET_GRANT_TYPE": "Curăţare toate tipurile de acordare selectate",
    "SELECT_ONE_GRANT_TYPE": "Selectaţi cel puţin un tip de acordare",
    "OPEN_GRANT_TYPE": "Deschidere listă de tipuri de acordare",
    "CLOSE_GRANT_TYPE": "Închidere listă de tipuri de acordare",
    "SPACE_HELPER_TEXT": "Valori separate prin spaţiu",
    "REDIRECT_URL_HELPER_TEXT": "URL-uri de redirectare absolute separate prin spaţiu",
    "DELETE_OAUTH_CLIENT_DESC": "Această operaţie şterge client înregistrat de serviciul de înregistrare clienţi.",
    "REGISTRATION_SAVED": "Au fost generate şi alocate un ID de client şi secret de client.",
    "REGISTRATION_UPDATED": "A fost generat şi alocat un nou Secret client pentru acest client.",
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
    "GENERIC_GET_CLIENTS_FAIL_MSG": "A apărut o eroarea la extragerea listei de clienţi OAuth."
};
