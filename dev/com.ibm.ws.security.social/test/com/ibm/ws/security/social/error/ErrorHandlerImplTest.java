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
package com.ibm.ws.security.social.error;

import static org.junit.Assert.assertEquals;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.wsspi.security.tai.TAIResult;

import test.common.SharedOutputManager;

public class ErrorHandlerImplTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private static final String HTML_REGEX = "<!DOCTYPE html>\\n?<html[^>]*>.+</html>";
    private static final String HEAD_REGEX = "<head>.*</head>";
    private static final String BODY_REGEX = "<body>.*</body>";
    private static final String FORM_REGEX = "<form .*</form>";

    private static final String HTML_PAGE_TITLE = "Social Media Selection Form";
    private static final String HTML_PAGE_HEADER = "Sign in";

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    public interface MockInterface {
        public void handleErrorResponse(HttpServletResponse response);

        public TAIResult handleErrorResponse(HttpServletResponse response, TAIResult result);

        public int getTaiResultStatus();

        public void handleErrorResponse(HttpServletResponse response, int httpErrorCode);

        public void writeErrorHtml();

        public String createCssContentString();
    }

    ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        errorHandler = new ErrorHandlerImpl();
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

    /************************************** handleErrorResponse(HttpServletResponse) **************************************/

    @Test
    public void test_handleErrorResponse_singleArg_nullResponse() {
        try {
            ErrorHandlerImpl mockHandler = new ErrorHandlerImpl() {
                @Override
                public void handleErrorResponse(HttpServletResponse response, int httpErrorCode) {
                    mockInterface.handleErrorResponse(response, httpErrorCode);
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).handleErrorResponse(null, HttpServletResponse.SC_UNAUTHORIZED);
                }
            });

            mockHandler.handleErrorResponse(null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_handleErrorResponse_singleArg() {
        try {
            ErrorHandlerImpl mockHandler = new ErrorHandlerImpl() {
                @Override
                public void handleErrorResponse(HttpServletResponse response, int httpErrorCode) {
                    mockInterface.handleErrorResponse(response, httpErrorCode);
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).handleErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED);
                }
            });

            mockHandler.handleErrorResponse(response);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_handleErrorResponse_respAndTai_nullArgs() {
        try {
            ErrorHandlerImpl mockHandler = new ErrorHandlerImpl() {
                @Override
                public int getTaiResultStatus(TAIResult result) {
                    return mockInterface.getTaiResultStatus();
                }

                @Override
                public void handleErrorResponse(HttpServletResponse response, int httpErrorCode) {
                    mockInterface.handleErrorResponse(response, httpErrorCode);
                }
            };

            final int status = HttpServletResponse.SC_FORBIDDEN;
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTaiResultStatus();
                    will(returnValue(status));
                    one(mockInterface).handleErrorResponse(null, status);
                }
            });

            mockHandler.handleErrorResponse(null, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_handleErrorResponse_respAndTai_nullTaiArgs() {
        try {
            ErrorHandlerImpl mockHandler = new ErrorHandlerImpl() {
                @Override
                public int getTaiResultStatus(TAIResult result) {
                    return mockInterface.getTaiResultStatus();
                }

                @Override
                public void handleErrorResponse(HttpServletResponse response, int httpErrorCode) {
                    mockInterface.handleErrorResponse(response, httpErrorCode);
                }
            };

            final int status = HttpServletResponse.SC_FORBIDDEN;
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTaiResultStatus();
                    will(returnValue(status));
                    one(mockInterface).handleErrorResponse(response, status);
                }
            });

            mockHandler.handleErrorResponse(response, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_handleErrorResponse_respAndTai() {
        try {
            ErrorHandlerImpl mockHandler = new ErrorHandlerImpl() {
                @Override
                public int getTaiResultStatus(TAIResult result) {
                    return mockInterface.getTaiResultStatus();
                }

                @Override
                public void handleErrorResponse(HttpServletResponse response, int httpErrorCode) {
                    mockInterface.handleErrorResponse(response, httpErrorCode);
                }
            };

            final int status = HttpServletResponse.SC_FORBIDDEN;
            final TAIResult result = TAIResult.create(status);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTaiResultStatus();
                    will(returnValue(status));
                    one(mockInterface).handleErrorResponse(response, status);
                }
            });

            mockHandler.handleErrorResponse(response, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_handleErrorResponse_respAndInt_responseCommitted() {
        try {
            ErrorHandlerImpl mockHandler = new ErrorHandlerImpl() {
                @Override
                public void writeErrorHtml(HttpServletResponse response, String errorHeader, String errorMessage) {
                    mockInterface.writeErrorHtml();
                }
            };

            final int status = HttpServletResponse.SC_FORBIDDEN;

            mockery.checking(new Expectations() {
                {
                    one(response).isCommitted();
                    will(returnValue(true));
                    one(mockInterface).writeErrorHtml();
                }
            });

            mockHandler.handleErrorResponse(response, status);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_handleErrorResponse_respAndInt_responseNotCommitted() {
        try {
            ErrorHandlerImpl mockHandler = new ErrorHandlerImpl() {
                @Override
                public void writeErrorHtml(HttpServletResponse response, String errorHeader, String errorMessage) {
                    mockInterface.writeErrorHtml();
                }
            };

            final int status = HttpServletResponse.SC_BAD_REQUEST;

            mockery.checking(new Expectations() {
                {
                    one(response).isCommitted();
                    will(returnValue(false));
                    one(response).setStatus(status);
                    one(mockInterface).writeErrorHtml();
                }
            });

            mockHandler.handleErrorResponse(response, status);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getTaiResultStatus **************************************/

    @Test
    public void test_getTaiResultStatus_nullArg() {
        try {
            int result = errorHandler.getTaiResultStatus(null);

            assertEquals("Result for a null input did not match expected value.", HttpServletResponse.SC_FORBIDDEN, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getTaiResultStatus() {
        try {
            int status = RandomUtils.getRandomSelection(HttpServletResponse.SC_BAD_REQUEST, HttpServletResponse.SC_FORBIDDEN, HttpServletResponse.SC_UNAUTHORIZED);

            int result = errorHandler.getTaiResultStatus(TAIResult.create(status));

            assertEquals("Result status did not match expected value.", status, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** writeErrorHtml **************************************/

    @Test
    public void test_writeErrorHtml_nullArgs() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(response).getWriter();
                    will(returnValue(writer));
                    one(writer).println(with(any(String.class)));
                    one(writer).flush();
                }
            });

            errorHandler.writeErrorHtml(response, null, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_writeErrorHtml() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(response).getWriter();
                    will(returnValue(writer));
                    one(writer).println(with(any(String.class)));
                    one(writer).flush();
                }
            });

            errorHandler.writeErrorHtml(response, "Some header", "Some error message");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createCssContentString **************************************/

    @Test
    public void test_createCssContentString() {
        try {
            String result = errorHandler.createCssContentString();

            // Don't need to verify the bulk of the content, just that the returned string begins and ends with valid HTML <style> tags
            verifyPattern(result, Pattern.compile("^" + Pattern.quote("<style>") + ".+" + Pattern.quote("</style>") + "$", Pattern.DOTALL));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
