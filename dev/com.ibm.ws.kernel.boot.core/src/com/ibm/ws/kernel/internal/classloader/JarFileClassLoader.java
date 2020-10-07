/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.internal.classloader;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.ibm.ws.kernel.boot.classloader.ClassLoaderHook;
import com.ibm.ws.kernel.boot.classloader.ClassLoaderHookFactory;

/**
 * ClassLoader implementation that can load jar files without signature verification. This
 * ClassLoader implementation is designed to work without SecurityManager installed.
 * <p/>
 * Current limitations:
 * <li> Class-Path headers are not processed.</li>
 * <li> Remote URLs that point to directories are not supported.</li>
 */
public class JarFileClassLoader extends SecureClassLoader implements Closeable {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    private final CopyOnWriteArrayList<URL> urls;
    private final CopyOnWriteArrayList<ResourceHandler> resourceHandlers;
    private final boolean verify;
    private final ClassLoaderHook hook;

    public JarFileClassLoader(URL[] urls, boolean verify, ClassLoader parent) {
        super(parent);

        if (System.getSecurityManager() != null) {
            throw new IllegalStateException("This ClassLoader does not work with SecurityManager");
        }

        this.verify = verify;
        hook = ClassLoaderHookFactory.getClassLoaderHook(this);
        if (urls == null) {
            this.urls = new CopyOnWriteArrayList<URL>();
            this.resourceHandlers = new CopyOnWriteArrayList<ResourceHandler>();
        } else {
            this.urls = new CopyOnWriteArrayList<URL>(Arrays.asList(urls));

            // avoid adding resource handlers one at a time to a copy on write list
            List<ResourceHandler> tempResourceHandlers = new ArrayList<ResourceHandler>(urls.length);
            for (URL url : urls) {
                tempResourceHandlers.add(createResoureHandler(url, verify));
            }
            // Create resourceHandler list in one go
            this.resourceHandlers = new CopyOnWriteArrayList<ResourceHandler>(tempResourceHandlers);
        }
    }

    private ResourceHandler createResoureHandler(URL url, boolean verify) {
        String urlFile = url.getFile();
        try {
            if (urlFile != null && urlFile.endsWith("/")) {
                if ("file".equals(url.getProtocol())) {
                    File file = new File(url.toURI().getPath());
                    return new DirectoryResourceHandler(file);
                } else {
                    throw new IllegalArgumentException("URL is not supported: " + url);
                }
            } else {
                return new JarResourceHandler(url, verify);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }

    @Override
    public void close() {
        for (ResourceHandler handler : resourceHandlers) {
            close(handler);
        }
        resourceHandlers.clear();
    }

    protected void addURL(URL url) {
        urls.add(url);
        resourceHandlers.add(createResoureHandler(url, verify));
    }

    public URL[] getURLs() {
        return urls.toArray(new URL[0]);
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        return findClass(className, false);
    }

    protected Class<?> findClass(String className, boolean returnNull) throws ClassNotFoundException {
        String resourceName = new StringBuilder(className.replace('.', '/')).append(".class").toString();

        ResourceEntry entry = findResourceEntry(resourceName);
        if (entry == null) {
            if (returnNull) {
                return null;
            }
            throw new ClassNotFoundException(className);
        }

        ResourceHandler resourceHandler = entry.getResourceHandler();
        URL jarURL = resourceHandler.toURL();

        byte[] classBytes = null;
        boolean foundInClassCache = false;
        if (hook != null) {
            classBytes = hook.loadClass(jarURL, className);
            foundInClassCache = (classBytes != null);
        }

        Manifest manifest = null;
        try {
            if (!foundInClassCache) {
                classBytes = entry.getBytes();
            }

            manifest = resourceHandler.getManifest();
        } catch (IOException e) {
            if (returnNull) {
                throw null;
            }
            throw new ClassNotFoundException(className, e);
        }

        int packageEnd = className.lastIndexOf('.');
        if (packageEnd >= 0) {
            String packageName = className.substring(0, packageEnd);
            String packagePath = resourceName.substring(0, packageEnd + 1);

            // define package
            definePackage(packageName, packagePath, jarURL, manifest);
        }

        // create code source
        CodeSource source = new CodeSource(jarURL, entry.getCertificates());

        // define class
        Class<?> clazz = defineClass(className, classBytes, 0, classBytes.length, source);

        if (hook != null && !foundInClassCache) {
            hook.storeClass(jarURL, clazz);
        }

        return clazz;
    }

    private void definePackage(String packageName, String packagePath, URL jarURL, Manifest manifest) {
        Package pkg = getPackage(packageName);
        if (pkg != null) {
            if (pkg.isSealed()) {
                if (!pkg.isSealed(jarURL)) {
                    throw new SecurityException("Seal violation: Package " + packageName + " is sealed with another URL.");
                }
            } else if (manifest != null) {
                Attributes packageAttributes = manifest.getAttributes(packagePath);
                Attributes mainAttributes = manifest.getMainAttributes();
                if (isSealed(packageAttributes, mainAttributes)) {
                    throw new SecurityException("Seal violation: Cannot seal already loaded package " + packageName);
                }
            }
        } else {
            String specTitle = null, specVendor = null, specVersion = null;
            String implTitle = null, implVendor = null, implVersion = null;
            URL sealBase = null;

            if (manifest != null) {
                Attributes packageAttributes = manifest.getAttributes(packagePath);
                Attributes mainAttributes = manifest.getMainAttributes();

                specTitle = getAttribute(Attributes.Name.SPECIFICATION_TITLE, packageAttributes, mainAttributes);
                specVendor = getAttribute(Attributes.Name.SPECIFICATION_VENDOR, packageAttributes, mainAttributes);
                specVersion = getAttribute(Attributes.Name.SPECIFICATION_VERSION, packageAttributes, mainAttributes);
                implTitle = getAttribute(Attributes.Name.IMPLEMENTATION_TITLE, packageAttributes, mainAttributes);
                implVendor = getAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, packageAttributes, mainAttributes);
                implVersion = getAttribute(Attributes.Name.IMPLEMENTATION_VERSION, packageAttributes, mainAttributes);

                if (isSealed(packageAttributes, mainAttributes)) {
                    sealBase = jarURL;
                }
            }

            try {
                definePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
            } catch (IllegalArgumentException iae) {
                // Ignore rather than protect against redefining packages when parallel-enabled
            }
        }
    }

    private static String getAttribute(Attributes.Name name, Attributes packageAttributes, Attributes mainAttributes) {
        if (packageAttributes != null) {
            String value = packageAttributes.getValue(name);
            if (value != null) {
                return value;
            }
        }
        return (mainAttributes != null) ? mainAttributes.getValue(name) : null;
    }

    private boolean isSealed(Attributes packageAttributes, Attributes mainAttributes) {
        String sealed = getAttribute(Attributes.Name.SEALED, packageAttributes, mainAttributes);
        return (sealed == null) ? false : "true".equalsIgnoreCase(sealed);
    }

    @Override
    public URL findResource(String resourceName) {
        ResourceEntry entry = findResourceEntry(resourceName);
        return (entry == null) ? null : entry.toURL();
    }

    @Override
    public Enumeration<URL> findResources(String resourceName) throws IOException {
        return new ResourceEntryEnumeration(resourceHandlers, resourceName);
    }

    private ResourceEntry findResourceEntry(String resourceName) {
        for (ResourceHandler handler : resourceHandlers) {
            ResourceEntry entry = handler.getEntry(resourceName);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    private static class ResourceEntryEnumeration implements Enumeration<URL> {

        private final String resourceName;
        private final Iterator<ResourceHandler> handlerIterator;
        private ResourceEntry entry;

        public ResourceEntryEnumeration(List<ResourceHandler> resourceHandlers, String resourceName) {
            this.resourceName = resourceName;
            this.handlerIterator = resourceHandlers.iterator();
            this.entry = null;
        }

        @Override
        public boolean hasMoreElements() {
            return next();
        }

        @Override
        public URL nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            ResourceEntry resourceEntry = entry;
            entry = null;
            return resourceEntry.toURL();
        }

        private boolean next() {
            if (entry == null) {
                while (handlerIterator.hasNext()) {
                    ResourceEntry resourceEntry = handlerIterator.next().getEntry(resourceName);
                    if (resourceEntry != null) {
                        entry = resourceEntry;
                        return true;
                    }
                }
                return false;
            } else {
                return true;
            }
        }
    }

    static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            // this is very unexpected
            throw new Error(e);
        }
    }

    static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }

    static byte[] getBytes(InputStream in, long inLength) throws IOException {
        int offset = 0;
        int len = (int) inLength;
        byte[] buf = new byte[len];
        int read;

        while (len > 0 && 0 < (read = in.read(buf, offset, len))) {
            len -= read;
            offset += read;
        }
        return buf;
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] bytes = new byte[1024];
        int read;
        while (0 <= (read = in.read(bytes))) {
            out.write(bytes, 0, read);
        }
    }
}
