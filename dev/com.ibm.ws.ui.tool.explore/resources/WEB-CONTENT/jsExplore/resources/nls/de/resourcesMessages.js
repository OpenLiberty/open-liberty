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
    ERROR : "Fehler",
    ERROR_STATUS : "{0} Fehler: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Beim Anfordern von {0} ist ein Fehler aufgetreten.", // url
    ERROR_URL_REQUEST : "{0} Beim Anfordern von {1} ist ein Fehler aufgetreten.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "Der Server hat nicht in der zulässigen Zeit geantwortet.",
    ERROR_APP_NOT_AVAILABLE : "Die Anwendung {0} ist für den Server {1} auf dem Host {2} im Verzeichnis {3} nicht verfügbar.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Beim Ausführen der Operation {0} für die Anwendung {1} auf dem Server {2} auf dem Host {3} im Verzeichnis {4} ist ein Fehler aufgetreten.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "Der Cluster {0} hat den Status {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "Der Cluster {0} ist nicht mehr verfügbar.", //clusterName
    STOP_FAILED_DURING_RESTART : "Die Stoppoperation während des Neustarts wurde nicht erfolgreich ausgeführt. Fehler: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Beim Ausführen der Operation {0} für den Cluster {1} ist ein Fehler aufgetreten.", //operation, clusterName
    SERVER_NONEXISTANT : "Der Server {0} ist nicht vorhanden.", // serverName
    ERROR_SERVER_OPERATION : "Beim Ausführen der Operation {0} für den Server {1} auf dem Host {2} im Verzeichnis {3} ist ein Fehler aufgetreten.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "Der Wartungsmodus für den Server {0} auf dem Host {1} im Verzeichnis {2} wurde aufgrund eines Fehlers nicht gesetzt.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "Der Versuch, den Wartungsmodus für den Server {0} auf dem Host {1} im Verzeichnis {2} aufzuheben, ist aufgrund eines Fehlers fehlgeschlagen.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "Der Wartungsmodus für den Host {0} wurde aufgrund eines Fehlers nicht gesetzt.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "Der Versuch, den Wartungsmodus für den Host {0} aufzuheben, ist aufgrund eines Fehlers fehlgeschlagen.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Beim Starten des Servers ist ein Fehler aufgetreten.",
    SERVER_START_CLEAN_ERROR: "Fehler bei server start --clean.",
    SERVER_STOP_ERROR: "Beim Stoppen des Servers ist ein Fehler aufgetreten.",
    SERVER_RESTART_ERROR: "Beim erneuten Starten des Servers ist ein Fehler aufgetreten.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'Der Server wurde nicht gestoppt. Die erforderliche API zum Stoppen des Servers ist nicht verfügbar.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'Der Server wurde nicht gestoppt. Die erforderliche API zum Stoppen des Servers konnte nicht bestimmt werden.',
    STANDALONE_STOP_FAILED : 'Die Operation zum Stoppen des Servers wurde nicht erfolgreich ausgeführt. Suchen Sie in den Serverprotokollen nach Einzelheiten.',
    STANDALONE_STOP_SUCCESS : 'Der Server wurde erfolgreich gestoppt.',
});

