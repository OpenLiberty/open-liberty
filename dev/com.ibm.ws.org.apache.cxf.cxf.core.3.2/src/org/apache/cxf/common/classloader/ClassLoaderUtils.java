/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.common.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * This class is extremely useful for loading resources and classes in a fault
 * tolerant manner that works across different applications servers. Do not
 * touch this unless you're a grizzled classloading guru veteran who is going to
 * verify any change on 6 different application servers.
 */
public final class ClassLoaderUtils {
    private static final boolean SKIP_SM = System.getSecurityManager() == null;

    private ClassLoaderUtils() {
    }

    public static class ClassLoaderHolder {
        ClassLoader loader;
        Thread thread;

        ClassLoaderHolder(final ClassLoader c, final Thread thread) {
            this.loader = c;
            this.thread = thread;
        }

        public void reset() {
            if (SKIP_SM) {
                thread.setContextClassLoader(loader);
            } else {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        thread.setContextClassLoader(loader);
                        return null;
                    }
                });
            }
        }
    }
    public static ClassLoaderHolder setThreadContextClassloader(final ClassLoader newLoader) {
        if (SKIP_SM) {
            final Thread thread = Thread.currentThread();
            final ClassLoader l = thread.getContextClassLoader();
            thread.setContextClassLoader(newLoader);
            return new ClassLoaderHolder(l, thread);
        }
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoaderHolder>() {
            public ClassLoaderHolder run() {
                final Thread thread = Thread.currentThread();
                final ClassLoader l = thread.getContextClassLoader();
                thread.setContextClassLoader(newLoader);
                return new ClassLoaderHolder(l, thread);
            }
        });
    }

    public static ClassLoader getURLClassLoader(
        final URL[] urls, final ClassLoader parent
    ) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return new URLClassLoader(urls, parent);
            }
        });
    }

    public static ClassLoader getURLClassLoader(
        final List<URL> urlList, final ClassLoader parent
    ) {
        return getURLClassLoader(urlList.toArray(new URL[0]), parent);
    }

    /**
     * Load a given resource. <p/> This method will try to load the resource
     * using the following methods (in order):
     * <ul>
     * <li>From Thread.currentThread().getContextClassLoader()
     * <li>From ClassLoaderUtil.class.getClassLoader()
     * <li>callingClass.getClassLoader()
     * </ul>
     *
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     */
    public static URL getResource(String resourceName, Class<?> callingClass) {
        URL url = getContextClassLoader().getResource(resourceName);
        if (url == null && resourceName.startsWith("/")) {
            //certain classloaders need it without the leading /
            url = getContextClassLoader().getResource(resourceName.substring(1));
        }

        ClassLoader cluClassloader = ClassLoaderUtils.class.getClassLoader();
        if (cluClassloader == null) {
            cluClassloader = ClassLoader.getSystemClassLoader();
        }
        if (url == null) {
            url = cluClassloader.getResource(resourceName);
        }
        if (url == null && resourceName.startsWith("/")) {
            //certain classloaders need it without the leading /
            url = cluClassloader.getResource(resourceName.substring(1));
        }

        if (url == null && callingClass != null) {
            ClassLoader cl = callingClass.getClassLoader();

            if (cl != null) {
                url = cl.getResource(resourceName);
            }
        }

        if (url == null && callingClass != null) {
            url = callingClass.getResource(resourceName);
        }

        if ((url == null) && (resourceName != null) && (resourceName.charAt(0) != '/')) {
            return getResource('/' + resourceName, callingClass);
        }

        return url;
    }

    /**
     * Load a given resources. <p/> This method will try to load the resources
     * using the following methods (in order):
     * <ul>
     * <li>From Thread.currentThread().getContextClassLoader()
     * <li>From ClassLoaderUtil.class.getClassLoader()
     * <li>callingClass.getClassLoader()
     * </ul>
     *
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     */
    @FFDCIgnore({IOException.class, IOException.class, IOException.class, IOException.class, IOException.class})
    public static List<URL> getResources(String resourceName, Class<?> callingClass) {
        List<URL> ret = new ArrayList<>();
        Enumeration<URL> urls = new Enumeration<URL>() {
            public boolean hasMoreElements() {
                return false;
            }
            public URL nextElement() {
                return null;
            }

        };
        try {
            urls = getContextClassLoader().getResources(resourceName);
        } catch (IOException e) {
            //ignore
        }
        if (!urls.hasMoreElements() && resourceName.startsWith("/")) {
            //certain classloaders need it without the leading /
            try {
                urls = getContextClassLoader().getResources(resourceName.substring(1));
            } catch (IOException e) {
                // ignore
            }
        }

        ClassLoader cluClassloader = ClassLoaderUtils.class.getClassLoader();
        if (cluClassloader == null) {
            cluClassloader = ClassLoader.getSystemClassLoader();
        }
        if (!urls.hasMoreElements()) {
            try {
                urls = cluClassloader.getResources(resourceName);
            } catch (IOException e) {
                // ignore
            }
        }
        if (!urls.hasMoreElements() && resourceName.startsWith("/")) {
            //certain classloaders need it without the leading /
            try {
                urls = cluClassloader.getResources(resourceName.substring(1));
            } catch (IOException e) {
                // ignore
            }
        }

        if (!urls.hasMoreElements()) {
            ClassLoader cl = callingClass.getClassLoader();

            if (cl != null) {
                try {
                    urls = cl.getResources(resourceName);
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        if (!urls.hasMoreElements()) {
            URL url = callingClass.getResource(resourceName);
            if (url != null) {
                ret.add(url);
            }
        }
        while (urls.hasMoreElements()) {
            ret.add(urls.nextElement());
        }


        if (ret.isEmpty() && (resourceName != null) && (resourceName.charAt(0) != '/')) {
            return getResources('/' + resourceName, callingClass);
        }
        return ret;
    }


    /**
     * This is a convenience method to load a resource as a stream. <p/> The
     * algorithm used to find the resource is given in getResource()
     *
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     */
    @FFDCIgnore({IOException.class})
    public static InputStream getResourceAsStream(String resourceName, Class<?> callingClass) {
        URL url = getResource(resourceName, callingClass);

        try {
            return (url != null) ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Load a class with a given name. <p/> It will try to load the class in the
     * following order:
     * <ul>
     * <li>From Thread.currentThread().getContextClassLoader()
     * <li>Using the basic Class.forName()
     * <li>From ClassLoaderUtil.class.getClassLoader()
     * <li>From the callingClass.getClassLoader()
     * </ul>
     *
     * @param className The name of the class to load
     * @param callingClass The Class object of the calling object
     * @throws ClassNotFoundException If the class cannot be found anywhere.
     */
    @FFDCIgnore({ClassNotFoundException.class})
    public static Class<?> loadClass(String className, Class<?> callingClass)
        throws ClassNotFoundException {
        try {
            ClassLoader cl = getContextClassLoader();

            if (cl != null) {
                return cl.loadClass(className);
            }
        } catch (ClassNotFoundException e) {
            //ignore
        }
        return loadClass2(className, callingClass);
    }
    @FFDCIgnore({ClassNotFoundException.class})
    public static <T> Class<? extends T> loadClass(String className, Class<?> callingClass, Class<T> type)
        throws ClassNotFoundException {
        try {
            ClassLoader cl = getContextClassLoader();

            if (cl != null) {
                return cl.loadClass(className).asSubclass(type);
            }
        } catch (ClassNotFoundException e) {
            //ignore
        }
        return loadClass2(className, callingClass).asSubclass(type);
    }
    public static String getClassLoaderName(Class<?> type) {
        ClassLoader loader = getClassLoader(type);
        return loader == null ? "null" : loader.toString();
    }

    public static Class<?> loadClassFromContextLoader(String className) throws ClassNotFoundException {
        return getContextClassLoader().loadClass(className);
    }

    @FFDCIgnore({ClassNotFoundException.class, ClassNotFoundException.class})
    private static Class<?> loadClass2(String className, Class<?> callingClass)
        throws ClassNotFoundException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            try {
                final ClassLoader loader = getClassLoader(ClassLoaderUtils.class);
                if (loader != null) {
                    return loader.loadClass(className);
                }
            } catch (ClassNotFoundException exc) {
                if (callingClass != null) {
                    final ClassLoader callingClassLoader = getClassLoader(callingClass);
                    if (callingClassLoader != null) {
                        return callingClassLoader.loadClass(className);
                    }
                }
            }
            throw ex;
        }
    }

    static ClassLoader getContextClassLoader() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    return loader != null ? loader : ClassLoader.getSystemClassLoader();
                }
            });
        } 
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader : ClassLoader.getSystemClassLoader();
    }

    private static ClassLoader getClassLoader(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        }
        return clazz.getClassLoader();
    }

}
