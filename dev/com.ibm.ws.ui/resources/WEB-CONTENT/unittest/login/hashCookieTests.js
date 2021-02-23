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
/**
 * The suite of unit tests for the 'login' module.
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "dojo/cookie",
     "dojo/hash",
     "dojo/topic",
     "login/hashCookie"
       ],
       
   function (tdd, assert, cookie, hash, topic, hashCookie) {

    with(assert) {
      
      /**
       * Defines the 'login' module test suite.
       */
      tdd.suite('Login HashCookie Tests', function() {
        
            tdd.beforeEach(function() {
              hash('');
            });
            
            tdd.afterEach(function() {
              hash('');
              hashCookie.stopCapturing();
            });

           tdd.test('Comprehensive tests: captureHashCookie, getHashCookieValue, clearHashCookie, stopCapturing', function() {
             hash('captureHashCookie.testhash.1');
      
             hashCookie.captureHashCookie();
             assert.equal(cookie('adminCenterHash'), 'captureHashCookie.testhash.1', 'The cookie did not get created with the correct value');
      
             // Call capture again, this ensures we're able to be called multiple times and the code is safe-guarded.
             hashCookie.captureHashCookie();
      
             // For now, don't try to test async setting of the cookie in unit tests. Its possible, but annoying.
             //  hash('captureHashCookie.testhash.2');
             //  assert.equal('captureHashCookie.testhash.2', cookie('adminCenterHash'), 'The cookie did not get updated with the correct value');
      
             hash(''); // Blank out the hash
      
             hashCookie.restoreHashFromCookie();
             assert.equal(hash(), 'captureHashCookie.testhash.1', 'The hash was not restored from the cookie');
             // cookie('adminCenterHash') returned undefined in DOH test 
             assert.isUndefined(cookie('adminCenterHash'), 'The cookie did not get cleared');
      
             hashCookie.stopCapturing();
      
             hash('captureHashCookie.testhash.3');
             assert.isUndefined(cookie('adminCenterHash'), 'The cookie listener did not get removed. A new cookie value was set');
      
             // Invoke stopCapturing again to ensure it can be called twice safely.
             hashCookie.stopCapturing();
      
             // Try restoring again
             hashCookie.restoreHashFromCookie();
             assert.equal(hash(), 'captureHashCookie.testhash.3', 'The hash should not be changed as there was nothing to restore from');
           });
      
       });
    }
});