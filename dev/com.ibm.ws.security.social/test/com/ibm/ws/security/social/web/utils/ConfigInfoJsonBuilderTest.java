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
package com.ibm.ws.security.social.web.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class ConfigInfoJsonBuilderTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private static final String URL = "https://some-domain.com:80/context/path";
    private static final String CONFIG_DISPLAY_NAME = "My 1st Social Media";

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    public interface MockInterface {
        public JSONArray buildSocialMediaList();

        public JSONObject buildSocialMediumEntry();

        public String getObscuredIdFromConfigId();
    }

    private final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class, "config");
    private final SocialLoginConfig config2 = mockery.mock(SocialLoginConfig.class, "config2");

    ConfigInfoJsonBuilder builder = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());

        builder = new ConfigInfoJsonBuilder((Collection<SocialLoginConfig>) null) {
            @Override
            String getObscuredIdFromConfigId(String configId) {
                return mockInterface.getObscuredIdFromConfigId();
            }
        };
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

    /************************************** buildJsonResponse **************************************/

    @Test
    public void test_buildJsonResponse_nullMediaList() {
        try {
            ConfigInfoJsonBuilder builder = new ConfigInfoJsonBuilder((Collection<SocialLoginConfig>) null) {
                @Override
                JSONArray buildSocialMediaList() {
                    return mockInterface.buildSocialMediaList();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).buildSocialMediaList();
                    will(returnValue(null));
                }
            });

            JSONObject result = builder.buildJsonResponse();

            assertNotNull("Result should not have been null but was.", result);
            assertTrue("Result should have been empty but was not. Result was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildJsonResponse_emptyMediaList() {
        try {
            ConfigInfoJsonBuilder builder = new ConfigInfoJsonBuilder((Collection<SocialLoginConfig>) null) {
                @Override
                JSONArray buildSocialMediaList() {
                    return mockInterface.buildSocialMediaList();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).buildSocialMediaList();
                    will(returnValue(new JSONArray()));
                }
            });

            JSONObject result = builder.buildJsonResponse();

            assertNotNull("Result should not have been null but was.", result);
            assertFalse("Result should not have been empty but was. Result was: " + result, result.isEmpty());
            assertTrue("Result should contain " + ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA + " but did not. Result was: " + result, result.containsKey(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA));
            JSONArray mediaList = (JSONArray) result.get(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA);
            assertEquals("Media list in result should be empty but was not. List was: " + mediaList, 0, mediaList.size());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_buildJsonResponse_nonEmptyMediaList() {
        try {
            ConfigInfoJsonBuilder builder = new ConfigInfoJsonBuilder((Collection<SocialLoginConfig>) null) {
                @Override
                JSONArray buildSocialMediaList() {
                    return mockInterface.buildSocialMediaList();
                }
            };
            final String entry1 = "entry1";
            final int entry2 = 2;
            final JSONArray list = new JSONArray();
            list.add(entry1);
            list.add(entry2);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).buildSocialMediaList();
                    will(returnValue(list));
                }
            });

            JSONObject result = builder.buildJsonResponse();

            assertNotNull("Result should not have been null but was.", result);
            assertFalse("Result should not have been empty but was. Result was: " + result, result.isEmpty());
            assertTrue("Result should contain " + ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA + " but did not. Result was: " + result, result.containsKey(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA));
            JSONArray mediaList = (JSONArray) result.get(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA);
            assertEquals("Media list in result should be empty but was not. List was: " + mediaList, list.size(), mediaList.size());
            assertTrue("Media list is missing " + entry1 + ". List was: " + mediaList, mediaList.contains(entry1));
            assertTrue("Media list is missing " + entry2 + ". List was: " + mediaList, mediaList.contains(entry2));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** buildSocialMediaList **************************************/

    @Test
    public void test_buildSocialMediaList_nullConfigIterator_CollectionCollection() {
        try {
            ConfigInfoJsonBuilder builder = new ConfigInfoJsonBuilder((Collection<SocialLoginConfig>) null);

            JSONArray result = builder.buildSocialMediaList();

            assertNull("Result should have been null but was: " + result, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildSocialMediaList_nullConfigIterator_IteratorConstructor() {
        try {
            ConfigInfoJsonBuilder builder = new ConfigInfoJsonBuilder((Iterator<SocialLoginConfig>) null);

            JSONArray result = builder.buildSocialMediaList();

            assertNull("Result should have been null but was: " + result, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildSocialMediaList_noConfigs() {
        try {
            ConfigInfoJsonBuilder builder = new ConfigInfoJsonBuilder(new HashSet<SocialLoginConfig>());

            JSONArray result = builder.buildSocialMediaList();

            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Media list in result should be empty but was not. List was: " + result, 0, result.size());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildSocialMediaList_nullAndNonNullConfigs() {
        try {
            Set<SocialLoginConfig> configs = new HashSet<SocialLoginConfig>();
            configs.add(config);
            configs.add(config2);

            ConfigInfoJsonBuilder builder = new ConfigInfoJsonBuilder(configs) {
                @Override
                JSONObject buildSocialMediumEntry(SocialLoginConfig config) {
                    return mockInterface.buildSocialMediumEntry();
                }
            };

            final JSONObject onlyEntry = new JSONObject();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).buildSocialMediumEntry();
                    will(returnValue(onlyEntry));
                    // Null entry shouldn't be added to the result
                    one(mockInterface).buildSocialMediumEntry();
                    will(returnValue(null));
                }
            });

            JSONArray result = builder.buildSocialMediaList();

            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Media list in result should have ignored null entry and only have one entry. Result was: " + result, 1, result.size());
            assertTrue("Media list is missing " + onlyEntry + ". List was: " + result, result.contains(onlyEntry));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** buildSocialMediumEntry **************************************/

    @Test
    public void test_buildSocialMediumEntry_nullArg() {
        try {
            JSONObject result = builder.buildSocialMediumEntry(null);
            assertNull("Result should have been null but was: " + result, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildSocialMediumEntry_nullConfigValues() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(null));
                    one(config).getWebsite();
                    will(returnValue(null));
                    one(config).getDisplayName();
                    will(returnValue(null));
                    one(mockInterface).getObscuredIdFromConfigId();
                    will(returnValue(null));
                }
            });

            JSONObject result = builder.buildSocialMediumEntry(config);

            verifySocialMediumResult(result, null, null, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildSocialMediumEntry_nullWebsiteAndDisplayName() {
        try {
            final String obscuredId = "someObscuredId";
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(config).getWebsite();
                    will(returnValue(null));
                    one(config).getDisplayName();
                    will(returnValue(null));
                    one(mockInterface).getObscuredIdFromConfigId();
                    will(returnValue(obscuredId));
                }
            });

            JSONObject result = builder.buildSocialMediumEntry(config);

            verifySocialMediumResult(result, obscuredId, null, uniqueId);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildSocialMediumEntry_idContainsJsonChars() {
        try {
            final String id = "{{]value}, ][ {,\"test}";
            final String obscuredId = "someObscuredId";
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(id));
                    one(config).getWebsite();
                    will(returnValue(null));
                    one(config).getDisplayName();
                    will(returnValue(null));
                    one(mockInterface).getObscuredIdFromConfigId();
                    will(returnValue(obscuredId));
                }
            });

            JSONObject result = builder.buildSocialMediumEntry(config);

            verifySocialMediumResult(result, obscuredId, null, id);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildSocialMediumEntry_nullDisplayName() {
        try {
            final String obscuredId = "someObscuredId";
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(config).getWebsite();
                    will(returnValue(URL));
                    one(config).getDisplayName();
                    will(returnValue(null));
                    one(mockInterface).getObscuredIdFromConfigId();
                    will(returnValue(obscuredId));
                }
            });

            JSONObject result = builder.buildSocialMediumEntry(config);

            // If displayName is missing, the config ID is used instead for the display-name entry
            verifySocialMediumResult(result, obscuredId, URL, uniqueId);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildSocialMediumEntry_allConfigValuesPresent() {
        try {
            final String obscuredId = "someObscuredId";
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(config).getWebsite();
                    will(returnValue(URL));
                    one(config).getDisplayName();
                    will(returnValue(CONFIG_DISPLAY_NAME));
                    one(mockInterface).getObscuredIdFromConfigId();
                    will(returnValue(obscuredId));
                }
            });

            JSONObject result = builder.buildSocialMediumEntry(config);

            verifySocialMediumResult(result, obscuredId, URL, CONFIG_DISPLAY_NAME);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    void verifySocialMediumResult(JSONObject result, String expectedId, String expectedWebsite, String expectedDisplayName) {
        assertNotNull("Result should not have been null but was.", result);

        // Verify ID
        assertTrue("Result should have " + ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_ID + " key but did not. Result was: " + result, result.containsKey(ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_ID));
        String id = (String) result.get(ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_ID);
        assertEquals(ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_ID + " value did not match expected value.", expectedId, id);

        verifySocialMediumEntry(result, ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_WEBSITE, expectedWebsite);
        verifySocialMediumEntry(result, ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_DISPLAY_NAME, expectedDisplayName);
    }

    void verifySocialMediumEntry(JSONObject result, String key, String expectedValue) {
        if (expectedValue == null) {
            assertFalse("Result should not have " + key + " key but did. Result was: " + result, result.containsKey(key));
        } else {
            String entry = (String) result.get(key);
            assertEquals(key + " value did not match expected value.", expectedValue, entry);
        }
    }

}