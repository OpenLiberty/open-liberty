/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;

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

    /**
     * An exception that happens when copying a Chains object
     * to indicate that the chains within the Chains object
     * have no more valid options to try.
     */
    static class DeadEndChain extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static final Object tc;

    static {
        Object temp = null;
        try {
            temp = Tr.register(FeatureResolverImpl.class);
        } catch (Throwable t) {
            // nothing
        }
        tc = temp;
    }

    @Override
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver#resolveFeatures(com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository, java.util.Collection, java.util.Set)
     */
    public Result resolveFeatures(FeatureResolver.Repository repository, Collection<String> rootFeatures, Set<String> preResolved, boolean allowMultipleVersions) {
        // Note that when no process type is passed we support all process types.
        return resolveFeatures(repository, Collections.<ProvisioningFeatureDefinition> emptySet(), rootFeatures, preResolved, allowMultipleVersions,
                               EnumSet.allOf(ProcessType.class));
    }

    @Override
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver#resolveFeatures(com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository, java.util.Collection,
     * java.util.Collection, java.util.Set)
     */
    public Result resolveFeatures(FeatureResolver.Repository repository, Collection<ProvisioningFeatureDefinition> kernelFeatures, Collection<String> rootFeatures,
                                  Set<String> preResolved, boolean allowMultipleVersions) {
        // Note that when no process type is passed we support all process types.
        return resolveFeatures(repository, kernelFeatures, rootFeatures, preResolved, allowMultipleVersions, EnumSet.allOf(ProcessType.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver#resolveFeatures(com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository, java.util.Collection,
     * java.util.Collection, java.util.Set,
     * boolean, java.util.EnumSet)
     * Here are the steps this uses to resolve:
     * 1) Primes the selected features with the pre-resolved and the root features (conflicts are reported, but no permutations for backtracking)
     * 2) Resolve the root features
     * 3) Check if there are any auto features to resolve; if so return to step 2 and resolve the auto-features as root features
     */
    @Override
    public Result resolveFeatures(Repository repository, Collection<ProvisioningFeatureDefinition> kernelFeatures, Collection<String> rootFeatures, Set<String> preResolved,
                                  boolean allowMultipleVersions,
                                  EnumSet<ProcessType> supportedProcessTypes) {
        SelectionContext selectionContext = new SelectionContext(repository, allowMultipleVersions, supportedProcessTypes);

        // this checks if the pre-resolved exists in the repo;
        // if one does not exist then we start over with an empty set of pre-resolved
        preResolved = checkPreResolvedExistAndSetFullName(preResolved, selectionContext);

        // check that the root features exist and are public; remove them if not; also get the full name
        rootFeatures = checkRootsAreAccessibleAndSetFullName(new ArrayList<String>(rootFeatures), selectionContext, preResolved);

        // Always prime the selected with the pre-resolved and the root features.
        // This will ensure that the root and pre-resolved features do not conflict
        selectionContext.primeSelected(preResolved);
        selectionContext.primeSelected(rootFeatures);

        // Even if the feature set hasn't changed, we still need to process the auto features and add any features that need to be
        // installed/uninstalled to the list. This recursively iterates over the auto Features, as previously installed features
        // may satisfy other auto features.
        Set<String> autoFeaturesToInstall = Collections.<String> emptySet();
        Set<String> seenAutoFeatures = new HashSet<String>();
        Set<String> resolved = Collections.emptySet();

        do {
            if (!!!autoFeaturesToInstall.isEmpty()) {
                // this is after the first pass;  use the autoFeaturesToInstall as the roots
                rootFeatures = autoFeaturesToInstall;
                // Need to prime the auto features as selected
                selectionContext.primeSelected(autoFeaturesToInstall);
                // and use the resolved as the preResolved
                preResolved = resolved;
                // A new resolution process will happen now along with the auto-features that match;
                // need to save off the current conflicts to be the pre resolved conflicts
                // otherwise they would get lost
                selectionContext.saveCurrentPreResolvedConflicts();
            }
            resolved = doResolveFeatures(rootFeatures, preResolved, selectionContext);
        } while (!!!(autoFeaturesToInstall = processAutoFeatures(kernelFeatures, resolved, seenAutoFeatures, selectionContext)).isEmpty());
        // Finally return the selected result
        return selectionContext.getResult();
    }

    private List<String> checkRootsAreAccessibleAndSetFullName(List<String> rootFeatures, SelectionContext selectionContext, Set<String> preResolved) {
        for (ListIterator<String> iRootFeatures = rootFeatures.listIterator(); iRootFeatures.hasNext();) {
            String rootFeatureName = iRootFeatures.next();
            ProvisioningFeatureDefinition rootFeatureDef = selectionContext.getRepository().getFeature(rootFeatureName);
            if (rootFeatureDef != null) {
                if (rootFeatureDef.getVisibility() != Visibility.PUBLIC) {
                    // don't allow non-public roots
                    selectionContext.getResult().addNonPublicRoot(rootFeatureName);
                    iRootFeatures.remove();
                } else if (!!!supportedProcessType(selectionContext._supportedProcessTypes, rootFeatureDef)) {
                    // don't allow features that are for the wrong container type
                    String rootSymbolicName = rootFeatureDef.getSymbolicName();
                    String[] nameAndVersion = parseNameAndVersion(rootSymbolicName);
                    String preferredVersion = nameAndVersion[1];
                    Chain chain = new Chain(Collections.<String> emptyList(), Collections.singletonList(rootSymbolicName), preferredVersion,
                                            rootSymbolicName);
                    selectionContext.getResult().addWrongProcessType(rootFeatureName, chain);
                    iRootFeatures.remove();
                } else if (preResolved.contains(rootFeatureDef.getSymbolicName())) {
                    // remove pre-resolved features from the root
                    iRootFeatures.remove();
                } else {
                    // set the full symbolicName
                    iRootFeatures.set(rootFeatureDef.getSymbolicName());
                }
            } else {
                selectionContext.getResult().addMissing(rootFeatureName);
                iRootFeatures.remove();
            }
        }
        return rootFeatures;
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
        // need to reset the initial black list count so we can recalculate it during the first pass
        selectionContext.resetInitialBlackListCount();
        Set<String> result = processCurrentPermutation(rootFeatures, preResolved, selectionContext);

        // if the first pass resulted in no conflicts return the results (optimistic)
        if (selectionContext.getResult().getConflicts().isEmpty()) {
            selectionContext.selectCurrentPermutation();
            return result;
        }

        // oh oh, we have conflicts;
        // NOTE, if the current solution has more conflicts than the initial count (black list)
        // then that means one of the toleration (postponed) choices we made introduced an
        // addition conflict.  That implies that a better solution may be available.
        // As long as there are more conflicts than the number of initial root conflicts
        // and there is a different permutation to try do another pass
        while (selectionContext.currentHasMoreThanInitialBlackListCount() && selectionContext.popPermutation()) {
            result = processCurrentPermutation(rootFeatures, preResolved, selectionContext);
        }

        // Return the best solution found
        selectionContext.restoreBestSolution();
        return selectionContext.getResult().getResolvedFeatures();
    }

    Set<String> processCurrentPermutation(Collection<String> rootFeatures, Set<String> preResolved, SelectionContext selectionContext) {
        Set<String> result;
        // The number of black listed is checked each time we process a postponed decision.
        // A check is done each time we process the roots after doing a postponed decision
        // to see if more features got black listed.  If more got black listed then we
        // re-process the roots again.
        // This is necessary to ensure the final result does not include one of the black list features
        int numBlacklisted;
        do {
            selectionContext.processPostponed();
            numBlacklisted = selectionContext.getBlackListCount();
            result = processRoots(rootFeatures, preResolved, selectionContext);
        } while (selectionContext.hasPostponed() || numBlacklisted != selectionContext.getBlackListCount());
        // Save the result in the current permutation
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
        // Prime the results with the pre-resolved; make sure to use the getFeatureName for the result
        for (String featureSymbolicName : preResolved) {
            ProvisioningFeatureDefinition featureDef = selectionContext._repository.getFeature(featureSymbolicName);
            result.add(featureDef.getFeatureName());
        }
        for (String rootFeatureName : rootFeatures) {
            ProvisioningFeatureDefinition rootFeatureDef = selectionContext.getRepository().getFeature(rootFeatureName);
            if (rootFeatureDef == null) {
                // missing case
                selectionContext.getResult().addMissing(rootFeatureName);
            } else {
                // process the selected root and its included features
                processSelected(rootFeatureDef, null, chain, result, selectionContext);
            }
        }
        // Note that this only saves the black list count on the first call during doResolveFeatures;
        // Any conflicts here will be due to hard failures with no alternative toleration choices.
        // In other words, it is the best conflict count we will ever achieve.
        selectionContext.setInitialRootBlackListCount();
        return result;
    }

    @FFDCIgnore(IllegalArgumentException.class)
    public static String[] parseNameAndVersion(String feature) {
        // figure out the base symbolic name and 'version'
        // using last dash as a convention to determine the version and symbolic name
        String baseName = feature;
        String version = null;
        int lastDash = feature.lastIndexOf('-');
        if (lastDash >= 0) {
            // remove the version part of the symbolic name
            version = feature.substring(lastDash + 1);
            // sanity check for the version syntax
            try {
                Version.parseVersion(version);
                baseName = feature.substring(0, lastDash);
            } catch (IllegalArgumentException e) {
                version = null;
            }
        }
        return new String[] { baseName, version };
    }

    private void processSelected(ProvisioningFeatureDefinition selectedFeature, Set<String> allowedTolerations, Deque<String> chain, Set<String> result,
                                 SelectionContext selectionContext) {
        if (selectedFeature == null) {
            return;
        }

        // first check if the feature is blacklisted as already in conflict
        String featureName = selectedFeature.getSymbolicName();
        String baseFeatureName = parseNameAndVersion(featureName)[0];
        if (selectionContext.isBlackListed(baseFeatureName)) {
            return;
        }
        // sanity check to make sure this feature is selected; this is really just to check bugs in the resolver
        if (!!!selectionContext._allowMultipleVersions && selectedFeature.isSingleton()) {
            Chain existingSelection = selectionContext.getSelected(baseFeatureName);
            String selectedFeatureName = existingSelection == null ? null : existingSelection.getCandidates().get(0);
            if (existingSelection == null || !!!featureName.equals(selectedFeatureName)) {
                throw new IllegalStateException("Expected feature \"" + featureName + "\" to be selected instead feature of \"" + selectedFeatureName);
            }
        }

        if (chain.contains(selectedFeature.getSymbolicName())) {
            // must be in a cycle
            return;
        }

        chain.addLast(selectedFeature.getSymbolicName());
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
            result.add(selectedFeature.getFeatureName());
        }
    }

    private void processIncluded(ProvisioningFeatureDefinition includingFeature, FeatureResource included, Set<String> allowedTolerations, Deque<String> chain, Set<String> result,
                                 SelectionContext selectionContext) {
        String symbolicName = included.getSymbolicName();
        if (symbolicName == null) {
            // TODO why do we report this feature as missing, seems a better error message would indicate the FeatureResource requirement has no SN
            // get the symbolic name from the last in chain
            if (!!!chain.isEmpty()) {
                selectionContext.getResult().addMissing(chain.peekLast());
            }
            return;
        }

        String[] nameAndVersion = parseNameAndVersion(symbolicName);
        String baseSymbolicName = nameAndVersion[0];
        String preferredVersion = nameAndVersion[1];
        boolean isSingleton = false;

        // if the base name is blacklisted then we do not continue with this included
        if (selectionContext.isBlackListed(baseSymbolicName)) {
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
        if (preferredCandidateDef != null && isAccessible(includingFeature, preferredCandidateDef)) {
            checkForFullSymbolicName(preferredCandidateDef, symbolicName, chain.getLast());
            isSingleton = preferredCandidateDef.isSingleton();
            candidateNames.add(symbolicName);
        }

        // Check for tolerated versions; but only if the preferred version is a singleton or we did not find the preferred version
        if (tolerates != null && (candidateNames.isEmpty() || isSingleton)) {
            for (String tolerate : tolerates) {
                if (selectionContext._allowMultipleVersions) {
                    // if we are in minify mode (_allowMultipleVersions) then we only want to continue to look for
                    // tolerated versions until we have found one candidate
                    if (!!!candidateNames.isEmpty()) {
                        break;
                    }
                }
                String toleratedSymbolicName = baseSymbolicName + '-' + tolerate;
                ProvisioningFeatureDefinition toleratedCandidateDef = selectionContext.getRepository().getFeature(toleratedSymbolicName);
                if (toleratedCandidateDef != null && !!!candidateNames.contains(toleratedCandidateDef.getSymbolicName()) && isAccessible(includingFeature, toleratedCandidateDef)) {
                    checkForFullSymbolicName(toleratedCandidateDef, toleratedSymbolicName, chain.getLast());
                    isSingleton |= toleratedCandidateDef.isSingleton();
                    // Only check against the allowed tolerations if this candidate feature is public or protected (NOT private)
                    if (isAllowedToleration(selectionContext, toleratedCandidateDef, allowedTolerations, overrideTolerates, baseSymbolicName, tolerate)) {
                        candidateNames.add(toleratedCandidateDef.getSymbolicName());
                    }
                }
            }
        }

        if (!!!isSingleton && candidateNames.size() > 1) {
            // If the candidates are not singleton and there are multiple then that means
            // someone is using tolerates for a non-singleton feature (error case?).
            // For now just use the first candidate
            candidateNames.retainAll(Collections.singleton(candidateNames.get(0)));
        }
        selectionContext.processCandidates(chain, candidateNames, symbolicName, baseSymbolicName, preferredVersion, isSingleton);
        // check if there is a single candidate left after processing
        if (candidateNames.size() == 1) {
            // We selected one candidate; now process the selected
            String selectedName = candidateNames.get(0);
            processSelected(selectionContext.getRepository().getFeature(selectedName), allowedTolerations, chain, result, selectionContext);
        }
    }

    /**
     * @param includingFeature
     * @param preferredCandidateDef
     * @return
     */
    private boolean isAccessible(ProvisioningFeatureDefinition includingFeature, ProvisioningFeatureDefinition candidateDef) {
        return candidateDef.getVisibility() != Visibility.PRIVATE || includingFeature.getBundleRepositoryType().equals(candidateDef.getBundleRepositoryType());
    }

    /**
     * @param selectionContext
     * @param toleratedCandidateDef
     * @param allowedTolerations
     * @param overrideTolerates
     * @param baseSymbolicName
     * @param tolerate
     * @return
     */
    private boolean isAllowedToleration(SelectionContext selectionContext, ProvisioningFeatureDefinition toleratedCandidateDef, Set<String> allowedTolerations,
                                        List<String> overrideTolerates,
                                        String baseSymbolicName, String tolerate) {
        // if in minify mode always allow (_allowMultipleVersions)
        if (selectionContext._allowMultipleVersions) {
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
        return false;
    }

    /**
     * @param preferredCandidateDef
     * @param symbolicName
     * @param includingFeature
     */
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
            if (!seenAutoFeatures.contains(featureSymbolicName))
                if (autoFeatureDef.isCapabilitySatisfied(filteredFeatureDefs)) {

                    // Add this auto feature to the list of auto features to ignore on subsequent recursions.
                    seenAutoFeatures.add(featureSymbolicName);
                    // Add the feature to the return value of auto features to install if they are supported in this process.
                    if (supportedProcessType(selectionContext._supportedProcessTypes, autoFeatureDef)) {
                        autoFeaturesToProcess.add(featureSymbolicName);
                    }
                }
        }

        return autoFeaturesToProcess;
    }

    /*
     * The selection context maintains the state of the resolve operation.
     * It records the selected candidates, the postponed decisions and
     * any blacklisted features. It also keeps a stack of permutations
     * that can be used to backtrack earlier decisions.
     */
    static class SelectionContext {
        static class Permutation {
            final Map<String, Chain> _selected = new HashMap<String, Chain>();
            final Map<String, Chains> _postponed = new LinkedHashMap<String, Chains>();
            final Set<String> _blacklistFeatures = new HashSet<String>();
            final ResultImpl _result = new ResultImpl();

            Permutation copy(Map<String, Collection<Chain>> preResolveConflicts) throws DeadEndChain {
                Permutation copy = new Permutation();
                copy._selected.putAll(_selected);

                // The conflicts should get populated from the preResolveConflicts (if any);
                // other conflicts will get recalculated
                copy._result._conflicts.putAll(preResolveConflicts);

                // Only copy the missing and nonPublicRoots from the result.
                // The wrongProcessTypes will get recalculated;
                // The resolved are set at the end of processing the permutation;
                copy._result._missing.addAll(_result.getMissing());
                copy._result._nonPublicRoots.addAll(_result.getNonPublicRoots());

                // now we need to copy each postponed Chains
                for (Map.Entry<String, Chains> chainsEntry : _postponed.entrySet()) {
                    copy._postponed.put(chainsEntry.getKey(), chainsEntry.getValue().copy());
                }

                // NOTE the black list features are NOT copied; they get recalculated
                return copy;
            }
        }

        private final FeatureResolver.Repository _repository;
        private final Deque<Permutation> _permutations = new ArrayDeque<Permutation>(Arrays.asList(new Permutation()));
        private final boolean _allowMultipleVersions;
        private final EnumSet<ProcessType> _supportedProcessTypes;
        private final AtomicInteger _initialBlackListCount = new AtomicInteger(-1);
        private final Map<String, Collection<Chain>> _preResolveConflicts = new HashMap<String, Collection<Chain>>();
        private Permutation _current = _permutations.getFirst();

        SelectionContext(FeatureResolver.Repository repository, boolean allowMultipleVersions, EnumSet<ProcessType> supportedProcessTypes) {
            this._repository = repository;
            this._allowMultipleVersions = allowMultipleVersions;
            this._supportedProcessTypes = supportedProcessTypes;
        }

        void saveCurrentPreResolvedConflicts() {
            _preResolveConflicts.clear();
            _preResolveConflicts.putAll(_current._result.getConflicts());
        }

        void resetInitialBlackListCount() {
            _initialBlackListCount.set(-1);
        }

        boolean currentHasMoreThanInitialBlackListCount() {
            return getBlackListCount() > _initialBlackListCount.get();
        }

        void setInitialRootBlackListCount() {
            // only set this once
            _initialBlackListCount.compareAndSet(-1, getBlackListCount());
        }

        void restoreBestSolution() {
            // NOTE that the best solution is kept as the last permutation
            while (popPermutation());
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
                _current = popped;
                return true;
            }
            return false;
        }

        @FFDCIgnore(DeadEndChain.class)
        void pushPermutation() {
            // We only want to backtrack this decision if the current
            // permutation does not add more black list conflicts to the initial root conflicts
            if (_initialBlackListCount.get() == getBlackListCount()) {
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

        boolean isBlackListed(String baseSymbolicName) {
            return _current._blacklistFeatures.contains(baseSymbolicName);
        }

        int getBlackListCount() {
            return _current._blacklistFeatures.size();
        }

        ResultImpl getResult() {
            return _current._result;
        };

        void processCandidates(Collection<String> chain, List<String> candidateNames, String symbolicName, String baseSymbolicName, String preferredVersion, boolean isSingleton) {
            // first check for container type
            for (Iterator<String> iCandidateNames = candidateNames.iterator(); iCandidateNames.hasNext();) {
                ProvisioningFeatureDefinition fd = _repository.getFeature(iCandidateNames.next());
                if (!!!supportedProcessType(_supportedProcessTypes, fd)) {
                    Chain c = new Chain(chain, candidateNames, preferredVersion, symbolicName);
                    _current._result.addWrongProcessType(symbolicName, c);
                    iCandidateNames.remove();
                }
            }
            if (candidateNames.isEmpty()) {
                _current._result.addMissing(symbolicName);
                return;
            }
            if (_allowMultipleVersions || !!!isSingleton) {
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
                    addConflict(baseSymbolicName, new ArrayList<Chain>(Arrays.asList(selectedChain, new Chain(chain, copyCandidates, preferredVersion, symbolicName))));
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
            // check if there is a postponed decision
            Chain conflict = getPostponedConflict(baseSymbolicName, selectedName);
            if (conflict != null) {
                addConflict(baseSymbolicName, new ArrayList<Chain>(Arrays.asList(conflict, new Chain(chain, copyCandidates, preferredVersion, symbolicName))));
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
            return;

        }

        Chain getSelected(String baseName) {
            return _current._selected.get(baseName);
        }

        boolean hasPostponed() {
            return !!!_current._postponed.isEmpty();
        }

        void processPostponed() {
            if (_current._postponed.isEmpty()) {
                return;
            }
            // Only process the first postponed and try again;
            // We have to do this one postpone at a time because
            // The decision of one postpone may effect the path of the
            // dependency in such a way to make later postponed decisions
            // unnecessary
            Map.Entry<String, Chains> firstPostponed = _current._postponed.entrySet().iterator().next();
            // try to find a good selection
            Chain selected = firstPostponed.getValue().select(firstPostponed.getKey(), this);
            if (selected != null) {
                // found a good one, select it.
                _current._selected.put(firstPostponed.getKey(), selected);
            }

            // clean postponed since we will walk the tree again and find them again if necessary
            _current._postponed.clear();
        }

        void primeSelected(Collection<String> features) {
            if (_allowMultipleVersions) {
                // no need to do any selecting when allowing multiple versions
                return;
            }
            // Need to prime each feature as a selected feature, while also checking that
            // there are not any current conflicts in the collection.
            // Note that the features may include two versions of the same feature (e.g. servlet-3.0 and servlet-3.1)
            // this case needs to be handled by removing both versions from the features collection
            // and black listing the base feature name (e.g. servlet)
            Map<String, String> conflicts = new HashMap<String, String>();
            for (Iterator<String> iFeatures = features.iterator(); iFeatures.hasNext();) {
                String featureName = iFeatures.next();
                ProvisioningFeatureDefinition featureDef = _repository.getFeature(featureName);
                if (featureDef != null && featureDef.isSingleton()) {
                    // Only need to prime selected for singletons.
                    // Be sure to get the real symbolic name; don't just use the feature name used to do the lookup
                    String featureSymbolicName = featureDef.getSymbolicName();
                    String[] nameAndVersion = parseNameAndVersion(featureSymbolicName);
                    String base = nameAndVersion[0];
                    String preferredVersion = nameAndVersion[1];
                    // check for an existing selection for this base feature name
                    Chain selectedChain = _current._selected.get(base);
                    if (selectedChain != null) {
                        // TODO Need to revisit why this is not always a conflict.
                        // We only keep the first selected one
                        iFeatures.remove();
                        // check if the selected feature is contained in the features collection;
                        // if so then it is a conflict also and we need to clean it up and blacklist it
                        String selectedFeature = selectedChain.getCandidates().get(0);
                        if (features.contains(selectedFeature)) {
                            Chain conflictedFeatureChain = new Chain(Collections.<String> emptyList(), Collections.singletonList(featureSymbolicName), preferredVersion,
                                                                     featureSymbolicName);
                            addConflict(base, new ArrayList<Chain>(Arrays.asList(selectedChain, conflictedFeatureChain)));
                            conflicts.put(selectedFeature, base);
                        }
                    } else {
                        _current._selected.put(base, new Chain(Collections.<String> emptyList(), Collections.singletonList(featureSymbolicName), preferredVersion,
                                                               featureSymbolicName));
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
            Chains existing = _current._postponed.get(baseName);
            if (existing == null) {
                existing = new Chains();
                _current._postponed.put(baseName, existing);
            }
            existing.add(chain);
        }

        Chain getPostponedConflict(String baseName, String selectedName) {
            Chains postponedChains = _current._postponed.get(baseName);
            return postponedChains == null ? null : postponedChains.findConflict(selectedName);
        }

        void addConflict(String baseFeatureName, List<Chain> conflicts) {
            _current._blacklistFeatures.add(baseFeatureName);
            _current._result.addConflict0(baseFeatureName, conflicts);
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
                while (insertion < _chains.size() && (existing = _chains.get(insertion)) != null && compare(existing, chain) == 0) {
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

        /*
         * (non-Javadoc)
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(Chain o1, Chain o2) {
            // We sort by preferred version where lowest sorts first
            return o1.getPreferredVersion().compareTo(o2.getPreferredVersion());
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

    static class ResultImpl implements Result {
        // TODO we should not have use a LinkedHashSet, but it appears auto-features are pretty sensitive to being installed last
        final Set<String> _resolved = new LinkedHashSet<String>();
        final Set<String> _missing = new HashSet<String>();
        final Set<String> _nonPublicRoots = new HashSet<String>();
        final Map<String, Collection<Chain>> _conflicts = new HashMap<String, Collection<Chain>>();
        final Map<String, Chain> _wrongProcessTypes = new HashMap<String, Chain>();

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result#getResolvedFeatures()
         */
        @Override
        public Set<String> getResolvedFeatures() {
            return _resolved;
        }

        ResultImpl setResolvedFeatures(Collection<String> resolved) {
            _resolved.addAll(resolved);
            return this;
        }

        @Override
        public Set<String> getMissing() {
            return _missing;
        }

        void addMissing(String missingFeature) {
            if (_missing.add(missingFeature)) {
                trace("Missing a feature: " + missingFeature);
            }
        }

        @Override
        public Map<String, Collection<Chain>> getConflicts() {
            return _conflicts;
        }

        void addConflict0(String baseFeatureName, Collection<Chain> conflicts) {
            trace("Found a conflict for feature: \"" + baseFeatureName + "\" with conficts: " + conflicts);
            _conflicts.put(baseFeatureName, conflicts);
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result#getNonPublicRoots()
         */
        @Override
        public Set<String> getNonPublicRoots() {
            return _nonPublicRoots;
        }

        void addNonPublicRoot(String nonPublicRoot) {
            if (_nonPublicRoots.add(nonPublicRoot)) {
                trace("A non-public root feature is being used: " + nonPublicRoot);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result#getWrongProcessTypes()
         */
        @Override
        public Map<String, Chain> getWrongProcessTypes() {
            return _wrongProcessTypes;
        }

        void addWrongProcessType(String wrongProcessType, Chain chain) {
            if (_wrongProcessTypes.put(wrongProcessType, chain) == null) {
                trace("A feature with the wrong process type is being used: \"" + wrongProcessType + "\" from chain: " + chain);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result#hasErrors()
         */
        @Override
        public boolean hasErrors() {
            return !!!(_missing.isEmpty() && _nonPublicRoots.isEmpty() && _conflicts.isEmpty() && _wrongProcessTypes.isEmpty());
        }
    }

    static void trace(String message) {
        if (tc != null) {
            if (TraceComponent.isAnyTracingEnabled() && ((TraceComponent) tc).isDebugEnabled()) {
                Tr.debug((TraceComponent) tc, message);
            }
        }
    }
}
