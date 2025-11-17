/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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
package com.ibm.ws.classloading.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.url.WSJarURLConnection;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.classloading.internal.util.ClassRedefiner;
import com.ibm.ws.classloading.internal.util.Keyed;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.classloader.ClassLoaderHook;
import com.ibm.ws.kernel.boot.classloader.ClassLoaderHookFactory;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.util.CacheHashMap;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.kernel.service.utils.CompositeEnumeration;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

import io.openliberty.checkpoint.spi.CheckpointPhase;

abstract class ContainerClassLoader extends LibertyLoader implements Keyed<ClassLoaderIdentity> {
    private static final boolean disableSharedClassesCache = Boolean.getBoolean("liberty.disableApplicationClassSharing");
    static final CheckpointPhase checkpointPhase = CheckpointPhase.getPhase();
    static {
        ClassLoader.registerAsParallelCapable();
    }
    static final TraceComponent tc = Tr.register(ContainerClassLoader.class);

    /**
     * This class will stop JARs from being cached when they are being read from. This will prevent java from keeping a stream open to the file and thus locking it so
     * the user cannot delete it. The class will do this disabling in a static initialization block so to be used it just needs to be loaded.
     */
    private static class JarCacheDisabler {
        static {
            try {
                URLConnection connection = new URL("jar:file:///something.jar!/").openConnection();
                connection.setDefaultUseCaches(false);
            } catch (MalformedURLException e) {
                Tr.warning(tc, "WARN_JARS_STILL_CACHED");
            } catch (IOException e) {
                Tr.warning(tc, "WARN_JARS_STILL_CACHED");
            }
        };

        /**
         * No-op method to trigger the initialization of this class and thus disable JAR caching.
         */
        public static void disableJarCaching() {
            /* already done exactly once by class init! */
        }
    }

    /**
     * The one and only instance of the smartClassPath, responsible for
     * coordinating lookups to the Container classpath.
     */
    private volatile SmartClassPath smartClassPath;

    private final List<UniversalContainer> nativeLibraryContainers = new ArrayList<UniversalContainer>();

    private final ClassRedefiner redefiner;

    final String jarProtocol;

    private final ClassLoaderHook hook;

    /**
     * Util method to totally read an input stream into a byte array.
     * Used for class definition.
     *
     * @param stream the stream to read.
     * @return byte array of data from stream.
     * @throws IOException if an error occurs.
     */
    @Trivial
    private static byte[] getBytes(InputStream stream, int knownSize) throws IOException {
        try {
            if (knownSize == -1) {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                try {
                    byte[] bytes = new byte[1024];
                    int read;
                    while (0 <= (read = stream.read(bytes)))
                        byteOut.write(bytes, 0, read);

                    return byteOut.toByteArray();
                } finally {
                    Util.tryToClose(byteOut);
                }
            } else {
                byte[] bytes = new byte[knownSize];
                int read;
                int offset = 0;
                while (knownSize > 0 && (read = stream.read(bytes, offset, knownSize)) > 0) {
                    offset += read;
                    knownSize -= read;
                }

                return bytes;
            }
        } finally {
            Util.tryToClose(stream);
        }
    }

    static class ContainerURL {
        final URL url;
        final String urlString;
        ContainerURL(URL url) {
            this.url = url;
            this.urlString = url.toString();
        }
        @Override
        public String toString() {
            return urlString;
        }
    }

    /**
     * A unifying interface to bridge ArtifactContainers, and adaptable Containers.
     */
    interface UniversalContainer {

        /**
         * A resource located within a UniversalContainer
         */
        interface UniversalResource {
            /**
             * Obtain the URL for this resource, or null if it has none.
             *
             * @return
             */
            public URL getResourceURL(String jarProtocol);

            /**
             * Obtain the ByteResourceInformation for this resource.
             *
             * @return
             * @throws IOException if the ByteResourceInformation is unable to be returned.
             */
            public ByteResourceInformation getByteResourceInformation(String className, ClassLoaderHook hook) throws IOException;

            /**
             * Get the physical path for this native library resource, extracting
             * it to the file system as necessary.
             */
            public String getNativeLibraryPath();

            public String getResourceName();
        }

        /**
         * Attempt to obtain a resource given a path.
         *
         * @param name the path to look for the resource at
         * @return UniversalResource if found, null otherwise.
         */
        UniversalResource getResource(String name);

        /**
         * Update the supplied map with information about this container.
         * method should add this Universal container to the map onto any
         * lists that exist for each package contained within this container.
         * adding the list to the map if not already present.
         * Map is keyed by the hashcode of the package string.
         */
        void updatePackageMap(Map<Integer, UniversalContainerList> map, boolean prepend);
        
        /**
         * Returns a collection of URLs represented by the underlying
         * Container or ArtifactContainer.  Depending on the instance of
         * this UniversalContainer, this will return either
         * <code>Container.getURLs()</code> or 
         * <code>ArtifactContainer.getURLs()</code>.
         */
        Collection<URL> getContainerURLs();

        /**
         * Defines a package using the provided <code>LibertyLoader</code>
         */
        void definePackage(String packageName, LibertyLoader loader, ContainerURL containerURL);

        /**
         * Returns the container URL where the content of the resource is located for this container
         * @return the container URL
         */
        ContainerURL getContainerURL(UniversalResource resource);

        /**
         * @return
         */
        URL getSharedClassCacheURL(UniversalResource resource);
    }

    @SuppressWarnings("unchecked")
    static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
            throw (E) e;
    }
    /**
     * Implementation of UniversalResource backed by an adaptable Entry.
     */
    private static class EntryUniversalResource implements UniversalContainer.UniversalResource {
        final UniversalContainer container;
        final Entry entry;
        final String resourceName;

        public EntryUniversalResource(UniversalContainer container, Entry entry, String resourceName) {
            this.container = container;
            this.entry = entry;
            this.resourceName = resourceName;
        }

        @Override
        public URL getResourceURL(String jarProtocol) {
            URL url = this.entry.getResource();
            if (url == null) {
                return null;
            }
            boolean uSlash = url.getPath().endsWith("/");
            boolean pSlash = resourceName.endsWith("/");

            if (uSlash == pSlash) {
                //if the path requested, and the url, both end, or both do not end, in slashes,
                //return the result.
                return url;
            } else {
                //path and url had different endings.. one had a slash, the other did not.
                if (uSlash) {
                    //if the url ended in a slash (and the path did not, else we would have returned
                    //already.. ) then strip the slash from the end of the url.
                    return ContainerClassLoader.stripTrailingSlash(url);
                } else {
                    //if the url did not end in a slash, but the path did, it was a request
                    //for a directory resource, and the entry represented a file resource
                    //so ignore it.
                    return null;
                }
            }
        }

        @Override
        public ByteResourceInformation getByteResourceInformation(String className, ClassLoaderHook hook) throws IOException {
            return new ByteResourceInformation(container, this, className, this::getActualBytes, hook);
        }

        @Trivial
        private byte[] getActualBytes() {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "CCL: EntryUniversalResource.getActualBytes for " + resourceName);
            }
            try {
                try {
                    InputStream is = this.entry.adapt(InputStream.class);
                    return ContainerClassLoader.getBytes(is, (int) entry.getSize());
                } catch (UnableToAdaptException e) {
                    throw new IOException(e);
                }
            } catch (IOException e) {
                sneakyThrow(e);
                return null; //never gets here
            }
        }
        @Override
        public String getNativeLibraryPath() {
            try {
                NativeLibrary nl = this.entry.adapt(NativeLibrary.class);
                if (nl != null) {
                    return nl.getLibraryFile().getPath();
                }
            } catch (UnableToAdaptException e1) {
                // Ignore (FFDC only).
            }
            return null;
        }

        @Override
        public String getResourceName() {
            return resourceName;
        }
    }

    private static class ContainerUniversalResource implements UniversalContainer.UniversalResource {
        private final Container container;

        public ContainerUniversalResource(Container c) {
            container = c;
        }

        @Override
        public URL getResourceURL(String jarProtocol) {
            Collection<URL> urls = container.getURLs();
            if (urls.isEmpty())
                return null;

            // best we can do is return the first one
            URL result = urls.iterator().next();
            if ("file".equals(result.getProtocol()) && !(result.getPath().endsWith("/"))) {
                // TODO can this apply to non file: URLs?
                try {
                    return new URL(jarProtocol + result.toExternalForm() + "!/");
                } catch (MalformedURLException e) {
                    return result;
                }
            }
            return result;
        }

        @Override
        public ByteResourceInformation getByteResourceInformation(String className, ClassLoaderHook hook) throws IOException {
            return null;
        }

        @Override
        public String getNativeLibraryPath() {
            return null;
        }

        @Override
        public String getResourceName() {
            return null;
        }
    }

    private static abstract class AbstractUniversalContainer<E> implements UniversalContainer {
        /**
         * Constant that is used to indicate that there is no main attributes used for definePackage.
         * This constant is used to indicate that getManifestMainAttributes() should return null.
         */
        private static final Map<Name, String> NULL_MAIN_ATTRIBUTES = Collections.emptyMap();

        private static final Map<Name,Name> packageAttributes;

        static {
            Map<Name,Name> packageAttrs = new HashMap<>();
            packageAttrs.put(Name.SPECIFICATION_TITLE, Name.SPECIFICATION_TITLE);
            packageAttrs.put(Name.SPECIFICATION_VERSION, Name.SPECIFICATION_VERSION);
            packageAttrs.put(Name.SPECIFICATION_VENDOR, Name.SPECIFICATION_VENDOR);
            packageAttrs.put(Name.IMPLEMENTATION_TITLE, Name.IMPLEMENTATION_TITLE);
            packageAttrs.put(Name.IMPLEMENTATION_VERSION, Name.IMPLEMENTATION_VERSION);
            packageAttrs.put(Name.IMPLEMENTATION_VENDOR, Name.IMPLEMENTATION_VENDOR);
            packageAttrs.put(Name.SEALED, Name.SEALED);
            packageAttributes = Collections.unmodifiableMap(packageAttrs);
        }

        private volatile Map<Name, String> manifestMainAttributes = null;
        private volatile Map<String, Map<Name, String>> manifestEntryAttributes = null;

        private final ContainerURL resourceContainerURL;
        private final URL resourceSharedClassCacheURL;
        private final File resourceContainerDir;
        public AbstractUniversalContainer(Collection<URL> containerURLs, ClassLoaderHook hook) {
            URL originalRoot = null;
            URL convertedRoot = null;
            boolean multiple = false;
            for (URL url : containerURLs) {
                URL converted = createContainerURL(url);
                if (converted != null) {
                    String path = converted.getPath();
                    if (!path.endsWith(".overlay/")) {
                        if (convertedRoot == null) {
                            convertedRoot = converted;
                            originalRoot = url;
                        } else {
                            multiple = true;
                        }
                    }
                }
            }
            if (multiple || convertedRoot == null) {
                resourceContainerURL = null;
                resourceContainerDir = null;
                resourceSharedClassCacheURL = null;
            } else {
                resourceContainerURL = new ContainerURL(convertedRoot);
                File containerFile = null;
                try {
                    containerFile = new File(resourceContainerURL.url.toURI());
                } catch (URISyntaxException e) {
                    // Auto-FFDC
                }
                resourceContainerDir = containerFile != null && isDirectory(containerFile) ? containerFile : null;
                resourceSharedClassCacheURL = hook != null ? createSharedClassCacheURL(resourceContainerURL, originalRoot, resourceContainerDir) : null;
            }
        }

        URL createContainerURL(URL base) {
            try {
                if ("file".equals(base.getProtocol())) {
                    // use file URLs as-is
                    return base;
                }

                URLConnection conn = base.openConnection();
                if (conn instanceof JarURLConnection) {
                    return ((JarURLConnection) conn).getJarFileURL();
                } else if (conn instanceof WSJarURLConnection) {
                    return ((WSJarURLConnection) conn).getFile().toURI().toURL();
                }
                throw new UnsupportedOperationException(base.getProtocol());
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }

        URL createSharedClassCacheURL(ContainerURL containerURL, URL originalRoot, File containerDir) {
            if (containerURL == null) {
                return null;
            }
            if (containerDir != null) {
                return containerURL.url;
            }
            try {
                String containerPath = containerURL.url.getPath();
                if (containerPath.endsWith(".jar") || containerPath.endsWith(".zip")) {
                    // use containerURL as-is if it is a jar or zip extension
                    return containerURL.url;
                }

                String basePath = originalRoot.getPath();
                int bangSlash = basePath.lastIndexOf("!/");
                if (bangSlash >= 0) {
                    // append the original !/ path (likely !/WEB-INF/classes)
                    String bangSlashPath = basePath.substring(bangSlash);
                    // If it is not a jar or zip and we add !/ only to the URL it will not be treated as a valid
                    // archive and should fall to the code below to handle that case.
                    if (bangSlashPath.length() > 2) {
                        return new URL(containerURL.urlString + bangSlashPath);
                    }
                }
                // If the URL is not to a directory and does not end with jar or zip file extension then
                // we assume it is still some type of archive (e.g. rar).
                // The Semeru shared classes cache logic will not recognize the URL as a valid archive
                // if it does not end with jar or zip file extension.
                // The URL will get recognized as a valid archive if it does contain '!/' with any path after.
                // Here we append '!/l' to the URL so that the Semeru shared classes cache logic will treat it
                // as a valid archive.  For example: file://path/to/myResourceAdaptor.rar!/l
                return new URL(containerURL.urlString + "!/l");
            } catch (MalformedURLException e) {
                return null;
            }
        }

        @Override
        public ContainerURL getContainerURL(UniversalResource resource) {
            if (resourceContainerURL != null) {
                if (resourceContainerDir != null) {
                    // need to make sure the resource is really in this directory
                    if (exists(new File(resourceContainerDir, resource.getResourceName()))) {
                        return resourceContainerURL;
                    }
                } else {
                    return resourceContainerURL;
                }
            }
            // TODO asking for "jar" but that is not honored in all cases so still need to handle "wsjar" being returned
            URL resourceUrl = resource.getResourceURL("jar");
            if (resourceUrl != null) {
                String protocol = resourceUrl.getProtocol();
                try {
                    if ("jar".equals(protocol)) {
                        URLConnection conn = resourceUrl.openConnection();
                        if (conn instanceof JarURLConnection) {
                            return new ContainerURL(((JarURLConnection) conn).getJarFileURL());
                        }
                        // unexpected; throw exception for FFDC indicating the connection class
                        throw new IOException(conn.getClass().getName());
                    } else if ("wsjar".equals(protocol)) {
                        URLConnection conn = resourceUrl.openConnection();
                        if (conn instanceof WSJarURLConnection) {
                            return new ContainerURL(((WSJarURLConnection) conn).getFile().toURI().toURL());
                        }
                        // unexpected; throw exception for FFDC indicating the connection class
                        throw new IOException(conn.getClass().getName());
                    } else if ("file".equals(protocol)) {
                        // A file URL - i.e. the contents of the classes are expanded on the disk.
                        // so a path like:  .../myServer/dropins/myWar.war/WEB-INF/classes/com/myPkg/MyClass.class
                        // should convert to: .../myServer/dropins/myWar.war/WEB-INF/classes/
                        return new ContainerURL(new URL(resourceUrl.toString().replace(resource.getResourceName(), "")));
                    }
                    // unexpected; throw exception for FFDC indicating the unexpected protocol
                    throw new IOException(protocol);
                } catch (IOException e) {
                    // auto-FFDC
                }
            }
            // TODO it is questionable to allow null here; currently the code handles null.
            return null;
        }

        @Override
        public URL getSharedClassCacheURL(UniversalResource resource) {
            if (resourceSharedClassCacheURL != null) {
                if (resourceContainerDir != null) {
                    // need to make sure the resource is really in this directory
                    if (exists(new File(resourceContainerDir, resource.getResourceName()))) {
                        return resourceSharedClassCacheURL;
                    }
                } else {
                    return resourceSharedClassCacheURL;
                }
            }
            return getSharedClassCacheURLFromResource(resource);
        }

        private URL getSharedClassCacheURLFromResource(UniversalResource resource) {
            // TODO asking for "jar" but that is not honored in all cases so still need to handle "wsjar" being returned
            URL resourceURL = resource.getResourceURL("jar");
            String resourceName = resource.getResourceName();
            URL sharedClassCacheURL;
            if (resourceURL == null) {
                return null;
            }
            String protocol = resourceURL.getProtocol();
            // Doing the conversion that the shared class cache logic does for jar
            // URLs in order to do less work while holding a shared class cache monitor.
            if ("jar".equals(protocol) || "wsjar".equals(protocol)) {
                String path = resourceURL.getPath();
                // Can only do this for jar files.  Shared class cache logic
                // cannot handle a file reference that is a war for instance.
                // Need to use the full path for war files.
                if (path.endsWith(resourceName)) {
                    path = path.substring(0, path.length() - resourceName.length());
                    if (path.endsWith(".jar!/") || path.endsWith(".zip!/")) {
                        path = path.substring(0, path.length() - 2);
                    } else {
                        // If the archive file name does not end with jar or zip file extension and the URL ends with !/, 
                        // the !/ will get stripped off by the shared classes cache logic and will not be recognized 
                        // correctly as a jar file when it is a RAR for instance so add an extra character to the end of the URL.
                        // Without this extra character, RAR files were not being recognized as being updated leading to 
                        // stale classes being returned after the RAR file was updated.
                        if (path.endsWith("!/")) {
                            path += "l";
                        }
                    }
                }
                try {
                    sharedClassCacheURL = new URL(path);
                } catch (MalformedURLException e) {
                    sharedClassCacheURL = null;
                }
            } else if (!"file".equals(protocol)) {
                sharedClassCacheURL = null;
            } else {
                String externalForm = resourceURL.toExternalForm();
                if (externalForm.endsWith(resourceName)) {
                    try {
                        sharedClassCacheURL = new URL(externalForm.substring(0, externalForm.length() - resourceName.length()));
                    } catch (MalformedURLException e) {
                        sharedClassCacheURL = null;
                    }
                } else {
                    sharedClassCacheURL = null;
                }
            }
            return sharedClassCacheURL;
        }

        @Override
        @FFDCIgnore(value = { IllegalArgumentException.class })
        public final void definePackage(String packageName, LibertyLoader loader, ContainerURL containerURL) {
            Map<Name, String> mainAttributes = getManifestMainAttributes();
            try {
                if (mainAttributes == NULL_MAIN_ATTRIBUTES && manifestEntryAttributes == null) {
                    loader.definePackage(packageName, null, null, null, null, null, null, null);
                } else {
                    //define package impl, that uses package sealing information as defined on wikipedia
                    //to set vars passed up to ClassLoader.definePackage.
                    String specTitle = null;
                    String specVersion = null;
                    String specVendor = null;
                    String implTitle = null;
                    String implVersion = null;
                    String implVendor = null;
                    String sealedString = null;

                    if (manifestEntryAttributes != null) {
                        String unixName = packageName.replace('.', '/') + "/"; //replace all dots with slash and add trailing slash
                        Map<Name, String> entryAttributes = manifestEntryAttributes.get(unixName);
                        if (entryAttributes != null) {
                            specTitle = entryAttributes.get(Name.SPECIFICATION_TITLE);
                            specVersion = entryAttributes.get(Name.SPECIFICATION_VERSION);
                            specVendor = entryAttributes.get(Name.SPECIFICATION_VENDOR);
                            implTitle = entryAttributes.get(Name.IMPLEMENTATION_TITLE);
                            implVersion = entryAttributes.get(Name.IMPLEMENTATION_VERSION);
                            implVendor = entryAttributes.get(Name.IMPLEMENTATION_VENDOR);
                            sealedString = entryAttributes.get(Name.SEALED);
                        }
                    }

                    if (mainAttributes != NULL_MAIN_ATTRIBUTES) {
                        if (specTitle == null) {
                            specTitle = mainAttributes.get(Name.SPECIFICATION_TITLE);
                        }
                        if (specVersion == null) {
                            specVersion = mainAttributes.get(Name.SPECIFICATION_VERSION);
                        }
                        if (specVendor == null) {
                            specVendor = mainAttributes.get(Name.SPECIFICATION_VENDOR);
                        }
                        if (implTitle == null) {
                            implTitle = mainAttributes.get(Name.IMPLEMENTATION_TITLE);
                        }
                        if (implVersion == null) {
                            implVersion = mainAttributes.get(Name.IMPLEMENTATION_VERSION);
                        }
                        if (implVendor == null) {
                            implVendor = mainAttributes.get(Name.IMPLEMENTATION_VENDOR);
                        }
                        if (sealedString == null) {
                            sealedString = mainAttributes.get(Name.SEALED);
                        }
                    }

                    URL sealBase = null;
                    if (sealedString != null && sealedString.equalsIgnoreCase("true")) {
                        sealBase = containerURL.url;
                    }

                    loader.definePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
                }
            } catch (IllegalArgumentException e) {
                // Ignore, this happens if the package is already defined but it is hard to guard against this in a thread safe way. See:
                // http://bugs.sun.com/view_bug.do?bug_id=4841786
            }
        }

        @FFDCIgnore(value = { IOException.class })
        Map<Name, String> getManifestMainAttributes() {
            // See if we've already loaded the manifest
            if (this.manifestMainAttributes == null) {
                synchronized (this) {
                    if (this.manifestMainAttributes == null) {
                        E e = getEntry("META-INF/MANIFEST.MF");
                        if (e != null) {
                            InputStream manifestStream = null;
                            try {
                                manifestStream = getInputStream(e);
                                if (manifestStream != null) {
                                    Manifest manifest = new Manifest(manifestStream);

                                    Map<String, Attributes> manifestEntries = manifest.getEntries();
                                    if (!manifestEntries.isEmpty()) {
                                        this.manifestEntryAttributes = filterEntryAttributes(manifestEntries);
                                    }
                                    
                                    Attributes mainAttributes = manifest.getMainAttributes();
                                    if (!mainAttributes.isEmpty()) {
                                        this.manifestMainAttributes = filterAttributes(mainAttributes);
                                    }
                                }
                            } catch (IOException e2) {
                                // Ignore, we'll just define a package with no package information
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "IOException thrown opening resource {0}", getResourceURL(e));
                                }
                            } finally {
                                Util.tryToClose(manifestStream);
                            }
                        }
                        // if it is still null, then set it to the static variable to 
                        // indicate there are no main attributes.
                        if (this.manifestMainAttributes == null) {
                            this.manifestMainAttributes = NULL_MAIN_ATTRIBUTES;
                        }
                    }
                }
            }
            return this.manifestMainAttributes;
        }

        private static Map<String, Map<Name, String>> filterEntryAttributes(Map<String, Attributes> manifestEntries) throws IOException {
            Map<String, Map<Name, String>> entries = null;
            for (Map.Entry<String, Attributes> entry : manifestEntries.entrySet()) {
                String key = entry.getKey();
                if (key != null && key.endsWith("/")) {
                    Attributes attributes = entry.getValue();
                    if (!attributes.isEmpty()) {
                        Map<Name, String> newAttributes = filterAttributes(attributes);
                        if (newAttributes != null) {
                            if (entries == null) {
                                entries = new HashMap<>(7);
                            }
                            entries.put(key, newAttributes);
                        }
                    }
                }
            }
            return entries;
        }

        private static Map<Name, String> filterAttributes(Attributes attributes) {
            Map<Name, String> newAttributes = null;
            for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
                Object key = entry.getKey();
                if (key instanceof Name) {
                    Name validName = packageAttributes.get(key);
                    // Use the constant instead of the one created from reading in the Manifest file.
                    if (validName != null) {
                        if (newAttributes == null) {
                            newAttributes = new HashMap<>(7);
                        }
                        newAttributes.put(validName, (String) entry.getValue());
                    }
                }
            }
            return newAttributes;
        }

        abstract E getEntry(String path);

        abstract InputStream getInputStream(E entry) throws IOException;

        abstract URL getResourceURL(E entry);
    }

    /**
     * Implementation of a UniversalContainer, backed by an adaptable Container.
     */
    private static class ContainerUniversalContainer extends AbstractUniversalContainer<Entry> {
        private final Container container;
        private final boolean isRoot;
        private String debugString;

        public ContainerUniversalContainer(Container container, ClassLoaderHook hook) {
            super(container.getURLs(), hook);
            this.container = container;
            this.isRoot = container.isRoot();
            // If we are doing checkpoint, process the manifest file when the container is created.
            if (!checkpointPhase.restored()) {
                getManifestMainAttributes();
            }
        }

        @Override
        public UniversalResource getResource(String path) {
            if (!isRoot) {
                //if the container this UC represents is not a path
                //root, then we have to ensure that the path requested
                //cannot escape to the parent container(s).
                path = PathUtils.normalize(path);
                if (!PathUtils.isNormalizedPathAbsolute(path)) {
                    return null;
                }
                //convert abs paths back to relative to this node for getEntry.
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
            }

            // handle "" and "/" for roots since they refer to the container itself
            if (path.length() == 0 || path.equals("/")) {
                return new ContainerUniversalResource(this.container);
            }

            //try a lookup for the path in the container.
            Entry e = this.container.getEntry(path);
            if (e != null) {
                return new EntryUniversalResource(this, e, path);
            } else {
                return null;
            }
        }

        @Trivial
        private void processContainer(Container c, Map<Integer, UniversalContainerList> map, int chop, boolean prepend) {
            for (Entry e : c) {
                try {
                    Container child = e.adapt(Container.class);
                    if (child != null && !child.isRoot()) {
                        Integer key = child.getPath().substring(chop).hashCode();
                        UniversalContainerList listForThisPath = map.get(key);
                        if (listForThisPath == null) {
                            listForThisPath = new UniversalContainerList(new ArrayList<>());
                            map.put(key, listForThisPath);
                        }
                        if (!listForThisPath.contains(this)) {
                            listForThisPath.add(this, prepend);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "CCL: {" + listForThisPath.size() + "} [" + this.hashCode() + "] adding : [" + key + "] " + (child.getPath().substring(chop)));
                        }
                        processContainer(child, map, chop, prepend);
                    }
                } catch (UnableToAdaptException ex) {
                    //ignore.
                }
            }
        }

        @Override
        @Trivial
        synchronized public void updatePackageMap(Map<Integer, UniversalContainerList> map, boolean prepend) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "CCL: updating map for adaptable container with path " + this.container.getPath());
            //could speed this up using an adapter to access the underlying artifact container to use localOnly..
            //we'll keep it simple for now though and just use the existing adaptable api layer.
            int chop = 1;
            if (!"/".equals(this.container.getPath())) {
                chop = this.container.getPath().length() + 1; //we add 1 to remove the leading slash from entries below this.
            }
            processContainer(this.container, map, chop, prepend);
        }

        @Override
        public Collection<URL> getContainerURLs() {
            return container == null ? null : container.getURLs();
        }

        @Override
        public String toString() {
            if (debugString == null) {
                String physicalPath = this.container.getPhysicalPath();
                if (physicalPath == null) {
                    physicalPath = this.container.getPath();
                }
                debugString = physicalPath;
            }
            return debugString;
        }

        @Override
        Entry getEntry(String path) {
            return this.container.getEntry(path);
        }

        @Override
        InputStream getInputStream(Entry e) throws IOException {
            try {
                return e.adapt(InputStream.class);
            } catch (UnableToAdaptException e1) {
                // Ignore, we'll just define a package with no package information
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "UnableToAdaptException thrown opening resource {0}", e.getResource());
                }
                return null;
            }
        }

        @Override
        URL getResourceURL(Entry e) {
            return e.getResource();
        }
    }

    /**
     * Implementation of a UniversalResource backed by an ArtifactEntry
     */
    private static class ArtifactEntryUniversalResource implements UniversalContainer.UniversalResource {
        final UniversalContainer container;
        final ArtifactEntry entry;
        final String resourceName;

        public ArtifactEntryUniversalResource(UniversalContainer container, ArtifactEntry entry, String resourceName) {
            this.container = container;
            this.entry = entry;
            this.resourceName = resourceName;
        }

        @Override
        public URL getResourceURL(String jarProtocol) {
            URL url = this.entry.getResource();
            if (url == null) {
                return null;
            }
            boolean uSlash = url.getPath().endsWith("/");
            boolean pSlash = resourceName.endsWith("/");
            if (uSlash == pSlash) {
                //if the path requested, and the url, both end, or both do not end, in slashes,
                //return the result.
                return url;
            } else {
                //path and url had different endings.. one had a slash, the other did not.
                if (uSlash) {
                    //if the url ended in a slash (and the path did not, else we would have returned
                    //already.. ) then strip the slash from the end of the url.
                    return ContainerClassLoader.stripTrailingSlash(url);
                } else {
                    //if the url did not end in a slash, but the path did, it was a request
                    //for a directory resource, and the entry represented a file resource
                    //so ignore it.
                    return null;
                }
            }
        }

        @Override
        public ByteResourceInformation getByteResourceInformation(String className, ClassLoaderHook hook) throws IOException {
            return new ByteResourceInformation(container, this, className, this::getActualBytes, hook);
        }

        @Trivial
        byte[] getActualBytes() {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "CCL: ArtifactEntryUniversalResource.getActualBytes for " + resourceName);
            }
            try {
                InputStream is = this.entry.getInputStream();
                return ContainerClassLoader.getBytes(is, (int) entry.getSize());
            } catch (IOException e) {
                sneakyThrow(e);
                return null; // never actually get here
            }
        }
        @Override
        public String getNativeLibraryPath() {
            try {
                File f = NativeLibraryAdapter.getFileForLibraryEntry(this.entry);
                if (f != null)
                    return f.getPath();
            } catch (IOException io) {
                //Just ffdc.
            }
            return null;
        }

        @Override
        public String getResourceName() {
            return resourceName;
        }
    }

    /**
     * Implementation of a UniversalContainer, backed by an ArtifactContainer.
     */
    private static class ArtifactContainerUniversalContainer extends AbstractUniversalContainer<ArtifactEntry> {
        final ArtifactContainer container;
        final boolean isRoot;

        public ArtifactContainerUniversalContainer(ArtifactContainer container, ClassLoaderHook hook) {
            super(container.getURLs(), hook);
            this.container = container;
            this.isRoot = container.isRoot();
            // If we are doing checkpoint, process the manifest file when the container is created.
            if (!checkpointPhase.restored()) {
                getManifestMainAttributes();
            }
        }

        @Override
        public UniversalResource getResource(String path) {
            if (!isRoot) {
                //if the container this UC represents is not a path
                //root, then we have to ensure that the path requested
                //cannot escape to the parent container(s).
                //this normalize is strictly not needed if the path has already been normalized
                //which is the case for map based processing, and we could update to remove it
                //to gain a little extra speed.
                //Currently the map based path is only in play when the maps are built, so
                //we normalise here for safety.
                path = PathUtils.normalize(path);
                if (!PathUtils.isNormalizedPathAbsolute(path)) {
                    return null;
                }
                //convert abs paths back to relative to this node for getEntry.
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
            }

            // handle "" and "/" for roots since they refer to the container itself
            if (path.length() == 0 || path.equals("/")) {
                return new ArtifactContainerUniversalResource(this.container);
            }

            try {
                //try a lookup for the path in the container.
                ArtifactEntry e = this.container.getEntry(path);
                if (e != null) {
                    return new ArtifactEntryUniversalResource(this, e, path);
                } else {
                    return null;
                }
            } catch (IllegalArgumentException e) {
                //getEntry can throw this for paths like .. on a root container
                //for us, it just means 'not found'.
                return null;
            }
        }

        private void processContainer(ArtifactContainer c, Map<Integer, UniversalContainerList> map, int chop, boolean prepend) {
            for (ArtifactEntry e : c) {
                ArtifactContainer child = e.convertToContainer(true);
                if (child != null) {
                    Integer key = child.getPath().substring(chop).hashCode();
                    UniversalContainerList listForThisPath = map.get(key);
                    if (listForThisPath == null) {
                        listForThisPath = new UniversalContainerList(new ArrayList<>());
                        map.put(key, listForThisPath);
                    }
                    if (!listForThisPath.contains(this)) {
                        listForThisPath.add(this, prepend);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "CCL: {" + listForThisPath.size() + "} [" + this.hashCode() + "] adding : [" + key + "] " + (child.getPath().substring(chop)));
                    }
                    processContainer(child, map, chop, prepend);
                }
            }
        }

        @Override
        synchronized public void updatePackageMap(Map<Integer, UniversalContainerList> map, boolean prepend) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "CCL: updating map for artifact container with path " + this.container.getPath());
            int chop = 1;
            if (!"/".equals(this.container.getPath())) {
                chop = this.container.getPath().length() + 1; //we add 1 to remove the leading slash from entries below this.
            }
            processContainer(container, map, chop, prepend);
        }
        
        @Override
        public Collection<URL> getContainerURLs() {
            return container == null ? null : container.getURLs();
        }

        @Override
        ArtifactEntry getEntry(String path) {
            return this.container.getEntry(path);
        }

        @Override
        InputStream getInputStream(ArtifactEntry e) throws IOException {
            return e.getInputStream();
        }

        @Override
        URL getResourceURL(ArtifactEntry e) {
            return e.getResource();
        }
    }

    private static class ArtifactContainerUniversalResource implements UniversalContainer.UniversalResource {
        private final ArtifactContainer container;

        public ArtifactContainerUniversalResource(ArtifactContainer c) {
            container = c;
        }

        @Override
        public URL getResourceURL(String jarProtocol) {
            Collection<URL> urls = container.getURLs();
            if (urls.isEmpty())
                return null;

            // best we can do is return the first one
            return urls.iterator().next();
        }

        @Override
        public ByteResourceInformation getByteResourceInformation(String className, ClassLoaderHook hook) throws IOException {
            return null;
        }

        @Override
        public String getNativeLibraryPath() {
            return null;
        }

        @Override
        public String getResourceName() {
            return null;
        }
    }

    private interface SmartClassPath {
        /**
         * Add an adaptable Container to the classpath.
         *
         * @param container the container to add.
         */
        void addContainer(Container container);

        /**
         * Add an ArtifactContainer to the classpath.
         *
         * @param container the container to add.
         */
        void addArtifactContainer(ArtifactContainer container);

        void addArtifactContainers(Iterable<ArtifactContainer> containers, boolean prepend);

        ByteResourceInformation getByteResourceInformation(String className, String path, ClassLoaderHook hook) throws IOException;

        URL getResourceURL(String path, String jarProtocol);

        Collection<URL> getResourceURLs(String path, String jarProtocol);

        boolean containsContainer(Container container);

        Collection<Collection<URL>> getClassPath();
    }

    /**
     * The one and only map creation queue.. used to update container package maps
     * while trying not to impact the rest of the system too much!
     */
    protected final static ExecutorService mapCreationQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("ClassloaderMapProcessing" + t.getName());
            return t;
        }
    });

    static {
        //submit a first job that will tie the queue up until server is started..
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "CCL: blocking the new map building thread");
        }
        mapCreationQueue.submit(new Runnable() {
            @Override
            public void run() {
                Bundle b = FrameworkUtil.getBundle(ContainerClassLoader.class);
                if (b != null) {
                    BundleContext bc = b.getBundleContext();
                    if (bc != null) {
                        //we don't really need the customizer, as we only want to block until service arrives, or we timeout.
                        final ServiceTracker<ServerStarted, ServerStarted> st = new ServiceTracker<ServerStarted, ServerStarted>(bc, ServerStarted.class, new ServiceTrackerCustomizer<ServerStarted, ServerStarted>() {
                            @Override
                            public ServerStarted addingService(ServiceReference<ServerStarted> arg0) {
                                return null;
                            }

                            @Override
                            public void modifiedService(ServiceReference<ServerStarted> arg0, ServerStarted arg1) {}

                            @Override
                            public void removedService(ServiceReference<ServerStarted> arg0, ServerStarted arg1) {}
                        });
                        try {
                            //if server hasn't started after 2 mins, unblock the map creation queue.
                            st.waitForService(2 * 60 * 1000);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "CCL: map building thread unblocked due to server start notify");
                        } catch (InterruptedException e) {
                            //if we're interrupted.. just unblock the queue.
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "CCL: map building thread unblocked due to interrupt");
                        }
                    }
                }
            }
        });
    }

    static class UniversalContainerList implements Iterable<UniversalContainer>{
        static final UniversalContainerList EMPTY = new UniversalContainerList();

        private final List<UniversalContainer> containers;
        private int prependIndex = 0;

        private UniversalContainerList() {
            // private constructor for empty
            containers = Collections.emptyList();
        }

        UniversalContainerList(List<UniversalContainer> containers) {
            this.containers = containers;
        }

        /**
         * @param containerUniversalContainer
         * @return
         */
        @Trivial
        public boolean contains(UniversalContainer container) {
            return containers.contains(container);
        }

        @Trivial
        void add(UniversalContainer container, boolean prepend) {
            if (prepend) {
                containers.add(prependIndex++, container);
            } else {
                containers.add(container);
            }
        }

        @Trivial
        int size() {
            return containers.size();
        }

        @Override
        @Trivial
        public Iterator<UniversalContainer> iterator() {
            return containers.iterator();
        }

        @Override
        @Trivial
        public String toString() {
            return containers.toString();
        }
    }

    /**
     * The "smart" classpath implementation.<p>
     * Uses a list of universal containers to implement a classpath.
     */
    private static class SmartClassPathImpl implements SmartClassPath {
        final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
        final AtomicInteger outstandingContainers = new AtomicInteger(0);

        final static boolean usePackageMap = !Boolean.getBoolean("com.ibm.ws.classloading.container.disableMap");
        final static Integer maxLastNotFound = Integer.getInteger("com.ibm.ws.classloading.container.lastNotFound", 250);
        final static Integer maxLastFound = Integer.getInteger("com.ibm.ws.classloading.container.lastFound", 900);
        final static Integer maxLastReallyNotFound = Integer.getInteger("com.ibm.ws.classloading.container.lastReallyNotFound", 900);
        final static boolean propsInUse = (!usePackageMap || System.getProperty("com.ibm.ws.classloading.container.lastNotFound") != null
                                           || System.getProperty("com.ibm.ws.classloading.container.lastFound") != null
                                           || System.getProperty("com.ibm.ws.classloading.container.lastReallyNotFound") != null);

        static {
            if (propsInUse && tc.isDebugEnabled()) {
                Tr.debug(tc, "CCL: custom cache properties in use : lastNotFound=" + maxLastNotFound + " lastFound=" + maxLastFound + " lastReallyNotFound="
                             + maxLastReallyNotFound);
                if (usePackageMap) {
                    Tr.debug(tc, "CCL: experimental package map engaged.. utoh!");
                }
            }
        }

        // This classPath field MUST be here and remain a List<UniversalContainer> to avoid breaking classgraph
        // https://github.com/classgraph/classgraph/blob/classgraph-4.8.44/src/main/java/nonapi/io/github/classgraph/classloaderhandler/WebsphereLibertyClassLoaderHandler.java#L137-L158
        final List<UniversalContainer> classPath = new CopyOnWriteArrayList<ContainerClassLoader.UniversalContainer>();
        final UniversalContainerList classPathContainers = new UniversalContainerList(classPath);
        /**
         * How many 'not found' paths to cache per classpath element.<p>
         * A not found path will accelerate future locations of 'found' elements by helping the
         * search skip quickly past classpath elements that do not contain it, and also help
         * the search quickly skip all locations for total not-founds.<p>
         * The value chosen is arbitrary and may be interesting to tweak.
         */
        final int MAX_LASTNOTFOUND = maxLastNotFound;
        final List<Set<String>> lastNotFound = new CopyOnWriteArrayList<Set<String>>();
        /**
         * How many found urls to cache for this entire classloader, helps a lot for frequent lookups
         */
        final int MAX_LASTFOUND = maxLastFound;
        final Map<String, URL> lastFoundURL = Collections.synchronizedMap(new CacheHashMap<String, URL>(MAX_LASTFOUND));
        /**
         * How many 'really not found' (eg, not known at all to this classloader) to cache.
         */
        final int MAX_LASTREALLYNOTFOUND = maxLastReallyNotFound;
        final Map<String, Object> lastReallyNotFoundURL = Collections.synchronizedMap(new CacheHashMap<String, Object>(MAX_LASTREALLYNOTFOUND));

        /**
         * This containers package map, indexed from hashCode of package string to list of relevant containers.
         */
        final Map<Integer, UniversalContainerList> packageMap = usePackageMap ? new HashMap<Integer, UniversalContainerList>() : null;

        final Set<Container> containers = Collections.newSetFromMap(new WeakHashMap<Container, Boolean>());

        final ClassLoaderHook hook;

        SmartClassPathImpl(ClassLoaderHook hook) {
            this.hook = hook;
        }

        /**
         * Internal method to add a new UniversalContainer to the list.
         *
         * @param uc
         */
        @SuppressWarnings("deprecation")
        private synchronized void addUniversalContainer(final UniversalContainer uc, final boolean prepend) {
            if (tc.isDebugEnabled()) {
                // Debug info for classpath elements as they are added.. candidate for Trace.debug.
                if (uc instanceof ArtifactContainerUniversalContainer) {
                    Tr.debug(tc, "CCL: " + this.hashCode() + " cpelt idx " + classPathContainers.size() + "wraps " + ((ArtifactContainerUniversalContainer) uc).container);
                    Tr.debug(tc, "CCL: " + this.hashCode() + " cpelt idx " + classPathContainers.size() + " ART url "
                                 + ((ArtifactContainerUniversalContainer) uc).container.getPhysicalPath());
                } else {
                    Tr.debug(tc, "CCL: " + this.hashCode() + " cpelt idx " + classPathContainers.size() + " wraps " + ((ContainerUniversalContainer) uc).container);
                    Tr.debug(tc, "CCL: " + this.hashCode() + " cpelt idx " + classPathContainers.size() + " CON url " + ((ContainerUniversalContainer) uc).container.getPhysicalPath());
                }
            }

            if (usePackageMap) {
                outstandingContainers.incrementAndGet();
                mapCreationQueue.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (tc.isDebugEnabled()) {
                            if (uc instanceof ArtifactContainerUniversalContainer) {
                                Tr.debug(tc, "CCL: " + this.hashCode() + " building package map for " + ((ArtifactContainerUniversalContainer) uc).container.getPhysicalPath());
                            } else {
                                Tr.debug(tc, "CCL: " + this.hashCode() + " building package map for " + ((ContainerUniversalContainer) uc).container.getPhysicalPath());
                            }
                        }
                        //perform the update of the map inside the write lock
                        //to prevent the classloader using the map in an inconsistent state.
                        WriteLock write = rwLock.writeLock();
                        write.lock();
                        try {
                            uc.updatePackageMap(packageMap, prepend);
                            outstandingContainers.decrementAndGet();
                        } finally {
                            write.unlock();
                        }
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "CCL: " + this.hashCode() + " done building package map.");
                    }
                });

            }

            //Note method is synchronized to attempt to keep these two always executing together,
            //although the implementation is written so it wont matter if the 'wrong' lastNotFound
            //set is used with a given cp entry. They all start empty, and are equiv at this stage.
            classPathContainers.add(uc, prepend);

            lastNotFound.add(Collections.synchronizedSet(new LinkedHashSet<String>()));
        }

        @Override
        public void addContainer(Container container) {
            containers.add(container);
            addUniversalContainer(new ContainerUniversalContainer(container, hook), false);
        }

        @Override
        public void addArtifactContainer(ArtifactContainer container) {
            addUniversalContainer(new ArtifactContainerUniversalContainer(container, hook), false);
        }

        @Override
        public void addArtifactContainers(Iterable<ArtifactContainer> containers, boolean prepend) {
            for (ArtifactContainer container : containers) {
                addUniversalContainer(new ArtifactContainerUniversalContainer(container, hook), prepend);
            }
        }

        @Trivial
        private UniversalContainerList getUniversalContainersForPath(String path, UniversalContainerList classpath) {
            //if we have outstanding requests, then we should just use the classpath, else
            //we risk not seeing content on the classpath that we should see.
            if (outstandingContainers.get() > 0) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "CCL: request for " + path + " made to use map while map update pending, reverting request to full classpath");
                return classpath;
            }

            //need to normalise path for map
            path = PathUtils.normalizeUnixStylePath(path);
            int startidx = path.startsWith("/") == true ? 1 : 0; //will use this in substring to chop off leading slash when needed.
            int slashidx = path.lastIndexOf('/');
            // '/' maps to all containers..
            // as will '/fish' and '/anything'
            // any paths at 2nd level deep, or below, eg /fish/chips  or /anything/else or /a/b/c will use map.
            //
            // slash idx -1 means no /'s in string                  - use all containers
            // slash idx 0 means / was the first and only '/' char  - use all containers
            // slash idx >0 means / was present after other chars, so we use the map..
            if (slashidx > 0) {
                Integer key = path.substring(startidx, slashidx).hashCode();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "CCL: checking map using key {" + key + "} for path '" + path.substring(0, slashidx) + "'    :    origpath: '" + path + "'");

                //Check the map with a read lock, can be shared by many users,
                //but will block if there is a package update in progress
                //This is a narrow window, as we already tested that the outstanding
                //containers value was zero, so to block here means another thread snuck
                //in between there, and here, and altered the classpath.
                //We don't alter the classpath often, and blocking will ensure correct
                //behavior.
                ReadLock read = rwLock.readLock();
                read.lock();
                UniversalContainerList containersForKey;
                try {
                    containersForKey = packageMap.get(key);
                } finally {
                    read.unlock();
                }
                if (containersForKey != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "CCL: got hit for key, returning container set with " + containersForKey.size() + " containers.");
                    return containersForKey;
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "CCL: key was unknown, returning empty set. ");
                    return UniversalContainerList.EMPTY;
                }
            } //else, leave locationsToCheck as classpath.
            else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "CCL: request for a root level resource... : '" + path + "' returning original set with " + classpath.size());
            }
            return classpath;
        }

        @Override
        public ByteResourceInformation getByteResourceInformation(String className, String path, ClassLoaderHook hook) throws IOException {
            int idx = 0;
            UniversalContainerList locationsToCheck = classPathContainers;
            if (usePackageMap) {
                locationsToCheck = getUniversalContainersForPath(path, locationsToCheck);
            }

            for (UniversalContainer uc : locationsToCheck) {
                Set<String> lastNotFoundForThisContainer = lastNotFound.get(idx);
                //when we use package map, the index for the cache lookup is invalid
                //to fix this needs the cache moving inside the universal containers
                if (usePackageMap || pathNotInlastNotFound(path, lastNotFoundForThisContainer)) {
                    //no hit in not-found-cache.. try to obtain.
                    UniversalContainer.UniversalResource ur = uc.getResource(path);
                    if (ur != null) {
                        //got one..
                        ByteResourceInformation is = ur.getByteResourceInformation(className, hook);
                        if (is != null) {
                            return is;
                        }
                    } else {
                        //looked, but did not find.. update the not-found-cache.
                        //(unless we used packagemap to get here, in which case it's
                        //(not the right cache! so don't touch it!
                        if (!usePackageMap && lastNotFoundForThisContainer != null) {
                            addPath(lastNotFoundForThisContainer, path);
                        }
                    }
                }
                idx++;
            }
            return null;
        }

        @Override
        public URL getResourceURL(String path, String jarProtocol) {
            //test positive cache 1st.
            URL cached = lastFoundURL.get(path);
            if (cached != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "CCL: [" + this.hashCode() + "]  getResourceURL : '" + path + "' " + "lastFound hit.");
                return cached;
            }
            //test negative cache next..
            if (lastReallyNotFoundURL.containsKey(path)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "CCL: [" + this.hashCode() + "]  getResourceURL : '" + path + "' " + "lastReallyNotFound hit.");
                return null;
            }

            UniversalContainerList locationsToCheck = classPathContainers;
            if (usePackageMap) {
                locationsToCheck = getUniversalContainersForPath(path, locationsToCheck);
            }

            int idx = 0;
            int skipped = 0;
            for (UniversalContainer uc : locationsToCheck) {
                Set<String> lastNotFoundForThisContainer = lastNotFound.get(idx);
                if (usePackageMap || pathNotInlastNotFound(path, lastNotFoundForThisContainer)) {
                    //no hit found, try getResource
                    UniversalContainer.UniversalResource ur = uc.getResource(path);
                    if (ur != null) {
                        URL url = ur.getResourceURL(jarProtocol);
                        //some resources may not have urls.. ensure we dont return null.
                        if (url != null) {
                            //add url to cache..
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "CCL: [" + this.hashCode() + "]  getResourceURL : '" + path + "' " + "found at classpath index " + idx
                                             + " local not found caches allowed us to skip " + skipped + " locations. Found cache is now.. "
                                             + lastFoundURL.size() + " and path was known to cache? " + lastFoundURL.containsKey(path));
                            lastFoundURL.put(path, url);
                            return url;
                        }
                    } else {
                        //looked, but did not find, update cache.
                        if (!usePackageMap && lastNotFoundForThisContainer != null) {
                            addPath(lastNotFoundForThisContainer, path);
                        }
                    }
                } else {
                    skipped++;
                }
                idx++;
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "CCL: [" + this.hashCode() + "]  getResourceURL : '" + path + "' " + "really not found. Cache size is now.. " + lastReallyNotFoundURL.size()
                             + " path already known to cache? " + lastReallyNotFoundURL.containsKey(path));
            lastReallyNotFoundURL.put(path, null);//abusing a map as a set here =)
            return null;
        }

        @Override
        public Collection<URL> getResourceURLs(String path, String jarProtocol) {
            List<URL> urls = new ArrayList<URL>();
            if (lastReallyNotFoundURL.containsKey(path)) {
                return urls;
            }

            UniversalContainerList locationsToCheck = classPathContainers;
            if (usePackageMap) {
                locationsToCheck = getUniversalContainersForPath(path, locationsToCheck);
            }

            int idx = 0;
            for (UniversalContainer uc : locationsToCheck) {
                Set<String> lastNotFoundForThisContainer = lastNotFound.get(idx);
                if (usePackageMap || pathNotInlastNotFound(path, lastNotFoundForThisContainer)) {
                    //cache did not know this path, attempt getResource
                    UniversalContainer.UniversalResource ur = uc.getResource(path);
                    if (ur != null) {
                        URL url = ur.getResourceURL(jarProtocol);
                        if (url != null) {
                            urls.add(url);
                        }
                    } else {
                        //looked but did not find.. update cache.
                        if (!usePackageMap && lastNotFoundForThisContainer != null) {
                            addPath(lastNotFoundForThisContainer, path);
                        }
                    }
                }
                idx++;
            }
            if (urls.isEmpty()) {
                lastReallyNotFoundURL.put(path, null);
            }
            return urls;
        }

        private void addPath(Set<String> lastNotFoundForThisContainer, String path) {
            synchronized (lastNotFoundForThisContainer) {
                if (lastNotFoundForThisContainer.size() >= MAX_LASTNOTFOUND) {
                    //remove 1st item from set.
                    Iterator<String> i = lastNotFoundForThisContainer.iterator();
                    i.next();
                    i.remove();
                }
                lastNotFoundForThisContainer.add(path);
            }
        }

        private boolean pathNotInlastNotFound(String path, Set<String> lastNotFoundForThisContainer) {
            boolean pathFound = false;
            if (lastNotFoundForThisContainer != null) {
                synchronized (lastNotFoundForThisContainer) {
                    pathFound = lastNotFoundForThisContainer.contains(path);
                }
            }
            return !!!pathFound;
        }

        @Override
        @Trivial
        public String toString() {
            return String.valueOf(classPathContainers);
        }

        @Override
        public boolean containsContainer(Container container) {
            return containers.contains(container);
        }

        @Override
        public Collection<Collection<URL>> getClassPath() {
            List<Collection<URL>> containerURLs = new ArrayList<>();
            for (UniversalContainer uc : classPathContainers) {
                containerURLs.add(uc.getContainerURLs());
            }
            return containerURLs;
        }
    }

    /**
     * A wrapper for the {@link SmartClassPathImpl} that passes all write operations
     * through to the underlying {@link SmartClassPathImpl} but calls {@link ContainerClassLoader#lazyInit()} and then "unwraps" itself
     * before the first read operation.
     */
    private class UnreadSmartClassPath implements SmartClassPath {
        SmartClassPathImpl delegate;

        UnreadSmartClassPath(ClassLoaderHook hook) {
            delegate = new SmartClassPathImpl(hook);
        }

        @Override
        public void addContainer(Container container) {
            delegate.addContainer(container);
        }

        @Override
        public void addArtifactContainer(ArtifactContainer container) {
            delegate.addArtifactContainer(container);
        }

        @Override
        public void addArtifactContainers(Iterable<ArtifactContainer> containers, boolean prepend) {
            delegate.addArtifactContainers(containers, prepend);
        }

        @Override
        public synchronized ByteResourceInformation getByteResourceInformation(String className, String path, ClassLoaderHook hook) throws IOException {
            unwrap();
            return delegate.getByteResourceInformation(className, path, hook);
        }

        @Override
        public synchronized URL getResourceURL(String path, String jarProtocol) {
            unwrap();
            return delegate.getResourceURL(path, jarProtocol);
        }

        @Override
        public synchronized Collection<URL> getResourceURLs(String path, String jarProtocol) {
            unwrap();
            return delegate.getResourceURLs(path, jarProtocol);
        }

        private void unwrap() {
            final String methodName = "UnreadSmartClassPath.unwrap(): ";
            // check we haven't already been unwrapped
            if (ContainerClassLoader.this.smartClassPath == delegate) {
                // already unwrapped, no more unwrapping required
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "Another thread has snuck in and performed the lazy initialisation already");
                return;
            }
            // looks like we might have to do it ourselves - use a sync block to ensure only one thread
            // invokes lazyInit()
            synchronized (this) { // Double-check locking works because smartClassPath is volatile
                if (ContainerClassLoader.this.smartClassPath == this) {
                    try {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName + "First read operation on class loader: perform lazy initialisation");
                        ContainerClassLoader.this.lazyInit();
                    } finally {
                        ContainerClassLoader.this.smartClassPath = delegate;
                    }
                }
            }
        }

        @Override
        @Trivial
        public String toString() {
            return delegate.toString();
        }

        @Override
        public boolean containsContainer(Container container) {
            return delegate.containsContainer(container);
        }

        @Override
        public Collection<Collection<URL>> getClassPath() {
            return delegate.getClassPath();
        }
    }

    /**
     * Class that represents byte data for a resource.<p>
     * A data structure that stores the resource URL and bytes for a particular class.<br>
     * It also has a utility to try to load a manifest from the resource URL (assuming it points to a JAR)<br>
     */
    static final class ByteResourceInformation {
        private final byte[] bytes;
        private final UniversalContainer resourceContainer;
        private final ContainerURL containerURL;
        private final URL sharedClassCacheURL;
        private final boolean fromClassCache;
        private final Supplier<byte[]> actualBytes;
        private final ClassLoaderHook hook;

        /**
         * @param bytes
         * @param resourceUrl
         */
        ByteResourceInformation(UniversalContainer root, UniversalContainer.UniversalResource resource, String className, Supplier<byte[]> actualBytes, ClassLoaderHook hook) {
            byte[] classBytes = null;
            if (hook == null) {
                sharedClassCacheURL = null;
            } else {
                sharedClassCacheURL = root.getSharedClassCacheURL(resource);
                if (sharedClassCacheURL != null) {
                    classBytes = hook.loadClass(sharedClassCacheURL, className);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        if (classBytes != null) {
                            Tr.debug(tc, "Found class in shared class cache", new Object[] {className, sharedClassCacheURL});
                        } else {
                            Tr.debug(tc, "Did not find class in shared class cache", new Object[] {className, sharedClassCacheURL});
                        }
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "No shared class cache URL to find class", className);
                    }
                }
            }
            fromClassCache = classBytes != null;
            if (!fromClassCache) {
                classBytes = actualBytes.get();
            }
            this.bytes = classBytes;
            this.resourceContainer = root;
            this.containerURL = root.getContainerURL(resource);
            this.actualBytes = actualBytes;
            this.hook = hook;
        }

        /**
         * Returns the bytes for the class loaded from this resource.
         *
         * @return The byte[]
         */
        @Trivial
        public byte[] getBytes() {
            return this.bytes;
        }

        void definePackage(String packageName, LibertyLoader loader) {
            resourceContainer.definePackage(packageName, loader, containerURL);
        }

        /**
         * Returns the container URL for this resource
         *
         * @return
         */
        public ContainerURL getContainerURL() {
            return containerURL;
        }

        public boolean foundInClassCache() {
            return fromClassCache;
        }

        @Trivial
        public byte[] getActualBytes() throws IOException {
            return actualBytes.get();
        }

        @Trivial
        public void storeInClassCache(Class<?> clazz, byte[] definedBytes ) {
            if (fromClassCache || hook == null) {
                return;
            }
            if (sharedClassCacheURL == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No shared class cache URL to store class", clazz.getName());
                }
                return;
            }
            if (!Arrays.equals(definedBytes, bytes)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Did not store class because defined bytes got modified", clazz.getName());
                }
                return;
            }
            hook.storeClass(sharedClassCacheURL, clazz);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Called shared class cache to store class", new Object[] {clazz.getName(), sharedClassCacheURL});
            }
        }
    }

    /**
     * Main constructor.. build a container loader with the specified adaptable Container classpath.<p>
     * Additional classpath entries are only addable via addLibraryFile.
     *
     * @param classpath Containers to use as classpath entries.
     * @param parent classloader to act as parent.
     */
    public ContainerClassLoader(List<Container> classpath, ClassLoader parent, ClassRedefiner redefiner, GlobalClassloadingConfiguration config) {
        super(parent);
        this.jarProtocol = config.useJarUrls() ? "jar:" : "wsjar:";
        //Temporary, reintroduced until WSJAR is implemented.
        JarCacheDisabler.disableJarCaching();

        hook = disableSharedClassesCache ? null : ClassLoaderHookFactory.getClassLoaderHook(this);
        smartClassPath = new UnreadSmartClassPath(hook);

        if (classpath != null) {
            for (Container c : classpath) {
                smartClassPath.addContainer(c);
            }
        }

        this.redefiner = redefiner;
    }

    @Override
    public URL findResource(String name) {
        return smartClassPath.getResourceURL(name, jarProtocol);
    }

    @Override
    public CompositeEnumeration<URL> findResources(String name) throws IOException {
        //start by collecting any from super, which checks parent, if any.
        CompositeEnumeration<URL> enumerations = new CompositeEnumeration<URL>(super.findResources(name));
        Collection<URL> urls = smartClassPath.getResourceURLs(name, jarProtocol);

        //no need to retry the smartClassPath with trailing /, it already handled that.
        if (!name.endsWith("/")) {
            enumerations.add(super.findResources(name + "/"));
            HashMap<String, URL> resourceMap = new HashMap<String, URL>();
            URL url = null;
            while (enumerations.hasMoreElements()) {
                url = stripTrailingSlash(enumerations.nextElement());
                resourceMap.put(url.toExternalForm(), url);
            }
            enumerations = new CompositeEnumeration<URL>(Collections.enumeration(resourceMap.values()));
        }

        enumerations.add(Collections.enumeration(urls));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            int i = 0;
            StringBuilder sb = new StringBuilder();
            List<URL> urlList = new ArrayList<URL>();
            while (enumerations.hasMoreElements()) {
                URL url = enumerations.nextElement();
                urlList.add(url);
                sb.append("\n  ").append(url);
                i++;
            }
            sb.append("\n  ").append("Total elements: ").append(i);
            enumerations = new CompositeEnumeration<URL>(Collections.enumeration(urlList));
            Tr.debug(tc, sb.toString());
        }
        return enumerations;
    }

    @Override
    protected String findLibrary(String libName) {
        String mappedName = System.mapLibraryName(libName);
        for (UniversalContainer uc : nativeLibraryContainers) {
            UniversalContainer.UniversalResource ur = uc.getResource(mappedName);
            if (ur != null) {
                String path = ur.getNativeLibraryPath();
                if (path != null) {
                    return path;
                }
            }
        }

        return null;
    }

    final ByteResourceInformation findClassBytes(String className, String resourceName) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return smartClassPath.getByteResourceInformation(className, resourceName, hook);
        } catch (IOException e) {
            Tr.error(tc, "cls.class.file.not.readable", className, resourceName);
            String message = String.format("Could not read class '%s' as resource '%s'", className, resourceName);
            ClassFormatError error = new ClassFormatError(message);
            error.initCause(e);
            throw error;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Add all the artifact containers to the class path
     */
    protected final void addToClassPath(Iterable<ArtifactContainer> artifacts, boolean prepend) {
        smartClassPath.addArtifactContainers(artifacts, prepend);
    }

    private static ServiceCaller<ArtifactContainerFactory> acf = new ServiceCaller<>(ContainerClassLoader.class, ArtifactContainerFactory.class); 
    /**
     * Method to allow adding shared libraries to this classloader, currently using File.
     *
     * @param f the File to add as a shared lib.. can be a dir or a jar (or a loose xml ;p)
     */
    @FFDCIgnore(IllegalStateException.class)
    protected void addLibraryFile(final File f) {

        if (!!!exists(f)) {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "cls.library.archive", f, new FileNotFoundException(f.getName()));
            }
            return;
        }

        // Skip files that are not archives of some sort.
        if (!isDirectory(f) && !isArchive(f))
            return;

        //this area subject to refactor following shared lib rework..
        //ideally the shared lib code will start passing us ArtifactContainers, and it
        //will own the management of the ACF via DS.
        BundleContext bc = FrameworkUtil.getBundle(ContainerClassLoader.class).getBundleContext();
        File dataFile = null;
        try {
            dataFile = bc == null ? null : bc.getDataFile("");
        } catch (IllegalStateException e) {
            Tr.debug(tc, "Invalid context. Liberty is likely shutting down.");
            return;
        }
        if (dataFile == null) {
            // Just being safe; Equinox never returns null from a valid context
            Tr.debug(tc, "Context returned null data file.");
            return;
        }
        final File df = dataFile;
        acf.call(new Consumer<ArtifactContainerFactory>() {
            @Override
            public void accept(ArtifactContainerFactory factory) {
                ArtifactContainer ac = factory.getContainer(df, f);
                if (ac == null) {
                    Tr.info(tc, "cls.library.file.forbidden", f);
                } else {
                    smartClassPath.addArtifactContainer(ac);
                }
            }
        });
    }

    protected void addNativeLibraryContainer(Container container) {
        nativeLibraryContainers.add(new ContainerUniversalContainer(container, hook));
    }

    /**
     * Check that a file is an archive
     *
     * @param f
     */
    @FFDCIgnore(PrivilegedActionException.class)
    private boolean isArchive(File f) {
        final File target = f;
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    new ZipFile(target).close();
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            Exception innerException = e.getException();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The following file can not be added to the classpath " + f + " due to error ", innerException);
            }
            return false;
        }
        return true;
    }

    /**
     * Subclasses should override this method to do any late initialization.
     * It will be invoked at most once, before the first lookup operation.
     * Any other lookup operations will see the effects of this method.
     */
    protected void lazyInit() {

    }

    /**
     * Returns a URL with the trailing / stripped off
     *
     * @param url
     * @return
     */
    private static URL stripTrailingSlash(URL url) {
        String externalForm = url.toExternalForm();
        if (externalForm.endsWith("/")) {
            externalForm = externalForm.substring(0, externalForm.length() - 1);
            try {
                url = new URL(externalForm);
            } catch (MalformedURLException e) {
                // will never happen.
            }
        }
        return url;
    }

    /**
     * Attempt to redefine classes files updated via the passed-in Notification.
     * This class will return true in two cases:
     * <ol>
     * <li>There are no class files in the notification to process.</li>
     * <li>All classes in the Notification that were loaded by this loader were able
     * to be redefined - i.e. only method bodies changed.</li>
     * </ol><br/>
     * Note that this method will return true if the notification contains classes
     * that loaded by other loaders, regardless of the changes to those classes.
     * Likewise, this method will return true if there are non-class files that
     * were modified.
     *
     * @param notification the Notification object containing modified files.
     * @return true if all classes in the notification that were loaded by this loader
     *         were able to be redefined. Otherwise, false.
     */
    public boolean redefineClasses(Notification notification) {
        // if there are no paths to process, then there is nothing to do
        if (notification.getPaths().isEmpty()) {
            return true;
        }

        Container container = notification.getContainer();
        // No need to do any processing if we are unable to redefine classes.
        boolean success;
        if (!smartClassPath.containsContainer(container)) {
            // This classloader is not associated with any container that has classes being modified,
            // so we can claim success.
            success = true;
        } else {

            // we only want to process *.class files
            List<String> classFilePaths = new ArrayList<String>();
            for (String path : notification.getPaths()) {
                if (path.endsWith(".class")) {
                    classFilePaths.add(path);
                }
            }

            if (classFilePaths.isEmpty()) {
                // no class files to process, we must return true.
                return true;
            }

            // This classloader is associated with a container that has classes being modified.
            // Success depends on whether we can redefine those classes or not.
            if (redefiner != null && redefiner.canRedefine()) {
                success = true;
                Set<ClassDefinition> classesToRedefine = new HashSet<ClassDefinition>();
                for (String path : classFilePaths) {

                    String className = convertToClassName(path);
                    // We only want to redefine classes that have been loaded.  Unloaded classes
                    // don't need to be redefined.
                    Class<?> clazz = findLoadedClass(className);
                    if (clazz != null && clazz.getClassLoader() == this) {
                        try {
                            InputStream is = container.getEntry(path).adapt(InputStream.class);
                            byte[] classBytes = loadBytes(is);
                            ClassDefinition def = new ClassDefinition(clazz, classBytes);
                            classesToRedefine.add(def);
                        } catch (Exception e) {
                            // This should be either an IOException or an UnableToAdaptException
                            FFDCFilter.processException(e, this.getClass().getName() + ".redefineClasses", "1557", this,
                                                        new Object[] { className, notification.getContainer(), path });
                            // failed to read class bytes - not good - probably something wrong
                            // on the file system - FFDC - and restart the app
                            success = false;
                            break;
                        }
                    } // temporary until metadata processing can identify newly added Java EE components:
                    else if (clazz == null) {
                        // hasn't been loaded -return false for now in case the class contains EE metadata,
                        // like @WebServlet, or @EJB, etc. that needs to be processed.
                        // TODO: remove this else block and return true for unloaded classes once new
                        // metadata processing is in place
                        return false;
                    }

                }

                if (success/* still */ && !classesToRedefine.isEmpty()) {
                    success = redefiner.redefineClasses(classesToRedefine);
                }

            } else {
                // classes were changed, but redefiner is null or cannot redefine classes, so we must return false
                success = false;
            }
        }
        return success;
    }

    private static String convertToClassName(String fileName) {
        String className;
        // remove ".class" extension
        className = fileName.replace(".class", "");

        // if necessary, remove WEB-INF/classes/
        className = className.replace("WEB-INF/classes/", "");

        // substitute dots for slashes and backslashes
        className = className.replace('/', '.');
        className = className.replace('\\', '.');

        if (className.startsWith(".")) {
            className = className.substring(1);
        }

        return className;
    }

    @FFDCIgnore(value = PrivilegedActionException.class)
    private static byte[] loadBytes(final InputStream is) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<byte[]>() {
                @Override
                @Trivial
                public byte[] run() throws IOException {

                    byte[] buf = new byte[2048];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        int bytesRead = is.read(buf);
                        while (bytesRead > -1) {
                            baos.write(buf, 0, bytesRead);
                            bytesRead = is.read(buf);
                        }
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                    return baos.toByteArray();
                }
            });
        } catch (PrivilegedActionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

    @Trivial
    Collection<Collection<URL>> getClassPath() {
        return smartClassPath.getClassPath();
    }

    static boolean exists(File f) {
        return System.getSecurityManager() == null ? f.exists() : AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return f.exists();
            }
        });
    }

    static boolean isDirectory(File f) {
        return System.getSecurityManager() == null ? f.isDirectory() : AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return f.isDirectory();
            }
        });
    }
}

