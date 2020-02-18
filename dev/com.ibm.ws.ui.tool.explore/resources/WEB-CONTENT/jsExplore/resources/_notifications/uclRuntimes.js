/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
   * Compare two Runtime objects to detect changes.
   * 
   * The dynamic fields (that we care about) are:
   * servers
   * metadata
   * alerts
   * 
   * See Runtime.js for the expected Runtime Change Event format.
   * 
   * @param {Object} nowRuntime The runtime returned by the REST API
   * @param {Object} cachedRuntime The runtime stored in the JSON cache
   * @return {Object} The detected changes, or null if no changes
   */
  function computeRuntimeChanges(cachedRuntime, nowRuntime) {
    var changes = {};
    var hasChanged = false;

    // Implementation note: the browser-side does not use the serversState field.
    // This is left in here to show what we could do if we did
    //// FIXME: server->client side mapping: serversState->state
    //if (nowRuntime.serversState != cachedRuntime.state) {
    //changes.state = nowRuntime.serversState;
    //hasChanged = true;
    //}

    // Compare the embedded servers collection
    var serversChanges = uclCommon.compareTwoLists(cachedRuntime.servers.list, nowRuntime.servers.ids);
    var mmChanges = false;
    if ( nowRuntime.servers && cachedRuntime.servers && nowRuntime.servers.inMaintenanceMode != cachedRuntime.servers.inMaintenanceMode ) {
      mmChanges = true;
    }
    if (cachedRuntime.servers.up != nowRuntime.servers.up ||
        cachedRuntime.servers.down != nowRuntime.servers.down ||
        cachedRuntime.servers.unknown != nowRuntime.servers.unknown ||
        serversChanges || mmChanges) {
      changes.servers = {
          up: nowRuntime.servers.up,
          down: nowRuntime.servers.down,
          unknown: nowRuntime.servers.unknown,
          inMaintenanceMode: nowRuntime.servers.inMaintenanceMode
      };
      if (serversChanges && serversChanges.added.length > 0) {
        changes.servers.added = serversChanges.added;
      }
      if (serversChanges && serversChanges.removed.length > 0) {
        changes.servers.removed = serversChanges.removed;
      }
      hasChanged = true;
    }
    
    hasChanged = uclCommon.compareMetadata(cachedRuntime, nowRuntime, changes) || hasChanged;
    hasChanged = uclCommon.compareAlerts(cachedRuntime, nowRuntime, changes) || hasChanged;

    if (hasChanged) {
      return changes;
    } else {
      return null;
    }
  }

  /**
   * Compares the current runtime's attributes with the attributes of the runtime in the cache, if that runtime is present.
   * 
   * @param {Object}
   *          nowRuntime The current runtime to compare with the cached runtime
   */
  function sendRuntimeChangeEvent(nowRuntime) {
    var cachedRuntime = resourceManager.getCached('runtime', nowRuntime.id);
    if (cachedRuntime) {
      var changes = computeRuntimeChanges(cachedRuntime, nowRuntime);
      if (changes) {
        changes.type = 'runtime';
        changes.id = nowRuntime.id;
        changes.origin = 'uclRuntimes.js:92';
        topicUtil.publish(topicUtil.getTopicByType('runtime', nowRuntime.id), changes);
      }
    }
  }
  
  /**
   * Compares the current runtimes with the runtimes in the cache.
   * 
   * @param {Object}
   *          now The current request
   */
  function sendRuntimesChangeEvents(now) {
    var cachedRuntimes = resourceManager.getCached('runtimes');
    if (!cachedRuntimes) {
      // If we do not have the Runtimes collection cached, then there is nothing to do here!
      return;
    }

    var runtimesChanges = uclCommon.compareTwoLists(cachedRuntimes.list, now.runtimes.ids);

    // Step through all of the runtimes in the current request and compare against the cache
    // This will send individual change notifications for anything that is cached
    for (var i = 0; i < now.runtimes.list.length; i++) {
      var runtime = now.runtimes.list[i];
      sendRuntimeChangeEvent(runtime);
    }

    // Step through all of the runtimes that were removed and send them the 'removed' event
    if (runtimesChanges) {
      for (var i = 0; i < runtimesChanges.removed.length; i++) {
        var removedRuntime = runtimesChanges.removed[i];
        if (resourceManager.getCached('runtime', removedRuntime)) {
          // If its cached, remove it
          topicUtil.publish(topicUtil.getTopicByType('runtime', removedRuntime), {
            type : 'runtime',
            id : removedRuntime,
            state : 'removed',
            origin : 'uclRuntimes.js:125'
          });
        }
      }
    }

    // Compare the cached Runtimes collection against the current values
    // FIXME: server->client side mapping: list->ids
    if (cachedRuntimes.up !== now.runtimes.allServersRunning || cachedRuntimes.down !== now.runtimes.allServersStopped ||
        cachedRuntimes.unknown !== now.runtimes.allServersUnknown || cachedRuntimes.partial !== now.runtimes.someServersRunning ||
        cachedRuntimes.empty !== now.runtimes.noServers || runtimesChanges) {
      var changes = {
          type : 'runtimes',
          up : now.runtimes.allServersRunning,
          down : now.runtimes.allServersStopped,
          unknown : now.runtimes.allServersUnknown,
          partial : now.runtimes.someServersRunning,
          empty : now.runtimes.noServers,
          origin : 'uclRuntimes.js:143'
      };
      if (runtimesChanges && runtimesChanges.added.length > 0) {
        changes.added = runtimesChanges.added;
      }
      if (runtimesChanges && runtimesChanges.removed.length > 0) {
        changes.removed = runtimesChanges.removed;
      }
      topicUtil.publish(topicUtil.getTopicByType('runtimes'), changes);
    }
  };

  return {

    /**
     * Compares the current runtimes with the runtimes in the cache.
     * 
     * @param {Object} now The current request
     */
    sendChangeEvents: function(now) {
      sendRuntimesChangeEvents(now);
    },

    /**
     * Exposed for unit testing.
     */
    __resourceManager: resourceManager

  };

});
