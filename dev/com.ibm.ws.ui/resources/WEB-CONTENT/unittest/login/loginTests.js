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
 * The suite of unit tests for the 'login' module.
 */
define(["intern!tdd", "intern/chai!assert", "login/login"], function(tdd, assert,login) {

  with(assert) {
    
    /**
     * Defines the 'login' module test suite.
     */
    tdd.suite('Login Tests', function() {
        
         tdd.test('setLoginButtonText - ensure innerHTML is set', function() {
           // Set up mocks
           var loginButton = {
               innerHTML: 'notSet'
           };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'loginButton') {
                   return loginButton;
                 } else {
                   return null;
                 }
               }
           };
  
           // Drive product code and validate
           login.__setLoginButtonText(mockDOM);
           assert.equal(loginButton.innerHTML, 'Submit', 'loginButton innerHTML was not set correctly');
         }),
  
         tdd.test('setTitleText - ensure innerHTML is set', function() {
           // Set up mocks
           var loginTitle = {
               innerHTML: 'notSet'
           };
           var loginTabTitle = {
               innerHTML: 'notSet'
           };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'loginTitle') {
                   return loginTitle;
                 } else if (id === 'loginTabTitle') {
                   return loginTabTitle;
                 } else {
                   return null;
                 }
               }
           };
  
           // Drive product code and validate
           login.__setTitleText(mockDOM);
           assert.equal(loginTitle.innerHTML, 'Liberty Admin Center', 'loginTitle innerHTML was not set correctly');
           assert.equal(loginTabTitle.innerHTML, 'Liberty Admin Center', 'loginTabTitle innerHTML was not set correctly');
         }),
  
         tdd.test('setPlaceholders', function() {
           // Set up mocks
           var jUser = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var jPwd = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var usernameLabel = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var passwordLabel = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'j_username') {
                   return jUser;
                 } else if (id === 'j_password') {
                   return jPwd;
                 } else if (id === 'usernameLabel') {
                   return usernameLabel;
                 } else if (id === 'passwordLabel') {
                   return passwordLabel;
                 } else {
                   return null;
                 }
               }
           };
           login.__setPlaceholders(mockDOM);
           assert.equal(jUser.placeholder, 'User Name');
           assert.equal(jUser.key, 'aria-label');
           assert.equal(jUser.value, 'User Name');
           assert.equal(jPwd.placeholder, 'Password');
           assert.equal(jPwd.key, 'aria-label');
           assert.equal(jPwd.value, 'Password');
         }),
  
         tdd.test('setErrorPathMessage - login.jsp', function() {
           // Nothing is set, no mock needed
           var mockDOM = null;
           login.__setErrorPathMessage(mockDOM, 'devAdminCenter/login.jsp', '');    
         }),
  
         tdd.test('setErrorPathMessage - login.jsp?ignored', function() {
           // Nothing is set, no mock needed
           var mockDOM = null;
           login.__setErrorPathMessage(mockDOM, 'devAdminCenter/login.jsp', '?ignored');
         }),
  
         tdd.test('setErrorPathMessage - login.jsp?login-error', function() {
           // Set up mocks
           var loginFooter = {
               innerHTML: 'notSet'
           };
           var jUser = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var jPwd = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'login-footer') {
                   return loginFooter;
                 } else if (id === 'j_username') {
                   return jUser;
                 } else if (id === 'j_password') {
                   return jPwd;
                 } else {
                   return null;
                 }
               }
           };
           login.__setErrorPathMessage(mockDOM, 'devAdminCenter/login.jsp', '?login-error');
           assert.isTrue(loginFooter.innerHTML.search('Login error') >= 0, 'loginFooter did not contain "Login error". loginFooter.innerHTML=' + loginFooter.innerHTML);
           assert.equal(jUser.key, 'aria-describedby');
           assert.equal(jUser.value, 'login-footer');
           assert.equal(jPwd.key, 'aria-describedby');
           assert.equal(jPwd.value, 'login-footer');
         }),
  
         tdd.test('setErrorPathMessage - login.jsp?user-unauthorized', function() {
           // Set up mocks
           var loginFooter = {
               innerHTML: 'notSet'
           };
           var jUser = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var jPwd = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'login-footer') {
                   return loginFooter;
                 } else if (id === 'j_username') {
                   return jUser;
                 } else if (id === 'j_password') {
                   return jPwd;
                 } else {
                   return null;
                 }
               }
           };
           login.__setErrorPathMessage(mockDOM, 'devAdminCenter/login.jsp', '?user-unauthorized');
           assert.isTrue(loginFooter.innerHTML.search('Access error') >= 0, 'loginFooter did not contain "Access error". loginFooter.innerHTML=' + loginFooter.innerHTML);
           assert.equal(jUser.key, 'aria-describedby');
           assert.equal(jUser.value, 'login-footer');
           assert.equal(jPwd.key, 'aria-describedby');
           assert.equal(jPwd.value, 'login-footer');
         }),
  
         tdd.test('setErrorPathMessage - other path', function() {
           // Set up mocks
           var loginFooter = {
               innerHTML: 'notSet'
           };
           var jUser = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var jPwd = {
               setAttribute: function(key,value) {
                 this.key = key;
                 this.value = value;
               }
           };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'login-footer') {
                   return loginFooter;
                 } else if (id === 'j_username') {
                   return jUser;
                 } else if (id === 'j_password') {
                   return jPwd;
                 } else {
                   return null;
                 }
               }
           };
           login.__setErrorPathMessage(mockDOM, 'devAdminCenter', '');
           assert.isTrue(loginFooter.innerHTML.search('Access error') >= 0, 'loginFooter did not contain "Access error". loginFooter.innerHTML=' + loginFooter.innerHTML);
           assert.equal(jUser.key, 'aria-describedby');
           assert.equal(jUser.value, 'login-footer');
           assert.equal(jPwd.key, 'aria-describedby');
           assert.equal(jPwd.value, 'login-footer');
         }),
  
         tdd.test('updateLoginCSSProperties - incomplete data', function() {
           // Call updateLoginCSSProperties with incomplete data. Nothing should happen.
           login.__updateLoginCSSProperties();
           login.__updateLoginCSSProperties({});
           login.__updateLoginCSSProperties({}, 1);
         }),
  
         tdd.test('updateLoginCSSProperties - desktop mode', function() {
           // Set up mocks
           var loginDiv = {};
           var mockDOM = {
               byId: function(id) {
                 if (id === 'login') {
                   return loginDiv;
                 } else {
                   return null;
                 }
               }
           };
  
           login.__updateLoginCSSProperties(mockDOM, 1920, 1080);
           assert.equal(loginDiv.className, '');
         }),
  
         tdd.test('updateLoginCSSProperties - phone mode (iPhone 5)', function() {
           // Set up mocks
           var loginDiv = {};
           var mockDOM = {
               byId: function(id) {
                 if (id === 'login') {
                   return loginDiv;
                 } else {
                   return null;
                 }
               }
           };
  
           login.__updateLoginCSSProperties(mockDOM, 320, 640);
           assert.equal(loginDiv.className, 'phone');
         }),
         
         tdd.test('establishLoginProcessing - sets correct events', function() {
           // Set up mocks
           var loginButton = { onclick: 'notset' };
           var j_username = { onkeyup: 'notset' };
           var j_password = { onkeyup: 'notset' };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'loginButton') {
                   return loginButton;
                 } else if (id === 'j_username') {
                   return j_username;
                 } else if (id === 'j_password') {
                   return j_password;
                 } else {
                   return null;
                 }
               }
           };
           login.__establishLoginProcessing(mockDOM);
           assert.isFalse(loginButton.onclick ===' notset', "The login button's onclick was not set");
           assert.equal(loginButton.type, 'button', "The login button's type did not get set to submit");
           assert.isFalse(j_username.onkeyup ===' notset', 'The j_username onkeyup event was not set');
           assert.isFalse(j_password.onkeyup ===' notset', 'The j_password onkeyup event was not set');
         }),
         
         tdd.test('performLogin - name not set', function() {
           // Set up mocks
           var mockXHR = { invoked: false, post: function() { this.invoked = true; } };
           var j_username = { };
           var j_password = { };
           var hiddenLoginFormSubmit = { disabled: true, wasClicked: false, click: function() { this.wasClicked = true; } };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'j_username') {
                   return j_username;
                 } else if (id === 'j_password') {
                   return j_password;
                 } else if (id === 'hiddenLoginFormSubmit') {
                   return hiddenLoginFormSubmit;
                 } else {
                   return null;
                 }
               }
           };
           login.__performLogin(mockDOM, mockXHR, false);
           assert.isFalse(mockXHR.invoked, 'XHR should not have been invoked for a logout');
           assert.isTrue(hiddenLoginFormSubmit.wasClicked, 'The hiddenLoginFormSubmit was not clicked even though the user name was not filled in');
           assert.isTrue(hiddenLoginFormSubmit.disabled, 'The hiddenLoginFormSubmit was not returned to disabled at the end of the method');
         }),
         
         tdd.test('performLogin - password not set', function() {
           // Set up mocks
           var mockXHR = { invoked: false, post: function() { this.invoked = true; } };
           var j_username = { value: 'admin' };
           var j_password = { };
           var hiddenLoginFormSubmit = { disabled: true, wasClicked: false, click: function() { this.wasClicked = true; } };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'j_username') {
                   return j_username;
                 } else if (id === 'j_password') {
                   return j_password;
                 } else if (id === 'hiddenLoginFormSubmit') {
                   return hiddenLoginFormSubmit;
                 } else {
                   return null;
                 }
               }
           };
           login.__performLogin(mockDOM, mockXHR, false);
           assert.isFalse(mockXHR.invoked, 'XHR should not have been invoked for a logout');
           assert.isTrue(hiddenLoginFormSubmit.wasClicked, 'The hiddenLoginFormSubmit was not clicked even though the user name was not filled in');
           assert.isTrue(hiddenLoginFormSubmit.disabled, 'The hiddenLoginFormSubmit was not returned to disabled at the end of the method');
         }),
         
         tdd.test('performLogin - simple login', function() {
           // Set up mocks
           var mockXHR = { invoked: false, post: function() { this.invoked = true; } };
           var j_username = { value: 'admin' };
           var j_password = { value: 'adminpwd' };
           var hiddenLoginFormSubmit = { disabled: true, wasClicked: false, click: function() { this.wasClicked = true; } };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'j_username') {
                   return j_username;
                 } else if (id === 'j_password') {
                   return j_password;
                 } else if (id === 'hiddenLoginFormSubmit') {
                   return hiddenLoginFormSubmit;
                 } else {
                   return null;
                 }
               }
           };
           login.__performLogin(mockDOM, mockXHR, false);
           assert.isFalse(mockXHR.invoked, 'XHR should not have been invoked for a logout');
           assert.isTrue(hiddenLoginFormSubmit.wasClicked, 'The hiddenLoginFormSubmit was not clicked even though the user name was not filled in');
           assert.isTrue(hiddenLoginFormSubmit.disabled, 'The hiddenLoginFormSubmit was not returned to disabled at the end of the method');
         }),
         
         tdd.test('performLogin - need to logout', function() {
           // Set up mocks
           var mockXHR = { invoked: false, post: function() { this.invoked = true; } };
           var j_username = { value: 'admin' };
           var j_password = { value: 'adminpwd' };
           var hiddenLoginFormSubmit = { disabled: true, wasClicked: false, click: function() { this.wasClicked = true; } };
           var mockDOM = {
               byId: function(id) {
                 if (id === 'j_username') {
                   return j_username;
                 } else if (id === 'j_password') {
                   return j_password;
                 } else if (id === 'hiddenLoginFormSubmit') {
                   return hiddenLoginFormSubmit;
                 } else {
                   return null;
                 }
               }
           };
           login.__performLogin(mockDOM, mockXHR, true);
           assert.isTrue(mockXHR.invoked, 'XHR should have been invoked to do a logout');
           assert.isTrue(hiddenLoginFormSubmit.wasClicked, 'The hiddenLoginFormSubmit was not clicked even though the user name was not filled in');
           assert.isTrue(hiddenLoginFormSubmit.disabled, 'The hiddenLoginFormSubmit was not returned to disabled at the end of the method');
         });
  
    });
  }
});