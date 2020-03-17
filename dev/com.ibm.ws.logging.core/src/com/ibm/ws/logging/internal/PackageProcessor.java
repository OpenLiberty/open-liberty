/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.kernel.provisioning.packages.PackageIndex;
import com.ibm.ws.kernel.provisioning.packages.SharedPackageInspector;
import com.ibm.ws.kernel.provisioning.packages.SharedPackageInspector.PackageType;

/**
 * Collects information about the exported and private packages of all
 * active (and resolved) bundles in a Liberty server. This lives in the
 * OSGi logging bundle to avoid creating dependencies on OSGi APIs in the
 * 'vanilla' logging jar. However, it gets used in the vanilla jar,
 * which it achieves by implementing interfaces from that jar and
 * pushing a singleton back to a factory in that jar.
 *
 * We could piggy back on similar code implemented in the FeatureManager. However, that introduces a kind
 * of ugly dependency, and the feature manager is doing something a lot more sophisticated than what we
 * need. We don't care about features at all, we only care about the bundles in the system.
 */
public class PackageProcessor implements BundleTrackerCustomizer<Object> {

    /**  */
    private static final String IBM = "IBM";
    /** Reference to active bundle context */
    private final BundleContext bundleContext;
    private final AtomicReference<BundlePackages> packages = new AtomicReference<BundlePackages>();
    private final ServiceTracker<SharedPackageInspector, SharedPackageInspector> st;
    private volatile Set<Pattern> bootDelegationPackages = null;

    private static final AtomicReference<PackageProcessor> instance = new AtomicReference<PackageProcessor>(null);

    public static PackageProcessor getPackageProcessor() {
        PackageProcessor processor = instance.get();
        if (processor != null)
            return processor;

        // This needs to be done inside a doPriv because it is the responsibility this class to obtain the bundle context
        // and the application should not be given the org.osgi.framework.AdminPermission permission for this function to work.
        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            @Override
            public Object run() {
                // TODO Auto-generated method stub
                BundleContext ctx = getSystemBundleContext();
                if (ctx != null && instance.compareAndSet(null, new PackageProcessor(ctx))) {
                    /*
                     * Let's start listening for new bundles, now.
                     * We do have a small concurrency window if a bundle gets added after we register the listener, and after we
                     * call bundleContext.getBundles(), and an exception is thrown. If its asynchronous registration event doesn't get to
                     * us before the isIBMPackage() call when we process the exception, we risk counting an internal package as external, and printing its stack in
                     * console.log. I think we can live with that risk.
                     */
                    BundleTracker<Object> bt = new BundleTracker<Object>(ctx, Bundle.RESOLVED, instance.get());
                    bt.open();
                }
                return null;
            }
        });

        return instance.get();
    }

    private static BundleContext getSystemBundleContext() {
        Bundle bundle = StackFinder.getInstance().getTopBundleFromCallStack();
        Bundle systemBundle = null;
        if (bundle != null) {
            // Get the context of the system bundle
            BundleContext topBundlesContext = bundle.getBundleContext();
            systemBundle = topBundlesContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        }
        // If we come through really early, the system bundle may not be accessible
        // (in that case, just let us try again later)
        return systemBundle == null ? null : systemBundle.getBundleContext();
    }

    /**
     * For test-purposes only. If JMock supported
     * mocking static methods it wouldn't be necessary. Public, so that
     * TruncatableThrowableTest can use it.
     *
     * @param processor
     */
    public static void setProcessor(PackageProcessor processor) {
        instance.set(processor);

    }

    private PackageProcessor(BundleContext ctx) {
        bundleContext = ctx;
        st = new ServiceTracker<SharedPackageInspector, SharedPackageInspector>(bundleContext, SharedPackageInspector.class.getName(), null);
        st.open();
    }

    /**
     * Returns true if the package is part of the Liberty server, and is *not*
     * a boot delegation package, spec package, or third-party package.
     *
     * @param packageName
     * @return
     */
    public boolean isIBMPackage(String packageName) {
        if (packageName != null) {
            BundlePackages p = populatePackageLists();
            // If we still can't get our list of packages (for example, in unit tests), let everything pass
            if (p != null) {
                if (p.containsPrivatePackage(packageName)) {
                    return true;
                } else if (p.containsExportedPackage(packageName)) {
                    // Add an extra check - if this is an exported package, it could be spec or third-party, not IBM
                    SharedPackageInspector inspector = st.getService();
                    if (inspector != null) {
                        PackageType exported = inspector.getExportedPackageType(packageName);
                        // If it's spec, third party, or stable it's not internal - otherwise, it is
                        if ( exported == null )
                            return true;
                        if ( exported.isSpecApi() || exported.isThirdPartyApi() || exported.isStableApi() ) 
                            return false;
                        
                        return true;
                    }

                    // If we couldn't get hold of the service, assume it was internal if our bundles export it
                    return true;
                }

            }
        }
        return false;
    }

    private BundlePackages populatePackageLists() {
        // Concurrency: we could go through this process twice, but that's non-fatal, only slightly annoying.
        // I judge the performance risk of going through it twice to be lower than the performance risk of synchronization

        BundlePackages p = packages.get();
        if (p == null) {
            if (bundleContext != null) {
                BundlePackages bp = new BundlePackages();

                for (Bundle bundle : bundleContext.getBundles()) {
                    harvestPackageList(bp, bundle);
                }

                populateBootDelegationPackageList();

                if (packages.compareAndSet(null, bp)) {
                    return bp;
                }
                return packages.get();
            }
        }
        return p;
    }

    /**
     * This method is static to avoid concurrent access issues.
     */
    private static void harvestPackageList(BundlePackages packages, Bundle bundle) {
        // Double check that the bundle is (still) installed:
        if (bundle.getLocation() != null && bundle.getState() != Bundle.UNINSTALLED) {
            BundleManifest manifest = new BundleManifest(bundle);
            /*
             * Only bundles with a bundle vendor of IBM should count as internal.
             * We definitely don't want to be filtering out user bundles in the shared bundle space.
             * This filter might catch stack products, which is probably a good thing
             * if it happens.
             * We also want to count the system bundle as internal. Some of
             * the bundles with higher IDs aren't ours, but since we rebuild them
             * with bnd, they get an IBM vendor header.
             */
            if (bundle.getBundleId() == 0 || IBM.equals(manifest.getBundleVendor()) || IBM.equals(manifest.getBundleDistributor())) {
                packages.addPackages(manifest);
            }
        }
    }

    private void populateBootDelegationPackageList() {
        Set<Pattern> patterns = new HashSet<Pattern>();
        // OSGi and Java require that java.* packages are always boot delegated
        patterns.add(Pattern.compile("java..*"));
        
        String boots = AccessController.doPrivileged(
        		new PrivilegedAction<String>() {
        			@Override
        			public String run() {
        				return bundleContext.getProperty("org.osgi.framework.bootdelegation");
        			}
                }
        );
        
        String[] packages = boots.split(",");
        for (String packageRegex : packages) {
            // Do a little manipulation to turn a little-p pattern into a regex so we can turn it into a big-p pattern
            patterns.add(Pattern.compile(packageRegex.replaceAll("\\*", ".*")));
        }
        // Set in instance field only after fully built to avoid concurrent use while being created
        bootDelegationPackages = patterns;
    }

    /**
     * Utility method. Given a line from a stack trace, like
     * "at com.ibm.ws.kernel.boot.Launcher.configAndLaunchPlatform(Launcher.java:311)"
     * work out the package name ("com.ibm.ws.kernel.boot").
     */
    public static String extractPackageFromStackTraceLine(String txt) {

        String packageName = null;
        // Look for character classes mixed with dots, then a dot, then a class name (which could include dollar signs), then a dot, then a method name and other stuff like the file name
        Pattern regexp = Pattern.compile("\tat ([\\w\\.]*)\\.[\\w\\$]+\\.[<>\\w]+\\(.*");
        Matcher matcher = regexp.matcher(txt);
        if (matcher.matches()) {
            packageName = matcher.group(1);
        }
        return packageName;
    }

    /**
     * Work out the package name from a StackTraceElement. This is easier than a stack trace
     * line, because we already know the class name.
     */
    public static String extractPackageFromStackTraceElement(StackTraceElement element) {
        String className = element.getClassName();
        int lastDotIndex = className.lastIndexOf(".");
        String packageName;
        if (lastDotIndex > 0) {
            packageName = className.substring(0, lastDotIndex);
        } else {
            packageName = className;
        }
        return packageName;
    }

    /**
     * Returns true is this package is distributed as part of the Liberty server, but
     * is 'external' to IBM and available to user applications - that is, if its
     * exposed as a boot delegation package, or a spec package, or a third party API.
     *
     * @param packageName the package name to check for
     * @return
     */
    public boolean isSpecOrThirdPartyOrBootDelegationPackage(String packageName) {

        SharedPackageInspector inspector = st.getService();
        if (inspector != null) {
            PackageType type = inspector.getExportedPackageType(packageName);
            if (type != null && type.isSpecApi()) {
                return true;
            }
        }

        boolean isBundlePackage = false;
        // Check the boot delegation packages second, since they're patterns, and slower
        for (Pattern pattern : bootDelegationPackages) {
            isBundlePackage = pattern.matcher(packageName).matches();
            if (isBundlePackage) {
                return isBundlePackage;
            }
        }

        // If we can't work it out, assume things aren't spec packages
        return false;

    }

    private enum OSGiPackageType {
        PRIVATE,
        EXPORTED;
    }

    /**
     * A package index representing all private and internally-exported packages. We have a simple subclass to both simplify
     * parameter declaration.
     *
     * The PackageIndex is a trie organized by package name. Each node is a package
     * segment. In this usage of the package index, there are no wildcards.
     */
    private static class BundlePackages extends PackageIndex<OSGiPackageType> {

        /**
         * Given the bundle manifest, add all of the exported and private packages from it into the map.
         */
        public void addPackages(BundleManifest manifest) {
            addPackages(manifest.getPrivatePackages(), OSGiPackageType.PRIVATE);
            addPackages(manifest.getExportedPackages(), OSGiPackageType.EXPORTED);
        }

        public boolean containsPrivatePackage(String packageName) {
            OSGiPackageType actualType = find(packageName);
            if (actualType == OSGiPackageType.PRIVATE) {
                return true;
            }
            return false;
        }

        public boolean containsExportedPackage(String packageName) {
            OSGiPackageType actualType = find(packageName);
            if (actualType == OSGiPackageType.EXPORTED) {
                return true;
            }
            return false;
        }

        private void addPackages(Set<String> packages, OSGiPackageType type) {
            for (String packageName : packages) {

                // Assume we aren't going to have package collisions often.
                // If we have the same package as both a private package and an export,
                // we know for sure it's an IBM package, but we just don't know if it's
                // an SPI. I think we can live with that.
                add(packageName, type);

            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.util.tracker.BundleTrackerCustomizer#addingBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent)
     */
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        // Packages become available on resolution, so take that as the state we care about

        //ignore gateway bundles
        if (!isGateway(bundle)) {
            /*
             * Make sure privatePackages isn't null.
             * If new bundles arrive while we're still populating our package list, we'll go through all bundles twice,
             * but at least we'll be correct.
             */
            BundlePackages bp = populatePackageLists();
            harvestPackageList(bp, bundle);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.util.tracker.BundleTrackerCustomizer#modifiedBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent, java.lang.Object)
     */
    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        // no-op

    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.util.tracker.BundleTrackerCustomizer#removedBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent, java.lang.Object)
     */
    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        // Don't bother removing the private packages of uninstalled bundles, since we can't guarantee
        // we didn't have split packages with bundles that are still installed, and going through
        // all our bundles seems like overkill, since the consequences of extra packages in our
        // package list are pretty slim.

        // no-op

    }

    private static boolean isGateway(Bundle b) {
        String gwHeader = b.getHeaders("").get("IBM-GatewayBundle");
        return "true".equalsIgnoreCase(gwHeader);
    }
}
