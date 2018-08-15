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
package com.ibm.ws.security.social.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.wsspi.wab.configure.WABConfiguration;

import test.common.SharedOutputManager;

public class SocialLoginWebappConfigImplTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    static final String BAD_PATH_CHARS = " `#^[]{}\\|\"<>?";
    static final String GOOD_PATH_CHARS = "._~%!$&'()*+,;=:@/-";

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    public interface MockInterface {
        public void initProps();

        public void validateAndSetContextPath();

        public void validateAndSetSelectionPageUrl();

        public boolean isSelectionPageUrlNullOrEmpty();

        public void validateAndSetNonEmptySelectionPageUrl();

        public boolean isHttpOrRelativeUrl();

        public boolean isValidUriPath();

    }

    SocialLoginWebappConfigImpl configImpl = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        configImpl = new SocialLoginWebappConfigImpl();
        Oauth2LoginConfigImpl.setContextRoot(Oauth2LoginConfigImpl.DEFAULT_CONTEXT_ROOT);
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

    /************************************** activate **************************************/

    @Test
    public void activate_emptyContextPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                public void initProps(Map<String, Object> props) {
                    mockInterface.initProps();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).initProps();
                }
            });

            configImpl.activate(cc, getRequiredConfigProps(""));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void activate_invalidContextPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                public void initProps(Map<String, Object> props) {
                    mockInterface.initProps();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).initProps();
                }
            });

            String contextPath = "Some invalid `#^[]{}\\|\"<>? context path";
            configImpl.activate(cc, getRequiredConfigProps(contextPath));

            // No message in server logs because method that emits the error message is mocked
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void activate_validContextPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                public void initProps(Map<String, Object> props) {
                    mockInterface.initProps();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).initProps();
                }
            });

            String contextPath = "Some/~1!2@34$5%67&8*9(0)-_=+;:',./good/path";
            configImpl.activate(cc, getRequiredConfigProps(contextPath));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** modified **************************************/

    @Test
    public void modified_emptyContextPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                public void initProps(Map<String, Object> props) {
                    mockInterface.initProps();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).initProps();
                }
            });

            configImpl.modified(getRequiredConfigProps(""));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void modified_invalidContextPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                public void initProps(Map<String, Object> props) {
                    mockInterface.initProps();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).initProps();
                }
            });

            String contextPath = "Some invalid #^[]{}\\|\"<> context path";
            configImpl.modified(getRequiredConfigProps(contextPath));

            // No message in server logs because method that emits the error message is mocked
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void modified_validContextPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                public void initProps(Map<String, Object> props) {
                    mockInterface.initProps();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).initProps();
                }
            });

            String contextPath = "Some/`~1!2@34$5%67&8*9(0)-_=+;:',./?/good/path";
            configImpl.modified(getRequiredConfigProps(contextPath));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** initProps **************************************/

    @Test
    public void initProps_emptyContextPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                void validateAndSetContextPath(String contextPath) {
                    mockInterface.validateAndSetContextPath();
                }

                @Override
                void validateAndSetSelectionPageUrl(String contextPath) {
                    mockInterface.validateAndSetSelectionPageUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).validateAndSetContextPath();
                    one(mockInterface).validateAndSetSelectionPageUrl();
                }
            });

            String contextPath = "";
            configImpl.initProps(getRequiredConfigProps(contextPath));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initProps_invalidContextPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                void validateAndSetContextPath(String contextPath) {
                    mockInterface.validateAndSetContextPath();
                }

                @Override
                void validateAndSetSelectionPageUrl(String contextPath) {
                    mockInterface.validateAndSetSelectionPageUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).validateAndSetContextPath();
                    one(mockInterface).validateAndSetSelectionPageUrl();
                }
            });

            String contextPath = "/bad/" + getRandomChar(BAD_PATH_CHARS) + "/path";
            configImpl.initProps(getRequiredConfigProps(contextPath));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initProps_validContextPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                void validateAndSetContextPath(String contextPath) {
                    mockInterface.validateAndSetContextPath();
                }

                @Override
                void validateAndSetSelectionPageUrl(String contextPath) {
                    mockInterface.validateAndSetSelectionPageUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).validateAndSetContextPath();
                    one(mockInterface).validateAndSetSelectionPageUrl();
                }
            });

            String contextPath = "/good/" + getRandomChar(GOOD_PATH_CHARS) + "/path";
            configImpl.initProps(getRequiredConfigProps(contextPath));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initProps_enableLocalAuthentication_missing() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                void validateAndSetContextPath(String contextPath) {
                    mockInterface.validateAndSetContextPath();
                }

                @Override
                void validateAndSetSelectionPageUrl(String contextPath) {
                    mockInterface.validateAndSetSelectionPageUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).validateAndSetContextPath();
                    one(mockInterface).validateAndSetSelectionPageUrl();
                }
            });

            String contextPath = "/good/" + getRandomChar(GOOD_PATH_CHARS) + "/path";
            configImpl.initProps(getRequiredConfigProps(contextPath));

            assertFalse("Local authentication should be disabled by default, but was found to be enabled.", configImpl.isLocalAuthenticationEnabled());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initProps_enableLocalAuthentication_false() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                void validateAndSetContextPath(String contextPath) {
                    mockInterface.validateAndSetContextPath();
                }

                @Override
                void validateAndSetSelectionPageUrl(String contextPath) {
                    mockInterface.validateAndSetSelectionPageUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).validateAndSetContextPath();
                    one(mockInterface).validateAndSetSelectionPageUrl();
                }
            });

            String contextPath = "/good/" + getRandomChar(GOOD_PATH_CHARS) + "/path";
            Map<String, Object> props = getRequiredConfigProps(contextPath);
            props.put(SocialLoginWebappConfigImpl.KEY_ENABLE_LOCAL_AUTHENTICATION, false);

            configImpl.initProps(props);

            assertFalse("Local authentication should be disabled but was found to be enabled.", configImpl.isLocalAuthenticationEnabled());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initProps_enableLocalAuthentication_true() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                void validateAndSetContextPath(String contextPath) {
                    mockInterface.validateAndSetContextPath();
                }

                @Override
                void validateAndSetSelectionPageUrl(String contextPath) {
                    mockInterface.validateAndSetSelectionPageUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).validateAndSetContextPath();
                    one(mockInterface).validateAndSetSelectionPageUrl();
                }
            });

            String contextPath = "/good/" + getRandomChar(GOOD_PATH_CHARS) + "/path";
            Map<String, Object> props = getRequiredConfigProps(contextPath);
            props.put(SocialLoginWebappConfigImpl.KEY_ENABLE_LOCAL_AUTHENTICATION, true);

            configImpl.initProps(props);

            assertTrue("Local authentication should be enabled but was found to be disabled.", configImpl.isLocalAuthenticationEnabled());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateAndSetContextPath **************************************/

    @Test
    public void validateAndSetContextPath_invalidPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                boolean isValidUriPath(String contextPath) {
                    return mockInterface.isValidUriPath();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isValidUriPath();
                    will(returnValue(false));
                }
            });

            String contextPath = "Some path";
            configImpl.validateAndSetContextPath(contextPath);

            assertEquals("Context root should have been the default.", Oauth2LoginConfigImpl.DEFAULT_CONTEXT_ROOT, Oauth2LoginConfigImpl.getContextRoot());

            verifyLogMessageWithInserts(outputMgr, CWWKS5465E_INVALID_CONTEXT_PATH_CHARS, contextPath);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateAndSetContextPath_validPath() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                boolean isValidUriPath(String contextPath) {
                    return mockInterface.isValidUriPath();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isValidUriPath();
                    will(returnValue(true));
                }
            });

            String contextPath = "Some path";
            configImpl.validateAndSetContextPath(contextPath);

            assertEquals("Context root should have been set to the provided path but was not.", contextPath, Oauth2LoginConfigImpl.getContextRoot());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateAndSetSelectionPageUrl **************************************/

    @Test
    public void validateAndSetSelectionPageUrl_nullOrEmptyUrl() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                boolean isSelectionPageUrlNullOrEmpty(String contextPath) {
                    return mockInterface.isSelectionPageUrlNullOrEmpty();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isSelectionPageUrlNullOrEmpty();
                    will(returnValue(true));
                }
            });

            String url = RandomUtils.getRandomSelection(null, "");
            configImpl.validateAndSetSelectionPageUrl(url);

            String resultUrl = configImpl.getSocialMediaSelectionPageUrl();
            assertNull("Selection page URL should have been set to null but was: [" + resultUrl + "].", resultUrl);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateAndSetSelectionPageUrl_nonEmptyUrl() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                boolean isSelectionPageUrlNullOrEmpty(String contextPath) {
                    return mockInterface.isSelectionPageUrlNullOrEmpty();
                }

                void validateAndSetNonEmptySelectionPageUrl(String selectionPageUrl) {
                    mockInterface.validateAndSetNonEmptySelectionPageUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isSelectionPageUrlNullOrEmpty();
                    will(returnValue(false));
                    one(mockInterface).validateAndSetNonEmptySelectionPageUrl();
                }
            });

            String url = "Some URL";
            configImpl.validateAndSetSelectionPageUrl(url);

            // Call to actually set the URL is mocked, so the URL should be null
            String resultUrl = configImpl.getSocialMediaSelectionPageUrl();
            assertNull("Selection page URL should have been null but was: [" + resultUrl + "].", resultUrl);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** isSelectionPageUrlNullOrEmpty **************************************/

    @Test
    public void isSelectionPageUrlNullOrEmpty_null() {
        try {
            assertTrue("Result should have been true for a null URL.", configImpl.isSelectionPageUrlNullOrEmpty(null));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isSelectionPageUrlNullOrEmpty_empty() {
        try {
            assertTrue("Result should have been true for an empty URL.", configImpl.isSelectionPageUrlNullOrEmpty(""));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isSelectionPageUrlNullOrEmpty_whitespace() {
        try {
            assertFalse("Result should have been false for a URL of only white space.", configImpl.isSelectionPageUrlNullOrEmpty(" \t\n\r"));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isSelectionPageUrlNullOrEmpty_nonEmpty() {
        try {
            assertFalse("Result should have been false for a non-empty URL.", configImpl.isSelectionPageUrlNullOrEmpty("Some/Url value"));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateAndSetNonEmptySelectionPageUrl **************************************/

    @Test
    public void validateAndSetNonEmptySelectionPageUrl_nullOrEmptyUrl() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                boolean isHttpOrRelativeUrl(String selectionPageUrl) {
                    return mockInterface.isHttpOrRelativeUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isHttpOrRelativeUrl();
                    will(returnValue(false));
                }
            });

            String url = RandomUtils.getRandomSelection(null, "");
            configImpl.validateAndSetNonEmptySelectionPageUrl(url);

            String resultUrl = configImpl.getSocialMediaSelectionPageUrl();
            assertNull("Selection page URL should have been set to null but was: [" + resultUrl + "].", resultUrl);

            verifyLogMessageWithInserts(outputMgr, CWWKS5431E_SELECTION_PAGE_URL_NOT_HTTP, url);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateAndSetNonEmptySelectionPageUrl_nonHttpUrl() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                boolean isHttpOrRelativeUrl(String selectionPageUrl) {
                    return mockInterface.isHttpOrRelativeUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isHttpOrRelativeUrl();
                    will(returnValue(false));
                }
            });

            String url = "ftp://www.ibm.com";
            configImpl.validateAndSetNonEmptySelectionPageUrl(url);

            String resultUrl = configImpl.getSocialMediaSelectionPageUrl();
            assertNull("Selection page URL should have been set to null but was: [" + resultUrl + "].", resultUrl);

            verifyLogMessageWithInserts(outputMgr, CWWKS5431E_SELECTION_PAGE_URL_NOT_HTTP, Pattern.quote(url));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateAndSetNonEmptySelectionPageUrl_invalidUrl() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                boolean isHttpOrRelativeUrl(String selectionPageUrl) {
                    return mockInterface.isHttpOrRelativeUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isHttpOrRelativeUrl();
                    will(returnValue(true));
                }
            });

            String url = "/bad/" + getRandomChar(BAD_PATH_CHARS) + "/URL";
            configImpl.validateAndSetNonEmptySelectionPageUrl(url);

            String resultUrl = configImpl.getSocialMediaSelectionPageUrl();
            assertNull("Selection page URL should have been set to null but was: [" + resultUrl + "].", resultUrl);

            // Depending on which bad path char is chosen, the sub message will be different
            String subMsg = "(" + CWWKS5417E_EXCEPTION_INITIALIZING_URL + "|" + CWWKS5488W_URI_CONTAINS_INVALID_CHARS + ")";
            verifyLogMessage(outputMgr, CWWKS5430W_SELECTION_PAGE_URL_NOT_VALID + ".+\\[" + Pattern.quote(url) + "\\].+" + subMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateAndSetNonEmptySelectionPageUrl_invalidPercentChar() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                boolean isHttpOrRelativeUrl(String selectionPageUrl) {
                    return mockInterface.isHttpOrRelativeUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isHttpOrRelativeUrl();
                    will(returnValue(true));
                }
            });

            String url = "/good/%/URL";
            configImpl.validateAndSetNonEmptySelectionPageUrl(url);

            String resultUrl = configImpl.getSocialMediaSelectionPageUrl();
            assertNull("Selection page URL should have been set to null but was: [" + resultUrl + "].", resultUrl);

            // The '%' character didn't have a hexadecimal value after it. So although the '%' is a valid char, the URL itself is not valid
            verifyLogMessage(outputMgr, CWWKS5430W_SELECTION_PAGE_URL_NOT_VALID + ".+\\[" + Pattern.quote(url) + "\\].+" + CWWKS5417E_EXCEPTION_INITIALIZING_URL);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateAndSetNonEmptySelectionPageUrl_validUrl() {
        try {
            configImpl = new SocialLoginWebappConfigImpl() {
                @Override
                boolean isHttpOrRelativeUrl(String selectionPageUrl) {
                    return mockInterface.isHttpOrRelativeUrl();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isHttpOrRelativeUrl();
                    will(returnValue(true));
                }
            });

            StringBuilder randomChar = new StringBuilder(getRandomChar(GOOD_PATH_CHARS));
            if (randomChar.toString().equals("%")) {
                // '%' character implies an encoded value (e.g. %20), so some hex value must be provided for this to be valid
                randomChar.append("20");
            }
            String url = "/good/" + randomChar.toString() + "/URL";
            configImpl.validateAndSetNonEmptySelectionPageUrl(url);

            String resultUrl = configImpl.getSocialMediaSelectionPageUrl();
            assertEquals("Selection page URL value was not set to the expected value.", url, resultUrl);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** isHttpOrRelativeUrl **************************************/

    @Test
    public void isHttpOrRelativeUrl_nullUrl() {
        try {
            String url = null;
            assertFalse("Result should have been false for a null URL.", configImpl.isHttpOrRelativeUrl(url));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isHttpOrRelativeUrl_emptyUrl() {
        try {
            String url = "";
            assertTrue("Result should have been true for an empty URL.", configImpl.isHttpOrRelativeUrl(url));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isHttpOrRelativeUrl_whitespaceUrl() {
        try {
            String url = " \t\r\n";
            assertTrue("Result should have been true for a URL of just white space.", configImpl.isHttpOrRelativeUrl(url));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isHttpOrRelativeUrl_simpleString() {
        try {
            // Randomly decide whether or not to start with '/' character
            String startingChar = RandomUtils.getRandomSelection("", "/");
            String url = startingChar + "Some simple/" + getRandomChar(GOOD_PATH_CHARS) + "/string";
            assertTrue("Result should have been true for URL [" + url + "].", configImpl.isHttpOrRelativeUrl(url));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isHttpOrRelativeUrl_nonHttpUrl() {
        try {
            // Verify that the output is the same for an empty host and non-empty host
            String host = RandomUtils.getRandomSelection("", "somehost.com");
            String url = "ftp://" + host;
            assertFalse("Result should have been false for URL [" + url + "].", configImpl.isHttpOrRelativeUrl(url));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isHttpOrRelativeUrl_httpUrl() {
        try {
            // Verify that the output is the same for an empty host and non-empty host
            String host = RandomUtils.getRandomSelection("", "somehost.com");
            String url = "http://" + host;
            assertTrue("Result should have been true for URL [" + url + "].", configImpl.isHttpOrRelativeUrl(url));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isHttpOrRelativeUrl_httpUrl_mixedCapitalization() {
        try {
            // Verify that the output is the same for an empty host and non-empty host
            String host = RandomUtils.getRandomSelection("", "somehost.com");
            String url = "hTtP://" + host;
            assertTrue("Result should have been true for URL [" + url + "].", configImpl.isHttpOrRelativeUrl(url));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isHttpOrRelativeUrl_httpsUrl() {
        try {
            // Verify that the output is the same for an empty host and non-empty host
            String host = RandomUtils.getRandomSelection("", "somehost.com");
            String url = "https://" + host;
            assertTrue("Result should have been true for URL [" + url + "].", configImpl.isHttpOrRelativeUrl(url));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isHttpOrRelativeUrl_httpsUrl_mixedCapitalization() {
        try {
            // Verify that the output is the same for an empty host and non-empty host
            String host = RandomUtils.getRandomSelection("", "somehost.com");
            String url = "hTtPs://" + host;
            assertTrue("Result should have been true for URL [" + url + "].", configImpl.isHttpOrRelativeUrl(url));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** isValidUriPath **************************************/

    @Test
    public void isValidUriPath_nullArg() {
        try {
            assertFalse("Result should have been false for a null path.", configImpl.isValidUriPath(null));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isValidUriPath_emptyArg() {
        try {
            String path = "";
            assertTrue("Result should have been true for an empty path.", configImpl.isValidUriPath(path));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isValidUriPath_alphaNumericPath() {
        try {
            for (char c = 'a'; c < 'z'; c++) {
                String path = new StringBuilder(c).toString();
                assertTrue("Result should have been true for an alphanumeric path with character [" + path + "].", configImpl.isValidUriPath(path));
            }
            for (char c = 'A'; c < 'Z'; c++) {
                String path = new StringBuilder(c).toString();
                assertTrue("Result should have been true for an alphanumeric path with character [" + path + "].", configImpl.isValidUriPath(path));
            }
            for (char c = '0'; c < '9'; c++) {
                String path = new StringBuilder(c).toString();
                assertTrue("Result should have been true for an alphanumeric path with character [" + path + "].", configImpl.isValidUriPath(path));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isValidUriPath_validSpecialChars() {
        try {
            for (int i = 0; i < GOOD_PATH_CHARS.length(); i++) {
                String path = GOOD_PATH_CHARS.substring(i, i + 1);
                assertTrue("Result should have been true for path with valid special character [" + path + "].", configImpl.isValidUriPath(path));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isValidUriPath_invalidSpecialChars() {
        try {
            for (int i = 0; i < BAD_PATH_CHARS.length(); i++) {
                String path = BAD_PATH_CHARS.substring(i, i + 1);
                assertFalse("Result should have been false for path with invalid special character [" + path + "].", configImpl.isValidUriPath(path));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isValidUriPath_typicalPath() {
        try {
            String path = "/some/1/typical-path/V%20alue";
            assertTrue("Result should have been true for typical path value [" + path + "].", configImpl.isValidUriPath(path));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isValidUriPath_individualCharCheck() {
        try {
            for (char c = 0; c < 2000; c++) {
                String character = "" + c;
                if (GOOD_PATH_CHARS.contains(character) || Pattern.matches("[a-zA-Z0-9]", character)) {
                    assertTrue("Result should have been true for path with valid character [" + character + "] (Code point " + ((int) c) + ").", configImpl.isValidUriPath(character));
                } else {
                    assertFalse("Result should have been false for path with invalid character [" + character + "] (Code point " + ((int) c) + ").", configImpl.isValidUriPath(character));
                }
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private Map<String, Object> getRequiredConfigProps(String contextPath) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(WABConfiguration.CONTEXT_PATH, contextPath);
        return props;
    }

    private char getRandomChar(String charString) {
        int randomIndex = new Random().nextInt(charString.length());
        char c = charString.charAt(randomIndex);
        System.out.println("Input: [" + charString + "]");
        System.out.println("Chose: [" + c + "]");
        return c;
    }
}
