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
    "CLEAR_SEARCH": "Vymazat vstup hledání",
    "CLEAR_FILTER": "Vymazat filtr",
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
    "TABLE_BATCH_BAR": "Řádek s akcemi tabulky",
    "TRY_AGAIN": "Zopakujte akci...",
    "UPDATE": "Aktualizovat",

    // Common Column Names
    "CLIENT_NAME_COL": "Jméno klienta",
    "EXPIRES_COL": "Vypršení platnosti",
    "ISSUED_COL": "Vydáno v",
    "NAME_COL": "Název",
    "TYPE_COL": "Typ",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Odstranit tokeny",
    "TOKEN_MGR_DESC": "Odstraňte hesla aplikací a tokeny aplikací pro určeného uživatele.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Zadejte ID uživatele",
    "DELETE_SELECTED": "Odstranit vybrané hesla aplikací tokeny aplikací",
    "DELETE_ARIA": "Odstranit {0} s názvem {1}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Odstranit toto heslo aplikace",
    "DELETE_TOKEN": "Odstranit tento token aplikace",
    "DELETE_FOR_USERID": "{0} pro {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "Tato akce odebere aktuálně přiřazené heslo aplikace.",
    "DELETE_WARNING_TOKEN": "Tato akce odebere aktuálně přiřazený token aplikace.",
    "DELETE_MANY": "Odstranit hesla aplikací/tokeny aplikací",
    "DELETE_MANY_FOR": "Přiřazeno uživateli {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "Tato akce odstraní vybrané heslo aplikace/token aplikace",
    "DELETE_MANY_MESSAGE": "Tato akce odstraní {0} vybraných hesel aplikací/tokenů aplikací.",  // 0 - number
    "DELETE_ALL_MESSAGE": "Tato akce odstraní všechny hesla aplikací/tokeny aplikací patřící k {0}.", // 0 - user id
    "DELETE_NONE": "Vybrat pro odstranění",
    "DELETE_NONE_MESSAGE": "Zaškrtněte políčko, chcete-li označit, která hesla aplikací nebo tokeny aplikací by měly být odstraněny.",
    "SINGLE_ITEM_SELECTED": "Počet vybraných položek: 1",
    "ITEMS_SELECTED": "Počet vybraných položek: {0}",            // 0 - number
    "SELECT_ALL_AUTHS": "Vybrat všechny hesla aplikací a tokeny aplikací pro tohoto uživatele",
    "SELECT_SPECIFIC": "Vybrat {0} s názvem {1} pro odstranění",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Hledáte něco? Zadejte ID uživatele pro zobrazení svých hesel aplikací a tokenů aplikací.",
    "GENERIC_FETCH_FAIL": "Chyba při načítání {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Nelze získat seznam {0} patřící k {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Chyba při odstraňování {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Došlo k chybě při odstraňování {0} s názvem {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "Došlo k chybě při odstraňování {0} pro {1}.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Chyba při odstraňování",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "Došlo k chybě při odstraňování následujícího hesla aplikace nebo tokenu aplikace:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "Došlo k chybě při odstraňování následujících {0} hesel aplikace nebo tokenů aplikace:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Chyba při načítání autentizací",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Nelze získat seznam hesel aplikací a tokenů aplikací patřících k {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Klient není nakonfigurován",
    "GENERIC_NOT_CONFIGURED_MSG": "Atributy klienta appPasswordAllowed a appTokenAllowed nejsou nakonfigurovány. Nelze načíst žádná data."
};
