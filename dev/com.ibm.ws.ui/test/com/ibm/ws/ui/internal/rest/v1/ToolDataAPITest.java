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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.ui.internal.rest.AdminCenterRestHandler.POSTResponse;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.IToolDataService;
import com.ibm.ws.ui.internal.v1.IToolbox;
import com.ibm.ws.ui.internal.v1.IToolboxService;
import com.ibm.ws.ui.internal.v1.pojo.ToolEntry;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import test.common.SharedOutputManager;

/**
 * The algorithm assessment of FIPS 140-3 by updating SHA512 checksum is based on slack discussion with component SMEs.
 */
public class ToolDataAPITest {
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
    private final IToolDataService mockToolDataService = mock.mock(IToolDataService.class);
    private final IToolbox mockToolbox = mock.mock(IToolbox.class);
    private ToolDataAPI handler;
    private final String tool1 = "tool1-1.0-1.0.0";
    private final String tool2 = "tool2-1.0-1.0.0";
    private final String tool3 = "tool3-1.0-1.0.0";
    private final ToolEntry toolEntry1 = new ToolEntry(tool1, ITool.TYPE_FEATURE_TOOL);
    private final ToolEntry toolEntry2 = new ToolEntry(tool2, ITool.TYPE_FEATURE_TOOL);
    private final ToolEntry toolEntry3 = new ToolEntry(tool3, ITool.TYPE_BOOKMARK);
    private final String message = "{\"key\":\"value\"}";
    private final String messageSHA512 = "0213f898602b6a489de25f20d8d32c3dbf3d6fee0fbe468f89ac803874ac846f8cd239294a78c2bddf3e0988acb8cd3d00e067a981c9c7933b43a42d3b273eae";

    private final String message2 = "{\"key\":\"value1\"}";
    private final String message2SHA512 = "30a9c1549c94169bc284e21ea24d49084b1d661d01a8f6c194b7823bcad429da2aafae945b6a1573994234806fb6000ca092718e8b92c02b062223650e6817e1";

    private final String url = "ibm/api/adminCenter/v1/tooldata";
    private final String createdURL = url + "/tool1";

    @Before
    public void setUp() {
        // We always look up the user, just mock it once and let it happen
        final List<ToolEntry> toolList = new ArrayList<ToolEntry>();
        toolList.add(toolEntry1);
        toolList.add(toolEntry3);
        toolList.add(toolEntry2);

        mock.checking(new Expectations() {
            {
                allowing(restRequest).getUserPrincipal();
                will(returnValue(mockPrincipal));

                allowing(mockPrincipal).getName();
                will(returnValue(USER_ID));
                allowing(mockToolboxService).getToolbox(USER_ID);
                will(returnValue(mockToolbox));

                allowing(mockToolbox).getToolEntries();
                will(returnValue(toolList));

            }
        });

        handler = new ToolDataAPI(mockToolDataService, mockToolboxService);
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
        handler = new ToolDataAPI(mockToolDataService, mockToolboxService);
        assertEquals("The handler's base URL was not what was expected",
                     "/adminCenter/v1/tooldata", handler.baseURL());
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
        assertTrue("'" + tool1 + "' should be a known child",
                   handler.isKnownChildResource("tool1", restRequest));
        assertTrue("'" + tool2 + "' should be a known child",
                   handler.isKnownChildResource("tool2", restRequest));
        assertFalse("'" + tool3 + "' should be an unknown child",
                    handler.isKnownChildResource("tool3", restRequest));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#getChild(RESTRequest, RESTResponse, String)}.
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_noData() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).promoteIfPossible(USER_ID, "tool1");

                one(mockToolDataService).getToolData(USER_ID, "tool1");
                will(returnValue(null));
            }
        });
        try {
            handler.getChild(restRequest, restResponse, "tool1");
            fail("Get a non exist data should return RESTException");
        } catch (RESTException e) {
            assertEquals("Status should be 204 (no content)",
                         204, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).promoteIfPossible(USER_ID, "tool1");

                one(mockToolDataService).getToolData(USER_ID, "tool1");
                will(returnValue(message));

                one(restResponse).setResponseHeader("ETag", messageSHA512);
            }
        });
        assertSame("Did not get back the the same message which should have been returned by the mock",
                   message, handler.getChild(restRequest, restResponse, "tool1"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_duplicateToolData() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).promoteIfPossible(USER_ID, "tool1");

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(true));
            }
        });
        try {
            handler.postChild(restRequest, restResponse, "tool1");
            fail("Post to existing tool data should return RESTException");
        } catch (RESTException e) {
            assertEquals("Status should be 409 (conflict)",
                         409, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild_internalError() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).promoteIfPossible(USER_ID, "tool1");

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(false));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(message.getBytes("UTF-8"))));

                one(mockToolDataService).addToolData(USER_ID, "tool1", message);
                will(returnValue(null));
            }
        });
        try {
            handler.postChild(restRequest, restResponse, "tool1");
            fail("Post data causes internal error should return RESTException");
        } catch (RESTException e) {
            assertEquals("Status should be 500 (internal error)",
                         500, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void postChild() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).promoteIfPossible(USER_ID, "tool1");

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(false));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(message.getBytes("UTF-8"))));

                one(mockToolDataService).addToolData(USER_ID, "tool1", message);
                will(returnValue(message));

                one(restRequest).getURL();
                will(returnValue(url));

                one(restResponse).setResponseHeader("ETag", messageSHA512);
            }
        });
        POSTResponse response = handler.postChild(restRequest, restResponse, "tool1");
        assertEquals("did not get created url", createdURL, response.createdURL);
        assertEquals("did not get created data", message, response.jsonPayload);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#putChild(RESTRequest, RESTResponse, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
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
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void putChild_preconditionNotSet() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getHeader("If-Match");
                will(returnValue(null));
            }
        });
        try {
            handler.putChild(restRequest, restResponse, "tool1");
            fail("Put data without If-Match header should return RESTException");
        } catch (RESTException e) {
            assertEquals("Status should be 412 (Pre-condition)",
                         412, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void putChild_updateNonExistingData() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(false));

                one(restRequest).getHeader("If-Match");
                will(returnValue(messageSHA512));
            }
        });
        try {
            handler.putChild(restRequest, restResponse, "tool1");
            fail("Update non-existing tool data should return RESTException");
        } catch (RESTException e) {
            assertEquals("Status should be 404 (Not found)",
                         404, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void putChild_checksumMismatch() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).getToolData(USER_ID, "tool1");
                will(returnValue(message));

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(true));

                one(restRequest).getHeader("If-Match");
                will(returnValue("123"));
            }
        });
        try {
            handler.putChild(restRequest, restResponse, "tool1");
        } catch (RESTException e) {
            assertEquals("Should return HTTP_PRECONDITION_FAILED error message when checksums do not match", 412, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void putChild_internalError() throws Exception {
        mock.checking(new Expectations() {
            {

                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getHeader("If-Match");
                will(returnValue(messageSHA512));

                one(mockToolDataService).getToolData(USER_ID, "tool1");
                will(returnValue(message));

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(message.getBytes("UTF-8"))));

                one(mockToolDataService).addToolData(USER_ID, "tool1", message);
                will(returnValue(null));
            }
        });
        try {
            handler.putChild(restRequest, restResponse, "tool1");
            fail("Precondition mismatch should return RESTException");
        } catch (RESTException e) {
            assertEquals("Status should be 500 (internal error)",
                         500, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#postChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void putChild() throws Exception {
        mock.checking(new Expectations() {
            {

                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getHeader("If-Match");
                will(returnValue(messageSHA512));

                one(mockToolDataService).getToolData(USER_ID, "tool1");
                will(returnValue(message));

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(true));

                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(message2.getBytes("UTF-8"))));

                one(mockToolDataService).addToolData(USER_ID, "tool1", message2);
                will(returnValue(message2));

                one(restResponse).setResponseHeader("ETag", message2SHA512);
            }
        });
        Object obj = handler.putChild(restRequest, restResponse, "tool1");
        assertEquals("did not get updated data", message2, obj);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#deleteChild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
    public void deleteChild_unknown() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.deleteChild(restRequest, restResponse, "unknown");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#deleteChild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void deleteChild_nonExisting() throws Exception {

        mock.checking(new Expectations() {
            {

                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(false));

            }
        });
        try {
            handler.deleteChild(restRequest, restResponse, "tool1");
        } catch (RESTException e) {
            assertEquals("Status should be 404 (Not Found)",
                         404, e.getStatus());

        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#deleteChild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void deleteChild_failed() throws Exception {

        mock.checking(new Expectations() {
            {

                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(true));
                one(mockToolDataService).deleteToolData(USER_ID, "tool1");
                will(returnValue(false));
            }
        });
        try {
            handler.deleteChild(restRequest, restResponse, "tool1");
        } catch (RESTException e) {
            assertEquals("Should return internal error message when delete failed", 500, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolDataAPI#deleteChild(RESTRequest, RESTResponse, String, String)}.
     */
    @Test
    public void deleteChild() throws Exception {

        mock.checking(new Expectations() {
            {

                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockToolDataService).exists(USER_ID, "tool1");
                will(returnValue(true));
                one(mockToolDataService).deleteToolData(USER_ID, "tool1");
                will(returnValue(true));
            }
        });
        try {
            handler.deleteChild(restRequest, restResponse, "tool1");
        } catch (RESTException e) {
            assertEquals("Should return 200 when delete succeeded", 200, e.getStatus());
        }
    }
}
