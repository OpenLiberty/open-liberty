/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package test.library.packages;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.ws.classloading.LibraryAccess;
import com.ibm.ws.classloading.LibraryAccess.PackageVisibility;
import com.ibm.wsspi.library.Library;

/**
 *
 */
@Component(service = { ManagedServiceFactory.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                       Constants.SERVICE_VENDOR + "=" + "IBM",
                       Constants.SERVICE_PID + "=test.library.package.user"
           })
public class LibraryPackageUser implements ManagedServiceFactory {
    private static final String LIBRARY_REF_ATT = "libraryRef";
    private static final String LIBRARY_PACKAGE_ATT = "package";
    private final ReentrantLock trackerLock = new ReentrantLock();
    private ServiceTracker<Library, Library> tracker;
    private BundleContext context;
    private final ConcurrentLinkedQueue<Callable<?>> callables = new ConcurrentLinkedQueue<Callable<?>>();
    private LibraryAccess libraryAccess;
    private RegionDigraph digraph;

    @Activate
    protected void activate(BundleContext context) {
        this.context = context;
    }

    @Deactivate
    protected void deactivate() {
        trackerLock.lock();
        try {
            if (tracker != null) {
                tracker.close();
                tracker = null;
            }
        } finally {
            trackerLock.unlock();
        }
    }

    /**
     * @param tag
     *
     */
    private void invokeCallable(String tag) {
        for (Callable<?> callable : callables) {
            try {
                System.out.println(tag + ": " + getRegionName(callable) + ": " + callable.call());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param callable
     * @return
     */
    private String getRegionName(Callable<?> callable) {
        Region region = digraph.getRegion(FrameworkUtil.getBundle(callable.getClass()));
        return region.getName();
    }

    @Reference
    protected void setLibraryAccess(LibraryAccess libraryAccess) {
        System.out.println("LibraryPackageUser - entered setLibraryAccess method");
        this.libraryAccess = libraryAccess;
    }

    protected void unsetLibraryAccess(LibraryAccess libraryAccess) {
        System.out.println("LibraryPackageUser - entered unsetLibraryAccess method");
        // nothing
    }

    @Reference
    protected void setDigraph(RegionDigraph digraph) {
        System.out.println("LibraryPackageUser - entered setDigraph method");
        this.digraph = digraph;
    }

    protected void unsetDigraph(RegionDigraph digraph) {
        System.out.println("LibraryPackageUser - entered unsetDigraph method");
        // nothing
    }

    @Reference(cardinality=ReferenceCardinality.MULTIPLE,
               target="(test.library.packages=true)",
               policy=ReferencePolicy.DYNAMIC)
    protected void setCallable(Callable<?> callable) {
        System.out.println("LibraryPackageUser - entered setCallable method");
        callables.add(callable);
    }

    protected void unsetCallable(Callable<?> callable) {
        System.out.println("LibraryPackageUser - entered unsetCallable method");
        callables.remove(callable);
    }

    @Override
    public void deleted(String pid) {
        // doing nothing for testing, should set the packages of the library to empty.
    }

    @Override
    public String getName() {
        return "LibraryPackageUser";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> config) throws ConfigurationException {
        System.out.println("LibraryPackageUser - entered updated method");
        String libraryRef = (String) config.get(LIBRARY_REF_ATT);
        final String[] packages = (String[]) config.get(LIBRARY_PACKAGE_ATT);
        // it is unclear if only looking at the id would work here.
        // other examples in classloading use both id and service.pid to look up so doing the same here.
        String libraryStatusFilter = String.format("(&(objectClass=%s)(|(id=%s)(service.pid=%s)))", Library.class.getName(), libraryRef, libraryRef);
        Filter filter;
        try {
            filter = context.createFilter(libraryStatusFilter);
        } catch (InvalidSyntaxException e) {
            // should not happen, but blow up if it does
            throw new RuntimeException(e);
        }
        // create a tracker that will register the services once the library becomes available
        ServiceTracker<Library, Library> newTracker = null;
        newTracker = new ServiceTracker<Library, Library>(context, filter, new ServiceTrackerCustomizer<Library, Library>() {
            @Override
            public Library addingService(ServiceReference<Library> libraryRef) {
                invokeCallable("PRE_SET_PACKAGES");
                Library library = context.getService(libraryRef);
                libraryAccess.setPackages(library, packages == null ? null : Arrays.asList(packages), PackageVisibility.LIBERTY_FEATURES);
                invokeCallable("POST_SET_PACKAGES");
                return library;
            }

            @Override
            public void modifiedService(ServiceReference<Library> libraryRef, Library library) {
                // don't care
            }

            @Override
            public void removedService(ServiceReference<Library> libraryRef, Library library) {
                libraryAccess.setPackages(library, Collections.<String>emptyList(), PackageVisibility.LIBERTY_FEATURES);
                context.ungetService(libraryRef);
            }
        });
        trackerLock.lock();
        try {
            // note that we close the previous tracker;
            // this has the side effect of clearing the existing packages before applying the new ones;
            // this allows a consistent pre and post packages check when we set the packages because it
            // ensures no packages are configured before applying the configured packages.
            if (tracker != null) {
                tracker.close();
            }
            tracker = newTracker;
            tracker.open();
        } finally {
            trackerLock.unlock();
        }
    }
}
