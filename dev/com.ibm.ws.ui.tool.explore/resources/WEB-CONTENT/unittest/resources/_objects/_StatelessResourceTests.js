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
 * Test cases for StatelessResource
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "intern/dojo/declare",
     "resources/_objects/_StatelessResource"
       ],
       
    function(tdd, assert, declare, StatelessResource) {

    with(assert) {
      
      /**
       * Defines the 'StatelessResource Object Tests' module test suite.
       */
      tdd.suite('StatelessResource Object Tests', function() {
          tdd.test('constructor - no initialization object', function() {
            try {
              new StatelessResource();
              assert.ok(false, 'StatelessResource was successfully created when it should have failed - an initialization object is required');
            } catch(error) {
              assert.equal(error, 'TypedListener created without an initialization object', 'Error reported did not match expected error');
            }
          });
    
          tdd.test('constructor - no initial id', function() {
            try {
              new StatelessResource({type: 'resource', _handleChangeEvent: function() {/*no-op*/}});
              assert.ok(false, 'StatelessResource was successfully created when it should have failed - an id is required');
            } catch(error) {
              assert.equal(error, 'ObservableResource created without an id', 'Error reported did not match expected error');
            }
          });
    
          tdd.test('constructor - default name to ID', function() {
            var stateless = new StatelessResource({ id: 'resource1', type: 'resource', _handleChangeEvent: function() {/*no-op*/} });
            assert.equal(stateless.id,   'resource1', 'StatelessResource was constructed with the wrong id');
            assert.equal(stateless.name, 'resource1', 'StatelessResource was constructed with the wrong name - should be equal to ID when not specified');
            assert.notOk(stateless.alerts,            'StatelessResource was constructed with no alerts but the alerts attribute was not falsy');
          });
    
          tdd.test('constructor - specify name', function() {
            var stateless = new StatelessResource({ id: 'resource1', type: 'resource', name: 'myResource', _handleChangeEvent: function() {/*no-op*/} });
            assert.equal(stateless.id,   'resource1',  'StatelessResource was constructed with the wrong id');
            assert.equal(stateless.name, 'myResource', 'StatelessResource was constructed with the wrong name');
            assert.notOk(stateless.alerts,            'StatelessResource was constructed with no alerts but the alerts attribute was not falsy');
          });
          
          tdd.test('constructor - hard-coded name in sub-class', function() {
            var HardcodedNamedResource = declare([StatelessResource], {
              name: 'myName'
            });
            var named = new HardcodedNamedResource({ id: 'resource1', type: 'resource', _handleChangeEvent: function() {/*no-op*/} });
            assert.equal(named.id,   'resource1', 'HardcodedNamedResource was constructed with the wrong id');
            assert.equal(named.name, 'myName',    'HardcodedNamedResource was constructed with the wrong name - should be equal the name of the child object');
            assert.notOk(named.alerts,            'HardcodedNamedResource was constructed with no alerts but the alerts attribute was not falsy');
          });
          
          tdd.test('constructor - name set by sub-class constructor', function() {
            var ConstructorNamedResource = declare([StatelessResource], {
              constructor: function() {
                this.name = 'myName';
              }
            });
            var named = new ConstructorNamedResource({ id: 'resource1', type: 'resource', _handleChangeEvent: function() {/*no-op*/} });
            assert.equal(named.id,   'resource1', 'ConstructorNamedResource was constructed with the wrong id');
            assert.equal(named.name, 'myName',    'ConstructorNamedResource was constructed with the wrong name - should be equal the name of the child object');
            assert.notOk(named.alerts,            'ConstructorNamedResource was constructed with no alerts but the alerts attribute was not falsy');
          });
          
          tdd.test('constructor - alerts provided during construction', function() {
            var alerts = { count: 0 };
            var ConstructorNamedResource = declare([StatelessResource], {
              constructor: function() {
                this.name = 'myName';
              }
            });
            var named = new ConstructorNamedResource({ id: 'resource1', type: 'resource', _handleChangeEvent: function() {/*no-op*/}, alerts: alerts });
            assert.equal(named.id,     'resource1', 'ConstructorNamedResource was constructed with the wrong id');
            assert.equal(named.name,   'myName',    'ConstructorNamedResource was constructed with the wrong name - should be equal the name of the child object');
            assert.equal(named.alerts, alerts,      'ConstructorNamedResource was constructed with a set of alerts but the alerts attribute was not correct');
          });
        });
    }
});
