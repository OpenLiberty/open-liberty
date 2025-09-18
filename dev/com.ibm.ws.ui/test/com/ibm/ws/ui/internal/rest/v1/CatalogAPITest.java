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
package com.ibm.ws.ui.internal.rest.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.util.Collections;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.ws.ui.internal.Filter;
import com.ibm.ws.ui.internal.rest.AdminCenterRestHandler.POSTResponse;
import com.ibm.ws.ui.internal.rest.HTTPConstants;
import com.ibm.ws.ui.internal.rest.exceptions.BadRequestException;
import com.ibm.ws.ui.internal.rest.exceptions.MethodNotSupportedException;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.v1.ICatalog;
import com.ibm.ws.ui.internal.v1.ICatalogService;
import com.ibm.ws.ui.internal.v1.pojo.Bookmark;
import com.ibm.ws.ui.internal.v1.pojo.DuplicateException;
import com.ibm.ws.ui.internal.v1.pojo.FeatureTool;
import com.ibm.ws.ui.internal.v1.pojo.Message;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import test.common.SharedOutputManager;

/**
 *
 */
public class CatalogAPITest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final RESTRequest restRequest = mock.mock(RESTRequest.class);
    private final RESTResponse restResponse = mock.mock(RESTResponse.class);
    private final InputStream mockInputStream = mock.mock(InputStream.class);
    private final ICatalogService mockCatalogService = mock.mock(ICatalogService.class);
    private final ICatalog mockCatalog = mock.mock(ICatalog.class);
    private final JSON mockJson = mock.mock(JSON.class);
    private final Filter mockFilter = mock.mock(Filter.class);
    private CatalogAPI handler;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(mockCatalogService).getCatalog();
                will(returnValue(mockCatalog));
            }
        });
        handler = new CatalogAPI(mockCatalogService, mockFilter, mockJson);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Validates the setup of the object matches our expectations. This is a
     * validation and not really a business logic test.
     */
    @Test
    public void objectValidation() {
        handler = new CatalogAPI(mockCatalogService, mockFilter, mockJson);
        assertEquals("The handler's base URL was not what was expected",
                     "/adminCenter/v1/catalog", handler.baseURL());
        assertTrue("The handler should expect to handle children",
                   handler.hasChildren());
    }

    @Test
    public void isKnownChildResource_unknown() {
        assertFalse("When the tool is not known, isKnown should be false",
                    handler.isKnownChildResource("child", restRequest));
    }

    @Test
    public void isKnownChildResource_known() {
        assertTrue("'bookmarks' should be a known child",
                   handler.isKnownChildResource("bookmarks", restRequest));
        assertTrue("'featureTools' should be a known child",
                   handler.isKnownChildResource("featureTools", restRequest));
        assertTrue("'_metadata' should be a known child",
                   handler.isKnownChildResource("_metadata", restRequest));
    }

    @Test
    public void isKnownGrandchildResource_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockCatalog).getBookmark("unknown");
                will(returnValue(null));

                one(mockCatalog).getFeatureTool("unknown");
                will(returnValue(null));
            }
        });
        assertFalse("When the child is not known, isKnown should be false",
                    handler.isKnownGrandchildResource("unknown", "ignored", restRequest));
        assertFalse("When the grandchild is not known, isKnown should be false",
                    handler.isKnownGrandchildResource("bookmarks", "unknown", restRequest));
        assertFalse("When the grandchild is not known, isKnown should be false",
                    handler.isKnownGrandchildResource("featureTools", "unknown", restRequest));
    }

    @Test
    public void isKnownGrandchildResource_known() {
        mock.checking(new Expectations() {
            {
                one(mockCatalog).getBookmark("known");
                will(returnValue(new Bookmark("", "", "")));

                one(mockCatalog).getFeatureTool("known");
                will(returnValue(new FeatureTool("", "", "", "", "", "", "")));
            }
        });
        assertTrue("Any 'bookmarks' should be a known grandchild",
                   handler.isKnownGrandchildResource("bookmarks", "known", restRequest));
        assertTrue("Any 'featureTools' should be a known grandchild",
                   handler.isKnownGrandchildResource("featureTools", "known", restRequest));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void getBase_catalog() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
                one(restRequest).getParameter("fields");
                will(returnValue(null));
                one(mockFilter).applyFieldFilter(null, mockCatalog);
                will(returnValue(mockCatalog));
            }
        });

        assertSame("Did not get Catalog back",
                   mockCatalog, handler.getBase(restRequest, restResponse));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
    public void getChild_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.getChild(restRequest, restResponse, "unknown");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_metadata() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockCatalog).get_metadata();
                will(returnValue(Collections.EMPTY_MAP));

                one(restRequest).getParameter("fields");
                will(returnValue(null));
                one(mockFilter).applyFieldFilter(null, Collections.EMPTY_MAP);
                will(returnValue(Collections.EMPTY_MAP));
            }
        });

        assertSame("Did not get mock Catalog response back",
                   Collections.EMPTY_MAP, handler.getChild(restRequest, restResponse, "_metadata"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_featureTools() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockCatalog).getFeatureTools();
                will(returnValue(Collections.EMPTY_LIST));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, Collections.EMPTY_LIST);
                will(returnValue(Collections.EMPTY_LIST));
            }
        });

        assertSame("Did not get mock Catalog response back",
                   Collections.EMPTY_LIST, handler.getChild(restRequest, restResponse, "featureTools"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_bookmarks() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockCatalog).getBookmarks();
                will(returnValue(Collections.EMPTY_LIST));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, Collections.EMPTY_LIST);
                will(returnValue(Collections.EMPTY_LIST));
            }
        });

        assertSame("Did not get mock Catalog response back",
                   Collections.EMPTY_LIST, handler.getChild(restRequest, restResponse, "bookmarks"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
    public void getGrandchild_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.getGrandchild(restRequest, restResponse, "unknown", "ignored");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void getGrandchild_featureToolDoesntExist() throws Exception {
        final String toolId = "doesntExist";
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                allowing(restRequest).getPath();
                will(returnValue(V1Constants.CATALOG_PATH + "/" + toolId));

                one(mockCatalog).getFeatureTool(toolId);
                will(returnValue(null));
            }
        });

        try {
            handler.getGrandchild(restRequest, restResponse, "featureTools", toolId);
            fail("Should have thrown a NoSuchhandlerException");
        } catch (NoSuchResourceException e) {
            Message error = (Message) e.getPayload();
            assertTrue("Thrown exception did not contain an ErrorMessage with the expected translated message",
                       error.getMessage().matches("CWWKX1001E:.*" + toolId + ".*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getGrandchild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getGrandchild_featureToolExists() throws Exception {
        final String toolId = "myName-1.0";
        final FeatureTool myTool = new FeatureTool("feature", "1.0", "tool-1.0", "my Name", "myURL", "myIcon", "myDescription");
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                allowing(restRequest).getPath();
                will(returnValue(V1Constants.CATALOG_PATH + "/" + toolId));

                one(mockCatalog).getFeatureTool(toolId);
                will(returnValue(myTool));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, myTool);
                will(returnValue(myTool));
            }
        });

        assertSame("Did not get back mock returned tool",
                   myTool, handler.getGrandchild(restRequest, restResponse, "featureTools", toolId));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getGrandchild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getGrandchild_featureToolIdWithSpaceExists() throws Exception {
        final FeatureTool myTool = new FeatureTool("my Name", "1.0", "tool-1.0", "my Name", "myURL", "myIcon", "myDescription");
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));

                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockCatalog).getFeatureTool("my Name-1.0");
                will(returnValue(myTool));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, myTool);
                will(returnValue(myTool));
            }
        });

        assertSame("Did not get back mock returned tool",
                   myTool, handler.getGrandchild(restRequest, restResponse, "featureTools", "my Name-1.0"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void getGrandchild_bookmarkDoesntExist() throws Exception {
        final String toolId = "doesntExist";
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + V1Constants.CATALOG_PATH + "/" + toolId));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                one(mockCatalog).getBookmark(toolId);
                will(returnValue(null));
            }
        });

        try {
            handler.getGrandchild(restRequest, restResponse, "bookmarks", toolId);
            fail("Should have thrown a NoSuchhandlerException");
        } catch (NoSuchResourceException e) {
            Message error = (Message) e.getPayload();
            assertTrue("Thrown exception did not contain an ErrorMessage with the expected translated message",
                       error.getMessage().matches("CWWKX1001E:.*" + toolId + ".*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getGrandchild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getGrandchild_toolExists() throws Exception {
        final String toolId = "myName-1.0";
        final Bookmark myTool = new Bookmark("myName", "myURL", "myIcon", "myDescription");
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + V1Constants.CATALOG_PATH + "/" + toolId));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                one(mockCatalog).getBookmark(toolId);
                will(returnValue(myTool));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, myTool);
                will(returnValue(myTool));
            }
        });

        assertSame("Did not get back mock returned tool",
                   myTool, handler.getGrandchild(restRequest, restResponse, "bookmarks", toolId));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#getGrandchild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getGrandchild_toolIdWithSpaceExists() throws Exception {
        final Bookmark myTool = new Bookmark("my Name", "myURL", "myIcon", "myDescription");
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockCatalog).getBookmark("my Name");
                will(returnValue(myTool));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, myTool);
                will(returnValue(myTool));
            }
        });

        assertSame("Did not get back mock returned tool",
                   myTool, handler.getGrandchild(restRequest, restResponse, "bookmarks", "my Name"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test(expected = MethodNotSupportedException.class)
    public void postChild_notBookmarks() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.postChild(restRequest, restResponse, "notBookmarks");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_noToolPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(mockInputStream));

                one(mockInputStream).read(with(any(byte[].class)));
                will(throwException(new EOFException("TestException")));

                one(mockInputStream).close();
            }
        });

        try {
            handler.postChild(restRequest, restResponse, CatalogAPI.CHILD_RESOURCE_BOOKMARKS);
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertTrue("ErrorMessage message start with CWWKX1019E and contain Tool. Message: " + error.getMessage(),
                       error.getMessage().matches("CWWKX1019E:.*Bookmark.*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_invalidTool() throws Exception {
        final String mockPayload = "{\"name\":\"myName\",\"url\":\"myURL\",\"icon\":\"myIcon\",\"description\":\"myDescription\"}";
        final Bookmark mockBk = mock.mock(Bookmark.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Bookmark.class);
                will(returnValue(mockBk));

                one(mockCatalog).addBookmark(with(any(Bookmark.class)));
                will(throwException(new InvalidToolException("TestException")));
            }
        });

        try {
            handler.postChild(restRequest, restResponse, CatalogAPI.CHILD_RESOURCE_BOOKMARKS);
            fail("Posting an invalid tool should result in a BadRequestException");
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertEquals("ErrorMessage message should be the exception message",
                         "TestException", error.getMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_duplicateTool() throws Exception {
        final String mockPayload = "{\"name\":\"myName\",\"url\":\"myURL\",\"icon\":\"myIcon\",\"description\":\"myDescription\"}";
        final Bookmark mockBk = mock.mock(Bookmark.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Bookmark.class);
                will(returnValue(mockBk));

                one(mockCatalog).addBookmark(with(any(Bookmark.class)));
                will(throwException(new DuplicateException("TestException")));
            }
        });

        try {
            handler.postChild(restRequest, restResponse, CatalogAPI.CHILD_RESOURCE_BOOKMARKS);
            fail("Posting a duplicate tool should result in a RESTException");
        } catch (RESTException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 409 (conflict)",
                         409, error.getStatus());
            assertEquals("ErrorMessage message should be the exception message",
                         "TestException", error.getMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_validTool() throws Exception {
        final String mockPayload = "{\"id\":\"myName\",\"type\":\"Bookmark\",\"name\":\"myName\",\"url\":\"myURL\",\"icon\":\"myIcon\",\"description\":\"myDescription\"}";
        final Bookmark myTool = new Bookmark("myName", "myURL", "myIcon", "myDescription");
        final String url = "ibm/api/adminCenter/v1/catalog";
        final String createdURL = url + "/" + myTool.getId();
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Bookmark.class);
                will(returnValue(myTool));

                one(mockCatalog).addBookmark(with(any(Bookmark.class)));
                will(returnValue(myTool));

                one(restRequest).getURL();
                will(returnValue(url));
            }
        });

        POSTResponse response = handler.postChild(restRequest, restResponse, CatalogAPI.CHILD_RESOURCE_BOOKMARKS);
        assertEquals("Did not get back the added tool",
                     myTool, response.jsonPayload);
        assertEquals("Did not get back the expected created URL",
                     createdURL, response.createdURL);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_impliedTool() throws Exception {
        final String mockPayload = "{\"name\":\"myName\",\"url\":\"myURL\",\"icon\":\"myIcon\",\"description\":\"myDescription\"}";
        final Bookmark myTool = new Bookmark("myName", "myURL", "myIcon", "myDescription");
        final String url = "ibm/api/adminCenter/v1/catalog";
        final String createdURL = url + "/" + myTool.getId();
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Bookmark.class);
                will(returnValue(myTool));

                one(mockCatalog).addBookmark(with(any(Bookmark.class)));
                will(returnValue(myTool));

                one(restRequest).getURL();
                will(returnValue(url));
            }
        });

        POSTResponse response = handler.postChild(restRequest, restResponse, CatalogAPI.CHILD_RESOURCE_BOOKMARKS);
        assertEquals("Did not get back the added tool",
                     myTool, response.jsonPayload);
        assertEquals("Did not get back the expected created URL",
                     createdURL, response.createdURL);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#deleteBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void deleteBase_noConfirmation() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getParameter("resetCatalog");
                will(returnValue(null));
            }
        });

        try {
            handler.deleteBase(restRequest, restResponse);
            fail("Expected BadRequestException to be thrown");
        } catch (BadRequestException e) {
            Message msg = (Message) e.getPayload();
            assertEquals("FAIL: Even though the request did not complete, we want to response with 400",
                         400, msg.getStatus());
            assertTrue("FAIL: The NLS message CWWKX1024E was not the content of the message",
                       msg.getMessage().startsWith("CWWKX1024E"));
            assertNotNull("FAIL: The developer message was not set",
                          msg.getDeveloperMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#deleteBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void deleteBase_wrongConfirmation() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getParameter("resetCatalog");
                will(returnValue("false"));
            }
        });

        try {
            handler.deleteBase(restRequest, restResponse);
            fail("Expected BadRequestException to be thrown");
        } catch (BadRequestException e) {
            Message msg = (Message) e.getPayload();
            assertEquals("FAIL: Even though the request did not complete, we want to response with 400",
                         400, msg.getStatus());
            assertTrue("FAIL: The NLS message CWWKX1024E was not the content of the message",
                       msg.getMessage().startsWith("CWWKX1024E"));
            assertNotNull("FAIL: The developer message was not set",
                          msg.getDeveloperMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#deleteBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void deleteBase_goodConfirmation() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getParameter("resetCatalog");
                will(returnValue("true"));

                one(mockCatalog).reset();
            }
        });

        Message msg = (Message) handler.deleteBase(restRequest, restResponse);
        assertEquals("FAIL: Even though the request did not complete, we want to response with 200",
                     200, msg.getStatus());
        assertTrue("FAIL: The NLS message CWWKX1023I was not the content of the message",
                   msg.getMessage().startsWith("CWWKX1023I"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test(expected = MethodNotSupportedException.class)
    public void deleteGrandchild_notBookmarks() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.deleteGrandchild(restRequest, restResponse, "notBookmarks", "ignored");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String}.
     */
    @Test
    public void deleteGrandchild_noSuchTool() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockCatalog).deleteBookmark("noSuchTool");
                will(returnValue(null));
            }
        });

        try {
            handler.deleteGrandchild(restRequest, restResponse, CatalogAPI.CHILD_RESOURCE_BOOKMARKS, "noSuchTool");
            fail("DELETE on a tool which is not defiend should result in a NoSuchhandlerException");
        } catch (NoSuchResourceException e) {
            Message error = (Message) e.getPayload();
            assertTrue("Exception message did not match expected 'CWWKX1001E:.*noSuchTool.*'",
                       error.getMessage().matches("CWWKX1001E:.*noSuchTool.*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String}.
     */
    @Test
    public void deleteGrandchild_definedTool() throws Exception {
        final Bookmark myTool = new Bookmark("myName", "myURL", "myIcon", "myDescription");
        final String toolId = myTool.getId();

        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockCatalog).deleteBookmark(toolId);
                will(returnValue(myTool));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, myTool);
                will(returnValue(myTool));
            }
        });

        Bookmark removedTool = (Bookmark) handler.deleteGrandchild(restRequest, restResponse, CatalogAPI.CHILD_RESOURCE_BOOKMARKS, toolId);
        assertEquals("The returned (deleted) tool did not match the mock'd return",
                     myTool, removedTool);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test(expected = MethodNotSupportedException.class)
    public void deleteGrandchild_knownFeatureTool() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockCatalog).getFeatureTool("known");
                will(returnValue(new FeatureTool("", "", "", "", "", "", "")));
            }
        });

        handler.deleteGrandchild(restRequest, restResponse, "featureTools", "known");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.CatalogAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
    public void deleteGrandchild_unknownFeatureTool() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockCatalog).getFeatureTool("unknown");
                will(returnValue(null));
            }
        });

        handler.deleteGrandchild(restRequest, restResponse, "featureTools", "unknown");
    }
}
