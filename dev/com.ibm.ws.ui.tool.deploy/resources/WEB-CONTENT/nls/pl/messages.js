var messages = {
//General
"DEPLOY_TOOL_TITLE": "Wdróż",
"SEARCH" : "Szukaj",
"SEARCH_HOSTS" : "Szukaj hostów",
"EXPLORE_TOOL": "NARZĘDZIE EKSPLORACJI",
"EXPLORE_TOOL_INSERT": "Spróbuj użyć narzędzia eksploracji",
"EXPLORE_TOOL_ARIA": "Wyszukaj hosty w narzędziu eksploracji na nowej karcie",

//Rule Selector Panel
"RULESELECT_EDIT" : "EDYTOWANIE",
"RULESELECT_CHANGE_SELECTION" : "EDYTUJ WYBÓR",
"RULESELECT_SERVER_DEFAULT" : "DOMYŚLNE TYPY SERWERÓW",
"RULESELECT_SERVER_CUSTOM" : "TYPY NIESTANDARDOWE",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Niestandardowe typy serwerów",
"RULESELECT_NEXT" : "DALEJ",
"RULESELECT_SERVER_TYPE": "Typ serwera",
"RULESELECT_SELECT_ONE": "Wybierz jedną z opcji",
"RULESELECT_DEPLOY_TYPE" : "Reguła wdrażania",
"RULESELECT_SERVER_SUBHEADING": "Serwer",
"RULESELECT_CUSTOM_PACKAGE": "Pakiet niestandardowy",
"RULESELECT_RULE_DEFAULT" : "REGUŁY DOMYŚLNE",
"RULESELECT_RULE_CUSTOM" : "REGUŁY NIESTANDARDOWE",
"RULESELECT_FOOTER" : "Wybierz typ serwera i typ reguły przed ponownym uruchomieniem formularza wdrażania.",
"RULESELECT_CONFIRM" : "Potwierdź",
"RULESELECT_CUSTOM_INFO": "Istnieje możliwość zdefiniowania własnych reguł z dostosowanymi danymi wejściowymi i zachowaniem wdrażania.",
"RULESELECT_CUSTOM_INFO_LINK": "Więcej informacji",
"RULESELECT_BACK": "Wstecz",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Panel wyboru reguł {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Otwórz",
"RULESELECT_CLOSED" : "Zamknięte",
"RULESELECT_SCROLL_UP": "Przewiń w górę",
"RULESELECT_SCROLL_DOWN": "Przewiń w dół",
"RULESELECT_EDIT_SERVER_ARIA" : "Edytuj typ serwera, bieżący wybór {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Edytuj regułę, bieżący wybór {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Następny panel",

//SERVER TYPES
"LIBERTY_SERVER" : "Serwer Liberty",
"NODEJS_SERVER" : "Serwer Node.js",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Pakiet aplikacji", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Pakiet serwera", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Kontener produktu Docker", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Parametry wdrożenia",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Parametry wdrożenia ({0})",
"PARAMETERS_DESCRIPTION": "Szczegółowe informacje są wyświetlane na podstawie wybranego serwera i typu szablonu.",
"PARAMETERS_TOGGLE_CONTROLLER": "Użyj pliku znajdującego się w kontrolerze kolektywu",
"PARAMETERS_TOGGLE_UPLOAD": "Prześlij plik",
"SEARCH_IMAGES": "Szukaj obrazów",
"SEARCH_CLUSTERS": "Szukaj klastrów",
"CLEAR_FIELD_BUTTON_ARIA": "Wyczyść wartość wejściową",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Prześlij plik pakietu serwera",
"BROWSE_TITLE": "Przesyłanie pliku {0}",
"STRONGLOOP_BROWSE": "Przeciągnij plik tutaj lub {0}, aby podać nazwę pliku", //BROWSE_INSERT
"BROWSE_INSERT" : "przeglądaj",
"BROWSE_ARIA": "przeglądaj pliki",
"FILE_UPLOAD_PREVIOUS" : "Użyj pliku znajdującego się w kontrolerze kolektywu",
"IS_UPLOADING": "Pakiet {0} jest przesyłany...",
"CANCEL" : "ANULUJ",
"UPLOAD_SUCCESSFUL" : "Pakiet {0} został pomyślnie przesłany", // Package Name
"UPLOAD_FAILED" : "Przesyłanie nie powiodło się",
"RESET" : "resetuj",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "Lista katalogów zapisu jest pusta.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "Określona ścieżka musi znajdować się na liście katalogów zapisu.",
"PARAMETERS_FILE_ARIA" : "Parametry wdrożenia lub pakiet {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Należy skonfigurować repozytorium produktu Docker",
"DOCKER_EMPTY_IMAGE_ERROR": "Nie znaleziono obrazów w skonfigurowanym repozytorium produktu Docker.",
"DOCKER_GENERIC_ERROR": "Nie załadowano żadnych obrazów produktu Docker. Sprawdź, czy skonfigurowano repozytorium produktu Docker.",
"REFRESH": "Odśwież",
"REFRESH_ARIA": "Odśwież obrazy produktu Docker",
"PARAMETERS_DOCKER_ARIA": "Parametry wdrożenia lub wyszukiwanie obrazów produktu Docker",
"DOCKER_IMAGES_ARIA" : "Lista obrazów produktu Docker",
"LOCAL_IMAGE": "lokalna nazwa obrazu",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "Nazwa kontenera musi być zgodna z formatem [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "Liczba wybranych hostów: {0}", //quantity
"N_SELECTED_HOSTS": "Liczba wybranych hostów: {0}", //quantity
"SELECT_HOSTS_MESSAGE": "Wybierz z listy dostępnych hostów. Hosty można wyszukiwać według nazwy lub znaczników.",
"ONE_HOST" : "Liczba wyników: {0}", //quantity
"N_HOSTS": "Liczba wyników: {0}", //quantity
"SELECT_HOSTS_FOOTER": "Potrzebne jest bardziej złożone wyszukiwanie? {0}", //EXPLORE_TOOL_INSERT
"NAME": "NAZWA",
"NAME_FILTER": "Filtruj hosty wg nazwy", // Used for aria-label
"TAG": "ZNACZNIK",
"TAG_FILTER": "Filtruj hosty wg znacznika",
"ALL_HOSTS_LIST_ARIA" : "Lista wszystkich hostów",
"SELECTED_HOSTS_LIST_ARIA": "Lista wybranych hostów",

//Security Details
"SECURITY_DETAILS": "Szczegóły zabezpieczeń",
"SECURITY_DETAILS_FOR_GROUP": "Szczegóły zabezpieczeń dla grupy {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Dodatkowe referencje dotyczące zabezpieczeń serwera.",
"SECURITY_CREATE_PASSWORD" : "Utwórz hasło",
"KEYSTORE_PASSWORD_MESSAGE": "Określ hasło, aby chronić nowo wygenerowane pliki kluczy zawierające referencje uwierzytelniające serwera.",
"PASSWORD_MESSAGE": "Podaj hasło w celu ochrony nowo wygenerowanych plików, które zawierają referencje uwierzytelniające serwera.",
"KEYSTORE_PASSWORD": "Hasło magazynu kluczy",
"CONFIRM_KEYSTORE_PASSWORD": "Potwierdź hasło do magazynu kluczy",
"PASSWORDS_DONT_MATCH": "Hasła nie są zgodne",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "Potwierdź hasło {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "Potwierdź hasło {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Przegląd i wdrożenie",
"REVIEW_AND_DEPLOY_MESSAGE" : "Wszystkie pola {0} przed wdrożeniem.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "należy wypełnić",
"READY_FOR_DEPLOYMENT": "gotowe do wdrożenia",
"READY_FOR_DEPLOYMENT_CAPS": "Gotowe do wdrożenia.",
"READY_TO_DEPLOY": "Formularz jest kompletny. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "Formularz jest kompletny. Pakiet serwera: {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "Formularz jest kompletny. Kontener produktu Docker: {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "WDRÓŻ",

"DEPLOY_UPLOADING" : "Przesyłanie pakietu serwera musi się zakończyć...",
"DEPLOY_FILE_UPLOADING" : "Kończenie przesyłania pliku...",
"UPLOADING": "Przesyłanie...",
"DEPLOY_UPLOADING_MESSAGE" : "To okno powinno pozostać otwarte do chwili rozpoczęcia procesu wdrażania.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "Po przesłaniu pakietu {0} w tym miejscu można monitorować postęp procesu wdrażania.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} -ukończono {1}%", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Obserwuj aktualizacje tutaj lub zamknij okno i zezwól na działanie w tle.",
"DEPLOY_CHECK_STATUS": "W dowolnej chwili można sprawdzić status wdrażania, klikając ikonę Zadania w tle znajdującą się w prawym górnym rogu tego ekranu.",
"DEPLOY_IN_PROGRESS": "Trwa wdrażanie.",
"DEPLOY_VIEW_BG_TASKS": "Wyświetl zadania w tle",
"DEPLOYMENT_PROGRESS": "Postęp wdrażania",
"DEPLOYING_IMAGE": "{0} na hostach : {1}", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Wyświetl pomyślnie wdrożone serwery",
"DEPLOY_PERCENTAGE": "UKOŃCZONO {0}%", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "Wdrażanie zostało zakończone!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "Wdrożenie zostało zakończone, ale wystąpiły błędy.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "Można przejrzeć szczegółowe informacje o błędach, sprawdzić nowo wdrożone serwery lub rozpocząć inne wdrażanie.",
"DEPLOYING": "Wdrażanie...",
"DEPLOYMENT_FAILED": "Wdrażanie nie powiodło się.",
"RETURN_DEPLOY": "Wróć do formularza wdrożenia i uruchom je ponownie",
"REUTRN_DEPLOY_HEADER": "Spróbuj ponownie",

//Footer
"FOOTER": "Więcej serwerów do wdrożenia?",
"FOOTER_BUTTON_MESSAGE" : "Rozpocznij inny proces wdrażania",

//Error stuff
"ERROR_TITLE": "Podsumowanie błędów",
"ERROR_VIEW_DETAILS" : "Wyświetl szczegóły błędów",
"ONE_ERROR_ONE_HOST": "Wystąpił jeden błąd na jednym hoście",
"ONE_ERROR_MULTIPLE_HOST": "Wystąpił jeden błąd na wielu hostach",
"MULTIPLE_ERROR_ONE_HOST": "Wystąpiło wiele błędów na jednym hoście",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Wystąpiło wiele błędów na wielu hostach",
"INITIALIZATION_ERROR_MESSAGE": "Nie można uzyskać dostępu do hosta lub informacji o regułach wdrażania na serwerze",
"TRANSLATIONS_ERROR_MESSAGE" : "Nie można uzyskać dostępu do udostępnionych łańcuchów",
"MISSING_HOST": "Wybierz z listy co najmniej jeden host.",
"INVALID_CHARACTERS" : "Pole nie może zawierać znaków specjalnych, takich jak '()$%&'",
"INVALID_DOCKER_IMAGE" : "Nie znaleziono obrazu",
"ERROR_HOSTS" : "{0} i {1} inne" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
