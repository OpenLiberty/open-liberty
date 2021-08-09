/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.ejb.NamedMethod;

public class DDUtilTest
{
    private static List<String> stringList(String... strings)
    {
        return Arrays.asList(strings);
    }

    private static Class<?>[] classes(Class<?>... classes)
    {
        return classes;
    }

    private static NamedMethod namedMethod(final String name, final String... paramTypeNames)
    {
        return new NamedMethod()
        {
            @Override
            public String getMethodName()
            {
                return name;
            }

            @Override
            public List<String> getMethodParamList()
            {
                return paramTypeNames == null ? null : stringList(paramTypeNames);
            }
        };
    }

    @Test
    public void testMethodParamsMatch()
    {
        Assert.assertTrue(DDUtil.methodParamsMatch(stringList("boolean"), classes(boolean.class)));
        Assert.assertTrue(DDUtil.methodParamsMatch(stringList("boolean[]"), classes(boolean[].class)));
        Assert.assertTrue(DDUtil.methodParamsMatch(stringList("boolean[][]"), classes(boolean[][].class)));
        Assert.assertTrue(DDUtil.methodParamsMatch(stringList("java.lang.String"), classes(String.class)));
        Assert.assertTrue(DDUtil.methodParamsMatch(stringList("java.lang.String[]"), classes(String[].class)));
        Assert.assertTrue(DDUtil.methodParamsMatch(stringList("java.lang.String[][]"), classes(String[][].class)));
        Assert.assertTrue(DDUtil.methodParamsMatch(stringList("boolean", "java.lang.String"), classes(boolean.class, String.class)));
    }

    @Test
    public void testFindMethod()
                    throws Exception
    {
        Method[] methods = TestFindMethod.class.getMethods();

        Assert.assertNull(DDUtil.findMethod(namedMethod("equals", (String[]) null), methods));
        Assert.assertEquals(TestFindMethod.class.getMethod("equals", Object.class),
                            DDUtil.findMethod(namedMethod("equals", "java.lang.Object"), methods));
        Assert.assertEquals(TestFindMethod.class.getMethod("method"),
                            DDUtil.findMethod(namedMethod("method", (String[]) null), methods));
        Assert.assertEquals(TestFindMethod.class.getMethod("method", boolean.class),
                            DDUtil.findMethod(namedMethod("method", "boolean"), methods));
        Assert.assertEquals(TestFindMethod.class.getMethod("method", boolean[].class),
                            DDUtil.findMethod(namedMethod("method", "boolean[]"), methods));
        Assert.assertEquals(TestFindMethod.class.getMethod("method", boolean[][].class),
                            DDUtil.findMethod(namedMethod("method", "boolean[][]"), methods));
        Assert.assertEquals(TestFindMethod.class.getMethod("method", String.class),
                            DDUtil.findMethod(namedMethod("method", "java.lang.String"), methods));
        Assert.assertEquals(TestFindMethod.class.getMethod("method", String[].class),
                            DDUtil.findMethod(namedMethod("method", "java.lang.String[]"), methods));
        Assert.assertEquals(TestFindMethod.class.getMethod("method", String[][].class),
                            DDUtil.findMethod(namedMethod("method", "java.lang.String[][]"), methods));
        Assert.assertEquals(TestFindMethod.class.getMethod("method", boolean.class, String.class),
                            DDUtil.findMethod(namedMethod("method", "boolean", "java.lang.String"), methods));
    }

    public static class TestFindMethod
    {
        public void method() {}

        public void method(boolean a) {}

        public void method(boolean[] a) {}

        public void method(boolean[][] a) {}

        public void method(String a) {}

        public void method(String[] a) {}

        public void method(String[][] a) {}

        public void method(boolean a, String b) {}
    }
}
