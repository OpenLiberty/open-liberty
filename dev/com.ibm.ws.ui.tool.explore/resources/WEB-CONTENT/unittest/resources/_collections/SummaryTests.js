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
 * Test cases for Summary
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_collections/Summary",
        "resources/Observer"
        ], 
    function(tdd, assert, declare, Summary, Observer) {

    var SummaryObserver = declare([Observer], {
      id: 'testObserver',
  
      onApplicationsTallyChange: function(newTally, oldTally) {
        this.newAppsTally = newTally;
        this.oldAppsTally = oldTally;
      },
      
      onClustersTallyChange: function(newTally, oldTally) {
        this.newClustersTally = newTally;
        this.oldClustersTally = oldTally;
      },
      
      onServersTallyChange: function(newTally, oldTally) {
        this.newServersTally = newTally;
        this.oldServersTally = oldTally;
      },
      
      onHostsTallyChange: function(newTally, oldTally) {
        this.newHostsTally = newTally;
        this.oldHostsTally = oldTally;
      }
    });
    
    function createInitializedSummary() {
      var apps = {up: 1, down: 2, unknown: 3, partial: 4};
      var clusters = {up: 5, down: 6, unknown: 7, partial: 8};
      var servers = {up: 9, down: 10, unknown: 11};
      var hosts = {up: 12, down: 13, unknown: 14, partial: 15, empty: 16};
      var runtimes = {up: 12, down: 13, unknown: 14, partial: 15, empty: 16};
      return new Summary({name: 'name', uuid: '123', applications: apps, clusters: clusters, servers: servers, hosts: hosts, runtimes: runtimes});
    }
    
    with(assert) {
  
      /**
       * Defines the 'Summary Collection Tests' module test suite.
       */
      tdd.suite('Summary Collection Tests', function() {
    
          tdd.test('constructor - no initialization object', function() {
             try {
               new Summary();
               assert.isNotNull(false, 'Summary was successfully created when it should have failed - an initialization object is required');
             } catch(error) {
               assert.equal(error, 'Summary created without an initialization object', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial name', function() {
             try {
               var apps = {};
               var clusters = {};
               var servers = {};
               var hosts = {};
               new Summary({uuid: '123', applications: apps, clusters: clusters, servers: servers, hosts: hosts});
               assert.isNotNull(false, 'Summary was successfully created when it should have failed - an initial name value is required');
             } catch(error) {
               assert.equal(error, 'Summary created without a name', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial uuid', function() {
             try {
               var apps = {};
               var clusters = {};
               var servers = {};
               var hosts = {};
               new Summary({name: 'name', applications: apps, clusters: clusters, servers: servers, hosts: hosts});
               assert.isNotNull(false, 'Summary was successfully created when it should have failed - an initial uuid value is required');
             } catch(error) {
               assert.equal(error, 'Summary created without a uuid', 'Error reported did not match expected error');
             }
           }),
           
           tdd.test('constructor - no initial applications', function() {
             try {
               var clusters = {};
               var servers = {};
               var hosts = {};
               new Summary({name: 'name', uuid: '123', clusters: clusters, servers: servers, hosts: hosts});
               assert.isNotNull(false, 'Summary was successfully created when it should have failed - an initial applications value is required');
             } catch(error) {
               assert.equal(error, 'Summary created without an applications tally', 'Error reported did not match expected error');
             }
           }),
           
           tdd.test('constructor - no initial clusters', function() {
             try {
               var apps = {};
               var servers = {};
               var hosts = {};
               new Summary({name: 'name', uuid: '123', applications: apps, servers: servers, hosts: hosts});
               assert.isNotNull(false, 'Summary was successfully created when it should have failed - an initial clusters value is required');
             } catch(error) {
               assert.equal(error, 'Summary created without an clusters tally', 'Error reported did not match expected error');
             }
           }),
           
           tdd.test('constructor - no initial servers', function() {
             try {
               var apps = {};
               var clusters = {};
               var hosts = {};
               new Summary({name: 'name', uuid: '123', applications: apps, clusters: clusters, hosts: hosts});
               assert.isNotNull(false, 'Summary was successfully created when it should have failed - an initial servers value is required');
             } catch(error) {
               assert.equal(error, 'Summary created without an servers tally', 'Error reported did not match expected error');
             }
           }),
           
           tdd.test('constructor - no initial hosts', function() {
             try {
               var apps = {};
               var clusters = {};
               var servers = {};
               new Summary({name: 'name', uuid: '123', applications: apps, clusters: clusters, servers: servers});
               assert.isNotNull(false, 'Summary was successfully created when it should have failed - an initial hosts value is required');
             } catch(error) {
               assert.equal(error, 'Summary created without an hosts tally', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - initial values', function() {
             var summary = createInitializedSummary();
    
             assert.equal(summary.name,             'name', 'Summary.name did not have the correct initialized value');
             assert.equal(summary.uuid,             '123',  'Summary.uuid did not have the correct initialized value');
             
             assert.isNotNull(summary.applications,              'Summary.applications was not set');
             assert.equal(summary.applications.up,      1,  'Summary.applications.up did not have the correct initialized value');
             assert.equal(summary.applications.down,    2,  'Summary.applications.down did not have the correct initialized value');
             assert.equal(summary.applications.unknown, 3,  'Summary.applications.unknown did not have the correct initialized value');
             assert.equal(summary.applications.partial, 4,  'Summary.applications.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.clusters,               'Summary.clusters was not set');
             assert.equal(summary.clusters.up,       5,  'Summary.clusters.up did not have the correct initialized value');
             assert.equal(summary.clusters.down,     6,  'Summary.clusters.down did not have the correct initialized value');
             assert.equal(summary.clusters.unknown,  7,  'Summary.clusters.unknown did not have the correct initialized value');
             assert.equal(summary.clusters.partial,  8,  'Summary.clusters.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.servers,                'Summary.servers was not set');
             assert.equal(summary.servers.up,        9,  'Summary.servers.up did not have the correct initialized value');
             assert.equal(summary.servers.down,      10, 'Summary.servers.down did not have the correct initialized value');
             assert.equal(summary.servers.unknown,   11, 'Summary.servers.unknown did not have the correct initialized value');
             
             assert.isNotNull(summary.hosts,                  'Summary.hosts was not set');
             assert.equal(summary.hosts.up,          12, 'Summary.hosts.up did not have the correct initialized value');
             assert.equal(summary.hosts.down,        13, 'Summary.hosts.down did not have the correct initialized value');
             assert.equal(summary.hosts.unknown,     14, 'Summary.hosts.unknown did not have the correct initialized value');
             assert.equal(summary.hosts.partial,     15, 'Summary.hosts.partial did not have the correct initialized value');
             assert.equal(summary.hosts.empty,       16, 'Summary.hosts.empty did not have the correct initialized value');
           }),
    
           tdd.test('handleChangeEvent - update applications only & observer invocation', function() {
             var observer = new SummaryObserver();
             var summary = createInitializedSummary();
    
             summary.subscribe(observer);
             
             // Send event
             summary._handleChangeEvent({type: 'summary', applications: {up: 20, down: 21, unknown: 22, partial: 23}});
    
             assert.isNotNull(summary.applications,              'Summary.applications was not set');
             assert.equal(summary.applications.up,      20, 'Summary.applications.up did not have the correct initialized value');
             assert.equal(summary.applications.down,    21, 'Summary.applications.down did not have the correct initialized value');
             assert.equal(summary.applications.unknown, 22, 'Summary.applications.unknown did not have the correct initialized value');
             assert.equal(summary.applications.partial, 23, 'Summary.applications.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.clusters,               'Summary.clusters was not set');
             assert.equal(summary.clusters.up,       5,  'Summary.clusters.up did not have the correct initialized value');
             assert.equal(summary.clusters.down,     6,  'Summary.clusters.down did not have the correct initialized value');
             assert.equal(summary.clusters.unknown,  7,  'Summary.clusters.unknown did not have the correct initialized value');
             assert.equal(summary.clusters.partial,  8,  'Summary.clusters.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.servers,                'Summary.servers was not set');
             assert.equal(summary.servers.up,        9,  'Summary.servers.up did not have the correct initialized value');
             assert.equal(summary.servers.down,      10, 'Summary.servers.down did not have the correct initialized value');
             assert.equal(summary.servers.unknown,   11, 'Summary.servers.unknown did not have the correct initialized value');
             
             assert.isNotNull(summary.hosts,                  'Summary.hosts was not set');
             assert.equal(summary.hosts.up,          12, 'Summary.hosts.up did not have the correct initialized value');
             assert.equal(summary.hosts.down,        13, 'Summary.hosts.down did not have the correct initialized value');
             assert.equal(summary.hosts.unknown,     14, 'Summary.hosts.unknown did not have the correct initialized value');
             assert.equal(summary.hosts.partial,     15, 'Summary.hosts.partial did not have the correct initialized value');
             assert.equal(summary.hosts.empty,       16, 'Summary.hosts.empty did not have the correct initialized value');
             
             assert.isNotNull(observer.newAppsTally,           'The onApplicationsTallyChange method was not called');
             assert.equal(observer.newAppsTally.up,   20, 'The onApplicationsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newAppsTally.down, 21, 'The onApplicationsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newAppsTally.unknown, 22, 'The onApplicationsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newAppsTally.partial, 23, 'The onApplicationsTallyChange method was not called with the correct previous up tally');
             
             assert.isNotNull(observer.oldAppsTally,           'The onApplicationsTallyChange method was not called');
             assert.equal(observer.oldAppsTally.up,   1,  'The onApplicationsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldAppsTally.down, 2,  'The onApplicationsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldAppsTally.unknown, 3,  'The onApplicationsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldAppsTally.partial, 4,  'The onApplicationsTallyChange method was not called with the correct previous up tally');
             
             assert.isUndefined(observer.newClustersTally, 'The event should not invoke the onClustersTallyChange method');
             assert.isUndefined(observer.oldClustersTally, 'The event should not invoke the onClustersTallyChange method');
             assert.isUndefined(observer.newServersTally,  'The event should not invoke the onServersTallyChange method');
             assert.isUndefined(observer.oldServersTally,  'The event should not invoke the onServersTallyChange method');
             assert.isUndefined(observer.newHostsTally,    'The event should not invoke the onHostsTallyChange method');
             assert.isUndefined(observer.oldHostsTally,    'The event should not invoke the onHostsTallyChange method');
           }),
           
           tdd.test('handleChangeEvent - update clusters only & observer invocation', function() {
             var observer = new SummaryObserver();
             var summary = createInitializedSummary();
    
             summary.subscribe(observer);
             
             // Send event
             summary._handleChangeEvent({type: 'summary', clusters: {up: 20, down: 21, unknown: 22, partial: 23}});
    
             assert.isNotNull(summary.applications,              'Summary.applications was not set');
             assert.equal(summary.applications.up,      1,  'Summary.applications.up did not have the correct initialized value');
             assert.equal(summary.applications.down,    2,  'Summary.applications.down did not have the correct initialized value');
             assert.equal(summary.applications.unknown, 3,  'Summary.applications.unknown did not have the correct initialized value');
             assert.equal(summary.applications.partial, 4,  'Summary.applications.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.clusters,              'Summary.clusters was not set');
             assert.equal(summary.clusters.up,      20, 'Summary.clusters.up did not have the correct initialized value');
             assert.equal(summary.clusters.down,    21, 'Summary.clusters.down did not have the correct initialized value');
             assert.equal(summary.clusters.unknown, 22, 'Summary.clusters.unknown did not have the correct initialized value');
             assert.equal(summary.clusters.partial, 23, 'Summary.clusters.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.servers,                'Summary.servers was not set');
             assert.equal(summary.servers.up,        9,  'Summary.servers.up did not have the correct initialized value');
             assert.equal(summary.servers.down,      10, 'Summary.servers.down did not have the correct initialized value');
             assert.equal(summary.servers.unknown,   11, 'Summary.servers.unknown did not have the correct initialized value');
             
             assert.isNotNull(summary.hosts,                  'Summary.hosts was not set');
             assert.equal(summary.hosts.up,          12, 'Summary.hosts.up did not have the correct initialized value');
             assert.equal(summary.hosts.down,        13, 'Summary.hosts.down did not have the correct initialized value');
             assert.equal(summary.hosts.unknown,     14, 'Summary.hosts.unknown did not have the correct initialized value');
             assert.equal(summary.hosts.partial,     15, 'Summary.hosts.partial did not have the correct initialized value');
             assert.equal(summary.hosts.empty,       16, 'Summary.hosts.empty did not have the correct initialized value');
             
             assert.isNotNull(observer.newClustersTally,           'The onClustersTallyChange method was not called');
             assert.equal(observer.newClustersTally.up,   20, 'The onClustersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newClustersTally.down, 21, 'The onClustersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newClustersTally.unknown, 22, 'The onClustersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newClustersTally.partial, 23, 'The onClustersTallyChange method was not called with the correct previous up tally');
             
             assert.isNotNull(observer.oldClustersTally,           'The onClustersTallyChange method was not called');
             assert.equal(observer.oldClustersTally.up,   5,  'The onClustersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldClustersTally.down, 6,  'The onClustersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldClustersTally.unknown, 7,  'The onClustersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldClustersTally.partial, 8,  'The onClustersTallyChange method was not called with the correct previous up tally');
             
             assert.isUndefined(observer.newAppsTally,     'The event should not invoke the onApplicationsTallyChange method');
             assert.isUndefined(observer.oldAppsTally,     'The event should not invoke the onApplicationsTallyChange method');
             assert.isUndefined(observer.newServersTally,  'The event should not invoke the onServersTallyChange method');
             assert.isUndefined(observer.oldServersTally,  'The event should not invoke the onServersTallyChange method');
             assert.isUndefined(observer.newHostsTally,    'The event should not invoke the onHostsTallyChange method');
             assert.isUndefined(observer.oldHostsTally,    'The event should not invoke the onHostsTallyChange method');
           }),
           
           tdd.test('handleChangeEvent - update servers only & observer invocation', function() {
             var observer = new SummaryObserver();
             var summary = createInitializedSummary();
    
             summary.subscribe(observer);
             
             // Send event
             summary._handleChangeEvent({type: 'summary', servers: {up: 20, down: 21, unknown: 22}});
    
             assert.isNotNull(summary.applications,              'Summary.applications was not set');
             assert.equal(summary.applications.up,      1,  'Summary.applications.up did not have the correct initialized value');
             assert.equal(summary.applications.down,    2,  'Summary.applications.down did not have the correct initialized value');
             assert.equal(summary.applications.unknown, 3,  'Summary.applications.unknown did not have the correct initialized value');
             assert.equal(summary.applications.partial, 4,  'Summary.applications.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.clusters,               'Summary.clusters was not set');
             assert.equal(summary.clusters.up,       5,  'Summary.clusters.up did not have the correct initialized value');
             assert.equal(summary.clusters.down,     6,  'Summary.clusters.down did not have the correct initialized value');
             assert.equal(summary.clusters.unknown,  7,  'Summary.clusters.unknown did not have the correct initialized value');
             assert.equal(summary.clusters.partial,  8,  'Summary.clusters.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.servers,              'Summary.servers was not set');
             assert.equal(summary.servers.up,      20, 'Summary.servers.up did not have the correct initialized value');
             assert.equal(summary.servers.down,    21, 'Summary.servers.down did not have the correct initialized value');
             assert.equal(summary.servers.unknown, 22, 'Summary.servers.unknown did not have the correct initialized value');
             
             assert.isNotNull(summary.hosts,                  'Summary.hosts was not set');
             assert.equal(summary.hosts.up,          12, 'Summary.hosts.up did not have the correct initialized value');
             assert.equal(summary.hosts.down,        13, 'Summary.hosts.down did not have the correct initialized value');
             assert.equal(summary.hosts.unknown,     14, 'Summary.hosts.unknown did not have the correct initialized value');
             assert.equal(summary.hosts.partial,     15, 'Summary.hosts.partial did not have the correct initialized value');
             assert.equal(summary.hosts.empty,       16, 'Summary.hosts.empty did not have the correct initialized value');
             
             assert.isNotNull(observer.newServersTally,           'The onServersTallyChange method was not called');
             assert.equal(observer.newServersTally.up,   20, 'The onServersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newServersTally.down, 21, 'The onServersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newServersTally.unknown, 22, 'The onServersTallyChange method was not called with the correct previous up tally');
             
             assert.isNotNull(observer.oldServersTally,           'The onServersTallyChange method was not called');
             assert.equal(observer.oldServersTally.up,   9,  'The onServersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldServersTally.down, 10, 'The onServersTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldServersTally.unknown, 11, 'The onServersTallyChange method was not called with the correct previous up tally');
             
             assert.isUndefined(observer.newAppsTally,     'The event should not invoke the onApplicationsTallyChange method');
             assert.isUndefined(observer.oldAppsTally,     'The event should not invoke the onApplicationsTallyChange method');
             assert.isUndefined(observer.newClustersTally, 'The event should not invoke the onClustersTallyChange method');
             assert.isUndefined(observer.oldClustersTally, 'The event should not invoke the onClustersTallyChange method');
             assert.isUndefined(observer.newHostsTally,    'The event should not invoke the onHostsTallyChange method');
             assert.isUndefined(observer.oldHostsTally,    'The event should not invoke the onHostsTallyChange method');
           }),
           
           tdd.test('handleChangeEvent - update hosts only & observer invocation', function() {
             var observer = new SummaryObserver();
             var summary = createInitializedSummary();
    
             summary.subscribe(observer);
             
             // Send event
             summary._handleChangeEvent({type: 'summary', hosts: {up: 20, down: 21, unknown: 22, partial: 23, empty: 24}});
    
             assert.isNotNull(summary.applications,              'Summary.applications was not set');
             assert.equal(summary.applications.up,      1,  'Summary.applications.up did not have the correct initialized value');
             assert.equal(summary.applications.down,    2,  'Summary.applications.down did not have the correct initialized value');
             assert.equal(summary.applications.unknown, 3,  'Summary.applications.unknown did not have the correct initialized value');
             assert.equal(summary.applications.partial, 4,  'Summary.applications.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.clusters,               'Summary.clusters was not set');
             assert.equal(summary.clusters.up,       5,  'Summary.clusters.up did not have the correct initialized value');
             assert.equal(summary.clusters.down,     6,  'Summary.clusters.down did not have the correct initialized value');
             assert.equal(summary.clusters.unknown,  7,  'Summary.clusters.unknown did not have the correct initialized value');
             assert.equal(summary.clusters.partial,  8,  'Summary.clusters.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.servers,                'Summary.servers was not set');
             assert.equal(summary.servers.up,        9,  'Summary.servers.up did not have the correct initialized value');
             assert.equal(summary.servers.down,      10, 'Summary.servers.down did not have the correct initialized value');
             assert.equal(summary.servers.unknown,   11, 'Summary.servers.unknown did not have the correct initialized value');
    
             assert.isNotNull(summary.hosts,                  'Summary.hosts was not set');
             assert.equal(summary.hosts.up,          20, 'Summary.hosts.up did not have the correct initialized value');
             assert.equal(summary.hosts.down,        21, 'Summary.hosts.down did not have the correct initialized value');
             assert.equal(summary.hosts.unknown,     22, 'Summary.hosts.unknown did not have the correct initialized value');
             assert.equal(summary.hosts.partial,     23, 'Summary.hosts.partial did not have the correct initialized value');
             assert.equal(summary.hosts.empty,       24, 'Summary.hosts.partial did not have the correct initialized value');
             
             assert.isNotNull(observer.newHostsTally,           'The onHostsTallyChange method was not called');
             assert.equal(observer.newHostsTally.up,   20, 'The onHostsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newHostsTally.down, 21, 'The onHostsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newHostsTally.unknown, 22, 'The onHostsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.newHostsTally.partial, 23, 'The onHostsTallyChange method was not called with the correct previous up tally');
             
             assert.isNotNull(observer.oldHostsTally,           'The onHostsTallyChange method was not called');
             assert.equal(observer.oldHostsTally.up,   12, 'The onHostsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldHostsTally.down, 13, 'The onHostsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldHostsTally.unknown, 14, 'The onHostsTallyChange method was not called with the correct previous up tally');
             assert.equal(observer.oldHostsTally.partial, 15, 'The onHostsTallyChange method was not called with the correct previous up tally');
    
             assert.isUndefined(observer.newAppsTally,     'The event should not invoke the onApplicationsTallyChange method');
             assert.isUndefined(observer.oldAppsTally,     'The event should not invoke the onApplicationsTallyChange method');
             assert.isUndefined(observer.newClustersTally, 'The event should not invoke the onClustersTallyChange method');
             assert.isUndefined(observer.oldClustersTally, 'The event should not invoke the onClustersTallyChange method');
             assert.isUndefined(observer.newServersTally,  'The event should not invoke the onServersTallyChange method');
             assert.isUndefined(observer.oldServersTally,  'The event should not invoke the onServersTallyChange method');
           }),
    
           tdd.test('handleChangeEvent - unset or wrong event type is ignored', function() {
             var observer = new SummaryObserver();
             var summary = createInitializedSummary();
    
             summary.subscribe(observer);
             
             // Simulate ignored events
             summary._handleChangeEvent({applications: {up: 20, down: 21, unknown: 22, partial: 23}});
             summary._handleChangeEvent({type: 'servers', applications: {up: 20, down: 21, unknown: 22, partial: 23}});
    
             assert.equal(summary.name,             'name', 'Summary.name did not have the correct initialized value');
             assert.equal(summary.uuid,             '123',  'Summary.uuid did not have the correct initialized value');
             
             assert.isNotNull(summary.applications,              'Summary.applications was not set');
             assert.equal(summary.applications.up,      1,  'Summary.applications.up did not have the correct initialized value');
             assert.equal(summary.applications.down,    2,  'Summary.applications.down did not have the correct initialized value');
             assert.equal(summary.applications.unknown, 3,  'Summary.applications.unknown did not have the correct initialized value');
             assert.equal(summary.applications.partial, 4,  'Summary.applications.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.clusters,               'Summary.clusters was not set');
             assert.equal(summary.clusters.up,       5,  'Summary.clusters.up did not have the correct initialized value');
             assert.equal(summary.clusters.down,     6,  'Summary.clusters.down did not have the correct initialized value');
             assert.equal(summary.clusters.unknown,  7,  'Summary.clusters.unknown did not have the correct initialized value');
             assert.equal(summary.clusters.partial,  8,  'Summary.clusters.partial did not have the correct initialized value');
             
             assert.isNotNull(summary.servers,                'Summary.servers was not set');
             assert.equal(summary.servers.up,        9,  'Summary.servers.up did not have the correct initialized value');
             assert.equal(summary.servers.down,      10, 'Summary.servers.down did not have the correct initialized value');
             assert.equal(summary.servers.unknown,   11, 'Summary.servers.unknown did not have the correct initialized value');
             
             assert.isNotNull(summary.hosts,                  'Summary.hosts was not set');
             assert.equal(summary.hosts.up,          12, 'Summary.hosts.up did not have the correct initialized value');
             assert.equal(summary.hosts.down,        13, 'Summary.hosts.down did not have the correct initialized value');
             assert.equal(summary.hosts.unknown,     14, 'Summary.hosts.unknown did not have the correct initialized value');
             assert.equal(summary.hosts.partial,     15, 'Summary.hosts.partial did not have the correct initialized value');
             assert.equal(summary.hosts.empty,       16, 'Summary.hosts.empty did not have the correct initialized value');
           });  
        });
    }
});
