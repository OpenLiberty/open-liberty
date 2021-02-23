/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
      ACCOUNTING_STRING : "Identifiant comptable",
      SEARCH_RESOURCE_TYPE_ALL : "Tout",
      SEARCH : "Rechercher",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Entrez les critères de recherche en sélectionnant le bouton permettant d'ajouter des critères de recherche et en spécifiant une valeur",
      SUBMITTED : "Soumis",
      JMS_QUEUED : "En file d'attente JMS",
      JMS_CONSUMED : "Consommé par JMS",
      JOB_PARAMETER : "Paramètre de travail",
      DISPATCHED : "Distribué",
      FAILED : "En échec",
      STOPPED : "Arrêté",
      COMPLETED : "Terminé",
      ABANDONED : "Abandonné",
      STARTED : "Démarré",
      STARTING : "Démarrage",
      STOPPING : "En cours d'arrêt",
      REFRESH : "Actualiser",
      INSTANCE_STATE : "Etat d'instance",
      APPLICATION_NAME : "Nom d'application",
      APPLICATION: "Application",
      INSTANCE_ID : "ID instance",
      LAST_UPDATE : "Dernière mise à jour",
      LAST_UPDATE_RANGE : "Dernière plage de mise à jour",
      LAST_UPDATED_TIME : "Heure de la dernière mise à jour",
      DASHBOARD_VIEW : "Vue Tableau de bord",
      HOMEPAGE : "Page d'accueil",
      JOBLOGS : "Journaux de travail",
      QUEUED : "En file d'attente",
      ENDED : "Interrompu",
      ERROR : "Erreur",
      CLOSE : "Fermer",
      WARNING : "Avertissement",
      GO_TO_DASHBOARD: "Accéder au tableau de bord",
      DASHBOARD : "Tableau de bord",
      BATCH_JOB_NAME: "Nom de travail par lots",
      SUBMITTER: "Emetteur",
      BATCH_STATUS: "Statut du lot",
      EXECUTION_ID: "ID exécution de travail",
      EXIT_STATUS: "Statut de sortie",
      CREATE_TIME: "Heure de création",
      START_TIME: "Heure de début",
      END_TIME: "Heure de fin",
      SERVER: "Serveur",
      SERVER_NAME: "Nom du serveur",
      SERVER_USER_DIRECTORY: "Répertoire utilisateur",
      SERVERS_USER_DIRECTORY: "Répertoire utilisateur du serveur",
      HOST: "Hôte",
      NAME: "Nom",
      JOB_PARAMETERS: "Paramètres du travail",
      JES_JOB_NAME: "Nom du travail JES",
      JES_JOB_ID: "ID travail JES",
      ACTIONS: "Actions",
      VIEW_LOG_FILE: "Afficher le fichier journal",
      STEP_NAME: "Nom d'étape",
      ID: "ID",
      PARTITION_ID: "Partition {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Afficher les détails sur l'exécution de travail {0}",    // Job Execution ID number
      PARENT_DETAILS: "Informations détaillées sur le parent",
      TIMES: "Heures",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Statut",
      SEARCH_ON: "Sélectionner de filtrer sur {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Entrez des critères de recherche.",
      BREADCRUMB_JOB_INSTANCE : "Instance de travail {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Exécution de travail {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Journal de travail {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "Les critères de recherche ne sont pas valides.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "Les critères de recherche ne peuvent pas comporter plusieurs paramètres {0}.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Tableau des instances de travail",
      EXECUTIONS_TABLE_IDENTIFIER: "Tableau des exécutions de travail",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Tableau des détails des étapes",
      LOADING_VIEW : "Informations en cours de chargement sur la page",
      LOADING_VIEW_TITLE : "Chargement de la vue",
      LOADING_GRID : "En attente du renvoi des résultats de la recherche par le serveur",
      PAGENUMBER : "Numéro de page",
      SELECT_QUERY_SIZE: "Sélectionnez une taille de requête",
      LINK_EXPLORE_HOST: "Sélectionnez l'affichage des détails sur l'hôte {0} dans l'outil Explorer.",      // Host name
      LINK_EXPLORE_SERVER: "Sélectionnez l'affichage des détails sur le serveur {0} dans l'outil Explorer.",  // Server name

      //ACTIONS
      RESTART: "Redémarrer",
      STOP: "Arrêter",
      PURGE: "Purger",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Actions pour l'instance de travail {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Instance de travail : menu d'actions",

      RESTART_INSTANCE_MESSAGE: "Voulez-vous redémarrer l'exécution de travail la plus récente associée à l'instance de travail {0} ?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Voulez-vous arrêter l'exécution de travail la plus récente associée à l'instance de travail {0} ?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Voulez-vous purger toutes les entrées de base de données et tous les journaux de travail associés à l'instance de travail {0} ?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Purger uniquement le magasin de travaux",

      RESTART_INST_ERROR_MESSAGE: "La demande de redémarrage a échoué.",
      STOP_INST_ERROR_MESSAGE: "La demande d'arrêt a échoué.",
      PURGE_INST_ERROR_MESSAGE: "La demande de purge a échoué.",
      ACTION_REQUEST_ERROR_MESSAGE: "La demande d'action a échoué avec le code de statut : {0}.  URL : {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Réutiliser les paramètres de la précédente exécution",
      JOB_PARAMETERS_EMPTY: "Lorsque '{0}' n'est pas coché, utilisez cette zone pour entrer les paramètres du travail.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Nom de paramètre",
      JOB_PARAMETER_VALUE: "Valeur de paramètre",
      PARM_NAME_COLUMN_HEADER: "Paramètre",
      PARM_VALUE_COLUMN_HEADER: "Valeur",
      PARM_ADD_ICON_TITLE: "Ajouter un paramètre",
      PARM_REMOVE_ICON_TITLE: "Retirer le paramètre",
      PARMS_ENTRY_ERROR: "Un nom de paramètre est requis.",
      JOB_PARAMETER_CREATE: "Sélectionnez {0} pour ajouter des paramètres à la prochaine exécution de cette instance de travail.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Bouton Ajouter un paramètre dans l'en-tête de tableau.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Contenu du journal de travail",
      FILE_DOWNLOAD : "Téléchargement de fichier",
      DOWNLOAD_DIALOG_DESCRIPTION : "Voulez-vous télécharger le fichier journal ?",
      INCLUDE_ALL_LOGS : "Inclure tous les fichiers journaux pour l'exécution de travail",
      LOGS_NAVIGATION_BAR : "Barre de navigation des journaux de travail",
      DOWNLOAD : "Télécharger",
      LOG_TOP : "Début des journaux",
      LOG_END : "Fin des journaux",
      PREVIOUS_PAGE : "Page précédente",
      NEXT_PAGE : "Page suivante",
      DOWNLOAD_ARIA : "Télécharger le fichier",

      //Error messages for popups
      REST_CALL_FAILED : "L'appel pour extraire des données a échoué.",
      NO_JOB_EXECUTION_URL : "Soit aucun numéro d'exécution de travail n'a été indiqué dans l'URL, soit cette instance ne comporte aucun fichier journal d'exécution de travail à afficher.",
      NO_VIEW : "Erreur d'URL : il n'existe aucune vue de ce type.",
      WRONG_TOOL_ID : "La chaîne de recherche de l'URL ne commence pas par l'ID d'outil {0}, mais commence à la place par {1}.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "Erreur d'URL : il n'existe aucun journal.",
      NOT_A_NUMBER : "Erreur d'URL : {0} doit être un nombre.",                                                // {0} is a field name
      PARAMETER_REPETITION : "Erreur d'URL : {0} ne peut figurer qu'une seule fois dans les paramètres.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "Erreur d'URL : le paramètre page n'est pas compris dans la plage admise.",
      INVALID_PARAMETER : "Erreur d'URL : {0} n'est pas un paramètres valide.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "Erreur d'URL : l'URL peut spécifier une exécution de travail ou une instance de travail, mais pas les deux.",
      MISSING_EXECUTION_ID_PARAM : "Un paramètre d'ID d'exécution requis est manquant.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Une configuration de base de données Java Batch persistante est requise pour utiliser l'outil Java Batch.",
      IGNORED_SEARCH_CRITERIA : "Les critères de filtrage suivants ont été ignorés dans les résultats : {0}",

      GRIDX_SUMMARY_TEXT : "Affichage des ${0} instances de travail les plus récentes"

});

