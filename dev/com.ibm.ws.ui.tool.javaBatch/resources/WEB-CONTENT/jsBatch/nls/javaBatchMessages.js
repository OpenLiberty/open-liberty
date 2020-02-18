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
    root: {
      ACCOUNTING_STRING : "Accounting String",
      SEARCH_RESOURCE_TYPE_ALL : "All",
      SEARCH : "Search",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Enter search criteria by selecting the Add Search Criteria button and then specifying a value",
      SUBMITTED : "Submitted",
      JMS_QUEUED : "JMS Queued",
      JMS_CONSUMED : "JMS Consumed",
      JOB_PARAMETER : "Job Parameter",
      DISPATCHED : "Dispatched",
      FAILED : "Failed",
      STOPPED : "Stopped",
      COMPLETED : "Completed",
      ABANDONED : "Abandoned",
      STARTED : "Started",
      STARTING : "Starting",
      STOPPING : "Stopping",
      REFRESH : "Refresh",
      INSTANCE_STATE : "Instance State",
      APPLICATION_NAME : "Application Name",
      APPLICATION: "Application",
      INSTANCE_ID : "Instance ID",
      LAST_UPDATE : "Last Update",
      LAST_UPDATE_RANGE : "Last Update Range",
      LAST_UPDATED_TIME : "Last Updated Time",
      DASHBOARD_VIEW : "Dashboard View",
      HOMEPAGE : "Home Page",
      JOBLOGS : "Job Logs",
      QUEUED : "Queued",
      ENDED : "Ended",
      ERROR : "Error",
      CLOSE : "Close",
      WARNING : "Warning",
      GO_TO_DASHBOARD: "Go to Dashboard",
      DASHBOARD : "Dashboard",
      BATCH_JOB_NAME: "Batch Job Name",
      SUBMITTER: "Submitter",
      BATCH_STATUS: "Batch Status",
      EXECUTION_ID: "Job Execution ID",
      EXIT_STATUS: "Exit Status",
      CREATE_TIME: "Create Time",
      START_TIME: "Start Time",
      END_TIME: "End Time",
      SERVER: "Server",
      SERVER_NAME: "Server Name",
      SERVER_USER_DIRECTORY: "User Directory",
      SERVERS_USER_DIRECTORY: "Server's User Directory",
      HOST: "Host",
      NAME: "Name",
      JOB_PARAMETERS: "Job Parameters",
      JES_JOB_NAME: "JES Job Name",
      JES_JOB_ID: "JES Job ID",
      ACTIONS: "Actions",
      VIEW_LOG_FILE: "View log file",
      STEP_NAME: "Step Name",
      ID: "ID",
      PARTITION_ID: "Partition {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "View job execution {0} details",    // Job Execution ID number
      PARENT_DETAILS: "Parent Info Details",
      TIMES: "Times",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Status",
      SEARCH_ON: "Select to filter on {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Enter search criteria.",
      BREADCRUMB_JOB_INSTANCE : "Job Instance {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Job Execution {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Job Log {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "The search criteria are not valid.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "The search criteria cannot have multiple filter by {0} parameters.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Job Instances Table",
      EXECUTIONS_TABLE_IDENTIFIER: "Job Executions Table",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Steps Detail Table",
      LOADING_VIEW : "Page is currently loading information",
      LOADING_VIEW_TITLE : "Loading View",
      LOADING_GRID : "Waiting for search results to return from server",
      PAGENUMBER : "Page Number",
      SELECT_QUERY_SIZE: "Select query size",
      
      LINK_EXPLORE_HOST: "Select to view details on Host {0} in the Explore tool.",      // Host name
      LINK_EXPLORE_SERVER: "Select to view details on Server {0} in the Explore tool.",  // Server name

      //ACTIONS
      RESTART: "Restart",
      STOP: "Stop",
      PURGE: "Purge",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Actions for job instance {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Job instance actions menu",

      RESTART_INSTANCE_MESSAGE: "Do you want to restart the most recent job execution associated with job instance {0}?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Do you want to stop the most recent job execution associated with job instance {0}?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Do you want to purge all database entries and job logs associated with job instance {0}?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Purge job store only",

      RESTART_INST_ERROR_MESSAGE: "Restart request failed.",
      STOP_INST_ERROR_MESSAGE: "Stop request failed.",
      PURGE_INST_ERROR_MESSAGE: "Purge request failed.",
      ACTION_REQUEST_ERROR_MESSAGE: "Action request failed with status code: {0}.  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Reuse parameters from previous execution",
      JOB_PARAMETERS_EMPTY: "When '{0}' is not selected, use this area to enter job parameters.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Parameter name",
      JOB_PARAMETER_VALUE: "Parameter value",
      PARM_NAME_COLUMN_HEADER: "Parameter",
      PARM_VALUE_COLUMN_HEADER: "Value",
      PARM_ADD_ICON_TITLE: "Add parameter",
      PARM_REMOVE_ICON_TITLE: "Remove parameter",
      PARMS_ENTRY_ERROR: "Parameter name is required.",
      JOB_PARAMETER_CREATE: "Select {0} to add parameters to the next execution of this job instance.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Add parameter button in the table header.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Job log content",
      FILE_DOWNLOAD : "File Download",
      DOWNLOAD_DIALOG_DESCRIPTION : "Do you want to download the log file?",
      INCLUDE_ALL_LOGS : "Include all log files for job execution",
      LOGS_NAVIGATION_BAR : "Job logs navigation bar",
      DOWNLOAD : "Download",
      LOG_TOP : "Top of Logs",
      LOG_END : "End of Logs",
      PREVIOUS_PAGE : "Previous Page",
      NEXT_PAGE : "Next Page",
      DOWNLOAD_ARIA : "Download file",

      //Error messages for popups
      REST_CALL_FAILED : "The call to fetch data failed.",
      NO_JOB_EXECUTION_URL : "Either no job execution number was provided in the URL or the instance does not have any job execution logs to show.",
      NO_VIEW : "URL Error: No such view exists.",
      WRONG_TOOL_ID : "The query string of the URL did not start with the tool ID {0}, but {1} instead.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "URL Error: No logs exist.",
      NOT_A_NUMBER : "URL Error: {0} must be a number.",                                                // {0} is a field name
      PARAMETER_REPETITION : "URL Error: {0} can exist one time only in the parameters.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "URL Error: The page parameter is out of range.",
      INVALID_PARAMETER : "URL Error: {0} is not a valid parameter.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "URL Error: The URL can specify either job execution or job instance, but not both.",
      MISSING_EXECUTION_ID_PARAM : "Required execution ID parameter is missing.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "A Java batch persistent database configuration is required to use the Java Batch tool.",
      IGNORED_SEARCH_CRITERIA : "The following filter criteria were ignored in the results: {0}",

      GRIDX_SUMMARY_TEXT : "Showing latest ${0} job instances"

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
