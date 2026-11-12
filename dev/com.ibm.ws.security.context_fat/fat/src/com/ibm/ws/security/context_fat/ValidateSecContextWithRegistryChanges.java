/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.context_fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

@Mode(TestMode.FULL)
public class ValidateSecContextWithRegistryChanges extends CommonUtils{
	private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.context_fat");
	private static final Class<?> thisClass = ValidateSecContextWithRegistryChanges.class;
	private static final TestConfiguration testConfig = new TestConfiguration(server, appNames);
	private static String DEFAULT_CONFIG = "/config/server.xml";
	private static String CACHEKEY_CONFIG = "/config/customTAIServer.xml";
	private static String CACHEKEY_USER3_CONFIG = "/config/customTAIServerWithUser3.xml";
	

	@Rule
	public final TestWatcher logger = new TestWatcher() {
		@Override
		public void starting(Description description) {
			Log.info(thisClass, description.getMethodName(), "Entering test " + description.getMethodName());
		}

		@Override
		public void finished(Description description) {
			Log.info(thisClass, description.getMethodName(), "Exiting test " + description.getMethodName());
		}
	};

	@AfterClass
	public static void tearDown() throws Exception {
		if (server != null && server.isStarted()) {
			server.stopServer("CWWKS0800E", "CWWKG0033W");
		}
	}

	/**
	 * Serializes a secured servlet's security context for user1/role1. The serialized .ser file is written to wlp/usr/servers/xxx/*.ser.
	 * This file is renamed to contain the release level as opposed to vNext and copied to webApps.war/resources/WEB-INF/files/serialized.
	 * If anything changes with the objects that get serialized, the the *.ser file in ../files/serialized needs to be
	 * replaced with the newly generated/renamed file.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testSerializeSecuredServletSecurityContextForUser1UnderRole1() throws Exception {
		testConfig.updateServerConfig("/config/serverUser1InRegistry.xml", false);
		Map<String, String> props = new HashMap<String, String>();
		props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER1_NAME, USER1_PASSWORD));
		HttpURLConnection connection = getConnection("/webApps/secured?test=testSerializeSecuredServletSecurityContext&serFileName=securedServletSecurityContextForUser1Role1-vNext.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");
	}

	/**
	 * Tests the cache  key is serialized/deserialized.
	 * <ol>Login with user1, and hard-coded cache key is added to the subject.</ol>
	 * <ol>User1's security context is serialized to a file.</ol>
	 * <ol>Log in with user2 and confirm access is denied to the role1 method.</ol>
	 * <ol>With this user's subject on the thread, deserialize user1's security context containing the cache key.</ol> 
	 * <ol>Verify that the subject on the thread has the same hard-coded cache key, and that it has access to the role1 method, but not the role2 method.</ol> 
	 * <ol>After the securityContext is removed, validate that user2's security context is restored.</ol> 
	 */
	@Mode(TestMode.FULL)
	@Test
	public void testSerializeDeserializeSecurityContextWithCacheKey() throws Exception {
		testConfig.updateServerConfig(CACHEKEY_CONFIG, true);
		Map<String, String> props = new HashMap<String, String>();
		props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER1_NAME, USER1_PASSWORD));
		HttpURLConnection connection = getConnection("/webApps/securedWithCacheKey?test=testSerializeSecurityContextWithCacheKey&serFileName=authenticatedContextForUser1WithCacheKey-vNext.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		String serializedCacheKey = returnStringInHttpConnection(connection, "Starting cache key: ");
		Log.info(ValidateSecContextWithRegistryChanges.class, "testSerializeDeserializeSecurityContextWithCacheKey", serializedCacheKey);
		
		// Have user2 (role2) call the secured servlet that calls a secured bean method under role1. The request to the servlet
		// should be OK, but the bean method call should fail.
		Map<String, String> props2 = new HashMap<String, String>();
		props2.put("Authorization", "Basic " + encodeUserNameAndPassword(USER2_NAME, USER2_PASSWORD));
		HttpURLConnection connection2 = getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props2, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection2, "javax.ejb.EJBAccessException", "CWWKS9400A");

		// Deserialize security context, which should put user1 on the thread, check the cache key is the same, and call the bean method secured under role1.
		HttpURLConnection connection3 = getConnection("/webApps/securedWithCacheKey?test=testDeserializeSecurityContextWithCacheKey&serFileName=/WEB-INF/files/serialized/authenticatedContextForUser1WithCacheKey-8554.ser&contextAllowedRole=" + ROLE1_NAME + "&contextDeniedRole=" + ROLE2_NAME + "&originalAllowedRole=" + ROLE2_NAME + "&originalDeniedRole=" + ROLE1_NAME+ "&cacheKey="+serializedCacheKey+"&cacheMiss=false", HTTPRequestMethod.GET, null, props2, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection3, "COMPLETED SUCCESSFULLY");
		testConfig.updateServerConfig(DEFAULT_CONFIG, false);
	} 
	
	/**
	 * Tests the cache  key is serialized/deserialized, but login still succeeds when cachekey entry doesn't exist in the auth cache.
	 * <ol>Login with user1, and hard-coded cache key is added to the subject.</ol>
	 * <ol>User1's security context is serialized to a file.</ol>
	 * <ol>Add user3 to server.xml to trigger clearing of the auth cache.
	 * <ol>Log in with user2 and confirm access is denied to the role1 method.</ol>
	 * <ol>With this user's subject on the thread, deserialize user1's security context containing the cache key.</ol> 
	 * <ol>Verify that the subject on the thread has the same hard-coded cache key, and that it has access to the role1 method, but not the role2 method.</ol> 
	 * <ol>After the securityContext is removed, validate that user2's security context is restored.</ol> 
	 */
	@Mode(TestMode.FULL)
	@Test
	public void testSerializeDeserializeSecurityContextWithCacheKeyCacheMiss() throws Exception {
		testConfig.updateServerConfig(CACHEKEY_CONFIG, true);
		Map<String, String> props = new HashMap<String, String>();
		props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER1_NAME, USER1_PASSWORD));
		HttpURLConnection connection = getConnection("/webApps/securedWithCacheKey?test=testSerializeSecurityContextWithCacheKey&serFileName=authenticatedContextForUser1WithCacheKeyCacheMiss-vNext.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		String serializedCacheKey = returnStringInHttpConnection(connection, "Starting cache key: ");
		Log.info(ValidateSecContextWithRegistryChanges.class, "testSerializeDeserializeSecurityContextWithCacheKey", serializedCacheKey);
			
		//trigger auth cache cleanup
		testConfig.updateServerConfig(CACHEKEY_USER3_CONFIG, false);
		
		// Have user2 (role2) call the secured servlet that calls a secured bean method under role1. The request to the servlet
		// should be OK, but the bean method call should fail.
		Map<String, String> props2 = new HashMap<String, String>();
		props2.put("Authorization", "Basic " + encodeUserNameAndPassword(USER2_NAME, USER2_PASSWORD));
		HttpURLConnection connection2 = getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props2, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection2, "javax.ejb.EJBAccessException", "CWWKS9400A");

		// Deserialize security context, which should put user1 on the thread, check the cache key is the same, and call the bean method secured under role1.
		HttpURLConnection connection3 = getConnection("/webApps/securedWithCacheKey?test=testDeserializeSecurityContextWithCacheKey&serFileName=/WEB-INF/files/serialized/authenticatedContextForUser1WithCacheKeyCacheMiss-8554.ser&contextAllowedRole=" + ROLE1_NAME + "&contextDeniedRole=" + ROLE2_NAME + "&originalAllowedRole=" + ROLE2_NAME + "&originalDeniedRole=" + ROLE1_NAME+ "&cacheKey="+serializedCacheKey+"&cacheMiss=true", HTTPRequestMethod.GET, null, props2, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection3, "COMPLETED SUCCESSFULLY");
		testConfig.updateServerConfig(DEFAULT_CONFIG, false);
	} 
	
	/**
	 * Serializes a secured servlet's security context for user1 in group1 under role1. The serialized .ser file is written to wlp/usr/servers/xxx/*.ser.
	 * This file is renamed to contain the release level as opposed to vNext and copied to webApps.war/resources/WEB-INF/files/serialized.
	 * If anything changes with the objects that get serialized, the the *.ser file in ../files/serialized needs to be
	 * replaced with the newly generated/renamed file.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testSerializeSecuredServletSecurityContextForUser1InGroup1UnderRole1() throws Exception {
		testConfig.updateServerConfig("/config/serverUser1InGroup1AndInRegistry.xml", false);
		Map<String, String> props = new HashMap<String, String>();
		props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER1_NAME, USER1_PASSWORD));
		HttpURLConnection connection = getConnection("/webApps/secured?test=testSerializeSecuredServletSecurityContext&serFileName=securedServletSecurityContextForUser1InGroup1UnderRole1-vNext.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");
	}

	/**
	 * Tests that an unauthenticated user can call a unsecured servlet that calls a secured bean method under user1 under role1. User1 is not in the registry.
	 * The call to the secured bean method should NOT be possible after deserializing the security context containing user1's credential.
	 * 
	 */
	@Test
	@ExpectedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
	public void testSecurityContextDeserializationFromUnsecuredServletWithUserNotInRegistry() throws Exception {
		testConfig.updateServerConfig("/config/serverUser1InRegistry.xml", true);

		// Have an unknown user drive some work on an unsecured servlet.
		HttpURLConnection connection = getConnection("/webApps/unsecured?test=callUnsecuredBeanMethod", HTTPRequestMethod.GET, null, null, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");

		// Deserialize user1/role security context/object and call the bean method secured under role1. User1 is in the registry.
		HttpURLConnection connection2 = getConnection("/webApps/unsecured?test=deserializeUser1Role1CtxAndCallRole1BeanMethod&serFileName=/WEB-INF/files/serialized/securedServletSecurityContextForUser1Role1-8554.ser", HTTPRequestMethod.GET, null, null, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection2, "COMPLETED SUCCESSFULLY");

		// Remove user1 from the registry.
		testConfig.updateServerConfig("/config/serverUser1NotInRegistry.xml", false);

		// Deserialize user1/role security context/object and call the bean method secured under role1. User1 is not in the registry.
		HttpURLConnection connection3 = getConnection("/webApps/unsecured?test=deserializeUser1Role1CtxAndCallRole1BeanMethod&serFileName=/WEB-INF/files/serialized/securedServletSecurityContextForUser1Role1-8554.ser", HTTPRequestMethod.GET, null, null, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection3, "javax.ejb.EJBAccessException", "CWWKS9400A");
		assertNotNull("Message CWWKS0800E for auth failure during deserialization was not found in message log.", server.waitForStringInLogUsingMark("CWWKS0800E.*"));
	}

	/**
	 * Tests that a secured servlet call to a bean method under role1 is NOT possible after the security context for user1 under role1 is 
	 * deserialized on the calling servlet's thread, but user1 is not in the registry.
	 */
	@Test
	@ExpectedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
	public void testSecurityContextDeserializationFromSecuredServletWithUserNotInRegistry() throws Exception {
		testConfig.updateServerConfig("/config/serverUser1InRegistry.xml", false);
		Map<String, String> props = new HashMap<String, String>();

		// User1 under role1 should be able to call the secured servlet and secured bean.
		props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER1_NAME, USER1_PASSWORD));
		HttpURLConnection connection = getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");

		// Remove user1 from the registry.
		testConfig.updateServerConfig("/config/serverUser1NotInRegistry.xml", true);

		// Make sure user1 has no longer access to the secured servlet. It should fail: 401 response.
		getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_UNAUTHORIZED);        

		// Have user2 (role2) call the secured servlet that calls a secured bean method under role1. The request to the servlet
		// should be OK, but the bean method call should fail.
		props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER2_NAME, USER2_PASSWORD));
		HttpURLConnection connection2 = getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection2, "javax.ejb.EJBAccessException", "CWWKS9400A");

		// Deserialize user1/role security context/object and call the servlet method that calls the bean method secured under role1. User1 is not in the registry.
		HttpURLConnection connection3 = getConnection("/webApps/secured?test=deserializeUser1Role1CtxAndCallRole1BeanMethod&serFileName=/WEB-INF/files/serialized/securedServletSecurityContextForUser1Role1-8554.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection3, "javax.ejb.EJBAccessException", "CWWKS9400A");
		assertNotNull("Message CWWKS0800E for auth failure during deserialization was not found in message log.", server.waitForStringInLogUsingMark("CWWKS0800E.*"));
	}

	/**
	 * Tests that an unauthenticated user can call a unsecured servlet that calls a secured bean method under user1 in group1 under role1. User1 is not in the group in the registry.
	 * The call to the secured bean method should NOT be possible after deserializing the security context containing user1's credential.
	 */
	@Test
	public void testSecurityContextDeserializationFromUnsecuredServletWithUserNotInRegistryGroup() throws Exception {
		testConfig.updateServerConfig("/config/serverUser1InGroup1AndInRegistry.xml", true);

		// Have an unknown user drive some work on an unsecured servlet.
		HttpURLConnection connection = getConnection("/webApps/unsecured?test=callUnsecuredBeanMethod", HTTPRequestMethod.GET, null, null, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");

		// Deserialize the (user1 in group1 under role1) security context/object and call the bean method secured under role1. User1 is in group1 defined in the registry.
		HttpURLConnection connection2 = getConnection("/webApps/unsecured?test=deserializeUser1Role1CtxAndCallRole1BeanMethod&serFileName=/WEB-INF/files/serialized/securedServletSecurityContextForUser1InGroup1UnderRole1-8554.ser", HTTPRequestMethod.GET, null, null, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection2, "COMPLETED SUCCESSFULLY");

		// Remove user1 from group1. Group 1 still in the registry.
		testConfig.updateServerConfig("/config/serverUser1NotInRegistryGroup1.xml", false);

		// Deserialize the (user1 in group1 under role1) security context/object and call the bean method secured under role1. User1 is not in the registry.
		HttpURLConnection connection3 = getConnection("/webApps/unsecured?test=deserializeUser1Role1CtxAndCallRole1BeanMethod&serFileName=/WEB-INF/files/serialized/securedServletSecurityContextForUser1InGroup1UnderRole1-8554.ser", HTTPRequestMethod.GET, null, null, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection3, "javax.ejb.EJBAccessException", "CWWKS9400A");
	}

	/**
	 * Tests that a secured servlet call to a bean method under role1 is NOT possible after the security context for user1 in group1 under role1 is 
	 * deserialized on the calling servlet's thread.
	 */
	@Test
	public void testSecurityContextDeserializatioFromSecuredServletWithUserNotInRegistryGroup() throws Exception {
		testConfig.updateServerConfig("/config/serverUser1InGroup1AndInRegistry.xml", false);
		Map<String, String> props = new HashMap<String, String>();

		// User1 in group1 under role1 should be able to call the secured servlet and secured bean.
		props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER1_NAME, USER1_PASSWORD));
		HttpURLConnection connection = getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");

		// Remove user1 from group1. Group 1 still in the registry.
		testConfig.updateServerConfig("/config/serverUser1NotInRegistryGroup1.xml", false);

		// Make sure user1 has no longer access to the secured servlet.
		getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_FORBIDDEN);        

		// Have user2 (in group2 under role2) call the secured servlet that calls a secured bean method under role1. The request to the servlet
		// should be OK, but the bean method call should fail.
		props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER2_NAME, USER2_PASSWORD));
		HttpURLConnection connection2 = getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection2, "javax.ejb.EJBAccessException", "CWWKS9400A");

		// Deserialize (user1 in group1 under role1) security context/object and call the servlet method that calls the bean method secured under role1. User1 is not in group1 defined in the registry.
		HttpURLConnection connection3 = getConnection("/webApps/secured?test=deserializeUser1Role1CtxAndCallRole1BeanMethod&serFileName=/WEB-INF/files/serialized/securedServletSecurityContextForUser1InGroup1UnderRole1-8554.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection3, "javax.ejb.EJBAccessException", "CWWKS9400A");
	}

	/**
	 * call servlet to test security context authorization
	 * in enterprise concurrency 
	 */
	@Test
	public void testUnauthenticatedDeserializeConcurrentSec() throws Exception {
		testConfig.updateServerConfig(DEFAULT_CONFIG, true);
		Map<String, String> props = new HashMap<String, String>();
		HttpURLConnection connection = getConnection("/webApps/unsecured?test=testUnAuthenticatedTaskDeserialize&serFileName=/WEB-INF/files/serialized/authenticatedContextForUser1Role1-8554.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");    		   	  	
	}

	/**
	 * call servlet to test security context authorization
	 * in enterprise concurrency 
	 */
	@Test
	public void testUnauthenticatedTaskLogin() throws Exception {
		testConfig.updateServerConfig(DEFAULT_CONFIG, true);
		Map<String, String> props = new HashMap<String, String>();
		HttpURLConnection connection = getConnection("/webApps/unsecured?test=testUnAuthenticatedTaskLogin&serFileName=/WEB-INF/files/serialized/authenticatedContextForUser1Role1-8554.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
		HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");
	}

	/**
	 * @inherit
	 */
	@Override
	public LibertyServer getServer() {
		return server;
	}
	
	public static String returnStringInHttpConnection(HttpURLConnection con, String... stringsToFind) throws IOException {
        String stringToReturn=null;
		try {
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line;

            // Convert the strings to find to a set and then remove strings as they are found
            Set<String> stringsToFindSet = toSet(stringsToFind);

            StringBuilder outputBuilder = new StringBuilder();

            while ((line = br.readLine()) != null) {
            	outputBuilder.append(line);
                outputBuilder.append("\n");
                // Continue to read until response is finished, but only bother
                // looking at lines if we're still trying to find stuff..
                if (stringsToFindSet.size() > 0) {
                    Iterator<String> stringToFindIterator = stringsToFindSet.iterator();
                    while (stringToFindIterator.hasNext()) {
                        String textToFind = stringToFindIterator.next();
                        if (line.contains(textToFind)) {
                        	stringToReturn = line.substring(line.indexOf(textToFind)+textToFind.length(), line.length());
                            stringToFindIterator.remove();
                        }
                    }
                }
            }
            assertEquals("The response did not contain \"" + stringsToFindSet.toString() + "\".  Full output is:\"\n" + outputBuilder.toString() + "\".",
                         0, stringsToFindSet.size());
        } catch (IOException e) {
            Log.info(ValidateSecContextWithRegistryChanges.class, "returnStringInHttpConnection", "Exception " + e.getClass().getName() + " requesting URL=" + con.getURL().toString(), e);
            throw e;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
		return stringToReturn;
    }
	
	  private static Set<String> toSet(String[] array) {
	        Set<String> collection = new HashSet<String>();
	        for (String string : array) {
	            collection.add(string);
	        }
	        return collection;
	    }
}
