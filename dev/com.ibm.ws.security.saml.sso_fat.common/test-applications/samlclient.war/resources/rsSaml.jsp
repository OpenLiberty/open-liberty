 <!--
    Copyright (c) 2020 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    
    import="java.net.URL"
    import="java.net.HttpURLConnection"
    import="java.io.OutputStreamWriter"
    import="java.io.InputStream"
    import="java.io.InputStreamReader"
    import="java.util.Random"
    import="java.net.URLDecoder"
%>

<%!
    // helper method for creating random state string
    String generateRandomState() {
        // random 20 character state
        StringBuffer sb = new StringBuffer();
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random r = new Random();
        for (int i = 0; i < 20; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    } 
%>
<%
    // mode constants used to decide which part of the flow we are in
    final String MODE_AUTO = "auto";
    final String MODE_MANUAL = "manual";

    String mode = MODE_MANUAL;
    
    if ("true".equals(request.getParameter("auto"))) {
        mode = MODE_AUTO;
    } 
    // https://localhost:8020/samlclient/ProtectedServlet?auto=true&http_method=POST&jaxrs_url=https://localhost:8020/samlclient/SimpleServlet&header_name=Authorization&test_case=testCase1&param_1=param1Value&param_2=parm2Value
    // generate random state
    String state = generateRandomState();

    String urlBase = "https://localhost:8020";

    String httpMethod ="POST";
    String formAction = urlBase + "/samlclient/rsSaml.jsp";
    String jaxrsUrl = urlBase + "/samlclient/SimpleServlet";
    String headerName = "Authorization";
    String headerContent = "THIS_IS_SUPPOSED_THE_SAML_TOKEN(" + state + ")";
    String testCase = "testCase1";
    String param1 = "param1Value";
    String param2 = "parm2Value";
    if (mode.equals(MODE_AUTO)) {
        httpMethod = request.getParameter("http_method");
        jaxrsUrl = request.getParameter("jaxrs_url");
        if( !jaxrsUrl.startsWith("http")){
        	jaxrsUrl =  urlBase + "/samlclient/" + jaxrsUrl;
        }
        headerName = request.getParameter("header_name");
        String headerContentEncoded = request.getParameter("header_content");
        headerContent = headerContentEncoded;
        //// headerContent is URLEncoder and org.opensaml.xml.util.Base64 encoded
        //try{
        //	// It's still org.opensaml.xml.util.Base64 encoded 
        //    headerContent = URLDecoder.decode(headerContentEncoded, "UTF8");       	
        //} catch( Exception e ){
        //	e.printStackTrace();
        //}

        testCase = request.getParameter("test_case");
        param1 = request.getParameter("param_1");
        param2 = request.getParameter("param_2");

        String content = "\nContent:\n  httpMethod:" + httpMethod + 
                       "\n  jaxrsUrl:"  +  jaxrsUrl  + 
                       "\n  headerName:"  +  headerName  +
                       "\n  headerContent:"  +  headerContent  +
                       "\n  testCase:"  +  testCase  +
                       "\n  param1:"  +  param1  +  
                       "\n  param2:"  +  param2;
        System.out.println(content);

        URL urlResource = new URL(jaxrsUrl);
        HttpURLConnection connResource = (HttpURLConnection) urlResource.openConnection();
        connResource.setRequestMethod(httpMethod);
        connResource.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        connResource.setRequestProperty(headerName, headerContent);
        connResource.setDoOutput(true);
        OutputStreamWriter wrResource = new OutputStreamWriter(connResource.getOutputStream());
        wrResource.write(content);
        wrResource.flush();
        wrResource.close();


        // read response
        connResource.connect();
        InputStream streamResource = null;
        int responseCodeResource = connResource.getResponseCode();
        if (responseCodeResource >= 200 && responseCodeResource < 400) {
            streamResource = connResource.getInputStream();
        } else {
            streamResource = connResource.getErrorStream();
        }
        final char[] bufferResource = new char[1024];
        StringBuffer sbResource = new StringBuffer();
        InputStreamReader srResource = new InputStreamReader(streamResource, "UTF-8");

        int readResource;
        do {
            readResource = srResource.read(bufferResource, 0, bufferResource.length);
            if (readResource > 0) {
                sbResource.append(bufferResource, 0, readResource);
            }
        } while (readResource >= 0);
        srResource.close();

        String responseContent = "\n" + new String(sbResource.toString().trim());

%>
<html>
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<meta http-equiv="Pragma" content="no-cache">
<title>JAXRS SAML Response</title>
</head>
<body>
<h1>JAXRS SAML Response</h1>
<form name="authform" id="authform" method="GET" action="<%=formAction%>">
<table width=800>
<tr><td>responseContent</td><td>
  <textarea name="response_content" rows="200" cols="100" >value=<%=responseContent%></textarea>                            
</td></tr>
<tr><td colspan="2"><center><button type="submit" name="processAzn" style="width:100%">Process Authorization</button></center></td></tr>
</table>  
</form>


<%
    } else
    if( mode.equals( MODE_MANUAL)  ) {


%>

<html>
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<meta http-equiv="Pragma" content="no-cache">
<title>JAXRS SAML Manual</title>
</head>
<body>
<h1>JAXRS SAML manual</h1>
<form name="authform" id="authform" method="GET" action="<%=formAction%>">
<input type="hidden" name="auto" value="true" />
<table width=800>
<tr><td>httpMethod</td><td><input type="text" name="http_method" value="<%=httpMethod%>" /></td></tr>
<tr><td>jaxrsUrl</td><td><input type="text" name="jaxrs_url" value="<%=jaxrsUrl%>" /></td></tr>
<tr><td>headerName</td><td><input type="text" name="header_name" value="<%=headerName%>" /></td></tr>
<tr><td>headerContent</td><td><input type="text" name="header_content" value="<%=headerContent%>" size="3072" /></td></tr>
<tr><td>testCase</td><td><input type="text" name="test_case" value="<%=testCase%>" size="60" /></td></tr>
<tr><td>param1</td><td><input type="text" name="param_1" value="<%=param1%>" size="60" /></td></tr>
<tr><td>param2</td><td><input type="text" name="param_2" value="<%=param2%>" size="60" /></td></tr>
<tr><td colspan="2"><center><button type="submit" name="processAzn" style="width:100%">Process Authorization</button></center></td></tr>
</table>  
</form>
<%
    } 
%>

</body>
</html>
