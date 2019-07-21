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
    "ADD_NEW": "Dodaj nowe",
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
    "GENERATE": "Generuj",
    "LOADING": "Trwa ładowanie",
    "LOGOUT": "Wylogowanie",
    "NEXT_PAGE": "Następna strona",
    "NO_RESULTS_FOUND": "Nie znaleziono żadnych wyników",
    "PAGES": "{0} z {1} stron",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Wybierz numer strony do wyświetlenia",
    "PREVIOUS_PAGE": "Poprzednia strona",
    "PROCESSING": "Przetwarzanie",
    "REGENERATE": "Wygeneruj ponownie",
    "REGISTER": "Zarejestruj",
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
    "CLIENT_ADMIN_DESC": "Użyj tego narzędzia do dodawania lub edytowania klientów i ponownego generowania kluczy tajnych klientów.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Filtruj według nazwy klienta OAuth",
    "ADD_NEW_CLIENT": "Dodaj nowego klienta OAuth",
    "CLIENT_NAME": "Nazwa klienta",
    "CLIENT_ID": "Identyfikator klienta",
    "EDIT_ARIA": "Edytuj klienta OAuth {0}",      // {0} - name
    "DELETE_ARIA": "Usuń klienta OAuth {0}",  // {0} - name
    "CLIENT_SECRET": "Klucz tajny klienta",
    "GRANT_TYPES": "Typy nadania",
    "SCOPE": "Zasięg",
    "PREAUTHORIZED_SCOPE": "Wstępnie autoryzowany zasięg (opcjonalny)",
    "REDIRECT_URLS": "Adresy URL przekierowania (opcjonalne)",
    "ADDITIONAL_PROPS": "Właściwości dodatkowe",
    "ADDITIONAL_PROPS_OPTIONAL": "Właściwości dodatkowe (opcjonalne)",
    "CLIENT_SECRET_CHECKBOX": "Wygeneruj ponownie klucz tajny klienta",
    "PROPERTY_PLACEHOLDER": "Właściwość",
    "VALUE_PLACEHOLDER": "Wartość",
    "GRANT_TYPES_SELECTED": "Liczba wybranych typów nadań",
    "GRANT_TYPES_NONE_SELECTED": "Nie wybrano żadnego",
    "MODAL_EDIT_TITLE": "Edycja klienta OAut",
    "MODAL_REGISTER_TITLE": "Rejestracja nowego klienta OAuth",
    "MODAL_SECRET_REGISTER_TITLE": "Zapisano rejestrację OAuth",
    "MODAL_SECRET_UPDATED_TITLE": "Zaktualizowano rejestrację OAuth",
    "MODAL_DELETE_CLIENT_TITLE": "Usuń tego klienta OAuth",
    "VALUE_COL": "Wartość",
    "ADD": "Dodaj",
    "DELETE_PROP": "Usuń właściwość niestandardową",
    "RESET_GRANT_TYPE": "Wyczyść wszystkie typy nadań",
    "SELECT_ONE_GRANT_TYPE": "Wybierz co najmniej jeden typ nadania",
    "OPEN_GRANT_TYPE": "Otwórz listę typów nadań",
    "CLOSE_GRANT_TYPE": "Zamknij listę typów nadań",
    "SPACE_HELPER_TEXT": "Wartości rozdzielane spacjami",
    "REDIRECT_URL_HELPER_TEXT": "Rozdzielane spacjami bezwzględne adresy URL przekierowania",
    "DELETE_OAUTH_CLIENT_DESC": "Ta operacja powoduje usunięcie zarejestrowanego klienta z usługi rejestracji klienta.",
    "REGISTRATION_SAVED": "Wygenerowano i przypisano ID klienta i klucz tajny klienta.",
    "REGISTRATION_UPDATED": "Wygenerowano i przypisano nowy klucz dla tego tajny klienta.",
    "REGISTRATION_UPDATED_NOSECRET": "Klient {0} został zaktualizowany.",                 // {0} - client name
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
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Wystąpił błąd podczas pobierania listy klientów OAuth."
};
