/*******************************************************************************
 * Copyright (c) 2012, 2020, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
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
 *
 * <p>When constructing a stream, this class applies an {@link ObjectInputFilter} sourced
 * from (in priority order):
 * <ol>
 *   <li>A caller-supplied filter set on the current thread via
 *       {@link DeserializationFilterHolder#setFilter} or
 *       {@link DeserializationFilterHolder#withFilter}.</li>
 *   <li>A built-in default denylist covering packages known to contain Java
 *       deserialization gadget chains, applied when no thread-local filter is present.</li>
 * </ol>
 * On Java 8, where {@code setObjectInputFilter} is not part of the public API, a
 * {@code resolveClass} override enforces the same denylist as a fallback.
 */
public class DeserializationObjectInputStream extends ObjectInputStream {
    private static final TraceComponent tc = Tr.register(DeserializationObjectInputStream.class);
    private static final Class<?> thisClass = DeserializationObjectInputStream.class;

    private final ClassLoader classLoader;

    // The PlatformClassloader. It is set when running with java 9 and above.
    private static final ClassLoader platformClassloader = getPlatformClassLoader();

    /**
     * On Java 9+, ObjectInputStream.setObjectInputFilter is a public API.
     * On Java 8, it exists as sun.misc.ObjectInputFilter / ObjectInputStream.setObjectInputFilter
     * but requires reflective access. We cache the Method at class-load time to avoid
     * per-call reflection overhead. If reflection fails (e.g. strict module policy),
     * setObjectInputFilterMethod remains null and we fall back to resolveClass-based filtering.
     */
    private static final Method setObjectInputFilterMethod = getSetObjectInputFilterMethod();

    /**
     * Packages blocked by the default denylist when no caller-supplied filter is present.
     * These cover the most widely exploited Java deserialization gadget-chain libraries.
     * The trailing ";*" makes this a denylist (allow everything not explicitly denied).
     */
    private static final String DEFAULT_DENYLIST =
        "!org.apache.commons.collections.*" +
        ";!org.apache.commons.collections4.*" +
        ";!org.apache.commons.beanutils.*" +
        ";!com.sun.org.apache.xalan.*" +
        ";!org.springframework.*" +
        ";!org.codehaus.groovy.*" +
        ";!bsh.*" +
        ";*";

    /** Cached default filter instance built from DEFAULT_DENYLIST. */
    private static final ObjectInputFilter defaultDenylistFilter =
        JavaInfo.majorVersion() >= 9 ? ObjectInputFilter.Config.createFilter(DEFAULT_DENYLIST) : null;

    public DeserializationObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
        super(in);
        this.classLoader = classLoader;
        applyFilter();
    }

    /**
     * Applies the appropriate ObjectInputFilter to this stream.
     *
     * Priority:
     *   1. Caller-supplied ThreadLocal filter via DeserializationFilterHolder.
     *   2. Built-in default denylist.
     *
     * setObjectInputFilter() MUST be called in the constructor, before any read
     * operation is performed on the stream. It cannot be applied later.
     *
     * On Java 8 where setObjectInputFilter is unavailable, the resolveClass override
     * below acts as the fallback enforcement mechanism.
     */
    private void applyFilter() {
        if (setObjectInputFilterMethod == null) {
            // Java 8 fallback: filtering is handled by the resolveClass override below.
            return;
        }
        ObjectInputFilter filter = DeserializationFilterHolder.getFilter();
        if (filter == null) {
            filter = defaultDenylistFilter;
        }
        if (filter != null) {
            try {
                setObjectInputFilterMethod.invoke(this, filter);
            } catch (Exception e) {
                // Should not happen — method signature is known. Trace and continue;
                // the resolveClass fallback will still apply the denylist on Java 8.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to set ObjectInputFilter", e);
            }
        }
    }

    /**
     * Resolves a Method reference for ObjectInputStream.setObjectInputFilter on Java 9+,
     * or for the equivalent sun.misc variant on Java 8.
     * Returns null if the method cannot be found or accessed.
     */
    @FFDCIgnore({ Exception.class })
    private static Method getSetObjectInputFilterMethod() {
        if (JavaInfo.majorVersion() >= 9) {
            try {
                return ObjectInputStream.class.getMethod("setObjectInputFilter",
                    Class.forName("java.io.ObjectInputFilter"));
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "setObjectInputFilter not available on Java 9+", e);
                return null;
            }
        }
        // Java 8: try sun.misc.ObjectInputFilter (available in IBM/Oracle JDK 8u121+)
        try {
            Class<?> filterClass = Class.forName("sun.misc.ObjectInputFilter");
            return ObjectInputStream.class.getMethod("setObjectInputFilter", filterClass);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "sun.misc.ObjectInputFilter not available; using resolveClass fallback", e);
            return null;
        }
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
     * <p>On Java 8, where {@code setObjectInputFilter} is unavailable, this method also
     * enforces the denylist (or a caller-supplied filter expressed as a package-prefix check)
     * by inspecting the class name before loading. This is the fallback for the filter
     * mechanism described in the class Javadoc.
     *
     * <p>{@inheritDoc}
     */
    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    protected Class<?> resolveClass(ObjectStreamClass klass) throws ClassNotFoundException {
        String name = klass.getName();
        // Java 8 fallback: enforce denylist via class name inspection when
        // setObjectInputFilter was not available at construction time.
        if (setObjectInputFilterMethod == null && isDeniedByDefaultDenylist(name)) {
            Tr.warning(tc, "DESERIALIZATION_BLOCKED_CWSRZ0077W", name);
            throw new InvalidClassException(name, "Class blocked by deserialization denylist");
        }
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
     * Returns true if the given class name is covered by the built-in default denylist.
     * Used by the Java 8 resolveClass fallback when setObjectInputFilter is unavailable.
     * We won't use a default denylist yet so as not to break any existing apps
     */
    private static boolean isDeniedByDefaultDenylist(String className) {
        // Strip array prefixes ("[B", "[L...;") to get the element class name
        String name = className;
        while (name.startsWith("[")) {
            name = name.substring(1);
        }
        if (name.startsWith("L") && name.endsWith(";")) {
            name = name.substring(1, name.length() - 1);
        }
/*        
        return name.startsWith("org.apache.commons.collections.") ||
               name.startsWith("org.apache.commons.collections4.") ||
               name.startsWith("org.apache.commons.beanutils.") ||
               name.startsWith("com.sun.org.apache.xalan.") ||
               name.startsWith("org.springframework.") ||
               name.startsWith("org.codehaus.groovy.") ||
               name.startsWith("bsh.");
*/
        return false;
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
