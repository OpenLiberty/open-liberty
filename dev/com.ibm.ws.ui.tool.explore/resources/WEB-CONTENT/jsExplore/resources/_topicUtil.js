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
 * This topicUtil module is intended to ensure that all of our topic construction is normalized and common.
 * 
 * A word on terminology:
 * event - the payload delivered to a topic
 * topic - the name to which an event is delivered
 * notification - (TODO: refine this) interchangeable with event. Usually indicates a specific type of event, e.g. state change notification
 * 
 * A word on the topic structure:
 * The topic structure mimics the URL structure. See hashUtils.js.
 * 
 * Collections and their topic structure:
 * /explore-1.0/servers - servers
 * /explore-1.0/clusters - clusters
 * /explore-1.0/applications - applications
 * /explore-1.0/hosts - hosts
 * /explore-1.0/runtimes - runtimes
 * 
 * Stateful Resources and their topic structure:
 * /explore-1.0/servers/serverTuple - server
 * /explore-1.0/clusters/clusterName - cluster
 * /explore-1.0/clusters/clusterName/apps/appName - app on cluster
 * /explore-1.0/servers/serverTuple/apps/appName - application instance, app on server
 * 
 * Non-Stateful Resources and their topic structure:
 * /explore-1.0/hosts/hostName - host
 * /explore-1.0/runtimes/runtimeName - runtime
 * 
 * A few notes on dojo topics:
 * 1. dojo topics do not support coomas. As such, this utility class will 'normalize' all topic
 * strings into something that is valid for dojo topics. This most importantly applies to
 * a server tuple, which has the format of 'host,userdir,server'. It is for this reason that
 * we need to convert commas to another character.
 * 
 * 2. dojo uses commas to separate listening topics when subscribing an event handler.
 * Commas can not be used in publishing topics.
 * 
 * The following is valid:
 * topic.subscribe('a,b', function() {});
 * topic.public('a', event);
 * topic.public('b', event);
 * 
 * The following is not valid:
 * topic.subscribe('a', function() {});
 * topic.subscribe('b', function() {});
 * topic.public('a,b', event);
 * 
 * 3. dojo topics do not support wildcarding. While we have namespaces for each type of resource,
 * a listener can not be established on an entire namespace. The following is not valid:
 * topic.subscribe('/explore-1.0/servers/*', function() {});
 * 
 * 4. We are using dojo/topic. I am not sure if we should consider something smaller or different:
 * https://github.com/cowboy/jquery-tiny-pubsub
 * 
 */

/**
 * The eventUtil module documents all events and event conventions.
 * 
 * Event names, such as 'State Event', are terms which are applied to objects with a particular
 * format. There is no formal structure around the objects. They are not classes. All of the events
 * we use can be combined in such a way that the union of the objects can be decomposed into the
 * individual event types. The only common field of all events is 'origin'.
 * 
 * The 'origin' field indicates where the event originated from. This is solely for debugging purposes
 * and is not used by the runtime. The origin field should be set to the file and line number of the
 * event publisher. The line number should be as 'common' as possible, so shared code which publishes
 * events should have the origin passed in, and should not be the origin of the shared file or line.
 * 
 * State Event, represents a resource state change:
 * {
 *   state: 'new state value' ('STARTED', 'STARTING', 'STOPPED', 'STOPPING', 'PARTIALLY_STARTED', 'UNKNOWN'),
 *   origin: 'file.lineNumber' or 'class.method.lineNumber' -- used for debugging only
 * }
 */

define(['dojo/topic', 'js/common/tr'], function(topic, tr) {

  /**
   * Replace all commas as dojo topics do not support commas.
   */
  function __removeCommas(str) {
    return str.replace(/,/g, '#');
  }

  /**
   * Builds the topic string.
   */
  function __buildTopic(topicStr) {
    return __removeCommas('/explore-1.0/' + topicStr);
  }

  /**
   * Obtain the topic string by type.
   */
  function __getTopicByType(type, x, y, z) {
    switch (type) {
    case 'standaloneServer':
      return __buildTopic('standaloneServer');
    case 'summary':
      return __buildTopic('summary');
    case 'alerts':
      return __buildTopic('alerts');
    case 'servers':
      return __buildTopic('servers');
    case 'serversOnHost':
      return __buildTopic('hosts/' + x + '/servers');
    case 'serversOnCluster':
      return __buildTopic('clusters/' + x + '/servers');
    case 'serversOnRuntime':
      return __buildTopic('hosts/' + x + '/runtimes/' + y + '/servers');
    case 'server':
      var tuple = x;
      if (x && y && z) {
        // All 3 args, likely a broken up tuple
        var host = x;
        var userdir = y;
        var server = z;
        tuple = host + ',' + userdir + ',' + server;
      }
      return __buildTopic('servers/' + tuple);
    case 'clusters':
      return __buildTopic('clusters');
    case 'cluster':
      return __buildTopic('clusters/' + x);
    case 'hosts':
      return __buildTopic('hosts');
    case 'host':
      return __buildTopic('hosts/' + x);
    case 'applications':
      return __buildTopic('applications');
    case 'application':
      return __buildTopic('applications/' + x);
    case 'appsOnServer':
      return __buildTopic('servers/' + x + '/apps');
    case 'appOnServer':
      return __buildTopic('servers/' + x + '/apps/' + y);
    case 'appsOnCluster':
      return __buildTopic('clusters/' + x + '/apps');
    case 'appOnCluster':
      return __buildTopic('clusters/' + x + '/apps/' + y);
    case 'appInstancesByCluster':
      return __buildTopic('clusters/' + x + '/apps/' + y + '/instances');
    case 'runtimes':
      return __buildTopic('runtimes');
    case 'runtimesOnHost':
      return __buildTopic('hosts/' + x + '/runtimes');
    case 'runtime':
      return __buildTopic('runtimes/' + x + ',' + y);
    default:
      tr.throwMsg('Attempting to compute topic for an unknown type: ' + type);
    }
  }

  return {
    /**
     * Gets the event topic for a given resource.
     * 
     * @param {Resource} resource The resource for which the topic will be computed
     * @return {String} The topic for the given resource
     */
    getTopic : function(resource) {
      if (resource.type === 'appOnServer' || resource.type === 'appsOnServer') {
        return __getTopicByType(resource.type, resource.server.id, resource.name);
      } else if (resource.type === 'appOnCluster' || resource.type === 'appsOnCluster' || resource.type === 'serversOnCluster' || resource.type === 'appInstancesByCluster') {
        return __getTopicByType(resource.type, resource.cluster.id, resource.name);
      } else if (resource.type === 'serversOnHost' || resource.type === 'runtimesOnHost') {
        return __getTopicByType(resource.type, resource.host.id);
      } else if (resource.type === 'serversOnRuntime') {
        return __getTopicByType(resource.type, resource.host.id, resource.path);
      } else if (resource.type === 'runtime') {
        // Currently, the runtime object's id is {host},{runtime} so we need to parse it into two seperate parts.
        // Alternatively we could change the server side to return the id to just be the runtime and have a host.id for the hostname.
        // We could also make this change on the client side, but that would mean the client and server side resources would not be in sync. 
        var host = resource.id.substring(0, resource.id.indexOf(','));
        var runtime = resource.id.substring(resource.id.indexOf(',') + 1);
    	return __getTopicByType(resource.type, host, runtime);
      } else {
        return __getTopicByType(resource.type, resource.id);
      }
    },

    /**
     * Gets the event topic for a given type and name or tuple components.
     * 
     * @param {String} type The type of resource
     * @param {String} x The resource name, the server tuple, or the host if specifying parts of a tuple
     * @param {String} y The userdir if specifying parts of a tuple
     * @param {String} z The server name if specifying parts of a tuple
     * @return {String} The computed topic
     */
    getTopicByType : __getTopicByType,
    
    /**
     * Wrapper for dojo topic's publish method. The key behaviour of the wrapper is to
     * try / catch the call, so that a misbehaving listener won't kill the code flow.
     * This method is used in our resource notification system.
     * 
     * @param {string} t The topic to publish to
     * @param {object} payload The payload to send
     */
    publish: function(t, payload) {
      try {
        topic.publish(t, payload);
      } catch(err) {
        tr.ffdc('Error occurred while publishing to topic ' + t, err, payload);
      }
    }
  };
});
