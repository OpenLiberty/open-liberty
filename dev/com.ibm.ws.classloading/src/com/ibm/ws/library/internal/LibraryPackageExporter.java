/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.library.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

import com.ibm.ws.classloading.LibraryAccess;
import com.ibm.ws.classloading.internal.LibertyLoader;
import com.ibm.ws.dynamic.bundle.BundleFactory;
import com.ibm.ws.kernel.feature.ApiRegion;
import com.ibm.wsspi.kernel.equinox.module.ModuleDelegateClassLoaderFactory;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.library.Library;

/**
 * The library package exporter is responsible for configuring packages with a shared library
 * so that they can be imported by OSGi bundles.
 */
public class LibraryPackageExporter implements ModuleDelegateClassLoaderFactory, LibraryAccess {
    private static final String EXPORTER_POSTFIX = "_library_exporter";
    private static final String REGION_EXPORT_TO_LIBERTY = "liberty.library.export.to.liberty";
    private static final String REGION_EXPORT_TO_OSGI_APPS = "liberty.library.export.to.osgi.app";
    private final String REGION_KERNEL;

    /*
     * LibraryVisibility manages the installation/update of the exporting bundles for PackageVisibility types
     */
    private class LibraryVisibility {
        // a mapping of PackageVisibility to the bundle that exports the configured packages. 
        private final EnumMap<PackageVisibility, Bundle> packageExporters = new EnumMap<PackageVisibility, Bundle>(PackageVisibility.class);
        // each library visibility gets a unique id so we can ensure 
        // unique symbolic names of the bundles associated bundles
        private final long id = counter.incrementAndGet();

        void update(Library library, Collection<String> packageNames, PackageVisibility visibility) {
            if (packageNames == null || packageNames.isEmpty()) {
                // uninstall the exporting bundle in this case.
                uninstallExporter(visibility);
            } else {
                updateExporter(library, packageNames, visibility);
            }
        }

        private void uninstallExporter(PackageVisibility visibility) {
            Bundle existing;
            synchronized (this) {
                // uninstall the exporting bundle for the specified visibility
                existing = packageExporters.remove(visibility);
                if (existing != null) {
                    bundleToLibrary.remove(existing);
                }
            }
            if (existing != null) {
                try {
                    existing.uninstall();
                } catch (BundleException e) {
                    // auto-FFDC is fine
                }
                refresh(Collections.singletonList(existing));
            }
        }

        private void updateExporter(Library library, Collection<String> packageNames, PackageVisibility visibility) {
            LibertyLoader loader = (LibertyLoader) library.getClassLoader();
            Bundle gatewayBundle = loader.getBundle();

            try {
                Region region = getLibraryExporterRegion(digraph, visibility);
                // make sure we have a unique postfix for this visibility
                String exporterPostfix = EXPORTER_POSTFIX + "_" + id;
                Bundle result = new BundleFactory()
                                .setBundleContext(context)
                                .setBundleLocationPrefix("")
                                .setBundleLocation(gatewayBundle.getLocation() + exporterPostfix)
                                .setBundleSymbolicName(gatewayBundle.getSymbolicName() + exporterPostfix)
                                .setBundleVersion(gatewayBundle.getVersion())
                                .setBundleName(gatewayBundle.getHeaders("").get(Constants.BUNDLE_NAME) + exporterPostfix)
                                .addAttributeValues(Constants.EXPORT_PACKAGE, packageNames.toArray())
                                .setRegion(region)
                                .createBundle();
                result.adapt(BundleStartLevel.class).setStartLevel(1);
                // note that we want to maintain an atomic operation when updating
                // the maps containing the exporting bundle
                synchronized (this) {
                    bundleToLibrary.put(result, library);
                    packageExporters.put(visibility, result);
                }
                // always refresh here in case of an existing installation.
                // BundleFactory does an update in that case
                refresh(Collections.singletonList(result));
            } catch (BundleException e) {
                // auto FFDC is fine here
            }
        }

        void delete() {
            for (PackageVisibility visibility : PackageVisibility.values()) {
                uninstallExporter(visibility);
            }
        }

        void refreshExporters() {
            Collection<Bundle> toRefresh;
            synchronized (this) {
                toRefresh = new ArrayList<Bundle>(packageExporters.values());
            }
            refresh(toRefresh);
        }
    }

    private final AtomicLong counter = new AtomicLong();
    private final BundleContext context;
    private final RegionDigraph digraph;
    // bundle to library mapping; used to answer the getDelegateClassLoader by getting the library class loader
    private final ConcurrentMap<Bundle, Library> bundleToLibrary = new ConcurrentHashMap<Bundle, Library>();
    // library to LibraryVisibility mapping.
    private final ConcurrentMap<Library, LibraryVisibility> libraryToVisibility = new ConcurrentHashMap<Library, LibraryVisibility>();

    public LibraryPackageExporter(BundleContext context, RegionDigraph digraph) {
        super();
        this.context = context;
        this.digraph = digraph;
        // This bundle is in the kernel region so just get our own region to figure out the kernel region name
        Region thisRegion = digraph.getRegion(context.getBundle());
        REGION_KERNEL = thisRegion.getName();
    }

    /*
     * Does a refreshBundles call and waits for the async operation to complete before returning.
     */
    private void refresh(Collection<Bundle> bundles) {
        if (FrameworkState.isStopping()) {
            // do nothing; system is shutting down removal pendings will be removed automatically
            return;
        }
        Bundle system = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        FrameworkWiring fwkWiring = system.adapt(FrameworkWiring.class);
        final CountDownLatch refreshed = new CountDownLatch(1);
        fwkWiring.refreshBundles(bundles, new FrameworkListener() {

            @Override
            public void frameworkEvent(FrameworkEvent event) {
                refreshed.countDown();
            }
        });
        try {
            // only wait for 30 seconds
            refreshed.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // not really expected; auto-FFDC is ok
            // keep thread interrupted
            Thread.interrupted();
        }
    }

    /*
     * Gets or creates the region to install the exporting bundle to.
     * The implementation only uses one of two regions.
     * 1) for all bundles that expose packages to the liberty kernel region
     * 2) for all bundles that expose packages to the osgi applications region for API access
     */
    private Region getLibraryExporterRegion(final RegionDigraph digraph, final PackageVisibility visibility) throws BundleException {
        final String libertyExporterRegionName = getExportFromRegion(visibility);
        final Region existingLibraryExporter = digraph.getRegion(libertyExporterRegionName);
        if (existingLibraryExporter == null) {
            ApiRegion.update(digraph, new Callable<RegionDigraph>() {
                @Override
                public RegionDigraph call() throws Exception {
                    RegionDigraph copy = digraph.copy();
                    Region libraryExporter = copy.getRegion(libertyExporterRegionName);
                    if (libraryExporter != null) {
                        // another thread won creating the region
                        return null;
                    }
                    libraryExporter = copy.createRegion(libertyExporterRegionName);
                    // notice that we assume the exportTo region already exists.
                    // if it does not then we will get an NPE below.
                    Region exportTo = copy.getRegion(getExportToRegion(visibility));
                    RegionFilterBuilder builder = copy.createRegionFilterBuilder();
                    // allow all packages into the product hub so that all packages provided by 
                    // the library are visible for all liberty feature bundles to use.
                    builder.allowAll(RegionFilter.VISIBLE_PACKAGE_NAMESPACE);
                    exportTo.connectRegion(libraryExporter, builder.build());
                    return copy;
                }
            });
            return digraph.getRegion(libertyExporterRegionName);
        }
        return existingLibraryExporter;
    }

    private String getExportFromRegion(PackageVisibility visibility) {
        // we use a single region for each visibility type
        switch (visibility) {
            case LIBERTY_FEATURES:
                return REGION_EXPORT_TO_LIBERTY;
            case OSGI_APPS:
                return REGION_EXPORT_TO_OSGI_APPS;
            default:
                throw new IllegalArgumentException("Illegal visibility type: " + visibility);
        }
    }

    private String getExportToRegion(PackageVisibility visibility) {
        switch (visibility) {
            case LIBERTY_FEATURES:
                // TODO note that we only expose the packages to the kernel for now
                // if this becomes SPI then we will have to revisit this.
                return REGION_KERNEL;
            case OSGI_APPS:
                // The ALL region is only used to expose all APIs to OSGi apps
                // we just add the exports to that region so OSGi apps can see them.
                return ApiRegion.ALL.getRegionName();
            default:
                throw new IllegalArgumentException("Illegal visibility type: " + visibility);
        }
    }

    @Override
    public ClassLoader getDelegateClassLoader(Bundle bundle) {
        // find if there is a library for this bundle;
        // if not then the bundle is likely a normal bundle and should be ignored;
        // otherwise we return the library class loader.
        Library library = bundleToLibrary.get(bundle);
        return library == null ? null : library.getClassLoader();
    }

    @Override
    public void setPackages(Library library, Collection<String> packageNames, PackageVisibility visibility) {
        LibraryVisibility libraryVisibility = new LibraryVisibility();
        LibraryVisibility current = libraryToVisibility.putIfAbsent(library, libraryVisibility);
        if (current != null) {
            // found a current one; use it instead of the constructed one above.
            // we could try to avoid unnecessary construction of the LibraryVisibility, but
            // it should not be expensive to construct and this is not a performance sensitive method.
            // In most cases it will be called at most once per library.
            libraryVisibility = current;
        }
        // update the visibility; this actually creates/updates the exporting bundle
        libraryVisibility.update(library, packageNames, visibility);
    }

    void delete(SharedLibraryImpl library) {
        // library is going away; we need to uninstall/delete the exporting bundles
        LibraryVisibility libraryVisibility = libraryToVisibility.remove(library);
        if (libraryVisibility != null) {
            libraryVisibility.delete();
        }
    }

    void refreshExporters(SharedLibraryImpl library) {
        // library has been modified; we need to refresh the exporters
        LibraryVisibility libraryVisibility = libraryToVisibility.get(library);
        if (libraryVisibility != null) {
            libraryVisibility.refreshExporters();
        }
    }
}
