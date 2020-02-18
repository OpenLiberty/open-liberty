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
   * Compare two cluster objects to detect changes.
   * 
   * @param {Object} cachedCluster The cluster stored in the JSON cache
   * @param {Object} nowCluster The cluster returned by the REST API
   * @return {Object} The detected changes, or null if no changes
   */
  function computeClusterChanges(cachedCluster, nowCluster) {
    var changes = {};
    var hasChanged = false;

    if (nowCluster.state != cachedCluster.state) {
      changes.state = nowCluster.state;
      hasChanged = true;
    }
    if (nowCluster.scalingPolicy != cachedCluster.scalingPolicy) {
      changes.scalingPolicy = nowCluster.scalingPolicy;
      hasChanged = true;
    }
    if (nowCluster.scalingPolicyEnabled != cachedCluster.scalingPolicyEnabled) {
      changes.scalingPolicyEnabled = nowCluster.scalingPolicyEnabled;
      hasChanged = true;
    }

    // Compare the embedded servers collection
    var serversChanges = uclCommon.compareTwoLists(cachedCluster.servers.list, nowCluster.servers.ids);
    if (cachedCluster.servers.up != nowCluster.servers.up ||
        cachedCluster.servers.down != nowCluster.servers.down ||
        cachedCluster.servers.unknown != nowCluster.servers.unknown ||
        serversChanges) {
      changes.servers = {
          up: nowCluster.servers.up,
          down: nowCluster.servers.down,
          unknown: nowCluster.servers.unknown
      };
      if (serversChanges && serversChanges.added.length > 0) {
        changes.servers.added = serversChanges.added;
      }
      if (serversChanges && serversChanges.removed.length > 0) {
        changes.servers.removed = serversChanges.removed;
      }
      hasChanged = true;
    }

    // Compare the embedded apps collection
    var addedApplications = [];
    var removedApplications = [];
    var changedApplications = [];

    for (var i = 0; i < nowCluster.apps.list.length; i++) {
      var nowAoC = nowCluster.apps.list[i];
      var cachedAoC = uclCommon.findObjectInListById(cachedCluster.apps.list, nowAoC.id);
      if (!cachedAoC) {
        addedApplications.push(nowAoC);  // In this case, we push the app object
      } else {
        // The app is in the list, see if its changed... (this is similar to computeApplicationChangeEvent)
        var appChanged = false;

        // Compare state & tallies
        if (nowAoC.state !== cachedAoC.state ||
            nowAoC.up !== cachedAoC.up ||
            nowAoC.down !== cachedAoC.down ||
            nowAoC.unknown !== cachedAoC.unknown) {
          appChanged = true;
        }

        // Compare the embedded servers collection
        var cachedAoCServersChanges = uclCommon.compareTwoLists(cachedAoC.servers.ids, nowAoC.servers.ids);
        if (cachedAoC.servers.up != nowAoC.servers.up ||
            cachedAoC.servers.down != nowAoC.servers.down ||
            cachedAoC.servers.unknown != nowAoC.servers.unknown ||
            cachedAoCServersChanges) {
          appChanged = true;
        }
        appChanged = uclCommon.compareMetadata(cachedAoC, nowAoC, {}) || appChanged;

        if (appChanged) {
          changedApplications.push(nowAoC);  // In this case, we push the app object
        }
      }
    }

    // Iterate over the cloned cached apps to see if something was missed
    for (var z = 0; z < cachedCluster.apps.list.length; z++) {
      var cachedAoC = cachedCluster.apps.list[z];
      if (!uclCommon.findObjectInListById(nowCluster.apps.list, cachedAoC.id)) {
        removedApplications.push(cachedAoC.name);  // In this case, we push ONLY the app name
      }
    }

    if (cachedCluster.apps.up != nowCluster.apps.up ||
        cachedCluster.apps.down != nowCluster.apps.down ||
        cachedCluster.apps.unknown != nowCluster.apps.unknown ||
        cachedCluster.apps.partial != nowCluster.apps.partial ||
        addedApplications.length > 0 ||
        removedApplications.length > 0 ||
        changedApplications.length > 0) {
      changes.apps = {
          up : nowCluster.apps.up,
          down : nowCluster.apps.down,
          unknown : nowCluster.apps.unknown,
          partial : nowCluster.apps.partial
      };
      if (addedApplications.length > 0) {
        changes.apps.added = addedApplications;
      }
      if (removedApplications.length > 0) {
        changes.apps.removed = removedApplications;
      }
      if (changedApplications.length > 0) {
        changes.apps.changed = changedApplications;
      }
      hasChanged = true;
    }

    // Compare tags, owner and contacts
    hasChanged = uclCommon.compareMetadata(cachedCluster, nowCluster, changes) || hasChanged;
    hasChanged = uclCommon.compareAlerts(cachedCluster, nowCluster, changes) || hasChanged;

    if (hasChanged) {
      return changes;
    } else {
      return null;
    }
  };

  /**
   * Compares the current cluster's attributes with the attributes of the cluster in the cache, if that cluster is present.
   * 
   * @param {Object}
   *          nowCluster The current cluster to compare with the cached cluster
   */
  function sendClusterChangeEvent(nowCluster) {
    var cachedCluster = resourceManager.getCached('cluster', nowCluster.id);
    if (cachedCluster) {
      var changes = computeClusterChanges(cachedCluster, nowCluster);
      if (changes) {
        changes.type = 'cluster';
        changes.id = nowCluster.id;
        changes.origin = 'unifiedChangeListener.js:678';
        topicUtil.publish(topicUtil.getTopicByType('cluster', nowCluster.id), changes);
      }
    }
  }

  /**
   * Compares the current clusters with the clusters in the cache.
   * 
   * @param {Object}
   *          now The current request
   */
  function sendClustersChangeEvents(now) {
    var cachedClusters = resourceManager.getCached('clusters');
    var cachedAlerts = resourceManager.getCached('alerts'); // Used to update filterBar if alerts changed but up,down,unknown,partial did not
    if (!cachedClusters) {
      // If we do not have the Clusters collection cached, then there is nothing to do here!
      return;
    }

    var clustersChanges = uclCommon.compareTwoLists(cachedClusters.list, now.clusters.ids);

    // Step through all of the clusters in the current request and compare against the cache
    // This will send individual change notifications for anything that is cached
    for (var i = 0; i < now.clusters.list.length; i++) {
      var cluster = now.clusters.list[i];
      sendClusterChangeEvent(cluster);
    }

    // Step through all of the clusters that were removed and send them the 'removed' event
    if (clustersChanges) {
      for (var i = 0; i < clustersChanges.removed.length; i++) {
        var removedCluster = clustersChanges.removed[i];
        if (resourceManager.getCached('cluster', removedCluster)) {
          // If its cached, remove it
          topicUtil.publish(topicUtil.getTopicByType('cluster', removedCluster), {
            type : 'cluster',
            id : removedCluster,
            state : 'removed',
            origin : 'unifiedChangeListener.js:735'
          });
        }
      }
    }

    // Compare the cached Clusters collection against the current values
    // FIXME: server->client side mapping: list->ids
    if (cachedClusters.up !== now.clusters.up || cachedClusters.down !== now.clusters.down ||
        cachedClusters.unknown !== now.clusters.unknown || cachedClusters.partial !== now.clusters.partial ||
        cachedAlerts.count !== now.alerts.count ||
        clustersChanges) {
      var changes = {
          type : 'clusters',
          up : now.clusters.up,
          down : now.clusters.down,
          unknown : now.clusters.unknown,
          partial : now.clusters.partial,
          origin : 'unifiedChangeListener.js:709'
      };
      if (clustersChanges && clustersChanges.added.length > 0) {
        changes.added = clustersChanges.added;
      }
      if (clustersChanges && clustersChanges.removed.length > 0) {
        changes.removed = clustersChanges.removed;
      }
      topicUtil.publish(topicUtil.getTopicByType('clusters'), changes);
    }
  };

  return {

    /**
     * Compares the current clusters with the clusters in the cache.
     * 
     * @param {Object} now The current request
     */
    sendChangeEvents: function(now) {
      sendClustersChangeEvents(now);
    },

    /**
     * Exposed for unit testing.
     */
    __resourceManager: resourceManager

  };

});
