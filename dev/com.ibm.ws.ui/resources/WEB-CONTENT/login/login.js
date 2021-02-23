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
 * This JavaScript file contains all of the logic for rendering the login page.
 * 
 * A few things to know about the logic:
 * First, the formatting of the page is done using pure JS (no dojo or other libraries)
 * Second, assumptions are made by determining HOW we got to the login page.
 * 
 * Exactly how we arrived (our location URL) tells us WHY we arrived:
 * 1. If the location is .../login.jsp, then we're here to do a login
 * 2. If the location is .../login.jsp?login-error, then we're here because the login
 *    failed and we've been redirected back to handle the bad user name and password case.
 * 3. If the location is .../login.jsp?user-unauthorized, then we're here expressly to handle
 *    a user who is not authorized to access the Admin Center.
 * 4. If the location is ANYTHING ELSE, then we're here because of a 403. We need this
 *    final case because of how the webcontainer 'redirects' to us in the event of a 403.
 *    Rather than a real redirect, the login.jsp page contents are served up raw and therefore
 *    our location is whatever the resource being accessed was (e.g. ANYTHING ELSE).
 */
define(['dojo/dom','dojo/request/xhr','dojo/i18n!./nls/loginMessages', './hashCookie', 'dojo/_base/kernel', 'dojo/on', 'dojo/dom-class', 'dojo/io-query', 'dojox/html/entities'],
    function(dom, xhr, i18n, hashCookie, kernel, on, domClass, ioQuery, entities) {
  'use strict';
  
  /**
   * Show Username/Password when placeholder disappears (on focus)
   */
  function setLabelListeners() {
    var usernameLogin = dom.byId('usernameLabel');
    var j_username = dom.byId('j_username');
    var passwordLogin = dom.byId('passwordLabel');
    var j_password = dom.byId('j_password');
    
    // If there is a saved password, display the password label (user label is displayed by default since autofocus)
    if(j_password.value && j_password !== '') {
      domClass.add(passwordLogin, 'login-label-visible');
    }
    
    on(j_username, 'focus', function(evt){
      domClass.add(usernameLogin, 'login-label-visible');
    });
    on(j_username, 'blur', function(evt){
      // Once the user has entered form input, keep label displayed for easy validation
      if(!j_username.value || j_username.value === '') {
        domClass.remove(usernameLogin, 'login-label-visible');
      }
    });
    on(j_password, 'focus', function(evt){
      domClass.add(passwordLogin, 'login-label-visible');
    });
    on(j_password, 'blur', function(evt){
      // Once the user has entered form input, keep label displayed for easy validation
      if(!j_password.value || j_password.value === '') {
        domClass.remove(passwordLogin, 'login-label-visible');
      }
    });
  }

  /**
   * Sets the login button text to translated 'Login'
   * 
   * @param dom Dojo DOM object
   */
  function setLoginButtonText(dom) {
    dom.byId('loginButton').innerHTML = i18n.LOGIN_BUTTON;
  }

  /**
   * Sets the title text to translated version
   * 
   * @param dom Dojo DOM object
   */
  function setTitleText(dom) {
//    var queryParams = window.location.search;
//    var product = null;
//    var title = null;
//    if (queryParams) {
//      if (queryParams[0] === "?") {
//        queryParams = queryParams.substring(1);
//      }
//      var queryObject = ioQuery.queryToObject(queryParams);
//      if (queryObject.product) {
//        product = queryObject.product;
//      }
//      if (queryObject.title) {
//        title = queryObject.title;
//      }
//    }
//    if (title) {
//      // replace product title
//      dom.byId('loginTitle').innerHTML = entities.encode(title);
//      dom.byId('loginTabTitle').innerHTML = entities.encode(title);
//    } else {
      dom.byId('loginTitle').innerHTML = i18n.LOGIN_TITLE;
      dom.byId('loginTabTitle').innerHTML = i18n.LOGIN_TITLE;
//    }
  }

  /**
   * Sets the input placeholders & labels to translated version
   * 
   * @param dom Dojo DOM object
   */
  function setPlaceholders(dom) {
    dom.byId('j_username').placeholder = i18n.LOGIN_USER;
    dom.byId('j_password').placeholder = i18n.LOGIN_PASSWORD;
    dom.byId('j_username').setAttribute("aria-label", i18n.LOGIN_USER);
    dom.byId('j_password').setAttribute("aria-label", i18n.LOGIN_PASSWORD);
    
    dom.byId('usernameLabel').innerHTML = i18n.LOGIN_USER;
    dom.byId('passwordLabel').innerHTML = i18n.LOGIN_PASSWORD;
    dom.byId('usernameLabel').setAttribute("aria-label", i18n.LOGIN_USER);
    dom.byId('passwordLabel').setAttribute("aria-label", i18n.LOGIN_PASSWORD);
  }

  /**
   * Sets a message into the login footer element.
   * 
   * @param dom Dojo DOM object
   * @param msg Message to inject into the login footer
   */
  function updateLoginFooter(dom, msg) {
    dom.byId('login-footer').innerHTML=msg;
    dom.byId('j_username').setAttribute("aria-describedby", "login-footer");
    dom.byId('j_password').setAttribute("aria-describedby", "login-footer");
  }
  
  /**
   * Redirects to our standard 404 page
   */
  function redirectTo404() {
    // Redirect to /adminCenter/login/404.html
    window.top.location = window.location.protocol + '//' + window.location.host + '/adminCenter/404/404.html';
  }

  /**
   * Examines the page's pathname and query params to determine how we got here and
   * what, if anything, should be displayed.
   * 
   * Handling the errors is done in priority order:
   * 1st Login error [ /login.jsp?login-error ] - the user provided invalid credentials 
   * 2nd Unauthorized [ /login.jsp?user-unauthorized ] - the user is valid but not authorized
   * 3rd Unauthorized [ ANYTHING ELSE ]
   * 4th login.jsp with no known query params
   * 
   * The query params are defined in the web.xml
   * 
   * @param dom Dojo DOM object
   * @param pathname The window's location pathname (e.g. adminCenter/login.jsp)
   * @param queryParamsString The window's location query string (e.g. ?login-error)
   */
  function setErrorPathMessage(dom, pathname, queryParamsString) {
    var page = pathname.substring(pathname.lastIndexOf('/')+1);
    if (page === 'login.jsp') {
      // We are 1 2 or 3
      if (queryParamsString.search('login-error') >= 0) {
        // Case 2 - bad creds
        updateLoginFooter(dom, i18n.LOGIN_ERROR_MESSAGE);
      } else if (queryParamsString.search('user-unauthorized') >= 0) {
        // Case 3 - explicit unauthorized
        updateLoginFooter(dom, i18n.ACCESS_ERROR_MESSAGE);
      }
    } else {
      // Case 4 - ANYTHING ELSE (i.e. 403)
      updateLoginFooter(dom, i18n.ACCESS_ERROR_MESSAGE);
    }
    // Case 1 - default login.jsp (possibly with ignored query params)
  }

  /**
   * Given the X, Y and login div, do the math to compute where we draw and
   * whether or not its a phone.
   * 
   * @param x The window's X (horizontal)
   * @param y The window's Y (vertical)
   * @param login The login div element
   */
  function updateLoginCSSProperties(dom, x, y) {
    // If we know how big the login box is, then we can do some cool math
    if (dom && x && y) {
      // From http://screensiz.es/phone
      if (x <= 600 || (y <= 640 && x <= 940) || (y <= 480)) {
        // If we're in a portrait or a tiny screen, assume we're a phone
        dom.byId('login').className='phone';
      } else {
        // Otherwise, we're default (no class)
        dom.byId('login').className='';
      }
    } else {
      // We are unable to determine anything safely, default to CSS settings
      console.log('Unable to figure out what to do safely, using default CSS settings');
    }
  }

  /**
   * Sets up the login CSS properties based on the viewport. This handles the case where the viewport is:
   * 1. A desktop or mobile device in landscape mode (where y > x)
   * 2. A phone in portrait mode (where x < y)
   * 3. Error case where we can't determine anything... in which case we defer to default CSS
   */
  function setLoginCSSProperties() {
    // Safely compute the X and Y of the visible space
    var w=window,d=document,e=d.documentElement,g=d.getElementsByTagName('body')[0],x=w.innerWidth||e.clientWidth||g.clientWidth,y=w.innerHeight||e.clientHeight||g.clientHeight;
    updateLoginCSSProperties(dom, x, y);
  }

  /**
   * Triggers a log in, and handles a prior invocation to logout if necessary.
   * <p>
   * As documented in 'establishLoginProcessing', there are conditions where
   * performing a logout is necessary to work-around some Liberty behaviours.
   * <p>
   * This method uses the hidden input 'hiddenLoginFormSubmit' to actually
   * trigger a form submission. The reason we trigger the form submission via
   * the button click (and not some other means) is to take advantage of the
   * browser's natural form validation. Prior to invoking the form submit,
   * it is checked to see if the user has specified a user name and password.
   * If so, then if the page is an 'unauthorized' page, then the current user
   * needs to be logged out before the new user can properly log in.
   */
  function performLogin(dom, xhr, needToLogout) {
    var j_username = dom.byId('j_username').value;
    var j_password = dom.byId('j_password').value;
    // If a user name and password have been specified, do a login.
    // Otherwise, trigger the browser's native form validation by clicking the 'hiddenLoginFormSubmit' button
    if (j_username && j_password && needToLogout) {
      xhr.post('ibm_security_logout', {sync: true, data: 'logoutExitPage=%2Flogin.jsp', headers: {'Content-type':'application/x-www-form-urlencoded'}});
    }
    var submit = dom.byId('hiddenLoginFormSubmit');
    submit.disabled=false;
    submit.click();
    submit.disabled=true;    
  }

  /**
   * Wrapper to pass elements into performLogin so it can be unit tested.
   */
  function doLogin() {
    var needToLogout = window.location.search.search('user-unauthorized') >= 0 || window.location.pathname.substring(window.location.pathname.lastIndexOf('/')+1)!=='login.jsp';
    performLogin(dom, xhr, needToLogout);
    hashCookie.stopCapturing();
    var noAccess = window.location.search.search('no_access') >= 0;
    if (noAccess) {
      // Sent here by SessionFilter; user cannot access j_security_check
      redirectTo404();
    }
  }

  /**
   * Key Event Handler for input fields. If the user presses enter while typing,
   * then perform a login. Only on an Enter key should this event occur.
   */
  function loginIfEnterPressed(e) {
    if (e && e.keyCode === 13) {
      doLogin();
    }
  }

  /**
   * Override the default form login page with a custom login form processor.
   * <p>
   * Why is this necessary? The default behaviour of Liberty does not allow for a session
   * to be changed between users. So there is currently a flow which will cause the user
   * to enter an unusable state:
   * 1. Access the login page (if already authenticated as a non-Admin user, go to #3)
   * 2. Login as a non-admin user
   * 3. The user is unauthorized to access the Admin Center, and the page will then redirect
   *    to the login page to allow for a new authentication
   * 4. If the user authenticates as a new user, then the session established by the previous login
   *    will not be converted to the new user, and access to any resource on the server will fail.
   *    
   * The current solution is to log out the previous user BEFORE logging in with the new user ID.
   * This of course means that the user will be logged out even if the new credentials are bad.
   * However, if the user is attempting to log in with a new user, they can always log back in
   * as the previous user.
   * <p>
   * In order to be as resilient and reliable as possible, the login page does not require the
   * user to have any JavaScript enabled. If no JavaScript is enabled, then the above scenario
   * can occur. However, since almost all modern browsers have JavaScript, we can do some clever
   * things to improve the user's experience. This method establishes that better experience.
   * <p>
   * The way the better experience is enabled is to change the login button from a 'submit'
   * type input to a 'button' type input. This means the button can be clicked but the browser
   * won't actually do anything with the form. Our 'better' submit logic is tied in using the
   * 'onclick' event, which means that when a user clicks (or tabs to and selects) the login
   * button, we can process the login to avoid the aforementioned problem.
   * <p>
   * In addition to responding to a click event on the login button, we also respond to an
   * "Enter" key in any of the text fields. The typical user flow would be:
   * 1. [Mouse driven] Click Enter user, click on password, enter password, click on Login
   * 2. [Keyboard driven] Type in user, tab, type in password, enter
   * <p>
   * See 'performLogin' for more details.
   */
  function establishLoginProcessing(dom) {
    var loginButton = dom.byId('loginButton');
    loginButton.onclick=doLogin;
    loginButton.type='button';

    var j_username = dom.byId('j_username');
    var j_password = dom.byId('j_password');
    j_username.onkeyup=loginIfEnterPressed;
    j_password.onkeyup=loginIfEnterPressed;
  }

  function initPage() {
    // 1. Set page locale and override the English text accordingly.
    document.documentElement.setAttribute("lang", kernel.locale);
    setLoginButtonText(dom);
    setTitleText(dom);
    setPlaceholders(dom);
    dom.byId('loginCopyright').innerHTML = i18n.LOGIN_COPYRIGHT;

    // 2. Set any error path messages
    setErrorPathMessage(dom, window.location.pathname, window.location.search);

    // 3. Register a handler for window resizing. This should come before invoking
    //    setLoginCSSProperties so that we respond to page resizes as soon as possible.
    window.onresize = function(event) {
      setLoginCSSProperties();
    };

    // 4. Set the login CSS properties based on steps 1 & 2
    setLoginCSSProperties();
    
    // 5. Set the Login Labels
    setLabelListeners();

    // 6. Override the Login Button behaviour to work-around the unauthorized login path
    establishLoginProcessing(dom);

    // 7. Grab the URL hash and store it in a cookie. We need to do this because
    //    (what we think is happening) is that the form login POST and subsequent 302
    //    is removing the # part of the URL entirely. We store what we get in a cookie
    //    and pass it through the login and its pulled out on the other side by the
    //    toolbox loader.
    hashCookie.captureHashCookie();
  }

  // Export our methods ( __xyz are for test purposes)
  return {
    initPage: initPage,
    __setLoginButtonText: setLoginButtonText,
    __setTitleText: setTitleText,
    __setPlaceholders: setPlaceholders,
    __setErrorPathMessage: setErrorPathMessage,
    __updateLoginCSSProperties: updateLoginCSSProperties,
    __establishLoginProcessing: establishLoginProcessing,
    __performLogin: performLogin
  };

});
