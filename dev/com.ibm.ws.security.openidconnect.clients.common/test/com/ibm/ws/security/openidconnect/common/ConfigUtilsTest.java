/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import test.common.SharedOutputManager;

public class ConfigUtilsTest extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.common.*=all=enabled");

    @SuppressWarnings("unchecked")
    protected final AtomicServiceReference<ConfigurationAdmin> configAdminRef = mockery.mock(AtomicServiceReference.class);
    private final ConfigurationAdmin configAdmin = mockery.mock(ConfigurationAdmin.class);
    final Configuration configuration = mockery.mock(Configuration.class);

    private final String uniqueId = "myConfig";
    private final String configAttributeName = "forwardLoginParameter";

    private static final String CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED = "CWWKS1783W";

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

    /************************************** readAndSanitizeForwardLoginParameter **************************************/

    @Test
    public void test_readAndSanitizeForwardLoginParameter_emptyProps() {
        try {
            final Map<String, Object> props = new HashMap<String, Object>();

            List<String> sanitizedValue = utils.readAndSanitizeForwardLoginParameter(props, uniqueId, configAttributeName);
            assertNotNull("Value read should not have been null, but was.", sanitizedValue);
            assertTrue("Value read should have been empty, but was " + sanitizedValue + ".", sanitizedValue.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardLoginParameter_missingAttribute() {
        try {
            final Map<String, Object> props = createSampleProps();

            List<String> sanitizedValue = utils.readAndSanitizeForwardLoginParameter(props, uniqueId, configAttributeName);
            assertNotNull("Value read should not have been null, but was.", sanitizedValue);
            assertTrue("Value read should have been empty, but was " + sanitizedValue + ".", sanitizedValue.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardLoginParameter_configValueEmptyArray() {
        try {
            final Map<String, Object> props = createSampleProps();

            String[] value = new String[0];
            props.put(configAttributeName, value);

            List<String> sanitizedValue = utils.readAndSanitizeForwardLoginParameter(props, uniqueId, configAttributeName);
            assertNotNull("Value read should not have been null, but was.", sanitizedValue);
            assertTrue("Value read should have been empty, but was " + sanitizedValue + ".", sanitizedValue.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardLoginParameter_configValueSingleValue_valid() {
        try {
            final Map<String, Object> props = createSampleProps();

            String[] value = new String[] { "testing" };
            props.put(configAttributeName, value);

            List<String> sanitizedValue = utils.readAndSanitizeForwardLoginParameter(props, uniqueId, configAttributeName);
            assertEquals("Read value did not match expected value.", Arrays.asList(value), sanitizedValue);

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardLoginParameter_configValueSingleValue_disallowedValue() {
        try {
            final Map<String, Object> props = createSampleProps();

            String disallowedValue = "nonce";
            String[] configValue = new String[] { disallowedValue };
            props.put(configAttributeName, configValue);

            List<String> sanitizedValue = utils.readAndSanitizeForwardLoginParameter(props, uniqueId, configAttributeName);

            assertNotNull("Value read should not have been null, but was. Parameter value was " + Arrays.toString(configValue) + ".", sanitizedValue);
            assertTrue("Value read should have been empty, but wasn't. Value read was " + sanitizedValue + ".", sanitizedValue.isEmpty());

            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + disallowedValue);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardLoginParameter_configValueMultipleValues_allValid() {
        try {
            final Map<String, Object> props = createSampleProps();

            String entry1 = "myval";
            String entry2 = "This is another `~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? special value";
            String entry3 = "state client_id";
            String[] configValue = new String[] { entry1, entry2, entry3 };
            props.put(configAttributeName, configValue);

            List<String> sanitizedValue = utils.readAndSanitizeForwardLoginParameter(props, uniqueId, configAttributeName);

            assertNotNull("Value read should not have been null, but was. Parameter value was " + Arrays.toString(configValue) + ".", sanitizedValue);
            assertTrue("Value read did not contain original [" + entry1 + "] entry. Value read was " + sanitizedValue + ".", sanitizedValue.contains(entry1));
            assertTrue("Value read did not contain original [" + entry2 + "] entry. Value read was " + sanitizedValue + ".", sanitizedValue.contains(entry2));
            assertTrue("Value read did not contain original [" + entry3 + "] entry. Value read was " + sanitizedValue + ".", sanitizedValue.contains(entry3));

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardLoginParameter_configValueMultipleValues_allDisallowed() {
        try {
            final Map<String, Object> props = createSampleProps();

            String entry1 = "redirect_uri";
            String entry2 = "scope";
            String[] configValue = new String[] { entry1, entry2 };
            props.put(configAttributeName, configValue);

            List<String> sanitizedValue = utils.readAndSanitizeForwardLoginParameter(props, uniqueId, configAttributeName);

            assertNotNull("Value read should not have been null, but was. Parameter value was " + Arrays.toString(configValue) + ".", sanitizedValue);
            assertTrue("Value read should have been empty, but wasn't. Value read was " + sanitizedValue + ".", sanitizedValue.isEmpty());

            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry1);
            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readAndSanitizeForwardLoginParameter_configValueMultipleValues_mixedValidAndDisallowed() {
        try {
            final Map<String, Object> props = createSampleProps();

            String bad1 = "redirect_uri";
            String bad2 = "client_id";
            String good1 = "good one";
            String good2 = "two good";
            String[] configValue = new String[] { good1, bad1, bad2, good2 };
            props.put(configAttributeName, configValue);

            List<String> sanitizedValue = utils.readAndSanitizeForwardLoginParameter(props, uniqueId, configAttributeName);

            assertNotNull("Value read should not have been null, but was. Parameter value was " + Arrays.toString(configValue) + ".", sanitizedValue);
            assertTrue("Value read did not contain original [" + good1 + "] entry. Value read was " + sanitizedValue + ".", sanitizedValue.contains(good1));
            assertTrue("Value read did not contain original [" + good2 + "] entry. Value read was " + sanitizedValue + ".", sanitizedValue.contains(good2));
            assertFalse("Value read should not have contained [" + bad1 + "] entry. Value read was " + sanitizedValue + ".", sanitizedValue.contains(bad1));
            assertFalse("Value read should not have contained [" + bad2 + "] entry. Value read was " + sanitizedValue + ".", sanitizedValue.contains(bad2));
            assertEquals("Value read did not have expected number of entries. Value read was " + sanitizedValue + ".", 2, sanitizedValue.size());

            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + bad1);
            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + bad2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** removeDisallowedForwardAuthzParametersFromConfiguredList **************************************/

    @Test
    public void test_removeDisallowedForwardAuthzParametersFromConfiguredList_nullList() {
        try {
            List<String> configuredList = null;

            List<String> updatedList = utils.removeDisallowedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list should have been empty, but wasn't. Updated list was " + updatedList, updatedList.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeDisallowedForwardAuthzParametersFromConfiguredList_emptyList() {
        try {
            List<String> configuredList = new ArrayList<String>();

            List<String> updatedList = utils.removeDisallowedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list should have been empty, but wasn't. Updated list was " + updatedList, updatedList.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeDisallowedForwardAuthzParametersFromConfiguredList_singleEntry_validEntry() {
        try {
            String entry = "value";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry);

            List<String> updatedList = utils.removeDisallowedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list did not contain original entry. Updated list was " + updatedList + ".", updatedList.contains(entry));

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeDisallowedForwardAuthzParametersFromConfiguredList_singleEntry_disallowedEntry() {
        try {
            String entry = "scope";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry);

            List<String> updatedList = utils.removeDisallowedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list should have had original disallowed entry removed. Updated list was " + updatedList, updatedList.isEmpty());

            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeDisallowedForwardAuthzParametersFromConfiguredList_singleEntry_disallowedEntry_trailingWhitespace() {
        try {
            String entry = "response_type ";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry);

            List<String> updatedList = utils.removeDisallowedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list did not contain original entry. Updated list was " + updatedList + ".", updatedList.contains(entry));

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeDisallowedForwardAuthzParametersFromConfiguredList_singleEntry_disallowedEntry_superstring() {
        try {
            String entry = "stateandmore";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry);

            List<String> updatedList = utils.removeDisallowedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list did not contain original entry. Updated list was " + updatedList + ".", updatedList.contains(entry));

            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeDisallowedForwardAuthzParametersFromConfiguredList_multipleEntries_allValid() {
        try {
            String entry1 = "1";
            String entry2 = "2";
            String entry3 = "3";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry1);
            configuredList.add(entry2);
            configuredList.add(entry3);

            List<String> updatedList = utils.removeDisallowedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

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
    public void test_removeDisallowedForwardAuthzParametersFromConfiguredList_multipleEntries_allDisallowed() {
        try {
            String entry1 = "scope";
            String entry2 = "nonce";
            String entry3 = "scope";
            List<String> configuredList = new ArrayList<String>();
            configuredList.add(entry1);
            configuredList.add(entry2);
            configuredList.add(entry3);

            List<String> updatedList = utils.removeDisallowedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list should have had original disallowed entry removed. Updated list was " + updatedList, updatedList.isEmpty());

            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry1);
            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry2);
            // entry3 is a duplicate of entry1 and should only show up once
            verifyNoLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + entry1 + ".*" + entry3);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeDisallowedForwardAuthzParametersFromConfiguredList_multipleEntries_mixOfValidAndDisallowedList() {
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

            List<String> updatedList = utils.removeDisallowedForwardAuthzParametersFromConfiguredList(configuredList, uniqueId, configAttributeName);

            assertNotNull("Updated parameter list should not have been null, but was. Method input was " + configuredList + ".", updatedList);
            assertTrue("Updated parameter list did not contain original [" + good1 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(good1));
            assertTrue("Updated parameter list did not contain original [" + good2 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(good2));
            assertTrue("Updated parameter list did not contain original [" + good3 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(good3));
            assertFalse("Updated parameter list should not have contained [" + bad1 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(bad1));
            assertFalse("Updated parameter list should not have contained [" + bad2 + "] entry. Updated list was " + updatedList + ".", updatedList.contains(bad2));
            assertEquals("Updated parameter list did not have expected number of entries. Updated list was " + updatedList + ".", 3, updatedList.size());

            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + bad1);
            verifyLogMessage(outputMgr, CWWKS1783W_DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED + ".*" + bad2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getDisallowedForwardAuthzParameterNames **************************************/

    @Test
    public void test_getDisallowedForwardAuthzParameterNames() {
        try {
            Set<String> disallowedList = utils.getDisallowedForwardAuthzParameterNames();
            List<String> expectedDisallowedList = Arrays.asList("redirect_uri", "client_id", "response_type", "nonce", "state", "scope");

            assertNotNull("Disallowed list of forward authorization parameter names should not have been null, but was.", disallowedList);
            assertFalse("Disallowed list of forward authorization parameter names should not have been empty, but was.", disallowedList.isEmpty());
            assertEquals("Disallowed list of forward authorization parameter names did not have the expected number of entries.", expectedDisallowedList.size(), disallowedList.size());
            for (String expectedDisallowedEntry : expectedDisallowedList) {
                assertTrue("Disallowed list did not contain expected value [" + expectedDisallowedEntry + "]. Disallowed list was: " + disallowedList, disallowedList.contains(expectedDisallowedEntry));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** populateCustomRequestParameterMap **************************************/

    @Test
    public void test_populateCustomRequestParameterMap_nullAgs() {
        try {
            HashMap<String, String> paramMapToPopulate = null;
            String[] configuredCustomRequestParams = null;
            String configAttrName = null;
            String configAttrValue = null;

            utils.populateCustomRequestParameterMap(configAdmin, paramMapToPopulate, configuredCustomRequestParams, configAttrName, configAttrValue);

            assertNull("Inputted map should have remained unmodified and null, but was: [" + paramMapToPopulate + "].", paramMapToPopulate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_populateCustomRequestParameterMap_emptyConfiguredParamList_nullInputMap() {
        try {
            HashMap<String, String> paramMapToPopulate = null;
            String[] configuredCustomRequestParams = new String[0];
            String configAttrName = null;
            String configAttrValue = null;

            utils.populateCustomRequestParameterMap(configAdmin, paramMapToPopulate, configuredCustomRequestParams, configAttrName, configAttrValue);

            assertNull("Inputted map should have remained unmodified and null, but was: [" + paramMapToPopulate + "].", paramMapToPopulate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_populateCustomRequestParameterMap_emptyConfiguredParamList_nonNullInputMap() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String[] configuredCustomRequestParams = new String[0];
            String configAttrName = null;
            String configAttrValue = null;

            utils.populateCustomRequestParameterMap(configAdmin, paramMapToPopulate, configuredCustomRequestParams, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_populateCustomRequestParameterMap_oneConfiguredParam_noMatchingConfiguration() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            final String configValue = "config1";
            String[] configuredCustomRequestParams = new String[] { configValue };
            String configAttrName = null;
            String configAttrValue = null;
            mockery.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(configValue, "");
                    will(returnValue(null));
                }
            });

            utils.populateCustomRequestParameterMap(configAdmin, paramMapToPopulate, configuredCustomRequestParams, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_populateCustomRequestParameterMap_oneConfiguredParam_matchingConfigMissingProperties() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            final String configValue = "config1";
            String[] configuredCustomRequestParams = new String[] { configValue };
            String configAttrName = null;
            String configAttrValue = null;
            final Dictionary<String, Object> props = createSampleConfigProps();
            mockery.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(configValue, "");
                    will(returnValue(configuration));
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.populateCustomRequestParameterMap(configAdmin, paramMapToPopulate, configuredCustomRequestParams, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_populateCustomRequestParameterMap_oneConfiguredParam() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            final String configValue = "config1";
            String[] configuredCustomRequestParams = new String[] { configValue };
            String configAttrName = "name";
            String configAttrValue = "value";
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            String name = "a configured name";
            String value = "a configured value";
            props.put(configAttrName, name);
            props.put(configAttrValue, value);
            mockery.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(configValue, "");
                    will(returnValue(configuration));
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.populateCustomRequestParameterMap(configAdmin, paramMapToPopulate, configuredCustomRequestParams, configAttrName, configAttrValue);

            assertFalse("Inputted map should not have remained unmodified and empty, but did.", paramMapToPopulate.isEmpty());
            assertEquals("Updated map's size did not match expected size. Updated map was [" + paramMapToPopulate + "].", 1, paramMapToPopulate.size());
            assertTrue("Updated map did not contain expected [" + name + "] key. Map was [" + paramMapToPopulate + "].", paramMapToPopulate.containsKey(name));
            assertEquals("Updated map did not contain the expected value for the [" + name + "] key.", value, paramMapToPopulate.get(name));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_populateCustomRequestParameterMap_multipleConfiguredParams_noMatchingPropertiesFound() {
        try {
            final Configuration config1 = mockery.mock(Configuration.class, "config1");
            final Configuration config2 = mockery.mock(Configuration.class, "config2");
            final Configuration config3 = mockery.mock(Configuration.class, "config3");

            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            final String configValue1 = "config1";
            final String configValue2 = "config2";
            final String configValue3 = "config3";
            String[] configuredCustomRequestParams = new String[] { configValue1, configValue2, configValue3 };
            String configAttrName = "name";
            String configAttrValue = "value";

            // None of the configurations associated with the custom request parameters exist or contain the required properties
            mockery.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(configValue1, "");
                    will(returnValue(config1));
                    one(config1).getProperties();
                    will(returnValue(null));
                    one(configAdmin).getConfiguration(configValue2, "");
                    will(returnValue(config2));
                    one(config2).getProperties();
                    will(returnValue(new Hashtable<String, Object>()));
                    one(configAdmin).getConfiguration(configValue3, "");
                    will(returnValue(config3));
                    one(config3).getProperties();
                    will(returnValue(createSampleConfigProps()));
                }
            });

            utils.populateCustomRequestParameterMap(configAdmin, paramMapToPopulate, configuredCustomRequestParams, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_populateCustomRequestParameterMap_multipleConfiguredParams_someMatchingPropertiesFound() {
        try {
            final Configuration config1 = mockery.mock(Configuration.class, "config1");
            final Configuration config2 = mockery.mock(Configuration.class, "config2");
            final Configuration config3 = mockery.mock(Configuration.class, "config3");
            final Configuration config4 = mockery.mock(Configuration.class, "config4");

            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            final String configValue1 = "config1";
            final String configValue2 = "config2";
            final String configValue3 = "config3";
            final String configValue4 = "config4";
            String[] configuredCustomRequestParams = new String[] { configValue1, configValue2, configValue3, configValue4 };
            String configAttrName = "name";
            String configAttrValue = "value";

            // Only two of the configurations associated with the custom request parameters contain the required properties
            String config2Name = "config 2 name";
            String config2Value = "config 2 value";
            String config3Name = "config 3 name";
            String config3Value = "config 3 value";
            final Dictionary<String, Object> config2Props = createSampleConfigProps();
            config2Props.put(configAttrName, config2Name);
            config2Props.put(configAttrValue, config2Value);
            final Dictionary<String, Object> config3Props = createSampleConfigProps();
            config3Props.put(configAttrName, config3Name);
            config3Props.put(configAttrValue, config3Value);
            mockery.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(configValue1, "");
                    will(returnValue(config1));
                    one(config1).getProperties();
                    will(returnValue(null));
                    one(configAdmin).getConfiguration(configValue2, "");
                    will(returnValue(config2));
                    one(config2).getProperties();
                    will(returnValue(config2Props));
                    one(configAdmin).getConfiguration(configValue3, "");
                    will(returnValue(config3));
                    one(config3).getProperties();
                    will(returnValue(config3Props));
                    one(configAdmin).getConfiguration(configValue4, "");
                    will(returnValue(config4));
                    one(config4).getProperties();
                    will(returnValue(createSampleConfigProps()));
                }
            });

            utils.populateCustomRequestParameterMap(configAdmin, paramMapToPopulate, configuredCustomRequestParams, configAttrName, configAttrValue);

            assertFalse("Inputted map should not have remained unmodified and empty, but did.", paramMapToPopulate.isEmpty());
            assertEquals("Updated map's size did not match expected size. Updated map was [" + paramMapToPopulate + "].", 2, paramMapToPopulate.size());
            assertTrue("Updated map did not contain expected [" + config2Name + "] key. Map was [" + paramMapToPopulate + "].", paramMapToPopulate.containsKey(config2Name));
            assertEquals("Updated map did not contain the expected value for the [" + config2Name + "] key.", config2Value, paramMapToPopulate.get(config2Name));
            assertTrue("Updated map did not contain expected [" + config3Name + "] key. Map was [" + paramMapToPopulate + "].", paramMapToPopulate.containsKey(config3Name));
            assertEquals("Updated map did not contain the expected value for the [" + config3Name + "] key.", config3Value, paramMapToPopulate.get(config3Name));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addCustomRequestParameterValueToMap **************************************/

    @Test
    public void test_addCustomRequestParameterValueToMap_nullProperties() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = "name";
            String configAttrValue = "value";
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(null));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addCustomRequestParameterValueToMap_emptyProperties() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = "name";
            String configAttrValue = "value";
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addCustomRequestParameterValueToMap_emptyProperties_nullAttributeName() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = null;
            String configAttrValue = "value";
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(configAttrValue, "some value");
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addCustomRequestParameterValueToMap_emptyProperties_nullAttributeValue() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = "name";
            String configAttrValue = null;
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(configAttrName, "some name");
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addCustomRequestParameterValueToMap_missingNameProperty() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = "name";
            String configAttrValue = "value";
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("integer", new Integer(1));
            props.put("string", "some string value");
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addCustomRequestParameterValueToMap_missingValueProperty() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = "name";
            String configAttrValue = "value";
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            String name = "name value";
            props.put(configAttrName, name);
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addCustomRequestParameterValueToMap_emptyPropertyValues() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = "name";
            String configAttrValue = "value";
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            String name = "";
            String value = "";
            props.put(configAttrName, name);
            props.put(configAttrValue, value);
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertFalse("Inputted map should not have remained unmodified and empty, but did.", paramMapToPopulate.isEmpty());
            assertEquals("Updated map's size did not match expected size. Updated map was [" + paramMapToPopulate + "].", 1, paramMapToPopulate.size());
            assertTrue("Updated map did not contain expected [" + name + "] key. Map was [" + paramMapToPopulate + "].", paramMapToPopulate.containsKey(name));
            assertEquals("Updated map did not contain the expected value for the [" + name + "] key.", value, paramMapToPopulate.get(name));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addCustomRequestParameterValueToMap_nonStringName() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = "name";
            String configAttrValue = "value";
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(configAttrName, new Integer(42));
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addCustomRequestParameterValueToMap_nonStringValue() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = "name";
            String configAttrValue = "value";
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(configAttrName, "some name value");
            props.put(configAttrValue, new Character('a'));
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertTrue("Inputted map should have remained unmodified and empty, but was: [" + paramMapToPopulate + "].", paramMapToPopulate.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addCustomRequestParameterValueToMap_normalStringValues() {
        try {
            HashMap<String, String> paramMapToPopulate = new HashMap<String, String>();
            String configAttrName = "name";
            String configAttrValue = "value";
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            String name = "some name";
            String value = "an associated value";
            props.put(configAttrName, name);
            props.put(configAttrValue, value);
            mockery.checking(new Expectations() {
                {
                    one(configuration).getProperties();
                    will(returnValue(props));
                }
            });

            utils.addCustomRequestParameterValueToMap(configuration, paramMapToPopulate, configAttrName, configAttrValue);

            assertFalse("Inputted map should not have remained unmodified and empty, but did.", paramMapToPopulate.isEmpty());
            assertEquals("Updated map's size did not match expected size. Updated map was [" + paramMapToPopulate + "].", 1, paramMapToPopulate.size());
            assertTrue("Updated map did not contain expected [" + name + "] key. Map was [" + paramMapToPopulate + "].", paramMapToPopulate.containsKey(name));
            assertEquals("Updated map did not contain the expected value for the [" + name + "] key.", value, paramMapToPopulate.get(name));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private Map<String, Object> createSampleProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("my sample prop 1", "sample prop 1 value");
        props.put("my_sample_prop_2", new Integer(42));
        props.put("mySampleProp3", new HashMap<String, String>());
        return props;
    }

    private Dictionary<String, Object> createSampleConfigProps() {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("my sample prop 1", "sample prop 1 value");
        props.put("my_sample_prop_2", new Integer(42));
        props.put("mySampleProp3", new HashMap<String, String>());
        return props;
    }

}
