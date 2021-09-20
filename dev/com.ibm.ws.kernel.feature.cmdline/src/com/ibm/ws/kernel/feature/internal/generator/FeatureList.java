/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.stream.XMLStreamException;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;
import com.ibm.ws.config.xml.internal.schema.MetaTypeInformationSpecification;
import com.ibm.ws.config.xml.internal.schema.ObjectClassDefinitionSpecification;
import com.ibm.ws.config.xml.internal.schema.SchemaMetaTypeParser;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.cmdline.APIType;
import com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.ReturnCode;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.HeaderElementDefinition;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

public class FeatureList {

    private final FeatureListOptions options;
    private final Map<String, ProvisioningFeatureDefinition> features;
    private final Map<String, ProvisioningFeatureDefinition> coreFeatures;

    private final Map<String, List<Map<String, Object>>> cachedJavaVersionsByFeature = new HashMap<String, List<Map<String, Object>>>();
    private final Map<File, List<Map<String, Object>>> cachedJavaVersionsByBundle = new HashMap<File, List<Map<String, Object>>>();

    private final FeatureListUtils featureListUtils;
    private final static boolean writingJavaVersion = Boolean.getBoolean("ibm.javaVersion");
    private static final List<Map<String, Object>> possibleJavaVersions = new ArrayList<Map<String, Object>>();
    private static final Map<String, Collection<GenericMetadata>> eeToCapability = new HashMap<String, Collection<GenericMetadata>>();

    private static File installDir;
    private static boolean gaBuild = true;

    static {
        if (writingJavaVersion) {
            addJVM(possibleJavaVersions, "1.7", "1.6", "1.5", "1.4", "1.3", "1.2", "1.1");
            addJVM(possibleJavaVersions, "1.8", "1.7", "1.6", "1.5", "1.4", "1.3", "1.2", "1.1");
            addJVM(possibleJavaVersions, "11", "10", "9", "1.8", "1.7", "1.6", "1.5", "1.4", "1.3", "1.2", "1.1");
            addJVM(possibleJavaVersions, "17", "16", "15", "14", "13", "12", "11", "10", "9", "1.8", "1.7", "1.6", "1.5", "1.4", "1.3", "1.2", "1.1");

            List<GenericMetadata> mostGeneralRange = ManifestHeaderProcessor.parseCapabilityString("osgi.ee; filter:=\"(&(osgi.ee=JavaSE)(version=1.7))\"");

            eeToCapability.put("J2SE-1.2", mostGeneralRange);
            eeToCapability.put("J2SE-1.3", mostGeneralRange);
            eeToCapability.put("J2SE-1.4", mostGeneralRange);
            eeToCapability.put("JavaSE-1.5", mostGeneralRange);
            eeToCapability.put("JavaSE-1.6", mostGeneralRange);
            eeToCapability.put("JavaSE-1.7", mostGeneralRange);
            eeToCapability.put("JavaSE-1.8", ManifestHeaderProcessor.parseCapabilityString("osgi.ee; filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\""));
            eeToCapability.put("JavaSE-11", ManifestHeaderProcessor.parseCapabilityString("osgi.ee; filter:=\"(&(osgi.ee=JavaSE)(version=11))\""));
            eeToCapability.put("JavaSE-17", ManifestHeaderProcessor.parseCapabilityString("osgi.ee; filter:=\"(&(osgi.ee=JavaSE)(version=17))\""));
        }

        gaBuild = isGABuild();
    }

    /**
     * Constructor.
     *
     * @param options The list of command line options.
     * @param fds The list of features associated with a particular product.
     * @param coreFDs The list of features associated with the core product, or null if
     *            productId is the core product.
     * @param location The location of the product whose features are being processed.
     * @param productId The product ID of the product whose features are being processed.
     *            The product IDs for core and usr products are null. Product IDs for
     *            product extensions with no productId property defined are also null.
     *            Null product IDs are not added to the output xml.
     */
    public FeatureList(FeatureListOptions options,
                       Map<String, ProvisioningFeatureDefinition> fds,
                       Map<String, ProvisioningFeatureDefinition> coreFDs,
                       FeatureListUtils utils) {

        this.options = options;
        this.features = fds;
        this.coreFeatures = coreFDs;

        this.featureListUtils = utils;
        // Initialize placeholders for the default repositories ("", and "usr"),
        // we do not want to use caches, and we can't use Tr
        BundleRepositoryRegistry.initializeDefaults(null, false);
    }

    /**
     * @param possiblejavaversions2
     * @param d
     */
    private static void addJVM(List<Map<String, Object>> possibleJavaVersions, String version, String... versions) {
        List<Version> versionsList = new ArrayList<Version>();
        Version v = new Version(version);
        versionsList.add(v);
        for (String aVersion : versions) {
            versionsList.add(new Version(aVersion));
        }
        Map<String, Object> supportedJVM = new HashMap<String, Object>();
        supportedJVM.put("osgi.ee", "JavaSE");
        supportedJVM.put("version", versionsList);
        supportedJVM.put("bree", "JavaSE-" + v.getMajor() + "." + v.getMinor());
        possibleJavaVersions.add(supportedJVM);
    }

    public void writeFeatureList(ManifestFileProcessor mfp) {
        try {

            File installDir = Utils.getInstallDir();
            try {
                String productName = options.getProductName();
                FeatureListWriter writer = new FeatureListWriter(featureListUtils);

                for (Map.Entry<String, ProvisioningFeatureDefinition> entry : features.entrySet()) {
                    writeFeature(writer, mfp, installDir, entry.getValue(), productName);
                }

            } catch (XMLStreamException e) {
                throw new IOException("Error generating feature list", e);
            }
        } catch (IOException ex) {
            options.setReturnCode(ReturnCode.RUNTIME_EXCEPTION);
            throw new RuntimeException(ex);
        }
    }

    private void writeFeature(FeatureListWriter writer, ManifestFileProcessor mfp, File installDir, ProvisioningFeatureDefinition fd, String productName) throws IOException, XMLStreamException {
        boolean publicFeature = false;
        boolean showExternals = true;
        boolean privateFeature = false;
        Collection<HeaderElementDefinition> autoFeature = fd.getHeaderElements(FeatureDefinitionUtils.IBM_PROVISION_CAPABILITY);

        String apiPkgs = fd.getHeader("IBM-API-Package");
        String spiPkgs = fd.getHeader("IBM-SPI-Package");

        if (fd.getVisibility() == Visibility.PUBLIC) {
            String featureName = fd.getIbmShortName();
            if (featureName == null) {
                featureName = fd.getSymbolicName();
            }
            writer.startFeature("feature", featureName);
            publicFeature = true;
        } else if (fd.getVisibility() == Visibility.PROTECTED) {
            writer.startFeature("protectedFeature");
            showExternals = false;
        } else if (fd.isKernel()) {
            writer.startFeature("kernelFeature");
        } else if (!!!autoFeature.isEmpty()) {
            writer.startFeature("autoFeature");
        } else {
            writer.startFeature("privateFeature");
            showExternals = false;
            privateFeature = true;
        }

        writer.writeTextElement("symbolicName", fd.getSymbolicName());

        if (fd.isSingleton()) {
            writer.writeTextElement("singleton", "true");
        }

        // WARNING!! Special case for client only features
        if (FeatureDefinitionUtils.ALLOWED_ON_CLIENT_ONLY_FEATURES.contains(fd.getSymbolicName())) {
            writer.writeTextElement("processType", ProcessType.CLIENT.name());
        }

        if (!!!privateFeature) {
            String name = fd.getHeader("Subsystem-Name", options.getLocale());
            String desc = fd.getHeader("Subsystem-Description", options.getLocale());
            if (name != null) {
                writer.writeTextElement("displayName", name);
            }
            if (fd.isSuperseded()) {
                writer.writeTextElement("superseded", "true");
            }
            String supersededBy = fd.getSupersededBy();
            if (supersededBy != null) {
                String[] supersededByList = supersededBy.split(",");
                for (String supersedByFeature : supersededByList) {
                    String supersedByFeatureTrim = supersedByFeature.trim();
                    if (!supersedByFeatureTrim.equals("")) {
                        writer.writeTextElement("supersededBy", supersedByFeatureTrim);
                    }
                }

            }

            if (desc != null) {
                writer.writeTextElement("description", desc);
            }

            if (writingJavaVersion) {
                List<Map<String, Object>> javaVersions = getJavaVersion(mfp, fd);

                for (Map<String, Object> version : javaVersions) {
                    writer.writeTextElement("javaVersion", toJavaEE(version));
                }
            }
            String categories = fd.getHeader("Subsystem-Category");
            if (categories != null) {
                for (String category : categories.split(",")) {
                    writer.writeTextElement("category", category);
                }
            }
        }

        for (HeaderElementDefinition element : autoFeature) {
            String filter = element.getDirectives().get("filter");
            writer.writeTextElement("autoProvision", filter);
        }

        if (!!!privateFeature) {
            ContentBasedLocalBundleRepository cbr = getBundleRepo(fd.getFeatureName(), mfp);
            Set<File> bundles = new TreeSet<File>();
            Map<File,Map<String,String>> apiJars = showExternals ? new TreeMap<File,Map<String,String>>() : null;
            Map<File,Map<String,String>> spiJars = new TreeMap<File,Map<String,String>>();

            if (publicFeature) {
                Set<String> enabledFeatureNames = new TreeSet<String>();
                searchContent(fd, cbr, enabledFeatureNames, bundles, apiJars, spiJars);

                for (String enabledFeatureName : enabledFeatureNames) {
                    writer.writeTextElement("enables", enabledFeatureName);
                }
            } else {
                searchJars(fd, cbr, bundles, apiJars, spiJars);
            }

            writeApiSpiJars(writer, installDir, apiJars, "apiJar");
            writeApiSpiJars(writer, installDir, spiJars, "spiJar");

            // write api/spi packages
            writeApiSpiPkgs(writer, apiPkgs, "apiPackage");
            writeApiSpiPkgs(writer, spiPkgs, "spiPackage");

            // configElement
            if (showExternals) {
                Set<String> elements = new HashSet<String>();
                // if we see the kernel feature we want to add include and variable.
                if ("com.ibm.websphere.appserver.kernelCore-1.0".equals(fd.getSymbolicName())) {
                    elements.add("include");
                    elements.add("variable");
                }
                boolean includeInternal = options.getIncludeInternals();
                SchemaMetaTypeParser parser = new SchemaMetaTypeParser(Locale.getDefault(), new ArrayList<File>(bundles), productName);
                List<MetaTypeInformationSpecification> info = parser.getMetatypeInformation();
                for (MetaTypeInformationSpecification spec : info) {
                    for (ObjectClassDefinitionSpecification ocds : spec.getObjectClassSpecifications()) {
                        if (includeInternal || !!!"internal".equals(ocds.getName())) {
                            if (ocds.getExtensionUris().contains(XMLConfigConstants.METATYPE_EXTENSION_URI)) {
                                Map<String, String> attribs = ocds.getExtensionAttributes(XMLConfigConstants.METATYPE_EXTENSION_URI);
                                if (attribs != null) {
                                    String isBeta = attribs.get("beta");
                                    if ( ! (gaBuild && "true".equals(isBeta))) {
                                        String alias = attribs.get("alias");
                                        if (alias != null && !attribs.containsKey("childAlias")) {
                                            elements.add(alias);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                for (String configElement : elements) {
                    writer.writeTextElement("configElement", configElement);
                }
            }
        }

        for (FeatureResource included : fd.getConstituents(SubsystemContentType.FEATURE_TYPE)) {

            String shortName = null;
            String symbolicName = included.getSymbolicName();
            ProvisioningFeatureDefinition other = features.get(symbolicName);

            // For extensions, also look for core features.
            if (other == null && coreFeatures != null) {
                other = coreFeatures.get(symbolicName);
            }

            // It is possible the required feature is missing so we shouldn't NPE.
            if (other != null) {
                if (other.getVisibility() == Visibility.PUBLIC) {
                    shortName = other.getFeatureName();
                }
            }

            writer.writeIncludeFeature(included.getSymbolicName(), included.getTolerates(), shortName);
        }
        writer.endFeature();
    }

    /**
     * @param version
     * @return
     */
    private String toJavaEE(Map<String, Object> version) {
        return (String) version.get("bree");
    }

    /**
     * @param mfp
     * @param installDir
     * @param fd
     * @return
     */
    private List<Map<String, Object>> getJavaVersion(ManifestFileProcessor mfp, ProvisioningFeatureDefinition fd) {

        List<Map<String, Object>> result = cachedJavaVersionsByFeature.get(fd.getFeatureName());

        if (result == null) {
            // TODO Work this out
            result = new ArrayList<Map<String, Object>>(possibleJavaVersions);

            for (FeatureResource res : fd.getConstituents(SubsystemContentType.BUNDLE_TYPE)) {
                ContentBasedLocalBundleRepository repo = mfp.getBundleRepository(fd.getBundleRepositoryType(), null);

                File bundleFile = repo.selectBundle(res.getLocation(), res.getSymbolicName(), res.getVersionRange());

                List<Map<String, Object>> bundleMatches = cachedJavaVersionsByBundle.get(bundleFile);

                if (bundleMatches == null) {
                    bundleMatches = new ArrayList<Map<String, Object>>();
                    JarFile jar = null;
                    try {
                        jar = new JarFile(bundleFile);
                        Manifest man = jar.getManifest();
                        Attributes a = man.getMainAttributes();
                        List<GenericMetadata> capabilityRequirements;

                        @SuppressWarnings("deprecation")
                        String eeValue = a.getValue(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
                        if (eeValue != null) {
                            String[] values = eeValue.split(",");
                            capabilityRequirements = toCapabilityRequirements(values);
                        } else {
                            String rq = a.getValue(Constants.REQUIRE_CAPABILITY);
                            if (rq != null) {
                                capabilityRequirements = ManifestHeaderProcessor.parseCapabilityString(rq);
                            } else {
                                capabilityRequirements = Collections.emptyList();
                                bundleMatches = new ArrayList<Map<String, Object>>(possibleJavaVersions);
                            }
                        }

                        for (GenericMetadata capability : capabilityRequirements) {
                            if ("osgi.ee".equals(capability.getNamespace())) {
                                String filterString = String.valueOf(capability.getDirectives().get("filter"));
                                try {
                                    Filter filter = FrameworkUtil.createFilter(filterString);
                                    for (Map<String, Object> props : possibleJavaVersions) {
                                        if (filter.matches(props)) {
                                            bundleMatches.add(props);
                                        }
                                    }
                                } catch (InvalidSyntaxException e) {
                                }
                            }
                        }
                        cachedJavaVersionsByBundle.put(bundleFile, bundleMatches);
                    } catch (IOException e) {
                        // unlikely to occur probably should do something though.
                    } finally {
                        if (jar != null) {
                            try {
                                jar.close();
                            } catch (IOException e) {
                                // ignore since we are doing cleanup.
                            }
                        }
                    }
                }
                result.retainAll(bundleMatches);
            }
            for (FeatureResource res : fd.getConstituents(SubsystemContentType.FEATURE_TYPE)) {
                ProvisioningFeatureDefinition otherFD = mfp.getFeatureDefinitions().get(res.getSymbolicName());
                // This will NPE when we call getJavaVersion, so skip
                if (otherFD != null)
                {
                  List<Map<String, Object>> featureSupportedVersion = getJavaVersion(mfp, otherFD);
                  result.retainAll(featureSupportedVersion);
                }
            }

            cachedJavaVersionsByFeature.put(fd.getFeatureName(), result);
        }

        return result.isEmpty() ? possibleJavaVersions : result;
    }

    /**
     * @param values
     * @return
     */
    private List<GenericMetadata> toCapabilityRequirements(String[] values) {
        List<GenericMetadata> metadata = new ArrayList<ManifestHeaderProcessor.GenericMetadata>();
        for (String val : values) {
            Collection<GenericMetadata> data = eeToCapability.get(val);
            if (data != null) {
                metadata.addAll(data);
            }
        }
        return metadata;
    }

    private void writeApiSpiJars(FeatureListWriter writer, File installDir, Map<File,Map<String,String>> jars, String elementName) throws IOException, XMLStreamException {
        if (jars != null) {
            for (Entry<File, Map<String, String>> e : jars.entrySet()) {
                File f = e.getKey();
                String nameToWrite;

                if (f.getAbsolutePath().startsWith(installDir.getAbsolutePath())) {
                    int installDirLen = installDir.getAbsolutePath().length() + 1;
                    nameToWrite = PathUtils.slashify(f.getAbsolutePath().substring(installDirLen));
                } else {
                    nameToWrite = PathUtils.slashify(f.getAbsolutePath());
                }

                writer.writeTextElementWithAttributes(elementName, nameToWrite, e.getValue());
            }
        }
    }

    private void writeApiSpiPkgs(FeatureListWriter writer, String pkgs, String pkgType) throws IOException, XMLStreamException {
        if (pkgs != null) {
            List<NameValuePair> pkgList = ManifestHeaderProcessor.parseExportString(pkgs);
            for (NameValuePair pkg : pkgList) {
                Map<String, String> attrs = pkg.getAttributes();
                if (!!!"internal".equals(attrs.get("type")))
                    writer.writeTextElementWithAttributes(pkgType, pkg.getName(), attrs);
            }
        }
    }

    private ContentBasedLocalBundleRepository getBundleRepo(String enabledFeatureName, ManifestFileProcessor mfp) {
        if (enabledFeatureName == null) {
            enabledFeatureName = "";
        }
        ContentBasedLocalBundleRepository cbr;
        if (enabledFeatureName.startsWith("usr:")) {
            cbr = BundleRepositoryRegistry.getUsrInstallBundleRepository();
        } else {
            if (enabledFeatureName.contains(":")) {
                cbr = mfp.getBundleRepository(enabledFeatureName.substring(0, enabledFeatureName.indexOf(":")), null);
            } else {
                cbr = BundleRepositoryRegistry.getInstallBundleRepository();
            }
        }
        return cbr;
    }

    private void searchContent(ProvisioningFeatureDefinition feature,
                               ContentBasedLocalBundleRepository cbr,
                               Set<String> enabledFeatureNames,
                               Set<File> bundles,
                               Map<File,Map<String,String>> apiJars,
                               Map<File,Map<String,String>> spiJars) {
        for (FeatureResource fr : feature.getConstituents(SubsystemContentType.FEATURE_TYPE)) {
            String symbolicName = fr.getSymbolicName();
            ProvisioningFeatureDefinition other = features.get(symbolicName);

            // For extensions, also look for core features.
            if (other == null && coreFeatures != null) {
                other = coreFeatures.get(symbolicName);
            }

            // It is possible the required feature is missing so we shouldn't NPE.
            if (other != null) {
                if (other.getVisibility() == Visibility.PUBLIC) {
                    if (enabledFeatureNames != null) {
                        enabledFeatureNames.add(other.getFeatureName());
                    }
                    searchContent(other, cbr, null, null, null, null);
                } else {
                    searchContent(other, cbr, enabledFeatureNames, bundles,
                                  apiJars != null && APIType.API.matches(fr) ? apiJars : null,
                                  spiJars != null && APIType.SPI.matches(fr) ? spiJars : null);
                }
            }
        }

        searchJars(feature, cbr, bundles, apiJars, spiJars);
    }

    private void searchJars(ProvisioningFeatureDefinition feature,
                            ContentBasedLocalBundleRepository cbr,
                            Set<File> bundles,
                            Map<File,Map<String,String>> apiJars,
                            Map<File,Map<String,String>> spiJars) {
        if (bundles != null || apiJars != null || spiJars != null) {
            searchJars(feature, cbr, SubsystemContentType.JAR_TYPE, null, apiJars, spiJars);
            searchJars(feature, cbr, SubsystemContentType.BUNDLE_TYPE, bundles, apiJars, spiJars);
        }
    }

    private void searchJars(ProvisioningFeatureDefinition feature,
                            ContentBasedLocalBundleRepository cbr,
                            SubsystemContentType contentType,
                            Set<File> bundles,
                            Map<File,Map<String,String>> apiJars,
                            Map<File,Map<String,String>> spiJars) {
        for (FeatureResource fr : feature.getConstituents(contentType)) {
            String location = fr.getLocation();
            File f = cbr.selectBundle(location, fr.getSymbolicName(), fr.getVersionRange());
            if (f != null) {
                if (bundles != null) {
                    bundles.add(f);
                }

                APIType apiType = APIType.getAPIType(fr);
                Map<String,String> attrs = new HashMap<String,String>(1);
                if (fr.getRequireJava() != null)
                    attrs.put("require-java", fr.getRequireJava().toString());
                if (apiType == APIType.API) {
                    if (apiJars != null) {
                        apiJars.put(f, attrs);
                    }
                } else if (apiType == APIType.SPI) {
                    if (spiJars != null) {
                        spiJars.put(f, attrs);
                    }
                }
            }
        }
    }

    /**
     * Work out whether we should generate the schema for a GA build or not.
     *
     * @return true if ga schema, false otherwise.
     */
    private static boolean isGABuild() {
        boolean result = true;

        final Properties props = new Properties();
        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            @Override
            public Object run() {
                try {
                    final File version = new File(getInstallDir(), "lib/versions/WebSphereApplicationServer.properties");
                    Reader r = new InputStreamReader(new FileInputStream(version), "UTF-8");
                    props.load(r);
                    r.close();
                } catch (IOException e) {
                    // ignore because we fail safe. Returning true will result in a GA suitable schema
                }
                return null;
            }
        });
        String v = props.getProperty("com.ibm.websphere.productVersion");

        if (v != null) {
            int index = v.indexOf('.');
            if (index != -1) {
                try {
                    int major = Integer.parseInt(v.substring(0, index));
                    if (major > 2012) {
                        result = false;
                    }
                } catch (NumberFormatException nfe) {
                    // ignore because we fail safe. True for this hides stuff
                }
            }
        }

        return result;
    }

    private static File getInstallDir() {
        if (installDir == null) {
            String installDirProp = System.getProperty("wlp.install.dir");
            if (installDirProp == null) {
                URL url = FeatureList.class.getProtectionDomain().getCodeSource().getLocation();

                if (url.getProtocol().equals("file")) {
                    // Got the file for the command line launcher, this lives in lib
                    try {
                        if (url.getAuthority() != null) {
                            url = new URL("file://" + url.toString().substring("file:".length()));
                        }

                        File f = new File(url.toURI());
                        // The parent of the jar is lib, so the parent of the parent is the install.
                        installDir = f.getParentFile();
                    } catch (MalformedURLException e) {
                        // Not sure we can get here so ignore.
                    } catch (URISyntaxException e) {
                        // Not sure we can get here so ignore.
                    }
                }
            } else {
                installDir = new File(installDirProp);
            }
        }

        return installDir;
    }
}
