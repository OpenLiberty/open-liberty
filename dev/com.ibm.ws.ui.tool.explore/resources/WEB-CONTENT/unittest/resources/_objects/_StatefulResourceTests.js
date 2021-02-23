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
 * Test cases for StatefulResource
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "intern/dojo/declare",
     "resources/_objects/_StatefulResource"
       ],
       
    function(tdd, assert, declare, StatefulResource) {

    /** Function definition used by the tests */
    var noop = function() {/*no-op*/};
  
    with(assert) {
      
    /**
     * Defines the 'StatefulResource Object Tests' module test suite.
     */
    tdd.suite('StatefulResource Object Tests', function() {
         tdd.test('constructor - no initialization object', function() {
           try {
             new StatefulResource();
             assert.ok(false, 'StatefulResource was successfully created when it should have failed - an initialization object is required');
           } catch(error) {
             assert.equal(error, 'TypedListener created without an initialization object', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - no initial state', function() {
           try {
             new StatefulResource({id: 'resource1', type: 'resource', _handleChangeEvent: noop, start: noop, stop: noop, restart: noop});
             assert.ok(false, 'StatefulResource was successfully created when it should have failed - a state is required');
           } catch(error) {
             assert.equal(error, 'StatefulResource created without a state', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - no start operation', function() {
           try {
             new StatefulResource({id: 'resource1', type: 'resource', _handleChangeEvent: noop, state: 'UP', stop: noop, restart: noop });
             assert.ok(false, 'StatefulResource was successfully created when it should have failed - a start operation is required');
           } catch(error) {
             assert.equal(error, 'StatefulResource created without a start operation', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - no stop operation', function() {
           try {
             new StatefulResource({id: 'resource1', type: 'resource', _handleChangeEvent: noop, state: 'UP', start: noop, restart: noop });
             assert.ok(false, 'StatefulResource was successfully created when it should have failed - a stop operation is required');
           } catch(error) {
             assert.equal(error, 'StatefulResource created without a stop operation', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - no restart operation', function() {
           try {
             new StatefulResource({id: 'resource1', type: 'resource', _handleChangeEvent: noop, state: 'UP', start: noop, stop: noop });
             assert.ok(false, 'StatefulResource was successfully created when it should have failed - a restart operation is required');
           } catch(error) {
             assert.equal(error, 'StatefulResource created without a restart operation', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - extender which hard-codes state is not supported', function() {
           try {
             // Example of invalid extender which tries to hard-code the state
             var InvalidExtender = declare([StatefulResource], {
               state: 'up', // It is not valid to hard-code the initial state
               id: 'resource1',
               type: 'resource',
               _handleChangeEvent: noop,
               start: noop,
               stop: noop,
               restart: noop
             });
             new InvalidExtender({});
             assert.ok(false, 'StatefulResource was successfully created when it should have failed - a state is required');
           } catch(error) {
             assert.equal(error, 'StatefulResource created without a state', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - extender which injects the state is valid', function() {
           var ValidExtender = declare([StatefulResource], {
             id: 'resource1',
             type: 'resource',
             _handleChangeEvent: noop,
             start: noop,
             stop: noop,
             restart: noop
           });
           var stateful = new ValidExtender({ state: 'up' });
  
           assert.equal(stateful.state,   'up', 'StatefulResource created with wrong initial value for state');
           assert.equal(stateful.start,   noop, 'StatefulResource created with wrong value for start operation');
           assert.equal(stateful.stop,    noop, 'StatefulResource created with wrong value for stop operation');
           assert.equal(stateful.restart, noop, 'StatefulResource created with wrong value for restart operation');
         });
      });
    }
});
