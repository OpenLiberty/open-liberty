/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
  root : {
    EXPLORER : "Explorer",
    EXPLORE : "Explore",
    DASHBOARD : "Dashboard",
    DASHBOARD_VIEW_ALL_APPS : "View all applications",
    DASHBOARD_VIEW_ALL_SERVERS : "View all servers",
    DASHBOARD_VIEW_ALL_CLUSTERS : "View all clusters",
    DASHBOARD_VIEW_ALL_HOSTS : "View all hosts",
    DASHBOARD_VIEW_ALL_RUNTIMES : "View all runtimes",
    SEARCH : "Search",
    SEARCH_RECENT : "Recent Searches",
    SEARCH_RESOURCES : "Search Resources",
    SEARCH_RESULTS : "Search Results",
    SEARCH_NO_RESULTS : "No results",
    SEARCH_NO_MATCHES : "No matches",
    SEARCH_TEXT_INVALID : "Search text includes invalid characters",
    SEARCH_CRITERIA_INVALID : "The search criteria are not valid.",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} is not valid when specified with {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "Specify {0} only once.",
    SEARCH_TEXT_MISSING : "Search text is required",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "Searching of application tags on a server is not supported.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "Searching of application tags on a cluster is not supported.",
    SEARCH_UNSUPPORT : "Search criteria are not supported.",
    SEARCH_SWITCH_VIEW : "Switch view",
    FILTERS : "Filters",
    DEPLOY_SERVER_PACKAGE : "Deploy Server Package",
    MEMBER_OF : "Member of",
    N_CLUSTERS: "{0} Clusters ...",

    INSTANCE : "Instance",
    INSTANCES : "Instances",
    APPLICATION : "Application",
    APPLICATIONS : "Applications",
    SERVER : "Server",
    SERVERS : "Servers",
    CLUSTER : "Cluster",
    CLUSTERS : "Clusters",
    CLUSTER_NAME : "Cluster Name: ",
    CLUSTER_STATUS : "Cluster Status: ",
    APPLICATION_NAME : "Application Name: ",
    APPLICATION_STATE : "Application State: ",
    HOST : "Host",
    HOSTS : "Hosts",
    RUNTIME : "Runtime",
    RUNTIMES : "Runtimes",
    PATH : "Path",
    CONTROLLER : "Controller",
    CONTROLLERS : "Controllers",
    OVERVIEW : "Overview",
    CONFIGURE : "Configure",

    SEARCH_RESOURCE_TYPE: "Type", // Search by resource types
    SEARCH_RESOURCE_STATE: "State", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "All", // Search all resource types
    SEARCH_RESOURCE_NAME: "Name", // Search by resource name
    SEARCH_RESOURCE_TAG: "Tag", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Container", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "None", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Runtime Type", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Owner", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Contact", // Search by contact
    SEARCH_RESOURCE_NOTE: "Note", // Search by note

    GRID_HEADER_USERDIR : "User Directory",
    GRID_HEADER_NAME : "Name",
    GRID_LOCATION_NAME : "Location",
    GRID_ACTIONS : "Grid Actions",
    GRID_ACTIONS_LABEL : "{0} grid actions",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} on {1} ({2})", // server on host (/path)

    STATS : "Monitor",
    STATS_ALL : "All",
    STATS_VALUE : "Value: {0}",
    CONNECTION_IN_USE_STATS : "{0} In Use = {1} Managed - {2} Free",
    CONNECTION_IN_USE_STATS_VALUE : "Value: {0} In Use = {1} Managed - {2} Free",
    DATA_SOURCE : "Data Source: {0}",
    STATS_DISPLAY_LEGEND : "Show legend",
    STATS_HIDE_LEGEND : "Hide legend",
    STATS_VIEW_DATA : "View chart data",
    STATS_VIEW_DATA_TIMESTAMP : "Timestamp",
    STATS_ACTION_MENU : "{0} action menu",
    STATS_SHOW_HIDE : "Add Resource Metrics",
    STATS_SHOW_HIDE_SUMMARY : "Add Metrics for Summary",
    STATS_SHOW_HIDE_TRAFFIC : "Add Metrics for Traffic",
    STATS_SHOW_HIDE_PERFORMANCE : "Add Metrics for Performance",
    STATS_SHOW_HIDE_AVAILABILITY : "Add Metrics for Availability",
    STATS_SHOW_HIDE_ALERT : "Add Metrics for Alert",
    STATS_SHOW_HIDE_LIST_BUTTON : "Show or hide the resource metrics list",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Edit Charts",
    STATS_SHOW_HIDE_CONFIRM : "Save",
    STATS_SHOW_HIDE_CANCEL : "Cancel",
    STATS_SHOW_HIDE_DONE : "Done",
    STATS_DELETE_GRAPH : "Delete Chart",
    STATS_ADD_CHART_LABEL : "Add chart to view",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Add all JVM charts to view",
    STATS_HEAP_TITLE : "Used Heap Memory",
    STATS_HEAP_USED : "Used: {0} MB",
    STATS_HEAP_COMMITTED : "Committed: {0} MB",
    STATS_HEAP_MAX : "Max: {0} MB",
    STATS_HEAP_X_TIME : "Time",
    STATS_HEAP_Y_MB : "MB Used",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "Loaded Classes",
    STATS_CLASSES_LOADED : "Loaded: {0}",
    STATS_CLASSES_UNLOADED : "Unloaded: {0}",
    STATS_CLASSES_TOTAL : "Total: {0}",
    STATS_CLASSES_Y_TOTAL : "Loaded classes",
    STATS_PROCESSCPU_TITLE : "CPU Usage",
    STATS_PROCESSCPU_USAGE : "CPU Usage: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "CPU percentage",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Active JVM Threads",
    STATS_LIVE_MSG_INIT : "Showing live data",
    STATS_LIVE_MSG :"This chart has no historical data. It will continue to show the most recent 10 minutes of data.",
    STATS_THREADS_ACTIVE : "Live: {0}",
    STATS_THREADS_PEAK : "Peak: {0}",
    STATS_THREADS_TOTAL : "Total: {0}",
    STATS_THREADS_Y_THREADS : "Threads",
    STATS_TP_POOL_SIZE : "Pool size",
    STATS_JAXWS_TITLE : "JAX-WS Web Services",
    STATS_JAXWS_BUTTON_LABEL : "Add all JAX-WS Web Services charts to view",
    STATS_JW_AVG_RESP_TIME : "Average Response Time",
    STATS_JW_AVG_INVCOUNT : "Average Invocation Count",
    STATS_JW_TOTAL_FAULTS : "Total Runtime Faults",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Select resources...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} resources",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 resource",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "You must select at least one resource.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "No data is available for the selected time range.",
    STATS_ACCESS_LOG_TITLE : "Access Log",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Add all Access Log charts to view",
    STATS_ACCESS_LOG_GRAPH : "Access Log Message Count",
    STATS_ACCESS_LOG_SUMMARY : "Access Log Summary",
    STATS_ACCESS_LOG_TABLE : "Access Log Message List",
    STATS_MESSAGES_TITLE : "Messages and Trace",
    STATS_MESSAGES_BUTTON_LABEL : "Add all Messages and Trace charts to view",
    STATS_MESSAGES_GRAPH : "Log Message Count",
    STATS_MESSAGES_TABLE : "Log Message List",
    STATS_FFDC_GRAPH : "FFDC Count",
    STATS_FFDC_TABLE : "FFDC List",
    STATS_TRACE_LOG_GRAPH : "Trace Message Count",
    STATS_TRACE_LOG_TABLE : "Trace Message List",
    STATS_THREAD_POOL_TITLE : "Thread Pool",
    STATS_THREAD_POOL_BUTTON_LABEL : "Add all Thread Pool charts to view",
    STATS_THREADPOOL_TITLE : "Active Liberty Threads",
    STATS_THREADPOOL_SIZE : "Pool Size: {0}",
    STATS_THREADPOOL_ACTIVE : "Active: {0}",
    STATS_THREADPOOL_TOTAL : "Total: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Active Threads",
    STATS_SESSION_MGMT_TITLE : "Sessions",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Add all Sessions charts to view",
    STATS_SESSION_CONFIG_LABEL : "Select sessions...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} sessions",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 session",
    STATS_SESSION_CONFIG_SELECT_ONE : "You must select at least one session.",
    STATS_SESSION_TITLE : "Active Sessions",
    STATS_SESSION_Y_ACTIVE : "Active Sessions",
    STATS_SESSION_LIVE_LABEL : "Live Count: {0}",
    STATS_SESSION_CREATE_LABEL : "Create Count: {0}",
    STATS_SESSION_INV_LABEL : "Invalidated Count: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Invalidated Count by Timeout: {0}",
    STATS_WEBCONTAINER_TITLE : "Web Applications",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Add all Web Applications charts to view",
    STATS_SERVLET_CONFIG_LABEL : "Select servlets...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} servlets",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "You must select at least one servlet.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Request Count",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Request Count",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Response Count",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Response Count",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Average Response Time (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Response Time (ns)",
    STATS_CONN_POOL_TITLE : "Connection Pool",
    STATS_CONN_POOL_BUTTON_LABEL : "Add all Connection Pool charts to view",
    STATS_CONN_POOL_CONFIG_LABEL : "Select data sources...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} data sources",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 data source",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "You must select at least one data source.",
    STATS_CONNECT_IN_USE_TITLE : "In Use Connections",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Connections",
    STATS_CONNECT_IN_USE_LABEL : "In Use: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Used: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Free: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Created: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Destroyed: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Average Wait Time (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Wait Time (ms)",
    STATS_TIME_ALL : "All",
    STATS_TIME_1YEAR : "1y",
    STATS_TIME_1MONTH : "1mo",
    STATS_TIME_1WEEK : "1w",
    STATS_TIME_1DAY : "1d",
    STATS_TIME_1HOUR : "1h",
    STATS_TIME_10MINUTES : "10m",
    STATS_TIME_5MINUTES : "5m",
    STATS_TIME_1MINUTE : "1m",
    STATS_PERSPECTIVE_SUMMARY : "Summary",
    STATS_PERSPECTIVE_TRAFFIC : "Traffic",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "JVM Traffic",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Connection Traffic",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Access Log Traffic",
    STATS_PERSPECTIVE_PROBLEM : "Problem",
    STATS_PERSPECTIVE_PERFORMANCE : "Performance",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "JVM Performance",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Connection Performance",
    STATS_PERSPECTIVE_ALERT : "Alert Analysis",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Access Log Alert",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Message and Trace Log Alert",
    STATS_PERSPECTIVE_AVAILABILITY : "Availability",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Last minute",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "Last 5 minutes",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "Last 10 minutes",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Last hour",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Last day",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Last week",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Last month",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Last year",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Last {0}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Last {0}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Last {0}m {1}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Last {0}h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Last {0}h {1}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Last {0}d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Last {0}d {1}h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Last {0}w",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Last {0}w {1}d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Last {0}mo",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Last {0}mo {1}d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Last {0}y",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Last {0}y {1}mo",

    STATS_LIVE_UPDATE_LABEL: "Live Updating",
    STATS_TIME_SELECTOR_NOW_LABEL: "Now",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Log Messages",

    AUTOSCALED_APPLICATION : "Auto Scaled Application",
    AUTOSCALED_SERVER : "Auto Scaled Server",
    AUTOSCALED_CLUSTER : "Auto Scaled Cluster",
    AUTOSCALED_POLICY : "Auto scaling policy",
    AUTOSCALED_POLICY_DISABLED : "Auto scaling policy is disabled",
    AUTOSCALED_NOACTIONS : "Actions are not available for auto scaled resources",

    START : "Start",
    START_CLEAN : "Start --clean",
    STARTING : "Starting",
    STARTED : "Started",
    RUNNING : "Running",
    NUM_RUNNING: "{0} Running",
    PARTIALLY_STARTED : "Partially Started",
    PARTIALLY_RUNNING : "Partially Running",
    NOT_STARTED : "Not Started",
    STOP : "Stop",
    STOPPING : "Stopping",
    STOPPED : "Stopped",
    NUM_STOPPED : "{0} Stopped",
    NOT_RUNNING : "Not Running",
    RESTART : "Restart",
    RESTARTING : "Restarting",
    RESTARTED : "Restarted",
    ALERT : "Alert",
    ALERTS : "Alerts",
    UNKNOWN : "Unknown",
    NUM_UNKNOWN : "{0} Unknown",
    SELECT : "Select",
    SELECTED : "Selected",
    SELECT_ALL : "Select All",
    SELECT_NONE : "Select None",
    DESELECT: "Deselect",
    DESELECT_ALL : "Deselect All",
    TOTAL : "Total",
    UTILIZATION : "Over {0}% Utilization", // percent

    ELLIPSIS_ARIA: "Expand for more options.",
    EXPAND : "Expand",
    COLLAPSE: "Collapse",

    ALL : "All",
    ALL_APPS : "All Applications",
    ALL_SERVERS : "All Servers",
    ALL_CLUSTERS : "All Clusters",
    ALL_HOSTS : "All Hosts",
    ALL_APP_INSTANCES : "All App Instances",
    ALL_RUNTIMES : "All Runtimes",

    ALL_APPS_RUNNING : "All Apps Running",
    ALL_SERVER_RUNNING : "All Servers Running",
    ALL_CLUSTERS_RUNNING : "All Clusters Running",
    ALL_APPS_STOPPED : "All Apps Stopped",
    ALL_SERVER_STOPPED : "All Servers Stopped",
    ALL_CLUSTERS_STOPPED : "All Clusters Stopped",
    ALL_SERVERS_UNKNOWN : "All Servers Unknown",
    SOME_APPS_RUNNING : "Some Apps Running",
    SOME_SERVERS_RUNNING : "Some Servers Running",
    SOME_CLUSTERS_RUNNING : "Some Clusters Running",
    NO_APPS_RUNNING : "No Apps Running",
    NO_SERVERS_RUNNING : "No Servers Running",
    NO_CLUSTERS_RUNNING : "No Clusters Running",

    HOST_WITH_ALL_SERVERS_RUNNING: "Hosts with all servers running", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Hosts with some servers running",
    HOST_WITH_NO_SERVERS_RUNNING: "Hosts with no servers running", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Hosts with all servers stopped",
    HOST_WITH_SERVERS_RUNNING: "Hosts with servers running",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Runtimes with some servers running",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Runtimes with all servers stopped",
    RUNTIME_WITH_SERVERS_RUNNING: "Runtimes with servers running",

    START_ALL_APPS : "Start all apps?",
    START_ALL_INSTANCES : "Start all app instances?",
    START_ALL_SERVERS : "Start all servers?",
    START_ALL_CLUSTERS : "Start all clusters?",
    STOP_ALL_APPS : "Stop all apps?",
    STOPE_ALL_INSTANCES : "Stop all app instances?",
    STOP_ALL_SERVERS : "Stop all servers?",
    STOP_ALL_CLUSTERS : "Stop all clusters?",
    RESTART_ALL_APPS : "Restart all apps?",
    RESTART_ALL_INSTANCES : "Restart all app instances?",
    RESTART_ALL_SERVERS : "Restart all servers?",
    RESTART_ALL_CLUSTERS : "Restart all clusters?",

    START_INSTANCE : "Start application instance?",
    STOP_INSTANCE : "Stop application instance?",
    RESTART_INSTANCE : "Restart application instance?",

    START_SERVER : "Start server {0}?",
    STOP_SERVER : "Stop server {0}?",
    RESTART_SERVER : "Restart server {0}?",

    START_ALL_INSTS_OF_APP : "Start all instances of {0}?", // application name
    START_APP_ON_SERVER : "Start {0} on {1}?", // app name, server name
    START_ALL_APPS_WITHIN : "Start all apps within {0}?", // resource
    START_ALL_APP_INSTS_WITHIN : "Start all app instances within {0}?", // resource
    START_ALL_SERVERS_WITHIN : "Start all servers within {0}?", // resource
    STOP_ALL_INSTS_OF_APP : "Stop all instances of {0}?", // application name
    STOP_APP_ON_SERVER : "Stop {0} on {1}?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Stop all apps within {0}?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Stop all app instances within {0}?", // resource
    STOP_ALL_SERVERS_WITHIN : "Stop all servers within {0}?", // resource
    RESTART_ALL_INSTS_OF_APP : "Restart all instances of {0}?", // application name
    RESTART_APP_ON_SERVER : "Restart {0} on {1}?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Restart all apps within {0}?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Restart all app instances within {0}?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Restart all running servers within {0}?", // resource

    START_SELECTED_APPS : "Start all instances of selected apps?",
    START_SELECTED_INSTANCES : "Start selected app instances?",
    START_SELECTED_SERVERS : "Start selected servers?",
    START_SELECTED_SERVERS_LABEL : "Start selected servers",
    START_SELECTED_CLUSTERS : "Start selected clusters?",
    START_CLEAN_SELECTED_SERVERS : "Start --clean selected servers?",
    START_CLEAN_SELECTED_CLUSTERS : "Start --clean selected clusters?",
    STOP_SELECTED_APPS : "Stop all instances of selected apps?",
    STOP_SELECTED_INSTANCES : "Stop selected app instances?",
    STOP_SELECTED_SERVERS : "Stop selected servers?",
    STOP_SELECTED_CLUSTERS : "Stop selected clusters?",
    RESTART_SELECTED_APPS : "Restart all instances of selected apps?",
    RESTART_SELECTED_INSTANCES : "Restart selected app instances?",
    RESTART_SELECTED_SERVERS : "Restart selected servers?",
    RESTART_SELECTED_CLUSTERS : "Restart selected clusters?",

    START_SERVERS_ON_HOSTS : "Start all servers on selected hosts?",
    STOP_SERVERS_ON_HOSTS : "Stop all servers on selected hosts?",
    RESTART_SERVERS_ON_HOSTS : "Restart all running servers on selected hosts?",

    SELECT_APPS_TO_START : "Select stopped apps to start.",
    SELECT_APPS_TO_STOP : "Select started apps to stop.",
    SELECT_APPS_TO_RESTART : "Select started apps to restart.",
    SELECT_INSTANCES_TO_START : "Select stopped app instances to start.",
    SELECT_INSTANCES_TO_STOP : "Select started app instances to stop.",
    SELECT_INSTANCES_TO_RESTART : "Select started app instances to restart.",
    SELECT_SERVERS_TO_START : "Select stopped servers to start.",
    SELECT_SERVERS_TO_STOP : "Select started servers to stop.",
    SELECT_SERVERS_TO_RESTART : "Select started servers to restart.",
    SELECT_CLUSTERS_TO_START : "Select stopped clusters to start.",
    SELECT_CLUSTERS_TO_STOP : "Select started clusters to stop.",
    SELECT_CLUSTERS_TO_RESTART : "Select started clusters to restart.",

    STATUS : "Status",
    STATE : "State:",
    NAME : "Name:",
    DIRECTORY : "Directory",
    INFORMATION : "Information",
    DETAILS : "Details",
    ACTIONS : "Actions",
    CLOSE : "Close",
    HIDE : "Hide",
    SHOW_ACTIONS : "Show Actions",
    SHOW_SERVER_ACTIONS_LABEL : "Server {0} actions",
    SHOW_APP_ACTIONS_LABEL : "Application {0} actions",
    SHOW_CLUSTER_ACTIONS_LABEL : "Cluster {0} actions",
    SHOW_HOST_ACTIONS_LABEL : "Host {0} actions",
    SHOW_RUNTIME_ACTIONS_LABEL : "Runtime {0} actions",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "Server {0} actions menu",
    SHOW_APP_ACTIONS_MENU_LABEL : "Application {0} actions menu",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "Cluster {0} actions menu",
    SHOW_HOST_ACTIONS_MENU_LABEL : "Host {0} actions menu",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "Runtime {0} actions menu",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "Runtime on host {0} actions menu",
    SHOW_COLLECTION_MENU_LABEL : "Collection {0} state actions menu",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "Search {0} state actions menu",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: unknown state", // resourceName
    UNKNOWN_STATE_APPS : "{0} apps in unknown state", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} app instances in unknown state", // quantity
    UNKNOWN_STATE_SERVERS : "{0} servers in unknown state", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} clusters in unknown state", // quantity

    INSTANCES_NOT_RUNNING : "{0} app instances not running", // quantity
    APPS_NOT_RUNNING : "{0} apps not running", // quantity
    SERVERS_NOT_RUNNING : "{0} servers not running", // quantity
    CLUSTERS_NOT_RUNNING : "{0} clusters not running", // quantity

    APP_STOPPED_ON_SERVER : "{0} stopped on running servers: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} apps stopped on running servers: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} apps stopped on running servers.", // quantity
    NUMBER_RESOURCES : "{0} Resources", // quantity
    NUMBER_APPS : "{0} Applications", // quantity
    NUMBER_SERVERS : "{0} Servers", // quantity
    NUMBER_CLUSTERS : "{0} Clusters", // quantity
    NUMBER_HOSTS : "{0} Hosts", // quantity
    NUMBER_RUNTIMES : "{0} Runtimes", // quantity
    SERVERS_INSERT : "servers",
    INSERT_STOPPED_ON_INSERT : "{0} stopped on running {1}.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} stopped on running server {1}", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "{0} on cluster {1} stopped on running servers: {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} app instances stopped on running servers.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: app instance not running", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: not all apps running", // serverName[]
    NO_APPS_RUNNING : "{0}: no apps running", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} servers with not all apps running", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} servers with no apps running", // quantity

    COUNT_OF_APPS_SELECTED : "{0} applications selected",
    RATIO_RUNNING : "{0} running", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0} selected",

    NO_HOSTS_SELECTED : "No hosts selected",
    NO_DEPLOY_RESOURCE : "No resource for deploy installation",
    NO_TOPOLOGY : "There are no {0}.",
    COUNT_OF_APPS_STARTED  : "{0} applications started",

    APPS_LIST : "{0} Applications",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} instances running",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} servers running",
    RESOURCE_ON_RESOURCE : "{0} on {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} on server {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} on cluster {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Restart is disabled for this server because it is hosting Admin Center",
    ACTION_DISABLED_FOR_USER: "Actions are disabled on this resource because the user is not authorized",

    RESTART_AC_TITLE: "No Restart for Admin Center",
    RESTART_AC_DESCRIPTION: "{0} is providing Admin Center. Admin Center cannot restart itself.",
    RESTART_AC_MESSAGE: "All other selected servers will be restarted.",
    RESTART_AC_CLUSTER_MESSAGE: "All other selected clusters will be restarted.",

    STOP_AC_TITLE: "Stop Admin Center",
    STOP_AC_DESCRIPTION: "Server {0} is a collective controller that runs Admin Center. Stopping it might impact Liberty collective management operations and make Admin Center unavailable.",
    STOP_AC_MESSAGE: "Do you want to stop this controller?",
    STOP_STANDALONE_DESCRIPTION: "Server {0} runs Admin Center. Stopping it will make Admin Center unavailable.",
    STOP_STANDALONE_MESSAGE: "Do you want to stop this server?",

    STOP_CONTROLLER_TITLE: "Stop Controller",
    STOP_CONTROLLER_DESCRIPTION: "Server {0} is a collective controller. Stopping it might impact Liberty collective operations.",
    STOP_CONTROLLER_MESSAGE: "Do you want to stop this controller?",

    STOP_AC_CLUSTER_TITLE: "Stop cluster {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "Cluster {0} contains a collective controller that runs Admin Center.  Stopping it might affect Liberty collective management operations and make Admin Center unavailable.",
    STOP_AC_CLUSTER_MESSAGE: "Do you want to stop this cluster?",

    INVALID_URL: "Page does not exist.",
    INVALID_APPLICATION: "Application {0} no longer exists in the collective.", // application name
    INVALID_SERVER: "Server {0} no longer exists in the collective.", // server name
    INVALID_CLUSTER: "Cluster {0} no longer exists in the collective.", // cluster name
    INVALID_HOST: "Host {0} no longer exists in the collective.", // host name
    INVALID_RUNTIME: "Runtime {0} no longer exists in the collective.", // runtime name
    INVALID_INSTANCE: "Application instance {0} no longer exists in the collective.", // application instance name
    GO_TO_DASHBOARD: "Go To Dashboard",
    VIEWED_RESOURCE_REMOVED: "Oops! The resource was removed or is no longer available.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "The connection to the server has been lost. The page will no longer show dynamic changes to the environment. Refresh the page to restore the connection and dynamic updates.",
    ERROR_MESSAGE: "Connection Interrupted",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Stop Server',

    // Tags
    RELATED_RESOURCES: "Related resources",
    TAGS : "Tags",
    TAG_BUTTON_LABEL : "Tag {0}",  // tag value
    TAGS_LABEL : "Enter tags separated by comma, space, enter, or tab.",
    OWNER : "Owner",
    OWNER_BUTTON_LABEL : "Owner {0}",  // owner value
    CONTACTS : "Contacts",
    CONTACT_BUTTON_LABEL : "Contact {0}",  // contact value
    PORTS : "Ports",
    CONTEXT_ROOT : "Context Root",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "More",  // alt text for the ... button
    MORE_BUTTON_MENU : "{0} More menu", // alt text for the menu
    NOTES: "Notes",
    NOTE_LABEL : "Note {0}",  // note value
    SET_ATTRIBUTES: "Tags and Metadata",
    SETATTR_RUNTIME_NAME: "{0} on {1}",  // runtime, host
    SAVE: "Save",
    TAGINVALIDCHARS: "Characters '/', '<', and '>' are not valid.",
    ERROR_GET_TAGS_METADATA: "The product cannot get current tags and metatdata for the resource.",
    ERROR_SET_TAGS_METADATA: "An error prevented the product from setting the tags and metadata.",
    METADATA_WILLBE_INHERITED: "Metadata is set on the application and shared across all instances in the cluster.",
    ERROR_ALT: "Error",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Current statistics are not available for this server because it is stopped. Start the server to begin monitoring it.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Current statistics are not available for this application because its associated server is stopped. Start the server to begin monitoring this application.",
    GRAPH_FEATURES_NOT_CONFIGURED: "There's nothing here yet! Monitor this resource by selecting the Edit icon and adding metrics.",
    NO_GRAPHS_AVAILABLE: "There are no available metrics to add. Try installing additional monitoring features to make more metrics available.",
    NO_APPS_GRAPHS_AVAILABLE: "There are no available metrics to add. Try installing additional monitoring features to make more metrics available. Also, ensure the application is in use.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Unsaved Changes",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "You have unsaved changes. If you move to another page, you will lose the changes.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Do you want to save your changes?",

    NO_CPU_STATS_AVAILABLE : "CPU Usage statistics are not available for this server.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "To enable this view, install the Server Config tool.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Save changes to {0} before closing?",
    SAVE: "Save",
    DONT_SAVE: "Don't Save",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Enable Maintenance Mode",
    DISABLE_MAINTENANCE_MODE: "Disable Maintenance Mode",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Enable maintenance mode",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Disable maintenance mode",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Enable maintenance mode on the host and all its servers ({0} servers)",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Enable maintenance mode on the hosts and all their servers ({0} servers)",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Enable maintenance mode on the server",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Enable maintenance mode on the servers",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Disable maintenance mode on the host and all its servers ({0} servers)",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Disable maintenance mode on the server",
    BREAK_AFFINITY_LABEL: "Break affinity with active sessions",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Enable",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Disable",
    MAINTENANCE_MODE: "Maintenance mode",
    ENABLING_MAINTENANCE_MODE: "Enabling maintenance mode",
    MAINTENANCE_MODE_ENABLED: "Maintenance mode enabled",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "Maintenance mode was not enabled because alternate servers did not start.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Select Force to enable maintenance mode without starting alternate servers. Force might break auto scaling policies.",
    MAINTENANCE_MODE_FAILED: "Maintenance mode cannot be enabled.",
    MAINTENANCE_MODE_FORCE_LABEL: "Force",
    MAINTENANCE_MODE_CANCEL_LABEL: "Cancel",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0} servers are currently in maintenance mode.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Enabling maintenance mode on all host servers.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Enabling maintenance mode on all host servers.  Display the Servers view for status.",

    SERVER_API_DOCMENTATION: "View server API definition",

    // objectView title
    TITLE_FOR_CLUSTER: "Cluster {0}", // cluster name
    TITLE_FOR_HOST: "Host {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Collective controller",
    LIBERTY_SERVER : "Liberty server",
    NODEJS_SERVER : "Node.js server",
    CONTAINER : "Container",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Liberty server in a Docker container",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Node.js server in a Docker container",
    RUNTIME_LIBERTY : "Liberty Runtime",
    RUNTIME_NODEJS : "Node.js Runtime",
    RUNTIME_DOCKER : "Runtime in a Docker container"

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
