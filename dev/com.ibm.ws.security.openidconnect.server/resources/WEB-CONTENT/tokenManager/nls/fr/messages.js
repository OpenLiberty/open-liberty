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
    "ADD_NEW": "Ajouter nouveau",
    "CANCEL": "Annuler",
    "CLEAR_SEARCH": "Effacer l'entrée de recherche",
    "CLEAR_FILTER": "Effacer le filtre",
    "CLICK_TO_SORT": "Cliquer pour trier la colonne",
    "CLOSE": "Fermer",
    "COPY_TO_CLIPBOARD": "Copier dans le presse-papiers",
    "COPIED_TO_CLIPBOARD": "Copié dans le presse-papiers",
    "DELETE": "Supprimer",
    "DONE": "Terminé",
    "EDIT": "Editer",
    "FALSE": "False",
    "GENERATE": "Générer",
    "LOADING": "En cours de chargement",
    "LOGOUT": "Déconnexion",
    "NEXT_PAGE": "Page suivante",
    "NO_RESULTS_FOUND": "Aucun résultat trouvé",
    "PAGES": "{0} page(s) sur {1}",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Sélectionnez le nombre de pages à afficher",
    "PREVIOUS_PAGE": "Page précédente",
    "PROCESSING": "Traitement en cours",
    "REGENERATE": "Régénérer",
    "REGISTER": "Enregistrer",
    "TABLE_BATCH_BAR": "Barre d'actions de table",
    "TABLE_FIELD_SORT_ASC": "La table est triée en fonction de {0} suivant l'ordre croissant.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "La table est triée en fonction de {0} suivant l'ordre décroissant.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Réessayer...",
    "UPDATE": "Mettre à jour",

    // Common Column Names
    "CLIENT_NAME_COL": "Nom du client",
    "EXPIRES_COL": "Expire le",
    "ISSUED_COL": "Emis le",
    "NAME_COL": "Nom",
    "TYPE_COL": "Type",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Supprimer les jetons",
    "TOKEN_MGR_DESC": "Supprimez des éléments app-password et app-token pour un utilisateur spécifié.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Entrez l'ID utilisateur",
    "TABLE_FILLED_WITH": "La table a été mise à jour pour afficher {0} authentications appartenant à {1}. ",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "Supprimez les éléments app-password et app-token sélectionnés.",
    "DELETE_ARIA": "Supprimer l'élément {0} nommé {1}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Supprimer cet élément app-password",
    "DELETE_TOKEN": "Supprimer cet élément app-token",
    "DELETE_FOR_USERID": "{0} pour {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "Cette action retire l'élément app-password actuellement affecté.",
    "DELETE_WARNING_TOKEN": "Cette action retire l'élément app-token actuellement affecté.",
    "DELETE_MANY": "Supprimer les éléments app-password/app-token",
    "DELETE_MANY_FOR": "Affecté à {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "Cette action supprime l'élément app-password/app-token sélectionné. ",
    "DELETE_MANY_MESSAGE": "Cette action supprime les {0} éléments app-password/app-token sélectionnés.",  // 0 - number
    "DELETE_ALL_MESSAGE": "Cette action supprime tous les éléments app-password/app-token appartenant à {0}.", // 0 - user id
    "DELETE_NONE": "Sélectionner pour suppression",
    "DELETE_NONE_MESSAGE": "Sélectionnez les cases à cocher correspondantes pour indiquer les éléments app-password ou app-token à supprimer.",
    "SINGLE_ITEM_SELECTED": "1 élément sélectionné",
    "ITEMS_SELECTED": "{0} éléments sélectionnés",            // 0 - number
    "SELECT_ALL_AUTHS": "Sélectionnez tous les éléments app-password et app-token de cet utilisateur. ",
    "SELECT_SPECIFIC": "Sélectionnez l'élément {0} nommé {1} pour le supprimer. ",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Vous cherchez quelque chose ? Entrez un ID utilisateur pour afficher ses éléments app-password et app-token.",
    "GENERIC_FETCH_FAIL": "Erreur lors de l'extraction des éléments {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Impossible d'obtenir la liste des éléments {0} appartenant à {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Erreur lors de la suppression des éléments {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Une erreur est survenue lors de la suppression de l'élément {0} nommé {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "Une erreur est survenue lors de la suppression des éléments {0} pour {1}.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Erreur lors de la suppression",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "Une erreur est survenue lors de la suppression de l'élément app-password ou app-token suivant :",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "Une erreur est survenue lors de la suppression des {0} éléments app-password et app-token suivants :",  // 0 - number
    "IDENTIFY_AUTH": "{0} pour {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Erreur lors de l'extraction des authentifications",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Impossible d'obtenir la liste des éléments app-password et app-token appartenant à {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Client non configuré",
    "GENERIC_NOT_CONFIGURED_MSG": "Les attributs de client appPasswordAllowed et appTokenAllowed ne sont pas configurés.  Aucune donnée ne peut être extraite."
};
