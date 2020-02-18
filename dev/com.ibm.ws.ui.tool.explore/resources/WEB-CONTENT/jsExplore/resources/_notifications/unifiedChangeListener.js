/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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
 * The unified change listener uses the collective/hosts API as this is the only API which gives us everything we need in a single shot. The
 * cost to doing polling is lessened by querying a single URL, but this does not mean this will scale. This change listener is a fallback to
 * the preferred WebSockets based push model.
 */
define([ '../_topicUtil', './_changeListener', '../resourceManager', './uclApplications', './uclClusters', './uclHosts', './uclServers', './uclRuntimes' ],
    function(topicUtil, ChangeListener, resourceManager, uclApplications, uclClusters, uclHosts, uclServers, uclRuntimes) {

  // TODO: The unified change listener needs to slow itself down @ 10k. It takes 5 seconds and 4.1 MB of data back with a Frappe data
  // mining implementation.
  var pullFrequency = 2000;/* ms */// Should be no less than 1000ms,
  var debug = false;

  /**
   * Send change events for the Summary. The event is only fired if the Summary has changed.
   * 
   * @param {Object}
   *          now The current processed JSON response from the REST API
   */
  function sendSummaryChangeEvent(now) {
    var summary = resourceManager.getCached('summary');
    if (!summary) {
      return;
    }

    var changed = false;
    var changes = {type : 'summary', origin : 'unifiedChangeListener.js:1003'};

    // Hosts Event
    if (summary.hosts.up !== now.hosts.allServersRunning
        || summary.hosts.down !== now.hosts.allServersStopped
        || summary.hosts.unknown !== now.hosts.allServersUnknown
        || summary.hosts.partial !== now.hosts.someServersRunning
        || summary.hosts.empty !== now.hosts.noServers) {
      changed = true;
      changes.hosts = {
          up : now.hosts.allServersRunning,
          down : now.hosts.allServersStopped,
          unknown : now.hosts.allServersUnknown,
          partial : now.hosts.someServersRunning,
          empty : now.hosts.noServers
      };
    }

    // Runtimes Event
    if (summary.runtimes.up !== now.runtimes.allServersRunning
        || summary.runtimes.down !== now.runtimes.allServersStopped
        || summary.runtimes.unknown !== now.runtimes.allServersUnknown
        || summary.runtimes.partial !== now.runtimes.someServersRunning
        || summary.runtimes.empty !== now.runtimes.noServers) {
      changed = true;
      changes.runtimes = {
          up : now.runtimes.allServersRunning,
          down : now.runtimes.allServersStopped,
          unknown : now.runtimes.allServersUnknown,
          partial : now.runtimes.someServersRunning,
          empty : now.runtimes.noServers
      };
    }

    // Servers Event
    if (summary.servers.up !== now.servers.up
        || summary.servers.down !== now.servers.down
        || summary.servers.unknown !== now.servers.unknown) {
      changed = true;
      changes.servers = {
          up : now.servers.up,
          down : now.servers.down,
          unknown : now.servers.unknown
      };
    }

    // Clusters Event
    if (summary.clusters.up !== now.clusters.up
        || summary.clusters.down !== now.clusters.down
        || summary.clusters.unknown !== now.clusters.unknown
        || summary.clusters.partial !== now.clusters.partial) {
      changed = true;
      changes.clusters = {
          up : now.clusters.up,
          down : now.clusters.down,
          unknown : now.clusters.unknown,
          partial : now.clusters.partial
      };
    }

    // Applications Event
    if (summary.applications.up !== now.applications.up
        || summary.applications.down !== now.applications.down
        || summary.applications.unknown !== now.applications.unknown
        || summary.applications.partial !== now.applications.partial) {
      changed = true;
      changes.applications = {
          up : now.applications.up,
          down : now.applications.down,
          unknown : now.applications.unknown,
          partial : now.applications.partial
      };
    }

    // If any of the tallies changed, then update the summary
    if (changed) {
      topicUtil.publish(topicUtil.getTopicByType('summary'), changes);
    }
  };

  /**
   * Send change events for the Alerts. The event is only fired if the Alerts has changed.
   * 
   * @param {Object}
   *          now The current processed JSON response from the REST API
   */
  function sendAlertsChangeEvent(now) {
    var alerts = resourceManager.getCached('alerts');
    if (!alerts) {
      return;
    }

    var changed = false;

    if (alerts.count != now.alerts.count) {
      changed = true;
    }

    if (!changed) {
      if (JSON.stringify(alerts.unknown) !== JSON.stringify(now.alerts.unknown) ){
        changed = true;
      }
    }

    if (!changed) {
      // Only check if need to, if we know we've changed no work is needed
      if (JSON.stringify(alerts.app) !== JSON.stringify(now.alerts.app) ){ 
        changed = true;
      }
    }

    // If any of the tallies changed, then update the summary
    if (changed) {
      var changes = {
          type : 'alerts',
          count: now.alerts.count,
          unknown: now.alerts.unknown,
          app: now.alerts.app,
          origin : 'unifiedChangeListener.js:1105' };
      topicUtil.publish(topicUtil.getTopicByType('alerts'), changes);
    }
  };

  /**
   * Compute the delta between the cached resources and now, and send notifications for changes.
   * 
   * The order shall be "ground up" to ensure dependencies are available.
   * 1. Hosts
   * 2. Runtimes
   * 3. Servers
   * 4. Clusters
   * 5. Applications
   * 6. Summary
   * 7. Alerts
   */
  function sendUnifiedNotifications(now) {
    if (debug) {
      console.log('sendUnifiedNotifications', now);      
    }

    uclHosts.sendChangeEvents(now);
    uclRuntimes.sendChangeEvents(now);
    uclServers.sendChangeEvents(now);
    uclClusters.sendChangeEvents(now);
    uclApplications.sendChangeEvents(now);
    sendSummaryChangeEvent(now);
    sendAlertsChangeEvent(now);
  };

  /**
   * Define the listener.
   */
  var pull = new ChangeListener({
    name : 'unifiedChangeListener',
    pullFrequency : pullFrequency,
    url : '/ibm/api/collective/v1/dump',
    updateProcessor : sendUnifiedNotifications,
    debug : debug
  });

  return {
    /**
     * Start the notification engine.
     */
    start : function() {
      pull.start();
    },

    /**
     * Stop the notification engine.
     */
    stop : function() {
      pull.stop();
    },

    /**
     * Exposed for unit testing.
     */
    __resourceManager: resourceManager,
    __sendUnifiedNotifications : sendUnifiedNotifications
  };
});