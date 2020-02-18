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
 * Test cases for TypedListener
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "intern/dojo/declare",
     "resources/_TypedListener",
     "dojo/topic",
     "resources/_topicUtil"
       ],
     
    function(tdd, assert, declare, TypedListener, topic, topicUtil) {

    /**
     * The TypedExtender simulates a resource class with a type but no handleChangeEvent method.
     * This is an example of a non-valid extender of TypedListener.
     */
    var TypedExtender = declare([TypedListener], {
      type: 'servers',
    });
  
    /**
     * The UntypedExtender simulates a resource class with a handleChangeEvent method but no type.
     * This is an example of a non-valid extender of TypedListener.
     */
    var UntypedExtender = declare([TypedListener], {
      _handleChangeEvent: function() { /*no-op */ }
    });
  
    /**
     * The ValidExtender simulates a resource class. These classes are defined by type and therefore
     * hard-code their type value. The ValidExtender serves as a test double for the resource classes
     * to ensure we can create a TypedListener with a hard-coded type and handleChangeEvent method.
     */
    var ValidExtender = declare([TypedListener], {
      type: 'servers',
      
      /**
       * When handleChangeEvent is invoked, an internal test spy 'handlerCalled' is set to true.
       * This is how we can detect that the handleChangeEvent method was called and that the
       * listener's topic was subscribed to. 
       */
      _handleChangeEvent: function() {
        this.handlerCalled = true;
      }
    });
  
    with (assert) {
    
    /**
     * Defines the 'TypedListener Object Tests' module test suite.
     */
    tdd.suite('TypedListener Object Tests', function() {
    
         tdd.test('constructor - no initialization object', function() {
           try {
             new TypedListener();
             assert.ok(false, 'TypedListener was successfully created when it should have failed - an initialization object is required');
           } catch(error) {
             assert.equal(error, 'TypedListener created without an initialization object', 'TypedListener creation error did not throw the expected error');
           }
         });
  
         tdd.test('constructor - no type', function() {
           try {
             new TypedListener({_handleChangeEvent: function() {/*no-op*/}});
             assert.ok(false, 'TypedListener was successfully created when it should have failed - a type is required');
           } catch(error) {
             assert.equal(error, 'TypedListener created without a type', 'TypedListener creation error did not throw the expected error');
           }
         });
  
         tdd.test('constructor - no handleChangeEvent', function() {
           try {
             new TypedListener({type: 'resource'});
             assert.ok(false, 'TypedListener was successfully created when it should have failed - a handleChangeEvent operation is required');
           } catch(error) {
             assert.equal(error, 'TypedListener created without a handleChangeEvent operation', 'TypedListener creation error did not throw the expected error');
           }
         });
  
         tdd.test('constructor - all required injected values', function() {
           new TypedListener({type: 'resource', _handleChangeEvent: function() {/*no-op*/} });
         });
  
         tdd.test('constructor - extended object with hard-coded type without handleChangeEvent', function() {
           try {
             new TypedExtender({});
             assert.ok(false, 'TypedListener was successfully created when it should have failed - a handleChangeEvent operation is required');
           } catch(error) {
             assert.equal(error, 'TypedListener created without a handleChangeEvent operation', 'TypedListener creation error did not throw the expected error');
           }
         });
  
         tdd.test('constructor - extended object with hard-coded type with injected handleChangeEvent', function() {
           new TypedExtender({ _handleChangeEvent: function() {/*no-op*/} });
         });
  
         tdd.test('constructor - extended object with hard-coded handleChangeEvent without type', function() {
           try {
             new UntypedExtender({});
             assert.ok(false, 'TypedListener was successfully created when it should have failed - a type is required');
           } catch(error) {
             assert.equal(error, 'TypedListener created without a type', 'TypedListener creation error did not throw the expected error');
           }
         });
  
         tdd.test('constructor - extended object with hard-coded handleChangeEvent with injected type', function() {
           new UntypedExtender({ type: 'resource' });
         });
  
         tdd.test('constructor - extended object with hard-coded handleChangeEvent and type', function() {
           new ValidExtender();
         });
  
         tdd.test('init - ensures topic creation and subhandle set', function() {
           var expectedTopic = topicUtil.getTopicByType('servers');
           var listener = new ValidExtender();
  
           var ret = listener.init();
           assert.equal(ret, listener, 'The TypedListener.init() operation did not return the listener instance');
           assert.equal(listener.__myTopic, expectedTopic, 'The value of TypedListener.__myTopic was not correct');
           assert.ok(listener.__subHandle, 'The TypedListener was initialized but did not have a subscription handle');
  
           topic.publish(expectedTopic, 'ignoredEvent');
  
           assert.ok(listener.handlerCalled, 'The listener.handleChangeEvent method was not called in response to a topic publish after it had been initialized');
         });
  
         tdd.test('destroy - after initialization', function() {
           var listener = new ValidExtender();
  
           listener.init();
           assert.ok(listener.__subHandle, 'The TypedListener was initialized but did not have a subscription handle');
  
           listener.destroy();
           assert.notOk(listener.__subHandle, 'The TypedListener was not initialized and should not have a subscription handle');
  
           topic.publish(topicUtil.getTopicByType('servers'), 'ignoredEvent');
  
           assert.notOk(listener.handlerCalled, 'The TypedListener.handleChangeEvent method should not be called after the listener was destroyed');
         });
  
         tdd.test('destroy - without initialization', function() {
           var listener = new ValidExtender();
           assert.notOk(listener.__subHandle, 'The TypedListener was not initialized and should not have a subscription handle');
  
           listener.destroy();
           // Pass, make sure nothing goes boom!
         });
      });
    }
});
