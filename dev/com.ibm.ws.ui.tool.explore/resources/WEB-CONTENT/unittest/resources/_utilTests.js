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
define([
        'intern!tdd',
        'intern/chai!assert',
        'resources/_util'
        ],

        function (tdd, assert, util) {

  with(assert) {

    /**
     * Defines the 'Topic Util Tests' module test suite.
     */
    tdd.suite("Topic Util Tests", function() {

      var mockServer = sinon.fakeServer.create();

      tdd.beforeEach(function() {
        mockServer = sinon.fakeServer.create();
      });

      tdd.afterEach(function() {
        mockServer.restore();
      });

      /**
       * Drive doPopulate and simulate the happy path.
       */
      tdd.test("doPopulate - simple success case", function() {
        var dfd = this.async(1000);

        var objToUpdate = { testField : true };
        var deferred = util.doPopulate('/populate/url', objToUpdate, function() { return 'result'; });
        deferred.then(function(result) {
          if (result.populating) {
            dfd.reject('The result object of doPopulate was still set as populating');
            return;
          }
          if (!result.populated) {
            dfd.reject('The result object of doPopulate was not set as populated');
            return;
          }
          if (!result.testField) {
            dfd.reject('The result object of doPopulate did not have the initial testField set as true');
            return;
          }
          dfd.resolve('The simple populate flow completed successfully');
        });

        // Simulate the REST API response (note the use of regex, not a string)
        mockServer.respondWith('GET', /\/populate\/url.*/, [200, { 'Content-Type': 'application/json' }, '{}']);
        mockServer.respond();

        return dfd;
      });

      /**
       * Drive the doPopulate method for an object which is already populated.
       * It should return immediately and not hit the server.
       */
      tdd.test("doPopulate - already populated case", function() {
        var dfd = this.async(1000);

        var objToUpdate = { testField : true, populated: true };
        var deferred = util.doPopulate('/populate/url', objToUpdate, function() { return 'result'; });
        deferred.then(function(result) {
          if (result.populating) {
            dfd.reject('The result object of doPopulate was still set as populating');
            return;
          }
          if (!result.populated) {
            dfd.reject('The result object of doPopulate was not set as populated');
            return;
          }
          if (!result.testField) {
            dfd.reject('The result object of doPopulate did not have the initial testField set as true');
            return;
          }
          dfd.resolve('The simple populate flow completed successfully');
        });

        return dfd;
      });

      /**
       * Drive the doPopulate method for an object which is currently populating.
       * It should return immediately with the active Deferred object.
       */
      tdd.test("doPopulate - currently populating case", function() {
        var objToUpdate = { testField : true, populating: 'populating object' };
        var deferred = util.doPopulate('/populate/url', objToUpdate, function() { return 'result'; });
        assert.equal(deferred, 'populating object', 'The object was populating, and should have returned the Deferred which is doing the populate (in this case a mock)');
      });

      /**
       * Drive the doPopulate method where the updateFn throws an exception. The Deferred should be rejected
       * with the thrown exception object.
       */
      tdd.test("doPopulate - update function throws exception error case", function() {
        var dfd = this.async(1000);

        var objToUpdate = { testField : true };
        var deferred = util.doPopulate('/populate/url', objToUpdate, function() { throw 'Test Exception'; });
        deferred.then(function(result) {
          dfd.reject('The doPopulate Deferred should be rejected because the updateFn threw an exception');
        }, function(err) {
          if (err === 'Test Exception') {
            dfd.resolve('The doPopulate Deferred was correctly rejected with the thrown object when the updateFn threw an exception');
          }
          dfd.reject('The doPopulate Deferred was correctly rejected but with the wrong err payload');
        });

        // Simulate the REST API response (note the use of regex, not a string)
        mockServer.respondWith('GET', /\/populate\/url.*/, [200, { 'Content-Type': 'application/json' }, '{}']);
        mockServer.respond();

        return dfd;
      });

    });
  }
});