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
   * Compare two Host objects to detect changes.
   * 
   * The dynamic fields (that we care about) are:
   * runtimes
   * runtime's metadata
   * servers
   * metadata
   * alerts
   * 
   * See Host.js for the expected Host Change Event format.
   * 
   * @param {Object} nowHost The host returned by the REST API
   * @param {Object} cachedHost The host stored in the JSON cache
   * @return {Object} The detected changes, or null if no changes
   */
  function computeHostChanges(cachedHost, nowHost) {
    var changes = {};
    var hasChanged = false;

    // Implementation note: the browser-side does not use the serversState field.
    // This is left in here to show what we could do if we did
    //// FIXME: server->client side mapping: serversState->state
    //if (nowHost.serversState != cachedHost.state) {
    //changes.state = nowHost.serversState;
    //hasChanged = true;
    //}

    // Compare the two lists and find what was added and was what removed
    var runtimeChanges = uclCommon.compareTwoListsById(cachedHost.runtimes.list, nowHost.runtimes.list);
    if (runtimeChanges) {
      hasChanged = true;
      changes.runtimes = runtimeChanges;
    }
    
    // Check the metadata of the nowHost.runtimes to see if it was updated
    var runtimeMetadataUpdated = false;
    var runtimeMetadataChanges = [];
    for (var i = 0; i < nowHost.runtimes.list.length; i++) {
      // Compare with the cached Runtime's metadata
      for (var x = 0; x < cachedHost.runtimes.list.length; x++) {
        if (nowHost.runtimes.list[i].id === cachedHost.runtimes.list[x].id) {
          var metadataChanges = {id: nowHost.runtimes.list[i].id};
          if (uclCommon.compareMetadata(cachedHost.runtimes.list[x], nowHost.runtimes.list[i], metadataChanges)) {
            runtimeMetadataUpdated = true;
            runtimeMetadataChanges[runtimeMetadataChanges.length] = metadataChanges; 
          }
        }
      }
    }
    if (runtimeMetadataUpdated) {
      hasChanged = true;
      changes.runtimesMetadata = runtimeMetadataChanges;
    }

    // Compare the embedded servers collection
    var serversChanges = uclCommon.compareTwoLists(cachedHost.servers.list, nowHost.servers.ids);
    var mmChanges = false;
    if ( nowHost.servers && cachedHost.servers && nowHost.servers.inMaintenanceMode != cachedHost.servers.inMaintenanceMode ) {
      mmChanges = true;
    }
    if (cachedHost.servers.up != nowHost.servers.up ||
        cachedHost.servers.down != nowHost.servers.down ||
        cachedHost.servers.unknown != nowHost.servers.unknown ||
        serversChanges || mmChanges) {
      changes.servers = {
          up: nowHost.servers.up,
          down: nowHost.servers.down,
          unknown: nowHost.servers.unknown,
          inMaintenanceMode: nowHost.servers.inMaintenanceMode
      };
      if (serversChanges && serversChanges.added.length > 0) {
        changes.servers.added = serversChanges.added;
      }
      if (serversChanges && serversChanges.removed.length > 0) {
        changes.servers.removed = serversChanges.removed;
      }
      hasChanged = true;
    }
    
    if (nowHost.maintenanceMode != cachedHost.maintenanceMode) {
      changes.maintenanceMode = nowHost.maintenanceMode;
      hasChanged = true;
    }
    
    hasChanged = uclCommon.compareMetadata(cachedHost, nowHost, changes) || hasChanged;
    hasChanged = uclCommon.compareAlerts(cachedHost, nowHost, changes) || hasChanged;

    if (hasChanged) {
      return changes;
    } else {
      return null;
    }
  }

  /**
   * Compares the current host's attributes with the attributes of the host in the cache, if that host is present.
   * 
   * @param {Object}
   *          nowHost The current host to compare with the cached host
   */
  function sendHostChangeEvent(nowHost) {
    var cachedHost = resourceManager.getCached('host', nowHost.id);
    if (cachedHost) {
      var changes = computeHostChanges(cachedHost, nowHost);
      if (changes) {
        changes.type = 'host';
        changes.id = nowHost.id;
        changes.origin = 'uclHosts.js:95';
        topicUtil.publish(topicUtil.getTopicByType('host', nowHost.id), changes);
      }
    }
  }

  /**
   * Checks if there are any maintenance mode changes
   */
  function hasMaintenanceModeChange(nowHost) {
    // Find the cached server and compare with current server
    var hasChange = false;
    var cachedHost = resourceManager.getCached('host', nowHost.id);
    if ( cachedHost && nowHost.maintenanceMode != cachedHost.maintenanceMode ) {
        hasChange = true; 
    }
    if (hasChange === false ) {
      // check inMaintenanceMode of the host.servers
      if ( cachedHost && cachedHost.servers && nowHost.servers && cachedHost.servers.inMaintenanceMode != nowHost.servers.inMaintenanceMode )
        hasChange = true;
    }
    return hasChange;
  };
  
  /**
   * Compares the current hosts with the hosts in the cache.
   * 
   * @param {Object}
   *          now The current request
   */
  function sendHostsChangeEvents(now) {
    var cachedHosts = resourceManager.getCached('hosts');
    if (!cachedHosts) {
      // If we do not have the Hosts collection cached, then there is nothing to do here!
      return;
    }

    var hostsChanges = uclCommon.compareTwoLists(cachedHosts.list, now.hosts.ids);

    // Step through all of the hosts in the current request and compare against the cache
    // This will send individual change notifications for anything that is cached
    for (var i = 0; i < now.hosts.list.length; i++) {
      var host = now.hosts.list[i];
      sendHostChangeEvent(host);
    }

    // Step through all of the hosts that were removed and send them the 'removed' event
    if (hostsChanges) {
      for (var i = 0; i < hostsChanges.removed.length; i++) {
        var removedHost = hostsChanges.removed[i];
        if (resourceManager.getCached('host', removedHost)) {
          // If its cached, remove it
          topicUtil.publish(topicUtil.getTopicByType('host', removedHost), {
            type : 'host',
            id : removedHost,
            state : 'removed',
            origin : 'uclHosts.js:133'
          });
        }
      }
    }

    // Compare the cached Hosts collection against the current values
    // FIXME: server->client side mapping: list->ids
    if (cachedHosts.up !== now.hosts.allServersRunning || cachedHosts.down !== now.hosts.allServersStopped ||
        cachedHosts.unknown !== now.hosts.allServersUnknown || cachedHosts.partial !== now.hosts.someServersRunning ||
        cachedHosts.empty !== now.hosts.noServers || hostsChanges) {
      var changes = {
          type : 'hosts',
          up : now.hosts.allServersRunning,
          down : now.hosts.allServersStopped,
          unknown : now.hosts.allServersUnknown,
          partial : now.hosts.someServersRunning,
          empty : now.hosts.noServers,
          origin : 'uclHosts.js:172'
      };
      if (hostsChanges && hostsChanges.added.length > 0) {
        changes.added = hostsChanges.added;
      }
      if (hostsChanges && hostsChanges.removed.length > 0) {
        changes.removed = hostsChanges.removed;
      }
      topicUtil.publish(topicUtil.getTopicByType('hosts'), changes);
    }
  };

  return {

    /**
     * Compares the current hosts with the hosts in the cache.
     * 
     * @param {Object} now The current request
     */
    sendChangeEvents: function(now) {
      sendHostsChangeEvents(now);
    },

    /**
     * Exposed for unit testing.
     */
    __resourceManager: resourceManager

  };

});
