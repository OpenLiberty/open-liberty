/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.InstallKernelMap;

/**
 *
 */
public class InstallKernelMapTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

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

}
