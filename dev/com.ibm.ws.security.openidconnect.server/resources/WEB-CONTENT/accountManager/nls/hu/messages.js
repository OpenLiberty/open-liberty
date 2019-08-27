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
    "CLEAR": "Keresési adatbevitel törlése",
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
    "TABLE_FIELD_SORT_ASC": "A tábla {0} szerint növekvő sorrendben van rendezve.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "A tábla {0} szerint csökkenő sorrendben van rendezve.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Próbálkozzon újra...",
    "UPDATE": "Frissítés",

    // Common Column Names
    "EXPIRES_COL": "Lejárat",
    "ISSUED_COL": "Kibocsátva",
    "NAME_COL": "Név",
    "TYPE_COL": "Típus",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Személyes tokenek kezelése",
    "ACCT_MGR_DESC": "App-password és app-token elemek létrehozása, törlése és újra-előállítása.",
    "ADD_NEW_AUTHENTICATION": "Új app-password vagy app-token hozzáadása.",
    "NAME_IDENTIFIER": "Név: {0}",
    "ADD_NEW_TITLE": "Új hitelesítés regisztrálása",
    "NOT_GENERATED_PLACEHOLDER": "Nem kerül előállításra",
    "AUTHENTICAION_GENERATED": "Előállított hitelesítés",
    "GENERATED_APP_PASSWORD": "Előállított app-password",
    "GENERATED_APP_TOKEN": "Előállított app-token",
    "COPY_APP_PASSWORD": "App-password másolása a vágólapra",
    "COPY_APP_TOKEN": "App-token másolása a vágólapra",
    "REGENERATE_APP_PASSWORD": "App-Password újra-előállítása",
    "REGENERATE_PW_WARNING": "Ez a művelet felül fogja írni a jelenlegi app-password elemet.",
    "REGENERATE_PW_PLACEHOLDER": "Jelszó előző előállításának dátuma: {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "App-Token újra-előállítása",
    "REGENERATE_TOKEN_WARNING": "Ez a művelet felül fogja írni a jelenlegi app-token elemet.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Token előző előállításának dátuma: {0}",        // 0 - date
    "DELETE_PW": "Az app-password törlése",
    "DELETE_TOKEN": "Az app-token törlése",
    "DELETE_WARNING_PW": "Ez a művelet el fogja távolítani a jelenleg hozzárendelt app-password elemet.",
    "DELETE_WARNING_TOKEN": "Ez a művelet el fogja távolítani a jelenleg hozzárendelt app-token elemet.",
    "REGENERATE_ARIA": "{0} újra-előállítása a következőhöz: {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Törölje a(z) {0} elemet (név: {1})",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Hiba a(z) {0} előállításakor", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "Hiba történt az új {0} előállításakor (név: {1}).",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "A név már {0} elemhez tartozik, vagy túl hosszú.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Hiba a(z) {0} törlésekor",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Hiba történt a(z) {0} törlésekor (név: {1}).",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Hiba a(z) {0} újra-előállításkor",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "Hiba történt a(z) {0} újra-előállításkor (név: {1}).",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "Hiba történt a(z) {0} újra-előállításkor (név: {1}). A(z) {0} törölve lett, de nem lehetett újra létrehozni.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Hiba a hitelesítések beolvasásakor",
    "GENERIC_FETCH_FAIL_MSG": "Nem lehet lekérdezni az app-password vagy app-token elemek aktuális listáját.",
    "GENERIC_NOT_CONFIGURED": "Az ügyfél nincs beállítva",
    "GENERIC_NOT_CONFIGURED_MSG": "Az appPasswordAllowed és appTokenAllowed ügyfél attribútumok nincsenek beállítva.  Nem lehet adatokat beolvasni.",
    "APP_PASSWORD_NOT_CONFIGURED": "Az appPasswordAllowed ügyfél attribútum nincs beállítva.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "Az appTokenAllowed ügyfél attribútum nincs beállítva."         // 'appTokenAllowed' is a config option.  Do not translate.
};
