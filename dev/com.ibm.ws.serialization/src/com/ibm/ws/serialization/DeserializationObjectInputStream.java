/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.JavaInfo;

/**
 * Constructs a class loader that delegates class loading to a specific class
 * loader rather than the caller class loader. This class is only useful for
 * deserializing objects with classes from the specified class loader only. For
 * objects with classes potentially owned by the runtime,
 * see {@link SerializationService}. When deserializing application objects, the
 * specified class loader is typically the thread context class loader.
 */
public class DeserializationObjectInputStream extends ObjectInputStream {
    private static final TraceComponent tc = Tr.register(DeserializationObjectInputStream.class);
    private static final Class<?> thisClass = DeserializationObjectInputStream.class;

    private final ClassLoader classLoader;

    // The PlatformClassloader. It is set when running with java 9 and above.
    private static final ClassLoader platformClassloader = getPlatformClassLoader();

    public DeserializationObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
        super(in);
        this.classLoader = classLoader;
    }

    /**
     * @return the result of {@link ClassLoader#loadClass} on the specified class loader
     */
    @FFDCIgnore(ClassNotFoundException.class)
    protected Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            // NOTE: If you're investigating a stack trace that shows a
            // ClassNotFoundException for an internal/WAS class via this method,
            // then you either need to read the comments in
            // DeserializationObjectInputStreamImpl.loadClass
            // (if it's also on the stack), or you need to use
            // SerializationService.createObjectInputStream.
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            if (name != null) {
                String retryName;
                if (name.startsWith("javax."))
                    retryName = "jakarta." + name.substring(6);
                else if (name.startsWith("jakarta."))
                    retryName = "javax." + name.substring(8);
                else
                    retryName = null;
                if (retryName != null)
                    try {
                        return classLoader.loadClass(retryName);
                    } catch (ClassNotFoundException x) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "unable to load " + retryName, x);
                    }
            }
            // Some JVMs have poor error handling for ClassNotFoundException in
            // ObjectInputStream, so add some extra trace.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unable to load " + name, e);
            }
            throw e;
        }
    }

    /**
     * Resolves array class names ("[B" or "[Ljava.lang.Object;"), delegating
     * to {@link #loadClass} to load non-array or non-primitive array classes.
     */
    protected Class<?> resolveClass(String name) throws ClassNotFoundException {
        // ClassLoader.loadClass is not guaranteed to support array classes:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4976356
        // ...so we must handle them ourselves.
        if (name.length() > 0 && name.charAt(0) == '[') {
            int numComponents = 0;
            do {
                numComponents++;
                if (numComponents == name.length()) {
                    throw new ClassNotFoundException(name);
                }
            } while (name.charAt(numComponents) == '[');

            if (name.charAt(numComponents) != 'L') {
                // Primitive array class expected.
                return resolveClassWithCL(name);
            }

            if (name.charAt(name.length() - 1) != ';') {
                // Unexpected erroneous class name.
                throw new ClassNotFoundException(name);
            }

            if (name.regionMatches(numComponents + 1, "java.", 0, 5)) {
                // Path for "java." classes.
                return resolveClassWithCL(name);
            }

            // Load the actual class, and then use that class' loader to load
            // the desired array class.
            String className = name.substring(numComponents + 1, name.length() - 1);
            Class<?> klass = loadClass(className);
            return Class.forName(name, false, getClassLoader(klass));
        }

        if (name.startsWith("java.")) {
            // Path for "java." classes.
            return resolveClassWithCL(name);
        }

        return loadClass(name);
    }

    /**
     * Resolves a class using the appropriate classloader.
     *
     * @param name The name of the class to resolve.
     * @return The resolved class.
     *
     * @throws ClassNotFoundException
     */
    private Class<?> resolveClassWithCL(String name) throws ClassNotFoundException {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            return Class.forName(name, false, getClassLoader(thisClass));
        }

        // The platform classloader is null if we failed to get it when using java 9, or if we are
        // running with a java level below 9. In those cases, the bootstrap classloader
        // is used to resolve the needed class.
        // Note that this change is being made to account for the fact that in java 9, classes
        // such as java.sql.* (java.sql module) are no longer discoverable through the bootstrap
        // classloader. Those classes are now discoverable through the java 9 platform classloader.
        // The platform classloader is between the bootstrap classloader and the app classloader.
        return Class.forName(name, false, platformClassloader);
    }

    /**
     * Delegates class loading to to {@link #resolveClass(String)} rather than
     * using {@code Class.forName}.
     *
     * <p>{@inheritDoc}
     */
    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    protected Class<?> resolveClass(ObjectStreamClass klass) throws ClassNotFoundException {
        String name = klass.getName();
        try {
            return resolveClass(name);
        } catch (ClassNotFoundException e) {
            if (name.indexOf('.') == -1) {
                // Manually-implemented Java 7 string switch.
                switch (name.hashCode()) {
                    case 0x3db6c28:
                        if (name.equals("boolean")) {
                            return boolean.class;
                        }
                        break;
                    case 0x2e6108:
                        if (name.equals("byte")) {
                            return byte.class;
                        }
                        break;
                    case 0x685847c:
                        if (name.equals("short")) {
                            return short.class;
                        }
                        break;
                    case 0x2e9356:
                        if (name.equals("char")) {
                            return char.class;
                        }
                        break;
                    case 0x197ef:
                        if (name.equals("int")) {
                            return int.class;
                        }
                        break;
                    case 0x5d0225c:
                        if (name.equals("float")) {
                            return float.class;
                        }
                        break;
                    case 0x32c67c:
                        if (name.equals("long")) {
                            return long.class;
                        }
                        break;
                    case 0xb0f77bd1:
                        if (name.equals("double")) {
                            return double.class;
                        }
                        break;
                }
            }

            throw e;
        }
    }

    /**
     * Delegates class loading to the specified class loader.
     *
     * <p>{@inheritDoc}
     */
    @Override
    protected Class<?> resolveProxyClass(String[] interfaceNames) throws ClassNotFoundException {
        ClassLoader proxyClassLoader = classLoader;
        Class<?>[] interfaces = new Class[interfaceNames.length];
        Class<?> nonPublicInterface = null;

        for (int i = 0; i < interfaceNames.length; i++) {
            Class<?> intf = loadClass(interfaceNames[i]);

            if (!Modifier.isPublic(intf.getModifiers())) {
                ClassLoader classLoader = getClassLoader(intf);
                if (nonPublicInterface != null) {
                    if (classLoader != proxyClassLoader) {
                        throw new IllegalAccessError(nonPublicInterface + " and " + intf + " both declared non-public in different class loaders");
                    }
                } else {
                    nonPublicInterface = intf;
                    proxyClassLoader = classLoader;
                }
            }

            interfaces[i] = intf;
        }

        try {
            return Proxy.getProxyClass(proxyClassLoader, interfaces);
        } catch (IllegalArgumentException ex) {
            throw new ClassNotFoundException(null, ex);
        }
    }

    private static ClassLoader getClassLoader(final Class<?> klass) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return klass.getClassLoader();
            }
        });
    }

    /**
     * Returns the PlatformClassloader when running with java 9 and above; otherwise returns null.
     */
    private static ClassLoader getPlatformClassLoader() {
        if (JavaInfo.majorVersion() >= 9) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    ClassLoader pcl = null;
                    try {
                        Method getPlatformClassLoader = ClassLoader.class.getMethod("getPlatformClassLoader");
                        pcl = (ClassLoader) getPlatformClassLoader.invoke(null);
                    } catch (Throwable t) {
                        // Log an FFDC.
                    }
                    return pcl;
                }
            });
        }
        return null;
    }
}
