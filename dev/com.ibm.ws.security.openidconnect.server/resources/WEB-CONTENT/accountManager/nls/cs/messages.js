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
    "ADD_NEW": "Přidat nový",
    "CANCEL": "Storno",
    "CLEAR": "Vymazat vstup hledání",
    "CLICK_TO_SORT": "Klepnutím seřadíte sloupec",
    "CLOSE": "Zavřít",
    "COPY_TO_CLIPBOARD": "Kopírovat do schránky",
    "COPIED_TO_CLIPBOARD": "Zkopírováno do schránky",
    "DELETE": "Odstranit",
    "DONE": "Hotovo",
    "EDIT": "Upravit",
    "GENERATE": "Generovat",
    "LOADING": "Načítání",
    "LOGOUT": "Odhlásit",
    "NEXT_PAGE": "Další stránka",
    "NO_RESULTS_FOUND": "Nebyly nalezeny žádné výsledky",
    "PAGES": "Stránka {0} z {1}",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Vyberte číslo stránky pro zobrazení",
    "PREVIOUS_PAGE": "Předchozí stránka",
    "PROCESSING": "Zpracování",
    "REGENERATE": "Regenerovat",
    "REGISTER": "Registrovat",
    "TRY_AGAIN": "Zopakujte akci...",
    "UPDATE": "Aktualizovat",

    // Common Column Names
    "EXPIRES_COL": "Vypršení platnosti",
    "ISSUED_COL": "Vydáno v",
    "NAME_COL": "Název",
    "TYPE_COL": "Typ",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Správa osobních tokenů",
    "ACCT_MGR_DESC": "Vytvořte, odstraňte a znovu vygenerujte hesla aplikací a tokeny aplikací.",
    "ADD_NEW_AUTHENTICATION": "Přidat nové heslo aplikace nebo token aplikace",
    "NAME_IDENTIFIER": "Název: {0}",
    "ADD_NEW_TITLE": "Registrovat nové ověření",
    "NOT_GENERATED_PLACEHOLDER": "Negenerované",
    "REGENERATE_APP_PASSWORD": "Znovu generovat heslo aplikace",
    "REGENERATE_PW_WARNING": "Tato akce přepíše aktuální heslo aplikace.",
    "REGENERATE_PW_PLACEHOLDER": "Heslo dříve generované dne {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "Znovu generovat token aplikace",
    "REGENERATE_TOKEN_WARNING": "Tato akce přepíše aktuální token aplikace.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Token dříve generovaný dne {0}",        // 0 - date
    "DELETE_PW": "Odstranit toto heslo aplikace",
    "DELETE_TOKEN": "Odstranit tento token aplikace",
    "DELETE_WARNING_PW": "Tato akce odebere aktuálně přiřazené heslo aplikace.",
    "DELETE_WARNING_TOKEN": "Tato akce odebere aktuálně přiřazený token aplikace.",
    "REGENERATE_ARIA": "Znovu generovat {0} pro {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Odstranit {0} s názvem {1}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Chyba při generování {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "Došlo k chybě při generování nového {0} s názvem {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "Název je již přidružen k {0}, nebo je příliš dlouhý.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Chyba při odstraňování {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Došlo k chybě při odstraňování {0} s názvem {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Chyba při opětném generování {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "Došlo k chybě při regenerování {0} s názvem {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "Došlo k chybě při regenerování {0} s názvem {1}. {0} bylo odstraněno, ale nebylo možné znovuvytvoření.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Chyba při načítání autentizací",
    "GENERIC_FETCH_FAIL_MSG": "Nelze získat aktuální seznam hesel aplikací nebo tokenů aplikací.",
    "GENERIC_NOT_CONFIGURED": "Klient není nakonfigurován",
    "GENERIC_NOT_CONFIGURED_MSG": "Atributy klienta appPasswordAllowed a appTokenAllowed nejsou nakonfigurovány. Nelze načíst žádná data.",
    "APP_PASSWORD_NOT_CONFIGURED": "Atribut appPasswordAllowed klienta není nakonfigurován.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "Atribut appTokenAllowed klienta není nakonfigurován."         // 'appTokenAllowed' is a config option.  Do not translate.
};
