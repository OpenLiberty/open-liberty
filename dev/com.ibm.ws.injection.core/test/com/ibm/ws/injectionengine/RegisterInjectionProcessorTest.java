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
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.Resources;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;
import com.ibm.wsspi.injectionengine.InjectionSimpleBinding;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessor;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessorProvider;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.OverrideInjectionProcessor;

public class RegisterInjectionProcessorTest
{
    @Test
    public void testSimple()
                    throws Exception
    {
        TestHelper helper = new TestHelper();
        TestInjectionEngineImpl engine = helper.createInjectionEngine();
        engine.registerInjectionProcessor(TestSimpleAnnotationProcessor.class, TestSimpleAnnotation.class);

        TestSimple instance = new TestSimple();
        helper
                        .setJavaColonCompEnvMap()
                        .processAndInject(instance);

        for (String name : new String[] { "class", "plural1", "plural2", "field", "method" })
        {
            Assert.assertEquals(name, ((TestSimpleValue) helper.getJavaColonCompEnvMap().get(name).getInjectionObject()).ivName);
        }

        Assert.assertEquals("field", instance.ivField.ivName);
        Assert.assertEquals("method", instance.ivMethod.ivName);
        Assert.assertEquals("merged:merge", instance.ivMerge.ivName);
        Assert.assertEquals("merged:merge", instance.ivMerge2.ivName);
    }

    @TestSimpleAnnotation(name = "class")
    @TestSimpleAnnotations({
                            @TestSimpleAnnotation(name = "plural1"),
                            @TestSimpleAnnotation(name = "plural2")
    })
    public static class TestSimple
    {
        @TestSimpleAnnotation(name = "field")
        TestSimpleValue ivField;

        TestSimpleValue ivMethod;

        @TestSimpleAnnotation(name = "method")
        public void setValue(TestSimpleValue value)
        {
            ivMethod = value;
        }

        @TestSimpleAnnotation(name = "merge", merge = true)
        TestSimpleValue ivMerge;
        @TestSimpleAnnotation(name = "merge", merge = true)
        TestSimpleValue ivMerge2;
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
        public TestSimpleAnnotation[] getAnnotations(TestSimpleAnnotations annotation)
        {
            return annotation.value();
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
            TestSimpleAnnotationInjectionBinding saib = (TestSimpleAnnotationInjectionBinding) injectionBinding;
            String valueName = (saib.ivMerged ? "merged:" : "") + injectionBinding.getJndiName();
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

    @Test
    public void testOverride()
                    throws Exception
    {
        TestHelper helper = new TestHelper();
        helper.createInjectionEngine().registerInjectionProcessorProvider(new TestOverrideAnnotationProcessorProvider());

        TestOverride instance = new TestOverride();
        helper
                        .setJavaColonCompEnvMap()
                        .processAndInject(instance);

        for (String name : new String[] { "class", "plural", "field", "merge", "method" })
        {
            Assert.assertEquals(name, ((TestOverrideValue) helper.getJavaColonCompEnvMap().get(name).getInjectionObject()).ivName);
        }

        Assert.assertEquals("field", instance.ivField.ivName);
        Assert.assertEquals("merge", instance.ivMerge.ivName);
        Assert.assertEquals("merge", instance.ivMerge2.ivName);
        Assert.assertEquals("method", instance.ivMethod.ivName);
    }

    @Resource(name = "class")
    @Resources({
                @Resource(name = "plural")
    })
    public static class TestOverride
    {
        @Resource(name = "field")
        TestOverrideValue ivField;

        @Resource(name = "merge")
        TestOverrideValue ivMerge;
        @Resource(name = "merge")
        TestOverrideValue ivMerge2;

        TestOverrideValue ivMethod;

        @Resource(name = "method")
        public void setValue(TestOverrideValue value)
        {
            Assert.assertNull(ivMethod);
            ivMethod = value;
        }
    }

    public static class TestOverrideValue
    {
        final String ivName;

        TestOverrideValue(String name)
        {
            ivName = name;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestOverrideAnnotation
    {
        // Empty.
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestOverrideAnnotations
    {
        // Empty.
    }

    public static class TestOverrideAnnotationProcessorProvider
                    extends InjectionProcessorProvider<TestOverrideAnnotation, TestOverrideAnnotations>
    {
        @Override
        public Class<TestOverrideAnnotation> getAnnotationClass()
        {
            return TestOverrideAnnotation.class;
        }

        @Override
        public Class<TestOverrideAnnotations> getAnnotationsClass()
        {
            return null;
        }

        @Override
        public Class<? extends Annotation> getOverrideAnnotationClass()
        {
            return Resource.class;
        }

        @Override
        public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses()
        {
            return null;
        }

        @Override
        public InjectionProcessor<TestOverrideAnnotation, TestOverrideAnnotations> createInjectionProcessor()
        {
            return new TestOverrideAnnotationProcessor();
        }
    }

    public static class TestOverrideAnnotationProcessor
                    extends InjectionProcessor<TestOverrideAnnotation, TestOverrideAnnotations>
                    implements OverrideInjectionProcessor<TestOverrideAnnotation, Resource>
    {
        TestOverrideAnnotationProcessor()
        {
            super(TestOverrideAnnotation.class, TestOverrideAnnotations.class);
        }

        @Override
        public String getJndiName(TestOverrideAnnotation ann)
        {
            return "";
        }

        @Override
        public TestOverrideAnnotation[] getAnnotations(TestOverrideAnnotations anns)
        {
            return null;
        }

        @Override
        public void processXML()
        {
            // Nothing.
        }

        @Override
        public InjectionBinding<TestOverrideAnnotation> createInjectionBinding(TestOverrideAnnotation annotation,
                                                                               Class<?> instanceClass,
                                                                               Member member,
                                                                               String jndiName)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void resolve(InjectionBinding<TestOverrideAnnotation> injectionBinding)
                        throws InjectionException
        {
            int expectedMergeCount = injectionBinding.getJndiName().equals("merge") ? 1 : 0;
            Assert.assertEquals(expectedMergeCount, ((TestOverrideAnnotationInjectionBinding) injectionBinding).ivMergeCount);

            injectionBinding.setObjects(new TestOverrideValue(injectionBinding.getJndiName()), null);
        }

        @Override
        public InjectionBinding<TestOverrideAnnotation> createOverrideInjectionBinding(Class<?> instanceClass,
                                                                                       Member member,
                                                                                       Resource annotation,
                                                                                       String name)
        {
            Assert.assertEquals(TestOverride.class, instanceClass);

            if (name.equals("class"))
            {
                Assert.assertNull(member);
                Assert.assertEquals(annotation.name(), "class");
            }
            else if (name.equals("plural"))
            {
                Assert.assertNull(member);
                Assert.assertEquals(annotation.name(), "plural");
            }
            else if (name.equals("field"))
            {
                Assert.assertTrue(member.toString(), member instanceof Field);
            }
            else if (name.equals("merge"))
            {
                Assert.assertTrue(member.toString(), member instanceof Field);
                Assert.assertTrue(member.toString(), member.getName().equals("ivMerge") || member.getName().equals("ivMerge2"));
            }
            else if (name.equals("method"))
            {
                Assert.assertTrue(member.toString(), member instanceof Method);
                Assert.assertTrue(member.toString(), member.getName().equals("setValue"));
            }
            else
            {
                throw new IllegalArgumentException(name);
            }

            return new TestOverrideAnnotationInjectionBinding(ivNameSpaceConfig);
        }

        @Override
        public void mergeOverrideInjectionBinding(Class<?> instanceClass,
                                                  Member member,
                                                  Resource annotation,
                                                  InjectionBinding<TestOverrideAnnotation> injectionBinding)
                        throws InjectionException
        {
            Assert.assertEquals(TestOverride.class, instanceClass);
            Assert.assertTrue(member.toString(), member instanceof Field);
            Assert.assertTrue(member.toString(), member.getName().equals("ivMerge") || member.getName().equals("ivMerge2"));
            Assert.assertEquals("merge", annotation.name());
            Assert.assertEquals("merge", injectionBinding.getJndiName());

            ((TestOverrideAnnotationInjectionBinding) injectionBinding).ivMergeCount++;
        }
    }

    public static class TestOverrideAnnotationInjectionBinding
                    extends InjectionBinding<TestOverrideAnnotation>
    {
        int ivMergeCount;

        public TestOverrideAnnotationInjectionBinding(ComponentNameSpaceConfiguration compNSConfig)
        {
            super(null, compNSConfig);
        }

        @Override
        public void merge(TestOverrideAnnotation annotation, Class<?> instanceClass, Member member)
                        throws InjectionException
        {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testOverrideRegisterMethod()
                    throws Exception
    {
        try
        {
            new TestHelper().createInjectionEngine().registerInjectionProcessor(TestOverrideAnnotationProcessor.class, TestOverrideAnnotation.class);
            Assert.fail();
        } catch (IllegalArgumentException ex)
        {
            // Pass.
        }
    }

    @Test
    public void testNoBinding()
                    throws Exception
    {
        TestHelper helper = new TestHelper();
        TestInjectionEngineImpl engine = helper.createInjectionEngine();
        engine.registerInjectionProcessor(TestNoBindingAnnotationProcessor.class, TestNoBindingAnnotation.class);

        TestNoBinding instance = new TestNoBinding();
        helper
                        .setJavaColonCompEnvMap()
                        .processAndInject(instance);

        Assert.assertEquals(Collections.emptyMap(), helper.getJavaColonCompEnvMap());
        Assert.assertEquals(instance, instance.ivField.ivTarget);
    }

    public static class TestNoBinding
    {
        @TestNoBindingAnnotation
        TestNoBindingValue ivField;
    }

    public static class TestNoBindingValue
    {
        final Object ivTarget;

        TestNoBindingValue(Object target)
        {
            ivTarget = target;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestNoBindingAnnotation
    {
        // Nothing.
    }

    public static class TestNoBindingAnnotationProcessor
                    extends InjectionSimpleProcessor<TestNoBindingAnnotation>
    {
        public TestNoBindingAnnotationProcessor()
        {
            super(TestNoBindingAnnotation.class);
        }

        @Override
        public InjectionBinding<TestNoBindingAnnotation> createInjectionBinding(TestNoBindingAnnotation annotation,
                                                                                Class<?> instanceClass,
                                                                                Member member)
        {
            return new TestNoBindingInjectionBinding(annotation, ivNameSpaceConfig);
        }

        @Override
        public void resolve(InjectionBinding<TestNoBindingAnnotation> injectionBinding)
        {
            TestNoBindingInjectionBinding tnbInjectionBinding = (TestNoBindingInjectionBinding) injectionBinding;
            Member member = tnbInjectionBinding.getInjectionTarget().getMember();
            Assert.assertEquals(member.toString(), TestNoBinding.class, member.getDeclaringClass());
            Assert.assertEquals(member.toString(), "ivField", member.getName());
        }
    }

    public static class TestNoBindingInjectionBinding
                    extends InjectionSimpleBinding<TestNoBindingAnnotation>
    {
        public TestNoBindingInjectionBinding(TestNoBindingAnnotation annotation,
                                             ComponentNameSpaceConfiguration compNSConfig)
        {
            super(annotation, compNSConfig);
        }

        @Override
        public boolean isResolved()
        {
            return true;
        }

        @Override
        public Object getInjectionObjectInstance(Object target, InjectionTargetContext targetContext)
        {
            Assert.assertNotNull(target);
            return new TestNoBindingValue(target);
        }
    }

    @Test
    public void testRegisterProvider()
                    throws Exception
    {
        TestHelper helper = new TestHelper();

        TestInjectionEngineImpl engine = helper.createInjectionEngine();
        engine.registerInjectionProcessorProvider(new TestRegisterProviderInjectionProcessorProviderImpl());

        TestRegisterProvider instance = new TestRegisterProvider();
        helper.processAndInject(instance);

        Assert.assertEquals("field", instance.ivField.ivName);
    }

    public static class TestRegisterProvider
    {
        @TestRegisterProviderAnnotation(name = "field")
        TestRegisterProviderValue ivField;
    }

    public static class TestRegisterProviderValue
    {
        final String ivName;

        public TestRegisterProviderValue(String name)
        {
            ivName = name;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestRegisterProviderAnnotation
    {
        String name();
    }

    public static class TestRegisterProviderInjectionProcessorProviderImpl
                    extends InjectionSimpleProcessorProvider<TestRegisterProviderAnnotation>
    {
        @Override
        public Class<TestRegisterProviderAnnotation> getAnnotationClass()
        {
            return TestRegisterProviderAnnotation.class;
        }

        @Override
        public InjectionSimpleProcessor<TestRegisterProviderAnnotation> createInjectionProcessor()
        {
            return new TestRegisterProviderAnnotationProcessor();
        }
    }

    public static class TestRegisterProviderAnnotationProcessor
                    extends InjectionSimpleProcessor<TestRegisterProviderAnnotation>
    {
        public TestRegisterProviderAnnotationProcessor()
        {
            super(TestRegisterProviderAnnotation.class);
        }

        @Override
        public InjectionBinding<TestRegisterProviderAnnotation> createInjectionBinding(TestRegisterProviderAnnotation annotation,
                                                                                       Class<?> instanceClass,
                                                                                       Member member)
        {
            return new TestRegisterProviderAnnotationInjectionBinding(annotation, ivNameSpaceConfig);
        }

        @Override
        public void resolve(InjectionBinding<TestRegisterProviderAnnotation> binding)
                        throws InjectionException
        {
            binding.setObjects(new TestRegisterProviderValue(binding.getAnnotation().name()), null);
        }
    }

    public static class TestRegisterProviderAnnotationInjectionBinding
                    extends InjectionBinding<TestRegisterProviderAnnotation>
    {

        public TestRegisterProviderAnnotationInjectionBinding(TestRegisterProviderAnnotation annotation,
                                                              ComponentNameSpaceConfiguration compNSConfig)
        {
            super(annotation, compNSConfig);
        }

        @Override
        public void merge(TestRegisterProviderAnnotation annotation, Class<?> instanceClass, Member member) { /* empty */}
    }
}
