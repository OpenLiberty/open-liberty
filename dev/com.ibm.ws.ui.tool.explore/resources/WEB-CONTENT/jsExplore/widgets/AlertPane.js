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
define([ 'dijit/layout/ContentPane', 'dojox/mobile/Icon', 'dojo/_base/declare', 
         'dojo/dom-construct', 'dojo/on', 'js/common/platform', 
         'dojo/i18n!../nls/explorerMessages', 'dojo/_base/lang', 'dijit/registry', 
         'jsExplore/resources/resourceManager', 'jsExplore/views/viewFactory', 
         'jsExplore/resources/utils', 'jsShared/utils/imgUtils' ], 
    function(ContentPane, Icon, declare, domConstruct, on, platform, i18n,
             lang, registry, resourceManager, viewFactory, utils, imgUtils) {

  function getNameFromTuple(tuple) {
    return tuple.substring(tuple.lastIndexOf(',') + 1);
  }
  return declare('AlertPane', [ ContentPane ], {
      id : null,
      baseClass : 'baseAlertPane',
      'class' : 'hiddenAlertPane',
      region : 'top',
      doLayout : false,
      
      constructor : function(params) {
        this.resources = [];
        this.alertsCollection = null;
      },

      /**
       * As far as I can tell, this doesn't do anything substantial...
       */
      postCreate : function() {
        this.inherited(arguments);
      },

      /**
       * When the alerts change for the resource I'm watching, recompute alerts.
       */
      onAlertsChange : function() {
        this.recomputeAlerts();
      },

      /**
       * Recomputes the complete set of alerts across all monitored resources.
       */
      recomputeAlerts : function() {
        var alertableSet = {};

        var unknownSetApps = [];
        var unknownSetServers = [];
        var unknownSetClusters = [];
        var unknownSetAppOnServer = [];
        
        var alertsCollection = this.alertsCollection;
        if (alertsCollection && alertsCollection.count > 0) {
          if (alertsCollection.unknown && alertsCollection.unknown.length > 0) {
            for (var s = 0; s < alertsCollection.unknown.length; s++) {
              var id = alertsCollection.unknown[s].id;
              switch (alertsCollection.unknown[s].type) {
              case 'server':
                unknownSetServers[id] = id;
                break;
              case 'application':
                unknownSetApps[id] = id;
                break;
              case 'appOnServer':
                unknownSetAppOnServer[id] = id;
                break;
              case 'cluster':
                unknownSetClusters[id] = id;
                break;
              }
            }
          }
          if (alertsCollection.app && alertsCollection.app.length > 0) {
            for (var a = 0; a < alertsCollection.app.length; a++) {
              var appId = alertsCollection.app[a].name;
              var appOrClusterId = resourceManager.getClusterOrServerName(appId);
              var appServers = alertsCollection.app[a].servers;
              if (alertableSet[appId]) {
                for (var f = 0; f < appServers.length; f++) {
                  var found = false;
                  for (var z = alertableSet[appId].servers.length - 1; z >= 0; z--) {
                    if (appServers[f] === alertableSet[appId].servers[z]) {
                      found = true;
                      break;
                    }
                  }
                  if (!found) {
                    alertableSet[appId].servers.push(appServers[f]);
                  }
                }
              } else {
                var appType = "appOnCluster";
                if (appServers.length == 1  &&  appOrClusterId == appServers[0]) {
                  appType = "appOnServer";
                }
                alertableSet[appId] = {
                    name : resourceManager.getAppNameFromId(appId),
                    type : appType,                   
                    id   : appId,
                    appOrClusterId: appOrClusterId,
                    servers : appServers
                };
              }
            }
          }
        }

        for ( var i in this.resources) {
          if (this.resources.hasOwnProperty(i)) {
            var resource = this.resources[i];
            if (resource.alerts && resource.alerts.count > 0) {
              if (resource.alerts.unknown && resource.alerts.unknown.length > 0) {
                for (var s = 0; s < resource.alerts.unknown.length; s++) {
                  var id = resource.alerts.unknown[s].id;
                  switch (resource.alerts.unknown[s].type) {
                  case 'server':
                    unknownSetServers[id] = id;
                    break;
                  case 'application':
                    unknownSetApps[id] = id;
                    break;
                  case 'appOnServer':
                    unknownSetAppOnServer[id] = id;
                    break;
                  case 'cluster':
                    unknownSetClusters[id] = id;
                    break;
                  }
                }
              }
              if (resource.alerts.app && resource.alerts.app.length > 0) {
                for (var a = 0; a < resource.alerts.app.length; a++) {
                  var appId = resource.alerts.app[a].name;
                  var appOrClusterId = resourceManager.getClusterOrServerName(appId);
                  var appServers = resource.alerts.app[a].servers;
                  if (alertableSet[appId]) {
                    for (var f = 0; f < appServers.length; f++) {
                      var found = false;
                      for (var z = alertableSet[appId].servers.length - 1; z >= 0; z--) {
                        if (appServers[f] === alertableSet[appId].servers[z]) {
                          found = true;
                          break;
                        }
                      }
                      if (!found) {
                        alertableSet[appId].servers.push(appServers[f]);
                      }
                    }
                  } else {
                    var appType = "appOnCluster";
                    if (appServers.length == 1  &&  appOrClusterId == appServers[0]) {
                      appType = "appOnServer";
                    }
                    alertableSet[appId] = {
                        name : resourceManager.getAppNameFromId(appId),
                        type : appType,
                        id   : appId,
                        appOrClusterId : appOrClusterId,
                        servers : appServers
                    };
                  }
                }
              }
            }
          }
        }
        // Convert the objects to arrays
        var alertableList = convertSetToList(alertableSet);
        var unknownListServers = convertSetToList(unknownSetServers);
        var unknownListApps = convertSetToList(unknownSetApps);
        var unknownListClusters = convertSetToList(unknownSetClusters);
        var unknownListAppOnServer = convertSetToList(unknownSetAppOnServer);

        this.__createContent(alertableList, unknownListServers, unknownListApps, unknownListClusters, unknownListAppOnServer);
      },
      
      /**
       * Adds alerts to this AlertPane.
       * 
       * @param Collection of alerts to display.
       */
      addAlerts: function(alertsCollection) {
        this.alertsCollection = alertsCollection;
        this.alertsCollection.subscribe(this);
        
        this.recomputeAlerts();
      },

      /**
       * Adds a resource to be watched for alerts.
       * 
       * @param resourceToWatch
       */
      addResource : function(resourceToWatch) {
        if (!this.resources[resourceToWatch.id]) {
          // Add only if we are not tracking the ID
          this.resources[resourceToWatch.id] = resourceToWatch;
          resourceToWatch.subscribe(this);
        }
        this.recomputeAlerts();
      },

      /**
       * Generates the content based on the alertableList and unknownList.
       * 
       * @param alertableList
       * @param unknownList
       * 
       */
      __createContent : function(alertableList, unknownListServers, unknownListApps, unknownListClusters, unknownListAppOnServer) {
        this.destroyDescendants(false);
        if (alertableList.length > 0 || unknownListServers.length > 0 || unknownListApps.length > 0 || unknownListClusters.length > 0
            || unknownListAppOnServer.length > 0) {
          
          var mainDiv = domConstruct.toDom('<div></div>');

          // Determine the alerts to be displayed and add them to mainDiv
          
          if (unknownListServers.length > 0) {
            this.__unknownAlerts(mainDiv, unknownListServers, 'server');
          }

          if (unknownListApps.length > 0) {
            this.__unknownAlerts(mainDiv, unknownListApps, 'application');
          }

          if (unknownListClusters.length > 0) {
            // this.__unknownAlerts(mainDiv, unknownListClusters, 'cluster');
          }

          if (unknownListAppOnServer.length > 0) {
            this.__unknownAlerts(mainDiv, unknownListAppOnServer, 'appOnServer');
          }

          if (alertableList.length > 3) {
            // We have a whole bunch of alerts, so just give a brief one-liner
            this.__multiAppOnServers(mainDiv, alertableList);
          } else {
            // this is a shorter list, so put them out one at a time
            alertableList.forEach(lang.hitch(this, function(singleAlert) {
              this.__singleAppOnServers(mainDiv, alertableList, singleAlert);
            }));
          }


          // Alerts displayed on Dashboard are formatted differently than those
          // on the overview pages.  
          // Format the alerts for the appropriate page.
       
          if (this.dashboardDisplay) {
            // alertPane on dashboard.  Add in Alert icon and heading.
            var alertImage = imgUtils.getSVGSmall('status-alert');
            var html = alertImage + '<span class="dashboardAlertHeading">'+ i18n.ALERTS + '</span>';

            var alertIcon = new ContentPane({
                id : this.id + "-alertIcon",
                content: html,
                alt : i18n.ALERTS,
                baseClass : "dashboardAlertIcon"
            });
            alertIcon.startup = function() {
            };
            this.addChild(alertIcon);
            
            var alerts = new ContentPane({
              id : this.id + "-alerts",
              baseClass : 'baseAlertListPane',
              doLayout : false,
              content : mainDiv
            });
            this.addChild(alerts);
            this.set('class', 'container dashboardAlertPane');
          
          } else {   
            // alertPane on objectViewHeaderPane.  Add in icon and orange horizontal line
            // to divide the alerts from the rest of the OVHP.
            var alertImage = imgUtils.getSVGSmall('status-alert');
            
            var alertIcon = new ContentPane({
              id : this.id + "-alertIcon",
              content : alertImage,
              alt : i18n.ALERTS,
              baseClass : "overviewAlertIcon"
            });
            alertIcon.startup = function() {
            };
            this.addChild(alertIcon);
            
            var html = domConstruct.toDom('<div style="display: inline-block; margin-left: 31px;"></div>');
            var horizontalLine = domConstruct.toDom('<div style="border-top: 1.5px solid #D74108; margin-top:10px; margin-bottom: 9px;"></div>');
            domConstruct.place(horizontalLine, html);
            domConstruct.place(mainDiv, html);
            
            var alerts = new ContentPane({
              id : this.id + "-alerts",
              baseClass : 'baseAlertListPane',
              doLayout : false,
              content: html
            });
            this.addChild(alerts);
          
            this.set('class', 'overviewAlertPane');   
          }
        } else {
          this.set('class', 'hiddenAlertPane');
        }
        if (this.parentPane) {
          this.parentPane.resize();
        }
      },
      
      // build the alertNode for a number of applications stopped on server(s)
      __multiAppOnServers : function(mainDiv, alertableList) {
        var appIns;
        if (utils.isStandalone()) {
          appIns = domConstruct.toDom("<div style='display:inline-block'>" + lang.replace(i18n.NUMBER_APPS, [ alertableList.length ])
              + "</div>");
        } else {
          appIns = domConstruct.toDom("<div style='display:inline-block;'><a class='alertLink' href='#' onclick=\"return false;\">"
              + lang.replace(i18n.NUMBER_APPS, [ alertableList.length ]) + "</a></div>");
          on(appIns, "click", openApplicationsAlerts);
        }
        this.__buildAlertNode(appIns, mainDiv, alertableList);
      },

      // build the alertNode for a single application stopped on server(s)
      // handles listing individual server links or the generic "servers" link
      __singleAppOnServers : function(mainDiv, alertableList, singleAlert) {
        var appIns;
        if (utils.isStandalone()) {
          appIns = domConstruct.toDom("<div style='display:inline-block' dir='" + utils.getStringTextDirection(singleAlert.name) + "'>" + singleAlert.name + "</div>");
        } else {
          appIns = domConstruct.toDom("<div style='display:inline-block;' dir='" + utils.getStringTextDirection(singleAlert.name) + "'><a class='alertLink' href='#' onclick=\"return false;\">" + singleAlert.name + "</a></div>");
          on(appIns, "click", function() {
            openApplicationView(singleAlert.id);
          });
        }
        this.__buildAlertNode(appIns, mainDiv, alertableList, singleAlert);
      },
      
      __buildClusterNode : function(singleAlert) {
        var clusterIns = domConstruct.toDom("<div style='display:inline-block;' dir='" + utils.getStringTextDirection(singleAlert.appOrClusterId) + "'><a class='alertLink' href='#' onclick=\"return false;\">" + singleAlert.appOrClusterId + " </a></div>");
        on(clusterIns, "click", function() {
          openClusterView(singleAlert.appOrClusterId);
        });
        return clusterIns;
      },

      // build the alertNode for a number of applications stopped on server(s)
      __buildAlertNode : function(appIns, mainDiv, alertableList, singleAlert) {
        var alertNode = domConstruct.toDom("<div></div>");

        var message = i18n.INSERT_STOPPED_ON_INSERT;
        if (singleAlert) {
          if (singleAlert.type == "appOnServer") {
            message = i18n.APPSERVER_STOPPED_ON_SERVER;           
          } else {   // appOnCluster 
            message = i18n.APPCLUSTER_STOPPED_ON_SERVER;
          }
        }
 
        // any part before the first insert
        var msgPart1 = message.substring(0, message.indexOf("{")); // part before first insert
        if (msgPart1.length > 0) {
          domConstruct.place(domConstruct.toDom(msgPart1), alertNode);
        }

        // first insert
        message = message.substring(msgPart1.length);
        var insert1 = message.substring(0, 3);
        this.__constructInsert(insert1, appIns, alertNode, alertableList, singleAlert);

        // any part between the inserts 1 & 2
        message = message.substring(3);
        var msgPart2 = message.substring(0, message.indexOf("{"));// part between inserts 1 & 2
        if (msgPart2.length > 0) {
          domConstruct.place(domConstruct.toDom(msgPart2), alertNode);
        }
        
        // second insert
        message = message.substring(msgPart2.length);
        var insert2 = message.substring(0, 3);
        this.__constructInsert(insert2, appIns, alertNode, alertableList, singleAlert);

        // any part after the second insert
        message = message.substring(3); // part after second insert
        if (message.length > 0) {
          if (message.indexOf("{") >= 0) {   // Is there a third insert?            
            var msgPart3 = message.substring(0, message.indexOf("{")); // part between inserts 2 & 3
            if (msgPart3.length > 0) {              
              domConstruct.place(domConstruct.toDom(msgPart3), alertNode);
            }
            
            // third insert
            message = message.substring(msgPart3.length);
            var insert3 = message.substring(0,3);
            this.__constructInsert(insert3, appIns, alertNode, alertableList, singleAlert);
            
            // Anything following the third insert?
            message = message.substring(3);
            if (message.length > 0) {  
              domConstruct.place(domConstruct.toDom(message), alertNode);
            }
          } else {  // No insert 3, just add the rest of the message
            domConstruct.place(domConstruct.toDom(message), alertNode);
          }
        }
        
        domConstruct.place(alertNode, mainDiv);
      },
      
      /**
       * The alert messages are comprised of 2 or 3 variable inserts.  This method inserts the 
       * correct text into the correct spot as follows:
       *      {0}:  appName or number of applications as dictated by appIns
       *      {1}:  serverName for APPSERVER_STOPPED_ON_SERVER
       *            clusterName for APPCLUSTER_STOPPED_ON_SERVER
       *            'servers' for INSERT_STOPPED_ON_INSERT
       *      {2}:  serverName(s) for APPCLUSTER_STOPPED_ON_SERVER  
       * @param insert - parameter 0, 1, or 2
       * @param appIns - The application insert (appName or number of applications if >3)
       * @param alertNode - The node consisting of the concatenation of the message and inserts
       * @param alertableList - Server names the application is stopped on
       * @param singleAlert - Information for a single application (<3) stopped
       */
      __constructInsert : function(insert, appIns, alertNode, alertableList, singleAlert) {        
        if (insert === "{0}") {    // Application insert
          domConstruct.place(appIns, alertNode);
        } else if ( !singleAlert ||                     // Server insert
                   (insert === "{1}" && singleAlert.type == 'appOnServer') ||
                   (insert === "{2}")) {
          // add servers
          if (alertableList.length > 3) {
            var serverIns;
            if (utils.isStandalone()) {
              serverIns = domConstruct.toDom("<div style='display:inline-block'>" + i18n.SERVERS_INSERT + "</div>");
            } else {
              serverIns = domConstruct.toDom("<div style='display:inline-block;'><a class='alertLink' href=\"#\" onclick=\"return false;\">" + i18n.SERVERS_INSERT + "</a></div>");
              on(serverIns, "click", openServersAlerts);
            }
            domConstruct.place(serverIns, alertNode);
          } else {
            this.__singleAppAddServersToAlert(alertNode, singleAlert);
          }
        } else if ((insert === "{1}" && singleAlert.type == 'appOnCluster')) {
          // add cluster
          var clusterIns = this.__buildClusterNode(singleAlert);
          domConstruct.place(clusterIns, alertNode);
        }
      },

      /**
       * Creation of the Unknown Alerts string.
       * Less than 3 unknown is clickable to single resource page.
       * Greater than 3 unknown is clickable to all resource page.
       * 
       * @param mainDiv
       * @param unknownList - list of alerts
       * @param type - resource type
       */
      __unknownAlerts : function(mainDiv, unknownList, type) {
        var unknownAlertNode = domConstruct.toDom("<div></div>");
        var message = i18n.UNKNOWN_STATE;
        
        if (unknownList.length > 3) {
          switch (type) {
            case 'server':
              message = i18n.UNKNOWN_STATE_SERVERS;
              break;
            case 'application':
              message = i18n.UNKNOWN_STATE_APPS;
              break;
            case 'cluster':
              message = i18n.UNKNOWN_STATE_CLUSTERS;
              break;
            case 'appOnServer':
              message = i18n.UNKNOWN_STATE_APP_INSTANCES;
              break;
          }
        }

        // any part before the first insert
        var msgPart1 = message.substring(0, message.indexOf("{")); // part before first insert
        if (msgPart1.length > 0) {
          domConstruct.place(domConstruct.toDom(msgPart1), unknownAlertNode);
        }

        // first insert
        message = message.substring(msgPart1.length);

        // add resource number or resources, depending on message.
        if (unknownList.length > 3) {
          var resourceIns;
          resourceIns = domConstruct.toDom("<div style='display:inline-block;'><a class='alertLink' href=\"#\" onclick=\"return false;\">" + unknownList.length + "</a></div>");
          on(resourceIns, "click", openServersAlerts);
          domConstruct.place(resourceIns, unknownAlertNode);
        } else {
          for (var j = 0; j < unknownList.length; j++) {
            var seperator = unknownList.length === j + 1 ? "&nbsp;" : ",&nbsp;&nbsp;";
            switch (type) {
            case 'server':
              var displayName = unknownList[j].substring(unknownList[j].lastIndexOf(',') + 1);
              var serverId = unknownList[j];
              var serverIns = domConstruct.toDom("<div style='display:inline-block;'><a class='alertLink' href=\"#\" onclick=\"return false;\">" + displayName + "</a>" + seperator
                  + "</div>");

              on(serverIns, "click", function() {
                openServerView(serverId);
              });
              domConstruct.place(serverIns, unknownAlertNode);
              break;
            case 'application':
              var displayName = unknownList[j];
              var appIns = domConstruct.toDom("<div style='display:inline-block;'><a class='alertLink' href=\"#\" onclick=\"return false;\">" + displayName + "</a>" + seperator
                  + "</div>");
              on(appIns, "click", function() {
                openApplicationView(displayName);
              });
              domConstruct.place(appIns, unknownAlertNode);
              break;
            case 'appOnServer':
              var displayName = unknownList[j];
              var appOnServerIns = domConstruct.toDom("<div style='display:inline-block;'><a class='alertLink' href=\"#\" onclick=\"return false;\">" + displayName + "</a>"
                  + seperator + "</div>");
              on(appOnServerIns, "click", function() {
                openAppOnServerView(displayName);
              });
              domConstruct.place(appOnServerIns, unknownAlertNode);
              break;
            case 'cluster':
              var displayName = unknownList[j];
              var clusterIns = domConstruct.toDom("<div style='display:inline-block;'><a class='alertLink' href=\"#\" onclick=\"return false;\">" + displayName + "</a>" + seperator
                  + "</div>");
              on(clusterIns, "click", function() {
                openClusterView(displayName);
              });
              domConstruct.place(clusterIns, unknownAlertNode);
              break;
            }
          }

        }

        // any part between the inserts
        var msgPart3 = message.substring(3); // part after second insert
        if (msgPart3.length > 0) {
          domConstruct.place(domConstruct.toDom(msgPart3), unknownAlertNode);
        }
        domConstruct.place(unknownAlertNode, mainDiv);
      },

      __singleAppAddServersToAlert : function(alertNode, singleAlert) {
        if (singleAlert.servers.length > 3) {
          // We have a ton of affected servers, just shortcut them
          var serverIns;
          if (utils.isStandalone()) {
            serverIns = domConstruct.toDom("<div style='display:inline-block'>" + i18n.SERVERS_INSERT + "</div>");
          } else {
            serverIns = domConstruct.toDom("<div style='display:inline-block;'><a class='alertLink' href=\"#\" onclick=\"return false;\">" + i18n.SERVERS_INSERT + "</a></div>");
            on(serverIns, "click", openServersAlerts);
          }
          domConstruct.place(serverIns, alertNode);
        } else {
          singleAlert.servers.forEach(lang.hitch(this, function(serverId, index) {
            var serverIns;
            if (utils.isStandalone()) {
              serverIns = domConstruct.toDom("<div style='display:inline-block'>" + getNameFromTuple(serverId) + "&nbsp;</div>");
            } else {
              var seperator = singleAlert.servers.length === index + 1 ? "" : ",";
              serverIns = domConstruct.toDom("<div style='display:inline-block;'><a class='alertLink' href=\"#\" onclick=\"return false;\">" + getNameFromTuple(serverId) + "</a>"
                  + seperator + "&nbsp;&nbsp;</div>");
              on(serverIns, "click", function() {
                openServerView(serverId);
              });
            }
            domConstruct.place(serverIns, alertNode);
          }));
        }
      },

      destroy : function() {
        // Unsubscribe from all watched resources
        for ( var r in this.resources) {
          if (this.resources.hasOwnProperty(r)) {
            this.resources[r].unsubscribe(this);
          }
        }
        if (this.alertsCollection) {
          this.alertsCollection.unsubscribe(this);
        }
        this.inherited(arguments);
      }

  });

  /**
   * Converts a 'set' (an Object of name value pairs) where the name is unique and value is what we want in the list.
   */
  function convertSetToList(set) {
    var list = [];
    for ( var e in set) {
      if (set.hasOwnProperty(e)) {
        list.push(set[e]);
      }
    }
    return list;
  }

  /**
   * Opens the Applications view with the Alert filter selected.
   */
  function openApplicationsAlerts() {
    resourceManager.getApplications().then(function(apps) {
      viewFactory.openView(apps, 'Alert');
    });
  }

  /**
   * Opens the Servers view with the Alert filter selected.
   */
  function openServersAlerts() {
    resourceManager.getServers().then(function(servers) {
      viewFactory.openView(servers, 'Alert');
    });
  }

  /* Opens AppOnServer alerts */

  function openAppOnServerAlerts() {
    resourceManager.getServers().then(function(servers) {
      viewFactory.openView(servers, 'Alert');
    });
  }

  /**
   * Opens an Application view for the given application.
   * 
   * applicationId: {tuple|cluster},appName
   */
  function openApplicationView(applicationId) {
    // TODO: We really should be passing around the resource objects, not just names
    resourceManager.getApplication(applicationId).then(function(app) {
      viewFactory.openView(app);
    });
  }

  /**
   * Opens a Server view for the given server.
   */
  function openServerView(serverName) {
    resourceManager.getServer(serverName).then(function(server) {
      viewFactory.openView(server);
    });
  }

  /**
   * Opens a Cluster view for the given cluster.
   */
  function openClusterView(clusterName) {
    resourceManager.getCluster(clusterName).then(function(cluster) {
      viewFactory.openView(cluster);
    });
  }

  function openAppOnServerView(appOnServerName) {
    var ind = appOnServerName.lastIndexOf("-");
    var serverId = appOnServerName.substring(0, ind);
    var appName = appOnServerName.substring(ind + 1);

    resourceManager.getServer(serverId).then(function(server) {
      server.getApps().then(function(appsOnServer) {
        var found = null;
        for (var x = 0; x < appsOnServer.list.length; x++) {
          if (appsOnServer.list[x].name === appName) {
            found = appsOnServer.list[x];
            break;
          }
        }
        if (found) {
          viewFactory.openView(found);
        }

      });

    });
  }

});