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

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;

public class JavaNameSpaceTest
{
    @Test
    public void testComp()
                    throws Exception
    {
        TestHelper helper = new TestHelper();
        TestInjectionEngineImpl ie = helper.createInjectionEngine();
        helper
                        .setTestResIndirectJndiLookupReferenceFactory()
                        .addInjectionClass(TestComp.class)
                        .process();

        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:comp/env/" + TestHelper.envName(TestComp.class, "ivDefaultName")));
        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:comp/env/basic"));
        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:comp/env/subcontext/basic"));
        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:comp/nonenv"));

        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:comp/env/" + TestHelper.envName(TestComp.class, "ivDefaultUnbound")));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:comp/env/basicUnbound"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:comp/env/subcontext/basicUnbound"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:comp/nonenvUnbound"));
    }

    public static class TestComp
    {
        @Resource(lookup = "lookup")
        public String ivDefaultName;
        @Resource
        public String ivDefaultUnbound;

        @Resource(name = "basic", lookup = "lookup")
        public String ivBasicName;
        @Resource(name = "basicUnbound")
        public String ivBasicUnbound;

        @Resource(name = "subcontext/basic", lookup = "lookup")
        public String ivSubcontextName;
        @Resource(name = "subcontext/basicUnbound")
        public String ivSubcontextUnbound;

        @Resource(name = "java:comp/nonenv", lookup = "value")
        public String ivNonenv;
        @Resource(name = "java:comp/nonenvUnbound")
        public String ivNonenvUnbound;
    }

    @Test
    public void testModuleUnbound()
                    throws Exception
    {
        TestHelper helper = new TestHelper();
        TestInjectionEngineImpl ie = helper.createInjectionEngine();
        helper
                        .setTestResIndirectJndiLookupReferenceFactory()
                        .addInjectionClass(TestModule.class)
                        .process();

        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:module/env/basic"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:module/env/subcontext/basic"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:module/nonenv"));
    }

    @Test
    public void testModule()
                    throws Exception
    {
        TestHelper helper = new TestHelper();
        TestInjectionEngineImpl ie = helper.createInjectionEngine();
        ie.setBindNonCompInjectionBindings(true);
        helper
                        .setTestResIndirectJndiLookupReferenceFactory()
                        .addInjectionClass(TestModule.class)
                        .process();

        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:module/env/basic"));
        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:module/env/subcontext/basic"));
        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:module/nonenv"));

        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:module/env/basicUnbound"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:module/env/subcontext/basicUnbound"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:module/nonenvUnbound"));
    }

    public static class TestModule
    {
        @Resource(name = "java:module/env/basic", lookup = "lookup")
        public String ivBasicName;
        @Resource(name = "java:module/env/basicUnbound")
        public String ivBasicUnbound;

        @Resource(name = "java:module/env/subcontext/basic", lookup = "lookup")
        public String ivSubcontextName;
        @Resource(name = "java:module/env/subcontext/basicUnbound")
        public String ivSubcontextUnbound;

        @Resource(name = "java:module/nonenv", lookup = "value")
        public String ivNonenv;
        @Resource(name = "java:module/nonenvUnbound")
        public String ivNonenvUnbound;
    }

    @Test
    public void testApp()
                    throws Exception
    {
        TestHelper helper = new TestHelper();
        TestInjectionEngineImpl ie = helper.createInjectionEngine();
        ie.setBindNonCompInjectionBindings(true);
        helper
                        .setTestResIndirectJndiLookupReferenceFactory()
                        .addInjectionClass(TestApp.class)
                        .process();

        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:app/env/basic"));
        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:app/env/subcontext/basic"));
        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:app/nonenv"));

        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:app/env/basicUnbound"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:app/env/subcontext/basicUnbound"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:app/nonenvUnbound"));
    }

    public static class TestApp
    {
        @Resource(name = "java:app/env/basic", lookup = "lookup")
        public String ivBasicName;
        @Resource(name = "java:app/env/basicUnbound")
        public String ivBasicUnbound;

        @Resource(name = "java:app/env/subcontext/basic", lookup = "lookup")
        public String ivSubcontextName;
        @Resource(name = "java:app/env/subcontext/basicUnbound")
        public String ivSubcontextUnbound;

        @Resource(name = "java:app/nonenv", lookup = "value")
        public String ivNonenv;
        @Resource(name = "java:app/nonenvUnbound")
        public String ivNonenvUnbound;
    }

    @Test
    public void testGlobal()
                    throws Exception
    {
        TestHelper helper = new TestHelper();
        TestInjectionEngineImpl ie = helper.createInjectionEngine();
        ie.setBindNonCompInjectionBindings(true);
        helper
                        .setTestResIndirectJndiLookupReferenceFactory()
                        .addInjectionClass(TestGlobal.class)
                        .process();

        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:global/env/basic"));
        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:global/env/subcontext/basic"));
        Assert.assertNotNull(ie.getJavaNameSpaceObjectBinding("java:global/nonenv"));

        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:global/env/basicUnbound"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:global/env/subcontext/basicUnbound"));
        Assert.assertNull(ie.getJavaNameSpaceObjectBinding("java:global/nonenvUnbound"));
    }

    public static class TestGlobal
    {
        @Resource(name = "java:global/env/basic", lookup = "lookup")
        public String ivBasicName;
        @Resource(name = "java:global/env/basicUnbound")
        public String ivBasicUnbound;

        @Resource(name = "java:global/env/subcontext/basic", lookup = "lookup")
        public String ivSubcontextName;
        @Resource(name = "java:global/env/subcontext/basicUnbound")
        public String ivSubcontextUnbound;

        @Resource(name = "java:global/nonenv", lookup = "value")
        public String ivNonenv;
        @Resource(name = "java:global/nonenvUnbound")
        public String ivNonenvUnbound;
    }
}
