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
package com.ibm.ws.injectionengine;

import java.util.Map;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;

public class SavedInjectionBindingTest
{
    @Test
    public void testSimple()
                    throws InjectionException
    {
        TestInjectionEngineImpl ie = new TestInjectionEngineImpl();
        ie.setSaveNonCompInjectionBindings(true);
        TestHelper helper = new TestHelper().addInjectionClass(TestSimple1.class);
        ie.processInjectionMetaData(helper.ivTargets, helper);

        Map<String, InjectionBinding<?>> globalBindings = ie.getSavedGlobalInjectionBindings().values().iterator().next();
        Assert.assertEquals(2, globalBindings.size());
        Assert.assertNotNull(globalBindings.get("java:global/env/name1"));
        Assert.assertNotNull(globalBindings.get("java:global/env/common"));

        Map<String, InjectionBinding<?>> appBindings = ie.getSavedAppInjectionBindings().values().iterator().next();
        Assert.assertEquals(2, appBindings.size());
        Assert.assertNotNull(appBindings.get("java:app/env/name1"));
        Assert.assertNotNull(appBindings.get("java:app/env/common"));

        Map<String, InjectionBinding<?>> moduleBindings = ie.getSavedModuleInjectionBindings().values().iterator().next();
        Assert.assertEquals(2, moduleBindings.size());
        Assert.assertNotNull(moduleBindings.get("java:module/env/name1"));
        Assert.assertNotNull(moduleBindings.get("java:module/env/common"));

        TestHelper helper2 = new TestHelper().addInjectionClass(TestSimple2.class);
        ie.processInjectionMetaData(helper2.ivTargets, helper2);

        Assert.assertEquals(3, globalBindings.size());
        Assert.assertNotNull(globalBindings.get("java:global/env/name1"));
        Assert.assertNotNull(globalBindings.get("java:global/env/name2"));
        Assert.assertNotNull(globalBindings.get("java:global/env/common"));

        Assert.assertEquals(3, appBindings.size());
        Assert.assertNotNull(appBindings.get("java:app/env/name1"));
        Assert.assertNotNull(appBindings.get("java:app/env/name2"));
        Assert.assertNotNull(appBindings.get("java:app/env/common"));

        Assert.assertEquals(3, moduleBindings.size());
        Assert.assertNotNull(moduleBindings.get("java:module/env/name1"));
        Assert.assertNotNull(moduleBindings.get("java:module/env/name2"));
        Assert.assertNotNull(moduleBindings.get("java:module/env/common"));
    }

    public class TestSimple1
    {
        @Resource(name = "java:global/env/name1")
        public String ivGlobal;
        @Resource(name = "java:global/env/common")
        public String ivGlobalCommon;

        @Resource(name = "java:app/env/name1")
        public String ivApp;
        @Resource(name = "java:app/env/common")
        public String ivAppCommon;

        @Resource(name = "java:module/env/name1")
        public String ivModule;
        @Resource(name = "java:module/env/common")
        public String ivModuleCommon;
    }

    public class TestSimple2
    {
        @Resource(name = "java:global/env/name2")
        public String ivGlobal;
        @Resource(name = "java:global/env/common")
        public String ivGlobalCommon;

        @Resource(name = "java:app/env/name2")
        public String ivApp;
        @Resource(name = "java:app/env/common")
        public String ivAppCommon;

        @Resource(name = "java:module/env/name2")
        public String ivModule;
        @Resource(name = "java:module/env/common")
        public String ivModuleCommon;
    }
}
