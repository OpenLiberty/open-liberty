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
define(['intern!tdd', 'intern/chai!assert', 'js/widgets/LibertyHeader'],
    function(tdd, assert, LibertyHeader) {
  
    var header;

    with(assert) {
      /*
       * This testrun will cause the following stack to be outputted to error tab of console:
       * 
         TypeError: Cannot read property 'appendChild' of undefined
          at oneuiRoot.Header.declare._injectTemplate  <devAdminCenter/idx/app/Header.js:653:17>
          at <devAdminCenter/idx/app/Header.js:790:9>
          at execModule  <devAdminCenter/intern/node_modules/dojo/dojo.js:515:54>
          at <devAdminCenter/intern/node_modules/dojo/dojo.js:582:7>
          at guardCheckComplete  <devAdminCenter/intern/node_modules/dojo/dojo.js:566:4>
          at checkComplete  <devAdminCenter/intern/node_modules/dojo/dojo.js:574:27>
          at onLoadCallback  <devAdminCenter/intern/node_modules/dojo/dojo.js:656:7>
          
       * Ignore it. It does not cause test failures.
       */
      
      /**
       * Defines the 'toolbox' module test suite.
       */
      tdd.suite('LibertyHeader Tests', function() { 
    
           tdd.beforeEach(function() {
             header = new LibertyHeader();
           });
           
           tdd.afterEach(function() {
             header.destroy();
           });
      
           tdd.test('Help Menu Item - launches InfoCenter', function() {
             var mockTab = {
                 focus: function() {
                   this.__focused = true;
                 }
             };
             var mockWindow = {
                 open: function(url, target) {
                   this.__openedUrl = url;
                   this.__openedTarget = target;
                   return mockTab;
                 }
             };
             
             var helpMenuItem = header.__createHelpMenuItem(mockWindow);
             
             assert.equal(helpMenuItem.id, 'helpMenuItem', 'Help menu item created with wrong id');
             assert.equal(helpMenuItem.label, 'Help', 'Help menu item created with wrong label');
             
             helpMenuItem.onClick();
             
             // Hard-code the URL here to indepdently verify the target is correct. If this ever changed we would
             // have to update this test.
             assert.equal('http://www14.software.ibm.com/webapp/wsbroker/redirect?version=cord&product=was-nd-mp&topic=twlp_ui',
                 mockWindow.__openedUrl, 'The mock window did not get opened with the right URL');
             assert.equal(mockWindow.__openedTarget, '_blank', 'The mock window did not get opened with the right target');
             assert.isTrue(mockTab.__focused, 'The mock tab did not get a focus event');
             
           });
    
           tdd.test('Help Menu Item for tool - launches InfoCenter', function() {
             // Same as the above, only difference is in the created ID - don't repeat the same tests
             var mockWindow = {};
             
             var helpMenuItem = header.__createHelpMenuItem(mockWindow, 'MyToolId');
             assert.equal(helpMenuItem.id, 'helpMenuItemMyToolId', 'Help menu item for a tool view created with wrong id');
           });
      });
    }
});
