/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Individual, testable module used by the unifiedChangeListener.
 */
define([ '../_topicUtil', '../resourceManager', './uclCommon'],
    function(topicUtil, resourceManager, uclCommon) {

  /**
   * Compare two server objects to detect changes.
   *
   * @param {Object} cachedServer The server stored in the JSON cache
   * @param {Object} nowServer The server returned by the REST API
   * @return {Object} The detected changes, or null if no changes
   */
  function computeServerChanges(cachedServer, nowServer) {
    var changes = {};
    var hasChanged = false;
    defineArrayCompare();

    if (nowServer.state != cachedServer.state) {
      changes.state = nowServer.state;
      hasChanged = true;
    }
    if (nowServer.wlpInstallDir != cachedServer.wlpInstallDir) {
      changes.wlpInstallDir = nowServer.wlpInstallDir;
      hasChanged = true;
    }
    if (nowServer.cluster != cachedServer.cluster) {
      changes.cluster = nowServer.cluster;
      hasChanged = true;
    }
    if (nowServer.scalingPolicy != cachedServer.scalingPolicy) {
      changes.scalingPolicy = nowServer.scalingPolicy;
      hasChanged = true;
    }
    if (nowServer.scalingPolicyEnabled != cachedServer.scalingPolicyEnabled) {
      changes.scalingPolicyEnabled = nowServer.scalingPolicyEnabled;
      hasChanged = true;
    }
    if (nowServer.isCollectiveController != cachedServer.isCollectiveController) {
      changes.isCollectiveController = nowServer.isCollectiveController;
      hasChanged = true;
    }

    if (nowServer.maintenanceMode != cachedServer.maintenanceMode) {
      changes.maintenanceMode = nowServer.maintenanceMode;
      hasChanged = true;
    }

    if (!nowServer.hasOwnProperty('explorerURL')) { nowServer.explorerURL = null; }
    if (nowServer.explorerURL != cachedServer.explorerURL) {
      changes.explorerURL = nowServer.explorerURL;
      hasChanged = true;
    }

    if (!nowServer.ports.httpPorts.equals(cachedServer.ports.httpPorts) || !nowServer.ports.httpsPorts.equals(cachedServer.ports.httpsPorts) ) {
      changes.ports = nowServer.ports;
      hasChanged = true;
    }

    var newApps = [];
    var changedApps = [];
    var removedApps = [];

    // Clone the apps list, so we can mark them inspected or not
    var cachedApps = (cachedServer.apps && cachedServer.apps.list) ? cachedServer.apps.list : [];
    cachedApps = JSON.parse(JSON.stringify(cachedApps));

    // Compare the apps
    if (nowServer.apps) {
      // Need to compare the apps I have to the apps that are cached
      for (var a = 0; a < nowServer.apps.list.length; a++) {
        var nowApp = nowServer.apps.list[a];
        var cachedApp = uclCommon.findNamedObjectInList(cachedApps, nowApp.name);
        if (!cachedApp) {
          newApps.push(nowApp); // In this case, we push the app object
        } else {
          cachedApp.inspected = true;
          if (nowApp.state !== cachedApp.state) {
            changedApps.push(nowApp); // In this case, we push the app object
          }
          var tagChanges = {};
          if (uclCommon.compareMetadata(cachedApp, nowApp, tagChanges)) {
            changedApps.push(nowApp);
          }
        }
      }
    }

    // Iterate over the cloned cached apps to see if something was missed
    if (cachedApps.length > 0) {
      for (var a = 0; a < cachedApps.length; a++) {
        var app = cachedApps[a];
        if (!app.inspected) {
          removedApps.push(app.name); // In this case, we push ONLY the app name
        }
      }
    }

    // If anything changed, then update the changes object
    if (newApps.length > 0 || changedApps.length > 0 || removedApps.length > 0) {
      hasChanged = true;
      // Set the app tallies to their current value
      if (nowServer.apps) {
        changes.apps = {
            up : nowServer.apps.up,
            down : nowServer.apps.down,
            unknown : nowServer.apps.unknown
        };
      } else {
        changes.apps = {
            up : 0,
            down : 0,
            unknown : 0
        };
      }
      if (newApps.length > 0) {
        changes.apps.added = newApps;
      }
      if (changedApps.length > 0) {
        changes.apps.changed = changedApps;
      }
      if (removedApps.length > 0) {
        changes.apps.removed = removedApps;
      }
    }

    // Compare tags, owner and contacts
    hasChanged = uclCommon.compareMetadata(cachedServer, nowServer, changes) || hasChanged;
    hasChanged = uclCommon.compareAlerts(cachedServer, nowServer, changes) || hasChanged;

    if (hasChanged) {
      return changes;
    } else {
      return null;
    }
  };

  function defineArrayCompare() {
    // attach the .equals method to Array's prototype to call it on any array
    Array.prototype.equals = function (array) {
        // if the other array is a falsy value, return
        if (!array) {
            return false;
        }

        // compare lengths - can save a lot of time
        if (this.length != array.length) {
            return false;
        }

        for (var i = 0, l=this.length; i < l; i++) {
            // Check if we have nested arrays
            if (this[i] instanceof Array && array[i] instanceof Array) {
                // recurse into the nested arrays
                if (!this[i].equals(array[i])) {
                    return false;
                  }
            }
            else if (this[i] != array[i]) {
                // Warning - two different object instances will never be equal: {x:20} != {x:20}
                return false;
            }
        }
        return true;
    }
    // Hide method from for-in loops
    Object.defineProperty(Array.prototype, "equals", {enumerable: false});
  }

  /**
   * Checks if there are any maintenance mode changes
   */
  function hasMaintenanceModeChange(nowServer) {
    // Find the cached server and compare with current server
    var hasChange = false;
    var cachedServer = resourceManager.getCached('server', nowServer.id);
    if ( cachedServer && nowServer.maintenanceMode != cachedServer.maintenanceMode ) {
        hasChange = true;
    }
    return hasChange;
  };

  /**
   * Compares the current server's attributes with the attributes of the server in the cache, if that server is present.
   *
   * @param {Object}
   *          nowServer The current server to compare with the cached server
   */
  function sendServerChangeEvent(nowServer) {
    // Find the cached server and compare with current server
    var cachedServer = resourceManager.getCached('server', nowServer.id);
    if (cachedServer) {
      var changes = computeServerChanges(cachedServer, nowServer);
      if (changes) {
        changes.type = 'server';
        changes.id = nowServer.id;
        changes.origin = 'uclServers.js:146';
        topicUtil.publish(topicUtil.getTopicByType('server', nowServer.id), changes);
      }
    }
  };

  function sendServersChangeEvents(now) {
    var cachedServers = resourceManager.getCached('servers');
    if (!cachedServers) {
      // If we do not have the Servers collection cached, then there is nothing to do here!
      return;
    }

    var serversChanges = uclCommon.compareTwoLists(cachedServers.list, now.servers.ids);

    // Step through all of the servers in the current request and compare against the cache
    // This will send individual change notifications for anything that is cached
    for (var i = 0; i < now.servers.list.length; i++) {
      var server = now.servers.list[i];
      sendServerChangeEvent(server);
    }

    // Step through all of the servers that were removed and send them the 'removed' event
    if (serversChanges) {
      for (var j = 0; j < serversChanges.removed.length; j++) {
        var removedServer = serversChanges.removed[j];
        if (resourceManager.getCached('server', removedServer)) {
          // If its cached, remove it
          topicUtil.publish(topicUtil.getTopicByType('server', removedServer), {
            type : 'server',
            id : removedServer,
            state : 'removed',
            origin : 'uclServers.js:178'
          });
        }
      }
    }

    // Compare the cached Hosts collection against the current values
    // FIXME: server->client side mapping: list->ids
    if (cachedServers.up !== now.servers.up || cachedServers.down !== now.servers.down
        || cachedServers.unknown !== now.servers.unknown || serversChanges) {
      var changes = {
          type : 'servers',
          up : now.servers.up,
          down : now.servers.down,
          unknown : now.servers.unknown,
          origin : 'uclServers.js:210'
      };
      if (serversChanges && serversChanges.added.length > 0) {
        changes.added = serversChanges.added;
      }
      if (serversChanges && serversChanges.removed.length > 0) {
        changes.removed = serversChanges.removed;
      }
      topicUtil.publish(topicUtil.getTopicByType('servers'), changes);
    }
  };

  return {

    /**
     * Compares the current servers with the servers in the cache.
     *
     * @param {Object} now The current request
     */
    sendChangeEvents: function(now) {
      sendServersChangeEvents(now);
    },

    /**
     * Exposed for unit testing.
     */
    __resourceManager: resourceManager

  };

});
