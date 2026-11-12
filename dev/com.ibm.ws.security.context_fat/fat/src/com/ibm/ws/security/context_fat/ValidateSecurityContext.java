/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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

@Mode(TestMode.LITE)
public class ValidateSecurityContext extends CommonUtils {
	private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.context_fat");
	private static final Class<?> thisClass = ValidateSecurityContext.class;
	private static final TestConfiguration testConfig = new TestConfiguration(server, appNames);
			
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
    
	/**
	 * Logs in with User1 and serializes the security context. Logs in with User2 and deserializes 
	 * the security context. Validates that User2 has proper access to resources using the security context
	 * and that proper access is restored without the serialized context.
	 *  
	 * @throws Exception
	 */
    @Test
    public void testValidateSecurityContextForAuthenticatedUser() throws Exception {
    	testConfig.updateServerConfig("/config/serverUser1InGroup1AndInRegistry.xml", true);
        Map<String, String> props = new HashMap<String, String>();
        
        //Login with User1 and serialize context
        props.put("Authorization", "Basic " + encodeUserNameAndPassword(USER1_NAME, USER1_PASSWORD));
        HttpURLConnection connection = getConnection("/webApps/secured?test=testSerializeSecuredServletSecurityContext&serFileName=securedServletSecurityContextForUser1Role1-vNext.ser", HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");
        
        //Login with User2 and deserialize context
        Map<String, String> props2 = new HashMap<String, String>();
        props2.put("Authorization", "Basic " + encodeUserNameAndPassword(USER2_NAME, USER2_PASSWORD));
        HttpURLConnection connection2 = getConnection("/webApps/secured?test=testUserCtxToAccessBeanRolesAndConfirmCtxIsRestored&serFileName=/WEB-INF/files/serialized/securedServletSecurityContextForUser1Role1-8554.ser&contextAllowedRole=" + ROLE1_NAME + "&contextDeniedRole=" + ROLE2_NAME + "&originalAllowedRole=" + ROLE2_NAME + "&originalDeniedRole=" + ROLE1_NAME, HTTPRequestMethod.GET, null, props2, HttpURLConnection.HTTP_OK);
        HttpUtils.findStringInHttpConnection(connection2, "COMPLETED SUCCESSFULLY");     
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKS0800E", "CWWKG0033W");
        }
    }
    
        
    @Override
	public LibertyServer getServer() {
		return server;
	}
    
}
