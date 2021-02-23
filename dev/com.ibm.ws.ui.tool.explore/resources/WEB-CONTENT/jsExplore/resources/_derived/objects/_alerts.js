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
 * Set of operations to construct alerts object and augment the container object.
 * 
 * Alerts Object: <code>
 * { count: int,
 *   unknown: [ { name: 'the resource name',
 *                type: 'the resource type' } ],
 *   app: [ { name: 'the app ID == serverTuple|clusterName,appName',
 *            servers: [ name : 'short name', fullName : 'full name (tuple) of the server' ] } ]
 * }
 * </code>
 * 
 * @author Michael Thompson <mcthomps@us.ibm.com>
 * @module resources/alerts
 * 
 * A word on referencing: calls like server.apps here is 'safe' because we are only running after the resource has been constructed or
 * populated. Normally, in external calls, server.apps is referenced by server.getApps(), but because we are in the control of our call
 * sequence, we can do this safely.
 * 
 * @return {Object} Containing all the alerts methods
 */
define([],
    /**
     * @exports resources/_alerts
     * @return alert
     */
    function() {
  return {
    /**
     * @param {Object} to The object to merge from into
     * @param {Object} from The object from which the alerts will be merged
     */
    merge: function(to, from) {
      if (!from) {
        return;
      }
      
      var i, j;
      for(i = 0; i < from.unknown.length; i++) {
        var found = false;
        for(j = 0; j < to.unknown.length; j++) {
          if ((to.unknown[j].id === from.unknown[i].id) && (to.unknown[j].type === from.unknown[i].type)) {
            found = true;
          }
        }
        if (!found) {
          to.unknown.push(from.unknown[i]);
          to.count++;
        }
      }
      
      for(i = 0; i < from.app.length; i++) {
        var found = false;
        for(j = 0; j < to.app.length; j++) {
          if ((to.app[j].name === from.app[i].name)) {
            found = true;
            // We've found the app, now make sure all servers are listed
            var ii, jj;
            for (ii = 0; ii < from.app[i].servers.length; ii++) {
              var sFound = false;
              for (jj = 0; jj < to.app[j].servers.length; jj++) {
                if (to.app[j].servers[jj] === from.app[i].servers[ii]) {
                  sFound = true;
                }
              }
              if (!sFound) {
                to.app[j].servers.push(from.app[i].servers[ii]);
              }
            }
          }
        }
        if (!found) {
          to.app.push(from.app[i]);
          to.count++;
        }
      }
    },
    
    combineAlerts : function(resource, server) {
      var type = resource.type;
      // Handled types: runtime
      switch (type) {
      case 'runtime':
        delete resource.alerts;
        __combineAlertsRuntime(resource);
        break;
      default:
        console.error('Alerts are not supported for the type ' + type);
      }
    },

    /**
     * Add a '.alerts' attribute to the given object if there are alert conditions which should be reported.
     * 
     * @return VOID
     */
    addAlerts : function(resource) {
      delete resource.alerts;
      var type = resource.type;
      // Handled types: application, server, host, cluster
      switch (type) {
      case 'appOnServer':
        __addAlertsAppOnServer(resource);
        break;
      case 'cluster':
        __addAlertsCluster(resource);
        break;
      case 'host':
        __addAlertsHost(resource);
        break;
      case 'server':
        __addAlertsServer(resource);
        break;
      case 'runtime':
        __addAlertsRuntime(resource);
        break;
      case 'appOnCluster':
        __addAlertsAppOnCluster(resource);
        break;
      default:
        console.debug('Alerts are not supported for the type ' + type);
      }
    }
  };

  /**
   * Creates an initialized alerts object.
   * 
   * @private
   * @return {Object} an initialized alerts object.
   */
  function __init() {
    var alerts = {};
    alerts.count = 0;
    alerts.unknown = [];
    alerts.app = [];
    alerts.__appAlerts = {}; // Format: 'appName': [ 'serverTuple', ... ]
    return alerts;
  }

  /**
   * Merges an alerts object into another object, preserving or augmenting all of the field values.
   * 
   * @private
   * @param {Object}
   *          source The source of the data to merge into the destination
   * @param {Object}
   *          destination The destination to merge the source into
   */
  function __merge(source, destination) {
    if(source.unknown.length>0){
      destination.count += source.unknown.length;
      destination.unknown = destination.unknown.concat(source.unknown);
    }
    for ( var p in source.__appAlerts) {
      if (source.__appAlerts.hasOwnProperty(p)) {
        if (destination.__appAlerts[p]) {
          destination.__appAlerts[p] = destination.__appAlerts[p].concat(source.__appAlerts[p]);
        } else {
          destination.__appAlerts[p] = source.__appAlerts[p];
        }
      }
    }
  }

  /**
   * Adds the relevant alerts object fields to the container object.
   * 
   * @private
   * @param {Object}
   *          alerts - alerts object
   * @param {Object}
   *          destination - container object to which the alerts will be added
   */
  function __add(alerts, destination) {
    // Process any appAlerts we may have found
    for ( var k in alerts.__appAlerts) {
      if (alerts.__appAlerts.hasOwnProperty(k)) {
        alerts.app.push({
          name : k,
          servers : (function() {
            var servs = new Array();
            for ( var serv in alerts.__appAlerts[k]) {
              if (alerts.__appAlerts[k][serv] != null) {
                servs.push(alerts.__appAlerts[k][serv]);
              }
            }
            return servs;
          })()
        });
        alerts.count++;
      }
    }

    if (alerts && alerts.count > 0) {
      if(destination.type !== "application"){
        destination.alerts = {};
      }
      destination.alerts.count = alerts.count;
      if (alerts.unknown.length > 0) {
        destination.alerts.unknown = alerts.unknown;
      }
      if (alerts.app.length > 0) {
        destination.alerts.app = alerts.app;
      }
    }
  }

  /**
   * Determines if the server's state is an alert condition ('NOT_FOUND' state).
   * 
   * @private
   * @param {Object}
   *          alerts - alerts object
   * @param {Object}
   *          server - The servers object to process. Expected format: <code>
   *     { state, fullName, name, ... }
   *     </code>
   */
  function __findServerAlerts(alerts, server) {
    if (server.state === 'NOT_FOUND') {
      alerts.unknown.push({
        name : server.name,
        fullName : server.fullName,
        type : 'server'
      });
      alerts.count++;
    }
    if (server.apps) { // If we have no apps, skip this!
      if ((server.apps.down > 0 && server.state === 'STARTED') || server.apps.unknown > 0) {
        for (var i = 0; i < server.apps.list.length; i++) {
          if (server.apps.list[i].state === 'UNKNOWN') {
            alerts.unknown.push({
              name : server.apps.list[i].name,
              type : 'application'
            });
            alerts.count++;
          } else if (server.apps.list[i].state !== 'STARTED' && server.apps.list[i].state !== 'PARTIALLY_STARTED') {
            alerts.app.push({
              name : server.apps.list[i].name,
              servers : [ {
                name : server.name,
                fullName : server.fullName
              } ]
            });
            alerts.count++;
          }
        }
      }
    }
  }

  /**
   * Finds any servers which have the 'unknown' ('NOT_FOUND') state.
   * 
   * @private
   * @param {Object}
   *          alerts - alerts object
   * @param {Object}
   *          servers - The servers object to process. Expected format: <code>
   *     { up, down, unknown, list: [{state, fullName, name, ...}] }
   *     </code>
   */
  function __findServersAlerts(alerts, servers) {
    if (servers.unknown > 0 && servers.list) {
      for (var i = 0; i < servers.list.length; i++) {
        if (servers.list[i].state === 'NOT_FOUND') {
          alerts.unknown.push({
            name : servers.list[i].name,
            fullName : servers.list[i].fullName,
            type : 'server'
          });
          alerts.count++;
        }
      }
    }
  }

  /**
   * Finds any applications which have the 'unknown' state, or any application instances which have the 'unknown' state, or any applications
   * which are stopped on running servers.
   * 
   * @private
   * @param {Object}
   *          alerts - alerts object
   * @param {Object}
   *          apps - The applications object to process. Expected format: <code>
   *     { up, down, partial, unknown, 
   *       list: [{ name, state, down, up, unknown,
   *                hostServers:[ {name, status, appState} ]
   *             }]
   *     }
   *     </code>
   */
  function __findAppsAlerts(alerts, apps) {
    // If everything is running then no work!
    if (apps.unknown > 0 || apps.down > 0 || apps.partial > 0) {
      for (var i = 0; i < apps.list.length; i++) {
        var app = apps.list[i];
        __findAppAlerts(alerts, app);
      }
    }
  }

  /**
   * Finds any alerts for the application. An application can have the following alerts: 'unknown' state, or any application instances which
   * have the 'unknown' state, or any applications which are stopped on running servers.
   * 
   * @private
   * @param {Object}
   *          alerts - alerts object
   * @param {Object}
   *          apps - The applications object to process. Expected format: <code>
   *     { name, state, down, up, unknown,
   *       hostServers:[ {name, status, appState} ] }
   *     </code>
   */
  function __findAppAlerts(alerts, app) {
    if (app.state === 'UNKNOWN') {
      alerts.unknown.push({
        name : app.name,
        type : 'application'
      });
      alerts.count++;
    }
    if (app.unknown > 0 || app.down > 0) {
      for (var j = 0; j < app.hostServers.length; j++) {
        var appHost = app.hostServers[j];
        var servName = appHost.fullName;
        if (!appHost.fullName) {
          servName = appHost.name;
        }
        if (appHost.appState === 'UNKNOWN') {
          alerts.unknown.push({
            name : servName + '-' + app.name,
            type : 'appOnServer'
          });
          alerts.count++;
        }
        // This really is 'status'
        if (appHost.status === 'STARTED' && (appHost.appState !== 'STARTED' && appHost.appState !== 'UNKNOWN')) {
          if (!alerts.__appAlerts[app.name]) {
            alerts.__appAlerts[app.name] = [];
          }
          // due to inconsistency in the population of hostServers, need to
          // take into account if fullName is not set when creating alert for host.
          if (appHost.fullName) {
            alerts.__appAlerts[app.name].push(appHost.fullName);
          } else if (appHost.name) {
            alerts.__appAlerts[app.name].push(appHost.name);
          }
        }
      }
    }
  }

  /**
   * Processes a cluster object looking for alert conditions.
   * 
   * @private
   * @param {Object}
   *          alerts - alerts object
   * @param {Object}
   *          cluster - the cluster object
   */
  function __findClusterAlerts(alerts, cluster) {
    if (cluster.state === 'UNKNOWN') {
      alerts.unknown.push({
        name : cluster.name,
        type : 'cluster'
      });
      alerts.count++;
    }
    __findServersAlerts(alerts, cluster.servers);
    __findAppsAlerts(alerts, cluster.apps);
  }

  /**
   * TODO: This is not being used... remove? Find any clusters which have an 'UNKNOWN' state and flag those alerts.
   * 
   * @private
   * @param {Object}
   *          alerts - alerts object
   * @param {Object}
   *          clusters - the clusters object
   */
  function __findClustersAlerts(alerts, clusters) {
    if (clusters.unknown > 0) {
      console.log("+++++++++++Cluster alerts are added");
      for (var i = 0; i < clusters.list.length; i++) {
        if (clusters.list[i].state === 'UNKNOWN') {
          alerts.unknown.push({
            name : clusters.list[i].name,
            type : 'cluster'
          });
          alerts.count++;
        }
      }
    }
  }

  /**
   * Processes a host object looking for server and application alerts.
   * 
   * @private
   * @param {Object}
   *          alerts - alerts object
   * @param {Object}
   *          host - the host object
   */
  function __findHostAlerts(alerts, host) {
    __findServersAlerts(alerts, host.servers);
    __findAppsAlerts(alerts, host.apps);
  }

  /**
   * Processes a runtime object looking for server alerts.
   * 
   * @private
   * @param {Object}
   *          alerts - alerts object
   * @param {Object}
   *          runtime - the runtime object
   */
  function __findRuntimeAlerts(alerts, runtime) {
    if (runtime.servers.list) {
      for (var i = 0; i < runtime.servers.list.length; i++) {
        __findServerAlerts(alerts, runtime.servers.list[i]);
      }
    }
  }

  /**
   * @param app
   *          an AppOnServer object
   */
  function __addAlertsAppOnServer(appOnServer) {
    var as = __init();

    appOnServer.getApplication().then(function(app) {

      if (app.unknown > 0 || app.down > 0 || app.partial > 0) {
        var a = __init();
        __findAppAlerts(a, app);
        __add(a, app);
        __merge(a, as);
      }
      __add(as, appOnServer);
    });

  }
  ;

  /**
   * @param cluster
   *          a Cluster object
   */
  function __addAlertsCluster(cluster) {
    var a = __init();
    __findClusterAlerts(a, cluster);
    __add(a, cluster);
  }

  /**
   * @param app
   *          an AppOnCluster object
   */
  function __addAlertsAppOnCluster(appOnCluster) {
    var as = __init();

    console.log("appOnCluster: ", appOnCluster);
    appOnCluster.getApplication().then(function(app) {
      console.log("app: ", app);
      if (app.unknown > 0 || app.down > 0 || app.partial > 0) {
        var a = __init();
        __findAppAlerts(a, app);
        __add(a, app);
        __merge(a, as);
      }
      console.log("alert: ", a);
      console.log("as: ", as);
      __add(as, appOnCluster);
      console.log("appOnCluster after: ", appOnCluster);
    });

  }
  ;

  /**
   * @param host
   *          a Host object
   */
  function __addAlertsHost(host) {
    var a = __init();
    __findHostAlerts(a, host);
    __add(a, host);
  }
  ;

  /**
   * @param server
   *          a Server object
   */
  function __addAlertsServer(server) {
    var a = __init();
    __findServerAlerts(a, server);
    __add(a, server);
  }

  /**
   * @param runtime
   *          a Runtime object
   */
  function __addAlertsRuntime(runtime) {
    var a = __init();
    __findRuntimeAlerts(a, runtime);
    __add(a, runtime);
  }

  /**
   * @param runtime
   *          a Runtime object
   */
  function __combineAlertsRuntime(runtime) {
    var alerts = __init();
    if (runtime.servers.list) {
      for (var i = 0; i < runtime.servers.list.length; i++) {
        var server = runtime.servers.list[i];
        if (server.alerts && server.alerts.count > 0) {
          var serverAlerts = __init();
          // Store all of the unknown alerts. This is always safe to do since they can not overlap
          if (server.alerts.unknown && server.alerts.unknown.length > 0) {
            Array.prototype.push.apply(serverAlerts.unknown, server.alerts.unknown);
            serverAlerts.count += server.alerts.unknown.length;
          }
          if (server.alerts.app && server.alerts.app.length > 0) {
            for (var j = 0; j < server.alerts.app.length; j++) {
              var appAlert = server.alerts.app[j];
              serverAlerts.__appAlerts[appAlert.name] = appAlert.servers;
            }
          }
          __merge(serverAlerts, alerts);
        }
      }
    }
    __add(alerts, runtime);
  }

  /**
   * @param cluster
   *          a Cluster object
   */
  function __combineAlertsCluster(cluster, server) {
    var clusterAlerts = cluster.alerts;
    if (!clusterAlerts) {
      clusterAlerts = __init();
    }
    if (server.alerts && server.alerts.count > 0) {
      var serverAlerts = __init();
      // Store all of the unknown alerts. This is always safe to do since they can not overlap
      if (server.alerts.unknown && server.alerts.unknown.length > 0) {
        Array.prototype.push.apply(serverAlerts.unknown, server.alerts.unknown);
        serverAlerts.count += server.alerts.unknown.length;
      }
      if (server.alerts.app && server.alerts.app.length > 0) {
        for (var j = 0; j < server.alerts.app.length; j++) {
          var appAlert = server.alerts.app[j];
          serverAlerts.__appAlerts[appAlert.name] = appAlert.servers;
        }
      }
      __merge(serverAlerts, clusterAlerts);
    }
    __add(clusterAlerts, cluster);
  }
  
  /**
   * @param application
   *          a Application object
   */
  function __combineAlertsApplication(application, server) {
    var applicationAlerts = application.alerts;
    if (server.alerts && server.alerts.count > 0) {
      var serverAlerts = __init();
      if (server.alerts.unknown && server.alerts.unknown.length > 0) {
        for (var j = 0; j < server.alerts.unknown.length; j++) {
          if(server.alerts.unknown[j].type === "application")
            {
              serverAlerts.unknown.add(server.alerts.unknown[j]);
              serverAlerts.count++;
            }
        }
      }
      if (server.alerts.app && server.alerts.app.length > 0) {
        for (var j = 0; j < server.alerts.app.length; j++) {
          if(server.alerts.app[j].name == application.id)
            {
              var appAlert = server.alerts.app[j];
              serverAlerts.__appAlerts[appAlert.name] = appAlert.servers;
            }
        }
      }
      __merge(serverAlerts, applicationAlerts);
    }
    __add(applicationAlerts, application);
  }


});
