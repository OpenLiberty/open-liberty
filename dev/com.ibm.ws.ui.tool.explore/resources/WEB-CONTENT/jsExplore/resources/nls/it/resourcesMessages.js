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
    ERROR : "Errore",
    ERROR_STATUS : "Errore {0}: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Si è verificato un errore durante la richiesta di {0}.", // url
    ERROR_URL_REQUEST : "Si è verificato l'errore {0} durante la richiesta di {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "Il server non ha risposto nel tempo assegnato.",
    ERROR_APP_NOT_AVAILABLE : "L'applicazione {0} non è più disponibile per il server {1} sull'host {2} nella directory {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Si è verificato un errore durante il tentativo di {0} l'applicazione {1} sul server {2} sull'host {3} nella directory {4}.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "Il cluster {0} è {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "Il cluster {0} non è più disponibile.", //clusterName
    STOP_FAILED_DURING_RESTART : "L'arresto non è stato completato correttamente durante il riavvio.  L'errore era: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Si è verificato un errore durante il tentativo di {0} il cluster {1}.", //operation, clusterName
    SERVER_NONEXISTANT : "Il server {0} non esiste.", // serverName
    ERROR_SERVER_OPERATION : "Si è verificato un errore durante il tentativo di {0} il server {1} sull'host {2} nella directory {3}.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "La modalità di manutenzione per il server {0} sull'host {1} nella directory {2} non è stata impostata a causa di un errore.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "Il tentativo di annullare l'impostazione della modalità di manutenzione per il server {0} sull'host {1} nella directory {2} non è stato completato a causa di un errore.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "La modalità di manutenzione per l'host {0} non è stata impostata a causa di un errore.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "Il tentativo di annullare l'impostazione della modalità di manutenzione per l'host {0} non è stato completato a causa di un errore.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Errore durante l'avvio del server.",
    SERVER_START_CLEAN_ERROR: "Errore durante l'avvio del server --clean. ",
    SERVER_STOP_ERROR: "Errore durante l'arresto del server.",
    SERVER_RESTART_ERROR: "Errore durante il riavvio del server.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'Il server non si è arrestato. L\'API richiesta per arrestare il server non era disponibile.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'Il server non si è arrestato. Non è stato possibile determinare l\'API richiesta per arrestare il server.',
    STANDALONE_STOP_FAILED : 'L\'operazione di arresto del server non è stata completata correttamente. Per i dettagli, consultare i log del server.',
    STANDALONE_STOP_SUCCESS : 'Arresto del server riuscito.',
});

