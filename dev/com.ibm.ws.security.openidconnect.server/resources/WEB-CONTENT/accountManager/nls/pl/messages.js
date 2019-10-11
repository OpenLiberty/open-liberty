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
    "ADD_NEW": "Dodaj nowy",
    "CANCEL": "Anuluj",
    "CLEAR": "Wyczyść dane wejściowe wyszukiwania",
    "CLICK_TO_SORT": "Kliknij, aby posortować kolumnę",
    "CLOSE": "Zamknij",
    "COPY_TO_CLIPBOARD": "Kopiuj do schowka",
    "COPIED_TO_CLIPBOARD": "Skopiowano do schowka",
    "DELETE": "Usuń",
    "DONE": "Gotowe",
    "EDIT": "Edytuj",
    "FALSE": "Fałsz",
    "GENERATE": "Generuj",
    "LOADING": "Trwa ładowanie",
    "LOGOUT": "Wylogowanie",
    "NEXT_PAGE": "Następna strona",
    "NO_RESULTS_FOUND": "Nie znaleziono żadnych wyników",
    "PAGES": "Strona: {0} z {1}",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Wybierz numer strony do wyświetlenia",
    "PREVIOUS_PAGE": "Poprzednia strona",
    "PROCESSING": "Przetwarzanie",
    "REGENERATE": "Generuj ponownie",
    "REGISTER": "Rejestruj",
    "TABLE_FIELD_SORT_ASC": "Tabela jest sortowana według {0} w kolejności rosnącej.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Tabela jest sortowana według {0} w kolejności malejącej.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "Prawda",
    "TRY_AGAIN": "Spróbuj ponownie...",
    "UPDATE": "Aktualizuj",

    // Common Column Names
    "EXPIRES_COL": "Traci ważność dnia",
    "ISSUED_COL": "Data wystawienia",
    "NAME_COL": "Nazwa",
    "TYPE_COL": "Typ",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Zarządzanie znacznikami osobistymi",
    "ACCT_MGR_DESC": "Twórz, usuwaj i generuj ponownie elementy app-password i app-token.",
    "ADD_NEW_AUTHENTICATION": "Dodaj nowy element app-password lub app-token",
    "NAME_IDENTIFIER": "Nazwa: {0}",
    "ADD_NEW_TITLE": "Rejestracja nowego uwierzytelnienia",
    "NOT_GENERATED_PLACEHOLDER": "Nie wygenerowano",
    "AUTHENTICAION_GENERATED": "Wygenerowano uwierzytelnienie",
    "GENERATED_APP_PASSWORD": "Wygenerowano element app-password",
    "GENERATED_APP_TOKEN": "Wygenerowano element app-token",
    "COPY_APP_PASSWORD": "Kopiuj element app-password do schowka",
    "COPY_APP_TOKEN": "Kopiuj element app-token do schowka",
    "REGENERATE_APP_PASSWORD": "Ponowne generowanie elementu App-Password",
    "REGENERATE_PW_WARNING": "To działanie spowoduje zastąpienie bieżącego elementu app-password.",
    "REGENERATE_PW_PLACEHOLDER": "Hasło zostało wcześniej wygenerowane {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "Ponowne generowanie elementu App-Token",
    "REGENERATE_TOKEN_WARNING": "To działanie spowoduje zastąpienie bieżącego elementu app-token.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Znacznik został wcześniej wygenerowany {0}",        // 0 - date
    "DELETE_PW": "Usuń element app-password",
    "DELETE_TOKEN": "Usuń element app-token",
    "DELETE_WARNING_PW": "To działanie spowoduje usunięcie aktualnie przypisanego elementu app-password.",
    "DELETE_WARNING_TOKEN": "To działanie spowoduje usunięcie aktualnie przypisanego elementu app-token.",
    "REGENERATE_ARIA": "Wygeneruj ponownie element {0} dla {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Usuń element {0} o nazwie {1}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Błąd generowania elementu {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "Wystąpił błąd podczas generowania nowego elementu {0} o nazwie {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "Ta nazwa jest już powiązana z elementem {0} lub jest za długa.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Błąd podczas usuwania elementu {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Wystąpił błąd podczas usuwania elementu {0} o nazwie {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Błąd podczas ponownego generowania elementu {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "Wystąpił błąd podczas ponownego generowania elementu {0} o nazwie {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "Wystąpił błąd podczas ponownego generowania elementu {0} o nazwie {1}. Element {0} został usunięty, ale nie można go odtworzyć.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Błąd podczas pobierania danych uwierzytelniających",
    "GENERIC_FETCH_FAIL_MSG": "Nie można pobrać bieżącej listy elementów app-password lub app-token.",
    "GENERIC_NOT_CONFIGURED": "Klient nie został skonfigurowany",
    "GENERIC_NOT_CONFIGURED_MSG": "Atrybuty klienta appPasswordAllowed i appTokenAllowed nie zostały skonfigurowane.  Nie można pobrać danych.",
    "APP_PASSWORD_NOT_CONFIGURED": "Atrybut klienta appPasswordAllowed nie został skonfigurowany.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "Atrybut klienta appTokenAllowed nie został skonfigurowany."         // 'appTokenAllowed' is a config option.  Do not translate.
};
