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
    LIBERTY_HEADER_TITLE: "Centrum administracyjne serwera Liberty",
    LIBERTY_HEADER_PROFILE: "Preferencje",
    LIBERTY_HEADER_LOGOUT: "Wyloguj",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Wyloguj użytkownika {0}",
    TOOLBOX_BANNER_LABEL: "Baner {0}",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Panel narzędzi",
    TOOLBOX_TITLE_LOADING_TOOL: "Ładowanie narzędzia...",
    TOOLBOX_TITLE_EDIT: "Edycja panelu narzędzi",
    TOOLBOX_EDIT: "Edytuj",
    TOOLBOX_DONE: "Gotowe",
    TOOLBOX_SEARCH: "Filtruj",
    TOOLBOX_CLEAR_SEARCH: "Wyczyść kryteria filtrowania",
    TOOLBOX_END_SEARCH: "Zakończ filtrowanie",
    TOOLBOX_ADD_TOOL: "Dodaj narzędzie",
    TOOLBOX_ADD_CATALOG_TOOL: "Dodaj narzędzie",
    TOOLBOX_ADD_BOOKMARK: "Dodaj zakładkę",
    TOOLBOX_REMOVE_TITLE: "Usuwanie narzędzia {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Usuwanie narzędzia",
    TOOLBOX_REMOVE_MESSAGE: "Czy na pewno chcesz usunąć narzędzie {0}?",
    TOOLBOX_BUTTON_REMOVE: "Usuń",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Idź do panelu narzędzi",
    TOOLBOX_BUTTON_CANCEL: "Anuluj",
    TOOLBOX_BUTTON_BGTASK: "Zadania w tle",
    TOOLBOX_BUTTON_BACK: "Wstecz",
    TOOLBOX_BUTTON_USER: "Użytkownik",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "Wystąpił błąd podczas dodawania narzędzia {0}: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "Wystąpił błąd podczas usuwania narzędzia {0}: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "Wystąpił błąd podczas pobierania narzędzi z panelu narzędzi: {0}",
    TOOLCATALOG_TITLE: "Katalog narzędzi",
    TOOLCATALOG_ADDTOOL_TITLE: "Dodawanie narzędzia",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Czy na pewno chcesz dodać narzędzie {0} do panelu narzędzi?",
    TOOLCATALOG_BUTTON_ADD: "Dodaj",
    TOOL_FRAME_TITLE: "Ramka narzędzi",
    TOOL_DELETE_TITLE: "Usuwanie narzędzia {0}",
    TOOL_ADD_TITLE: "Dodawanie narzędzia {0}",
    TOOL_ADDED_TITLE: "Narzędzie {0} zostało już dodane",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Nie znaleziono narzędzia",
    TOOL_LAUNCH_ERROR_MESSAGE: "Żądane narzędzie nie zostało uruchomione, ponieważ nie znajduje się w katalogu.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Błąd",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Ostrzeżenie",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Informacja",
    LIBERTY_UI_CATALOG_GET_ERROR: "Wystąpił błąd podczas pobierania katalogu: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "Wystąpił błąd podczas pobierania narzędzia {0} z katalogu: {1}",
    PREFERENCES_TITLE: "Preferencje",
    PREFERENCES_SECTION_TITLE: "Preferencje",
    PREFERENCES_ENABLE_BIDI: "Włącz obsługę tekstu dwukierunkowego",
    PREFERENCES_BIDI_TEXTDIR: "Kierunek tekstu",
    PREFERENCES_BIDI_TEXTDIR_LTR: "Od lewej do prawej",
    PREFERENCES_BIDI_TEXTDIR_RTL: "Od prawej do lewej",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Kontekstowy",
    PREFERENCES_SET_ERROR_MESSAGE: "Wystąpił błąd podczas ustawiania preferencji użytkownika na panelu narzędzi: {0}",
    BGTASKS_PAGE_LABEL: "Zadania w tle",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Wdróż instalację {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Wdróż instalację {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "Działający",
    BGTASKS_STATUS_FAILED: "Niepowodzenie",
    BGTASKS_STATUS_SUCCEEDED: "Zakończono", 
    BGTASKS_STATUS_WARNING: "Częściowe powodzenie",
    BGTASKS_STATUS_PENDING: "Oczekiwanie",
    BGTASKS_INFO_DIALOG_TITLE: "Szczegóły",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Wyjście standardowe:",
    BGTASKS_INFO_DIALOG_STDERR: "Standardowe wyjście błędów:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Wyjątek:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Wynik:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Nazwa serwera:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Katalog użytkownika:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Aktywne zadania w tle",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Brak",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "Brak aktywnych zadań w tle",
    BGTASKS_DISPLAY_BUTTON: "Szczegóły i historia zadań",
    BGTASKS_EXPAND: "Rozwiń sekcję",
    BGTASKS_COLLAPSE: "Zwiń sekcję",
    PROFILE_MENU_HELP_TITLE: "Pomoc",
    DETAILS_DESCRIPTION: "Opis",
    DETAILS_OVERVIEW: "Przegląd",
    DETAILS_OTHERVERSIONS: "Inne wersje",
    DETAILS_VERSION: "Wersja: {0}",
    DETAILS_UPDATED: "Zaktualizowano: {0}",
    DETAILS_NOTOPTIMIZED: "Nie zoptymalizowano dla bieżącego urządzenia.",
    DETAILS_ADDBUTTON: "Dodaj do panelu narzędzi bieżącego użytkownika",
    DETAILS_OPEN: "Otwórz",
    DETAILS_CATEGORY: "Kategoria {0}",
    DETAILS_ADDCONFIRM: "Narzędzie {0} zostało pomyślnie dodane do panelu narzędzi.",
    CONFIRM_DIALOG_HELP: "Pomoc",
    YES_BUTTON_LABEL: "{0} - tak",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} - nie",  // insert is dialog title

    YES: "Tak",
    NO: "Nie",

    TOOL_OIDC_ACCESS_DENIED: "Użytkownik nie pełni roli, która ma uprawnienia do wykonywania tego żądania.",
    TOOL_OIDC_GENERIC_ERROR: "Wystąpił błąd. Przejrzyj ten błąd w dzienniku, aby uzyskać więcej informacji.",
    TOOL_DISABLE: "Użytkownik nie ma uprawnienia do używania tego narzędzia. Uprawnienie do używania tego narzędzia mają tylko użytkownicy z rolą administratora." 
});
