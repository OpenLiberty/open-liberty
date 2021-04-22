/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
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
    private static final int FEATURE_CACHE_VERSION = 3;
    private static final String EMPTY = "";

    /**
     * Whether or not any elements in the cache are stale:
     * This is initially set by the constructor, after validating whether or not
     * feature definition files have changed since last read.
     */
    private boolean isDirty;

    /** Cache file is reachable/working */
    private boolean cacheOk = true;

    private final WsResource cacheRes;

    private final BundleContext bundleContext;

    /** List of currently installed features */
    private volatile Set<String> installedFeatures = Collections.emptySet();

    /** List of currently configured features */
    private volatile Set<String> configuredFeatures = Collections.emptySet();

    /** Flag to indicate that the list of configured features contains resolution errors */
    private volatile boolean configurationError = true;

    /** Map of symbolic name to subsystem feature definition */
    private final Map<String, SubsystemFeatureDefinitionImpl> cachedFeatures = new HashMap<String, SubsystemFeatureDefinitionImpl>();

    /** Map of public features to short names */
    private final Map<String, String> publicFeatureNameToSymbolicName = new HashMap<String, String>();

    /** Map of public feature alternate name to correct feature name */
    private final Map<String, String> alternateFeatureNameToPublicName = new HashMap<String, String>();

    private final ConcurrentMap<String, LibertyFeatureServiceFactory> featureServiceFactories = new ConcurrentHashMap<String, LibertyFeatureServiceFactory>();

    /** PROVISIONING:Map of symbolic name to autoFeature */
    private ArrayList<SubsystemFeatureDefinitionImpl> autoFeatures;
    /** PROVISIONING:Map of file to subystem definition */
    Map<File, SubsystemFeatureDefinitionImpl> knownFeatures;
    /** PROVISIONING:Map of file to subystem definition */
    Map<File, BadFeature> knownBadFeatures;

    public FeatureRepository() {
        cacheRes = null;
        isDirty = true;
        bundleContext = null;
    }

    public FeatureRepository(WsResource res, BundleContext bundleContext) {
        cacheRes = res;
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
     * This is called at the beginning of a provisioning operation. It re-initializes
     * or refreshes the cache against what is known in the filesystem.
     * <p>
     * Note that for update operations (rather than initial provisioning), cachedFeatures will
     * be pre-populated..
     */
    public void init() {
        isDirty = false;

        // If we haven't looked at anything from the cache or the filesystem, this is bootstrap time
        boolean firstInit = cachedFeatures.isEmpty();

        autoFeatures = new ArrayList<SubsystemFeatureDefinitionImpl>();
        knownFeatures = new HashMap<File, SubsystemFeatureDefinitionImpl>();
        knownBadFeatures = new HashMap<File, BadFeature>();

        // Read feature cache, this will only toss files that no longer exist
        // stale will be marked true if feature definitions were discarded.
        readCache(firstInit);
        // Now we need to go find / update the cache with anything new/interesting/different.
        // This will happen at most twice...
        readFeatureManifests();

        // If something was out of sync with the filesystem and this is the first pass,
        // reset the installed features list so we re-figure out what should be installed.
        if (isDirty && firstInit) {
            // If stale, reset to empty list as we'll be rebuilding....
            installedFeatures = Collections.emptySet();
            configuredFeatures = Collections.emptySet();
        }
    }

    /**
     * Release all resources associated with the provisioning operation
     */
    public void dispose() {
        storeCache();

        // PURGE provisioning data! BYE-BYE!!
        autoFeatures = null;
        knownFeatures = null;
        knownBadFeatures = null;

        for (SubsystemFeatureDefinitionImpl def : cachedFeatures.values()) {
            def.setProvisioningDetails(null);
        }
    }

    /**
     * This is called at the beginning of a provisioning operation. It re-initializes
     * or refreshes the cache against what is known in the filesystem.
     * <p>
     * Note that for update operations (rather than initial provisioning), cachedFeatures will
     * be pre-populated..
     *
     * @see #storeCache()
     */
    private void readCache(boolean firstInit) {
        if (cacheOk && cacheRes != null && cacheRes.exists()) {

            List<SubsystemFeatureDefinitionImpl> cachedEntries = new ArrayList<>();
            Set<String> installed = new HashSet<>();
            Set<String> configured = new HashSet<>();
            Map<File, BadFeature> knownBad = new HashMap<>();
            boolean configError = false;
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(cacheRes.get()))) {
                if (in.readInt() != FEATURE_CACHE_VERSION) {
                    return; // not a version we understand; ignore the cache
                }
                int numFeatures = in.readInt();
                for (int i = 0; i < numFeatures; i++) {
                    ImmutableAttributes newAttr = loadFeatureAttributes(in);
                    ProvisioningDetails details = loadProvisioningDetails(in, newAttr);
                    String symbolicName = newAttr.symbolicName;

                    SubsystemFeatureDefinitionImpl cachedEntry = cachedFeatures.get(symbolicName);

                    // Get the immutable attributes from the cached feature element..
                    ImmutableAttributes cachedAttr = cachedEntry == null ? null : cachedEntry.getImmutableAttributes();

                    if (!newAttr.featureFile.exists()) {
                        // feature file no longer exists; throw away the value
                        newAttr = null;
                    } else if (cachedAttr != null) {
                        // we had this value already; use it
                        newAttr = cachedAttr;
                    }
                    // loadAttributes will return new attributes (didn't exist before),
                    // the cachedAttributes (unchanged), or null (file no longer present OR invalid cache line )

                    if (newAttr != null) {
                        // New shiny attributes...
                        if (cachedAttr == newAttr) {
                            // woo-hoo!! cache hit!
                            // the provisioning details need to be reconstituted
                            cachedEntry.setProvisioningDetails(details);
                        } else {
                            // Only set to is dirty if not firstInit.
                            // On firstInit we are creating the definitions from cache for the first time here
                            if (!!!firstInit)
                                isDirty = true;
                            cachedEntry = new SubsystemFeatureDefinitionImpl(newAttr, details);
                        }
                    } else if (cachedAttr != null) {
                        // so sad! the file went away or something is askew with the cache
                        isDirty = true;
                        cachedFeatures.remove(symbolicName);
                        cachedEntry = null;
                    } else {
                        // I know this happens: null cachedAttr, null newAttr..
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "cacheAttr and newAttr BOTH null while reading cache", symbolicName);
                        }
                        isDirty = true;
                        cachedEntry = null;
                    }

                    cachedEntries.add(cachedEntry);
                }

                int numInstalled = in.readInt();
                for (int i = 0; i < numInstalled; i++) {
                    installed.add(in.readUTF());
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
                    if (f.isFile())
                        knownBad.put(f, new BadFeature(lastModified, length));
                }
            } catch (IOException e) {
                cacheWarning(e);
                return;
            }

            installedFeatures = Collections.unmodifiableSet(installed);
            configuredFeatures = Collections.unmodifiableSet(configured);
            configurationError = configError;
            knownBadFeatures.putAll(knownBad);

            // Update the associated maps we need for provisioning
            for (SubsystemFeatureDefinitionImpl cachedEntry : cachedEntries) {
                updateMaps(cachedEntry);
            }
        }
    }

    /**
     * Write the feature cache file
     *
     * @see #readCache()
     */
    public void storeCache() {
        if (cacheOk && cacheRes != null && isDirty) {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(cacheRes.putStream()))) {
                Collection<Entry<ImmutableAttributes, ProvisioningDetails>> features = new ArrayList<>();
                for (SubsystemFeatureDefinitionImpl entry : cachedFeatures.values()) {
                    ImmutableAttributes imAttrs = entry.getImmutableAttributes();
                    ProvisioningDetails provDetails = entry.getProvisioningDetails();
                    if (imAttrs != null && provDetails != null) {
                        features.add(new SimpleEntry<>(imAttrs, provDetails));
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Unable to write out " + entry.getFeatureName() + " to cache because the provisioning "
                                         + "detail: " + provDetails + " or imAttrs: " + imAttrs + " is null");
                        }
                    }
                }
                out.writeInt(FEATURE_CACHE_VERSION);
                out.writeInt(features.size());
                for (Entry<ImmutableAttributes, ProvisioningDetails> entry : features) {
                    writeFeatureAttributes(entry.getKey(),
                                           entry.getValue(),
                                           out);
                }

                Collection<String> curInstalled = installedFeatures;
                out.writeInt(curInstalled.size());
                for (String installed : curInstalled) {
                    out.writeUTF(installed);
                }

                Collection<String> curConfigured = configuredFeatures;
                out.writeInt(curConfigured.size());
                for (String configured : curConfigured) {
                    out.writeUTF(configured);
                }

                out.writeBoolean(configurationError);

                Map<File, BadFeature> curKnownBad = knownBadFeatures;
                out.writeInt(curKnownBad.size());
                for (Map.Entry<File, BadFeature> entry : knownBadFeatures.entrySet()) {
                    out.writeUTF(entry.getKey().getAbsolutePath());
                    out.writeLong(entry.getValue().lastModified);
                    out.writeLong(entry.getValue().length);
                }

                isDirty = false;
            } catch (IOException e) {
                cacheWarning(e);
            }

        }
    }

    static void writeFeatureAttributes(ImmutableAttributes iAttr, ProvisioningDetails details, DataOutputStream out) throws IOException {
        if (iAttr == null || details == null) // programmer error
            throw new NullPointerException("Both attributes and details are required for caching: attr=" + iAttr + ", details=" + details);

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
        return new ImmutableAttributes(repositoryType, symbolicName, shortName, altNames, featureVersion, visibility, appRestart,
                                       version, featureFile, lastModified, fileSize, isAutoFeature, hasApiServices, hasApiPackages,
                                       hasSpiPackages, isSingleton, disableOnConflict, processTypes, activationType);
    }

    /**
     * @param in
     * @return
     * @throws IOException
     */
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
     * Look over the collection of all the manifest files:
     * a) see if any have changed (invalidate/replace the cache elements)
     * b) add any we didn't know about before
     */
    private void readFeatureManifests() {
        // For each bundle repository, we need to look at the manifest files
        for (final BundleRepositoryHolder holder : BundleRepositoryRegistry.holders()) {

            // Checking isDirectory is just paranoia: Things don't get into the BundleRepositoryRegistry if they don't exist
            File libFeatureDir = new File(holder.getInstallDir(), ProvisionerConstants.LIB_FEATURE_PATH);
            if (libFeatureDir.isDirectory()) {

                // Let's look at all the manifest files in this dir
                libFeatureDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        // Note: we always return false. We do the work as we see the files,
                        // instead of iterating to build a list that we then have to iterate over again...

                        if (file == null)
                            return false; // NEXT!

                        // Pessimistic test first: Is this a file we know is bad?
                        BadFeature bad = knownBadFeatures.get(file);
                        if (isFeatureStillBad(file, bad))
                            return false; // NEXT!

                        // Test: if we've seen this file before, is it the same as what we saw last time?
                        SubsystemFeatureDefinitionImpl def = knownFeatures.get(file);
                        if (isCachedEntryValid(file, def))
                            return false; // NEXT!

                        if (!file.isFile())
                            return false; // NEXT!

                        String name = file.getName();
                        int pos = name.lastIndexOf('.');
                        if (pos < 0)
                            return false; // NEXT!
                        // Look only at the file extension, case insensitively
                        if (name.regionMatches(true, pos, ".mf", 0, 3)) {
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
                                    // this feature should be ignored (perhaps backlevel version)
                                    // add it to list of files to skip
                                    knownBadFeatures.put(file, new BadFeature(file.lastModified(), file.length()));
                                    return false; // NEXT
                                }

                                // we're good to go: basic attributes read and a good feature version!
                                def = new SubsystemFeatureDefinitionImpl(attr, details);

                                // update cache(s) with new definition
                                updateMaps(def);

                            } catch (IOException e) {
                                // TODO: NLS MESSAGE
                                // We have no message for "An exception occurred reading the feature manifest"
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "An exception occurred while reading the feature manifest", e.toString());
                                }
                                knownBadFeatures.put(file, new BadFeature(file.lastModified(), file.length()));
                            }
                        }

                        return false; // NEXT!
                    }
                });
            }
        }
    }

    boolean isFeatureStillBad(File f, BadFeature bf) {
        return bf != null
               && f.lastModified() == bf.lastModified
               && f.length() == bf.length;
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
                if (cachedAttr.length == f.length())
                    return true;
            }

            // If we got here, something changed with the entry we had:
            // could be anything inside the file, so be thorough
            // -- knownFeature entry will be replaced by caller
            cachedFeatures.remove(cachedAttr.symbolicName);
            publicFeatureNameToSymbolicName.remove(lowerFeature(cachedAttr.featureName));
            if (cachedAttr.isAutoFeature)
                autoFeatures.remove(def);
        }

        return false;
    }

    private void updateMaps(SubsystemFeatureDefinitionImpl def) {
        if (def != null) {
            ImmutableAttributes cachedAttr = def.getImmutableAttributes();

            // Update the feature cache: symbolic name to definition
            SubsystemFeatureDefinitionImpl previousValue = cachedFeatures.put(cachedAttr.symbolicName, def);
            if (previousValue != null && !previousValue.equals(def)) {
                // UH-OH!! we have a symbolic name collision, which is just not supposed to happen.
                // a) keep the first one
                // b) Create an FFDC record indicating this happened
                // c) TODO: NLS message
                cachedFeatures.put(cachedAttr.symbolicName, previousValue);
                FeatureManifestException fme = new FeatureManifestException("Duplicate symbolic name: " + cachedAttr.symbolicName
                                                                            + ", " + def.getFeatureDefinitionFile().getAbsolutePath()
                                                                            + " will be ignored. The file " + previousValue.getFeatureDefinitionFile().getAbsolutePath()
                                                                            + " will be used instead.",
                                                                            (String) null); // TODO: nls message here..

                FFDCFilter.processException(fme, this.getClass().getName(), "updateMaps",
                                            this, new Object[] { previousValue, def });

                // Ignore this definition...
                File file = cachedAttr.featureFile;
                knownBadFeatures.put(file, new BadFeature(file.lastModified(), file.length()));
                return;
            }

            // Remember that we've seen this file: file to definition
            knownFeatures.put(cachedAttr.featureFile, def);

            // If there is a public feature name,
            // populate the map with down-case featureName to real symbolic name
            // populate the map with down-case symbolicName to real symbolic name
            // Note: we only ignore case when looking up public feature names!
            if (!cachedAttr.featureName.equals(cachedAttr.symbolicName))
                publicFeatureNameToSymbolicName.put(lowerFeature(cachedAttr.featureName), cachedAttr.symbolicName);
            if (def.getVisibility() == Visibility.PUBLIC)
                publicFeatureNameToSymbolicName.put(lowerFeature(cachedAttr.symbolicName), cachedAttr.symbolicName);

            // populate mapping from known, commonly used alternative names to allow hints when the wrong feature
            // name is specified in a server config.
            for (String s : cachedAttr.alternateNames) {
                alternateFeatureNameToPublicName.put(s, cachedAttr.featureName);
            }

            // If this is an auto-feature, add it to that collection
            // we're going with the bold assertion that
            if (cachedAttr.isAutoFeature)
                autoFeatures.add(def);
        }
    }

    private void cacheWarning(IOException ioe) {
        if (cacheOk) {
            cacheOk = false;
            Tr.warning(tc, "UPDATE_BUNDLE_CACHE_WARNING", new Object[] { cacheRes.toExternalURI(), ioe.toString() });
        }
    }

    /**
     * Change the active list of installed features
     *
     * @param newInstalledFeatures new set of installed features. Replaces the previous set.
     */
    public void setInstalledFeatures(Set<String> newInstalledFeatures, Set<String> newConfiguredFeatures, boolean configurationError) {
        Set<String> current = installedFeatures;
        if (!current.equals(newInstalledFeatures)) {
            isDirty = true;
        }
        if (newInstalledFeatures.isEmpty())
            installedFeatures = Collections.emptySet();
        else
            installedFeatures = Collections.unmodifiableSet(new HashSet<String>(newInstalledFeatures));

        current = configuredFeatures;
        if (!current.equals(newConfiguredFeatures)) {
            isDirty = true;
        }
        if (newConfiguredFeatures.isEmpty())
            configuredFeatures = Collections.emptySet();
        else
            configuredFeatures = Collections.unmodifiableSet(new HashSet<String>(newConfiguredFeatures));

        this.configurationError = configurationError;
    }

    public Set<String> getInstalledFeatures() {
        return installedFeatures;
    }

    public Set<String> getConfiguredFeatures() {
        return configuredFeatures;
    }

    public boolean hasConfigurationError() {
        return configurationError;
    }

    /**
     * Copies the active list of installed features into the given set.
     *
     * @param features the set to copy to.
     */
    public void copyInstalledFeaturesTo(Set<String> features) {
        features.addAll(installedFeatures);
    }

    /**
     * @return
     */
    public boolean emptyFeatures() {
        return installedFeatures.isEmpty();
    }

    public boolean featureSetEquals(Set<String> newFeatureSet) {
        if (newFeatureSet == null)
            return false;

        return !isDirty && newFeatureSet.equals(installedFeatures);
    }

    /**
     * @return
     */
    @Override
    public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
        if (autoFeatures == null)
            throw new IllegalStateException("Method called outside of provisioining operation");

        return asProvisioningFeatureDefinitionCollection(Collections.unmodifiableCollection(autoFeatures));
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

    /**
     * @param featureName
     * @return
     */
    @Override
    public ProvisioningFeatureDefinition getFeature(String featureName) {
        SubsystemFeatureDefinitionImpl result = cachedFeatures.get(featureName);
        if (result == null) {
            String name = publicFeatureNameToSymbolicName.get(lowerFeature(featureName));
            result = cachedFeatures.get(name);
        }
        return result;
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
     * Get feature name with product extension unchanged and the
     * feature name converted to lower case.
     *
     * note: feature type in featureLocations is case specific.
     * In order for the bundles to be found, the case of the
     * product extension in the feature name needs to be preserved.
     * If it is not, an incorrect bundle repository type will be put into
     * the feature definition when it is loaded from the feature cache.
     *
     * @param inFeature feature name
     * @return String product extension unchanged and the feature name converted to lower case.
     */
    @Trivial
    public static String lowerFeature(String inFeature) {
        if (inFeature == null || inFeature.isEmpty())
            return inFeature;

        // Preserve the prefix (no case shift)
        int colonIndex = inFeature.indexOf(":");

        if (colonIndex > -1 && inFeature.length() > colonIndex) {
            // Put together preserved extension name with the downcased feature name
            return inFeature.substring(0, colonIndex).trim() + ':' + inFeature.substring(colonIndex + 1).trim().toLowerCase(Locale.ENGLISH);
        } else {
            return inFeature.toLowerCase(Locale.ENGLISH).trim();
        }
    }

    static class BadFeature {
        final long lastModified;
        final long length;

        BadFeature(long lastModified, long length) {
            this.lastModified = lastModified;
            this.length = length;
        }
    }

    /**
     * @param bundleContext
     */
    public void updateServices() {
        if (bundleContext == null) {
            // do nothing; not really in a running system (unit tests etc.)
            return;
        }
        Set<String> installedSymbolicNames = new HashSet<String>();
        for (String featureName : installedFeatures) {
            String symbolicName = publicFeatureNameToSymbolicName.get(lowerFeature(featureName));
            if (symbolicName != null) {
                installedSymbolicNames.add(symbolicName);
            }
        }
        Set<String> removedFactories = new HashSet<String>(featureServiceFactories.keySet());
        removedFactories.removeAll(installedSymbolicNames);
        for (String currentFactorySymbolicName : removedFactories) {
            LibertyFeatureServiceFactory factory = featureServiceFactories.remove(currentFactorySymbolicName);
            if (factory != null) {
                factory.unregisterService();
            }
        }

        for (String currentFactorySymbolicName : installedSymbolicNames) {
            SubsystemFeatureDefinitionImpl featureDef = cachedFeatures.get(currentFactorySymbolicName);
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

    /**
     * This is registered as a ServiceFactory, so that each bundle requesting an instance of the
     * Liberty feature makes a separate request for the service. This allows us to maintain a usage count.
     * When the usage count is back to zero, we can indicate that the details aren't needed anymore
     * (to satisfy service requests).
     */
    private static class LibertyFeatureServiceFactory implements ServiceFactory<LibertyFeature> {
        private final Hashtable<String, Object> serviceProps = new Hashtable<String, Object>();
        private ServiceRegistration<LibertyFeature> registration;
        private volatile SubsystemFeatureDefinitionImpl _featureDef;

        @Override
        public LibertyFeature getService(Bundle bundle, ServiceRegistration<LibertyFeature> registration) {
            SubsystemFeatureDefinitionImpl current = _featureDef;
            ProvisioningDetails details = current.getProvisioningDetails();
            if (details == null) {
                try {
                    details = new ProvisioningDetails(current.getImmutableAttributes().featureFile, null);
                } catch (IOException e) {
                    // Unlikely, as this feature manifest has been read before.
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "An exception occurred while reading the feature manifest", e.toString());
                    }
                }
            }
            current.setProvisioningDetails(details);

            return current;
        }

        @Override
        public void ungetService(Bundle bundle, ServiceRegistration<LibertyFeature> registration, LibertyFeature service) {
            _featureDef.setProvisioningDetails(null);
        }

        void registerService(BundleContext bundleContext) {
            registration = bundleContext.registerService(LibertyFeature.class, this, serviceProps);
        }

        void unregisterService() {
            if (registration != null) {
                registration.unregister();
            }
        }

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

        private void getServiceProps() {
            serviceProps.clear();
            serviceProps.put(FEATURE_SERVICE_NAME, _featureDef.getFeatureName());
            serviceProps.put(FEATURE_SERVICE_SYMBOLIC_NAME, _featureDef.getSymbolicName());
            serviceProps.put(FEATURE_SERVICE_VERSION, _featureDef.getVersion());
            String category = _featureDef.getHeader("Subsystem-Category");
            if (category != null) {
                serviceProps.put(FEATURE_SERVICE_CATEGORY, category.split("\\s*,\\s*"));
            }
        }
    }

    /**
     * @return
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Remove an installed feature from the list. This is intended to be used to remove features that failed during
     * bundle resolution because of java version restrictions.
     *
     * It will also set configurationError to true, as this should only be called in an error scenario.
     *
     * @param feature The feature to remove
     */
    public void removeInstalledFeature(String feature) {
        this.configurationError = true;
        HashSet<String> newInstalledFeatures = new HashSet<>(installedFeatures);
        if (newInstalledFeatures.remove(feature)) {
            installedFeatures = newInstalledFeatures.isEmpty() ? Collections.<String> emptySet() : Collections.unmodifiableSet(newInstalledFeatures);
        }
    }
}