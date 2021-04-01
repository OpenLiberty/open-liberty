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
/**
 * A ListViewAlertIcon object is a representation of the current state of a stateful resource.
 * This is the icon that appears in the Alert column in the search results list view.
 * 
 * N.B. The word state and status are used pretty interchangeably outside of the Admin Center.
 *      Outside of the Admin Center, a server and a cluster have a status. The word 'status'
 *      is used by the wlp/bin/server command and is used in the ServerCommands and ClusterManager
 *      MBeans. Applications however have a 'state', as per their application MBean. In order
 *      to keep ourselves sane in our code base, we have opted to use the word 'state' everywhere,
 *      changing API responses that return 'status' to be 'state'.
 */
define([ 'dojo/_base/declare', "jsExplore/resources/resourceManager", "jsExplore/resources/utils", 'jsExplore/utils/constants', 'dojo/dom', 'dojo/dom-construct', 'dojo/on', 'dojo/i18n!../../nls/explorerMessages', 'dojo/_base/lang', 'js/common/tr', 'jsShared/utils/imgUtils' ],
    function(declare, resourceManager, utils, constants, dom, domc, on, i18n, lang, tr, imgUtils) {

  function getNameFromTuple(tuple) {
    return tuple.substring(tuple.lastIndexOf(',')+1);
  }

  /**
   * Defines the ListViewAlertIcon. The ListViewAlertIcon is a resource-state aware entity which will auto-update
   * its icon based on state change events. While resources will update themselves, this establishes
   * its own state change event listener so that the icon is updated without need for external
   * calls.
   * The ListView is on the Search panel.
   */
  return declare('ListViewAlertIcon', [], {

    constructor: function(configObj) {
      if (!configObj) {
        tr.throwMsg('Programming Error: No configuration object passed into the constructor. Go read the JSDoc!');
      }
      this.resource = configObj.resource;
      this.id = 'searchView-' + configObj.resource.id + '-alert';
      
      this.alertString = "";  // Used to track the alert icons being displayed for this resource
                              // (alert, Maintenance Mode, etc).  It is used to determine the sort 
                              // order when the Alert column is selected for sorting.

      this.resource.subscribe(this);
    },

    destroy: function() {
      this.resource.unsubscribe(this);
      domc.destroy(this.id);
    },

    /**
     * onChange listener for maintenance mode change
     */
    onMaintenanceModeChange: function() {
      this.update();
    },
    
    onAlertsChange: function() {
      this.update();
    },
    
    update: function() {
      var domElement = dom.byId('searchView-' + this.resource.id + '-alertIcon');
      if (domElement) {
          domElement.innerHTML = this.__getImageTag();
      } 
    },
    
    getHTML: function() {
      return '<span id="' + 'searchView-' + this.resource.id + '-alertIcon' + '">' + this.__getImageTag() + '</span>';
    },

    __getImageTag : function() {
      var imgTag = "";
      this.alertString = "";            // Reset the value and update as it is determined which
                                        // icons should be displayed.
      var alertText = this.__getAlertText();
      if (alertText) {
//        imgTag = '<img src="images/status-alert-dropdown-menu-S.png" title="' + alertText + '" alt="' + alertText + '">';
        imgTag = imgUtils.getSVGSmall('status-alert', 'gridAlert', i18n.ALERT);
        this.alertString += "alert";
      }
      if (this.resource.type === 'host' || this.resource.type === 'server' || this.resource.type === 'appOnServer') {
        var mMode = this.resource.maintenanceMode;
        var spacer = "";
        if ( imgTag !== "" ) {
          // we have regular alert icon, so add a 8px spacer
          spacer = "<span style='width:8px;display:inline-block;'></span>";
        }
        //inMaintenanceMode/notInMaintenanceMode/alternateServerStarting/alternateServerUnavailable
        if ( mMode === null || mMode === constants.MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE) {
          // don't do anything since the imgTag will not have MM icon.
        } else if ( mMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE) {
          // need to set the icon to in maintenance mode
//          imgTag += spacer + '<img src="images/maintenance-mode-20-DT.png" title="' + i18n.MAINTENANCE_MODE_ENABLED + '" alt="' + i18n.MAINTENANCE_MODE_ENABLED + '">';
          imgTag += spacer + imgUtils.getSVGSmall('maintenanceMode', 'gridMaintenanceMode', i18n.MAINTENANCE_MODE_ENABLED);
          this.alertString += "MMode";
        } else if ( mMode === "alternateServerStarting" ) {
          // need to set the icon to in progress
          imgTag += spacer + '<img src="images/enabling-maintenance-mode-20-DT.gif" title="' + i18n.ENABLING_MAINTENANCE_MODE + '" alt="' + i18n.ENABLING_MAINTENANCE_MODE + '">';
          this.alertString += "enablingMMode";
        }
        
      }
      return imgTag;
    },
    
    __getAlertText : function() {
      switch (this.resource.type) {
      case 'appOnServer':
      case 'appOnCluster':  
        return this.__getApplicationAlertText(this.resource);
        break;
      case 'server':
        return this.__getServerAlertText(this.resource);
        break;
      case 'cluster':
        return this.__getClusterAlertText(this.resource);
        break;
      case 'host':
      case 'runtime':
        return this.__getHostOrRuntimeAlertText(this.resource);
        break;
      };
    },
    
    __getApplicationAlertText: function(application) {
      var alertString = '';
      if (application.alerts && application.alerts.count > 0) {
        if (application.alerts.app && application.alerts.app.length > 0) {
          var serverNames = [];
          for ( var j in application.alerts.app[0].servers) {
            serverNames.push('&nbsp;' + getNameFromTuple(application.alerts.app[0].servers[j]));
          }
          // !application.down was added in the test below because the resource object of an 
          // appOnServer does not have the down, up, or unknown properties.  So, if it is missing we know
          // the resource is an appOnServer (vs an appOnCluster) and will only ever have one instance down.  
          // Post the more specific message to indicate the appOnServer is stopped.
          if (!application.down || application.down < 3) {
            alertString += lang.replace(i18n.APP_STOPPED_ON_SERVER, [ application.name, serverNames ]) + '&nbsp;';
          } else {
            alertString += lang.replace(i18n.INSTANCES_STOPPED_ON_SERVERS, [ serverNames.length ]) + '&nbsp;';
          }
        }
        if (application.alerts.unknown && application.alerts.unknown.length > 0) {
          for ( var s in application.alerts.unknown) {
            alertString += lang.replace(i18n.UNKNOWN_STATE_APP_INSTANCES, [ application.alerts.unknown[s].id ]);
          }
        }
      }
      return alertString;
    },
    
    __getServerAlertText: function(server) {
      var alertString = "";
      if (server.alerts && server.alerts.count > 0) {
        if (server.alerts.app && server.alerts.app.length > 0) {
          if (server.alerts.app.length < 3) {
            var appNames = [];          
            for ( var a in server.alerts.app) {
              var spanClose = '</span>';
              var appName = resourceManager.getAppNameFromId(server.alerts.app[a].name);
              var clusterOrServerName = resourceManager.getClusterOrServerName(server.alerts.app[a].name);
              if (server.alerts.app[a].servers.length == 1 && clusterOrServerName === server.alerts.app[a].servers[0]) {
                // appOnServer              
                clusterOrServerName = getNameFromTuple(clusterOrServerName);
              } 
              var appDisplayName = " " + lang.replace(i18n.RESOURCE_ON_RESOURCE, [ appName, clusterOrServerName ]);
              
              appNames.push(appDisplayName);
            }
            alertString += lang.replace(i18n.INSTANCE_STOPPED_ON_SERVERS, [ appNames ]) + '&nbsp;';
          } else {
            alertString += lang.replace(i18n.APPS_NOT_RUNNING, [ server.alerts.app.length ]) + '&nbsp;';
          }
        }
        if (server.alerts.unknown && server.alerts.unknown.length > 0) {
          alertString += lang.replace(i18n.UNKNOWN_STATE, [ getNameFromTuple(server.alerts.unknown[0].id) ]);
        }
      }
      return alertString;
    },

    __getClusterAlertText : function(cluster) {
      var alertString = '';
      if (cluster.alerts && cluster.alerts.count > 0) {
        if (cluster.alerts.app && cluster.alerts.app.length > 0) {
          var serverNames = [];
          for ( var a in cluster.alerts.app) {
            for ( var j in cluster.alerts.app[a].servers) {
              serverNames.push('&nbsp;' + getNameFromTuple(cluster.alerts.app[a].servers[j]));
            }
          }
          if (serverNames.length < 3) {
            alertString += lang.replace(i18n.NOT_ALL_APPS_RUNNING, [ serverNames ]) + '&nbsp;';
          } else {
            alertString += lang.replace(i18n.NOT_ALL_APPS_RUNNING_SERVERS, [ serverNames.length ]) + '&nbsp;';
          }
        }
        if (cluster.alerts.unknown && cluster.alerts.unknown.length > 0) {
          alertString += lang.replace(i18n.UNKNOWN_STATE_CLUSTERS, [ cluster.alerts.unknown[0].id ]);
        }
      }
      return alertString;
    },

    __getHostOrRuntimeAlertText : function(hostOrRuntime) {
      var alertString = '';
      hostOrRuntime.getServers().then(function(servers) {
        if (hostOrRuntime.alerts && hostOrRuntime.alerts.count > 0) {
          var uniqueFunction = function(value, index, self) {
            return self.indexOf(value) === index;
          };
          if (hostOrRuntime.alerts.app && hostOrRuntime.alerts.app.length > 0) {
            var serverNames = [];
            for ( var a in hostOrRuntime.alerts.app) {
              for ( var j in hostOrRuntime.alerts.app[a].servers) {
                serverNames.push(getNameFromTuple(hostOrRuntime.alerts.app[a].servers[j]));
              }
            }
            var uniqueServerNames = serverNames.filter(uniqueFunction);
            if (uniqueServerNames.length < 3) {
              alertString += lang.replace(i18n.NOT_ALL_APPS_RUNNING, [ uniqueServerNames ]) + '&nbsp;';
            } else {
              alertString += lang.replace(i18n.NOT_ALL_APPS_RUNNING_SERVERS, [ uniqueServerNames.length ]) + '&nbsp;';
            }
          }
          if (hostOrRuntime.alerts.unknown && hostOrRuntime.alerts.unknown.length > 0) {
            var serverNames = [];
            for ( var s in hostOrRuntime.alerts.unknown) {
              serverNames.push(getNameFromTuple(hostOrRuntime.alerts.unknown[s].id));
            }
            var uniqueServerNames = serverNames.filter(uniqueFunction);
            if (uniqueServerNames.length < 3) {
              alertString += lang.replace(i18n.UNKNOWN_STATE, [ uniqueServerNames ]);
            } else {
              alertString += lang.replace(i18n.UNKNOWN_STATE_SERVERS, [ uniqueServerNames.length ]);
            }
          }
        }
      });
      return alertString;
    }


  });
});