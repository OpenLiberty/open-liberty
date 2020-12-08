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
   * Compare two application objects to detect changes.
   * 
   * @param {Object} cachedApplication The application stored in the JSON cache
   * @param {Object} nowApplication The application returned by the REST API
   * @return {Object} The detected changes, or null if no changes
   */
  function computeApplicationChanges(cachedApplication, nowApplication) {
    var changes = {};
    var hasChanged = false;

    // Compare state & tallies
    if (nowApplication.state !== cachedApplication.state ||
        nowApplication.up !== cachedApplication.up ||
        nowApplication.down !== cachedApplication.down ||
        nowApplication.unknown !== cachedApplication.unknown) {
      // The change values will be set later in this method when anything changes
      hasChanged = true;
    }

    // Compare scalingPolicy
    if (!!nowApplication.scalingPolicy != !!cachedApplication.scalingPolicy) {
      changes.scalingPolicy = !!nowApplication.scalingPolicy; // Convert to a boolean value
      hasChanged = true;
    }
    if (!!nowApplication.scalingPolicyEnabled != !!cachedApplication.scalingPolicyEnabled) {
      changes.scalingPolicyEnabled = !!nowApplication.scalingPolicyEnabled; // Convert to a boolean value
      hasChanged = true;
    }
    
    // Compare the embedded servers collection    
    if (nowApplication.servers && cachedApplication.servers) {
      var serversChanges = uclCommon.compareTwoLists(cachedApplication.servers.list, nowApplication.servers.ids);
      if (cachedApplication.servers.up != nowApplication.servers.up ||
          cachedApplication.servers.down != nowApplication.servers.down ||
          cachedApplication.servers.unknown != nowApplication.servers.unknown ||
          serversChanges) {
        changes.servers = {
            up: nowApplication.servers.up,
            down: nowApplication.servers.down,
            unknown: nowApplication.servers.unknown
        };
        if (serversChanges && serversChanges.added.length > 0) {
          changes.servers.added = serversChanges.added;
        }
        if (serversChanges && serversChanges.removed.length > 0) {
          changes.servers.removed = serversChanges.removed;
        }
        hasChanged = true;
      }
    } else if ((nowApplication.servers && !cachedApplication.servers) ||
               (!nowApplication.servers && cachedApplication.servers)) {
      hasChanged = true;
    } 

    // Compare the embedded clusters collection
    if (nowApplication.clusters && cachedApplication.clusters) {
      var clustersChanges = uclCommon.compareTwoLists(cachedApplication.clusters.list, nowApplication.clusters.ids);
      if (cachedApplication.clusters.up != nowApplication.clusters.up ||
          cachedApplication.clusters.down != nowApplication.clusters.down ||
          cachedApplication.clusters.unknown != nowApplication.clusters.unknown ||
          cachedApplication.clusters.partial != nowApplication.clusters.partial ||
          clustersChanges) {
        changes.clusters = {
            up: nowApplication.clusters.up,
            down: nowApplication.clusters.down,
            unknown: nowApplication.clusters.unknown,
            partial: nowApplication.clusters.partial
        };
        if (clustersChanges && clustersChanges.added.length > 0) {
          changes.clusters.added = clustersChanges.added;
        }
        if (clustersChanges && clustersChanges.removed.length > 0) {
          changes.clusters.removed = clustersChanges.removed;
        }
        hasChanged = true;
      }
    }

    hasChanged = uclCommon.compareAlerts(cachedApplication, nowApplication, changes) || hasChanged;

    if (hasChanged) {
      // Always send the state and tallies
      changes.state = nowApplication.state;
      changes.up = nowApplication.up;
      changes.down = nowApplication.down;
      changes.unknown = nowApplication.unknown;
      return changes;
    } else {
      return null;
    }
  };

  /**
   * Compares the current application's attributes with the attributes of the application in the cache, if that application is present.
   * 
   * @param {Object}
   *          nowApplication The current application to compare with the cached application
   */
  function sendApplicationChangeEvent(nowApplication) {    
    var cachedApplication = resourceManager.getCached('application', nowApplication.id);
    if (cachedApplication) {
      var changes = computeApplicationChanges(cachedApplication, nowApplication);
      if (changes) {
        changes.type = 'application';
        changes.id = nowApplication.id;
        changes.origin = 'uclApplications.js:119';
        topicUtil.publish(topicUtil.getTopicByType('application', nowApplication.id), changes);
      }
    }
  }

  /**
   * Compares the current applications with the applications in the cache.
   * 
   * @param {Object}
   *          now The current request
   */
  function sendApplicationsChangeEvents(now) {
    var cachedApplications = resourceManager.getCached('applications');
    if (!cachedApplications) {
      // If we do not have the Applications collection cached, then there is nothing to do here!
      return;
    }

    var applicationsChanges = uclCommon.compareTwoLists(cachedApplications.list, now.applications.ids);

    // Step through all of the applications in the current request and compare against the cache
    // This will send individual change notifications for anything that is cached
    for (var i = 0; i < now.applications.list.length; i++) {
      var application = now.applications.list[i];
      sendApplicationChangeEvent(application);
    }

    // Step through all of the applications that were removed and send them the 'removed' event
    if (applicationsChanges) {
      for (var i = 0; i < applicationsChanges.removed.length; i++) {
        var removedApplication = applicationsChanges.removed[i];
        if (resourceManager.getCached('application', removedApplication)) {
          // If its cached, remove it
          topicUtil.publish(topicUtil.getTopicByType('application', removedApplication), {
            type : 'application',
            id : removedApplication,
            state : 'removed',
            origin : 'uclApplications.js:157'
          });
        }
      }
    }

    // Compare the cached Applications collection against the current values
    // FIXME: server->client side mapping: list->ids
    if (cachedApplications.up !== now.applications.up || cachedApplications.down !== now.applications.down ||
        cachedApplications.unknown !== now.applications.unknown || cachedApplications.partial !== now.applications.partial ||
        applicationsChanges) {
      var changes = {
          type : 'applications',
          up : now.applications.up,
          down : now.applications.down,
          unknown : now.applications.unknown,
          partial : now.applications.partial,
          origin : 'ulcApplications.js:174'
      };
      if (applicationsChanges && applicationsChanges.added.length > 0) {
        changes.added = applicationsChanges.added;
      }
      if (applicationsChanges && applicationsChanges.removed.length > 0) {
        changes.removed = applicationsChanges.removed;
      }
      topicUtil.publish(topicUtil.getTopicByType('applications'), changes);
    }
  };

  return {

    /**
     * Compares the current applications with the applications in the cache.
     * 
     * @param {Object} now The current request
     */
    sendChangeEvents: function(now) {
      sendApplicationsChangeEvents(now);
    },

    /**
     * Exposed for unit testing.
     */
    __resourceManager: resourceManager

  };

});