/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Member;
import java.util.Arrays;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionSimpleBinding;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessor;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class InjectionTargetMapTest
{
    @Test
    public void testInterface()
                    throws Exception
    {
        new TestHelper()
                        .addInjectionClass(Runnable.class)
                        .process();
    }

    @Test
    public void testVisibility()
    {
        TestVisibility instance = new TestVisibility();
        new TestHelper()
                        .addEnvEntryValue(TestHelper.envName(TestVisibility.class, "ivPublicField"), "PublicField")
                        .addEnvEntryValue(TestHelper.envName(TestVisibility.class, "ivProtectedField"), "ProtectedField")
                        .addEnvEntryValue(TestHelper.envName(TestVisibility.class, "ivPackageField"), "PackageField")
                        .addEnvEntryValue(TestHelper.envName(TestVisibility.class, "ivPrivateField"), "PrivateField")
                        .addEnvEntryValue(TestHelper.envName(TestVisibility.class, "publicMethod"), "PublicMethod")
                        .addEnvEntryValue(TestHelper.envName(TestVisibility.class, "protectedMethod"), "ProtectedMethod")
                        .addEnvEntryValue(TestHelper.envName(TestVisibility.class, "packageMethod"), "PackageMethod")
                        .addEnvEntryValue(TestHelper.envName(TestVisibility.class, "privateMethod"), "PrivateMethod")
                        .processAndInject(instance);

        Assert.assertEquals("PublicField", instance.ivPublicField);
        Assert.assertEquals("ProtectedField", instance.ivProtectedField);
        Assert.assertEquals("PackageField", instance.ivPackageField);
        Assert.assertEquals("PrivateField", instance.getPrivateField());
        Assert.assertEquals("PublicMethod", instance.ivPublicMethod);
        Assert.assertEquals("ProtectedMethod", instance.ivProtectedMethod);
        Assert.assertEquals("PackageMethod", instance.ivPackageMethod);
        Assert.assertEquals("PrivateMethod", instance.ivPrivateMethod);
    }

    public static class TestVisibility
    {
        @Resource
        public String ivPublicField;
        @Resource
        protected String ivProtectedField;
        @Resource
        String ivPackageField;
        @Resource
        private String ivPrivateField;

        public String getPrivateField()
        {
            return ivPrivateField;
        }

        String ivPublicMethod;
        String ivProtectedMethod;
        String ivPackageMethod;
        String ivPrivateMethod;

        @Resource
        public void setPublicMethod(String value)
        {
            ivPublicMethod = value;
        }

        @Resource
        protected void setProtectedMethod(String value)
        {
            ivProtectedMethod = value;
        }

        @Resource
        void setPackageMethod(String value)
        {
            ivPackageMethod = value;
        }

        @Resource
        private void setPrivateMethod(String value)
        {
            ivPrivateMethod = value;
        }
    }

    /**
     * Ensure all fields are injected before all methods, regardless of
     * annotation type.
     */
    @Test
    public void testOrder()
                    throws Exception
    {
        TestHelper helper = new TestHelper();
        InjectionEngine ie = helper.createInjectionEngine();
        ie.registerInjectionProcessor(TestOrderAnnotation1Processor.class, TestOrderAnnotation1.class);
        ie.registerInjectionProcessor(TestOrderAnnotation2Processor.class, TestOrderAnnotation2.class);

        TestOrder instance = new TestOrder();
        helper.processAndInject(instance);

        instance.checkFields();
        Assert.assertNotNull(instance.ivMethod1);
        Assert.assertNotNull(instance.ivMethod2);
    }

    public static class TestOrder
    {
        @TestOrderAnnotation1
        String ivField1;
        @TestOrderAnnotation2
        String ivField2;

        String ivMethod1;
        String ivMethod2;

        void checkFields()
        {
            Assert.assertNotNull(ivField1);
            Assert.assertNotNull(ivField2);
        }

        @TestOrderAnnotation1
        void setMethod1(String value)
        {
            checkFields();
            ivMethod1 = value;
        }

        @TestOrderAnnotation2
        void setMethod2(String value)
        {
            checkFields();
            ivMethod2 = value;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestOrderAnnotation1 { /* empty */}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestOrderAnnotation2 { /* empty */}

    public static class TestOrderAnnotationProcessor<A extends Annotation>
                    extends InjectionProcessor<A, Annotation>
    {
        public TestOrderAnnotationProcessor(Class<A> annotationClass)
        {
            super(annotationClass, null);
        }

        @Override
        public void processXML() { /* empty */}

        @Override
        public String getJndiName(A annotation)
        {
            return "";
        }

        @Override
        public A[] getAnnotations(Annotation pluralAnnotation)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public InjectionBinding<A> createInjectionBinding(A annotation, Class<?> instanceClass, Member member, String jndiName)
        {
            return new InjectionSimpleBinding<A>(annotation, ivNameSpaceConfig);
        }

        @Override
        public void resolve(InjectionBinding<A> binding)
                        throws InjectionException
        {
            binding.setObjects(binding.getJndiName(), null);
        }
    }

    public static class TestOrderAnnotation1Processor
                    extends TestOrderAnnotationProcessor<TestOrderAnnotation1>
    {
        TestOrderAnnotation1Processor()
        {
            super(TestOrderAnnotation1.class);
        }
    }

    public static class TestOrderAnnotation2Processor
                    extends TestOrderAnnotationProcessor<TestOrderAnnotation2>
    {
        TestOrderAnnotation2Processor()
        {
            super(TestOrderAnnotation2.class);
        }
    }

    @Test
    public void testOverride()
    {
        TestOverride instance = new TestOverride();
        TestOverrideSuper superInstance = new TestOverrideSuper();

        new TestHelper()
                        .addEnvEntryValue(TestHelper.envName(TestOverride.class, "method"), "subMethod")
                        .addEnvEntryValue(TestHelper.envName(TestOverride.class, "private"), "subPrivate")
                        .addEnvEntryValue(TestHelper.envName(TestOverrideSuper.class, "method"), "superMethod")
                        .addEnvEntryValue(TestHelper.envName(TestOverrideSuper.class, "private"), "superPrivate")
                        .processAndInject(instance, superInstance);

        Assert.assertNotNull(instance.ivMethod);
        Assert.assertNotNull(instance.ivPrivate);
        Assert.assertNull(instance.ivSuperMethod);
        Assert.assertNotNull(instance.ivSuperPrivate);

        Assert.assertNotNull(superInstance.ivSuperMethod);
        Assert.assertNotNull(superInstance.ivSuperPrivate);
    }

    public class TestOverride
                    extends TestOverrideSuper
    {
        String ivMethod;
        String ivPrivate;

        @Resource
        @Override
        void setMethod(String value)
        {
            Assert.assertNull(ivMethod + ":" + value, ivMethod);
            ivMethod = value;
        }

        @Resource
        private void setPrivate(String value)
        {
            Assert.assertNull(ivPrivate + ":" + value, ivPrivate);
            ivPrivate = value;
        }
    }

    public class TestOverrideSuper
    {
        String ivSuperMethod;
        String ivSuperPrivate;

        @Resource
        void setMethod(String value)
        {
            Assert.assertNull(ivSuperMethod + ":" + value, ivSuperMethod);
            ivSuperMethod = value;
        }

        @Resource
        private void setPrivate(String value)
        {
            Assert.assertNull(ivSuperPrivate + ":" + value, ivSuperPrivate);
            ivSuperPrivate = value;
        }
    }

    @Test
    public void testInjectionSimpleBinding()
                    throws Exception
    {
        TestHelper helper = new TestHelper()
                        .addInjectionClass(TestInjectionSimpleBinding1.class)
                        .addInjectionClass(TestInjectionSimpleBinding2.class);
        TestInjectionEngineImpl engine = helper.createInjectionEngine();
        engine.registerInjectionProcessor(TestSimpleBindingProcessor.class, TestSimpleBindingAnnotation.class);
        helper.setCheckApplicationConfiguration(true);
        ReferenceContext rc = helper.createReferenceContext();
        rc.process();
        InjectionTarget[] targets = rc.getInjectionTargets(TestInjectionSimpleBinding1.class);
        Assert.assertEquals(Arrays.toString(targets), 1, targets.length);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestSimpleBindingAnnotation
    {
        // Nothing.
    }

    public static class TestSimpleBindingProcessor
                    extends InjectionSimpleProcessor<TestSimpleBindingAnnotation>
    {
        public TestSimpleBindingProcessor()
        {
            super(TestSimpleBindingAnnotation.class);
        }

        @Override
        public void resolve(InjectionBinding<TestSimpleBindingAnnotation> binding)
        {
            binding.setObjects(true, true);
        }
    }

    public class TestInjectionSimpleBindingSuper
    {
        @TestSimpleBindingAnnotation
        Object ivField;
    }

    public class TestInjectionSimpleBinding1
                    extends TestInjectionSimpleBindingSuper
    {
        // Nothing.
    }

    public class TestInjectionSimpleBinding2
                    extends TestInjectionSimpleBindingSuper
    {
        // Nothing.
    }
}
