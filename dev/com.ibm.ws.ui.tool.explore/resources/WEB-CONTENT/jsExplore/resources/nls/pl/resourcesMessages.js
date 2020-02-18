/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    ERROR : "Błąd",
    ERROR_STATUS : "{0} Błąd: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Wystąpił błąd podczas żądania adresu {0}.", // url
    ERROR_URL_REQUEST : "{0} Wystąpił błąd podczas żądania adresu {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "Serwer nie odpowiedział w przydzielonym czasie.",
    ERROR_APP_NOT_AVAILABLE : "Aplikacja {0} nie jest już dostępna dla serwera {1} na hoście {2} w katalogu {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Wystąpił błąd podczas próby wykonania operacji {0} dla aplikacji {1} na serwerze {2} hosta {3} w katalogu {4}.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "Status klastra {0} to {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "Klaster {0} jest już niedostępny.", //clusterName
    STOP_FAILED_DURING_RESTART : "Zatrzymanie nie zostało wykonane pomyślnie podczas restartowania.  Błąd: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Wystąpił błąd podczas próby wykonania operacji {0} dla klastra {1}.", //operation, clusterName
    SERVER_NONEXISTANT : "Serwer {0} nie istnieje.", // serverName
    ERROR_SERVER_OPERATION : "Wystąpił błąd podczas próby wykonania operacji {0} dla serwera {1} na hoście {2} w katalogu {3}.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "Tryb konserwacji serwera {0} na hoście {1} w katalogu {2} nie został ustawiony z powodu błędu.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "Próba cofnięcia ustawienia trybu konserwacji serwera {0} na hoście {1} w katalogu {2} nie powiodła się z powodu błędu.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "Nie ustawiono trybu konserwacji dla hosta {0} z powodu błędu.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "Próba cofnięcia ustawienia trybu konserwacji dla hosta {0} nie powiodła się z powodu błędu.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Wystąpił błąd podczas uruchamiania serwera.",
    SERVER_START_CLEAN_ERROR: "Błąd podczas uruchamiania serwera z parametrem --clean.",
    SERVER_STOP_ERROR: "Wystąpił błąd podczas zatrzymywania serwera.",
    SERVER_RESTART_ERROR: "Wystąpił błąd podczas restartowania serwera.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'Serwer nie został zatrzymany. Funkcja API wymagana do zatrzymania serwera była niedostępna.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'Serwer nie został zatrzymany. Nie można było ustalić funkcji API wymaganej do zatrzymania serwera.',
    STANDALONE_STOP_FAILED : 'Operacja zatrzymania serwera nie została pomyślnie wykonana. Szczegółowe informacje zawiera dziennik serwera.',
    STANDALONE_STOP_SUCCESS : 'Serwer został pomyślnie zatrzymany.',
});

