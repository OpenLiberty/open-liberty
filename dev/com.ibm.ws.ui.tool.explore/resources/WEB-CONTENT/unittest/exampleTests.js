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
 * Example unittest for trying out Intern 
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "resources/hashUtils"
       ],
       
   function (tdd, assert, hashUtils) {
     
      with(assert) {
   
          tdd.suite("Example test to try out Intern!", function() {
   
              tdd.test("Empty test", function() {
                console.log("In example Intern unittest!");
                assert.isTrue(true, "Assertion passes :)");
                var str = "hello world!";
                // With Chai assert.equal(), please make sure to specify
                // actual result first, then expected result
                assert.equal((typeof str === "string"), true, "Expected a true value");
              });
              
          });
      }
  });