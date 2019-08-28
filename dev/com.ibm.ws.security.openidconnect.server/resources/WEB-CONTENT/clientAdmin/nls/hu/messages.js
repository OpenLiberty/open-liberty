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

    // Client Admin
    "CLIENT_ADMIN_TITLE": "OAuth ügyfelek kezelése",
    "CLIENT_ADMIN_DESC": "Ezzel az eszközzel adhat hozzá és szerkeszthet ügyfeleket, és állíthatja újra elő az ügyféltitkokat.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Szűrés az OAuth ügyfél nevére",
    "ADD_NEW_CLIENT": "Új OAuth ügyfél hozzáadása.",
    "CLIENT_NAME": "Ügyfél neve",
    "CLIENT_ID": "Ügyfél-azonosító",
    "EDIT_ARIA": "A(z) {0} OAuth ügyfél szerkesztése",      // {0} - name
    "DELETE_ARIA": "A(z) {0} OAuth ügyfél törlése",  // {0} - name
    "CLIENT_SECRET": "Ügyfél titok",
    "GRANT_TYPES": "Kiosztási típusok",
    "SCOPE": "Hatókör",
    "PREAUTHORIZED_SCOPE": "Előre-felhatalmazott hatókör (elhagyható)",
    "REDIRECT_URLS": "Átirányítási URL (elhagyható)",
    "CLIENT_SECRET_CHECKBOX": "Ügyféltitok újra előállítása",
    "NONE_SELECTED": "Nincs kijelölve semmi",
    "MODAL_EDIT_TITLE": "OAuth ügyfél szerkesztése",
    "MODAL_REGISTER_TITLE": "Új OAuth ügyfél regisztrálása",
    "MODAL_SECRET_REGISTER_TITLE": "OAuth regisztráció mentve",
    "MODAL_SECRET_UPDATED_TITLE": "OAuth regisztráció frissítve",
    "MODAL_DELETE_CLIENT_TITLE": "OAuth ügyfél törlése",
    "RESET_GRANT_TYPE": "Összes kijelölt feljogosítási típus törlése.",
    "SELECT_ONE_GRANT_TYPE": "Válasszon ki legalább egy feljogosítási típust.",
    "SPACE_HELPER_TEXT": "Szóközzel tagolt lista",
    "REDIRECT_URL_HELPER_TEXT": "Abszolút átirányítási URL címek szóközzel tagolt listája",
    "DELETE_OAUTH_CLIENT_DESC": "Ez a művelet törli a regisztrált ügyfelet az ügyfél-regisztrációs szolgáltatásból.",
    "REGISTRATION_SAVED": "Ügyfél-azonosító és ügyféltitok került előállításra és hozzárendelésre.",
    "REGISTRATION_UPDATED": "Új ügyféltitok lett előállítva és hozzárendelve az ügyfélhez.",
    "COPY_CLIENT_ID": "Ügyfél-azonosító másolása a vágólapra",
    "COPY_CLIENT_SECRET": "Ügyféltitok másolása a vágólapra",
    "REGISTRATION_UPDATED_NOSECRET": "A(z) {0} OAuth ügyfél frissítésre került.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "Legalább egy feljogosítási típust ki kell választani.",
    "ERR_REDIRECT_URIS": "AZ értékek csak abszolút URI címek lehetnek.",
    "GENERIC_REGISTER_FAIL": "Hiba az OAuth ügyfél regisztrálásakor",
    "GENERIC_UPDATE_FAIL": "Hiba az OAuth ügyfél frissítésekor",
    "GENERIC_DELETE_FAIL": "Hiba az OAuth ügyfél törlésekor",
    "GENERIC_MISSING_CLIENT": "Hiba az OAuth ügyfél beolvasásakor",
    "GENERIC_REGISTER_FAIL_MSG": "Hiba történt a(z) {0} OAuth ügyfél regisztrálásakor.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "Hiba történt a(z) {0} OAuth ügyfél frissítésekor.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "Hiba történt a(z) {0} OAuth ügyfél törlésekor.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "A(z) {0} OAuth ügyfél (azonosító: {1}) nem található.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "Hiba történt az információk beolvasása közben a(z) {0} OAuth ügyfélen.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Hiba az OAuth ügyfelek beolvasásakor",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Hiba történt az OAuth ügyfelek listájának beolvasásakor.",

    "RESET_SELECTION": "Összes kiválasztott {0} törlése",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "Kiválasztott {0} száma",     // {0} - field name
    "OPEN_LIST": "Nyissa meg a(z) {0} listát. ",                   // {0} - field name
    "CLOSE_LIST": "Zárja be a(z) {0} listát. ",                 // {0} - field name
    "ENTER_PLACEHOLDER": "Adja meg az értéket",
    "ADD_VALUE": "Elem hozzáadása",
    "REMOVE_VALUE": "Elem eltávolítása",
    "REGENERATE_CLIENT_SECRET": "'*' megőrzi a meglévő értéket. Az üres érték új ügyféltitok értéket állít elő. A nem üres paraméterérték felülírja a meglévő értéket az újonnan megadott értékkel.",
    "ALL_OPTIONAL": "Minden mező elhagyható"
};
