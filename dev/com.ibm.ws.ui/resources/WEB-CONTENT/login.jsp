<%--
    Copyright (c) 2014 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
 --%>
<%@ page session="false" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <meta name="DC.Rights" content="Â© Copyright IBM Corp. 2014" />
  <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,minimum-scale=1,user-scalable=no" />
  <meta name="apple-mobile-web-app-capable" content="yes" />
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta name="apple-touch-fullscreen" content="yes" />

  <link href="login/images/favicon.ico" rel="icon" />
  <link href="login/images/favicon.ico" rel="shortcut icon" />
  <link href="login/images/apple-touch-icon.png" rel="apple-touch-icon" />
  <link href="login/login.css" rel="stylesheet"></link>

<%
    // If a user is logged in or there is a valid session, logout and invalidate
    // Create a session if there isn't on as it will still be set on response even when invalidated,
    // allowing reverse proxies to route via session affinity.  The initial/immediate session is necessary
    // to prevent a corner case where more than one admin center is present and they're of different versions
    // with different login resource files.
    request.logout();
    
    HttpSession session = request.getSession(true);
    
    if (session != null) {
        session.invalidate();
    }
    
    // Always force the above by never caching the jsp
    response.setHeader("Pragma", "No-cache");
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setDateHeader("Expires", -1);

    // Set security headers	
    response.setHeader("X-XSS-Protection", "1");	
    response.setHeader("X-Content-Type-Options", "nosniff");	
    response.setHeader("X-Frame-Options", "SAMEORIGIN");

    // TODO: In newer browsers, the lang could have a variant which is not handled by dojo. ex. zh-hant-tw
    // So, construct a "normal" lang-country from the locale
    // TODO: for some reason, getVariant() is returning the country so for now ...
    String userLocale = request.getLocale().getLanguage().toLowerCase();
    if (request.getLocale().getVariant().length() > 0) {
        userLocale += "-" + request.getLocale().getVariant().toLowerCase();
    } else if (request.getLocale().getCountry().length() > 0) {
        userLocale += "-" + request.getLocale().getCountry().toLowerCase();
    }
    String dojoConfigString = "locale: '" + userLocale + "'";
%>

  <script src="dojo/dojo.js" data-dojo-config="<%=dojoConfigString%>"></script>
  <script src="login/login-init.js"></script>
  <title id="loginTabTitle">Liberty Admin Center</title>
</head>
<body>
  <noscript>
    <div id="disabledJavaScriptBanner" role="region" aria-label="JavaScript required">
      <h2>Admin Center requires JavaScript. JavaScript is currently disabled.</h2>
      <h2>Enable JavaScript or use a browser which supports JavaScript.</h2>
    </div>
  </noscript>
  <div class="bg"></div>
  <div class="bg-fill-color"></div>
  <section id="login">
    <div class="login-panel" role="main">
      <img class="liberty-logo" src="login/images/WAS-Liberty-Logo-White.png" alt="">
      <header class="login-header">
        <h1 id="loginTitle">Liberty Admin Center</h1>
      </header>
    <div class="login-form">
      <form action="j_security_check" method="POST">
          <div class="login-label login-label-visible" id="usernameLabel">User Name</div>       
          <input id="j_username" class="loginTextBox" name="j_username" type="text" placeholder="User Name" autocomplete="off" autocapitalize="off" required autofocus />
          <div class="login-label" id="passwordLabel">Password</div>
          <input id="j_password" class="loginTextBox" name="j_password" type="password" placeholder="Password" autocomplete="off" autocapitalize="off" required />
        <div class="button-bar">
          <button id="loginButton" class="mblButton submit-btn" type="submit">Submit</button>
        </div>
        <button id="hiddenLoginFormSubmit" type="submit" hidden disabled style="display:none;">Submit</button>
      </form>
    </div>
      <div id="login-footer" class="login-footer"></div>
    </div>
  </section>
  <footer class="login-copyrightFooter">
      <div style="display:inline-block">
        <img class="login-ibm-logo" alt="" src="login/images/IBM_logo_white.png"/>
      </div>
      <div class="login-legal-copy" id="loginCopyright">
        Fill in
      </div>
  </footer>

</body>
</html>
