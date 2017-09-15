/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.Map;

import javax.annotation.Resource;
import javax.annotation.Resources;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfigurationProvider;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class ReferenceContextImplTest
{
    @Test
    public void testAdd()
                    throws Exception
    {
        TestHelper helper = new TestHelper()
                        .addEnvEntryValue("name", "value")
                        .addInjectionClass(TestAdd.class);

        ReferenceContext rc = helper.createReferenceContext();
        rc.process();

        InjectionBinding<?> binding = rc.getJavaColonCompEnvMap().get("name");
        Assert.assertEquals("value", binding.getInjectionObject());

        InjectionTarget[] targets = rc.getInjectionTargets(TestAdd.class);
        Assert.assertEquals(1, targets.length);
        Assert.assertEquals(TestAdd.class.getDeclaredField("field"), targets[0].getMember());

        Assert.assertEquals(0, rc.getInjectionTargets(Object.class).length);

        try
        {
            rc.add(helper);
            Assert.fail("expected IllegalStateException");
        } catch (IllegalStateException e)
        {
            // Expected.
        }
    }

    public static class TestAdd
    {
        @Resource(name = "name")
        String field;
    }

    @Test
    public void testProcessEmpty()
                    throws Exception
    {
        try
        {
            new TestInjectionEngineImpl().createReferenceContext().process();
            Assert.fail("expected IllegalStateException");
        } catch (IllegalStateException ex)
        {
            // Expected.
        }
    }

    private static TestInjectionEngineImpl.JavaNameSpace getComponentNameSpace(ReferenceContext rc)
    {
        return (TestInjectionEngineImpl.JavaNameSpace) rc.getComponentNameSpace();
    }

    @Test
    public void testJavaNameSpaceDefault()
                    throws Exception
    {
        ReferenceContext rc = new TestHelper()
                        .addInjectionClass(Object.class)
                        .createReferenceContext();
        rc.process();

        TestInjectionEngineImpl.JavaNameSpace jns = getComponentNameSpace(rc);
        Assert.assertNull(jns.ivLogicalAppName);
        Assert.assertEquals("testmod.jar", jns.ivModuleName);
        Assert.assertNull(jns.ivLogicalModuleName);
        Assert.assertEquals("test", jns.ivComponentName);
    }

    @Test
    public void testJavaNameSpaceLogical()
                    throws Exception
    {
        ReferenceContext rc = new TestHelper()
                        .setTestLogicalModuleName("logicalapp", "logicalmod")
                        .addInjectionClass(Object.class)
                        .createReferenceContext();
        rc.process();

        TestInjectionEngineImpl.JavaNameSpace jns = getComponentNameSpace(rc);
        Assert.assertEquals("logicalapp", jns.ivLogicalAppName);
        Assert.assertEquals("testmod.jar", jns.ivModuleName);
        Assert.assertEquals("logicalmod", jns.ivLogicalModuleName);
        Assert.assertEquals("test", jns.ivComponentName);
    }

    @Test
    public void testJavaNameSpaceLogicalStandalone()
                    throws Exception
    {
        ReferenceContext rc = new TestHelper()
                        .setTestLogicalModuleName(null, "logicalmod")
                        .addInjectionClass(Object.class)
                        .createReferenceContext();
        rc.process();

        TestInjectionEngineImpl.JavaNameSpace jns = getComponentNameSpace(rc);
        Assert.assertEquals("logicalmod", jns.ivLogicalAppName);
        Assert.assertEquals("testmod.jar", jns.ivModuleName);
        Assert.assertEquals("logicalmod", jns.ivLogicalModuleName);
        Assert.assertEquals("test", jns.ivComponentName);
    }

    @Test
    public void testJavaNameSpaceMultiple()
                    throws Exception
    {
        ReferenceContext rc = new TestInjectionEngineImpl().createReferenceContext();
        rc.add(new TestHelper("test1").setTestLogicalModuleName("logicalapp", "logicalmod"));
        rc.add(new TestHelper("test2").setTestLogicalModuleName("logicalapp", "logicalmod"));
        rc.process();

        TestInjectionEngineImpl.JavaNameSpace jns = getComponentNameSpace(rc);
        Assert.assertEquals("logicalapp", jns.ivLogicalAppName);
        Assert.assertEquals("testmod.jar", jns.ivModuleName);
        Assert.assertEquals("logicalmod", jns.ivLogicalModuleName);
        Assert.assertNull(jns.ivComponentName);
    }

    @Test
    public void testMetadataComplete()
                    throws Exception
    {
        new TestHelper()
                        .createReferenceContext()
                        .process();
    }

    @Test
    public void testAddProvider()
                    throws Exception
    {
        TestHelper helper = new TestHelper()
                        .addEnvEntryValue("name", "value")
                        .addInjectionClass(TestAdd.class);

        ReferenceContext rc = new TestInjectionEngineImpl().createReferenceContext();
        rc.add(helper.createComponentNameSpaceConfigurationProvider());
        rc.process();

        InjectionBinding<?> binding = rc.getJavaColonCompEnvMap().get("name");
        Assert.assertEquals("value", binding.getInjectionObject());

        InjectionTarget[] targets = rc.getInjectionTargets(TestAdd.class);
        Assert.assertEquals(1, targets.length);
        Assert.assertEquals(TestAdd.class.getDeclaredField("field"), targets[0].getMember());

        Assert.assertEquals(0, rc.getInjectionTargets(Object.class).length);

        try
        {
            rc.add(helper.createComponentNameSpaceConfigurationProvider());
            Assert.fail("expected IllegalStateException");
        } catch (IllegalStateException e)
        {
            // Pass.
        }
    }

    @Test
    public void testAddProviderFailTwice()
    {
        ReferenceContext rc = new TestInjectionEngineImpl().createReferenceContext();

        final int[] numGetCalls = new int[1];
        rc.add(new ComponentNameSpaceConfigurationProvider()
        {
            @Override
            public ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration()
                            throws InjectionException
            {
                numGetCalls[0]++;
                throw new InjectionException();
            }
        });

        for (int i = 0; i < 2; i++)
        {
            try
            {
                rc.process();
                Assert.fail("expected InjectionException");
            } catch (InjectionException ex)
            {
                // Expected.
            }

            Assert.assertEquals(1, numGetCalls[0]);
        }
    }

    @Test
    public void testAddProviderFailTwiceUnchecked() throws Exception {
        ReferenceContext rc = new TestInjectionEngineImpl().createReferenceContext();

        final int[] numGetCalls = new int[1];
        rc.add(new ComponentNameSpaceConfigurationProvider() {
            @Override
            public ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration() throws InjectionException {
                numGetCalls[0]++;
                throw new UncheckedException();
            }
        });

        for (int i = 0; i < 2; i++) {
            try {
                rc.process();
                Assert.fail("expected UncheckedException");
            } catch (UncheckedException ex) {
                // Expected.
            }
        }

        Assert.assertEquals(2, numGetCalls[0]);
    }

    @SuppressWarnings("serial")
    private static class UncheckedException extends RuntimeException {}

    private static InjectionTarget findInjectionTarget(InjectionTarget[] targets, Member member)
    {
        for (InjectionTarget target : targets)
        {
            if (target.getMember().equals(member))
            {
                return target;
            }
        }
        return null;
    }

    @Test
    public void testProcessDynamic()
                    throws Exception
    {
        TestHelper helper = new TestHelper()
                        .addEnvEntryValue("name", "value")
                        .addInjectionClass(TestProcessDynamicStatic.class);

        ReferenceContext rc = helper.createReferenceContext();
        rc.process();

        Map<String, InjectionBinding<?>> bindings = rc.getJavaColonCompEnvMap();
        Assert.assertEquals(1, bindings.size());
        Assert.assertEquals("name", bindings.keySet().iterator().next());

        // Ensure the CNSC is not used since the class is already processed.
        rc.processDynamic(new TestProcessDynamicHelper().addInjectionClass(TestProcessDynamicStatic.class));
        bindings = rc.getJavaColonCompEnvMap();
        Assert.assertEquals(1, bindings.size());
        Assert.assertEquals("name", bindings.keySet().iterator().next());

        for (int i = 0; i < 2; i++)
        {
            // On the second iteration, ensure the CNSC is not used since the
            // class has already been processed.
            TestHelper dynamicHelper = (i == 0 ? new TestHelper() : new TestProcessDynamicHelper())
                            .setClassLoader()
                            .addEnvEntry(new TestHelper.EnvEntryImpl("xml", null, "value", null,
                                            TestHelper.createInjectionTarget(TestProcessDynamicDynamic.class, "ivXMLField")))
                            .addInjectionClass(TestProcessDynamicDynamic.class);
            rc.processDynamic(dynamicHelper);

            // getJavaColonCompEnvMap should be unaffected.
            bindings = rc.getJavaColonCompEnvMap();
            Assert.assertEquals(1, bindings.size());
            Assert.assertEquals("name", bindings.keySet().iterator().next());

            // The targets should be updated.
            InjectionTarget[] targets = rc.getInjectionTargets(TestProcessDynamicDynamic.class);
            Assert.assertEquals(Arrays.toString(targets), 2, targets.length);
            Assert.assertNotNull(Arrays.toString(targets), findInjectionTarget(targets, TestProcessDynamicDynamic.class.getDeclaredField("ivAnnotationField")));
            Assert.assertNotNull(Arrays.toString(targets), findInjectionTarget(targets, TestProcessDynamicDynamic.class.getDeclaredField("ivXMLField")));
        }
    }

    static class TestProcessDynamicHelper
                    extends TestHelper
    {
        @Override
        public boolean isMetaDataComplete()
        {
            throw new IllegalStateException();
        }
    }

    public static class TestProcessDynamicStatic
    {
        @Resource(name = "name")
        String ivAnnotationField;
    }

    public static class TestProcessDynamicDynamic
    {
        @Resource(name = "name")
        String ivAnnotationField;
        String ivXMLField;
    }

    @Test
    public void testProcessDynamicConflict()
                    throws Exception
    {
        TestHelper helper = new TestHelper()
                        .addEnvEntry(new TestHelper.EnvEntryImpl("resolvedXML", String.class, "value", null))
                        .addEnvEntry(new TestHelper.EnvEntryImpl("unresolvedXML", String.class, null, null))
                        .addEnvEntryValue("resolvedClass", "value")
                        .addEnvEntryValue("resolvedField", "value")
                        .addInjectionClass(TestProcessDynamicConflictStatic.class);

        ReferenceContext rc = helper.createReferenceContext();
        rc.process();

        for (Class<?> klass : new Class<?>[] {
                                              TestProcessDynamicConflictWithResolvedXML.class,
                                              TestProcessDynamicConflictWithUnresolvedXML.class,
                                              TestProcessDynamicConflictWithResolvedClass.class,
                                              TestProcessDynamicConflictWithUnresolvedClass.class,
                                              TestProcessDynamicConflictWithResolvedField.class,
                                              TestProcessDynamicConflictWithUnresolvedField.class,
        })
        {
            try
            {
                rc.processDynamic(new TestProcessDynamicHelper().addInjectionClass(klass));
                Assert.fail();
            } catch (InjectionException ex)
            {
                // Expected
            }
        }
    }

    @Resources({
                @Resource(name = "resolvedClass", type = String.class),
                @Resource(name = "unresolvedClass", type = String.class)
    })
    public static class TestProcessDynamicConflictStatic
    {
        @Resource(name = "resolvedField")
        String ivResolvedField;
        @Resource(name = "unresolvedField")
        String ivUnresolvedField;
    }

    @Resource(name = "resolvedXML", type = Integer.class)
    public static class TestProcessDynamicConflictWithResolvedXML { /* empty */}

    @Resource(name = "unresolvedXML", type = Integer.class)
    public static class TestProcessDynamicConflictWithUnresolvedXML { /* empty */}

    @Resource(name = "resolvedClass", type = Integer.class)
    public static class TestProcessDynamicConflictWithResolvedClass { /* empty */}

    @Resource(name = "unresolvedClass", type = Integer.class)
    public static class TestProcessDynamicConflictWithUnresolvedClass { /* empty */}

    @Resource(name = "resolvedField", type = Integer.class)
    public static class TestProcessDynamicConflictWithResolvedField { /* empty */}

    @Resource(name = "unresolvedField", type = Integer.class)
    public static class TestProcessDynamicConflictWithUnresolvedField { /* empty */}
}
