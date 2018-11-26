/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.parsers;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.aries.util.VersionRange;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;

import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveEntryNotFoundException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveIOException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveInvalidEntryException;
import com.ibm.ws.repository.parsers.internal.EsaSubsystemFeatureDefinitionImpl;
import com.ibm.ws.repository.parsers.internal.ManifestHeaderProcessor;
import com.ibm.ws.repository.parsers.internal.ManifestHeaderProcessor.GenericMetadata;
import com.ibm.ws.repository.resources.internal.AppliesToProcessor;
import com.ibm.ws.repository.resources.writeable.AttachmentResourceWritable;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class EsaParser extends ParserBase implements Parser<EsaResourceWritable> {

    /**  */
    private static final String REQUIRE_CAPABILITY_HEADER_NAME = "Require-Capability";

    /**  */
    private static final String JAVA_FILTER_KEY = "JavaSE";

    /**  */
    private static final String VERSION_FILTER_KEY = "version";

    /**  */
    private static final String OSGI_EE_NAMESPACE_ID = "osgi.ee";

    private static final VersionRange JAVA_8_RANGE = VersionRange.parseVersionRange("[1.2,1.8]");
    private static final VersionRange JAVA_7_RANGE = new VersionRange("[1.2,1.7]");
    private static final VersionRange JAVA_6_RANGE = new VersionRange("[1.2,1.6]");

    public EsaParser() {

    }

    public EsaParser(boolean overrideLicenseToNonSpecified) {
        this.overrideLicenseToNonSpecified = overrideLicenseToNonSpecified;
    }

    /** {@inheritDoc} */
    @Override
    public EsaResourceWritable parseFileToResource(File assetFile, File metadataFile, String contentUrl) throws RepositoryException {

        ArtifactMetadata artifactMetadata = explodeArtifact(assetFile, metadataFile);

        // Read the meta data from the esa
        ProvisioningFeatureDefinition feature;
        try {
            feature = EsaSubsystemFeatureDefinitionImpl.constructInstance(assetFile);
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), assetFile, e);
        }

        /*
         * First see if we already have this feature in MaaSive, note this means
         * we can only have one version of the asset in MaaSive at a time
         */
        EsaResourceWritable resource = WritableResourceFactory.createEsa(null);
        String symbolicName = feature.getSymbolicName();
        String version = feature.getVersion().toString();

        // Massive assets are always English, find the best name
        String subsystemName = feature.getHeader("Subsystem-Name",
                                                 Locale.ENGLISH);
        String shortName = feature.getIbmShortName();
        String metadataName = artifactMetadata != null ? artifactMetadata.getName() : null;
        final String name;

        /*
         * We want to be able to override the name in the built ESA with a value supplied in the metadata so use this in preference of what is in the ESA so that we can correct any
         * typos post-GM
         */
        if (metadataName != null && !metadataName.isEmpty()) {
            name = metadataName;
        } else if (subsystemName != null && !subsystemName.isEmpty()) {
            name = subsystemName;
        } else if (shortName != null && !shortName.isEmpty()) {
            name = shortName;
        } else {
            // symbolic name is always set
            name = symbolicName;
        }

        resource.setName(name);
        String shortDescription = null;
        String overriddenDisplayPolicy = null;
        if (artifactMetadata != null) {
            shortDescription = artifactMetadata.getShortDescription();
            resource.setDescription(artifactMetadata.getLongDescription());
            resource.setVanityURL(artifactMetadata.getProperty(PROP_VANITY_URL));
            overriddenDisplayPolicy = artifactMetadata.getProperty(PROP_DISPLAY_POLICY);
        }
        if (shortDescription == null) {
            shortDescription = feature.getHeader("Subsystem-Description", Locale.ENGLISH);
        }
        resource.setShortDescription(shortDescription);
        resource.setVersion(version);

        //Add icon files
        processIcons(assetFile, feature, resource);

        String provider = feature.getHeader("Subsystem-Vendor");
        if (provider != null && !provider.isEmpty()) {
            resource.setProviderName(provider);
            if ("IBM".equals(provider)) {
                resource.setProviderUrl("http://www.ibm.com");
            }
        } else {
            // Massive breaks completely if the provider is not filled in so
            // make sure it is!
            throw new InvalidParameterException("Subsystem-Vendor must be set in the manifest headers");
        }

        // Add custom attributes for WLP
        resource.setProvideFeature(symbolicName);

        //Check for AppliesTo in metadata.zip else grab it from esa.
        if (artifactMetadata != null && artifactMetadata.getProperty("IBM-AppliesTo") != null) {
            resource.setAppliesTo(artifactMetadata.getProperty("IBM-AppliesTo"));
        } else {
            resource.setAppliesTo(feature.getHeader("IBM-AppliesTo"));
        }
        Visibility visibility;

        /*
         * Two things affect the display policy - the visibility and the install policy. If a private auto feature is set to manual install we need to make it visible so people
         * know that it exists and can be installed
         */
        DisplayPolicy displayPolicy;
        DisplayPolicy webDisplayPolicy;
        if (com.ibm.ws.kernel.feature.Visibility.PUBLIC.equals(feature.getVisibility())) {
            visibility = Visibility.PUBLIC;
            displayPolicy = DisplayPolicy.VISIBLE;
            webDisplayPolicy = DisplayPolicy.VISIBLE;
        } else if (com.ibm.ws.kernel.feature.Visibility.PROTECTED.equals(feature.getVisibility())) {
            visibility = Visibility.PROTECTED;
            displayPolicy = DisplayPolicy.HIDDEN;
            webDisplayPolicy = DisplayPolicy.HIDDEN;
        } else if (com.ibm.ws.kernel.feature.Visibility.INSTALL.equals(feature.getVisibility())) {
            // Always hide "install" visility from the website.  These are really addons so if we want them on the website we add a second asset just for that purpose.
            // Show them in WDT though as they are now supported.
            visibility = Visibility.INSTALL;
            displayPolicy = DisplayPolicy.VISIBLE;
            webDisplayPolicy = DisplayPolicy.HIDDEN;
        } else {
            visibility = Visibility.PRIVATE;
            displayPolicy = DisplayPolicy.HIDDEN;
            webDisplayPolicy = DisplayPolicy.HIDDEN;
        }
        resource.setVisibility(visibility);

        if (feature.isAutoFeature()) {
            resource.setProvisionCapability(feature.getHeader("IBM-Provision-Capability"));
            String IBMInstallPolicy = feature.getHeader("IBM-Install-Policy");

            // Default InstallPolicy is set to MANUAL
            InstallPolicy installPolicy;
            if (IBMInstallPolicy != null && ("when-satisfied".equals(IBMInstallPolicy))) {
                installPolicy = InstallPolicy.WHEN_SATISFIED;
            } else {
                installPolicy = InstallPolicy.MANUAL;
                // As discussed above set the display policy to visible for any manual auto features
                displayPolicy = DisplayPolicy.VISIBLE;
                webDisplayPolicy = DisplayPolicy.VISIBLE;
            }
            resource.setInstallPolicy(installPolicy);
        }

        // if we are dealing with a beta feature hide it otherwise apply the
        // display policies from above
        if (isBeta(resource.getAppliesTo())) {
            resource.setWebDisplayPolicy(DisplayPolicy.HIDDEN);
        } else {
            resource.setWebDisplayPolicy(webDisplayPolicy);
        }

        // The side zip can override the display policy so if its been set then use it
        if (overriddenDisplayPolicy != null) {
            displayPolicy = DisplayPolicy.valueOf(overriddenDisplayPolicy);
        }
        // Always set displayPolicy
        resource.setDisplayPolicy(displayPolicy);

        // handle required iFixes
        String requiredFixes = feature.getHeader("IBM-Require-Fix");
        if (requiredFixes != null && !requiredFixes.isEmpty()) {
            String[] fixes = requiredFixes.split(",");
            for (String fix : fixes) {
                fix = fix.trim();
                if (!fix.isEmpty()) {
                    resource.addRequireFix(fix);
                }
            }
        }

        resource.setShortName(shortName);

        // Calculate which features this relies on

        Collection<FeatureResource> requiredFeatures = feature.getConstituents(SubsystemContentType.FEATURE_TYPE);
        for (FeatureResource featureResource : requiredFeatures) {
            List<String> tolerates = featureResource.getTolerates();
            String requiredFeatureSymbolicName = featureResource.getSymbolicName();
            resource.addRequireFeatureWithTolerates(requiredFeatureSymbolicName, tolerates);
        }

        // feature.supersededBy is a comma-separated list of shortNames. Add
        // each of the elements to either supersededBy or supersededByOptional.
        String supersededBy = feature.getSupersededBy();
        if (supersededBy != null && !supersededBy.trim().isEmpty()) {
            String[] supersededByArray = supersededBy.split(",");
            for (String f : supersededByArray) {
                // If one of the elements is surrounded by [square brackets] then we
                // strip the brackets off and treat it as optional
                if (f.startsWith("[")) {
                    f = f.substring(1, f.length() - 1);
                    resource.addSupersededByOptional(f);
                } else {
                    resource.addSupersededBy(f);
                }
            }
        }

        //resource.setLicenseType(getArtifactMedataLicenseType(artifactMetadata));
        if (artifactMetadata != null) {
            attachLicenseData(artifactMetadata, resource);
        }

        setJavaRequirements(assetFile, resource);

        String attachmentName = symbolicName + ".esa";
        addContent(resource, assetFile, attachmentName, artifactMetadata, contentUrl);

        // Now look for LI, LA files inside the .esa
        // We expect to find them in wlp/lafiles/LI_{Locale} or /LA_{Locale}
        try {
            processLAandLI(assetFile, resource, feature);
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), assetFile, e);
        }
        resource.setLicenseId(feature.getHeader("Subsystem-License"));

        resource.setSingleton(Boolean.toString(feature.isSingleton()));

        resource.setIBMInstallTo(feature.getHeader("IBM-InstallTo"));

        return resource;
    }

    private void processIcons(File esa, ProvisioningFeatureDefinition feature, EsaResourceWritable resource) throws RepositoryException {
        //checking icon file

        String current = "";
        String sizeString = "";
        String iconName = "";
        String subsystemIcon = feature.getHeader("Subsystem-Icon");

        if (subsystemIcon != null) {
            subsystemIcon.replaceAll("\\s", "");

            StringTokenizer s = new StringTokenizer(subsystemIcon, ",");
            while (s.hasMoreTokens()) {
                current = s.nextToken();
                int size = 0;

                if (current.contains(";")) { //if the icon has an associated size
                    StringTokenizer t = new StringTokenizer(current, ";");
                    while (t.hasMoreTokens()) {
                        sizeString = t.nextToken();

                        if (sizeString.contains("size=")) {
                            String sizes[] = sizeString.split("size=");
                            size = Integer.parseInt(sizes[sizes.length - 1]);
                        } else {
                            iconName = sizeString;
                        }
                    }

                } else {
                    iconName = current;
                }

                File icon = this.extractFileFromArchive(esa.getAbsolutePath(), iconName.trim()).getExtractedFile();
                if (icon.exists()) {
                    AttachmentResourceWritable at = resource.addAttachment(icon, AttachmentType.THUMBNAIL);
                    if (size != 0) {
                        at.setImageDimensions(size, size);
                    }
                } else {
                    throw new RepositoryArchiveEntryNotFoundException("Icon does not exist", esa, iconName);
                }
            }
        }
    }

    public static boolean isBeta(String appliesTo) {
        // Use the appliesTo string to determine whether a feature is a Beta or a regular feature.
        // Beta features are of the format:
        // "com.ibm.websphere.appserver; productVersion=2014.8.0.0; productInstallType=Archive",
        //
        // Update: features build using FeatureBnd look like this
        // "\"com.ibm.websphere.appserver;productEdition=EARLY_ACCESS;productVersion=\"2015.4.0.0\""
        // the changed regex supports the version quoted or unquoted.
        if (appliesTo == null) {
            return false;
        } else {
            String regex = ".*productVersion=\"?" + AppliesToProcessor.BETA_REGEX;
            boolean matches = appliesTo.matches(regex);
            return matches;
        }
    }

    /**
     * BundleFinder
     * Used to find the bundle jars within a feature
     */
    static class BundleFinder extends SimpleFileVisitor<Path> {
        FileSystem _zipSystem;
        PathMatcher _bundleMatcher;
        ArrayList<Path> bundles = new ArrayList<Path>();

        private BundleFinder(FileSystem zipSystem) {
            super();
            _zipSystem = zipSystem;
            // Bundles should be jars in the root of the zip
            _bundleMatcher = _zipSystem.getPathMatcher("glob:/*.jar");
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (_bundleMatcher.matches(file)) {
                bundles.add(file);
            }
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Look in the esa for bundles with particular java version requirements. Create an aggregate
     * requirement of the esa as a whole, and write the data into the supplied resource
     *
     * @param esa
     * @param resource
     * @throws RepositoryException If there are any IOExceptions reading the esa, or if the
     *             the bundles have conflicting Java version requirements.
     */
    private static void setJavaRequirements(File esa, EsaResourceWritable resource) throws RepositoryException {

        Map<String, String> bundleRequirements = new HashMap<String, String>();
        Path zipfile = esa.toPath();
        Map<String, VersionRange> matchingEnvs = new LinkedHashMap<String, VersionRange>();

        matchingEnvs.put("Java 6", JAVA_6_RANGE);
        matchingEnvs.put("Java 7", JAVA_7_RANGE);
        matchingEnvs.put("Java 8", JAVA_8_RANGE);

        StringBuilder message = new StringBuilder();

        // Map of Path of an esa or jar, to its Require-Capability string
        Map<Path, String> requiresMap = new HashMap<Path, String>();

        // build a set of capabilities of each of manifests in the bundles and the subsystem
        // manifest in the feature
        try (final FileSystem zipSystem = FileSystems.newFileSystem(zipfile, null)) {

            // get the paths of each bundle jar in the root directory of the esa
            Iterable<Path> roots = zipSystem.getRootDirectories();
            BundleFinder finder = new EsaParser.BundleFinder(zipSystem);
            for (Path root : roots) {
                // Bundles should be in the root of the zip, so depth is 1
                Files.walkFileTree(root, new HashSet<FileVisitOption>(), 1, finder);
            }

            // Go through each bundle jar in the root of the esa and add their require
            // capabilites to the map
            for (Path bundle : finder.bundles) {
                addBundleManifestRequireCapability(zipSystem, bundle, requiresMap);
            }

            // now add the require capabilities of the esa subsystem manifest
            addSubsystemManifestRequireCapability(esa, requiresMap);
        } catch (IOException e) {
            // Any IOException means that the version info isn't reliable, so only thing to do is ditch out.
            throw new RepositoryArchiveIOException(e.getMessage(), esa, e);
        }

        // Loop through the set of requires capabilities
        Set<Entry<Path, String>> entries = requiresMap.entrySet();
        for (Entry<Path, String> entry : entries) {
            Path path = entry.getKey();

            // Get the GenericMetadata
            List<GenericMetadata> requirementMetadata = ManifestHeaderProcessor.parseRequirementString(entry.getValue());
            GenericMetadata eeVersionMetadata = null;
            for (GenericMetadata metaData : requirementMetadata) {
                if (metaData.getNamespace().equals(OSGI_EE_NAMESPACE_ID)) {
                    eeVersionMetadata = metaData;
                    break;
                }
            }

            if (eeVersionMetadata == null) {
                // No version requirements, go to the next bundle
                continue;
            }

            Map<String, String> dirs = eeVersionMetadata.getDirectives();
            for (String key : dirs.keySet()) {

                if (!key.equals("filter")) {
                    continue;
                }

                Map<String, String> filter = null;
                filter = ManifestHeaderProcessor.parseFilter(dirs.get(key));

                // The interesting filter should contain osgi.ee=JavaSE and version=XX
                if (!(filter.containsKey(OSGI_EE_NAMESPACE_ID) && filter.get(OSGI_EE_NAMESPACE_ID).equals(JAVA_FILTER_KEY)
                      && filter.containsKey(VERSION_FILTER_KEY))) {
                    continue; // Uninteresting filter
                }

                // Store the raw filter to add to the resource later.
                bundleRequirements.put(path.getFileName().toString(), dirs.get(key));

                VersionRange range = ManifestHeaderProcessor.parseVersionRange(filter.get(VERSION_FILTER_KEY));
                Iterator<Entry<String, VersionRange>> iterator = matchingEnvs.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<String, VersionRange> capability = iterator.next();
                    VersionRange intersection = capability.getValue().intersect(range);
                    if (intersection == null) {
                        // Store what caused this env to be removed, for error message later
                        message.append("Manifest from " + path.getFileName() + " with range " + range + " caused env for "
                                       + capability.getKey() + " to be removed. ");
                        iterator.remove();
                    }
                }

                // Assume there is only one Java version filter, so stop looking
                break;
            }
        }

        if (matchingEnvs.size() == 0) {
            throw new RepositoryException("ESA " + resource.getName() +
                                          " is invalid as no Java execution environment matches all the bundle requirements: "
                                          + message);
        }

        ArrayList<String> rawRequirements = new ArrayList<String>();
        for (String key : bundleRequirements.keySet()) {
            rawRequirements.add(key + ": " + bundleRequirements.get(key));
        }
        if (rawRequirements.size() == 0) {
            rawRequirements = null;
        }

        // The only thing that really matter is the minimum Java level required for this
        // esa, as later Java levels provide earlier envs. Hence for now, the max is
        // always set to null
        // Need to get the first entry in the matchingEnvs map (it is a linked and hence ordered map),
        // hence the silliness below.
        Version min = matchingEnvs.entrySet().iterator().next().getValue().getMaximumVersion();
        resource.setJavaSEVersionRequirements(min.toString(), null, rawRequirements);

    }

    @Override
    protected void checkRequiredProperties(ArtifactMetadata artifact) throws RepositoryArchiveInvalidEntryException {
        checkPropertySet(PROP_DESCRIPTION, artifact);
    }

    /**
     * Adds the Require-Capability Strings from a bundle jar to the Map of
     * Require-Capabilities found
     *
     * @param zipSystem - the FileSystem mapping to the feature containing this bundle
     * @param bundle - the bundle within a zipped up feature
     * @param requiresMap - Map of Path to Require-Capability
     * @throws IOException
     */
    private static void addBundleManifestRequireCapability(FileSystem zipSystem,
                                                           Path bundle,
                                                           Map<Path, String> requiresMap) throws IOException {

        Path extractedJar = null;
        try {
            // Need to extract the bundles to read their manifest, can't find a way to do this in place.
            extractedJar = Files.createTempFile("unpackedBundle", ".jar");
            extractedJar.toFile().deleteOnExit();
            Files.copy(bundle, extractedJar, StandardCopyOption.REPLACE_EXISTING);

            Manifest bundleJarManifest = null;
            JarFile bundleJar = null;
            try {
                bundleJar = new JarFile(extractedJar.toFile());
                bundleJarManifest = bundleJar.getManifest();
            } finally {
                if (bundleJar != null) {
                    bundleJar.close();
                }
            }

            Attributes bundleManifestAttrs = bundleJarManifest.getMainAttributes();
            String requireCapabilityAttr = bundleManifestAttrs.getValue(REQUIRE_CAPABILITY_HEADER_NAME);
            if (requireCapabilityAttr != null) {
                requiresMap.put(bundle, requireCapabilityAttr);
            }

        } finally {
            if (extractedJar != null) {
                extractedJar.toFile().delete();
            }
        }
    }

    /**
     * Adds the Require-Capability Strings from a SUBSYSTEM.MF to the Map of
     * Require-Capabilities found
     *
     * @param esa - the feature file containing the SUBSYSTEM.MF
     * @param requiresMap - Map of Path to Require-Capability
     * @throws IOException
     */
    private static void addSubsystemManifestRequireCapability(File esa,
                                                              Map<Path, String> requiresMap) throws IOException {
        String esaLocation = esa.getAbsolutePath();
        ZipFile zip = null;
        try {
            zip = new ZipFile(esaLocation);
            Enumeration<? extends ZipEntry> zipEntries = zip.entries();
            ZipEntry subsystemEntry = null;
            while (zipEntries.hasMoreElements()) {
                ZipEntry nextEntry = zipEntries.nextElement();
                if ("OSGI-INF/SUBSYSTEM.MF".equalsIgnoreCase(nextEntry.getName())) {
                    subsystemEntry = nextEntry;
                    break;
                }
            }
            if (subsystemEntry == null) {
                ;
            } else {
                Manifest m = ManifestProcessor.parseManifest(zip.getInputStream(subsystemEntry));
                Attributes manifestAttrs = m.getMainAttributes();
                String requireCapabilityAttr = manifestAttrs.getValue(REQUIRE_CAPABILITY_HEADER_NAME);
                requiresMap.put(esa.toPath(), requireCapabilityAttr);
            }
        } finally {
            if (zip != null) {
                zip.close();
            }
        }
    }

}