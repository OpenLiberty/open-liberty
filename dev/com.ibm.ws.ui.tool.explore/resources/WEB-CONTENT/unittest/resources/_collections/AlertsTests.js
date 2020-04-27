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
 * Test cases for Alerts
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_collections/Alerts",
        "resources/Observer"
        ], 
    function(tdd, assert, declare, Alerts, Observer) {

    var AlertsObserver = declare([Observer], {
      id: 'testObserver',
  
      onAlertsChange: function(newAlerts, oldAlerts) {
        this.newAlerts = newAlerts;
        this.oldAlerts = oldAlerts;
      }
    });

    with(assert) {
      
      /**
       * Defines the 'Alerts Collection Tests' module test suite.
       */
      tdd.suite('Alerts Collection Tests', function() {

           tdd.test('constructor - no initialization object', function() {
             try {
               new Alerts();
               assert.isTrue(false, 'Alerts was successfully created when it should have failed - an initialization object is required');
             } catch(error) {
               assert.equal(error, 'Alerts created without an initialization object', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial count', function() {
             try {
               new Alerts({unknown: [], app: []});
               assert.isTrue(false, 'Alerts was successfully created when it should have failed - an initial count value is required');
             } catch(error) {
               assert.equal(error, 'Alerts created without a count', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial unknown list', function() {
             try {
               new Alerts({count: 0,  app: []});
               assert.isTrue(false, 'Alerts was successfully created when it should have failed - an initial unknown list value is required');
             } catch(error) {
               assert.equal(error, 'Alerts created without an unknown list', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial app list', function() {
             try {
               new Alerts({count: 0, unknown: []});
               assert.isTrue(false, 'Alerts was successfully created when it should have failed - an initial app list value is required');
             } catch(error) {
               assert.equal(error, 'Alerts created without an app list', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - initial values empty', function() {
             var alerts = new Alerts({count: 0, unknown: [], app: []});
    
             assert.equal(alerts.count,           0, 'Alerts.count did not have the correct initialized value');
    
             assert.isNotNull(alerts.unknown,             'Alerts.unknown was not set');
             assert.equal(alerts.unknown.length, 0,  'Alerts.unknown.length did not have the correct initialized value');
             assert.isNotNull(alerts.app,                 'Alerts.app was not set');
             assert.equal(alerts.app.length,     0,  'Alerts.app.legnth did not have the correct initialized value');
           }),
    
           tdd.test('constructor - initial values some set', function() {
             var alerts = new Alerts({count: 2, unknown: [{id: 's1', type: 'server'}], app: [{name:'s1,app',servers: ['s1']}]});
    
             assert.equal(alerts.count,                        2, 'Alerts.count did not have the correct initialized value');
             assert.isNotNull(alerts.unknown,                          'Alerts.unknown was not set');
             assert.equal(alerts.unknown.length,        1,        'Alerts.unknown.length did not have the correct initialized value');
             assert.equal(alerts.unknown[0].id,         's1',     'Alerts.unknown[0].name did not have the correct initialized value');
             assert.equal(alerts.unknown[0].type,       'server', 'Alerts.unknown[0].type did not have the correct initialized value');
             assert.isNotNull(alerts.app,                              'Alerts.app was not set');
             assert.equal(alerts.app.length,            1,        'Alerts.app.legnth did not have the correct initialized value');
             assert.equal(alerts.app[0].name,           's1,app',    'Alerts.app[0].name did not have the correct initialized value');
             assert.equal(alerts.app[0].servers.length, 1,        'Alerts.app[0].servers.length did not have the correct initialized value');
             assert.equal(alerts.app[0].servers[0],     's1',     'Alerts.app[0].servers[0] did not have the correct initialized value');
           }),
    
           tdd.test('handleChangeEvent - update applications only & observer invocation', function() {
             var observer = new AlertsObserver();
             var alerts = new Alerts({count: 0, unknown: [], app: []});
    
             assert.equal(alerts.count,           0, 'Alerts.count did not have the correct initialized value');
             assert.isNotNull(alerts.unknown,             'Alerts.unknown was not set');
             assert.equal(alerts.unknown.length, 0,  'Alerts.unknown.length did not have the correct initialized value');
             assert.isNotNull(alerts.app,                 'Alerts.app was not set');
             assert.equal(alerts.app.length,     0,  'Alerts.app.legnth did not have the correct initialized value');
    
             alerts.subscribe(observer);
    
             // Send event
             alerts._handleChangeEvent({type: 'alerts', count: 2, unknown: [{id: 's1', type: 'server'}], app: [{name:'s1,app',servers: ['s1']}]});
    
             assert.equal(2, alerts.count,                        'Alerts.count did not have the correct updated value');
             assert.isNotNull(alerts.unknown,                          'Alerts.unknown was not set');
             assert.equal(alerts.unknown.length,        1,        'Alerts.unknown.length did not have the correct updated value');
             assert.equal(alerts.unknown[0].id,         's1',     'Alerts.unknown[0].name did not have the correct updated value');
             assert.equal(alerts.unknown[0].type,       'server', 'Alerts.unknown[0].type did not have the correct updated value');
             assert.isNotNull(alerts.app,                              'Alerts.app was not set');
             assert.equal(alerts.app.length,            1,        'Alerts.app.legnth did not have the correct updated value');
             assert.equal(alerts.app[0].name,           's1,app',    'Alerts.app[0].name did not have the correct updated value');
             assert.equal(alerts.app[0].servers.length, 1,        'Alerts.app[0].servers.length did not have the correct updated value');
             assert.equal(alerts.app[0].servers[0],     's1',     'Alerts.app[0].servers[0] did not have the correct updated value');
    
             // Check observer received values
             assert.equal(2, observer.newAlerts.count,                        'Observer.newAlerts.count did not have the correct updated value');
             assert.isNotNull(observer.newAlerts.unknown,                          'Observer.newAlerts.unknown was not set');
             assert.equal(observer.newAlerts.unknown.length,        1,        'Observer.newAlerts.unknown.length did not have the correct updated value');
             assert.equal(observer.newAlerts.unknown[0].id,         's1',     'Observer.newAlerts.unknown[0].name did not have the correct updated value');
             assert.equal(observer.newAlerts.unknown[0].type,       'server', 'Observer.newAlerts.unknown[0].type did not have the correct updated value');
             assert.isNotNull(observer.newAlerts.app,                              'Observer.newAlerts.app was not set');
             assert.equal(observer.newAlerts.app.length,            1,        'Observer.newAlerts.app.legnth did not have the correct updated value');
             assert.equal(observer.newAlerts.app[0].name,           's1,app',    'Observer.newAlerts.app[0].name did not have the correct updated value');
             assert.equal(observer.newAlerts.app[0].servers.length, 1,        'Observer.newAlerts.app[0].servers.length did not have the correct updated value');
             assert.equal(observer.newAlerts.app[0].servers[0],     's1',     'Observer.newAlerts.app[0].servers[0] did not have the correct updated value');
    
             assert.equal(observer.oldAlerts.count,           0, 'Observer.oldAlerts.count did not have the correct initialized value');
             assert.isNotNull(observer.oldAlerts.unknown,             'Observer.oldAlerts.unknown was not set');
             assert.equal(observer.oldAlerts.unknown.length, 0,  'Observer.oldAlerts.unknown.length did not have the correct initialized value');
             assert.isNotNull(observer.oldAlerts.app,                 'Observer.oldAlerts.app was not set');
             assert.equal(observer.oldAlerts.app.length,     0,  'Observer.oldAlerts.app.legnth did not have the correct initialized value');
           }),
           
           tdd.test('handleChangeEvent - unset or wrong event type is ignored', function() {
             var observer = new AlertsObserver();
             var alerts = new Alerts({count: 0, unknown: [], app: []});
    
             assert.equal(alerts.count,           0, 'Alerts.count did not have the correct initialized value');
             assert.isNotNull(alerts.unknown,             'Alerts.unknown was not set');
             assert.equal(alerts.unknown.length, 0,  'Alerts.unknown.length did not have the correct initialized value');
             assert.isNotNull(alerts.app,                 'Alerts.app was not set');
             assert.equal(alerts.app.length,     0,  'Alerts.app.legnth did not have the correct initialized value');
    
             alerts.subscribe(observer);
    
             // Send event
             alerts._handleChangeEvent({count: 2, unknown: [{id: 's1', type: 'server'}], app: [{name:'s1,app',servers: ['s1']}]});
             alerts._handleChangeEvent({type: 'servers', count: 2, unknown: [{id: 's1', type: 'server'}], app: [{name:'s1,app',servers: ['s1']}]});
             
             assert.equal(alerts.count,           0, 'Alerts.count did not have the correct initialized value');
             assert.isNotNull(alerts.unknown,             'Alerts.unknown was not set');
             assert.equal(alerts.unknown.length, 0,  'Alerts.unknown.length did not have the correct initialized value');
             assert.isNotNull(alerts.app,                 'Alerts.app was not set');
             assert.equal(alerts.app.length,     0,  'Alerts.app.legnth did not have the correct initialized value');
           });    
      });
    }
});
