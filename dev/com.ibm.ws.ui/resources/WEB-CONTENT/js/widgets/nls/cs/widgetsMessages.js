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
    LIBERTY_HEADER_TITLE: "Administrační centrum Liberty",
    LIBERTY_HEADER_PROFILE: "Předvolby",
    LIBERTY_HEADER_LOGOUT: "Odhlásit se",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Odhlášení {0}",
    TOOLBOX_BANNER_LABEL: "Proužek {0}",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Panel nástrojů",
    TOOLBOX_TITLE_LOADING_TOOL: "Načítání nástroje...",
    TOOLBOX_TITLE_EDIT: "Upravit panel nástrojů",
    TOOLBOX_EDIT: "Upravit",
    TOOLBOX_DONE: "Hotovo",
    TOOLBOX_SEARCH: "Filtr",
    TOOLBOX_CLEAR_SEARCH: "Vymazat kritéria filtru",
    TOOLBOX_END_SEARCH: "Ukončit filtr",
    TOOLBOX_ADD_TOOL: "Přidat nástroj",
    TOOLBOX_ADD_CATALOG_TOOL: "Přidat nástroj",
    TOOLBOX_ADD_BOOKMARK: "Přidat záložku",
    TOOLBOX_REMOVE_TITLE: "Odebrat nástroj {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Odebrat nástroj",
    TOOLBOX_REMOVE_MESSAGE: "Opravdu chcete odebrat nástroj {0}?",
    TOOLBOX_BUTTON_REMOVE: "Odebrat",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Přejít na panel nástrojů",
    TOOLBOX_BUTTON_CANCEL: "Storno",
    TOOLBOX_BUTTON_BGTASK: "Úlohy na pozadí",
    TOOLBOX_BUTTON_BACK: "Zpět",
    TOOLBOX_BUTTON_USER: "Uživatel",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "Došlo k chybě při přidávání nástroje {0}: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "Došlo k chybě při odebírání nástroje {0}: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "Došlo k chybě při získávání nástrojů v panelu nástrojů: {0}",
    TOOLCATALOG_TITLE: "Katalog nástrojů",
    TOOLCATALOG_ADDTOOL_TITLE: "Přidat nástroj",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Opravdu chcete na panel nástrojů přidat nástroj {0}?",
    TOOLCATALOG_BUTTON_ADD: "Přidat",
    TOOL_FRAME_TITLE: "Rámec nástroje",
    TOOL_DELETE_TITLE: "Odstranit {0}",
    TOOL_ADD_TITLE: "Přidat {0}",
    TOOL_ADDED_TITLE: "Nástroj {0} byl již přidán",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Nástroj nebyl nalezen",
    TOOL_LAUNCH_ERROR_MESSAGE: "Požadovaný nástroj se nespustil, protože není v katalogu.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Chyba",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Varování",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Informace",
    LIBERTY_UI_CATALOG_GET_ERROR: "Došlo k chybě při získávání katalogu: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "Došlo k chybě při získávání nástroje {0} z katalogu: {1}",
    PREFERENCES_TITLE: "Předvolby",
    PREFERENCES_SECTION_TITLE: "Předvolby",
    PREFERENCES_ENABLE_BIDI: "Povolit obousměrnou podporu",
    PREFERENCES_BIDI_TEXTDIR: "Směr textu",
    PREFERENCES_BIDI_TEXTDIR_LTR: "Zleva doprava",
    PREFERENCES_BIDI_TEXTDIR_RTL: "Zprava doleva",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Podle kontextu",
    PREFERENCES_SET_ERROR_MESSAGE: "Došlo k chybě při nastavování uživatelských předvoleb na panelu nástrojů: {0}",
    BGTASKS_PAGE_LABEL: "Úlohy na pozadí",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Implementace instalace {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Implementace instalace {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "Spuštěno",
    BGTASKS_STATUS_FAILED: "Nezdařilo se",
    BGTASKS_STATUS_SUCCEEDED: "Dokončeno", 
    BGTASKS_STATUS_WARNING: "Částečně úspěšné",
    BGTASKS_STATUS_PENDING: "Nevyřízeno",
    BGTASKS_INFO_DIALOG_TITLE: "Podrobnosti",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Standardní výstup:",
    BGTASKS_INFO_DIALOG_STDERR: "Standardní chyba:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Výjimka:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Výsledek:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Název serveru:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Uživatelský adresář",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Aktivní úlohy na pozadí",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Není",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "Žádné aktivní úlohy na pozadí",
    BGTASKS_DISPLAY_BUTTON: "Podrobnosti úlohy a historie",
    BGTASKS_EXPAND: "Rozbalit sekci",
    BGTASKS_COLLAPSE: "Sbalit sekci",
    PROFILE_MENU_HELP_TITLE: "Nápověda",
    DETAILS_DESCRIPTION: "Popis",
    DETAILS_OVERVIEW: "Přehled",
    DETAILS_OTHERVERSIONS: "Další verze",
    DETAILS_VERSION: "Verze: {0}",
    DETAILS_UPDATED: "Aktualizováno: {0}",
    DETAILS_NOTOPTIMIZED: "Není optimalizováno pro aktuální zařízení.",
    DETAILS_ADDBUTTON: "Přidat do mého panelu nástrojů",
    DETAILS_OPEN: "Začátek",
    DETAILS_CATEGORY: "Kategorie {0}",
    DETAILS_ADDCONFIRM: "Nástroj {0} byl úspěšně přidán do panelu nástrojů.",
    CONFIRM_DIALOG_HELP: "Nápověda",
    YES_BUTTON_LABEL: "{0} ano",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} ne",  // insert is dialog title

    YES: "Ano",
    NO: "Ne",

    TOOL_OIDC_ACCESS_DENIED: "Uživatel se nenachází v roli, která má oprávnění k dokončení tohoto požadavku.",
    TOOL_OIDC_GENERIC_ERROR: "Došlo k chybě. Další informace o chybě naleznete v protokolu.",
    TOOL_DISABLE: "Uživatel nemá oprávnění k použití tohoto nástroje. K použití tohoto nástroje mají oprávnění pouze uživatelé v roli administrátora." 
});
