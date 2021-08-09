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
    "CLEAR": "Clear search input",
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
    "EXPIRES_COL": "Expires On",
    "ISSUED_COL": "Issued On",
    "NAME_COL": "Name",
    "TYPE_COL": "Type",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Manage Personal Tokens",
    "ACCT_MGR_DESC": "Create, delete and regenerate app-passwords and app-tokens.",
    "ADD_NEW_AUTHENTICATION": "Add new app-password or app-token.",
    "NAME_IDENTIFIER": "Name: {0}",
    "ADD_NEW_TITLE": "Register New Authentication",
    "NOT_GENERATED_PLACEHOLDER": "Not generated",
    "AUTHENTICAION_GENERATED": "Generated authentication",
    "GENERATED_APP_PASSWORD": "Generated app-password",
    "GENERATED_APP_TOKEN": "Generated app-token",
    "COPY_APP_PASSWORD": "Copy app-password to clipboard",
    "COPY_APP_TOKEN": "Copy app-token to clipboard",
    "REGENERATE_APP_PASSWORD": "Regenerate App-Password",
    "REGENERATE_PW_WARNING": "This action will overwrite the current app-password.",
    "REGENERATE_PW_PLACEHOLDER": "Password previously generated on {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "Regenerate App-Token",
    "REGENERATE_TOKEN_WARNING": "This action will overwrite the current app-token.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Token previously generated on {0}",        // 0 - date
    "DELETE_PW": "Delete this app-password",
    "DELETE_TOKEN": "Delete this app-token",
    "DELETE_WARNING_PW": "This action will remove the currently assigned app-password.",
    "DELETE_WARNING_TOKEN": "This action will remove the currently assigned app-token.",
    "REGENERATE_ARIA": "Regenerate {0} for {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Delete the {0} named {1}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Error Generating {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "An error occurred generating a new {0} with the name {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "The name is already associated with an {0}, or it is too long.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Error Deleting {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "An error occurred deleting the {0} named {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Error Regenerating {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "An error occurred regenerating the {0} named {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "An error occurred regenerating the {0} named {1}. The {0} was deleted but could not be recreated.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Error Retrieving Authentications",
    "GENERIC_FETCH_FAIL_MSG": "Unable to get the current list of app-passwords or app-tokens.",
    "GENERIC_NOT_CONFIGURED": "Client Not Configured",
    "GENERIC_NOT_CONFIGURED_MSG": "The appPasswordAllowed and appTokenAllowed client attributes are not configured.  No data can be retrieved.",
    "APP_PASSWORD_NOT_CONFIGURED": "appPasswordAllowed client attribute is not configured.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "appTokenAllowed client attribute is not configured."         // 'appTokenAllowed' is a config option.  Do not translate.
};