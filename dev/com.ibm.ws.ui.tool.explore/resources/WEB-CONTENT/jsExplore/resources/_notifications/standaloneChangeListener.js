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
 * See _changeListener.js for the general doc.
 * 
 * The event can have the following properties:
 * StandaloneServer Event {
 *   type: 'standaloneServer',
 *   cluster, scalingPolicy, scalingPolicyEnabled, (optional)
 *   apps: {
 *     up, down, unknown,
 *     added: [ { name, state } ],   (optional)
 *     removed: [ { name, state } ],   (optional)
 *     changed: [ { name, state } ]   (optional)
 *   } (optional)
 * }
 */
define([ '../_topicUtil', '../_util', './_changeListener', '../resourceManager' ],
    function(topicUtil, util, ChangeListener, resourceManager) {

  var pullFrequency = 2000;/*ms*/ // Should be no less than 1000ms,
  var debug = false;

  /**
   * Find a server with the matching name in the list.
   */
  var getApp = function(list, name) {
    for (var i = 0; i < list.length; i++) {
      if (list[i].name === name) {
        return list[i];
      }
    }
  };

  /**
   * Compute the delta between the cached resource and now (the REST API responses), and send notifications for changes.
   */
  var sendStandaloneNotifications = function(now) {
    var cached = resourceManager.getCached('standaloneServer');
    if (!cached) {
      return;
    }

    if (debug) console.log('sendStandaloneNotifications', cached, now);

    // Keep track of what apps were added, changed or removed
    var hasChanges = false;
    var addedApps = [];
    var changedApps = [];
    var removedApps = [];

    // Clone the apps list, so we can mark them inspected or not
    var cachedApps = (cached.apps && cached.apps.list) ? cached.apps.list : [];
    cachedApps = JSON.parse(JSON.stringify(cachedApps));

    // Iterate through all of the apps and compare their state / detect new apps
    for(var i = 0; i < now.apps.list.length; i++) {
      var nowApp = now.apps.list[i];
      var cachedApp = getApp(cachedApps, nowApp.name);
      if (debug) console.log('Comparing apps ', cachedApp, nowApp);
      if (!cachedApp) {
        addedApps.push(nowApp);
      } else {
        cachedApp.inspected = true;
        if (cachedApp.state != nowApp.state) {
          changedApps.push(nowApp);
        }
      }
    }

    // Iterate through all of the cached apps to see if we've inspected them.
    // If not, then they were not in the current state and were be removed from the server.
    for(var j = 0; j < cachedApps.length; j++) {
      var cachedApp = cachedApps[j];
      if (!cachedApp.inspected) {
        removedApps.push(cachedApp.name);
      }
    }

    changes = {
        type: 'standaloneServer',
        origin: 'sendStandaloneNotifications.js:78'
    };
    if (cached.cluster != now.cluster) {
      hasChanges = true;
      changes.cluster = now.cluster;
    }
    if (cached.scalingPolicy != now.scalingPolicy) {
      hasChanges = true;
      changes.scalingPolicy = now.scalingPolicy;
    }
    if (cached.scalingPolicyEnabled != now.scalingPolicyEnabled) {
      hasChanges = true;
      changes.scalingPolicyEnabled = now.scalingPolicyEnabled;
    }
    
    // Fire our totals event last. This ensures that anyone who queries the
    // children will have the children be up to date
    if (cached.apps.up != now.apps.up || cached.apps.down != now.apps.down || cached.apps.unknown != now.apps.unknown ||
        addedApps.length > 0 || removedApps.length > 0 || changedApps.length > 0) {
      hasChanges = true;
      changes.apps = {
          up: now.apps.up,
          down: now.apps.down,
          unknown: now.apps.unknown
      };
      if (addedApps.length > 0) {
        changes.apps.added = addedApps;
      }
      if (changedApps.length > 0) {
        changes.apps.changed = changedApps;
      }
      if (removedApps.length > 0) {
        changes.apps.removed = removedApps;
      }
    }

    if (hasChanges) {
      topicUtil.publish(topicUtil.getTopicByType('standaloneServer'), changes);
    }
  };

  /**
   * Define the listener.
   */
  var pull = new ChangeListener({
    name: 'standaloneChangeListener',
    pullFrequency: pullFrequency,
    debug: debug
  });

  pull.customOnTickGetAndProcess = function() {
    var compareDeltas = function(ignored, serverInfo) {
      try {
        sendStandaloneNotifications(serverInfo);
      } catch(e) {
        console.error(name + ' customOnTickGetAndProcess encountered an error', e);
      }
      pull.__running = false;
    };
    return util.populateStandaloneServer(this, compareDeltas, true);
  };

  return {
    /**
     * Start the notification engine.
     */
    start: function() {
      pull.start();
    },

    /**
     * Stop the notification engine.
     */
    stop: function() {
      pull.stop();
    },

    /**
     * Exposed for unit testing.
     */
    __resourceManager: resourceManager,
    __sendStandaloneNotifications: sendStandaloneNotifications
  };

});