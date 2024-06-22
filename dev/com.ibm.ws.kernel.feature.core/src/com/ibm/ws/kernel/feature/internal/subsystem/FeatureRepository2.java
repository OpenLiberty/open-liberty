/*******************************************************************************
* Copyright (c) 2014, 2024 IBM Corporation and others.
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
import java.io.OutputStream;
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
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.wsspi.kernel.feature.LibertyFeature;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 * Feature definition repository.
 */
public final class FeatureRepository2 implements FeatureResolver.Repository {
    private static final TraceComponent tc = Tr.register(FeatureRepository2.class);

    public static final boolean isBeta = Boolean.valueOf(System.getProperty("com.ibm.ws.beta.edition"));

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

    //

    @Trivial
    private static String consumeEmpty(String s) {
        return (((s != null) && s.isEmpty()) ? null : s);
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

    @Trivial
    private static <T> Set<T> asUnmodifiable(Collection<T> source) {
        if ((source == null) || source.isEmpty()) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(new HashSet<T>(source));
        }
    }

    //

    private static final int FEATURE_CACHE_VERSION = 3;

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
        return (isBeta ? (FEATURE_CACHE_VERSION + 1) : FEATURE_CACHE_VERSION);
    }

    /**
     * Test initializer. Create a feature repository with
     * no cache resource and no bundle context.
     *
     * Mark the repository as dirty.
     */
    public FeatureRepository2() {
        this.cacheResource = null;
        this.isDirty = true;

        this.bundleContext = null;
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
    public FeatureRepository2(WsResource cacheResource, BundleContext bundleContext) {
        this.cacheResource = cacheResource;
        this.isDirty = false;

        this.bundleContext = bundleContext;
    }

    //

    private final BundleContext bundleContext;

    private static final String TOLERATE_PREFIX = "tolerates.";

    /**
     * Answer the tolerates which are configured for a base feature.
     *
     * The value is stored as a bundle property using the key {@link #TOLERATE_PREFIX}
     * plus <code>"." + baseSymbolicName</code>.
     *
     * Answer an empty collection if the bundle context is null, or if no
     * tolerates are configured for the base symbolic name.
     *
     * @return The configured tolerates of the base feature.
     */
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
        List<String> versions = new ArrayList<String>(parts.length);
        for (String p : parts) {
            p = p.trim();
            if (!p.isEmpty()) {
                versions.add(p);
            }
        }
        return versions;
    }

    //

    /**
     * Binary store of the feature repository.
     *
     * May be null, in which case caching is not performed.
     */
    private final WsResource cacheResource;

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

    private void cacheWarning(IOException ioe) {
        if (cacheOk) {
            cacheOk = false;
            Tr.warning(tc, "UPDATE_BUNDLE_CACHE_WARNING", new Object[] { cacheResource.toExternalURI(), ioe.toString() });
        }
    }

    private boolean isCacheReadable() {
        return (cacheOk && (cacheResource != null) && cacheResource.exists());
    }

    private boolean isCacheWritable() {
        return (cacheOk && (cacheResource != null));
    }

    //

    private boolean isDirty; // Is the cache resource up to date.

    private void setClean() {
        isDirty = false;
    }

    private void setDirty() {
        isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    //

    /**
     * Information for a feature file which could not be read.
     *
     * TODO: Rename this to 'FileSignature'. The information is
     * last modified and length values of a file. The information
     * is used to tell if a file has changed.
     */
    private static class BadFeature {
        public final long lastModified;
        public final long length;

        public BadFeature(File f) {
            this(getLastModified(f), f.length());
        }

        protected BadFeature(long lastModified, long length) {
            this.lastModified = lastModified;
            this.length = length;
        }

        /**
         * Test for feature manifest changes by looking at the feature
         * manifest last modified time and length.
         *
         * @param f A feature manifest file.
         *
         * @return Tell if the feature information is the same as for the current
         *         feature manifest file.
         */
        public boolean sameAs(File f) {
            return ((getLastModified(f) == lastModified) && (f.length() == length));
        }
    }

    private static final long LAST_MODIFIED_PRECISION = 1000;

    private static long getLastModified(File f) {
        return reducePrecision(f.lastModified(), LAST_MODIFIED_PRECISION);
    }

    /**
     * Truncate a long value based on a precision value. The
     * precision value is expected to be a power of 10.
     *
     * For example, "10,123" reduced using precision "1,000"
     * yields "10,000".
     *
     * @param value     The value which is to be truncated.
     * @param precision The amount to remove from the value.
     *
     * @return The value with reduced precision.
     */
    private static long reducePrecision(long value, long precision) {
        return (value / precision) * precision;
    }

    // Tables of feature definitions present in the server installation.

    private final Map<String, SubsystemFeatureDefinitionImpl> installedFeatures = new HashMap<>();
    private final Map<String, LibertyFeatureServiceFactory> featureServiceFactories = new ConcurrentHashMap<>();

    private Map<File, BadFeature> knownBadFeatureFiles;
    private Map<File, SubsystemFeatureDefinitionImpl> knownGoodFeatureFiles;

    private List<SubsystemFeatureDefinitionImpl> autoFeatures;
    private Map<String, SubsystemFeatureDefinitionImpl> compatibilityFeatures;

    private void putBadFeature(File file) {
        knownBadFeatureFiles.put(file, new BadFeature(file));
    }

    /**
     * Local direct lookup of a feature using the feature's
     * symbolic name.
     *
     * @param symbolicName A feature symbolic name.
     *
     * @return The installed feature having the symbolic name.
     */
    private SubsystemFeatureDefinitionImpl getFeatureSymbolic(String symbolicName) {
        return installedFeatures.get(symbolicName);
    }

    public Map<String, SubsystemFeatureDefinitionImpl> getAllFeatures() {
        return installedFeatures;
    }

    /**
     * Store an installed feature.
     *
     * No normalization is done on the name.
     *
     * @param symbolicName The symbolic name of the feature.
     * @param featureDef   The feature definition.
     *
     * @return The previously stored feature definition.
     */
    private SubsystemFeatureDefinitionImpl putInstalledFeature(String symbolicName, SubsystemFeatureDefinitionImpl featureDef) {
        return installedFeatures.put(symbolicName, featureDef);
    }

    /**
     * Remove an installed feature.
     *
     * Caution! This should be {@link #removeInstalledFeature(String)}, but that
     * method name was already used for removing a resolved feature.
     *
     * @param symbolicName The symbolic name of the feature which is to be removed.
     */
    private void removeFeature(String symbolicName) {
        installedFeatures.remove(symbolicName);
    }

    // TODO: The API should be typed
    // as 'List<? extends ProvisioningFeatureDefinition>',
    // and this API should be typed 'List<SubsystemFeatureDefinitionImpl>',
    // however, changing the API is hard.

    @Override
    public List<ProvisioningFeatureDefinition> getFeatures() {
        return new ArrayList<>(installedFeatures.values());
    }

    /**
     * Lookup a installed feature by feature name or by symbolic name.
     *
     * See {@link #getSymbolicName(String)}.
     *
     * @param featureName A feature name or a feature symbolic name.
     *
     * @return The installed feature having the specified name or
     *         symbolic name.
     */
    @Override
    public SubsystemFeatureDefinitionImpl getFeature(String featureName) {
        SubsystemFeatureDefinitionImpl featureDef = installedFeatures.get(featureName);
        if (featureDef == null) {
            String symbolicName = getSymbolicName(featureName);
            featureDef = installedFeatures.get(symbolicName);
        }
        return featureDef;
    }

    public boolean disableAllFeaturesOnConflict(String featureName) {
        SubsystemFeatureDefinitionImpl feature = getFeature(featureName);
        return ((feature != null) && feature.getImmutableAttributes().disableOnConflict);
    }

    // Subset categories of installed features.

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
        return Collections.unmodifiableCollection(autoFeatures);
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
        return compatibilityFeatures.get(toLower(platformName, DO_STORE));
    }

    private void putCompatibilityFeature(String platformName, SubsystemFeatureDefinitionImpl featureDef) {
        compatibilityFeatures.put(toLower(platformName, DO_STORE), featureDef);
    }

    private void removeCompatibilityFeature(String platformName) {
        compatibilityFeatures.remove(toLower(platformName, !DO_STORE));
    }

    // Tables of configured features and resolved configured features.

    private volatile Set<String> configuredPlatforms = Collections.emptySet();
    private volatile String platformEnvVar;

    private volatile Set<String> configuredFeatures = Collections.emptySet();
    private volatile Set<String> resolvedFeatures = Collections.emptySet();
    private volatile boolean configurationError = true;

    public Set<String> getConfiguredPlatforms() {
        return configuredPlatforms;
    }

    public void setPlatformEnvVar(String platformEnvVar) {
        this.platformEnvVar = consumeEmpty(platformEnvVar);
    }

    public String getPlatformEnvVar() {
        return platformEnvVar;
    }

    public Set<String> getConfiguredFeatures() {
        return configuredFeatures;
    }

    /**
     * Deprecated - Replaced by getResolveFeatures
     */
    @Deprecated
    public Set<String> getInstalledFeatures() {
        return getResolvedFeatures();
    }

    public Set<String> getResolvedFeatures() {
        return resolvedFeatures;
    }

    public boolean emptyFeatures() {
        return resolvedFeatures.isEmpty();
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

    public void copyResolvedFeaturesTo(Set<String> features) {
        features.addAll(resolvedFeatures);
    }

    public boolean featureSetEquals(Set<String> newFeatureSet) {
        if (newFeatureSet == null) {
            return false;
        }
        return !isDirty && newFeatureSet.equals(resolvedFeatures);
    }

    /**
     * Deprecated - Replaced by setResolveFeatures
     */
    @Deprecated
    public void setInstalledFeatures(Set<String> newResolvedFeatures,
                                     Set<String> newConfiguredFeatures,
                                     boolean configurationError) {

        setResolvedFeatures(newResolvedFeatures, newConfiguredFeatures, configurationError,
                            Collections.emptySet(), null);
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

        if (isBeta) {
            if (!configuredPlatforms.equals(newConfiguredPlatforms)) {
                setDirty();
                configuredPlatforms = asUnmodifiable(newConfiguredPlatforms);
            }

            platformEnv = consumeEmpty(platformEnv);
            if (!equals(platformEnvVar, platformEnv)) {
                setDirty();
                platformEnvVar = platformEnv;
            }
        }

        if (!configuredFeatures.equals(newConfiguredFeatures)) {
            setDirty();
            configuredFeatures = asUnmodifiable(newConfiguredFeatures);
        }

        if (!resolvedFeatures.equals(newResolvedFeatures)) {
            setDirty();
            resolvedFeatures = asUnmodifiable(newResolvedFeatures);
        }

        // TODO: No 'setDirty' if this changes?
        this.configurationError = configurationError;
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
        configurationError = true;
        resolvedFeatures.remove(feature);
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

        if (isBeta) {
            if (!getConfiguredPlatforms().equals(newConfiguredPlatforms)) {
                return false;
            }
            if (!equals(getPlatformEnvVar(), newPlatformEnvironmentVar)) {
                return false;
            }
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

    // Support for canonical feature names.

    // Canonical feature names are all lower case,
    // and with spaces trimmed off the parts.

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
    public static String lowerFeature(String feature) {
        if ((feature == null) || feature.isEmpty()) {
            return feature;
        }

        int colonIndex = feature.indexOf(":");
        if ((colonIndex > -1) && (feature.length() > colonIndex)) {
            // Preserve the case of the prefix; only convert the suffix.
            String prefix = feature.substring(0, colonIndex).trim();
            String suffix = feature.substring(colonIndex + 1).trim();
            suffix = suffix.toLowerCase(Locale.ENGLISH);
            feature = prefix + ':' + suffix;
        } else {
            // No prefix; convert everything.
            feature = feature.trim();
            feature = feature.toLowerCase(Locale.ENGLISH);
        }

        return feature;
    }

    private final Map<String, String> lcFeatures = new HashMap<>();

    private static final boolean DO_STORE = true;

    @Trivial
    public String toLower(String feature, boolean doStore) {
        if ((feature == null) || feature.isEmpty()) {
            return feature;
        }

        String lcFeature = lcFeatures.get(feature);
        if (lcFeature == null) {
            lcFeature = lowerFeature(feature);
            if (doStore) {
                lcFeatures.put(feature, lcFeature);
            }
        }
        return lcFeature;
    }

    // Additional installed features tables ...

    private final Map<String, String> publicFeatureNameToSymbolicName = new HashMap<>();
    private final Map<String, String> alternateFeatureNameToPublicName = new HashMap<>();

    private void putSymbolicName(String featureName, String symbolicName) {
        publicFeatureNameToSymbolicName.put(toLower(featureName, DO_STORE), symbolicName);
    }

    private void removeSymbolicName(String featureName) {
        publicFeatureNameToSymbolicName.remove(toLower(featureName, !DO_STORE));
    }

    private String getSymbolicName(String featureName) {
        return publicFeatureNameToSymbolicName.get(toLower(featureName, !DO_STORE));
    }

    private void putAlternateName(String altName, String featureName) {
        alternateFeatureNameToPublicName.put(toLower(altName, DO_STORE), featureName);
    }

    /**
     * Use to check if a feature name is a commonly used alternate to an existing feature name
     *
     * @param featureName
     * @return The existing feature name or null if no match
     */
    public String matchesAlternate(String featureName) {
        return alternateFeatureNameToPublicName.get(toLower(featureName, DO_STORE));
    }

    // Cache lifecycle

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
        // TODO: This presumes that there will always
        //       be at one valid installed feature.
        boolean firstInit = installedFeatures.isEmpty();

        // Clear the installed features associated data.
        // This data is updated when reading the feature
        // manifests.
        autoFeatures = new ArrayList<>();
        knownGoodFeatureFiles = new HashMap<>();
        knownBadFeatureFiles = new HashMap<>();
        compatibilityFeatures = new HashMap<>();

        setClean();

        // Read the cache, then read the feature manifest.
        // The installed features, the installed features
        // associated data, and the resolved features are
        // populated.

        readCache(firstInit);
        readFeatureManifests();

        // As a special case, if there was a change to the
        // installed features, if this is the first initialization,
        // then clear the resolved features data.

        if (firstInit && isDirty()) {
            resolvedFeatures = Collections.emptySet();

            configuredFeatures = Collections.emptySet();
            configurationError = false;

            configuredPlatforms = Collections.emptySet();
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
     * Note that for update operations (rather than initial provisioning),
     * installedFeatures will be pre-populated..
     */
    private void readCache(boolean firstInit) {
        if (!isCacheReadable()) {
            return;
        }

        List<SubsystemFeatureDefinitionImpl> cachedInstalledFeatures = new ArrayList<>();
        Map<File, BadFeature> knownBad = new HashMap<>();

        Set<String> cachedConfiguredPlatforms = new HashSet<>();
        String cachedEnvVar = null;
        Set<String> cachedConfiguredFeatures = new HashSet<>();

        Set<String> cachedResolvedFeatures = new HashSet<>();
        boolean cachedConfigError = false;

        try (InputStream input = cacheResource.get();
                        DataInputStream in = new DataInputStream(new BufferedInputStream(input))) {

            if (in.readInt() != getCacheVersion()) {
                return; // not a version we understand; ignore the cache
            }

            int numFeatures = in.readInt();
            for (int i = 0; i < numFeatures; i++) {
                ImmutableAttributes attr = loadFeatureAttributes(in);
                ProvisioningDetails details = loadProvisioningDetails(in, attr);
                String symbolicName = attr.symbolicName;

                SubsystemFeatureDefinitionImpl installedFeature = getFeatureSymbolic(symbolicName);

                if (!attr.featureFile.exists()) {
                    // The feature file of a cached feature no longer exists.
                    // Mark that the repository as dirty, and, if stored,
                    // remove the installed feature.

                    setDirty();

                    if (installedFeature != null) {
                        // The IBM short name to symbolic name mapping for the feature
                        // is not removed.  This is harmless, since the short name
                        // associations are not allowed to change between features.
                        // A lookup of the short name will obtain a symbolic name,
                        // but the lookup of the symbolic name will fail.
                        removeFeature(symbolicName);
                    }

                } else {
                    // The feature file of the cached feature still exists.
                    // If the feature is stored, replace the installed feature
                    // provisioning details with the cached details.
                    //
                    // Otherwise, create an entirely new feature definition
                    // using the cached data.  If this is not the first
                    // initialization, mark the repository as dirty.

                    if (installedFeature != null) {
                        installedFeature.setProvisioningDetails(details);
                    } else {
                        if (!firstInit) {
                            setDirty();
                        }
                        installedFeature = new SubsystemFeatureDefinitionImpl(attr, details);
                    }
                    cachedInstalledFeatures.add(installedFeature);
                }
            }

            int numResolved = in.readInt();
            for (int i = 0; i < numResolved; i++) {
                cachedResolvedFeatures.add(in.readUTF());
            }

            int numConfigured = in.readInt();
            for (int i = 0; i < numConfigured; i++) {
                cachedConfiguredFeatures.add(in.readUTF());
            }

            cachedConfigError = in.readBoolean();

            int numBad = in.readInt();
            for (int i = 0; i < numBad; i++) {
                File f = new File(in.readUTF());
                if (f.isFile()) {
                    knownBad.put(f, new BadFeature(f));
                }
            }

            if (isBeta) {
                int numPlatforms = in.readInt();
                for (int i = 0; i < numPlatforms; i++) {
                    cachedConfiguredPlatforms.add(in.readUTF());
                }
                boolean hasPlatformEnv = in.readBoolean();
                if (hasPlatformEnv) {
                    cachedEnvVar = in.readUTF();
                }
            }

        } catch (IOException e) {
            cacheWarning(e);
            return;
        }

        knownBadFeatureFiles.putAll(knownBad);

        configuredPlatforms = Collections.unmodifiableSet(cachedConfiguredPlatforms);
        platformEnvVar = cachedEnvVar;
        configuredFeatures = Collections.unmodifiableSet(cachedConfiguredFeatures);

        resolvedFeatures = Collections.unmodifiableSet(cachedResolvedFeatures);
        configurationError = cachedConfigError;

        for (SubsystemFeatureDefinitionImpl cachedInstalledFeature : cachedInstalledFeatures) {
            updateMaps(cachedInstalledFeature);
        }
    }

    public void storeCache() {
        if (!isDirty || !isCacheWritable()) {
            return;
        }

        List<Entry<ImmutableAttributes, ProvisioningDetails>> features = new ArrayList<>();
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

        try (OutputStream cacheOutput = cacheResource.putStream();
                        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(cacheOutput))) {

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

            if (isBeta) {
                out.writeInt(configuredPlatforms.size());
                for (String plat : configuredPlatforms) {
                    out.writeUTF(plat);
                }

                if (platformEnvVar == null) {
                    out.writeBoolean(false);
                } else {
                    out.writeBoolean(true);
                    out.writeUTF(platformEnvVar);
                }
            }

            setClean();

        } catch (IOException e) {
            cacheWarning(e);
        }
    }

    private static final String EMPTY = "";

    static void writeFeatureAttributes(ImmutableAttributes iAttr, ProvisioningDetails details, DataOutputStream out) throws IOException {
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
        for (BundleRepositoryHolder holder : BundleRepositoryRegistry.holders()) {
            final String featureType = holder.getFeatureType();

            File libFeatureDir = new File(holder.getInstallDir(), ProvisionerConstants.LIB_FEATURE_PATH);
            if (!libFeatureDir.isDirectory()) {
                continue;
            }

            libFeatureDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    processFeatureManifest(featureType, file);

                    // Always answer false.  The {@link FileFilter#accept(File)} API is used
                    // to shift processing to the iteration step.  Effects of processing each
                    // feature manifest are side effects of accepting the file.
                    return false;
                }
            });
        }
    }

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
    private void processFeatureManifest(String featureType, File file) {
        if (file == null) {
            return;
        } else if (!file.isFile()) {
            return;
        } else if (!isManifest(file)) {
            return;
        }

        // Don't process a feature file which failed to read previously,
        // and which has no changes since the prior read.
        BadFeature bad = knownBadFeatureFiles.get(file);
        if ((bad != null) && bad.sameAs(file)) {
            return;
        }

        // Test: if we've seen this file before, is it the same as what we saw last time?
        SubsystemFeatureDefinitionImpl def = knownGoodFeatureFiles.get(file);
        if (def != null) {
            if (isUnchanged(file, def)) {
                return; // Case 1: Unchanged
            } else {
                removeFromMaps(def); // Case 2: Changed
            }
        } else {
            // Case 3: new
        }

        setDirty();

        ProvisioningDetails details;
        ImmutableAttributes attr;
        try {
            details = new ProvisioningDetails(file, null);
            attr = FeatureDefinitionUtils.loadAttributes(featureType, file, details);
        } catch (IOException e) {
            debug("Exception reading feature manifest [ " + file.getAbsolutePath() + " ]", e.toString()); // TODO: NLS
            putBadFeature(file);
            return;
        }

        // Oops: The feature format changed.
        if (!attr.isSupportedFeatureVersion()) {
            debug("Unsupported feature version [ " + file.getAbsolutePath() + " ]"); // TODO: NLS
            putBadFeature(file);
            return;
        }

        def = new SubsystemFeatureDefinitionImpl(attr, details);
        updateMaps(def);
    }

    private static final String MANIFEST_EXT = ".mf";
    private static final int MANIFEST_EXT_LEN = MANIFEST_EXT.length();

    // Look only at the file extension, case insensitively.
    private static boolean isManifest(File file) {
        String name = file.getName();
        int len = name.length();
        if (len < MANIFEST_EXT_LEN) {
            return false;
        } else {
            return name.regionMatches(true, len - MANIFEST_EXT_LEN, ".mf", 0, 3);
        }
    }

    /**
     * Answer true or false telling if the feature manifest file has no
     * changed.
     *
     * @param f          The feature manifest file.
     * @param featureDef The feature definition.
     *
     * @return True or false telling if the feature manifest file has no changes.
     */
    private boolean isUnchanged(File f, SubsystemFeatureDefinitionImpl featureDef) {
        ImmutableAttributes attr = featureDef.getImmutableAttributes();

        // No changes since the read of the feature definition.  No updates are needed.

        return ((reducePrecision(attr.lastModified, LAST_MODIFIED_PRECISION) == getLastModified(f)) &&
                (attr.length == f.length()));
    }

    private void removeFromMaps(SubsystemFeatureDefinitionImpl featureDef) {
        ImmutableAttributes attr = featureDef.getImmutableAttributes();

        removeSymbolicName(attr.featureName);

        removeFeature(attr.symbolicName);
        autoFeatures.remove(featureDef);

        String platform = getCompatibilityPlatform(featureDef, attr);
        if (platform != null) {
            removeCompatibilityFeature(platform);
        }
    }

    private void updateMaps(SubsystemFeatureDefinitionImpl def) {
        ImmutableAttributes attr = def.getImmutableAttributes();

        SubsystemFeatureDefinitionImpl oldDef = putInstalledFeature(attr.symbolicName, def);

        // UH-OH!! we have a symbolic name collision, which is just not supposed to happen.
        // a) keep the first one
        // b) Create an FFDC record indicating this happened
        // c) TODO: NLS message

        if ((oldDef != null) && !oldDef.equals(def)) {
            putInstalledFeature(attr.symbolicName, oldDef);

            String msg = "Duplicate symbolic name: " + attr.symbolicName +
                         ", " + def.getFeatureDefinitionFile().getAbsolutePath() +
                         " will be ignored. The file " + oldDef.getFeatureDefinitionFile().getAbsolutePath() +
                         " will be used instead.";
            FeatureManifestException fme = new FeatureManifestException(msg, (String) null);
            FFDCFilter.processException(fme, this.getClass().getName(), "updateMaps", this, new Object[] { oldDef, def });

            putBadFeature(attr.featureFile); // Ignore this feature
            return;
        }

        // Remember that we've seen this file: file to definition
        knownGoodFeatureFiles.put(attr.featureFile, def);

        // If there is a public feature name,
        // populate the map with down-case featureName to real symbolic name
        // populate the map with down-case symbolicName to real symbolic name
        // Note: we only ignore case when looking up public feature names!
        if (!attr.featureName.equals(attr.symbolicName)) {
            putSymbolicName(attr.featureName, attr.symbolicName);
        }

        if (def.getVisibility() == Visibility.PUBLIC) {
            putSymbolicName(attr.symbolicName, attr.symbolicName);
            for (String altName : attr.alternateNames) {
                putAlternateName(altName, attr.featureName);
            }

        } else if (def.getVisibility() == Visibility.PRIVATE) {
            if ((attr.platforms != null) && !attr.platforms.isEmpty()) {
                putCompatibilityFeature(def.getPlatformName(), def);
            }
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

        if (attr.isAutoFeature) {
            autoFeatures.add(def);
        }
    }

    //

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
            return; // Do nothing.  Probably in a unit test.
        }

        // Collect the symbolic names of the resolved features.
        Set<String> resolvedSymbolicNames = new HashSet<String>();
        for (String featureName : resolvedFeatures) {
            String symbolicName = getSymbolicName(featureName);
            if (symbolicName != null) {
                resolvedSymbolicNames.add(symbolicName);
            }
        }

        // Collect the symbolic names of previously registered resolved features ...
        Set<String> removedFactories = new HashSet<String>(featureServiceFactories.keySet());
        // ... but take out any which are still resolved.
        // That leaves the symbolic names of the resolved features which were removed.
        removedFactories.removeAll(resolvedSymbolicNames);

        // Unregister the features which were removed.
        for (String symbolicName : removedFactories) {
            LibertyFeatureServiceFactory factory = featureServiceFactories.remove(symbolicName);
            if (factory != null) {
                factory.unregisterService();
            }
        }

        // Register (or re-register) all resolved features.
        for (String symbolicName : resolvedSymbolicNames) {
            SubsystemFeatureDefinitionImpl featureDef = getFeatureSymbolic(symbolicName);
            if (featureDef != null) {
                LibertyFeatureServiceFactory factory = featureServiceFactories.get(symbolicName);
                if (factory == null) {
                    factory = new LibertyFeatureServiceFactory();
                    featureServiceFactories.put(symbolicName, factory);
                }
                // factory = featureServiceFactories.computeIfAbsent(symbolicName, (String sym) -> new LibertyFeatureServiceFactory());

                factory.update(featureDef, bundleContext);
            }
        }
    }

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
        private volatile SubsystemFeatureDefinitionImpl featureDef;

        private final Hashtable<String, Object> serviceProps = new Hashtable<>(4);
        private ServiceRegistration<LibertyFeature> registration;

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
            if (this.featureDef == featureDef) {
                return; // nothing to do
            }

            unregisterService();
            clearServiceProps();

            this.featureDef = featureDef;

            setServiceProps();
            registerService(bundleContext);
        }

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
            ProvisioningDetails details = featureDef.getProvisioningDetails();
            if (details == null) {
                try {
                    details = new ProvisioningDetails(featureDef.getImmutableAttributes().featureFile, null);
                } catch (IOException e) {
                    // Unlikely, as this feature manifest has been read before.
                    debug("An exception occurred while reading the feature manifest", e.toString());
                }
            }

            // TODO: This CANNOT be moved inside the 'details == null' loop without
            //       additional review.  'setProvisioningDetails' increases a use count on
            //       the feature definition.
            featureDef.setProvisioningDetails(details);

            return featureDef;
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
        public void ungetService(Bundle bundle,
                                 ServiceRegistration<LibertyFeature> useRegistration,
                                 LibertyFeature service) {
            // 'service' should be the same as 'featureDef'.
            featureDef.setProvisioningDetails(null);
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
            // Note the use of 'this' as a liberty feature / service.
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
                registration = null;
            }
        }

        private static final String FEATURE_SERVICE_NAME = "ibm.featureName";
        private static final String FEATURE_SERVICE_SYMBOLIC_NAME = "osgi.symbolicName";
        private static final String FEATURE_SERVICE_VERSION = "osgi.version";
        private static final String FEATURE_SERVICE_CATEGORY = "osgi.category";

        private static final String FEATURE_SUBSYSTEM_CATEGORY = "Subsystem-Category";

        /**
         * Remove all current service properties.
         */
        private void clearServiceProps() {
            serviceProps.clear();
        }

        /**
         * Store feature values into the service properties.
         *
         * Store the feature name, the feature symbolic name, the feature
         * version, and the feature subsystem category value (from the feature
         * header).
         */
        private void setServiceProps() {
            serviceProps.put(FEATURE_SERVICE_NAME, featureDef.getFeatureName());
            serviceProps.put(FEATURE_SERVICE_SYMBOLIC_NAME, featureDef.getSymbolicName());
            serviceProps.put(FEATURE_SERVICE_VERSION, featureDef.getVersion());

            String category = featureDef.getHeader(FEATURE_SUBSYSTEM_CATEGORY);
            if (category != null) {
                serviceProps.put(FEATURE_SERVICE_CATEGORY, category.split("\\s*,\\s*"));
            }
        }
    }
}