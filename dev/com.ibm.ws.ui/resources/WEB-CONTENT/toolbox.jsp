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

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.io.BufferedReader"%>
<%@ page import="java.io.IOException"%>
<%@ page import="java.io.InputStreamReader"%>
<%@ page import="java.io.OutputStreamWriter"%>
<%@ page import="java.net.*"%>
<%
    // GET PRODUCT NAME - Detect if this is Open Liberty or WebSphere
    boolean isOpenLiberty = false;
    
    try {
        // Try to read from properties files
        String installRoot = System.getProperty("wlp.install.dir");
        if (installRoot != null) {
            java.io.File versionsDir = new java.io.File(installRoot, "lib/versions");
            
            if (versionsDir.exists() && versionsDir.isDirectory()) {
                java.io.File[] allFiles = versionsDir.listFiles();
                
                boolean foundOpenLiberty = false;
                boolean foundOtherProduct = false;
                
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
                                
                                // Check if this is Open Liberty
                                if ("io.openliberty".equals(productId)) {
                                    foundOpenLiberty = true;
                                } else if (productId != null && !productId.isEmpty()) {
                                    // Found another product (WebSphere)
                                    foundOtherProduct = true;
                                }
                            } finally {
                                if (fis != null) {
                                    try { fis.close(); } catch (Exception e) {}
                                }
                            }
                        }
                    }
                    
                    // Only set isOpenLiberty to true if we found Open Liberty and no other product
                    isOpenLiberty = foundOpenLiberty && !foundOtherProduct;
                }
            }
        }
    } catch (Exception ex) {
        // If we can't determine, default to false (WebSphere)
        isOpenLiberty = false;
    }
%>
<!DOCTYPE html>
<html style="height: 100%; width: 100%; margin: 0px; padding: 0px;">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="DC.Rights" content="Â© Copyright IBM Corp. 2014" />
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>

<link href="<%= isOpenLiberty ? "login/images/runtime-fav-icon.svg" : "login/images/favicon.ico" %>" rel="icon" />
<link href="<%= isOpenLiberty ? "login/images/runtime-fav-icon.svg" : "login/images/favicon.ico" %>" rel="shortcut icon" />
<link href="login/images/apple-touch-icon.png" rel="apple-touch-icon" />
<link rel="stylesheet" href="dojo/resources/dojo.css" />
<link rel="stylesheet" href="dijit/themes/dijit.css" />
<link rel="stylesheet" href="idx/themes/oneui/oneui.css" />

<link rel="stylesheet" href="css/toolbox.css" />

<script>
	var BIDI_PREFS_STRING = '{"bidiEnabled":false,"bidiTextDirection":"ltr"}';
</script>

<%
	boolean isAdmin = request.isUserInRole("Administrator");
	String userRole = isAdmin ? "Administrator" :
	                  request.isUserInRole("Reader") ? "Reader" :
				  "";
%>
<script type="text/javascript">
	globalIsAdmin=<%=isAdmin%>
</script>

<%
    Cookie[] cookies = request.getCookies();
    response.setContentType("text/html");
    
    // Remove old JSESSIONID cookie - inline implementation
    if (cookies != null) {
        for (int i = 0; i < cookies.length; i++) {
            if ("JSESSIONID".equals(cookies[i].getName())) {
                cookies[i].setMaxAge(0);
                cookies[i].setValue(null);
                cookies[i].setPath("/");
                response.addCookie(cookies[i]);
                break;
            }
        }
    }

    // Retrieve and validate CSRF token from cookie - inline implementation
    String csrfToken = null;
    boolean isValidToken = false;
    if (cookies != null) {
        for (int i = 0; i < cookies.length; i++) {
            if ("csrfToken".equals(cookies[i].getName())) {
                String tokenValue = cookies[i].getValue();
                // Validate UUID format: 8-4-4-4-12 hexadecimal characters
                if (tokenValue != null && tokenValue.length() == 36 &&
                    tokenValue.matches("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$")) {
                    csrfToken = tokenValue;
                    isValidToken = true;
                }
                break;
            }
        }
    }

    // Prevent any session fixation/hijacking hijinx by getting new session after logging in
    request.getSession().invalidate();
    request.getSession(true);

    // Always force the above by never caching the jsp
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.setHeader("Pragma", "No-cache");
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setDateHeader("Expires", -1);

    // Set security headers
    response.setHeader("X-XSS-Protection", "1");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "SAMEORIGIN");
    response.setHeader("Content-Security-Policy", "default-src 'self' 'unsafe-inline' 'unsafe-eval'; form-action 'self'; frame-ancestors 'self'");
    response.setHeader("Strict-Transport-Security", "max-age=99999999");

    String hasBidi = "";       // used to initialize dojo
    String userId = request.getRemoteUser();     // passed to widgets
    String dojoConfigString = ""; // this is required otherwise it won't run
    
    String localAddress = request.getLocalAddr();
    // ipv6 addresses must be enclosed with square brackets in URLs
    localAddress = localAddress.contains(":") ? "[" + localAddress + "]" : localAddress;
    String urlString = "https://" + localAddress + ":" + request.getLocalPort();
    urlString = urlString + "/ibm/api/adminCenter/v1/toolbox/preferences";
    
    HttpURLConnection connection = null;
    BufferedReader rd  = null;
    CookieManager cm = new CookieManager();
    CookieHandler.setDefault(cm);
    if ( request.getCookies() != null ) {
	    for (int i = 0; i < request.getCookies().length; i++){
	        cm.getCookieStore().add(new URI(request.getRequestURI()), new HttpCookie(request.getCookies()[i].getName(),request.getCookies()[i].getValue()));
        }
    }
    StringBuffer sb = new StringBuffer();
    for (Iterator iter = cm.getCookieStore().getCookies().iterator(); iter.hasNext(); ){
        HttpCookie cookie = (HttpCookie) iter.next();
        if (sb.length() > 0){
            sb.append("; ");
        }
        sb.append(cookie.getName()).append("=").append(cookie.getValue());
    }
    URL serverURL = null;
    try {
        serverURL = new URL(urlString);
        //set up out communications stuff
        connection = null;
        
        //Set up the initial connection
        connection = (HttpURLConnection)serverURL.openConnection();
        connection.setRequestProperty("Cookie", sb.toString());
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setReadTimeout(10000);
        connection.connect();
        
        //read the result from the server
        rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        
        String line = null;
        while ((line = rd.readLine()) != null){
            // this should be one line like this: {"bidiEnabled":true,"bidiTextDirection":"ltr"}
%>
<script>
BIDI_PREFS_STRING = '<%=line%>';
</script>
<%
            if (line.indexOf("\"bidiEnabled\":true") != -1){
                hasBidi = "has:{'adminCenter-bidi': true, 'dojo-bidi': true}"; 
                //System.out.println("Bidi is enabled for the UI application with properties:" + hasBidi);
%>
        <link rel="stylesheet" href="dijit/themes/dijit_rtl.css" />
<%                
            }
        }
    } catch (MalformedURLException e) {
        // just default to no bidi
        //e.printStackTrace();
    } catch (ProtocolException e) {
        // just default to no bidi
        //e.printStackTrace();
    } catch (IOException e) {
        // just default to no bidi
        //e.printStackTrace();
    }
    finally
    {
        // clean up: close the connection
        connection.disconnect();
        rd = null;
        sb = null;
        connection = null;
    }
%>

<script src="404/404.js"></script>
<script>
    // Double-Submit Cookie CSRF Protection
    // Token is read from cookie and sent in custom header for all state-changing requests
    
    function getCookie(name) {
        const value = document.cookie
            .split(";")
            .map(c => c.trim())
            .find(c => c.startsWith(name + "="));
        return value ? value.split("=")[1] : null;
    }
    
    function isValidCsrfToken(token) {
        if (!token || typeof token !== "string") return false;
        // Validate UUID format: 8-4-4-4-12 hexadecimal characters
        const uuidRegex = /^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/;
        return uuidRegex.test(token);
    }
    
    // Validate CSRF token on page load
    const initialToken = getCookie("csrfToken");
    if (!isValidCsrfToken(initialToken)) {
        window.location.href = "/adminCenter/login.jsp";
    }
    
    // Validate CSRF token on hash changes
    window.addEventListener("hashchange", () => {
        const token = getCookie("csrfToken");
        if (!isValidCsrfToken(token)) {
            window.location.href = "/adminCenter/login.jsp";
            return;
        }
    });
    
    // Intercept native XMLHttpRequest as fallback for non-Dojo requests
    // Check if header already exists to prevent duplication
    (function() {
        
        const originalOpen = XMLHttpRequest.prototype.open;
        const originalSend = XMLHttpRequest.prototype.send;
        const originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;
        
        XMLHttpRequest.prototype.open = function(method, url, async, user, password) {
            this._method = method;
            this._url = url;
            this._requestHeaders = {}; // Track all headers
            return originalOpen.apply(this, arguments);
        };
        
        XMLHttpRequest.prototype.setRequestHeader = function(header, value) {
            // Track headers being set
            if (this._requestHeaders) {
                this._requestHeaders[header.toLowerCase()] = value;
            }
            return originalSetRequestHeader.apply(this, arguments);
        };
        
        XMLHttpRequest.prototype.send = function(data) {
            
            // Add CSRF token header for state-changing requests
            if (this._method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(this._method.toUpperCase())) {
                
                // Only add if not already present (check case-insensitive)
                const hasToken = this._requestHeaders && this._requestHeaders['x-csrf-token'];
                
                if (!hasToken) {
                    const token = getCookie("csrfToken");
                    
                    if (token && isValidCsrfToken(token)) {
                        this.setRequestHeader('X-CSRF-Token', token);
                        console.log('✓ Added CSRF token via native XHR interceptor to:', this._url);
                    } else {
                        console.error("Cannot send request: Invalid CSRF token");
                        throw new Error("CSRF token validation failed");
                    }
                } else {
                    console.log('✓ CSRF token already present, skipping');
                }
            }
            return originalSend.apply(this, arguments);
        };
    })();
    
    // Function to inject CSRF interceptor into iframe contexts
    function injectCsrfInterceptorIntoIframe(iframe) {
        try {
            const iframeWindow = iframe.contentWindow;
            const iframeDocument = iframe.contentDocument || iframeWindow.document;
            
            // Check if iframe is accessible (same-origin)
            if (!iframeWindow || !iframeWindow.XMLHttpRequest) {
                return;
            }
            
            // Inject getCookie and isValidCsrfToken functions into iframe
            iframeWindow.getCookie = getCookie;
            iframeWindow.isValidCsrfToken = isValidCsrfToken;
            
            // Inject XHR interceptor into iframe
            const originalOpen = iframeWindow.XMLHttpRequest.prototype.open;
            const originalSend = iframeWindow.XMLHttpRequest.prototype.send;
            const originalSetRequestHeader = iframeWindow.XMLHttpRequest.prototype.setRequestHeader;
            
            iframeWindow.XMLHttpRequest.prototype.open = function(method, url, async, user, password) {
                this._method = method;
                this._url = url;
                this._requestHeaders = {};
                return originalOpen.apply(this, arguments);
            };
            
            iframeWindow.XMLHttpRequest.prototype.setRequestHeader = function(header, value) {
                if (this._requestHeaders) {
                    this._requestHeaders[header.toLowerCase()] = value;
                }
                return originalSetRequestHeader.apply(this, arguments);
            };
            
            iframeWindow.XMLHttpRequest.prototype.send = function(data) {
                
                if (this._method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(this._method.toUpperCase())) {
                    const hasToken = this._requestHeaders && this._requestHeaders['x-csrf-token'];
                    
                    if (!hasToken) {
                        // Get token from parent window's cookie
                        const token = window.parent.getCookie("csrfToken");
                        if (token && window.parent.isValidCsrfToken(token)) {
                            this.setRequestHeader('X-CSRF-Token', token);
                        } else {
                            throw new Error("CSRF token validation failed");
                        }
                    }
                }
                return originalSend.apply(this, arguments);
            };
            
        } catch (e) {
            console.error('Failed to inject CSRF interceptor into iframe:', e.message);
        }
    }
    
    // Setup iframe monitoring after DOM is ready
    function setupIframeMonitoring() {
        
        // Monitor for iframes being added to the page
        const iframeObserver = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
                mutation.addedNodes.forEach(function(node) {
                    if (node.tagName === 'IFRAME') {
                        // Wait for iframe to load before injecting
                        node.addEventListener('load', function() {
                            injectCsrfInterceptorIntoIframe(node);
                        });
                        // Also try immediately in case it's already loaded
                        if (node.contentWindow) {
                            setTimeout(function() {
                                injectCsrfInterceptorIntoIframe(node);
                            }, 100);
                        }
                    }
                });
            });
        });
        
        // Start observing document.body
        if (document.body) {
            iframeObserver.observe(document.body, {
                childList: true,
                subtree: true
            });
        } else {
            console.error('document.body not available yet');
        }
        
        // Inject into any existing iframes
        const iframes = document.querySelectorAll('iframe');
        iframes.forEach(function(iframe) {
            if (iframe.contentWindow) {
                injectCsrfInterceptorIntoIframe(iframe);
            }
            iframe.addEventListener('load', function() {
                injectCsrfInterceptorIntoIframe(iframe);
            });
        });
        
    }
    
    // Run setup when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setupIframeMonitoring);
    } else {
        // DOM already loaded
        setupIframeMonitoring();
    }
    
    // Intercept fetch API calls to add CSRF token header (Double-Submit Pattern)
    (function() {
        const originalFetch = window.fetch;
        window.fetch = function(url, options) {
            options = options || {};
            const method = (options.method || 'GET').toUpperCase();
            
            // Add CSRF token header for state-changing requests
            if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
                options.headers = options.headers || {};
                
                // Check if header already exists (case-insensitive)
                let hasToken = false;
                if (options.headers instanceof Headers) {
                    hasToken = options.headers.has('X-CSRF-Token') || options.headers.has('x-csrf-token');
                } else {
                    hasToken = Object.keys(options.headers).some(key =>
                        key.toLowerCase() === 'x-csrf-token'
                    );
                }
                
                if (!hasToken) {
                    const token = getCookie("csrfToken");
                    if (token && isValidCsrfToken(token)) {
                        if (options.headers instanceof Headers) {
                            options.headers.append('X-CSRF-Token', token);
                        } else {
                            options.headers['X-CSRF-Token'] = token;
                        }
                        console.debug('Added CSRF token via fetch interceptor');
                    } else {
                        console.error("Cannot send request: Invalid CSRF token");
                        return Promise.reject(new Error("CSRF token validation failed"));
                    }
                }
            }
            return originalFetch.apply(this, arguments);
        };
    })();
</script>

<script type="text/javascript">
    var languageLocale = getLanguageCode();
    document.documentElement.setAttribute("lang", languageLocale);
</script>
<%
    if (hasBidi.length() == 0) {
%>
<script type="text/javascript">
        var dojoConfig = {
            locale: languageLocale
        };
</script>
<%
    } else {
%>
<script type="text/javascript">
        var dojoConfig = {
            locale: languageLocale,
            has: {
                'adminCenter-bidi': true,
                'dojo-bidi': true
            }
        };
</script>
<%
    }
%>
<script src="dojo/dojo.js"></script>
<script>
    // Intercept Dojo XHR requests after Dojo loads
    require(["dojo/request/xhr", "dojo/aspect", "dojo/ready"], function(xhr, aspect, ready) {
        // Wrap each xhr method to add CSRF token
        function wrapXhrMethod(methodName, httpMethod) {
            aspect.around(xhr, methodName, function(originalMethod) {
                return function(url, options) {
                    options = options || {};
                    
                    // Add CSRF token header for state-changing requests
                    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(httpMethod)) {
                        options.headers = options.headers || {};
                        
                        var token = getCookie("csrfToken");
                        if (token && isValidCsrfToken(token)) {
                            options.headers['X-CSRF-Token'] = token;
                            console.debug('Added CSRF token to Dojo ' + methodName + ' request');
                        } else {
                            console.error("Cannot send Dojo request: Invalid CSRF token");
                            throw new Error("CSRF token validation failed");
                        }
                    }
                    
                    // Call the original method
                    return originalMethod.call(this, url, options);
                };
            });
        }
        
        // Wrap all state-changing methods
        wrapXhrMethod('post', 'POST');
        wrapXhrMethod('put', 'PUT');
        wrapXhrMethod('del', 'DELETE');
        
        
        // Use ready() to ensure toolbox loads after DOM and Dojo are fully ready
        ready(function() {
            require(["js/loadToolbox"]);
        });
    });
</script>

 
<title id="toolbox_tab_title"></title>

</head>

<body class="oneui" style="height:100%; width:100%; padding-top: 0px;">

  <svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="64px" height="64px" viewBox="0 0 64 64" enable-background="new 0 0 64 64" xml:space="preserve" display="none"><g id="status-icons-old"><g id="status-alert"><path fill="#DF7300" d="M32,6L2,58h60L32,6z M32,10l26.5,46h-53L32,10z"/><polygon fill="#DF7300" points="30,28 30,32 31,42 33,42 34,32 34,28"/><circle fill="#DF7300" cx="32" cy="46" r="2"/></g><g id="status-alert-small"><path fill="#DF7300" d="M16,2.1l-15,26h30L16,2.1z M16,6.1l11.5,20H4.5L16,6.1z"/><polygon fill="#DF7300" points="15,13.1 15,15.1 15.5,20.1 16.5,20.1 17,15.1 17,13.1"/><circle fill="#DF7300" cx="16" cy="22.1" r="1"/></g><g id="status-alert-gray-small"><path fill="#6F6F6F" d="M16,1.1l-15,26h30L16,1.1z M16,5.1l11.5,20h-23L16,5.1z"/><polygon fill="#6F6F6F" points="15,12.1 15,14.1 15.5,19.1 16.5,19.1 17,14.1 17,12.1"/><circle fill="#6F6F6F" cx="16" cy="21.1" r="1"/></g></g></svg>

  <noscript>
    <div id="disabledJavaScriptBanner" role="region" aria-label="JavaScript required">>
      <h2>Admin Center requires JavaScript. JavaScript is currently disabled.</h2>
      <h2>Enable JavaScript or use a browser which supports JavaScript.</h2>
    </div>
  </noscript>
  <div id="mainContainer" class="mainContainer" style="height:100%; width:100%">

    <!-- dojox.mobile.ScrollableView with fixed="top" attribute on toolbox_headerWidget allows filter box and icons to scroll up,
          underneath "My Toolbox". Can't use that type, but dojox.mobile.View is not allowing the desired behavior --> 
    <!-- Hard code the aria-label for the headers to English here. This prevents the RPT violation. Note the actual value when
         the specific view is displayed is set programmatically in LibertyHeader.js.  -->
    <div id='toolboxContainer' data-dojo-type="dojox.mobile.View" data-dojo-props=" 
             style: 'width: 100%; margin: auto;'">
      <div data-dojo-type="js/widgets/LibertyHeader" id="toolBox_headerWidget" containerId="toolboxContainer" userName="<%=userId%>" aria-label="Toolbox"> 
      </div> <!-- end of header -->
      <div data-dojo-type="js/widgets/LibertyToolbox" id="toolIconContainer" role="main">
      </div> 
      <div id="addBookmarkDialogId" ></div>
    </div> <!-- end of contentPane -->
 
    <div id='catalogContainer' data-dojo-type="dojox.mobile.View" data-dojo-props="style: 'width: 100%; height: 100%; margin: auto;'">
      <div data-dojo-type="js/widgets/LibertyHeader" id="catalog_headerWidget" containerId="catalogContainer" userName="<%=userId%>" aria-label="Tool catalog"> 
      </div> <!-- end of header -->
      <div data-dojo-type="js/widgets/LibertyCatalog" id="catalogIconContainer" role="main">
      </div> 
    </div> <!-- end of contentPane -->

    <div id='bgTasksContainer' data-dojo-type="dojox.mobile.View" data-dojo-props="style: 'width: 100%; height: 100%; margin: auto; background-color: #F8F8F7'">
      <div data-dojo-type="js/widgets/LibertyHeader" id="bgTasks_headerWidget" containerId="bgTasksContainer" userName="<%=userId%>" aria-label="Background tasks"> 
      </div> <!-- end of header -->
      <div data-dojo-type="js/widgets/BGTasks" id="bgTasksTreeContainer" role="main"></div>
      <div style="clear: both"></div>
      <div style="height: 15px;width: 100%"></div>
      <div id="bgTasksTreeView"></div> 
    </div> <!-- end of contentPane -->


    <div id='toolContainer' data-dojo-type="dojox.mobile.View" data-dojo-props="style: 'width: 100%; height: 100%; margin: auto; overflow: hidden;'">
      <div data-dojo-type="js/widgets/LibertyHeader" id="tool_headerWidget" containerId="toolContainer" userName="<%=userId%>" aria-label="Tool"></div>
      <div id='toolContentContainer' class='toolContentContainerDiv'></div>
    </div>

    <div id='prefsContainer' data-dojo-type="dojox.mobile.View" data-dojo-props="style: 'width: 100%; height: 100%; margin: auto;'">
      <div data-dojo-type="js/widgets/LibertyHeader" id="prefs_headerWidget" containerId="prefsContainer" userName="<%=userId%>" aria-label="Preferences"></div>
      <div data-dojo-type="js/widgets/LibertyPrefs" class="profile-container" id="prefsContentContainer" role="main" userName="<%=userId%>" userRole="<%=userRole%>" style="width: 100%; height: 100%;"></div>
    </div>            

  </div>
</body>
</html>

