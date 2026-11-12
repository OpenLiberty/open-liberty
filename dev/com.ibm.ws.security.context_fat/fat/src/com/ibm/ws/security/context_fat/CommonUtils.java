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

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.codec.binary.Base64;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 * Common FAT utilities.
 */
public abstract class CommonUtils {
	

	/**
	 * Application list used to make sure they are available before the test starts.
	 */
    protected static final Set<String> appNames = new TreeSet<String>(Arrays.asList("securityContext"));
    
    /**
     * Users and passwords.
     */
    public static final String USER1_NAME = "user1";
    public static final String USER1_PASSWORD = "user1pwd";
    public static final String USER2_NAME = "user2";
    public static final String USER2_PASSWORD = "user2pwd";
    public static final String USER3_NAME = "user3";
    public static final String USER3_PASSWORD = "user3pwd";
    
    public static final String ROLE1_NAME = "role1";
    public static final String ROLE2_NAME = "role2";
    public static final String ROLE3_NAME = "role3";
    
    /**
     * Gets a connection to a servlet.
     * 
     * @param path The serlvet URI path.
     * @param method The HTTPRequest method type (GET/POST/etc.)
     * @param payload The request payload in String form.
     * @param headerProps The map containing header properties.
     * @param expectedResponseCode The expected response code.
     * @return the HTTPUrlConnection.
     * 
     * @throws Exception
     */
    public HttpURLConnection getConnection(String path, HTTPRequestMethod method, String payload, Map<String, String> headerProps, int expectedResponseCode) throws Exception {
        // Put together the URL. For example: http://localhost:8010/webApps/...
        URL url = new URL("http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + path);

        // Get a HTTP connection to the URL we created. 
        ByteArrayInputStream payloadInputStream = (payload == null) ? null : new ByteArrayInputStream(payload.getBytes());
        HttpURLConnection connection = HttpUtils.getHttpConnection(url, expectedResponseCode, null, 5, method, headerProps, payloadInputStream);
        return connection;
    }
    
    /**
     * Encodes a user name and password.
     * 
     * @param userName The user name.
     * @param password The user's password.
     * @return The encoded name and password.
     * 
     * @throws Exception
     */
    public String encodeUserNameAndPassword(String userName, String password) throws Exception {
    	String authData = userName + ":" + password;
        return new String(Base64.encodeBase64(new String(authData).getBytes("UTF-8")));
    }
    
    public abstract LibertyServer getServer();
}
