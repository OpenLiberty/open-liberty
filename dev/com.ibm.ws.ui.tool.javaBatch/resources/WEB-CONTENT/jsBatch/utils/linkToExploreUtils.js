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
/* jshint strict: false */
/*****************************************************************************
  * module:
  *   jsBatch/utils/linkToExploreUtils
  * summary:
  *   A Util class that contains a list of utility functions and variables
  *   specific to linking the Java Batch Tool to the Explore Tool.
  *   
  *   Links occur to access the Server and Host pages of the Explore Tool.
  *
  * @return {Object} Containing all the utils methods
  ****************************************************************************/
define([ 'dojo/_base/lang',
         'dojo/Deferred', 'dojo/request',    
         'jsShared/utils/utils'
       ],

function(lang, Deferred, request, sharedUtils) {

  var haveToolboxLinkPrefixToExplore = "/adminCenter/#explore";
  var noToolboxLinkPrefixToExplore = "/ibm/adminCenter/explore1.0";
  
  // Setting of these variables is initiated from Java Batch's main.js as the first thing
  // we do in setting up the tool via a call to __getExploreInfo().   
  var __haveExploreTool = null;
  var __haveToolbox = null;
  var __haveStandaloneServer = null;
  var __standaloneServerInfo = null;         // { name: server, userdir: userdir, host: hostname }
  var __exploreCollectiveServerList = {};    // A mapping of serverIds ('host,userDir,serverName')
                                             // with value True if it is part of a collective, or 
                                             // False if it is not.
  var __exploreHostList = {};                // A mapping of host names with value True if monitored
                                             // by the Explore Tool; False if not.
  
  return {
    getExploreInfo: __getExploreInfo,
    hasExploreTool: __hasExploreTool,
    hasToolbox: __hasToolbox,
    hasStandaloneServer: __hasStandaloneServer,
    getStandaloneServerInfo: __getStandaloneServerInfo,
    isServerInCollective: __isServerInCollective,
    isHostInExploreTool: __isHostInExploreTool,
    getLinkPrefixToExploreTool: __getLinkPrefixToExploreTool
  };

  /**
   * Invoked from Java Batch's main.js as the first thing we do in setting up the tool,
   * this sets the variables used in linking to the Explore Tool.
   * 
   */
  function __getExploreInfo() {
      var deferred = new Deferred();
      
      sharedUtils.checkIfToolboxExists().then(function(result) {
          __haveToolbox = result;
          sharedUtils.checkIfExploreToolExists().then(function(result) {
              __haveExploreTool = result;
                  
              if (__haveExploreTool) {
                  __haveStandaloneServer = sharedUtils.isStandalone();  // Synchronous call
                  if (__haveStandaloneServer) {
                      sharedUtils.getStandaloneServerInfo().then(function(serverInfo) {
                         // serverInfo = { name: server, userdir: userdir, host: hostname }
                         __standaloneServerInfo = serverInfo; 
                      });
                      // If fail to get the standalone server info then just leave
                      // __standaloneServerInfo null.
                  }
              }
              
              deferred.resolve();
            });
      });
      
      return deferred;
  }  

  function __hasToolbox() {
      if (__haveToolbox !== null) {
          return __haveToolbox;
      } else {
          return false;
      }
  }

  function __hasExploreTool() {
      if (__haveExploreTool !== null) {
          return __haveExploreTool;
      } else {
          return false;
      }
  }

  function __hasStandaloneServer() {
      if (__haveStandaloneServer !== null) {
         __haveStandaloneServer = sharedUtils.isStandalone();  // Synchronous call
         return __haveStandaloneServer;
      } 
      return false;
  }
  
  function __getStandaloneServerInfo() {
      // standaloneServerInfo = { name: server, userdir: userdir, host: hostname }
      if (__hasStandaloneServer()) {
          return __standaloneServerInfo;              
      } else {
          return null;
      }
  }
  
  /**
   * Determines if the server is a server in a Collective.
   * 
   * @param serverObj   { name: server, userdir: userdir, host: hostname }
   * 
   * @returns  Deferred
   *           When resolved, the server name will be tracked in the __exploreCollectiveServerList
   *           to be easily referenced in subsequent queries. Each server value in the list will
   *           indicate True if it is part of a collective; otherwise, False.
   */
  function __isServerInCollective(serverObj) {
      var deferred = new Deferred();

      var serverId = serverObj.host + "," + serverObj.userdir + "," + serverObj.name;
      if (serverId in __exploreCollectiveServerList) {
          // If serverID already in the list, immediately resolve the deferred.
          deferred.resolve(__exploreCollectiveServerList[serverId]);
      } else {
           __addServerToExploreCollectiveServerList(serverId).then(function(response) {
              deferred.resolve(response);
          });
      }
      
      return deferred;
  }
    
  /**
   * 
   * @param serverObj   { name: server, userdir: userdir, host: hostname }
   * 
   * @returns Deferred
   *          When resolved, the server will be added to the __exploreCollectiveServerList
   *          with a value of True if it is part of a collective; False otherwise.
   */
  function __addServerToExploreCollectiveServerList(serverId) {
      var deferred = new Deferred();

      if (__hasExploreTool()) {             
          var url = '/ibm/api/collective/v1/servers/' + serverId;
          request.get(url).then(function(response) {
              __exploreCollectiveServerList[serverId] = true;
              deferred.resolve(true);
          }), lang.hitch(this, function(err) {
              __exploreCollectiveServerList[serverId] = false;
              deferred.reject(false);
          });
      } else {
          // If no Explore tool, then immediately resolve the deferred with false
          // to indicate this is NOT a server in a collective.
          __exploreCollectiveServerList[serverId] = false;
          deferred.resolve(false);
      }
       
      return deferred;      
  }
  
  /**
   * 
   * @param hostName
   * 
   * @returns Deferred
   *          When resolved, the host will be added to the __exploreHostList
   *          with a value of True if it is monitored by the Explore tool.
   */
  function __isHostInExploreTool(hostName) {
      var deferred = new Deferred();
      
      if (hostName in __exploreHostList) {
          // If host name already in the list, immediately resolve the deferred.
          deferred.resolve(__exploreHostList[hostName]);
      } else {
           __addHostToExploreHostList(hostName).then(function(response) {
              deferred.resolve(response);
          });      
      }
      
      return deferred;
  }

  /**
   * 
   * @param hostName
   * 
   * @returns Deferred
   *          When resolved, the host will be added to the __exploreHostList
   *          with a value of True if it is monitored by the Explore tool.
   */
  function __addHostToExploreHostList(hostName) {
      var deferred = new Deferred();

      if (__hasExploreTool()) {             
          var url = '/ibm/api/collective/v1/hosts/' + hostName;
          request.get(url).then(function(response) {
              __exploreHostList[hostName] = true;
              deferred.resolve(true);
          }), lang.hitch(this, function(err) {
              __exploreHostList[hostName] = false;
              deferred.reject(false);
          });
      } else {
          // If no Explore tool, then immediately resolve the deferred with false
          // to indicate this is NOT a server in a collective.
          __exploreHostList[hostName] = false;
          deferred.resolve(false);
      }
       
      return deferred;      
  }

  
  /**
   * 
   * @returns {String}  indicating the correct prefix to the Explore Tool.
   */
  function __getLinkPrefixToExploreTool() {
      if (__hasToolbox()) {
          return haveToolboxLinkPrefixToExplore;
      } else {
          return noToolboxLinkPrefixToExplore;
      }
  }
});
