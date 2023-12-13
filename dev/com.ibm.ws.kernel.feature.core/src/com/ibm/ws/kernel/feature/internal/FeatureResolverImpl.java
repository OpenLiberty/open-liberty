/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;

/**
 * Feature resolution engine.
 *
 * Two modes are supported: Feature resolution as a part of server startup, and
 * feature resolution for feature installation.
 *
 * Feature resolution operates on a pre-specified list of features (requested
 * features).  Resolution of these features means locating dependency features
 * which are needed by the requested features, and means locating auto-features
 * which are satisfied by the requested and located dependency features.  These two
 * steps are intermixed, with new requested and dependency features possibly
 * determining new auto-features, and with new auto-features possibly determining
 * new dependency features.
 *
 * When locating dependency features and auto-features, feature selection selects
 * particular feature versions.  Generally, when selecting multiple features and
 * feature versions, only particular combinations of feature versions will be
 * compatible with each other.  The consequence is that feature resolution must
 * perform two types of searches: First, feature resolution must walk the graph
 * of feature dependencies.  Second, feature resolution must process feature version
 * combinations.
 *
 * The resolver implementation uses backtracking to handle the two types of
 * searches.  When selecting a feature which has multiple versions, the several
 * possible versions of the feature are tried while growing a stack of the
 * current features and feature versions.  If a conflict is detected the
 * feature version is discarded, and the next candidate feature version is
 * attempted.
 *
 * The order in which dependency features are located impacts what feature versions
 * are selected.  For maximal consistency, features resolution is strictly ordered:
 * Particular feature versions are given priority by the order in which they
 * are processed, which is determined by the order in which version compatibility
 * is specified in feature definitions.  This has two consequences: First,
 * feature definitions must be carefully expressed to so put preferred versions
 * earlier in the feature definition.  Second, feature resolution must preserve
 * and use the version ordering when selecting feature versions.
 *
 * The selection algorithm is computational expensive, with known performance
 * problems when many feature versions are available.  Poor performance is
 * particularly a problem when processing versionless features.
 */
public class FeatureResolverImpl implements FeatureResolver {
    // TODO: This strange initialization of 'tc' seems to be to
    //       avoid interacting with trace injection.  Why is this necessary?

    private static final Object tc;

    static {
        Object temp;
        try {
            temp = Tr.register(FeatureResolverImpl.class,
                               com.ibm.ws.kernel.feature.internal.ProvisionerConstants.TR_GROUP,
                               com.ibm.ws.kernel.feature.internal.ProvisionerConstants.NLS_PROPS);
        } catch (Throwable t) {
            temp = null;
        }
        tc = temp;
    }

    @Trivial
    protected static final void info(String method, String message, Object... parms) {
        Tr.info((TraceComponent) tc, "FeatureResolver." + method + ": " + message, parms);
    }

    @Trivial
    protected static final void trace(String message, Object... parms) {
        if ( isTraceEnabled() ) {
            rawTrace(message, parms);
        }
    }

    @Trivial
    protected static final boolean isTraceEnabled() {
        if ( tc == null ) {
            return false;
        }
        if ( !TraceComponent.isAnyTracingEnabled() ) {
            return false;
        }
        return ( ((TraceComponent) tc).isDebugEnabled() );
    }

    @Trivial
    protected static final void rawTrace(String message, Object... parms) {
        Tr.debug((TraceComponent) tc, message, parms);
    }

    @Trivial
    protected static final void error(String message, Object... parms) {
        Tr.error((TraceComponent) tc, message, parms);
    }

    protected static final boolean isBeta = Boolean.valueOf(System.getProperty("com.ibm.ws.beta.edition"));

    static {
        if ( isBeta ) {
            trace("Beta mode detected: Versionless feature support enabled.");
        }
    }

    //

    private static final String COLLECT_TIMING_PROPERTY_NAME = "feature.resolver.timing";
    private static final String collectTimingPropertyValue = System.getProperty(COLLECT_TIMING_PROPERTY_NAME);
    protected static final boolean collectTiming = ( (collectTimingPropertyValue == null) ? false : Boolean.parseBoolean(collectTimingPropertyValue) );

    static {
        if ( collectTiming ) {
            info("<static>", "Timing data requested");
        }
    }

    protected static long updateTime(long lastTimeNs, String method, String message) {
        long nextTimeNs = System.nanoTime();

        long deltaNs = nextTimeNs - lastTimeNs;
        String deltaNsText = String.format("%8d", Long.valueOf(deltaNs));
        info(method, "[ " + deltaNsText + " ] " + message);

        return nextTimeNs;
    }

    // Cache parsed versions and name-version pairs.
    //
    // TODO: These caches should be stored in the feature repository.

    /**
     * Cache of parsed versions.  Null is stored for any version value
     * which fails to parse.
     */
    private static Map<String, Version> parseV = new HashMap<>();

    public static Version parseVersion(String feature, String versionText) {
        return parseV.computeIfAbsent(versionText,
                                      (String useVersionText) -> rawParseVersion(feature, useVersionText));
    }

    public static Version parseVersion(String versionText) {
        return parseVersion(null, versionText);
    }

    public static Version parseVersion(String versionText, Version defaultVersion) {
        Version parsedVersion = parseVersion(versionText);
        return ( (parsedVersion == null) ? defaultVersion : parsedVersion );
    }

    @FFDCIgnore(IllegalArgumentException.class)
    public static Version rawParseVersion(String feature, String versionText) {
        try {
            return Version.parseVersion(versionText);
        } catch (IllegalArgumentException e) {
            if (feature == null) {
                // TODO: E: Encountered non-valid preferred version "{0}".
                error("FEATURE_VERSION_NOT_VALID", versionText);
            } else {
                // TODO: E: Feature dependency "{0}" has non-valid version "{1}".
                error("FEATURE_VERSION_NOT_VALID", feature, versionText);
            }
            return null;
        }
    }

    /**
     * Cache of parsed features.  Keys are the feature base symbolic
     * name plus version.  Values are pairs of the base symbolic
     * and the version substrings.
     *
     * Null is never stored: A feature which has no version substring
     * is stored as the pair of the feature and null. A feature which has
     * a version which fails to parse is stored as the feature and null,
     * where the feature includes the unparsable version substring.  A
     * feature which has a version substring which parses is stored as
     * the base symbolic name substring and the version substring.
     *
     * Processing of the version substring uses {@link Version#parseVersion(String)},
     * using the {@link #parseVersion()} API, which caches the parsed version.
     */
    private static Map<String, String[]> parseNAV = new HashMap<>();

    public static String[] parseNameAndVersion(String feature) {
        return parseNAV.computeIfAbsent(feature, FeatureResolverImpl::rawParseNameAndVersion);
    }

    /**
     * Parse a full feature symbolic name, splitting out the base symbolic name
     * and version.
     *
     * The full name is expected to contain a base name, followed by a dash ('-')
     * followed by a version.  Answer the pair of the base name and the version.
     *
     * Answer the feature plus null if no dash is located.
     *
     * Answer the feature plus null if the version fails to parse.
     * (See @link {@link #parseVersion(String)}.
     *
     * Otherwise, answer the base name and the version.
     *
     * Use of {@link #parseVersion(String)} means that as a side effect, the parsed
     * {@link Version} is cached.
     *
     * @param feature A full feature symbolic name, including a base name and
     *     a version.
     * @return The pair of the base symbolic name and version substrings.
     */
    public static String[] rawParseNameAndVersion(String feature) {
        int lastDash = feature.lastIndexOf('-');
        if (lastDash == -1) {
            return new String[] { feature, null };
        }

        String versionText = feature.substring(lastDash + 1);

        Version version = parseVersion(feature, versionText);
        if ( version == null ) {
            return new String[] { feature, null };
        }

        String baseName = feature.substring(0, lastDash);
        return new String[] { baseName, versionText };
    }

    //

    private static final String PREFERRED_VERSIONS_PROPERTY_NAME = "PREFERRED_FEATURE_VERSIONS";
    private static final String preferredFeatureVersions = System.getProperty(PREFERRED_VERSIONS_PROPERTY_NAME);
    protected static final String[][] parsedPreferredVersions;

    static {
        String[] preferredVersions =
            ((preferredFeatureVersions == null) ? new String[] {}
                                                : preferredFeatureVersions.split(","));

        String[][] parsedVersions = new String[preferredVersions.length][];
        for (int i = 0; i < preferredVersions.length; i++) {
            parsedVersions[i] = parseNameAndVersion(preferredVersions[i]);
        }
        parsedPreferredVersions = parsedVersions;

        if ( isTraceEnabled() ) {
            if ( preferredFeatureVersions == null ) {
                rawTrace("No preferred feature versions were specified");
            } else {
                rawTrace("Preferred feature versions [ " + PREFERRED_VERSIONS_PROPERTY_NAME + " ]:" +
                         " [ " + preferredFeatureVersions + " ]");
                for (int i = 0; i < preferredVersions.length; i++) {
                    rawTrace("Base [ " + parsedVersions[i][0] + " ] Version [ " + parsedVersions[i][1]);
                }
            }
        }
    }

    /**
     * Process parameter: Used to limit the output to one warning about versionless features.
     */
    private static Set<String> shownVersionlessError;

    protected static void showVersionlessError(String baseName) {
        if ( shownVersionlessError == null ) {
            shownVersionlessError = new HashSet<>(1);
        } else if ( shownVersionlessError.contains(baseName) ) {
            return;
        }

        shownVersionlessError.add(baseName);

        String shortName = baseName.replace("io.openliberty.internal.versionless.", "");

        if (preferredFeatureVersions == null) {
            error("UPDATE_MISSING_VERSIONLESS_ENV_VAR", shortName);
        } else {
            error("UPDATE_MISSING_VERSIONLESS_FEATURE_VAL", shortName);
        }
    }

    //

    /**
     * Tell if a feature is supported by specified process types.
     *
     * See {@link ProvisioningFeatureDefinition#getProcessTypes()}.
     *
     * @param supportedTypes The process types which are supported.
     * @param fd The definition of the feature which is to be tested.
     *
     * @return True or false telling if the feature has a supported process type.
     */
    protected static boolean supportedProcessType(EnumSet<ProcessType> supportedTypes,
                                                  ProvisioningFeatureDefinition fd) {
        // TODO: This should be a method of ProvisioningFeatureDefinition.

        for (ProcessType processType : fd.getProcessTypes()) {
            if (supportedTypes.contains(processType)) {
                return true;
            }
        }
        return false;
    }

    // Core feature resolution API:

    // When no process type is passed we support all process types.
    @Trivial
    @Override
    public Result resolveFeatures(FeatureResolver.Repository repository,
                                  Collection<String> explicitNames,
                                  Set<String> preResolvedNames,
                                  boolean allowMultipleVersions) {

        return resolveFeatures(repository,
                               Collections.emptySet(), explicitNames, preResolvedNames,
                               (allowMultipleVersions ? Collections.<String> emptySet() : null),
                               EnumSet.allOf(ProcessType.class));
    }

    // When no process type is passed we support all process types.
    @Trivial
    @Override
    public Result resolveFeatures(FeatureResolver.Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelDefs,
                                  Collection<String> explicitNames,
                                  Set<String> preResolvedNames,
                                  boolean allowMultipleVersions) {

        return resolveFeatures(repository,
                               kernelDefs, explicitNames, preResolvedNames,
                               (allowMultipleVersions ? Collections.<String> emptySet() : null),
                               EnumSet.allOf(ProcessType.class));
    }

    @Trivial
    @Override
    public Result resolveFeatures(Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelDefs,
                                  Collection<String> explicitNames,
                                  Set<String> preResolvedNames,
                                  boolean allowMultipleVersions,
                                  EnumSet<ProcessType> supportedProcessTypes) {

        return resolveFeatures(repository,
                               kernelDefs, explicitNames, preResolvedNames,
                               (allowMultipleVersions ? Collections.<String> emptySet() : null),
                               supportedProcessTypes);
    }

    /**
     * Core feature resolution API: Resolve features relative to a feature repository.
     *
     * Two control parameters may be specified: Whether multiple versions may be
     * specified, and what to what process types to limit feature resolution.
     *
     * Feature resolution starts with a predetermined collection of kernel features
     * and a collection of requested features.  The kernel features are usually
     * determined by a bootstrapping step which has a pre-coded collection of
     * hidden, required, kernel features.  The requested features are, when resolution
     * is for server startup, as specified in the server configuration, or, when
     * installing features, as specified to the installation command.
     *
     * Optionally, a pre-resolved collection of feature may be specified.  This is
     * to enable caching of the resolution results.
     *
     * Resolution is performed in two steps, which are iterated as needed until
     * no new features are located.  The first step is to locate dependency features
     * of the kernel and requested features.  The second step is to locate
     * auto-features which are enabled by the kernel, requested, and dependency
     * features.  These steps must be iterated, since new kernel, requested, and
     * dependency features may locate new auto-features, and new auto-features
     * may locate new dependency features.
     *
     * @param repository The feature repository relative to which the resolution
     *     is performed.
     * @param kernelDefs Kernel features which are to be resolved.
     * @param explicitNames Names of the features which are to be resolved.
     * @param preResolvedNames Option pre-resolved features, obtained from a prior
     *     resolution of the kernel and requested features.
     * @param allowedMultipleVersions Multiple versions specification.  Used
     *     when performing feature resolution for feature installation.
     * @param supportedProcessTypes What process types to limit selected features.
     */
    @Override
    public Result resolveFeatures(Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelDefs,
                                  Collection<String> explicitNames,
                                  Set<String> preResolvedNames,
                                  Set<String> allowedMultipleVersions,
                                  EnumSet<ProcessType> supportedProcessTypes) {

        String methodName = "resolveFeatures";

        if ( collectTiming ) {
            info(methodName, "Kernel features:");
            for ( ProvisioningFeatureDefinition kernelDef : kernelDefs ) {
                info(methodName, "  " + kernelDef.getSymbolicName());
            }
            info(methodName, "Explicit features:");
            for ( String name : explicitNames ) {
                info(methodName, "  " + name);
            }
        }

        long resolveStartNs = (collectTiming ? System.nanoTime() : -1L);

        SelectionContext selectionContext =
            new SelectionContext(repository, allowedMultipleVersions, supportedProcessTypes);

        selectionContext.resolveFeatures(kernelDefs, explicitNames, preResolvedNames);

        Result result = selectionContext.getResult();

        @SuppressWarnings("unused")
        long resolveEndNs = ( collectTiming ? updateTime(resolveStartNs, methodName, "Resolved") : -1L );

        if ( collectTiming ) {
            info(methodName, "Total permutations: " + selectionContext.getTotalPermutations());
            logResult(result);
        }

        return result;
    }

    protected void logResult(Result result) {
        String methodName = "logResult";

        info(methodName, "Resolved features:");
        for ( String feature : result.getResolvedFeatures() ) {
            info(methodName, "  " + feature);
        }
        info(methodName, "Missing features:");
        for ( String missing : result.getMissing() ) {
            info(methodName, "  " + missing);
        }
        info(methodName, "Non-public features:");
        for ( String nonPublic : result.getNonPublicRoots() ) {
            info(methodName, "  " + nonPublic);
        }
        info(methodName, "Unsupported features:");
        for ( String feature : result.getWrongProcessTypes().keySet() ) {
            info(methodName, "  " + feature);
        }
        info(methodName, "Conflicted features:");
        for ( String feature : result.getConflicts().keySet() ) {
            info(methodName, "  " + feature);
        }
    }

    /*
     * The selection context maintains the state of the resolve operation.
     * It records the selected candidates, the postponed decisions and
     * any blocked features. It also keeps a stack of permutations
     * that can be used to backtrack earlier decisions.
     */
    static class SelectionContext {
        private final FeatureResolver.Repository _repository;
        private final Set<String> _allowedMultipleVersions;
        private final boolean _allowMultipleVersions;
        private final EnumSet<ProcessType> _supportedProcessTypes;

        private final Deque<Permutation> _permutations;
        private int _totalPermutations;
        private Permutation _current;

        private final AtomicInteger _initialBlockedCount;
        private final Map<String, Collection<Chain>> _preResolvedConflicts;
        private final Map<String, List<String>> _versionless;

        SelectionContext(FeatureResolver.Repository repository,
                         Set<String> allowedMultipleVersions,
                         EnumSet<ProcessType> supportedProcessTypes) {

            this._repository = repository;
            this._allowedMultipleVersions = allowedMultipleVersions;
            this._allowMultipleVersions = ( (allowedMultipleVersions != null) && _allowedMultipleVersions.isEmpty() );
            this._supportedProcessTypes = supportedProcessTypes;

            this._permutations = new ArrayDeque<Permutation>(1);
            this._permutations.add( this._current = new Permutation() );

            this._initialBlockedCount = new AtomicInteger(-1);
            this._preResolvedConflicts = new HashMap<String, Collection<Chain>>();

            this._versionless = new HashMap<>();
        }

        // Base context ...

        @Trivial
        FeatureResolver.Repository getRepository() {
            return _repository;
        }

        Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
            return _repository.getAutoFeatures();
        }

        ProvisioningFeatureDefinition getFeature(String symbolicName) {
            return _repository.getFeature(symbolicName);
        }

        @Trivial
        EnumSet<ProcessType> getSupportedProcessTypes() {
            return _supportedProcessTypes;
        }

        boolean allowMultipleVersions(String baseName) {
            return ( (_allowedMultipleVersions != null) &&
                     ( _allowedMultipleVersions.isEmpty() ||
                       _allowedMultipleVersions.contains(baseName)) );
        }

        // Current permutations access ...

        @Trivial
        Permutation getCurrent() {
            return _current;
        }

        boolean isBlocked(String baseName) {
            return _current._blockedFeatures.contains(baseName);
        }

        int getBlockedCount() {
            return _current._blockedFeatures.size();
        }

        @Trivial
        Map<String, Collection<Chain>> getConflicts() {
            return _current._result.getConflicts();
        }

        @Trivial
        ResultImpl getResult() {
            return _current._result;
        }

        void addMissing(String featureName) {
            _current._result.addMissing(featureName);
        }

        void addNonPublicRoot(String featureName) {
            _current._result.addNonPublicRoot(featureName);
        }

        void addConflict(String baseName, List<Chain> conflicts) {
            _current._blockedFeatures.add(baseName);
            _current._result.addConflict(baseName, conflicts);
        }

        @Trivial
        void addConflict(String baseName, Chain chain0, Chain chain1) {
            addConflict(baseName, asList(chain0, chain1));
        }

        @Trivial
        private static List<Chain> asList(Chain chain0, Chain chain1) {
            List<Chain> result = new ArrayList<Chain>(2);
            result.add(chain0);
            result.add(chain1);
            return result;
        }

        boolean hasConflicts() {
            return _current._result.getConflicts().isEmpty();
        }

        void addWrongProcessType(String symbolicName, Chain chain) {
            _current._result.addWrongProcessType(symbolicName, chain);
        }

        void addWrongProcessType(String symbolicName) {
            String[] nameAndVersion = parseNameAndVersion(symbolicName);
            String preferredVersion = nameAndVersion[1];

            addWrongProcessType(symbolicName,
                                new ChainImpl(symbolicName, preferredVersion));
        }

        Set<String> getResolvedFeatures() {
            return _current._result.getResolvedFeatures();
        }

        void setResolvedFeatures(Collection<String> resolvedFeatures) {
            _current._result.setResolvedFeatures(resolvedFeatures);
        }

        // Permutations access ...

        void restoreBestSolution() {
            if ( _permutations.size() > 1 ) {
                _current = _permutations.getLast();
                _permutations.clear();
                _permutations.add(_current);
            }
        }

        void selectCurrentPermutation() {
            _permutations.clear();
            _permutations.addFirst(_current);
        }

        void checkForBestSolution() {
            // check if the current best (store as the last permutation) has more conflicts
            // than the current solution.
            if (_permutations.getLast().getNumConflicts() > _current.getNumConflicts() ) {
                // Replace the current best (stored as the last permutation)
                _permutations.pollLast();
                _permutations.addLast(_current);
            }
        }

        /**
         * Pop a permutation, but only if there are two or more permutations.
         * (The first permutation is never popped.)
         *
         * If a permutation was popped, update the current permutation to the
         * one which was popped.
         *
         * @return True or false telling if a permutation was popped.
         */
        boolean popPermutation() {
            if ( _permutations.size() < 2 ) {
                return false;
            } else {
                _current = _permutations.pollFirst();
                return true;
            }
        }

        /**
         * Attempt to push a permutation.  Do so only if the current permutation
         * does not add more conflicts than the initial permutation.  Do so only
         * if none of the chains of the current permutation is exhausted (has tried
         * all of its candidates).
         *
         * Strangely, the current permutation is not updated.
         */
        void pushPermutation() {
            if ( _initialBlockedCount.get() != getBlockedCount() ) {
                return;
            } else if ( _current.exhaustedAny() ) {
                return;
            } else {
                _permutations.addFirst( _current.copy(_preResolvedConflicts) );
                _totalPermutations++;
            }
        }

        void saveCurrentPreResolvedConflicts() {
            _preResolvedConflicts.clear();
            _preResolvedConflicts.putAll(getConflicts());
        }

        @Trivial
        int getTotalPermutations() {
            return _totalPermutations;
        }

        // Conflict counting ...

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

        //

        Chain getSelected(String baseName) {
            return _current._selected.get(baseName);
        }

        void processCandidates(Collection<String> chain,
                               List<String> candidateNames,
                               String symbolicName, String baseName, String preferredVersion,
                               boolean isSingleton) {

            Iterator<String> iCandidateNames = candidateNames.iterator();
            while ( iCandidateNames.hasNext() ) {
                String candidateName = iCandidateNames.next();
                ProvisioningFeatureDefinition candidateDef = getFeature(candidateName);
                if ( !supportedProcessType(_supportedProcessTypes, candidateDef) ) {
                    addWrongProcessType(symbolicName,
                                        new ChainImpl(chain, candidateNames, preferredVersion, symbolicName));
                    iCandidateNames.remove();
                }
            }

            if ( candidateNames.isEmpty() ) {
                addMissing(symbolicName);
                return;
            }

            if ( !isSingleton || allowMultipleVersions(baseName) ) {
                return; // must allow all candidates
            }

            // make a copy for the chain if there is a conflict
            List<String> copyCandidates = new ArrayList<String>(candidateNames);
            // check if the base symbolic name is already selected and different than the candidates
            Chain selectedChain = getSelected(baseName);
            if (selectedChain != null) {
                // keep only the selected candidates (it will be only one)
                candidateNames.retainAll(selectedChain.getCandidates());
                if (candidateNames.isEmpty()) {
                    addConflict(baseName, selectedChain,
                                new ChainImpl(chain, copyCandidates, preferredVersion, symbolicName));
                    return;
                }
            }
            if (candidateNames.size() > 1) {
                // if the candidates are more than one then postpone the decision
                addPostponed(baseName,
                             new ChainImpl(chain, candidateNames, preferredVersion, symbolicName));
                return;
            }

            // must select this one
            String selectedName = candidateNames.get(0);
            // check if there is a postponed decision
            Chain conflict = getPostponedConflict(baseName, selectedName);
            if (conflict != null) {
                addConflict(baseName, conflict, new ChainImpl(chain, copyCandidates, preferredVersion, symbolicName));
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
                _current._selected.put(baseName, new ChainImpl(chain, selectedName, preferredVersion, symbolicName));
            }

            // if there was a postponed decision remove it
            _current._postponed.remove(baseName);
        }

        /**
         * Process the first postponed feature.
         *
         * Only the first postponed feature can be processed: Processing
         * of that feature invalidates the remaining postponed features.
         */
        void processPostponed() {
            if ( _current._postponed.isEmpty() ) {
                return;
            }

            Map.Entry<String, Chains> firstPostponedEntry = _current._postponed.entrySet().iterator().next();
            String firstBaseName = firstPostponedEntry.getKey();
            Chains firstChains = firstPostponedEntry.getValue();

            Chain selectedChain = firstChains.select(firstBaseName);
            if (selectedChain != null) {
                pushPermutation();
                _current._selected.put(firstBaseName, selectedChain);
            } else {
                addConflict(firstBaseName, firstChains.getChains());
            }

            // Remove all postponed (not just the first).  Processing
            // the first postponed invalidates the remaining postponed.
            // TODO: Is this true if the first postponed is not used??
            _current._postponed.clear();
        }

        /**
         * Set the selected features.  Remove conflicts.
         *
         * There may be two versions of the same feature: "servlet-3.0" and "servlet-3.1".
         * Do not select either, and record a conflict on the base name: "servlet".
         */
        void primeSelected(Collection<String> featureNames) {
            if ( _allowMultipleVersions ) {
                return; // No selection is needed when allowing multiple versions.
            }

            Map<String, String> conflicts = new HashMap<>();

            Iterator<String> iFeatures = featureNames.iterator();
            while ( iFeatures.hasNext() ) {
                String featureName = iFeatures.next();

                ProvisioningFeatureDefinition featureDef = getFeature(featureName);
                if ( featureDef == null ) {
                    continue; // TODO: This should not be possible.
                } else if ( !featureDef.isSingleton() ) {
                    continue; // No conflicts if the feature is not a singleton.
                }

                String featureSymbolicName = featureDef.getSymbolicName();
                String[] nameAndVersion = parseNameAndVersion(featureSymbolicName);
                String baseName = nameAndVersion[0];
                String preferredVersion = nameAndVersion[1];

                if ( allowMultipleVersions(baseName) ) {
                    continue;
                }

                Chain selectedChain = _current._selected.get(baseName);
                if ( selectedChain == null ) {
                    _current._selected.put(baseName, new ChainImpl(featureSymbolicName, preferredVersion));

                } else {
                    iFeatures.remove(); // Keep only the first selection.

                    // TODO Need to revisit why this is not always a conflict.
                    // check if the selected feature is contained in the features collection;
                    // if so then it is a conflict also and we need to clean it up and block it.

                    String selectedFeature = selectedChain.getCandidates().get(0);
                    if ( featureNames.contains(selectedFeature) ) {
                        Chain conflictChain = new ChainImpl(featureSymbolicName, preferredVersion);
                        addConflict(baseName, selectedChain, conflictChain);
                        conflicts.put(selectedFeature, baseName);
                    }
                }
            }

            for ( Map.Entry<String, String> conflict : conflicts.entrySet() ) {
                featureNames.remove( conflict.getKey() );
                _current._selected.remove( conflict.getValue() );
            }
        }

        boolean hasPostponed() {
            return !_current._postponed.isEmpty();
        }

        void addPostponed(String baseName, Chain chain) {
            Chains existing = _current._postponed.computeIfAbsent(baseName, (String useBaseName) -> new Chains());
            existing.add(chain);
        }

        Chain getPostponedConflict(String baseName, String selectedName) {
            Chains postponedChains = _current._postponed.get(baseName);
            return postponedChains == null ? null : postponedChains.findConflict(selectedName);
        }

        // Basic versionless access ...

        void putVersionless(String feature, List<String> tolerates) {
            _versionless.put(feature, tolerates);
        }

        List<String> copyVersionless(String feature) {
            List<String> raw = _versionless.get(feature);
            return raw == null ? null : new ArrayList<>(raw);
        }

        //

        void resolveFeatures(Collection<ProvisioningFeatureDefinition> kernelDefs,
                             Collection<String> requestedNames,
                             Set<String> preResolvedNames) {

            String methodName = "resolveFeatures";

            // Filter the pre-resolved features: If any no longer exists in the repository,
            // clear them and start from scratch.  Otherwise, change the pre-resolved feature
            // names to feature symbolic names.  The pre-resolved features are used
            // as the initial resolved symbolic names.

            Set<String> resolvedSymbolicNames = checkPreResolved(preResolvedNames);

            // Filter root features: Remove any which are missing from the repository,
            // which are not public, which have an unsupported process type, or which
            // are already resolved.  Answer the symbolic names of the remaining features.
            // The requested symbolic names are used as the first new resolved symbolic
            // names.

            Collection<String> newResolvedSymbolicNames = checkRequested(requestedNames, resolvedSymbolicNames);

            //

            primeSelected(resolvedSymbolicNames);

            Set<String> satisfiedSymbolicNames = new HashSet<>( kernelDefs.size() );
            Set<ProvisioningFeatureDefinition> capabilityDefs = new HashSet<>(kernelDefs);
            for ( String resolvedSymbolicName : resolvedSymbolicNames ) {
                satisfiedSymbolicNames.add(resolvedSymbolicName);
                capabilityDefs.add( getFeature(resolvedSymbolicName) );
            }

            // While there are new resolved features:
            //
            // 1) Resolve dependency features for those new resolved features.
            //    Add the new dependency features to the resolved features.
            // 2) Select auto-features for all resolved features.  Newly
            //    selected auto-features force a loop, with the new auto-features
            //    being the new resolved features for the next loop.

            boolean firstPass = true;

            while ( !newResolvedSymbolicNames.isEmpty() ) {
                primeSelected(newResolvedSymbolicNames);
                for ( String newResolvedSymbolicName : resolvedSymbolicNames ) {
                    satisfiedSymbolicNames.add(newResolvedSymbolicName);
                    capabilityDefs.add( getFeature(newResolvedSymbolicName) );
                }

                if ( firstPass ) {
                    firstPass = false;
                } else {
                    saveCurrentPreResolvedConflicts();
                }

                long loopStartNs = ( collectTiming ? System.nanoTime() : -1L );

                resolvedSymbolicNames = resolveFeatures(newResolvedSymbolicNames, resolvedSymbolicNames);

                long dependencyEndNs = ( collectTiming ? updateTime(loopStartNs, methodName, "Select dependency features") : -1L );

                newResolvedSymbolicNames = selectAutoFeatures(satisfiedSymbolicNames,
                                                              capabilityDefs, resolvedSymbolicNames);

                @SuppressWarnings("unused")
                long autoEndNs = ( collectTiming ? updateTime(dependencyEndNs, methodName, "Select auto features") : -1L );
            }
        }

        /*
         * Verify that all pre-resolved features are still available in the
         * feature repository.  If any feature is not available, answer an empty
         * collection.
         *
         * Otherwise, answer the symbolic names of the pre-resolved features.
         *
         * @param preResolved Pre-resolved features list.  Usually obtained from
         *     a prior feature resolution.
         * @param selectionContect Feature resolution context.  Provides access to
         *     the feature repository.
         *
         * @return The symbolic names of the pre-resolved features.  An empty collection
         *     if any of the pre-resolved features is not found in the feature repository.
         */
        private Set<String> checkPreResolved(Set<String> preResolvedFeatureNames) {
            String methodName = "checkPreResolved";

            boolean doTrace = FeatureResolverImpl.isTraceEnabled();

            Set<String> symbolicNames = new LinkedHashSet<>(preResolvedFeatureNames.size());

            for ( String featureName : preResolvedFeatureNames ) {
                ProvisioningFeatureDefinition featureDef = getFeature(featureName);
                if (featureDef == null) {
                    if ( doTrace ) {
                        rawTrace(methodName, "Pre-resolved feature [ " + featureName + " ] is no longer available");
                    }
                    symbolicNames = null;
                } else {
                    if ( symbolicNames != null ) {
                        String symbolicName = featureDef.getSymbolicName();
                        if ( doTrace ) {
                            rawTrace(methodName, "Pre-resolved feature [ " + featureName + " ] processed as [ " + symbolicName + " ]");
                        }
                        symbolicNames.add(symbolicName);
                    }
                }
            }

            return ( (symbolicNames == null) ? Collections.emptySet() : symbolicNames );
        }

        /**
         * Answer the symbolic names of the usable requested features.
         *
         * A requested feature is usable if and only if the feature is found
         * in the feature repository, has public visibility, has a supported
         * process type, and is not already resolved.
         *
         * @param requestedNames The names of features which are to be examined.
         * @param preResolvedNames Pre-resolved features.
         *
         * @return A list of usable requested feature symbolic names.
         */
        private List<String> checkRequested(Collection<String> requestedNames, Set<String> preResolvedNames) {
            List<String> requestedSymbolicNames = new ArrayList<String>(requestedNames.size());

            for ( String requestedName : requestedNames ) {
                ProvisioningFeatureDefinition requestedDef = getFeature(requestedName);
                if ( requestedDef == null ) {
                    addMissing(requestedName);

                } else {
                    String requestedSymbolicName = requestedDef.getSymbolicName();

                    if ( requestedDef.getVisibility() != Visibility.PUBLIC ) {
                        addNonPublicRoot(requestedName);

                    } else if ( !supportedProcessType(getSupportedProcessTypes(), requestedDef) ) {
                        addWrongProcessType(requestedSymbolicName);

                    } else if ( !preResolvedNames.contains(requestedSymbolicName) ) {
                        requestedSymbolicNames.add(requestedSymbolicName);
                    }
                }
            }

            return requestedSymbolicNames;
        }

        /*
         * There are two phases to resolving features.
         * 1) Optimistically process all multiple candidate features to the end picking the most preferred features
         * 2) If this is successful then return the result with no conficts
         * 3) If conflicts were found then backtrack over the permutations and choose different candidates. Note this may create more permutations
         * 4) if a permutation is found that has not conflicts return the results
         * 5) Otherwise use the first permutation and report the original conflicts
         */
        private Set<String> resolveFeatures(Collection<String> newFeatures, Set<String> preResolved) {

            resetInitialBlockedCount();

            // First, process the new features, selecting candidates when multiple versions are available.

            // Optimistically, the first step will have no conflicts, in which case the
            // resolution is complete.

            Set<String> resolvedNames = processCurrentPermutation(newFeatures, preResolved);

            if ( getConflicts().isEmpty() ) {
                selectCurrentPermutation();
                return resolvedNames;
            }

            // More commonly, there will be conflicts.  Process permutations until
            // there are no more conflicts than we started with, or until no more
            // permutations are available.

            while ( currentHasMoreThanInitialBlockedCount() && popPermutation() ) {
                resolvedNames = processCurrentPermutation(newFeatures, preResolved);
            }

            restoreBestSolution();

            // Copy the resolved features.
            return new LinkedHashSet<>( getResolvedFeatures() );
        }

        Set<String> processCurrentPermutation(Collection<String> newResolved, Set<String> resolved) {

            // The number of blocked is checked each time we process a postponed decision.
            // A check is done each time we process the roots after doing a postponed decision
            // to see if more features got blocked.  If more got blocked then we
            // re-process the roots again.
            // This is necessary to ensure the final result does not include one of the blocked features

            Set<String> resolvedNames;

            int numBlocked;
            do {
                processPostponed();
                numBlocked = getBlockedCount();
                resolvedNames = processResolved(newResolved, resolved);
            } while ( hasPostponed() || (numBlocked != getBlockedCount()) );

            setResolvedFeatures(resolvedNames);
            checkForBestSolution();

            return resolvedNames;
        }

        private Set<String> processResolved(Collection<String> newResolvedSymbolicNames,
                                            Set<String> resolvedSymbolicNames) {

            Deque<String> chain = new ArrayDeque<>();
            // Always prime the result with the preResolved.
            // Using a ordered set to keep behavior of bundle order the same as before
            // TODO we should not have to do this, but it appears auto-features are pretty sensitive to being installed last

            Set<String> resolvedNames = new LinkedHashSet<>(resolvedSymbolicNames.size());

            // Prime the results with the pre-resolved; make sure to use the feature names.
            for (String resolvedSymbolicName : resolvedSymbolicNames) {
                ProvisioningFeatureDefinition resolvedDef = getFeature(resolvedSymbolicName);
                resolvedNames.add( resolvedDef.getFeatureName() );
            }

            for ( String resolvedSymbolicName : newResolvedSymbolicNames ) {
                ProvisioningFeatureDefinition resolvedDef = getFeature(resolvedSymbolicName);
                if ( resolvedDef == null ) {
                    addMissing(resolvedSymbolicName);
                } else {
                    processSelected(resolvedDef, null, chain, resolvedNames);
                }
            }

            // Note that this only saves the blocked count on the first call during doResolveFeatures;
            // Any conflicts here will be due to hard failures with no alternative toleration choices.
            // In other words, it is the best conflict count we will ever achieve.
            setInitialRootBlockedCount();
            return resolvedNames;
        }

        private void processSelected(ProvisioningFeatureDefinition featureDef,
                                     Set<String> allowedTolerations,
                                     Deque<String> chain,
                                     Set<String> resolvedNames) {

            // first check if the feature is blocked as already in conflict
            String featureSymbolicName = featureDef.getSymbolicName();

            String baseName = parseNameAndVersion(featureSymbolicName)[0];
            if ( isBlocked(baseName) ) {
                return;
            }

            // Validation to make sure this feature is selected; this is really just to check bugs in the resolver
            if (featureDef.isSingleton() && !allowMultipleVersions(baseName)) {
                Chain existingSelection = getSelected(baseName);
                String selectedFeatureName = existingSelection == null ? null : existingSelection.getCandidates().get(0);
                if (existingSelection == null || !featureSymbolicName.equals(selectedFeatureName)) {
                    throw new IllegalStateException("Expected feature \"" + featureSymbolicName + "\" to be selected instead feature of \"" + selectedFeatureName);
                }
            }

            if (chain.contains(featureSymbolicName)) {
                return; // must be in a cycle
            }

            chain.addLast(featureSymbolicName);
            try {
                // Depth-first: process any included features first.
                // Postpone decisions on variable candidates until after the first pass
                Collection<FeatureResource> includes = featureDef.getConstituents(SubsystemContentType.FEATURE_TYPE);

                boolean isRoot = chain.size() == 1;
                // do a first pass to get all the base feature names that are included
                Set<String> includedBaseNames = new HashSet<>();
                for (FeatureResource included : includes) {
                    String includedSymbolicName = included.getSymbolicName();
                    if (includedSymbolicName != null) {
                        String[] nameAndVersion = parseNameAndVersion(includedSymbolicName);
                        String includedBaseName = nameAndVersion[0];
                        includedBaseNames.add(includedBaseName);
                    }
                }
                if (allowedTolerations == null) {
                    // if allowTolerations is null then use the whole includedBaseFeatureNames, this is a root feature; but lets make sure
                    if (!isRoot) {
                        throw new IllegalStateException("A null allowTolerations is only valid for root features.");
                    }
                } else {
                    // otherwise we need to take the intersection from the parent's toleration with the ones we include directly here
                    includedBaseNames.retainAll(allowedTolerations);
                }
                allowedTolerations = includedBaseNames;
                for (FeatureResource included : includes) {
                    processIncluded(featureDef, included, allowedTolerations, chain, resolvedNames);
                }
            } finally {
                chain.removeLast();
                // NOTE 1: We always add the feature to the results because if we are processing the feature
                // then we know it is the selected one; see check above
                // NOTE 2: We add after processing included so that we get a the same order as previous feature manager
                // (TODO we really should not need to do this!! but things seem to really depend on install order!!)
                // NOTE 3: it is very important that the result includes the full feature name, not just the symbolic name
                // this ensures we include the product extension prefix if it exists.
                resolvedNames.add(featureDef.getFeatureName());
            }
        }

        private void processIncluded(ProvisioningFeatureDefinition includingFeature,
                                     FeatureResource included,
                                     Set<String> allowedTolerations,
                                     Deque<String> chain,
                                     Set<String> result) {

            String symbolicName = included.getSymbolicName();
            if (symbolicName == null) {
                // TODO Why report the feature as missing?  A better error message would
                // indicate the FeatureResource requirement has no symbolic name.
                if (!chain.isEmpty()) {
                    addMissing(chain.peekLast());
                }
                return;
            }

            String[] nameAndVersion = parseNameAndVersion(symbolicName);
            String baseName = nameAndVersion[0];
            String preferredVersion = nameAndVersion[1];
            boolean isSingleton = false;

            // if the base name is blocked then we do not continue with this include
            if (isBlocked(baseName)) {
                return;
            }

            // only look in tolerates if the base name is allowed to be tolerated
            List<String> tolerates = included.getTolerates();
            List<String> overrideTolerates = getRepository().getConfiguredTolerates(baseName);
            if (!overrideTolerates.isEmpty()) {
                tolerates = tolerates == null ? new ArrayList<String>() : new ArrayList<String>(tolerates);
                // Note that we do not check for dups here, that is handled while getting the actual candidates below
                tolerates.addAll(overrideTolerates);
            }

            // Handle versionless feature preferences which were
            // specified using environment variable PREFERRED_FEATURE_VERSIONS.
            //
            // PREFERRED_FEATURE_VERSIONS=mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpHealth-5.0,mpHealth-3.1

            if (isBeta) {
                if (baseName.startsWith("io.openliberty.internal.versionless.")) {
                    if (parsedPreferredVersions.length == 0) {
                        preferredVersion = "";
                        tolerates = new ArrayList<String>();
                    } else if ((tolerates = copyVersionless(baseName)) != null) {
                        preferredVersion = tolerates.remove(0);
                        symbolicName = baseName + "-" + preferredVersion;
                    } else {
                        preferredVersion = "";
                        tolerates = new ArrayList<String>();
                        for (String[] nAV : parsedPreferredVersions) {
                            if (baseName.endsWith(nAV[0])) {
                                if (preferredVersion.isEmpty()) {
                                    preferredVersion = nAV[1];
                                    symbolicName = baseName + "-" + nAV[1];
                                } else {
                                    tolerates.add(nAV[1]);
                                }
                            }
                        }
                        List<String> allVersions = new ArrayList<String>(tolerates.size() + 1);
                        allVersions.add(preferredVersion);
                        allVersions.addAll(tolerates);
                        putVersionless(baseName, allVersions);
                    }

                    if (preferredVersion.isEmpty()) {
                        showVersionlessError(baseName);

                        baseName = "";
                        symbolicName = "";
                        return;
                    }
                }
            }

            List<String> candidateNames = new ArrayList<String>(1 + (tolerates == null ? 0 : tolerates.size()));

            // look for the preferred feature using the fully qualified symbolicName first
            ProvisioningFeatureDefinition preferredCandidateDef = getFeature(symbolicName);
            if (preferredCandidateDef != null && isAccessible(includingFeature, preferredCandidateDef)) {
                checkForFullSymbolicName(preferredCandidateDef, symbolicName, chain.getLast());
                isSingleton = preferredCandidateDef.isSingleton();
                candidateNames.add(symbolicName);
            }

            // Check for tolerated versions; but only if the preferred version is a singleton or we did not find the preferred version
            if (tolerates != null && (candidateNames.isEmpty() || isSingleton)) {
                for (String tolerate : tolerates) {
                    if (allowMultipleVersions(baseName)) {
                        // if we are in minify mode (_allowMultipleVersions) then we only want to continue to look for
                        // tolerated versions until we have found one candidate
                        if (!candidateNames.isEmpty()) {
                            break;
                        }
                    }
                    String toleratedSymbolicName = baseName + '-' + tolerate;
                    ProvisioningFeatureDefinition toleratedCandidateDef = getFeature(toleratedSymbolicName);
                    if (toleratedCandidateDef != null && !candidateNames.contains(toleratedCandidateDef.getSymbolicName()) && isAccessible(includingFeature, toleratedCandidateDef)) {
                        checkForFullSymbolicName(toleratedCandidateDef, toleratedSymbolicName, chain.getLast());
                        isSingleton |= toleratedCandidateDef.isSingleton();
                        // Only check against the allowed tolerations if this candidate feature is public or protected (NOT private)
                        if (isAllowedToleration(toleratedCandidateDef, allowedTolerations, overrideTolerates, baseName, tolerate, chain)) {
                            candidateNames.add(toleratedCandidateDef.getSymbolicName());
                        }
                    }
                }
            }

            if (!isSingleton && candidateNames.size() > 1) {
                // If the candidates are not singleton and there are multiple then that means
                // someone is using tolerates for a non-singleton feature (error case?).
                // For now just use the first candidate
                candidateNames.retainAll(Collections.singleton(candidateNames.get(0)));
            }
            processCandidates(chain, candidateNames, symbolicName, baseName, preferredVersion, isSingleton);
            // check if there is a single candidate left after processing
            if (candidateNames.size() == 1) {
                // We selected one candidate; now process the selected
                String selectedName = candidateNames.get(0);
                ProvisioningFeatureDefinition selectedDef = getFeature(selectedName);
                if ( selectedDef != null ) {
                    processSelected(selectedDef, allowedTolerations, chain, result);
                }
            }
        }

        private boolean isAccessible(ProvisioningFeatureDefinition includingFeature,
                                     ProvisioningFeatureDefinition candidateDef) {
            return !candidateDef.getFeatureName().startsWith("io.openliberty.versionless.") &&
                   ( candidateDef.getVisibility() != Visibility.PRIVATE ||
                     includingFeature.getBundleRepositoryType().equals(candidateDef.getBundleRepositoryType()) );
        }

        private boolean isAllowedToleration(ProvisioningFeatureDefinition toleratedCandidateDef,
                                            Set<String> allowedTolerations,
                                            List<String> overrideTolerates,
                                            String baseName, String tolerate, Deque<String> chain) {

            // if in minify mode always allow (_allowMultipleVersions)
            if (allowMultipleVersions(baseName)) {
                return true;
            }
            // all private features tolerations are allowed
            if (Visibility.PRIVATE == toleratedCandidateDef.getVisibility()) {
                return true;
            }
            // if it is part of the allowed tolerates from the dependency chain then allow
            if (allowedTolerations.contains(baseName)) {
                return true;
            }
            // otherwise if it is part of the override list
            if (overrideTolerates.contains(tolerate)) {
                return true;
            }
            if (isBeta) {
                if (chain.peekFirst().startsWith("io.openliberty.versionless.")) {
                    return true;
                }
            }
            return false;
        }

        private void checkForFullSymbolicName(ProvisioningFeatureDefinition candidateDef,
                                              String symbolicName,
                                              String includingFeature) {

            if ( !symbolicName.equals(candidateDef.getSymbolicName()) ) {
                throw new IllegalArgumentException("A feature is not allowed to use short feature names when including other features. "
                                                   + "Detected short name \"" + symbolicName + "\" being used instead of \"" + candidateDef.getSymbolicName()
                                                   + "\" by feature \"" + includingFeature + "\".");
            }
        }

        /**
         * Locate newly satisfied (and supported) auto features.
         *
         * Current capabilities are provided by the kernel features and the
         * resolved features.
         *
         * Test all available auto-features:
         *
         * Skip any which was previously satisfied.  As a side effect, record
         * any newly satisfied features.
         *
         * Answer the newly satisfied auto-features which are supported by the
         * current platforms.
         *
         * @param satisfiedSymbolicNames All satisfied symbolic names,
         *     including those which are not on a supported platform.
         * @param capabilityDefs Definitions of currently resolved features.
         * @param resolvedSymbolicNames The features which have been resolved.
         *
         * @return Newly selected, satisfied, supported, auto-features.
         */
        private Set<String> selectAutoFeatures(Set<String> satisfiedSymbolicNames,
                                               Collection<ProvisioningFeatureDefinition> capabilityDefs,
                                               Set<String> resolvedSymbolicNames) {

            Set<String> newAutoSymbolicNames = new HashSet<>();

            for ( ProvisioningFeatureDefinition autoDef : getAutoFeatures() ) {
                String autoSymbolicName = autoDef.getSymbolicName();

                if ( satisfiedSymbolicNames.contains(autoSymbolicName) ) {
                    continue; // Already detected as satisfied.  Don't process it again.
                } else if ( !autoDef.isCapabilitySatisfied(capabilityDefs) ) {
                    continue; // Not yet satisfied.  Ignore it.
                }

                satisfiedSymbolicNames.add(autoSymbolicName);

                if ( !supportedProcessType(getSupportedProcessTypes(), autoDef)) {
                    continue; // Not supported.  Ignore it.
                }

                newAutoSymbolicNames.add(autoSymbolicName);
            }

            return newAutoSymbolicNames;
        }
    }

    static class Permutation {
        protected final Map<String, Chain> _selected;
        protected final Map<String, Chains> _postponed;
        protected final Set<String> _blockedFeatures;
        protected final ResultImpl _result;

        Permutation() {
            this._selected = new HashMap<String, Chain>();
            this._postponed = new LinkedHashMap<String, Chains>();
            this._blockedFeatures = new HashSet<String>();
            this._result = new ResultImpl();
        }

        Permutation(Permutation other,
                    Map<String, Collection<Chain>> preResolvedConflicts) {

            this._selected = new HashMap<>(other._selected);

            Map<String, Chains> postponed = new LinkedHashMap<>( other._postponed.size() );
            for ( Map.Entry<String, Chains> entry : other._postponed.entrySet() ) {
                postponed.put( entry.getKey(), entry.getValue().copy() );
            }
            this._postponed = postponed;

            this._blockedFeatures = new HashSet<String>(); // Will be recalculated.

            this._result = new ResultImpl(other._result, preResolvedConflicts);
        }

        /**
         * Tell if any of the postponed chains is exhausted (has attempted all of its
         * candidates).
         *
         * @return True or false telling if any of the postponed chains is exhausted.
         */
        public boolean exhaustedAny() {
            for ( Chains chains : _postponed.values() ) {
                if ( chains.exhaustedAny() ) {
                    return true;
                }
            }
            return false;
        }

        public Permutation copy(Map<String, Collection<Chain>> preResolvedConflicts) {
            return new Permutation(this, preResolvedConflicts);
        }

        public int getNumConflicts() {
            return _result.getNumConflicts();
        }
    }

    // Sort by preferred version, numerically lowest to numerically highest.
    public static int compare(Chain chain0, Chain chain1) {
        return chain0.getPreferredVersion().compareTo(chain1.getPreferredVersion());
    }

    /**
     * A dependency chain of feature requirements that lead to
     * a singleton feature and a list of candidates.
     */
    static class ChainImpl implements FeatureResolver.Chain {
        // TODO: Don't understand the use of 'featureReq' as both arguments.
        public ChainImpl(String featureReq, String preferred) {
            this(featureReq, preferred, featureReq);
        }

        public ChainImpl(String candidate, String preferred, String featureReq) {
            this._chain = Collections.<String> emptyList();
            this._candidates = Collections.singletonList(candidate);
            this._preferred = parseVersion(preferred, Version.emptyVersion);
            this._featureReq = featureReq;
        }

        public ChainImpl(Collection<String> chain, String candidate, String preferred, String featureReq) {
            this._chain = chain.isEmpty() ? Collections.<String> emptyList() : new ArrayList<String>(chain);
            this._candidates = Collections.singletonList(candidate);
            this._preferred = parseVersion(preferred, Version.emptyVersion);
            this._featureReq = featureReq;
        }

        /**
         * Create a new chain based on this chain with a single selected candidate.
         */
        @Override
        public Chain select(String candidate) {
            return new ChainImpl( getChain(),
                                  candidate,
                                  getPreferredVersion().toString(),
                                  getFeatureRequirement() );
        }

        /**
         * Creates a dependency chain.
         *
         * @param chain The chain of feature names that have lead to a requirement on a singleton feature
         * @param candidates The tolerated candidates that were found which may satisfy the feature requirement
         * @param preferred The preferred version
         * @param featureReq The full feature name that is required.
         */
        public ChainImpl(Collection<String> chain, List<String> candidates, String preferred, String featureReq) {
            this._chain = chain.isEmpty() ? Collections.<String> emptyList() : new ArrayList<String>(chain);
            this._candidates = candidates;
            this._preferred = parseVersion(preferred, Version.emptyVersion);
            this._featureReq = featureReq;
        }

        private final List<String> _chain;

        @Trivial
        @Override
        public List<String> getChain() {
            return _chain;
        }

        private final List<String> _candidates;

        @Trivial
        @Override
        public List<String> getCandidates() {
            return _candidates;
        }

        @Trivial
        @Override
        public boolean hasCandidate(String candidate) {
            return _candidates.contains(candidate);
        }

        private final Version _preferred;

        @Trivial
        @Override
        public Version getPreferredVersion() {
            return _preferred;
        }

        private final String _featureReq;

        @Trivial
        @Override
        public String getFeatureRequirement() {
            return _featureReq;
        }

        @Trivial
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ROOT->");
            for (String featureName : _chain) {
                builder.append(featureName).append("->");
            }
            builder.append(_candidates);
            builder.append(" ").append(_preferred);
            return builder.toString();
        }
    }

    static class Chains {
        private final Set<String> _attempted;
        private final List<Chain> _chains;

        @Trivial
        public List<Chain> getChains() {
            return _chains;
        }

        protected Chains() {
            this._chains = new ArrayList<Chain>();
            this._attempted = new HashSet<String>();
        }

        protected Chains(Chains other) {
            this._chains = new ArrayList<>(other._chains);
            this._attempted = new HashSet<>(other._attempted);
        }

        protected Chains copy() {
            return new Chains(this);
        }

        protected void add(Chain chain) {
            int insertion = Collections.binarySearch(_chains, chain, FeatureResolverImpl::compare);
            if (insertion < 0) {
                insertion = ( -insertion - 1 );
            } else {
                // make sure we insert in the order we are added when we have the same preferred;
                // we do this by checking each insertion element to see if we are equal
                // until we find one that is not
                int numChains = _chains.size();
                while ( (insertion < numChains) &&
                        FeatureResolverImpl.compare( _chains.get(insertion), chain ) == 0 ) {
                    insertion++;
                }
            }
            _chains.add(insertion, chain);
        }

        /**
         * Tell if any of the chains has no more available candidates.
         *
         * That is, if there is at least one chain for which all candidates
         * were attempted.
         *
         * @return True or false telling if there is at least one exhausted
         *     chain.
         */
        public boolean exhaustedAny() {
            int numAttempted = _attempted.size();

            for (Chain chain : _chains) {
                List<String> candidates = chain.getCandidates();
                if ( numAttempted < candidates.size() ) {
                    continue; // All candidates could not have been attempted.
                }

                boolean isExhausted = true;
                for (String candidate : candidates ) {
                    if ( !_attempted.contains(candidate) ) {
                        isExhausted = false;
                        break;
                    }
                }
                if (isExhausted) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Select a chain that matches a specified base feature.
         *
         * Check the preferred candidate of each of the available chains.
         * If any of these matches, return that match as a new chain.
         *
         * Next, check all candidates of each of the available chains.
         * Again, if any of these matches, return that match as a new chain.
         *
         * Finally, if no match is found, record that the specified base
         * feature has a conflict with the available chains and answer null.
         *
         * @param baseName A base feature name.
         * @param selectionContext The context in which to locate a match.
         *
         * @return The matching candidate as a new chain.  Null if no match
         *     is found.
         */
        protected Chain select(String baseName) {
            // The first candidate is always the preferred candidate.
            // Check these preferred candidates first.

            for ( Chain chain : _chains ) {
                String preferredCandidate = chain.getCandidates().get(0);
                if ( _attempted.add(preferredCandidate) ) {
                    Chain selection = select(preferredCandidate, chain);
                    if ( selection != null ) {
                        return selection;
                    }
                }
            }

            // Check the remaining candidates.

            for ( Chain chain : _chains ) {
                boolean isFirst = true;
                for ( String candidate : chain.getCandidates() ) {
                    if ( isFirst ) {
                        isFirst = false;
                        continue;
                    }

                    if ( _attempted.add(candidate) ) {
                        Chain selection = select(candidate, chain);
                        if ( selection != null ) {
                            return selection;
                        }
                    }
                }
            }

            return null;
        }

        private Chain select(String candidate, Chain chain) {
            for ( Chain otherChain : _chains ) {
                if (otherChain == chain) {
                    continue;
                }
                if ( !otherChain.hasCandidate(candidate) ) {
                    return null;
                }
            }
            return chain.select(candidate);
        }

        /**
         * Answer the first chain which does not have a specified candidate.
         * Answer null if all chains have the candidate (which means that
         * there are no conflicts.)
         *
         * @param candidate A candidate to locate.
         *
         * @return The first chain which does not have a specified candidate.
         *     Null if all chains have the candidate.
         */
        protected Chain findConflict(String candidate) {
            for ( Chain chain : _chains ) {
                if ( !chain.hasCandidate(candidate) ) {
                    return chain;
                }
            }
            return null;
        }
    }

    /**
     * Encapsulate the results of performing resolution.  The
     * results include the actual resolved features, and include
     * any error cases.
     *
     * Error cases are:
     *
     * <ul>
     * <li>Features which are missing from the feature repository.</li>
     * <li>Requested features which are not public.</li>
     * <li>Conflicts between required dependency features.</li>
     * <li>Dependency features which are not supported by the current process types.</li>
     * </ul>
     */
    static class ResultImpl implements FeatureResolver.Result {
        /**
         * Base constructor: Create a result with no resolved
         * features and with no errors.
         */
        @Trivial
        ResultImpl() {
            // TODO: We should not have use a LinkedHashSet,
            // Auto-features appear to be very sensitive
            // to being installed last.

            this._resolved = new LinkedHashSet<>();

            this._missing = new HashSet<>();
            this._nonPublicRoots = new HashSet<>();
            this._conflicts = new HashMap<>();
            this._wrongProcessTypes = new HashMap<>();
        }

        /**
         * Copy constructor.  Create a result which is a partial copy of
         * another result.
         *
         * The missing and non-public roots of the other result are copied.
         * The conflicts of the other result are copied and passed in as
         * an added parameter.  This is done ... ++.  The wrong process type
         * features are not copied and are expected to be recomputed.
         *
         * @param other The other result which is to be copied.
         * @param conflicts Already copied conflicts.
         */
        ResultImpl(ResultImpl other, Map<String, Collection<Chain>> conflicts) {
            this._resolved = new LinkedHashSet<>(); // Set after processing the parent permutation.

            this._missing = new HashSet<>(other._missing);
            this._nonPublicRoots = new HashSet<>(other._nonPublicRoots);
            this._conflicts = new HashMap<>(conflicts);
            this._wrongProcessTypes = new HashMap<>(); // Recalculated
        }

        private final Set<String> _resolved;

        @Trivial
        @Override
        public Set<String> getResolvedFeatures() {
            return _resolved;
        }

        public ResultImpl setResolvedFeatures(Collection<String> resolved) {
            // NOTE: This should replace any existing resolved.
            // When processing auto-features we start with
            // an already processed permutation with a result
            // we must replace that with this new set of resolved
            _resolved.clear();
            _resolved.addAll(resolved);
            return this;
        }

        @Override
        public boolean hasErrors() {
            return !( _missing.isEmpty() &&
                      _nonPublicRoots.isEmpty() &&
                      _conflicts.isEmpty() &&
                      _wrongProcessTypes.isEmpty() );
        }

        private final Set<String> _missing;

        @Trivial
        @Override
        public Set<String> getMissing() {
            return _missing;
        }

        public void addMissing(String missingFeature) {
            if (_missing.add(missingFeature)) {
                trace("Feature not found [ " + missingFeature + " ]");
            }
        }

        private final Map<String, Collection<Chain>> _conflicts;

        @Trivial
        @Override
        public Map<String, Collection<Chain>> getConflicts() {
            return _conflicts;
        }

        @Trivial
        @Override
        public int getNumConflicts() {
            return _conflicts.size();
        }

        public void addConflict(String baseName, Collection<Chain> conflicts) {
            trace("Feature conflicts [ " + baseName + " ] with [ " + conflicts + " ]");
            _conflicts.put(baseName, conflicts);
        }

        private final Set<String> _nonPublicRoots;

        @Trivial
        @Override
        public Set<String> getNonPublicRoots() {
            return _nonPublicRoots;
        }

        public void addNonPublicRoot(String nonPublicRoot) {
            if (_nonPublicRoots.add(nonPublicRoot)) {
                trace("Non-public root feature [ " + nonPublicRoot + " ]");
            }
        }

        private final Map<String, Chain> _wrongProcessTypes;

        @Trivial
        @Override
        public Map<String, Chain> getWrongProcessTypes() {
            return _wrongProcessTypes;
        }

        public void addWrongProcessType(String symbolicName, Chain chain) {
            if (_wrongProcessTypes.put(symbolicName, chain) == null) {
                trace("Feature with unsupported process type [ " + symbolicName + " ] in chain [ " + chain + " ]");
            }
        }
    }
}
