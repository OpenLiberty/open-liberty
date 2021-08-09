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
    "CLEAR": "Effacer l'entrée de recherche",
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
    "TABLE_FIELD_SORT_ASC": "La table est triée en fonction de {0} suivant l'ordre croissant.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "La table est triée en fonction de {0} suivant l'ordre décroissant.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Réessayer...",
    "UPDATE": "Mettre à jour",

    // Common Column Names
    "EXPIRES_COL": "Expire le",
    "ISSUED_COL": "Emis le",
    "NAME_COL": "Nom",
    "TYPE_COL": "Type",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Gérer les jetons personnels",
    "ACCT_MGR_DESC": "Créez, supprimez et régénérez des éléments app-password et app-token.",
    "ADD_NEW_AUTHENTICATION": "Ajoutez un nouvel élément app-password ou app-token. ",
    "NAME_IDENTIFIER": "Nom : {0}",
    "ADD_NEW_TITLE": "Enregistrer une nouvelle authentification",
    "NOT_GENERATED_PLACEHOLDER": "Non généré",
    "AUTHENTICAION_GENERATED": "Authentification générée",
    "GENERATED_APP_PASSWORD": "Elément app-password généré",
    "GENERATED_APP_TOKEN": "Elément app-token généré",
    "COPY_APP_PASSWORD": "Copier l'élément app-password dans le presse-papiers",
    "COPY_APP_TOKEN": "Copier l'élément app-token dans le presse-papiers",
    "REGENERATE_APP_PASSWORD": "Régénérer l'élément app-password",
    "REGENERATE_PW_WARNING": "Cette action remplace l'élément app-password en cours.",
    "REGENERATE_PW_PLACEHOLDER": "Mot de passe généré précédemment le {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "Régénérer l'élément app-token",
    "REGENERATE_TOKEN_WARNING": "Cette action remplace l'élément app-token en cours.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Jeton généré précédemment le {0}",        // 0 - date
    "DELETE_PW": "Supprimer cet élément app-password",
    "DELETE_TOKEN": "Supprimer cet élément app-token",
    "DELETE_WARNING_PW": "Cette action retire l'élément app-password actuellement affecté.",
    "DELETE_WARNING_TOKEN": "Cette action retire l'élément app-token actuellement affecté.",
    "REGENERATE_ARIA": "Régénérer l'élément {0} pour {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Supprimer l'élément {0} nommé {1}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Erreur lors de la génération de l'élément {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "Une erreur est survenue lors de la génération d'un nouvel élément {0} avec le nom {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "Le nom est déjà associé à un élément {0} ou est trop long.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Erreur lors de la suppression de l'élément {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Une erreur est survenue lors de la suppression de l'élément {0} nommé {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Erreur lors de la régénération de l'élément {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "Une erreur est survenue lors de la régénération de l'élément {0} nommé {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "Une erreur est survenue lors de la régénération de l'élément {0} nommé {1}. L'élément {0} a été supprimé mais n'a pas pu être recréé.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Erreur lors de l'extraction des authentifications",
    "GENERIC_FETCH_FAIL_MSG": "Impossible d'obtenir la liste en cours des éléments app-password ou app-token.",
    "GENERIC_NOT_CONFIGURED": "Client non configuré",
    "GENERIC_NOT_CONFIGURED_MSG": "Les attributs de client appPasswordAllowed et appTokenAllowed ne sont pas configurés.  Aucune donnée ne peut être extraite.",
    "APP_PASSWORD_NOT_CONFIGURED": "L'attribut de client appPasswordAllowed n'est pas configuré.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "L'attribut de client appTokenAllowed n'est pas configuré."         // 'appTokenAllowed' is a config option.  Do not translate.
};
