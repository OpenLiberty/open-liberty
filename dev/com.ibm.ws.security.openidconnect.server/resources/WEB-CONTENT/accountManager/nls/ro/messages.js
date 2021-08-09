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
    "CLEAR": "Curăţare intrare de căutare",
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
    "EXPIRES_COL": "Expiră pe",
    "ISSUED_COL": "Emis pe",
    "NAME_COL": "Nume",
    "TYPE_COL": "Tip",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Gestionare jetoane personale",
    "ACCT_MGR_DESC": "Creaţi, ştergeţi şi regeneraţi app-password-uri şi app-token-uri .",
    "ADD_NEW_AUTHENTICATION": "Adăugaţi un app-password sau app-token nou.",
    "NAME_IDENTIFIER": "Nume: {0}",
    "ADD_NEW_TITLE": "Înregistrare autentificare nouă",
    "NOT_GENERATED_PLACEHOLDER": "Negenerat",
    "AUTHENTICAION_GENERATED": "Autentificare generată.",
    "GENERATED_APP_PASSWORD": "App-password generat",
    "GENERATED_APP_TOKEN": "App-token generat",
    "COPY_APP_PASSWORD": "Copiaţi app-password la clipboard",
    "COPY_APP_TOKEN": "Copiaţi app-token la clipboard",
    "REGENERATE_APP_PASSWORD": "Regenerare app-password",
    "REGENERATE_PW_WARNING": "Această acţiune va suprascrie app-password curent.",
    "REGENERATE_PW_PLACEHOLDER": "Parola anterioară a fost generată pe {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "Regenerare app-token",
    "REGENERATE_TOKEN_WARNING": "Această acţiune va suprascrie app-token curent.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Jetonul anterior a fost generat pe {0}",        // 0 - date
    "DELETE_PW": "Ştergere acest app-password.",
    "DELETE_TOKEN": "Ştergere acest app-token.",
    "DELETE_WARNING_PW": "Această acţiune va înlătura app-password alocat curent.",
    "DELETE_WARNING_TOKEN": "Această acţiune va înlătura app-token alocat curent.",
    "REGENERATE_ARIA": "Regenerare {0} pentru {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Ştergere {0} numit(ă) {1}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Eroare generare {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "A apărut o eroare la generarea unui nou {0} cu numele {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "Numnele este deja asociat cu un {0}, sau este prea lung.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Eroare la ştergere {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "A apărut o eroarea la ştergerea {0} cu numele {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Eroare la regenerarea {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "A apărut o eroarea la regenerarea {0} cu numele {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "A apărut o eroarea la regenerarea {0} cu numele {1}. {0} a fost şters, dar nu a putut fi recreat.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Eroare la extragerea autentificărilor",
    "GENERIC_FETCH_FAIL_MSG": "Nu s-a putut obţine lista curentă de app-password-uri sau app-token-uri.",
    "GENERIC_NOT_CONFIGURED": "Clientul nu este configurat",
    "GENERIC_NOT_CONFIGURED_MSG": "Atributele de client appPasswordAllowed şi appTokenAllowed nu sunt configurate.  Nu au putut fi extrase date.",
    "APP_PASSWORD_NOT_CONFIGURED": "Atributul de client appPasswordAllowed nu este configurat.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "Atributul de client appTokenAllowed nu este configurat."         // 'appTokenAllowed' is a config option.  Do not translate.
};
