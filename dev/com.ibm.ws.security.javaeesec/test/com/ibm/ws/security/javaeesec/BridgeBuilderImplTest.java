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

import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;

import test.common.SharedOutputManager;

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
    private CDI cdi;
    private Instance<ModulePropertiesProvider> mppi;
    private Instance<HttpAuthenticationMechanism> hami;
    private HttpAuthenticationMechanism ham;
    private ModulePropertiesProvider mpp;
    private Iterator itl;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void setUp() throws Exception {
        bridgeBuilder = new BridgeBuilderImplTestDouble();
        providerFactory = mockery.mock(AuthConfigFactory.class);
        authConfigProvider = null;
        cdi = mockery.mock(CDI.class);
        mppi = mockery.mock(Instance.class, "mppi");
        mpp = mockery.mock(ModulePropertiesProvider.class);
        hami = mockery.mock(Instance.class, "hami");
        ham = mockery.mock(HttpAuthenticationMechanism.class, "ham");
        itl = mockery.mock(Iterator.class, "itl");
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @Test
    public void testNoModulePropDoesNotRegisterProvider() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(HttpAuthenticationMechanism.class);
                will(returnValue(hami));
                one(hami).iterator();
                will(returnValue(itl));
                exactly(3).of(itl).hasNext();
                will(onConsecutiveCalls(returnValue(true), returnValue(true), returnValue(false)));
                allowing(itl).next();
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(null));
            }
        });

        withNoCachedProvider().doesNotRegisterProvider();

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
        // No error message for this scenario, since this is normal when an application
        // is not using HttpAuthMech.
    }

    @Test
    public void testUnsatisfiedModulePropDoesNotRegisterProvider() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(HttpAuthenticationMechanism.class);
                will(returnValue(hami));
                one(hami).iterator();
                will(returnValue(itl));
                exactly(3).of(itl).hasNext();
                will(onConsecutiveCalls(returnValue(true), returnValue(true), returnValue(false)));
                allowing(itl).next();
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(mppi));
                one(mppi).isUnsatisfied();
                will(returnValue(true));
            }
        });

        withNoCachedProvider().doesNotRegisterProvider();

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
        // this is an invalid scenario, thus the error message should be shown.
        assertTrue("CWWKS1913E: message was not logged", outputMgr.checkForStandardErr("CWWKS1913E:"));

    }

    @Test
    public void testNoAuthMechDoesNotRegisterProvider() throws Exception {
        withNoCachedProvider().withCDInoHAM().doesNotRegisterProvider();

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
        assertTrue("CWWKS1912E: message was not logged", outputMgr.checkForStandardErr("CWWKS1912E:"));
    }

    @Test
    public void testOneAuthMechRegistersProvider() throws Exception {

        withNoCachedProvider().withCDI();

        mockery.checking(new Expectations() {
            {
                one(hami).isUnsatisfied();
                will(returnValue(false));
                one(hami).isAmbiguous();
                will(returnValue(false));
                one(hami).get();
                will(returnValue(ham));
                one(providerFactory).registerConfigProvider(with(aNonNull(AuthConfigProvider.class)), with("HttpServlet"), with(APP_CONTEXT),
                                                            with(aNonNull(String.class)));
            }
        });

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
    }

    @Test
    public void testMoreThanOneAuthMechDoesNotRegisterProvider() throws Exception {

        withNoCachedProvider().withCDI().doesNotRegisterProvider();
        mockery.checking(new Expectations() {
            {
                one(hami).isUnsatisfied();
                will(returnValue(false));
                one(hami).isAmbiguous();
                will(returnValue(true));
            }
        });

        bridgeBuilder.buildBridgeIfNeeded(APP_CONTEXT, providerFactory);
        // TODO: Assert serviceability message is issued.
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

    private BridgeBuilderImplTest withCDI() throws Exception {
        final List<Class> clist = new ArrayList<Class>();
        final Class dummy = String.class;
        clist.add(dummy);

        mockery.checking(new Expectations() {
            {
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(mppi));
                one(mppi).isUnsatisfied();
                will(returnValue(false));
                one(mppi).isAmbiguous();
                will(returnValue(false));
                one(cdi).select(HttpAuthenticationMechanism.class);
                will(returnValue(hami));
                one(hami).iterator();
                will(returnValue(itl));
                exactly(3).of(itl).hasNext();
                will(onConsecutiveCalls(returnValue(true), returnValue(true), returnValue(false)));
                allowing(itl).next();
                one(mppi).get();
                will(returnValue(mpp));
                one(mpp).getAuthMechClassList();
                will(returnValue(clist));
                one(cdi).select(dummy);
                will(returnValue(hami));
            }
        });
        return this;
    }

    private BridgeBuilderImplTest withCDInoHAM() throws Exception {
        final List<Class> clist = new ArrayList<Class>();

        mockery.checking(new Expectations() {
            {
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(mppi));
                one(mppi).isUnsatisfied();
                will(returnValue(false));
                one(mppi).isAmbiguous();
                will(returnValue(false));
                one(cdi).select(HttpAuthenticationMechanism.class);
                will(returnValue(hami));
                one(hami).iterator();
                will(returnValue(itl));
                exactly(3).of(itl).hasNext();
                will(onConsecutiveCalls(returnValue(true), returnValue(true), returnValue(false)));
                allowing(itl).next();
                one(mppi).get();
                will(returnValue(mpp));
                one(mpp).getAuthMechClassList();
                will(returnValue(clist));
            }
        });
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
        protected CDI getCDI() {
            return cdi;
        }
    }
}
