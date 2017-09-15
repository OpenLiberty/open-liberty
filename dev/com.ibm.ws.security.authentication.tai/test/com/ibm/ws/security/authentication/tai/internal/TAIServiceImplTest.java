/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.tai.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

@SuppressWarnings("unchecked")
public class TAIServiceImplTest {
    private final String KEY_TRUST_ASSOCIATION = "trustAssociation";
    private final String KEY_INTERCEPTORS = "interceptors";

    static final String KEY_ID = "id";
    static final String KEY_CLASS_NAME = "className";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_INVOKE_BEFORE_SSO = "invokeBeforeSSO";
    static final String KEY_INVOKE_AFTER_SSO = "invokeAfterSSO";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    protected final AtomicServiceReference<ClassLoadingService> classLoadingServiceRef = mock.mock(AtomicServiceReference.class, "classLoadingRef");
    protected final ClassLoadingService classLoadingService = mock.mock(ClassLoadingService.class);

    protected final org.osgi.service.cm.Configuration config = mock.mock(org.osgi.service.cm.Configuration.class);
    private final BundleContext bundleContext = mock.mock(BundleContext.class);

    private final ComponentContext cc = mock.mock(ComponentContext.class);

    Map<String, Object> properties = new HashMap<String, Object>();

    private static SharedOutputManager outputMgr;

    private final AtomicServiceReference<TAIServiceImpl> taiServiceRef = new AtomicServiceReference<TAIServiceImpl>("taiService");

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(classLoadingServiceRef).getService();
                will(returnValue(classLoadingService));
            }
        });
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            TAIService taiService = new TAIServiceImpl();
            assertNotNull("There must be a TAI Service", taiService);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testInitialize_nothing() throws Exception {
        final String methodName = "testInitialize_nothing";
        try {

            final Map<String, Object> taiProps = new Hashtable<String, Object>();
            String[] noTAIs = {};
            taiProps.put(KEY_INTERCEPTORS, noTAIs);
            taiProps.put(TAIConfigImpl.KEY_INVOKE_FOR_UNPROTECTED_URI, false);
            taiProps.put(TAIConfigImpl.KEY_INVOKE_FOR_FORM_LOGIN, false);
            taiProps.put(TAIConfigImpl.KEY_FAIL_OVER_TO_APP_AUTH_TYPE, false);

            mock.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    will(returnValue(bundleContext));
                }
            });

            TAIServiceImpl taiService = new TAIServiceImpl();
            taiServiceRef.activate(cc);
            taiService.activate(cc, taiProps);

            assertFalse(taiService.isFailOverToAppAuthType());
            assertFalse(taiService.isInvokeForFormLogin());
            assertFalse(taiService.isInvokeForUnprotectedURI());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testActivateCreatesConfiguration() {
        final String methodName = "testActivateCreatesConfiguration";
        try {
            TAIServiceImpl taiService = new TAIServiceImpl();
            final Map<String, Object> myTAIProps = new Hashtable<String, Object>();
            String[] myTAI = { "myTAI" };
            myTAIProps.put(KEY_INTERCEPTORS, myTAI);
            myTAIProps.put(TAIConfigImpl.KEY_INVOKE_FOR_UNPROTECTED_URI, false);
            myTAIProps.put(TAIConfigImpl.KEY_INVOKE_FOR_FORM_LOGIN, false);
            myTAIProps.put(TAIConfigImpl.KEY_FAIL_OVER_TO_APP_AUTH_TYPE, false);

            final Dictionary<String, Object> taiProps = new Hashtable<String, Object>();
            taiProps.put(KEY_TRUST_ASSOCIATION, "trustAssociation");
            taiProps.put(KEY_ID, "id");

            taiProps.put(KEY_INTERCEPTORS, "interceptors");
            taiProps.put(KEY_ENABLED, true);
            taiProps.put(KEY_CLASS_NAME, "className");
            taiProps.put(KEY_INVOKE_BEFORE_SSO, true);
            taiProps.put(KEY_INVOKE_AFTER_SSO, true);

            mock.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    will(returnValue(bundleContext));

                    allowing(config).getProperties();
                    will(returnValue(taiProps));
                }
            });
            taiServiceRef.activate(cc);
            taiService.activate(cc, myTAIProps);

            Map<String, TrustAssociationInterceptor> taiInstances = taiService.getTais(true);
            assertNotNull("There must be TAI instance", taiInstances);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
