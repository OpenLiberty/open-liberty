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
 * Test cases for AppInstancesByCluster
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "intern/dojo/declare",
     "resources/_derived/collections/AppInstancesByCluster",
     "resources/_objects/Cluster",
     "resources/_objects/Server",
     "resources/_derived/objects/AppOnCluster",
     "resources/_derived/objects/AppOnServer",
     "resources/Observer",
     "resources/resourceManager",
     "dojo/Deferred"
       ],
       
    function(tdd, assert, declare, AppInstancesByCluster, Cluster, Server, AppOnCluster, AppOnServer, Observer, ResourceManager, Deferred) {

    var AppInstancesByClusterObserver = declare([Observer], {
      id: 'testObserver',
  
      onTallyChange: function(newTally, oldTally) {
        this.newTally = newTally;
        this.oldTally = oldTally;
      },
  
      onAppsListChange: function(newList, oldList, added, removed) {
        this.newList = newList;
        this.oldList = oldList;
        this.added = added;
        this.removed = removed;
      },
  
      onDestroyed: function() {
        this.destroyed = true;
      }
    });

    with(assert) {
      
  
    /**
     * Defines the 'AppInstancesByCluster Collection Tests' module test suite.
     */
    tdd.suite('AppInstancesByCluster Collection Tests', function() {
      
         tdd.test('constructor - no initialization object', function() {
           try {
             new AppInstancesByCluster();
             assert.isTrue(false, 'AppInstancesByCluster was successfully created when it should have failed - an initialization object is required');
           } catch(error) {
             assert.equal(error, 'AppInstancesByCluster created without an initialization object', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no cluster', function() {
           try {
             new AppInstancesByCluster({application: {/*...*/}, appOnServer: []});
             assert.isTrue(false, 'AppInstancesByCluster was successfully created when it should have failed - a Cluster is required');
           } catch(error) {
             assert.equal(error, 'AppInstancesByCluster created without a Cluster', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no application', function() {
           try {
             new AppInstancesByCluster({cluster: {/*...*/}, appOnServer: []});
             assert.isTrue(false, 'AppInstancesByCluster was successfully created when it should have failed - an Application is required');
           } catch(error) {
             assert.equal(error, 'AppInstancesByCluster created without an AppOnCluster', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no array of AppOnServer', function() {
           try {
             new AppInstancesByCluster({cluster: {/*...*/}, application: {/*...*/}});
             assert.isTrue(false, 'AppInstancesByCluster was successfully created when it should have failed - an array of AppOnServer is required');
           } catch(error) {
             assert.equal(error, 'AppInstancesByCluster created without an array of AppOnServer', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - one app instance', function() {
           var cluster = new Cluster({
             id: 'cluster1', 
             state: 'STARTED', 
             scalingPolicy: 'default', 
             servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
             apps: {up: 1, down: 0, unknown: 0, partial: 0, list: [ {name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}} ] },
             alerts: {count: 8}
           });
           var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           server.resourceManager = ResourceManager;
           var appOnServer = new AppOnServer({server: server, name: 'snoop'});
           var appInstances = new AppInstancesByCluster({cluster: cluster, application: appOnCluster, appOnServer: [appOnServer]});
  
           assert.equal(appInstances.id, 'appInstancesByCluster(cluster1,snoop(cluster1))', 'AppInstancesByCluster.id did not have the correct initialized value');
           assert.equal(appInstances.up,          1,           'AppInstancesByCluster.up did not have the correct initialized value');
           assert.equal(appInstances.down,        0,           'AppInstancesByCluster.down did not have the correct initialized value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appInstances.list),       'AppInstancesByCluster.list was not an Array');
           assert.equal(appInstances.list.length, 1,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer, 'AppInstancesByCluster.list[0] was not correct');
         }),
  
         tdd.test('constructor - two app instances', function() {
           var cluster = new Cluster({
             id: 'cluster1', 
             state: 'STARTED', 
             scalingPolicy: 'default', 
             servers: {up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2']}, 
             apps: {up: 1, down: 0, unknown: 0, partial: 0, list: [ {name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}} ] },
             alerts: {count: 8}
           });
           var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
           var server1 = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           server1.resourceManager = ResourceManager;
           var appOnServer1 = new AppOnServer({server: server1, name: 'snoop'});
           var server2 = new Server({id: 'localhost,/wlp/usr,server2', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           server2.resourceManager = ResourceManager;
           var appOnServer2 = new AppOnServer({server: server2, name: 'snoop'});
           var appInstances = new AppInstancesByCluster({cluster: cluster, application: appOnCluster, appOnServer: [appOnServer1, appOnServer2]});
           var observer = new AppInstancesByClusterObserver();
  
           appInstances.subscribe(observer);
  
           assert.equal('appInstancesByCluster(cluster1,snoop(cluster1))', appInstances.id, 'AppInstancesByCluster.id did not have the correct initialized value');
           assert.equal(appInstances.up,          2,           'AppInstancesByCluster.up did not have the correct initialized value');
           assert.equal(appInstances.down,        0,           'AppInstancesByCluster.down did not have the correct initialized value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appInstances.list),       'AppInstancesByCluster.list was not an Array');
           assert.equal(appInstances.list.length, 2,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer1, 'AppInstancesByCluster.list[0] was not correct');
           assert.equal(appInstances.list[1],     appOnServer2, 'AppInstancesByCluster.list[1] was not correct');
         }),
  
         tdd.test('AppOnServer changes - state changed to down', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           server.resourceManager = ResourceManager;
           var appOnServer = new AppOnServer({server: server, name: 'snoop'});
           var cluster = new Cluster({
             id: 'cluster1', 
             state: 'STARTED', 
             scalingPolicy: 'default', 
             servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
             apps: {up: 1, down: 0, unknown: 0, partial: 0, list: [ {name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}} ] },
             alerts: {count: 8}
           });
           var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
           var appInstances = new AppInstancesByCluster({cluster: cluster, application: appOnCluster, appOnServer: [appOnServer]});
           var observer = new AppInstancesByClusterObserver();
  
           appInstances.subscribe(observer);
 
           assert.equal(appInstances.up,          1,           'AppInstancesByCluster.up did not have the correct initialized value');
           assert.equal(appInstances.down,        0,           'AppInstancesByCluster.down did not have the correct initialized value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appInstances.list),       'AppInstancesByCluster.list was not an Array');
           assert.equal(appInstances.list.length, 1,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer, 'AppInstancesByCluster.list[0] was not correct');
  
           // Trigger the application's onTallyChange method via the Application event handler
           cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 0, down: 1, unknown: 0, changed: [{name: 'snoop', state: 'STOPPED', up: 0, down: 1, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}]}});
  
           assert.equal(appInstances.up,          0,           'AppInstancesByCluster.up did not have the correct updated value');
           assert.equal(appInstances.down,        1,           'AppInstancesByCluster.down did not have the correct updated value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct updated value');
           assert.equal(appInstances.list.length, 1,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer, 'AppInstancesByCluster.list[0] was not correct');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newTally,             'AppInstancesByClusterObserver.newTally did not get set, when it should have been');
           assert.equal(observer.newTally.up,      0, 'AppInstancesByClusterObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    1, 'AppInstancesByClusterObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 0, 'AppInstancesByClusterObserver did not get the correct new value for the unknown tally');
  
           assert.isNotNull(observer.oldTally,             'AppInstancesByClusterObserver.oldTally did not get set, when it should have been');
           assert.equal(observer.oldTally.up,      1, 'AppInstancesByClusterObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    0, 'AppInstancesByClusterObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 0, 'AppInstancesByClusterObserver did not get the correct old value for the unknown tally');
         }),
  
         tdd.test('AppOnServer changes - state changed to up', function() {
           var server =  new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STOPPED'}]}});
           server.resourceManager = ResourceManager;
           var appOnServer = new AppOnServer({server: server, name: 'snoop'});
           var cluster = new Cluster({
             id: 'cluster1', 
             state: 'STARTED', 
             scalingPolicy: 'default', 
             servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
             apps: {up: 0, down: 1, unknown: 0, partial: 0, list: [ {name: 'snoop', up: 0, down: 1, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}} ] },
             alerts: {count: 8}
           });
           var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
           var appInstances = new AppInstancesByCluster({cluster: cluster, application: appOnCluster, appOnServer: [appOnServer]});
           var observer = new AppInstancesByClusterObserver();
  
           appInstances.subscribe(observer);
           
           assert.equal(appInstances.up,          0,           'AppInstancesByCluster.up did not have the correct initialized value');
           assert.equal(appInstances.down,        1,           'AppInstancesByCluster.down did not have the correct initialized value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appInstances.list),       'AppInstancesByCluster.list was not an Array');
           assert.equal(appInstances.list.length, 1,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer, 'AppInstancesByCluster.list[0] was not correct');
  
           // Trigger the application's onTallyChange method via the Application event handler
           cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 1, down: 0, unknown: 0, changed: [{name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}]}});
  
           assert.equal(appInstances.up,          1,           'AppInstancesByCluster.up did not have the correct updated value');
           assert.equal(appInstances.down,        0,           'AppInstancesByCluster.down did not have the correct updated value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct updated value');
           assert.equal(appInstances.list.length, 1,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer, 'AppInstancesByCluster.list[0] was not correct');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newTally,             'AppInstancesByClusterObserver.newTally did not get set, when it should have been');
           assert.equal(observer.newTally.up,      1, 'AppInstancesByClusterObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    0, 'AppInstancesByClusterObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 0, 'AppInstancesByClusterObserver did not get the correct new value for the unknown tally');
  
           assert.isNotNull(observer.oldTally,             'AppInstancesByClusterObserver.oldTally did not get set, when it should have been');
           assert.equal(observer.oldTally.up,      0, 'AppInstancesByClusterObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    1, 'AppInstancesByClusterObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 0, 'AppInstancesByClusterObserver did not get the correct old value for the unknown tally');
         }),
  
         tdd.test('AppOnServer changes - state changed to starting', function() {
           var cluster = new Cluster({
             id: 'cluster1', 
             state: 'STARTED', 
             scalingPolicy: 'default', 
             servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
             apps: {up: 1, down: 0, unknown: 0, partial: 0, list: [ {name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}} ] },
             alerts: {count: 8}
           });
           var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STOPPED'}]}});
           server.resourceManager = ResourceManager;
           var appOnServer = new AppOnServer({server: server, name: 'snoop'});
           var appInstances = new AppInstancesByCluster({cluster: cluster, application: appOnCluster, appOnServer: [appOnServer]});
           var observer = new AppInstancesByClusterObserver();
  
           appInstances.subscribe(observer);
  
           assert.equal(appInstances.up,          0,           'AppInstancesByCluster.up did not have the correct initialized value');
           assert.equal(appInstances.down,        1,           'AppInstancesByCluster.down did not have the correct initialized value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appInstances.list),       'AppInstancesByCluster.list was not an Array');
           assert.equal(appInstances.list.length, 1,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer, 'AppInstancesByCluster.list[0] was not correct');
  
           // Trigger the application's onTallyChange method via the Application event handler
           appOnServer._handleChangeEvent({type: 'appOnServer', id: 'snoop(localhost,/wlp/usr,server1)', state: 'STARTING'});
  
           assert.equal(appInstances.up,          0,           'AppInstancesByCluster.up did not have the correct updated value');
           assert.equal(appInstances.down,        1,           'AppInstancesByCluster.down did not have the correct updated value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct updated value');
           assert.equal(appInstances.list.length, 1,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer, 'AppInstancesByCluster.list[0] was not correct');
  
           // Validate the Observer onTallyChange method was not called
           assert.isUndefined(observer.newTally,             'AppInstancesByClusterObserver.newTally should not be set when there is no effective tally change');
           assert.isUndefined(observer.oldTally,             'AppInstancesByClusterObserver.oldTally should not be set when there is no effective tally change');
         }),
  
         tdd.test('Cluster changes - added server', function() {
           var cluster = new Cluster({
             id: 'cluster1', 
             state: 'STARTED', 
             scalingPolicy: 'default', 
             servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
             apps: {up: 1, down: 0, unknown: 0, partial: 0, list: [ {name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}} ] },
             alerts: {count: 8}
           });
           var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
           var server1 = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           server1.resourceManager = ResourceManager;
           var appOnServer1 = new AppOnServer({server: server1, name: 'snoop'});
           var appInstances = new AppInstancesByCluster({cluster: cluster, application: appOnCluster, appOnServer: [appOnServer1]});
           var observer = new AppInstancesByClusterObserver();
  
           appInstances.subscribe(observer);
  
           var server2 = new Server({id: 'localhost,/wlp/usr,server2', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           server2.resourceManager = ResourceManager;
           var appOnServer2 = new AppOnServer({server: server2, name: 'snoop'});
           appInstances.resourceManager = {
               getServer: function(list) {
                 var deferred = new Deferred();
                 deferred.resolve([server2], true);
                 return deferred;
               },
               getAppOnServer: function(serverList, applicationName) {
                 var deferred = new Deferred();
                 deferred.resolve([appOnServer2], true);
                 return deferred;
               }
           };
  
           var dfd = this.async(1000);
  
           // Need to wait until the onListChange method fires before we 
           observer.onAppsListChange = function(newList, oldList, added, removed) {
             try {
               assert.equal(appInstances.up,          2,           'AppInstancesByCluster.up did not have the correct updated value');
               assert.equal(appInstances.down,        0,           'AppInstancesByCluster.down did not have the correct updated value');
               assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct updated value');
               assert.isTrue(Array.isArray(appInstances.list),       'AppInstancesByCluster.list was not an Array');
               assert.equal(appInstances.list.length, 2,           'AppInstancesByCluster.list.length was not the correct size');
               assert.equal(appInstances.list[0],     appOnServer1, 'AppInstancesByCluster.list[0] was not correct');
               assert.equal(appInstances.list[1],     appOnServer2, 'AppInstancesByCluster.list[1] was not correct');
  
               // Validate the Observer was passed the correct tally objects after the first event
               assert.isNotNull(observer.newTally,             'AppInstancesByClusterObserver.newTally did not get set, when it should have been');
               assert.equal(observer.newTally.up,      2, 'AppInstancesByClusterObserver did not get the correct new value for the up tally');
               assert.equal(observer.newTally.down,    0, 'AppInstancesByClusterObserver did not get the correct new value for the down tally');
               assert.equal(observer.newTally.unknown, 0, 'AppInstancesByClusterObserver did not get the correct new value for the unknown tally');
  
               assert.isNotNull(observer.oldTally,             'AppInstancesByClusterObserver.oldTally did not get set, when it should have been');
               assert.equal(observer.oldTally.up,      1, 'AppInstancesByClusterObserver did not get the correct old value for the up tally');
               assert.equal(observer.oldTally.down,    0, 'AppInstancesByClusterObserver did not get the correct old value for the down tally');
               assert.equal(observer.oldTally.unknown, 0, 'AppInstancesByClusterObserver did not get the correct old value for the unknown tally');
  
               assert.isNotNull(newList,              'AppInstancesByClusterObserver.newList did not get set, when it should have been');
               assert.equal(newList.length, 2,           'AppInstancesByClusterObserver.newList.length was not the correct size');
               assert.equal(newList[0],     appOnServer1, 'AppInstancesByClusterObserver.newList[0] was not correct');
               assert.equal(newList[1],     appOnServer2, 'AppInstancesByClusterObserver.newList[1] was not correct');
               assert.isNotNull(oldList,              'AppInstancesByClusterObserver.oldList did not get set, when it should have been');
               assert.equal(oldList.length, 1,           'AppInstancesByClusterObserver.oldList.length was not the correct size');
               assert.equal(oldList[0],     appOnServer1, 'AppInstancesByClusterObserver.oldList[0] was not correct');
               assert.isNotNull(added,                'AppInstancesByClusterObserver.added did not get set, when it should have been');
               assert.equal(added.length,     1, 'AppInstancesByClusterObserver.added was not of expected size');
               assert.equal(added[0], 'localhost,/wlp/usr,server2', 'AppInstancesByClusterObserver.added[0] was not of expected value');
               assert.isUndefined(removed,             'AppInstancesByClusterObserver.removed got set when it should not have been');
  
               dfd.resolve('OK');
             } catch(err) {
               // TODO: Better practice to call this?
               //dfd.rejectOnError('Error in onListChange: ' + err);
               dfd.reject(err);
             }
           };
  
           // Trigger the application's onServersListChange method via the Application event handler
           cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', servers: { up: 2, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server2'] } });
  
           return dfd;
         }),
  
         tdd.test('Cluster changes - removed server', function() {
           var server1 = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           server1.resourceManager = ResourceManager;
           var appOnServer1 = new AppOnServer({server: server1, name: 'snoop'});
           var server2 = new Server({id: 'localhost,/wlp/usr,server2', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           server2.resourceManager = ResourceManager;
           var appOnServer2 = new AppOnServer({server: server2, name: 'snoop'});
           var cluster = new Cluster({
             id: 'cluster1', 
             state: 'STARTED', 
             scalingPolicy: 'default', 
             servers: {up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2']}, 
             apps: {up: 1, down: 0, unknown: 0, partial: 0, list: [ {name: 'snoop', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ]}} ] },
             alerts: {count: 8}
           });
           var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
           var appInstances = new AppInstancesByCluster({cluster: cluster, application: appOnCluster, appOnServer: [appOnServer1, appOnServer2]});
           var observer = new AppInstancesByClusterObserver();
  
           appInstances.subscribe(observer);

           assert.equal(appInstances.up,          2,           'AppInstancesByCluster.up did not have the correct initialized value');
           assert.equal(appInstances.down,        0,           'AppInstancesByCluster.down did not have the correct initialized value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appInstances.list),       'AppInstancesByCluster.list was not an Array');
           assert.equal(appInstances.list.length, 2,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer1, 'AppInstancesByCluster.list[0] was not correct');
           assert.equal(appInstances.list[1],     appOnServer2, 'AppInstancesByCluster.list[1] was not correct');
  
           // Trigger the application's onServersListChange method via the Application event handler
           cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', servers: { up: 1, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2'] } });
  
           assert.equal(appInstances.up,          1,           'AppInstancesByCluster.up did not have the correct updated value');
           assert.equal(appInstances.down,        0,           'AppInstancesByCluster.down did not have the correct updated value');
           assert.equal(appInstances.unknown,     0,           'AppInstancesByCluster.unknown did not have the correct updated value');
           assert.isTrue(Array.isArray(appInstances.list),       'AppInstancesByCluster.list was not an Array');
           assert.equal(appInstances.list.length, 1,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(appInstances.list[0],     appOnServer1, 'AppInstancesByCluster.list[0] was not correct');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newTally,             'AppInstancesByClusterObserver.newTally did not get set, when it should have been');
           assert.equal(observer.newTally.up,      1, 'AppInstancesByClusterObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    0, 'AppInstancesByClusterObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 0, 'AppInstancesByClusterObserver did not get the correct new value for the unknown tally');
  
           assert.isNotNull(observer.oldTally,             'AppInstancesByClusterObserver.oldTally did not get set, when it should have been');
           assert.equal(observer.oldTally.up,      2, 'AppInstancesByClusterObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    0, 'AppInstancesByClusterObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 0, 'AppInstancesByClusterObserver did not get the correct old value for the unknown tally');
  
           assert.isNotNull(observer.newList,              'AppInstancesByClusterObserver.newList did not get set, when it should have been');
           assert.equal(observer.newList.length, 1,           'AppInstancesByCluster.list.length was not the correct size');
           assert.equal(observer.newList[0],     appOnServer1, 'AppInstancesByCluster.list[0] was not correct');
           assert.isNotNull(observer.oldList,              'AppInstancesByClusterObserver.oldList did not get set, when it should have been');
           assert.equal(observer.oldList.length, 2,           'AppInstancesByClusterObserver.oldList.length was not the correct size');
           assert.equal(observer.oldList[0],     appOnServer1, 'AppInstancesByClusterObserver.oldList[0] was not correct');
           assert.equal(observer.oldList[1],     appOnServer2, 'AppInstancesByClusterObserver.oldList[1] was not correct');
           assert.isUndefined(observer.added,               'AppInstancesByClusterObserver.added got set when it should not have been');
           assert.isNotNull(observer.removed,              'AppInstancesByClusterObserver.removed did not get set, when it should have been');
           assert.equal(observer.removed.length,   1, 'AppInstancesByClusterObserver.removed was not empty');
           assert.equal(observer.removed[0], 'localhost,/wlp/usr,server2', 'AppInstancesByClusterObserver.removed[0] did not have the correct value for the object name');
         }),
         
         tdd.test('Cluster changes - cluster was removed from the collective', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           var appOnServer = new AppOnServer({server: server, name: 'snoop'});
           var cluster = new Cluster({
             id: 'cluster1', 
             state: 'STARTED', 
             scalingPolicy: 'default', 
             servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
             apps: {up: 1, down: 0, unknown: 0, partial: 0, list: [ {name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}} ] },
             alerts: {count: 8}
           });
           var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
           var appInstances = new AppInstancesByCluster({cluster: cluster, application: appOnCluster, appOnServer: [appOnServer]});
           var observer = new AppInstancesByClusterObserver();
  
           appInstances.subscribe(observer);
  
           // Application is removed from collective
           cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'removed'});
  
           assert.isTrue(appInstances.isDestroyed,      'AppInstancesByCluster.isDestroyed flag did not get set in response to a "removed" event');
           
           // Confirm the application is destroyed
           assert.isTrue(observer.destroyed,            'AppInstancesObserver.onDestroyed did not get called');
         });
         
      });
    }
});
