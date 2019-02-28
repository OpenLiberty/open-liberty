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
package com.ibm.ws.classloading.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
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
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.classloading.internal.util.ClassRedefiner;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.classloader.ClassLoaderHook;
import com.ibm.ws.kernel.boot.classloader.ClassLoaderHookFactory;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.util.CacheHashMap;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.utils.CompositeEnumeration;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

abstract class ContainerClassLoader extends IdentifiedLoader {
    static final TraceComponent tc = Tr.register(ContainerClassLoader.class);

    /**
     * This class will stop JARs from being cached when they are being read from. This will prevent java from keeping a stream open to the file and thus locking it so
     * the user cannot delete it. The class will do this disabling in a static initialization block so to be used it just needs to be loaded.
     */
    private static class JarCacheDisabler {
        static {
            try {
                URLConnection connection = new URL("jar:file://something.jar!/").openConnection();
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

    protected final ClassLoaderHook hook;

    final String jarProtocol;

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

    /**
     * A unifying interface to bridge ArtifactContainers, and adaptable Containers.
     */
    private interface UniversalContainer {
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
        void updatePackageMap(Map<Integer, List<UniversalContainer>> map);
    }

    /**
     * Computes the shared class cache URL from the resource URL.
     * 
     * If the URL is a jar protocol URL, then use it as is.
     * If it is a wsjar protocol URL, then change it to a jar protocol URL.
     * If it is a file protocol URL, confirm that the URL ends with the
     * class file name, and return the directory before the package
     * qualified class file name.
     * 
     * @param resourceURL The URL of the location of the class file.
     * @param resourceName The resource path of the class file. i.e. package/sub/MyClass.class
     * @return the URL to pass to the shared class cache, or null if protocol is wrong,
     *         or path doesn't include resourceName.
     */
    static URL getSharedClassCacheURL(URL resourceURL, String resourceName) {
        URL sharedClassCacheURL;
        if (resourceURL == null) {
            sharedClassCacheURL = null;
        } else {
            String protocol = resourceURL.getProtocol();
            if ("jar".equals(protocol)) {
                sharedClassCacheURL = resourceURL;
            } else if ("wsjar".equals(protocol)) {
                try {
                    sharedClassCacheURL = new URL(resourceURL.toExternalForm().substring(2));
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
        }
        return sharedClassCacheURL;
    }

    private static byte[] getClassBytesFromHook(UniversalContainer.UniversalResource resource, String className, String resourceName, ClassLoaderHook hook) {
        byte[] bytes = null;
        if (hook != null) {
            final URL resourceURL = resource.getResourceURL("jar");
            URL sharedClassCacheURL = getSharedClassCacheURL(resourceURL, resourceName);
            if (sharedClassCacheURL != null) {
                bytes = hook.loadClass(sharedClassCacheURL, className);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (bytes != null) {
                        Tr.debug(tc, "Found class in shared class cache", new Object[] {className, sharedClassCacheURL});
                    } else {
                        Tr.debug(tc, "Did not find class in shared class cache", new Object[] {className, sharedClassCacheURL});
                    }
                }
            }
        }
        return bytes;
    }

    /**
     * Implementation of UniversalResource backed by an adaptable Entry.
     */
    private static class EntryUniversalResource implements UniversalContainer.UniversalResource {
        final Container container;
        final Entry entry;
        final String resourceName;

        public EntryUniversalResource(Container container, Entry entry, String resourceName) {
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
            byte[] bytes = ContainerClassLoader.getClassBytesFromHook(this, className, resourceName, hook);

            boolean foundInClassCache = bytes != null;
            if (!foundInClassCache) {
                try {
                    InputStream is = this.entry.adapt(InputStream.class);
                    bytes = ContainerClassLoader.getBytes(is, (int) entry.getSize());
                } catch (UnableToAdaptException e) {
                    throw new IOException(e);
                }
            }
            return new EntryByteResourceInformation(bytes, this.entry, this.container, resourceName, foundInClassCache);
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
    }

    /**
     * Implementation of a UniversalContainer, backed by an adaptable Container.
     */
    private static class ContainerUniversalContainer implements UniversalContainer {
        private final Container container;
        private final boolean isRoot;
        private String debugString;

        public ContainerUniversalContainer(Container container) {
            this.container = container;
            this.isRoot = container.isRoot();
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
                return new EntryUniversalResource(this.container, e, path);
            } else {
                return null;
            }
        }

        private void processContainer(Container c, Map<Integer, List<UniversalContainer>> map, int chop) {
            for (Entry e : c) {
                try {
                    Container child = e.adapt(Container.class);
                    if (child != null && !child.isRoot()) {
                        Integer key = child.getPath().substring(chop).hashCode();
                        List<UniversalContainer> listForThisPath = map.get(key);
                        if (listForThisPath == null) {
                            listForThisPath = new ArrayList<UniversalContainer>();
                            map.put(key, listForThisPath);
                        }
                        if (!listForThisPath.contains(this)) {
                            listForThisPath.add(this);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "CCL: {" + listForThisPath.size() + "} [" + this.hashCode() + "] adding : [" + key + "] " + (child.getPath().substring(chop)));
                        }
                        processContainer(child, map, chop);
                    }
                } catch (UnableToAdaptException ex) {
                    //ignore.
                }
            }
        }

        @Override
        synchronized public void updatePackageMap(Map<Integer, List<UniversalContainer>> map) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "CCL: updating map for adaptable container with path " + this.container.getPath());
            //could speed this up using an adapter to access the underlying artifact container to use localOnly..
            //we'll keep it simple for now though and just use the existing adaptable api layer.
            int chop = 1;
            if (!"/".equals(this.container.getPath())) {
                chop = this.container.getPath().length() + 1; //we add 1 to remove the leading slash from entries below this.
            }
            processContainer(this.container, map, chop);
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
    }

    /**
     * Implementation of a UniversalResource backed by an ArtifactEntry
     */
    private static class ArtifactEntryUniversalResource implements UniversalContainer.UniversalResource {
        final ArtifactContainer container;
        final ArtifactEntry entry;
        final String resourceName;

        public ArtifactEntryUniversalResource(ArtifactContainer container, ArtifactEntry entry, String resourceName) {
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
            byte[] bytes = ContainerClassLoader.getClassBytesFromHook(this, className, resourceName, hook);

            boolean foundInClassCache = bytes != null;
            if (!foundInClassCache) {
                InputStream is = this.entry.getInputStream();
                bytes = ContainerClassLoader.getBytes(is, (int) entry.getSize());
            }
            return new ArtifactEntryByteResourceInformation(bytes, this.entry, this.container, resourceName, foundInClassCache);
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
    }

    /**
     * Implementation of a UniversalContainer, backed by an ArtifactContainer.
     */
    private static class ArtifactContainerUniversalContainer implements UniversalContainer {
        final ArtifactContainer container;
        final boolean isRoot;

        public ArtifactContainerUniversalContainer(ArtifactContainer container) {
            this.container = container;
            this.isRoot = container.isRoot();
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
                    return new ArtifactEntryUniversalResource(this.container, e, path);
                } else {
                    return null;
                }
            } catch (IllegalArgumentException e) {
                //getEntry can throw this for paths like .. on a root container
                //for us, it just means 'not found'.
                return null;
            }
        }

        private void processContainer(ArtifactContainer c, Map<Integer, List<UniversalContainer>> map, int chop) {
            for (ArtifactEntry e : c) {
                ArtifactContainer child = e.convertToContainer(true);
                if (child != null) {
                    Integer key = child.getPath().substring(chop).hashCode();
                    List<UniversalContainer> listForThisPath = map.get(key);
                    if (listForThisPath == null) {
                        listForThisPath = new ArrayList<UniversalContainer>();
                        map.put(key, listForThisPath);
                    }
                    if (!listForThisPath.contains(this)) {
                        listForThisPath.add(this);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "CCL: {" + listForThisPath.size() + "} [" + this.hashCode() + "] adding : [" + key + "] " + (child.getPath().substring(chop)));
                    }
                    processContainer(child, map, chop);
                }
            }
        }

        @Override
        synchronized public void updatePackageMap(Map<Integer, List<UniversalContainer>> map) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "CCL: updating map for artifact container with path " + this.container.getPath());
            int chop = 1;
            if (!"/".equals(this.container.getPath())) {
                chop = this.container.getPath().length() + 1; //we add 1 to remove the leading slash from entries below this.
            }
            processContainer(container, map, chop);
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

        ByteResourceInformation getByteResourceInformation(String className, String path, ClassLoaderHook hook) throws IOException;

        URL getResourceURL(String path, String jarProtocol);

        Collection<URL> getResourceURLs(String path, String jarProtocol);

        boolean containsContainer(Container container);
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

        final List<UniversalContainer> classPath = new CopyOnWriteArrayList<UniversalContainer>();
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
        final Map<Integer, List<UniversalContainer>> packageMap = usePackageMap ? new HashMap<Integer, List<UniversalContainer>>() : null;

        final Set<Container> containers = Collections.newSetFromMap(new WeakHashMap<Container, Boolean>());

        /**
         * Internal method to add a new UniversalContainer to the list.
         *
         * @param uc
         */
        @SuppressWarnings("deprecation")
        private synchronized void addUniversalContainers(final UniversalContainer uc) {
            if (tc.isDebugEnabled()) {
                // Debug info for classpath elements as they are added.. candidate for Trace.debug.
                if (uc instanceof ArtifactContainerUniversalContainer) {
                    Tr.debug(tc, "CCL: " + this.hashCode() + " cpelt idx " + classPath.size() + "wraps " + ((ArtifactContainerUniversalContainer) uc).container);
                    Tr.debug(tc, "CCL: " + this.hashCode() + " cpelt idx " + classPath.size() + " ART url "
                                 + ((ArtifactContainerUniversalContainer) uc).container.getPhysicalPath());
                } else {
                    Tr.debug(tc, "CCL: " + this.hashCode() + " cpelt idx " + classPath.size() + " wraps " + ((ContainerUniversalContainer) uc).container);
                    Tr.debug(tc, "CCL: " + this.hashCode() + " cpelt idx " + classPath.size() + " CON url " + ((ContainerUniversalContainer) uc).container.getPhysicalPath());
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
                            uc.updatePackageMap(packageMap);
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
            classPath.add(uc);
            lastNotFound.add(Collections.synchronizedSet(new LinkedHashSet<String>()));
        }

        @Override
        public void addContainer(Container container) {
            containers.add(container);
            addUniversalContainers(new ContainerUniversalContainer(container));
        }

        @Override
        public void addArtifactContainer(ArtifactContainer container) {
            addUniversalContainers(new ArtifactContainerUniversalContainer(container));
        }

        private List<UniversalContainer> getUniversalContainersForPath(String path, List<UniversalContainer> classpath) {
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
                List<UniversalContainer> containersForKey;
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
                    return Collections.emptyList();
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
            List<UniversalContainer> locationsToCheck = classPath;
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

            List<UniversalContainer> locationsToCheck = classPath;
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

            List<UniversalContainer> locationsToCheck = classPath;
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
            return String.valueOf(classPath);
        }

        @Override
        public boolean containsContainer(Container container) {
            return containers.contains(container);
        }
    }

    /**
     * A wrapper for the {@link SmartClassPathImpl} that passes all write operations
     * through to the underlying {@link SmartClassPathImpl} but calls {@link ContainerClassLoader#lazyInit()} and then "unwraps" itself
     * before the first read operation.
     */
    private class UnreadSmartClassPath implements SmartClassPath {
        SmartClassPathImpl delegate;

        UnreadSmartClassPath() {
            delegate = new SmartClassPathImpl();
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
    }

    /**
     * Interface that represents byte data for a resource.<p>
     * A data structure that stores the resource URL and bytes for a particular class.<br>
     * It also has a utility to try to load a manifest from the resource URL (assuming it points to a JAR)<br>
     */
    protected interface ByteResourceInformation {
        /**
         * Returns the bytes for the class loaded from this resource.
         *
         * @return The byte[]
         */
        byte[] getBytes();

        /**
         * Attempts to load the manifest for the current resource URL and returns it.
         *
         * @return The manifest or <code>null</code> if an error occurred loading it (or it didn't exist)
         */
        Manifest getManifest();

        /**
         * Returns the resource URL for this resource
         *
         * @return
         */
        public URL getResourceUrl();

        /**
         * Returns the resource style path to this resource, this will be in the form "a/b/c" rather than a . notation.
         *
         * @return The resource path
         */
        public String getResourcePath();

        /**
         * Returns whether the class was found in the shared class cache or not.  If it is found in the cache,
         * there is no need to call the cache to store the class again.
         * 
         * @return whether the Class was found in the shared class cache or not.
         */
        public boolean foundInClassCache();
    }

    /**
     * Implementation of {@link ByteResourceInformation} backed by an Entry. <p>
     * Enables use of the Container for MANIFEST.MF location.
     */
    static class EntryByteResourceInformation implements ByteResourceInformation {
        private final byte[] bytes;
        private final Entry resourceEntry;
        private final Container resourceContainer;
        private final String resourcePath;
        private final boolean fromClassCache;

        private Manifest manifest;
        private boolean manifestLoaded;

        /**
         * @param bytes
         * @param resourceUrl
         */
        EntryByteResourceInformation(byte[] bytes, Entry resourceUrl, Container root, String resourcePath, boolean fromClassCache) {
            this.bytes = bytes;
            this.resourceEntry = resourceUrl;
            this.resourceContainer = root;
            this.resourcePath = resourcePath;
            this.fromClassCache = fromClassCache;
        }

        /**
         * Returns the bytes for the class loaded from this resource.
         *
         * @return The byte[]
         */
        @Override
        public byte[] getBytes() {
            return this.bytes;
        }

        /**
         * Attempts to load the manifest for the current resource URL and returns it.
         *
         * @return The manifest or <code>null</code> if an error occurred loading it (or it didn't exist)
         */
        @Override
        @FFDCIgnore(value = { IOException.class })
        public Manifest getManifest() {
            // See if we've already loaded the manifest
            if (!this.manifestLoaded) {
                // No matter what happens set the boolean to true, if we fail to load a manifest we won't succeed next time so don't waste time trying, just return null
                this.manifestLoaded = true;

                Entry e = this.resourceContainer.getEntry("META-INF/MANIFEST.MF");
                if (e != null) {
                    InputStream manifestStream = null;
                    try {
                        manifestStream = e.adapt(InputStream.class);
                        if (manifestStream != null) {
                            Manifest manifestLoading = new Manifest(manifestStream);
                            this.manifest = manifestLoading;
                        }
                    } catch (UnableToAdaptException e1) {
                        // Ignore, we'll just define a package with no package information
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "UnableToAdaptException thrown opening resource {0}", this.resourceEntry.getResource());
                        }
                    } catch (IOException e2) {
                        // Ignore, we'll just define a package with no package information
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "IOException thrown opening resource {0}", this.resourceEntry.getResource());
                        }
                    } finally {
                        Util.tryToClose(manifestStream);
                    }
                }
            }

            return this.manifest;
        }

        /**
         * Returns the resource URL for this resource
         *
         * @return
         */
        @Override
        public URL getResourceUrl() {
            return this.resourceEntry.getResource();
        }

        /**
         * Returns the resource style path to this resource, this will be in the form "a/b/c" rather than a . notation.
         *
         * @return The resource path
         */
        @Override
        public String getResourcePath() {
            return this.resourcePath;
        }

        @Override
        public boolean foundInClassCache() {
            return fromClassCache;
        }
    }

    /**
     * Implementation of {@link ByteResourceInformation} backed by an ArtifactEntry. <p>
     * Enables use of the ArtifactContainer for MANIFEST.MF location.
     */
    static class ArtifactEntryByteResourceInformation implements ByteResourceInformation {
        private final byte[] bytes;
        private final ArtifactEntry resourceEntry;
        private final ArtifactContainer resourceContainer;
        private final String resourcePath;
        private final boolean fromClassCache;

        private Manifest manifest;
        private boolean manifestLoaded;

        /**
         * @param bytes
         * @param resourceUrl
         */
        ArtifactEntryByteResourceInformation(byte[] bytes, ArtifactEntry resourceUrl, ArtifactContainer root, String resourcePath, boolean fromClassCache) {
            this.bytes = bytes;
            this.resourceEntry = resourceUrl;
            this.resourceContainer = root;
            this.resourcePath = resourcePath;
            this.fromClassCache = fromClassCache;
        }

        /**
         * Returns the bytes for the class loaded from this resource.
         *
         * @return The byte[]
         */
        @Override
        public byte[] getBytes() {
            return this.bytes;
        }

        /**
         * Attempts to load the manifest for the current resource URL and returns it.
         *
         * @return The manifest or <code>null</code> if an error occurred loading it (or it didn't exist)
         */
        @Override
        @FFDCIgnore(value = { IOException.class })
        public Manifest getManifest() {
            // See if we've already loaded the manifest
            if (!this.manifestLoaded) {
                // No matter what happens set the boolean to true, if we fail to load a manifest we won't succeed next time so don't waste time trying, just return null
                this.manifestLoaded = true;

                ArtifactEntry e = this.resourceContainer.getEntry("META-INF/MANIFEST.MF");
                if (e != null) {
                    InputStream manifestStream = null;
                    try {
                        manifestStream = e.getInputStream();
                        if (manifestStream != null) {
                            Manifest manifestLoading = new Manifest(manifestStream);
                            this.manifest = manifestLoading;
                        }
                    } catch (IOException e2) {
                        // Ignore, we'll just define a package with no package information
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "IOException thrown opening resource {0}", this.resourceEntry.getResource());
                        }
                    } finally {
                        Util.tryToClose(manifestStream);
                    }
                }
            }

            return this.manifest;
        }

        /**
         * Returns the resource URL for this resource
         *
         * @return
         */
        @Override
        public URL getResourceUrl() {
            return this.resourceEntry.getResource();
        }

        /**
         * Returns the resource style path to this resource, this will be in the form "a/b/c" rather than a . notation.
         *
         * @return The resource path
         */
        @Override
        public String getResourcePath() {
            return this.resourcePath;
        }

        @Override
        public boolean foundInClassCache() {
            return fromClassCache;
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

        hook = ClassLoaderHookFactory.getClassLoaderHook(this);

        smartClassPath = new UnreadSmartClassPath();

        if (classpath != null) {
            for (Container c : classpath) {
                smartClassPath.addContainer(c);
            }
        }

        this.redefiner = redefiner;
    }

    @Override
    public URL findResource(String name) {
        //check super first, which checks parent, if any.
        URL url = super.findResource(name);
        if (url != null) {
            return url;
        }
        url = smartClassPath.getResourceURL(name, jarProtocol);

        //no need to retry smartClassPath with trailing / it already dealt with that.
        if (url == null && !name.endsWith("/")) {
            url = super.findResource(name);
            if (url != null)
                url = stripTrailingSlash(url);
        }

        return url;
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

    protected ByteResourceInformation findClassBytes(String className, String resourceName) throws IOException {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return smartClassPath.getByteResourceInformation(className, resourceName, hook);
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    //define package impl, that uses package sealing information as defined on wikipedia
    //to set vars passed up to ClassLoader.definePackage.
    public Package definePackage(String name, Manifest manifest, URL sealBase) throws IllegalArgumentException {
        Attributes mA = manifest.getMainAttributes();
        String specTitle = mA.getValue(Name.SPECIFICATION_TITLE);
        String specVersion = mA.getValue(Name.SPECIFICATION_VERSION);
        String specVendor = mA.getValue(Name.SPECIFICATION_VENDOR);
        String implTitle = mA.getValue(Name.IMPLEMENTATION_TITLE);
        String implVersion = mA.getValue(Name.IMPLEMENTATION_VERSION);
        String implVendor = mA.getValue(Name.IMPLEMENTATION_VENDOR);
        String sealedString = mA.getValue(Name.SEALED);
        Boolean sealed = (sealedString == null ? Boolean.FALSE : sealedString.equalsIgnoreCase("true"));

        //now overwrite global attributes with the specific attributes
        String unixName = name.replaceAll("\\.", "/") + "/"; //replace all dots with slash and add trailing slash
        mA = manifest.getAttributes(unixName);
        if (mA != null) {
            String s = mA.getValue(Name.SPECIFICATION_TITLE);
            if (s != null)
                specTitle = s;
            s = mA.getValue(Name.SPECIFICATION_VERSION);
            if (s != null)
                specVersion = s;
            s = mA.getValue(Name.SPECIFICATION_VENDOR);
            if (s != null)
                specVendor = s;
            s = mA.getValue(Name.IMPLEMENTATION_TITLE);
            if (s != null)
                implTitle = s;
            s = mA.getValue(Name.IMPLEMENTATION_VERSION);
            if (s != null)
                implVersion = s;
            s = mA.getValue(Name.IMPLEMENTATION_VENDOR);
            if (s != null)
                implVendor = s;
            s = mA.getValue(Name.SEALED);
            if (s != null)
                sealed = s.equalsIgnoreCase("true");
        }

        if (!sealed)
            sealBase = null;
        return definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    /**
     * Add all the artifact containers to the class path
     */
    protected void addToClassPath(Iterable<ArtifactContainer> artifacts) {

        for (ArtifactContainer art : artifacts) {
            smartClassPath.addArtifactContainer(art);
        }
    }

    /**
     * Method to allow adding shared libraries to this classloader, currently using File.
     *
     * @param f the File to add as a shared lib.. can be a dir or a jar (or a loose xml ;p)
     */
    @FFDCIgnore(NullPointerException.class)
    protected void addLibraryFile(File f) {

        if (!!!f.exists()) {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "cls.library.archive", f, new FileNotFoundException(f.getName()));
            }
            return;
        }

        // Skip files that are not archives of some sort.
        if (!f.isDirectory() && !isArchive(f))
            return;

        //this area subject to refactor following shared lib rework..
        //ideally the shared lib code will start passing us ArtifactContainers, and it
        //will own the management of the ACF via DS.

        //NASTY.. need to use DS to get the ACF, not OSGi backdoor ;p
        BundleContext bc = FrameworkUtil.getBundle(ContainerClassLoader.class).getBundleContext();
        ServiceReference<ArtifactContainerFactory> acfsr = bc.getServiceReference(ArtifactContainerFactory.class);
        if (acfsr != null) {
            ArtifactContainerFactory acf = bc.getService(acfsr);
            if (acf != null) {
                //NASTY.. using this bundle as the cache dir location for the data file..
                try {
                    ArtifactContainer ac = acf.getContainer(bc.getBundle().getDataFile(""), f);
                    smartClassPath.addArtifactContainer(ac);
                } catch (NullPointerException e) {
                    // TODO completed under task 74097
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception while adding files to classpath", e);
                    }
                    if (tc.isInfoEnabled()) {
                        Tr.info(tc, "cls.library.file.forbidden", f);
                    }
                }
            }
        }
    }

    protected void addNativeLibraryContainer(Container container) {
        nativeLibraryContainers.add(new ContainerUniversalContainer(container));
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
}
