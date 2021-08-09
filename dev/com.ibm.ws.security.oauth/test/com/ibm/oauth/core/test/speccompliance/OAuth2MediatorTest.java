/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test.speccompliance;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.ibm.oauth.core.api.OAuthComponentFactory;
import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException;
import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.mediator.OAuth20MediatorWrapper;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;
import com.ibm.oauth.core.test.base.BaseConfig;
import com.ibm.oauth.core.test.base.MockMediator;
import com.ibm.oauth.core.test.base.MockServletRequest;
import com.ibm.oauth.core.test.base.MockServletResponse;

import junit.framework.TestCase;

public class OAuth2MediatorTest extends TestCase {
    protected static final String SIMPLE_ACCESS_TOKEN = "TEST_TOKEN";
    protected static final String PUBLIC_CLIENT_ID = "key";
    protected static final String CONF_CLIENT_ID = "key";
    protected static final String CONF_CLIENT_SECRET = "secret";

    protected static final String PUBLIC_REDIRECT_URI = "http://localhost:9080/oauth/client.jsp";
    protected static final String CONF_REDIRECT_URI = "http://localhost:9080/oauth/client.jsp";

    private class OAuth2MediatorTestConfiguration extends BaseConfig {
        public OAuth2MediatorTestConfiguration() {
            super();
        }

        public void setConfiguration(String name, String... value) {
            _config.put(name, value);
        }

        @Override
        public String getUniqueId() {
            // use different id for different cases so that the mediator is
            // instantiated every time
            return UUID.randomUUID().toString();
        }
    };

    private OAuth2MediatorTestConfiguration config = new OAuth2MediatorTestConfiguration();

    private OAuth20Component getOAuth20Component() throws OAuthException {
        OAuthComponentInstance me = OAuthComponentFactory.getOAuthComponentInstance(config);
        return me.getOAuth20Component();
    }

    @Override
    protected void setUp() throws Exception {
        // clean the mediators in the thread
        MockMediator.init();
        config = new OAuth2MediatorTestConfiguration();
    }

    public void testMediatorInitialization() {
        config.setConfiguration(
                                OAuthComponentConfigurationConstants.OAUTH20_MEDIATOR_CLASSNAMES,
                                MockMediator1.class.getName(), MockMediator2.class.getName(), MockMediator3.class.getName());
        config.setConfiguration("testConfig", "testValue");
        config.setConfiguration("testConfigArray", "value1", "value2");

        OAuth20Component component = null;
        try {
            component = getOAuth20Component();
        } catch (OAuthException e) {
            fail("No exception expected");
        }
        List<OAuthComponentConfiguration> initConfigs = null;
        OAuthComponentConfiguration cfg = null;
        MockMediator m = null;

        LinkedList<MockMediator> mediators = MockMediator.getMediators();
        assertEquals("number of mediators", 3, mediators.size());

        // validate mediator1
        m = mediators.get(0);
        assertTrue("instance of mediator 1", m instanceof MockMediator1);
        initConfigs = m.getInitConfig();
        cfg = initConfigs.get(0);
        assertEquals("init called", 1, initConfigs.size());
        assertEquals("init config value", "testValue", cfg.getConfigPropertyValue("testConfig"));
        assertEquals("init config value", "value1", cfg.getConfigPropertyValues("testConfigArray")[0]);
        assertEquals("init config value", "value2", cfg.getConfigPropertyValues("testConfigArray")[1]);
        validateCallCount(m, 0, 0, 0, 0, 0, 0);

        // validate mediator2
        m = mediators.get(1);
        assertTrue("instance of mediator 2", m instanceof MockMediator2);
        initConfigs = m.getInitConfig();
        cfg = initConfigs.get(0);
        assertEquals("init called", 1, initConfigs.size());
        assertEquals("init config value", "testValue", cfg.getConfigPropertyValue("testConfig"));
        assertEquals("init config value", "value1", cfg.getConfigPropertyValues("testConfigArray")[0]);
        assertEquals("init config value", "value2", cfg.getConfigPropertyValues("testConfigArray")[1]);
        validateCallCount(m, 0, 0, 0, 0, 0, 0);

        // validate mediator3
        m = mediators.get(2);
        assertTrue("instance of mediator 3", m instanceof MockMediator3);
        initConfigs = m.getInitConfig();
        cfg = initConfigs.get(0);
        assertEquals("init called", 1, initConfigs.size());
        assertEquals("init config value", "testValue", cfg.getConfigPropertyValue("testConfig"));
        assertEquals("init config value", "value1", cfg.getConfigPropertyValues("testConfigArray")[0]);
        assertEquals("init config value", "value2", cfg.getConfigPropertyValues("testConfigArray")[1]);
        validateCallCount(m, 0, 0, 0, 0, 0, 0);
    }

    public void testMediatorWithAuthorizationCodeFlow() {
        config.setConfiguration(
                                OAuthComponentConfigurationConstants.OAUTH20_MEDIATOR_CLASSNAMES,
                                MockMediator1.class.getName(), MockMediator2.class.getName(), MockMediator3.class.getName());
        config.setConfiguration(
                                OAuthComponentConfigurationConstants.OAUTH20_ALLOW_PUBLIC_CLIENTS,
                                "true");
        config.setConfiguration("testConfig", "testValue");
        config.setConfiguration("testConfigArray", "value1", "value2");

        OAuth20Component component = null;
        try {
            component = getOAuth20Component();
        } catch (OAuthException e) {
            fail("No exception expected");
        }

        MockMediator m = null;
        LinkedList<MockMediator> mediators = MockMediator.getMediators();
        assertEquals("number of mediators", 3, mediators.size());

        String responseType = "code";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse responseauth = new MockServletResponse();

        StringWriter responseBuffer = new StringWriter();
        responseauth.setWriter(responseBuffer);

        OAuthResult result = component.processAuthorization("testuser",
                                                            PUBLIC_CLIENT_ID, PUBLIC_REDIRECT_URI, "wrong code", state,
                                                            scope, responseauth);
        m = mediators.get(0);
        validateCallCount(m, 0, 0, 0, 1, 0, 0);
        m = mediators.get(1);
        validateCallCount(m, 0, 0, 0, 1, 0, 0);
        m = mediators.get(2);
        validateCallCount(m, 0, 0, 0, 1, 0, 0);

        result = component.processAuthorization("testuser", PUBLIC_CLIENT_ID,
                                                PUBLIC_REDIRECT_URI, responseType, state, scope, responseauth);
        // validate mediator1
        m = mediators.get(0);
        assertEquals("init called", 1, m.getInitConfig().size());
        validateCallCount(m, 1, 0, 0, 1, 0, 0);
        // validate mediator2
        m = mediators.get(1);
        assertEquals("init called", 1, m.getInitConfig().size());
        validateCallCount(m, 1, 0, 0, 1, 0, 0);
        // validate mediator3
        m = mediators.get(2);
        assertEquals("init called", 1, m.getInitConfig().size());
        validateCallCount(m, 1, 0, 0, 1, 0, 0);

        // get access token
        String code = result.getAttributeList().getAttributeValueByName(
                                                                        "authorization_code_id");

        MockServletRequest req = new MockServletRequest();
        req.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                      "application/x-www-form-urlencoded");
        req.setParameter("client_id", CONF_CLIENT_ID);
        req.setParameter("client_secret", CONF_CLIENT_SECRET);
        req.setParameter("grant_type", "authorization_code");
        req.setParameter("redirect_uri", CONF_REDIRECT_URI);
        req.setParameter("code", code);
        req.setMethod("GET");
        req.setServletPath("/oauth2");
        req.setPathInfo("/access_token");

        MockServletResponse resp = new MockServletResponse();
        responseBuffer = new StringWriter();
        resp.setWriter(responseBuffer);

        result = component.processTokenRequest(null, req, resp);
        String accessToken = result.getAttributeList().getAttributeValueByName(
                                                                               "access_token");

        m = mediators.get(0);
        validateCallCount(m, 1, 1, 0, 1, 0, 0);
        m = mediators.get(1);
        validateCallCount(m, 1, 1, 0, 1, 0, 0);
        m = mediators.get(2);
        validateCallCount(m, 1, 1, 0, 1, 0, 0);

        req.setParameter("code", code + "xyz");
        result = component.processTokenRequest(null, req, resp);

        m = mediators.get(0);
        validateCallCount(m, 1, 1, 0, 1, 1, 0);
        m = mediators.get(1);
        validateCallCount(m, 1, 1, 0, 1, 1, 0);
        m = mediators.get(2);
        validateCallCount(m, 1, 1, 0, 1, 1, 0);

        req = new MockServletRequest();
        req.setHeader("Authorization", "Bearer " + accessToken);
        result = component.processResourceRequest(req);

        m = mediators.get(0);
        validateCallCount(m, 1, 1, 1, 1, 1, 0);
        m = mediators.get(1);
        validateCallCount(m, 1, 1, 1, 1, 1, 0);
        m = mediators.get(2);
        validateCallCount(m, 1, 1, 1, 1, 1, 0);

        req = new MockServletRequest();
        req.setHeader("Authorization", "Bearer " + accessToken + "xyz");
        result = component.processResourceRequest(req);
        validateCallCount(m, 1, 1, 1, 1, 1, 1);
        m = mediators.get(1);
        validateCallCount(m, 1, 1, 1, 1, 1, 1);
        m = mediators.get(2);
        validateCallCount(m, 1, 1, 1, 1, 1, 1);
    }

    public void testMediatorWrapperImpl() {
        MockMediator1 m1 = new MockMediator1();
        MockMediator2 m2 = new MockMediator2();
        MockMediator3 m3 = new MockMediator3();

        List<OAuth20Mediator> mediator = Arrays.asList(new OAuth20Mediator[] {
                                                                               m1, m2, m3 });

        OAuth20MediatorWrapper wrapper = new OAuth20MediatorWrapper(mediator, new OAuthStatisticsImpl());

        OAuthComponentConfiguration cfg = new OAuth2MediatorTestConfiguration();
        wrapper.init(cfg);
        assertEquals("init call count", 1, m1.getInitConfig().size());
        assertEquals("init call param", cfg, m1.getInitConfig().get(0));
        assertEquals("init call count", 1, m2.getInitConfig().size());
        assertEquals("init call param", cfg, m2.getInitConfig().get(0));
        assertEquals("init call count", 1, m3.getInitConfig().size());
        assertEquals("init call param", cfg, m3.getInitConfig().get(0));

        AttributeList attr = null;
        OAuthException ex = null;
        try {
            for (int i = 0; i < 3; i++) {
                // mediateAuthorize
                attr = new AttributeList();
                wrapper.mediateAuthorize(attr);
                validateCallCount(m1, i + 1, i, i, i, i, i);
                assertEquals("mediateAuthorize call param", attr, m1.getMediateAuthorizeAttr().get(i));
                validateCallCount(m2, i + 1, i, i, i, i, i);
                assertEquals("mediateAuthorize call param", attr, m2.getMediateAuthorizeAttr().get(i));
                validateCallCount(m3, i + 1, i, i, i, i, i);
                assertEquals("mediateAuthorize call param", attr, m3.getMediateAuthorizeAttr().get(i));
                // mediateToken
                attr = new AttributeList();
                wrapper.mediateToken(attr);
                validateCallCount(m1, i + 1, i + 1, i, i, i, i);
                assertEquals("mediateTokend call param", attr, m1.getMediateTokenAttr().get(i));
                validateCallCount(m2, i + 1, i + 1, i, i, i, i);
                assertEquals("mediateTokend call param", attr, m2.getMediateTokenAttr().get(i));
                validateCallCount(m3, i + 1, i + 1, i, i, i, i);
                assertEquals("mediateTokend call param", attr, m3.getMediateTokenAttr().get(i));
                // mediateResource
                attr = new AttributeList();
                wrapper.mediateResource(attr);
                validateCallCount(m1, i + 1, i + 1, i + 1, i, i, i);
                assertEquals("mediateResourced call param", attr, m1.getMediateResourceAttr().get(i));
                validateCallCount(m2, i + 1, i + 1, i + 1, i, i, i);
                assertEquals("mediateResourced call param", attr, m2.getMediateResourceAttr().get(i));
                validateCallCount(m3, i + 1, i + 1, i + 1, i, i, i);
                assertEquals("mediateResourced call param", attr, m3.getMediateResourceAttr().get(i));
                // mediateAuthorizeException
                attr = new AttributeList();
                ex = new OAuth20Exception("mock exception", "mock exception", null);
                wrapper.mediateAuthorizeException(attr, ex);
                validateCallCount(m1, i + 1, i + 1, i + 1, i + 1, i, i);
                assertEquals("mediateAuthorizeException call param", attr, m1.getMediateAuthorizeExceptionAttr().get(i));
                assertEquals("mediateAuthorizeException call param", ex, m1.getMediateAuthorizeExceptionEx().get(i));
                validateCallCount(m2, i + 1, i + 1, i + 1, i + 1, i, i);
                assertEquals("mediateAuthorizeException call param", attr, m2.getMediateAuthorizeExceptionAttr().get(i));
                assertEquals("mediateAuthorizeException call param", ex, m2.getMediateAuthorizeExceptionEx().get(i));
                validateCallCount(m3, i + 1, i + 1, i + 1, i + 1, i, i);
                assertEquals("mediateAuthorizeException call param", attr, m3.getMediateAuthorizeExceptionAttr().get(i));
                assertEquals("mediateAuthorizeException call param", ex, m3.getMediateAuthorizeExceptionEx().get(i));
                // mediateTokenException
                attr = new AttributeList();
                ex = new OAuth20Exception("mock exception", "mock exception", null);
                wrapper.mediateTokenException(attr, ex);
                validateCallCount(m1, i + 1, i + 1, i + 1, i + 1, i + 1, i);
                assertEquals("mediateTokenException call param", attr, m1.getMediateTokenExceptionAttr().get(i));
                assertEquals("mediateTokenException call param", ex, m1.getMediateTokenExceptionEx().get(i));
                validateCallCount(m2, i + 1, i + 1, i + 1, i + 1, i + 1, i);
                assertEquals("mediateTokenException call param", attr, m2.getMediateTokenExceptionAttr().get(i));
                assertEquals("mediateTokenException call param", ex, m2.getMediateTokenExceptionEx().get(i));
                validateCallCount(m3, i + 1, i + 1, i + 1, i + 1, i + 1, i);
                assertEquals("mediateTokenException call param", attr, m3.getMediateTokenExceptionAttr().get(i));
                assertEquals("mediateTokenException call param", ex, m3.getMediateTokenExceptionEx().get(i));
                // mediateResourceException
                attr = new AttributeList();
                ex = new OAuth20Exception("mock exception", "mock exception", null);
                wrapper.mediateResourceException(attr, ex);
                validateCallCount(m1, i + 1, i + 1, i + 1, i + 1, i + 1, i + 1);
                assertEquals("mediateResourceException call param", attr, m1.getMediateResourceExceptionAttr().get(i));
                assertEquals("mediateResourceException call param", ex, m1.getMediateResourceExceptionEx().get(i));
                validateCallCount(m2, i + 1, i + 1, i + 1, i + 1, i + 1, i + 1);
                assertEquals("mediateResourceException call param", attr, m2.getMediateResourceExceptionAttr().get(i));
                assertEquals("mediateResourceException call param", ex, m2.getMediateResourceExceptionEx().get(i));
                validateCallCount(m3, i + 1, i + 1, i + 1, i + 1, i + 1, i + 1);
                assertEquals("mediateResourceException call param", attr, m3.getMediateResourceExceptionAttr().get(i));
                assertEquals("mediateResourceException call param", ex, m3.getMediateResourceExceptionEx().get(i));
            }
        } catch (OAuth20MediatorException e) {
            fail("no exceptions should be thrown");
        }
    }

    public void testMediatorWrapperImplWithException() {
        MockMediator1 m1 = new MockMediator1();
        MockMediator4 m2 = new MockMediator4();
        MockMediator3 m3 = new MockMediator3();

        List<OAuth20Mediator> mediator = Arrays.asList(new OAuth20Mediator[] {
                                                                               m1, m2, m3 });

        OAuth20MediatorWrapper wrapper = new OAuth20MediatorWrapper(mediator, new OAuthStatisticsImpl());

        OAuthComponentConfiguration cfg = new OAuth2MediatorTestConfiguration();
        wrapper.init(cfg);

        AttributeList attr = null;
        OAuthException ex = null;
        int i = 0;

        attr = new AttributeList();
        try {
            wrapper.mediateAuthorize(attr);
            fail("should not reach here");
        } catch (OAuth20MediatorException e) {
            assertEquals("exception message", "mock mediator exception", e.getMessage());
        }
        validateCallCount(m1, i + 1, i, i, i, i, i);
        assertEquals("mediateAuthorize call param", attr, m1.getMediateAuthorizeAttr().get(i));
        validateCallCount(m2, i + 1, i, i, i, i, i);
        assertEquals("mediateAuthorize call param", attr, m2.getMediateAuthorizeAttr().get(i));
        validateCallCount(m3, 0, 0, 0, 0, 0, 0);

        attr = new AttributeList();
        try {
            wrapper.mediateToken(attr);
            fail("should not reach here");
        } catch (OAuth20MediatorException e) {
            assertEquals("exception message", "mock mediator exception", e.getMessage());
        }
        validateCallCount(m1, i + 1, i + 1, i, i, i, i);
        assertEquals("mediateToken call param", attr, m1.getMediateTokenAttr().get(i));
        validateCallCount(m2, i + 1, i + 1, i, i, i, i);
        assertEquals("mediateToken call param", attr, m2.getMediateTokenAttr().get(i));
        validateCallCount(m3, 0, 0, 0, 0, 0, 0);

        attr = new AttributeList();
        try {
            wrapper.mediateResource(attr);
            fail("should not reach here");
        } catch (OAuth20MediatorException e) {
            assertEquals("exception message", "mock mediator exception", e.getMessage());
        }
        validateCallCount(m1, i + 1, i + 1, i + 1, i, i, i);
        assertEquals("mediateResource call param", attr, m1.getMediateResourceAttr().get(i));
        validateCallCount(m2, i + 1, i + 1, i + 1, i, i, i);
        assertEquals("mediateResource call param", attr, m2.getMediateResourceAttr().get(i));
        validateCallCount(m3, 0, 0, 0, 0, 0, 0);

        attr = new AttributeList();
        ex = new OAuth20Exception("mock exception", "mock exception", null);
        try {
            wrapper.mediateAuthorizeException(attr, ex);
            fail("should not reach here");
        } catch (OAuth20MediatorException e) {
            assertEquals("exception message", "mock mediator exception", e.getMessage());
            assertEquals("cause exception", ex, e.getCause());
        }
        validateCallCount(m1, i + 1, i + 1, i + 1, i + 1, i, i);
        assertEquals("mediateAuthorizeException call param", attr, m1.getMediateAuthorizeExceptionAttr().get(i));
        assertEquals("mediateAuthorizeException call param", ex, m1.getMediateAuthorizeExceptionEx().get(i));
        validateCallCount(m2, i + 1, i + 1, i + 1, i + 1, i, i);
        assertEquals("mediateAuthorizeException call param", attr, m2.getMediateAuthorizeExceptionAttr().get(i));
        assertEquals("mediateAuthorizeException call param", ex, m2.getMediateAuthorizeExceptionEx().get(i));
        validateCallCount(m3, 0, 0, 0, 0, 0, 0);

        attr = new AttributeList();
        ex = new OAuth20Exception("mock exception", "mock exception", null);
        try {
            wrapper.mediateTokenException(attr, ex);
            fail("should not reach here");
        } catch (OAuth20MediatorException e) {
            assertEquals("exception message", "mock mediator exception", e.getMessage());
            assertEquals("cause exception", ex, e.getCause());
        }
        validateCallCount(m1, i + 1, i + 1, i + 1, i + 1, i + 1, i);
        assertEquals("mediateTokenException call param", attr, m1.getMediateTokenExceptionAttr().get(i));
        assertEquals("mediateTokenException call param", ex, m1.getMediateTokenExceptionEx().get(i));
        validateCallCount(m2, i + 1, i + 1, i + 1, i + 1, i + 1, i);
        assertEquals("mediateTokenException call param", attr, m2.getMediateTokenExceptionAttr().get(i));
        assertEquals("mediateTokenException call param", ex, m2.getMediateTokenExceptionEx().get(i));
        validateCallCount(m3, 0, 0, 0, 0, 0, 0);

        attr = new AttributeList();
        ex = new OAuth20Exception("mock exception", "mock exception", null);
        try {
            wrapper.mediateResourceException(attr, ex);
            fail("should not reach here");
        } catch (OAuth20MediatorException e) {
            assertEquals("exception message", "mock mediator exception", e.getMessage());
            assertEquals("cause exception", ex, e.getCause());
        }
        validateCallCount(m1, i + 1, i + 1, i + 1, i + 1, i + 1, i + 1);
        assertEquals("mediateResourceException call param", attr, m1.getMediateResourceExceptionAttr().get(i));
        assertEquals("mediateResourceException call param", ex, m1.getMediateResourceExceptionEx().get(i));
        validateCallCount(m2, i + 1, i + 1, i + 1, i + 1, i + 1, i + 1);
        assertEquals("mediateResourceException call param", attr, m2.getMediateResourceExceptionAttr().get(i));
        assertEquals("mediateResourceException call param", ex, m2.getMediateResourceExceptionEx().get(i));
        validateCallCount(m3, 0, 0, 0, 0, 0, 0);
    }

    public static class MockMediator1 extends MockMediator {
        public MockMediator1() {
            super();
        }
    }

    public static class MockMediator2 extends MockMediator {
        public MockMediator2() {
            super();
        }
    }

    public static class MockMediator3 extends MockMediator {
        public MockMediator3() {
            super();
        }
    }

    public static class MockMediator4 extends MockMediator {
        public MockMediator4() {
            super();
        }

        @Override
        public void mediateAuthorize(AttributeList attributeList) throws OAuth20MediatorException {
            super.mediateAuthorize(attributeList);
            throw new OAuth20MediatorException("mock mediator exception", new OAuth20Exception("mock exception", "cause", null));
        }

        @Override
        public void mediateToken(AttributeList attributeList) throws OAuth20MediatorException {
            super.mediateToken(attributeList);
            throw new OAuth20MediatorException("mock mediator exception", new OAuth20Exception("mock exception", "cause", null));
        }

        @Override
        public void mediateResource(AttributeList attributeList) throws OAuth20MediatorException {
            super.mediateResource(attributeList);
            throw new OAuth20MediatorException("mock mediator exception", new OAuth20Exception("mock exception", "cause", null));
        }

        @Override
        public void mediateAuthorizeException(AttributeList attributeList,
                                              OAuthException exception) throws OAuth20MediatorException {
            super.mediateAuthorizeException(attributeList, exception);
            throw new OAuth20MediatorException("mock mediator exception", exception);
        }

        @Override
        public void mediateTokenException(AttributeList attributeList,
                                          OAuthException exception) throws OAuth20MediatorException {
            super.mediateTokenException(attributeList, exception);
            throw new OAuth20MediatorException("mock mediator exception", exception);
        }

        @Override
        public void mediateResourceException(AttributeList attributeList,
                                             OAuthException exception) throws OAuth20MediatorException {
            super.mediateResourceException(attributeList, exception);
            throw new OAuth20MediatorException("mock mediator exception", exception);
        }

    }

    private void validateCallCount(MockMediator m, int auth, int token,
                                   int res, int authEx, int tokenEx, int resEx) {
        assertEquals("mediateAuthorize call count", auth, m.getMediateAuthorizeAttr().size());
        assertEquals("mediateToken call count", token, m.getMediateTokenAttr().size());
        assertEquals("mediateResource call count", res, m.getMediateResourceAttr().size());
        assertEquals("mediateAuthorizeException call count", authEx, m.getMediateAuthorizeExceptionAttr().size());
        assertEquals("mediateAuthorizeException call count", authEx, m.getMediateAuthorizeExceptionEx().size());
        assertEquals("mediateTokenException call count", tokenEx, m.getMediateTokenExceptionAttr().size());
        assertEquals("mediateTokenException call count", tokenEx, m.getMediateTokenExceptionEx().size());
        assertEquals("mediateResourceException call count", resEx, m.getMediateResourceExceptionAttr().size());
        assertEquals("mediateResourceException call count", resEx, m.getMediateResourceExceptionEx().size());
    }

}
