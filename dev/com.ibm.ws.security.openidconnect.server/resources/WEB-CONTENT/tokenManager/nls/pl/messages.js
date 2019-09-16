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
    "CLEAR_SEARCH": "Wyczyść dane wejściowe wyszukiwania",
    "CLEAR_FILTER": "Wyczyść filtr",
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
    "TABLE_BATCH_BAR": "Pasek działań tabeli",
    "TABLE_FIELD_SORT_ASC": "Tabela jest sortowana według {0} w kolejności rosnącej.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Tabela jest sortowana według {0} w kolejności malejącej.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "Prawda",
    "TRY_AGAIN": "Spróbuj ponownie...",
    "UPDATE": "Aktualizuj",

    // Common Column Names
    "CLIENT_NAME_COL": "Nazwa klienta",
    "EXPIRES_COL": "Traci ważność dnia",
    "ISSUED_COL": "Data wystawienia",
    "NAME_COL": "Nazwa",
    "TYPE_COL": "Typ",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Usuwanie znaczników",
    "TOKEN_MGR_DESC": "Usuń elementy app-password i app-token określonego użytkownika.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Wprowadź ID użytkownika",
    "TABLE_FILLED_WITH": "Tabel została zaktualizowana w taki sposób, aby zawierała uwierzytelnienia ({0}) należące do {1}.",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "Usuń wybrane elementy app-password i app-token",
    "DELETE_ARIA": "Usuń element {0} o nazwie {1}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Usuń element app-password",
    "DELETE_TOKEN": "Usuń element app-token",
    "DELETE_FOR_USERID": "{0} użytkownika {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "To działanie spowoduje usunięcie aktualnie przypisanego elementu app-password.",
    "DELETE_WARNING_TOKEN": "To działanie spowoduje usunięcie aktualnie przypisanego elementu app-token.",
    "DELETE_MANY": "Usuń elementy App-Password/App-Token",
    "DELETE_MANY_FOR": "przypisane do użytkownika {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "To działanie spowoduje usunięcie wybranych elementów app-password/app-token.",
    "DELETE_MANY_MESSAGE": "To działanie spowoduje usunięcie następującej liczby {0} wybranych elementów app-password/app-token",  // 0 - number
    "DELETE_ALL_MESSAGE": "To działanie spowoduje usunięcie wszystkich elementów app-password/app-token należących do użytkownika {0}.", // 0 - user id
    "DELETE_NONE": "Wybór elementów do usunięcia",
    "DELETE_NONE_MESSAGE": "Zaznacz pola wyboru, aby wskazać elementy app-password lub app-token do usunięcia.",
    "SINGLE_ITEM_SELECTED": "Wybrano 1 element",
    "ITEMS_SELECTED": "Wybrano następującą liczbę elementów: {0}",            // 0 - number
    "SELECT_ALL_AUTHS": "Wybierz wszystkie elementy app-password i app-tokens dla tego użytkownika",
    "SELECT_SPECIFIC": "Wybierz element {0} o nazwie {1} do usunięcia.",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Szukasz czegoś? Wprowadź ID użytkownika, aby wyświetlić jego elementy app-password i app-token.",
    "GENERIC_FETCH_FAIL": "Błąd podczas pobierania elementu {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Nie można pobrać listy elementów {0} należących do {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Błąd podczas usuwania elementu {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Wystąpił błąd podczas usuwania elementu {0} o nazwie {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "Wystąpił błąd podczas usuwania elementu {0} dla {1}.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Błąd usuwania",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "Wystąpił błąd podczas usuwania następujących elementów app-password lub app-token:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "Wystąpił błąd podczas usuwania następującej liczby: {0} elementów app-password i app-token:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Błąd podczas pobierania danych uwierzytelniających",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Nie można pobrać listy elementów app-password i app-token należących do {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Klient nie został skonfigurowany",
    "GENERIC_NOT_CONFIGURED_MSG": "Atrybuty klienta appPasswordAllowed i appTokenAllowed nie zostały skonfigurowane.  Nie można pobrać danych."
};
