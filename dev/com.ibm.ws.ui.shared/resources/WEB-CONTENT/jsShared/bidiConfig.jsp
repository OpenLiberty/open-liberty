<%--
    Copyright (c) 2014, 2025 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0

    Contributors:
        IBM Corporation - initial API and implementation
 --%>
<%@ page session="false" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.io.BufferedReader"%>
<%@ page import="java.io.IOException"%>
<%@ page import="java.io.InputStreamReader"%>
<%@ page import="java.io.OutputStreamWriter"%>
<%@ page import="java.net.*"%>

<%
	boolean isAdmin = request.isUserInRole("Administrator");
%>
<script type="text/javascript">
	globalIsAdmin=<%=isAdmin%>
	console.info('isUserInRole("Administrator")=', globalIsAdmin);
</script>

<%

    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    String hasBidi = "";       // used to initialize dojo

    // Set security headers	
    response.setHeader("X-XSS-Protection", "1");	
    response.setHeader("X-Content-Type-Options", "nosniff");	
    response.setHeader("X-Frame-Options", "SAMEORIGIN");
    response.setHeader("Content-Security-Policy", "default-src 'self' 'unsafe-inline' 'unsafe-eval'; form-action 'self'; frame-ancestors 'self'");
   
    String dojoConfigString = "";
    
    String localAddress = request.getLocalAddr();
    // ipv6 addresses must be enclosed with square brackets in URLs
    localAddress = localAddress.contains(":") ? "[" + localAddress + "]" : localAddress;
    String urlString = "https://" + localAddress + ":" + request.getLocalPort();
    urlString = urlString + "/ibm/api/adminCenter/v1/toolbox/preferences";
    
    HttpURLConnection connection = null;
    BufferedReader rd  = null;
    CookieManager cm = new CookieManager();
    CookieHandler.setDefault(cm);
    if ( request.getCookies()!= null ) {
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

            if (line.indexOf("\"bidiEnabled\":true") > 0) {
                
                String bidiType = "";
                if (line.indexOf("\"bidiTextDirection\":\"ltr\"") > 0) {
                    bidiType = "ltr";
                } else if (line.indexOf("\"bidiTextDirection\":\"rtl\"") > 0) {
                    bidiType = "rtl";
                } else if (line.indexOf("\"bidiTextDirection\":\"contextual\"") > 0) {
                    bidiType = "contextual";
                }
                
                hasBidi = "has:{'dojo-bidi': true, 'adminCenter-bidi-type': '" + bidiType + "', 'adminCenter-bidi': true}"; 
                
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