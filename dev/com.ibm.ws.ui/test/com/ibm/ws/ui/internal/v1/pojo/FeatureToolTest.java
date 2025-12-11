/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.ui.internal.v1.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.codehaus.jackson.map.ObjectMapper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.utils.LogRule;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

import test.common.JSONablePOJOTestCase;
import test.common.SharedOutputManager;

/**
 *
 */
public class FeatureToolTest extends JSONablePOJOTestCase {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = new LogRule(outputMgr);

    private final String TOOL_ID = "com.ibm.websphere.feature-1.0";
    private final String TOOL_TYPE = ITool.TYPE_FEATURE_TOOL;
    private final String TOOL_FEATURE_NAME = "com.ibm.websphere.feature";
    private final String TOOL_FEATURE_VERSION = "1.0";
    private final String TOOL_FEATURE_SHORT_NAME = "tool-1.0";
    private final String TOOL_NAME = "myFeature";
    private final String TOOL_URL = "ibm.com";
    private final String TOOL_ICON = "icon.png";
    private final String TOOL_DESCRIPTION = "a really great tool";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final JSON mockJson = mock.mock(JSON.class);

    @Before
    public void setUp() {
        jsonablePojo = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        sourceJson = "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"featureShortName\":\"tool-1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}";
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#FeatureTool(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getters() {
        FeatureTool tool = new FeatureTool(TOOL_ID, TOOL_TYPE, TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: FeatureTool did not return the expected ID",
                     TOOL_ID, tool.getId());
        assertEquals("FAIL: FeatureTool did not return the expected type",
                     TOOL_TYPE, tool.getType());
        assertEquals("FAIL: FeatureTool did not return the expected name",
                     TOOL_NAME, tool.getName());
        assertEquals("FAIL: FeatureTool did not return the expected icon",
                     TOOL_ICON, tool.getIcon());
        assertEquals("FAIL: FeatureTool did not return the expected url",
                     TOOL_URL, tool.getURL());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#FeatureTool(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getters_implied() {
        FeatureTool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: FeatureTool did not return the expected ID",
                     TOOL_FEATURE_NAME + "-" + TOOL_FEATURE_VERSION, tool.getId());
        assertEquals("FAIL: FeatureTool did not return the expected type",
                     ITool.TYPE_FEATURE_TOOL, tool.getType());
        assertEquals("FAIL: FeatureTool did not return the expected short name",
                     TOOL_FEATURE_SHORT_NAME, tool.getFeatureShortName());
        assertEquals("FAIL: FeatureTool did not return the expected name",
                     TOOL_NAME, tool.getName());
        assertEquals("FAIL: FeatureTool did not return the expected icon",
                     TOOL_ICON, tool.getIcon());
        assertEquals("FAIL: FeatureTool did not return the expected url",
                     TOOL_URL, tool.getURL());
    }

    @Test
    public void validateSelfAbsoluteURLsHTTP() {
        FeatureTool featureTool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "http://cnn.com", "http://cnn.com/icon.png", "myDescription");
        try {
            featureTool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Override
    @Test
    public void incompleteJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue("{}", FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id, type"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Toolbox should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Toolbox should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":null,\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Toolbox should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_wrongId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"wrongId\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Toolbox with an id should have a non-zero hashcode",
                   0 != incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("type"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":null,\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("type"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("type"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_incorrectType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"bookmark\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the incorrect type. Message: " + e.getMessage(),
                       e.getMessage().contains("featureTool"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingFeatureName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("featureName"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullFeatureName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":null,\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("featureName"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyFeatureName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("featureName"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingFeatureVersion() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("featureVersion"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullFeatureVersion() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":null,\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("featureVersion"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyFeatureVersion() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("featureVersion"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("name"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":null,\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("name"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("name"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("url"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":null,\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("url"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("url"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingIcon() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("icon"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullIcon() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":null,\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("icon"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyIcon() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue(
                                                     "{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"\",\"description\":\"a really great tool\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("icon"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingDescription() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue("{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("description"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullDescription() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue("{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":null}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("description"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyDescription() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue("{\"id\":\"com.ibm.websphere.feature-1.0\",\"type\":\"featureTool\",\"featureName\":\"com.ibm.websphere.feature\",\"featureVersion\":\"1.0\",\"name\":\"myFeature\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"\"}",
                                                     FeatureTool.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("description"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void validateSelf_bookmark() throws Exception {
        final ITool tool = new FeatureTool(TOOL_ID, ITool.TYPE_BOOKMARK, TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not match the expected format. Message: " + e.getMessage(),
                       e.getMessage().matches(".*" + ITool.TYPE_FEATURE_TOOL + ".*" + ITool.TYPE_BOOKMARK + ".*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void validateSelf_featureToolWithoutShortName() throws Exception {
        final ITool tool = new FeatureTool(TOOL_ID, ITool.TYPE_FEATURE_TOOL, TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, null, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        tool.validateSelf();
        // Should pass validation
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#validateSelf()}.
     */
    @Test
    public void validateSelf_featureTool() throws Exception {
        final ITool tool = new FeatureTool(TOOL_ID, ITool.TYPE_FEATURE_TOOL, TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        tool.validateSelf();
        // Should pass validation
    }

    @Test
    public void validateSelf_absoluteURLsHTTP() {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "http://cnn.com", "http://cnn.com/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    @Test
    public void validateSelf_absoluteURLsHTTPS() {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "https://cnn.com", "https://cnn.com/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    @Test
    public void validateSelf_absoluteURLsHTTPInCaps() {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "HTTP://CNN.COM", "HTTP://CNN.COM/ICON.PNG", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    @Test
    public void validateSelf_absoluteURLsHTTPSInCaps() {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "HTTPS://CNN.COM", "HTTPS://CNN.COM/ICON.PNG", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    @Test
    public void validateSelf_relativeServerURLs() {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "/toolA", "/toolA/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: Server relative URLs are valid but validation failed");
        }
    }

    @Test
    public void validateSelf_relativePageURLs() {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "toolB", "toolB/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: Page relative URLs are valid but validation failed");
        }
    }

    @Test
    public void validateSelf_relativePageURLsMoreDots() {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "toolB/index...html", "toolB/icon....png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: Page relative URLs are valid (even with a lot of dots!) but validation failed");
        }
    }

    @Ignore("For now, this is a pretty insane case. We need to be using a real library!")
    @Test
    public void validateSelf_relativePageURLsWackyResource() {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "toolB/sub:/index.html", "toolB/sub:/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: Funky URLs should be considered valid, even when not encoded");
        }
    }

    /**
     * This scenario is really weird but it IS valid...
     */
    @Test
    public void validateSelf_DoubleProtocol() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "http://ibm.com/http://localhost/index.html", "http://ibm.com/http://localhost/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssName() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, "CN<script>N", TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A tool with XSS in 'name' is not valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssURL() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "http://cnn.com?<script>alert('a')</script>", TOOL_ICON, TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A tool with XSS in 'url' is not valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssIcon() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, "http://i.cdn.turner.com/cnn/.e/<img src='abc'></img>", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A tool with XSS in 'icon' is not valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssDescription() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, "CNN<script src='abc'>");
        tool.validateSelf();
        fail("FAIL: A tool with XSS in 'name' is not valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssAll() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, "CNN<script src='abc'>", "http://cnn.com/<script>altert('hi')</script>", "http://i.cdn.turner.com/cnn/.e/<img src='abc'></img>", "CNN<script src='abc'>");
        tool.validateSelf();
        fail("FAIL: A tool with XSS in 'icon' is not valid");
    }

    @Test
    public void validateSelf_nameChars() {
        // test for valid and invalid characters in the name
        // invalid chars are  ~ & : ; / \ ? {} < > []
        String[] invalidChars = new String[] { "~", "&", ":", ";", "/", "\\", "?", "{", "}", "<", ">", "[", "]" };
        String[] validChars = new String[] { "`", "!", "@", "#", "$", "%", "^", "*", "(", ")", "-", "_", "=", "+", "|", "\"", "'", ",", "." };
        String toolName = null;
        ITool tool;

        for (int i = 0; i < validChars.length; i++) {
            toolName = "CN" + validChars[i] + "N";
            tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, toolName, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
            try {
                tool.validateSelf();
                // no exception so pass
            } catch (InvalidToolException e) {
                fail("FAIL: A valid tool with " + validChars[i] + " in the 'name' is not valid");
            }
        }

        for (int i = 0; i < invalidChars.length; i++) {
            toolName = "CN" + invalidChars[i] + "N";
            tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, toolName, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
            try {
                tool.validateSelf();
                fail("FAIL: A tool with invalid char " + toolName + " in the 'name' is valid");
            } catch (InvalidToolException e) {
                // pass
            }
        }
    }

    @Test
    public void validateSelf_descriptionChars() {
        // test for valid and invalid characters in the name
        // invalid chars are  ~ & : ; / \ ? {} < > []
        String[] invalidChars = new String[] { "~", "&", ":", ";", "/", "\\", "?", "{", "}", "<", ">", "[", "]" };
        String[] validChars = new String[] { "`", "!", "@", "#", "$", "%", "^", "*", "(", ")", "-", "_", "=", "+", "|", "\"", "'", ",", "." };
        String toolDescription = null;
        ITool tool;

        for (int i = 0; i < validChars.length; i++) {
            toolDescription = "CN" + validChars[i] + "N";
            tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, toolDescription);
            try {
                tool.validateSelf();
                // no exception so pass
            } catch (InvalidToolException e) {
                fail("FAIL: A valid tool with " + validChars[i] + " in the 'name' is not valid");
            }
        }

        for (int i = 0; i < invalidChars.length; i++) {
            toolDescription = "CN" + invalidChars[i] + "N";
            tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, toolDescription);
            try {
                tool.validateSelf();
                fail("FAIL: A tool with invalid char " + toolDescription + " in the 'name' is valid");
            } catch (InvalidToolException e) {
                // pass
            }
        }
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_FTP() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "ftp://ibm.com", "ftp://ibm.com/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with non-HTTP or HTTPS URL protocols was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_SSH() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "ssh://ibm.com", "ssh://ibm.com/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with non-HTTP or HTTPS URL protocols was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_FileURL() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "file:/ibm", "file:/ibm/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with non-HTTP or HTTPS URL protocols was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_MisspelledHTTP() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "htp://ibm.com", "htp://ibm.com/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with incorrectly spelled HTTP or HTTPS URL was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_MisspelledHTTPS() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "htps://ibm.com", "htps://ibm.com/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with incorrectly spelled HTTP or HTTPS URL was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_relativeSubPathURLs() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "../toolA", "../toolA/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with relative path elements was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_relativeSubPathURLs2() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "a/../toolA", "a/../toolA/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with relative path elements was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_relativeSubPathURLs3() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "..", "..", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with relative path elements was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_relativeSubPathURLs4() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "/..", "/..", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with relative path elements was considered valid");
    }

    /** {@inheritDoc} */
    @Override
    protected void extraPojoMatchesSourceJSONChecks(SelfValidatingPOJO unmarshalledPojo) throws Exception {
        unmarshalledPojo.validateSelf();
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameInstance() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: The same FeatureTool instance did not compare as equals=true",
                     t1, t1);
        assertEquals("FAIL: The same FeatureTool instance did not compare hashCode as equals=true",
                     t1.hashCode(), t1.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameValues() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: Two equal FeatureTool with only required fields did not compare as equals",
                     t1, t2);
        assertEquals("FAIL: Two equal FeatureTool with only required fields did not compare hashCode as equals",
                     t1.hashCode(), t2.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameValuesEmpty() {
        final ITool t1 = new FeatureTool();
        final ITool t2 = new FeatureTool();
        assertEquals("FAIL: Two equal FeatureTool with only required fields did not compare as equals",
                     t1, t2);
        assertEquals("FAIL: Two equal FeatureTool with only required fields did not compare hashCode as equals",
                     t1.hashCode(), t2.hashCode());
    }

    @Test
    public void equalsNullThisId() {
        final ITool t1 = new FeatureTool(null, TOOL_FEATURE_SHORT_NAME, TOOL_TYPE, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'id' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatId() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(null, TOOL_FEATURE_SHORT_NAME, TOOL_TYPE, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'id' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchId() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool("yourURL", TOOL_FEATURE_SHORT_NAME, TOOL_TYPE, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'id' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisType() {
        final ITool t1 = new FeatureTool(TOOL_ID, null, TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'type' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatType() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_ID, null, TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'type' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchType() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_ID, ITool.TYPE_BOOKMARK, TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'type' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisName() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, null, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'name' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatName() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, null, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'name' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchName() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, "yourURL", TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'name' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisShortName() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, null, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'name' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatShortName() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, null, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'name' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchShortName() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, "other-1.0", TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'name' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisURL() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, null, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'url' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatURL() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, null, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'url' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchURL() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, "google.com", TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'url' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisIcon() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, null, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatIcon() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, null, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchIcon() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, "other.png", TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisDescription() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, null);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatDescription() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, null);
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchDescription() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, "other desc");
        assertFalse("FAIL: Two FeatureTool considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNotAFeatureTool() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: A non-FeatureTool object was considered to equal a FeatureTool",
                    t1.equals(new Object()));
    }

    @Test
    public void equalsNull() {
        final ITool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Null was conisdered to equal a Tool",
                    t1.equals(null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#hashCode()}.
     */
    @Test
    public void hashCode_nullId() {
        final ITool tool = new FeatureTool(null, TOOL_TYPE, TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: when the ID is null (which is not a valid case) hashcode should be 0",
                     0, tool.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#hashCode()}.
     */
    @Test
    public void hashCode_hasId() {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: when the ID is set hashcode should be non-zero",
                    0 == tool.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#getMyJson(JSON)}.
     */
    @Test
    public void getMyJsonJsonGenerationException() throws Exception {
        final FeatureTool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);

        mock.checking(new Expectations() {
            {
                one(mockJson).stringify(t1);
                will(throwException(new JSONMarshallException("TestException")));
            }
        });

        assertNull("FAIL: Mapper throwing an exception should result in null JSON",
                   t1.getMyJson(mockJson));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#getMyJson(JSON)}.
     */
    @Test
    public void getMyJsonJsonMappingException() throws Exception {
        final FeatureTool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);

        mock.checking(new Expectations() {
            {
                one(mockJson).stringify(t1);
                will(throwException(new JSONMarshallException("TestException")));
            }
        });

        assertNull("FAIL: Mapper throwing an exception should result in null JSON",
                   t1.getMyJson(mockJson));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#getMyJson(JSON)}.
     */
    @Test
    public void getMyJsonIOException() throws Exception {
        final FeatureTool t1 = new FeatureTool(TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);

        mock.checking(new Expectations() {
            {
                one(mockJson).stringify(t1);
                will(throwException(new JSONMarshallException("TestException")));
            }
        });

        assertNull("FAIL: Mapper throwing an exception should result in null JSON",
                   t1.getMyJson(mockJson));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#toString()}.
     */
    @Test
    public void toString_emptyObject() {
        final ITool tool = new FeatureTool();
        assertEquals("FAIL: the empty tool object did not return the expected toString",
                     "FeatureTool {\"id\":null,\"type\":null,\"featureName\":null,\"featureVersion\":null,\"featureShortName\":null,\"name\":null,\"url\":null,\"icon\":null,\"description\":null}",
                     tool.toString());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.FeatureTool#toString()}.
     */
    @Test
    public void toString_validObject() {
        assertEquals("FAIL: the valid tool object did not return the expected toString",
                     "FeatureTool " + sourceJson, jsonablePojo.toString());
    }

    /**
     * Second incomplete scenario as we have a different code path when no ID is defined.
     */
    @Test
    public void incompleteJsonWithNoId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FeatureTool incompleteObj = mapper.readValue("{\"name\": \"myName\"}", FeatureTool.class);

        try {
            incompleteObj.validateSelf();
            fail("FAIL: A Tool with a mismatched id was considered to be valid");
        } catch (InvalidToolException e) {
            // Pass
        }
        assertFalse("FAIL: An incomplete Tool was considered to equal a valid Tool",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Tool with no 'id' should have a zero hashcode",
                   0 == incompleteObj.hashCode());
    }

    /**
     * Third incomplete scenario as we have a different code path when a mismatched ID is defined.
     */
    @Test
    public void validateSelfWithBadId() throws Exception {
        FeatureTool badId = new FeatureTool("badId", ITool.TYPE_FEATURE_TOOL, TOOL_FEATURE_NAME, TOOL_FEATURE_VERSION, TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        try {
            badId.validateSelf();
            fail("FAIL: An incomplete Tool was considered to be valid");
        } catch (InvalidToolException e) {
            // Pass
        }
        assertFalse("FAIL: An incomplete Tool was considered to equal a valid Tool",
                    badId.equals(jsonablePojo));
    }

    /**
     * Third incomplete scenario as we have a different code path when a mismatched ID is defined.
     */
    @Test
    public void validateSelfWithMajorVersion() throws Exception {
        FeatureTool testMajorVersion = new FeatureTool(TOOL_FEATURE_NAME, "1", TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        try {
            testMajorVersion.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: A version of 1 failed the validation but should have passed: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void validateSelfWithMajorMinorVersion() throws Exception {
        FeatureTool testMajorMinorVersion = new FeatureTool(TOOL_FEATURE_NAME, "1.0", TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        try {
            testMajorMinorVersion.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: A version of 1.0 failed the validation but should have passed: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void validateSelfWithMajorMinorMicroVersion() throws Exception {
        FeatureTool testMajorMinorMicroVersion = new FeatureTool(TOOL_FEATURE_NAME, "1.0.0", TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        try {
            testMajorMinorMicroVersion.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: A version of 1.0.0 failed the validation but should have passed: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void validateSelfWithMajorMinorMicroQualifierVersion() throws Exception {
        FeatureTool testMajorMinorMicroQualifierVersion = new FeatureTool(TOOL_FEATURE_NAME, "1.0.0.20141028-1330", TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        try {
            testMajorMinorMicroQualifierVersion.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: A version of 1.0.0.20141028-1330 failed the validation but should have passed: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void validateSelfWithMajorMinorMicroQualifierVersion2() throws Exception {
        FeatureTool testMajorMinorMicroQualifierVersion = new FeatureTool(TOOL_FEATURE_NAME, "1.0.0.20141028_1330", TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        try {
            testMajorMinorMicroQualifierVersion.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: A version of 1.0.0.20141028_1330 failed the validation but should have passed: " + e.getLocalizedMessage());
        }
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelfwithInvalidVersion1() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, ITool.TYPE_FEATURE_TOOL, TOOL_FEATURE_NAME, "1.0.0.!!!!", TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A version containing invalid chars did not throw an InvalidToolException");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelfwithInvalidVersion2() throws Exception {
        final ITool tool = new FeatureTool(TOOL_FEATURE_NAME, ITool.TYPE_FEATURE_TOOL, TOOL_FEATURE_NAME, "1.a.0", TOOL_FEATURE_SHORT_NAME, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A version containing invalid chars did not throw an InvalidToolException");
    }

}