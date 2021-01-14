/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
 define({
    LIBERTY_HEADER_TITLE: "Centre d'administration Liberty",
    LIBERTY_HEADER_PROFILE: "Préférences",
    LIBERTY_HEADER_LOGOUT: "Se déconnecter",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Se déconnecter de {0}",
    TOOLBOX_BANNER_LABEL: "Bannière {0}",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Boîte à outils",
    TOOLBOX_TITLE_LOADING_TOOL: "Chargement de l'outil...",
    TOOLBOX_TITLE_EDIT: "Editer la boîte à outils",
    TOOLBOX_EDIT: "Editer",
    TOOLBOX_DONE: "Terminé",
    TOOLBOX_SEARCH: "Filtre",
    TOOLBOX_CLEAR_SEARCH: "Effacer les critères de filtrage",
    TOOLBOX_END_SEARCH: "Arrêter le filtre",
    TOOLBOX_ADD_TOOL: "Ajouter un outil",
    TOOLBOX_ADD_CATALOG_TOOL: "Ajouter un outil",
    TOOLBOX_ADD_BOOKMARK: "Ajouter un signet",
    TOOLBOX_REMOVE_TITLE: "Retirer l'outil {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Retirer un outil",
    TOOLBOX_REMOVE_MESSAGE: "Voulez-vous vraiment retirer {0} ?",
    TOOLBOX_BUTTON_REMOVE: "Retirer",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Accéder à la boîte à outils",
    TOOLBOX_BUTTON_CANCEL: "Annuler",
    TOOLBOX_BUTTON_BGTASK: "Tâches en arrière-plan",
    TOOLBOX_BUTTON_BACK: "Arrière",
    TOOLBOX_BUTTON_USER: "Utilisateur",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "Une erreur s'est produite lors de l'ajout de l'outil {0} : {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "Une erreur s'est produite lors du retrait de l'outil {0} : {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "Une erreur s'est produite lors de l'extraction des outils dans la boîte à outils : {0}",
    TOOLCATALOG_TITLE: "Catalogue d'outils",
    TOOLCATALOG_ADDTOOL_TITLE: "Ajouter un outil",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Voulez-vous vraiment ajouter l'outil {0} à votre boîte à outils ?",
    TOOLCATALOG_BUTTON_ADD: "Ajouter",
    TOOL_FRAME_TITLE: "Axe outil",
    TOOL_DELETE_TITLE: "Supprimer {0}",
    TOOL_ADD_TITLE: "Ajouter {0}",
    TOOL_ADDED_TITLE: "{0} déjà ajouté",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Outil introuvable",
    TOOL_LAUNCH_ERROR_MESSAGE: "L'outil demandé n'a pu être lancé car il ne se trouve pas dans le catalogue.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Erreur",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Avertissement",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Informations",
    LIBERTY_UI_CATALOG_GET_ERROR: "Une erreur s'est produite lors de l'obtention du catalogue : {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "Une erreur s'est produite lors de l'obtention de l'outil {0} du catalogue : {1}",
    PREFERENCES_TITLE: "Préférences",
    PREFERENCES_SECTION_TITLE: "Préférences",
    PREFERENCES_ENABLE_BIDI: "Activer le support bidirectionnel",
    PREFERENCES_BIDI_TEXTDIR: "Orientation du texte",
    PREFERENCES_BIDI_TEXTDIR_LTR: "De gauche à droite",
    PREFERENCES_BIDI_TEXTDIR_RTL: "De droite à gauche",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Contextuelle",
    PREFERENCES_SET_ERROR_MESSAGE: "Une erreur s'est produite lors de la définition des préférences utilisateur dans la boîte à outils : {0}",
    BGTASKS_PAGE_LABEL: "Tâches en arrière-plan",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Déployer l'installation {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Déployer l'installation {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "En cours d'exécution",
    BGTASKS_STATUS_FAILED: "Echec",
    BGTASKS_STATUS_SUCCEEDED: "Terminé", 
    BGTASKS_STATUS_WARNING: "Réussite partielle",
    BGTASKS_STATUS_PENDING: "En attente",
    BGTASKS_INFO_DIALOG_TITLE: "Détails",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Sortie standard :",
    BGTASKS_INFO_DIALOG_STDERR: "Erreur standard :",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Exception :",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Résultat :",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Nom du serveur :",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Annuaire d'utilisateurs :",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Tâches en arrière-plan actives",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Aucune",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "Aucune tâche en arrière-plan active",
    BGTASKS_DISPLAY_BUTTON: "Détails de tâche et historique",
    BGTASKS_EXPAND: "Développer la section",
    BGTASKS_COLLAPSE: "Réduire la section",
    PROFILE_MENU_HELP_TITLE: "Aide",
    DETAILS_DESCRIPTION: "Description",
    DETAILS_OVERVIEW: "Présentation",
    DETAILS_OTHERVERSIONS: "Autres versions",
    DETAILS_VERSION: "Version : {0}",
    DETAILS_UPDATED: "Mise à jour : {0}",
    DETAILS_NOTOPTIMIZED: "Non optimisé pour l'unité en cours.",
    DETAILS_ADDBUTTON: "Ajouter à Ma boîte à outils",
    DETAILS_OPEN: "Ouvrir",
    DETAILS_CATEGORY: "Catégorie {0}",
    DETAILS_ADDCONFIRM: "L'outil {0} a été ajouté à la boîte à outils.",
    CONFIRM_DIALOG_HELP: "Aide",
    YES_BUTTON_LABEL: "{0} - oui",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} - non",  // insert is dialog title

    YES: "Oui",
    NO: "Non",

    TOOL_OIDC_ACCESS_DENIED: "L'utilisateur ne possède pas le rôle permettant de traiter cette demande.",
    TOOL_OIDC_GENERIC_ERROR: "Une erreur est survenue. Examinez-la dans le journal pour plus d'informations.",
    TOOL_DISABLE: "L'utilisateur n'est pas autorisé à utiliser cet outil. Seuls les utilisateurs disposant du rôle d'administrateur sont autorisés à l'utiliser. " 
});
