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
    "TRY_AGAIN": "Réessayez...",
    "UPDATE": "Mettre à jour",

    // Common Column Names
    "CLIENT_NAME_COL": "Nom du client",
    "EXPIRES_COL": "Expire le",
    "ISSUED_COL": "Emis le",
    "NAME_COL": "Nom",
    "TYPE_COL": "Type",

    // Client Admin
    "CLIENT_ADMIN_TITLE": "Gérer les clients OAuth",
    "CLIENT_ADMIN_DESC": "Utilisez cet outil pour ajouter et éditer des clients, et régénérer les secrets client.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Filtrer en fonction d'un nom de client OAuth",
    "ADD_NEW_CLIENT": "Ajouter un nouveau client OAuth",
    "CLIENT_NAME": "Nom du client",
    "CLIENT_ID": "ID client",
    "EDIT_ARIA": "Editer le client OAuth {0}",      // {0} - name
    "DELETE_ARIA": "Supprimer le client OAuth {0}",  // {0} - name
    "CLIENT_SECRET": "Secret client",
    "GRANT_TYPES": "Types d'octroi",
    "SCOPE": "Portée",
    "PREAUTHORIZED_SCOPE": "Portée pré-autorisée (facultatif)",
    "REDIRECT_URLS": "URL de redirection (facultatif)",
    "ADDITIONAL_PROPS": "Propriétés supplémentaires",
    "ADDITIONAL_PROPS_OPTIONAL": "Propriétés supplémentaires (facultatif)",
    "CLIENT_SECRET_CHECKBOX": "Régénérer le secret client",
    "PROPERTY_PLACEHOLDER": "Propriété",
    "VALUE_PLACEHOLDER": "Valeur",
    "GRANT_TYPES_SELECTED": "Nombre de types d'octroi sélectionnés",
    "GRANT_TYPES_NONE_SELECTED": "Aucun élément sélectionné",
    "MODAL_EDIT_TITLE": "Edition du client OAuth",
    "MODAL_REGISTER_TITLE": "Enregistrement d'un nouveau client OAuth",
    "MODAL_SECRET_REGISTER_TITLE": "Enregistrement OAuth sauvegardé",
    "MODAL_SECRET_UPDATED_TITLE": "Enregistrement OAuth mis à jour",
    "MODAL_DELETE_CLIENT_TITLE": "Suppression de ce client OAuth",
    "VALUE_COL": "Valeur",
    "ADD": "Ajouter",
    "DELETE_PROP": "Supprimer la propriété personnalisée",
    "RESET_GRANT_TYPE": "Effacer tous les types d'octroi sélectionnés",
    "SELECT_ONE_GRANT_TYPE": "Sélectionner au moins un type d'octroi",
    "OPEN_GRANT_TYPE": "Ouvrir la liste des types d'octroi",
    "CLOSE_GRANT_TYPE": "Fermer la liste des types d'octroi",
    "SPACE_HELPER_TEXT": "Valeurs séparées par un espace",
    "REDIRECT_URL_HELPER_TEXT": "URL de redirection absolues séparées par un espace",
    "DELETE_OAUTH_CLIENT_DESC": "Cette opération supprime le client enregistré du service d'enregistrement des clients.",
    "REGISTRATION_SAVED": "Un ID client et un secret client ont été générés et affectés.",
    "REGISTRATION_UPDATED": "Un nouveau secret client a été généré et affecté pour ce client.",
    "REGISTRATION_UPDATED_NOSECRET": "Le client OAuth {0} a été mis à jour.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "Un type d'octroi au moins doit être sélectionné.",
    "ERR_REDIRECT_URIS": "Les valeurs doivent être des URI absolus.",
    "GENERIC_REGISTER_FAIL": "Erreur lors de l'enregistrement du client OAuth",
    "GENERIC_UPDATE_FAIL": "Erreur lors de la mise à jour du client OAuth",
    "GENERIC_DELETE_FAIL": "Erreur lors de la suppression du client OAuth",
    "GENERIC_MISSING_CLIENT": "Erreur lors de l'extraction du client OAuth",
    "GENERIC_REGISTER_FAIL_MSG": "Une erreur est survenue lors de l'enregistrement du client OAuth {0}.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "Une erreur est survenue lors de la mise à jour du client OAuth {0}.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "Une erreur est survenue lors de la suppression du client OAuth {0}.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "Le client OAuth {0} dont l'ID est {1} est introuvable.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "Une erreur est survenue lors de l'extraction des informations sur le client OAuth {0}.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Erreur lors de l'extraction des clients OAuth",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Une erreur est survenue lors de l'extraction de la liste des clients OAuth."
};
