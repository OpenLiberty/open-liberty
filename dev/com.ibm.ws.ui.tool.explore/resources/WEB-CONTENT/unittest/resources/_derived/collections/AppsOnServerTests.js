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
 * Test cases for AppsOnServer
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "dojo/Deferred",
        "resources/_derived/collections/AppsOnServer",
        "resources/_derived/objects/AppOnServer",
        "resources/_objects/Server",
        "resources/Observer",
        "dojo/aspect"
        ],
        
    function(tdd, assert, declare, Deferred, AppsOnServer, AppOnServer, Server, Observer, Aspect) {

    var AppsOnServerObserver = declare([Observer], {
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
     * Defines the 'AppsOnServer Collection Tests' module test suite.
     */
    tdd.suite('AppsOnServer Collection Tests', function() {

        tdd.test('constructor - no initialization object', function() {
           try {
             new AppsOnServer();
             assert.isTrue(false, 'AppsOnServer was successfully created when it should have failed - an initialization object is required');
           } catch(error) {
             assert.equal(error, 'AppsOnServer created without an initialization object', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no server', function() {
           try {
             new AppsOnServer({ appOnServer: [] });
             assert.isTrue(false, 'AppsOnServer was successfully created when it should have failed - a server is required');
           } catch(error) {
             assert.equal(error, 'AppsOnServer created without a server', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no array of AppOnServer', function() {
           try {
             new AppsOnServer({ server: {/*...*/} });
             assert.isTrue(false, 'AppsOnServer was successfully created when it should have failed - an array of AppOnServer is required');
           } catch(error) {
             assert.equal(error, 'AppsOnServer created without an array of AppOnServer', 'Error reported did not match expected error');
           }
         }),
  
  
         tdd.test('constructor - server with no apps', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var appsOnServer = new AppsOnServer({server: server, appOnServer: []});
  
           assert.equal(appsOnServer.id, 'appsOnServer(localhost,/wlp/usr,server1)', 'AppsOnServer.id did not have the correct initialized value');
           assert.equal(appsOnServer.up,          0,           'AppsOnServer.up did not have the correct initialized value');
           assert.equal(appsOnServer.down,        0,           'AppsOnServer.down did not have the correct initialized value');
           assert.equal(appsOnServer.unknown,     0,           'AppsOnServer.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appsOnServer.list),       'AppsOnServer.list was not an Array');
           assert.equal(appsOnServer.list.length, 0,           'AppsOnServer.list.length was not initially empty');
         }),
  
         tdd.test('constructor - server with apps', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 2, unknown: 3, list: [{name: 'snoop', state: 'STARTED'}]}, alerts: {count: 8, unknown: [], app: []}});
           var appOnServer = new AppOnServer({server: server, name: 'snoop'});
           var appsOnServer = new AppsOnServer({server: server, appOnServer: [appOnServer]});
  
           assert.equal(appsOnServer.id, 'appsOnServer(localhost,/wlp/usr,server1)', 'AppsOnServer.id did not have the correct initialized value');
           assert.equal(appsOnServer.up,          1,           'AppsOnServer.up did not have the correct initialized value');
           assert.equal(appsOnServer.down,        2,           'AppsOnServer.down did not have the correct initialized value');
           assert.equal(appsOnServer.unknown,     3,           'AppsOnServer.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appsOnServer.list),       'AppsOnServer.list was not an Array');
           assert.equal(appsOnServer.list.length, 1,           'AppsOnServer.list.length was not the correct value');
           assert.equal(appsOnServer.list[0].name, 'snoop',     'AppsOnServer.list[0] did not have the correct value for the object name');
         }),
  
         tdd.test('Server changes - apps tallies changed', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var appsOnServer = new AppsOnServer({server: server, appOnServer: []});
           var observer = new AppsOnServerObserver();
  
           appsOnServer.subscribe(observer);
  
           assert.equal(appsOnServer.up,          0,           'AppsOnServer.up did not have the correct initialized value');
           assert.equal(appsOnServer.down,        0,           'AppsOnServer.down did not have the correct initialized value');
           assert.equal(appsOnServer.unknown,     0,           'AppsOnServer.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appsOnServer.list),       'AppsOnServer.list was not an Array');
           assert.equal(appsOnServer.list.length, 0,           'AppsOnServer.list.length was not initially empty');
  
           // Trigger the server's onAppsTallyChange method via the Server event handler
           server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 1, down: 2, unknown: 3}});
  
           assert.equal(appsOnServer.up,          1,           'AppsOnServer.up did not have the correct updated value');
           assert.equal(appsOnServer.down,        2,           'AppsOnServer.down did not have the correct updated value');
           assert.equal(appsOnServer.unknown,     3,           'AppsOnServer.unknown did not have the correct updated value');
           assert.isTrue(Array.isArray(appsOnServer.list),       'AppsOnServer.list was not an Array');
           assert.equal(appsOnServer.list.length, 0,           'AppsOnServer.list.length was not initially empty');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newTally,             'AppsOnServerObserver.newTally did not get set, when it should have been');
           assert.equal(observer.newTally.up,      1, 'AppsOnServerObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    2, 'AppsOnServerObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 3, 'AppsOnServerObserver did not get the correct new value for the unknown tally');
  
           assert.isNotNull(observer.oldTally,             'AppsOnServerObserver.oldTally did not get set, when it should have been');
           assert.equal(observer.oldTally.up,      0, 'AppsOnServerObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    0, 'AppsOnServerObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 0, 'AppsOnServerObserver did not get the correct old value for the unknown tally');
         }),
  
         tdd.test('Server changes - added application - onAppsListChange', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var appsOnServer = new AppsOnServer({server: server, appOnServer: []});
           var observer = new AppsOnServerObserver();
  
           appsOnServer.subscribe(observer);
  
           var appOnServer = new AppOnServer({server: server, name: 'snoop'});
           appsOnServer.resourceManager = {
               getAppOnServer: function(server, listOfNames) {
                 var deferred = new Deferred();
                 deferred.resolve([appOnServer], true);
                 return deferred;
               }
           };
  
           var dfd = this.async(1000);
  
           // Need to wait until the onListChange method fires before we 
           observer.onAppsListChange = function(newList, oldList, added, removed) {
             try {
               assert.isNotNull(newList,              'AppsOnServerObserver.newList did not get set, when it should have been');
               assert.equal(newList.length,   1, 'AppsOnServerObserver.newList was not of expected size');
               assert.equal(newList[0].name, 'snoop', 'AppsOnServerObserver.newList[0] was not of expected value');
               assert.isNotNull(oldList,              'AppsOnServerObserver.oldList did not get set, when it should have been');
               assert.equal(oldList.length,   0, 'AppsOnServerObserver.oldList was not empty');
               assert.isNotNull(added,                'AppsOnServerObserver.added did not get set, when it should have been');
               assert.equal(added.length,     1, 'AppsOnServerObserver.added was not of expected size');
               assert.equal(added[0].name, 'snoop', 'AppsOnServerObserver.added[0] was not of expected value');
               assert.isUndefined(removed,             'AppsOnServerObserver.removed got set when it should not have been');
  
               dfd.resolve('OK');
             } catch(err) {
               dfd.reject(err);
             }
           };
  
           // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
           server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 1, down: 0, unknown: 0, added: [{name: 'snoop', state: 'STARTED'}]}});
  
           return dfd;
         }),
         
         tdd.test('Server changes - added application - onTallyChange', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var appsOnServer = new AppsOnServer({server: server, appOnServer: []});
           var observer = new AppsOnServerObserver();
  
           appsOnServer.subscribe(observer);
  
           var appOnServer = new AppOnServer({server: server, name: 'snoop'});
           appsOnServer.resourceManager = {
               getAppOnServer: function(server, listOfNames) {
                 var deferred = new Deferred();
                 deferred.resolve([appOnServer], true);
                 return deferred;
               }
           };
  
           var dfd = this.async(1000);
  
           // Need to wait until after the onListChange method fires before we check the values
           Aspect.after(observer, "onTallyChange", function() {
             try {
               assert.equal(appsOnServer.up,          1,           'AppsOnServer.up did not have the correct updated value');
               assert.equal(appsOnServer.down,        0,           'AppsOnServer.down did not have the correct updated value');
               assert.equal(appsOnServer.unknown,     0,           'AppsOnServer.unknown did not have the correct updated value');
               assert.isTrue(Array.isArray(appsOnServer.list),       'AppsOnServer.list was not an Array');
               assert.equal(appsOnServer.list.length, 1,           'AppsOnServer.list.length was not the correct updated value');
               assert.equal(appsOnServer.list[0].name, 'snoop',     'AppsOnServer.list[0] did not have the correct updated value for the object name');
  
               // Validate the Observer was passed the correct tally objects after the first event
               assert.isNotNull(observer.newTally,             'AppsOnServerObserver.newTally did not get set, when it should have been');
               assert.equal(observer.newTally.up,      1, 'AppsOnServerObserver did not get the correct new value for the up tally');
               assert.equal(observer.newTally.down,    0, 'AppsOnServerObserver did not get the correct new value for the down tally');
               assert.equal(observer.newTally.unknown, 0, 'AppsOnServerObserver did not get the correct new value for the unknown tally');
  
               assert.isNotNull(observer.oldTally,             'AppsOnServerObserver.oldTally did not get set, when it should have been');
               assert.equal(observer.oldTally.up,      0, 'AppsOnServerObserver did not get the correct old value for the up tally');
               assert.equal(observer.oldTally.down,    0, 'AppsOnServerObserver did not get the correct old value for the down tally');
               assert.equal(observer.oldTally.unknown, 0, 'AppsOnServerObserver did not get the correct old value for the unknown tally');
               
               dfd.resolve('OK');
             } catch(err) {
               dfd.reject(err);
             }
           });

           // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
           server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 1, down: 0, unknown: 0, added: [{name: 'snoop', state: 'STARTED'}]}});
               
           return dfd;
         }),
  
         tdd.test('Server changes - removed application', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           var appOnServer = new AppOnServer({server: server, name: 'snoop'});
           var appsOnServer = new AppsOnServer({server: server, appOnServer: [appOnServer]});
           var observer = new AppsOnServerObserver();
  
           appsOnServer.subscribe(observer);
  
           assert.equal(appsOnServer.up,          1,           'AppsOnServer.up did not have the correct initialized value');
           assert.equal(appsOnServer.down,        0,           'AppsOnServer.down did not have the correct initialized value');
           assert.equal(appsOnServer.unknown,     0,           'AppsOnServer.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appsOnServer.list),       'AppsOnServer.list was not an Array');
           assert.equal(appsOnServer.list.length, 1,           'AppsOnServer.list.length was not the correct initial value');
           assert.equal(appsOnServer.list[0].name, 'snoop',     'AppsOnServer.list[0] did not have the correct value for the object name');
  
           // Trigger the server's onAppsTallyChange method via the event handler
           server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 0, down: 0, unknown: 0, removed: ['snoop']}});
  
           assert.equal(appsOnServer.up,          0,           'AppsOnServer.up did not have the correct updated value');
           assert.equal(appsOnServer.down,        0,           'AppsOnServer.down did not have the correct updated value');
           assert.equal(appsOnServer.unknown,     0,           'AppsOnServer.unknown did not have the correct updated value');
           assert.isTrue(Array.isArray(appsOnServer.list),       'AppsOnServer.list was not an Array');
           assert.equal(appsOnServer.list.length, 0,           'AppsOnServer.list.length was not updated to be empty');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newTally,             'AppsOnServerObserver.newTally did not get set, when it should have been');
           assert.equal(observer.newTally.up,      0, 'AppsOnServerObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    0, 'AppsOnServerObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 0, 'AppsOnServerObserver did not get the correct new value for the unknown tally');
  
           assert.isNotNull(observer.oldTally,             'AppsOnServerObserver.oldTally did not get set, when it should have been');
           assert.equal(observer.oldTally.up,      1, 'AppsOnServerObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    0, 'AppsOnServerObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 0, 'AppsOnServerObserver did not get the correct old value for the unknown tally');
  
           assert.isNotNull(observer.newList,              'AppsOnServerObserver.newList did not get set, when it should have been');
           assert.equal(observer.newList.length,   0, 'AppsOnServerObserver.newList was not of expected size');
           assert.isNotNull(observer.oldList,              'AppsOnServerObserver.oldList did not get set, when it should have been');
           assert.equal(observer.oldList.length,   1, 'AppsOnServerObserver.oldList was not empty');
           assert.equal(observer.oldList[0].name, 'snoop', 'AppsOnServerObserver.oldList[0] did not have the correct value for the object name');
           assert.isUndefined(observer.added,               'AppsOnServerObserver.added got set when it should not have been');
           assert.isNotNull(observer.removed,              'AppsOnServerObserver.removed did not get set, when it should have been');
           assert.equal(observer.removed.length,   1, 'AppsOnServerObserver.removed was not empty');
           assert.equal(observer.removed[0], 'snoop', 'AppsOnServerObserver.removed[0] did not have the correct value for the object name');
         }),
         
         /**
          * This test is to ensure we have the right splice logic
          */
         tdd.test('Server changes - removed application', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 3, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}, {name: 'melanie', state: 'STARTED'}, {name: 'lynne', state: 'STARTED'}]}});
           var appOnServer1 = new AppOnServer({server: server, name: 'snoop'});
           var appOnServer2 = new AppOnServer({server: server, name: 'melanie'});
           var appOnServer3 = new AppOnServer({server: server, name: 'lynne'});
           var appsOnServer = new AppsOnServer({server: server, appOnServer: [appOnServer1, appOnServer2, appOnServer3]});
           var observer = new AppsOnServerObserver();
  
           appsOnServer.subscribe(observer);
  
           assert.equal(appsOnServer.up,          3,           'AppsOnServer.up did not have the correct initialized value');
           assert.equal(appsOnServer.down,        0,           'AppsOnServer.down did not have the correct initialized value');
           assert.equal(appsOnServer.unknown,     0,           'AppsOnServer.unknown did not have the correct initialized value');
           assert.isTrue(Array.isArray(appsOnServer.list),       'AppsOnServer.list was not an Array');
           assert.equal(appsOnServer.list.length, 3,           'AppsOnServer.list.length was not the correct initial value');
           assert.equal(appsOnServer.list[0].name, 'snoop',     'AppsOnServer.list[0] did not have the correct value for the object name');
           assert.equal(appsOnServer.list[1].name, 'melanie',   'AppsOnServer.list[1] did not have the correct value for the object name');
           assert.equal(appsOnServer.list[2].name, 'lynne',     'AppsOnServer.list[2] did not have the correct value for the object name');
  
           // Trigger the server's onAppsTallyChange method via the event handler
           server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 2, down: 0, unknown: 0, removed: ['melanie']}});
  
           assert.equal(appsOnServer.up,          2,           'AppsOnServer.up did not have the correct updated value');
           assert.equal(appsOnServer.down,        0,           'AppsOnServer.down did not have the correct updated value');
           assert.equal(appsOnServer.unknown,     0,           'AppsOnServer.unknown did not have the correct updated value');
           assert.isTrue(Array.isArray(appsOnServer.list),       'AppsOnServer.list was not an Array');
           assert.equal(appsOnServer.list.length, 2,           'AppsOnServer.list.length was not updated to be 2');
           assert.equal(appsOnServer.list[0].name, 'snoop',     'AppsOnServer.list[0] did not have the correct updated value for the object name');
           assert.equal(appsOnServer.list[1].name, 'lynne',     'AppsOnServer.list[1] did not have the correct updated value for the object name');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newTally,             'AppsOnServerObserver.newTally did not get set, when it should have been');
           assert.equal(observer.newTally.up,      2, 'AppsOnServerObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    0, 'AppsOnServerObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 0, 'AppsOnServerObserver did not get the correct new value for the unknown tally');
  
           assert.isNotNull(observer.oldTally,             'AppsOnServerObserver.oldTally did not get set, when it should have been');
           assert.equal(observer.oldTally.up,      3, 'AppsOnServerObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    0, 'AppsOnServerObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 0, 'AppsOnServerObserver did not get the correct old value for the unknown tally');
  
           assert.isNotNull(observer.newList,              'AppsOnServerObserver.newList did not get set, when it should have been');
           assert.equal(observer.newList.length,   2, 'AppsOnServerObserver.newList was not of expected size');
           assert.equal(observer.newList[0].name, 'snoop', 'AppsOnServerObserver.newList[0] did not have the correct value for the object name');
           assert.equal(observer.newList[1].name, 'lynne', 'AppsOnServerObserver.newList[1] did not have the correct value for the object name');
           assert.isNotNull(observer.oldList,              'AppsOnServerObserver.oldList did not get set, when it should have been');
           assert.equal(observer.oldList.length,   3, 'AppsOnServerObserver.oldList was not empty');
           assert.equal(observer.oldList[0].name,   'snoop', 'AppsOnServerObserver.oldList[0] did not have the correct value for the object name');
           assert.equal(observer.oldList[1].name, 'melanie', 'AppsOnServerObserver.oldList[1] did not have the correct value for the object name');
           assert.equal(observer.oldList[2].name,   'lynne', 'AppsOnServerObserver.oldList[2] did not have the correct value for the object name');
           assert.isUndefined(observer.added,               'AppsOnServerObserver.added got set when it should not have been');
           assert.isNotNull(observer.removed,              'AppsOnServerObserver.removed did not get set, when it should have been');
           assert.equal(observer.removed.length,   1, 'AppsOnServerObserver.removed was not empty');
           assert.equal(observer.removed[0], 'melanie', 'AppsOnServerObserver.removed[0] did not have the correct value for the object name');
         }),
  
         tdd.test('Server changes - server was removed from the collective', function() {
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
           var appsOnServer = new AppsOnServer({server: server, appOnServer: []});
           var observer = new AppsOnServerObserver();
  
           appsOnServer.subscribe(observer);
  
           // Server is removed from collective
           server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', state: 'removed'});
  
           assert.isTrue(appsOnServer.isDestroyed,    'AppsOnServer.isDestroyed flag did not get set in response to a "removed" event');
           
           // Confirm the application is destroyed
           assert.isTrue(observer.destroyed,          'AppOnServerObserver.onDestroyed did not get called');
         });
    });
   }
});
