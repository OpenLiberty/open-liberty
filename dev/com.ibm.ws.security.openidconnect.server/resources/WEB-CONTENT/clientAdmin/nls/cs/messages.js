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
    "FALSE": "Ne",
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
    "TABLE_FIELD_SORT_ASC": "Tabulka je řazena podle {0} vzestupném pořadí.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Tabulka je řazena podle {0} sestupném pořadí.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "Ano",
    "TRY_AGAIN": "Zopakujte akci...",
    "UPDATE": "Aktualizovat",

    // Common Column Names
    "CLIENT_NAME_COL": "Jméno klienta",
    "EXPIRES_COL": "Vypršení platnosti",
    "ISSUED_COL": "Vydáno",
    "NAME_COL": "Název",
    "TYPE_COL": "Typ",

    // Client Admin
    "CLIENT_ADMIN_TITLE": "Správa klientů OAuth",
    "CLIENT_ADMIN_DESC": "Tento nástroj slouží k přidávání a úpravám klientů a znovugenerování tajných klíčů klienta.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Filtrovat podle jména klienta OAuth",
    "ADD_NEW_CLIENT": "Přidejte nového klienta OAuth.",
    "CLIENT_NAME": "Jméno klienta",
    "CLIENT_ID": "ID klienta",
    "EDIT_ARIA": "Upravit klienta OAuth {0}",      // {0} - name
    "DELETE_ARIA": "Odstranit klienta OAuth {0}",  // {0} - name
    "CLIENT_SECRET": "Tajný klíč klienta",
    "GRANT_TYPES": "Typy udělení",
    "SCOPE": "Rozsah",
    "PREAUTHORIZED_SCOPE": "Předem autorizovaný rozsah (volitelný)",
    "REDIRECT_URLS": "-Přesměrovat adresy URL (volitelné)",
    "CLIENT_SECRET_CHECKBOX": "Znovu vygenerovat tajný klíč klienta",
    "NONE_SELECTED": "Bez výběru",
    "MODAL_EDIT_TITLE": "Upravit klienta OAuth",
    "MODAL_REGISTER_TITLE": "Registrovat nového klienta OAuth",
    "MODAL_SECRET_REGISTER_TITLE": "Registrace OAuth byla uložena",
    "MODAL_SECRET_UPDATED_TITLE": "Registrace OAuth byla aktualizována",
    "MODAL_DELETE_CLIENT_TITLE": "Odstranit tohoto klienta OAuth",
    "RESET_GRANT_TYPE": "Vymažte všechny vybrané typy udělení.",
    "SELECT_ONE_GRANT_TYPE": "Vybrat alespoň jeden typ udělení",
    "SPACE_HELPER_TEXT": "Hodnoty oddělené mezerami",
    "REDIRECT_URL_HELPER_TEXT": "Mezerami oddělené absolutní adresy URL pro přesměrování",
    "DELETE_OAUTH_CLIENT_DESC": "Tato operace odstraní registrovaného klienta ze služby registrace klienta.",
    "REGISTRATION_SAVED": "ID klienta a tajný klíč klienta byly generovány a přiřazeny.",
    "REGISTRATION_UPDATED": "Byl vygenerován a přiřazen nový tajný klíč klienta pro tohoto klienta.",
    "COPY_CLIENT_ID": "Kopírovat ID klienta do schránky",
    "COPY_CLIENT_SECRET": "Kopírovat tajný klíč klienta do schránky",
    "REGISTRATION_UPDATED_NOSECRET": "Klienta OAuth {0} byl aktualizován.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "Musí být vybrán alespoň jeden typ udělení.",
    "ERR_REDIRECT_URIS": "Hodnoty musí být absolutní identifikátory URI.",
    "GENERIC_REGISTER_FAIL": "Chyba při registraci klienta OAuth",
    "GENERIC_UPDATE_FAIL": "Chyba při aktualizaci klienta OAuth",
    "GENERIC_DELETE_FAIL": "Chyba při odstraňování klienta OAuth",
    "GENERIC_MISSING_CLIENT": "Chyba při načítání klienta OAuth",
    "GENERIC_REGISTER_FAIL_MSG": "Došlo k chybě při registraci klienta OAuth {0}.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "Došlo k chybě při aktualizaci klienta OAuth {0}.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "Došlo k chybě při odstraňování klienta OAuth {0}.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "Klient OAuth {0} s ID {1} nebyl nalezen.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "Došlo k chybě při načítání informací o klientovi OAuth {0}.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Chyba při načítání klientů OAuth",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Došlo k chybě při načítání seznamu klientů OAuth.",

    "RESET_SELECTION": "Vymazat všechny vybrané {0}",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "Počet vybraných {0}",     // {0} - field name
    "OPEN_LIST": "Otevřete seznam {0}.",                   // {0} - field name
    "CLOSE_LIST": "Zavřete seznam {0}.",                 // {0} - field name
    "ENTER_PLACEHOLDER": "Zadat hodnotu",
    "ADD_VALUE": "Přidat prvek",
    "REMOVE_VALUE": "Odebrat prvek",
    "REGENERATE_CLIENT_SECRET": "'*' zachovává existující hodnotu. Prázdná hodnota generuje nový tajný klíč klienta. Neprázdná hodnota parametru potlačuje existující hodnotu nově určenou hodnotou.",
    "ALL_OPTIONAL": "Všechna pole jsou volitelná"
};
