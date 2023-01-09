/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    ERROR : "Błąd",
    ERROR_STATUS : "Błąd {0}: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Wystąpił błąd podczas żądania adresu URL {0}.", // url
    ERROR_URL_REQUEST : "Wystąpił błąd {0} podczas żądania adresu URL {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "Serwer nie odpowiedział w przydzielonym czasie.",
    ERROR_APP_NOT_AVAILABLE : "Aplikacja {0} nie jest już dostępna dla serwera {1} na hoście {2} w katalogu {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Wystąpił błąd podczas próby wykonania operacji {0} dla aplikacji {1} na serwerze {2} na hoście {3} w katalogu {4}.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "Klaster {0} ma następujący status: {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "Klaster {0} nie jest już dostępny.", //clusterName
    STOP_FAILED_DURING_RESTART : "Zatrzymywanie podczas restartowania nie zostało zakończone pomyślnie.  Wystąpił następujący błąd: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Wystąpił błąd podczas próby wykonania operacji {0} dla klastra {1}.", //operation, clusterName
    SERVER_NONEXISTANT : "Serwer {0} nie istnieje.", // serverName
    ERROR_SERVER_OPERATION : "Wystąpił błąd podczas próby wykonania operacji {0} dla serwera {1} na hoście {2} w katalogu {3}.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "Tryb konserwacji serwera {0} na hoście {1} w katalogu {2} nie został ustawiony z powodu błędu.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "Próba anulowania ustawienia trybu konserwacji dla serwera {0} na hoście {1} w katalogu {2} nie została zakończona z powodu błędu.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "Tryb konserwacji hosta {0} nie został ustawiony z powodu błędu.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "Próba anulowania ustawienia trybu konserwacji dla hosta {0} nie została zakończona z powodu błędu.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Błąd podczas uruchamiania serwera.",
    SERVER_START_CLEAN_ERROR: "Błąd podczas uruchamiania serwera z opcją --clean.",
    SERVER_STOP_ERROR: "Błąd podczas zatrzymywania serwera.",
    SERVER_RESTART_ERROR: "Błąd podczas restartowania serwera.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'Serwer nie został zatrzymany. Interfejs API wymagany do zatrzymania serwera był niedostępny.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'Serwer nie został zatrzymany. Nie można określić interfejsu API wymaganego do zatrzymania serwera.',
    STANDALONE_STOP_FAILED : 'Operacja zatrzymania serwera nie została zakończona pomyślnie. Szczegółowe informacje zawiera dziennik serwera.',
    STANDALONE_STOP_SUCCESS : 'Pomyślnie zatrzymano serwer.',
});
