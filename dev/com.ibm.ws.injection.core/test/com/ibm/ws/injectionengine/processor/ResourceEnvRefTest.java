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
package com.ibm.ws.injectionengine.processor;

import java.util.Arrays;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.injectionengine.TestHelper;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;

public class ResourceEnvRefTest
{
    private static class ResourceEnvRefImpl
                    extends TestHelper.AbstractResourceGroup
                    implements ResourceEnvRef
    {
        private final Class<?> ivType;

        ResourceEnvRefImpl(String name,
                           Class<?> type,
                           String lookup,
                           InjectionTarget... injectionTargets)
        {
            super(name, lookup, Arrays.asList(injectionTargets));
            ivType = type;
        }

        @Override
        public String getTypeName()
        {
            return ivType == null ? null : ivType.getName();
        }
    }

    @Test
    public void testLookup()
    {
        TestLookup instance = new TestLookup();
        new TestHelper()
                        .setClassLoader()
                        .addIndirectJndiLookupValue("lookup", new TestLookupValue())
                        .addResourceEnvRef(new ResourceEnvRefImpl("resenvref", TestLookupValue.class, "lookup",
                                        TestHelper.createInjectionTarget(TestLookup.class, "ivValue")))
                        .processAndInject(instance);
        Assert.assertNotNull(instance.ivValue);
    }

    public static class TestLookup
    {
        TestLookupValue ivValue;
    }

    public static class TestLookupValue { /* empty */}

    @Test
    public void testBinding()
    {
        TestBinding instance = new TestBinding();
        new TestHelper()
                        .setClassLoader()
                        .addIndirectJndiLookupValue("binding", new TestBindingValue())
                        .addResourceEnvRefBinding("rer", "binding")
                        .processAndInject(instance);
        Assert.assertNotNull(instance.ivValue);
    }

    public static class TestBinding
    {
        @Resource(name = "rer")
        TestBindingValue ivValue;
    }

    public static class TestBindingValue { /* empty */}
}
