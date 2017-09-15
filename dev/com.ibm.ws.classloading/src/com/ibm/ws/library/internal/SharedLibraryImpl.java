/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.library.internal;

import static com.ibm.ws.classloading.internal.ClassLoadingConstants.GLOBAL_SHARED_LIBRARY_ID;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.library.internal.SharedLibraryConstants.SharedLibraryAttribute;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.config.FilesetChangeListener;
import com.ibm.wsspi.kernel.service.utils.PathUtils;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.library.LibraryChangeListener;

/**
 * This class defines the <library> element implementation in the server.xml allowing users
 * to specify shared libraries used by web applications
 */
public class SharedLibraryImpl implements Library {
    private static final TraceComponent tc = Tr.register(SharedLibraryImpl.class);

    private static final LibraryChangeListener[] EMPTY_LIBRARY_LISTENERS = {};

    private volatile boolean deleted;

    /**
     * A tracker for listeners interested in changes to this library
     */
    private volatile ServiceTracker<LibraryChangeListener, LibraryChangeListener> libraryListenersTracker = null;

    /**
     * the service registration for this Library
     */
    private volatile ServiceRegistration<Library> libraryReg = null;

    final BundleContext ctx;
    private final ClassLoadingService classLoadingService;
    private final ConfigurationAdmin configAdmin;
    private final String resolvedBasePath;

    final Object generationLock = new Object() {};

    private volatile LibraryGeneration currentGeneration = null;
    private LibraryGeneration nextGeneration = null;

    final ArtifactContainerFactory artifactContainerFactory;

    private final LibraryPackageExporter packageExporter;

    SharedLibraryImpl(BundleContext ctx, ClassLoadingService classLoadingService, ConfigurationAdmin configAdmin, String resolvedBasePath,
                      ArtifactContainerFactory artifactContainerFactory, LibraryPackageExporter packageExporter) {
        this.ctx = ctx;
        this.classLoadingService = classLoadingService;
        this.configAdmin = configAdmin;
        this.resolvedBasePath = resolvedBasePath;
        this.artifactContainerFactory = artifactContainerFactory;
        this.packageExporter = packageExporter;
    }

    boolean isDeleted() {
        return deleted;
    }

    // called from SharedLibraryFactory inside a synchronized block over this instance
    void update(Dictionary<String, Object> props) {
        if (deleted) {
            return;
        }

        final String instancePid = (String) props.get("service.pid");
        final String instanceId = (String) props.get("id");
        if (instanceId == null) {
            Tr.error(tc, "cls.library.id.missing");
        }
        if (libraryListenersTracker == null) {
            //set up a tracker for notification listeners for this library
            Filter listenerFilter = null;
            // To better support parent first nested config, also filter on libraryRef,
            // which will be a list of pid (or, if cardinality=0, then just a pid).
            try {
                listenerFilter = FrameworkUtil.createFilter("(&(objectClass=" + LibraryChangeListener.class.getName() +
                                                            ")(|(library=" + instanceId + ")(libraryRef=*" + instancePid + "*)))");
            } catch (InvalidSyntaxException e) {
                //auto FFDC because the syntax shouldn't be wrong!
                Tr.error(tc, "cls.library.id.invalid", instanceId, e.toString());
            }

            ServiceTracker<LibraryChangeListener, LibraryChangeListener> tracker;
            tracker = new ServiceTracker<LibraryChangeListener, LibraryChangeListener>(this.ctx, listenerFilter, null);
            tracker.open();
            libraryListenersTracker = tracker;
        }

        LibraryGeneration nextGen;
        synchronized (generationLock) {
            nextGen = nextGeneration;
            if (nextGen != null) {
                // there is already a new generation in the process of being updated.
                // since it is using stale properties we want to cancel that generation
                // and start a new one.
                nextGeneration = null;
                nextGen.cancel();
            }
            nextGeneration = nextGen = new LibraryGeneration(this, instanceId, props);
        }
        nextGen.fetchFilesets();
    }

    void delete() {
        deleted = true;

        if (libraryListenersTracker != null) {
            libraryListenersTracker.close();
            libraryListenersTracker = null;
        }

        synchronized (generationLock) {
            LibraryGeneration libGen = nextGeneration;
            if (libGen != null) {
                nextGeneration = null;
                libGen.cancel();
            }
            libGen = currentGeneration;
            if (libGen != null) {
                currentGeneration = null;
                libGen.cancel();
            }
        }

        if (libraryReg != null) {
            libraryReg.unregister();
            libraryReg = null;
        }
        // make sure to delete any package exporters
        packageExporter.delete(this);
    }

    @Override
    public String id() {
        final LibraryGeneration currentGen = currentGeneration;
        return currentGen != null ? currentGen.getLibraryId() : null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoadingService.getSharedLibraryClassLoader(this);
    }

    @Override
    public EnumSet<ApiType> getApiTypeVisibility() {
        final LibraryGeneration currentGen = currentGeneration;
        return currentGen == null ? null : currentGen.getApiTypeVisibility();
    }

    @Override
    public String toString() {
        final LibraryGeneration currentGen = currentGeneration;
        String genStr = currentGen == null ? "<uninitialized>" : currentGen.getLibraryId();
        return String.format("Shared Library %s @ %x", genStr, hashCode());
    }

    @Override
    public Collection<File> getFiles() {
        final LibraryGeneration currentGen = currentGeneration;
        return currentGen == null ? null : currentGen.getFiles();
    }

    @Override
    public Collection<File> getFolders() {
        final LibraryGeneration currentGen = currentGeneration;
        return currentGen == null ? null : currentGen.getFolders();
    }

    @Override
    public Collection<Fileset> getFilesets() {
        final LibraryGeneration currentGen = currentGeneration;
        return currentGen == null ? null : currentGen.getFilesets();
    }

    @Override
    public Collection<ArtifactContainer> getContainers() {
        final LibraryGeneration currentGen = currentGeneration;
        return currentGen == null ? null : currentGen.getContainers();
    }

    void setLibraryServiceProperties(Dictionary<String, Object> svcProps) {
        if (deleted) {
            return;
        }

        final ServiceRegistration<Library> svcReg;
        if (libraryReg == null) {
            svcReg = libraryReg = this.ctx.registerService(Library.class, this, svcProps);
        } else {
            svcReg = libraryReg;
            svcReg.setProperties(svcProps);
            // something has changed with the library, we must refresh any exporters
            // so they can get a new library class loader
            packageExporter.refreshExporters(this);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "svc prop keys are " + Arrays.toString(svcReg.getReference().getPropertyKeys()));
        }
    }

    void notifyListeners() {
        if (deleted) {
            return;
        }

        // Notify those interested in this library that something has changed
        final ServiceTracker<LibraryChangeListener, LibraryChangeListener> ls = libraryListenersTracker;
        if (ls == null) {
            return;
        }

        for (LibraryChangeListener listener : ls.getServices(EMPTY_LIBRARY_LISTENERS)) {
            if (deleted) {
                return;
            }

            if (listener == null) {
                continue;
            }

            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "notifying listener: " + listener);
                }
                listener.libraryNotification();
            } catch (Exception e) {
                // Swallow the error so that others may continue
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "caught exception from listener: " + listener, e);
                }
            }
        }
    }

    Collection<File> retrieveFiles(String[] pids, String displayId) {
        if (pids == null || pids.length == 0) {
            return Collections.emptyList();
        }

        ArrayList<File> result = new ArrayList<File>();
        for (String pid : pids) {
            try {
                Configuration config = configAdmin.getConfiguration(pid);
                Dictionary<String, Object> configProps = config.getProperties();
                if (configProps == null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "setFiles: Configuration not found for " + pid);
                    }
                    config.delete();
                } else {
                    String fileName = (String) configProps.get("name");
                    File f = null;
                    if (fileName != null && !!!fileName.isEmpty()) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "setFiles: found" + fileName);
                        }

                        f = new File(normalizePath(fileName));
                        if (f.exists() && !!!f.isDirectory()) {
                            result.add(f);
                        }
                    }

                    if (tc.isWarningEnabled()) {
                        if (f == null || f.isDirectory() || !!!f.exists()) {
                            Tr.warning(tc, "cls.library.file.invalid", displayId, fileName);
                        }
                    }
                }
            } catch (IOException ignored) {
                // auto-FFDC this exception
            }
        }
        return result;
    }

    Collection<File> retrieveFolders(String instanceId, String[] pids, String displayId) {
        if (pids == null || pids.length == 0) {
            return Collections.emptyList();
        }

        ArrayList<File> result = new ArrayList<File>();
        for (String pid : pids) {
            try {
                Configuration config = configAdmin.getConfiguration(pid);
                Dictionary<String, Object> configProps = config.getProperties();
                if (configProps == null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "setFolders: Configuration not found for " + pid);
                    }
                    config.delete();
                } else {
                    String dir = (String) configProps.get("dir");
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "setFolders: Found " + dir);
                    }

                    File f = null;
                    if (dir != null && false == "".equals(dir)) {
                        f = new File(normalizePath(dir));
                        if (f.isDirectory()) {
                            result.add(f);
                        }
                    }

                    // Issue a warning if any library defines a folder which isn't a directory,
                    // or if any library other than the global one defines a folder which doesn't exist.
                    if (tc.isWarningEnabled()) {
                        if (f == null
                            || (f.exists() && !!!f.isDirectory())
                            || (!!!f.exists() && !!!GLOBAL_SHARED_LIBRARY_ID.equals(instanceId)))
                            Tr.warning(tc, "cls.library.folder.invalid", displayId, dir);
                    }
                }
            } catch (IOException ignored) {
                // auto-FFDC this exception
            }
        }
        return result;
    }

    private String normalizePath(String path) {
        String result = PathUtils.slashify(path);
        if (!PathUtils.pathIsAbsolute(result)) {
            result = resolvedBasePath + result;
        }
        result = PathUtils.normalize(result);
        return result;
    }

    void publishGeneration(LibraryGeneration libraryGeneration) {
        synchronized (generationLock) {
            if (libraryGeneration.isCancelled()) {
                return;
            }
            final LibraryGeneration libGen = currentGeneration;
            currentGeneration = libraryGeneration;
            if (libGen != null) {
                libGen.cancel();
            }
        }
        setLibraryServiceProperties(libraryGeneration.getProperties());
        notifyListeners();
    }
}

