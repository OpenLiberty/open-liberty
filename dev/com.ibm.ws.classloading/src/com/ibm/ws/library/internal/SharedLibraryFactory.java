/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
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
package com.ibm.ws.library.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.LibraryAccess;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.equinox.module.ModuleDelegateClassLoaderFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.library.Library;

@Component(service = { ManagedServiceFactory.class, ModuleDelegateClassLoaderFactory.class, LibraryAccess.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                        Constants.SERVICE_VENDOR + "=" + "IBM",
                        Constants.SERVICE_PID + "=" + SharedLibraryConstants.SERVICE_PID
           })
public class SharedLibraryFactory implements ManagedServiceFactory, ModuleDelegateClassLoaderFactory, LibraryAccess {

    /**
     * The directory will contains sub dirs which are the cache location for containers.
     */
    public static final String CONT_CACHE = "libcont";

    private static final TraceComponent tc = Tr.register(SharedLibraryFactory.class);

    private final ConcurrentMap<String, SharedLibraryImpl> instances = new ConcurrentHashMap<String, SharedLibraryImpl>();

    private final BundleContext ctx;
    private final ClassLoadingService classLoadingService;
    private final ConfigurationAdmin configAdmin;
    private final String resolvedBasePath;
    private final AtomicInteger rankingCounter = new AtomicInteger(0);

    private final ArtifactContainerFactory artifactContainerFactory;
    private final LibraryPackageExporter packageExporter;

    @Activate
    public SharedLibraryFactory(BundleContext ctx, //
                                @Reference ArtifactContainerFactory acf, //
                                @Reference ClassLoadingService cls, //
                                @Reference ConfigurationAdmin configAdmin, //
                                @Reference WsLocationAdmin locationService, //
                                @Reference RegionDigraph digraph
                                ) {
        this.ctx = ctx;
        this.artifactContainerFactory = acf;
        this.classLoadingService = cls;
        this.configAdmin = configAdmin;
        this.resolvedBasePath = locationService.resolveString(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR);

        clearContainerCache();
        File f = this.ctx.getDataFile(CONT_CACHE);
        boolean ok = f.mkdir();
        if (ok != true) {
            if (!FrameworkState.isStopping()) {
                if (tc.isErrorEnabled()) {
                    Tr.error(tc, "slf.no.cache");
                }
            }
        }

        packageExporter = new LibraryPackageExporter(ctx, digraph);
    }

    /**
     * @return
     */
    private File clearContainerCache() {
        File f = this.ctx.getDataFile(CONT_CACHE);
        try {
            delete(f);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // e.printStackTrace();
        }
        return f;
    }

    void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            if (f.exists()) {
                if (tc.isWarningEnabled()) {
                    Tr.warning(tc, "slf.failed.delete", f.getAbsolutePath());
                }

            }
    }

    @Deactivate
    protected void deactivate(BundleContext ctx) {
        for (SharedLibraryImpl instance : instances.values()) {
            instance.delete();
        }
        instances.clear();
        clearContainerCache();
    }

    @Override
    public void deleted(String pid) {
        SharedLibraryImpl instance = instances.remove(pid);
        if (instance != null) {
            instance.delete();
        }
    }

    @Override
    public String getName() {
        return "SharedLibrary";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
        if (FrameworkState.isStopping()) {
            // Shutting down, just return
            return;
        }

        // try to find an instance in the map
        SharedLibraryImpl existingInstance = instances.get(pid);

        //increment ranking counter - this helps avoid situations where two libraries share the same name
        // which happens most often when the id is not specified and a default id is provided
        int ranking = rankingCounter.getAndIncrement();
        properties.put("service.ranking", ranking);

        // CREATE if there wasn't an instance
        if (existingInstance == null) {
            SharedLibraryImpl newInstance = new SharedLibraryImpl(ctx, classLoadingService, configAdmin, resolvedBasePath, new ArtifactContainerFactory() {

                @Override
                public ArtifactContainer getContainer(File workAreaCacheDir, ArtifactContainer parent, ArtifactEntry entry, Object o) {
                    if (SharedLibraryFactory.this.artifactContainerFactory != null) {
                        return SharedLibraryFactory.this.artifactContainerFactory.getContainer(workAreaCacheDir, parent, entry, o);
                    }

                    if (!FrameworkState.isStopping()) {
                        if (tc.isErrorEnabled()) {
                            Tr.error(tc, "slf.no.acf");
                        }
                    }

                    return null;
                }

                @Override
                public ArtifactContainer getContainer(File workAreaCacheDir, Object o) {
                    if (SharedLibraryFactory.this.artifactContainerFactory != null) {
                        return SharedLibraryFactory.this.artifactContainerFactory.getContainer(workAreaCacheDir, o);
                    }

                    if (!FrameworkState.isStopping()) {
                        if (tc.isErrorEnabled()) {
                            Tr.error(tc, "slf.no.acf");
                        }
                    }
                    return null;
                }
            }, packageExporter);
            // sync so that putIfAbsent() and registerServices() are atomic
            synchronized (newInstance) {
                existingInstance = instances.putIfAbsent(pid, newInstance);
                // Only register the library if it was successfully inserted into the map.
                if (existingInstance == null) {
                    newInstance.update(properties);
                    return;
                }
                // Otherwise, fall through to the update code below
            }
        }

        // UPDATE if there was already an instance
        synchronized (existingInstance) {
            // synchronized so we know inserted instance is
            existingInstance.update(properties);
        }
    }

    @Override
    public ClassLoader getDelegateClassLoader(Bundle bundle) {
        // delegate to the package exporter.
        return packageExporter.getDelegateClassLoader(bundle);
    }

    @Override
    public void setPackages(Library library, Collection<String> packageNames, PackageVisibility visibility) {
        // delegate to the package exporter
        packageExporter.setPackages(library, packageNames, visibility);
    }
}
