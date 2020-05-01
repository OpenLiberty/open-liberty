/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import java.util.Dictionary;

import javax.naming.Context;
import javax.naming.Reference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class WSContextTest {

    private final Mockery mockery = new Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private int mockeryId;
    private final BundleContext bundleContext = mockery.mock(BundleContext.class);
    final WSContextFactory mWSCF = mockery.mock(WSContextFactory.class);

    private ServiceReference<?> mockServiceReference(final String objectClass,
                                                     final String origin,
                                                     final String explicitClassName,
                                                     final String refClassName) {
        final ServiceReference<?> ref = mockery.mock(ServiceReference.class, ServiceReference.class + " " + mockeryId++);

        mockery.checking(new Expectations() {
            {
                allowing(ref).getProperty(with(Constants.OBJECTCLASS));
                will(returnValue(new String[] { objectClass }));
                allowing(ref).getProperty(with(JNDIServiceBinder.OSGI_JNDI_SERVICE_ORIGIN));
                will(returnValue(origin));
                allowing(ref).getProperty(with(JNDIServiceBinder.OSGI_JNDI_SERVICE_CLASS));
                will(returnValue(explicitClassName));
                if (refClassName != null) {
                    allowing(mWSCF).getService(with(ref));
                    will(returnValue(new Reference(refClassName)));
                }
            }
        });
        return ref;
    }

    @Test
    public void testResolveObjectClassName() {
        WSContext c = new WSContext(bundleContext, null, null, mWSCF);
        final String CO = Object.class.getName();
        final String CC = Context.class.getName();
        final String CI = Integer.class.getName();
        final String CN = Number.class.getName();
        final String CR = Reference.class.getName();
        final String OV = JNDIServiceBinder.OSGI_JNDI_SERVICE_ORIGIN_VALUE;

        Assert.assertEquals(null, c.resolveObjectClassName(null));
        Assert.assertEquals(CI, c.resolveObjectClassName(0));
        Assert.assertEquals(CC, c.resolveObjectClassName(new ContextNode()));

        for (ServiceReferenceWrapper wrapper : ServiceReferenceWrapper.values()) {
            Assert.assertEquals(null, c.resolveObjectClassName(wrapper.wrap(mockServiceReference(CO, OV, null, null))));
            Assert.assertEquals(CN, c.resolveObjectClassName(wrapper.wrap(mockServiceReference(CO, OV, CN, null))));
            Assert.assertEquals(CI, c.resolveObjectClassName(wrapper.wrap(mockServiceReference(CI, null, null, null))));
            Assert.assertEquals(CN, c.resolveObjectClassName(wrapper.wrap(mockServiceReference(CI, null, CN, null))));
            Assert.assertEquals(CN, c.resolveObjectClassName(wrapper.wrap(mockServiceReference(CR, null, null, CN))));
        }
    }

    enum ServiceReferenceWrapper {
        ServiceReference {
            @Override
            public <T> Object wrap(ServiceReference<T> ref) {
                return ref;
            }
        },
        AutoBindNode {
            @Override
            public <T> Object wrap(ServiceReference<T> ref) {
                return new AutoBindNode(ref);
            }
        },
        ServiceRegistration {
            @Override
            public <T> Object wrap(final ServiceReference<T> ref) {
                return new ServiceRegistration<T>() {
                    @Override
                    public ServiceReference<T> getReference() {
                        return ref;
                    }

                    @Override
                    public void setProperties(Dictionary<String, ?> properties) {
                    }

                    @Override
                    public void unregister() {
                    }
                };
            }
        };

        public abstract <T> Object wrap(ServiceReference<T> ref);
    }
}
