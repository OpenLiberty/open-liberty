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
  root: {
    LIBERTY_HEADER_TITLE: "Liberty Admin Center",
    LIBERTY_HEADER_PROFILE: "Preferences",
    LIBERTY_HEADER_LOGOUT: "Log out",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Log out {0}",
    TOOLBOX_BANNER_LABEL: "{0} banner",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Toolbox",
    TOOLBOX_TITLE_LOADING_TOOL: "Loading tool...",
    TOOLBOX_TITLE_EDIT: "Edit Toolbox",
    TOOLBOX_EDIT: "Edit",
    TOOLBOX_DONE: "Done",
    TOOLBOX_SEARCH: "Filter",
    TOOLBOX_CLEAR_SEARCH: "Clear filter criteria",
    TOOLBOX_END_SEARCH: "End filter",
    TOOLBOX_ADD_TOOL: "Add tool",
    TOOLBOX_ADD_CATALOG_TOOL: "Add Tool",
    TOOLBOX_ADD_BOOKMARK: "Add Bookmark",
    TOOLBOX_REMOVE_TITLE: "Remove Tool {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Remove Tool",
    TOOLBOX_REMOVE_MESSAGE: "Are you sure you want to remove {0}?",
    TOOLBOX_BUTTON_REMOVE: "Remove",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Go To Toolbox",
    TOOLBOX_BUTTON_CANCEL: "Cancel",
    TOOLBOX_BUTTON_BGTASK: "Background Tasks",
    TOOLBOX_BUTTON_BACK: "Back",
    TOOLBOX_BUTTON_USER: "User",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "An error occurred adding tool {0}: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "An error occurred removing tool {0}: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "An error occurred retrieving the tools in the toolbox: {0}",
    TOOLCATALOG_TITLE: "Tool Catalog",
    TOOLCATALOG_ADDTOOL_TITLE: "Add Tool",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Are you sure you want to add tool {0} to your toolbox?",
    TOOLCATALOG_BUTTON_ADD: "Add",
    TOOL_FRAME_TITLE: "Tool frame",
    TOOL_DELETE_TITLE: "Delete {0}",
    TOOL_ADD_TITLE: "Add {0}",
    TOOL_ADDED_TITLE: "{0} already added",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Tool Not Found",
    TOOL_LAUNCH_ERROR_MESSAGE: "The requested tool did not launch because the tool is not in the catalog.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Error",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Warning",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Information",
    LIBERTY_UI_CATALOG_GET_ERROR: "An error occurred getting the catalog: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "An error occurred getting tool {0} from the catalog: {1}",
    PREFERENCES_TITLE: "Preferences",
    PREFERENCES_SECTION_TITLE: "Preferences",
    PREFERENCES_ENABLE_BIDI: "Enable bidirectional support",
    PREFERENCES_BIDI_TEXTDIR: "Text direction",
    PREFERENCES_BIDI_TEXTDIR_LTR: "Left to right",
    PREFERENCES_BIDI_TEXTDIR_RTL: "Right to left",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Contextual",
    PREFERENCES_SET_ERROR_MESSAGE: "An error occurred setting the user preferences in the toolbox: {0}",
    BGTASKS_PAGE_LABEL: "Background Tasks",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Deploy Installation {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Deploy Installation {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "Running",
    BGTASKS_STATUS_FAILED: "Failed",
    BGTASKS_STATUS_SUCCEEDED: "Finished", 
    BGTASKS_STATUS_WARNING: "Partial Succeeded",
    BGTASKS_STATUS_PENDING: "Pending",
    BGTASKS_INFO_DIALOG_TITLE: "Details",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Standard output:",
    BGTASKS_INFO_DIALOG_STDERR: "Standard error:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Exception:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Result:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Server name:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "User directory:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Active Background Tasks",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "None",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "No Active Background Tasks",
    BGTASKS_DISPLAY_BUTTON: "Task Details and History",
    BGTASKS_EXPAND: "Expand section",
    BGTASKS_COLLAPSE: "Collapse section",
    PROFILE_MENU_HELP_TITLE: "Help",
    DETAILS_DESCRIPTION: "Description",
    DETAILS_OVERVIEW: "Overview",
    DETAILS_OTHERVERSIONS: "Other versions",
    DETAILS_VERSION: "Version: {0}",
    DETAILS_UPDATED: "Updated: {0}",
    DETAILS_NOTOPTIMIZED: "Not optimized for current device.",
    DETAILS_ADDBUTTON: "Add To My Toolbox",
    DETAILS_OPEN: "Open",
    DETAILS_CATEGORY: "Category {0}",
    DETAILS_ADDCONFIRM: "Tool {0} was successfully added to the toolbox.",
    CONFIRM_DIALOG_HELP: "Help",
    YES_BUTTON_LABEL: "{0} yes",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} no",  // insert is dialog title

    YES: "Yes",
    NO: "No",

    TOOL_OIDC_ACCESS_DENIED: "The user is not in the role that has permission to complete this request.",
    TOOL_OIDC_GENERIC_ERROR: "An error has occurred. Review the error in the log for more information.",
    TOOL_DISABLE: "The user does not have permission to use this tool. Only users in the Administrator role have permission to use this tool." 
  },

  "cs": true,
  "de": true,
  "es": true, 
  "fr": true,
  "hu": true,
  "it": true,
  "ja": true,
  "ko": true,
  "pl": true,
  "pt-br": true,
  "ro": true,
  "ru": true,
  "zh": true,
  "zh-tw": true

});