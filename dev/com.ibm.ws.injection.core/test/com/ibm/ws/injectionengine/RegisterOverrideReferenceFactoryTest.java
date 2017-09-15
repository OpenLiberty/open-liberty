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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Member;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.factory.OverrideReferenceFactory;

public class RegisterOverrideReferenceFactoryTest
{
    @Test
    public void testOverride()
                    throws Exception
    {
        TestHelper helper = new TestHelper("test", new J2EENameImpl("testapp", "testmod.jar", "testcomp"));
        InjectionEngine ie = helper.createInjectionEngine();
        ie.registerInjectionProcessor(TestSimpleAnnotationProcessor.class, TestSimpleAnnotation.class);
        ie.registerOverrideReferenceFactory(TestSimpleAnnotation.class, new TestOverrideReferenceFactoryImpl());

        TestOverride instance = new TestOverride();
        helper.setClassLoader().setJavaColonCompEnvMap().processAndInject(instance);
        Assert.assertNotNull(instance.ivInject);
        Assert.assertEquals("overridden", instance.ivInject.ivName);
    }

    public static class TestOverride
    {
        @TestSimpleAnnotation(name = "simpleName")
        TestSimpleValue ivInject;
    }

    static class TestOverrideReferenceFactoryImpl
                    implements OverrideReferenceFactory<TestSimpleAnnotation>
    {
        @Override
        public boolean hasModuleOverride(String applicationName, String moduleName)
        {
            return applicationName.equals("testapp") &&
                   moduleName.equals("testmod.jar");
        }

        @Override
        public Reference createReference(String application,
                                         String module,
                                         String component,
                                         String refName,
                                         Class<?> refType,
                                         String bindingName,
                                         TestSimpleAnnotation annotation)
        {
            Assert.assertEquals("testapp", application);
            Assert.assertEquals("testmod.jar", module);
            Assert.assertEquals("testcomp", component);
            Assert.assertEquals("simpleName", refName);
            Assert.assertEquals(TestSimpleValue.class, refType);
            Assert.assertNull(bindingName);
            return new Reference(refType.getName(), TestOverrideObjectFactory.class.getName(), null);
        }
    }

    public static class TestOverrideObjectFactory
                    implements ObjectFactory
    {
        @Override
        public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt)
        {
            return new TestSimpleValue("overridden");
        }
    }

    public static class TestSimpleValue
    {
        final String ivName;

        TestSimpleValue(String name)
        {
            ivName = name;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestSimpleAnnotation
    {
        String name();

        boolean merge() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestSimpleAnnotations
    {
        TestSimpleAnnotation[] value();
    }

    public static class TestSimpleAnnotationProcessor
                    extends InjectionProcessor<TestSimpleAnnotation, TestSimpleAnnotations>
    {
        public TestSimpleAnnotationProcessor()
        {
            super(TestSimpleAnnotation.class, TestSimpleAnnotations.class);
        }

        @Override
        public InjectionBinding<TestSimpleAnnotation> createInjectionBinding(TestSimpleAnnotation annotation,
                                                                             Class<?> instanceClass,
                                                                             Member member,
                                                                             String jndiName)
        {
            return new TestSimpleAnnotationInjectionBinding(annotation, ivNameSpaceConfig);
        }

        @Override
        public String getJndiName(TestSimpleAnnotation annotation)
        {
            return annotation.name();
        }

        @Override
        public TestSimpleAnnotation[] getAnnotations(TestSimpleAnnotations annotations)
        {
            return annotations.value();
        }

        @Override
        public void processXML()
        {
            // Nothing.
        }

        @Override
        public void resolve(InjectionBinding<TestSimpleAnnotation> injectionBinding)
                        throws InjectionException
        {
            String refJndiName = injectionBinding.getJndiName();
            Class<?> injectType = injectionBinding.getInjectionClassType();

            if (ivOverrideReferenceFactories != null && injectType != null)
            {
                J2EEName j2eeName = ivNameSpaceConfig.getJ2EEName();
                for (OverrideReferenceFactory<TestSimpleAnnotation> factory : ivOverrideReferenceFactories)
                {
                    Reference ref = factory.createReference(j2eeName.getApplication(),
                                                            j2eeName.getModule(),
                                                            j2eeName.getComponent(),
                                                            refJndiName,
                                                            injectType,
                                                            null,
                                                            injectionBinding.getAnnotation());
                    if (ref != null)
                    {
                        injectionBinding.setObjects(null, ref);
                        return;
                    }
                }
            }

            TestSimpleAnnotationInjectionBinding saib = (TestSimpleAnnotationInjectionBinding) injectionBinding;
            String valueName = (saib.ivMerged ? "merged:" : "") + refJndiName;
            injectionBinding.setObjects(new TestSimpleValue(valueName), null);
        }
    }

    public static class TestSimpleAnnotationInjectionBinding
                    extends InjectionBinding<TestSimpleAnnotation>
    {
        boolean ivMerged;

        TestSimpleAnnotationInjectionBinding(TestSimpleAnnotation annotation, ComponentNameSpaceConfiguration compNSConfig)
        {
            super(annotation, compNSConfig);
        }

        @Override
        public void merge(TestSimpleAnnotation annotation, Class<?> instanceClass, Member member)
                        throws InjectionException
        {
            ivMerged |= true;
        }
    }
}
