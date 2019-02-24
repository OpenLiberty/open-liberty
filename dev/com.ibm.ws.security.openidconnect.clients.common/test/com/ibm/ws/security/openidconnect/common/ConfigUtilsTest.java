/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import test.common.SharedOutputManager;

public class ConfigUtilsTest extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @SuppressWarnings("unchecked")
    protected final AtomicServiceReference<ConfigurationAdmin> configAdminRef = mockery.mock(AtomicServiceReference.class);

    private final String uniqueId = "myConfig";
    private final String configAttributeName = "forwardAuthzParameter";

    private static final String CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED = "CWWKS1783W";

    ConfigUtils utils = new ConfigUtils(null);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new ConfigUtils(configAdminRef);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** readAndSanitizeForwardAuthzParameter **************************************/

    @Test
    public void test_readAndSanitizeForwardAuthzParameter_emptyProps() {
        try {
            final Map<String, Object> props = new HashMap<String, Object>();

            List<String> forwardAuthzParameter = utils.readAndSanitizeForwardAuthzParameter(props, uniqueId, configAttributeName);
            assertNotNull("Value read should not have been null, but was.", forwardAuthzParameter);
            assertTrue("Value read should have been empty, but was " + forwardAuthzParameter + ".", forwardAuthzParameter.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardAuthzParameter_missingAttribute() {
        try {
            final Map<String, Object> props = createSampleProps();

            List<String> forwardAuthzParameter = utils.readAndSanitizeForwardAuthzParameter(props, uniqueId, configAttributeName);
            assertNotNull("Value read should not have been null, but was.", forwardAuthzParameter);
            assertTrue("Value read should have been empty, but was " + forwardAuthzParameter + ".", forwardAuthzParameter.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardAuthzParameter_configValueEmptyArray() {
        try {
            final Map<String, Object> props = createSampleProps();

            String[] value = new String[0];
            props.put(configAttributeName, value);

            List<String> forwardAuthzParameter = utils.readAndSanitizeForwardAuthzParameter(props, uniqueId, configAttributeName);
            assertNotNull("Value read should not have been null, but was.", forwardAuthzParameter);
            assertTrue("Value read should have been empty, but was " + forwardAuthzParameter + ".", forwardAuthzParameter.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardAuthzParameter_configValueSingleValue_valid() {
        try {
            final Map<String, Object> props = createSampleProps();

            String[] value = new String[] { "testing" };
            props.put(configAttributeName, value);

            List<String> forwardAuthzParameter = utils.readAndSanitizeForwardAuthzParameter(props, uniqueId, configAttributeName);
            assertEquals("Read value did not match expected value.", Arrays.asList(value), forwardAuthzParameter);

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardAuthzParameter_configValueSingleValue_blacklistedValue() {
        try {
            final Map<String, Object> props = createSampleProps();

            String blacklistedValue = "nonce";
            String[] configValue = new String[] { blacklistedValue };
            props.put(configAttributeName, configValue);

            List<String> forwardAuthzParameter = utils.readAndSanitizeForwardAuthzParameter(props, uniqueId, configAttributeName);

            assertNotNull("Value read should not have been null, but was. Parameter value was " + Arrays.toString(configValue) + ".", forwardAuthzParameter);
            assertTrue("Value read should have been empty, but wasn't. Value read was " + forwardAuthzParameter + ".", forwardAuthzParameter.isEmpty());

            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + blacklistedValue);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardAuthzParameter_configValueMultipleValues_allValid() {
        try {
            final Map<String, Object> props = createSampleProps();

            String entry1 = "myval";
            String entry2 = "This is another `~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? special value";
            String entry3 = "state client_id";
            String[] configValue = new String[] { entry1, entry2, entry3 };
            props.put(configAttributeName, configValue);

            List<String> forwardAuthzParameter = utils.readAndSanitizeForwardAuthzParameter(props, uniqueId, configAttributeName);

            assertNotNull("Value read should not have been null, but was. Parameter value was " + Arrays.toString(configValue) + ".", forwardAuthzParameter);
            assertTrue("Value read did not contain original [" + entry1 + "] entry. Value read was " + forwardAuthzParameter + ".", forwardAuthzParameter.contains(entry1));
            assertTrue("Value read did not contain original [" + entry2 + "] entry. Value read was " + forwardAuthzParameter + ".", forwardAuthzParameter.contains(entry2));
            assertTrue("Value read did not contain original [" + entry3 + "] entry. Value read was " + forwardAuthzParameter + ".", forwardAuthzParameter.contains(entry3));

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardAuthzParameter_configValueMultipleValues_allBlacklisted() {
        try {
            final Map<String, Object> props = createSampleProps();

            String entry1 = "redirect_uri";
            String entry2 = "scope";
            String[] configValue = new String[] { entry1, entry2 };
            props.put(configAttributeName, configValue);

            List<String> forwardAuthzParameter = utils.readAndSanitizeForwardAuthzParameter(props, uniqueId, configAttributeName);

            assertNotNull("Value read should not have been null, but was. Parameter value was " + Arrays.toString(configValue) + ".", forwardAuthzParameter);
            assertTrue("Value read should have been empty, but wasn't. Value read was " + forwardAuthzParameter + ".", forwardAuthzParameter.isEmpty());

            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry1);
            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardAuthzParameter_configValueMultipleValues_mixedValidAndBlacklisted() {
        try {
            final Map<String, Object> props = createSampleProps();

            String bad1 = "redirect_uri";
            String bad2 = "client_id";
            String good1 = "good one";
            String good2 = "two good";
            String[] configValue = new String[] { good1, bad1, bad2, good2 };
            props.put(configAttributeName, configValue);

            List<String> forwardAuthzParameter = utils.readAndSanitizeForwardAuthzParameter(props, uniqueId, configAttributeName);

            assertNotNull("Value read should not have been null, but was. Parameter value was " + Arrays.toString(configValue) + ".", forwardAuthzParameter);
            assertTrue("Value read did not contain original [" + good1 + "] entry. Value read was " + forwardAuthzParameter + ".", forwardAuthzParameter.contains(good1));
            assertTrue("Value read did not contain original [" + good2 + "] entry. Value read was " + forwardAuthzParameter + ".", forwardAuthzParameter.contains(good2));
            assertFalse("Value read should not have contained [" + bad1 + "] entry. Value read was " + forwardAuthzParameter + ".", forwardAuthzParameter.contains(bad1));
            assertFalse("Value read should not have contained [" + bad2 + "] entry. Value read was " + forwardAuthzParameter + ".", forwardAuthzParameter.contains(bad2));
            assertEquals("Value read did not have expected number of entries. Value read was " + forwardAuthzParameter + ".", 2, forwardAuthzParameter.size());

            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + bad1);
            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + bad2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** removeBlacklistedForwardAuthzParametersFromConfiguredList **************************************/

    @Test
    public void test_removeBlacklistedForwardAuthzParametersFromConfiguredList_nullList() {
        try {
            List<String> configuredList = null;

            List<String> updatedList = utils.removeBlacklistedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list should have been empty, but wasn't. Updated list was " + updatedList, updatedList.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeBlacklistedForwardAuthzParametersFromConfiguredList_emptyList() {
        try {
            List<String> configuredList = new ArrayList<String>();

            List<String> updatedList = utils.removeBlacklistedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list should have been empty, but wasn't. Updated list was " + updatedList, updatedList.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeBlacklistedForwardAuthzParametersFromConfiguredList_singleEntry_validEntry() {
        try {
            String entry = "value";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry);

            List<String> updatedList = utils.removeBlacklistedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list did not contain original entry. Updated list was " + updatedList + ".", updatedList.contains(entry));

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeBlacklistedForwardAuthzParametersFromConfiguredList_singleEntry_blacklistedEntry() {
        try {
            String entry = "scope";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry);

            List<String> updatedList = utils.removeBlacklistedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list should have had original blacklisted entry removed. Updated list was " + updatedList, updatedList.isEmpty());

            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeBlacklistedForwardAuthzParametersFromConfiguredList_singleEntry_blacklistedEntry_trailingWhitespace() {
        try {
            String entry = "response_type ";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry);

            List<String> updatedList = utils.removeBlacklistedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list did not contain original entry. Updated list was " + updatedList + ".", updatedList.contains(entry));

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeBlacklistedForwardAuthzParametersFromConfiguredList_singleEntry_blacklistedEntry_superstring() {
        try {
            String entry = "stateandmore";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry);

            List<String> updatedList = utils.removeBlacklistedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list did not contain original entry. Updated list was " + updatedList + ".", updatedList.contains(entry));

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeBlacklistedForwardAuthzParametersFromConfiguredList_multipleEntries_allValid() {
        try {
            String entry1 = "1";
            String entry2 = "2";
            String entry3 = "3";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry1);
            configuredList.add(entry2);
            configuredList.add(entry3);

            List<String> updatedList = utils.removeBlacklistedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list did not contain original [" + entry1 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(entry1));
            assertTrue("Updated parameter list did not contain original [" + entry2 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(entry2));
            assertTrue("Updated parameter list did not contain original [" + entry3 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(entry3));

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeBlacklistedForwardAuthzParametersFromConfiguredList_multipleEntries_allBlacklisted() {
        try {
            String entry1 = "scope";
            String entry2 = "nonce";
            String entry3 = "scope";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry1);
            configuredList.add(entry2);
            configuredList.add(entry3);

            List<String> updatedList = utils.removeBlacklistedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list should have had original blacklisted entry removed. Updated list was " + updatedList, updatedList.isEmpty());

            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry1);
            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry2);
            // entry3 is a duplicate of entry1 and should only show up once
            verifyNoLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry1 + ".*" + entry3);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeBlacklistedForwardAuthzParametersFromConfiguredList_multipleEntries_mixOfValidAndBlacklist() {
        try {
            String bad1 = "scope";
            String bad2 = "redirect_uri";
            String good1 = "one";
            String good2 = "two";
            String good3 = "three";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(bad1);
            configuredList.add(good1);
            configuredList.add(good2);
            configuredList.add(bad2);
            configuredList.add(good3);

            List<String> updatedList = utils.removeBlacklistedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list did not contain original [" + good1 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(good1));
            assertTrue("Updated parameter list did not contain original [" + good2 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(good2));
            assertTrue("Updated parameter list did not contain original [" + good3 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(good3));
            assertFalse("Updated parameter list should not have contained [" + bad1 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(bad1));
            assertFalse("Updated parameter list should not have contained [" + bad2 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(bad2));
            assertEquals("Updated parameter list did not have expected number of entries. Updated list was " + updatedList + ".", 3, updatedList.size());

            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + bad1);
            verifyLogMessage(outputMgr, CWWKS1783W_BLACKLISTED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + bad2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getBlacklistedForwardAuthzParameterNames **************************************/

    @Test
    public void test_getBlacklistedForwardAuthzParameterNames() {
        try {
            Set<String> blacklist = utils.getBlacklistedForwardAuthzParameterNames();
            List<String> expectedBlacklist = Arrays.asList("redirect_uri", "client_id", "response_type", "nonce", "state", "scope");

            assertNotNull("Blacklist of forward authorization parameter names should not have been null, but was.", blacklist);
            assertFalse("Blacklist of forward authorization parameter names should not have been empty, but was.", blacklist.isEmpty());
            assertEquals("Blacklist of forward authorization parameter names did not have the expected number of entries.", expectedBlacklist.size(), blacklist.size());
            for (String expectedBlacklistEntry : expectedBlacklist) {
                assertTrue("Blacklist did not contain expected value [" + expectedBlacklistEntry + "]. Blacklist was: " + blacklist, blacklist.contains(expectedBlacklistEntry));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private Map<String, Object> createSampleProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("my sample prop 1", "sample prop 1 value");
        props.put("my_sample_prop_2", "sample_prop_2_value");
        props.put("mySampleProp3", "sampleProp3Value");
        return props;
    }

}
