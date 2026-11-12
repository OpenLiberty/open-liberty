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

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

@Mode(TestMode.FULL)
public class ValidateSecContextWithJaasConfig extends CommonUtils{
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.context_fat");
    private static final Class<?> thisClass = ValidateSecContextWithJaasConfig.class;
    private static final TestConfiguration testConfig = new TestConfiguration(server, appNames);
    private static String DEFAULT_CONFIG = "/config/serverCustomJaasLoginContext.xml";
    private static String INVALID_JAAS_CONFIG = "/config/serverCustomInvalidJaasLoginContext.xml";
    private static String NONEXISTENT_JAAS_CONFIG = "/config/serverCustomNonexistentJaasLoginContext.xml";
    		
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
     * Serializes a secured servlet's security context with a valid JAAS login context entry ref. The serialized .ser file is written to wlp/usr/servers/xxx/*.ser.
     * This file is renamed to contain the release level as opposed to vNext and copied to webApps.war/resources/WEB-INF/files/serialized.
     * If anything changes with the objects that get serialized, the the *.ser file in ../files/serialized needs to be
     * replaced with the newly generated/renamed file.
     */
    @Test
    public void testSerializeSecurityContextWithValidLoginContext() throws Exception {
    	testConfig.updateServerConfig(DEFAULT_CONFIG, false);
        Map<String, String> props = new HashMap<String, String>();
        props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER3_NAME, USER3_PASSWORD));
        HttpURLConnection connection = getConnection("/webApps/secured?test=testSerializeSecuredServletSecurityContext&serFileName=securityContextWithValidLoginContext-vNext.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");
    }
    
    /**
     * Serializes a secured servlet's security context with an invalid JAAS login context entry ref. The serialized .ser file is written to wlp/usr/servers/xxx/*.ser.
     * This file is renamed to contain the release level as opposed to vNext and copied to webApps.war/resources/WEB-INF/files/serialized.
     * If anything changes with the objects that get serialized, the the *.ser file in ../files/serialized needs to be
     * replaced with the newly generated/renamed file.
     */
    @Test
    public void testSerializeSecurityContextWithInvalidLoginContext() throws Exception {
    	testConfig.updateServerConfig(INVALID_JAAS_CONFIG, false);
        Map<String, String> props = new HashMap<String, String>();
        props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER3_NAME, USER3_PASSWORD));
        HttpURLConnection connection = getConnection("/webApps/secured?test=testSerializeSecuredServletSecurityContext&serFileName=securityContextWithInvalidLoginContext-vNext.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");
    }

    /**
     * Test the a valid jaas login context entry is properly deserialized and used during the deserialize login.
     *  <ol>1. Call an EJB with user2, and confirm authorization failure to access the bean's role1.</ol>
     *  <ol>2. deserialize the security context which should restore user3, but the login module configured for 
     *     the serialized login context entry maps this to user1. </ol>
     *  <ol>3. Confirm that user1 has access to role1 but not role2</ol>
	 *  <ol>4. Validate the previous user (user2) is restored on the thread.</ol>
     * 
     */
    @Test
    public void testDeserializeSecurityContextWithValidLoginContext() throws Exception {
    	testConfig.updateServerConfig(DEFAULT_CONFIG, true);
        
        // Have user2 (role2) call the secured servlet that calls a secured bean method under role1. The request to the servlet
        // should be OK, but the bean method call should fail.
    	Map<String, String> props = new HashMap<String, String>();
    	props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER2_NAME, USER2_PASSWORD));
        HttpURLConnection connection = getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection, "javax.ejb.EJBAccessException", "CWWKS9400A");

        // Deserialize security context, which should put user1 on the thread, and call the bean method secured under role1.
        HttpURLConnection connection2 = getConnection("/webApps/secured?test=testUserCtxToAccessBeanRolesAndConfirmCtxIsRestored&serFileName=/WEB-INF/files/serialized/securityContextWithValidLoginContext-8554.ser&contextAllowedRole=" + ROLE1_NAME + "&contextDeniedRole=" + ROLE2_NAME + "&originalAllowedRole=" + ROLE2_NAME + "&originalDeniedRole=" + ROLE1_NAME, HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection2, "COMPLETED SUCCESSFULLY");
    }

    /**
     * Test that an invalid jaas login context entry is not used during the deserialize login.
     *  <ol>1. Call an EJB with user2, and confirm authorization failure to access bean's role1.</ol>
     *  <ol>2. deserialize the security context which should restore user3. The deserialized login 
     *  	   context entry in the security context should be null and use the default (system.DESERIALIZE_CONTEXT), 
     *  	   so the user will not get mapped in the Custom Login Module.</ol>
     *  <ol>3. Confirm that user3 has access to bean's role3 but not role1</ol>
	 *  <ol>4. Validate the previous user (user2) is restored on the thread.</ol>
     * 
     */
    @Test
    public void testDeserializeSecurityContextWithInvalidLoginContext() throws Exception {
    	testConfig.updateServerConfig(DEFAULT_CONFIG, true);
        
        // Have user2 (role2) call the secured servlet that calls a secured bean method under role1. The request to the servlet
        // should be OK, but the bean method call should fail.
    	Map<String, String> props = new HashMap<String, String>();
    	props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER2_NAME, USER2_PASSWORD));
        HttpURLConnection connection = getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection, "javax.ejb.EJBAccessException", "CWWKS9400A");

        // Deserialize security context, which should put user3 on the thread, and call the bean method secured under role1.
        HttpURLConnection connection2 = getConnection("/webApps/secured?test=testUserCtxToAccessBeanRolesAndConfirmCtxIsRestored&serFileName=/WEB-INF/files/serialized/securityContextWithInvalidLoginContext-8554.ser&contextAllowedRole=" + ROLE3_NAME + "&contextDeniedRole=" + ROLE1_NAME + "&originalAllowedRole=" + ROLE2_NAME + "&originalDeniedRole=" + ROLE1_NAME , HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection2, "COMPLETED SUCCESSFULLY");
    }
    
    /**
     * Test that a jaas login context entry that no longer exists in the server configuration is not used 
     * during the deserialize login.
     *  <ol>1. Call an EJB with user2, and confirm authorization failure to access bean's role1.</ol>
     *  <ol>2. deserialize the security context which should restore user3. The deserialized login 
     *  	   context entry ref points to a nonexistent entry (was deleted from the server.xml), 
     *  	   so an error message is issued to the log and the UNAUTHENTICATED subject is used
     *  	   for the life of that context.</ol>
     *  <ol>3. Confirm that UNAUTHENTICATED does not have access to the protected servlet</ol>
	 *  <ol>4. Validate the previous user (user2) is restored on the thread.</ol>
     * 
     */
    @Test
    public void testDeserializeSecurityContextWithNonexistentLoginContext() throws Exception {
    	testConfig.updateServerConfig(NONEXISTENT_JAAS_CONFIG, true);
        
        // Have user2 (role2) call the secured servlet that calls a secured bean method under role1. The request to the servlet
        // should be OK, but the bean method call should fail.
    	Map<String, String> props = new HashMap<String, String>();
    	props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER2_NAME, USER2_PASSWORD));
        HttpURLConnection connection = getConnection("/webApps/secured?test=callRole1BeanMethod", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection, "javax.ejb.EJBAccessException", "CWWKS9400A");

        // Deserialize security context, which should put UNAUTHENTICATED on the thread, and call the bean method secured under role1.
        HttpURLConnection connection2 = getConnection("/webApps/secured?test=testUnauthenticatedCtxAndConfirmCtxIsRestored&serFileName=/WEB-INF/files/serialized/securityContextWithValidLoginContext-8554.ser&originalAllowedRole=" + ROLE2_NAME + "&originalDeniedRole=" + ROLE1_NAME , HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection2, "COMPLETED SUCCESSFULLY");
    }
    
    /**
     * @inherit
     */
	@Override
	public LibertyServer getServer() {
		return server;
	}
}
