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
import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;

/**
 * Test that injection metadata can be dynamically updated.
 */
public class DynamicTest
{
    @Test
    public void testMerge()
    {
        TestMerge1 instance = new TestMerge1();
        TestHelper helper = new TestHelper()
                        .setClassLoader()
                        .setJavaColonCompEnvMap()
                        .addResourceRefBinding("name", "binding")
                        .addResRefLookupValue("binding", TestHelper.createProxyInstance(DataSource.class))
                        .processAndInject(instance);

        Map<String, InjectionBinding<?>> completedBindings = helper.getJavaColonCompEnvMap();

        TestMerge2 instance2 = new TestMerge2();
        helper = new TestHelper();
        helper.createInjectionEngine().getCompletedInjectionBindings().putAll(completedBindings);
        helper
                        .setClassLoader()
                        .addResourceRefBinding("name", "binding")
                        .addResRefLookupValue("binding", TestHelper.createProxyInstance(DataSource.class))
                        .processAndInject(instance2);
    }

    public static class TestMerge1
    {
        @Resource(name = "name")
        DataSource dataSource;
    }

    public static class TestMerge2
    {
        @Resource(name = "name")
        DataSource dataSource;
    }

    @Test
    public void testConflict()
                    throws Exception
    {
        TestHelper helper = new TestHelper()
                        .setClassLoader()
                        .setJavaColonCompEnvMap()
                        .addResourceRefBinding("name", "binding")
                        .addResRefLookupValue("binding", TestHelper.createProxyInstance(DataSource.class))
                        .addInjectionClass(TestConflict1.class)
                        .process();

        Map<String, InjectionBinding<?>> completedBindings = helper.getJavaColonCompEnvMap();

        helper = new TestHelper();
        helper.createInjectionEngine().getCompletedInjectionBindings().putAll(completedBindings);
        helper.addInjectionClass(TestConflict2.class);

        try
        {
            helper.process();
            Assert.fail("expected process failure");
        } catch (InjectionConfigurationException ex)
        {
            // Expected failure.
        }
    }

    public static class TestConflict1
    {
        @Resource(name = "name")
        DataSource dataSource;
    }

    public static class TestConflict2
    {
        @Resource(name = "name", authenticationType = Resource.AuthenticationType.APPLICATION)
        DataSource dataSource;
    }
}
