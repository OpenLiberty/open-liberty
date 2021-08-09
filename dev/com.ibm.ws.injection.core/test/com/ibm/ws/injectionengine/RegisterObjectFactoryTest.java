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
package com.ibm.ws.injectionengine;

import java.lang.annotation.Annotation;
import java.util.Hashtable;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;
import com.ibm.wsspi.injectionengine.factory.ResourceInfo;
import com.ibm.wsspi.injectionengine.factory.ResourceInfoRefAddr;

public class RegisterObjectFactoryTest
{
    @Test
    public void testObjectFactory()
                    throws Exception
    {
        TestHelper helper = new TestHelper("test", new J2EENameImpl("testapp", "testmod.jar", "testcomp"));

        InjectionEngine engine = helper.createInjectionEngine();
        engine.registerObjectFactory(Resource.class,
                                     TestOverrideValue.class,
                                     TestOverrideValueObjectFactoryImpl.class,
                                     false);
        engine.registerObjectFactory(Resource.class,
                                     TestOverrideValue2.class,
                                     TestOverrideValue2ObjectFactoryImpl.class,
                                     true);

        TestOverride instance = new TestOverride();
        helper
                        .setClassLoader()
                        // These binding are ignored because allowOverride=false.
                        .addResourceRefBinding("noOverrideRef", "binding")
                        .addResourceRefBinding("noOverrideRefLookup", "binding")
                        .addIndirectJndiLookupValue("lookup", new TestOverrideValue(false))
                        .addResRefLookupValue("binding", new TestOverrideValue2(false))
                        .processAndInject(instance);
        Assert.assertNotNull(instance.ivNoOverride);
        Assert.assertTrue(instance.ivNoOverride.ivFactory);
        Assert.assertNotNull(instance.ivNoOverrideLookup);
        Assert.assertFalse(instance.ivNoOverrideLookup.ivFactory);
        Assert.assertNotNull(instance.ivOverride);
        Assert.assertTrue(instance.ivOverride.ivFactory);
        Assert.assertNotNull(instance.ivOverrideBinding);
        Assert.assertFalse(instance.ivOverrideBinding.ivFactory);
    }

    public static class TestOverride
    {
        @Resource(name = "noOverrideRef")
        TestOverrideValue ivNoOverride;
        @Resource(name = "noOverrideRefLookup", lookup = "lookup")
        TestOverrideValue ivNoOverrideLookup;

        @Resource(name = "overrideRef")
        TestOverrideValue2 ivOverride;
        @Resource(name = "overrideRefBinding", lookup = "binding")
        TestOverrideValue2 ivOverrideBinding;
    }

    public static class TestOverrideValue
    {
        final boolean ivFactory;

        public TestOverrideValue(boolean factory)
        {
            ivFactory = factory;
        }
    }

    public static class TestOverrideValue2
    {
        final boolean ivFactory;

        public TestOverrideValue2(boolean factory)
        {
            ivFactory = factory;
        }
    }

    public static class TestOverrideValueObjectFactoryImpl
                    implements ObjectFactory
    {
        String getName()
        {
            return "noOverrideRef";
        }

        Object getObjectInstance()
        {
            return new TestOverrideValue(true);
        }

        @Override
        public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt)
        {
            Reference ref = (Reference) o;
            ResourceInfoRefAddr refAddr = (ResourceInfoRefAddr) ref.get(0);
            ResourceInfo info = (ResourceInfo) refAddr.getContent();
            Object instance = getObjectInstance();

            Assert.assertEquals("testapp", info.getApplication());
            Assert.assertEquals("testmod.jar", info.getModule());
            // TODO - displayName passed instead of component
            //Assert.assertEquals("testcomp", info.getComponent());
            Assert.assertEquals(getName(), info.getName());
            Assert.assertEquals(AuthenticationType.CONTAINER, info.getAuthenticationType());
            Assert.assertTrue(info.isShareable());
            Assert.assertEquals(instance.getClass().getName(), info.getType());

            return instance;
        }
    }

    public static class TestOverrideValue2ObjectFactoryImpl
                    extends TestOverrideValueObjectFactoryImpl
    {
        @Override
        String getName()
        {
            return "overrideRef";
        }

        @Override
        Object getObjectInstance()
        {
            return new TestOverrideValue2(true);
        }
    }

    @Test
    public void testNoRefAddr()
                    throws Exception
    {
        TestHelper helper = new TestHelper();

        InjectionEngine engine = helper.createInjectionEngine();
        engine.registerObjectFactory(Resource.class,
                                     TestNoRefAddrValue.class,
                                     TestNoRefAddrValueObjectFactoryImpl.class,
                                     false,
                                     null,
                                     false);

        TestNoRefAddr instance = new TestNoRefAddr();
        helper
                        .processAndInject(instance);
        Assert.assertNotNull(instance.ivValue);
    }

    public static class TestNoRefAddr
    {
        @Resource
        TestNoRefAddrValue ivValue;
    }

    public static class TestNoRefAddrValue { /* empty */}

    public static class TestNoRefAddrValueObjectFactoryImpl
                    implements ObjectFactory
    {
        @Override
        public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt)
        {
            Reference ref = (Reference) o;
            Assert.assertEquals(0, ref.size());
            return new TestNoRefAddrValue();
        }
    }

    @Test
    public void testRegisterInfo()
                    throws Exception
    {
        TestHelper helper = new TestHelper();

        InjectionEngine engine = helper.createInjectionEngine();
        engine.registerObjectFactoryInfo(new TestRegisterInfoObjectFactoryInfoImpl());

        TestRegisterInfo instance = new TestRegisterInfo();
        helper
                        .processAndInject(instance);
        Assert.assertNotNull(instance.ivValue);
    }

    public static class TestRegisterInfo
    {
        @Resource
        TestRegisterInfoValue ivValue;
    }

    public static class TestRegisterInfoValue { /* empty */}

    public static class TestRegisterInfoObjectFactoryInfoImpl
                    extends ObjectFactoryInfo
    {
        @Override
        public Class<? extends Annotation> getAnnotationClass()
        {
            return Resource.class;
        }

        @Override
        public Class<? extends ObjectFactory> getObjectFactoryClass()
        {
            return TestRegisterInfoValueObjectFactoryImpl.class;
        }

        @Override
        public Class<?> getType()
        {
            return TestRegisterInfoValue.class;
        }

        @Override
        public boolean isOverrideAllowed()
        {
            return false;
        }
    }

    public static class TestRegisterInfoValueObjectFactoryImpl
                    implements ObjectFactory
    {
        @Override
        public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt)
        {
            return new TestRegisterInfoValue();
        }
    }
}
