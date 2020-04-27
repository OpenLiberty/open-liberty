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
define([ 'dojo/Deferred',
         'dojo/promise/all',
         'dojo/request/xhr',
         'dijit/registry',
         './_util',
         './_collections/Summary',
         './_collections/Alerts',
         './_collections/Applications',
         './_derived/collections/AppInstancesByCluster',
         './_derived/collections/AppsOnCluster',
         './_derived/collections/AppsOnServer',
         './_collections/Clusters',
         './_collections/Hosts',
         './_derived/collections/RuntimesOnHost',
         './_collections/Servers',
         './_derived/collections/ServersOnCluster',
         './_derived/collections/ServersOnHost',
         './_derived/collections/ServersOnRuntime',
         './_derived/objects/AppOnCluster',
         './_derived/objects/AppOnServer',
         './_objects/Cluster',
         './_objects/Host',
         './_derived/objects/Runtime',
         './_collections/Runtimes',
         './_objects/Server',
         './_objects/StandaloneServer',
         './_collections/Search',
         'jsExplore/utils/ID'
         ],
         function(Deferred,
             all,
             xhr,
             registry,
             util,
             Summary,
             Alerts,
             Applications,
             AppInstancesByCluster,
             AppsOnCluster,
             AppsOnServer,
             Clusters,
             Hosts,
             RuntimesOnHost,
             Servers, 
             ServersOnCluster,
             ServersOnHost,
             ServersOnRuntime,
             AppOnCluster,
             AppOnServer,
             Cluster,
             Host,
             Runtime,
             Runtimes,
             Server,
             StandaloneServer,
             Search,
             ID) {

  // A word on the InstanceHolder.
  // The instanceHolder is a JavaScript object we use to track whether or not an object has been
  // requested, instantiated or is currently populating. The object is defined as:
  //
  // { instance: null, populated: false, populating: null }
  //
  // - The instance field holds the actual object which is fully initialized.
  // - The populated field indicates to the populate logic (which drives the REST API) that the
  //   object is already popuated, and the REST API should not be invoked again. If the populated
  //   field is not set, the populating field is checked.
  // - The populating field indicates if there is a current Deferred created to query the REST API.
  //   In order to not send more than 1 request, if the object is not 'populated' but it is currently
  //   'populating' then the Deferred held in populating will be returned so that it can be resolved
  //   once and have potentially multiple observers of the resolution be notified.

  var standaloneServer = { instance: null, populated: false, populating: null };
  var summary = { instance: null, populated: false, populating: null };
  var alerts = { instance: null, populated: false, populating: null };

  // Each resource type has its own cache object, which stores the collection and a map of individual objects by ID.
  var applications = {
      collection: { instance: null, populated: false, populating: null },
      objects: {} // Empty map of object ids
  };

  var clusters = {
      collection: { instance: null, populated: false, populating: null },
      objects: {}, // Empty map of object ids
      appsOnCluster: {},
      serversOnCluster: {},
      appOnCluster: {},
      appInstances: {}
  };

  var hosts = {
      collection: { instance: null, populated: false, populating: null },
      objects: {}, // Empty map of object ids
      runtimesOnHost: {},
      runtimes: {},
      serversOnHost: {},
      serversOnRuntime: {}
  };

  var servers = {
      collection: { instance: null, populated: false, populating: null },
      objects: {}, // Empty map of object ids
      appsOnServer: {},
      appOnServer: {}
  };

  var runtimes = {
      collection: { instance: null, populated: false, populating: null },
      objects: {} // Empty map of object ids
  };

  /**
   * Reset the instance holders to their initial values.
   * Needed for unit tests.... would be nice to not have to do this!
   */
  function reset() {
    standaloneServer.instance = null;
    standaloneServer.populated = false;
    standaloneServer.populating = null;

    summary.instance = null;
    summary.populated = false;
    summary.populating = null;

    alerts.instance = null;
    alerts.populated = false;
    alerts.populating = null;

    applications.collection.instance = null;
    applications.collection.populated = false;
    applications.collection.populating = null;
    applications.objects = {};

    clusters.collection.instance = null;
    clusters.collection.populated = false;
    clusters.collection.populating = null;
    clusters.objects = {};
    clusters.appsOnCluster = {};
    clusters.serversOnCluster = {};
    clusters.appOnCluster = {};
    clusters.appInstances = {};

    servers.collection.instance = null;
    servers.collection.populated = false;
    servers.collection.populating = null;
    servers.objects = {};
    servers.appsOnServer = {};
    servers.appOnServer = {};

    hosts.collection.instance = null;
    hosts.collection.populated = false;
    hosts.collection.populating = null;
    hosts.objects = {};
    hosts.runtimesOnHost= {};
    hosts.runtimes= {};
    hosts.serversOnHost= {};
    hosts.serversOnRuntime = {};

    runtimes.collection.instance = null;
    runtimes.collection.populated = false;
    runtimes.collection.populating = null;
    runtimes.objects = {};
  }

  /**
   * resourceManager is the central repository to get all the collections and objects.
   * All methods return a {Promise}. The Promise will be resolved with the resource if the resource could be found.
   * If the resource could not be found, the {Promise} is resolved with null. If an error occurs making the request
   * (other than a 404) then the {Promise} is rejected.
   */
  var resourceManager = {
      // Exposed for unit test
      __standaloneServer: standaloneServer,
      __summary: summary,
      __alerts: alerts,
      __applications: applications,
      __clusters: clusters,
      __servers: servers,
      __hosts: hosts,
      __runtimes: runtimes,
      __reset: reset,

      getStandaloneServer: _getStandaloneServer,

      getSummary: __getSummary,
      getAlerts: __getAlerts,

      getApplications: _getApplications,
      getApplication: _getApplication,

      getClusters: _getClusters,
      getCluster: _getCluster,

      getHosts: _getHosts,    
      getHost: _getHost,   

      getServers: _getServers,
      getServer: _getServer,

      getRuntime: _getRuntime,
      getRuntimeForServer: _getRuntimeForServer,
      getRuntimes: _getRuntimes,

      getAppInstancesByCluster: _getAppInstancesByCluster,

      getAppsOnServer: _getAppsOnServer,
      getAppOnServer: _getAppOnServer,

      getAppsOnCluster: _getAppsOnCluster,
      getServersOnCluster: _getServersOnCluster,
      getAppOnCluster: _getAppOnCluster,
      getAppNameFromId: _getAppNameFromId,
      getClusterOrServerName: _getClusterOrServerName,

      getRuntimesOnHost: _getRuntimesOnHost,
      getServersOnHost: _getServersOnHost,

      getServersOnRuntime: _getServersOnRuntime,

      getSearchResults: _getSearchResults,

      getCached: _getCached
  };

  /**
   * Simple type checking method to determine if the value is a string or not.
   * 
   * @return {bool} true if the value is a string, false otherwise
   */
  function isString(val) {
    return (typeof val === 'string');
  }

  /**
   * Simple method to return the provide value wrapped in a deferred.
   * 
   * @return {Deferred} A Deferred which will be resolved with the given value.
   */
  function returnAsDeferred(value) {
    var deferred = new Deferred();
    deferred.resolve(value, true);
    return deferred;
  }

  /**
   * Populate the initial collection state.
   * 
   * @return {Deferred} The returned Deferred will be resolved or rejected according to populateStandaloneServer when the update is complete.
   */
  function __createStandaloneServer() {
    // populateStandaloneServer handles any exceptions thrown by this method
    var updateInstanceHolder = function(instanceHolder, payload) {
      var server = new StandaloneServer(payload);
      server.resourceManager = resourceManager;
      server.init();

      // Set into the holder
      instanceHolder.instance = server;
    };

    return util.populateStandaloneServer(standaloneServer, updateInstanceHolder);
  }

  /**
   * Retrieve the StandaloneServer object.
   * 
   * @return {Deferred} A Deferred which will resolve with the StandaloneServer object, or be rejected if it could not be created.
   */
  function _getStandaloneServer() {
    if (standaloneServer.instance) {
      // Check our created copy, don't create twice
      return returnAsDeferred(standaloneServer.instance); 
    } else {
      var deferred = new Deferred();
      __createStandaloneServer().then(function(instanceHolder) {
        deferred.resolve(instanceHolder.instance, true);
      }, function(err) {
        console.error('_getStandaloneServer: Unable to create the StandaloneServer resource. Encountered an unexpected error', err);
        deferred.reject(err, true);
      });
      return deferred;  
    }
  }

  /**
   * Create and populate the initial Summary collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the instanceHolder, or rejected according to doPopulate, when the update is complete.
   */
  function __createSummary() {
    // doPopulate handles any exceptions thrown by this method
    var updateInstanceHolder = function(instanceHolder, payload) {
      var initObj = {
          name: payload.name,
          uuid: payload.uuid,
          hosts: {
            up: payload.hosts.allServersRunning,
            down: payload.hosts.allServersStopped,
            unknown: payload.hosts.allServersUnknown,
            partial: payload.hosts.someServersRunning,
            empty: payload.hosts.noServers
          },
          runtimes: {
            up: payload.runtimes.allServersRunning,
            down: payload.runtimes.allServersStopped,
            unknown: payload.runtimes.allServersUnknown,
            partial: payload.runtimes.someServersRunning,
            empty: payload.runtimes.noServers
          },
          servers: payload.servers,
          clusters: payload.clusters,
          applications: payload.applications
      };
      var summary = new Summary(initObj);
      summary.init();

      // Set into the holder
      instanceHolder.instance = summary;
    };

    return util.doPopulate('/ibm/api/collective/v1/summary', summary, updateInstanceHolder);
  }

  /**
   * Get the Summary collection. If the Summary collection already exists, return the copy. 
   * Otherwise, make a REST API call to get the collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the Summary collection, or rejected if it could not be created.
   */
  function __getSummary() {
    if (summary.instance) {
      // Check our created copy, don't create twice
      return returnAsDeferred(summary.instance); 
    } else {
      var deferred = new Deferred();
      __createSummary().then(function(instanceHolder) {
        deferred.resolve(instanceHolder.instance, true);
      }, function(err) {
        console.error('_getSummary: Unable to create the Summary resource. Encountered an unexpected error', err);
        deferred.reject(err, true);
      });
      return deferred;  
    }
  }

  /**
   * Create and populate the initial Alerts collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the instanceHolder, or rejected according to doPopulate, when the update is complete.
   */
  function __createAlerts() {
    // doPopulate handles any exceptions thrown by this method
    var updateInstanceHolder = function(instanceHolder, payload) {
      var alerts = new Alerts(payload);
      alerts.init();

      // Set into the holder
      instanceHolder.instance = alerts;
    };

    return util.doPopulate('/ibm/api/collective/v1/alerts', alerts, updateInstanceHolder);
  }

  /**
   * Get the Alerts collection. If the Alerts collection already exists, return the copy. 
   * Otherwise, make a REST API call to get the collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the Alerts collection, or rejected if it could not be created.
   */
  function __getAlerts() {
    if (alerts.instance) {
      // Check our created copy, don't create twice
      return returnAsDeferred(alerts.instance); 
    } else {
      var deferred = new Deferred();
      __createAlerts().then(function(instanceHolder) {
        deferred.resolve(instanceHolder.instance, true);
      }, function(err) {
        console.error('__getAlerts: Unable to create the Alert resource. Encountered an unexpected error', err);
        deferred.reject(err, true);
      });
      return deferred;  
    }
  }

  /**
   * Generic logic to determine if we have a given collection. If we do, the instance
   * is returned. Otherwise, it will be created and then returned.
   * 
   * @param {Object} cache The cache object, by type
   * @param {function} createFn The function to use to create the collection if needed
   * @return {Deferred} The returned Deferred will be resolved with the requested collection, or rejected if it could not be created.
   */
  function getOrCreateCollection(cache, createFn) {
    if (cache.collection.instance) {
      // We have a created copy, don't create twice
      return returnAsDeferred(cache.collection.instance);
    } else {
      var deferred = new Deferred();
      // We don't need to worry about looking at or setting the populating field here, the createFn handles that.
      createFn().then(function(instanceHolder) {
        deferred.resolve(instanceHolder.instance, true);
      }, function(err) {
        // The error handling for creation a collection is simple:
        // The collection REST APIs should always be present. There is never an expected time
        // when the REST API is unavailable. If it is unavailable, then that is a critical error
        // which Admin Center can not recover from or work around.
        if (err && err.response && err.response.status === 404) {
          console.error('getOrCreateCollection: Unable to create Collection resource. Encountered a 404 error', err);
          deferred.reject(err, true);
        } else {
          console.error('getOrCreateCollection: Unable to create Collection resource. Encountered an unexpected error', err);
          deferred.reject(err, true);
        }
      });
      return deferred;  
    }
  }

  /**
   * Create and populate the initial Applications collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the instanceHolder, or rejected according to doPopulate, when the update is complete.
   */
  function __createApplications() {
    // doPopulate handles any exceptions thrown by this method
    var updateInstanceHolder = function(instanceHolder, payload) {
      var initObj = payload;
      initObj.list = payload.ids; // TODO: Eventually sync the client side, for now, just map ids to list

      var applications = new Applications(initObj);
      applications.init();

      // Set into the holder
      instanceHolder.instance = applications;
    };

    return util.doPopulate('/ibm/api/collective/v1/applications', applications.collection, updateInstanceHolder);
  }

  /**
   * Get the Applications collection. If the Applications collection already exists, return the copy. 
   * Otherwise, make a REST API call to get the collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the Applications collection, or rejected if it could not be created.
   */
  function _getApplications() {
    return getOrCreateCollection(applications, __createApplications);
  }

  /**
   * Create and populate the initial Clusters collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the instanceHolder, or rejected according to doPopulate, when the update is complete.
   */
  function __createClusters() {
    // doPopulate handles any exceptions thrown by this method
    var updateInstanceHolder = function(instanceHolder, payload) {
      var initObj = payload;
      initObj.list = payload.ids; // TODO: Eventually sync the client side, for now, just map ids to list

      var clusters = new Clusters(initObj);
      clusters.init();

      // Set into the holder
      instanceHolder.instance = clusters;
    };

    return util.doPopulate('/ibm/api/collective/v1/clusters', clusters.collection, updateInstanceHolder);
  }

  /**
   * Get the Clusters collection. If Clusters already exists, return the copy. 
   * Otherwise, make a REST API call to get the collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the Clusters collection, or rejected if it could not be created.
   */
  function _getClusters() {
    return getOrCreateCollection(clusters, __createClusters);
  };

  /**
   * Create and populate the initial Hosts summary collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the instanceHolder, or rejected according to doPopulate, when the update is complete.
   */
  function __createHosts() {
    // doPopulate handles any exceptions thrown by this method
    var updateInstanceHolder = function(instanceHolder, payload) {
      var initObj = {
          up: payload.allServersRunning,
          down: payload.allServersStopped,
          unknown: payload.allServersUnknown,
          partial: payload.someServersRunning,
          empty: payload.noServers,
          list: payload.ids // TODO: Eventually sync the client side, for now, just map ids to list
      };

      var hosts = new Hosts(initObj);
      hosts.init();

      // Set into the holder
      instanceHolder.instance = hosts;
    };

    return util.doPopulate('/ibm/api/collective/v1/hosts', hosts.collection, updateInstanceHolder);
  }

  /**
   * Get the Hosts collection. If Hosts already exists, return the copy. 
   * Otherwise, make a REST API call to get the summary.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the Hosts collection, or rejected if it could not be created.
   */
  function _getHosts() {
    return getOrCreateCollection(hosts, __createHosts);
  };

  /**
   * Create and populate the initial Servers summary collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the instanceHolder, or rejected according to doPopulate, when the update is complete.
   */
  function __createServers() {
    // doPopulate handles any exceptions thrown by this method
    var updateInstanceHolder = function(instanceHolder, payload) {
      var initObj = payload;
      initObj.list = payload.ids; // TODO: Eventually sync the client side, for now, just map ids to list

      var servers = new Servers(initObj);
      servers.init();

      // Set into the holder
      instanceHolder.instance = servers;
    };

    return util.doPopulate('/ibm/api/collective/v1/servers', servers.collection, updateInstanceHolder);
  }

  /**
   * Get the Servers collection. If Servers already exists, return the copy. 
   * Otherwise, make a REST API call to get the summary.
   * 
   * 
   * @return {Deferred} The returned Deferred will be resolved with the Servers collection, or rejected if it could not be created.
   */
  function _getServers() {
    return getOrCreateCollection(servers, __createServers);
  }

  /**
   * Create and populate the initial Runtimes summary collection.
   * 
   * @return {Deferred} The returned Deferred will be resolved with the instanceHolder, or rejected according to doPopulate, when the update is complete.
   */
  function __createRuntimes() {
    // doPopulate handles any exceptions thrown by this method
    var updateInstanceHolder = function(instanceHolder, payload) {
      var initObj = {
          up: payload.allServersRunning,
          down: payload.allServersStopped,
          unknown: payload.allServersUnknown,
          partial: payload.someServersRunning,
          empty: payload.noServers,
          list: payload.ids // TODO: Eventually sync the client side, for now, just map ids to list
      };

      var runtimes = new Runtimes(initObj);
      runtimes.init();

      // Set into the holder
      instanceHolder.instance = runtimes;
    };

    return util.doPopulate('/ibm/api/collective/v1/runtimes', runtimes.collection, updateInstanceHolder);
  }

  /**
   * Get the Runtimes collection. If Runtimes already exists, return the copy. 
   * Otherwise, make a REST API call to get the summary.
   * 
   * 
   * @return {Deferred} The returned Deferred will be resolved with the Runtimes collection, or rejected if it could not be created.
   */
  function _getRuntimes() {
    return getOrCreateCollection(runtimes, __createRuntimes);
  }

  /**
   * Wrapper method to make multiple get calls to obtain all of the IDs with a given getter.
   * 
   * @param {Array.<string>} ids The list of IDs to obtain
   * @param {function} getter The method to invoke for each ID
   * @return {Deferred} Returns a Deferred which will resolve with a list of the specified Objects (the list may be empty).
   * The Deferred is rejected if the network request could not be completed.
   */
  // TODO: This should use the search API!
  function getMany(ids, getter) {
    var deferred = new Deferred();

    var gets = [];
    // For now, this is a bad implementation and we'll want to change it to use a single network request
    // using the new search API
    for (var i = 0; i < ids.length; i++) {
      gets.push(getter(ids[i]));
    }

    all(gets).then(function(allObjs) {
      deferred.resolve(allObjs, true);
    }, function(err) {
      if (err && err.response && err.response.status === 404) {
        console.error('getMany: Unable to create list of resources: ' + ids + '. Encountered a 404 error', err);
        deferred.reject(err, true);
      } else {
        console.error('getMany: Unable to create list of resources: ' + ids + '. Encountered an unexpected error', err);
        deferred.reject(err, true);
      }
    });

    return deferred;
  }

  /**
   * Wrapper method to make multiple get calls to obtain all of the IDs with a given getter.
   * 
   * @param {Array.<string>} ids The list of IDs to obtain
   * @param {string} type The type of resource the IDs are for
   * @return {Deferred} Returns a Deferred which will resolve with a list of the specified Objects. The returned list may be smaller
   * and in a different order than the requested list. The size may not match if a match is not found for a given ID, and the order
   * may not match if objects are already cached.
   */
  function getManyWithSearch(ids, type) {
    // Initialize the response Object list with the same length as the request ID list 
    var resourceList = [];
    var deferred = new Deferred();

    var url = '/ibm/api/collective/v1/search?type='+type;
    var urlIDs = '';
    for (var i = 0; i < ids.length; i++) {
      // Don't search for something we already have!
      var resource = _getCached(type, ids[i]);
      // If a rsource is marked as destroyed, need to get a new copy of the resource from the server side
      if (resource && !resource.isDestroyed) {
        resourceList.push(resource);
      } else {
        urlIDs += '&id=~eq~'+encodeURIComponent(ids[i]);
      }
    }

    // Everything was already cached, immediately return the resourceList
    if (urlIDs.length == 0) {
      deferred.resolve(resourceList, true);
      return deferred;
    } else {
      url += urlIDs;
    }

    // Some IDs were not present in the cache, try and search for them
    var options = {
        handleAs : 'json',
        preventCache : true,
        headers : {
          'Content-type' : 'application/json'
        }
    };

    xhr(url, options).then(function(response) {
      // TODO: This really, really needs to be refactored. We should not be replicating the instance holder logic here
      if (type == 'application') {
        for (var a = 0; a < response.applications.list.length; a++) {
          var payload = response.applications.list[a];
          var id = payload.id;

          var instanceHolder = applications.objects[id];
          if (!instanceHolder) {
            instanceHolder = applications.objects[id] = { instance: null, populated: false, populating: null };
          }

          // Create and set into the instance holder
          try {
            __createApplicationFromPayload(payload).then(function(application) {
               application.resourceManager = resourceManager;
               
               // Determine the cache ID value for this application
               var cacheId = application.id;
               if (application.type == 'appOnServer'){
                 cacheId = application.server.id + ',' + application.name;
               } else {    // appOnCluster
                 cacheId = application.cluster.id + ',' + application.name;
               }
               var instanceHolder = applications.objects[cacheId];
               instanceHolder.instance = application;
               instanceHolder.populated = true;

               resourceList.push(application);
               // With application, each object in the list is populated using deferred.
               // Will return the entire list once all the objects are populated.
               if (resourceList.length === ids.length) {
                 deferred.resolve(resourceList, true);
               }
            });           
            
          } catch(err) {
            console.error('While processing the list of matching applications in getManyWithSearch, failed to create the application object from the payload. This application is ignored.', err, payload);
          }
        }
      } else if (type == 'cluster') {
        for (var a = 0; a < response.clusters.list.length; a++) {
          var payload = response.clusters.list[a];
          var id = payload.id;

          var instanceHolder = clusters.objects[id];
          if (!instanceHolder) {
            instanceHolder = clusters.objects[id] = { instance: null, populated: false, populating: null };
          }

          // Create and set into the instance holder
          try {
            var cluster = __createClusterFromPayload(payload);
            instanceHolder.instance = cluster;
            instanceHolder.populated = true;

            resourceList.push(cluster);
          } catch(err) {
            console.error('While processing the list of matching clusters in getManyWithSearch, failed to create the cluster object from the payload. This cluster is ignored.', err, payload);
          }
        }
      } else if (type == 'host') {
        for (var a = 0; a < response.hosts.list.length; a++) {
          var payload = response.hosts.list[a];
          var id = payload.id;

          var instanceHolder = hosts.objects[id];
          if (!instanceHolder) {
            instanceHolder = hosts.objects[id] = { instance: null, populated: false, populating: null };
          }

          // Create and set into the instance holder
          try {
            var host = __createHostFromPayload(payload);
            instanceHolder.instance = host;
            instanceHolder.populated = true;

            resourceList.push(host);
          } catch(err) {
            console.error('While processing the list of matching hosts in getManyWithSearch, failed to create the host object from the payload. This host is ignored.', err, payload);
          }
        }
      } else if (type == 'server') {
        for (var a = 0; a < response.servers.list.length; a++) {
          var payload = response.servers.list[a];
          var id = payload.id;

          var instanceHolder = servers.objects[id];
          if (!instanceHolder) {
            instanceHolder = servers.objects[id] = { instance: null, populated: false, populating: null };
          }

          // Create and set into the instance holder
          try {
            var server = __createServerFromPayload(payload);
            instanceHolder.instance = server;
            instanceHolder.populated = true;

            resourceList.push(server);
          } catch(err) {
            console.error('While processing the list of matching servers in getManyWithSearch, failed to create the server object from the payload. This server is ignored.', err, payload);
          }
        }
      }
      if (type !== 'application') {
        deferred.resolve(resourceList, true);
      }
    }, function(err) {
      if (err && err.response && err.response.status === 404) {
        console.error('getManyWithSearch encountered a 404 error', err);
        deferred.reject(err, true);
      } else {
        console.error('getManyWithSearch encountered an unexpected error', err);
        deferred.reject(err, true);
      }
    });

    return deferred;
  }

  /**
   * Generic logic to determine if we have a given object in the cache. If we do, the instance
   * is returned. Otherwise, it will be created and then returned.
   * 
   * @param {Object} cache The cache object, by type
   * @param {string} id The ID of the object
   * @param {function} createFn The function to use to create the object if needed
   * @return {Deferred} The returned Deferred will be resolved with the requested object.
   */
  function getOrCreateObject(cache, id, createFn) {
    var instanceHolder = cache.objects[id];
    if (!instanceHolder) {
      instanceHolder = cache.objects[id] = { instance: null, populated: false, populating: null };
    }

    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete cache.objects[id];
        return getOrCreateObject(cache, id, createFn);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);    
      }
    } else {
      var deferred = new Deferred();
      // We don't need to worry about looking at or setting the populating field here, the createFn handles that.
      createFn(id, instanceHolder).then(function(instanceHolder) {
        deferred.resolve(instanceHolder.instance, true);
      }, function(err) {
        if (err && err.response && err.response.status === 404) {
          console.error('getOrCreateObject encountered a 404 error', err);
          deferred.reject(err, true);
        } else {
          console.error('getOrCreateObject encountered an unexpected error', err);
          deferred.reject(err, true);
        }
      });
      return deferred;  
    }
  }

  /**
   * Utility to get the application name from the Id.
   */
  function _getAppNameFromId(id) {
    // filter out the server/cluster name from appname {tuple|clusterName},{appName}
    var endIndex = id.lastIndexOf(",") + 1;
    var name = id.substring(endIndex);      
    return name;
  }
  
  function _getClusterOrServerName(id) {
    // filter out the server/cluster name from appname {tuple|clusterName},{appName}
    var endIndex = id.lastIndexOf(",");
    var name = id.substring(0, endIndex);      
    return name;
  }
  
  /**
   * Create and initialize the AppOnServer or AppOnCluster object from the given payload.
   * 
   * @param {object} The REST API response
   * @return {AppOnServer | AppOnCluster} The initialized AppOnServer or AppOnCluster object
   * @throws {string} Error message describing the problem
   */
  function __createApplicationFromPayload(payload) {
    var initObj = payload;
    initObj.scalingPolicy = !!payload.scalingPolicy; // Convert to a boolean
    if (payload.type === "appOnCluster") {
      initObj.servers.list = initObj.servers.ids; // TODO: Eventually sync the client side, for now, just map ids to list
    }

    var serverOrclusterName = _getClusterOrServerName(payload.id);  
    var appName = payload.name;
    if (payload.type === "appOnCluster") {
      return _getAppOnClusterSingleName(serverOrclusterName, appName);    
    } else if (payload.type === "appOnServer") {
      return _getAppOnServerSingleName(serverOrclusterName, appName);
    } 
  }
  
  /**
   * Create and populate the initial Application object for the given id.
   * 
   * @param {string} id The ID of the Application object
   * @param {object} instanceHolder The instance holder for the object
   * @return {Deferred} The returned Deferred will be resolved with the instance holder when the update is complete.
   */
  function __createApplication(id, instanceHolder) {
    
    var updateInstanceHolder = function(instanceHolder, payload) {

    var deferred = new Deferred();
      __createApplicationFromPayload(payload).then(function(application) {
        application.resourceManager = resourceManager;
        
        instanceHolder.instance = application;
        instanceHolder.populated = true;
        
        deferred.resolve(application, true);
      });

      return deferred.promise;
    };
    
    return util.doPopulate('/ibm/api/collective/v1/applications/' + encodeURIComponent(id), instanceHolder, updateInstanceHolder);
  }

  /**
   * For the given list of IDs, return a list of Application objects which match the given id(s).
   * If the ID is a single string, then a single Application object will be returned.
   * 
   * @param {(string|Array.<string>)} id The ID or list of IDs to resolve as Application objects
   * @return {Deferred} Returns a Deferred which will resolve with either an Application object or a list of Application objects {(Application|Array.<Application>)} based on the input
   */
  function _getApplication(id) {
    if (Array.isArray(id)) {
      return getManyWithSearch(id, ID.getApplication());      
    } else {
      return getOrCreateObject(applications, id, __createApplication);      
    }
  }

  /**
   * Create and initialize the Cluster object from the given payload.
   * 
   * @param {object} The REST API response
   * @return {Cluster} The initialized Cluster object
   * @throws {string} Error message describing the problem
   */
  function __createClusterFromPayload(payload) {
    var initObj = payload;
    initObj.servers.list = initObj.servers.ids; // TODO: Eventually sync the client side, for now, just map ids to list

    var cluster = new Cluster(initObj);
    cluster.resourceManager = resourceManager;
    cluster.init();

    return cluster;
  }

  /**
   * Create and populate the initial Cluster object for the given id.
   * 
   * @param {string} id The ID of the Cluster object
   * @param {object} instanceHolder The instance holder for the object
   * @return {Deferred} The returned Deferred will be resolved with the instance holder when the update is complete.
   */
  function __createCluster(id, instanceHolder) {
    var updateInstanceHolder = function(instanceHolder, payload) {
      // Create and set into the instance holder
      instanceHolder.instance = __createClusterFromPayload(payload);
    };

    return util.doPopulate('/ibm/api/collective/v1/clusters/' + encodeURIComponent(id), instanceHolder, updateInstanceHolder);
  }

  /**
   * For the given list of IDs, return a list of Cluster objects which match the given id(s).
   * If the ID is a single string, then a single Cluster object will be returned.
   * 
   * For example:
   * getCluster('cluster1') -> Deferred.resolve(new Cluster('cluster1'));
   * getCluster(['cluster1','cluster2']) -> Deferred.resolve([new Cluster('cluster1'), new Cluster('cluster2')]);
   * 
   * @param {(string|Array.<string>)} id The ID or list of IDs to resolve as Cluster objects
   * @return {Deferred} Returns a Deferred which will resolve with either an Cluster object or a list of Cluster objects {(Cluster|Array.<Cluster>)} based on the input
   */
  function _getCluster(id) {
    if (Array.isArray(id)) {
      return getManyWithSearch(id, ID.getCluster());      
    } else {
      return getOrCreateObject(clusters, id, __createCluster);      
    }
  }

  /**
   * Create and initialize the Host object from the given payload.
   * 
   * @param {object} The REST API response
   * @return {Host} The initialized Host object
   * @throws {string} Error message describing the problem
   */
  function __createHostFromPayload(payload) {
    var initObj = payload;
    initObj.servers.list = initObj.servers.ids; // TODO: Eventually sync the client side, for now, just map ids to list

    var host = new Host(initObj);
    host.resourceManager = resourceManager;
    host.init();

    return host;
  }

  /**
   * Create and populate the initial Host object for the given id.
   * 
   * @param {string} id The ID of the Host object
   * @param {object} instanceHolder The instance holder for the object
   * @return {Deferred} The returned Deferred will be resolved with the instance holder when the update is complete.
   */
  function __createHost(id, instanceHolder) {
    var updateInstanceHolder = function(instanceHolder, payload) {
      // Create and set into the instance holder
      instanceHolder.instance = __createHostFromPayload(payload);
    };

    return util.doPopulate('/ibm/api/collective/v1/hosts/' + encodeURIComponent(id), instanceHolder, updateInstanceHolder);
  }

  /**
   * For the given list of IDs, return a list of Host objects which match the given id(s).
   * If the ID is a single string, then a single Host object will be returned.
   * 
   * @param {(string|Array.<string>)} id The ID or list of IDs to resolve as Host objects
   * @return {Deferred} Returns a Deferred which will resolve with either an Host object or a list of Host objects {(Host|Array.<Host>)} based on the input
   */
  function _getHost(id) {
    if (Array.isArray(id)) {
      return getManyWithSearch(id, ID.getHost());
    } else {
      return getOrCreateObject(hosts, id, __createHost);
    }
  }

  /**
   * Create and initialize the Server object from the given payload.
   * 
   * @param {object} The REST API response
   * @return {Server} The initialized Server object
   * @throws {string} Error message describing the problem
   */
  function __createServerFromPayload(payload) {
    var initObj = payload;
    initObj.isAdminCenterServer = (util.getAdminCenterServer().id === initObj.id);

    var server = new Server(initObj);
    server.resourceManager = resourceManager;
    server.init();

    return server;
  }

  /**
   * Create and populate the initial Server object for the given id.
   * 
   * @param {string} id The ID of the Server object
   * @param {object} instanceHolder The instance holder for the object
   * @return {Deferred} The returned Deferred will be resolved with the instance holder when the update is complete.
   */
  function __createServer(id, instanceHolder) {
    var updateInstanceHolder = function(instanceHolder, payload) {
      // Create and set into the instance holder
      instanceHolder.instance = __createServerFromPayload(payload);
    };

    return util.doPopulate('/ibm/api/collective/v1/servers/' + encodeURIComponent(id), instanceHolder, updateInstanceHolder);
  }

  /**
   * For the given list of IDs, return a list of Server objects which match the given id(s).
   * If the ID is a single string, then a single Server object will be returned.
   * 
   * @param {(string|Array.<string>)} id The ID or list of IDs to resolve as Server objects
   * @return {Deferred} Returns a Deferred which will resolve with either an Server object or a list of Server objects {(Server|Array.<Server>)} based on the input
   */
  function _getServer(id) {
    if (Array.isArray(id)) {
      return getManyWithSearch(id, ID.getServer());
    } else {
      return getOrCreateObject(servers, id, __createServer);
    }
  }

  /**
   * Create the Runtime object for the given Host.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized Runtime object, or resolve with null if the Runtime is for a path not on the host, or to reject if there is an error
   * @param {Object} instanceHolder The InstanceHolder for the Runtime given the ID
   * @param {Host} hostObj The Host object.  This contains runtimes.list which is an array
   *        of objects representing a runtime. 
   * @param {String} path The path of the Runtime
   * @param {Array.<Server>} serversOnHostObjs The list of Server objects that are on the Host
   */
  function __createRuntime(deferred, instanceHolder, hostObj, path, serversOnHostObjs) {
    try {
      // We need to check to see if this runtime exists, if not, return null
      for (var i = 0; i < hostObj.runtimes.list.length; i++) {
        if (hostObj.runtimes.list[i].id === (hostObj.name+','+path)) {
          var metadata = {};
          if (Array.isArray(hostObj.runtimes.list[i].tags)) { metadata.tags = hostObj.runtimes.list[i].tags; }
          if (hostObj.runtimes.list[i].owner) { metadata.owner = hostObj.runtimes.list[i].owner; }
          if (Array.isArray(hostObj.runtimes.list[i].contacts)) { metadata.contacts = hostObj.runtimes.list[i].contacts; }
          if (hostObj.runtimes.list[i].note) { metadata.note = hostObj.runtimes.list[i].note; }

          var runtimeType = hostObj.runtimes.list[i].runtimeType ? hostObj.runtimes.list[i].runtimeType : null;
          var containerType = hostObj.runtimes.list[i].containerType ? hostObj.runtimes.list[i].containerType : null;
          var obj = new Runtime({host: hostObj, path: path, servers: serversOnHostObjs, metadata: metadata, runtimeType: runtimeType, containerType: containerType});
          obj.resourceManager = resourceManager;
          obj.init();
          instanceHolder.instance = obj;
          deferred.resolve(obj, true);
          return;
        }
      }
      console.warn('__createRuntime invoked for a path which was not defined on the host: ' + path, hostObj);
      deferred.resolve(null, true);
    } catch(err) {
      console.error('Unable to create Runtime resource. Error: ', err);
      deferred.reject(err, true);
    }
  }

  /**
   * Gets an instance or list of Runtime objects for the given id(s).
   * 
   * @param {(string|Object|Array.Object)} id The ID or list of objects with an id property 
   *                                              for which to resolve Runtime objects.
   * @return {Deferred} Returns a Deferred which resolves with the Runtime object or list of Runtime objects {(Runtime|Array.<Runtime>)}. The return value is a list if the id was a list.
   */
  function _getRuntime(id) {
    if (Array.isArray(id)) {
      return getMany(id, _getRuntime);
    } else {
      var runtimeId = (isString(id) ? id : id.id);
      var host = runtimeId.substring(0, runtimeId.indexOf(ID.getComma()));
      var path = runtimeId.substring(runtimeId.indexOf(ID.getComma())+1);
      var instanceHolder = hosts.runtimes[runtimeId];
      if (!instanceHolder) {
        instanceHolder = hosts.runtimes[runtimeId] = { instance: null, populating: null };
      }

      // We've now found or created the instanceHolder, so do what needs to be done
      if (instanceHolder.instance) {
        // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
        if (instanceHolder.instance.isDestroyed) {
          delete hosts.runtimes[runtimeId];
          return _getRuntime(id);
        } else {
          // Otherwise, we have a still-valid copy, don't create twice
          return returnAsDeferred(instanceHolder.instance);  
        }
      } else if (instanceHolder.populating) {
        // An object is already being created, return the Deferred which is handling that
        return instanceHolder.populating;
      } else {
        var deferred = new Deferred();
        instanceHolder.populating = deferred;

        _getHost(host).then(function(hostObj) {
          _getServer(hostObj.servers.list).then(function(serversList) {
            __createRuntime(deferred, instanceHolder, hostObj, path, serversList);
          }, function(err) {
            console.error('Unable to create Runtime resource. The Server list ' + hostObj.servers.list + ' could not be obtained. Error: ', err);
            deferred.reject(err, true);
          });
        }, function(err) {
          console.error('Unable to create Runtime resource. The Host ' + host + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });

        return deferred;
      }
    }
  }

  /**
   * Gets the Runtime object for the given Server object.
   * 
   * @param {Server} server The Server used to look up the Runtime 
   * @returns Deferred which resolves with the Runtime object
   */
  function _getRuntimeForServer(server) {
    var id = ID.commaDelimit(server.host, server.wlpInstallDir);
    return _getRuntime(id);
  }

  /**
   * Create the AppInstancesByCluster object.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized AppInstancesByCluster object, or to reject if there is an error
   * @param {Object} instanceHolder The instance holder for the AppInstancesByCluster object for the given Application
   * @param {Cluster} clusterObj The Cluster object
   * @param {Application} applicationObj The appOnCluster object
   * @param {Array.<AppOnServer>} appOnServerList The array of AppOnServer objects
   */
  function __createAppInstancesByCluster(deferred, instanceHolder, clusterObj, applicationObj, appOnServerList) {
    try {
      var obj = new AppInstancesByCluster({cluster: clusterObj, application: applicationObj, appOnServer: appOnServerList});      
      obj.resourceManager = resourceManager;
      instanceHolder.instance = obj;
      deferred.resolve(obj, true);
    } catch(err) {
      console.error('Unable to create instances for cluster ' + cluster.id + ' and application ' + application.name + '. Error: ', err);
      deferred.reject(err, true);
    }
  }

  /**
   * Gets the AppInstancesByCluster collection for the given cluster and application.
   * 
   * @param {(string|Cluster)} cluster Either a String or a Cluster object.
   * @param {(string|Application)} application Either a String or an Application object.
   * @return {Deferred} Returns a Deferred which resolves with the AppInstancesByCluster collection, or be rejected if it could not be created.
   */
  function _getAppInstancesByCluster(cluster, application) {    
    var id = ID.commaDelimit((isString(cluster)? cluster : cluster.id), (isString(application) ? application : application.id), ID.getAppInstancesByCluster());
        
    var instanceHolder = clusters.appInstances[id];
    if (!instanceHolder) {
      instanceHolder = clusters.appInstances[id] = { instance: null, populating: null };
    }
    
    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete clusters.appInstances[id];
        return _getAppInstancesByCluster(cluster, application);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);  
      }
    } else if (instanceHolder.populating) {
      // An object is already being created, return the Deferred which is handling that        
      return instanceHolder.populating;
    } else {
      var deferred = new Deferred();
      instanceHolder.populating = deferred;

      // Internal funtion to do the common required lookup for AppInstancesByCluster
      var createAppInstancesByCluster = function(deferred, cluster, application) {          
        _getServer(cluster.servers.list).then(function(serverList) {           
          _getAppOnServer(serverList, application.name).then(function(appOnServerList) {       	  
            __createAppInstancesByCluster(deferred, instanceHolder, cluster, application, appOnServerList);              
          }, function(err) {
            console.error('Unable to create AppInstancesByCluster resource. The AppOnServer list for ' + application.name + ' could not be obtained. Error: ', err);
            deferred.reject(err, true);
          });
        }, function(err) {
          console.error('Unable to create AppInstancesByCluster resource. The Server list ' + cluster.servers.list + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      };

      if (!isString(cluster) && !isString(application)) {     
        // We have an Application and Cluster object, so get the AppsOnServer for the cluster servers
        createAppInstancesByCluster(deferred, cluster, application);
      } else if (isString(application) && !isString(cluster)) {        
        var appId = cluster.id + "," + application;
        // Need to look up the Application object
        _getApplication(appId).then(function(applicationObj) {
          createAppInstancesByCluster(deferred, cluster, applicationObj);
        }, function(err) {
          console.error('Unable to create AppInstancesByCluster resource. The Application ' + application + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      } else if (!isString(application) && isString(cluster)) {
        // Need to look up the Cluster object
        _getCluster(cluster).then(function(clusterObj) {          
          createAppInstancesByCluster(deferred, clusterObj, application);
        }, function(err) {
          console.error('Unable to create AppInstancesByCluster resource. The Cluster ' + cluster + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      } else {
        // Need to look up both the Application and Cluster objects
        _getCluster(cluster).then(function(clusterObj) {            
          _getApplication(clusterObj.id + "," + application).then(function(applicationObj) {
            createAppInstancesByCluster(deferred, clusterObj, applicationObj);
          }, function(err) {
            console.error('Unable to create AppInstancesByCluster resource. The Application ' + application + ' could not be obtained. Error: ', err);
            deferred.reject(err, true);
          });
        }, function(err) {
          console.error('Unable to create AppInstancesByCluster resource. The Cluster ' + cluster + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      }

      return deferred;
    }
  }

  /**
   * Create the AppsOnServer object.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized AppsOnServer object, or to reject if there is an error
   * @param {Object} instanceHolder The instance holder for the AppsOnServer object for the given server
   * @param {Server} serverObj The Server object
   * @param {Array.<AppOnServer>} appOnServerList The array of AppOnServer objects
   */
  function __createAppsOnServer(deferred, instanceHolder, serverObj, appOnServerList) {
    try {
      var obj = new AppsOnServer({server: serverObj, appOnServer: appOnServerList});
      obj.resourceManager = resourceManager;
      instanceHolder.instance = obj;
      deferred.resolve(obj, true);
    } catch(err) {
      console.error('Unable to create AppsOnServer resource. Error: ', err);
      deferred.reject(err, true);
    }
  }

  /**
   * Gets the AppsOnServer object for the given server.
   * 
   * @param {(string|Server)} server Either a String or a Server object.
   * @return {Deferred} Returns a Deferred which resolves with the AppsOnServer object, or be rejected if it could not be created.
   */
  function _getAppsOnServer(server) {
    var id = ID.commaDelimit((isString(server) ? server : server.id), ID.getAppsOnServer());
    var instanceHolder = servers.appsOnServer[id];
    if (!instanceHolder) {
      instanceHolder = servers.appsOnServer[id] = { instance: null, populating: null };
    }

    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete servers.appsOnServer[id];
        return _getAppsOnServer(server);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);  
      }
    } else if (instanceHolder.populating) {
      // An object is already being created, return the Deferred which is handling that
      return instanceHolder.populating;
    } else {
      var deferred = new Deferred();
      instanceHolder.populating = deferred;

      // Need to extract the name of applications
      var getAppNames = function(serverAppsList) {
        var appNames = [];
        for (var i = 0; i < serverAppsList.length; i++) {
          appNames.push(serverAppsList[i].name);
        }
        return appNames;
      };

      if (isString(server)) {
        _getServer(server).then(function(serverObj) {
          _getAppOnServer(serverObj, getAppNames(serverObj.apps.list)).then(function(appOnServerList) {
            __createAppsOnServer(deferred, instanceHolder, serverObj, appOnServerList);
          }, function(err) {
            console.error('Unable to create AppsOnServer resource. The AppOnServer list ' + serverObj.apps.list + ' could not be obtained. Error: ', err);
            deferred.reject(err, true);
          });
        }, function(err) {
          console.error('Unable to create AppsOnServer resource. The Server ' + server + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      } else {
        _getAppOnServer(server, getAppNames(server.apps.list)).then(function(appOnServerList) {
          __createAppsOnServer(deferred, instanceHolder, server, appOnServerList);  
        }, function(err) {
          console.error('Unable to create AppsOnServer resource. The AppOnServer list ' + server.apps.list + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      }

      return deferred;
    }
  }

  /**
   * Create the AppOnServer object for the given Server and application name, if such an application exists.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized AppOnServer object, resolve with null if it does not exist, or to reject if there is an error
   * @param {Object} instanceHolder The InstanceHolder for the AppOnServer given the ID
   * @param {Server} serverObj The Server object 
   * @param {String} appName The name of the application
   */
  function __createAppOnServer(deferred, instanceHolder, serverObj, appName) {
    try {
      // We need to check to see if this app exists, if not, return null
      for (var i = 0; i < serverObj.apps.list.length; i++) {
        if (serverObj.apps.list[i].name === appName) {
          var obj = new AppOnServer({server: serverObj, name: appName});
          obj.init();
          obj.resourceManager = resourceManager;
          instanceHolder.instance = obj;
          deferred.resolve(obj, true);
          return;
        }
      }
      console.warn('__createAppOnServer invoked for an application which was not defined on the server: ' + appName, serverObj);
    } catch(err) {
      console.error('Unable to create AppOnServer resource. Error: ', err);
      deferred.reject(err, true);
    }
  }

  /**
   * Gets an instance of AppOnServer for the given server and application name.
   * 
   * @param {(string|Server)} server The server for which to resolve AppOnServer objects. This may be the Server object or just the ID of the server.
   * @param {string} name The name for which to resolve AppOnServer objects
   * @return {Deferred} Returns a Deferred which resolves with an AppOnServer object, or be rejected if it could not be created.
   */
  function _getAppOnServerSingleServerSingleName(server, appName) {
    var id = ID.commaDelimit((isString(server) ? server : server.id) , appName);
    var instanceHolder = servers.appOnServer[id];
    if (!instanceHolder) {
      instanceHolder = servers.appOnServer[id] = { instance: null, populating: null };
    }

    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete servers.appOnServer[id];
        return _getAppOnServerSingleServerSingleName(server, appName);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);  
      }
    } else if (instanceHolder.populating) {
      // An object is already being created, return the Deferred which is handling that
      return instanceHolder.populating;
    } else {
      var deferred = new Deferred();
      instanceHolder.populating = deferred;
      if (isString(server)) {
        _getServer(server).then(function(serverObj) {
          __createAppOnServer(deferred, instanceHolder, serverObj, appName);
          if (instanceHolder.instance === null) {
          	// With web socket, the server may not have updated with all the apps when
          	// getAppOnServer is called. Delete the null value appOnServer instance.
          	console.log("delete appOnServer from array");
          	delete servers.appOnServer[id];
          	deferred.resolve(null, true);
          }
        }, function(err) {
          console.error('Unable to create AppOnServer resource. The Server ' + server + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      } else {
        __createAppOnServer(deferred, instanceHolder, server, appName);
        if (instanceHolder.instance === null) {
        	// With web socket, the server may not have updated with all the apps when
        	// getAppOnServer is called. Delete the null value appOnServer instance.
        	console.log("delete appOnServer from array");
        	delete servers.appOnServer[id];
        	deferred.resolve(null, true);
        }
      }
      return deferred;
    }
  }

  /**
   * Gets an instance or list of AppOnServer for the given server(s) and application name.
   * 
   * @param {(string|Array.<string>|Server|Array.<Server>)} server The server or list of servers for which to resolve AppOnServer objects. This may be the Server object or just the ID of the server.
   * @param {string} name The name for which to resolve AppOnServer objects
   * @return {Deferred} Returns a Deferred which resolves with an AppOnServer object or list of AppOnServer objects {(AppOnServer|Array.<AppOnServer>)}. The return value if a list if server was a list.
   */
  function _getAppOnServerSingleName(server, appName) {
    // Case 1: server is an array
    if (Array.isArray(server)) {
      // We've been asked to to get multiple AppOnServer with the same name on different servers, iterate through and get each one by server
      var deferred = new Deferred();

      var gets = [];
      // For now, this is a bad implementation and we'll want to change it to use a single network request
      // using the new search API
      for (var i = 0; i < server.length; i++) {
        gets.push(_getAppOnServerSingleServerSingleName(server[i], appName));
      }

      all(gets).then(function(allObjs) {
        deferred.resolve(allObjs, true);
      });

      return deferred;
    }

    // Case 2: server is a single value
    return _getAppOnServerSingleServerSingleName(server, appName);
  }

  /**
   * Gets an instance or list of AppOnServer for the given server(s) and application name(s).
   * 
   * @param {(string|Array.<string>|Server|Array.<Server>)} server The server or list of servers for which to resolve AppOnServer objects. This may be the Server object or just the ID of the server.
   * @param {(string|Array.<string>)} name The name or list of names for which to resolve AppOnServer objects
   * @return {Deferred} Returns a Deferred which resolves with an AppOnServer object or list of AppOnServer objects {(AppOnServer|Array.<AppOnServer>)}. The return value if a list if either server or appName was a list.
   */
  function _getAppOnServer(server, appName) {
    // Case 1: appName is an array
    if (Array.isArray(appName)) {
      // We've been asked to to get multiple AppOnServer with different names, iterate through and get each one by name
      var deferred = new Deferred();

      var gets = [];
      // For now, this is a bad implementation and we'll want to change it to use a single network request
      // using the new search API
      for (var i = 0; i < appName.length; i++) {
        gets.push(_getAppOnServerSingleName(server, appName[i]));
      }

      all(gets).then(function(allObjs) {
        // TODO: We need to resolve this but we need to strip out nulls. Nulls are not real objects.
        deferred.resolve(allObjs, true);
      });

      return deferred;
    }

    // Case 2: appName is a single value
    return _getAppOnServerSingleName(server, appName);
  }

  /**
   * Create the AppsOnCluster object.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized AppsOnCluster object, or to reject if there is an error
   * @param {Object} instanceHolder The InstanceHolder for the AppsOnCluster given the ID
   * @param {Cluster} clusterObj The Cluster object
   * @param {Array.<AppOnCluster>} appOnClusterList The list of AppOnCluster
   */
  function __createAppsOnCluster(deferred, instanceHolder, clusterObj, appOnClusterList) {
    try {
      var obj = new AppsOnCluster({cluster: clusterObj, appOnCluster: appOnClusterList});
      obj.resourceManager = resourceManager;
      instanceHolder.instance = obj;
      deferred.resolve(obj, true);
    } catch(err) {
      console.error('Unable to create AppsOnCluster resource. Error: ', err);
      deferred.reject(err, true);
    }
  }
  
  /**
   * Extracts the names from the embedded colleciton of AppOnCluster objects. 
   * 
   * TODO: This is really silly, and we shouldn't need to do this extra level of processing.
   * 
   * @return {Array} The array of AppOnCluster names
   */
  function __getAppOnClusterNames(cluster) {
    var appOnClusterNames = [];
    for(var i = 0; i < cluster.apps.list.length; i++) {
      appOnClusterNames.push(cluster.apps.list[i].name);
    }
    return appOnClusterNames;
  }

  /**
   * Gets an instance of AppsOnCluster for the given cluster.
   * 
   * @param cluster Either a String or an Object. If String, then the cluster is looked up.
   * @return {Deferred} Returns a Deferred which resolves with an AppsOnCluster object, or be rejected if it could not be created.
   */
  function _getAppsOnCluster(cluster) {
    var id = ID.commaDelimit((isString(cluster) ? cluster : cluster.id), ID.getAppsOnCluster());
    var instanceHolder = clusters.appsOnCluster[id];
    if (!instanceHolder) {
      instanceHolder = clusters.appsOnCluster[id] = { instance: null, populating: null };
    }

    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete clusters.appsOnCluster[id];
        return _getAppsOnCluster(cluster);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);  
      }
    } else if (instanceHolder.populating) {
      // An object is already being created, return the Deferred which is handling that
      return instanceHolder.populating;
    } else {
      var deferred = new Deferred();
      instanceHolder.populating = deferred;
      if (isString(cluster)) {
        _getCluster(cluster).then(function(clusterObj) {
          var appOnClusterNames = __getAppOnClusterNames(clusterObj);
          _getAppOnCluster(clusterObj, appOnClusterNames).then(function(appOnClusterList) {
            __createAppsOnCluster(deferred, instanceHolder, clusterObj, appOnClusterList);  
          }, function(err) {
            console.error('Unable to create AppsOnCluster resource. The AppOnCluster list ' + appOnClusterNames + ' could not be obtained. Error: ', err);
            deferred.reject(err, true);
          });
        }, function(err) {
          console.error('Unable to create AppsOnCluster resource. The Cluster ' + cluster + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      } else {
        var appOnClusterNames = __getAppOnClusterNames(cluster);
        _getAppOnCluster(cluster, appOnClusterNames).then(function(appOnClusterList) {
          __createAppsOnCluster(deferred, instanceHolder, cluster, appOnClusterList);  
        }, function(err) {
          console.error('Unable to create AppsOnCluster resource. The AppOnCluster list ' + appOnClusterNames + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      }
      return deferred;
    }
  }

  /**
   * Create the ServersOnCluster object.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized ServersOnCluster object, or to reject if there is an error
   * @param {Object} instanceHolder The InstanceHolder for the ServersOnCluster given the ID
   * @param {Cluster} clusterObj The Cluster object
   * @param {Array.<Server>} serversOnClusterObjs The list of Server object
   */
  function __createServersOnCluster(deferred, instanceHolder, clusterObj, serversOnClusterObjs) {
    try {
      var obj = new ServersOnCluster({cluster: clusterObj, servers: serversOnClusterObjs});
      obj.resourceManager = resourceManager;
      instanceHolder.instance = obj;
      deferred.resolve(obj, true);
    } catch(err) {
      console.error('Unable to create ServersOnCluster resource. Error: ', err);
      deferred.reject(err, true);
    }
  }

  /**
   * Gets an instance of ServersOnCluster for the given cluster.
   * 
   * @param cluster Either a String or an Object. If String, then the cluster is looked up.
   * @return {Deferred} Returns a Deferred which resolves with an ServersOnCluster object, or be rejected if it could not be created.
   */
  function _getServersOnCluster(cluster) {
    var id = ID.commaDelimit((isString(cluster) ? cluster : cluster.id), ID.getServersOnCluster());
    var instanceHolder = clusters.serversOnCluster[id];
    if (!instanceHolder) {
      instanceHolder = clusters.serversOnCluster[id] = { instance: null, populating: null };
    }

    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete clusters.serversOnCluster[id];
        return _getServersOnCluster(cluster);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);  
      }
    } else if (instanceHolder.populating) {
      // An object is already being created, return the Deferred which is handling that
      return instanceHolder.populating;
    } else {
      var deferred = new Deferred();
      instanceHolder.populating = deferred;
      if (isString(cluster)) {
        _getCluster(cluster).then(function(clusterObj) {
          _getServer(clusterObj.servers.list).then(function(serversList) {
            __createServersOnCluster(deferred, instanceHolder, clusterObj, serversList);
          }, function(err) {
            console.error('Unable to create ServersOnCluster resource. The Server list ' + clusterObj.servers.list + ' could not be obtained. Error: ', err);
            deferred.reject(err, true);
          });
        }, function(err) {
          console.error('Unable to create ServersOnCluster resource. The Cluster ' + cluster + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      } else {
        _getServer(cluster.servers.list).then(function(serversList) {
          __createServersOnCluster(deferred, instanceHolder, cluster, serversList);
        }, function(err) {
          console.error('Unable to create ServersOnCluster resource. The Server list ' + cluster.servers.list + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      }
      return deferred;
    }
  }

  /**
   * Create the AppOnCluster object.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized AppOnCluster object, or to reject if there is an error
   * @param {Object} instanceHolder The instance holder for the AppOnCluster object for the given Cluster
   * @param {Cluster} clusterObj The Cluster object
   * @param {String} name The name of the application
   */
  function __createAppOnCluster(deferred, instanceHolder, clusterObj, name) {
    try {
      // Could not add the new codes as it depends on whether the apps.list has updated
      // before getting the card. For now, keeping the old codes.
      // We need to check to see if this app exists, if not, return null
//      for (var i = 0; i < clusterObj.apps.list.length; i++) {
//        if (clusterObj.apps.list[i].name === name) {
          var obj = new AppOnCluster({cluster: clusterObj, name: name});
          obj.resourceManager = resourceManager;
          obj.init();
          instanceHolder.instance = obj;
          deferred.resolve(obj, true);
//          return;
//        }
//      }
//      console.warn('__createAppOnCluster invoked for an application which was not defined on the cluster: ' + name, clusterObj);
//      deferred.resolve(null, true);
    } catch(err) {
      console.error('Unable to create AppOnCluster resource. Error: ', err);
      deferred.reject(err, true);
    }
  }

  /**
   * Gets the AppOnCluster collection for the given cluster and application.
   * 
   * @param {(string|Cluster)} cluster Either a String or a Cluster object.
   * @param {(string)} name The name of the application.
   * @return {Deferred} Returns a Deferred which resolves with the AppOnCluster collection, or be rejected if it could not be created.
   */
  function _getAppOnClusterSingleName(cluster, name) {
    var id = ID.commaDelimit((isString(cluster) ? cluster : cluster.id), name, ID.getAppOnCluster());
    var instanceHolder = clusters.appOnCluster[id];
    if (!instanceHolder) {
      instanceHolder = clusters.appOnCluster[id] = { instance: null, populating: null };
    }

    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete clusters.appOnCluster[id];
        return _getAppOnClusterSingleName(cluster, name);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);  
      }
    } else if (instanceHolder.populating) {
      // An object is already being created, return the Deferred which is handling that
      return instanceHolder.populating;
    } else {
      var deferred = new Deferred();
      instanceHolder.populating = deferred;

      if (!isString(cluster)) {
        // We have the required Cluster object
        __createAppOnCluster(deferred, instanceHolder, cluster, name);
      } else if (isString(cluster)) {
        // Need to look up the Cluster object
        _getCluster(cluster).then(function(clusterObj) {
          __createAppOnCluster(deferred, instanceHolder, clusterObj, name);
        }, function(err) {
          console.error('Unable to create AppOnCluster resource ' + name + '. The Cluster ' + cluster + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      }

      return deferred;
    }
  }

  /**
   * Gets an instance or list of AppOnCluster for the given cluster and application name(s).
   * 
   * @param {(string|Cluster)} cluster The Cluster id or object
   * @param {(string|Array.<string>)} name The application name for which to resolve AppOnCluster objects
   * @return {Deferred} Returns a Deferred which resolves with an AppOnCluster object or list of AppOnCluster objects {(AppOnCluster|Array.<AppOnCluster>)}. The return value if a list if application was a list.
   */
  function _getAppOnCluster(cluster, name) {
    // Case 1: appName is an array
    if (Array.isArray(name)) {
      // We've been asked to to get multiple AppOnServer with different names, iterate through and get each one by name
      var deferred = new Deferred();

      var gets = [];
      // For now, this is a bad implementation and we'll want to change it to use a single network request
      // using the new search API
      for (var i = 0; i < name.length; i++) {
        gets.push(_getAppOnClusterSingleName(cluster, name[i]));
      }

      all(gets).then(function(allObjs) {
        deferred.resolve(allObjs, true);
      });

      return deferred;
    }

    // Case 2: name is a single value
    return _getAppOnClusterSingleName(cluster, name);
  }

  /**
   * Create the RuntimesOnHost object for the given Host.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized RuntimesOnHost object, or to reject if there is an error
   * @param {Object} instanceHolder The InstanceHolder for the RuntimesOnHost given the ID
   * @param {Host} hostObj The Host object
   * @param {Array.<Runtime>} runtimesList The list of Runtime objects
   */
  function __createRuntimesOnHost(deferred, instanceHolder, hostObj, runtimeList) {
    try {
      var obj = new RuntimesOnHost({host: hostObj, runtime: runtimeList});
      obj.resourceManager = resourceManager;
      instanceHolder.instance = obj;
      deferred.resolve(obj, true);
    } catch(err) {
      console.error('Unable to create RuntimesOnHost resource. Error: ', err);
      deferred.reject(err, true);
    }
  }

  /**
   * Gets an instance of RuntimesOnHost for the given Host.
   * 
   * @param host Either a String or an Object. If String, then the host is looked up.
   * @return {Deferred} Returns a Deferred which resolves with the RuntimesOnHost object, or be rejected if it could not be created.
   */
  function _getRuntimesOnHost(host) {
    var id = ID.commaDelimit((isString(host) ? host : host.id), ID.getRuntimesOnHost());
    var instanceHolder = hosts.runtimesOnHost[id];
    if (!instanceHolder) {
      instanceHolder = hosts.runtimesOnHost[id] = { instance: null, populating: null };
    }

    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete hosts.runtimesOnHost[id];
        return _getRuntimesOnHost(host);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);  
      }
    } else if (instanceHolder.populating) {
      // An object is already being created, return the Deferred which is handling that
      return instanceHolder.populating;
    } else {
      var deferred = new Deferred();
      instanceHolder.populating = deferred;
      if (isString(host)) {
        _getHost(host).then(function(hostObj) {
          _getRuntime(hostObj.runtimes.list).then(function(runtimeList) {
            __createRuntimesOnHost(deferred, instanceHolder, hostObj, runtimeList);
          }, function(err) {
            console.error('Unable to create RuntimesOnHost resource. The Runtime list ' + hostObj.runtimes + ' could not be obtained. Error: ', err);
            deferred.reject(err, true);
          });
        }, function(err) {
          console.error('Unable to create RuntimesOnHost resource. The Host ' + host + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      } else {
        _getRuntime(host.runtimes.list).then(function(runtimeList) {
          __createRuntimesOnHost(deferred, instanceHolder, host, runtimeList);
        }, function(err) {
          console.error('Unable to create RuntimesOnHost resource. The Runtime list ' + host.runtimes + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      }
      return deferred;
    }
  }

  /**
   * Create the ServersOnHost object for the given Host.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized ServersOnHost object, or to reject if there is an error
   * @param {Object} instanceHolder The InstanceHolder for the ServersOnHost given the ID
   * @param {Host} hostObj The Host object 
   * @param {Array.<Server>} serversList The list of Server objects for the Host
   */
  function __createServersOnHost(deferred, instanceHolder, hostObj, serversList) {
    try {
      var obj = new ServersOnHost({host: hostObj, servers: serversList});
      obj.resourceManager = resourceManager;
      instanceHolder.instance = obj;
      deferred.resolve(obj, true);
    } catch(err) {
      console.error('Unable to create ServersOnHost resource. Error: ', err);
      deferred.reject(err, true);
    }
  }

  /**
   * Gets an instance of ServersOnHost for the given Host.
   * 
   * @param {(string|Host)} host Either a String or an Object. If String, then the host is looked up.
   * @return {Deferred} Returns a Deferred which resolves with the ServersOnHost object, or be rejected if it could not be created.
   */
  function _getServersOnHost(host) {
    var id = ID.commaDelimit((isString(host) ? host : host.id), ID.getServersOnHost());
    var instanceHolder = hosts.serversOnHost[id];
    if (!instanceHolder) {
      instanceHolder = hosts.serversOnHost[id] = { instance: null, populating: null };
    }

    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete hosts.serversOnHost[id];
        return _getServersOnHost(host);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);  
      }
    } else if (instanceHolder.populating) {
      // An object is already being created, return the Deferred which is handling that
      return instanceHolder.populating;
    } else {
      var deferred = new Deferred();
      instanceHolder.populating = deferred;
      if (isString(host)) {
        _getHost(host).then(function(hostObj) {
          _getServer(hostObj.servers.list).then(function(serversList) {
            __createServersOnHost(deferred, instanceHolder, hostObj, serversList);  
          }, function(err) {
            console.error('Unable to create ServersOnHost resource. The Server list ' + hostObj.servers.list + 'could not be obtained. Error: ', err);
            deferred.reject(err, true);
          });
        }, function(err) {
          console.error('Unable to create ServersOnHost resource. The Host ' + host + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      } else {
        _getServer(host.servers.list).then(function(serversList) {
          __createServersOnHost(deferred, instanceHolder, host, serversList);  
        }, function(err) {
          console.error('Unable to create ServersOnHost resource. The Server list ' + host.servers.list + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      }
      return deferred;
    }
  }

  /**
   * Create the ServersOnRuntime object.
   * 
   * @param {Deferred} deferred The Deferred the resolve with the created, initialized ServersOnRuntime object, or to reject if there is an error 
   * @param {Object} instanceHolder The instance holder for the ServersOnRuntime object for the given Runtime
   * @param {Server} runtimeObj The Runtime object
   */
  function __createServersOnRuntime(deferred, instanceHolder, runtimeObj) {
    try {
      var obj = new ServersOnRuntime({runtime: runtimeObj});
      obj.resourceManager = resourceManager;
      instanceHolder.instance = obj;
      deferred.resolve(obj, true);
    } catch(err) {
      console.error('Unable to create ServersOnRuntime resource. Error: ', err);
      deferred.reject(err, true);
    }
  }

  /**
   * Gets an instance of ServersOnRuntime for the given Runtime.
   * 
   * @param {(string|Runtime)} runtime Either the Runtime object or the runtime ID
   * @return {Deferred} Returns a Deferred which resolves with the ServersOnRuntime object, or be rejected if it could not be created.
   */
  function _getServersOnRuntime(runtime) {
    var id = ID.commaDelimit((isString(runtime) ? runtime : runtime.id), ID.getServersOnRuntime());
    var instanceHolder = hosts.serversOnRuntime[id];
    if (!instanceHolder) {
      instanceHolder = hosts.serversOnRuntime[id] = { instance: null, populating: null };
    }

    // We've now found or created the instanceHolder, so do what needs to be done
    if (instanceHolder.instance) {
      // If the instance was removed, we need to invalidate the cache entry (delete it) and create a new one
      if (instanceHolder.instance.isDestroyed) {
        delete hosts.serversOnRuntime[id];
        return _getServersOnRuntime(runtime);
      } else {
        // Otherwise, we have a still-valid copy, don't create twice
        return returnAsDeferred(instanceHolder.instance);  
      }
    } else if (instanceHolder.populating) {
      // An object is already being created, return the Deferred which is handling that
      return instanceHolder.populating;
    } else {
      var deferred = new Deferred();
      instanceHolder.populating = deferred;

      if (isString(runtime)) {
        _getRuntime(runtime).then(function(runtimeObj) {
          __createServersOnRuntime(deferred, instanceHolder, runtimeObj);
        }, function(err) {
          console.error('Unable to create ServersOnRuntime resource. The Runtime ' + runtime + ' could not be obtained. Error: ', err);
          deferred.reject(err, true);
        });
      } else {
        __createServersOnRuntime(deferred, instanceHolder, runtime);  
      }

      return deferred;
    }
  }

  var globalSearchResults = null;
  function __populateSearch(collection, options) {
    var deferred = new Deferred();
    collection.populate(options).then(function(populatedCollection) {
      deferred.resolve(populatedCollection, true);
    }, function(err) {
      console.error('Error populating the Search collection. Error: ', err);
      deferred.reject(err, true);
    });
    return deferred;
  }

  function _getSearchResults(options) {
    /**Design is now asking to remove the "Search Results" button. Leaving here in case it changes back
    registry.byId('breadcrumbPane-id').setSearchBreadCrumb(true) ;
     */   

    if (!globalSearchResults) {
      globalSearchResults = new Search();
      globalSearchResults.resourceManager = resourceManager;
    }
    return __populateSearch(globalSearchResults, options);
  }

  /**
   * Gets the cached copy of the type / id resource. If the resource is not available,
   * null is returned.
   * 
   * @param {string} type The type of the collection or object to return
   * @param {string} id The ID of the object to return, if the type is of an object
   * @return {Object} The collection or object requested, or null if it has not been loaded 
   */
  function _getCached(type, id) {
    if (type === 'standaloneServer') {
      return standaloneServer.instance;
    } else if (type === 'summary') {
      return summary.instance;
    } else if (type === 'alerts') {
      return alerts.instance;
    } else if (type === 'applications') {
      return applications.collection.instance;
    } else if (type === 'application') {
      if (applications.objects[id]) {
        return applications.objects[id].instance;
      }
    } else if (type === 'clusters') {
      return clusters.collection.instance;
    } else if (type === 'cluster') {
      if (clusters.objects[id]) {
        return clusters.objects[id].instance;
      }
    } else if (type === 'servers') {
      return servers.collection.instance;
    } else if (type === 'server') {
      if (servers.objects[id]) {
        return servers.objects[id].instance;
      }
    } else if (type === 'hosts') {
      return hosts.collection.instance;
    } else if (type === 'host') {
      if (hosts.objects[id]) {
        return hosts.objects[id].instance;
      }
    } else if (type === 'runtimes') {
      return runtimes.collection.instance;
    } else if (type === 'runtime') {
      if (runtimes.objects[id]) {
        return runtimes.objects[id].instance;
      }
    } else {
      return null;
    }
  }

  return resourceManager;
});

