/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.install.internal.InstallKernelMap;

import test.common.SharedOutputManager;

/**
 *
 */
public class InstallKernelMapTest {
    @Rule
    public static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        InstallKernelMap ikm = new InstallKernelMap();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testGet() {
        InstallKernelMap ikm = new InstallKernelMap();
        try {
            ikm.size();
            fail("InstallKernelMap.size() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.keySet();
            fail("InstallKernelMap.keySet() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.entrySet();
            fail("InstallKernelMap.entrySet() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.values();
            fail("InstallKernelMap.values() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.clear();
            fail("InstallKernelMap.clear() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.containsValue("");
            fail("InstallKernelMap.containsValue() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.putAll(new HashMap<String, String>());
            fail("InstallKernelMap.putAll() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.remove("");
            fail("InstallKernelMap.remove() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        assertTrue("InstallKernelMap.containsKey() should return true.", ikm.containsKey("license.accept"));
        assertFalse("InstallKernelMap.containsKey() should return false.", ikm.isEmpty());
    }

    @Test
    public void testPut() {
        InstallKernelMap ikm = new InstallKernelMap();
        try {
            ikm.put("license.accept", "");
            fail("InstallKernelMap.put(license.accept) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("runtime.install.dir", "");
            fail("InstallKernelMap.put(runtime.install.dir) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("repositories.properties", "");
            fail("InstallKernelMap.put(repositories.properties) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("dowload.external.deps", "");
            fail("InstallKernelMap.put(dowload.external.deps) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("user.agent", Boolean.TRUE);
            fail("InstallKernelMap.put(user.agent) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("progress.monitor.message", "");
            fail("InstallKernelMap.put(progress.monitor.message) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("progress.monitor.cancelled", "");
            fail("InstallKernelMap.put(progress.monitor.cancelled) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("target.user.directory", "");
            fail("InstallKernelMap.put(target.user.directory) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("message.locale", "");
            fail("InstallKernelMap.put(message.locale) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("action.install", "");
            fail("InstallKernelMap.put(action.install) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("uninstall.user.features", "");
            fail("InstallKernelMap.put(uninstall.user.features) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("action.uninstall", "");
            fail("InstallKernelMap.put(action.uninstall) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("debug", "");
            fail("InstallKernelMap.put(debug) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("debug", "");
            fail("InstallKernelMap.put(debug) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("local.esa.download.dir", "");
            fail("InstallKernelMap.put(local.esa.download.dir) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("install.local.esa", "");
            fail("InstallKernelMap.put(install.local.esa) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("features.to.resolve", "");
            fail("InstallKernelMap.put(features.to.resolve) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("single.json.file", "");
            fail("InstallKernelMap.put(single.json.file) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("repositories.properties", new File("unknown"));
            fail("InstallKernelMap.put(repositories.properties) didn't throw exception.");
        } catch (RuntimeException e) {
        }

        ikm.put("action.install", new File("abc.jar"));
        assertEquals("Expected action.result is 1", 1, ikm.get("action.result"));
        assertTrue("Expected CWWKF1502E", ((String) ikm.get("action.error.message")).contains("CWWKF1502E"));

        ikm.put("action.install", new File("abc.esa"));
        assertEquals("Expected action.result is 1", 1, ikm.get("action.result"));
        assertTrue("Expected CWWKF1267E", ((String) ikm.get("action.error.message")).contains("CWWKF1267E"));
    }

    @Test
    public void testInvalidProxyPort() {
        InstallKernelMap ikm = new InstallKernelMap();
        HashMap<String, Object> overrideEnv = new HashMap<String, Object>();
        overrideEnv.put("http.proxyHost", "proxy.example.com");
        overrideEnv.put("http.proxyPort", "invalidPort");
        ikm.put(InstallConstants.OVERRIDE_ENVIRONMENT_VARIABLES, overrideEnv);

        String errorMsg = (String) ikm.get(InstallConstants.ACTION_ERROR_MESSAGE);
        assertTrue("Expected CWWKF1368E for invalid proxy port, got: " + errorMsg, errorMsg != null && errorMsg.contains("CWWKF1368E"));
    }

    private static class TestInstallKernelMap extends InstallKernelMap {
        public Map<String, String> testGetProxyVariables(String proxyEnvVar, String protocol) throws InstallException {
            return getProxyVariables(proxyEnvVar, protocol);
        }
    }

    @Test
    public void testProxyVariablesWithTrailingSlash() throws Exception {
        TestInstallKernelMap ikm = new TestInstallKernelMap();

        // HTTP with trailing slash
        Map<String, String> httpVars = ikm.testGetProxyVariables("http://proxy-server:8080/", "http");
        assertEquals("proxy-server", httpVars.get("http.proxyHost"));
        assertEquals("8080", httpVars.get("http.proxyPort"));

        // HTTP without trailing slash
        httpVars = ikm.testGetProxyVariables("http://proxy-server:8080", "http");
        assertEquals("proxy-server", httpVars.get("http.proxyHost"));
        assertEquals("8080", httpVars.get("http.proxyPort"));

        // HTTPS with credentials and trailing slash
        Map<String, String> httpsVars = ikm.testGetProxyVariables("https://user:password@proxy-server:3128/", "https");
        assertEquals("proxy-server", httpsVars.get("https.proxyHost"));
        assertEquals("3128", httpsVars.get("https.proxyPort"));
        assertEquals("user", httpsVars.get("https.proxyUser"));
        assertEquals("password", httpsVars.get("https.proxyPassword"));

        // HTTPS with credentials and without trailing slash
        httpsVars = ikm.testGetProxyVariables("https://user:password@proxy-server:3128", "https");
        assertEquals("proxy-server", httpsVars.get("https.proxyHost"));
        assertEquals("3128", httpsVars.get("https.proxyPort"));
        assertEquals("user", httpsVars.get("https.proxyUser"));
        assertEquals("password", httpsVars.get("https.proxyPassword"));
    }

    @Test
    public void testProxyVariablesInvalidFormat() {
        TestInstallKernelMap ikm = new TestInstallKernelMap();
        try {
            ikm.testGetProxyVariables("invalid_url", "http");
            fail("Expected InstallException for invalid proxy URL");
        } catch (InstallException e) {
            assertTrue("Expected CWWKF1400E", e.getMessage().contains("CWWKF1400E"));
        }

        try {
            ikm.testGetProxyVariables("http://proxy-server", "http");
            fail("Expected InstallException for proxy URL missing port");
        } catch (InstallException e) {
            assertTrue("Expected CWWKF1400E", e.getMessage().contains("CWWKF1400E"));
        }
    }

}