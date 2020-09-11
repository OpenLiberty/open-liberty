/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

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

import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

import test.common.SharedOutputManager;

@SuppressWarnings("unchecked")
public class TAIServiceImplTest {
    private final String KEY_TRUST_ASSOCIATION_ELEMENT = "trustAssociation";
    private final String KEY_INTERCEPTORS_ELEMENT = "interceptors";

    static final String KEY_ID = "id";
    static final String KEY_CLASS_NAME = "className";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_INVOKE_BEFORE_SSO = "invokeBeforeSSO";
    static final String KEY_INVOKE_AFTER_SSO = "invokeAfterSSO";
    static final String KEY_DISABLE_LTPA_COOKIE = "disableLtpaCookie";

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

            Map<String, Object> taiProps = createTrustAssociationPros(false);

            String[] noInterceptors = {};
            taiProps.put(KEY_INTERCEPTORS_ELEMENT, noInterceptors);

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
            assertFalse(taiService.isDisableLtpaCookie(KEY_ID));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testActivateCreatesConfiguration() {
        final String methodName = "testActivateCreatesConfiguration";
        try {
            TAIServiceImpl taiService = new TAIServiceImpl();
            Map<String, Object> myTAIProps = createTrustAssociationPros(false);
            String[] myTAI = { "myTAI" };
            myTAIProps.put(KEY_INTERCEPTORS_ELEMENT, myTAI);

            Dictionary<String, Object> interceptorProps = createInterceptorsProps(true);
            interceptorProps.put(KEY_ID, "id");
            myTAIProps.putAll((Map<? extends String, ? extends Object>) interceptorProps);

            mock.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    will(returnValue(bundleContext));

                    allowing(config).getProperties();
                    will(returnValue(interceptorProps));
                }
            });
            taiServiceRef.activate(cc);
            taiService.activate(cc, myTAIProps);

            Map<String, TrustAssociationInterceptor> taiInstances = taiService.getTais(true);
            assertNotNull("There must be TAI instance", taiInstances);
            assertTrue(taiService.isDisableLtpaCookie(KEY_ID));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * @param trustAssociation element configuration attributes
     */
    private Map<String, Object> createTrustAssociationPros(boolean value) {
        final Map<String, Object> taiProps = new Hashtable<String, Object>();
        taiProps.put(TAIConfigImpl.KEY_INVOKE_FOR_UNPROTECTED_URI, value);
        taiProps.put(TAIConfigImpl.KEY_INVOKE_FOR_FORM_LOGIN, value);
        taiProps.put(TAIConfigImpl.KEY_FAIL_OVER_TO_APP_AUTH_TYPE, value);
        taiProps.put(TAIConfigImpl.KEY_DISABLE_LTPA_COOKIE, value);
        taiProps.put(TAIConfigImpl.KEY_INIT_AT_FIRST_REQUEST, value);
        return taiProps;
    }

    /**
     * @param Configuration attributes for shared library interceptor element
     */
    private Dictionary<String, Object> createInterceptorsProps(boolean value) {
        final Dictionary<String, Object> interceptorProps = new Hashtable<String, Object>();
        interceptorProps.put(KEY_INTERCEPTORS_ELEMENT, "interceptors");
        interceptorProps.put(KEY_ENABLED, value);
        interceptorProps.put(KEY_CLASS_NAME, "className");
        interceptorProps.put(KEY_INVOKE_BEFORE_SSO, value);
        interceptorProps.put(KEY_INVOKE_AFTER_SSO, value);
        interceptorProps.put(KEY_DISABLE_LTPA_COOKIE, value);
        return interceptorProps;
    }
}
