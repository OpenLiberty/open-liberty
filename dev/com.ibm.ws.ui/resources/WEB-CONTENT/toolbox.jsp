<%--
    Copyright (c) 2014 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

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

<!DOCTYPE html>
<html style="height: 100%; width: 100%; margin: 0px; padding: 0px;">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="DC.Rights" content="Â© Copyright IBM Corp. 2014" />
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>

<link href="login/images/favicon.ico" rel="icon" />
<link href="login/images/favicon.ico" rel="shortcut icon" />
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
    // Prevent any session fixation/hijacking hijinx by getting new session after logging in 
    request.getSession().invalidate();
    HttpSession newSession = request.getSession(true);

    // Always force the above by never caching the jsp
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.setHeader("Pragma", "No-cache");
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setDateHeader("Expires", -1);

    // Set security headers	
    response.setHeader("X-XSS-Protection", "1");	
    response.setHeader("X-Content-Type-Options", "nosniff");	
    response.setHeader("X-Frame-Options", "SAMEORIGIN");

    String hasBidi = "";       // used to initialize dojo
    String userId = request.getRemoteUser();     // passed to widgets
    // String userLocale = request.getLocale().toString().replace('_', '-').toLowerCase();
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
        if (sb.length() == 0){
            sb.append(iter.next());
        } else {
            sb.append("," + iter.next());
        }
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
        if (hasBidi.length() > 0){
            dojoConfigString = dojoConfigString + ", " + hasBidi;
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

<script src="dojo/dojo.js" data-dojo-config="<%=dojoConfigString%>"></script>
<script>require(["js/loadToolbox"])</script>

 
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
