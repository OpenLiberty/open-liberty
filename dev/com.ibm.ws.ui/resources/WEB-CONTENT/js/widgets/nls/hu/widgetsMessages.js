/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
 define({
    LIBERTY_HEADER_TITLE: "Liberty Admin Center",
    LIBERTY_HEADER_PROFILE: "Beállítások",
    LIBERTY_HEADER_LOGOUT: "Kijelentkezés",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Kijelentkezés: {0}",
    TOOLBOX_BANNER_LABEL: "{0} fejléccsík",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Eszközkészlet",
    TOOLBOX_TITLE_LOADING_TOOL: "Eszköz betöltése...",
    TOOLBOX_TITLE_EDIT: "Eszközkészlet szerkesztése",
    TOOLBOX_EDIT: "Szerkesztés",
    TOOLBOX_DONE: "Kész",
    TOOLBOX_SEARCH: "Szűrő",
    TOOLBOX_CLEAR_SEARCH: "Szűrőfeltételek törlése",
    TOOLBOX_END_SEARCH: "Szűrő leállítása",
    TOOLBOX_ADD_TOOL: "Eszköz hozzáadása",
    TOOLBOX_ADD_CATALOG_TOOL: "Eszköz hozzáadása",
    TOOLBOX_ADD_BOOKMARK: "Könyvjelző hozzáadása",
    TOOLBOX_REMOVE_TITLE: "{0} eszköz eltávolítása",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Eszköz eltávolítása",
    TOOLBOX_REMOVE_MESSAGE: "Biztosan eltávolítja a következőt: {0}?",
    TOOLBOX_BUTTON_REMOVE: "Eltávolítás",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Ugrás az eszközkészlethez",
    TOOLBOX_BUTTON_CANCEL: "Mégse",
    TOOLBOX_BUTTON_BGTASK: "Háttérfeladatok",
    TOOLBOX_BUTTON_BACK: "Vissza",
    TOOLBOX_BUTTON_USER: "Felhasználó",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "Hiba történt a(z) {0} eszköz hozzáadása során: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "Hiba történt a(z) {0} eszköz eltávolítása során: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "Hiba történt az eszközkészletben lévő eszközök lekérése során: {0}",
    TOOLCATALOG_TITLE: "Eszközkatalógus",
    TOOLCATALOG_ADDTOOL_TITLE: "Eszköz hozzáadása",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Biztosan hozzáadja a(z) {0} eszközt az eszközkészletéhez?",
    TOOLCATALOG_BUTTON_ADD: "Hozzáadás",
    TOOL_FRAME_TITLE: "Eszközkeret",
    TOOL_DELETE_TITLE: "{0} törlése",
    TOOL_ADD_TITLE: "{0} hozzáadása",
    TOOL_ADDED_TITLE: "{0} már hozzá van adva",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Eszköz nem található",
    TOOL_LAUNCH_ERROR_MESSAGE: "A kért eszköz nem indult el, mert nincs a katalógusban.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Hiba",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Figyelmeztetés",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Információk",
    LIBERTY_UI_CATALOG_GET_ERROR: "Hiba történt a katalógus lekérése során: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "Hiba történt a(z) {0} eszköz lekérése során a katalógusból: {1}",
    PREFERENCES_TITLE: "Beállítások",
    PREFERENCES_SECTION_TITLE: "Beállítások",
    PREFERENCES_ENABLE_BIDI: "Kétirányú írás támogatásának engedélyezése",
    PREFERENCES_BIDI_TEXTDIR: "Szövegirány",
    PREFERENCES_BIDI_TEXTDIR_LTR: "Balról jobbra",
    PREFERENCES_BIDI_TEXTDIR_RTL: "Jobbról balra",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Szövegfüggő",
    PREFERENCES_SET_ERROR_MESSAGE: "Hiba történt a felhasználói beállítások megadása során az eszközkészletben: {0}",
    BGTASKS_PAGE_LABEL: "Háttérfeladatok",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "{0} telepítés rendszerbe állítása", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "{0} / {1} telepítés rendszerbe állítása", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "Fut",
    BGTASKS_STATUS_FAILED: "Meghiúsult",
    BGTASKS_STATUS_SUCCEEDED: "Befejezve", 
    BGTASKS_STATUS_WARNING: "Részlegesen sikerült",
    BGTASKS_STATUS_PENDING: "Függőben lévő",
    BGTASKS_INFO_DIALOG_TITLE: "Részletek",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Szabványos kimenet:",
    BGTASKS_INFO_DIALOG_STDERR: "Szabványos hibakimenet:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Kivétel:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Találat:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Kiszolgáló neve:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Felhasználói könyvtár:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Aktív háttérfeladatok",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Nincs",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "Nincs aktív háttérben futó feladat",
    BGTASKS_DISPLAY_BUTTON: "Feladat részletei és előzmények",
    BGTASKS_EXPAND: "Szakasz kibontása",
    BGTASKS_COLLAPSE: "Szakasz összehúzása",
    PROFILE_MENU_HELP_TITLE: "Súgó",
    DETAILS_DESCRIPTION: "Leírás",
    DETAILS_OVERVIEW: "Áttekintés",
    DETAILS_OTHERVERSIONS: "Egyéb változatok",
    DETAILS_VERSION: "Változat: {0}",
    DETAILS_UPDATED: "Frissítve: {0}",
    DETAILS_NOTOPTIMIZED: "Nincs optimalizálva az aktuális eszközhöz.",
    DETAILS_ADDBUTTON: "Hozzáadás saját eszközkészlethez",
    DETAILS_OPEN: "Megnyitás",
    DETAILS_CATEGORY: "Kategória {0}",
    DETAILS_ADDCONFIRM: "A(z) {0} eszköz sikeresen hozzáadva az eszközkészlethez.",
    CONFIRM_DIALOG_HELP: "Súgó",
    YES_BUTTON_LABEL: "{0} igen",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} nem",  // insert is dialog title

    YES: "Igen",
    NO: "Nem",

    TOOL_OIDC_ACCESS_DENIED: "A felhasználó nincs benne abban a szerepben, amelynek van engedélye ennek a kérésnek a végrehajtására.",
    TOOL_OIDC_GENERIC_ERROR: "Hiba történt. További információkért tekintse meg a hibát a naplóban.",
    TOOL_DISABLE: "A felhasználó nem rendelkezik jogosultsággal az eszköz elérésére. Csak az adminisztrátori szereppel felruházott felhasználók rendelkeznek jogosultsággal az eszköz használatára. " 
});
