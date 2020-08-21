/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.audit.fat.common.tooling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class AuditCommonTest {

	
	public AuditCommonTest(LibertyServer server, Class<?> logClass) {
	    this.server = server;
	    this.logClass = logClass;
	}

	// Values to be set by the child class
    protected LibertyServer server;
    protected Class<?> logClass;
    protected String auditLogFilePath = null;

    protected String getCurrentTestName() {
        return "Test name not set";
    }
   
   
    // Constants for tests
    public static final String GET_REMOTE_USER = "getRemoteUser: ";
    public static final String GET_USER_PRINCIPAL = "getUserPrincipal: WSPrincipal:";
    
    public static final String AUDIT_BASICAUTH_PROTECTED_SERVLET_URI = "/AuditBasicAuthServlet/AuditBasic";
    public static final String AUDIT_BASICAUTH_SERVLET = "AuditBasicAuthServlet";
    public static final String REGISTRY_BASICREALM = "BasicRealm";
    
    // Standard audit values
    public static final String STANDARD_OBSERVER_TYPEURI_VALUE = "/service/server";
    public static final String STANDARD_TARGET_TYPEURI_VALUE = "service/application/web";
    public static final String SERVICE_AUDIT_START = "service/audit/start";
    public static final String SERVICE_AUDIT_STOP = "service/audit/stop";
    public static final String SERVICE_AUDIT_SERVICE = "AuditService";
    public static final String HANDLER_DEFAULT_FILEHANDLER = "AuditHandler:AuditFileHandler";
    
    // Standard audit values for JMX
    public static final String JMX_SERVICE = "JMXService";
    public static final String STANDARD_JMX_MBEAN_TARGET_TYPE_URI = "server/mbean";
    public static final String STANDARD_JMX_NOTIF_TARGET_TYPE_URI = "server/mbean/notification";
    public static final String STANDARD_JMX_OBSERVER_TYPE_URI = "service/server";
    
    // Standard audit values for JMS Messaging
    public static final String STANDARD_JMS_ENGINE_TYPE_URI = "service/jms/messagingEngine";
    public static final String STANDARD_JMS_RESOURCE_TYPE_URI = "service/jms/messagingResource";
    public static final String STANDARD_JMS_OBSERVER_NAME = "JMSMessagingImplementation";
    public static final String TARGET_JMS_DEFAULT_ENGINE = "defaultME";
    public static final String TARGET_JMS_DEFAULT_BUS = "defaultBus";
    public static final String TARGET_JMS_AUTHN_TYPE = "Userid+Password";
    
    //Standard audit values for OIDC 
    public static final String STANDARD_OIDC_TYPEURI_VALUE = "service/oidc";
    public static final String STANDARD_OIDC_OBSERVER_NAME = "OidcSecurityService";
    public static final String STANDARD_OIDC_APP_PASSWORDS_ENDPOINT = "app-passwords";
    public static final String STANDARD_OIDC_APP_TOKENS_ENDPOINT = "app-tokens";
    public static final String STANDARD_OIDC_APP_PASSWORDS_URI = "/oidc/endpoint/OidcConfigSample/app-passwords";
    public static final String STANDARD_OIDC_APP_PASSWORDS_COPY_URI = "/oidc/endpoint/OidcConfigSample_copy/app-passwords";
    public static final String STANDARD_OIDC_APP_TOKENS_URI = "/oidc/endpoint/OidcConfigSample/app-tokens";
    public static final String STANDARD_OAUTH_PROVIDER = "OAuthConfigSample";
    
    public static final String DEFAULT_AUDIT_LOG = "audit.log";
    
    protected static String ENCRYPTION_PUBLIC_KEY_ALIAS = "enccert";
    protected static String ENCRYPTION_PUBLIC_KEY_ALIAS2 = "enccert2";
    protected static String ENCRYPTION_PUBLIC_KEY_ALIAS_ONLY = "encrsigner";
    protected static String ENCRYPTION_KEYSTORE_PRIVATE_SIGNING_CERT_ALIAS = "auditsigning";
    protected static String ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS = "auditencryption";
    protected static String ENCRYPTION_KEYSTORE = "AuditEncryptionKeyStore.jks";
    protected static String ENCRYPTION_PUBLICKEY_KEYSTORE = "AuditEncryptionPublicKeyKeystore.jks";
    protected static String ENCRYPTION_KEYSTORE2 = "AuditEncryptionKeyStore2.jks";
    
    protected static String ENCRYPTION_CERTIFICATE_RSA = "RSA";
    protected static String ENCRYPTION_CERTIFICATE_EC = "EC";
    protected static String DEFAULT_KEYSTORE = "key.jks";
    protected static String DEFAULT_KEYSTORE_CERT_ALIAS = "default";
    protected static String PASSWORD_PROTECTED_CERT_KEYSTORE = "AuditCertPasswordKeystore.jks";
    protected static String PASSWORD_PROTECTED_CERT_ALIAS = "passprotect";
    
    protected static String SIGNING_PRIVATE_KEY_ALIAS = "auditsigning";
    protected static String SIGNING_PRIVATE_KEY_ALIAS2 = "auditsigning2";
    protected static String SIGNING_PUBLIC_KEY_ALIAS = "signcert";
    protected static String SIGNING_PUBLIC_KEY_ALIAS2 = "signcert2";
    protected static String SIGNING_KEYSTORE = "AuditSigningKeyStore.jks";
    protected static String SIGNING_KEYSTORE2 = "AuditSigningKeyStore2.jks";
    protected static String SIGNING_CERTIFICATE_RSA = "RSA";
    protected static String SIGNING_CERTIFICATE_EC = "EC";
    
    protected static String ENCRYPTION_SECTION ="EncryptionInformation";
    protected static String SIGNING_SECTION ="SigningInformation";
    
    protected static String HEADER_ENCRYPTED_SHARED_KEY = "encryptedSharedKey";
    protected static String HEADER_ENCRYPTION_CERT_ALIAS = "encryptionCertAlias";
    protected static String HEADER_ENCRYPTION_KEYSTORE = "encryptionKeyStore";
    protected static String HEADER_ENCRYPTION_CERTIFICATE = "encryptionCertificate";
    
    protected static String HEADER_SIGNING_SHARED_KEY = "signingSharedKey";
    protected static String HEADER_SIGNING_CERT_ALIAS = "signingCertAlias";
    protected static String HEADER_SIGNING_KEYSTORE = "signingKeyStore";
    protected static String HEADER_SIGNING_CERTIFICATE = "signingCertificate";
    
    protected static String AUDIT_RECORD = "auditRecord";
    
    protected static boolean FULL_RESPONSE_OUTPUT = true;
    protected static boolean MINIMAL_RESPONSE_OUTPUT = false;
    
    public static void verifyAuditAndAuditFileHandlerReady(LibertyServer server) {
        assertNotNull("Audit service did not report it was ready",
             server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY));
        assertNotNull("Audit file handler service did not report it was ready",
    		server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY));
    }
    
    public static void verifyServerStartedWithAuditFeature(LibertyServer server) {
        assertNotNull("FeatureManager did not report that feature update was complete",
            server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKF0008I_FEATURE_UPDATE_COMPLETE));
        assertNotNull("FeatureManager did not report the audit feature was installed",
            server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKF0012I_AUDIT_FEATURE_INSTALLED));
        verifyAuditAndAuditFileHandlerReady(server);
    }
    
    public static void verifyServerStartedWithAuditFeatureAndSecurityService(LibertyServer server) {
        verifyServerStartedWithAuditFeature(server);
        verifySecurityServiceReady(server);
    }
    
    public static void verifySecurityServiceReady(LibertyServer server) {
        verifyAuditAndAuditFileHandlerReady(server);
        assertNotNull("Security service did not report it was ready",
                server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKS0008I_SECURITY_SERVICE_READY));
    }
    
    public void verifyProgrammaticAPIValues(String loginUser, String test3, String authType) {
        // Verify programmatic APIs
        assertTrue("Failed to find expected getAuthType: " + loginUser, test3.contains("getAuthType: " + authType));
        assertTrue("Failed to find expected getRemoteUser: " + loginUser, test3.contains("getRemoteUser: " + loginUser));
        assertTrue("Failed to find expected getUserPrincipal: " + loginUser, test3.contains("getUserPrincipal: " + "WSPrincipal:" + loginUser));
    }   
    
    public String executeGetRequestBasicAuthCreds(DefaultHttpClient httpClient, String url, String userid, String password, int expectedStatusCode) throws Exception {

   	 return executeGetRequestBasicAuthCreds(httpClient,url,userid,password,expectedStatusCode,FULL_RESPONSE_OUTPUT);

   }
    
    public String executeGetRequestBasicAuthCreds(DefaultHttpClient httpClient, String url, String userid, String password, int expectedStatusCode, boolean fullOutput) throws Exception {

        String methodName = "executeGetRequestBasicAuthCreds";
        Log.info(logClass, getCurrentTestName(), "Servlet url: " + url + " userid: " + userid + ", password: " + password + ", expectedStatusCode=" + expectedStatusCode
                                                 + " , method=" + methodName);
        HttpGet getMethod = new HttpGet(url);
        if (userid != null)
            httpClient.getCredentialsProvider().setCredentials(new AuthScope("localhost", AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                                                               new UsernamePasswordCredentials(userid, password));
        HttpResponse response = httpClient.execute(getMethod);
        Log.info(logClass, methodName, "Actual response: " + response.toString());

        if (fullOutput)
       	 return processResponse(response, expectedStatusCode,FULL_RESPONSE_OUTPUT);
        else
       	 return processResponse(response, expectedStatusCode,MINIMAL_RESPONSE_OUTPUT);

    }
    
    public String executePostRequestBasicAuthCreds(DefaultHttpClient httpClient, String url, String userid, String password, int expectedStatusCode) throws Exception {

        String methodName = "executePostRequestBasicAuthCreds";
        Log.info(logClass, getCurrentTestName(), "Servlet url: " + url + " userid: " + userid + ", password: " + password + ", expectedStatusCode=" + expectedStatusCode
                                                 + " , method=" + methodName);
        HttpPost postMethod = new HttpPost(url);
        if (userid != null)
            httpClient.getCredentialsProvider().setCredentials(new AuthScope("localhost", AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                                                               new UsernamePasswordCredentials(userid, password));
        HttpResponse response = httpClient.execute(postMethod);
        Log.info(logClass, methodName, "Actual response: " + response.toString());

        return processResponse(response, expectedStatusCode);

    }
    
    public String executePutRequestBasicAuthCreds(DefaultHttpClient httpClient, String url, String userid, String password, int expectedStatusCode) throws Exception {

        String methodName = "executePostRequestBasicAuthCreds";
        Log.info(logClass, getCurrentTestName(), "Servlet url: " + url + " userid: " + userid + ", password: " + password + ", expectedStatusCode=" + expectedStatusCode
                                                 + " , method=" + methodName);
        HttpPut postMethod = new HttpPut(url);
        if (userid != null)
            httpClient.getCredentialsProvider().setCredentials(new AuthScope("localhost", AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                                                               new UsernamePasswordCredentials(userid, password));
        HttpResponse response = httpClient.execute(postMethod);
        Log.info(logClass, methodName, "Actual response: " + response.toString());

        return processResponse(response, expectedStatusCode);

    }   
    
    public String executeDeleteRequestBasicAuthCreds(DefaultHttpClient httpClient, String url, String userid, String password, int expectedStatusCode) throws Exception {

        String methodName = "executePostRequestBasicAuthCreds";
        Log.info(logClass, getCurrentTestName(), "Servlet url: " + url + " userid: " + userid + ", password: " + password + ", expectedStatusCode=" + expectedStatusCode
                                                 + " , method=" + methodName);
        HttpDelete postMethod = new HttpDelete(url);
        if (userid != null)
            httpClient.getCredentialsProvider().setCredentials(new AuthScope("localhost", AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                                                               new UsernamePasswordCredentials(userid, password));
        HttpResponse response = httpClient.execute(postMethod);
        Log.info(logClass, methodName, "Actual response: " + response.toString());

        return processResponse(response, expectedStatusCode);

    }       
    
  
    public String executeGetRequestNoAuthCreds(DefaultHttpClient httpClient, String url, int expectedStatusCode) throws Exception {
        return executeGetRequestBasicAuthCreds(httpClient, url, null, null, expectedStatusCode);
    }
    
    private void mustContain(String response, String target) {
        assertTrue("Expected result " + target + " not found in response", response.contains(target));
    }
    
    public void verifyUserResponse(String response, String getUserPrincipal, String getRemoteUser) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + getUserPrincipal + ", " + getRemoteUser);
        mustContain(response, getUserPrincipal);
        mustContain(response, getRemoteUser);
    }
    
    /**
     * Process the response from an http invocation, such as validating
     * the status code, extracting the response entity. Process with either full
     * response content to test log, or reduced output.
     * 
     * @param response the HttpResponse
     * @param expectedStatusCode
     * @return The response entity text, or null if request failed
     * @throws IOException
     */
    public String processResponse(HttpResponse response,
            int expectedStatusCode, boolean fullResponseOutput) throws IOException {
		String methodName = "processResponse";
		
		Log.info(logClass, methodName, "getMethod status: " + response.getStatusLine());
		HttpEntity entity = response.getEntity();
		String content = EntityUtils.toString(entity);
		if (fullResponseOutput)
			Log.info(logClass, methodName, "Servlet full response content: \n" + content);
		EntityUtils.consume(entity);
		
		assertEquals("Expected " + expectedStatusCode + " was not returned",
		expectedStatusCode, response.getStatusLine().getStatusCode());
		
		return content;
}
    
    /**
     * Process the response from an http invocation, such as validating
     * the status code, extracting the response entity, and logging full response output to test output.log
     * 
     * @param response the HttpResponse
     * @param expectedStatusCode
     * @return The response entity text, or null if request failed
     * @throws IOException
     */
    public String processResponse(HttpResponse response,
                                     int expectedStatusCode) throws IOException {
        return processResponse(response, expectedStatusCode, FULL_RESPONSE_OUTPUT);
    }
   
    /**
     * Scans the server.xml file for the given string and returns true if the string is contained anywhere in the
     * config. If the keytab attribute is specified as the string to find and is found, its corresponding value is
     * recorded as the configured keytab file path.
     * 
     * @param findString
     * @return
     * @throws Exception
     */
    protected boolean checkServerXMLForLogLocation(String findString) throws Exception {
        String methodName = "checkServerXMLForLogLocation";
        String serverConfig = server.getServerRoot() + "/server.xml";
        boolean stringFound = false;

        try {
            Log.info(logClass, methodName, "Checking for \"" + findString + "\" in server config:" + serverConfig);
            File inp = new File(serverConfig);
            InputStreamReader inputStream = new InputStreamReader(new FileInputStream(inp));
            BufferedReader dataStream = new BufferedReader(inputStream);

            String line = null;
            while ((line = dataStream.readLine()) != null) {
                if (line.contains(findString)) {
                    stringFound = true;
                    Log.info(logClass, methodName, "Found \"" + findString + "\" in server.xml: " + line.trim());
                    if (findString.equals("logDirectory")) {
                        Log.info(logClass, methodName, "logDirectory attribute found; it will be used as the log file path in tests");
                        setLogFilePath(line);
                    }
                    break;
                }
            }
            dataStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return stringFound;
    }

    /**
     * Records the audit log file path based on the string provided. If line contains the ${server.config.dir} variable,
     * the variable is replaced with the server root path before extracting the audit log location.
     * 
     * @param line - String expected to contain the audit log file location in property=value format. If the value portion
     *            contains the ${server.config.dir} variable, it is replaced with the server root path. The value
     *            portion is then set as the audit log file path.
     * @throws Exception
     */
    private void setLogFilePath(String line) throws Exception {
        String methodName = "setLogFilePath";
        // Strip off anything before the actual logDirectory attribute
        String tempLocation = line.substring(line.indexOf("logDirectory"));
        String serverConfigVariable = "${server.config.dir}";

        if (tempLocation.contains(serverConfigVariable)) {
            // Substitute the server root for all occurrences of the server config variable
            String serverConfigVariableRegex = "\\$\\{server.config.dir\\}";
            tempLocation = tempLocation.replaceAll(serverConfigVariableRegex, server.getServerRoot());
            Log.info(logClass, methodName, "Audit logFile location after server root variable substitution: " + tempLocation);
        }

        int locationStartIndex = tempLocation.indexOf("=") + 1;
        int locationEndIndex = tempLocation.length();
        if (tempLocation.charAt(locationStartIndex) == '"') {
            // Attribute is enclosed in quotes; find the index of the closing quote
            locationEndIndex = tempLocation.indexOf('"', locationStartIndex + 1);
        }

        auditLogFilePath = tempLocation.substring(locationStartIndex + 1, locationEndIndex);
        Log.info(logClass, methodName, "Audit logFile location set to: " + auditLogFilePath);
        if (auditLogFilePath == null) {
            throw new RemoteException("Failed to get Audit logFile location");
        }
    }  
    
    public String getAuditLogFilePath() {
        return auditLogFilePath;
    }
    
    /*
     * Helper method to make custom method call
     */
    public String httpCustomMethodResponse(String urlLink, String httpMethod, boolean secure, int port, String user, String password) throws Exception {
        URL url = null;
        try {
            url = new URL(urlLink);
        } catch (MalformedURLException e) {
            return "Invalid URL " + urlLink;
        }

        Socket socket = null;

        if (urlLink.indexOf("https") > -1) {
            SocketFactory socketFactory = SSLSocketFactory.getDefault();
            socket = socketFactory.createSocket(url.getHost(), url.getPort());
        } else {
            socket = new Socket(url.getHost(), url.getPort());
        }

        // Send header
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        wr.write(httpMethod + " " + url.getPath() + " HTTP/1.0\r\n");
        //wr.write("GET" + " " + url.getPath() + " HTTP/1.0\r\n");  // try doGET

        if (secure) {
            byte[] encoding = Base64.encodeBase64((user + ":" + password).getBytes());

            String encodedStr = new String(encoding);
            wr.write("Authorization: Basic " + encodedStr + "\r\n");
        }
        wr.write("\r\n");
        wr.flush();

        // Get response

        BufferedReader d = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        StringBuffer responseString1 = new StringBuffer();
        String line1 = null;
        try {
            while ((line1 = d.readLine()) != null) {
                if (!(line1.isEmpty())) {
                    line1.concat(line1.trim());
                    responseString1.append(line1);
                }
            }
        } catch (IOException e) {
            throw new Exception("Failed to access the URL " + urlLink + " with message " + e.getMessage());
        }

        return responseString1.toString();
    }    
    /**
     * Returns a list of audit log files present in the audit log directory for the given Liberty server.
     * 
     * @param server - Liberty server for which the list of audit logs is to be returned.
     * @throws IOException
     */
    public List<File> returnListOfAuditLogs(LibertyServer server) throws IOException {
     	List<File> list = new ArrayList<File>();

             java.nio.file.Path dir = java.nio.file.Paths.get(server.getLogsRoot());
             try {
                java.nio.file.DirectoryStream<java.nio.file.Path> dirStream = java.nio.file.Files.newDirectoryStream(dir);
                for (java.nio.file.Path entry: dirStream) {
    
                    if (entry.getFileName().toString().startsWith("audit")) {
                        list.add(entry.toFile());
                    }
                }
                dirStream.close();
             } catch (java.nio.file.DirectoryIteratorException ex) {
                        // I/O error encountered during the iteration, the cause is an IOException
                        throw ex.getCause();
             }
         return list;
     }  
    
	/**
	 * Reads lines from the header section of the secure (encrypted and/or signed) audit log until the section end for the header.
	 * @return the list of strings from the header if section is found; return null if not found
	 * @throws IOException
	 */
	public static List<String> readSecureLogHeader(String logName, String section) throws IOException {

			List<String> results = new ArrayList<String>();
			BufferedReader br = new BufferedReader(new FileReader(logName));
			String line;
			StringBuilder buildLine = new StringBuilder();
			String sectionStart = "<" + section;
			String sectionEnd = "</" + section;
			boolean hasXMLStart = false;
			System.out.println("Searching for section " + section + " in audit log header.");
		  try { 
		      while ((line = br.readLine()) != null) {

		    	  if (line.contains(sectionStart))
		    		  hasXMLStart = true;
		    	  
		    	  if (hasXMLStart)
		        	  results.add(line);
		 
		          if (line.contains(sectionEnd)) {
		        	  System.out.println("Found section " + section + " contents: " + results.toString());
		    	      return results;
		          }

		      } 
		      System.out.println("Section " + section + " not found.");
	          return null;
		  } finally {
				try {
					br.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
		  }
		}
	
	/**
	 * Reads lines from the encrypted audit log and counts the number of sections which have a specific section name.
	 * @return a Map with the count for each section name 
	 * @throws IOException
	 */
	public Map<String, Integer> countSections(String logName, String...keys) throws IOException {
		
		Map<String, Integer> results = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(logName));
		String line;
		Stack <String> s = new Stack<String>();
		
		for (String key : keys)
			results.put(key, 0);
		
		Pattern startRx = Pattern.compile("^\\s*<([a-zA-Z]+?)>.*$");
		Pattern endRx = Pattern.compile("^.*</([a-zA-Z]+?)>.*$");
		
	
	  try { 
	      while ((line = br.readLine()) != null) {

	    	  Matcher startM = startRx.matcher(line);
	    	  Matcher endM = endRx.matcher(line);
	    	  
	    	  boolean startMatches = startM.matches();
	    	  boolean endMatches = endM.matches();
	    	  
    		  String startKey = startMatches ? startM.group(1) : null;
    		  String endKey = endMatches  ? endM.group(1) : null;
    		  
	    	  if (startMatches && endMatches) {

	    		  if (!startKey.equals(endKey))
	    			  throw new RuntimeException("Mismatched section elememts: " + startKey + ", " + endKey );
	    		  if (results.containsKey(startKey)){
	    			  int current = results.get(startKey);
	    			  current++;
	    			  results.put(startKey, current);
	    		  }
 	    	  }
	    	  else if (startMatches && !endMatches) {

	    		  s.push(startKey);
	    	  }
	    	  else if (!startMatches && endMatches) {
	    		  if (!s.isEmpty()) {
	    			  String saved = s.pop();
		    		   if (saved != null && saved.equals(endKey)){
		 	    		  if (results.containsKey(endKey)){
			    			  int current = results.get(endKey);
			    			  current++;
			    			  results.put(endKey, current);
			    		  }
		    		   }
	    		  } else {
	    			  throw new RuntimeException("Mismatched section elements for: "  + endKey);
	    		  }
	    	  }
	      } 
	  } finally {
			try {
				br.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
	  }
	  if (results != null) System.out.println ("Complete records found : " + results.toString());
	  return results;
	}
    
	/**
	 * Reads lines from the header section of the audit log and capture the values for each of the keys specified.
	 * @return Map <key, value> 
	 */
	public Map<String, String> parseValues(List<String> header, String... keys) {
		Map<String, String> results = new HashMap<String, String>();
		for (String key : keys)
			results.put(key, null);
		for (String line : header) {
			if (line != null && !line.trim().isEmpty()) {
				for (String key : results.keySet()) {
					if (results.get(key) == null) {
						if (line.contains(key)) {
							String result = line.replace("<" + key + ">", "")
									.replace("</" + key + ">", "");
							results.put(key, result);
							break;
						}
					}
				}
			}
		}
		return results;
	}
	
    protected void delay(int sleepSecs) {
		try {
			Thread.sleep(sleepSecs * 1000);
		} catch (InterruptedException e) {
		}
	}
}
