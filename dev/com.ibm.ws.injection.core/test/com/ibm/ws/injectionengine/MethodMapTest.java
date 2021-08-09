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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.injectionengine.pkg1.MethodMapTestClass1;
import com.ibm.ws.injectionengine.pkg1.MethodMapTestClass4;
import com.ibm.ws.injectionengine.pkg2.MethodMapTestClass2;
import com.ibm.ws.injectionengine.pkg2.MethodMapTestClass3;
import com.ibm.wsspi.injectionengine.MethodMap;

public class MethodMapTest
{
    @Test
    public void testSimple()
    {
        Assert.assertEquals(Collections.emptySet(), new HashSet<MethodMap.MethodInfo>(MethodMap.getAllDeclaredMethods(null)));
        Assert.assertEquals(Collections.emptySet(), new HashSet<MethodMap.MethodInfo>(MethodMap.getAllDeclaredMethods(Object.class)));
        Assert.assertEquals(Collections.emptySet(), new HashSet<MethodMap.MethodInfo>(MethodMap.getAllDeclaredMethods(Runnable.class)));
    }

    private static MethodMap.MethodInfo getMethodInfo(Collection<MethodMap.MethodInfo> methodInfos,
                                                      Class<?> declaringClass,
                                                      String name,
                                                      Class<?>[] params)
    {
        MethodMap.MethodInfo result = null;
        for (MethodMap.MethodInfo methodInfo : methodInfos)
        {
            Method method = methodInfo.getMethod();
            if (method.getName().equals(name) &&
                (declaringClass == null || method.getDeclaringClass() == declaringClass) &&
                (params == null || Arrays.equals(method.getParameterTypes(), params)))
            {
                Assert.assertNull(result + " + " + methodInfo, result);
                result = methodInfo;
            }
        }

        return result;
    }

    @Test
    public void testVisibility()
    {
        Collection<MethodMap.MethodInfo> methodInfos = MethodMap.getAllDeclaredMethods(TestVisibility.class);
        Assert.assertNotNull(getMethodInfo(methodInfos, null, "publicMethod", null));
        Assert.assertNotNull(getMethodInfo(methodInfos, null, "packageMethod", null));
        Assert.assertNotNull(getMethodInfo(methodInfos, null, "protectedMethod", null));
        Assert.assertNotNull(getMethodInfo(methodInfos, null, "privateMethod", null));
    }

    public static class TestVisibility
    {
        public void publicMethod() { /* empty */}

        void packageMethod() { /* empty */}

        protected void protectedMethod() { /* empty */}

        @SuppressWarnings("unused")
        private void privateMethod() { /* empty */}
    }

    @Test
    public void testOverride()
    {
        Collection<MethodMap.MethodInfo> methodInfos = MethodMap.getAllDeclaredMethods(TestOverride.class);
        getMethodInfo(methodInfos, TestOverride.class, "publicMethod", null);
        getMethodInfo(methodInfos, TestOverride.class, "packageMethod", null);
        getMethodInfo(methodInfos, TestOverride.class, "protectedMethod", null);
        getMethodInfo(methodInfos, TestOverride.class, "privateMethod", null);
        getMethodInfo(methodInfos, TestVisibility.class, "privateMethod", null);
    }

    public static class TestOverride
                    extends TestVisibility
    {
        @Override
        public void publicMethod() { /* empty */}

        @Override
        void packageMethod() { /* empty */}

        @Override
        protected void protectedMethod() { /* empty */}

        @SuppressWarnings("unused")
        private void privateMethod() { /* empty */}
    }

    @Test
    public void testDefaultAccessOverride()
    {
        //                                     pk1g.Class1  pkg2.Class2  pkg2.Class3  pkg1.Class4
        // pkg2NotOverridePkg1                 default      default
        // pkg1OverridePkg1                    default      default                   default
        // pkg1NotOverridePkg2OverridePkg2                  default      default      default
        // pkg1OverridePkg1AndPublicPkg2       default      public                    public
        // pkg1OverridePublicPkg2OverridePkg2               default      public       public

        {
            Collection<MethodMap.MethodInfo> methodInfos = MethodMap.getAllDeclaredMethods(MethodMapTestClass2.class);

            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg2NotOverridePkg1", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg2NotOverridePkg1", null));

            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1OverridePkg1", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1OverridePkg1", null));

            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1NotOverridePkg2OverridePkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1NotOverridePkg2OverridePkg2", null));

            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1OverridePkg1AndPublicPkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1OverridePkg1AndPublicPkg2", null));

            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1OverridePublicPkg2OverridePkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1OverridePublicPkg2OverridePkg2", null));
        }

        {
            Collection<MethodMap.MethodInfo> methodInfos = MethodMap.getAllDeclaredMethods(MethodMapTestClass3.class);

            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg2NotOverridePkg1", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg2NotOverridePkg1", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg2NotOverridePkg1", null));

            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1OverridePkg1", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1OverridePkg1", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg1OverridePkg1", null));

            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1NotOverridePkg2OverridePkg2", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1NotOverridePkg2OverridePkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg1NotOverridePkg2OverridePkg2", null));

            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1OverridePkg1AndPublicPkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1OverridePkg1AndPublicPkg2", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg1OverridePkg1AndPublicPkg2", null));

            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1OverridePublicPkg2OverridePkg2", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1OverridePublicPkg2OverridePkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg1OverridePublicPkg2OverridePkg2", null));
        }

        {
            Collection<MethodMap.MethodInfo> methodInfos = MethodMap.getAllDeclaredMethods(MethodMapTestClass4.class);

            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg2NotOverridePkg1", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg2NotOverridePkg1", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg2NotOverridePkg1", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass4.class, "pkg2NotOverridePkg1", null));

            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1OverridePkg1", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1OverridePkg1", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg1OverridePkg1", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass4.class, "pkg1OverridePkg1", null));

            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1NotOverridePkg2OverridePkg2", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1NotOverridePkg2OverridePkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg1NotOverridePkg2OverridePkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass4.class, "pkg1NotOverridePkg2OverridePkg2", null));

            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1OverridePkg1AndPublicPkg2", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1OverridePkg1AndPublicPkg2", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg1OverridePkg1AndPublicPkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass4.class, "pkg1OverridePkg1AndPublicPkg2", null));

            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass1.class, "pkg1OverridePublicPkg2OverridePkg2", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass2.class, "pkg1OverridePublicPkg2OverridePkg2", null));
            Assert.assertNull(getMethodInfo(methodInfos, MethodMapTestClass3.class, "pkg1OverridePublicPkg2OverridePkg2", null));
            Assert.assertNotNull(getMethodInfo(methodInfos, MethodMapTestClass4.class, "pkg1OverridePublicPkg2OverridePkg2", null));
        }
    }

    @Test
    public void testClassDepth()
    {
        Collection<MethodMap.MethodInfo> methodInfos = MethodMap.getAllDeclaredMethods(TestClassDepth.class);

        int classDepth = 0;
        for (MethodMap.MethodInfo methodInfo : methodInfos)
        {
            Assert.assertTrue(classDepth + " <= " + methodInfo.getClassDepth(), classDepth <= methodInfo.getClassDepth());
            classDepth = methodInfo.getClassDepth();
        }

        Assert.assertEquals(0, getMethodInfo(methodInfos, TestClassDepth.class, "subMethod", null).getClassDepth());
        Assert.assertEquals(0, getMethodInfo(methodInfos, TestClassDepth.class, "privateMethod", null).getClassDepth());
        Assert.assertEquals(0, getMethodInfo(methodInfos, TestClassDepth.class, "overrideMethod", null).getClassDepth());
        Assert.assertEquals(2, getMethodInfo(methodInfos, TestClassDepthSuperSuper.class, "superMethod", null).getClassDepth());
        Assert.assertEquals(2, getMethodInfo(methodInfos, TestClassDepthSuperSuper.class, "privateMethod", null).getClassDepth());
    }

    public static class TestClassDepth
                    extends TestClassDepthSuper
    {
        public void subMethod() { /* empty */}

        @SuppressWarnings("unused")
        private void privateMethod() { /* empty */}

        @Override
        public void overrideMethod() { /* empty */}
    }

    public static class TestClassDepthSuper
                    extends TestClassDepthSuperSuper { /* empty */}

    public static class TestClassDepthSuperSuper
    {
        public void superMethod() { /* empty */}

        @SuppressWarnings("unused")
        private void privateMethod() { /* empty */}

        public void overrideMethod() { /* empty */}
    }

    @Test
    public void testMethodInfo()
                    throws Exception
    {
        Collection<MethodMap.MethodInfo> methodInfos = MethodMap.getAllDeclaredMethods(TestMethodInfo.class);

        MethodMap.MethodInfo methodInfo0 = getMethodInfo(methodInfos, null, "method0", null);
        Assert.assertEquals(TestMethodInfo.class.getMethod("method0"), methodInfo0.getMethod());
        Assert.assertEquals(0, methodInfo0.getNumParameters());

        MethodMap.MethodInfo methodInfo1 = getMethodInfo(methodInfos, null, "method1", null);
        Assert.assertEquals(TestMethodInfo.class.getMethod("method1", boolean.class), methodInfo1.getMethod());
        Assert.assertEquals(1, methodInfo1.getNumParameters());
        Assert.assertEquals(boolean.class, methodInfo1.getParameterType(0));

        MethodMap.MethodInfo methodInfo2 = getMethodInfo(methodInfos, null, "method2", null);
        Assert.assertEquals(TestMethodInfo.class.getMethod("method2", Object.class, int.class), methodInfo2.getMethod());
        Assert.assertEquals(2, methodInfo2.getNumParameters());
        Assert.assertEquals(Object.class, methodInfo2.getParameterType(0));
        Assert.assertEquals(int.class, methodInfo2.getParameterType(1));
    }

    public static class TestMethodInfo
    {
        public void method0() { /* empty */}

        public void method1(boolean b) { /* empty */}

        public void method2(Object o, int x) { /* empty */}
    }
}
