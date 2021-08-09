/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils;
import com.ibm.ws.kernel.feature.internal.subsystem.KernelFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.ws.kernel.provisioning.packages.PackageIndex;
import com.ibm.ws.kernel.provisioning.packages.PackageIndex.Filter;
import com.ibm.ws.kernel.provisioning.packages.SharedPackageInspector;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.wsspi.logging.Introspector;

/**
 *
 */
public class PackageInspectorImpl implements SharedPackageInspector, Introspector {
    private static final TraceComponent tc = Tr.register(PackageInspectorImpl.class);

    private ServiceRegistration<?> registration = null;

    /**
     * The package index. This is an index of all packages declared as IBM-API or IBM-SPI
     * by any bundle from any product.
     */
    private volatile ProductPackages packageIndex = null;

    void activate(BundleContext bundleContext) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("service.vendor", "IBM");
        registration = bundleContext.registerService(new String[] { SharedPackageInspector.class.getName(),
                                                                    Introspector.class.getName() },
                                                     this, props);
    }

    void deactivate() {
        registration.unregister();
    }

    /**
     * This method creates and sets new instances of the two maps used to answer questions
     * about packages in the system:
     * <OL>
     * <LI>The set of currently installed features is iterated querying the bundle resources for each feature
     * <LI> The list of currently installed bundles (getBundles()) is iterated
     * <LI> each bundle is checked against each feature definition
     * <LI> feature definitions with matching bundles are added to the collection
     * <LI> the collection is added to the map, keyed by bundle for faster checks during resolution.
     * </OL>
     */
    final void populateSPIInfo(BundleContext bundleContext, FeatureManager fm) {

        //if the bundleContext is null we aren't going to be able to do this
        if (bundleContext != null) {

            // We're going to rebuild new indices, so use local variables
            ProductPackages newPackageIndex = new ProductPackages();

            // For figuring out SPI information, we need the installed features and the kernel features.
            Collection<ProvisioningFeatureDefinition> allInstalledFeatures = fm.getInstalledFeatureDefinitions();
            allInstalledFeatures.addAll(KernelFeatureDefinitionImpl.getKernelFeatures(bundleContext, fm.getLocationService()));

            // For all installed features, get information about the declared product packages
            // and the declared osgi bundles..
            for (ProvisioningFeatureDefinition def : allInstalledFeatures) {
                // Add package information to ProductPackages..
                newPackageIndex.addPackages(def);
            }

            // compact the read-only index
            newPackageIndex.compact();

            // Replace the member variables with the new instances
            packageIndex = newPackageIndex;
        }
    }

    /**
     * This iterator will walk the package index, returning only packages that indicate
     * they are API packages.
     *
     * @see com.ibm.ws.kernel.provisioning.packages.SharedPackageInspector#listApiPackages()
     */
    @Override
    public Iterator<String> listApiPackages() {
        ProductPackages index = packageIndex;
        if (index != null) {
            return index.packageIterator(new Filter<PackageInfo>() {
                @Override
                public boolean includeValue(String packageName, PackageInfo value) {
                    return value.isApi();
                }

            });
        }
        return new EmptyIterator();
    }

    @Override
    public PackageType getExportedPackageType(String packageName) {
        ProductPackages index = packageIndex;
        if (index != null) {
            // Find information about whether or not this package is exported
            return index.find(packageName);
        }
        return null;
    }

    enum PkgType {
        API(true, "api"),
        API_IBM(true, "ibm-api"),
        API_Internal(true, "internal"),
        API_Spec(true, "spec"),
        API_SpecOsgi(true, "spec:osgi"),
        API_ThirdParty(true, "third-party"),
        API_Stable(true, "stable"),

        SPI(false, "spi"),
        SPI_Spec(false, "spec"),
        SPI_ThirdParty(false, "third-party");

        private final boolean isApi;
        private final String attributeName;

        PkgType(boolean isApi, String attributeName) {
            this.isApi = isApi;
            this.attributeName = attributeName;
        }

        static PkgType fromString(String value, PkgType defaultType) {
            if (value != null) {
                value = value.trim();

                Set<PkgType> possible = Collections.emptySet();
                if (defaultType == API)
                    possible = API_types;
                else if (defaultType == SPI)
                    possible = SPI_types;

                for (PkgType t : possible) {
                    if (t.attributeName.equals(value)) {
                        return t;
                    }
                }
            }

            // if no type is specified, default to "api" or "spi"
            return defaultType;
        }

        static EnumSet<PkgType> API_types = EnumSet.noneOf(PkgType.class);
        static EnumSet<PkgType> SPI_types = EnumSet.noneOf(PkgType.class);

        // Static initializer for the init
        static {
            for (PkgType type : PkgType.values()) {
                if (type.isApi)
                    API_types.add(type);
                else
                    SPI_types.add(type);
            }
        }
    }

    static class PackageInfo implements PackageType {
        final EnumSet<PkgType> types;
        final ArrayList<String> productRepo;

        // Keep track of what type of feature(s) have exported this package
        private boolean isKernelExport = false;
        private boolean isCoreExport = false;
        private boolean isExtensionExport = false;

        PackageInfo(PkgType expType, String repo, boolean isKrnlExport) {
            this.types = EnumSet.of(expType);
            productRepo = new ArrayList<String>(1);
            productRepo.add(repo);
            evaluateForFeatureType(repo, isKrnlExport);
        }

        public void add(PkgType anotherType, String repo, boolean isKrnlExport) {

            add(anotherType, repo);
            evaluateForFeatureType(repo, isKrnlExport);
        }

        private void evaluateForFeatureType(String repo, boolean isKrnlExport) {

            // Determine what type of feature is exporting this package
            // It is expected to be one of the three.
            if (isKrnlExport) {
                isKernelExport = true;
            }
            if (repo.equals(ExtensionConstants.CORE_EXTENSION) && !!!isKrnlExport) {
                isCoreExport = true;
            }
            if (repo.equals(ExtensionConstants.USER_EXTENSION) && !!!isKrnlExport) {
                isExtensionExport = true;
            }
        }

        public void add(PkgType anotherType, String repo) {
            types.add(anotherType); // union

            // If the list doesn't contain the repo already, add it
            if (!productRepo.contains(repo)) {
                productRepo.add(repo);
            }
        }

        public boolean exportedByProduct(String repo) {
            return productRepo.contains(repo);
        }

        /**
         * @return true if this package is exported as any kind of API
         */
        public boolean isApi() {
            for (PkgType type : types)
                if (type.isApi)
                    return true;

            return false;
        }

        /**
         * @return true if this package is exported as any kind of SPI
         */
        public boolean isSpi() {
            for (PkgType type : types)
                if (!type.isApi) // if it isn't API, it's SPI
                    return true;

            return false;
        }

        /** IBM API type="api" */
        @Override
        public boolean isUserDefinedApi() {
            return types.contains(PkgType.API);
        }

        /** IBM API type="ibm-api" */
        @Override
        public boolean isIbmApi() {
            return types.contains(PkgType.API_IBM);
        }

        /** IBM API type="internal" */
        @Override
        public boolean isInternalApi() {
            return types.contains(PkgType.API_Internal);
        }

        /** IBM-API type="spec" or type="spec:osgi" */
        @Override
        public boolean isSpecApi() {
            return types.contains(PkgType.API_Spec)
                   || types.contains(PkgType.API_SpecOsgi);
        }

        /** IBM-API type="spec:osgi" */
        @Override
        public boolean isSpecOsgiApi() {
            return types.contains(PkgType.API_SpecOsgi);
        }

        /** IBM-API type="spec" only */
        @Override
        public boolean isStrictSpecApi() {
            return types.contains(PkgType.API_Spec);
        }

        /** IBM-API type="third-party" only */
        @Override
        public boolean isThirdPartyApi() {
            return types.contains(PkgType.API_ThirdParty);
        }

        /** type="stable" only */
        @Override
        public boolean isStableApi() {
            return types.contains(PkgType.API_Stable);
        }

        public boolean isKernelExportBlockedPackage() {

            if ((isKernelExport && isCoreExport && !isSpecOsgiApi() && !isSpi())) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return types + "|" + productRepo;
        }

    }

    /**
     * This *is* the package index. We have a simple subclass to both simplify
     * parameter declaration.
     *
     * The PackageIndex is a trie organized by package name. Each node is a package
     * segment. In this usage of the package index, there are no wildcards.
     */
    static class ProductPackages extends PackageIndex<PackageInfo> {

        /**
         * Given the feature definition, add all of the API and SPI packages from it into the map.
         * We're working from the assumption that most of the time, we won't have multiple
         * features working with the same package: i.e. try to add the package first, then if there
         * is a collision, modify the attributes of the existing element.
         *
         * @param def ProvisioningFeatureDefinition that is the source of the API/SPI packages
         */
        public void addPackages(ProvisioningFeatureDefinition def) {
            addPackages(def, FeatureDefinitionUtils.IBM_API_PACKAGE, PkgType.API);
            addPackages(def, FeatureDefinitionUtils.IBM_SPI_PACKAGE, PkgType.SPI);
        }

        private void addPackages(ProvisioningFeatureDefinition def, String packageHeader, PkgType defaultPkgType) {
            String packages = def.getHeader(packageHeader);

            // Get all the packages defined by the packageHeader
            if (packages != null && !packages.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "feature {0} added {1} packages to product {2}: {3}",
                             def.getFeatureName(),
                             defaultPkgType,
                             def.getBundleRepositoryType(),
                             packages);
                }

                // add all the exported packages
                for (NameValuePair nvp : ManifestHeaderProcessor.parseExportString(packages)) {
                    String packageName = nvp.getName();
                    String type = nvp.getAttributes().get("type");
                    String requireOsgiEE = nvp.getAttributes().get("require-java:");

                    // If the Java requirements are not met for exposing this package, do not add the package
                    if (requireOsgiEE != null && JavaInfo.majorVersion() < Integer.parseInt(requireOsgiEE))
                        continue;

                    PkgType expType = PkgType.fromString(type, defaultPkgType);
                    if (expType == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "{0} was associated with an unknown type {1}", packageName, nvp);
                        }
                    } else {
                        // Assume we aren't going to have package collisions often.
                        // Try to just add first:
                        PackageInfo packageInfo = new PackageInfo(expType, def.getBundleRepositoryType(), def.isKernel());
                        if (!add(packageName, packageInfo)) {
                            // ok. so the add didn't work because something else already marked this
                            // package as API or SPI. Find the existing EnumSet and add
                            // this export/package type to it..
                            // Note: we have packages that we promote from SPI to API for tests.
                            PackageInfo prevValue = find(packageName);
                            prevValue.add(expType, def.getBundleRepositoryType(), def.isKernel());

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "{0} was provided by more than one feature, combined attributes are {1}", packageName, prevValue);
                            }
                        }
                    }
                }
            }
        }
    }

    static class EmptyIterator implements Iterator<String> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public String next() {
            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String getIntrospectorName() {
        return "SharedPackages";
    }

    @Override
    public String getIntrospectorDescription() {
        return "List of declared API/SPI packages in the runtime";
    }

    @Override
    public void introspect(PrintWriter ps) throws IOException {

        ProductPackages index = packageIndex;
        if (index == null)
            ps.println("Product packages = null");
        else {
            ps.println("Product packages: ");
            ps.println(index.dump());
        }

        ps.flush();
    }

    public Iterator<String> getExtensionPackages(final String productName) {
        ProductPackages index = packageIndex;
        if (index == null) {
            return Collections.<String> emptyList().iterator();
        }
        return index.packageIterator(new Filter<PackageInspectorImpl.PackageInfo>() {
            @Override
            public boolean includeValue(String packageName, PackageInfo value) {
                return value.exportedByProduct(productName);
            }
        });
    }

    public Iterator<String> getGatewayPackages(final String productName) {
        ProductPackages index = packageIndex;
        if (index == null) {
            return Collections.<String> emptyList().iterator();
        }
        return index.packageIterator(new Filter<PackageInspectorImpl.PackageInfo>() {
            @Override
            public boolean includeValue(String packageName, PackageInfo value) {
                return value.isApi() && value.exportedByProduct(productName);
            }
        });
    }

    /**
     * This iterator will walk the package index, returning only packages that indicate
     * they are API packages and are included both by the kernel (core)feature and
     * another enabled liberty feature or features.
     *
     * @see com.ibm.ws.kernel.provisioning.packages.SharedPackageInspector#listKernelApiPackages()
     */
    public Iterator<String> listKernelBlockedApiPackages() {
        ProductPackages index = packageIndex;
        if (index != null) {
            return index.packageIterator(new Filter<PackageInfo>() {
                @Override
                public boolean includeValue(String packageName, PackageInfo value) {
                    return value.isApi() && value.isKernelExportBlockedPackage();
                }

            });
        }
        return new EmptyIterator();
    }
}