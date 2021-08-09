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

    // Client Admin
    "CLIENT_ADMIN_TITLE": "Zarządzanie klientami OAuth",
    "CLIENT_ADMIN_DESC": "To narzędzie służy do dodawania i edytowania klientów oraz do ponownego generowania kluczy tajnych klientów.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Filtruj według nazwy klienta OAuth",
    "ADD_NEW_CLIENT": "Dodaj nowy klient OAuth",
    "CLIENT_NAME": "Nazwa klienta",
    "CLIENT_ID": "Identyfikator klienta",
    "EDIT_ARIA": "Edytuj klienta OAuth {0}",      // {0} - name
    "DELETE_ARIA": "Usuń klienta OAuth {0}",  // {0} - name
    "CLIENT_SECRET": "Klucz tajny klienta",
    "GRANT_TYPES": "Typy nadania",
    "SCOPE": "Zasięg",
    "PREAUTHORIZED_SCOPE": "Wstępnie autoryzowany zasięg (opcjonalny)",
    "REDIRECT_URLS": "Adresy URL przekierowywania (opcjonalne)",
    "CLIENT_SECRET_CHECKBOX": "Wygeneruj ponownie klucz tajny klienta",
    "NONE_SELECTED": "Nie wybrano żadnego",
    "MODAL_EDIT_TITLE": "Edycja klienta OAuth",
    "MODAL_REGISTER_TITLE": "Rejestracja nowego klienta OAuth",
    "MODAL_SECRET_REGISTER_TITLE": "Zapisano rejestrację OAuth",
    "MODAL_SECRET_UPDATED_TITLE": "Zaktualizowano rejestrację OAuth",
    "MODAL_DELETE_CLIENT_TITLE": "Usuń klient OAuth",
    "RESET_GRANT_TYPE": "Wyczyść wszystkie typy nadań.",
    "SELECT_ONE_GRANT_TYPE": "Wybierz co najmniej jeden typ nadania",
    "SPACE_HELPER_TEXT": "Wartości rozdzielane spacjami",
    "REDIRECT_URL_HELPER_TEXT": "Rozdzielane spacjami bezwzględne adresy URL przekierowywania",
    "DELETE_OAUTH_CLIENT_DESC": "Ta operacja spowoduje usunięcie zarejestrowanego klienta z usługi rejestracji klienta.",
    "REGISTRATION_SAVED": "Wygenerowano i przypisano ID klienta oraz klucz tajny klienta.",
    "REGISTRATION_UPDATED": "Wygenerowano i przypisano nowy klucz dla tego tajny klienta.",
    "COPY_CLIENT_ID": "Kopiuj identyfikator klienta do schowka",
    "COPY_CLIENT_SECRET": "Kopiuj klucz tajny klienta do schowka",
    "REGISTRATION_UPDATED_NOSECRET": "Klient OAuth {0} został zaktualizowany.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "Należy wybrać co najmniej jeden typ nadania.",
    "ERR_REDIRECT_URIS": "Wartości muszą być bezwzględnymi identyfikatorami URI.",
    "GENERIC_REGISTER_FAIL": "Błąd podczas rejestrowania klienta OAuth",
    "GENERIC_UPDATE_FAIL": "Błąd podczas aktualizowania klienta OAuth",
    "GENERIC_DELETE_FAIL": "Błąd podczas usuwania klienta OAuth",
    "GENERIC_MISSING_CLIENT": "Błąd podczas pobierania klienta OAuth",
    "GENERIC_REGISTER_FAIL_MSG": "Wystąpił błąd podczas rejestrowania klienta OAuth {0}.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "Wystąpił błąd podczas aktualizowania klienta OAuth {0}.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "Wystąpił błąd podczas usuwania klienta OAuth {0}.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "Nie znaleziono klienta OAuth {0} o ID {1}.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "Wystąpił błąd podczas pobierania informacji o kliencie OAuth {0}.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Błąd podczas pobierania klientów OAuth",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Wystąpił błąd podczas pobierania listy klientów OAuth.",

    "RESET_SELECTION": "Wyczyść wszystkie wybrane elementy {0}",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "Liczba wybranych elementów {0}",     // {0} - field name
    "OPEN_LIST": "Otwórz listę elementów {0}.",                   // {0} - field name
    "CLOSE_LIST": "Zamknij listę elementów {0}.",                 // {0} - field name
    "ENTER_PLACEHOLDER": "Wprowadź wartość",
    "ADD_VALUE": "Dodaj element",
    "REMOVE_VALUE": "Usuń element",
    "REGENERATE_CLIENT_SECRET": "'*' zachowuje istniejącą wartość. Pusta wartość generuje nowy element client_secret. Niepusta wartość parametru nadpisuje istniejącą wartość nową wartością.",
    "ALL_OPTIONAL": "Wszystkie pola są opcjonalne"
};
