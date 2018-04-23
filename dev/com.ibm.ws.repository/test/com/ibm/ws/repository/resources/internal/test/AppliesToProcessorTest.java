/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resources.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.resources.internal.AppliesToProcessor;
import com.ibm.ws.repository.resources.internal.AppliesToProcessor.AppliesToEntry;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;

public class AppliesToProcessorTest {

    @Test
    public void testBasicAppliesToHeader() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0; productEditions=\"BASE\"");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testShortVersion() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5; productEditions=\"BASE\"");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5", "8.5", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5", "8.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5", "8.5", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5", "8.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBasicOrderIrrelevant() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productEditions=\"BASE\"; productVersion=8.5.5.0");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testUnbounded() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0+; productEditions=\"BASE\"");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNull("filter info should NOT have maxVersion information", atfi.getMaxVersion());
        assertFalse("filter info should not have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testQuotedUnbounded() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=\"8.5.5.0+\"; productEditions=\"BASE\"");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNull("filter info should NOT have maxVersion information", atfi.getMaxVersion());
        assertFalse("filter info should not have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testNoEditionInBetaFeature() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertTrue("filter info should be for base", atfi.getEditions().get(0).equals("Beta"));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Beta", "Beta", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Beta", "Beta", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testDateBased() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13; productEditions=\"BASE\"");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Beta", "Beta", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Beta", "Beta", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testUnboundedDateBased() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13+; productEditions=\"BASE\"");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Beta", "Beta", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNull("filter info should not have maxVersion information", atfi.getMaxVersion());
        assertFalse("filter info should not have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testCoreTranslated() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13; productEditions=\"Core\"");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for Liberty Core", "Liberty Core", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Beta", "Beta", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Beta", "Beta", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testListWithCore() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13; productEditions=\"Core,BASE\"");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for two editions", 2, atfi.getEditions().size());
        assertEquals("filter info should be for Liberty Core", "Liberty Core", atfi.getEditions().get(0));
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(1));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Beta", "Beta", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Beta", "Beta", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testMultipleProduct() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13; productEditions=\"Core,BASE\",com.ibm.tinypinkelf; productVersion=8.5.5.0; productEditions=\"Base\"");
        assertEquals("Expected two filter info to be returned", 2, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for two editions", 2, atfi.getEditions().size());
        assertEquals("filter info should be for Liberty Core", "Liberty Core", atfi.getEditions().get(0));
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(1));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Beta", "Beta", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Beta", "Beta", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
        atfi = atfis.get(1);
        assertEquals("filter info should be for tinypinkelf", "com.ibm.tinypinkelf", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testOmittedEditions() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for six editions", 6, atfi.getEditions().size());

        //"Liberty Core", "Base", "Express", "Developers", "ND", "z/OS"
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Liberty Core"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Base"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Express"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Developers"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("ND"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("z/OS"));

        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testNoVersion() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info not should have some edition info (the defaults)", atfi.getEditions());
        assertNull("filter info should not have minVersion information", atfi.getMinVersion());
        assertNull("filter info should not have maxVersion information", atfi.getMaxVersion());
        assertFalse("filter info should not have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBASE_ILAN() {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.4; productEditions=\"BASE_ILAN\"");
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should have zero editions", 0, atfi.getEditions().size());
    }

    @Test
    public void testValidateBASE_ILAN() throws RepositoryResourceCreationException {
        String appliesToHeader = "com.ibm.fish; productVersion=8.5.5.4; productEditions=\"BASE_ILAN\"";
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader(appliesToHeader);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should have zero editions", 0, atfi.getEditions().size());
        AppliesToProcessor.validateEditions(atfi, appliesToHeader);
    }

    @Test
    public void testUnknownEdition() {
        String appliesToHeader = "com.ibm.cheese-ston; productVersion=8.5.5.4; productEditions=\"AN_UNKNOWN\"";
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader(appliesToHeader);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should have one edition", 1, atfi.getEditions().size());
        assertEquals("filter info.editions should have the correct unkonwn edition", "AN_UNKNOWN", atfi.getEditions().get(0));
    }

    @Test(expected = RepositoryResourceCreationException.class)
    public void testValidateUnknownEdition() throws RepositoryResourceCreationException {
        String appliesToHeader = "com.ibm.cheese-ston; productVersion=8.5.5.4; productEditions=\"AN_UNKNOWN\"";
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader(appliesToHeader);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        AppliesToProcessor.validateEditions(atfi, appliesToHeader);
    }

    @Test
    public void testParseEntriesWithVersionAndEdition() {
        List<AppliesToEntry> entries = AppliesToProcessor.parseAppliesToEntries("com.ibm.fish; productVersion=fish; productEditions=\"stiletto\"");
        assertEquals("Expected one entry to be returned", 1, entries.size());
        AppliesToEntry entry = entries.get(0);
        assertEquals("entry should be for fish", "com.ibm.fish", entry.getProductId());
        assertEquals("entry should contain stiletto", "stiletto", entry.getEditions());
        assertEquals("entry should contain version fish", "fish", entry.getVersion());
    }

    @Test
    public void testParseEntriesWithVersion() {
        List<AppliesToEntry> entries = AppliesToProcessor.parseAppliesToEntries("com.ibm.fish; productVersion=fish");
        assertEquals("Expected one entry to be returned", 1, entries.size());
        AppliesToEntry entry = entries.get(0);
        assertEquals("entry should be for fish", "com.ibm.fish", entry.getProductId());
        assertNull("entry should not have edition info", entry.getEditions());
        assertEquals("entry should contain version fish", "fish", entry.getVersion());
    }

    @Test
    public void testParseEntriesWitMultipleProducts() throws Exception {
        List<AppliesToEntry> entries = AppliesToProcessor.parseAppliesToEntries("com.ibm.fish; productVersion=2013.13.13; productEditions=\"Core,BASE\",com.ibm.tinypinkelf; productVersion=8.5.5.0; productEditions=\"Base\"");
        assertEquals("Expected two entries to be returned", 2, entries.size());
        AppliesToEntry entry = entries.get(0);
        assertEquals("entry should be for fish", "com.ibm.fish", entry.getProductId());
        assertEquals("entry should be for \"Core,BASE\"", "Core,BASE", entry.getEditions());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", entry.getVersion());
        entry = entries.get(1);
        assertEquals("entry should be for inypinkelf", "com.ibm.tinypinkelf", entry.getProductId());
        assertEquals("entry should be for \"Core,BASE\"", "Base", entry.getEditions());
        assertEquals("filter info minVersion should be for 2013.13.13", "8.5.5.0", entry.getVersion());
    }
}
