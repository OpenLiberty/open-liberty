/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    EXPLORER : "Explorateur",
    EXPLORE : "Explorer",
    DASHBOARD : "Tableau de bord",
    DASHBOARD_VIEW_ALL_APPS : "Afficher toutes les applications",
    DASHBOARD_VIEW_ALL_SERVERS : "Afficher tous les serveurs",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Afficher tous les clusters",
    DASHBOARD_VIEW_ALL_HOSTS : "Afficher tous les hôtes",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Afficher tous les environnements d'exécution",
    SEARCH : "Recherche",
    SEARCH_RECENT : "Recherches récentes",
    SEARCH_RESOURCES : "Rechercher des ressources",
    SEARCH_RESULTS : "Résultats de recherche",
    SEARCH_NO_RESULTS : "Aucun résultat",
    SEARCH_NO_MATCHES : "Aucune correspondance",
    SEARCH_TEXT_INVALID : "Le texte à rechercher comporte des caractères non valides",
    SEARCH_CRITERIA_INVALID : "Les critères de recherche ne sont pas valides.",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} n'est pas une valeur valide si elle est associée à {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "Indiquez {0} seulement une fois.",
    SEARCH_TEXT_MISSING : "Le texte à rechercher est obligatoire.",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "La recherche d'étiquettes d'application sur un serveur n'est pas prise en charge.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "La recherche d'étiquettes d'application sur un cluster n'est pas prise en charge.",
    SEARCH_UNSUPPORT : "Les critères de recherche ne sont pas pris en charge.",
    SEARCH_SWITCH_VIEW : "Changer de vue",
    FILTERS : "Filtres",
    DEPLOY_SERVER_PACKAGE : "Déployer le package serveur",
    MEMBER_OF : "Membre de",
    N_CLUSTERS: "{0} Clusters ...",

    INSTANCE : "Instance",
    INSTANCES : "Instances",
    APPLICATION : "Application",
    APPLICATIONS : "Applications",
    SERVER : "Serveur",
    SERVERS : "Serveurs",
    CLUSTER : "Cluster",
    CLUSTERS : "Clusters",
    CLUSTER_NAME : "Nom du cluster : ",
    CLUSTER_STATUS : "Statut du cluster : ",
    APPLICATION_NAME : "Nom de l'application : ",
    APPLICATION_STATE : "Etat de l'application : ",
    HOST : "Hôte",
    HOSTS : "Hôtes",
    RUNTIME : "Exécution",
    RUNTIMES : "Environnements d'exécution",
    PATH : "Chemin d'accès",
    CONTROLLER : "Contrôleur",
    CONTROLLERS : "Contrôleurs",
    OVERVIEW : "Présentation",
    CONFIGURE : "Configurer",

    SEARCH_RESOURCE_TYPE: "Type", // Search by resource types
    SEARCH_RESOURCE_STATE: "Etat", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Tout", // Search all resource types
    SEARCH_RESOURCE_NAME: "Nom", // Search by resource name
    SEARCH_RESOURCE_TAG: "Balise", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Conteneur", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Aucune", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Type d'environnement d'exécution", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Propriétaire", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Contact", // Search by contact
    SEARCH_RESOURCE_NOTE: "Important", // Search by note

    GRID_HEADER_USERDIR : "Annuaire d'utilisateurs",
    GRID_HEADER_NAME : "Nom",
    GRID_LOCATION_NAME : "Emplacement",
    GRID_ACTIONS : "Actions de grille",
    GRID_ACTIONS_LABEL : "Actions de la grille {0}",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} sur {1} ({2})", // server on host (/path)

    STATS : "Moniteur",
    STATS_ALL : "Tout",
    STATS_VALUE : "Valeur : {0}",
    CONNECTION_IN_USE_STATS : "{0} en cours d'utilisation = {1} gérés - {2} disponibles",
    CONNECTION_IN_USE_STATS_VALUE : "Valeur : {0} en cours d'utilisation = {1} gérés - {2} disponibles",
    DATA_SOURCE : "Source de données : {0}",
    STATS_DISPLAY_LEGEND : "Afficher une légende",
    STATS_HIDE_LEGEND : "Masquer une légende",
    STATS_VIEW_DATA : "Afficher les données du graphique",
    STATS_VIEW_DATA_TIMESTAMP : "Horodatage",
    STATS_ACTION_MENU : "Menu des actions de {0}",
    STATS_SHOW_HIDE : "Ajouter les métriques de ressource",
    STATS_SHOW_HIDE_SUMMARY : "Ajouter des métriques pour le récapitulatif",
    STATS_SHOW_HIDE_TRAFFIC : "Ajouter des métriques pour le trafic",
    STATS_SHOW_HIDE_PERFORMANCE : "Ajouter des métriques pour les performances",
    STATS_SHOW_HIDE_AVAILABILITY : "Ajouter des métriques pour la disponibilité",
    STATS_SHOW_HIDE_ALERT : "Ajouter des métriques pour les alertes",
    STATS_SHOW_HIDE_LIST_BUTTON : "Afficher ou masquer la liste des métriques de ressource",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Editer les graphiques",
    STATS_SHOW_HIDE_CONFIRM : "Sauvegarder",
    STATS_SHOW_HIDE_CANCEL : "Annuler",
    STATS_SHOW_HIDE_DONE : "Terminé",
    STATS_DELETE_GRAPH : "Supprimer un graphique",
    STATS_ADD_CHART_LABEL : "Ajouter un graphique pour l'afficher",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Ajouter tous les graphiques de la machine virtuelle Java pour les afficher",
    STATS_HEAP_TITLE : "Mémoire dynamique utilisée",
    STATS_HEAP_USED : "Utilisé : {0} Mo",
    STATS_HEAP_COMMITTED : "Envoyé : {0} Mo",
    STATS_HEAP_MAX : "Max : {0} Mo",
    STATS_HEAP_X_TIME : "Heure",
    STATS_HEAP_Y_MB : "Mégaoctets utilisés",
    STATS_HEAP_Y_MB_LABEL : "{0} Mo",
    STATS_CLASSES_TITLE : "Classes chargées",
    STATS_CLASSES_LOADED : "Chargé : {0}",
    STATS_CLASSES_UNLOADED : "Déchargé : {0}",
    STATS_CLASSES_TOTAL : "Total : {0}",
    STATS_CLASSES_Y_TOTAL : "Classes chargées",
    STATS_PROCESSCPU_TITLE : "Utilisation de l'unité centrale",
    STATS_PROCESSCPU_USAGE : "Utilisation de l'unité centrale : {0} %",
    STATS_PROCESSCPU_Y_PERCENT : "Pourcentage d'espace de l'unité centrale",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Unités d'exécution de la machine virtuelle Java actives",
    STATS_LIVE_MSG_INIT : "Affichage des données réelles",
    STATS_LIVE_MSG :"Ce graphique ne comporte pas de données d'historique. Il continuera à afficher les données les plus récentes des 10 dernières minutes.",
    STATS_THREADS_ACTIVE : "Actif : {0}",
    STATS_THREADS_PEAK : "Activité maximale : {0}",
    STATS_THREADS_TOTAL : "Total : {0}",
    STATS_THREADS_Y_THREADS : "Unités d'exécution",
    STATS_TP_POOL_SIZE : "Taille de pool",
    STATS_JAXWS_TITLE : "Services Web JAX-WS",
    STATS_JAXWS_BUTTON_LABEL : "Ajouter tous les graphiques de l'API Java des services Web XML pour les afficher",
    STATS_JW_AVG_RESP_TIME : "Temps de réponse moyen",
    STATS_JW_AVG_INVCOUNT : "Nombre d'appels moyen",
    STATS_JW_TOTAL_FAULTS : "Incidents d'exécution totaux",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Sélectionner des ressources...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} ressources",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 ressource",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "Vous devez sélectionner au moins une ressource.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "Aucune donnée n'est disponible pour l'intervalle sélectionné.",
    STATS_ACCESS_LOG_TITLE : "Journal d'accès",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Ajouter tous les graphiques du journal d'accès pour les afficher",
    STATS_ACCESS_LOG_GRAPH : "Nombre de messages de journal d'accès",
    STATS_ACCESS_LOG_SUMMARY : "Récapitulatif du journal d'accès",
    STATS_ACCESS_LOG_TABLE : "Liste des messages du journal d'accès",
    STATS_MESSAGES_TITLE : "Messages et traces",
    STATS_MESSAGES_BUTTON_LABEL : "Ajouter tous les graphiques des messages et traces pour les afficher",
    STATS_MESSAGES_GRAPH : "Nombre de messages de journal",
    STATS_MESSAGES_TABLE : "Liste de messages de journal",
    STATS_FFDC_GRAPH : "Nombre d'outils de diagnostic de premier niveau (FFDC)",
    STATS_FFDC_TABLE : "Liste d'outils de diagnostic de premier niveau (FFDC)",
    STATS_TRACE_LOG_GRAPH : "Nombre de messages trace",
    STATS_TRACE_LOG_TABLE : "Liste de messages trace",
    STATS_THREAD_POOL_TITLE : "Pool d'unités d'exécution",
    STATS_THREAD_POOL_BUTTON_LABEL : "Ajouter tous les graphiques du pool d'unités d'exécution pour les afficher",
    STATS_THREADPOOL_TITLE : "Unités d'exécution Liberty actives",
    STATS_THREADPOOL_SIZE : "Taille de pool : {0}",
    STATS_THREADPOOL_ACTIVE : "Actif : {0}",
    STATS_THREADPOOL_TOTAL : "Total : {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Unités d'exécution actives",
    STATS_SESSION_MGMT_TITLE : "Sessions",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Ajouter tous les graphiques des sessions pour les afficher",
    STATS_SESSION_CONFIG_LABEL : "Sélectionner une ou plusieurs sessions...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} sessions",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 session",
    STATS_SESSION_CONFIG_SELECT_ONE : "Vous devez sélectionner au moins une session.",
    STATS_SESSION_TITLE : "Sessions actives",
    STATS_SESSION_Y_ACTIVE : "Sessions actives",
    STATS_SESSION_LIVE_LABEL : "Nombre d'éléments actifs : {0}",
    STATS_SESSION_CREATE_LABEL : "Nombre d'éléments créés : {0}",
    STATS_SESSION_INV_LABEL : "Nombre d'éléments invalidés : {0}",
    STATS_SESSION_INV_TIME_LABEL : "Nombre d'éléments invalidés en fonction du délai d'expiration : {0}",
    STATS_WEBCONTAINER_TITLE : "Applications Web",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Ajouter tous les graphiques des applications Web pour les afficher",
    STATS_SERVLET_CONFIG_LABEL : "Sélectionner un ou plusieurs servlets...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} servlets",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "Vous devez sélectionner au moins un servlet.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Nombre de demande",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Nombre de demande",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Nombre de réponses",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Nombre de réponses",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Temps de réponse moyen (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Temps de réponse (ns)",
    STATS_CONN_POOL_TITLE : "Pool de connexions",
    STATS_CONN_POOL_BUTTON_LABEL : "Ajouter tous les graphiques du pool de connexions pour les afficher",
    STATS_CONN_POOL_CONFIG_LABEL : "Sélectionner une ou plusieurs sources de données...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} sources de données",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 source de données",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "Vous devez sélectionner au moins une source de données.",
    STATS_CONNECT_IN_USE_TITLE : "Connexions en cours d'utilisation",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Connexions",
    STATS_CONNECT_IN_USE_LABEL : "En cours d'utilisation : {0}",
    STATS_CONNECT_USED_USED_LABEL : "Utilisé : {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Libre : {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Créé : {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Supprimé : {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Temps d'attente moyen (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Temps d'attente (milliseconde)",
    STATS_TIME_ALL : "Tout",
    STATS_TIME_1YEAR : "1 an",
    STATS_TIME_1MONTH : "1 mois",
    STATS_TIME_1WEEK : "1 sem",
    STATS_TIME_1DAY : "1 j",
    STATS_TIME_1HOUR : "1 h",
    STATS_TIME_10MINUTES : "10 min",
    STATS_TIME_5MINUTES : "5 min",
    STATS_TIME_1MINUTE : "1 min",
    STATS_PERSPECTIVE_SUMMARY : "Récapitulatif",
    STATS_PERSPECTIVE_TRAFFIC : "Trafic",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "Trafic relatif à la machine virtuelle Java",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Trafic relatif à la connexion",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Trafic relatif au journal d'accès",
    STATS_PERSPECTIVE_PROBLEM : "Problème",
    STATS_PERSPECTIVE_PERFORMANCE : "Performances",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "Performances de la machine virtuelle Java",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Performances de connexion",
    STATS_PERSPECTIVE_ALERT : "Analyse des alertes",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Alertes du journal d'accès",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Alertes du journal de trace ou de messages",
    STATS_PERSPECTIVE_AVAILABILITY : "Disponibilité",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Dernière minute",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "5 dernières minutes",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "10 dernières minutes",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Dernière heure",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Dernier jour",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Semaine dernière",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Mois dernier",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Année dernière",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Il y a moins de {0} sec.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Il y a moins de {0} min.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Il y a moins de {0} minute(s) {1} seconde(s)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Il y a moins de {0} h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Il y a moins de {0} h {1} min.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Il y a moins de {0} jour(s)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Il y a moins de {0} jour(s) {1} h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Il y a moins de {0} semaine(s)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Il y a moins de {0} semaine(s) {1} jour(s)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Il y a moins de {0} mois",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Il y a moins de {0} mois {1} jour(s)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Il y a moins de {0} an(s)",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Il y a moins de {0} an(s) {1} mois",

    STATS_LIVE_UPDATE_LABEL: "Mise à jour en temps réel",
    STATS_TIME_SELECTOR_NOW_LABEL: "Maintenant",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Messages de journal",

    AUTOSCALED_APPLICATION : "Application automatiquement échelonnée",
    AUTOSCALED_SERVER : "Serveur automatiquement échelonné",
    AUTOSCALED_CLUSTER : "Cluster automatiquement échelonné",
    AUTOSCALED_POLICY : "Règles de mise à l'échelle automatique",
    AUTOSCALED_POLICY_DISABLED : "Les règles de mise à l'échelle automatique sont désactivées",
    AUTOSCALED_NOACTIONS : "Actions indisponibles pour les ressources automatiquement échelonnées",

    START : "Démarrer",
    START_CLEAN : "Démarrer --clean",
    STARTING : "Démarrage",
    STARTED : "Démarré",
    RUNNING : "En cours d'exécution",
    NUM_RUNNING: "{0} en cours d'exécution",
    PARTIALLY_STARTED : "Partiellement démarré",
    PARTIALLY_RUNNING : "Partiellement en cours d'exécution",
    NOT_STARTED : "Non démarré",
    STOP : "Arrêter",
    STOPPING : "En cours d'arrêt",
    STOPPED : "Arrêté",
    NUM_STOPPED : "{0} arrêté (s)",
    NOT_RUNNING : "Pas en cours d'exécution",
    RESTART : "Redémarrer",
    RESTARTING : "Redémarrage en cours",
    RESTARTED : "Redémarré",
    ALERT : "Alerte",
    ALERTS : "Alertes",
    UNKNOWN : "Inconnu",
    NUM_UNKNOWN : "{0} inconnu(s)",
    SELECT : "Sélectionner",
    SELECTED : "Sélectionné",
    SELECT_ALL : "Sélectionner tout",
    SELECT_NONE : "Ne rien sélectionner",
    DESELECT: "Désélectionner",
    DESELECT_ALL : "Désélectionner tout",
    TOTAL : "Total",
    UTILIZATION : "Utilisation supérieure à {0} %", // percent

    ELLIPSIS_ARIA: "Développez pour afficher d'autres options.",
    EXPAND : "Développer",
    COLLAPSE: "Réduire",

    ALL : "Tout",
    ALL_APPS : "Toutes les applications",
    ALL_SERVERS : "Tous les serveurs",
    ALL_CLUSTERS : "Tous les clusters",
    ALL_HOSTS : "Tous les hôtes",
    ALL_APP_INSTANCES : "Toutes les instances d'application",
    ALL_RUNTIMES : "Tous les environnements d'exécution",

    ALL_APPS_RUNNING : "Toutes les applications en cours d'exécution",
    ALL_SERVER_RUNNING : "Tous les serveurs en cours d'exécution",
    ALL_CLUSTERS_RUNNING : "Tous les cluster en cours d'exécution",
    ALL_APPS_STOPPED : "Toutes les applications arrêtées",
    ALL_SERVER_STOPPED : "Tous les serveurs arrêtés",
    ALL_CLUSTERS_STOPPED : "Tous les clusters arrêtés",
    ALL_SERVERS_UNKNOWN : "Tous les serveurs inconnus",
    SOME_APPS_RUNNING : "Quelques applications en cours d'exécution",
    SOME_SERVERS_RUNNING : "Quelques serveurs en cours d'exécution",
    SOME_CLUSTERS_RUNNING : "Quelques cluster en cours d'exécution",
    NO_APPS_RUNNING : "Aucune application en cours d'exécution",
    NO_SERVERS_RUNNING : "Aucun serveur en cours d'exécution",
    NO_CLUSTERS_RUNNING : "Aucun cluster en cours d'exécution",

    HOST_WITH_ALL_SERVERS_RUNNING: "Hôtes avec tous serveurs en cours d'exécution", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Hôtes avec des serveurs en cours d'exécution",
    HOST_WITH_NO_SERVERS_RUNNING: "Hôtes sans aucun serveur en cours d'exécution", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Hôtes avec tous serveurs arrêtés",
    HOST_WITH_SERVERS_RUNNING: "Hôtes avec serveurs en cours d'exécution",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Environnements d'exécution avec certains serveurs en cours d'exécution",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Environnements d'exécution avec tous les serveurs arrêtés",
    RUNTIME_WITH_SERVERS_RUNNING: "Environnements d'exécution avec des serveurs en cours d'exécution",

    START_ALL_APPS : "Démarrer toutes les applications ?",
    START_ALL_INSTANCES : "Démarrer toutes les instances d'applications ?",
    START_ALL_SERVERS : "Démarrer tous les serveurs ?",
    START_ALL_CLUSTERS : "Démarrer tous les clusters ?",
    STOP_ALL_APPS : "Arrêter toutes les applications ?",
    STOPE_ALL_INSTANCES : "Arrêter toutes les instances d'applications ?",
    STOP_ALL_SERVERS : "Arrêter tous les serveurs ?",
    STOP_ALL_CLUSTERS : "Arrêter tous les clusters ?",
    RESTART_ALL_APPS : "Redémarrer toutes les applications ?",
    RESTART_ALL_INSTANCES : "Redémarrer toutes les instances d'applications ?",
    RESTART_ALL_SERVERS : "Redémarrer tous les serveurs ?",
    RESTART_ALL_CLUSTERS : "Redémarrer tous les clusters ?",

    START_INSTANCE : "Démarrer l'instance d'application ?",
    STOP_INSTANCE : "Arrêter l'instance d'application ?",
    RESTART_INSTANCE : "Redémarrer l'instance d'application ?",

    START_SERVER : "Démarrer le serveur {0} ?",
    STOP_SERVER : "Arrêter le serveur {0} ?",
    RESTART_SERVER : "Redémarrer le serveur {0} ?",

    START_ALL_INSTS_OF_APP : "Démarrer toutes les instances de {0} ?", // application name
    START_APP_ON_SERVER : "Démarrer {0} sur {1} ?", // app name, server name
    START_ALL_APPS_WITHIN : "Démarrer toutes les applications au sein de {0} ?", // resource
    START_ALL_APP_INSTS_WITHIN : "Démarrer toutes les instances d'applications au sein de {0} ?", // resource
    START_ALL_SERVERS_WITHIN : "Démarrer tous les serveurs au sein de {0} ?", // resource
    STOP_ALL_INSTS_OF_APP : "Arrêter toutes les instances de {0} ?", // application name
    STOP_APP_ON_SERVER : "Arrêter {0} sur {1} ?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Arrêter toutes les applications au sein de {0} ?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Arrêter toutes les instances d'applications au sein de {0} ?", // resource
    STOP_ALL_SERVERS_WITHIN : "Arrêter tous les serveurs au sein de {0} ?", // resource
    RESTART_ALL_INSTS_OF_APP : "Redémarrer toutes les instances de {0} ?", // application name
    RESTART_APP_ON_SERVER : "Redémarrer {0} sur {1} ?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Redémarrer toutes les applications au sein de {0} ?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Redémarrer toutes les instances d'applications au sein de {0} ?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Redémarrer tous les serveurs en cours d'exécution au sein de {0} ?", // resource

    START_SELECTED_APPS : "Démarrer toutes les instances des applications sélectionnées ?",
    START_SELECTED_INSTANCES : "Démarrer les instances d'applications sélectionnées ?",
    START_SELECTED_SERVERS : "Démarrer les serveurs sélectionnés ?",
    START_SELECTED_SERVERS_LABEL : "Démarrer les serveurs sélectionnés",
    START_SELECTED_CLUSTERS : "Démarrer les clusters sélectionnés ?",
    START_CLEAN_SELECTED_SERVERS : "Démarrer l'option --clean sur les serveurs sélectionnés ?",
    START_CLEAN_SELECTED_CLUSTERS : "Démarrer l'option --clean sur les clusters sélectionnés ?",
    STOP_SELECTED_APPS : "Arrêter toutes les instances des applications sélectionnées ?",
    STOP_SELECTED_INSTANCES : "Arrêter les instances d'applications sélectionnées ?",
    STOP_SELECTED_SERVERS : "Arrêter les serveurs sélectionnés ?",
    STOP_SELECTED_CLUSTERS : "Arrêter les clusters sélectionnés ?",
    RESTART_SELECTED_APPS : "Redémarrer toutes les instances des applications sélectionnées ?",
    RESTART_SELECTED_INSTANCES : "Redémarrer les instances d'applications sélectionnées ?",
    RESTART_SELECTED_SERVERS : "Redémarrer les serveurs sélectionnés ?",
    RESTART_SELECTED_CLUSTERS : "Redémarrer les clusters sélectionnés ?",

    START_SERVERS_ON_HOSTS : "Démarrer tous les serveurs sur les hôtes sélectionnés ?",
    STOP_SERVERS_ON_HOSTS : "Arrêter tous les serveurs sur les hôtes sélectionnés ?",
    RESTART_SERVERS_ON_HOSTS : "Redémarrer tous les serveurs en cours d'exécution sur les hôtes sélectionnés ?",

    SELECT_APPS_TO_START : "Sélectionner les applications arrêtées à démarrer.",
    SELECT_APPS_TO_STOP : "Sélectionner les applications démarrées à arrêter.",
    SELECT_APPS_TO_RESTART : "Sélectionner les applications démarrées à redémarrer.",
    SELECT_INSTANCES_TO_START : "Sélectionner les instances d'applications arrêtées à démarrer.",
    SELECT_INSTANCES_TO_STOP : "Sélectionner les instances d'applications démarrées à arrêter.",
    SELECT_INSTANCES_TO_RESTART : "Sélectionner les instances d'applications démarrées à redémarrer.",
    SELECT_SERVERS_TO_START : "Sélectionner les serveurs arrêtés à démarrer.",
    SELECT_SERVERS_TO_STOP : "Sélectionner les serveurs démarrés à arrêter.",
    SELECT_SERVERS_TO_RESTART : "Sélectionner les serveurs démarrés à redémarrer.",
    SELECT_CLUSTERS_TO_START : "Sélectionner les clusters arrêtés à démarrer.",
    SELECT_CLUSTERS_TO_STOP : "Sélectionner les clusters démarrés à arrêter.",
    SELECT_CLUSTERS_TO_RESTART : "Sélectionner les clusters démarrés à redémarrer.",

    STATUS : "Statut",
    STATE : "Etat :",
    NAME : "Nom : ",
    DIRECTORY : "Répertoire.",
    INFORMATION : "Informations",
    DETAILS : "Détails",
    ACTIONS : "Actions",
    CLOSE : "Fermer",
    HIDE : "Masquer",
    SHOW_ACTIONS : "Afficher les actions",
    SHOW_SERVER_ACTIONS_LABEL : "Serveur : {0} actions",
    SHOW_APP_ACTIONS_LABEL : "Application : {0} actions",
    SHOW_CLUSTER_ACTIONS_LABEL : "Cluster : {0} actions",
    SHOW_HOST_ACTIONS_LABEL : "Hôte : {0} actions",
    SHOW_RUNTIME_ACTIONS_LABEL : "Actions de l'environnement d'exécution {0}",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "Menu des actions du serveur {0}",
    SHOW_APP_ACTIONS_MENU_LABEL : "Menu des actions de l'application {0}",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "Menu des actions du cluster {0}",
    SHOW_HOST_ACTIONS_MENU_LABEL : "Menu des actions de l'hôte {0}",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "Menu des actions de l'environnement d'exécution {0}",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "Menu des actions de l'environnement d'exécution sur l'hôte {0}",
    SHOW_COLLECTION_MENU_LABEL : "Collection : menu d'actions d'état {0}",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "Recherche : menu d'actions d'état {0}",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0} : état inconnu", // resourceName
    UNKNOWN_STATE_APPS : "{0} applications dans un état inconnu", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} instances d'applications dans un état inconnu", // quantity
    UNKNOWN_STATE_SERVERS : "{0} serveurs dans un état inconnu", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} clusters dans un état inconnu", // quantity

    INSTANCES_NOT_RUNNING : "{0} instances d'applications non en cours d'exécution", // quantity
    APPS_NOT_RUNNING : "{0} applications non en cours d'exécution", // quantity
    SERVERS_NOT_RUNNING : "{0} serveurs non en cours d'exécution", // quantity
    CLUSTERS_NOT_RUNNING : "{0} clusters non en cours d'exécution", // quantity

    APP_STOPPED_ON_SERVER : "{0} arrêtés sur les serveurs en cours d'exécution : {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} applications arrêtées sur les serveurs en cours d'exécution : {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} applications arrêtées sur les serveurs en cours d'exécution.", // quantity
    NUMBER_RESOURCES : "{0} ressources", // quantity
    NUMBER_APPS : "{0} applications", // quantity
    NUMBER_SERVERS : "{0} serveurs", // quantity
    NUMBER_CLUSTERS : "{0} clusters", // quantity
    NUMBER_HOSTS : "{0} hôtes", // quantity
    NUMBER_RUNTIMES : "{0} environnements d'exécution", // quantity
    SERVERS_INSERT : "serveurs",
    INSERT_STOPPED_ON_INSERT : "{0} arrêtés sur {1} serveurs en cours d'exécution.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} arrêté sur le serveur {1} en cours d'exécution", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "{0} sur le cluster {1} arrêté sur les serveurs en cours d'exécution : {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} instances d'applications arrêtées sur les serveurs en cours d'exécution.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0} : instance d'applications non en cours d'exécution", // serverNames

    NOT_ALL_APPS_RUNNING : "{0} : certaines applications ne sont pas en cours d'exécution", // serverName[]
    NO_APPS_RUNNING : "{0} : aucune application n'est en cours d'exécution", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} serveurs avec certaines applications en cours d'exécution", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} serveurs avec aucune applications en cours d'exécution", // quantity

    COUNT_OF_APPS_SELECTED : "{0} applications sélectionnées",
    RATIO_RUNNING : "{0} en cours d'exécution", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0} sélectionnées",

    NO_HOSTS_SELECTED : "Aucun hôte sélectionné",
    NO_DEPLOY_RESOURCE : "Aucune ressource pour déployer l'installation",
    NO_TOPOLOGY : "Aucune {0}.",
    COUNT_OF_APPS_STARTED  : "{0} applications démarrées",

    APPS_LIST : "{0} applications",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} instance(s) en cours d'exécution",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} serveur(s) en cours d'exécution",
    RESOURCE_ON_RESOURCE : "{0} sur {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} sur le serveur {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} sur le cluster {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Le redémarrage est désactivé pour ce serveur car il héberge le centre d'administration",
    ACTION_DISABLED_FOR_USER: "Les actions sont désactivées sur cette ressource car l'utilisateur n'est pas autorisé",

    RESTART_AC_TITLE: "Pas de redémarrage pour le centre d'administration",
    RESTART_AC_DESCRIPTION: "{0} fournit le centre d'administration. Le centre d'administration ne peut pas être redémarré.",
    RESTART_AC_MESSAGE: "Tous les autres serveurs sélectionnés seront redémarrés.",
    RESTART_AC_CLUSTER_MESSAGE: "Tous les autres clusters sélectionnés seront redémarrés.",

    STOP_AC_TITLE: "Arrêter le centre d'administration",
    STOP_AC_DESCRIPTION: "Le serveur {0} est un contrôleur de collectivité qui exécute le centre d'administration. L'arrêter risque d'impacter les opérations de gestion de collectivité Liberty et rendre le centre d'administration indisponible.",
    STOP_AC_MESSAGE: "Voulez-vous arrêter ce contrôleur ?",
    STOP_STANDALONE_DESCRIPTION: "Le serveur {0} exécute le centre d'administration. En cas d'arrêt du serveur, le centre d'administration sera indisponible.",
    STOP_STANDALONE_MESSAGE: "Voulez-vous arrêter ce serveur ?",

    STOP_CONTROLLER_TITLE: "Arrêter le contrôleur",
    STOP_CONTROLLER_DESCRIPTION: "Le serveur {0} est un contrôleur de collectivité. L'arrêt du contrôleur peut avoir un impact sur les opérations de collectivité Liberty.",
    STOP_CONTROLLER_MESSAGE: "Voulez-vous arrêter ce contrôleur ?",

    STOP_AC_CLUSTER_TITLE: "Arrêter le cluster {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "Le cluster {0} contient un contrôleur de collectivité qui permet d'exécuter le centre d'administration.  En cas d'arrêt, les opérations de gestion de collectivité Liberty pourraient être affectées et le centre d'administration indisponible.",
    STOP_AC_CLUSTER_MESSAGE: "Voulez-vous arrêter ce cluster ?",

    INVALID_URL: "La page n'existe pas.",
    INVALID_APPLICATION: "L'application {0} n'existe plus dans la collectivité.", // application name
    INVALID_SERVER: "Le serveur {0} n'existe plus dans la collectivité.", // server name
    INVALID_CLUSTER: "Le cluster {0} n'existe plus dans la collectivité.", // cluster name
    INVALID_HOST: "L'hôte {0} n'existe plus dans la collectivité.", // host name
    INVALID_RUNTIME: "L'environnement d'exécution {0} n'existe plus dans la collectivité.", // runtime name
    INVALID_INSTANCE: "L'instance d'application {0} n'existe plus dans la collectivité.", // application instance name
    GO_TO_DASHBOARD: "Aller au tableau de bord",
    VIEWED_RESOURCE_REMOVED: "Désolé. La ressource a été retirée et n'est plus disponible.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "La connexion au serveur a été perdue. La page n'affiche plus de changements dynamiques dans l'environnement. Actualisez la page pour restaurer la connexion et les mises à jour dynamiques.",
    ERROR_MESSAGE: "Connexion interrompue",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Arrêter le serveur',

    // Tags
    RELATED_RESOURCES: "Ressources connexes",
    TAGS : "Balises",
    TAG_BUTTON_LABEL : "Balise {0}",  // tag value
    TAGS_LABEL : "Entrez les balises en les séparant d'une virgule, d'un espace, d'un retour à la ligne (touche Entrée) ou d'une tabulation.",
    OWNER : "Propriétaire",
    OWNER_BUTTON_LABEL : "Propriétaire {0}",  // owner value
    CONTACTS : "Contacts",
    CONTACT_BUTTON_LABEL : "Contact {0}",  // contact value
    PORTS : "Ports",
    CONTEXT_ROOT : "Racine du contexte",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Plus",  // alt text for the ... button
    MORE_BUTTON_MENU : "Menu Plus de {0}", // alt text for the menu
    NOTES: "Remarques",
    NOTE_LABEL : "Note {0}",  // note value
    SET_ATTRIBUTES: "Etiquettes et métadonnées",
    SETATTR_RUNTIME_NAME: "{0} sur {1}",  // runtime, host
    SAVE: "Sauvegarder",
    TAGINVALIDCHARS: "Les caractères '/', '<', et '>' ne sont pas valides.",
    ERROR_GET_TAGS_METADATA: "Le produit ne peut pas utiliser les balises et les métadonnées en cours pour la ressource.",
    ERROR_SET_TAGS_METADATA: "Le produit ne peut pas paramétrer les balises et les métadonnées à cause d'une erreur.",
    METADATA_WILLBE_INHERITED: "Les métadonnées sont définies dans l'application et partagées dans toutes les instances du cluster.",
    ERROR_ALT: "Erreur",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Les statistiques en cours ne sont pas disponibles pour ce serveur car il est arrêté. Démarrez le serveur pour commencer le contrôle.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Les statistiques en cours ne sont pas disponibles pour cette application car le serveur associé est arrêté. Démarrez le serveur pour commencer le contrôle de cette application.",
    GRAPH_FEATURES_NOT_CONFIGURED: "Vous n'avez sauvegardé aucun élément pour le moment. Contrôlez cette ressource en sélectionnant l'icône Editer et en ajoutant des métriques.",
    NO_GRAPHS_AVAILABLE: "Aucune métrique disponible à ajouter. Essayez d'installer des fonctionnalités supplémentaires pour que davantage de métriques soient disponibles. ",
    NO_APPS_GRAPHS_AVAILABLE: "Aucune métrique disponible à ajouter. Essayez d'installer des fonctionnalités supplémentaires pour que davantage de métriques soient disponibles. De même, vérifiez que l'application est en cours d'utilisation.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Modifications non sauvegardées",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "Certaines modifications n'ont pas été sauvegardées. Si vous changez de page, ces modifications seront perdues.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Souhaitez-vous sauvegarder vos modifications ?",

    NO_CPU_STATS_AVAILABLE : "Les statistiques d'utilisation de l'unité centrale ne sont pas disponibles pour ce serveur.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "Pour activer cette vue, installez l'outil de configuration du serveur.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Souhaitez-vous sauvegarder les changements dans {0} avant de fermer ?",
    SAVE: "Sauvegarder",
    DONT_SAVE: "Ne pas sauvegarder",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Activer le mode maintenance",
    DISABLE_MAINTENANCE_MODE: "Désactiver le mode maintenance",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Activer le mode maintenance",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Désactiver le mode maintenance",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Activer le mode maintenance sur l'hôte et sur tous ses serveurs ({0} serveurs)",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Activer le mode maintenance sur les hôtes et sur tous leurs serveurs ({0} serveurs)",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Activer le mode maintenance sur le serveur",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Activer le mode maintenance sur les serveurs",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Désactiver le mode maintenance sur l'hôte et sur tous ses serveurs ({0} serveurs)",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Désactiver le mode maintenance sur le serveur",
    BREAK_AFFINITY_LABEL: "Rompre l'affinité avec les sessions actives",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Activer",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Désactivation",
    MAINTENANCE_MODE: "Mode maintenance",
    ENABLING_MAINTENANCE_MODE: "Activation du mode maintenance",
    MAINTENANCE_MODE_ENABLED: "Mode maintenance activé",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "Le mode maintenance n'a pas été activé car des serveurs de remplacement n'ont pas démarré.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Sélectionnez Forcer pour activer le mode maintenance sans démarrer les serveurs de remplacement. Le forçage pourrait provoquer l'arrêt des règles de mise à l'échelle automatique.",
    MAINTENANCE_MODE_FAILED: "Le mode maintenance ne peut pas être activé.",
    MAINTENANCE_MODE_FORCE_LABEL: "Forcer",
    MAINTENANCE_MODE_CANCEL_LABEL: "Annuler",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0} serveurs sont actuellement en mode maintenance.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Activation du mode maintenance sur tous les serveurs.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Activation du mode maintenance sur tous les serveurs.  Affichez la vue des serveurs pour consulter un statut.",

    SERVER_API_DOCMENTATION: "Afficher la définition d'API de serveur",

    // objectView title
    TITLE_FOR_CLUSTER: "Cluster {0}", // cluster name
    TITLE_FOR_HOST: "Hôte {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Collective controller",
    LIBERTY_SERVER : "serveur Liberty",
    NODEJS_SERVER : "Serveur Node.js",
    CONTAINER : "Conteneur",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Serveur Liberty dans un conteneur Docker",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Serveur Node.js dans un conteneur Docker",
    RUNTIME_LIBERTY : "Environnement d'exécution Liberty",
    RUNTIME_NODEJS : "Environnement d'exécution Node.js",
    RUNTIME_DOCKER : "Environnement d'exécution dans un conteneur Docker"

});
