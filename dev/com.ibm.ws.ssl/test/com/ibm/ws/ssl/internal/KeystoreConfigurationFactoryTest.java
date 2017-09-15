/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.Hashtable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 *
 */
@SuppressWarnings("unchecked")
public class KeystoreConfigurationFactoryTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final BundleContext bc = mock.mock(BundleContext.class);
    private final ServiceReference<WsLocationAdmin> locSvcRef = mock.mock(ServiceReference.class);
    private final WsLocationAdmin locSvc = mock.mock(WsLocationAdmin.class);
    private final Dictionary props = new Hashtable();
    private KeystoreConfigurationFactory ksConfigFactory;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
                will(returnValue(bc));
                allowing(cc).locateService("locMgr", locSvcRef);
                will(returnValue(locSvc));
            }
        });
        ksConfigFactory = new KeystoreConfigurationFactory();
        ksConfigFactory.setLocMgr(locSvcRef);
        ksConfigFactory.activate(cc);
    }

    @After
    public void tearDown() {
        ksConfigFactory.unsetLocMgr(locSvcRef);
        ksConfigFactory.deactivate(cc, 0);

        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfigurationFactory#updated(java.lang.String, java.util.Dictionary)}.
     */
    @Test
    public void updated_noId() throws Exception {
        ksConfigFactory.updated("registeredPid", props);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfigurationFactory#updated(java.lang.String, java.util.Dictionary)}.
     */
    @Test
    public void updated() throws Exception {
        props.put("id", "myId");

        ksConfigFactory.updated("registeredPid", props);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfigurationFactory#deleted(java.lang.String)}.
     */
    @Test
    public void deleted_notRegistered() {
        ksConfigFactory.deleted("unregisteredPid");
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfigurationFactory#deleted(java.lang.String)}.
     */
    @Test
    public void deleted_registered() throws Exception {
        props.put("id", "myId");
        ksConfigFactory.updated("registeredPid", props);

        ksConfigFactory.deleted("registeredPid");
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfigurationFactory#getName()}.
     */
    @Test
    public void getName() {
        assertEquals("Did not recieve expected name",
                     "Keystore configuration", ksConfigFactory.getName());
    }

}
