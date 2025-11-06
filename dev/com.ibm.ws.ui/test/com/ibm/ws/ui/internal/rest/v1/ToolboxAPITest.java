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
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.ws.ui.internal.Filter;
import com.ibm.ws.ui.internal.rest.AdminCenterRestHandler.POSTResponse;
import com.ibm.ws.ui.internal.rest.HTTPConstants;
import com.ibm.ws.ui.internal.rest.exceptions.BadRequestException;
import com.ibm.ws.ui.internal.rest.exceptions.MethodNotSupportedException;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.v1.IToolbox;
import com.ibm.ws.ui.internal.v1.IToolboxService;
import com.ibm.ws.ui.internal.v1.pojo.Bookmark;
import com.ibm.ws.ui.internal.v1.pojo.DuplicateException;
import com.ibm.ws.ui.internal.v1.pojo.Message;
import com.ibm.ws.ui.internal.v1.pojo.NoSuchToolException;
import com.ibm.ws.ui.internal.v1.pojo.ToolEntry;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import test.common.SharedOutputManager;

/**
 *
 */
public class ToolboxAPITest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private static final String USER_ID = "bob_johnson";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final RESTRequest restRequest = mock.mock(RESTRequest.class);
    private final RESTResponse restResponse = mock.mock(RESTResponse.class);
    private final Principal mockPrincipal = mock.mock(Principal.class);
    private final IToolboxService mockToolboxService = mock.mock(IToolboxService.class);
    private final IToolbox mockToolbox = mock.mock(IToolbox.class);
    private final JSON mockJson = mock.mock(JSON.class);
    private final Filter mockFilter = mock.mock(Filter.class);

    private ToolboxAPI handler;

    @Before
    public void setUp() {
        // We always look up the user, just mock it once and let it happen
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getUserPrincipal();
                will(returnValue(mockPrincipal));

                allowing(mockPrincipal).getName();
                will(returnValue(USER_ID));

                allowing(mockToolboxService).getToolbox(USER_ID);
                will(returnValue(mockToolbox));
            }
        });

        handler = new ToolboxAPI(mockToolboxService, mockFilter, mockJson);
    }

    @After
    public void tearDown() {
        handler = null;

        mock.assertIsSatisfied();
    }

    /**
     * Validates the setup of the object matches our expectations. This is a
     * validation and not really a business logic test.
     */
    @Test
    public void objectValidation() {
        handler = new ToolboxAPI(mockToolboxService, mockFilter, mockJson);
        assertEquals("The handler's base URL was not what was expected",
                     "/adminCenter/v1/toolbox", handler.baseURL());
        assertTrue("The handler should expect to handle children",
                   handler.hasChildren());
    }

    @Test
    public void isKnownChildResource_unknown() throws Exception {
        assertFalse("When the child is not known, isKnown should be false",
                    handler.isKnownChildResource("unknown", restRequest));
    }

    @Test
    public void isKnownChildResource_known() throws Exception {
        assertTrue("'bookmarks' should be a known child",
                   handler.isKnownChildResource("bookmarks", restRequest));
        assertTrue("'toolEntries' should be a known child",
                   handler.isKnownChildResource("toolEntries", restRequest));
        assertTrue("'_metadata' should be a known child",
                   handler.isKnownChildResource("_metadata", restRequest));
        assertTrue("'preferences' should be a known child",
                   handler.isKnownChildResource("preferences", restRequest));
    }

    @Test
    public void isKnownGrandchildResource_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockToolbox).getBookmark("unknown");
                will(returnValue(null));

                one(mockToolbox).getToolEntry("unknown");
                will(returnValue(null));
            }
        });
        assertFalse("When the child is not known, isKnown should be false",
                    handler.isKnownGrandchildResource("unknown", "ignored", restRequest));
        assertFalse("When the grandchild is not known, isKnown should be false",
                    handler.isKnownGrandchildResource("bookmarks", "unknown", restRequest));
        assertFalse("When the grandchild is not known, isKnown should be false",
                    handler.isKnownGrandchildResource("toolEntries", "unknown", restRequest));
    }

    @Test
    public void isKnownGrandchildResource_known() {
        mock.checking(new Expectations() {
            {
                one(mockToolbox).getBookmark("known");
                will(returnValue(new Bookmark("", "", "")));

                one(mockToolbox).getToolEntry("known");
                will(returnValue(new ToolEntry("", "")));
            }
        });
        assertTrue("Any 'bookmarks' should be a known grandchild",
                   handler.isKnownGrandchildResource("bookmarks", "known", restRequest));
        assertTrue("Any 'toolEntries' should be a known grandchild",
                   handler.isKnownGrandchildResource("toolEntries", "known", restRequest));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void getBase() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
                one(restRequest).getParameter("fields");
                will(returnValue(null));
                one(mockFilter).applyFieldFilter(null, mockToolbox);
                will(returnValue(mockToolbox));
            }
        });

        assertSame("Did not get Toolbox back",
                   mockToolbox, handler.getBase(restRequest, restResponse));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
    public void getChild_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.getChild(restRequest, restResponse, "unknown");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_metadata() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).get_metadata();
                will(returnValue(Collections.EMPTY_MAP));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, Collections.EMPTY_MAP);
                will(returnValue(Collections.EMPTY_MAP));
            }
        });

        assertSame("Did not get back the EmptyMap which should have been returned by the mock",
                   Collections.EMPTY_MAP, handler.getChild(restRequest, restResponse, "_metadata"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_preferences() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).getPreferences();
                will(returnValue(Collections.EMPTY_MAP));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, Collections.EMPTY_MAP);
                will(returnValue(Collections.EMPTY_MAP));
            }
        });

        assertSame("Did not get back the EmptyMap which should have been returned by the mock",
                   Collections.EMPTY_MAP, handler.getChild(restRequest, restResponse, "preferences"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_toolEntries() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).getToolEntries();
                will(returnValue(Collections.EMPTY_LIST));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, Collections.EMPTY_LIST);
                will(returnValue(Collections.EMPTY_LIST));
            }
        });

        assertSame("Did not get back the EmptyMap which should have been returned by the mock",
                   Collections.EMPTY_LIST, handler.getChild(restRequest, restResponse, "toolEntries"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_bookmarks() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).getBookmarks();
                will(returnValue(Collections.EMPTY_LIST));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, Collections.EMPTY_LIST);
                will(returnValue(Collections.EMPTY_LIST));
            }
        });

        assertSame("Did not get back the EmptyMap which should have been returned by the mock",
                   Collections.EMPTY_LIST, handler.getChild(restRequest, restResponse, "bookmarks"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
    public void getGrandchild_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.getGrandchild(restRequest, restResponse, "unknown", "ignored");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void getGrandchild_toolDoesntExist() throws Exception {
        final String toolId = "doesntExist";
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                allowing(restRequest).getPath();
                will(returnValue(V1Constants.TOOLBOX_PATH + "/" + toolId));

                one(mockToolbox).getToolEntry(toolId);
                will(returnValue(null));
            }
        });

        try {
            handler.getGrandchild(restRequest, restResponse, "toolEntries", toolId);
            fail("Should have thrown a NoSuchhandlerException");
        } catch (NoSuchResourceException e) {
            Message error = (Message) e.getPayload();
            System.out.println("error: " + error);
            assertTrue("Thrown exception did not contain an ErrorMessage with the expected translated message",
                       error.getMessage().matches("CWWKX1022E:.*" + toolId + ".*" + USER_ID + ".*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void getGrandchild_toolExists() throws Exception {
        final String toolId = "myName-1.0";
        final Bookmark myTool = new Bookmark("myName", "myURL", "myIcon");
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                allowing(restRequest).getPath();
                will(returnValue(V1Constants.TOOLBOX_PATH + "/" + toolId));

                one(mockToolbox).getToolEntry(toolId);
                will(returnValue(myTool));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, myTool);
                will(returnValue(myTool));
            }
        });

        assertSame("Did not get back mock returned tool",
                   myTool, handler.getGrandchild(restRequest, restResponse, "toolEntries", toolId));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void getGrandchild_toolIdWithSpaceExists() throws Exception {
        final Bookmark myTool = new Bookmark("my Name", "myURL", "myIcon");
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).getToolEntry("my Name");
                will(returnValue(myTool));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, myTool);
                will(returnValue(myTool));
            }
        });

        assertSame("Did not get back mock returned tool",
                   myTool, handler.getGrandchild(restRequest, restResponse, "toolEntries", "my Name"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void getGrandchild_bookmarkDoesntExist() throws Exception {
        final String toolId = "doesntExist";
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                allowing(restRequest).getPath();
                will(returnValue(V1Constants.TOOLBOX_PATH + "/" + toolId));

                one(mockToolbox).getBookmark(toolId);
                will(returnValue(null));
            }
        });

        try {
            handler.getGrandchild(restRequest, restResponse, "bookmarks", toolId);
            fail("Should have thrown a NoSuchhandlerException");
        } catch (NoSuchResourceException e) {
            Message error = (Message) e.getPayload();
            assertTrue("Thrown exception did not contain an ErrorMessage with the expected translated message",
                       error.getMessage().matches("CWWKX1022E:.*" + toolId + ".*" + USER_ID + ".*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void getGrandchild_bookmarkExists() throws Exception {
        final String toolId = "myName-1.0";
        final Bookmark myTool = new Bookmark("myName", "myURL", "myIcon");
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                allowing(restRequest).getPath();
                will(returnValue(V1Constants.TOOLBOX_PATH + "/" + toolId));

                one(mockToolbox).getBookmark(toolId);
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#getGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void getGrandchild_bookmarkIdWithSpaceExists() throws Exception {
        final Bookmark myTool = new Bookmark("my Name", "myURL", "myIcon");
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).getBookmark("my Name");
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test(expected = MethodNotSupportedException.class)
    public void postChild_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.postChild(restRequest, restResponse, "unknown");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_noBookmarkPayload() throws Exception {
        final InputStream mockInputStream = mock.mock(InputStream.class);
        mock.checking(new Expectations() {
            {
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
            handler.postChild(restRequest, restResponse, "bookmarks");
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_invalidBookmark() throws Exception {
        final String mockPayload = "{\"name\":\"myName\",\"url\":\"myURL\",\"icon\":\"myIcon\"}";
        final Bookmark mockBk = mock.mock(Bookmark.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Bookmark.class);
                will(returnValue(mockBk));

                one(mockToolbox).addBookmark(with(any(Bookmark.class)));
                will(throwException(new InvalidToolException("TestException")));
            }
        });

        try {
            handler.postChild(restRequest, restResponse, "bookmarks");
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_duplicateBookmark() throws Exception {
        final String mockPayload = "{\"name\":\"myName\",\"url\":\"myURL\",\"icon\":\"myIcon\"}";
        final Bookmark mockBk = mock.mock(Bookmark.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Bookmark.class);
                will(returnValue(mockBk));

                one(mockToolbox).addBookmark(with(any(Bookmark.class)));
                will(throwException(new DuplicateException("TestException")));
            }
        });

        try {
            handler.postChild(restRequest, restResponse, "bookmarks");
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_validBookmark() throws Exception {
        final String mockPayload = "{\"id\":\"myName\",\"type\":\"bookmark\",\"name\":\"myName\",\"url\":\"myURL\",\"icon\":\"myIcon\"}";
        final Bookmark myTool = new Bookmark("myName", "myURL", "myIcon");
        final String url = "ibm/api/adminCenter/v1/toolbox";
        final String createdURL = url + "/" + myTool.getId();
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Bookmark.class);
                will(returnValue(myTool));

                one(mockToolbox).addBookmark(with(any(Bookmark.class)));
                will(returnValue(myTool));

                one(restRequest).getURL();
                will(returnValue(url));
            }
        });

        POSTResponse response = handler.postChild(restRequest, restResponse, "bookmarks");
        assertEquals("Did not get back the added tool",
                     myTool, response.jsonPayload);
        assertEquals("Did not get back the expected created URL",
                     createdURL, response.createdURL);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_impliedBookmark() throws Exception {
        final String mockPayload = "{\"name\":\"myName\",\"url\":\"myURL\",\"icon\":\"myIcon\"}";
        final Bookmark myTool = new Bookmark("myName", "myURL", "myIcon");
        final String url = "ibm/api/adminCenter/v1/toolbox";
        final String createdURL = url + "/" + myTool.getId();
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Bookmark.class);
                will(returnValue(myTool));

                one(mockToolbox).addBookmark(with(any(Bookmark.class)));
                will(returnValue(myTool));

                one(restRequest).getURL();
                will(returnValue(url));
            }
        });

        POSTResponse response = handler.postChild(restRequest, restResponse, "bookmarks");
        assertEquals("Did not get back the added tool",
                     myTool, response.jsonPayload);
        assertEquals("Did not get back the expected created URL",
                     createdURL, response.createdURL);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_noToolEntryPayload() throws Exception {
        final InputStream mockInputStream = mock.mock(InputStream.class);
        mock.checking(new Expectations() {
            {
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
            handler.postChild(restRequest, restResponse, "toolEntries");
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertTrue("ErrorMessage message start with CWWKX1019E and contain Tool. Message: " + error.getMessage(),
                       error.getMessage().matches("CWWKX1019E:.*ToolEntry.*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_invalidToolEntry() throws Exception {
        final String mockPayload = "{\"id\":\"myId\",\"type\":\"myType\"}";
        final ToolEntry mockTe = mock.mock(ToolEntry.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, ToolEntry.class);
                will(returnValue(mockTe));

                one(mockToolbox).addToolEntry(with(any(ToolEntry.class)));
                will(throwException(new InvalidToolException("TestException")));
            }
        });

        try {
            handler.postChild(restRequest, restResponse, "toolEntries");
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_duplicateToolEntry() throws Exception {
        final String mockPayload = "{\"id\":\"myId\",\"type\":\"myType\"}";
        final ToolEntry mockTe = mock.mock(ToolEntry.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, ToolEntry.class);
                will(returnValue(mockTe));

                one(mockToolbox).addToolEntry(with(any(ToolEntry.class)));
                will(throwException(new DuplicateException("TestException")));
            }
        });

        try {
            handler.postChild(restRequest, restResponse, "toolEntries");
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_noSuchToolEntry() throws Exception {
        final String mockPayload = "{\"id\":\"myId\",\"type\":\"myType\"}";
        final ToolEntry mockTe = mock.mock(ToolEntry.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, ToolEntry.class);
                will(returnValue(mockTe));

                one(mockToolbox).addToolEntry(with(any(ToolEntry.class)));
                will(throwException(new NoSuchToolException("TestException")));
            }
        });

        try {
            handler.postChild(restRequest, restResponse, "toolEntries");
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_validToolEntry() throws Exception {
        final String mockPayload = "{\"id\":\"myId\",\"type\":\"myType\"}";
        final ToolEntry toolEntry = new ToolEntry("myId", "myType");
        final String url = "ibm/api/adminCenter/v1/toolbox";
        final String createdURL = url + "/" + toolEntry.getId();
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, ToolEntry.class);
                will(returnValue(toolEntry));

                one(mockToolbox).addToolEntry(with(any(ToolEntry.class)));
                will(returnValue(toolEntry));

                one(restRequest).getURL();
                will(returnValue(url));
            }
        });

        POSTResponse response = handler.postChild(restRequest, restResponse, "toolEntries");
        assertEquals("Did not get back the added tool",
                     toolEntry, response.jsonPayload);
        assertEquals("Did not get back the expected created URL",
                     createdURL, response.createdURL);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#putChild(RESTRequest, RESTResponse, String)}.
     */
    @Test(expected = MethodNotSupportedException.class)
    public void putChild_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.putChild(restRequest, restResponse, "unknown");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#putChild(RESTRequest, RESTResponse, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void putChild_preferences() throws Exception {
        final String mockPayload = "{}";
        final Map mockMap = mock.mock(Map.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Map.class);
                will(returnValue(mockMap));

                one(mockToolbox).updatePreferences(with(any(Map.class)));
                will(returnValue(Collections.EMPTY_MAP));
            }
        });

        assertSame("Did not get back the EmptyMap which should have been returned by the mock",
                   Collections.EMPTY_MAP, handler.putChild(restRequest, restResponse, "preferences"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#putChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void putChild_preferencesNonStringKey() throws Exception {
        final String mockPayload = "{123:\"abc\"}";
        final Map<Object, Object> mockMap = mock.mock(Map.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, Map.class);
                will(throwException(new JSONMarshallException(("Unable to parse non-well-formed content"))));
            }
        });

        try {
            handler.putChild(restRequest, restResponse, "preferences");
            fail("Expected BadRequestException to be thrown when the map's keys are non-String");
        } catch (BadRequestException e) {
            Message msg = (Message) e.getPayload();
            assertEquals("FAIL: Even though the request did not complete, we want to response with 400",
                         400, msg.getStatus());
            assertTrue("FAIL: The NLS message CWWKX1020E was not the content of the message. Message: " + msg.getMessage(),
                       msg.getMessage().startsWith("CWWKX1020E"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#putChild(RESTRequest, RESTResponse, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void putChild_toolEntriesIllegalArgumentException() throws Exception {
        // The contents of the array is irrelevant. The mockToolbox drives the behaviour.
        final String mockPayload = "[]";
        final ToolEntry mockTe = mock.mock(ToolEntry.class);
        final ToolEntry[] mockArray = { mockTe };
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                allowing(mockJson).parse(mockPayload, ToolEntry[].class);
                will(returnValue(mockArray));

                one(mockToolbox).updateToolEntries(with(any(List.class)));
                will(throwException(new IllegalArgumentException("TestException")));
            }
        });

        try {
            handler.putChild(restRequest, restResponse, "toolEntries");
            fail("Expected BadRequestException to be thrown when there is duplicate key in the tool entries list");
        } catch (BadRequestException e) {
            Message msg = (Message) e.getPayload();
            assertEquals("FAIL: ErrorMessage status should be 400",
                         400, msg.getStatus());
            assertTrue("FAIL: Message payload did not contain the thrown exception message",
                       msg.getMessage().contains("TestException"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#putChild(RESTRequest, RESTResponse, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void putChild_toolEntriesNoSuchToolException() throws Exception {
        // The contents of the array is irrelevant. The mockToolbox drives the behaviour.
        final String mockPayload = "[]";
        final ToolEntry mockTe = mock.mock(ToolEntry.class);
        final ToolEntry[] mockArray = { mockTe };

        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                allowing(mockJson).parse(mockPayload, ToolEntry[].class);
                will(returnValue(mockArray));

                one(mockToolbox).updateToolEntries(with(any(List.class)));
                will(throwException(new NoSuchToolException("TestException")));
            }
        });

        try {
            handler.putChild(restRequest, restResponse, "toolEntries");
            fail("Expected BadRequestException to be thrown when list has non exist tool entry");
        } catch (BadRequestException e) {
            Message msg = (Message) e.getPayload();
            assertEquals("FAIL: ErrorMessage status should be 400",
                         400, msg.getStatus());
            assertTrue("FAIL: Message payload did not contain the thrown exception message",
                       msg.getMessage().contains("TestException"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#putChild(RESTRequest, RESTResponse, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void putChild_validToolEntries() throws Exception {
        // The contents of the array is irrelevant. The mockToolbox drives the behaviour.
        final String mockPayload = "[]";
        final ToolEntry mockTe = mock.mock(ToolEntry.class);
        final ToolEntry[] mockArray = { mockTe };

        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                allowing(mockJson).parse(mockPayload, ToolEntry[].class);
                will(returnValue(mockArray));

                one(mockToolbox).updateToolEntries(with(any(List.class)));
            }
        });

        handler.putChild(restRequest, restResponse, "toolEntries");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#deleteBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void deleteBase_noConfirmation() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getParameter("resetToolbox");
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
            assertTrue("FAIL: The NLS message CWWKX1028E was not the content of the message",
                       msg.getMessage().startsWith("CWWKX1028E"));
            assertNotNull("FAIL: The developer message was not set",
                          msg.getDeveloperMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#deleteBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void deleteBase_wrongConfirmation() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getParameter("resetToolbox");
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
            assertTrue("FAIL: The NLS message CWWKX1028E was not the content of the message",
                       msg.getMessage().startsWith("CWWKX1028E"));
            assertNotNull("FAIL: The developer message was not set",
                          msg.getDeveloperMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#deleteBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void deleteBase_goodConfirmation() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getParameter("resetToolbox");
                will(returnValue("true"));

                one(mockToolbox).reset();
            }
        });

        Message msg = (Message) handler.deleteBase(restRequest, restResponse);
        assertEquals("FAIL: Even though the request did not complete, we want to response with 200",
                     200, msg.getStatus());
        assertTrue("FAIL: The NLS message CWWKX1027I was not the content of the message",
                   msg.getMessage().startsWith("CWWKX1027I"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test(expected = MethodNotSupportedException.class)
    public void deleteGrandchild_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.deleteGrandchild(restRequest, restResponse, "unknown", "ignored");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void deleteGrandchild_noSuchBookmark() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).deleteBookmark("noSuchTool");
                will(returnValue(null));
            }
        });

        try {
            handler.deleteGrandchild(restRequest, restResponse, "bookmarks", "noSuchTool");
            fail("DELETE on a tool which is not defiend should result in a NoSuchhandlerException");
        } catch (NoSuchResourceException e) {
            Message error = (Message) e.getPayload();
            assertTrue("Exception message did not match expected 'CWWKX1022E:.*noSuchTool.*'",
                       error.getMessage().matches("CWWKX1022E:.*noSuchTool.*" + USER_ID + ".*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void deleteGrandchild_definedBookmark() throws Exception {
        final Bookmark myTool = new Bookmark("myName", "myURL", "myIcon");
        final String toolId = myTool.getId();

        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).deleteBookmark(toolId);
                will(returnValue(myTool));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, myTool);
                will(returnValue(myTool));
            }
        });

        Bookmark removedTool = (Bookmark) handler.deleteGrandchild(restRequest, restResponse, "bookmarks", toolId);
        assertEquals("The returned (deleted) tool did not match the mock'd return",
                     myTool, removedTool);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void deleteGrandchild_noSuchToolEntry() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).deleteToolEntry("noSuchTool");
                will(returnValue(null));
            }
        });

        try {
            handler.deleteGrandchild(restRequest, restResponse, "toolEntries", "noSuchTool");
            fail("DELETE on a tool which is not defiend should result in a NoSuchhandlerException");
        } catch (NoSuchResourceException e) {
            Message error = (Message) e.getPayload();
            assertTrue("Exception message did not match expected 'CWWKX1022E:.*noSuchTool.*'",
                       error.getMessage().matches("CWWKX1022E:.*noSuchTool.*" + USER_ID + ".*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#deleteGrandchild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void deleteGrandchild_definedToolEntry() throws Exception {
        final ToolEntry myTool = new ToolEntry("myName", "featureTool");
        final String toolId = myTool.getId();

        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolbox).deleteToolEntry(toolId);
                will(returnValue(myTool));

                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, myTool);
                will(returnValue(myTool));
            }
        });

        ToolEntry removedTool = (ToolEntry) handler.deleteGrandchild(restRequest, restResponse, "toolEntries", toolId);
        assertEquals("The returned (deleted) tool did not match the mock'd return",
                     myTool, removedTool);
    }

}
