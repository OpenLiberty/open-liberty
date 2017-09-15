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
package com.ibm.ws.ejbcontainer.jitdeploy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Test;

public class ClassDefinerTest
{

    public static class TestClass1
    {
        String text = "testing";
    }

    public static class TestClass2
    {
        int i = 0;
    }

    public static class TestSecurity implements Callable<Void>, PrivilegedAction<Void>
    {
        @Override
        public Void call()
        {
            AccessController.doPrivileged(this, new AccessControlContext(new ProtectionDomain[] { getClass().getProtectionDomain() }));
            return null;
        }

        @Override
        public Void run()
        {
            AccessController.checkPermission(new AllPermission());
            return null;
        }
    }

    /**
     * Tests if a valid class will be defined properly using ClassLoader,
     * CompoundClassLoader,
     */
    @Test
    public void testDefineValid() throws Exception
    {
        byte[] classBytes = readClassBytes(TestClass1.class);
        for (ClassDefiner definer : new ClassDefiner[] { new ClassDefiner() })
        {
            for (ClassLoader loader : new ClassLoader[] { new ClassLoader() {} })
            {
                Class<?> definedClass = definer.defineClass(loader, TestClass1.class.getName(), classBytes);
                Assert.assertNotNull(definedClass);
                Assert.assertSame("definer = " + definer + ", loader = " + loader, loader, definedClass.getClassLoader());
            }
        }
    }

    /**
     * Tests if a valid class will be found successfully
     */
    @Test
    public void testFindLoadedValid() throws Exception
    {
        ClassDefiner definer = new ClassDefiner();
        Class<?> expected = ClassDefiner.class;

        Assert.assertSame(expected, definer.findLoadedClass(expected.getClassLoader(), expected.getName()));
    }

    /**
     * Tests if a valid class will be found successfully after using defineClass
     * Note that this test findLoadedClass and defineClass NOT findLoadedOrDefineClass
     */
    @Test
    public void testDefineFindLoadedValid() throws Exception
    {
        byte[] classBytes = readClassBytes(TestClass1.class);
        for (ClassDefiner definer : new ClassDefiner[] { new ClassDefiner() })
        {
            for (ClassLoader loader : new ClassLoader[] { new ClassLoader() {} })
            {
                Class<?> definedClass = definer.defineClass(loader, TestClass1.class.getName(), classBytes);
                Assert.assertSame("definer = " + definer + ", loader = " + loader,
                                  definedClass, definer.findLoadedClass(loader, TestClass1.class.getName()));
            }
        }
    }

    /**
     * Tests if a valid class will be defined properly
     * Note that this tests findLoadedOrDefineClass method
     */
    @Test
    public void testFindOrDefineValid() throws Exception
    {
        byte[] classBytes = readClassBytes(TestClass1.class);
        for (ClassDefiner definer : new ClassDefiner[] { new ClassDefiner() })
        {
            for (ClassLoader loader : new ClassLoader[] { new ClassLoader() {} })
            {
                Class<?> definedClass = definer.defineClass(loader, TestClass1.class.getName(), classBytes);

                Assert.assertSame("definer = " + definer + ", loader = " + loader,
                                  definedClass, definer.findLoadedOrDefineClass(loader, TestClass1.class.getName(), classBytes));
            }
        }
    }

    /**
     * Tests if multiple classes will be found successfully after using defineClass
     */
    @Test
    public void testDefineFindLoadedMultipleValid() throws Exception
    {
        byte[] classBytes1 = readClassBytes(TestClass1.class);
        byte[] classBytes2 = readClassBytes(TestClass2.class);
        for (ClassDefiner definer : new ClassDefiner[] { new ClassDefiner() })
        {
            for (ClassLoader loader : new ClassLoader[] { new ClassLoader() {} })
            {
                Class<?> definedClass1 = definer.defineClass(loader, TestClass1.class.getName(), classBytes1);
                Class<?> definedClass2 = definer.defineClass(loader, TestClass2.class.getName(), classBytes2);

                Assert.assertNotNull(definedClass1);
                Assert.assertNotNull(definedClass2);
                Assert.assertNotSame("definer = " + definer + ", loader = " + loader, definedClass1, definedClass2);
            }
        }
    }

    /**
     * Tests if a class not in the loader is looked for, it will fail
     */
    @Test
    public void testFindLoadedInvalidClass()
    {
        for (ClassDefiner definer : new ClassDefiner[] { new ClassDefiner() })
        {
            for (ClassLoader loader : new ClassLoader[] { new ClassLoader() {} })
            {
                Assert.assertNull("definer = " + definer + ", loader = " + loader, definer.findLoadedClass(loader, "InvalidClass"));
            }
        }
    }

    /**
     * Tests if valid bytes were passed in as parameters into defineClass
     */
    @Test
    public void testDefineInvalidBytes() throws Exception
    {
        byte[] bytes = { 0x12, 0x34 };
        for (ClassDefiner definer : new ClassDefiner[] { new ClassDefiner() })
        {
            for (ClassLoader loader : new ClassLoader[] { new ClassLoader() {} })
            {
                try
                {
                    definer.defineClass(loader, TestClass1.class.getName(), bytes);
                    Assert.fail("definer = " + definer + ", loader = " + loader + " FAILED");
                } catch (ClassFormatError e)
                {
                    //Expected
                }
            }
        }
    }

    /**
     * Tests if two identical classes were defined within the same loader
     */
    @Test
    public void testDefineDoubleClasses() throws Exception
    {
        byte[] classBytes = readClassBytes(TestClass1.class);
        for (ClassDefiner definer : new ClassDefiner[] { new ClassDefiner() })
        {
            for (ClassLoader loader : new ClassLoader[] { new ClassLoader() {} })
            {
                definer.defineClass(loader, TestClass1.class.getName(), classBytes);

                try
                {
                    definer.defineClass(loader, TestClass1.class.getName(), classBytes);
                    Assert.fail("definer = " + definer + ", loader = " + loader + " FAILED");
                } catch (LinkageError e)
                {
                    //Expected
                }
            }
        }
    }

    /**
     * Tests if permissions are given correctly
     */
    @Test
    public void testSecurity() throws Exception
    {
        byte[] classBytes = readClassBytes(TestSecurity.class);
        for (ClassDefiner definer : new ClassDefiner[] { new ClassDefiner() })
        {
            for (ClassLoader loader : new ClassLoader[] { new ClassLoader() {} })
            {
                Class<?> definedClass = definer.defineClass(loader, TestSecurity.class.getName(), classBytes);
                Callable<?> callable = (Callable<?>) definedClass.newInstance();
                callable.call();
            }
        }
    }

    private static byte[] readClassBytes(Class<?> c)
                    throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        InputStream in = c.getClassLoader().getResourceAsStream(c.getName().replace('.', '/') + ".class");
        try
        {
            byte[] buf = new byte[4096];
            for (int read; (read = in.read(buf)) != -1;)
            {
                baos.write(buf, 0, read);
            }
        } finally
        {
            in.close();
        }

        return baos.toByteArray();
    }

}
