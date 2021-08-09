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
    "TABLE_BATCH_BAR": "Table action bar",
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

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Delete Tokens",
    "TOKEN_MGR_DESC": "Delete app-passwords and app-tokens for a specified user.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Enter user ID",
    "TABLE_FILLED_WITH": "Table was updated to show {0} authentications belonging to {1}.",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "Delete selected app-passwords and app-tokens.",
    "DELETE_ARIA": "Delete the {0} named {1}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Delete this app-password",
    "DELETE_TOKEN": "Delete this app-token",
    "DELETE_FOR_USERID": "{0} for {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "This action will remove the currently assigned app-password.",
    "DELETE_WARNING_TOKEN": "This action will remove the currently assigned app-token.",
    "DELETE_MANY": "Delete App-Passwords/App-Tokens",
    "DELETE_MANY_FOR": "Assigned to {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "This action will delete the selected app-password/app-token.",
    "DELETE_MANY_MESSAGE": "This action will delete the {0} selected app-passwords/app-tokens.",  // 0 - number
    "DELETE_ALL_MESSAGE": "This action will delete all the app-passwords/app-tokens belonging to {0}.", // 0 - user id
    "DELETE_NONE": "Select for Deletion",
    "DELETE_NONE_MESSAGE": "Select a checkbox to indicate which app-passwords or app-tokens should be deleted.",
    "SINGLE_ITEM_SELECTED": "1 item selected",
    "ITEMS_SELECTED": "{0} items selected",            // 0 - number
    "SELECT_ALL_AUTHS": "Select all app-passwords and app-tokens for this user.",
    "SELECT_SPECIFIC": "Select the {0} named {1} for deletion.",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Looking for something? Enter a user ID to view their app-passwords and app-tokens.",
    "GENERIC_FETCH_FAIL": "Error Retrieving {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Unable to get the list of {0} belonging to {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Error Deleting {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "An error occurred deleting the {0} named {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "An error occurred deleting {0} for {1}.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Error Deleting",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "An error occurred deleting the following app-password or app-token:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "An error occurred deleting the following {0} app-passwords and app-tokens:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Error Retrieving Authentications",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Unable to get the list of app-passwords and app-tokens belonging to {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Client Not Configured",
    "GENERIC_NOT_CONFIGURED_MSG": "The appPasswordAllowed and appTokenAllowed client attributes are not configured.  No data can be retrieved."
};