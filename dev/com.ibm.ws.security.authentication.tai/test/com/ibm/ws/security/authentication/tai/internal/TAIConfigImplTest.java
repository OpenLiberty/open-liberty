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
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

import test.common.SharedOutputManager;

@SuppressWarnings("unchecked")
public class TAIConfigImplTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    protected final ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorConfigRef = new ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor>("interceptorService");

    Map<String, Object> properties = new HashMap<String, Object>();

    private TAIConfigImpl taiConfig = null;

    final Map<String, Object> interceptor = new Hashtable<String, Object>();

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
    public void testProcessTAIProps_defaults() throws Exception {
        final Map<String, Object> taiProps = new Hashtable<String, Object>();
        taiProps.put(TAIConfigImpl.KEY_INVOKE_FOR_UNPROTECTED_URI, false);
        taiProps.put(TAIConfigImpl.KEY_INVOKE_FOR_FORM_LOGIN, false);
        taiProps.put(TAIConfigImpl.KEY_FAIL_OVER_TO_APP_AUTH_TYPE, false);
        taiProps.put(TAIConfigImpl.KEY_DISABLE_LTPA_COOKIE, false);
        taiProps.put(TAIConfigImpl.KEY_INIT_AT_FIRST_REQUEST, false);

        taiConfig = new TAIConfigImpl(taiProps);
        assertFalse(taiConfig.isFailOverToAppAuthType());
        assertFalse(taiConfig.isInvokeForFormLogin());
        assertFalse(taiConfig.isInvokeForUnprotectedURI());
        assertFalse(taiConfig.isInitAtFirstRequest());
    }

    @Test
    public void testProcessTAIProps_true() throws Exception {
        final ServiceReference<TrustAssociationInterceptor> intRef = mock.mock(ServiceReference.class);
        final long id = 100;
        final long ranking = 100;
        mock.checking(new Expectations() {
            {
                mock.checking(new Expectations() {
                    {
                        allowing(intRef).getProperty("service.id");
                        will(returnValue(id));
                        allowing(intRef).getProperty("service.ranking");
                        will(returnValue(ranking));
                    }
                });
            }
        });
        interceptorConfigRef.putReference("key", intRef);
        final Map<String, Object> taiProps = new Hashtable<String, Object>();
        taiProps.put(TAIConfigImpl.KEY_INVOKE_FOR_UNPROTECTED_URI, true);
        taiProps.put(TAIConfigImpl.KEY_INVOKE_FOR_FORM_LOGIN, true);
        taiProps.put(TAIConfigImpl.KEY_FAIL_OVER_TO_APP_AUTH_TYPE, true);
        taiProps.put(TAIConfigImpl.KEY_DISABLE_LTPA_COOKIE, true);
        taiProps.put(TAIConfigImpl.KEY_INIT_AT_FIRST_REQUEST, true);

        taiConfig = new TAIConfigImpl(taiProps);
        assertTrue(taiConfig.isFailOverToAppAuthType());
        assertTrue(taiConfig.isInvokeForFormLogin());
        assertTrue(taiConfig.isInvokeForUnprotectedURI());
        assertTrue(taiConfig.isInitAtFirstRequest());
    }
}
