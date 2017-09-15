/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.ComponentContextMockery;

import com.ibm.ws.serialization.DeserializationClassProvider;
import com.ibm.ws.serialization.DeserializationObjectResolver;
import com.ibm.ws.serialization.SerializationObjectReplacer;

@RunWith(JMock.class)
public class SerializationServiceImplTest {
    private static final String REF_REPLACERS = "replacers";
    private static final String REF_RESOLVERS = "resolvers";
    private static final String REF_CLASS_PROVIDERS = "classProviders";

    private final Mockery mockery = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);

    @Test
    public void testIsReplaceObjectNeeded() {
        SerializationServiceImpl service = new SerializationServiceImpl();
        Assert.assertFalse(service.isReplaceObjectNeeded());

        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationObjectReplacer replacer = mockery.mock(SerializationObjectReplacer.class);
        ServiceReference<SerializationObjectReplacer> replacerSR = ccMockery.mockService(cc, REF_REPLACERS, replacer);

        service.addReplacer(replacerSR);
        Assert.assertTrue(service.isReplaceObjectNeeded());

        service.removeReplacer(replacerSR);
        Assert.assertFalse(service.isReplaceObjectNeeded());
    }

    @Test
    public void testReplaceObject() {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.activate(cc);
        Assert.assertEquals(0, service.replaceObject(0));

        final SerializationObjectReplacer replacer = mockery.mock(SerializationObjectReplacer.class);
        mockery.checking(new Expectations() {
            {
                allowing(replacer).replaceObject(0);
                will(returnValue(null));
                allowing(replacer).replaceObject(1);
                will(returnValue(2));
            }
        });
        ServiceReference<SerializationObjectReplacer> replacerSR = ccMockery.mockService(cc, REF_REPLACERS, replacer);
        service.addReplacer(replacerSR);
        Assert.assertEquals(0, service.replaceObject(0));
        Assert.assertEquals(2, service.replaceObject(1));
    }

    @Test
    public void testIsResolveObjectNeeded() {
        SerializationServiceImpl service = new SerializationServiceImpl();
        Assert.assertFalse(service.isResolveObjectNeeded());

        ComponentContext cc = mockery.mock(ComponentContext.class);
        DeserializationObjectResolver resolver = mockery.mock(DeserializationObjectResolver.class);
        ServiceReference<DeserializationObjectResolver> resolverSR = ccMockery.mockService(cc, REF_RESOLVERS, resolver);

        service.addResolver(resolverSR);
        Assert.assertTrue(service.isResolveObjectNeeded());

        service.removeResolver(resolverSR);
        Assert.assertFalse(service.isResolveObjectNeeded());
    }

    @Test
    public void testResolveObject() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.activate(cc);
        Assert.assertEquals(0, service.resolveObject(0));

        final DeserializationObjectResolver resolver = mockery.mock(DeserializationObjectResolver.class);
        mockery.checking(new Expectations() {
            {
                allowing(resolver).resolveObject(0);
                will(returnValue(null));
                allowing(resolver).resolveObject(1);
                will(returnValue(2));
            }
        });
        ServiceReference<DeserializationObjectResolver> resolverSR = ccMockery.mockService(cc, REF_RESOLVERS, resolver);
        service.addResolver(resolverSR);
        Assert.assertEquals(0, service.resolveObject(0));
        Assert.assertEquals(2, service.resolveObject(1));
    }

    @Test(expected = RuntimeException.class)
    public void testResolveObjectException() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.activate(cc);

        final DeserializationObjectResolver resolver = mockery.mock(DeserializationObjectResolver.class);
        mockery.checking(new Expectations() {
            {
                allowing(resolver).resolveObject(0);
                will(throwException(new IOException()));
            }
        });
        ServiceReference<DeserializationObjectResolver> resolverSR = ccMockery.mockService(cc, REF_RESOLVERS, resolver);
        service.addResolver(resolverSR);
        service.resolveObject(0);
    }

    @Test(expected = IOException.class)
    public void testResolveObjectWithException() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.activate(cc);

        final DeserializationObjectResolver resolver = mockery.mock(DeserializationObjectResolver.class);
        mockery.checking(new Expectations() {
            {
                allowing(resolver).resolveObject(0);
                will(throwException(new IOException()));
            }
        });
        ServiceReference<DeserializationObjectResolver> resolverSR = ccMockery.mockService(cc, REF_RESOLVERS, resolver);
        service.addResolver(resolverSR);
        service.resolveObjectWithException(0);
    }

    @Test(expected = IOException.class)
    public void testResolveObjectWithExceptionViaContext() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.activate(cc);

        final DeserializationObjectResolver resolver = mockery.mock(DeserializationObjectResolver.class);
        mockery.checking(new Expectations() {
            {
                allowing(resolver).resolveObject(0);
                will(throwException(new IOException()));
            }
        });
        ServiceReference<DeserializationObjectResolver> resolverSR = ccMockery.mockService(cc, REF_RESOLVERS, resolver);
        service.addResolver(resolverSR);

        DeserializationContextImpl context = new DeserializationContextImpl(service);
        Assert.assertTrue(context.isResolveObjectNeeded());
        context.resolveObject(0);
    }

    @Test
    public void testDeserializationClassProvider() {
        new DeserializationClassProvider();
    }

    private ServiceReference<DeserializationClassProvider> mockClassProvider(ComponentContext cc, final Object classesAttr, final Object packagesAttr, final Class<?>... classes) throws Exception {
        final ServiceReference<DeserializationClassProvider> sr = ccMockery.mockService(cc, REF_CLASS_PROVIDERS, null);
        final Bundle bundle = mockery.mock(Bundle.class);
        mockery.checking(new Expectations() {
            {
                allowing(sr).getProperty(DeserializationClassProvider.CLASSES_ATTRIBUTE);
                will(returnValue(classesAttr));
                allowing(sr).getProperty(DeserializationClassProvider.PACKAGES_ATTRIBUTE);
                will(returnValue(packagesAttr));
                allowing(sr).getBundle();
                will(returnValue(bundle));
                for (Class<?> klass : classes) {
                    allowing(bundle).loadClass(klass.getName());
                    will(returnValue(klass));
                }
                allowing(bundle).loadClass(with(any(String.class)));
                will(throwException(new ClassNotFoundException()));
            }
        });

        return sr;
    }

    @Test
    public void testLoadClassWithNone() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.activate(cc);
        Assert.assertEquals(null, service.loadClass(List.class.getName()));
        Assert.assertEquals(null, service.loadClass(ArrayList.class.getName()));
        service.deactivate(cc);
    }

    @Test
    public void testLoadClassWithClass() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        ServiceReference<DeserializationClassProvider> sr = mockClassProvider(cc, List.class.getName(), null, List.class);
        service.addClassProvider(sr);
        service.activate(cc);
        Assert.assertSame(List.class, service.loadClass(List.class.getName()));
        Assert.assertEquals(null, service.loadClass(ArrayList.class.getName()));
        service.deactivate(cc);
        service.removeClassProvider(sr);
    }

    @Test
    public void testLoadClassWithClassArray() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        ServiceReference<DeserializationClassProvider> sr =
                        mockClassProvider(cc, new String[] { List.class.getName(), ArrayList.class.getName() }, null, List.class, ArrayList.class);
        service.addClassProvider(sr);
        service.activate(cc);
        Assert.assertSame(List.class, service.loadClass(List.class.getName()));
        Assert.assertSame(ArrayList.class, service.loadClass(ArrayList.class.getName()));
        service.deactivate(cc);
        service.removeClassProvider(sr);
    }

    @Test
    public void testLoadClassWithInvalidClassType() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.addClassProvider(mockClassProvider(cc, true, null, List.class, ArrayList.class));
        service.activate(cc);
        Assert.assertEquals(null, service.loadClass(List.class.getName()));
        Assert.assertEquals(null, service.loadClass(ArrayList.class.getName()));
    }

    @Test(expected = ClassNotFoundException.class)
    public void testLoadClassWithMissingClass() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.addClassProvider(mockClassProvider(cc, List.class.getName(), null));
        service.activate(cc);
        service.loadClass(List.class.getName());
    }

    @Test
    public void testLoadClassWithPackage() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.addClassProvider(mockClassProvider(cc, null, List.class.getPackage().getName(), List.class));
        service.activate(cc);
        Assert.assertSame(List.class, service.loadClass(List.class.getName()));
    }

    @Test
    public void testLoadClassWithPackageArray() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.addClassProvider(mockClassProvider(cc, null, new String[] { List.class.getPackage().getName(), Runnable.class.getPackage().getName() }, List.class, Runnable.class));
        service.activate(cc);
        Assert.assertSame(List.class, service.loadClass(List.class.getName()));
        Assert.assertSame(Runnable.class, service.loadClass(Runnable.class.getName()));
    }

    @Test
    public void testLoadClassWithInvalidPackageType() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.addClassProvider(mockClassProvider(cc, null, true, List.class, ArrayList.class));
        service.activate(cc);
        Assert.assertEquals(null, service.loadClass(List.class.getName()));
        Assert.assertEquals(null, service.loadClass(ArrayList.class.getName()));
    }

    @Test(expected = ClassNotFoundException.class)
    public void testLoadClassWithMissingPackageClass() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.addClassProvider(mockClassProvider(cc, null, List.class.getPackage().getName()));
        service.activate(cc);
        service.loadClass(List.class.getName());
    }

    @Test
    public void testCreateStreams() throws Exception {
        SerializationServiceImpl service = new SerializationServiceImpl();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = service.createObjectOutputStream(baos);
        oos.writeObject(0);
        oos.close();
        ObjectInputStream ois = service.createObjectInputStream(new ByteArrayInputStream(baos.toByteArray()), getClass().getClassLoader());
        Assert.assertEquals(0, ois.readObject());
    }

    @Test
    public void testCreateContexts() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.activate(cc);
        final SerializationObjectReplacer replacer = mockery.mock(SerializationObjectReplacer.class);
        final DeserializationObjectResolver resolver = mockery.mock(DeserializationObjectResolver.class);
        mockery.checking(new Expectations() {
            {
                allowing(replacer).replaceObject(0);
                will(returnValue(1));

                allowing(resolver).resolveObject(1);
                will(returnValue(2));
            }
        });
        service.addReplacer(ccMockery.mockService(cc, REF_REPLACERS, replacer));
        service.addResolver(ccMockery.mockService(cc, REF_RESOLVERS, resolver));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = service.createSerializationContext().createObjectOutputStream(baos);
        oos.writeObject(0);
        oos.close();
        ObjectInputStream ois = service.createDeserializationContext().createObjectInputStream(new ByteArrayInputStream(baos.toByteArray()), getClass().getClassLoader());
        Assert.assertEquals(2, ois.readObject());
    }

    @Test
    public void testReplaceObjectForSerialization() throws Exception {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        SerializationServiceImpl service = new SerializationServiceImpl();
        service.activate(cc);
        final SerializationObjectReplacer replacer = mockery.mock(SerializationObjectReplacer.class);
        mockery.checking(new Expectations() {
            {
                allowing(replacer).replaceObject(0);
                will(returnValue(1));

                allowing(replacer).replaceObject(with(any(Object.class)));
                will(returnValue(null));
            }
        });
        service.addReplacer(ccMockery.mockService(cc, REF_REPLACERS, replacer));

        Object notSerializable = new Object();
        Assert.assertNull(service.replaceObjectForSerialization(notSerializable));

        @SuppressWarnings("serial")
        Object serializable = new Serializable() {};
        Assert.assertSame(serializable, service.replaceObjectForSerialization(serializable));

        Object externalizable = new Externalizable() {
            @Override
            public void writeExternal(ObjectOutput out) throws IOException {}

            @Override
            public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
        };
        Assert.assertSame(externalizable, service.replaceObjectForSerialization(externalizable));

        Assert.assertEquals(1, service.replaceObjectForSerialization(0));
    }
}
