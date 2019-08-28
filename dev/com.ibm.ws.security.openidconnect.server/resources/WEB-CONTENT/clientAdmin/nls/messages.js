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
    "ADD_NEW": "Add New",
    "CANCEL": "Cancel",
    "CLEAR_SEARCH": "Clear search input",
    "CLEAR_FILTER": "Clear filter",
    "CLICK_TO_SORT": "Click to sort column",
    "CLOSE": "Close",
    "COPY_TO_CLIPBOARD": "Copy to clipboard",
    "COPIED_TO_CLIPBOARD": "Copied to clipboard",
    "DELETE": "Delete",
    "DONE": "Done",
    "EDIT": "Edit",
    "FALSE": "False",
    "GENERATE": "Generate",
    "LOADING": "Loading",
    "LOGOUT": "Logout",
    "NEXT_PAGE": "Next page",
    "NO_RESULTS_FOUND": "No results found",
    "PAGES": "{0} of {1} pages",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Select page number to view",
    "PREVIOUS_PAGE": "Previous page",
    "PROCESSING": "Processing",
    "REGENERATE": "Regenerate",
    "REGISTER": "Register",
    "TABLE_FIELD_SORT_ASC": "Table is sorted by {0} in ascending order.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Table is sorted by {0} in descending order.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Try again...",
    "UPDATE": "Update",

    // Common Column Names
    "CLIENT_NAME_COL": "Client Name",
    "EXPIRES_COL": "Expires On",
    "ISSUED_COL": "Issued On",
    "NAME_COL": "Name",
    "TYPE_COL": "Type",

    // Client Admin
    "CLIENT_ADMIN_TITLE": "Manage OAuth Clients",
    "CLIENT_ADMIN_DESC": "Use this tool to add and edit clients and regenerate client secrets.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Filter on OAuth client name",
    "ADD_NEW_CLIENT": "Add new OAuth client.",
    "CLIENT_NAME": "Client name",
    "CLIENT_ID": "Client ID",
    "EDIT_ARIA": "Edit the {0} OAuth client",      // {0} - name
    "DELETE_ARIA": "Delete the {0} OAuth client",  // {0} - name
    "CLIENT_SECRET": "Client secret",
    "GRANT_TYPES": "Grant types",
    "SCOPE": "Scope",
    "PREAUTHORIZED_SCOPE": "Pre-authorized scope (optional)",
    "REDIRECT_URLS": "Redirect URLs (optional)",
    "CLIENT_SECRET_CHECKBOX": "Regenerate client secret",
    "NONE_SELECTED": "None selected",
    "MODAL_EDIT_TITLE": "Edit OAuth Client",
    "MODAL_REGISTER_TITLE": "Register New OAuth Client",
    "MODAL_SECRET_REGISTER_TITLE": "OAuth Registration Saved",
    "MODAL_SECRET_UPDATED_TITLE": "OAuth Registration Updated",
    "MODAL_DELETE_CLIENT_TITLE": "Delete this OAuth client",
    "RESET_GRANT_TYPE": "Clear all selected grant types.",
    "SELECT_ONE_GRANT_TYPE": "Select at least one grant type",
    "SPACE_HELPER_TEXT": "Space separated values",
    "REDIRECT_URL_HELPER_TEXT": "Space separated absolute redirect URLs",
    "DELETE_OAUTH_CLIENT_DESC": "This operation deletes the registered client from the client registration service.",
    "REGISTRATION_SAVED": "A Client ID and Client Secret have been generated and assigned.",
    "REGISTRATION_UPDATED": "A new Client Secret has been generated and assigned for this client.",
    "COPY_CLIENT_ID": "Copy client ID to clipboard",
    "COPY_CLIENT_SECRET": "Copy client secret to clipboard",
    "REGISTRATION_UPDATED_NOSECRET": "The {0} OAuth client is updated.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "At least one grant type must be selected.",
    "ERR_REDIRECT_URIS": "Values must be absolute URIs.",
    "GENERIC_REGISTER_FAIL": "Error Registering OAuth Client",
    "GENERIC_UPDATE_FAIL": "Error Updating OAuth Client",
    "GENERIC_DELETE_FAIL": "Error Deleting OAuth Client",
    "GENERIC_MISSING_CLIENT": "Error Retrieving OAuth Client",
    "GENERIC_REGISTER_FAIL_MSG": "An error occurred registering the {0} OAuth client.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "An error occurred updating the {0} OAuth client.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "An error occurred deleting the {0} OAuth client.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "The OAuth client {0} with ID {1} was not found.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "An error occurred retrieving information on the {0} OAuth client.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Error Retrieving OAuth Clients",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "An error occurred retrieving the list of OAuth clients.",

    "RESET_SELECTION": "Clear all selected {0}",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "Number of {0} selected",     // {0} - field name
    "OPEN_LIST": "Open {0} list.",                   // {0} - field name
    "CLOSE_LIST": "Close {0} list.",                 // {0} - field name
    "ENTER_PLACEHOLDER": "Enter value",
    "ADD_VALUE": "Add element",
    "REMOVE_VALUE": "Remove element",
    "REGENERATE_CLIENT_SECRET": "'*' preserves the existing value. A blank value generates a new client_secret. A non-blank parameter value overrides the existing value with the newly specified value.",
    "ALL_OPTIONAL": "All fields are optional"
};