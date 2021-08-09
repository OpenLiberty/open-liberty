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
package com.ibm.ws.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class DeserializationObjectInputStreamTest {
    private DeserializationObjectInputStream createResolveClassStream() throws IOException {
        return new DeserializationObjectInputStream(new ByteArrayInputStream(TestUtil.serialize(null)), getClass().getClassLoader()) {
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (!name.isEmpty() && name.charAt(0) == '[') {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name);
            }
        };
    }

    @Test
    public void testResolveClassPrimitiveArray() throws Exception {
        DeserializationObjectInputStream ois = createResolveClassStream();
        Assert.assertSame(boolean[].class, ois.resolveClass("[Z"));
        Assert.assertSame(boolean[][][].class, ois.resolveClass("[[[Z"));
        Assert.assertSame(byte[].class, ois.resolveClass("[B"));
        Assert.assertSame(short[].class, ois.resolveClass("[S"));
        Assert.assertSame(char[].class, ois.resolveClass("[C"));
        Assert.assertSame(int[].class, ois.resolveClass("[I"));
        Assert.assertSame(float[].class, ois.resolveClass("[F"));
        Assert.assertSame(long[].class, ois.resolveClass("[J"));
        Assert.assertSame(double[].class, ois.resolveClass("[D"));
    }

    @Test
    public void testResolveClass() throws Exception {
        DeserializationObjectInputStream ois = createResolveClassStream();
        Assert.assertSame(Object[].class, ois.resolveClass("[Ljava.lang.Object;"));
        Assert.assertSame(Object[][][].class, ois.resolveClass("[[[Ljava.lang.Object;"));
        Assert.assertSame(DeserializationObjectInputStream[].class, ois.resolveClass("[L" + DeserializationObjectInputStream.class.getName() + ";"));
    }

    @Test(expected = ClassNotFoundException.class)
    public void testResolveClassEmpty() throws Exception {
        createResolveClassStream().resolveClass("");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testResolveClassMissingArray() throws Exception {
        createResolveClassStream().resolveClass("[;");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testResolveClassMissingArrayTerminator() throws Exception {
        createResolveClassStream().resolveClass("[java.lang.Object");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testResolveClassTruncatedArrayClass() throws Exception {
        createResolveClassStream().resolveClass("[");
    }

    @Test
    public void testPrimitiveClasses() throws Exception {
        Class<?>[] primitiveClasses = { boolean.class, byte.class, short.class, char.class, int.class, float.class, long.class, double.class };
        byte[] bytes = TestUtil.serialize(primitiveClasses);

        ObjectInputStream ois = new DeserializationObjectInputStream(new ByteArrayInputStream(bytes), getClass().getClassLoader());
        Class<?>[] deserPrimitiveClasses = (Class<?>[]) ois.readObject();
        Assert.assertTrue(Arrays.equals(primitiveClasses, deserPrimitiveClasses));

        Class<?>[] primitiveArrayClasses = new Class<?>[primitiveClasses.length];
        for (int i = 0; i < primitiveClasses.length; i++) {
            primitiveArrayClasses[i] = Array.newInstance(primitiveClasses[i], 1).getClass();
        }
        ois = new DeserializationObjectInputStream(new ByteArrayInputStream(bytes), getClass().getClassLoader());
        Class<?>[] deserPrimitiveArrayClasses = (Class<?>[]) ois.readObject();
        Arrays.equals(primitiveArrayClasses, deserPrimitiveArrayClasses);
    }

    private Object testProxy(ClassLoader deserCL, Class<?>... interfaceClasses) throws IOException, ClassNotFoundException {
        Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), interfaceClasses, new TestInvocationHandlerImpl());
        byte[] bytes = TestUtil.serialize(proxy);

        ObjectInputStream ois = new DeserializationObjectInputStream(new ByteArrayInputStream(bytes), deserCL);
        return ois.readObject();
    }

    private Class<?> loadResourceClass(Class<?> klass) throws ClassNotFoundException {
        return new ResourceClassLoader(klass).loadClass(klass.getName());
    }

    @Test
    public void testPublicProxyClasses() throws Exception {
        testProxy(new ResourceClassLoader(TestIntf1.class, TestIntf2.class), TestIntf1.class, TestIntf2.class);
    }

    @Test
    public void testPublicProxyClassesMultipleLoaders() throws Exception {
        ClassLoader cl = new DelegatingClassLoader(loadResourceClass(TestIntf1.class), loadResourceClass(TestIntf2.class));
        testProxy(cl, TestIntf1.class, TestIntf2.class);
    }

    @Test
    public void testNonPublicProxyClass() throws Exception {
        testProxy(new ResourceClassLoader(TestPackageIntf1.class), TestPackageIntf1.class);
    }

    @Test
    public void testPublicAndNonPublicProxyClasses() throws Exception {
        ClassLoader cl = new ResourceClassLoader(new ResourceClassLoader(TestIntf1.class), TestPackageIntf1.class);
        testProxy(cl, TestIntf1.class, TestPackageIntf1.class);
    }

    @Test
    public void testNonPublicProxyClasses() throws Exception {
        ClassLoader cl = new ResourceClassLoader(TestPackageIntf1.class, TestPackageIntf2.class);
        testProxy(cl, TestPackageIntf1.class, TestPackageIntf2.class);
    }

    @Test(expected = IllegalAccessError.class)
    public void testNonPublicProxyClassesMultipleLoaders() throws Exception {
        ClassLoader cl = new DelegatingClassLoader(loadResourceClass(TestPackageIntf1.class), loadResourceClass(TestPackageIntf2.class));
        testProxy(cl, TestPackageIntf1.class, TestPackageIntf2.class);
    }

    /**
     * Verify that {@link ClassNotFoundException} is thrown when {@link Proxy#getProxyClass} throws {@link IllegalStateException}.
     */
    @Test(expected = ClassNotFoundException.class)
    public void testInvalidProxyClass() throws Exception {
        DelegatingClassLoader cl = new DelegatingClassLoader();
        cl.classes.put(TestIntf1.class.getName(), TestInvocationHandlerImpl.class);
        testProxy(cl, TestIntf1.class);
    }

    public static interface TestIntf1 {}

    public static interface TestIntf2 {}

    static interface TestPackageIntf1 {}

    static interface TestPackageIntf2 {}

    private static class TestInvocationHandlerImpl implements InvocationHandler, Serializable {
        private static final long serialVersionUID = 0;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    private static class DelegatingClassLoader extends ClassLoader {
        private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

        DelegatingClassLoader(Class<?>... classes) {
            super(null);
            this.classes.put(TestInvocationHandlerImpl.class.getName(), TestInvocationHandlerImpl.class);
            for (Class<?> klass : classes) {
                this.classes.put(klass.getName(), klass);
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean r) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (!name.startsWith("com.ibm.ws.")) {
                    return super.loadClass(name, r);
                }

                c = classes.get(name);
                if (c == null) {
                    c = super.loadClass(name, r);
                }
            }
            return c;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }
    }

    private static class ResourceClassLoader extends ClassLoader {
        private final Set<String> names = new HashSet<String>();

        ResourceClassLoader(ClassLoader parent, Class<?>... classes) {
            super(parent);
            this.names.add(TestInvocationHandlerImpl.class.getName());
            for (Class<?> klass : classes) {
                this.names.add(klass.getName());
            }
        }

        ResourceClassLoader(Class<?>... classes) {
            this(null, classes);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (!names.contains(name)) {
                    return super.loadClass(name, resolve);
                }

                InputStream in = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                try {
                    for (int read; (read = in.read(buf)) != -1;) {
                        baos.write(buf, 0, read);
                    }
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }

                byte[] bytes = baos.toByteArray();
                c = defineClass(name, bytes, 0, bytes.length);
            }

            return c;
        }
    }
}
