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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.utils.LogRule;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

import test.common.JSONablePOJOTestCase;
import test.common.SharedOutputManager;

/**
 *
 */
public class BookmarkTest extends JSONablePOJOTestCase {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = new LogRule(outputMgr);
    
    private final String TOOL_ID = "myURL";
    private final String TOOL_TYPE = ITool.TYPE_BOOKMARK;
    private final String TOOL_NAME = "myURL";
    private final String TOOL_URL = "ibm.com";
    private final String TOOL_ICON = "icon.png";
    private final String TOOL_DESCRIPTION = "a really great tool";

    @Before
    public void setUp() {
        jsonablePojo = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        sourceJson = "{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}";
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#Bookmark(String, String, String, String, String, String)}.
     */
    @Test
    public void getters() {
        Bookmark tool = new Bookmark(TOOL_ID, TOOL_TYPE, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: Bookmark did not return the expected ID",
                     TOOL_ID, tool.getId());
        assertEquals("FAIL: Bookmark did not return the expected type",
                     TOOL_TYPE, tool.getType());
        assertEquals("FAIL: Bookmark did not return the expected name",
                     TOOL_NAME, tool.getName());
        assertEquals("FAIL: Bookmark did not return the expected url",
                     TOOL_URL, tool.getURL());
        assertEquals("FAIL: Bookmark did not return the expected icon",
                     TOOL_ICON, tool.getIcon());
        assertEquals("FAIL: Bookmark did not return the expected description",
                     TOOL_DESCRIPTION, tool.getDescription());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#Bookmark(String, String, String, String)}.
     */
    @Test
    public void getters_implied() {
        Bookmark tool = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: Bookmark did not return the expected ID",
                     TOOL_NAME, tool.getId());
        assertEquals("FAIL: Bookmark did not return the expected type",
                     ITool.TYPE_BOOKMARK, tool.getType());
        assertEquals("FAIL: Bookmark did not return the expected name",
                     TOOL_NAME, tool.getName());
        assertEquals("FAIL: Bookmark did not return the expected icon",
                     TOOL_ICON, tool.getIcon());
        assertEquals("FAIL: Bookmark did not return the expected url",
                     TOOL_URL, tool.getURL());
        assertEquals("FAIL: Bookmark did not return the expected description",
                     TOOL_DESCRIPTION, tool.getDescription());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Override
    @Test
    public void incompleteJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{}", Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id, type"));
        }
        assertFalse("FAIL: An incomplete Bookmark was considered to equal a valid Bookmark",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Bookmark should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Bookmark was considered to equal a valid Bookmark",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Bookmark should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":null,\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Bookmark was considered to equal a valid Bookmark",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Bookmark should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Bookmark was considered to equal a valid Bookmark",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Bookmark should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_wrongId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"otherId\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Bookmark was considered to equal a valid Bookmark",
                    incompleteObj.equals(jsonablePojo));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("type"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":null,\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("type"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("type"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_incorrectType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"featureTool\"}", Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not match the expected format. Message: " + e.getMessage(),
                       e.getMessage().matches(".*" + ITool.TYPE_BOOKMARK + ".*" + ITool.TYPE_FEATURE_TOOL + ".*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("name"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":null,\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("name"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("name"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("url"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":null,\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("url"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"\",\"icon\":\"icon.png\",\"description\":\"a really great tool\"}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("url"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingIcon() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\"}", Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("icon"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullIcon() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":null}",
                                                  Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("icon"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyIcon() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"\"}", Bookmark.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("icon"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingDescription() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\"}",
                                                  Bookmark.class);
        incompleteObj.validateSelf();
        // Pass - the description is allowed to be missing, null or empty
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullDescription() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":null}",
                                                  Bookmark.class);
        incompleteObj.validateSelf();
        // Pass - the description is allowed to be missing, null or empty
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyDescription() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Bookmark incompleteObj = mapper.readValue("{\"id\":\"myURL\",\"type\":\"bookmark\",\"name\":\"myURL\",\"url\":\"ibm.com\",\"icon\":\"icon.png\",\"description\":\"\"}",
                                                  Bookmark.class);
        incompleteObj.validateSelf();
        // Pass - the description is allowed to be missing, null or empty
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void validateSelf_featureTool() throws Exception {
        final ITool tool = new Bookmark(TOOL_ID, ITool.TYPE_FEATURE_TOOL, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolException", e);
            assertTrue("FAIL: InvalidToolException message did not match the expected format. Message: " + e.getMessage(),
                       e.getMessage().matches(".*" + ITool.TYPE_BOOKMARK + ".*" + ITool.TYPE_FEATURE_TOOL + ".*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#validateSelf()}.
     */
    @Test
    public void validateSelf_bookmark() throws Exception {
        final ITool tool = new Bookmark(TOOL_ID, ITool.TYPE_BOOKMARK, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        tool.validateSelf();
        // Should pass validation
    }

    @Test
    public void validateSelf_absoluteURLsHTTP() {
        final ITool tool = new Bookmark(TOOL_NAME, "http://cnn.com", "http://cnn.com/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    @Test
    public void validateSelf_russianResource() {
        final ITool tool = new Bookmark(TOOL_NAME, "http://russian.co.ru/\u0420\u0430\u0437\u0432\u0435\u0440\u043d\u0443\u0442\u044c", "http://russian.co.ru/\u0420\u0430\u0437\u0432\u0435\u0440\u043d\u0443\u0442\u044c/icon.png", TOOL_DESCRIPTION);

        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    @Test
    public void validateSelf_russianResourceWithCarats() {
        final ITool tool = new Bookmark(TOOL_NAME, "http://russian.co.ru/\u0420\u0430\u0437\u0432\u0435\u0440\u043d\u0443\u0442<\u044c>", "http://russian.co.ru/\u0420\u0430\u0437\u0432\u0435\u0440\u043d\u0443\u0442<\u044c>/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
            fail("FAIL: A UTF-8 URL with <> characters was not considered invalid");
        } catch (InvalidToolException e) {
            // Pass
        }
    }

    @Test
    public void validateSelf_absoluteURLsHTTPS() {
        final ITool tool = new Bookmark(TOOL_NAME, "https://cnn.com", "https://cnn.com/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    @Test
    public void validateSelf_absoluteURLsHTTPInCaps() {
        final ITool tool = new Bookmark(TOOL_NAME, "HTTP://CNN.COM", "HTTP://CNN.COM/ICON.PNG", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    @Test
    public void validateSelf_absoluteURLsHTTPSInCaps() {
        final ITool tool = new Bookmark(TOOL_NAME, "HTTPS://CNN.COM", "HTTPS://CNN.COM/ICON.PNG", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: HTTP and HTTPS URLs are valid, regardless of case but validation failed");
        }
    }

    @Test
    public void validateSelf_relativeServerURLs() {
        final ITool tool = new Bookmark(TOOL_NAME, "/toolA", "/toolA/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: Server relative URLs are valid but validation failed");
        }
    }

    @Test
    public void validateSelf_relativePageURLs() {
        final ITool tool = new Bookmark(TOOL_NAME, "toolB", "toolB/icon.png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: Page relative URLs are valid but validation failed");
        }
    }

    @Test
    public void validateSelf_relativePageURLsMoreDots() {
        final ITool tool = new Bookmark(TOOL_NAME, "toolB/index...html", "toolB/icon....png", TOOL_DESCRIPTION);
        try {
            tool.validateSelf();
        } catch (InvalidToolException e) {
            fail("FAIL: Page relative URLs are valid (even with a lot of dots!) but validation failed");
        }
    }

    @Ignore("For now, this is a pretty insane case. We need to be using a real library!")
    @Test
    public void validateSelf_relativePageURLsWackyResource() {
        final ITool tool = new Bookmark(TOOL_NAME, "toolB/sub:/index.html", "toolB/sub:/icon.png", TOOL_DESCRIPTION);
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
        final ITool tool = new Bookmark(TOOL_NAME, "http://ibm.com/http://localhost/index.html", "http://ibm.com/http://localhost/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssName() throws Exception {
        final ITool tool = new Bookmark("CN<script>N", TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A tool with XSS in 'name' is not valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssURL() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, "http://cnn.com?<script>alert('a')</script>", TOOL_ICON, TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A tool with XSS in 'url' is not valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssIcon() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, TOOL_URL, "http://i.cdn.turner.com/cnn/.e/<img src='abc'></img>", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A tool with XSS in 'icon' is not valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssDescription() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, "CNN<script src='abc'>");
        tool.validateSelf();
        fail("FAIL: A tool with XSS in 'name' is not valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_xssAll() throws Exception {
        final ITool tool = new Bookmark("CNN<script src='abc'>", "http://cnn.com/<script>altert('hi')</script>", "http://i.cdn.turner.com/cnn/.e/<img src='abc'></img>", "CNN<script src='abc'>");
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
            tool = new Bookmark(toolName, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
            try {
                tool.validateSelf();
                // no exception so pass
            } catch (InvalidToolException e) {
                fail("FAIL: A valid tool with " + validChars[i] + " in the 'name' is not valid");
            }
        }

        for (int i = 0; i < invalidChars.length; i++) {
            toolName = "CN" + invalidChars[i] + "N";
            tool = new Bookmark(toolName, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
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
            tool = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, toolDescription);
            try {
                tool.validateSelf();
                // no exception so pass
            } catch (InvalidToolException e) {
                fail("FAIL: A valid tool with " + validChars[i] + " in the 'name' is not valid");
            }
        }

        for (int i = 0; i < invalidChars.length; i++) {
            toolDescription = "CN" + invalidChars[i] + "N";
            tool = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, toolDescription);
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
        final ITool tool = new Bookmark(TOOL_NAME, "ftp://ibm.com", "ftp://ibm.com/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with non-HTTP or HTTPS URL protocols was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_SSH() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, "ssh://ibm.com", "ssh://ibm.com/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with non-HTTP or HTTPS URL protocols was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_FileURL() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, "file:/ibm", "file:/ibm/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with non-HTTP or HTTPS URL protocols was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_MisspelledHTTP() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, "htp://ibm.com", "htp://ibm.com/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with incorrectly spelled HTTP or HTTPS URL was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_MisspelledHTTPS() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, "htps://ibm.com", "htps://ibm.com/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with incorrectly spelled HTTP or HTTPS URL was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_relativeSubPathURLs() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, "../toolA", "../toolA/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with relative path elements was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_relativeSubPathURLs2() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, "a/../toolA", "a/../toolA/icon.png", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with relative path elements was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_relativeSubPathURLs3() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, "..", "..", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with relative path elements was considered valid");
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_relativeSubPathURLs4() throws Exception {
        final ITool tool = new Bookmark(TOOL_NAME, "/..", "/..", TOOL_DESCRIPTION);
        tool.validateSelf();
        fail("FAIL: A URL with relative path elements was considered valid");
    }

    @Test
    public void validateSelf_exploreAppURL() throws Exception {
        final ITool tool = new Bookmark("explore server app URL", "https://localhost:9443/adminCenter/#explore/servers/localhost,/home/was/IBM/wlp/usr,server1/apps/lab", "prod.png", TOOL_DESCRIPTION);
        tool.validateSelf();
    }

    @Test
    public void validateSelf_exploreSearchURL() throws Exception {
        final ITool tool = new Bookmark("explore search URL", "https://localhost:9443/adminCenter/#explore/search/?type=application&name=lab&tag=~eq~prod", "prod.png", TOOL_DESCRIPTION);
        tool.validateSelf();
    }

    @Test(expected = InvalidToolException.class)
    public void validateSelf_twoPorts() throws Exception {
        final ITool tool = new Bookmark("explore search URL", "https://localhost:9443:9000/adminCenter/#explore/search/?type=application&name=lab&tag=~eq~prod", "prod.png", TOOL_DESCRIPTION);
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
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: The same Bookmark instance did not compare as equals=true",
                     t1, t1);
        assertEquals("FAIL: The same Bookmark instance did not compare hashCode as equals=true",
                     t1.hashCode(), t1.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameValues() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: Two equal Bookmark with only required fields did not compare as equals",
                     t1, t2);
        assertEquals("FAIL: Two equal Bookmark with only required fields did not compare hashCode as equals",
                     t1.hashCode(), t2.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameValuesEmpty() {
        final ITool t1 = new Bookmark();
        final ITool t2 = new Bookmark();
        assertEquals("FAIL: Two equal Bookmark with only required fields did not compare as equals",
                     t1, t2);
        assertEquals("FAIL: Two equal Bookmark with only required fields did not compare hashCode as equals",
                     t1.hashCode(), t2.hashCode());
    }

    @Test
    public void equalsNullThisId() {
        final ITool t1 = new Bookmark(null, TOOL_TYPE, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'id' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatId() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(null, TOOL_TYPE, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'id' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchId() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark("yourURL", TOOL_TYPE, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'id' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisType() {
        final ITool t1 = new Bookmark(TOOL_ID, null, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'type' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatType() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_ID, null, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'type' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchType() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_ID, ITool.TYPE_FEATURE_TOOL, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'type' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisName() {
        final ITool t1 = new Bookmark(null, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'name' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatName() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(null, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'name' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchName() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark("yourURL", TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'name' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisURL() {
        final ITool t1 = new Bookmark(TOOL_NAME, null, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'url' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatURL() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, null, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'url' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchURL() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, "google.com", TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'url' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisIcon() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, null, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatIcon() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, null, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchIcon() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, "other.png", TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisDescription() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, null);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatDescription() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, null);
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchDescription() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        final ITool t2 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, "other desc");
        assertFalse("FAIL: Two Bookmark considered equal when they have different 'icon' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNotABookmark() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: A non-Bookmark object was considered to equal a Bookmark",
                    t1.equals(new Object()));
    }

    @Test
    public void equalsNull() {
        final ITool t1 = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: Null was conisdered to equal a Tool",
                    t1.equals(null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#hashCode()}.
     */
    @Test
    public void hashCode_nullId() {
        final ITool tool = new Bookmark(null, TOOL_TYPE, TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertEquals("FAIL: when the ID is null (which is not a valid case) hashcode should be 0",
                     0, tool.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#hashCode()}.
     */
    @Test
    public void hashCode_hasId() {
        final ITool tool = new Bookmark(TOOL_NAME, TOOL_URL, TOOL_ICON, TOOL_DESCRIPTION);
        assertFalse("FAIL: when the ID is set hashcode should be non-zero",
                    0 == tool.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#toString()}.
     */
    @Test
    public void toString_emptyObject() {
        final ITool tool = new Bookmark();
        assertEquals("FAIL: the empty tool object did not return the expected toString",
                     "Bookmark {\"id\":null,\"type\":null,\"name\":null,\"url\":null,\"icon\":null,\"description\":null}", tool.toString());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Bookmark#toString()}.
     */
    @Test
    public void toString_validObject() {
        assertEquals("FAIL: the valid tool object did not return the expected toString",
                     "Bookmark " + sourceJson, jsonablePojo.toString());
    }

}
