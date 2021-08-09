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
    "TABLE_BATCH_BAR": "Bară de acţiune tabel",
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

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Ştergere jetoane",
    "TOKEN_MGR_DESC": "Ştergeţi app-password-uri şi app-token-uri pentru un utilizator specificat.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Introduceţi ID-ul de utilizator",
    "TABLE_FILLED_WITH": "Tabelul a fost actualizat pentru a arăta autentificările {0} care îi aparţin lui {1}.",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "Ştergeţi app-password-uri şi app-token-uri selectate . ",
    "DELETE_ARIA": "Ştergere {0} numit(ă) {1}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Ştergere acest app-password.",
    "DELETE_TOKEN": "Ştergere acest app-token.",
    "DELETE_FOR_USERID": "{0} pentru {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "Această acţiune va înlătura app-password alocat curent.",
    "DELETE_WARNING_TOKEN": "Această acţiune va înlătura app-token alocat curent.",
    "DELETE_MANY": "Ştergere app-password-uri/app-token-uri",
    "DELETE_MANY_FOR": "Alocate la {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "Această acţiune va şterge app-password-urile/app-token-urile selectate.",
    "DELETE_MANY_MESSAGE": "Această acţiune va şterge app-password-urile/app-token-urile {0} selectate",  // 0 - number
    "DELETE_ALL_MESSAGE": "Această acţiune va şterge app-password-urile/app-token-urile aparţinând lui {0}.", // 0 - user id
    "DELETE_NONE": "Selectare pentru ştergere",
    "DELETE_NONE_MESSAGE": "Selectaţi o casetă de bifare pentru a indica care app-password-uri sau app-token-uri trebuie şterse.",
    "SINGLE_ITEM_SELECTED": "1 articol selectat",
    "ITEMS_SELECTED": "{0} articole selectate",            // 0 - number
    "SELECT_ALL_AUTHS": "Selectaţi toate app-password-urile şi app-token-urile pentru acest utilizator.",
    "SELECT_SPECIFIC": "Selectaţi {0} cu numele {1} pentru ştergere.",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Căutaţi ceva? Introduceţi un ID de utilizator pentru a vizualiza app-password-urile şi app-token-urile lor.",
    "GENERIC_FETCH_FAIL": "Eroare la extragerea {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Nu s-a putut obţine lista de {0} aparţinând lui {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Eroare la ştergere {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "A apărut o eroarea la ştergerea {0} cu numele {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "A apărut o eroarea la ştergerea {0} pentru {1}.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Eroare la ştergere",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "A apărut o eroarea la ştergerea următorului app-password sau app-token:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "A apărut o eroare la ştergerea următoarele app-password-uri şi app-token-uri {0}:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Eroare la extragerea autentificărilor",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Nu s-a putut obţine lista de app-pasword-uri şi app-token-uri aparţinând lui {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Clientul nu este configurat",
    "GENERIC_NOT_CONFIGURED_MSG": "Atributele de client appPasswordAllowed şi appTokenAllowed nu sunt configurate.  Nu au putut fi extrase date."
};
