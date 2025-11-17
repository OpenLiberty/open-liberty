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
package com.ibm.ws.ui.internal.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.ws.ui.internal.Filter;
import com.ibm.ws.ui.internal.rest.AdminCenterRestHandler.POSTResponse;
import com.ibm.ws.ui.internal.rest.exceptions.BadRequestException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.v1.pojo.Message;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import test.common.SharedOutputManager;

/**
 *
 */
public class CommonJSONRESTHandlerTest {
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
    private final Filter mockFilter = mock.mock(Filter.class);
    private final JSON mockJson = mock.mock(JSON.class);

    private static final String RESOURCE_PATH = "/adminCenter/myResource";
    private static final String CHILD_RESOURCE = "myChild";
    private static final String GRANDCHILD_RESOURCE = "myGrandchild";
    private static final int GET_BASE_FLAG = 1;
    private static final int GET_CHILD_FLAG = 11;
    private static final int GET_GRANDCHILD_FLAG = 111;
    private static final int POST_BASE_FLAG = 2;
    private static final int POST_CHILD_FLAG = 22;
    private static final int POST_GRANDCHILD_FLAG = 222;
    private static final int PUT_BASE_FLAG = 3;
    private static final int PUT_CHILD_FLAG = 33;
    private static final int PUT_GRANDCHILD_FLAG = 333;
    private static final int DELETE_BASE_FLAG = 4;
    private static final int DELETE_CHILD_FLAG = 44;
    private static final int DELETE_GRANDCHILD_FLAG = 444;
    private static final int GET_BASE_EXCEPTION = 5;
    private static final int GET_CHILD_EXCEPTION = 55;
    private static final int GET_GRANDCHILD_EXCEPTION = 555;
    private static final int POST_BASE_EXCEPTION = 6;
    private static final int POST_CHILD_EXCEPTION = 66;
    private static final int POST_GRANDCHILD_EXCEPTION = 666;
    private static final int PUT_BASE_EXCEPTION = 7;
    private static final int PUT_CHILD_EXCEPTION = 77;
    private static final int PUT_GRANDCHILD_EXCEPTION = 777;
    private static final int DELETE_BASE_EXCEPTION = 8;
    private static final int DELETE_CHILD_EXCEPTION = 88;
    private static final int DELETE_GRANDCHILD_EXCEPTION = 888;

    private CommonJSONRESTHandler handler;

    /**
     * Give our abstract class some substance for testing the default implementations.
     */
    class EmptyCommonJSONRESTHandler extends CommonJSONRESTHandler {
        EmptyCommonJSONRESTHandler(String handlerURL, boolean handlesChildResource, boolean handlesGrandchildResource) {
            this(handlerURL, handlesChildResource, handlesGrandchildResource, new Filter());
        }

        EmptyCommonJSONRESTHandler(String handlerURL, boolean handlesChildResource, boolean handlesGrandchildResource, Filter f) {
            super(handlerURL, handlesChildResource, handlesGrandchildResource, f, mockJson);
        }

        @Override
        public boolean isKnownChildResource(String child, RESTRequest request) {
            return CHILD_RESOURCE.equals(child);
        }

        @Override
        public boolean isKnownGrandchildResource(String child, String grandchild, RESTRequest request) {
            return CHILD_RESOURCE.equals(child) && GRANDCHILD_RESOURCE.equals(grandchild);
        }
    }

    /**
     * Override the doX methods and set some clearly non-valid HTTP status
     * codes in the RESTResponse. This is how we "sense" we went down the right
     * code path.
     */
    class MethodCommonJSONRESTHandler extends CommonJSONRESTHandler {
        private final RESTException throwRESTException;

        MethodCommonJSONRESTHandler(String handlerURL, boolean handlesChildResource, boolean handlesGrandchildResource) {
            this(handlerURL, handlesChildResource, handlesGrandchildResource, null);
        }

        MethodCommonJSONRESTHandler(String handlerURL, boolean handlesChildResource, boolean handlesGrandchildResource, RESTException throwRESTException) {
            //super(handlerURL, handlesChildResource, handlesGrandchildResource);
            super(handlerURL, handlesChildResource, handlesGrandchildResource, null, mockJson);
            this.throwRESTException = throwRESTException;
        }

        /**
         * 1 - success GET base
         * 5 - failure GET base
         */
        @Override
        public Object getBase(RESTRequest request, RESTResponse response) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(GET_BASE_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(GET_BASE_FLAG);
                return null;
            }
        }

        /**
         * 11 - success GET child
         * 55 - failure GET child
         */
        @Override
        public Object getChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(GET_CHILD_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(GET_CHILD_FLAG);
                return null;
            }
        }

        /**
         * 111 - success GET grandchild
         * 555 - failure GET grandchild
         */
        @Override
        public Object getGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(GET_GRANDCHILD_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(GET_GRANDCHILD_FLAG);
                return null;
            }
        }

        /**
         * 2 - success POST base
         * 6 - failure POST base
         */
        @Override
        public POSTResponse postBase(RESTRequest request, RESTResponse response) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(POST_BASE_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(POST_BASE_FLAG);
                POSTResponse pr = new POSTResponse();
                pr.createdURL = "url";
                pr.jsonPayload = null;
                return pr;
            }
        }

        /**
         * 22 - success POST child
         * 66 - failure POST child
         */
        @Override
        public POSTResponse postChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(POST_CHILD_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(POST_CHILD_FLAG);
                POSTResponse pr = new POSTResponse();
                pr.createdURL = "url";
                pr.jsonPayload = null;
                return pr;
            }
        }

        /**
         * 222 - success POST grandchild
         * 666 - failure POST grandchild
         */
        @Override
        public POSTResponse postGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(POST_GRANDCHILD_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(POST_GRANDCHILD_FLAG);
                POSTResponse pr = new POSTResponse();
                pr.createdURL = "url";
                pr.jsonPayload = null;
                return pr;
            }
        }

        /**
         * 3 - success PUT base
         * 7 - failure PUT base
         */
        @Override
        public Object putBase(RESTRequest request, RESTResponse response) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(PUT_BASE_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(PUT_BASE_FLAG);
                return null;
            }
        }

        /**
         * 33 - success PUT child
         * 77 - failure PUT child
         */
        @Override
        public Object putChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(PUT_CHILD_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(PUT_CHILD_FLAG);
                return null;
            }
        }

        /**
         * 333 - success PUT grandchild
         * 777 - failure PUT grandchild
         */
        @Override
        public Object putGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(PUT_GRANDCHILD_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(PUT_GRANDCHILD_FLAG);
                return null;
            }
        }

        /**
         * 4 - success DELETE base
         * 8 - failure DELETE base
         */
        @Override
        public Object deleteBase(RESTRequest request, RESTResponse response) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(DELETE_BASE_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(DELETE_BASE_FLAG);
                return null;
            }
        }

        /**
         * 44 - success DELETE child
         * 88 - failure DELETE child
         */
        @Override
        public Object deleteChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(DELETE_CHILD_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(DELETE_CHILD_FLAG);
                return null;
            }
        }

        /**
         * 444 - success DELETE grandchild
         * 888 - failure DELETE grandchild
         */
        @Override
        public Object deleteGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException {
            if (throwRESTException != null) {
                throw new RESTException(DELETE_GRANDCHILD_EXCEPTION, throwRESTException.getContentType(), throwRESTException.getPayload());
            } else {
                response.setStatus(DELETE_GRANDCHILD_FLAG);
                return null;
            }
        }
    }

    @Before
    public void setUp() {
        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, false, false);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @Test
    public void constructor() {
        assertEquals("FAIL: The handler's baseURL was not returned as expected",
                     RESOURCE_PATH, handler.baseURL());
        assertFalse("FAIL: The handler's hasChildren should be false",
                    handler.hasChildren());
    }

    /**
     * Sets the correct expectations for an invocation of setJSONResponse.
     *
     * @throws JSONMarshallException
     */
    private void jsonResponseExpectations(final int status) throws IOException, JSONMarshallException {
        final OutputStream mockWriter = mock.mock(OutputStream.class);
        mock.checking(new Expectations() {
            {
                allowing(mockWriter);
            }
        });

        jsonResponseExpectations(status, mockWriter);
    }

    /**
     * Sets the correct expectations for an invocation of setJSONResponse.
     *
     * @throws JSONMarshallException
     */
    private void jsonResponseExpectations(final int status, final OutputStream spyWriter) throws IOException, JSONMarshallException {
        mock.checking(new Expectations() {
            {
                one(restResponse).setResponseHeader("Content-Type", "application/json; charset=UTF-8");

                one(restResponse).getOutputStream();
                will(returnValue(spyWriter));

                one(restResponse).setStatus(status);
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#setJSONResponse(com.ibm.wsspi.rest.handler.RESTResponse, java.lang.Object)}.
     */
    @Test
    public void setJSONResponse_success() throws Exception {
        final ByteArrayOutputStream spyWriter = new ByteArrayOutputStream();
        final String pojo = "\"String POJO!\"";
        spyWriter.write(pojo.getBytes("UTF-8"));
        mock.checking(new Expectations() {
            {
                allowing(mockJson).asBytes(pojo);
                will(returnValue(pojo.getBytes()));
                one(restResponse).getOutputStream().write(pojo.getBytes("UTF-8"));
                one(restResponse).setResponseHeader("Content-Type", "application/json; charset=UTF-8");
                one(restResponse).setStatus(200);
            }
        });

        handler.setJSONResponse(restResponse, pojo, 200);

        assertEquals("FAIL: Did not get the expected output written to the response Writer",
                     "\"String POJO!\"", new String(spyWriter.toByteArray(), "UTF-8"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#setJSONResponse(com.ibm.wsspi.rest.handler.RESTResponse, java.lang.Object)}.
     */
    @Test
    public void setJSONResponse_WriterIOException() throws Exception {
        final OutputStream mockWriter = mock.mock(OutputStream.class);
        final Object pojo = "String POJO!";

        mock.checking(new Expectations() {
            {
                one(restResponse).setResponseHeader("Content-Type", "application/json; charset=UTF-8");

                allowing(mockJson).asBytes(pojo);

                one(restResponse).setStatus(200);

                one(restResponse).getOutputStream();
                will(returnValue(mockWriter));

                allowing(mockWriter);
                will(throwException(new IOException("TestException")));

                // Test assertion - IOException will cause 500
                one(restResponse).setStatus(500);
            }
        });

        handler.setJSONResponse(restResponse, pojo, 200);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#setJSONResponse(com.ibm.wsspi.rest.handler.RESTResponse, java.lang.Object)}.
     */
    @Test
    public void setJSONResponse_ObjectMapperIOException() throws Exception {
        handler = new EmptyCommonJSONRESTHandler(null, false, false, null);
        final Object pojo = "String POJO!";

        mock.checking(new Expectations() {
            {
                one(restResponse).setResponseHeader("Content-Type", "application/json; charset=UTF-8");

                one(mockJson).asBytes(pojo);
                will(throwException(new JSONMarshallException("TestException")));

                // Test assertion - IOException will cause 500
                one(restResponse).setStatus(500);

                // Test assertion - status should never be set to 200
                never(restResponse).setStatus(200);

                // Test assertion - the output stream should never be asked for
                never(restResponse).getOutputStream();
            }
        });

        handler.setJSONResponse(restResponse, pojo, 200);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>This is a highly unlikely event, but best to code and test defensively</p>
     */
    @Test
    public void handleRequest_nullPath() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue(null));

                one(restRequest).getContextPath();
                will(returnValue(null));

                one(restRequest).getPath();
                will(returnValue(null));

                // Test expectation
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>This is a highly unlikely event, but best to code and test defensively</p>
     */
    @Test
    public void handleRequest_emptyPath() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test expectation
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>This is a highly unlikely event, but best to code and test defensively</p>
     */
    @Test
    public void handleRequest_wrongPath1() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api/wrong/myResource"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test expectation
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>This is a highly unlikely event, but best to code and test defensively</p>
     */
    @Test
    public void handleRequest_wrongPath2() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/wrongResource"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test expectation
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>This is a highly unlikely event, but best to code and test defensively</p>
     */
    @Test
    public void handleRequest_wrongPath3() throws Exception {
        mock.checking(new Expectations() {
            {

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/myResource/a"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test expectation
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_notHandlingChildPath() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test expectation
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_notHandlingGrandchildPath() throws Exception {
        mock.checking(new Expectations() {
            {

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test expectation
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_greatgrandchildPath() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild/myGreatgrandchild"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test expectation
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_unknownMethod() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getMethod();
                will(returnValue("UNKNOWN"));

                // Test expectation
                one(restResponse).setStatus(405);
            }
        });

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getBase() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getBase_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getChild_noChildren() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getChild_noChildrenTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getChild_hasChildrenUnknownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getChild_hasChildrenUnknownChildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getChild_hasChildrenKnownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getChild_hasChildrenKnownChildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getGrandchild_noGrandchildren() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getGrandchild_noGrandchildrenTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getGrandchild_hasGrandchildrenUnknownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getGrandchild_hasGrandchildrenUnknownGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getGrandchild_hasGrandchildrenKnownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_getGrandchild_hasGrandchildrenKnownGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_post_noMediaType() throws Exception {
        mock.checking(new Expectations() {
            {

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue(null));

                // Test expectation
                one(restResponse).setStatus(415);
            }
        });

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_post_nonJSONMediaType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("text/plain"));

                // Test expectation
                one(restResponse).setStatus(415);
            }
        });

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postBase_default() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postBase_defaultTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postChild_noChildren() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postChild_noChildrenTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postChild_hasChildrenUnknownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postChild_hasChildrenUnknownChildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postChild_hasChildrenKnownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postChild_hasChildrenKnownChildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postGrandchild_noGrandchildren() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postGrandchild_noGrandchildrenTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postGrandchild_hasGrandchildrenUnknownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postGrandchild_hasGrandchildrenUnknownGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postGrandchild_hasGrandchildrenKnownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_postGrandchild_hasGrandchildrenKnownGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_put_noMediaType() throws Exception {
        mock.checking(new Expectations() {
            {

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue(null));

                // Test expectation
                one(restResponse).setStatus(415);
            }
        });

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_put_nonJSONMediaType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("text/plain"));

                // Test expectation
                one(restResponse).setStatus(415);
            }
        });

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putBase_default() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putBase_defaultTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putChild_noChildren() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putChild_noChildrenTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putChild_hasChildrenUnknownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putChild_hasChildrenUnknownChildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putChild_hasChildrenKnownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putChild_hasChildrenKnownChildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putGrandchild_noGrandchildren() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putGrandchild_noGrandchildrenTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putGrandchild_hasGrandchildrenUnknownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putGrandchild_hasGrandchildrenUnknownGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putGrandchild_hasGrandchildrenKnownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_putGrandchild_hasGrandchildrenKnownGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteBase_default() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteBase_defaultTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteChild_noChildren() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteChild_noChildrenTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteChild_hasChildrenUnknownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteChild_hasChildrenUnknownChildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/child/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteChild_hasChildrenKnownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteChild_hasChildrenKnownChildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteGrandchild_noGrandchildren() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteGrandchild_noGrandchildrenTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteGrandchild_hasGrandchildrenUnknownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteGrandchild_hasGrandchildrenUnknownGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/grandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteGrandchild_hasGrandchildrenKnownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    @Test
    public void handleRequest_default_deleteGrandchild_hasGrandchildrenKnownGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/myChild/myGrandchild/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(405);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getBase() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(GET_BASE_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getBase_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(GET_BASE_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getBase_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test expectation
                one(restResponse).setStatus(GET_BASE_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Sets the correct expectations for an invocation of setJSONResponse.
     */
    private void textResponseExpectations(final int status) throws IOException {
        final OutputStream mockWriter = mock.mock(OutputStream.class);
        mock.checking(new Expectations() {
            {
                one(restResponse).setResponseHeader("Content-Type", "text/plain");

                one(restResponse).getOutputStream();
                will(returnValue(mockWriter));

                allowing(mockWriter);

                one(restResponse).setStatus(status);
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getBase_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));
            }
        });

        // Test expectation
        textResponseExpectations(GET_BASE_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getBase_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(mockJson).asBytes(null);
                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(GET_BASE_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getBase_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(GET_CHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getChild_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(GET_CHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getChild_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test expectation
                one(restResponse).setStatus(GET_CHILD_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getChild_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));
            }
        });

        // Test expectation
        textResponseExpectations(GET_CHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getChild_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(GET_CHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getChild_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>getChild is called when its defined, irrespective of the child being known or not.</p>
     */
    @Test
    public void handleRequest_getChild_unknownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/unknown"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test assertion
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(GET_CHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(GET_GRANDCHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getGrandchild_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(GET_GRANDCHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getGRandchild_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test expectation
                one(restResponse).setStatus(GET_GRANDCHILD_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getGrandchild_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));
            }
        });

        // Test expectation
        textResponseExpectations(GET_GRANDCHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getGrandchild_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(GET_GRANDCHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_getGrandchild_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>getChild is called when its defined, irrespective of the child being known or not.</p>
     */
    @Test
    public void handleRequest_getGrandchild_unknownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + "/unknown"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));

                // Test assertion
                one(restResponse).setStatus(GET_GRANDCHILD_FLAG);
                one(mockJson).asBytes(null);
            }
        });

        jsonResponseExpectations(200);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postBase() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectations
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(POST_BASE_FLAG);
                one(restResponse).setResponseHeader("Location", "url");
            }
        });

        jsonResponseExpectations(201);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postBase_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectations
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(POST_BASE_FLAG);
                one(restResponse).setResponseHeader("Location", "url");
            }
        });

        jsonResponseExpectations(201);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postBase_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(restResponse).setStatus(POST_BASE_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postBase_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation
        textResponseExpectations(POST_BASE_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postBase_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(POST_BASE_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postBase_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectations
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(POST_CHILD_FLAG);
                one(restResponse).setResponseHeader("Location", "url");
            }
        });

        jsonResponseExpectations(201);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postChild_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectations
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(POST_CHILD_FLAG);
                one(restResponse).setResponseHeader("Location", "url");
            }
        });

        jsonResponseExpectations(201);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postChild_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(restResponse).setStatus(POST_CHILD_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postChild_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation
        textResponseExpectations(POST_CHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postChild_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(POST_CHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postChild_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>postChild is called when its defined, irrespective of the child being known or not.</p>
     */
    @Test
    public void handleRequest_postChild_unknownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/unknown"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test assertions
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(POST_CHILD_FLAG);
                one(restResponse).setResponseHeader("Location", "url");
            }
        });

        jsonResponseExpectations(201);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectations
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(POST_GRANDCHILD_FLAG);
                one(restResponse).setResponseHeader("Location", "url");
            }
        });

        jsonResponseExpectations(201);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postGrandchild_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectations
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(POST_GRANDCHILD_FLAG);
                one(restResponse).setResponseHeader("Location", "url");
            }
        });

        jsonResponseExpectations(201);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postGrandchild_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(restResponse).setStatus(POST_GRANDCHILD_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postGrandchild_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation
        textResponseExpectations(POST_GRANDCHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postGrandchild_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(POST_GRANDCHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_postGrandchild_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("GET"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>postChild is called when its defined, irrespective of the child being known or not.</p>
     */
    @Test
    public void handleRequest_postGrandchild_unknownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/unknown"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("POST"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test assertions
                allowing(mockJson).asBytes(null);
                allowing(mockJson).asBytes("hi there");
                one(restResponse).setStatus(POST_GRANDCHILD_FLAG);
                one(restResponse).setResponseHeader("Location", "url");
            }
        });

        jsonResponseExpectations(201);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putBase() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(PUT_BASE_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putBase_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(PUT_BASE_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putBase_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(restResponse).setStatus(PUT_BASE_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putBase_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation
        textResponseExpectations(PUT_BASE_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putBase_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(PUT_BASE_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putBase_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(PUT_CHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putChild_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(PUT_CHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putChild_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(restResponse).setStatus(PUT_CHILD_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putChild_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation
        textResponseExpectations(PUT_CHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putChild_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(PUT_CHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putChild_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>putChild is called when its defined, irrespective of the child being known or not.</p>
     */
    @Test
    public void handleRequest_putChild_unknownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/unknown"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test assertion
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(PUT_CHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(PUT_GRANDCHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putGrandchild_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(PUT_GRANDCHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putGrandchild_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test expectation
                one(restResponse).setStatus(PUT_GRANDCHILD_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putGrandchild_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation
        textResponseExpectations(PUT_GRANDCHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putGrandchild_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(PUT_GRANDCHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_putGrandchild_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>putChild is called when its defined, irrespective of the child being known or not.</p>
     */
    @Test
    public void handleRequest_putGrandchild_unknownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/unknown"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("PUT"));

                allowing(restRequest).getHeader(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
                will(returnValue("application/json"));

                // Test assertion
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(PUT_GRANDCHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteBase() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(DELETE_BASE_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteBase_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(DELETE_BASE_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteBase_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test expectation
                one(restResponse).setStatus(DELETE_BASE_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteBase_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));
            }
        });

        // Test expectation
        textResponseExpectations(DELETE_BASE_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteBase_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(DELETE_BASE_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteBase_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, false, false, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(DELETE_CHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteChild_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(DELETE_CHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteChild_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test expectation
                one(restResponse).setStatus(DELETE_CHILD_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteChild_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));
            }
        });

        // Test expectation
        textResponseExpectations(DELETE_CHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteChild_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                allowing(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(DELETE_CHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteChild_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>putChild is called when its defined, irrespective of the child being known or not.</p>
     */
    @Test
    public void handleRequest_deleteChild_unknownChild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/unknown"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test assertion
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(DELETE_CHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, false);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(DELETE_GRANDCHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteGrandchild_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE + "/"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test expectation
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(DELETE_GRANDCHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteGrandchild_RESTException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test expectation
                one(restResponse).setStatus(DELETE_GRANDCHILD_EXCEPTION);
            }
        });

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteGrandchild_RESTExceptionStringPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));
            }
        });

        // Test expectation
        textResponseExpectations(DELETE_GRANDCHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteGrandchild_RESTExceptionJSONPayload() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                one(mockJson).asBytes("hi there");
            }
        });

        // Test expectation
        jsonResponseExpectations(DELETE_GRANDCHILD_EXCEPTION);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_deleteGrandchild_RESTExceptionMissingContentType() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/" + GRANDCHILD_RESOURCE));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));
            }
        });

        // Test expectation - 500 means we had a problem
        textResponseExpectations(500);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true, new RESTException(0, null, "hi there"));
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>putChild is called when its defined, irrespective of the child being known or not.</p>
     */
    @Test
    public void handleRequest_deleteGrandchild_unknownGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(returnValue("/ibm/api" + RESOURCE_PATH + "/" + CHILD_RESOURCE + "/unknown"));

                allowing(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                allowing(restRequest).getMethod();
                will(returnValue("DELETE"));

                // Test assertion
                one(mockJson).asBytes(null);
                one(restResponse).setStatus(DELETE_GRANDCHILD_FLAG);
            }
        });

        jsonResponseExpectations(200);

        // RESTException status of 0 will be replaced by the method that gets called
        handler = new MethodCommonJSONRESTHandler(RESOURCE_PATH, true, true);
        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     * <p>This is a highly unlikely event, but best to code and test defensively</p>
     */
    @Test
    public void handleRequest_runtimeException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(restRequest).getURI();
                will(throwException(new RuntimeException("TestException")));

                // Test expectation
                one(restResponse).setStatus(500);
            }
        });

        handler.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getChildResourceName(String)}.
     */
    @Test
    public void getChildResourceName_null() {
        assertNull("When the requested resource is null, return null",
                   handler.getChildResourceName(null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getChildResourceName(String)}.
     */
    @Test
    public void getChildResourceName_empty() {
        assertNull("When the requested resource is empty, return null",
                   handler.getChildResourceName(""));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getChildResourceName(String)}.
     */
    @Test
    public void getChildResourceName_notHandledResourcePath() {
        assertNull("When the requested resource is not handled, return null",
                   handler.getChildResourceName("/wrong"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getChildResourceName(String)}.
     */
    @Test
    public void getChildResourceName_isBaseResourcePath() {
        assertNull("When the requested resource is the handler's base resource path, return null",
                   handler.getChildResourceName(RESOURCE_PATH));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getChildResourceName(String)}.
     */
    @Test
    public void getChildResourceName_isChildResourcePath() {
        assertEquals("When the requested resource is a child of the handler's base resource path, return it",
                     "child", handler.getChildResourceName(RESOURCE_PATH + "/child"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getChildResourceName(String)}.
     */
    @Test
    public void getChildResourceName_isGrandchildResourcePath() {
        assertEquals("When the requested resource is a grandchild of the handler's base resource path, return null",
                     "child", handler.getChildResourceName(RESOURCE_PATH + "/child/grandchild"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getGrandchildResourceName(String)}.
     */
    @Test
    public void getGrandchildResourceName_null() {
        assertNull("When the requested resource is null, return null",
                   handler.getGrandchildResourceName(null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getGrandchildResourceName(String)}.
     */
    @Test
    public void getGrandchildResourceName_empty() {
        assertNull("When the requested resource is empty, return null",
                   handler.getGrandchildResourceName(""));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getGrandchildResourceName(String)}.
     */
    @Test
    public void getGrandchildResourceName_notHandledResourcePath() {
        assertNull("When the requested resource is not handled, return null",
                   handler.getGrandchildResourceName("/wrong"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getGrandchildResourceName(String)}.
     */
    @Test
    public void getGrandchildResourceName_isBaseResourcePath() {
        assertNull("When the requested resource is the handler's base resource path, return null",
                   handler.getGrandchildResourceName(RESOURCE_PATH));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getChildResourceName(String)}.
     */
    @Test
    public void getGrandchildResourceName_isChildResourcePath() {
        assertNull("When the requested resource is a child of the handler's base resource path, return null",
                   handler.getGrandchildResourceName(RESOURCE_PATH + "/child"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getChildResourceName(String)}.
     */
    @Test
    public void getGrandchildResourceName_isGrandchildResourcePath() {
        assertEquals("When the requested resource is a grandchild of the handler's base resource path, return it",
                     "grandchild", handler.getGrandchildResourceName(RESOURCE_PATH + "/child/grandchild"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#getChildResourceName(String)}.
     */
    @Test
    public void getGrandchildResourceName_isGreatGrandchildResourcePath() {
        assertEquals("When the requested resource is a grandchild of the handler's base resource path, return it",
                     "grandchild", handler.getGrandchildResourceName(RESOURCE_PATH + "/child/grandchild/greatgrandchild"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isChildResource(String)}.
     */
    @Test
    public void isChildResource_nullRequestPath() {
        assertFalse("A null request path should result in a false isChildResource",
                    handler.isChildResource(null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isChildResource(String)}.
     */
    @Test
    public void isChildResource_emptyRequestPath() {
        assertFalse("An empty request path should result in a false isChildResource",
                    handler.isChildResource(""));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isChildResource(String)}.
     */
    @Test
    public void isChildResource_baseRequestPath() {
        assertFalse("The base hanlded request path should result in a false isChildResource",
                    handler.isChildResource(RESOURCE_PATH));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isChildResource(String)}.
     */
    @Test
    public void isChildResource_baseRequestPathTrailingSlash() {
        assertFalse("The base hanlded request path should result in a false isChildResource",
                    handler.isChildResource(RESOURCE_PATH + "/"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isChildResource(String)}.
     */
    @Test
    public void isChildResource_childRequestPath() {
        assertTrue("A child request path should result in a true isChildResource when not matching a specific name",
                   handler.isChildResource(RESOURCE_PATH + "/child"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isChildResource(String)}.
     */
    @Test
    public void isChildResource_childRequestPathTrailingSlash() {
        assertTrue("A child request path should result in a true isChildResource when not matching a specific name",
                   handler.isChildResource(RESOURCE_PATH + "/child/"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isChildResource(String)}.
     */
    @Test
    public void isChildResource_grandchildRequestPath() {
        assertFalse("A child request path should result in a true isChildResource when not matching a specific name",
                    handler.isChildResource(RESOURCE_PATH + "/child/grandchild"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isChildResource(String)}.
     */
    @Test
    public void isChildResource_grandchildRequestPathTrailingSlash() {
        assertFalse("A child request path should result in a true isChildResource when not matching a specific name",
                    handler.isChildResource(RESOURCE_PATH + "/child/grandchild/"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isChildResource(String)}.
     */
    @Test
    public void isChildResource_notHandledRequestPath() {
        assertFalse("A non-handled request path should result in a false isChildResource",
                    handler.isChildResource("/wrong"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_nullRequestPath() {
        assertFalse("A null request path should result in a false isGrandchildResource",
                    handler.isGrandchildResource(null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_emptyRequestPath() {
        assertFalse("An empty request path should result in a false isGrandchildResource",
                    handler.isGrandchildResource(""));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_baseRequestPath() {
        assertFalse("The base hanlded request path should result in a false isGrandchildResource",
                    handler.isGrandchildResource(RESOURCE_PATH));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_baseRequestPathTrailingSlash() {
        assertFalse("The base hanlded request path should result in a false isGrandchildResource",
                    handler.isGrandchildResource(RESOURCE_PATH + "/"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_childRequestPath() {
        assertFalse("A child request path should result in a true isGrandchildResource when not matching a specific name",
                    handler.isGrandchildResource(RESOURCE_PATH + "/child"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_childRequestPathTrailingSlash() {
        assertFalse("A child request path should result in a true isGrandchildResource when not matching a specific name",
                    handler.isGrandchildResource(RESOURCE_PATH + "/child/"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_grandchildRequestPath() {
        assertTrue("A matching grandchild request path should result in a true isGrandchildResource",
                   handler.isGrandchildResource(RESOURCE_PATH + "/child/grandchild"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_grandchildRequestPathMatchingTrailingSlashSucceeds() {
        assertTrue("A matching grandchild request path should result in a true isGrandchildResource",
                   handler.isGrandchildResource(RESOURCE_PATH + "/child/grandchild/"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_greatgrandchildRequestPath() {
        assertFalse("A greatgrandchild request path should result in a false isGrandchildResource",
                    handler.isGrandchildResource(RESOURCE_PATH + "/child/grandchild/greatgrandchild"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_greatchildRequestPathMatchingTrailingSlashSucceeds() {
        assertFalse("A greatgrandchild request path should result in a false isGrandchildResource",
                    handler.isGrandchildResource(RESOURCE_PATH + "/child/grandchild/greatgrandchild/"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#isGrandchildResource(String)}.
     */
    @Test
    public void isGrandchildResource_notHandledRequestPath() {
        assertFalse("A non-handled request path should result in a false isGrandchildResource",
                    handler.isGrandchildResource("/wrong"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#applyFilter(RESTRequest, Object)}.
     */
    @Test
    public void applyFilter_nullObject() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getParameter("fields");
                will(returnValue(null));
            }
        });

        assertNull("Applying the filter to a null Object should beget null",
                   handler.applyFilter(restRequest, null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#applyFilter(RESTRequest, Object)}.
     */
    @Test
    public void applyFilter_objectNoFilter() throws Exception {
        final Object obj = "Object";
        mock.checking(new Expectations() {
            {
                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, obj);
                will(returnValue(obj));
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false, mockFilter);
        assertSame("Applying the filter when no filter is defined should result in no change to the Object",
                   obj, handler.applyFilter(restRequest, obj));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#applyFilter(RESTRequest, Object)}.
     */
    @Test
    public void applyFilter_objectWithFilter() throws Exception {
        final String filter = "id";
        final Object obj = "Object";
        mock.checking(new Expectations() {
            {
                one(restRequest).getParameter("fields");
                will(returnValue(filter));

                one(mockFilter).applyFieldFilter(filter, obj);
                will(returnValue(obj));
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false, mockFilter);
        assertSame("Applying the filter when no filter is defined should result in no change to the Object",
                   obj, handler.applyFilter(restRequest, obj));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#applyFilter(RESTRequest, Object)}.
     */
    @Test
    public void applyFilter_exception() throws Exception {
        final Object obj = "Object";
        mock.checking(new Expectations() {
            {
                one(restRequest).getParameter("fields");
                will(returnValue(null));

                one(mockFilter).applyFieldFilter(null, obj);
                will(throwException(new IntrospectionException("TestException")));
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false, mockFilter);
        try {
            handler.applyFilter(restRequest, obj);
            fail("Expected a RESTException to be thrown");
        } catch (RESTException e) {
            assertEquals("Did not get back expected 500 status code",
                         500, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     * <p>This probably will never happen, but code defensively!</p>
     */
    @Test
    public void readJSONPayload_noInputStream() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(null));
            }
        });

        try {
            handler.readJSONPayload(restRequest, String.class);
            fail("Should have caught a RESTException");
        } catch (RESTException e) {
            assertEquals("Should have caught a 500 RESTException",
                         500, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     * <p>This probably will never happen, but code defensively!</p>
     */
    @Test
    public void readJSONPayload_emptyInputStream() throws Exception {
        final InputStream mockInputStream = mock.mock(InputStream.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(mockInputStream));

                one(mockInputStream).read(with(any(byte[].class)));
                will(throwException(new EOFException("TestException")));

                one(mockInputStream).close();
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false, null);
        try {
            handler.readJSONPayload(restRequest, String.class);
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertTrue("ErrorMessage message start with CWWKX1019E and contain String. Message: " + error.getMessage(),
                       error.getMessage().matches("CWWKX1019E:.*String.*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     */
    @Test
    public void readJSONPayload_veryLargeInputStream() throws Exception {
        final InputStream mockInputStream = mock.mock(InputStream.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(mockInputStream));

                one(mockInputStream).read(with(any(byte[].class)));
                will(returnValue(APIConstants.POST_MAX_JSON_SIZE + 1));

                one(mockInputStream).close();
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false, null);
        try {
            handler.readJSONPayload(restRequest, String.class);
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertTrue("ErrorMessage message start with CWWKX1054E and contain String. Message: " + error.getMessage(),
                       error.getMessage().matches("CWWKX1054E:.*8,192.*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     */
    @Test
    public void readJSONPayload_jsonSyntaxProblem() throws Exception {
        final String mockPayload = "a";
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse("a", String.class);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false);
        try {
            handler.readJSONPayload(restRequest, String.class);
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertTrue("ErrorMessage message start with CWWKX1020E and contain String. Message: " + error.getMessage(),
                       error.getMessage().matches("CWWKX1020E:.*String.*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     *
     */
    @Test
    public void readJSONPayload_deserializationProblem() throws Exception {
        final String mockPayload = "{}";
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse("{}", String.class);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false);
        try {
            handler.readJSONPayload(restRequest, String.class);
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertTrue("ErrorMessage message start with CWWKX1021E and contain String. Message: " + error.getMessage(),
                       error.getMessage().matches("CWWKX1021E:.*String.*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     *
     */
    @Test
    public void readJSONPayload_ioProblem() throws Exception {
        final InputStream mockInputStream = mock.mock(InputStream.class);
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(mockInputStream));

                one(mockInputStream).read(with(any(byte[].class)));
                will(throwException(new IOException("TestException")));

                one(mockInputStream).close();
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false, null);
        try {
            handler.readJSONPayload(restRequest, String.class);
            fail("Should have caught a RESTException");
        } catch (RESTException e) {
            assertEquals("Should have caught a 500 RESTException",
                         500, e.getStatus());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     */
    @Test
    public void readJSONPayload_success() throws Exception {
        final String mockPayload = "\"SUCCESS\"";
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));

                one(mockJson).parse(mockPayload, String.class);
                will(returnValue("SUCCESS"));
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false);
        assertEquals("Should have gotten back mock'd response",
                     "SUCCESS", handler.readJSONPayload(restRequest, String.class));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     */
    @Test
    public void readJSONPayload_successMultitype() throws Exception {
        final String mockPayload = "\"stringy\"";
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));
                one(mockJson).parse(mockPayload, Integer.class);
                will(returnValue("stringy"));
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false);
        List<Class<? extends Object>> types = new ArrayList<Class<? extends Object>>();
        types.add(Integer.class);
        types.add(String.class);

        assertEquals("Should have gotten back mock'd response",
                     "stringy", handler.readJSONPayload(restRequest, Object.class, types));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     */
    @Test
    public void readJSONPayload_successMultitypeOrder() throws Exception {
        final String mockPayload = "1";
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));
                one(mockJson).parse(mockPayload, Long.class);
                will(returnValue(new Long("1")));
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false);
        List<Class<? extends Object>> types = new ArrayList<Class<? extends Object>>();
        types.add(Long.class);
        types.add(Integer.class);

        assertEquals("Should have gotten back mock'd response",
                     Long.valueOf(1L), handler.readJSONPayload(restRequest, Object.class, types));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler#readJSONPayload(RESTRequest, Class)}.
     */
    @Test
    public void readJSONPayload_failMultitype() throws Exception {
        final String mockPayload = "{}";
        mock.checking(new Expectations() {
            {
                one(restRequest).getInputStream();
                will(returnValue(new ByteArrayInputStream(mockPayload.getBytes("UTF-8"))));
                one(mockJson).parse("{}", Integer.class);
            }
        });

        handler = new EmptyCommonJSONRESTHandler(null, false, false);
        List<Class<? extends Object>> types = new ArrayList<Class<? extends Object>>();
        types.add(Integer.class);
        types.add(String.class);

        try {
            handler.readJSONPayload(restRequest, Object.class, types);
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertTrue("ErrorMessage message start with CWWKX1021E and contain String. Message: " + error.getMessage(),
                       error.getMessage().matches("CWWKX1021E:.*java.lang.Integer, java.lang.String.*"));
        }
    }
}
