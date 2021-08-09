/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.ComplexClientTest.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.NewCookie;

import com.ibm.ws.jaxrs20.client.JAXRSClientConstants;


/**
 *
 */
@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;

    private static final String moduleName = "complexclient";
    
    // The changes on these 2 variables are not thread safe
    private static NewCookie lastSavedCookie = null;
    private static int lastSavedStatus = 0;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter pw = resp.getWriter();

        String testMethod = req.getParameter("test");
        System.out.println("testMethod is:" + testMethod);
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }

        runTest(testMethod, pw, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        this.doGet(req, resp);
    }

    
    private void runTest(String testMethod, PrintWriter pw, HttpServletRequest req, HttpServletResponse resp) {
        try {
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = (String) itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            m.put("protocal", req.isSecure() ? "https" : "http");
            m.put("serverName", req.getLocalName() ); //.getLocalAddr());
            m.put("serverPort", String.valueOf(req.getLocalPort()));
            
            setBoolean(m,req, "save", "false");
            setBoolean(m,req, "setLastCookie", "false");
            setBoolean(m,req, "accessToken", "false");
            setBoolean(m,req, "jwtToken", "false");

            System.out.print("Before invoking " + testMethod );
            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());
            System.out.print("After invoked " + testMethod );
            System.out.print("response:" + ret.toString() );

        } catch (Exception e) {
            e.printStackTrace(pw);
        }
    }


    // test added by OAuthHandler
    public void testClientOAuthHandler(Map<String, String> param, StringBuilder ret) {
        String serverName = param.get("serverName");
        String serverPort = param.get("serverPort");
        String http = param.get("protocal");
        
        // **This is not thread safe
        boolean bSave = getBoolean(param,"save");
        boolean bSetLastCookie = getBoolean(param,"setLastCookie");
        boolean bAccessToken = getBoolean(param,"accessToken");
        boolean bJwtToken = getBoolean(param,"jwtToken");
        if( bSave && ! bSetLastCookie){
        	lastSavedStatus = 0;
        	lastSavedCookie = null;
        }
        if( !(bAccessToken || bJwtToken)){
        	bAccessToken = true;
        }

        if( serverName.equals("127.0.0.1")){
        	System.out.println("servername:" + serverName);
        	serverName = "localhost";
        }
        System.out.println( http + "://" + serverName + ":" + serverPort );
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.ssl.config", "goodSSLConfig");  // this needs to be set as the default SslConfig
        cb.property("com.ibm.ws.jaxrs.client.disableCNCheck", true ); // Disable the hostname checking. 
                                                                      // this should not be used in a product environment.
        if( bAccessToken ){
            cb.property(JAXRSClientConstants.OAUTH_HANDLER, true); // "com.ibm.ws.jaxrs.client.oauth.sendToken", true);
        }
        if( bJwtToken ){
            cb.property( JAXRSClientConstants.JWT_HANDLER, true); //"com.ibm.ws.jaxrs.client.oidc.sendJwtToken", true );  
        }
        
        Client c = cb.build();

        WebTarget t = c.target(http + "://" + serverName + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        System.out.println("WebTarget:" + t);
        if( bSetLastCookie && lastSavedCookie == null){
        	String errorMsg1 = "\nERROR: setLastCookie is true but the lastSavedCookie is null. Will not setLastCookie.\n";
        	ret.append( errorMsg1);
        	System.out.println(errorMsg1);
        	bSetLastCookie = false;
        }
        Response response = bSetLastCookie ? t.path("echo2").path("testClientOAuthHandler").request().cookie(lastSavedCookie).buildGet().invoke()
        		: t.path("echo2").path("testClientOAuthHandler").request().buildGet().invoke();
        String res = response.readEntity(String.class);
        res = res.trim();
        ret.append(res);
        
        
        Map<String,NewCookie> cookies = response.getCookies(); 
        Set<Entry<String,NewCookie>> entries = cookies.entrySet();
        ret.append("\n\n");
        ret.append("Response status code:" + response.getStatus() );
        // this is not thread safe
        if( bSave) {
        	lastSavedStatus = response.getStatus();
        }
        ret.append( "\n" );
        int iCookieCount = 1;
        for(Entry<String,NewCookie> entry : entries){
        	String key = entry.getKey();
        	NewCookie cookie = entry.getValue();
        	if(bSave){
        		lastSavedCookie = cookie;
        	}
        	ret.append("Cookie(" + iCookieCount + ") name:" + key + " value:" + cookie.getValue() + " cookie:" + cookie.toString()+"\n");
        	iCookieCount ++;
        }
        if( iCookieCount <= 1 ){
        	ret.append( "No Cookies found in the response\n");
        }
                
        /* This will prevent when the token expires and the oidc client is tryin to redirect to the OP for a new authentication 
        if(res.startsWith("<") && res.endsWith(">")){
        	try{
        		res = res.replaceAll("<", "{");
        		res = res.replaceAll(">", "}");
            	res = "<html><body><div>" + res + "</div></body></html>";        		
        	} catch( Exception e){
        		res = "GotException " + e;
        		e.printStackTrace();
        	}
        }
        */
        /* Example code for calling POST instead of get.
         * The code had been merged into this class, yet
            WebTarget myResource = client.target(appToCall);
					Form form = new Form();
					form.param("access_token", access_token);
					form.param("targetApp", appToCall);
					form.param("where", where);
					form.param("tokenContent", tokenContent);
					form.param("contextSet", contextSet);
					//				Form form = new Form() ;
					//				form.param("access_token", access_token) ;
					//				System.out.println("Adding: access_token=" + access_token + "   as a parm") ;
					//				form.param("targetApp", appToCall) ;
					//				System.out.println("Adding: targetApp=" + appToCall + "   as a parm - for parm testing of servlet output") ;
					localResponse = (String) myResource.request(MediaType.TEXT_PLAIN).post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);        
         */
        System.out.println("WebTarget response:" + res);
        c.close();

    }

    
	protected static TrustManager[] trustAllCerts = null;
	static{
		// Not needed any more
		// Unless you want to enable SSL debug
		// setupSSLClient();
	}
	

	/**
	 * Perform setup for testing with SSL connections: TrustManager, hostname
	 * verifier, ...
	 */
	private static void setupSSLClient() {
	    String JKS_LOCATION = "/resources/security/commonSslClientDefault.jks";
	    String protocols = "SSLv3,TLSv1";
        if (!System.getProperty("java.specification.version").startsWith("1.7"))
            protocols += ",TLSv1.1,TLSv1.2";
        System.setProperty("com.ibm.jsse2.disableSSLv3", "false");
        System.setProperty("https.protocols", protocols);
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        System.out.println("Enabled SSLv3. https.protocls=" + protocols);
		
		try {
			KeyManager keyManagers[] = null;

			// if the System.Properties already set up the keystore, initialize
			// it
			String ksPath = System.getProperty("javax.net.ssl.keyStore");
			if (ksPath != null && ksPath.length() > 0) {
				String ksPassword = System
						.getProperty("javax.net.ssl.keyStorePassword");
				String ksType = System
						.getProperty("javax.net.ssl.keyStoreType");
				System.out.println("setup Keymanager: " +
						"ksPath=" + ksPath + " ksPassword=" + ksPassword + " ksType=" + ksType);	
                if( ksPassword != null && ksType != null ){
    				KeyManagerFactory kmFactory = KeyManagerFactory
    						.getInstance(KeyManagerFactory.getDefaultAlgorithm());

    				File ksFile = new File(ksPath);
    				KeyStore keyStore = KeyStore.getInstance(ksType);
    				FileInputStream ksStream = new FileInputStream(ksFile);
    				keyStore.load(ksStream, ksPassword.toCharArray());

    				kmFactory.init(keyStore, ksPassword.toCharArray());
    				keyManagers = kmFactory.getKeyManagers();                	
                }
			}

			// Create a trust manager that does not validate certificate chains
			/* */
			trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs,
						String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs,
						String authType) {
				}
			} };

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			@SuppressWarnings("unused")
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            String userDir = System.getProperty("user.dir");
            System.out.println("user.dir:" + userDir );
            String jksPath = userDir.concat("/resources/security/commonSslClientDefault.jks");
			/* setup jdk ssl */
			System.out.println( "Setting trustStore to "
					+ jksPath);
			System.setProperty("javax.net.ssl.trustStore",
					jksPath);
			System.setProperty("javax.net.ssl.trustStorePassword",
			// "changeit");
					"LibertyClient");
			System.setProperty("javax.net.debug", "ssl");
			System.out.println( "javax.net.debug is set to: "
					+ System.getProperty("javax.net.debug"));

		} catch (Exception e) {
			System.out.println( "static initializer: " +
					"Unable to set default TrustManager:" + e);
			throw new RuntimeException("Unable to set default TrustManager", e);
		} finally {
            System.setProperty("javax.net.ssl.keyStore",""); // reset the System property to empty string on keyStore settings for next test suite
		}

	}
	
	boolean getBoolean( Map<String, String> param, String key){
        String save = param.get(key);
        if( save == null) save = "false";
        else save = save.toLowerCase();
        boolean bSave = save.equals("true");
        System.out.println("getBoolean " + key + ":" + bSave);
        return bSave;
	}
	
	void setBoolean( Map<String, String> props, HttpServletRequest req, String key, String defaultValue){
        String save = req.getParameter(key);
        if( save == null) save = defaultValue;
        if( save == null) save = "false";
        else save = save.toLowerCase();
        props.put( key, save);
	}
}
