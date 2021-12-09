/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
    ERROR : "Error",
    ERROR_STATUS : "{0} Error: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Error occurred when requesting {0}.", // url
    ERROR_URL_REQUEST : "{0} Error occurred when requesting {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "Server did not respond in the alloted time.",
    ERROR_APP_NOT_AVAILABLE : "Application {0} is no longer available for server {1} on host {2} in directory {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Error occurred when attempting to {0} application {1} on server {2} on host {3} in directory {4}.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "Cluster {0} is {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "Cluster {0} is no longer available.", //clusterName
    STOP_FAILED_DURING_RESTART : "Stop did not complete successfully during restart.  The error was: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Error occurred when attempting to {0} cluster {1}.", //operation, clusterName
    SERVER_NONEXISTANT : "Server {0} does not exist.", // serverName
    ERROR_SERVER_OPERATION : "Error occurred when attempting to {0} server {1} on host {2} in directory {3}.", //operation, serverName, hostName, userdirName
    
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "The maintenance mode for server {0} on host {1} in directory {2} was not set because of an error.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "The attempt to unset the maintenance mode for server {0} on host {1} in directory {2} did not complete because of an error.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "The maintenance mode for host {0} was not set because of an error.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "The attempt to unset the maintenance mode for host {0} did not complete because of an error.", // hostName
    
    
    
    
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Error during server start.",
    SERVER_START_CLEAN_ERROR: "Error during server start --clean.",
    SERVER_STOP_ERROR: "Error during server stop.",
    SERVER_RESTART_ERROR: "Error during server restart.",
    
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'The server did not stop. The required API to stop the server was unavailable.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'The server did not stop. The required API to stop the server could not be determined.',
    STANDALONE_STOP_FAILED : 'The server stop operation did not complete successfully. Check server logs for details.',
    STANDALONE_STOP_SUCCESS : 'Server successfully stopped.',
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