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
package com.ibm.ws.kernel.feature.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.util.VerifyEnv;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;

//import org.osgi.framework.Version;

/**
 * A feature resolver that determines the set of features that should be installed
 * to resolve an initial set of features. This resolver also handles auto features.
 * <p>
 * IMPLEMENTION note, this resolver does backtrack decisions made in the
 * presence of multiple candidates and conflicts. Each time a selection is made
 * when multiple candidates are available a snapshot (permutation) is made and
 * pushed onto a stack. If conflicts are found then a permutation is popped off
 * the stack and the next candidate is tried. The strategy always backs off the
 * last decision made by popping off the last copy of the permutation. This
 * algorithm is optimistic in the sense that it assumes earlier decisions are
 * more preferred than the later ones. This algorithm has the potential to
 * explode if there are a high level of features with multiple versions
 * and many conflicts.
 * <p>
 * Also note that the order dependencies are processed also effects the outcome
 * of the preferred features that are selected.
 * Randomness of this is mitigated by ensuring
 * the root resources and their dependencies are processed in a consistent order from
 * one run to the next.
 */
public class FeatureResolverImpl implements FeatureResolver {
    private static final Object tc;

    static {
        Object temp = null;
        try {
            temp = Tr.register(FeatureResolverImpl.class,
                               ProvisionerConstants.TR_GROUP, ProvisionerConstants.NLS_PROPS);
        } catch (Throwable t) {
            // ignore
        }
        tc = temp;
    }

    @Trivial
    protected static void trace(String message) {
        if (tc != null) {
            if (TraceComponent.isAnyTracingEnabled() && ((TraceComponent) tc).isDebugEnabled()) {
                Tr.debug((TraceComponent) tc, message);
            }
        }
    }

    @Trivial
    protected static void error(String message, Object... parms) {
        if (tc != null) {
            Tr.error((TraceComponent) tc, message, parms);
        }
    }

    @Trivial
    protected static void info(String message, Object... parms) {
        if (tc != null) {
            Tr.info((TraceComponent) tc, message, parms);
        }
    }

    protected static StringBuilder append(StringBuilder builder, String value) {
        if (builder != null) {
            builder.append(',');
        } else {
            builder = new StringBuilder(1 + value.length() + 1);
        }
        builder.append('"');
        builder.append(value);
        builder.append('"');
        return builder;
    }

    // Feature parse cache ...

    /**
     * Cache of parsed name and version values.
     *
     * Keys are versioned feature names, for example, "servlet-4.0".
     * Values are the pair of the parsed feature name plus the
     * text of the version. Continuing the example, "servlet" and "4.0".
     * A value which does not have a version will be returned with
     * null version text. That is, "servlet" is stored as { "servlet", null }.
     *
     * The version text is validated: A version value which is not value
     * is stored as null.
     */
    private static Map<String, String[]> parsedNAV = new HashMap<String, String[]>();

    /**
     * Answer the name of a versioned feature name.
     *
     * Use the parse cache to do this quickly.
     *
     * @see {@link #parseNameAndVersion(String)}.
     *
     * @param feature The versioned feature name which is to be parsed.
     *
     * @return The name of the feature name.
     */
    public static String parseName(String feature) {
        return parseNameAndVersion(feature)[0];
    }

    /**
     * Answer the version text of a versioned feature name.
     * Answer null if the feature name does not have a version,
     * or if the version text is not a valid version.
     *
     * Valid versions are expected to be dotted pairs of integers,
     * for example, "10.0".
     *
     * Use the parse cache to do this quickly.
     *
     * @see {@link #parseNameAndVersion(String)}.
     *
     * @param feature The versioned feature name which is to be parsed.
     *
     * @return The version text of the feature name.
     */
    public static String parseVersion(String feature) {
        return parseNameAndVersion(feature)[1];
    }

    /**
     * Answer the name and the version text of a versioned
     * feature name.
     *
     * Store null for the version text if the feature name does
     * not have a version, or if the version text is not a valid
     * version.
     *
     * Valid versions are expected to be dotted pairs of integers,
     * for example, "10.0".
     *
     * Use the parse cache to do this quickly.
     *
     * @param feature The versioned feature name which is to be parsed.
     *
     * @return The name and version text of the feature name.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    public static String[] parseNameAndVersion(String feature) {
        String[] result = parsedNAV.get(feature);
        if (result != null) {
            return result;
        }
        // figure out the base symbolic name and 'version'
        // using last dash as a convention to determine the version and symbolic name
        String baseName = feature;
        String version = null;
        int lastDash = feature.lastIndexOf('-');
        if (lastDash >= 0) {
            // remove the version part of the symbolic name
            version = feature.substring(lastDash + 1);
            // Validate the version syntax
            try {
                Version.parseVersion(version);
                baseName = feature.substring(0, lastDash);
            } catch (IllegalArgumentException e) {
                version = null;
            }
        }
        result = new String[] { baseName, version };
        parsedNAV.put(feature, result);
        return result;
    }

    // Platform handling ...

    public static final String PREFERRED_PLATFORM_VERSIONS_ENV_VAR = "PREFERRED_PLATFORM_VERSIONS";

    private static String preferredPlatformVersions = System.getenv(PREFERRED_PLATFORM_VERSIONS_ENV_VAR);

    private static HashMap<String, ProvisioningFeatureDefinition> allCompatibilityFeatures = new HashMap<>();

    /**
     * Override the environment defined preferred platform value.
     *
     * Clear the cached parsed platforms.
     *
     * This is used for testing.
     *
     * @param preferredPlatformVersions The list of preferred platform versions.
     */
    public static void setPreferredPlatforms(String preferredPlatformVersions) {
        FeatureResolverImpl.preferredPlatformVersions = preferredPlatformVersions;
    }

    /**
     * Process the platforms which were specified within the server configuration
     * or from the command line.
     *
     * This is almost the same as {@link #collectPlatformCompatibilityFeatures(com.ibm.ws.kernel.feature.resolver.FeatureResolverRepository)},
     * except that the platform values are provided using a parameter instead
     * of from an environment variable, and generated error messages are specific
     * to receiving the platforms using a parameter.
     *
     * @param repo          The feature repository.
     * @param rootPlatforms The specified platforms.
     *
     * @return The versioned compatibility feature names obtained from the
     *         platform values.
     */
    private Collection<String> collectConfiguredPlatforms(Repository repo, Collection<String> rootPlatforms, SelectionContext selectionContext) {
        if (rootPlatforms == null) {
            return null;
        }
        List<String> compatibilityFeatures = new ArrayList<String>();
        Map<String, Set<String>> duplicates = new HashMap<>();

        for (String plat : rootPlatforms) {
            //needs check for duplicate platforms with different versions, ex. can't have javaee7.0 and javaee8.0
            ProvisioningFeatureDefinition platformFeature = allCompatibilityFeatures.get(plat);

            if (platformFeature == null) {
                selectionContext.getResult().addMissingPlatform(plat);
                continue;
            }

            String parsedPlatformName = parseName(platformFeature.getSymbolicName());
            if (duplicates.containsKey(parsedPlatformName)) {
                duplicates.get(parsedPlatformName).add(plat);
            } else if (featureListContainsFeatureBaseName(compatibilityFeatures, parsedPlatformName)) {
                Set<String> dupes = new HashSet<String>();
                dupes.add(plat);
                String removeDuplicate = "";
                for (String compatibility : compatibilityFeatures) {
                    if (compatibility.startsWith(parsedPlatformName)) {
                        removeDuplicate = compatibility;
                        break;
                    }
                }
                compatibilityFeatures.remove(removeDuplicate);
                dupes.add(repo.getFeature(removeDuplicate).getPlatformName());
                duplicates.put(parsedPlatformName, dupes);
            } else {
                compatibilityFeatures.add(platformFeature.getSymbolicName());
            }
        }

        for (Map.Entry<String, Set<String>> entry : duplicates.entrySet()) {
            selectionContext.getResult().addDuplicatePlatforms(entry.getKey(), entry.getValue());
        }

        return compatibilityFeatures;
    }

    /**
     * Process the platforms which were specified within the server configuration
     * or from the command line.
     *
     * This is almost the same as {@link #collectPlatformCompatibilityFeatures(com.ibm.ws.kernel.feature.resolver.FeatureResolverRepository, Collection<String>)},
     * except that the platform values are obtained from an environment variable,
     * with corresponding changes to generated error messages.
     *
     * See {@link #PREFERRED_PLATFORM_VERSIONS_ENV_VAR}.
     *
     * @param repo The feature repository.
     *
     * @return The versioned compatibility feature names obtained from the
     *         platform values.
     */
    private List<String> collectEnvironmentPlatforms(Repository repo, Collection<String> rootPlatforms, SelectionContext selectionContext) {
        if (preferredPlatformVersions == null) {
            return Collections.emptyList();
        }

        Set<String> allPlatformBaseNames = selectionContext.compatibilityFeaturesBaseNames();

        if (allPlatformBaseNames.size() == rootPlatforms.size()) {
            return Collections.emptyList();
        }

        String[] preferredPlatforms = preferredPlatformVersions.split(",");

        List<String> compatibilityFeatures = new ArrayList<String>();

        for (String plat : preferredPlatforms) {
            plat = plat.trim();

            ProvisioningFeatureDefinition platformFeature = allCompatibilityFeatures.get(plat.toLowerCase());
            if (platformFeature != null) {
                String baseName = parseName(platformFeature.getSymbolicName());

                if (!featureListContainsFeatureBaseName(rootPlatforms, baseName)) {
                    compatibilityFeatures.add(platformFeature.getSymbolicName());
                }
            } else {
                selectionContext.getResult().addMissingPlatform(plat);
            }
        }

        return compatibilityFeatures;
    }

    /**
     * Process the platforms within the WLP-Platform header of
     * all specified features within the server configuration.
     * Does an intersection of all the features platform values
     * and if the following conditions are satisfied, return it:
     *
     * * theres only one common platform for all features
     * * a platform value in rootPlatforms does not already exist
     *
     *
     * This is almost the same as {@link #collectPlatformCompatibilityFeatures(com.ibm.ws.kernel.feature.resolver.FeatureResolverRepository)},
     * except that the platform values are provided
     * using the features in the server configuration
     *
     * @param repo
     * @param rootPlatforms
     * @param rootFeatures
     * @param selectionContext
     * @return The versioned compatibility feature names obtained from the
     *         platform values.
     */
    private List<String> collectFeaturePlatforms(Repository repo, Collection<String> rootPlatforms, Collection<String> rootFeatures, SelectionContext selectionContext) {
        //Loop through the features and do an intersection of all the platforms of the features
        Map<String, Set<String>> map = new HashMap<>();
        for (String feature : rootFeatures) {
            ProvisioningFeatureDefinition rootFeatureDef = repo.getFeature(feature);

            //This gets handled later
            if (rootFeatureDef == null) {
                continue;
            }

            List<String> wlpPlatform = rootFeatureDef.getPlatformNames();
            if (wlpPlatform != null && wlpPlatform.size() > 0) {
                String[] nav = parseNameAndVersion(wlpPlatform.get(0));
                String compatibilityFeature = selectionContext.getCompatibilityBaseName(nav[0]);
                List<String> featuresPlatforms = new ArrayList<String>();
                for (String platform : wlpPlatform) {
                    ProvisioningFeatureDefinition featureDef = allCompatibilityFeatures.get(platform.toLowerCase());
                    if (featureDef != null) {
                        featuresPlatforms.add(featureDef.getSymbolicName());
                    }
                }
                if (map.containsKey(compatibilityFeature)) {
                    map.get(compatibilityFeature).retainAll(featuresPlatforms);
                } else {
                    if (compatibilityFeature != null)
                        map.put(compatibilityFeature, new HashSet<String>(featuresPlatforms));
                }
            }
        }
        List<String> featurePlatforms = new ArrayList<String>();
        for (String key : map.keySet()) {
            if (featureListContainsFeatureBaseName(rootPlatforms, key)) {
                continue;
            }
            Set<String> current = map.get(key);
            if (current.size() == 1) {
                String compatibilitySymbolicName = current.toArray()[0].toString();
                featurePlatforms.add(compatibilitySymbolicName);
            }
        }
        return featurePlatforms;
    }

    private boolean featureListContainsFeatureBaseName(Collection<String> featureList, String containsFeature) {
        for (String feature : featureList) {
            if (feature.startsWith(containsFeature)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trim and lowercase the platform list
     * @param rootPlatforms
     * @return
     */
    private List<String> normalizeRootPlatforms(Collection<String> rootPlatforms){
        List<String> normalizedPlatforms = new ArrayList<String>();
        for(String plat : rootPlatforms){
            normalizedPlatforms.add(plat.trim().toLowerCase());
        }
        return normalizedPlatforms;
    }

    //////// BEGIN - deprecated resolveFeatures() methods without platforms

    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public Result resolveFeatures(FeatureResolver.Repository repository,
                                  Collection<String> rootFeatures,
                                  Set<String> preResolved,
                                  boolean allowMultipleVersions) {
        return resolve(repository,
                       Collections.<ProvisioningFeatureDefinition> emptySet(),
                       rootFeatures, preResolved,
                       (allowMultipleVersions ? Collections.<String> emptySet() : null),
                       EnumSet.allOf(ProcessType.class), // Default to all process types
                       null);
    }

    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public Result resolveFeatures(FeatureResolver.Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                  Collection<String> rootFeatures,
                                  Set<String> preResolved,
                                  boolean allowMultipleVersions) {
        return resolve(repository,
                       kernelFeatures, rootFeatures, preResolved,
                       (allowMultipleVersions ? Collections.<String> emptySet() : null),
                       EnumSet.allOf(ProcessType.class), // Default to all process types
                       null);
    }

    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public Result resolveFeatures(FeatureResolver.Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                  Collection<String> rootFeatures,
                                  Set<String> preResolved,
                                  boolean allowMultipleVersions,
                                  EnumSet<ProcessType> supportedProcessTypes) {

        return resolve(repository,
                       kernelFeatures, rootFeatures, preResolved,
                       (allowMultipleVersions ? Collections.<String> emptySet() : null),
                       supportedProcessTypes,
                       null);
    }

    /**
     * Old fully parameterized feature resolution method.
     *
     * This has been replaced with {@link #resolve}.
     */
    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public Result resolveFeatures(Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                  Collection<String> rootFeatures,
                                  Set<String> preResolved,
                                  Set<String> allowedMultipleVersions,
                                  EnumSet<ProcessType> supportedProcessTypes) {

        return resolve(repository,
                       kernelFeatures, rootFeatures, preResolved,
                       allowedMultipleVersions,
                       supportedProcessTypes,
                       null);
    }

    //////// END - deprecated resolveFeatures() methods without platforms

    @Override
    public Result resolve(FeatureResolver.Repository repository,
                          Collection<String> rootFeatures,
                          Set<String> preResolved,
                          boolean allowMultipleVersions,
                          Collection<String> rootPlatforms) {

        return resolve(repository,
                       Collections.<ProvisioningFeatureDefinition> emptySet(),
                       rootFeatures,
                       preResolved,
                       (allowMultipleVersions ? Collections.<String> emptySet() : null),
                       EnumSet.allOf(ProcessType.class), // Default to all process types
                       rootPlatforms);
    }

    @Override
    public Result resolve(FeatureResolver.Repository repository,
                          Collection<ProvisioningFeatureDefinition> kernelFeatures,
                          Collection<String> rootFeatures,
                          Set<String> preResolved, boolean allowMultipleVersions,
                          Collection<String> rootPlatforms) {

        return resolve(repository,
                       kernelFeatures, rootFeatures, preResolved,
                       (allowMultipleVersions ? Collections.<String> emptySet() : null),
                       EnumSet.allOf(ProcessType.class), // Default to all process types
                       rootPlatforms);
    }

    @Override
    public Result resolve(FeatureResolver.Repository repository,
                          Collection<ProvisioningFeatureDefinition> kernelFeatures,
                          Collection<String> rootFeatures,
                          Set<String> preResolved,
                          boolean allowMultipleVersions,
                          EnumSet<ProcessType> supportedProcessTypes,
                          Collection<String> rootPlatforms) {

        return resolve(repository,
                       kernelFeatures, rootFeatures, preResolved,
                       (allowMultipleVersions ? Collections.<String> emptySet() : null),
                       supportedProcessTypes,
                       rootPlatforms);
    }

    /**
     * Intercept {@link #doResolve} to inject test actions.
     *
     * When {@link VerifyEnv#REPO_FILE_NAME} is specified, write the
     * repository.
     *
     * When {@link VerifyEnv#RESULTS_SINGLETON_FILE_NAME} is specified, perform
     * test resolutions and write the results.
     *
     * After performing test actions, proceed to {@link #doResolve}.
     */
    @Override
    public Result resolve(Repository repository,
                          Collection<ProvisioningFeatureDefinition> kernelFeatures,
                          Collection<String> rootFeatures, Set<String> preResolved,
                          Set<String> allowedMultiple,
                          EnumSet<ProcessType> supportedProcessTypes,
                          Collection<String> rootPlatforms) {

        FeatureResolverBaseline.generate(this, repository, allowedMultiple, kernelFeatures);

        return doResolve(repository,
                         kernelFeatures, rootFeatures, preResolved,
                         allowedMultiple, supportedProcessTypes,
                         rootPlatforms);
    }

    public Result doResolve(Repository repository,
                            Collection<ProvisioningFeatureDefinition> kernelFeatures,
                            Collection<String> rootFeatures,
                            Set<String> preResolved,
                            Set<String> allowedMultipleVersions,
                            EnumSet<ProcessType> supportedProcessTypes,
                            Collection<String> rootPlatforms) {

        SelectionContext selectionContext = new SelectionContext(repository, allowedMultipleVersions, supportedProcessTypes);
        Collection<String> rootPlatformFeatures = new ArrayList<String>();

        if (hasRootVersionlessFeatures(repository, rootFeatures)) {
            selectionContext.setHasVersionlessFeatures();
            processCompatibilityFeatures(repository.getFeatures());

            rootPlatforms = normalizeRootPlatforms(rootPlatforms);
            rootPlatformFeatures = collectConfiguredPlatforms(repository, rootPlatforms, selectionContext);
            rootPlatformFeatures.addAll(collectEnvironmentPlatforms(repository, rootPlatformFeatures, selectionContext));
            rootPlatformFeatures.addAll(collectFeaturePlatforms(repository, rootPlatformFeatures, rootFeatures, selectionContext));
            for (String platform : rootPlatformFeatures) {
                selectionContext.getResult().addResolvedPlatform(repository.getFeature(platform).getPlatformName());
            }
        }

        // this checks if the pre-resolved exists in the repo;
        // if one does not exist then we start over with an empty set of pre-resolved
        preResolved = checkPreResolvedExistAndSetFullName(preResolved, selectionContext);

        // check that the root features exist and are public; remove them if not; also get the full name
        rootFeatures = checkRootsAreAccessibleAndSetFullName(new ArrayList<String>(rootFeatures), selectionContext, preResolved, rootPlatforms);

        // Always prime the selected with the pre-resolved and the root features.
        // This will ensure that the root and pre-resolved features do not conflict
        Collection<String> rootFeaturesList = new ArrayList<String>(rootFeatures);
        //Implementation for platform element
        if (rootPlatformFeatures != null && selectionContext.getHasVersionlessFeatures()) {
            rootFeaturesList.addAll(rootPlatformFeatures);
        }

        //add versionless after normal resolution for packaging
        List<String> filteredVersionless = new ArrayList<>();
        if (allowedMultipleVersions != null && selectionContext.getHasVersionlessFeatures()) {
            filteredVersionless = filterVersionless(rootFeaturesList, selectionContext);
        }
        //preresolve versionless features for regular resolution
        else if (selectionContext.getHasVersionlessFeatures()) {
            preresolveVersionless(rootFeaturesList, selectionContext, rootPlatformFeatures, filteredVersionless);
        }

        selectionContext.primeSelected(preResolved);
        selectionContext.primeSelected(rootFeaturesList);

        // Even if the feature set hasn't changed, we still need to process the auto features and add any features that need to be
        // installed/uninstalled to the list. This recursively iterates over the auto Features, as previously installed features
        // may satisfy other auto features.
        Set<String> autoFeaturesToInstall = Collections.<String> emptySet();
        Set<String> seenAutoFeatures = new HashSet<String>();
        Set<String> resolved = Collections.emptySet();

        do {
            if (!!!autoFeaturesToInstall.isEmpty()) {
                // this is after the first pass;  use the autoFeaturesToInstall as the roots
                rootFeaturesList = autoFeaturesToInstall;
                // Need to prime the auto features as selected
                selectionContext.primeSelected(autoFeaturesToInstall);
                // and use the resolved as the preResolved
                preResolved = resolved;
                // A new resolution process will happen now along with the auto-features that match;
                // need to save off the current conflicts to be the pre resolved conflicts
                // otherwise they would get lost
                selectionContext.saveCurrentPreResolvedConflicts();
            }
            resolved = doResolveFeatures(rootFeaturesList, preResolved, selectionContext);
        } while (!!!(autoFeaturesToInstall = processAutoFeatures(kernelFeatures, resolved, seenAutoFeatures, selectionContext)).isEmpty());

        //if we filtered versionless features in the pre resolution step, add them back after resolution is over.
        if (!filteredVersionless.isEmpty() && allowedMultipleVersions != null) {
            addBackVersionless(filteredVersionless, selectionContext);
        } else if (selectionContext.getHasVersionlessFeatures()) {
            finalizeVersionlessResults(selectionContext, filteredVersionless, rootPlatforms);
        }

        // Finally return the selected result
        return selectionContext.getResult();
    }

    private boolean hasRootVersionlessFeatures(Repository repo, Collection<String> featureList) {
        for (String s : featureList) {
            ProvisioningFeatureDefinition feature = repo.getFeature(s);
            if (feature == null)
                //Can't find the feature of that name - just skip for now....
                continue;
            if (feature.isVersionless()) {
                return true;
            }
        }
        return false;
    }

    private List<String> checkRootsAreAccessibleAndSetFullName(List<String> rootFeatures, SelectionContext selectionContext, Set<String> preResolved,
                                                               Collection<String> rootPlatforms) {
        ListIterator<String> iRootFeatures = rootFeatures.listIterator();
        while (iRootFeatures.hasNext()) {
            String rootFeatureName = iRootFeatures.next();
            ProvisioningFeatureDefinition rootFeatureDef = selectionContext.getRepository().getFeature(rootFeatureName);
            if (rootFeatureDef == null) {
                selectionContext.getResult().addMissingRoot(rootFeatureName);
                iRootFeatures.remove();
                continue;
            }

            String symbolicName = rootFeatureDef.getSymbolicName();
            if (rootFeatureDef.getVisibility() != Visibility.PUBLIC) {
                selectionContext.getResult().addNonPublicRoot(rootFeatureName);
                iRootFeatures.remove();
            } else if (!supportedProcessType(selectionContext._supportedProcessTypes, rootFeatureDef)) {
                selectionContext.getResult().addWrongRootFeatureType(symbolicName);
                iRootFeatures.remove();
            } else if (preResolved.contains(symbolicName)) {
                iRootFeatures.remove();
            } else {
                iRootFeatures.set(symbolicName); // Normalize to the symbolic name
            }
        }

        return rootFeatures;
    }

    private List<String> getCompatibilityCandidates(String baseName, Collection<String> rootPlatforms) {
        List<String> candidates = new ArrayList<String>();
        for (String plat : rootPlatforms) {
            if (plat.startsWith(baseName)) {
                candidates.add(plat);
            }
        }
        return candidates;
    }

    /**
     * Takes the specified versionless features and attempts to resolve them
     * to a versioned feature before resolution starts. Also does some processing
     * of specified platforms where, if multiple exist (env var was used with
     * multiple values), then store them to be postponed later.
     *
     * @param rootFeatures
     * @param selectionContext
     * @param rootPlatforms
     * @param filteredVersionless
     */
    private void preresolveVersionless(Collection<String> rootFeatures, SelectionContext selectionContext, Collection<String> rootPlatforms, List<String> filteredVersionless) {
        Set<String> addedRootFeatures = new HashSet<>();
        Set<String> removedVersionlessFeatures = new HashSet<>();
        Set<String> multiplePlatforms = new HashSet<>();
        Map<String, Set<String>> noPlatformFeatures = new HashMap<>();

        //Check if there is multiple rootPlatforms configured from the environment variable
        //if there are, we can't preresolve but instead we setup the compatibility feature to be postponed
        Set<String> usedPlatforms = new HashSet<>();
        for (String rootPlatform : rootPlatforms) {
            String baseCompatibilityName = parseName(rootPlatform);
            if (usedPlatforms.contains(baseCompatibilityName)) {
                multiplePlatforms.add(baseCompatibilityName);
            }
            usedPlatforms.add(baseCompatibilityName);
        }

        for (String multPlat : multiplePlatforms) {
            List<String> candidates = getCompatibilityCandidates(multPlat, rootPlatforms);
            rootFeatures.removeAll(candidates);
            rootPlatforms.removeAll(candidates);
            selectionContext.compatibilityFeaturesToPostpone.put(multPlat, new Chain(candidates, parseVersion(candidates.get(0)), candidates.get(0)));
        }

        //Loop through the root features to get the versionless ones
        for (String feature : rootFeatures) {
            ProvisioningFeatureDefinition rootFeatureDef = selectionContext.getRepository().getFeature(feature);
            if (!rootFeatureDef.isVersionless()) {
                continue;
            }
            Collection<FeatureResource> versionlessDeps = rootFeatureDef.getConstituents(SubsystemContentType.FEATURE_TYPE);
            List<String> versionlessLinkingFeatures = new ArrayList<>();
            for (FeatureResource privateVersionless : versionlessDeps) { //versionlessDeps.size will always be 1, the private versionless linking feature
                String[] nav = parseNameAndVersion(privateVersionless.getSymbolicName());
                versionlessLinkingFeatures.add(nav[0] + "-" + nav[1]);

                if (privateVersionless.getTolerates() != null) {
                    for (String version : privateVersionless.getTolerates()) {
                        versionlessLinkingFeatures.add(nav[0] + "-" + version);
                    }
                }
            }
            boolean addFeature = false;
            boolean hasMultiplePlatforms = false;
            String compatibilityBase = null;
            String linkingFeatureBase = null;
            // loops through the private features related to the versionless feature
            for (String linkingFeature : versionlessLinkingFeatures) {
                ProvisioningFeatureDefinition linkingDef = selectionContext.getRepository().getFeature(linkingFeature);
                if (linkingDef == null) {
                    continue;
                }
                linkingFeatureBase = parseName(linkingDef.getSymbolicName());
                Collection<FeatureResource> featureDeps = linkingDef.getConstituents(SubsystemContentType.FEATURE_TYPE);

                // The dependencies of the linking feature, will be either public versioned feature, compatibility feature, or noship feature
                // The logic in the loop makes sure to only handle the public versioned feature.
                for (FeatureResource featureDep : featureDeps) {
                    ProvisioningFeatureDefinition versionedFeature = selectionContext.getRepository().getFeature(featureDep.getSymbolicName());
                    if (versionedFeature == null || versionedFeature.getVisibility() != Visibility.PUBLIC || versionedFeature.getPlatformName() == null) {
                        continue;
                    }
                    compatibilityBase = selectionContext.getCompatibilityBaseName(parseName(versionedFeature.getPlatformName()));

                    if (multiplePlatforms.contains(compatibilityBase)) {
                        hasMultiplePlatforms = true;
                        break;
                    }

                    for (String platform : versionedFeature.getPlatformNames()) {
                        ProvisioningFeatureDefinition featureDef = allCompatibilityFeatures.get(platform.toLowerCase());
                        if (featureDef != null && rootPlatforms.contains(featureDef.getSymbolicName())) {
                            //found a match to the platform, add the versioned feature and filter the versionless features
                            //to be added back later
                            addFeature = true;
                            filteredVersionless.add(linkingDef.getFeatureName());
                            if (!rootFeatures.contains(versionedFeature.getSymbolicName())) {
                                addedRootFeatures.add(versionedFeature.getSymbolicName());
                            }
                            break;
                        }
                    }
                }
            }
            //if the feature was not added, filter it from the features list and set the result to null
            if (!addFeature) {
                if (!usedPlatforms.contains(compatibilityBase)) {
                    if (noPlatformFeatures.containsKey(compatibilityBase)) {
                        noPlatformFeatures.get(compatibilityBase).add(rootFeatureDef.getFeatureName());
                    } else {
                        Set<String> featureWithoutPlatform = new HashSet<>();
                        featureWithoutPlatform.add(rootFeatureDef.getFeatureName());
                        noPlatformFeatures.put(compatibilityBase, featureWithoutPlatform);
                    }
                }
            }
            if (!hasMultiplePlatforms) {
                filteredVersionless.add(rootFeatureDef.getFeatureName());
                removedVersionlessFeatures.add(rootFeatureDef.getSymbolicName());
            }
            //if theres multiple platforms, store the platform to be postponed
            else {
                linkingFeatureBaseNameToCompatibility.put(linkingFeatureBase, compatibilityBase);
            }
        }

        for (Map.Entry<String, Set<String>> featuresWithoutPlatform : noPlatformFeatures.entrySet()) {
            selectionContext.getResult().addNoPlatformVersionless(featuresWithoutPlatform.getKey(), featuresWithoutPlatform.getValue());
        }

        rootFeatures.addAll(addedRootFeatures);
        rootFeatures.removeAll(removedVersionlessFeatures);
    }

    static Map<String, String> linkingFeatureBaseNameToCompatibility = new HashMap<String, String>();

    static private boolean isLinkingFeature(String basename) {
        return linkingFeatureBaseNameToCompatibility.keySet().contains(basename);
    }

    static private String getLinkingFeaturesCompatibility(String basename) {
        return linkingFeatureBaseNameToCompatibility.get(basename);
    }

    /**
     * Take out versionless features from packaging
     *
     * @param rootFeatures
     * @param selectionContext
     * @return
     */
    private List<String> filterVersionless(Collection<String> rootFeatures, SelectionContext selectionContext) {
        List<String> versionless = new ArrayList<String>();

        for (String feature : rootFeatures) {
            ProvisioningFeatureDefinition featureDef = selectionContext.getRepository().getFeature(feature);
            if (featureDef.isVersionless()) {
                versionless.add(feature);
            }
        }

        rootFeatures.removeAll(versionless);

        return versionless;
    }

    /**
     * adds back versionless features after resolution to whatever they got resolved to.
     * This is done when allowMultipleVersions is set.
     *
     * @param versionlessFeatures
     * @param selectionContext
     */
    private void addBackVersionless(List<String> versionlessFeatures, SelectionContext selectionContext) {
        FeatureResolverResultImpl result = selectionContext.getResult();
        Set<String> addingFeatures = new HashSet<>();

        //loop through all the versionless features we filtered out earlier
        for (String versionlessFeature : versionlessFeatures) {
            ProvisioningFeatureDefinition versionlessDef = selectionContext.getRepository().getFeature(versionlessFeature);
            Collection<FeatureResource> versionlessDeps = versionlessDef.getConstituents(SubsystemContentType.FEATURE_TYPE);
            List<String> features = new ArrayList<>();
            for (FeatureResource privateVersionless : versionlessDeps) { //versionlessDeps.size will always be 1, the private versionless feature
                String[] nav = parseNameAndVersion(privateVersionless.getSymbolicName());
                features.add(nav[0] + "-" + nav[1]);

                if (privateVersionless.getTolerates() != null) {
                    for (String version : privateVersionless.getTolerates()) {
                        features.add(nav[0] + "-" + version);
                    }
                }
            }
            // loops through the private features related to the versionless feature
            for (String feature : features) {
                ProvisioningFeatureDefinition featureDef = selectionContext.getRepository().getFeature(feature);
                if (featureDef != null) {
                    boolean addFeature = false;
                    FeatureResource compatibleFeature = null;
                    Collection<FeatureResource> featureDeps = featureDef.getConstituents(SubsystemContentType.FEATURE_TYPE);
                    for (FeatureResource featureDep : featureDeps) { // could be multiple
                        ProvisioningFeatureDefinition versionedFeature = selectionContext.getRepository().getFeature(featureDep.getSymbolicName());
                        if (versionedFeature == null) {
                            continue;
                        }
                        if (versionedFeature.isCompatibility()) {
                            compatibleFeature = featureDep;
                        }
                        // if we resolved the public versioned feature, add the private versionless linking feature
                        if (versionedFeature.getIbmShortName() != null && result._resolved.contains(versionedFeature.getIbmShortName())) {
                            addFeature = true;
                        }
                    }
                    if (addFeature) {
                        addingFeatures.add(feature);
                        if (compatibleFeature != null) {
                            String[] nav = parseNameAndVersion(compatibleFeature.getSymbolicName());
                            String compatibleFeatureName = compatibleFeature.getSymbolicName();
                            if (shouldAddCompatibleFeature(result, compatibleFeatureName, selectionContext)) {
                                addingFeatures.add(compatibleFeatureName);
                            }

                            if (compatibleFeature.getTolerates() != null) {
                                for (String version : compatibleFeature.getTolerates()) {
                                    compatibleFeatureName = nav[0] + "-" + version;
                                    if (shouldAddCompatibleFeature(result, compatibleFeatureName, selectionContext)) {
                                        addingFeatures.add(compatibleFeatureName);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            addingFeatures.add(versionlessFeature);
        }

        result._resolved.addAll(addingFeatures);
    }

    /**
     * Check result for dependencies of the compatibility feature
     * if the dependencies are included, return true
     *
     * @param result
     * @param featureName
     * @return
     */
    private boolean shouldAddCompatibleFeature(FeatureResolverResultImpl result, String featureName, SelectionContext selectionContext) {
        ProvisioningFeatureDefinition feature = selectionContext.getRepository().getFeature(featureName);
        if (feature == null) {
            return false;
        }
        Collection<FeatureResource> featureDeps = feature.getConstituents(SubsystemContentType.FEATURE_TYPE);

        for (FeatureResource featureDep : featureDeps) { // only one
            ProvisioningFeatureDefinition versionedFeature = selectionContext.getRepository().getFeature(featureDep.getSymbolicName());
            if (versionedFeature == null) {
                continue;
            }
            if (result.getResolvedFeatures().contains(versionedFeature.getFeatureName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Add back versionless features to the result after resolution is completed.
     * Stores some versionless specific data into the result.
     *
     * @param selectionContext
     * @param filteredVersionless
     */
    private void finalizeVersionlessResults(SelectionContext selectionContext, List<String> filteredVersionless, Collection<String> rootPlatforms) {
        FeatureResolverResultImpl result = selectionContext.getResult();
        result._resolved.addAll(filteredVersionless);

        //If platform environment variable was used, getResolvedPlatforms() may have multiple platforms
        //Remove every platform except for the one that was used
        Set<String> existingPlatforms = new HashSet<String>();
        existingPlatforms.addAll(result.getResolvedPlatforms());
        result.emptyResolvedPlatforms();
        for (String feature : result.getResolvedFeatures()) {
            ProvisioningFeatureDefinition featureDef = selectionContext.getRepository().getFeature(feature);
            if (featureDef.isCompatibility()) {
                boolean addedPlatform = false;
                if(!rootPlatforms.isEmpty()){
                    for(String platName : featureDef.getPlatformNames()){
                        if(rootPlatforms.contains(platName)){
                            result.addResolvedPlatform(platName);
                            addedPlatform = true;
                        }
                    }
                }
                if (existingPlatforms.contains(featureDef.getPlatformName()) && !addedPlatform) {
                    String platforms = featureDef.getPlatformName();
                    result.addResolvedPlatform(platforms);
                }
            }
        }

        //Loops through the versionless features that were filtered out before resolution
        //going through each of the linking features, find the versioned feature that was added
        //and map them together in the result.versionedFeature map
        for (String versionless : filteredVersionless) {
            boolean added = false;
            ProvisioningFeatureDefinition versionlessFD = selectionContext.getRepository().getFeature(versionless);
            if (!versionlessFD.isVersionless()) {
                continue;
            }
            String linkingBaseName = "";
            for (FeatureResource dependency : versionlessFD.getConstituents(null)) {
                linkingBaseName = parseName(dependency.getSymbolicName());
            }
            for (String linking : filteredVersionless) {
                if (!linkingBaseName.equals(parseName(linking))) {
                    continue;
                }
                for (FeatureResource versionedFeature : selectionContext.getRepository().getFeature(linking).getConstituents(null)) {
                    //Find the right public feature (should only be one) - set the result
                    ProvisioningFeatureDefinition versionedFeatureDef = selectionContext.getRepository().getFeature(versionedFeature.getSymbolicName());
                    if (versionedFeatureDef == null || versionedFeatureDef.getVisibility() != Visibility.PUBLIC) {
                        continue;
                    }
                    result.addVersionlessFeature(versionless, versionedFeatureDef.getFeatureName());
                    added = true;
                }
            }
            if (!added) {
                result.addVersionlessFeature(versionless, null);
            }
        }
        //if versionless features didn't resolve, make sure their result feature is null
        List<String> unresolvedVersionless = new ArrayList<String>();
        for (Map.Entry<String, String> entry : result.getVersionlessFeatures().entrySet()) {
            //check symbolic name
            if (entry.getValue() == null) {
                unresolvedVersionless.add(entry.getKey());
            } else if (!result._resolved.contains(entry.getValue())
                       && !result._resolved.contains(selectionContext.getRepository().getFeature(entry.getValue()).getFeatureName())) {

                unresolvedVersionless.add(entry.getKey());
            }
        }
        for (String unresolved : unresolvedVersionless) {
            result.addVersionlessFeature(unresolved, null);
        }
    }

    /**
     * create the compatibility features map
     */
    private void processCompatibilityFeatures(List<ProvisioningFeatureDefinition> features) {
        allCompatibilityFeatures = new HashMap<>();
        for (ProvisioningFeatureDefinition feature : features) {
            if (feature.isCompatibility()) {
                for(String platformName : feature.getPlatformNames()){
                    allCompatibilityFeatures.put(platformName.toLowerCase(), feature);
                }
            }
        }
    }

    final static boolean supportedProcessType(EnumSet<ProcessType> supportedTypes, ProvisioningFeatureDefinition fd) {
        for (ProcessType processType : fd.getProcessTypes()) {
            if (supportedTypes.contains(processType)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Make sure the pre-resolved features still exist in the repository. If any of them do not
     * then we need to start over with an empty collection. Also set the full symbolic name
     */
    private Set<String> checkPreResolvedExistAndSetFullName(Set<String> preResolved, SelectionContext selectionContext) {
        Set<String> preResolvedSymbolicNames = new LinkedHashSet<String>(preResolved.size());
        for (String preResolvedFeatureName : preResolved) {
            ProvisioningFeatureDefinition preResolvedDef = selectionContext.getRepository().getFeature(preResolvedFeatureName);
            if (preResolvedDef == null) {
                return Collections.emptySet();
            } else {
                preResolvedSymbolicNames.add(preResolvedDef.getSymbolicName());
            }
        }
        return preResolvedSymbolicNames;
    }

    /*
     * There are two phases to resolving the root features.
     * 1) Optimistically process all multiple candidate features to the end picking the most preferred features
     * 2) If this is successful then return the result with no conficts
     * 3) If conflicts were found then backtrack over the permutations and choose different candidates. Note this may create more permutations
     * 4) if a permutation is found that has not conflicts return the results
     * 5) Otherwise use the first permutation and report the original conflicts
     */
    private Set<String> doResolveFeatures(Collection<String> rootFeatures, Set<String> preResolved, SelectionContext selectionContext) {
        // first pass; process the roots until we have selected all the candidates for multiple versions;
        // need to reset the initial blocked count so we can recalculate it during the first pass
        selectionContext.resetInitialBlockedCount();
        Set<String> result = processCurrentPermutation(rootFeatures, preResolved, selectionContext);

        // if the first pass resulted in no conflicts return the results (optimistic)
        if (selectionContext.getResult().getConflicts().isEmpty()) {
            selectionContext.selectCurrentPermutation();
            return result;
        }

        // oh oh, we have conflicts;
        // NOTE, if the current solution has more conflicts than the initial count (blocked)
        // then that means one of the toleration (postponed) choices we made introduced an
        // addition conflict.  That implies that a better solution may be available.
        // As long as there are more conflicts than the number of initial root conflicts
        // and there is a different permutation to try do another pass
        while (selectionContext.currentHasMoreThanInitialBlockedCount() && selectionContext.popPermutation()) {
            result = processCurrentPermutation(rootFeatures, preResolved, selectionContext);
        }

        // Return the best solution found
        selectionContext.restoreBestSolution();
        Set<String> resolvedFeatures = selectionContext.getResult().getResolvedFeatures();
        // return a copy to make sure the returned results do not change in another iteration
        // over auto features
        return new LinkedHashSet<String>(resolvedFeatures);
    }

    Set<String> processCurrentPermutation(Collection<String> rootFeatures, Set<String> preResolved, SelectionContext selectionContext) {
        Set<String> result;
        int numBlocked;
        do {
            selectionContext.processPostponed();
            numBlocked = selectionContext.getBlockedCount();
            result = processRoots(rootFeatures, preResolved, selectionContext);

            // Processing must continue if either:
            //     There are still more postponed features;
            //     More features are blocked;
            //     There are still unresolved versionless features.

        } while (selectionContext.hasPostponed() ||
                 (numBlocked != selectionContext.getBlockedCount()) ||
                 selectionContext.hasTriedVersionlessResolution());

        if (selectionContext.hasPostponedVersionless()) {
            selectionContext.addVersionlessConflicts();
        }

        selectionContext._current._result.setResolvedFeatures(result);
        selectionContext.checkForBestSolution();
        return result;
    }

    private Set<String> processRoots(Collection<String> rootFeatures, Set<String> preResolved, SelectionContext selectionContext) {
        Deque<String> chain = new ArrayDeque<String>();
        // Always prime the result with the preResolved.
        // Using a ordered set to keep behavior of bundle order the same as before
        // TODO we should not have to do this, but it appears auto-features are pretty sensitive to being installed last
        Set<String> result = new LinkedHashSet<String>(preResolved.size());

        for (String key : selectionContext.compatibilityFeaturesToPostpone.keySet()) {
            if (selectionContext.getSelected(key) == null && !selectionContext._current._postponed.containsKey(key)) {
                Chain compatibilityChain = selectionContext.compatibilityFeaturesToPostpone.get(key);

                selectionContext.processCandidates(compatibilityChain.getChain(),
                                                   compatibilityChain.getCandidates(),
                                                   compatibilityChain.getFeatureRequirement(),
                                                   key,
                                                   compatibilityChain.getPreferredVersion().toString(),
                                                   true);
            }
        }

        // Prime the results with the pre-resolved; make sure to use the getFeatureName for the result
        for (String featureSymbolicName : preResolved) {
            ProvisioningFeatureDefinition featureDef = selectionContext._repository.getFeature(featureSymbolicName);
            result.add(featureDef.getFeatureName());
        }
        for (String rootFeatureName : rootFeatures) {
            ProvisioningFeatureDefinition rootFeatureDef = selectionContext.getRepository().getFeature(rootFeatureName);
            if (rootFeatureDef == null) {
                selectionContext.getResult().addMissingReference(rootFeatureName);
            } else {
                processSelected(rootFeatureDef, null, chain, result, selectionContext);
            }
        }
        // Note that this only saves the blocked count on the first call during doResolveFeatures;
        // Any conflicts here will be due to hard failures with no alternative toleration choices.
        // In other words, it is the best conflict count we will ever achieve.
        selectionContext.setInitialRootBlockedCount();
        return result;
    }

    private void processSelected(ProvisioningFeatureDefinition selectedFeature, Set<String> allowedTolerations, Deque<String> chain, Set<String> result,
                                 SelectionContext selectionContext) {
        if (selectedFeature == null) {
            return;
        }

        // first check if the feature is blocked as already in conflict
        String featureName = selectedFeature.getSymbolicName();

        String baseFeatureName = parseNameAndVersion(featureName)[0];
        if (selectionContext.isBlocked(baseFeatureName)) {
            return;
        }

        // Validation to make sure this feature is selected; this is really just to check bugs in the resolver
        if (selectedFeature.isSingleton() && !!!selectionContext.allowMultipleVersions(baseFeatureName)) {
            Chain existingSelection = selectionContext.getSelected(baseFeatureName);
            String selectedFeatureName = existingSelection == null ? null : existingSelection.getCandidates().get(0);
            if ((existingSelection == null) || !!!featureName.equals(selectedFeatureName)) {
                throw new IllegalStateException("Expected feature \"" + featureName + "\" to be selected instead feature of \"" + selectedFeatureName);
            }
        }

        if (chain.contains(featureName)) {
            // must be in a cycle
            return;
        }

        chain.addLast(featureName);
        try {
            // Depth-first: process any included features first.
            // Postpone decisions on variable candidates until after the first pass
            Collection<FeatureResource> includes = selectedFeature.getConstituents(SubsystemContentType.FEATURE_TYPE);

            boolean isRoot = chain.size() == 1;
            // do a first pass to get all the base feature names that are included
            Set<String> includedBaseFeatureNames = new HashSet<String>();
            for (FeatureResource included : includes) {
                String symbolicName = included.getSymbolicName();
                if (symbolicName != null) {
                    String[] nameAndVersion = parseNameAndVersion(included.getSymbolicName());
                    String baseName = nameAndVersion[0];
                    includedBaseFeatureNames.add(baseName);
                }
            }
            if (allowedTolerations == null) {
                // if allowTolerations is null then use the whole includedBaseFeatureNames, this is a root feature; but lets make sure
                if (!!!isRoot) {
                    throw new IllegalStateException("A null allowTolerations is only valid for root features.");
                }
            } else {
                // otherwise we need to take the intersection from the parent's toleration with the ones we include directly here
                includedBaseFeatureNames.retainAll(allowedTolerations);
            }
            allowedTolerations = includedBaseFeatureNames;
            for (FeatureResource included : includes) {
                processIncluded(selectedFeature, included, allowedTolerations, chain, result, selectionContext);
            }
        } finally {
            chain.removeLast();
            // NOTE 1: We always add the feature to the results because if we are processing the feature
            // then we know it is the selected one; see check above
            // NOTE 2: We add after processing included so that we get a the same order as previous feature manager
            // (TODO we really should not need to do this!! but things seem to really depend on install order!!)
            // NOTE 3: it is very important that the result includes the full feature name, not just the symbolic name
            // this ensures we include the product extension prefix if it exists.
            String name = selectedFeature.getFeatureName();
            result.add(name);
        }
    }

    private void processIncluded(ProvisioningFeatureDefinition includingFeature, FeatureResource included, Set<String> allowedTolerations, Deque<String> chain, Set<String> result,
                                 SelectionContext selectionContext) {
        String symbolicName = included.getSymbolicName();

        if (symbolicName == null) {
            // TODO why do we report this feature as missing, seems a better error message would indicate the FeatureResource requirement has no SN
            // get the symbolic name from the last in chain
            if (!chain.isEmpty()) {
                selectionContext.getResult().addUnlabelledResource(included, chain);
            }
            return;
        }

        String[] nameAndVersion = parseNameAndVersion(symbolicName);
        String baseSymbolicName = nameAndVersion[0];
        String preferredVersion = nameAndVersion[1];
        boolean isSingleton = false;

        // if the base name is blocked then we do not continue with this include
        if (selectionContext.isBlocked(baseSymbolicName)) {
            return;
        }

        // only look in tolerates if the base name is allowed to be tolerated
        List<String> tolerates = included.getTolerates();
        List<String> overrideTolerates = selectionContext.getRepository().getConfiguredTolerates(baseSymbolicName);
        if (!!!overrideTolerates.isEmpty()) {
            tolerates = tolerates == null ? new ArrayList<String>() : new ArrayList<String>(tolerates);
            // Note that we do not check for dups here, that is handled while getting the actual candidates below
            tolerates.addAll(overrideTolerates);
        }

        List<String> candidateNames = new ArrayList<String>(1 + (tolerates == null ? 0 : tolerates.size()));

        // look for the preferred feature using the fully qualified symbolicName first
        ProvisioningFeatureDefinition preferredCandidateDef = selectionContext.getRepository().getFeature(symbolicName);
        if ((preferredCandidateDef != null) && isAccessible(includingFeature, preferredCandidateDef)) {
            checkForFullSymbolicName(preferredCandidateDef, symbolicName, chain.getLast());
            isSingleton = preferredCandidateDef.isSingleton();
            candidateNames.add(symbolicName);
        }

        // Check for tolerated versions; but only if the preferred version is a singleton or we did not find the preferred version
        if ((tolerates != null) && (candidateNames.isEmpty() || isSingleton)) {
            for (String tolerate : tolerates) {
                if (selectionContext.allowMultipleVersions(baseSymbolicName)) {
                    // if we are in minify mode (_allowMultipleVersions) then we only want to continue to look for
                    // tolerated versions until we have found one candidate
                    if (!!!candidateNames.isEmpty()) {
                        break;
                    }
                }
                String toleratedSymbolicName = baseSymbolicName + '-' + tolerate;
                ProvisioningFeatureDefinition toleratedCandidateDef = selectionContext.getRepository().getFeature(toleratedSymbolicName);
                if ((toleratedCandidateDef != null) && !!!candidateNames.contains(toleratedCandidateDef.getSymbolicName())
                    && isAccessible(includingFeature, toleratedCandidateDef)) {
                    checkForFullSymbolicName(toleratedCandidateDef, toleratedSymbolicName, chain.getLast());
                    isSingleton |= toleratedCandidateDef.isSingleton();
                    // Only check against the allowed tolerations if this candidate feature is public or protected (NOT private)
                    if (isAllowedToleration(selectionContext, toleratedCandidateDef, allowedTolerations, overrideTolerates, baseSymbolicName, tolerate, chain)) {
                        candidateNames.add(toleratedCandidateDef.getSymbolicName());
                    }
                }
            }
        }

        if (!!!isSingleton && (candidateNames.size() > 1)) {
            // If the candidates are not singleton and there are multiple then that means
            // someone is using tolerates for a non-singleton feature (error case?).
            // For now just use the first candidate
            candidateNames.retainAll(Collections.singleton(candidateNames.get(0)));
        }

        selectionContext.processCandidates(chain, candidateNames, symbolicName, baseSymbolicName, preferredVersion, isSingleton);

        // If a single candidate remains,
        // and it is not an unresolved versionless feature,
        // process that candidate as a selection.

        //revisit with versionless updates
        if ((candidateNames.size() == 1) && (!!!isLinkingFeature(baseSymbolicName)
                                             || (isLinkingFeature(baseSymbolicName) && (selectionContext.getSelected(baseSymbolicName) != null)))) {

            String selectedName = candidateNames.get(0);
            processSelected(selectionContext.getRepository().getFeature(selectedName),
                            allowedTolerations, chain, result, selectionContext);
        }
    }

    private boolean isAccessible(ProvisioningFeatureDefinition includingFeature, ProvisioningFeatureDefinition candidateDef) {
        return !!!candidateDef.isVersionless()
               && ((candidateDef.getVisibility() != Visibility.PRIVATE) || includingFeature.getBundleRepositoryType().equals(candidateDef.getBundleRepositoryType()));
    }

    private boolean isAllowedToleration(SelectionContext selectionContext, ProvisioningFeatureDefinition toleratedCandidateDef, Set<String> allowedTolerations,
                                        List<String> overrideTolerates,
                                        String baseSymbolicName, String tolerate, Deque<String> chain) {
        // if in minify mode always allow (_allowMultipleVersions)
        if (selectionContext.allowMultipleVersions(baseSymbolicName)) {
            return true;
        }
        // all private features tolerations are allowed
        if (Visibility.PRIVATE == toleratedCandidateDef.getVisibility()) {
            return true;
        }
        // if it is part of the allowed tolerates from the dependency chain then allow
        if (allowedTolerations.contains(baseSymbolicName)) {
            return true;
        }
        // otherwise if it is part of the override list
        if (overrideTolerates.contains(tolerate)) {
            return true;
        }
        // if it comes from a versionless feature
        if (selectionContext.getRepository().getFeature(chain.peekFirst()).isVersionless()) {
            return true;
        }
        return false;
    }

    private void checkForFullSymbolicName(ProvisioningFeatureDefinition candidateDef, String symbolicName, String includingFeature) {
        if (!!!symbolicName.equals(candidateDef.getSymbolicName())) {
            throw new IllegalArgumentException("A feature is not allowed to use short feature names when including other features. "
                                               + "Detected short name \"" + symbolicName + "\" being used instead of \"" + candidateDef.getSymbolicName()
                                               + "\" by feature \"" + includingFeature + "\".");
        }
    }

    /*
     * This method is passed a list of features to check against the auto features. For each auto feature, the method checks to see if the capability
     * statement has been satisfied by any of the other features in the list. If so, it is add to the list of features to process.
     * We then need to recursively check the new set of features to see if other features have their capabilities satisfied by these auto features and keep
     * going round until we've got the complete list.
     */
    private Set<String> processAutoFeatures(Collection<ProvisioningFeatureDefinition> kernelFeatures, Set<String> result, Set<String> seenAutoFeatures,
                                            SelectionContext selectionContext) {

        Set<String> autoFeaturesToProcess = new HashSet<String>();

        Set<ProvisioningFeatureDefinition> filteredFeatureDefs = new HashSet<ProvisioningFeatureDefinition>(kernelFeatures);
        for (String feature : result) {
            filteredFeatureDefs.add(selectionContext.getRepository().getFeature(feature));
        }

        // Iterate over all of the auto-feature definitions...
        for (ProvisioningFeatureDefinition autoFeatureDef : selectionContext.getRepository().getAutoFeatures()) {
            String featureSymbolicName = autoFeatureDef.getSymbolicName();

            // if we haven't been here before, check the capability header against the list of
            // installed features to see if it should be auto-installed.
            if (!seenAutoFeatures.contains(featureSymbolicName)) {
                if (autoFeatureDef.isCapabilitySatisfied(filteredFeatureDefs)) {

                    // Add this auto feature to the list of auto features to ignore on subsequent recursions.
                    seenAutoFeatures.add(featureSymbolicName);
                    // Add the feature to the return value of auto features to install if they are supported in this process.
                    if (supportedProcessType(selectionContext._supportedProcessTypes, autoFeatureDef)) {
                        autoFeaturesToProcess.add(featureSymbolicName);
                    }
                }
            }
        }

        return autoFeaturesToProcess;
    }

    static class DeadEndChain extends Exception {
        private static final long serialVersionUID = 1L;
    }

    /*
     * The selection context maintains the state of the resolve operation.
     * It records the selected candidates, the postponed decisions and
     * any blocked features. It also keeps a stack of permutations
     * that can be used to backtrack earlier decisions.
     */
    static class SelectionContext {
        static class Permutation {
            final Map<String, Chain> _selected = new HashMap<String, Chain>();
            final Map<String, Chains> _postponed = new LinkedHashMap<String, Chains>();
            final Map<String, Chains> _postponedVersionless = new LinkedHashMap<String, Chains>();
            final Set<String> _blockedFeatures = new HashSet<String>();
            final Set<String> _postponedFeaturesTried = new HashSet<String>();
            final FeatureResolverResultImpl _result = new FeatureResolverResultImpl();

            //possibly remove deadendchain
            Permutation copy(Map<String, Collection<Chain>> preResolveConflicts) throws DeadEndChain {
                Permutation copy = new Permutation();
                copy._selected.putAll(_selected);

                // The conflicts should get populated from the preResolveConflicts (if any);
                // other conflicts will get recalculated
                copy._result._conflicts.putAll(preResolveConflicts);

                // copy the missing and nonPublicRoots from the result.
                // The wrongProcessTypes will get recalculated;
                // The resolved are set at the end of processing the permutation;
                copy._result._missing.addAll(_result.getMissing());
                copy._result._nonPublicRoots.addAll(_result.getNonPublicRoots());

                // for versionless resolution, copy the missingPlatforms, resolvedPlatforms,
                // noPlatformVersionless, and duplicatePlatforms
                // The versionlessFeatures and resolvedPlatforms will get recalculated
                copy._result._missingPlatforms.addAll(_result._missingPlatforms);
                copy._result._resolvedPlatforms.addAll(_result._resolvedPlatforms);
                copy._result._noPlatformVersionless.putAll(_result._noPlatformVersionless);
                copy._result._duplicatePlatforms.putAll(_result._duplicatePlatforms);

                // now we need to copy each postponed Chains
                for (Map.Entry<String, Chains> chainsEntry : _postponed.entrySet()) {
                    copy._postponed.put(chainsEntry.getKey(), chainsEntry.getValue().copy());
                }

                // now we need to copy each postponed Chains
                for (Map.Entry<String, Chains> chainsEntry : _postponedVersionless.entrySet()) {
                    copy._postponedVersionless.put(chainsEntry.getKey(), chainsEntry.getValue().copy());
                }

                copy._postponedFeaturesTried.addAll(_postponedFeaturesTried);

                // NOTE the blocked features are NOT copied; they get recalculated
                return copy;
            }
        }

        final Map<String, Chain> compatibilityFeaturesToPostpone = new HashMap<String, Chain>();
        private final FeatureResolver.Repository _repository;
        private final Deque<Permutation> _permutations = new ArrayDeque<Permutation>(Arrays.asList(new Permutation()));
        private final Set<String> _allowedMultipleVersions;
        private final EnumSet<ProcessType> _supportedProcessTypes;
        private final AtomicInteger _initialBlockedCount = new AtomicInteger(-1);
        private final Map<String, Collection<Chain>> _preResolveConflicts = new HashMap<String, Collection<Chain>>();
        private Permutation _current = _permutations.getFirst();
        private boolean triedVersionless = false;
        private boolean hasVersionlessFeatures = false;

        void setHasVersionlessFeatures() {
            hasVersionlessFeatures = true;
        }

        boolean getHasVersionlessFeatures() {
            return hasVersionlessFeatures;
        }

        SelectionContext(FeatureResolver.Repository repository, Set<String> allowedMultipleVersions, EnumSet<ProcessType> supportedProcessTypes) {
            this._repository = repository;
            this._allowedMultipleVersions = allowedMultipleVersions;
            this._supportedProcessTypes = supportedProcessTypes;
        }

        void saveCurrentPreResolvedConflicts() {
            _preResolveConflicts.clear();
            _preResolveConflicts.putAll(_current._result.getConflicts());
        }

        void resetInitialBlockedCount() {
            _initialBlockedCount.set(-1);
        }

        boolean currentHasMoreThanInitialBlockedCount() {
            return getBlockedCount() > _initialBlockedCount.get();
        }

        void setInitialRootBlockedCount() {
            // only set this once
            _initialBlockedCount.compareAndSet(-1, getBlockedCount());
        }

        void restoreBestSolution() {
            // NOTE that the best solution is kept as the last permutation
            while (popPermutation()) {
                // EMPTY
            }
            _current = _permutations.getFirst();
        }

        void selectCurrentPermutation() {
            _permutations.clear();
            _permutations.addFirst(_current);
        }

        void checkForBestSolution() {
            // check if the current best (store as the last permutation) has more conflicts
            // than the current solution.
            if (_permutations.getLast()._result.getConflicts().size() > _current._result.getConflicts().size()) {
                // Replace the current best (stored as the last permutation)
                _permutations.pollLast();
                _permutations.addLast(_current);
            }
        }

        boolean popPermutation() {
            // we only pop as long as there is more than one because we don't want to reuse the first
            Permutation popped = _permutations.size() > 1 ? _permutations.pollFirst() : null;
            if (popped != null) {
                triedVersionless = false;
                _current = popped;
                return true;
            }
            return false;
        }

        @FFDCIgnore(DeadEndChain.class)
        void pushPermutation() {
            // We only want to backtrack this decision if the current
            // permutation does not add more blocked conflicts to the initial root conflicts
            if (_initialBlockedCount.get() == getBlockedCount()) {
                try {
                    _permutations.addFirst(_current.copy(_preResolveConflicts));
                } catch (DeadEndChain e) {
                    // expected if we are at the end of our options on a chain
                }
            }
        }

        FeatureResolver.Repository getRepository() {
            return _repository;
        }

        boolean isBlocked(String baseSymbolicName) {
            return _current._blockedFeatures.contains(baseSymbolicName);
        }

        boolean allowMultipleVersions(String baseSymbolicName) {
            return ((_allowedMultipleVersions != null) && ((_allowedMultipleVersions.size() == 0) || _allowedMultipleVersions.contains(baseSymbolicName)));
        }

        int getBlockedCount() {
            return _current._blockedFeatures.size();
        }

        FeatureResolverResultImpl getResult() {
            return _current._result;
        }

        Set<String> compatibilityFeaturesBaseNames() {
            Collection<ProvisioningFeatureDefinition> values = allCompatibilityFeatures.values();
            Set<String> baseNames = new HashSet<>();
            for (ProvisioningFeatureDefinition value : values) {
                baseNames.add(parseNameAndVersion(value.getSymbolicName())[0]);
            }
            return baseNames;
        }

        /** Table mapping base platform names to base compatibility feature names. */
        private Map<String, String> platToCompat;

        Map<String, String> platformToCompatibilityBaseName() {
            if (platToCompat == null) {
                Set<String> keys = allCompatibilityFeatures.keySet();
                Map<String, String> usePlatToCompat = new HashMap<>();
                for (String key : keys) {
                    String baseKey = parseName(key);
                    if (!usePlatToCompat.containsKey(baseKey)) {
                        usePlatToCompat.put(baseKey, parseName(allCompatibilityFeatures.get(key).getSymbolicName()));
                    }
                }
                platToCompat = usePlatToCompat;
            }
            return platToCompat;
        }

        String getCompatibilityBaseName(String plat) {
            Map<String, String> baseNames = platformToCompatibilityBaseName();
            return ((baseNames == null) ? null : baseNames.get(plat.toLowerCase()));
        }

        void processCandidates(Collection<String> chain,
                               List<String> candidateNames,
                               String symbolicName, String baseSymbolicName, String preferredVersion,
                               boolean isSingleton) {

            // A versionless candidate cannot be resolved until a version of the
            // corresponding compatibility feature is selected.  That happens either
            // because a platform was specified, or because a resolved feature pulls in
            // a specific compatibility feature.

            //if versionless, check if its corresponding compatibility feature has been resolved, otherwise postpone
            if (isLinkingFeature(baseSymbolicName)
                && getSelected(getLinkingFeaturesCompatibility(baseSymbolicName)) == null) {

                addPostponed(baseSymbolicName, new Chain(chain, candidateNames, preferredVersion, symbolicName));
                return;
            }

            // first check for container type
            List<String> origCandidateNames = new ArrayList<>(candidateNames);
            for (Iterator<String> iCandidateNames = candidateNames.iterator(); iCandidateNames.hasNext();) {
                ProvisioningFeatureDefinition fd = _repository.getFeature(iCandidateNames.next());
                if (!supportedProcessType(_supportedProcessTypes, fd)) {
                    Chain c = new Chain(chain, candidateNames, preferredVersion, symbolicName);
                    _current._result.addWrongResolvedFeatureType(symbolicName, c);
                    iCandidateNames.remove();
                }
            }
            if (candidateNames.isEmpty()) {
                _current._result.addIncomplete(symbolicName, origCandidateNames, chain);
                return;
            }
            if (!isSingleton || allowMultipleVersions(baseSymbolicName)) {
                // must allow all candidates
                return;
            }

            // make a copy for the chain if there is a conflict
            List<String> copyCandidates = new ArrayList<String>(candidateNames);
            // check if the base symbolic name is already selected and different than the candidates
            Chain selectedChain = getSelected(baseSymbolicName);
            if (selectedChain != null) {
                // keep only the selected candidates (it will be only one)
                candidateNames.retainAll(selectedChain.getCandidates());
                if (candidateNames.isEmpty()) {
                    addConflict(baseSymbolicName, asList(selectedChain, new Chain(chain, copyCandidates, preferredVersion, symbolicName)));
                    return;
                }
            }
            if (candidateNames.size() > 1) {
                // if the candidates are more than one then postpone the decision
                addPostponed(baseSymbolicName, new Chain(chain, candidateNames, preferredVersion, symbolicName));
                return;
            }

            // must select this one
            String selectedName = candidateNames.get(0);
            if (isLinkingFeature(baseSymbolicName)) {
                for (FeatureResource versionedFeature : _repository.getFeature(selectedName).getConstituents(null)) {
                    //Find the right public feature (should only be one) - set the result
                    ProvisioningFeatureDefinition versionedFeatureDef = _repository.getFeature(versionedFeature.getSymbolicName());
                    if (versionedFeatureDef.getVisibility() != Visibility.PUBLIC) {
                        continue;
                    }
                    getResult().addVersionlessFeature(chain.toArray()[0].toString(), versionedFeature.getSymbolicName());
                }
            }
            // check if there is a postponed decision
            Chain conflict = getPostponedConflict(baseSymbolicName, selectedName);
            if (conflict != null) {
                addConflict(baseSymbolicName, asList(conflict, new Chain(chain, copyCandidates, preferredVersion, symbolicName)));
                // Note that we do not return here because we have a single candidate that must be selected
                // and one or more postponed decisions that conflict with the single candidate.
                // We must continue on here and select the single candidate, but record the confict
                // from the postponed
            }

            // We have selected one; only create a new chain if there was not an existing selected
            // This can happen if there is a root feature X-1.0 and a transitive dependency on X that tolerates multiple versions.
            // It also can happen when processing the postponed decisions on a subsequent resolve pass after selecting the features we are
            // going to load.  In that case we don't replace the existing chain, but we still need to proceed with removing the
            // postponed decision and processing the selected.
            if (selectedChain == null) {
                _current._selected.put(baseSymbolicName, new Chain(chain, Collections.singletonList(selectedName), preferredVersion, symbolicName));
            }
            // if there was a postponed decision remove it
            _current._postponed.remove(baseSymbolicName);
        }

        List<Chain> asList(Chain chain1, Chain chain2) {
            List<Chain> result = new ArrayList<Chain>(2);
            result.add(chain1);
            result.add(chain2);
            return result;
        }

        Chain getSelected(String baseName) {
            return _current._selected.get(baseName);
        }

        boolean hasPostponed() {
            return !!!_current._postponed.isEmpty();
        }

        boolean hasPostponedVersionless() {
            return !!!_current._postponedVersionless.isEmpty();
        }

        void addVersionlessConflicts() {
            for (String s : _current._postponedVersionless.keySet()) {
                _current._result.addVersionlessFeature(_current._postponedVersionless.get(s).getChains().get(0).getChain().get(0), null);
            }
        }

        // Versionless features require eeCompatible to be resolved. In rare cases, eeCompatible will be resolved after
        // all versionles features have been postponed, and nothing else is postponed except for versionless features.
        // In that case we need to run the resolve loop one more time in order to not skip versionless features.

        //I think we can delete this once we are done our changes with wlp platform, need to test
        boolean hasTriedVersionlessResolution() {
            if (!triedVersionless) {
                triedVersionless = true;
                return !!!_current._postponedVersionless.isEmpty();
            }
            return false;
        }

        boolean hasAtLeastOneSelected(Set<String> baseNames) {
            for (String feature : baseNames) {
                if (getSelected(feature) != null) {
                    return true;
                }
            }
            return false;
        }

        void processPostponed() {
            if (_current._postponed.isEmpty() && _current._postponedVersionless.isEmpty()) {
                return;
            }

            // Only process the first postponed and try again;
            // We have to do this one postpone at a time because
            // The decision of one postpone may effect the path of the
            // dependency in such a way to make later postponed decisions
            // unnecessary

            //if a versionless feature is postponed, process that first
            if (!!!_current._postponedVersionless.isEmpty() && hasAtLeastOneSelected(compatibilityFeaturesBaseNames())) {

                Set<String> entries = _current._postponedVersionless.keySet();
                Iterator<Map.Entry<String, Chains>> postponedVersionlessIterator = _current._postponedVersionless.entrySet().iterator();
                Map.Entry<String, Chains> firstPostponedVersionless = null;

                //Check if we have any postponed versionless features that can be resolved,
                //If we do, choose the first one we see
                while (postponedVersionlessIterator.hasNext()) {
                    firstPostponedVersionless = postponedVersionlessIterator.next();

                    if (getSelected(getLinkingFeaturesCompatibility(firstPostponedVersionless.getKey())) != null) {
                        break;
                    }
                    firstPostponedVersionless = null;
                }

                if (firstPostponedVersionless != null) {
                    // try to find a good selection
                    Chain selected = firstPostponedVersionless.getValue().select(firstPostponedVersionless.getKey(), this);
                    if (selected != null) {
                        // found a good one, select it.
                        _current._selected.put(firstPostponedVersionless.getKey(), selected);
                    }

                    // clean postponed since we will walk the tree again and find them again if necessary
                    _current._postponed.clear();
                    _current._postponedVersionless.clear();
                    return;
                }
            }

            if (!!!_current._postponed.isEmpty()) {
                Map.Entry<String, Chains> firstPostponed = _current._postponed.entrySet().iterator().next();
                // try to find a good selection

                Chain selected = null;
                String postponedBaseName = firstPostponed.getKey();
                if (hasVersionlessFeatures && !_current._postponedFeaturesTried.contains(postponedBaseName)) {
                    _current._postponedFeaturesTried.add(postponedBaseName);
                    selected = firstPostponed.getValue().selectTryFirst(postponedBaseName, this);
                } else {
                    selected = firstPostponed.getValue().select(postponedBaseName, this);
                }

                if (selected != null) {
                    // found a good one, select it.
                    _current._selected.put(postponedBaseName, selected);
                }

                // clean postponed since we will walk the tree again and find them again if necessary
                _current._postponed.clear();
                _current._postponedVersionless.clear();
            }
        }

        void primeSelected(Collection<String> features) {
            if ((_allowedMultipleVersions != null) && (_allowedMultipleVersions.size() == 0)) {
                // no need to do any selecting when allowing multiple versions
                return;
            }
            // Need to prime each feature as a selected feature, while also checking that
            // there are not any current conflicts in the collection.
            // Note that the features may include two versions of the same feature (e.g. servlet-3.0 and servlet-3.1)
            // this case needs to be handled by removing both versions from the features collection
            // and blocking the base feature name (e.g. servlet)
            Map<String, String> conflicts = new HashMap<String, String>();
            for (Iterator<String> iFeatures = features.iterator(); iFeatures.hasNext();) {
                String featureName = iFeatures.next();
                ProvisioningFeatureDefinition featureDef = _repository.getFeature(featureName);
                if ((featureDef != null) && featureDef.isSingleton()) {
                    // Only need to prime selected for singletons.
                    // Be sure to get the real symbolic name; don't just use the feature name used to do the lookup
                    String featureSymbolicName = featureDef.getSymbolicName();
                    String[] nameAndVersion = parseNameAndVersion(featureSymbolicName);
                    String base = nameAndVersion[0];
                    String preferredVersion = nameAndVersion[1];
                    if (allowMultipleVersions(base)) {
                        continue;
                    }
                    // check for an existing selection for this base feature name
                    Chain selectedChain = _current._selected.get(base);
                    if (selectedChain != null) {
                        // TODO Need to revisit why this is not always a conflict.
                        // We only keep the first selected one
                        iFeatures.remove();
                        // check if the selected feature is contained in the features collection;
                        // if so then it is a conflict also and we need to clean it up and block it
                        String selectedFeature = selectedChain.getCandidates().get(0);
                        if (features.contains(selectedFeature)) {
                            Chain conflictedFeatureChain = new Chain(featureSymbolicName, preferredVersion, featureSymbolicName);
                            addConflict(base, asList(selectedChain, conflictedFeatureChain));
                            conflicts.put(selectedFeature, base);
                        }
                    } else {
                        _current._selected.put(base, new Chain(featureSymbolicName, preferredVersion, featureSymbolicName));
                    }
                }
            }
            // remove any conflicts recorded above
            for (Map.Entry<String, String> conflict : conflicts.entrySet()) {
                features.remove(conflict.getKey());
                _current._selected.remove(conflict.getValue());
            }
        }

        void addPostponed(String baseName, Chain chain) {
            Map<String, Chains> usePostponed;
            if (isLinkingFeature(baseName)) {
                usePostponed = _current._postponedVersionless;
            } else {
                usePostponed = _current._postponed;
            }
            Chains existing = usePostponed.get(baseName);
            if (existing == null) {
                existing = new Chains();
                usePostponed.put(baseName, existing);
            }
            existing.add(chain);
        }

        Chain getPostponedConflict(String baseName, String selectedName) {
            Chains postponedChains = _current._postponed.get(baseName);
            return postponedChains == null ? null : postponedChains.findConflict(selectedName);
        }

        void addConflict(String baseFeatureName, List<Chain> conflicts) {
            _current._blockedFeatures.add(baseFeatureName);
            _current._result.addConflict(baseFeatureName, conflicts);
        }

        Permutation getCurrent() {
            try {
                return _current.copy(_preResolveConflicts);
            } catch (DeadEndChain e) {
                // expected if we are at the end of our options on a chain
            }
            return null;
        }
    }

    static class Chains implements Comparator<Chain> {
        private final Set<String> _attempted = new HashSet<String>();
        private final List<Chain> _chains = new ArrayList<Chain>();

        void add(Chain chain) {
            int insertion = Collections.binarySearch(_chains, chain, this);
            if (insertion < 0) {
                insertion = (-(insertion) - 1);
            } else {
                // make sure we insert in the order we are added when we have the same preferred;
                // we do this by checking each insertion element to see if we are equal
                // until we find one that is not
                Chain existing;
                while ((insertion < _chains.size()) && ((existing = _chains.get(insertion)) != null) && (compare(existing, chain) == 0)) {
                    insertion++;
                }
            }
            _chains.add(insertion, chain);
        }

        public Chains copy() throws DeadEndChain {
            // make sure the chains have more options left to try
            if (noMoreCandidatesToTry()) {
                throw new DeadEndChain();
            }
            Chains copy = new Chains();
            copy._chains.addAll(_chains);
            copy._attempted.addAll(_attempted);

            return copy;
        }

        /**
         * Returns true if one or more chains have no more options
         * to try (a DeadEndChain).
         *
         * @return true if one or more chains have no more options to try.
         */
        private boolean noMoreCandidatesToTry() {
            // check each chain to see if all their candidates have been attempted.
            for (Chain chain : _chains) {
                boolean allAttempted = true;
                for (String candidate : chain.getCandidates()) {

                    allAttempted &= _attempted.contains(candidate);
                    if (!allAttempted) {
                        break;
                    }
                }
                if (allAttempted) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int compare(Chain o1, Chain o2) {
            // We sort by preferred version where lowest sorts first
            return o1.getPreferredVersion().compareTo(o2.getPreferredVersion());
        }

        //used only when versionless features exist in the config. Try the candidate that
        //is part of the configured platform
        Chain selectTryFirst(String baseFeatureName, SelectionContext selectionContext) {
            for (Chain selectedChain : _chains) {
                for (String candidate : selectedChain.getCandidates()) {
                    ProvisioningFeatureDefinition feature = selectionContext.getRepository().getFeature(candidate);
                    if (feature.getVisibility() != Visibility.PUBLIC || feature.getPlatformNames() == null) {
                        continue;
                    }
                    for (String plat : feature.getPlatformNames()) {
                        Chain c = selectionContext.getSelected(selectionContext.getCompatibilityBaseName(parseName(plat)));
                        if (c != null) {
                            ProvisioningFeatureDefinition fd = allCompatibilityFeatures.get(plat.toLowerCase());
                            if (fd == null)
                                continue;
                            if (c.getCandidates().size() == 1 && c.getCandidates().get(0).equals(fd.getSymbolicName())) {
                                Chain match = match(candidate, selectedChain, selectionContext);
                                if (match != null) {
                                    return new Chain(selectedChain.getChain(), Collections.singletonList(candidate), feature.getVersion().toString(),
                                                     selectedChain.getFeatureRequirement());
                                }
                            }
                        }
                    }
                }
            }

            return select(baseFeatureName, selectionContext);
        }

        Chain select(String baseFeatureName, SelectionContext selectionContext) {
            // First iterate over the chains in order trying the first candidate in each
            // This is done first because the chains are ordered by preferred versions
            // where their first candidate is the preferred one
            for (Chain selectedChain : _chains) {
                String preferredCandidate = selectedChain.getCandidates().get(0);
                if (_attempted.add(preferredCandidate)) {
                    Chain match = match(preferredCandidate, selectedChain, selectionContext);
                    if (match != null) {
                        return match;
                    }
                }
            }
            for (Chain selectedChain : _chains) {
                // now check every candidate since the preferred ones did not give a match
                for (String candidate : selectedChain.getCandidates()) {
                    if (_attempted.add(candidate)) {
                        Chain match = match(candidate, selectedChain, selectionContext);
                        if (match != null) {
                            return match;
                        }
                    }
                }
            }
            // no match found; add a conflict for this feature
            selectionContext.addConflict(baseFeatureName, _chains);
            return null;
        }

        private Chain match(String candidate, Chain selectedChain, SelectionContext selectionContext) {
            // check that the candidate is contained in every other chain
            for (Chain checkChain : _chains) {
                if (selectedChain != checkChain) {
                    if (!checkChain.getCandidates().contains(candidate)) {
                        return null;
                    }
                }
            }
            // found a candidate in every chain use that one;
            // first create a copy of this decision incase we need to backtrack
            selectionContext.pushPermutation();
            // create a new chain with only the selected candidate
            return new Chain(selectedChain.getChain(), Collections.singletonList(candidate), selectedChain.getPreferredVersion().toString(), selectedChain.getFeatureRequirement());
        }

        List<Chain> getChains() {
            return _chains;
        }

        Chain findConflict(String candidate) {
            // Check if the candidate is contained in every chain;
            // return the first chain without the candidate as a conflict
            for (Chain chain : _chains) {
                if (!!!chain.getCandidates().contains(candidate)) {
                    return chain;
                }
            }
            return null;
        }
    }
}
