/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.NamingException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BridgeBuilderImplTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String APP_CONTEXT = "localhost /contextRoot";

    private BridgeBuilderImpl bridgeBuilder;
    private AuthConfigFactory providerFactory;
    private AuthConfigProvider authConfigProvider;
    private BeanManager beanManager;
    private Set<Bean<HttpAuthenticationMechanism>> httpAuthMechs;

    @Before
    public void setUp() throws Exception {
        bridgeBuilder = new BridgeBuilderImplTestDouble();
        providerFactory = mockery.mock(AuthConfigFactory.class);
        authConfigProvider = null;
        beanManager = mockery.mock(BeanManager.class);
        httpAuthMechs = new HashSet<Bean<HttpAuthenticationMechanism>>();
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testNoAuthMechDoesNotRegisterProvider() throws Exception {
        withNoCachedProvider().withBeanManager().doesNotRegisterProvider();

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
    }

    @Test
    public void testOneAuthMechRegistersProvider() throws Exception {
        Bean<HttpAuthenticationMechanism> httpAuthenticationMechanismBean = mockery.mock(Bean.class);
        httpAuthMechs.add(httpAuthenticationMechanismBean);

        withNoCachedProvider().withBeanManager();

        mockery.checking(new Expectations() {
            {
                one(providerFactory).registerConfigProvider(with(aNonNull(AuthConfigProvider.class)), with("HttpServlet"), with(APP_CONTEXT),
                                                            with(aNonNull(String.class)));
            }
        });

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
    }

    @Test
    public void testMoreThanOneAuthMechDoesNotRegisterProvider() throws Exception {
        final Bean<HttpAuthenticationMechanism> httpAuthenticationMechanismBean1 = mockery.mock(Bean.class, "httpAuthenticationMechanismBean1");
        final Bean<HttpAuthenticationMechanism> httpAuthenticationMechanismBean2 = mockery.mock(Bean.class, "httpAuthenticationMechanismBean2");
        httpAuthMechs.add(httpAuthenticationMechanismBean1);
        httpAuthMechs.add(httpAuthenticationMechanismBean2);

        withNoCachedProvider().withBeanManager().doesNotRegisterProvider();
        // the debug might be enabled, therefore allowing to some invocation which is only invoked when trace is enabled.
        mockery.checking(new Expectations() {
            {
                allowing(httpAuthenticationMechanismBean1).getBeanClass();
                will(returnValue("httpAuthenticationMechanismBean1.class"));
                allowing(httpAuthenticationMechanismBean2).getBeanClass();
                will(returnValue("httpAuthenticationMechanismBean2.class"));
            }
        });

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
        // TODO: Assert serviceability message is issued.
    }

    @Test
    public void testNoBeanManagerDoesNotRegisterProvider() throws Exception {
        withNoCachedProvider().withNoBeanManager().doesNotRegisterProvider();

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
    }

    @Test
    public void testWithCachedProviderDoesNotRegisterProvider() throws Exception {
        withCachedProvider().doesNotRegisterProvider();

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
    }

    private BridgeBuilderImplTest withNoCachedProvider() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(providerFactory).getConfigProvider("HttpServlet", APP_CONTEXT, null);
                will(returnValue(null));
            }
        });
        return this;
    }

    private BridgeBuilderImplTest withCachedProvider() throws Exception {
        authConfigProvider = mockery.mock(AuthConfigProvider.class);
        mockery.checking(new Expectations() {
            {
                one(providerFactory).getConfigProvider("HttpServlet", APP_CONTEXT, null);
                will(returnValue(authConfigProvider));
            }
        });
        return this;
    }

    private BridgeBuilderImplTest withBeanManager() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(beanManager).getBeans((Type) with(HttpAuthenticationMechanism.class), (Annotation[]) with(Collections.EMPTY_LIST.toArray()));
                will(returnValue(httpAuthMechs));
            }
        });
        return this;
    }

    private BridgeBuilderImplTest withNoBeanManager() throws Exception {
        bridgeBuilder = new BridgeBuilderImpl() {
            @Override
            protected BeanManager getBeanManager() throws NamingException {
                throw new NamingException();
            }
        };
        return this;
    }

    private BridgeBuilderImplTest doesNotRegisterProvider() throws Exception {
        mockery.checking(new Expectations() {
            {
                never(providerFactory).registerConfigProvider(with(aNonNull(AuthConfigProvider.class)), with("HttpServlet"), with(APP_CONTEXT),
                                                              with(aNonNull(String.class)));
            }
        });
        return this;
    }

    class BridgeBuilderImplTestDouble extends BridgeBuilderImpl {

        @Override
        protected BeanManager getBeanManager() throws NamingException {
            return beanManager;
        }
    }
}
