/*******************************************************************************
 * Copyright (c) 2014,2024 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.internal.subsystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.AppForceRestart;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.ProvisionerConstants;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ImmutableAttributes;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ProvisioningDetails;
import com.ibm.ws.kernel.feature.provisioning.ActivationType;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Selector;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.wsspi.kernel.feature.LibertyFeature;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 * The feature cache maintains entries describing feature definitions:
 * feature name, characteristics of the file, and the features that definition includes.
 *
 * This class is not thread safe.
 */
public final class FeatureRepository implements FeatureResolver.Repository {
    private static final TraceComponent tc = Tr.register(FeatureRepository.class);

    private static void debug(String message, String parm) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, message, parm);
        }
    }

    private static void debug(String message) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, message);
        }
    }

    private static final int FEATURE_CACHE_VERSION = 4;

    private static final String EMPTY = "";

    /**
     * Answer the current cache version.
     *
     * That depends on whether versionless features are enabled, which
     * depends on the beta flag. (Eventually, the increated version will
     * be returned regardless.)
     *
     * Cache version 3 was in use pre-versionless features.
     *
     * Cache version 4 is in use with versionless features.
     *
     * Cache version 4 adds configured platforms and the value of the
     * platform environment variable. Storage of these values is necessary
     * to tell if the resolved feature configuration has changed between
     * restarts and between configuration updates.
     *
     * @return The current cache version.
     */
    public static int getCacheVersion() {
        return FEATURE_CACHE_VERSION;
    }

    private boolean isDirty; // Is the cache resource up to date.

    /**
     * Flag telling if no IO failures have occurred attempting
     * to read or write the repository cache. Cleared if an IO
     * failure occurs.
     *
     * Any single cache IO failure causes the caching function to
     * be disabled.
     *
     * There is no way to reset this flag.
     */
    private boolean cacheOk = true;

    /**
     * Binary store of the feature repository.
     *
     * May be null, in which case caching is not performed.
     */
    private final WsResource cacheResource;

    private final BundleContext bundleContext;

    /** List of currently resolved features */
    private volatile Set<String> resolvedFeatures = Collections.emptySet();

    private volatile Set<String> platforms = Collections.emptySet();

    private volatile String platformEnvVar = null;

    /** List of currently configured features */
    private volatile Set<String> configuredFeatures = Collections.emptySet();

    /** Flag to indicate that the list of configured features contains resolution errors */
    private volatile boolean configurationError = true;

    /** Map of symbolic name to subsystem feature definition */
    private final Map<String, SubsystemFeatureDefinitionImpl> installedFeatures = new HashMap<String, SubsystemFeatureDefinitionImpl>();

    /** Map of public features to short names */
    private final Map<String, String> publicFeatureNameToSymbolicName = new HashMap<String, String>();

    /** Map of public feature alternate name to correct feature name */
    private final Map<String, String> alternateFeatureNameToPublicName = new HashMap<String, String>();

    private final ConcurrentMap<String, LibertyFeatureServiceFactory> featureServiceFactories = new ConcurrentHashMap<String, LibertyFeatureServiceFactory>();

    private Map<File, SubsystemFeatureDefinitionImpl> knownGoodFeatureFiles;
    private Map<File, BadFeature> knownBadFeatureFiles;
    private List<SubsystemFeatureDefinitionImpl> autoFeatures;
    private Map<String, SubsystemFeatureDefinitionImpl> compatibilityFeatures;

    /**
     * Test initializer. Create a feature repository with
     * no cache resource and no bundle context.
     *
     * Mark the repository as dirty.
     */
    public FeatureRepository() {
        cacheResource = null;
        isDirty = true;
        bundleContext = null;
    }

    /**
     * Standard initializer. Create a feature repository with
     * the specified cache resource and bundle context.
     *
     * Mark the repository as clean.
     *
     * The repository is not yet usable: {@link #init()} must be
     * invoked to complete the initialization of the repository.
     *
     * @param cacheResource The resource used to cache bundle
     *                          information.
     * @param bundleContext The bundle context which is active for
     *                          the repository.
     */
    public FeatureRepository(WsResource res, BundleContext bundleContext) {
        cacheResource = res;
        this.bundleContext = bundleContext;
    }

    /**
     * Use to check if a feature name is a commonly used alternate to an existing feature name
     *
     * @param featureName
     * @return The existing feature name or null if no match
     */
    public String matchesAlternate(String featureName) {
        return alternateFeatureNameToPublicName.get(lowerFeature(featureName));
    }

    public boolean disableAllFeaturesOnConflict(String featureName) {
        SubsystemFeatureDefinitionImpl feature = (SubsystemFeatureDefinitionImpl) getFeature(featureName);
        if (feature != null) {
            return feature.getImmutableAttributes().disableOnConflict;
        }
        return false;
    }

    /**
     * Initialize the feature repository.
     *
     * Note if the repository is empty. That affects the
     * cache read and affects whether resolution related
     * fields are cleared.
     *
     * Read the cache, then read (or re-read) the feature
     * manifests.
     *
     * Conditionally, clear the resolution related data.
     */
    public void init() {
        isDirty = false;

        // TODO: This presumes that there will always
        //       be at one valid installed feature.
        boolean firstInit = installedFeatures.isEmpty();

        autoFeatures = new ArrayList<>();
        knownGoodFeatureFiles = new HashMap<>();
        knownBadFeatureFiles = new HashMap<>();
        compatibilityFeatures = new HashMap<>();

        // Read the cache, then read the feature manifest.
        // The installed features, the installed features
        // associated data, and the resolved features are
        // populated.

        readCache(firstInit);
        readFeatureManifests();

        // If something was out of sync with        // As a special case, if there was a change to the
        // installed features, if this is the first initialization,
        // then clear the resolved features data.

        if (isDirty && firstInit) {
            resolvedFeatures = Collections.emptySet();
            configuredFeatures = Collections.emptySet();
            // configurationError = false; // TODO?
            platforms = Collections.emptySet();
            platformEnvVar = null;
        }
    }

    /**
     * Strip provisioning data from the repository.
     *
     * First, write the cache to the repository.
     *
     * Then, strip installed features associated data.
     * Leave just the installed features.
     *
     * Also leave resolved feature information.
     */
    public void dispose() {
        storeCache();

        // PURGE provisioning data! BYE-BYE!!
        autoFeatures = null;
        knownGoodFeatureFiles = null;
        knownBadFeatureFiles = null;
        compatibilityFeatures = null;

        for (SubsystemFeatureDefinitionImpl def : installedFeatures.values()) {
            def.setProvisioningDetails(null);
        }
    }

    /**
     * This is called at the beginning of a provisioning operation. It re-initializes
     * or refreshes the cache against what is known in the filesystem.
     * <p>
     * Note that for update operations (rather than initial provisioning), cachedInstalledFeatures will
     * be pre-populated..
     */
    private void readCache(boolean firstInit) {
        if (!cacheOk || (cacheResource == null) || !cacheResource.exists()) {
            return;
        }

        List<SubsystemFeatureDefinitionImpl> cachedInstalledFeatures = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        Set<String> configured = new HashSet<>();
        Map<File, BadFeature> knownBad = new HashMap<>();
        boolean configError = false;
        Set<String> cachedPlatforms = new HashSet<>();
        String envVar = null;
        try (InputStream input = cacheResource.get();
                        DataInputStream in = new DataInputStream(new BufferedInputStream(input))) {

            if (in.readInt() != getCacheVersion()) {
                return; // not a version we understand; ignore the cache
            }
            int numFeatures = in.readInt();
            for (int i = 0; i < numFeatures; i++) {
                ImmutableAttributes cachedAttr = loadFeatureAttributes(in);
                ProvisioningDetails cachedDetails = loadProvisioningDetails(in, cachedAttr);
                String symbolicName = cachedAttr.symbolicName;

                SubsystemFeatureDefinitionImpl installedFeature = installedFeatures.get(symbolicName);

                // Get the immutable attributes from the cached feature element..
                ImmutableAttributes installedAttr = installedFeature == null ? null : installedFeature.getImmutableAttributes();

                if (!cachedAttr.featureFile.exists()) {
                    // The feature file of a cached feature no longer exists.
                    // Mark that the repository as dirty, and, if stored,
                    // remove the installed feature.
                    cachedAttr = null;
                } else if (installedAttr != null) {
                    // we had this value already; use it
                    cachedAttr = installedAttr;
                }
                // loadAttributes will return new attributes (didn't exist before),
                // the cachedAttributes (unchanged), or null (file no longer present OR invalid cache line )

                if (cachedAttr != null) {
                    // New shiny attributes...
                    if (installedAttr == cachedAttr) {
                        // woo-hoo!! cache hit!
                        // the provisioning details need to be reconstituted
                        installedFeature.setProvisioningDetails(cachedDetails);
                    } else {
                        // Only set to is dirty if not firstInit.
                        // On firstInit we are creating the definitions from cache for the first time here
                        if (!!!firstInit) {
                            isDirty = true;
                        }
                        installedFeature = new SubsystemFeatureDefinitionImpl(cachedAttr, cachedDetails);
                    }
                } else if (installedAttr != null) {
                    // so sad! the file went away or something is askew with the cache
                    isDirty = true;
                    installedFeatures.remove(symbolicName);
                    installedFeature = null;
                } else {
                    // I know this happens: null cachedAttr, null newAttr..
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "cacheAttr and newAttr BOTH null while reading cache", symbolicName);
                    }
                    isDirty = true;
                    installedFeature = null;
                }

                cachedInstalledFeatures.add(installedFeature);
            }

            int numResolved = in.readInt();
            for (int i = 0; i < numResolved; i++) {
                resolved.add(in.readUTF());
            }

            int numConfigured = in.readInt();
            for (int i = 0; i < numConfigured; i++) {
                configured.add(in.readUTF());
            }

            configError = in.readBoolean();

            int numBad = in.readInt();
            for (int i = 0; i < numBad; i++) {
                File f = new File(in.readUTF());
                long lastModified = in.readLong();
                long length = in.readLong();

                // If the file still exists, add it to our list. We'll check if anything
                // changed in readFeatureManifests
                if (f.isFile()) {
                    knownBad.put(f, new BadFeature(lastModified, length));
                }
            }

            //read in previous configured platforms
            int numPlatforms = in.readInt();
            for (int i = 0; i < numPlatforms; i++) {
                cachedPlatforms.add(in.readUTF());
            }

            //read previous platform environment variable from cache
            boolean hasPlatformEnv = in.readBoolean();
            if (hasPlatformEnv) {
                envVar = in.readUTF();
            }
        } catch (IOException e) {
            cacheWarning(e);
            return;
        }

        resolvedFeatures = Collections.unmodifiableSet(resolved);
        configuredFeatures = Collections.unmodifiableSet(configured);
        platforms = Collections.unmodifiableSet(cachedPlatforms);
        configurationError = configError;
        knownBadFeatureFiles.putAll(knownBad);
        platformEnvVar = envVar;

        for (SubsystemFeatureDefinitionImpl cachedInstalledFeature : cachedInstalledFeatures) {
            updateMaps(cachedInstalledFeature);
        }
    }

    /**
     * Return all the platforms the versionless feature is a part of.
     * ex. servlet would return javaee-6.0 through jakartaee-10.0
     * 
     * @return Set<String> platforms
     */
    public Set<String> getPlatformsForVersionlessFeature(String versionlessFeatureName){
        ProvisioningFeatureDefinition versionlessFeature = getFeature(versionlessFeatureName);
        Set<String> platforms = new HashSet<String>();

        if (versionlessFeature != null && versionlessFeature.isVersionless()) {
            for (ProvisioningFeatureDefinition child : findAllPossibleVersions(versionlessFeature)) {
                if (child == null) {
                    continue;
                }
                platforms.addAll(child.getPlatformNames());
            }
        }

        return platforms;
    }

    /**
     * Answer the versioned feature shortName if exists for the passed platform version or null if doesn't exist.
     *
     * @return String versionedFeatureName
     */
    public String getVersionlessFeatureVersionForPlatform(String versionlessFeatureName, String platformName) {
        ProvisioningFeatureDefinition versionlessFeature;
        versionlessFeature = getFeature(versionlessFeatureName);

        if (versionlessFeature != null && versionlessFeature.isVersionless()) {
            for (ProvisioningFeatureDefinition child : findAllPossibleVersions(versionlessFeature)) {
                if (child.getPlatformNames().contains(platformName))
                    return child.getIbmShortName();
            }
        }
        return null;
    }

    /**
     * Answer the list of public versioned features derived from the passed versionless feature or empty List if doesn't exist.
     *
     * @return List<ProvisioningFeatureDefinition>
     */
    private List<ProvisioningFeatureDefinition> findAllPossibleVersions(ProvisioningFeatureDefinition versionlessFeature) {
        List<ProvisioningFeatureDefinition> result = new ArrayList<>();
        for (FeatureResource dependency : versionlessFeature.getConstituents(null)) {
            result.add(getVersionedFeature(dependency.getSymbolicName()));

            String baseName = getFeatureBaseNameWithDash(dependency.getSymbolicName());
            List<String> tolerates = dependency.getTolerates();
            if (tolerates != null) {
                for (String toleratedVersion : tolerates) {
                    String featureName = baseName + toleratedVersion;
                    result.add(getVersionedFeature(featureName));
                }
            }
        }
        return result;
    }

    /**
     *
     * Answer the public versioned feature based on the internal versionless linking feature
     *
     * @param versionlessLinkingFeatureName
     * @return ProvisioningFeatureDefinition
     */
    private ProvisioningFeatureDefinition getVersionedFeature(String versionlessLinkingFeatureName) {
        ProvisioningFeatureDefinition result = null;
        ProvisioningFeatureDefinition feature = getFeature(versionlessLinkingFeatureName);
        if (feature != null) {
            //This is the versionless linking feature pointing to a public versioned feature
            for (FeatureResource versionedFeature : feature.getConstituents(null)) {
                //Find the right public feature (should only be one) - set the result
                ProvisioningFeatureDefinition versionedFeatureDef = getFeature(versionedFeature.getSymbolicName());
                if (versionedFeatureDef.getVisibility() != Visibility.PUBLIC) {
                    continue;
                }
                result = versionedFeatureDef;
            }
        }
        return result;
    }

    /**
     * Removes the version from the end of a feature symbolic name
     * <p>
     * The version is presumed to start after the last dash character in the name.
     * <p>
     * E.g. {@code getFeatureBaseNameWithDash("com.example.featureA-1.0")} returns {@code "com.example.featureA-"}
     *
     * @param nameAndVersion the feature symbolic name
     * @return the feature symbolic name with any version stripped
     */
    private String getFeatureBaseNameWithDash(String nameAndVersion) {
        int dashPosition = nameAndVersion.lastIndexOf('-');
        if (dashPosition != -1) {
            return nameAndVersion.substring(0, dashPosition + 1);
        } else {
            return nameAndVersion;
        }
    }

    /**
     * Removes the version from the end of a feature symbolic name
     * <p>
     * The version is presumed to start after the last dash character in the name.
     * <p>
     * E.g. {@code getFeatureBaseName("com.example.featureA-1.0")} returns {@code "com.example.featureA"}
     *
     * @param nameAndVersion the feature symbolic name
     * @return the feature symbolic name with any version stripped
     */
    public String getFeatureBaseName(String nameAndVersion) {
        int dashPosition = nameAndVersion.lastIndexOf('-');
        if (dashPosition != -1) {
            return nameAndVersion.substring(0, dashPosition);
        } else {
            return nameAndVersion;
        }
    }

    /**
     * Returns the common intersection of platform names between the set of features passed
     *
     *
     * @param features
     * @return Set<String>
     */
    public Set<String> getCommonPlatformsForFeatureSet(Set<String> features) {
        Set<String> commonPlatforms = null;
        for (String featureName : features) {
            ProvisioningFeatureDefinition versionedFeature = getFeature(featureName);
            if (versionedFeature != null && !versionedFeature.isVersionless()) {
                Set<String> featurePlatforms = new HashSet<>(versionedFeature.getPlatformNames());
                if (commonPlatforms == null)
                    commonPlatforms = featurePlatforms;
                else {
                    commonPlatforms.retainAll(featurePlatforms);
                }
            }
        }
        return commonPlatforms;
    }

    /**
     * Write the feature cache file
     */
    public void storeCache() {
        if (!cacheOk || (cacheResource == null) || !isDirty) {
            return;
        }

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(cacheResource.putStream()))) {
            Collection<Entry<ImmutableAttributes, ProvisioningDetails>> features = new ArrayList<>();
            for (SubsystemFeatureDefinitionImpl entry : installedFeatures.values()) {
                ImmutableAttributes imAttrs = entry.getImmutableAttributes();
                ProvisioningDetails provDetails = entry.getProvisioningDetails();
                if ((imAttrs != null) && (provDetails != null)) {
                    features.add(new SimpleEntry<>(imAttrs, provDetails));
                } else {
                    debug("Unable to write out " + entry.getFeatureName() +
                          " to cache because the provisioning detail: " + provDetails +
                          " or imAttrs: " + imAttrs +
                          " is null");
                }
            }
            out.writeInt(getCacheVersion());
            out.writeInt(features.size());
            for (Entry<ImmutableAttributes, ProvisioningDetails> entry : features) {
                writeFeatureAttributes(entry.getKey(),
                                       entry.getValue(),
                                       out);
            }

            Collection<String> curResolved = resolvedFeatures;
            out.writeInt(curResolved.size());
            for (String resolved : curResolved) {
                out.writeUTF(resolved);
            }

            Collection<String> curConfigured = configuredFeatures;
            out.writeInt(curConfigured.size());
            for (String configured : curConfigured) {
                out.writeUTF(configured);
            }

            out.writeBoolean(configurationError);

            Map<File, BadFeature> curKnownBad = knownBadFeatureFiles;
            out.writeInt(curKnownBad.size());
            for (Map.Entry<File, BadFeature> entry : knownBadFeatureFiles.entrySet()) {
                out.writeUTF(entry.getKey().getAbsolutePath());
                out.writeLong(entry.getValue().lastModified);
                out.writeLong(entry.getValue().length);
            }

            // Versionless features additions:
            //
            // [int] always: number of configured platforms
            // [UF8] zero or more: configured platforms
            // [bool] always: if the platform environment variable is non-null
            // [UTF8] optional: the value of the platform environment variable

            out.writeInt(platforms.size());
            for (String plat : platforms) {
                out.writeUTF(plat);
            }

            if (platformEnvVar == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeUTF(platformEnvVar);
            }

            isDirty = false;
        } catch (IOException e) {
            cacheWarning(e);
        }
    }

    static void writeFeatureAttributes(ImmutableAttributes iAttr, ProvisioningDetails details, DataOutputStream out) throws IOException {
        if ((iAttr == null) || (details == null)) {
            throw new NullPointerException("Both attributes and details are required for caching: attr=" + iAttr + ", details=" + details);
        }

        out.writeUTF(iAttr.bundleRepositoryType == null ? EMPTY : iAttr.bundleRepositoryType);
        out.writeUTF(iAttr.symbolicName);
        out.writeUTF(iAttr.featureFile == null ? EMPTY : iAttr.featureFile.getAbsolutePath());
        out.writeLong(iAttr.lastModified);
        out.writeLong(iAttr.length);
        out.writeUTF(iAttr.shortName == null ? EMPTY : iAttr.shortName);
        out.writeInt(iAttr.featureVersion);
        out.writeUTF(iAttr.visibility.toString());
        out.writeUTF(iAttr.appRestart.toString());

        out.writeInt(iAttr.version.getMajor());
        out.writeInt(iAttr.version.getMinor());
        out.writeInt(iAttr.version.getMicro());
        String qualifier = iAttr.version.getQualifier();
        out.writeUTF(qualifier == null ? EMPTY : qualifier);

        out.writeBoolean(iAttr.isAutoFeature);
        out.writeBoolean(iAttr.hasApiServices);
        out.writeBoolean(iAttr.hasApiPackages);
        out.writeBoolean(iAttr.hasSpiPackages);
        out.writeBoolean(iAttr.isSingleton);
        out.writeBoolean(iAttr.disableOnConflict);

        out.writeInt(iAttr.processTypes.size());
        for (ProcessType type : iAttr.processTypes) {
            out.writeUTF(type.toString());
        }

        out.writeUTF(iAttr.activationType.toString());

        out.writeInt(iAttr.alternateNames.size());
        for (String s : iAttr.alternateNames) {
            out.writeUTF(s);
        }

        out.writeInt(iAttr.platforms.size());
        for (String s : iAttr.platforms) {
            out.writeUTF(s);
        }

        // these attributes can be large so lets avoid the arbitrary limit of 65535 chars of writeUTF
        if (iAttr.isAutoFeature) {
            writeLongString(out, details.getCachedRawHeader(FeatureDefinitionUtils.IBM_PROVISION_CAPABILITY));
        }
        if (iAttr.hasApiServices) {
            writeLongString(out, details.getCachedRawHeader(FeatureDefinitionUtils.IBM_API_SERVICE));
        }
        if (iAttr.hasApiPackages) {
            writeLongString(out, details.getCachedRawHeader(FeatureDefinitionUtils.IBM_API_PACKAGE));
        }
        if (iAttr.hasSpiPackages) {
            writeLongString(out, details.getCachedRawHeader(FeatureDefinitionUtils.IBM_SPI_PACKAGE));
        }
    }

    static private void writeLongString(DataOutputStream out, String longString) throws IOException {
        byte[] data = longString.getBytes();
        if (data.length > 65535) {
            // need to special case long strings
            out.writeBoolean(true);
            out.writeInt(data.length);
            out.write(data);
        } else {
            out.writeBoolean(false);
            out.writeUTF(longString);
        }
    }

    static private String readLongString(DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            // this is a long string
            byte[] data = new byte[in.readInt()];
            in.readFully(data);
            return new String(data, StandardCharsets.UTF_8);
        } else {
            // normal string
            return in.readUTF();
        }
    }

    static ImmutableAttributes loadFeatureAttributes(DataInputStream in) throws IOException {
        String repositoryType = in.readUTF();
        if (repositoryType.isEmpty()) {
            repositoryType = EMPTY;
        }
        String symbolicName = in.readUTF();
        String featurePath = in.readUTF();
        File featureFile = featurePath.isEmpty() ? null : new File(featurePath);
        long lastModified = in.readLong();
        long fileSize = in.readLong();
        String shortName = in.readUTF();
        if (shortName.isEmpty()) {
            shortName = null;
        }
        int featureVersion = in.readInt();
        Visibility visibility = valueOf(in.readUTF(), Visibility.PRIVATE);
        AppForceRestart appRestart = valueOf(in.readUTF(), AppForceRestart.NEVER);

        Version version = new Version(in.readInt(), in.readInt(), in.readInt(), in.readUTF());

        boolean isAutoFeature = in.readBoolean();
        boolean hasApiServices = in.readBoolean();
        boolean hasApiPackages = in.readBoolean();
        boolean hasSpiPackages = in.readBoolean();
        boolean isSingleton = in.readBoolean();
        boolean disableOnConflict = in.readBoolean();

        int processTypeNum = in.readInt();
        EnumSet<ProcessType> processTypes = EnumSet.noneOf(ProcessType.class);
        for (int i = 0; i < processTypeNum; i++) {
            processTypes.add(valueOf(in.readUTF(), ProcessType.SERVER));
        }

        ActivationType activationType = valueOf(in.readUTF(), ActivationType.SEQUENTIAL);

        int altNamesCount = in.readInt();
        List<String> altNames = new ArrayList<>(altNamesCount);
        for (int x = 0; x < altNamesCount; x++) {
            altNames.add(in.readUTF());
        }

        int platformsCount = in.readInt();
        List<String> platforms = new ArrayList<>(platformsCount);
        for (int i = 0; i < platformsCount; i++) {
            platforms.add(in.readUTF());
        }

        return new ImmutableAttributes(repositoryType, symbolicName, shortName, altNames, featureVersion, visibility, appRestart,
                                       version, featureFile, lastModified, fileSize, isAutoFeature, hasApiServices, hasApiPackages,
                                       hasSpiPackages, isSingleton, disableOnConflict, processTypes, activationType, platforms);
    }

    static ProvisioningDetails loadProvisioningDetails(DataInputStream in, ImmutableAttributes iAttr) throws IOException {
        String autoFeatureCapability = iAttr.isAutoFeature ? readLongString(in) : null;
        String apiServices = iAttr.hasApiServices ? readLongString(in) : null;
        String apiPackages = iAttr.hasApiPackages ? readLongString(in) : null;
        String spiPackages = iAttr.hasSpiPackages ? readLongString(in) : null;
        return new ProvisioningDetails(iAttr, autoFeatureCapability, apiServices, apiPackages, spiPackages);
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore(IllegalArgumentException.class)
    public static <T extends Enum<T>> T valueOf(String name, T defaultValue) {
        try {
            return (T) Enum.valueOf(defaultValue.getClass(), name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Update this repository with feature manifests located in the "lib/features"
     * folder relative to the installation directory of all registered
     * bundle repositories.
     */
    private void readFeatureManifests() {
        // For each bundle repository, we need to look at the manifest files
        for (final BundleRepositoryHolder holder : BundleRepositoryRegistry.holders()) {

            // Checking isDirectory is just paranoia: Things don't get into the BundleRepositoryRegistry if they don't exist
            File libFeatureDir = new File(holder.getInstallDir(), ProvisionerConstants.LIB_FEATURE_PATH);
            if (libFeatureDir.isDirectory()) {

                // Let's look at all the manifest files in this dir
                libFeatureDir.listFiles(new FileFilter() {
                    /**
                     * Process a file as a potential feature manifest file.
                     *
                     * Skip files which are not simple files or which are not manifest files.
                     *
                     * Don't process files which were previously unreadable, and which have
                     * no changes: Keep the file as a 'bad' feature file.
                     *
                     * Don't process files which were previously read, and which have no changes.
                     * Keep the file as a 'good' feature file and keep using the previously
                     * read feature data.
                     *
                     * Always answer false. The {@link FileFilter#accept(File)} API is used
                     * to shift processing to the iteration step. Effects of processing each
                     * feature manifest are side effects of accepting the file.
                     *
                     * @param file   A candidate feature manifest file.
                     *
                     * @param holder A bundle repository holder used to process feature definitions.
                     * @param file   A possible feature manifest.
                     */
                    @Override
                    public boolean accept(File file) {
                        // Note: we always return false. We do the work as we see the files,
                        // instead of iterating to build a list that we then have to iterate over again...

                        if (file == null) {
                            return false; // NEXT!
                        }

                        if (!file.isFile()) {
                            return false; // NEXT!
                        }

                        String name = file.getName();
                        int pos = name.lastIndexOf('.');
                        if (pos < 0) {
                            return false; // NEXT!
                        }

                        // Look only at the file extension, case insensitively
                        if (!name.regionMatches(true, pos, ".mf", 0, 3)) {
                            return false; // NEXT!
                        }

                        // Don't process a feature file which failed to read previously,
                        // and which has no changes since the prior read.
                        BadFeature bad = knownBadFeatureFiles.get(file);
                        if (isFeatureStillBad(file, bad)) {
                            return false; // NEXT!
                        }

                        // Test: if we've seen this file before, is it the same as what we saw last time?
                        SubsystemFeatureDefinitionImpl def = knownGoodFeatureFiles.get(file);
                        if (isCachedEntryValid(file, def)) {
                            return false; // NEXT!
                        }

                        // Either we haven't seen it, or it changed, so we need to build a new
                        // definition for it. We also know the cache is dirty
                        isDirty = true;

                        try {
                            // We need to start with the details, as we'll have to read the information from the manifest
                            ProvisioningDetails details = new ProvisioningDetails(file, null);

                            // Now build the immutables
                            ImmutableAttributes attr = FeatureDefinitionUtils.loadAttributes(holder.getFeatureType(),
                                                                                             file,
                                                                                             details);

                            if (!attr.isSupportedFeatureVersion()) {
                                debug("Unsupported feature version [ " + file.getAbsolutePath() + " ]"); // TODO: NLS
                                knownBadFeatureFiles.put(file, new BadFeature(file.lastModified(), file.length()));
                                return false; // NEXT
                            }

                            // we're good to go: basic attributes read and a good feature version!
                            def = new SubsystemFeatureDefinitionImpl(attr, details);

                            // update cache(s) with new definition
                            updateMaps(def);

                        } catch (IOException e) {
                            debug("Exception reading feature manifest [ " + file.getAbsolutePath() + " ]", e.toString()); // TODO: NLS                            // TODO: NLS MESSAGE
                            knownBadFeatureFiles.put(file, new BadFeature(file.lastModified(), file.length()));
                        }
                        return false; // NEXT!
                    }
                });
            }
        }
    }

    boolean isFeatureStillBad(File f, BadFeature bf) {
        return (bf != null)
               && (f.lastModified() == bf.lastModified)
               && (f.length() == bf.length);
    }

    // Remove milliseconds from timestamp values to address inconsistencies in container file systems
    long reduceTimestampPrecision(long value) {
        return (value / 1000) * 1000;
    }

    boolean isCachedEntryValid(File f, SubsystemFeatureDefinitionImpl def) {
        if (def != null) {
            ImmutableAttributes cachedAttr = def.getImmutableAttributes();

            // See if the file has changed: if it has, we need to start over
            if (reduceTimestampPrecision(cachedAttr.lastModified) == reduceTimestampPrecision(f.lastModified())) {
                if (cachedAttr.length == f.length()) {
                    return true;
                }
            }

            // If we got here, something changed with the entry we had:
            // could be anything inside the file, so be thorough
            // -- knownFeature entry will be replaced by caller
            installedFeatures.remove(cachedAttr.symbolicName);
            publicFeatureNameToSymbolicName.remove(lowerFeature(cachedAttr.featureName));
            if (cachedAttr.isAutoFeature) {
                autoFeatures.remove(def);
            }

            String platform = getCompatibilityPlatform(def, cachedAttr);
            if (platform != null) {
                removeCompatibilityFeature(platform);
            }
        }

        return false;
    }

    private void updateMaps(SubsystemFeatureDefinitionImpl def) {
        if (def == null) {
            return;
        }

        ImmutableAttributes attr = def.getImmutableAttributes();

        // Update the feature cache: symbolic name to definition
        SubsystemFeatureDefinitionImpl oldDef = installedFeatures.put(attr.symbolicName, def);

        if ((oldDef != null) && !oldDef.equals(def)) {
            // UH-OH!! we have a symbolic name collision, which is just not supposed to happen.
            // a) keep the first one
            // b) Create an FFDC record indicating this happened
            // c) TODO: NLS message
            installedFeatures.put(attr.symbolicName, oldDef);
            FeatureManifestException fme = new FeatureManifestException("Duplicate symbolic name: " + attr.symbolicName
                                                                        + ", " + def.getFeatureDefinitionFile().getAbsolutePath()
                                                                        + " will be ignored. The file " + oldDef.getFeatureDefinitionFile().getAbsolutePath()
                                                                        + " will be used instead.",
                                                                        (String) null); // TODO: nls message here..

            FFDCFilter.processException(fme, this.getClass().getName(), "updateMaps",
                                        this, new Object[] { oldDef, def });

            // Ignore this definition...
            File file = attr.featureFile;
            knownBadFeatureFiles.put(file, new BadFeature(file.lastModified(), file.length()));
            return;
        }

        // Remember that we've seen this file: file to definition
        knownGoodFeatureFiles.put(attr.featureFile, def);

        // If there is a public feature name,
        // populate the map with down-case featureName to real symbolic name
        // populate the map with down-case symbolicName to real symbolic name
        // Note: we only ignore case when looking up public feature names!
        if (!attr.featureName.equals(attr.symbolicName)) {
            publicFeatureNameToSymbolicName.put(lowerFeature(attr.featureName), attr.symbolicName);
        }
        if (def.getVisibility() == Visibility.PUBLIC) {
            publicFeatureNameToSymbolicName.put(lowerFeature(attr.symbolicName), attr.symbolicName);
        } else if (def.getVisibility() == Visibility.PRIVATE) {
            if ((attr.platforms != null) && !attr.platforms.isEmpty()) {
                for(String plat : def.getPlatformNames()){
                    putCompatibilityFeature(plat, def);
                }
            }
        }

        // populate mapping from known, commonly used alternative names to allow hints when the wrong feature
        // name is specified in a server config.
        for (String s : attr.alternateNames) {
            alternateFeatureNameToPublicName.put(s, attr.featureName);
        }

        if (def.isCompatibility()) {
            compatibilityFeatures.put(def.getPlatformName().toLowerCase(), def);
        }

        // If this is an auto-feature, add it to that collection
        // we're going with the bold assertion that
        if (attr.isAutoFeature) {
            autoFeatures.add(def);
        }

        // TODO: In actual processing, auto features are always
        //       private features.  However, in the feature test data,
        //       there are auto features which are public, or which have
        //       unspecified visibility.
        //
        //       Allowing public auto-features is problematic.  However,
        //       to preserve prior behavior, they are still handled.
        //
        //       The TODO is to move this step into the PRIVATE visibility
        //       case.  That would require changes to test data.
    }

    private void cacheWarning(IOException ioe) {
        if (cacheOk) {
            cacheOk = false;
            Tr.warning(tc, "UPDATE_BUNDLE_CACHE_WARNING", new Object[] { cacheResource.toExternalURI(), ioe.toString() });
        }
    }

    public void setPlatforms(Set<String> platforms) {
        this.platforms = platforms;
    }

    public Set<String> getPlatforms() {
        return platforms;
    }

    public void setPlatformEnvVar(String platformEnvVar) {
        this.platformEnvVar = consumeEmpty(platformEnvVar);
    }

    public String getPlatformEnvVar() {
        return platformEnvVar;
    }

    @Deprecated
    public void setInstalledFeatures(Set<String> newResolvedFeatures, Set<String> newConfiguredFeatures, boolean configurationError) {
        setResolvedFeatures(newResolvedFeatures, newConfiguredFeatures, configurationError, platforms, platformEnvVar);
    }

    @Deprecated
    public Set<String> getInstalledFeatures() {
        return getResolvedFeatures();
    }

    /**
     * Set the configured and resolved features.
     *
     * Store the features using new, un-modifiable, storage. Do not
     * use references to the parameter feature sets.
     *
     * @param newResolvedFeatures    Symbolic names of resolved features.
     * @param newConfiguredFeatures  Symbolic names of configured features.
     * @param configurationError     True or false, telling if any errors occured
     *                                   during feature resolution.
     * @param newConfiguredPlatforms Newly configured platforms.
     * @param platformEnv            New platform environment value. (This is not expected
     *                                   to be different than the current value.)
     */
    public void setResolvedFeatures(Set<String> newResolvedFeatures,
                                    Set<String> newConfiguredFeatures, boolean configurationError,
                                    Set<String> newConfiguredPlatforms, String platformEnv) {
        Set<String> current = resolvedFeatures;
        if (!current.equals(newResolvedFeatures)) {
            isDirty = true;
        }
        if (newResolvedFeatures.isEmpty()) {
            resolvedFeatures = Collections.emptySet();
        } else {
            resolvedFeatures = Collections.unmodifiableSet(new HashSet<String>(newResolvedFeatures));
        }

        current = configuredFeatures;
        if (!current.equals(newConfiguredFeatures)) {
            isDirty = true;
        }
        if (newConfiguredFeatures.isEmpty()) {
            configuredFeatures = Collections.emptySet();
        } else {
            configuredFeatures = Collections.unmodifiableSet(new HashSet<String>(newConfiguredFeatures));
        }

        current = platforms;
        if (!current.equals(newConfiguredPlatforms)) {
            isDirty = true;
        }
        if (newConfiguredPlatforms.isEmpty()) {
            platforms = Collections.emptySet();
        } else {
            platforms = Collections.unmodifiableSet(new HashSet<String>(newConfiguredPlatforms));
        }
        platformEnvVar = consumeEmpty(platformEnv);

        this.configurationError = configurationError;
    }

    public Set<String> getResolvedFeatures() {
        return resolvedFeatures;
    }

    public Set<String> getConfiguredFeatures() {
        return configuredFeatures;
    }

    /**
     * Tell if there was an error when the feature were resolved.
     *
     * (The meaning is in the sense of "the configured features are
     * not valid" which is to say "an error occurred when resolving
     * the configured features".)
     *
     * @return True or false telling if there was a feature resolution
     *         error.
     */
    public boolean hasConfigurationError() {
        return configurationError;
    }

    @Deprecated
    public void copyInstalledFeaturesTo(Set<String> features) {
        copyResolvedFeaturesTo(features);
    }

    /**
     * Copies the active list of resolved features into the given set.
     */
    public void copyResolvedFeaturesTo(Set<String> features) {
        features.addAll(resolvedFeatures);
    }

    public boolean emptyFeatures() {
        return resolvedFeatures.isEmpty();
    }

    public boolean featureSetEquals(Set<String> newFeatureSet) {
        if (newFeatureSet == null) {
            return false;
        }

        return !isDirty && newFeatureSet.equals(resolvedFeatures);
    }

    /**
     * Answer the collection of auto-features.
     *
     * These are a distinguished category of private features. These
     * features are provisioned conditionally based on the resolution of
     * other features.
     *
     * @return The collection of auto-features.
     */
    @Override
    public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
        if (autoFeatures == null) {
            throw new IllegalStateException("Method called outside of provisioining operation");
        }

        return asProvisioningFeatureDefinitionCollection(Collections.unmodifiableCollection(autoFeatures));
    }

    /**
     * Answer the compatibility platform from a private
     * versionless compatibility feature.
     *
     * Such features are the feature analogues to platform values. When a platform
     * is specified, either through a platform element, or through an environment
     * variable, the platform is matched to a feature definition based on the
     * platform of the feature.
     *
     * This use of feature platforms is specific to private features. Only private
     * features which are compatibility features can have a platform name.
     *
     * At most one feature which is a versionless compatibility feature may
     * have a particular platform value. If more than one has the same platform
     * value, the preconditions of {@link #removeFromMaps} and {@link #updateMaps}
     * are not satisfied and the compatibility features table won't be accurately
     * maintained.
     *
     * @param def  A feature definition which may be a compatibility feature.
     * @param attr The attributes of the feature.
     *
     * @return Null, if the feature is not a versionless compatibility feature.
     *         The platform name (which includes a version!) if the feature is a
     *         compatibility feature.
     */
    public static String getCompatibilityPlatform(SubsystemFeatureDefinitionImpl def, ImmutableAttributes attr) {
        if (def.getVisibility() != Visibility.PRIVATE) {
            return null;
        } else if ((attr.platforms == null) || attr.platforms.isEmpty()) {
            return null;
        } else {
            return def.getPlatformName();
        }
    }

    /**
     * Answer the compatibility feature that has a specified platform name.
     *
     * @param platformName A platform of a compatibility feature.
     *
     * @return The compatibility feature that has the specified platform name.
     */
    // @Override
    public ProvisioningFeatureDefinition getCompatibilityFeature(String platformName) {
        return compatibilityFeatures.get(lowerFeature(platformName));
    }

    private void putCompatibilityFeature(String platformName, SubsystemFeatureDefinitionImpl featureDef) {
        compatibilityFeatures.put(lowerFeature(platformName), featureDef);
    }

    private void removeCompatibilityFeature(String platformName) {
        compatibilityFeatures.remove(lowerFeature(platformName));
    }

    @SuppressWarnings("unchecked")
    private Collection<ProvisioningFeatureDefinition> asProvisioningFeatureDefinitionCollection(Collection<? extends ProvisioningFeatureDefinition> collection) {
        return (Collection<ProvisioningFeatureDefinition>) collection;
    }

    @Trivial
    private static String toFeatureNameList(Collection<String> collection) {
        StringBuilder builder = new StringBuilder();
        for (String entry : collection) {
            builder.append(entry);
            builder.append(',');
        }

        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    // @Override
    public List<ProvisioningFeatureDefinition> select(Selector<ProvisioningFeatureDefinition> selector) {
        if (selector == null) {
            return new ArrayList<>(installedFeatures.values());
        } else {
            List<ProvisioningFeatureDefinition> selected = new ArrayList<>(installedFeatures.size());
            installedFeatures.values().forEach((ProvisioningFeatureDefinition def) -> {
                if (selector.test(def)) {
                    selected.add(def);
                }
            });
            return selected;
        }
    }

    @Override
    public List<ProvisioningFeatureDefinition> getFeatures() {
        return new ArrayList<>(installedFeatures.values());
    }

    @Override
    public ProvisioningFeatureDefinition getFeature(String featureName) {
        SubsystemFeatureDefinitionImpl result = installedFeatures.get(featureName);
        if (result == null) {
            String name = publicFeatureNameToSymbolicName.get(lowerFeature(featureName));
            result = installedFeatures.get(name);
        }
        return result;
    }

    public Map<String, SubsystemFeatureDefinitionImpl> getAllFeatures() {
        return installedFeatures;
    }

    private static final String TOLERATE_PREFIX = "tolerates.";

    @Override
    public List<String> getConfiguredTolerates(String baseSymbolicName) {
        if (bundleContext == null) {
            return Collections.emptyList();
        }
        String tolerates = bundleContext.getProperty(TOLERATE_PREFIX + baseSymbolicName);
        if (tolerates == null) {
            return Collections.emptyList();
        }
        String[] parts = tolerates.split(",");
        List<String> result = new ArrayList<String>();
        for (String p : parts) {
            if (!"".equals(p.trim())) {
                result.add(p.trim());
            }
        }
        return result;
    }

    /**
     * Convert a feature name to a canonical, lower case value.
     *
     * The feature name may include a product extension prefix value.
     * Do not convert the prefix value.
     *
     * (Feature type in featureLocations is case specific.
     * For the bundles to be found, the case of the product extension
     * in the feature name must be preserved. Otherwise, an incorrect
     * bundle repository type may be put into the feature definition
     * when it is loaded from the feature cache.)
     *
     * @param feature A feature name.
     *
     * @return The feature name with the suffix converted to lower case.
     */
    @Trivial
    public static String lowerFeature(String inFeature) {
        if ((inFeature == null) || inFeature.isEmpty()) {
            return inFeature;
        }

        // Preserve the prefix (no case shift)
        int colonIndex = inFeature.indexOf(":");

        if ((colonIndex > -1) && (inFeature.length() > colonIndex)) {
            // Put together preserved extension name with the downcased feature name
            return inFeature.substring(0, colonIndex).trim() + ':' + inFeature.substring(colonIndex + 1).trim().toLowerCase(Locale.ENGLISH);
        } else {
            return inFeature.toLowerCase(Locale.ENGLISH).trim();
        }
    }

    /**
     * Information for a feature file which could not be read.
     *
     * TODO: Rename this to 'FileSignature'. The information is
     * last modified and length values of a file. The information
     * is used to tell if a file has changed.
     */
    private static class BadFeature {
        final long lastModified;
        final long length;

        BadFeature(long lastModified, long length) {
            this.lastModified = lastModified;
            this.length = length;
        }
    }

    /**
     * Register all of the resolved features as service factories.
     *
     * Start by unregistering any previously registered features which
     * are no longer resolved.
     *
     * Finish by registering all resolved features. Update any previously
     * resolved features which are still resolved. Do a new registration
     * of any newly resolved feature.
     *
     * Do nothing if the bundle context is null, which indicates that
     * the repository is being used outside of a running server image.
     * (For example, the repository may be used in a unit test environment.)
     */
    public void updateServices() {
        if (bundleContext == null) {
            // do nothing; not really in a running system (unit tests etc.)
            return;
        }
        Set<String> resolvedSymbolicNames = new HashSet<String>();
        for (String featureName : resolvedFeatures) {
            String symbolicName = publicFeatureNameToSymbolicName.get(lowerFeature(featureName));
            if (symbolicName != null) {
                resolvedSymbolicNames.add(symbolicName);
            }
        }
        Set<String> removedFactories = new HashSet<String>(featureServiceFactories.keySet());
        removedFactories.removeAll(resolvedSymbolicNames);
        for (String currentFactorySymbolicName : removedFactories) {
            LibertyFeatureServiceFactory factory = featureServiceFactories.remove(currentFactorySymbolicName);
            if (factory != null) {
                factory.unregisterService();
            }
        }

        for (String currentFactorySymbolicName : resolvedSymbolicNames) {
            SubsystemFeatureDefinitionImpl featureDef = installedFeatures.get(currentFactorySymbolicName);
            if (featureDef != null) {
                LibertyFeatureServiceFactory factory = new LibertyFeatureServiceFactory();
                LibertyFeatureServiceFactory previous = featureServiceFactories.putIfAbsent(currentFactorySymbolicName, factory);
                factory = previous != null ? previous : factory;
                factory.update(featureDef, bundleContext);
            }
        }
    }

    private static String FEATURE_SERVICE_NAME = "ibm.featureName";
    private static String FEATURE_SERVICE_SYMBOLIC_NAME = "osgi.symbolicName";
    private static String FEATURE_SERVICE_VERSION = "osgi.version";
    private static String FEATURE_SERVICE_CATEGORY = "osgi.category";
    private static final String FEATURE_SUBSYSTEM_CATEGORY = "Subsystem-Category";

    /**
     * Service factory for a liberty feature.
     *
     * Each feature definition is also a liberty feature, meaning feature definitions and
     * liberty features are the same objects.
     *
     * This is registered as a ServiceFactory, so that each bundle requesting an instance of the
     * Liberty feature makes a separate request for the service. This allows us to maintain a usage count.
     * When the usage count is back to zero, we can indicate that the details aren't needed anymore
     * (to satisfy service requests).
     */
    private static class LibertyFeatureServiceFactory implements ServiceFactory<LibertyFeature> {
        private final Hashtable<String, Object> serviceProps = new Hashtable<String, Object>();
        private ServiceRegistration<LibertyFeature> registration;
        private volatile SubsystemFeatureDefinitionImpl _featureDef;

        /**
         * Ensure that the provisioning details of the associated feature
         * definition are assigned. Read them from the feature manifest
         * file if necessary.
         *
         * Increase the feature definition use count: See
         * {@link SubsystemFeatureDefinitionImpl#setProvisioningDetails}.
         *
         * @param bundle          The active bundle. Currently unused.
         * @param useRegistration The service registration. Currently unused.
         *
         * @return The associated feature definition (as a liberty feature).
         */
        @Override
        public LibertyFeature getService(Bundle bundle, ServiceRegistration<LibertyFeature> useRegistration) {
            SubsystemFeatureDefinitionImpl current = _featureDef;
            ProvisioningDetails details = current.getProvisioningDetails();
            if (details == null) {
                try {
                    details = new ProvisioningDetails(current.getImmutableAttributes().featureFile, null);
                } catch (IOException e) {
                    // Unlikely, as this feature manifest has been read before.
                    debug("An exception occurred while reading the feature manifest", e.toString());
                }
            }
            current.setProvisioningDetails(details);

            return current;
        }

        /**
         * Clear the provisioning details of the associated feature definition.
         *
         * Decrease the feature definition use count: See
         * {@link SubsystemFeatureDefinitionImpl#setProvisioningDetails}.
         *
         * @param bundle          The active bundle. Currently unused.
         * @param useRegistration The service registration. Currently unused.
         * @param service         The feature service. Usually the same as the feature
         *                            definition. Currently unused.
         */
        @Override
        public void ungetService(Bundle bundle, ServiceRegistration<LibertyFeature> useRegistration, LibertyFeature service) {
            _featureDef.setProvisioningDetails(null);
        }

        /**
         * Register this service factory with the bundle context
         * using the generated service properties. Record the
         * registration which is obtained.
         *
         * @param bundleContext The bundle context with which to register
         *                          this service factory.
         */
        void registerService(BundleContext bundleContext) {
            registration = bundleContext.registerService(LibertyFeature.class, this, serviceProps);
        }

        /**
         * Unregister this service factory.
         *
         * Do nothing if this service factory has no recorded registration.
         */
        void unregisterService() {
            if (registration != null) {
                registration.unregister();
            }
        }

        /**
         * Associate this service factory with a feature definition.
         *
         * Do nothing if the feature definition is the same as previously associated.
         *
         * A service factory is created with empty service properties, a null feature
         * definition, and a null registration.
         *
         * The initial invocation of update will detect a null associated feature
         * definition and will create the association.
         *
         * Subsequent invocations test the new feature definition against the
         * previously associated feature definition. If these are the same object,
         * no update is performed. Otherwise, any prior registration is cleared,
         * the service factory is associated with the new feature definition, and
         * the service is re-registered to the bundle context.
         *
         * @param featureDef    A feature definition to associate with this service
         *                          factory.
         * @param bundleContext Bundle context with which to register this service
         *                          factory.
         */
        void update(SubsystemFeatureDefinitionImpl featureDef, BundleContext bundleContext) {
            if (featureDef == _featureDef) {
                // nothing to do
                return;
            }
            // oh great, cached changed or something
            // unregister the existing one
            if (registration != null) {
                registration.unregister();
            }
            _featureDef = featureDef;
            getServiceProps();
            registerService(bundleContext);
        }

        /**
         * Store feature values into the service properties.
         *
         * Store the feature name, the feature symbolic name, the feature
         * version, and the feature subsystem category value (from the feature
         * header).
         */
        private void getServiceProps() {
            serviceProps.clear();
            serviceProps.put(FEATURE_SERVICE_NAME, _featureDef.getFeatureName());
            serviceProps.put(FEATURE_SERVICE_SYMBOLIC_NAME, _featureDef.getSymbolicName());
            serviceProps.put(FEATURE_SERVICE_VERSION, _featureDef.getVersion());
            String category = _featureDef.getHeader(FEATURE_SUBSYSTEM_CATEGORY);
            if (category != null) {
                serviceProps.put(FEATURE_SERVICE_CATEGORY, category.split("\\s*,\\s*"));
            }
        }
    }

    @Trivial
    private static String consumeEmpty(String s) {
        return (((s != null) && s.isEmpty()) ? null : s);
    }

    public boolean isDirty() {
        return isDirty;
    }

    @Deprecated
    public void removeInstalledFeature(String feature) {
        removeResolvedFeature(feature);
    }

    /**
     * Remove a resolved feature. Mark that there is a feature resolution
     * error.
     *
     * This is intended to be used to remove features that failed during
     * resolution because of java version restrictions.
     *
     * @param feature The symbolic name of a feature which is to be removed
     *                    as a resolved feature.
     */
    public void removeResolvedFeature(String feature) {
        this.configurationError = true;
        HashSet<String> newResolvedFeatures = new HashSet<>(resolvedFeatures);
        if (newResolvedFeatures.remove(feature)) {
            resolvedFeatures = newResolvedFeatures.isEmpty() ? Collections.<String> emptySet() : Collections.unmodifiableSet(newResolvedFeatures);
        }
    }

    /**
     * Tell if the configured features have changed.
     *
     * Always answer true if the configured features are dirty, or if there
     * was an error performing feature cache IO.
     *
     * If versionless features are enabled, tell if there are changes to
     * either the configured platforms or to the value of platform environment
     * variable.
     *
     * Tell if there are changes to the configured features, or if any of
     * the configured features is not present in the repository.
     *
     * This is used between server restarts and during configuration updates.
     *
     * During a server restart, all of the values might have changed.
     *
     * During a configuration update, the value of the platform environment
     * variable cannot change.
     *
     * @param newConfiguredFeatures     The new configured features.
     * @param newConfiguredPlatforms    The new configured platforms.
     * @param newPlatformEnvironmentVar The new value of the platform environment
     *                                      variable.
     *
     * @return True or false telling if the values have changed, or if the
     *         repository is dirty, or has a resolution error.
     */
    public boolean areConfiguredFeaturesGood(Set<String> newConfiguredFeatures,
                                             Set<String> newConfiguredPlatforms,
                                             String newPlatformEnvironmentVar) {

        if (isDirty() || hasConfigurationError()) {
            return false;
        }

        if (!getPlatforms().equals(newConfiguredPlatforms)) {
            return false;
        }
        if (!equals(getPlatformEnvVar(), newPlatformEnvironmentVar)) {
            return false;
        }
        if (!getConfiguredFeatures().equals(newConfiguredFeatures)) {
            return false;
        }

        for (String resolvedFeature : getResolvedFeatures()) {
            if (getFeature(resolvedFeature) == null) {
                return false;
            }
        }
        return true;
    }

    @Trivial
    private static boolean equals(String s0, String s1) {
        if (s0 == null) {
            return (s1 == null);
        } else {
            if (s1 == null) {
                return false;
            } else {
                return s0.equals(s1);
            }
        }
    }
}