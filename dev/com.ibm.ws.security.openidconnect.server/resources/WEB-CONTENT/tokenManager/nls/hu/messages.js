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
    "ADD_NEW": "Új hozzáadása",
    "CANCEL": "Mégse",
    "CLEAR_SEARCH": "Keresési adatbevitel törlése",
    "CLEAR_FILTER": "Szűrő törlése",
    "CLICK_TO_SORT": "Kattintson ide az oszlop rendezéséhez",
    "CLOSE": "Bezárás",
    "COPY_TO_CLIPBOARD": "Másolás a vágólapra",
    "COPIED_TO_CLIPBOARD": "Vágólapra másolva",
    "DELETE": "Törlés",
    "DONE": "Kész",
    "EDIT": "Szerkesztés",
    "FALSE": "False",
    "GENERATE": "Előállítás",
    "LOADING": "Betöltés",
    "LOGOUT": "Kijelentkezés",
    "NEXT_PAGE": "Következő oldal",
    "NO_RESULTS_FOUND": "Nincsenek találatok",
    "PAGES": "{0} / {1} oldal",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Válassza ki a megjelenítendő oldalszámot",
    "PREVIOUS_PAGE": "Előző oldal",
    "PROCESSING": "Feldolgozás",
    "REGENERATE": "Újragenerálás",
    "REGISTER": "Regisztrálás",
    "TABLE_BATCH_BAR": "Táblázat műveletsor",
    "TABLE_FIELD_SORT_ASC": "A tábla {0} szerint növekvő sorrendben van rendezve.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "A tábla {0} szerint csökkenő sorrendben van rendezve.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Próbálkozzon újra...",
    "UPDATE": "Frissítés",

    // Common Column Names
    "CLIENT_NAME_COL": "Ügyfél neve",
    "EXPIRES_COL": "Lejárat",
    "ISSUED_COL": "Kibocsátva",
    "NAME_COL": "Név",
    "TYPE_COL": "Típus",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Tokenek törlése",
    "TOKEN_MGR_DESC": "Megadott felhasználó app-password és app-token elemeinek törlése",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Adja meg a felhasználói azonosítót",
    "TABLE_FILLED_WITH": "A tábla {1} hitelesítéseire vonatkozóan {0} hitelesítés megjelenítésére lett frissítve.",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "Törölje a kijelölt app-password és app-token elemeket. ",
    "DELETE_ARIA": "Törölje a(z) {0} elemet (név: {1})",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Az app-password törlése",
    "DELETE_TOKEN": "Az app-token törlése",
    "DELETE_FOR_USERID": "{0} a következőhöz: {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "Ez a művelet el fogja távolítani a jelenleg hozzárendelt app-password elemet.",
    "DELETE_WARNING_TOKEN": "Ez a művelet el fogja távolítani a jelenleg hozzárendelt app-token elemet.",
    "DELETE_MANY": "App-Password/App-Token elemek törlése",
    "DELETE_MANY_FOR": "Hozzárendelve ehhez: {0}.",              // 0 - user id
    "DELETE_ONE_MESSAGE": "Ez a művelet törli a kijelölt app-password/app-token elemet. ",
    "DELETE_MANY_MESSAGE": "Ez a művelet törli a kijelölt {0} app-password/app-token elemet",  // 0 - number
    "DELETE_ALL_MESSAGE": "Ez a művelet a következőhöz tartozó összes app-password/app-token elemet törölni fogja: {0}.", // 0 - user id
    "DELETE_NONE": "Kijelölés törlésre",
    "DELETE_NONE_MESSAGE": "Jelölőnégyzet kiválasztásával jelezze, hogy mely app-password vagy app-token elemeket kell törölni.",
    "SINGLE_ITEM_SELECTED": "1 elem lett kijelölve",
    "ITEMS_SELECTED": "{0} kiválasztott elem",            // 0 - number
    "SELECT_ALL_AUTHS": "Válassza ki a felhasználóhoz tartozó összes app-password és app-token elemet. ",
    "SELECT_SPECIFIC": "A(z) {0} (név: {1}) kijelölése törlésre.",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Keres valamit? Adjon meg egy felhasználói azonosítót, és így megtekintheti azok  app-password és app-token elemeit.",
    "GENERIC_FETCH_FAIL": "Hiba a következő beolvasásakor: {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Nem lehet lekérdezni a következőhöz tartozó {0} elemek listáját: {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Hiba a(z) {0} törlésekor",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Hiba történt a(z) {0} törlésekor (név: {1}).",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "Hiba a történt a(z) {0} törlésekor (felhasználó: {1}).",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Hiba a törléskor",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "Hiba történt a következő app-password vagy app-token törlésekor:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "Hiba történt a következő {0} app-password és app-token törlésekor:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Hiba a hitelesítések beolvasásakor",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Nem lehet lekérdezni a következőhöz tartozó app-password és app-token elemek listáját: {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Az ügyfél nincs beállítva",
    "GENERIC_NOT_CONFIGURED_MSG": "Az appPasswordAllowed és appTokenAllowed ügyfél attribútumok nincsenek beállítva.  Nem lehet adatokat beolvasni."
};
