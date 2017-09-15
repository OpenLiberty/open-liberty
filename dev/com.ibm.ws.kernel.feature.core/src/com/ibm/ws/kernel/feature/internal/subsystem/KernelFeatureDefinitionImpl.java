/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.subsystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.HeaderElementDefinition;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
public class KernelFeatureDefinitionImpl extends SubsystemFeatureDefinitionImpl {
    private static final String OSGI_PKG_PREFIX = "org.osgi.";

    private static volatile List<ProvisioningFeatureDefinition> kernelDefs = null;
    private static String installRoot = null;

    /** FIXME: this is a hedge.. */
    private static volatile String kernelApiServices = null;

    /**
     * Get the kernel feature definitions in use by the runtime.
     *
     * Note: the kernel feature does not live in a standard repository location.
     * It will never be found while iterating over a collection of resources.
     *
     * @param ctx
     * @param locationService
     * @return
     */
    public static List<ProvisioningFeatureDefinition> getKernelFeatures(BundleContext ctx, WsLocationAdmin locationService) {
        List<ProvisioningFeatureDefinition> result = kernelDefs;
        if (result == null) {
            result = kernelDefs = getKernelFeatures(ctx, locationService, false);
        }
        return result;
    }

    public static void dispose() {
        kernelDefs = null;
    }

    /**
     * Get ALL kernel feature definitions (in active use or not). Minify requires this to ensure
     * that optional kernel features like binaryLogging are not minified out.
     *
     * This list is not cached.
     *
     * @param bundleContext
     * @param locationService
     * @return
     */
    public static Collection<ProvisioningFeatureDefinition> getAllKernelFeatures(BundleContext ctx,
                                                                                 WsLocationAdmin locationService) {
        return getKernelFeatures(ctx, locationService, true);
    }

    /**
     * FIXME: this is a hack
     *
     * @return
     */
    public static String getKernelApiServices() {
        return kernelApiServices;
    }

    private static List<ProvisioningFeatureDefinition> getKernelFeatures(BundleContext ctx,
                                                                         WsLocationAdmin locationService,
                                                                         boolean listAll) {

        if (installRoot == null) {
            String path = locationService.resolveString("${wlp.install.dir}");
            installRoot = new File(path).getAbsolutePath();
        }

        List<ProvisioningFeatureDefinition> kernelFeatures = new ArrayList<ProvisioningFeatureDefinition>();

        if (listAll) {
            // We need ALL files from the lib/platform dir. Just read them all.
            WsResource platformDir = locationService.resolveResource("${wlp.install.dir}/lib/platform/");
            Iterator<String> mfFileNames = platformDir.getChildren(".*\\.mf");
            while (mfFileNames.hasNext()) {
                WsResource kdr = platformDir.getChild(mfFileNames.next());
                try {
                    KernelFeatureDefinitionImpl kDef = new KernelFeatureDefinitionImpl(ctx, kdr, locationService);
                    kernelFeatures.add(kDef);
                } catch (IOException e) {
                    // For the most part, we know the features are readable because we wouldn't
                    // be running this code if the *.mf files hadn't been cracked open already...
                    // We will have an FFDC for this, and otherwise, thise definition would
                    // obviously be skipped... which might cause problems later..
                }
            }
        } else {
            // There are three framework properties that explain the manifest files used to load
            // the kernel: websphere.kernel, websphere.log.provider, websphere.os.extension
            // Of the 3, only websphere.os.extension can be missing.
            // These files may include other subsystem files using the usual syntax.
            String websphereKernel = "${wlp.install.dir}/lib/platform/${websphere.kernel}.mf";
            String websphereLogProvider = "${wlp.install.dir}/lib/platform/${websphere.log.provider}.mf";
            String websphereOsExtension = "${wlp.install.dir}/lib/platform/${websphere.os.extension}.mf";

            WsResource websphereKernelResource = locationService.resolveResource(locationService.resolveString(websphereKernel));
            WsResource websphereLogProviderResource = locationService.resolveResource(locationService.resolveString(websphereLogProvider));

            LinkedList<WsResource> resources = new LinkedList<WsResource>();
            resources.add(websphereKernelResource);
            resources.add(websphereLogProviderResource);

            if (ctx.getProperty("websphere.os.extension") != null) {
                WsResource websphereOsExtensionResource = locationService.resolveResource(locationService.resolveString(websphereOsExtension));
                resources.add(websphereOsExtensionResource);
            }

            WsResource kdr;
            while ((kdr = resources.poll()) != null) {
                try {
                    KernelFeatureDefinitionImpl kDef = new KernelFeatureDefinitionImpl(ctx, kdr, locationService);
                    kernelFeatures.add(kDef);

                    // A kernel feature may include another kernel feature.. we need to go find those, too.
                    for (FeatureResource fr : kDef.getConstituents(SubsystemContentType.FEATURE_TYPE)) {
                        // Kernel features should have location attributes because they live in a non-standard
                        // location (and thus they include this other thing from the non-standard location).
                        // Use the location field to find the included subsystem. If we can't, we'll have to
                        // skip it as unknown: however, this is the kernel, and thus we can yell about bad
                        // behaving kernel features at build time, which means this should be fine.
                        String location = fr.getLocation();
                        if (location != null) {
                            WsResource res = locationService.resolveResource("${wlp.install.dir}/" + location);
                            resources.add(res);
                        }
                    }
                } catch (IOException e) {
                    // For the most part, we know the features are readable because we wouldn't
                    // be running this code if the *.mf files hadn't been cracked open already...
                    // We will have an FFDC for this, and otherwise, this definition will
                    // obviously be skipped, which might cause problems later..
                }
            }
        }

        return kernelFeatures;
    }

    private final boolean isSystemBundleProvider;
    private final Collection<HeaderElementDefinition> sysPkgApiHeader;
    private final Collection<HeaderElementDefinition> sysPkgSpiHeader;

    private KernelFeatureDefinitionImpl(BundleContext ctx, WsResource resource, WsLocationAdmin locationService) throws IOException {
        super(ExtensionConstants.CORE_EXTENSION, resource.asFile());

        isSystemBundleProvider = Boolean.parseBoolean(super.getHeader("WebSphere-SystemBundleProvider"));

        if (isSystemBundleProvider) {
            // If this kernel definition is also the system bundle provider, we need to account
            // for API/SPI provided by the system bundle itself (i.e. API/SPI coming from base
            // osgi services, from the bootstrap part of the kernel, or what a customer might
            // add using osgi.system.packages.extra.
            //
            // Our logging SPI (Tr/FFDC) is an example of this, it is provided as SPI
            // via the system bundle. There are attributes on the Export-Package statement like so:
            //    com.ibm.websphere.ras;ibm-spi-type=spi;version="1.0"
            //
            // We need to convert that into the IBM-SPI-Package syntax so it can be easily integrated
            // with the rest of the API/SPI packages in the system.
            sysPkgApiHeader = new ArrayList<HeaderElementDefinition>();
            sysPkgSpiHeader = new ArrayList<HeaderElementDefinition>();

            // If this feature happens to declare API/SPI packages,
            // we can start with that...
            sysPkgApiHeader.addAll(super.getHeaderElements(FeatureDefinitionUtils.IBM_API_PACKAGE));
            sysPkgSpiHeader.addAll(super.getHeaderElements(FeatureDefinitionUtils.IBM_SPI_PACKAGE));

            // Process any tagged packages exported by the system bundle for inclusion in the API and SPI lists
            Bundle systemBundle = ctx.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            if (systemBundle != null) {

                // There are a lot of exports from the system bundle. So lets go through this once
                // and classify the packages as we go..
                String bundleExports = systemBundle.getHeaders("").get(Constants.EXPORT_PACKAGE);
                if (bundleExports != null) {
                    List<NameValuePair> exports = ManifestHeaderProcessor.parseExportString(bundleExports);

                    //check each package for the attribute
                    for (NameValuePair nvp : exports) {
                        Map<String, String> attrs = nvp.getAttributes();
                        String pkgName = nvp.getName();
                        String spiType = attrs.remove("ibm-spi-type");
                        String apiType = attrs.remove("ibm-api-type");

                        if (apiType != null) {
                            // API
                            attrs.put(FeatureDefinitionUtils.FILTER_TYPE_KEY, apiType);
                            sysPkgApiHeader.add(new HeaderElementDefinitionImpl(pkgName, attrs));
                        } else if (spiType != null) {
                            // SPI
                            attrs.put(FeatureDefinitionUtils.FILTER_TYPE_KEY, spiType);
                            sysPkgSpiHeader.add(new HeaderElementDefinitionImpl(pkgName, attrs));
                        } else if (pkgName.startsWith(OSGI_PKG_PREFIX)) {
                            // OSGI --> SPI type="spec"
                            //special case to add all OSGi packages as spec SPI
                            attrs.put(FeatureDefinitionUtils.FILTER_TYPE_KEY, "spec");
                            sysPkgSpiHeader.add(new HeaderElementDefinitionImpl(pkgName, attrs));
                        }
                    }
                }
            }
        } else {
            sysPkgApiHeader = null;
            sysPkgSpiHeader = null;
        }

        // Need one string for all API services provided by the kernel
        addKernelApiServices(getApiServices());
    }

    private void addKernelApiServices(String newServices) {
        String services = kernelApiServices;
        if (services != null && !services.isEmpty()) {
            services = services + "," + newServices;
        } else {
            services = newServices;
        }

        kernelApiServices = services;
    }

    private void addAllBeneath(String installRoot, File f, Collection<FeatureResource> result, String osTag) {
        if (f.exists()) {
            if (f.isFile()) {
                Map<String, String> attrs = new HashMap<String, String>(2);
                //relativize by removing the install root prefix
                String location = f.getAbsolutePath().replaceFirst(installRoot, "");
                attrs.put("location:", location);
                attrs.put("type", "file");
                if (osTag != null)
                    attrs.put("os", osTag);
                result.add(new FeatureResourceImpl(f.getPath(), attrs, "", this.getFeatureName()));
            } else if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    addAllBeneath(installRoot, c, result, osTag);
                }
            }
        }
    }

    @Override
    public Collection<FeatureResource> getConstituents(SubsystemContentType type) {
        if (isSystemBundleProvider) {
            Collection<FeatureResource> result = new ArrayList<FeatureResource>();
            result.addAll(super.getConstituents(type));

            // you ask for all types using null!
            if (type == null || type == SubsystemContentType.FILE_TYPE) {
                String installRootString = Pattern.quote(installRoot);

                //add in the 'special' dirs that the kernel owns all of
                //(for now).

                File f = new File(installRoot, "lib/versions");
                addAllBeneath(installRootString, f, result, null);
                f = new File(installRoot, "lib/platform");
                addAllBeneath(installRootString, f, result, null);
                f = new File(installRoot, "lib/fixes");
                addAllBeneath(installRootString, f, result, null);
            }
            return Collections.unmodifiableCollection(result);
        } else {
            return super.getConstituents(type);
        }
    }

    @Override
    public Collection<HeaderElementDefinition> getHeaderElements(String header) {
        if (isSystemBundleProvider) {
            if (FeatureDefinitionUtils.IBM_API_PACKAGE.equals(header))
                return Collections.unmodifiableCollection(sysPkgApiHeader);
            if (FeatureDefinitionUtils.IBM_SPI_PACKAGE.equals(header))
                return Collections.unmodifiableCollection(sysPkgSpiHeader);
        }
        return super.getHeaderElements(header);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String header) {
        if (isSystemBundleProvider) {
            if (FeatureDefinitionUtils.IBM_API_PACKAGE.equals(header))
                return generateHeaderFromDefinitions("IBM-MergedApiPackage", sysPkgApiHeader);
            if (FeatureDefinitionUtils.IBM_SPI_PACKAGE.equals(header))
                return generateHeaderFromDefinitions("IBM-MergedSpiPackage", sysPkgSpiHeader);
        }
        return super.getHeader(header);
    }

    @Override
    public boolean isCapabilitySatisfied(Collection<ProvisioningFeatureDefinition> featureDefinitionsToCheck) {
        //return true because the kernel is always satisfied
        return true;
    }

    @Override
    public boolean isKernel() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        //it is safe to use the super type equals, since it is based on symbolic name and version
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        //it is safe to use the super type hashcode, since it is based on symbolic name and version
        return super.hashCode();
    }

    private String generateHeaderFromDefinitions(String mergedHeader, Collection<HeaderElementDefinition> headerDefs) {
        String mergedValue = super.getHeader(mergedHeader);
        if (mergedValue == null) {
            StringBuilder headerBuilder = new StringBuilder();

            for (HeaderElementDefinition hed : headerDefs) {
                if (headerBuilder.length() > 0)
                    headerBuilder.append(",");

                //start with the symbolic name
                StringBuilder element = new StringBuilder(hed.getSymbolicName());

                //add the attributes
                for (Map.Entry<String, String> attr : hed.getAttributes().entrySet()) {
                    element.append(";");
                    element.append(attr.getKey());
                    element.append("=");
                    element.append(attr.getValue());
                }

                //add the directives
                for (Map.Entry<String, String> directive : hed.getDirectives().entrySet()) {
                    String key = directive.getKey();
                    element.append(";");
                    element.append(key);
                    element.append(":=");
                    element.append(directive.getValue());
                }

                headerBuilder.append(element.toString());
            }

            mergedValue = headerBuilder.toString();

            // Keep for later...
            setHeader(mergedHeader, mergedValue);
        }

        return mergedValue;
    }
}