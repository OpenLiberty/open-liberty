/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class SocialTaiRequestTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    final SocialLoginConfig config1 = mockery.mock(SocialLoginConfig.class, "config1");
    final SocialLoginConfig config2 = mockery.mock(SocialLoginConfig.class, "config2");
    final SocialLoginConfig config3 = mockery.mock(SocialLoginConfig.class, "config3");

    private final String CWWKS5425E_SOCIAL_LOGIN_MANY_PROVIDERS = "CWWKS5425E";

    private final String config1Id = "config1Id";
    private final String config2Id = "config2Id";
    private final String config3Id = "config3Id";

    SocialTaiRequest taiReq;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        taiReq = new SocialTaiRequest(request);
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

    /************************************** addFilteredConfig **************************************/

    @Test
    public void addFilteredConfig_nullConfig() {
        try {
            taiReq.addFilteredConfig(null);

            List<SocialLoginConfig> filteredConfigs = taiReq.getFilteredConfigs();
            assertNull("Filtered configs list should be null but was: " + filteredConfigs, filteredConfigs);
            List<SocialLoginConfig> genericConfigs = taiReq.getGenericConfigs();
            assertNull("Generic configs list should be null but was: " + genericConfigs, genericConfigs);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addFilteredConfig_duplicateConfigs() {
        try {
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);

            List<SocialLoginConfig> filteredConfigs = taiReq.getFilteredConfigs();
            assertNotNull("Filtered configs list should not be null but was.", filteredConfigs);
            assertEquals("Filtered configs list size did not match expected value. Filtered configs were: " + filteredConfigs, 2, filteredConfigs.size());
            assertTrue("Should have found config1 in the filtered configs list but did not. Filtered configs were: " + filteredConfigs, filteredConfigs.contains(config1));
            assertTrue("Should have found config2 in the filtered configs list but did not. Filtered configs were: " + filteredConfigs, filteredConfigs.contains(config2));

            List<SocialLoginConfig> genericConfigs = taiReq.getGenericConfigs();
            assertNull("Generic configs list should be null but was: " + genericConfigs, genericConfigs);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addFilteredConfig_multipleConfigs() {
        try {
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);
            taiReq.addFilteredConfig(null);
            taiReq.addFilteredConfig(config3);

            List<SocialLoginConfig> filteredConfigs = taiReq.getFilteredConfigs();
            assertNotNull("Filtered configs list should not be null but was.", filteredConfigs);
            assertEquals("Filtered configs list size did not match expected value. Filtered configs were: " + filteredConfigs, 3, filteredConfigs.size());
            assertFalse("Should not have found 'null' in the filtered configs list but did. Filtered configs were: " + filteredConfigs, filteredConfigs.contains(null));
            assertTrue("Should have found config1 in the filtered configs list but did not. Filtered configs were: " + filteredConfigs, filteredConfigs.contains(config1));
            assertTrue("Should have found config2 in the filtered configs list but did not. Filtered configs were: " + filteredConfigs, filteredConfigs.contains(config2));
            assertTrue("Should have found config3 in the filtered configs list but did not. Filtered configs were: " + filteredConfigs, filteredConfigs.contains(config3));

            List<SocialLoginConfig> genericConfigs = taiReq.getGenericConfigs();
            assertNull("Generic configs list should be null but was: " + genericConfigs, genericConfigs);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addGenericConfig **************************************/

    @Test
    public void addGenericConfig_nullConfig() {
        try {
            taiReq.addGenericConfig(null);

            List<SocialLoginConfig> genericConfigs = taiReq.getGenericConfigs();
            assertNull("Generic configs list should be null but was: " + genericConfigs, genericConfigs);
            List<SocialLoginConfig> filteredConfigs = taiReq.getFilteredConfigs();
            assertNull("Filtered configs list should be null but was: " + filteredConfigs, filteredConfigs);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addGenericConfig_duplicateConfigs() {
        try {
            taiReq.addGenericConfig(config1);
            taiReq.addGenericConfig(config2);
            taiReq.addGenericConfig(config1);
            taiReq.addGenericConfig(config2);

            List<SocialLoginConfig> genericConfigs = taiReq.getGenericConfigs();
            assertNotNull("Generic configs list should not be null but was.", genericConfigs);
            assertEquals("Generic configs list size did not match expected value. Generic configs were: " + genericConfigs, 2, genericConfigs.size());
            assertTrue("Should have found config1 in the generic configs list but did not. Generic configs were: " + genericConfigs, genericConfigs.contains(config1));
            assertTrue("Should have found config2 in the generic configs list but did not. Generic configs were: " + genericConfigs, genericConfigs.contains(config2));

            List<SocialLoginConfig> filteredConfigs = taiReq.getFilteredConfigs();
            assertNull("Filtered configs list should be null but was: " + filteredConfigs, filteredConfigs);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addGenericConfig_multipleConfigs() {
        try {
            taiReq.addGenericConfig(config1);
            taiReq.addGenericConfig(config2);
            taiReq.addGenericConfig(null);
            taiReq.addGenericConfig(config3);

            List<SocialLoginConfig> genericConfigs = taiReq.getGenericConfigs();
            assertNotNull("Generic configs list should not be null but was.", genericConfigs);
            assertEquals("Generic configs list size did not match expected value. Generic configs were: " + genericConfigs, 3, genericConfigs.size());
            assertFalse("Should not have found 'null' in the generic configs list but did. Generic configs were: " + genericConfigs, genericConfigs.contains(null));
            assertTrue("Should have found config1 in the generic configs list but did not. Generic configs were: " + genericConfigs, genericConfigs.contains(config1));
            assertTrue("Should have found config2 in the generic configs list but did not. Generic configs were: " + genericConfigs, genericConfigs.contains(config2));
            assertTrue("Should have found config3 in the generic configs list but did not. Generic configs were: " + genericConfigs, genericConfigs.contains(config3));

            List<SocialLoginConfig> filteredConfigs = taiReq.getFilteredConfigs();
            assertNull("Filtered configs list should be null but was: " + filteredConfigs, filteredConfigs);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getProviderName **************************************/

    @Test
    public void getProviderName_noConfigs() {
        try {
            String result = taiReq.getProviderName();
            assertNull("Result should have been null but was: [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getProviderName_filteredConfig() {
        try {
            taiReq.addFilteredConfig(config1);

            String result = taiReq.getProviderName();
            assertNull("Result should have been null but was: [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getProviderName_genericConfig() {
        try {
            taiReq.addGenericConfig(config1);

            String result = taiReq.getProviderName();
            assertNull("Result should have been null but was: [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getProviderName_withSpecificConfig() {
        try {
            taiReq.setSpecifiedConfig(config1);
            mockery.checking(new Expectations() {
                {
                    one(config1).getUniqueId();
                    will(returnValue(config1Id));
                }
            });

            String result = taiReq.getProviderName();
            assertEquals("Provider name should have matched specific config that was set.", config1Id, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getAllMatchingConfigs **************************************/

    @Test
    public void getAllMatchingConfigs_noConfigs() {
        try {
            Set<SocialLoginConfig> result = taiReq.getAllMatchingConfigs();
            assertTrue("Set of all matching configs should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAllMatchingConfigs_onlySpecificConfig() {
        try {
            taiReq.setSpecifiedConfig(config1);

            Set<SocialLoginConfig> result = taiReq.getAllMatchingConfigs();
            assertEquals("Set of all matching configs did not match expected size.", 1, result.size());
            assertTrue("All matching configs set did not contain expected config. Set was: " + result, result.contains(config1));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAllMatchingConfigs_onlyFilteredConfigs() {
        try {
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);

            Set<SocialLoginConfig> result = taiReq.getAllMatchingConfigs();
            assertEquals("Set of all matching configs did not match expected size.", 2, result.size());
            assertTrue("All matching configs set did not contain expected config [" + config1 + "]. Set was: " + result, result.contains(config1));
            assertTrue("All matching configs set did not contain expected config [" + config2 + "]. Set was: " + result, result.contains(config2));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAllMatchingConfigs_onlyGenericConfigs() {
        try {
            taiReq.addGenericConfig(config1);
            taiReq.addGenericConfig(config2);

            Set<SocialLoginConfig> result = taiReq.getAllMatchingConfigs();
            assertEquals("Set of all matching configs did not match expected size.", 2, result.size());
            assertTrue("All matching configs set did not contain expected config [" + config1 + "]. Set was: " + result, result.contains(config1));
            assertTrue("All matching configs set did not contain expected config [" + config2 + "]. Set was: " + result, result.contains(config2));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAllMatchingConfigs_mixedConfigs() {
        try {
            taiReq.setSpecifiedConfig(config1);
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);
            taiReq.addGenericConfig(config2);
            taiReq.addGenericConfig(config3);

            Set<SocialLoginConfig> result = taiReq.getAllMatchingConfigs();
            assertEquals("Set of all matching configs did not match expected size.", 3, result.size());
            assertTrue("All matching configs set did not contain expected config [" + config1 + "]. Set was: " + result, result.contains(config1));
            assertTrue("All matching configs set did not contain expected config [" + config2 + "]. Set was: " + result, result.contains(config2));
            assertTrue("All matching configs set did not contain expected config [" + config3 + "]. Set was: " + result, result.contains(config3));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getAllMatchingConfigIds **************************************/

    @Test
    public void getAllMatchingConfigIds_noConfigs() {
        try {
            Set<String> result = taiReq.getAllMatchingConfigIds();
            assertTrue("Set of all matching config IDs should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAllMatchingConfigIds_onlySpecificConfig() {
        try {
            taiReq.setSpecifiedConfig(config1);

            mockery.checking(new Expectations() {
                {
                    one(config1).getUniqueId();
                    will(returnValue(config1Id));
                }
            });

            Set<String> result = taiReq.getAllMatchingConfigIds();
            assertEquals("Set of all matching config IDs did not match expected size.", 1, result.size());
            assertTrue("All matching config ID set did not contain expected ID. Set was: " + result, result.contains(config1Id));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAllMatchingConfigIds_onlyFilteredConfigs() {
        try {
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);

            mockery.checking(new Expectations() {
                {
                    one(config1).getUniqueId();
                    will(returnValue(config1Id));
                    one(config2).getUniqueId();
                    will(returnValue(config2Id));
                }
            });

            Set<String> result = taiReq.getAllMatchingConfigIds();
            assertEquals("Set of all matching config IDs did not match expected size.", 2, result.size());
            assertTrue("All matching config ID set did not contain expected config ID [" + config1Id + "]. Set was: " + result, result.contains(config1Id));
            assertTrue("All matching config ID set did not contain expected config ID [" + config2Id + "]. Set was: " + result, result.contains(config2Id));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAllMatchingConfigIds_onlyGenericConfigs() {
        try {
            taiReq.addGenericConfig(config1);
            taiReq.addGenericConfig(config2);

            mockery.checking(new Expectations() {
                {
                    one(config1).getUniqueId();
                    will(returnValue(config1Id));
                    one(config2).getUniqueId();
                    will(returnValue(config2Id));
                }
            });

            Set<String> result = taiReq.getAllMatchingConfigIds();
            assertEquals("Set of all matching config IDs did not match expected size.", 2, result.size());
            assertTrue("All matching config ID set did not contain expected config ID [" + config1Id + "]. Set was: " + result, result.contains(config1Id));
            assertTrue("All matching config ID set did not contain expected config ID [" + config2Id + "]. Set was: " + result, result.contains(config2Id));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAllMatchingConfigIds_mixedConfigs() {
        try {
            taiReq.setSpecifiedConfig(config1);
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);
            taiReq.addGenericConfig(config2);
            taiReq.addGenericConfig(config3);

            mockery.checking(new Expectations() {
                {
                    one(config1).getUniqueId();
                    will(returnValue(config1Id));
                    one(config2).getUniqueId();
                    will(returnValue(config2Id));
                    one(config3).getUniqueId();
                    will(returnValue(config3Id));
                }
            });

            Set<String> result = taiReq.getAllMatchingConfigIds();
            assertEquals("Set of all matching config IDs did not match expected size.", 3, result.size());
            assertTrue("All matching config ID set did not contain expected config ID [" + config1Id + "]. Set was: " + result, result.contains(config1Id));
            assertTrue("All matching config ID set did not contain expected config ID [" + config2Id + "]. Set was: " + result, result.contains(config2Id));
            assertTrue("All matching config ID set did not contain expected config ID [" + config3Id + "]. Set was: " + result, result.contains(config3Id));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getTheOnlySocialLoginConfig **************************************/

    @Test
    public void getTheOnlySocialLoginConfig_noConfigs() {
        try {
            SocialLoginConfig result = taiReq.getTheOnlySocialLoginConfig();
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTheOnlySocialLoginConfig_taiException() {
        try {
            taiReq.setTaiException(new SocialLoginException(defaultExceptionMsg, null, null));

            try {
                SocialLoginConfig result = taiReq.getTheOnlySocialLoginConfig();
                fail("Should have thrown SocialLoginException but got: [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            assertNull("TAI exception stored by the request object should be null but was [" + taiReq.taiException + "].", taiReq.taiException);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTheOnlySocialLoginConfig_specificConfig() {
        try {
            taiReq.setSpecifiedConfig(config1);

            SocialLoginConfig result = taiReq.getTheOnlySocialLoginConfig();
            assertEquals("Result did not match expected config.", config1, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTheOnlySocialLoginConfig_oneFilteredConfig() {
        try {
            taiReq.addFilteredConfig(config1);

            SocialLoginConfig result = taiReq.getTheOnlySocialLoginConfig();
            assertEquals("Result did not match expected config.", config1, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTheOnlySocialLoginConfig_multipleFilteredConfig() {
        try {
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);

            mockery.checking(new Expectations() {
                {
                    allowing(config1).getUniqueId();
                    will(returnValue(config1Id));
                    allowing(config2).getUniqueId();
                    will(returnValue(config2Id));
                }
            });
            try {
                SocialLoginConfig result = taiReq.getTheOnlySocialLoginConfig();
                fail("Should have thrown SocialLoginException because of multiple filtered configs but got: [" + result + "].");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5425E_SOCIAL_LOGIN_MANY_PROVIDERS, config1Id + "[^\\]]+" + config2Id);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTheOnlySocialLoginConfig_oneGenericConfig() {
        try {
            taiReq.addGenericConfig(config1);

            SocialLoginConfig result = taiReq.getTheOnlySocialLoginConfig();
            assertEquals("Result did not match expected config.", config1, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTheOnlySocialLoginConfig_multipleGenericConfig() {
        try {
            taiReq.addGenericConfig(config1);
            taiReq.addGenericConfig(config2);

            mockery.checking(new Expectations() {
                {
                    allowing(config1).getUniqueId();
                    will(returnValue(config1Id));
                    allowing(config2).getUniqueId();
                    will(returnValue(config2Id));
                }
            });
            try {
                SocialLoginConfig result = taiReq.getTheOnlySocialLoginConfig();
                fail("Should have thrown SocialLoginException because of multiple generic configs but got: [" + result + "].");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5425E_SOCIAL_LOGIN_MANY_PROVIDERS, config1Id + "[^\\]]+" + config2Id);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTheOnlySocialLoginConfig_specificConfig_withOtherConfigs() {
        try {
            taiReq.setSpecifiedConfig(config1);
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);
            taiReq.addGenericConfig(config2);
            taiReq.addGenericConfig(config3);

            // Only the specific config should matter - all other configs will be ignored
            SocialLoginConfig result = taiReq.getTheOnlySocialLoginConfig();
            assertEquals("Result did not match expected config.", config1, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTheOnlySocialLoginConfig_filteredAndGenericConfigs() {
        try {
            taiReq.addFilteredConfig(config1);
            taiReq.addGenericConfig(config2);

            // Filtered configs take precedence over generic configs, so the single filtered config should be returned and the generic one ignored
            SocialLoginConfig result = taiReq.getTheOnlySocialLoginConfig();
            assertEquals("Result did not match expected config.", config1, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getConfigIds **************************************/

    @Test
    public void getConfigIds_nullArg() {
        try {
            String result = taiReq.getConfigIds(null);
            assertEquals("Result for a null input should be an empty string.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigIds_emptyArg() {
        try {
            String result = taiReq.getConfigIds(new ArrayList<SocialLoginConfig>());
            assertEquals("Result for an empty input should be an empty string.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigIds_singleEntry() {
        try {
            List<SocialLoginConfig> configs = Arrays.asList(config1);

            mockery.checking(new Expectations() {
                {
                    allowing(config1).getUniqueId();
                    will(returnValue(config1Id));
                }
            });

            String expectedResult = config1Id;
            String result = taiReq.getConfigIds(configs);
            assertEquals("Result for input with a single entry did not match the expected value.", expectedResult, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigIds_multipleEntries() {
        try {
            List<SocialLoginConfig> configs = Arrays.asList(config1, config2, config3);

            mockery.checking(new Expectations() {
                {
                    allowing(config1).getUniqueId();
                    will(returnValue(config1Id));
                    allowing(config2).getUniqueId();
                    will(returnValue(config2Id));
                    allowing(config3).getUniqueId();
                    will(returnValue(config3Id));
                }
            });

            String expectedResult = config1Id + ", " + config2Id + ", " + config3Id;
            String result = taiReq.getConfigIds(configs);
            assertEquals("Result for input with multiple entries did not match the expected value.", expectedResult, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** hasServices **************************************/

    @Test
    public void hasServices_noConfigs() {
        try {
            boolean result = taiReq.hasServices();
            assertFalse("Uninitialized object should not be considered to have services, but it was.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hasServices_taiException() {
        try {
            taiReq.setTaiException(new SocialLoginException(defaultExceptionMsg, null, null));

            boolean result = taiReq.hasServices();
            assertFalse("Request object with TAI exception set should not be considered to have services, but it was.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hasServices_specificConfig() {
        try {
            taiReq.setSpecifiedConfig(config1);

            boolean result = taiReq.hasServices();
            assertTrue("Request object with specific config set should be considered to have services, but it was not.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hasServices_oneFilteredConfig() {
        try {
            taiReq.addFilteredConfig(config1);

            boolean result = taiReq.hasServices();
            assertTrue("Request object with one filtered config set should be considered to have services, but it was not.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hasServices_multipleFilteredConfig() {
        try {
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);

            boolean result = taiReq.hasServices();
            assertTrue("Request object with multiple filtered configs set should be considered to have services, but it was not.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hasServices_oneGenericConfig() {
        try {
            taiReq.addGenericConfig(config1);

            boolean result = taiReq.hasServices();
            assertTrue("Request object with one generic config set should be considered to have services, but it was not.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hasServices_multipleGenericConfig() {
        try {
            taiReq.addGenericConfig(config1);
            taiReq.addGenericConfig(config2);

            boolean result = taiReq.hasServices();
            assertTrue("Request object with multiple generic configs set should be considered to have services, but it was not.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hasServices_specificConfig_withOtherConfigs() {
        try {
            taiReq.setSpecifiedConfig(config1);
            taiReq.addFilteredConfig(config1);
            taiReq.addFilteredConfig(config2);
            taiReq.addGenericConfig(config2);
            taiReq.addGenericConfig(config3);

            boolean result = taiReq.hasServices();
            assertTrue("Request object with multiple configs set should be considered to have services, but it was not.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
