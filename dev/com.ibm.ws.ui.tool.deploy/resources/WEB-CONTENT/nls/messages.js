var messages = {
//General
"DEPLOY_TOOL_TITLE": "Deploy",
"SEARCH" : "Search",
"SEARCH_HOSTS" : "Search Hosts",
"EXPLORE_TOOL": "EXPLORE TOOL",
"EXPLORE_TOOL_INSERT": "Try the Explore Tool",
"EXPLORE_TOOL_ARIA": "Search for hosts in the Explore Tool in a new tab",

//Rule Selector Panel
"RULESELECT_EDIT" : "EDIT",
"RULESELECT_CHANGE_SELECTION" : "EDIT SELECTION",
"RULESELECT_SERVER_DEFAULT" : "DEFAULT SERVER TYPES",
"RULESELECT_SERVER_CUSTOM" : "CUSTOM TYPES",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Custom Server Types",
"RULESELECT_NEXT" : "NEXT",
"RULESELECT_SERVER_TYPE": "Server Type",
"RULESELECT_SELECT_ONE": "Select One",
"RULESELECT_DEPLOY_TYPE" : "Deploy Rule",
"RULESELECT_SERVER_SUBHEADING": "Server",
"RULESELECT_CUSTOM_PACKAGE": "Custom Package",
"RULESELECT_RULE_DEFAULT" : "DEFAULT RULES",
"RULESELECT_RULE_CUSTOM" : "CUSTOM RULES",
"RULESELECT_FOOTER" : "Choose a server type and rule type before you return to the Deploy form.",
"RULESELECT_CONFIRM" : "CONFIRM",
"RULESELECT_CUSTOM_INFO": "You can define your own rules with customized inputs and deployment behavior.",
"RULESELECT_CUSTOM_INFO_LINK": "Learn More",
"RULESELECT_BACK": "Back",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Rule selection panel {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Open",
"RULESELECT_CLOSED" : "Closed",
"RULESELECT_SCROLL_UP": "Scroll Up",
"RULESELECT_SCROLL_DOWN": "Scroll Down",
"RULESELECT_EDIT_SERVER_ARIA" : "Edit server type, current selection {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Edit rule, current selection {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Next Panel",

//SERVER TYPES
"LIBERTY_SERVER" : "Liberty Server",
"NODEJS_SERVER" : "Node.js Server",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Application Package", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Server Package", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Docker Container", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Deployment Parameters",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Deployment Parameters ({0})",
"PARAMETERS_DESCRIPTION": "Details are based on the selected server and template type.",
"PARAMETERS_TOGGLE_CONTROLLER": "Use a file located on the collective controller",
"PARAMETERS_TOGGLE_UPLOAD": "Upload a file",
"SEARCH_IMAGES": "Search Images",
"SEARCH_CLUSTERS": "Search Clusters",
"CLEAR_FIELD_BUTTON_ARIA": "Clear the input value",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Upload Server Package File",
"BROWSE_TITLE": "Upload {0}",
"STRONGLOOP_BROWSE": "Drag a file here or {0} to provide the file name", //BROWSE_INSERT
"BROWSE_INSERT" : "browse",
"BROWSE_ARIA": "browse files",
"FILE_UPLOAD_PREVIOUS" : "Use a file located on the collective controller",
"IS_UPLOADING": "{0} is uploading...",
"CANCEL" : "CANCEL",
"UPLOAD_SUCCESSFUL" : "{0} uploaded successfully!", // Package Name
"UPLOAD_FAILED" : "Upload failed",
"RESET" : "reset",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "The write directory list is empty.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "The path specified must be in the write directory list.",
"PARAMETERS_FILE_ARIA" : "Deployment Parameters or {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "You must configure a Docker repository",
"DOCKER_EMPTY_IMAGE_ERROR": "No images found in the configured Docker repository",
"DOCKER_GENERIC_ERROR": "No Docker images loaded. Ensure you have a configured Docker repository.",
"REFRESH": "Refresh",
"REFRESH_ARIA": "Refresh Docker Images",
"PARAMETERS_DOCKER_ARIA": "Deployment Parameters or Search Docker Images",
"DOCKER_IMAGES_ARIA" : "Docker Images List",
"LOCAL_IMAGE": "local image name",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "Container name must match the format [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} Selected Host", //quantity
"N_SELECTED_HOSTS": "{0} Selected Hosts", //quantity
"SELECT_HOSTS_MESSAGE": "Make a selection from the list of available hosts. You can search for hosts by name or tag(s).",
"ONE_HOST" : "{0} Result", //quantity
"N_HOSTS": "{0} Results", //quantity
"SELECT_HOSTS_FOOTER": "Need a more complex search? {0}", //EXPLORE_TOOL_INSERT
"NAME": "NAME",
"NAME_FILTER": "Filter hosts by name", // Used for aria-label
"TAG": "TAG",
"TAG_FILTER": "Filter hosts by tag",
"ALL_HOSTS_LIST_ARIA" : "All hosts list",
"SELECTED_HOSTS_LIST_ARIA": "Selected hosts list",

//Security Details
"SECURITY_DETAILS": "Security Details",
"SECURITY_DETAILS_FOR_GROUP": "Security Details for {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Additional credentials needed for server security.",
"SECURITY_CREATE_PASSWORD" : "Create Password",
"KEYSTORE_PASSWORD_MESSAGE": "Specify a password to protect newly generated keystore files containing server authentication credentials.",
"PASSWORD_MESSAGE": "Specify a password to protect newly generated files containing server authentication credentials.",
"KEYSTORE_PASSWORD": "KeyStore Password",
"CONFIRM_KEYSTORE_PASSWORD": "Confirm KeyStore Password",
"PASSWORDS_DONT_MATCH": "Passwords do not match",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "Confirm {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "Confirm {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Review and Deploy",
"REVIEW_AND_DEPLOY_MESSAGE" : "All fields {0} before deployment.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "must be completed",
"READY_FOR_DEPLOYMENT": "ready for deployment.",
"READY_FOR_DEPLOYMENT_CAPS": "Ready for deployment.",
"READY_TO_DEPLOY": "Form is complete. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "Form is complete. Server Package is {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "Form is complete. Docker Container is {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "DEPLOY",

"DEPLOY_UPLOADING" : "Please allow server package to finish uploading...",
"DEPLOY_FILE_UPLOADING" : "Finishing file upload...",
"UPLOADING": "Uploading...",
"DEPLOY_UPLOADING_MESSAGE" : "Keep this window open until deployment process begins.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "After {0} finishes uploading, you can monitor the progress of your deployment here.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% complete", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Watch for updates here, or close this window and let it run in the background!",
"DEPLOY_CHECK_STATUS": "You can check the status of your deployment at any time by clicking the Background Tasks icon in the top right corner of this screen.",
"DEPLOY_IN_PROGRESS": "Your deployment is in progress!",
"DEPLOY_VIEW_BG_TASKS": "View Background Tasks",
"DEPLOYMENT_PROGRESS": "Deployment Progress",
"DEPLOYING_IMAGE": "{0} to {1} hosts", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "View successfully deployed servers",
"DEPLOY_PERCENTAGE": "{0}% COMPLETE", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "Your deployment is complete!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "Your deployment is complete, but there were some errors.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "You can investigate errors in greater detail, check on your newly deployed servers, or start another deployment.",
"DEPLOYING": "Deploying...",
"DEPLOYMENT_FAILED": "Your deployment has failed.",
"RETURN_DEPLOY": "Return to deploy form and resubmit",
"REUTRN_DEPLOY_HEADER": "Try Again",

//Footer
"FOOTER": "More to deploy?",
"FOOTER_BUTTON_MESSAGE" : "Start another deployment",

//Error stuff
"ERROR_TITLE": "Error Summary",
"ERROR_VIEW_DETAILS" : "View Error Details",
"ONE_ERROR_ONE_HOST": "One error occurred on one host",
"ONE_ERROR_MULTIPLE_HOST": "One error occurred on multiple hosts",
"MULTIPLE_ERROR_ONE_HOST": "Multiple errors occurred on one host",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Multiple errors occurred on multiple hosts",
"INITIALIZATION_ERROR_MESSAGE": "Cannot access host or deploy rules information on the server",
"TRANSLATIONS_ERROR_MESSAGE" : "Could not access externalized strings",
"MISSING_HOST": "Please select at least one host from the list",
"INVALID_CHARACTERS" : "Field cannot contain special characters like '()$%&'",
"INVALID_DOCKER_IMAGE" : "Image was not found",
"ERROR_HOSTS" : "{0} and {1} others" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
