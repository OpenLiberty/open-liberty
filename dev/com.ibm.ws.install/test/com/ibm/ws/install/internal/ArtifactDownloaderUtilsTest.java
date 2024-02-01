/*******************************************************************************n * Copyright (c) 2023 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License 2.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0n *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.install.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 *
 */
public class ArtifactDownloaderUtilsTest {

    @Test
    public void testNoProxyURL_localhost() {
        URL address;
        try {
            address = new URL("http://localhost:8081");

            Map<String, Object> envMap = new HashMap<>();
            envMap.put("NO_PROXY", "localhost");
            assertTrue(ArtifactDownloaderUtils.isNoProxyURL(address, envMap));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoProxyURL_suffixWithLeadingDot() {
        URL address;
        try {
            address = new URL("https://www.ibm.com");

            Map<String, Object> envMap = new HashMap<>();
            envMap.put("NO_PROXY", ".ibm.com");
            assertTrue(ArtifactDownloaderUtils.isNoProxyURL(address, envMap));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoProxyURL_subString() {
        URL address;
        try {
            address = new URL("https://www.ibm.com");

            Map<String, Object> envMap = new HashMap<>();
            envMap.put("NO_PROXY", ".ibm");
            assertFalse(ArtifactDownloaderUtils.isNoProxyURL(address, envMap));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoProxyURL_fullURL() {
        URL address;
        try {
            address = new URL("https://www.ibm.com/test/test");

            Map<String, Object> envMap = new HashMap<>();
            envMap.put("NO_PROXY", ".ibm.com");
            assertTrue(ArtifactDownloaderUtils.isNoProxyURL(address, envMap));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoProxyURL_yesProxy() {
        URL address;
        try {
            address = new URL("https://www.ibm.com");

            Map<String, Object> envMap = new HashMap<>();
            envMap.put("NO_PROXY", "test");
            assertFalse(ArtifactDownloaderUtils.isNoProxyURL(address, envMap));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoProxyURL_suffix() {
        URL address;
        try {
            address = new URL("https://www.ibm.com");

            Map<String, Object> envMap = new HashMap<>();
            envMap.put("NO_PROXY", "ibm.com");
            assertTrue(ArtifactDownloaderUtils.isNoProxyURL(address, envMap));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoProxyURL_IP() {
        URL address;
        try {
            address = new URL("http://9.160.4.63:8081");

            Map<String, Object> envMap = new HashMap<>();
            envMap.put("NO_PROXY", "9.160.4.63");
            assertTrue(ArtifactDownloaderUtils.isNoProxyURL(address, envMap));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoProxyURL_IPwithPort() {
        URL address;
        try {
            address = new URL("http://9.160.4.63:8081");

            Map<String, Object> envMap = new HashMap<>();
            envMap.put("NO_PROXY", "9.160.4.63:8081");
            assertTrue(ArtifactDownloaderUtils.isNoProxyURL(address, envMap));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoProxyURL_empty() {
        URL address;
        try {
            address = new URL("https://www.ibm.com");
            Map<String, Object> envMap = new HashMap<>();
            envMap.put("NO_PROXY", "");
            assertTrue(ArtifactDownloaderUtils.isNoProxyURL(address, envMap));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
