<%--
    Copyright (c) 2014, 2026 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0

    Contributors:
        IBM Corporation - initial API and implementation
 --%>
<%@ page session="false" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String productInfo = "";
    boolean isOpenLiberty = false;
    
    try {
        // Try to read from properties files
        // Check all properties files and use the one that's NOT io.openliberty if multiple exist
        String installRoot = System.getProperty("wlp.install.dir");
        if (installRoot != null) {
            java.io.File versionsDir = new java.io.File(installRoot, "lib/versions");
            
            if (versionsDir.exists() && versionsDir.isDirectory()) {
                java.io.File[] allFiles = versionsDir.listFiles();
                
                String openLibertyName = "";
                String otherProductName = "";
                
                // Check all properties files
                if (allFiles != null && allFiles.length > 0) {
                    for (int i = 0; i < allFiles.length; i++) {
                        java.io.File propsFile = allFiles[i];
                        String fileName = propsFile.getName();
                        
                        // Only process .properties files, skip service.fingerprint
                        if (fileName.endsWith(".properties") && !fileName.equals("service.fingerprint")) {
                            java.util.Properties props = new java.util.Properties();
                            java.io.FileInputStream fis = null;
                            try {
                                fis = new java.io.FileInputStream(propsFile);
                                props.load(fis);
                                
                                String productId = props.getProperty("com.ibm.websphere.productId", "");
                                String name = props.getProperty("com.ibm.websphere.productName", "");
                                
                                if (name != null && !name.isEmpty()) {
                                    // If this is Open Liberty, save it separately
                                    if ("io.openliberty".equals(productId)) {
                                        openLibertyName = name;
                                    } else {
                                        // This is WebSphere or another product - prioritize it
                                        otherProductName = name;
                                    }
                                }
                            } finally {
                                if (fis != null) {
                                    try { fis.close(); } catch (Exception e) {}
                                }
                            }
                        }
                    }
                    
                    // Use WebSphere/other product name if available, otherwise use Open Liberty
                    if (otherProductName != null && !otherProductName.isEmpty()) {
                        productInfo = otherProductName;
                        isOpenLiberty = false;
                    } else if (openLibertyName != null && !openLibertyName.isEmpty()) {
                        productInfo = openLibertyName;
                        isOpenLiberty = true;
                    }
                }
            }
        }
        
        // Fallback to default if we couldn't get the product name
        if (productInfo == null || productInfo.isEmpty()) {
            productInfo = "Liberty";
        }
        
        // Escape for JavaScript string
        if (productInfo != null && !productInfo.isEmpty()) {
            productInfo = productInfo.replace("\\", "\\\\")
                                     .replace("\"", "\\\"")
                                     .replace("'", "\\'")
                                     .replace("\n", "\\n")
                                     .replace("\r", "\\r");
        }
    } catch (Exception ex) {
        // If we can't get the product info, use default
        productInfo = "Liberty";
    }
%>
<!DOCTYPE html>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <meta name="DC.Rights" content="Â© Copyright IBM Corp. 2014" />
  <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,minimum-scale=1,user-scalable=no" />
  <meta name="apple-mobile-web-app-capable" content="yes" />
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta name="apple-touch-fullscreen" content="yes" />

  <link href="<%= isOpenLiberty ? "login/images/runtime-fav-icon.svg" : "login/images/favicon.ico" %>" rel="icon" />
  <link href="<%= isOpenLiberty ? "login/images/runtime-fav-icon.svg" : "login/images/favicon.ico" %>" rel="shortcut icon" />
  <link href="login/images/apple-touch-icon.png" rel="apple-touch-icon" />
  <link href="login/login.css" rel="stylesheet"></link>

<%
    // delete the JSESSION cookie
    // so that invalidate will generate a new session
    Cookie[] cookies = request.getCookies();
    response.setContentType("text/html");
    if (cookies != null) {
      for (Cookie cookie : cookies) {
          if ("JSESSIONID".equals(cookie.getName())) {
              String value = cookie.getValue();
              cookie.setMaxAge(0);
              cookie.setValue(null);
              cookie.setPath("/");
              response.addCookie(cookie);
              break;
          }
      }
    }
    
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
    response.setHeader("Content-Security-Policy", "default-src 'self' 'unsafe-inline' 'unsafe-eval'; form-action 'self'; frame-ancestors 'self'");
    response.setHeader("Strict-Transport-Security", "max-age=99999999");

    String dojoConfigString = ""; // this is required otherwise it won't run
%>
  <script src="404/404.js"></script>
  <script type="text/javascript">
    var userLocale = getLanguageCode();
    
    // Product info from server
    var productInfoData = "<%= productInfo %>";
    var dojoConfig = {
      locale: userLocale
    };

    document.documentElement.setAttribute("lang", userLocale);
  </script>
  <script src="dojo/dojo.js"></script>
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
      <img class="liberty-logo" src="<%= isOpenLiberty ? "login/images/runtime-icon.svg" : "login/images/WAS-Liberty-Logo-White.png" %>" alt="">
      <header class="login-header">
        <h1 id="loginTitle">Liberty Admin Center</h1>
      </header>
    <div class="login-form">
      <form action="j_security_check" method="POST" id="loginForm">
          <div class="login-label login-label-visible" id="usernameLabel">User Name</div>
          <input id="j_username" class="loginTextBox" name="j_username" type="text" placeholder="User Name" autocomplete="off" autocapitalize="off" required autofocus />
          <div class="login-label" id="passwordLabel">Password</div>
          <input id="j_password" class="loginTextBox" name="j_password" type="password" placeholder="Password" autocomplete="off" autocapitalize="off" required />
          <input type="hidden" id="csrfTokenField" name="X-CSRF-Token" value="" />
        <div class="button-bar">
          <button id="loginButton" class="mblButton submit-btn" type="submit">Submit</button>
        </div>
        <button id="hiddenLoginFormSubmit" type="submit" hidden disabled style="display:none;">Submit</button>
      </form>
    </div>
    <script type="text/javascript">
      // Read CSRF token from cookie and populate hidden field before form submission
      (function() {
        'use strict';
        function getCookie(name) {
          var value = "; " + document.cookie;
          var parts = value.split("; " + name + "=");
          if (parts.length === 2) return parts.pop().split(";").shift();
          return null;
        }
        
        var loginForm = document.getElementById('loginForm');
        if (loginForm) {
          loginForm.addEventListener('submit', function(e) {
            var csrfToken = getCookie('csrfToken');
            if (csrfToken) {
              document.getElementById('csrfTokenField').value = csrfToken;
            }
          });
        }
      })();
    </script>
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
