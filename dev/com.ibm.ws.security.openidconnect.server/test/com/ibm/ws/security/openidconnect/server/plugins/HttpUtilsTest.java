/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.openidconnect.server.internal.HttpUtils;
import com.ibm.ws.security.openidconnect.web.MockServletRequest;

/**
 * Test class to exercise the functionality of the HttpUtils class
 */
public class HttpUtilsTest {
    private static SharedOutputManager outputMgr;
    private static final String REQ_SERVLET_PATH = "/myServletPath";
    private static final String REQ_CONTEXT_PATH = "/myCtxPath";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testGetFullCtxServletPathDefaultInsecurePort() {
        String methodName = "testGetFullCtxServletPathDefaultInsecurePort";

        try {
            MockServletRequest request = new MockServletRequest() {
                @Override
                public String getScheme() {
                    return "http";
                }

                @Override
                public String getServerName() {
                    return "localhost";
                }

                @Override
                public int getServerPort() {
                    return 80;
                }

                @Override
                public String getContextPath() {
                    return REQ_CONTEXT_PATH;
                }
            };

            request.setServletPath(REQ_SERVLET_PATH);

            String expectedPath = (new StringBuffer())
                            .append("http://localhost")
                            .append(REQ_CONTEXT_PATH)
                            .append(REQ_SERVLET_PATH)
                            .toString();

            String calculatedPath = HttpUtils.getFullCtxServletPath(request);

            assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetFullCtxServletPathCustomInsecurePort() {
        String methodName = "testGetFullCtxServletPathCustomInsecurePort";

        try {
            MockServletRequest request = new MockServletRequest() {
                @Override
                public String getScheme() {
                    return "http";
                }

                @Override
                public String getServerName() {
                    return "localhost";
                }

                @Override
                public int getServerPort() {
                    return 81;
                }

                @Override
                public String getContextPath() {
                    return REQ_CONTEXT_PATH;
                }
            };

            request.setServletPath(REQ_SERVLET_PATH);

            String expectedPath = (new StringBuffer())
                            .append("http://localhost:81")
                            .append(REQ_CONTEXT_PATH)
                            .append(REQ_SERVLET_PATH)
                            .toString();

            String calculatedPath = HttpUtils.getFullCtxServletPath(request);

            assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetFullCtxServletPathDefaultSecurePort() {
        String methodName = "testGetFullCtxServletPathDefaultSecurePort";

        try {
            MockServletRequest request = new MockServletRequest() {
                @Override
                public String getScheme() {
                    return "https";
                }

                @Override
                public String getServerName() {
                    return "localhost";
                }

                @Override
                public int getServerPort() {
                    return 443;
                }

                @Override
                public String getContextPath() {
                    return REQ_CONTEXT_PATH;
                }
            };

            request.setServletPath(REQ_SERVLET_PATH);

            String expectedPath = (new StringBuffer())
                            .append("https://localhost")
                            .append(REQ_CONTEXT_PATH)
                            .append(REQ_SERVLET_PATH)
                            .toString();

            String calculatedPath = HttpUtils.getFullCtxServletPath(request);

            assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetFullCtxServletPathCustomSecurePort() {
        String methodName = "testGetFullCtxServletPathCustomSecurePort";

        try {
            MockServletRequest request = new MockServletRequest() {
                @Override
                public String getScheme() {
                    return "https";
                }

                @Override
                public String getServerName() {
                    return "localhost";
                }

                @Override
                public int getServerPort() {
                    return 444;
                }

                @Override
                public String getContextPath() {
                    return REQ_CONTEXT_PATH;
                }
            };

            request.setServletPath(REQ_SERVLET_PATH);

            String expectedPath = (new StringBuffer())
                            .append("https://localhost:444")
                            .append(REQ_CONTEXT_PATH)
                            .append(REQ_SERVLET_PATH)
                            .toString();

            String calculatedPath = HttpUtils.getFullCtxServletPath(request);

            assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
