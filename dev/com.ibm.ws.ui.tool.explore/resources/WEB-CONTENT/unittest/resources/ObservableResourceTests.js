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
 * Fake unittest for trying out Intern 
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "intern/dojo/declare",
     "resources/ObservableResource",
     "resources/Observer"
       ],
       
   function (tdd, assert, declare, ObservableResource, Observer) {

      /**
       * A valid initialization object for an ObservableResource.
       */
      var baseResource = {
          id: 'localhost,/wlp/usr,server1',
          type: 'server',
          _handleChangeEvent: function() {/*no-op*/}
      };
    
      /**
       * A valid ObservableResource implementation with a hard-coded ID.
       */
      var ObservableWithID = declare([ObservableResource], {
        id: 'hardcoded',
        type: 'ignored',
        _handleChangeEvent: function() { /*no-op */ }
      });
    
      /**
       * A valid Observer implementation with a hard-coded ID.
       */
      var ObserverWithID = declare([Observer], {
        id: 'hardcoded'
      });
    
      /**
       * A basic implementation of an ObservableResource to simulate a real object.
       */
      var ObservableServer = declare([ObservableResource], {
        type: 'server',
        state: 'STARTED',
        cluster: 'cluster1',
        _handleChangeEvent: function() { /*no-op */ }
      });
    
      /**
       * A basic implementation of an Observer observing the 'state' attribute.
       */
      var StateObserver = declare([Observer], {
        newState: null,
        oldState: null,
        unobservedMulti: null,
    
        onStateChange: function(newState, oldState) {
          this.newState = newState;
          this.oldState = oldState;
        }
      });
    
      /**
       * A basic implementation of an Observer observing the 'state' and 'multi' attributes.
       * The state attribute is a single value attribute, where the multi attribute is a list
       * attribute.
       */
      var StateAndListObserver = declare([Observer], {
        newState: null,
        oldState: null,
        newMulti: null,
        oldMulti: null,
        addedMulti: null,
        removedMulti: null,
    
        onStateChange: function(newState, oldState) {
          this.newState = newState;
          this.oldState = oldState;
        },
    
        onMultiChange: function(newMulti, oldMulti, addedMulti, removedMulti) {
          this.newMulti = newMulti;
          this.oldMulti = oldMulti;
          this.addedMulti = addedMulti;
          this.removedMulti = removedMulti;
        }
      });
  
      with(assert) {
   
        /**
         * Defines the 'ObservableResource' module test suite.
         */
        tdd.suite('ObservableResource/Observer pattern Tests', function() {
   
            tdd.test('Observer - construct with no initialization object', function() {
              try {
                new Observer();
                assert.ok(false, 'Observer was successfully created when it should have failed - an initialization object is required');
              } catch(err) {
                // Pass
                assert.equal(err, 'Observer created without an initialization object', 'Observer creation error did not throw the expected error');
              }
            }),
  
            tdd.test('Observer - construct with no provided ID', function() {
              try {
                new Observer({});
                assert.ok(false, 'Observer was successfully created when it should have failed - an id is required');
              } catch(err) {
                // Pass
                assert.equal(err, 'Observer created without an id', 'Observer creation error did not throw the expected error');
              }
            }),
  
            tdd.test('Observer - construct with hardcoded ID', function() {
              var observer = new ObserverWithID();
              assert.instanceOf(observer, Observer, 'Unable to construct a valid Observer');
              assert.equal(observer.id, 'hardcoded', 'The Observer was created with the wrong value for "id"');
            }),
  
            tdd.test('Observer - construct with parameterized ID', function() {
              var initObj = { id: 'card-server-localhost,/wlp/usr,server1' };
              var observer = new Observer(initObj);
  
              assert.instanceOf(observer, Observer, 'Unable to construct a valid Observer');
              assert.equal(observer.id, initObj.id, 'Observer was not constructed with correct value for "id"');
            }),
  
            tdd.test('ObservableResource - construct with no provided ID', function() {
              try {
                new ObservableResource({type:'abc', _handleChangeEvent: function() { /*no-op */ }});
                assert.ok(false, 'ObservableResource was successfully created when it should have failed - an id is required');
              } catch(err) {
                // Pass
                assert.equal(err, 'ObservableResource created without an id', 'ObservableResource creation error did not throw the expected error');
              }
            }),
  
            tdd.test('ObservableResource - construct with hardcoded ID', function() {
              var observable = new ObservableWithID();
              assert.instanceOf(observable, ObservableResource, 'Unable to construct a valid ObservableResource');
              assert.equal(observable.id, 'hardcoded', 'The ObservableResource was created with the wrong value for "id"');
            }),
  
            tdd.test('ObservableResource - construct with parameterized ID', function() {
              var observable = new ObservableResource(baseResource);
  
              assert.instanceOf(observable, ObservableResource, 'Unable to construct a valid ObservableResource');
              assert.equal(observable.id, 'localhost,/wlp/usr,server1', 'The ObservableResource was created with the wrong value for "id"');
            }),
  
            /**
             * This complex test scenario tests that each ObservablResource has its own independent list of Observers,
             * and that the various Observers are called correctly.
             */
            tdd.test('ObservableResource - subscribe and notify observers', function() {
              // Create the initial ObservableResources, ensure they have no observers
              var observable1 = new ObservableServer({id: 'localhost,/wlp/usr,server1'});
              assert.equal(observable1.__observers.length, 0, 'Observable 1 list of Observers should be empty');
  
              var observable2 = new ObservableServer({id: 'localhost,/wlp/usr,server2'});
              assert.equal(observable2.__observers.length, 0, 'Observable 2 list of Observers should be empty');
  
              // Create the initial Observers, ensure their initial states are blank
              var observer1a = new StateObserver({ id: 'card-server-localhost,/wlp/usr,server1' });
              assert.equal(observer1a.newState,        null,  'Observer 1a was not constructed with a null value for "newState"');
              assert.equal(observer1a.oldState,        null,  'Observer 1a was not constructed with a null value for "oldState"');
              assert.equal(observer1a.unobservedMulti, null,  'Observer 1b was not constructed with a null value for "unobservedMulti"');
  
              var observer1b = new StateAndListObserver({ id: 'objectView-server-localhost,/wlp/usr,server1' });
              assert.equal(observer1b.newState,        null,  'Observer 1b was not constructed with a null value for "newState"');
              assert.equal(observer1b.oldState,        null,  'Observer 1b was not constructed with a null value for "oldState"');
              assert.equal(observer1b.newMulti,        null,  'Observer 1b was not constructed with a null value for "newMulti"');
              assert.equal(observer1b.oldMulti,        null,  'Observer 1b was not constructed with a null value for "oldMulti"');
              assert.equal(observer1b.addedMulti,      null,  'Observer 1b was not constructed with a null value for "addedMulti"');
              assert.equal(observer1b.removedMulti,    null,  'Observer 1b was not constructed with a null value for "removedMulti"');
  
              var observer2 = new StateObserver({ id: 'objectView-server-localhost,/wlp/usr,server2' });
              assert.equal(observer2.newState,         null,  'Observer 2 was not constructed with a null value for "newState"');
              assert.equal(observer2.oldState,         null,  'Observer 2 was not constructed with a null value for "oldState"');
              assert.equal(observer2.unobservedMulti,  null,  'Observer 2 was not constructed with a null value for "unobservedMulti"');
  
              // Fire the _notifyObservers method and make sure the observers don't somehow change
              observable1._notifyObservers('onStateChange', ['STARTED', 'STOPPED']);
              observable1._notifyObservers('onMultiChange', ['multi2', 'multi1']);
              observable2._notifyObservers('onStateChange', ['STARTED', 'STOPPED']);
  
              // Sanity check to ensure that the Observers were not affected by the previous _notifyObservers calls
              assert.equal(observer1a.newState,        null,  'Observer 1a should not have detected a changed value for "newState"');
              assert.equal(observer1a.oldState,        null,  'Observer 1a should not have detected a changed value for "oldState"');
              assert.equal(observer1a.unobservedMulti, null,  'Observer 1b should not have detected a changed value for "unobservedMulti"');
              assert.equal(observer1b.newState,        null,  'Observer 1b should not have detected a changed value for "newState"');
              assert.equal(observer1b.oldState,        null,  'Observer 1b should not have detected a changed value for "oldState"');
              assert.equal(observer1b.newMulti,        null,  'Observer 1b should not have detected a changed value for "newMulti"');
              assert.equal(observer1b.oldMulti,        null,  'Observer 1b should not have detected a changed value for "oldMulti"');
              assert.equal(observer1b.addedMulti,      null,  'Observer 1b should not have detected a changed value for "addedMulti"');
              assert.equal(observer1b.removedMulti,    null,  'Observer 1b should not have detected a changed value for "removedMulti"');
              assert.equal(observer2.newState,         null,  'Observer 2 should not have detected a changed value for "newState"');
              assert.equal(observer2.oldState,         null,  'Observer 2 should not have detected a changed value for "oldState"');
              assert.equal(observer2.unobservedMulti,  null,  'Observer 2 should not have detected a changed value for "unobservedMulti"');
  
              // Subscribe the Observers for ObservableResource 1
              observable1.subscribe(observer1a);
              observable1.subscribe(observer1b);
              assert.equal(observable1.__observers.length, 2, 'Observable 1 list of Observers should be length 2'); 
  
              // Notify observers and check value change for only Observers of observable1
              observable1._notifyObservers('onStateChange', ['STOPPED', 'STARTED']);
              assert.equal(observer1a.newState,   'STOPPED',  'Observer 1a did not have its new state value change in response to a notification');
              assert.equal(observer1a.oldState,   'STARTED',  'Observer 1a did not have its old state value change in response to a notification');
              assert.equal(observer1b.newState,   'STOPPED',  'Observer 1b did not have its new state value change in response to a notification');
              assert.equal(observer1b.oldState,   'STARTED',  'Observer 1b did not have its old state value change in response to a notification');
              assert.equal(observer2.newState,    null,       'Observer 2 should not have detected a changed value for "newState"');
              assert.equal(observer2.oldState,    null,       'Observer 2 should not have detected a changed value for "oldState"');
  
              observable1._notifyObservers('onMultiChange', ['multi2', 'multi1', 1, 2]);
              assert.equal(observer1a.unobservedMulti, null,  'Observer 1a had its unobserved multi value changed when it should not have - it does not provide an onMultiChange method');
              assert.equal(observer1b.newMulti,   'multi2',   'Observer 1b did not have its observed new multi value change in response to a notification');
              assert.equal(observer1b.oldMulti,   'multi1',   'Observer 1b did not have its observed old multi value change in response to a notification');
              assert.equal(observer1b.addedMulti, 1,          'Observer 1b did not have its observed added multi value change in response to a notification');
              assert.equal(observer1b.removedMulti, 2,        'Observer 1b did not have its observed removed multi value change in response to a notification');
              assert.equal(observer2.unobservedMulti, null,   'Observer 2 should not have detected a changed value for "unobservedMulti"');
  
              // Subscribe the observer for Observer 2 and ensure the observers lists are independent
              observable2.subscribe(observer2);
              assert.equal(observable1.__observers.length, 2, 'Observable 1 list of observers should be length 2');
              assert.equal(observable2.__observers.length, 1, 'Observable 2 list of observers should be length 1');
  
              // Notify for Observer 2 and ensure observers for 1 are not affected 
              observable2._notifyObservers('onStateChange', ['UNKNOWN', 'STARTED']);
              assert.equal(observer2.newState,   'UNKNOWN', 'Observer 1b did not have its observed state value did not change in response to a notification');
              assert.equal(observer2.oldState,   'STARTED', 'Observer 1b did not have its observed state value did not change in response to a notification');
  
              // Check to ensure observer1a and observer1b are not affected
              assert.equal(observer1a.newState,  'STOPPED', 'Observer 1a should not have its observed state value change in response to a notification to observable2');
              assert.equal(observer1b.newState,  'STOPPED', 'Observer 1b should not have its observed state value change in response to a notification to observable2');
  
              // Unsubscribe observer1b and ensure its state does not change after a new notification
              observable1.unsubscribe(observer1b);
              observable1.unsubscribe(observer1b); // Unsubscribe twice, should have no effect
              assert.equal(observable1.__observers.length, 1, 'Observable 1 list of observers should be length 1');
  
              // Notify observers and check value change
              observable1._notifyObservers('onStateChange', ['STARTED', 'STOPPED']);
              assert.equal(observer1a.newState, 'STARTED', 'Observer 1a did not have its observed state value change in response to a notification');
              assert.equal(observer1b.newState, 'STOPPED', 'Observer 1b observed state was changed but it was unsubscribed so it should not have been affected');
  
              // Clean up the remaining observers
              observable1.unsubscribe(observer1a);
              observable2.unsubscribe(observer2);
  
              assert.equal(observable1.__observers.length, 0, 'Observable 1 list of observers should be empty after all observers are removed');
              assert.equal(observable2.__observers.length, 0, 'Observable 2 list of observers should be empty after all observers are removed');
            }),
  
            /**
             * When _updateAttribute is called for an attribute which is not present in the Event object,
             * no changes to the ObservableResource should be made, and no notifications to Observers should
             * happen.
             */
            tdd.test('ObservableResource - _updateAttribute should make no changes or notification when the event is empty', function() {
              var observable = new ObservableResource(baseResource);
              var observer = new ObserverWithID(baseResource);
  
              // Initialize the ObservableResource attribute 'attr', it should not change
              observable.attr = 1;
  
              // Initialize the Observer setNotified method to confirm the observer was never called
              observer.notified = false;
              observer.setNotified = function() {
                this.notified = true;
              };
  
              // Subscribe the observer
              observable.subscribe(observer);
  
              // Drive the method for an attribute not present in the event
              observable._updateAttribute('setNotified', {}, 'attr');
              assert.notOk(observer.notified,  'The Observer should not be notified when the attribute is not present in the event object');
              assert.equal(observable.attr, 1, 'The attribute of the ObservableResource was changed when it should not be as it was not present in the Event object');
            }),
  
            /**
             * When _updateAttribute is called for an attribute which is present in the Event object,
             * then the attribute on the ObservableResource should change and the Observers should be notified.
             */
            tdd.test('ObservableResource - _updateAttribute sets change and sends notification', function() {
              var observable = new ObservableResource(baseResource);
              var observer = new ObserverWithID(baseResource);
  
              // Initialize the ObservableResource attribute 'attr', it should get changed
              observable.attr = 1;
  
              // Initialize the Observer setNotified method to confirm the observer was called
              observer.newValue = null;
              observer.oldValue = null;
              observer.setNotified = function(newValue, oldValue) {
                this.newValue = newValue;
                this.oldValue = oldValue;
              };
  
              // Subscribe the observer
              observable.subscribe(observer);
  
              // Drive the method with no attribute for the event
              observable._updateAttribute('setNotified', {attr: 2}, 'attr');
              assert.equal(observable.attr,   2, 'The attribute of the ObservableResource was not changed when it should have been');
              assert.equal(observer.newValue, 2, 'The observer did not have the correct NEW value passed into the observer notify method');
              assert.equal(observer.oldValue, 1, 'The observer did not have the correct OLD value passed into the observer notify method');
            }),
  
            /**
             * Tests that _updateTally will only accept values from the Event object which are specifically
             * listed in the set of tally attributes to process.
             */
            tdd.test('ObservableResource - _updateTally sets changes and sends notification', function() {
              var observable = new ObservableResource(baseResource);
              var observer = new ObserverWithID(baseResource);
  
              // Initialize the ObservableResource attribute with a set of tallies that should get changed
              observable.tally1 = 1;
              observable.tally2 = 2;
              observable.tally3 = 3;
              observable.tally4 = 4;
  
              // Initialize the Observer setNotified method to confirm the observer was called
              observer.newTally = null;
              observer.oldTally = null;
              observer.setNotified = function(newTally, oldTally) {
                this.newTally = newTally;
                this.oldTally = oldTally;
              };
  
              // Subscribe the observer
              observable.subscribe(observer);
  
              // Drive the method with no attribute for the event
              observable._updateTally('setNotified', {tally1: 'a', tally2: 'b', tally4: 'd'}, observable, ['tally1', 'tally2']);
  
              assert.equal(observable.tally1, 'a', 'Observable.tally1 was not changed when it should have been');
              assert.equal(observable.tally2, 'b', 'Observable.tally2 was not changed when it should have been');
              assert.equal(observable.tally3, 3,   'Observable.tally3 was changed and should not have been. Tally3 was not in the list of tallies to change');
              assert.equal(observable.tally4, 4,   'Observable.tally4 was changed and should not have been. Tally4 was in the event but was not in the list of tallies to change');
  
              assert.equal(observer.newTally.tally1, 'a', 'The observer did not have the correct NEW value for tally1 passed into the observer notify method');
              assert.equal(observer.newTally.tally2, 'b', 'The observer did not have the correct NEW value for tally2 passed into the observer notify method');
              assert.notOk(observer.newTally.tally3,      'The observer had a NEW value set for tally3. This should not be set as tally3 was not in the list of tallies to change');
              assert.notOk(observer.newTally.tally4,      'The observer had a NEW value set for tally4. This should not be set as tally4 was in the event but was not in the list of tallies to change');
  
              assert.equal(observer.oldTally.tally1, 1,   'The observer did not have the correct OLD value for tally1 passed into the observer notify method');
              assert.equal(observer.oldTally.tally2, 2,   'The observer did not have the correct OLD value for tally2 passed into the observer notify method');
              assert.notOk(observer.oldTally.tally3,      'The observer had a OLD value set for tally3. This should not be set as tally3 was not in the list of tallies to change');
              assert.notOk(observer.oldTally.tally4,      'The observer had a OLD value set for tally4. This should not be set as tally4 was in the event but was not in the list of tallies to change');
            }),
  
            /**
             * Test to confirm that when _updateArray is called with added and removed arrays that are undefined
             * or empty that no changes are made.
             */
            tdd.test('ObservableResource - _updateArray no changes or notification', function() {
              var observable = new ObservableResource(baseResource);
              var observer = new ObserverWithID(baseResource);
  
              // Initialize the Observer setNotified method to confirm the observer was not called
              observer.notified = false;
              observer.setNotified = function() {
                this.notified = true;
              };
  
              // Subscribe the observer
              observable.subscribe(observer);
  
              var emptyArray = [];
  
              // Drive the method with no added or removed
              observable._updateArray('setNotified', emptyArray, null, null);
              assert.notOk(observer.notified,    'The Observer should not be notified when the added and removed arrays are null');
              assert.equal(emptyArray.length, 0, 'The array length was changed, it should have remained empty when the added and removed arrays are null');
  
              observable._updateArray('setNotified', emptyArray, [], []);
              assert.notOk(observer.notified,    'The Observer should not be notified when the added and removed arrays are empty');
              assert.equal(emptyArray.length, 0, 'The array length was changed, it should have remained empty when the added and removed arrays are empty');
            }),
  
           /**
            * Test to confirm that when _updateArray is called with added, removed and changed arrays that are undefined
            * or empty that no changes are made.
            */
           tdd.test('ObservableResource - _updateArray no changes or notification', function() {
             var observable = new ObservableResource(baseResource);
             var observer = new ObserverWithID(baseResource);

             // Initialize the Observer setNotified method to confirm the observer was not called
             observer.notified = false;
             observer.setNotified = function() {
               this.notified = true;
             };

             // Subscribe the observer
             observable.subscribe(observer);

             var emptyArray = [];

             // Drive the method with no added or removed
             observable._updateArray('setNotified', emptyArray, null, null, null);
             assert.notOk(observer.notified,    'The Observer should not be notified when the added and removed arrays are null');
             assert.equal(emptyArray.length, 0, 'The array length was changed, it should have remained empty when the added and removed arrays are null');

             observable._updateArray('setNotified', emptyArray, [], [], []);
             assert.notOk(observer.notified,    'The Observer should not be notified when the added and removed arrays are empty');
             assert.equal(emptyArray.length, 0, 'The array length was changed, it should have remained empty when the added and removed arrays are empty');
           }),
           
            /**
             * Tests that _updateArray will add elements from the added array.
             */
            tdd.test('ObservableResource - _updateArray add new elements', function() {
              var observable = new ObservableResource(baseResource);
              var array = [];
  
              observable._updateArray('ignored', array, ['element1', 'element2'], []);
  
              assert.equal(array.length, 2,      'The array length was not changed, it should have two elements');
              assert.equal(array[0], 'element1', 'The element at array[0] was not correct');
              assert.equal(array[1], 'element2', 'The element at array[1] was not correct');
            }),
  
            /**
             * Tests that _updateArray will remove elements from the removed array.
             */
            tdd.test('ObservableResource - _updateArray remove elements', function() {
              var observable = new ObservableResource(baseResource);
              var array = ['element1', 'element2', 'element3'];
  
              observable._updateArray('ignored', array, [], ['element1', 'element3']);
  
              assert.equal(array.length, 1,      'The array length was not changed, it should have one element');
              assert.equal(array[0], 'element2', 'The element at array[0] was not correct');
            }),
  
            /**
             * Tests that _updateArray will add and remove elements and notify any Observers.
             */
            tdd.test('ObservableResource - _updateArray add and remove elements with an Observer', function() {
              var observable = new ObservableResource(baseResource);
              var observer = new ObserverWithID(baseResource);
  
              // Set up the setNotified method to confirm the observer was called with the right values
              observer.newList = null;
              observer.oldList = null;
              observer.added = null;
              observer.removed = null;
              observer.setNotified = function(newList, oldList, added, removed) {
                this.newList = newList;
                this.oldList = oldList;
                this.added = added;
                this.removed = removed;
              };
  
              // Subscribe the observer
              observable.subscribe(observer);
  
              var array = ['element1', 'element2', 'element3'];
              var added = ['element4'];
              var removed = ['element1', 'element3'];
  
              observable._updateArray('setNotified', array, added, removed);
  
              // Validate the array was modified as expected
              assert.equal(array.length, 2,      'The array length was not changed, it should have two elements');
              assert.equal(array[0], 'element2', 'The element at array[0] was not correct');
              assert.equal(array[1], 'element4', 'The element at array[1] was not correct');
  
              // Validate the Observer got the correct values passed in
              assert.equal(JSON.stringify(observer.newList), JSON.stringify(['element2', 'element4']));
              assert.equal(JSON.stringify(observer.oldList), JSON.stringify(['element1', 'element2', 'element3']));
              assert.equal(observer.added, added);
              assert.equal(observer.removed, removed);
            }),
            
            /**
             * Tests that when notifying all Observers, the list of Observers is not changed. This is important
             * because the list of Observers CAN change while sending the onChange notifications, if an Observer
             * decides that it no longer needs to observe the resource. There was a bug in the _notifyObservers
             * method where we did not keep a copy of the original list. As such, when an Observer in the list was
             * removed, it shortened the list and caused the next Observer to be skipped. This test is intended
             * to prevent a regression.
             */
            tdd.test('ObservableResource - direct test of _notifyObservers', function() {
              var observable = new ObservableResource(baseResource);
              var observer1 = new ObserverWithID(baseResource);
              var observer2 = new ObserverWithID(baseResource);
  
              // Initialize Observers to inidicate their method was called
              observer1.notified = false;
              observer1.setNotified = function() {
                this.notified = true;
              };
              
              observer2.notified = false;
              observer2.setNotified = function() {
                this.notified = true;
              };
  
              // Subscribe the observers
              observable.subscribe(observer1);
              observable.subscribe(observer2);
              
              // Notify the observers
              observable._notifyObservers('setNotified', ['ignored']);
              
              assert.ok(observer1.notified,  'Observer1 was not notified when it should have been');
              assert.ok(observer2.notified,  'Observer2 was not notified when it should have been');
            }),
  
            /**
             * Tests that when notifying all Observers, the list of Observers is not changed. This is important
             * because the list of Observers CAN change while sending the onChange notifications, if an Observer
             * decides that it no longer needs to observe the resource. There was a bug in the _notifyObservers
             * method where we did not keep a copy of the original list. As such, when an Observer in the list was
             * removed, it shortened the list and caused the next Observer to be skipped. This test is intended
             * to prevent a regression.
             */
            tdd.test('ObservableResource - _notifyObservers unsubscribe themselves', function() {
              var observable = new ObservableResource(baseResource);
              var observer1 = new ObserverWithID(baseResource);
              var observer2 = new ObserverWithID(baseResource);
  
              // Initialize Observer1 to unsubscribe itself when setNotified method is called
              observer1.notified = false;
              observer1.setNotified = function() {
                this.notified = true;
                observable.unsubscribe(this);
              };
              
              observer2.notified = false;
              observer2.setNotified = function() {
                this.notified = true;
              };
  
              // Subscribe the observers
              observable.subscribe(observer1);
              observable.subscribe(observer2);
              assert.equal(observable.__observers.length, 2, 'The initial list of subscribed Observers was not of size 2');
              
              // Drive a method for an attribute not present in the event
              observable._notifyObservers('setNotified', ['ignored']);
              
              assert.ok(observer1.notified,  'Observer1 was not notified when it should have been');
              assert.ok(observer2.notified,  'Observer2 was not notified when it should have been');
              assert.equal(observable.__observers.length, 1, 'The final list of subscribed Observers was not of size 1');
            });
         });
      }
  });