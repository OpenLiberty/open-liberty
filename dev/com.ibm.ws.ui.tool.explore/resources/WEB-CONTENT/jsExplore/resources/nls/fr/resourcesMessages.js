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
    ERROR : "Erreur",
    ERROR_STATUS : "{0} Erreur : {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Erreur lors de la demande {0}.", // url
    ERROR_URL_REQUEST : "{0} Erreur lors de la demande de {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "Le serveur n'a pas répondu dans le temps alloué.",
    ERROR_APP_NOT_AVAILABLE : "L'application {0} n'est plus disponible pour le serveur {1} sur l'hôte {2} dans le répertoire {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Une erreur s'est produite lors d'une tentative pour {0} l'application {1} sur le serveur {2} sur l'hôte {3} dans le répertoire {4}.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "Le cluster {0} est {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "Le cluster {0} n'est plus disponible.", //clusterName
    STOP_FAILED_DURING_RESTART : "L'arrêt n'a pas abouti lors du redémarrage.  L'erreur était la suivante : {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Erreur lors d'une tentative pour {0} le cluster {1}.", //operation, clusterName
    SERVER_NONEXISTANT : "Le serveur {0} n'existe pas.", // serverName
    ERROR_SERVER_OPERATION : "Une erreur s'est produite lors d'une tentative pour {0} le serveur {1} sur l'hôte {2} dans le répertoire {3}.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "Le mode maintenance du serveur {0} sur l'hôte {1} dans le répertoire {2} n'a pas été défini en raison d'une erreur.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "Les critères utilisés pour définir le mode maintenance du serveur {0} sur l'hôte {1} dans le répertoire {2} n'ont pas pu être annulés en raison d'une erreur.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "Le mode maintenance de l'hôte {0} n'a pas été défini en raison d'une erreur.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "Les critères utilisés pour définir le mode maintenance de l'hôte {0} n'ont pas pu être annulés en raison d'une erreur.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Erreur lors du démarrage du serveur.",
    SERVER_START_CLEAN_ERROR: "Erreur lors du démarrage du serveur --clean.",
    SERVER_STOP_ERROR: "Erreur lors de l'arrêt du serveur.",
    SERVER_RESTART_ERROR: "Erreur lors du redémarrage du serveur.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'Le serveur ne s\'est pas arrêté. L\'API nécessaire à l\'arrêt du serveur n\'était pas disponible.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'Le serveur ne s\'est pas arrêté. L\'API nécessaire à l\'arrêt du serveur n\'a pas pu être identifiée.',
    STANDALONE_STOP_FAILED : 'L\'opération d\'arrêt du serveur ne s\'est pas terminée correctement. Pour plus de détails, consultez les journaux du serveur.',
    STANDALONE_STOP_SUCCESS : 'Le serveur s\'est arrêté correctement.',
});

