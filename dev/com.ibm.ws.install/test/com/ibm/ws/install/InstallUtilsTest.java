/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.install.internal.InstallUtils;

import test.common.SharedOutputManager;

/**
 *
 */
public class InstallUtilsTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testGetEditionName() {
        assertEquals("Expected empty string", "", InstallUtils.getEditionName("BASE"));
        assertEquals("Expected (ILAN)", "(ILAN)", InstallUtils.getEditionName("BASE_ILAN"));
        assertEquals("Expected for Developers", "for Developers", InstallUtils.getEditionName("DEVELOPERS"));
        assertEquals("Expected - Express", "- Express", InstallUtils.getEditionName("EXPRESS"));
        assertEquals("Expected Early Access", "Early Access", InstallUtils.getEditionName("EARLY_ACCESS"));
        assertEquals("Expected Liberty Core", "Liberty Core", InstallUtils.getEditionName("LIBERTY_CORE"));
        assertEquals("Expected Network Deployment", "Network Deployment", InstallUtils.getEditionName("ND"));
        assertEquals("Expected z/OS", "z/OS", InstallUtils.getEditionName("ZOS"));
    }

    @Test
    public void testGetAssetsMap() throws Exception {
        Collection<String> assets = new HashSet<String>();
        assets.add(":");
        assets.add("a");
        assets.add("b:");
        assets.add(" c :");
        assets.add(":d");
        assets.add(": e ");
        assets.add(" : f ");
        assets.add("usr:h");
        assets.add(" usr : j ");
        Map<String, Collection<String>> assetsMap = InstallUtils.getAssetsMap(assets, true);
        assertTrue("Expected default key", assetsMap.containsKey(InstallUtils.DEFAULT_TO_EXTENSION));
        assertEquals("Expected 4 default", 4, assetsMap.get(InstallUtils.DEFAULT_TO_EXTENSION).size());
        assertFalse("Expected no b key", assetsMap.containsKey("b"));
        assertFalse("Expected no c key", assetsMap.containsKey("c"));
        assertTrue("Expected usr key", assetsMap.containsKey("usr"));
        assertEquals("Expected 2 usr values", 2, assetsMap.get("usr").size());
    }

    @Test
    public void testGetAssetsMapUnknownExt() {
        Collection<String> assets = new HashSet<String>();
        assets.add("unknown:abc");
        try {
            InstallUtils.getAssetsMap(assets, false);
            fail("Exception is expected.");
        } catch (InstallException e) {
            assertTrue(e.getMessage().contains("CWWKF1297E"));
        }
        try {
            Map<String, Collection<String>> assetsMap = InstallUtils.getAssetsMap(assets, true);
            assertTrue("Expected unknown key", assetsMap.containsKey("unknown"));
            assertEquals("Expected 1 value.", 1, assetsMap.get("unknown").size());
        } catch (InstallException e) {
            fail("Exception is not expected: " + e.getMessage());
        }
    }

    @Test
    public void testToExtension() {
        assertEquals("Expected abc.", "abc", InstallUtils.toExtension("deFAULT", "abc"));
        assertEquals("Expected Other.", "Other", InstallUtils.toExtension("Other", "abc"));
    }

}
