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
define([ 'dojo/_base/declare', 'dojo/_base/lang', 'dojo/Deferred', 'dojo/request', 'dojo/request/xhr', './_StatelessResource',
         'dojo/i18n!../nls/resourcesMessages', 'jsExplore/utils/ID' ],
         function(declare, lang, Deferred, request, xhr, StatelessResource, i18n, ID) {

  /**
   * The StandaloneServer object represents a single, stand alone server which may or may not be a member
   * of the collective. The server is considered to be stateless because it can only be running when the
   * server is being accessed. It does expose a stop operation.
   * 
   * The StandaloneServer object intentionally mirrors the Server object, except it does not posses alerts.
   * 
   * The cumulative public attributes for StandaloneServer are as follows:
   * @class
   * @typedef {Object} StandaloneServer
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} name                  - The resource's display name, defaults to the ID
   * @property {string} userdir               - The server's user directory
   * @property {string} host                  - The host on which the server is defined
   * @property {string} cluster               - The cluster to which the server belongs. If null then the server does not belong to a cluster.
   * @property {string} scalingPolicy         - The scaling policy name which is applied to this server. It comes from the cluster. If null then no policy is applied.
   * @property {boolean} scalingPolicyEnabled - Flag to indicate whether or not the scaling policy which is applied to this server is enabled
   * @property {Object} apps                  - The set of applications which are deployed to the server
   * @property {number} apps.up               - The number of applications which are running
   * @property {number} apps.down             - The number of applications which are stopped
   * @property {number} apps.unknown          - The number of applications which are in an unknown state
   * @property {Array.<Object>} apps.list     - The list of applications which are deployed to server. The list will have no duplicate values.
   * @property {string} apps.list[i].name     - The name of the application
   * @property {string} apps.list[i].state    - The state of the application
   * @property {function} stop                - The stop operation for the resource
   * @property {string} explorerURL           - URL to formatted Liberty REST APIs if server has apiDiscovery feature (ibm/api/explorer)
   */
  return declare('StandaloneServer', [StatelessResource], {
    /** Hard-code the id and type to be 'standaloneServer' **/
    /** @type {string} */ id: ID.getStandaloneServer(),
    /** @type {string} */ type: 'standaloneServer',

    /** These values are injected from constructor, they should be modified after creation **/
    /** @type {string} */ host: null,
    /** @type {string} */ userdir: null,
    /** @type {string} */ name: null,

    /** Set during construction and handleChangeEvent **/
    /** @type {string} */ cluster: null,
    /** @type {string} */ scalingPolicy: null,
    /** @type {boolean} */ scalingPolicyEnabled: null,
    /** @type {Object} */ apps: null,
    /** @type {string} */ explorerURL: null,

    /**
     * Construct the initial Standalone Server state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!init) this._throwError('StandaloneServer created without an initialization object');
      (init.host) ? this.host = init.host : this._throwError('StandaloneServer created without a host');
      (init.userdir) ? this.userdir = init.userdir : this._throwError('StandaloneServer created without a userdir');
      (init.name) ? this.name = init.name : this._throwError('StandaloneServer created without a name');
      this.id = this.name; // Set the ID to the server's name
      if (init.cluster) { this.cluster = init.cluster; } // Set if present
      if (init.scalingPolicy) { this.scalingPolicy = init.scalingPolicy; } // Set if present
      if (init.scalingPolicyEnabled) { this.scalingPolicyEnabled = init.scalingPolicyEnabled; } // Set if present

      (init.apps) ? this.apps = init.apps : this._throwError('StandaloneServer created without an initial apps object');
      (init.apps.up >= 0) ? this.apps.up = init.apps.up : this._throwError('StandaloneServer created without an initial apps.up tally');
      (init.apps.down >= 0) ? this.apps.down = init.apps.down : this._throwError('StandaloneServer created without an initial apps.down tally');
      (init.apps.unknown >= 0) ? this.apps.unknown = init.apps.unknown : this._throwError('StandaloneServer created without an initial apps.unknown tally');
      (Array.isArray(init.apps.list)) ? this.apps.list = init.apps.list : this._throwError('StandaloneServer created without an initial apps.list array');
      // Be paranoid, ensure the list is is a list of name/state objects. We don't do this type checking elsewhere because everything else is supposed to be
      // Strings, and this is the one deviant case.
      for (var i = 0; i < this.apps.list.length; i++) {
        var curApp = this.apps.list[i];
        if (!(curApp.hasOwnProperty('name') && curApp.hasOwnProperty('state'))) {
          this._throwError('StandaloneServer created without an initial apps.list array with some elements which are not objects with name and state');
        }
      }
      
      // Determine if the apiDiscovery feature is enabled on this server by directly checking
      // to see if you can access the explorer URL or not.
      var url = window.location.protocol + "//" + window.location.host + '/ibm/api/explorer/';
      var options = {
          preventCache : true
      };
      request.get(url, options).then(lang.hitch(this, function(response) {
        // The url is accessible.  Update the ObjectViewHeaderPane to show the link by 
        // invoking the OVHP onApiDiscoveryChange method.
        this.explorerURL = url;
        this._updateAttribute('onApiDiscoveryChange', this, 'explorerURL');
      }), lang.hitch(this, function(err) {
        // The apiDiscovery feature was not enabled.
        this.exlorerURL = '';
      }));
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * StandaloneServer Event {
     *   type: 'standaloneServer',
     *   cluster, scalingPolicy, scalingPolicyEnabled, (optional)
     *   apps: {
     *     up, down, unknown,
     *     added: [ { name, state } ],   (optional)
     *     removed: [ name ],   (optional)
     *     changed: [ { name, state } ]   (optional)
     *   } (optional)
     * }
     * 
     * If the inbound event has the wrong type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following observer methods may be called:
     * -- onClusterChange(newCluster, oldCluster) - parameters are Strings
     * -- onScalingPolicyChange(newScalingPolicy, oldScalingPolicy) - parameters are Strings
     * -- onScalingPolicyEnabledChange(newScalingPolicyEnabled, oldScalingPolicyEnabled) - parameters are boolean
     * -- onAppsTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown, partial}
     * -- onAppsListChange(newList, oldList, added, removed, changed) - parameters are Array.{name,state} except for removed, which is Array.<string>
     * 
     * @param {Object} e The received StandaloneServer Event object
     */
    _handleChangeEvent: function(e) {
      console.log('StandaloneServer got an event!', e);

      if (e.type !== this.type) { 
        console.error('StandaloneServer '+this.id+' got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      this._updateAttribute('onClusterChange', e, 'cluster');
      this._updateAttribute('onScalingPolicyChange', e, 'scalingPolicy');
      this._updateAttribute('onScalingPolicyEnabledChange', e, 'scalingPolicyEnabled');

      if (e.apps) {
        this._updateTally('onAppsTallyChange', e.apps, this.apps, ['up', 'down', 'unknown']);

        // Process removals and changes at the same time. This maybe an opportunity to refactor _updateArray?
        // Probably not the most efficient implementation here, but it should work for now as the lists should be small.
        if (e.apps.added || e.apps.changed || e.apps.removed) {
          var prevList = this.apps.list.slice(0); // Make a shallow copy of the list

          if (e.apps.changed || e.apps.removed) {
            for (var i = (this.apps.list.length - 1); i >= 0; i--) {
              var curApp = this.apps.list[i];
              if (e.apps.removed) {
                // If it matches removal, splice it out
                for (var r = 0; r < e.apps.removed.length; r++) {
                  if (e.apps.removed[r] === curApp.name) 
                    this.apps.list.splice(i, 1);
                }              
              }
              if (e.apps.changed) {
                // If it matches changed, update it
                for (var c = 0; c < e.apps.changed.length; c++) {
                  var changedApp = e.apps.changed[c];
                  if (changedApp.name === curApp.name) {
                    curApp.state = changedApp.state;
                  }
                }
              }
            }
          }

          // Finally append new additions
          if (e.apps.added) {
            Array.prototype.push.apply(this.apps.list, e.apps.added);
          }

          this._notifyObservers('onAppsListChange', [this.apps.list, prevList, e.apps.added, e.apps.removed, e.apps.changed]);
        }
      }
    },

    // TODO: BEGIN REMOVE THIS
    // This is a temporary work around while we refactor the code to rely on resourceManager directly.
    // For now, we'll reference an instance of ResourceManager that is injected post-construction by
    // the ResourceManager code.
    /**
     * Returns a deferred which will resolve with the host object.
     * 
     * @returns
     */
    getHost: function() {
      console.error('Standalone.getHost() called, this wont really work');
      return null;
    },

    getCluster: function() {
      console.error('Standalone.getCluster() called, this wont really work');
      return null;
    },

    getApps: function() {
      return this.resourceManager.getAppsOnServer(this);
    },
    // TODO: END REMOVE THIS

    /**
     * Stops the StandaloneSerer.
     * 
     * @return {Deferred} Returns a Deferred which will resolve with a success message if the server stopped. If anything goes wrong, the Deferred is rejected with an error message.
     */
    stop: function() {
      var deferred = new Deferred();

      // Find the OSGi framework Mbean
      var url = '/IBMJMXConnectorREST/mbeans?objectName=osgi.core:type=framework,version=*,framework=org.eclipse.osgi,uuid=*';
      var options = {
          handleAs : 'json',
          headers : {
            "Content-type" : "application/json"
          }
      };

      console.log('Looking up the OSGi framework MBean');
      xhr.get(url, options).then(function(response) {
        if (response.length === 0) {
          console.error('Unable to stop the StandaloneServer. The OSGi framework MBean is not available.');
          deferred.reject(i18n.STANDALONE_STOP_NO_MBEAN, true);
        } else if (response.length === 1) {
          // Call the mbean
          var stopURL = response[0].URL + '/operations/shutdownFramework';
          var postOpts = {
              handleAs : 'json',
              headers : {
                'Content-type' : 'application/json'
              },
              data : '{"params":[],"signature":[]}'
          };

          console.log('Calling the OSGi framework MBean shutdownFramework operation');
          xhr.post(stopURL, postOpts).then(function(postRes) {
            console.log('StandaloneServer successfully stopped');
            deferred.resolve(i18n.STANDALONE_STOP_SUCCESS, true);
          }, function(err) {
            console.error('Unable to stop the StandaloneServer. POST to OSGi framework MBean shutdownFramework operation failed. Error: ', err);
            deferred.reject(i18n.STANDALONE_STOP_FAILED, true);
          });  
        } else {
          console.error('Unable to stop the StandaloneServer. Multiple matches reported for the OSGi framework MBean. Unable to determine which one to use, found ' + response.length + ' matches');
          deferred.reject(i18n.STANDALONE_STOP_CANT_DETERMINE_MBEAN, true);
        }
      }, function(err) {
        console.error('Unable to stop the StandaloneServer. GET to query OSGi framework MBean failed. Error: ', err);
        deferred.reject(i18n.STANDALONE_STOP_CANT_DETERMINE_MBEAN, true);
      });

      return deferred;
    }

  });

});
