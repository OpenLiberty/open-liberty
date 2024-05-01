/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Chain;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;

public class FeatureResolverResultImpl implements Result {
    public static void trace(String message) {
        FeatureResolverImpl.trace(message);
    }

    public FeatureResolverResultImpl() {
        this._missing = new HashSet<String>(0);
        this._missingRoots = new HashSet<>(0);
        this._missingReferences = new HashSet<>(0);

        this._incompleteFeatures = new HashMap<>(0);
        this._unlabelledResources = new HashMap<>(0);

        this._nonPublicRoots = new HashSet<String>(0);

        this._wrongProcessTypes = new HashMap<>(0);
        this._wrongRootProcessTypes = new HashSet<>(0);
        this._wrongResolvedProcessTypes = new HashMap<>(0);

        this._conflicts = new HashMap<>(0);

        this._resolved = new LinkedHashSet<>();
    }

    //

    @Override
    public boolean hasErrors() {
        return !(_missing.isEmpty() &&
                 _nonPublicRoots.isEmpty() &&
                 _wrongProcessTypes.isEmpty() &&
                 _conflicts.isEmpty());
    }

    //

    @Trivial
    private static String describeChain(Chain chain) {
        String featureName = chain.getFeatureRequirement();
        List<String> candidates = chain.getCandidates();
        List<String> resolutionPath = chain.getChain();

        StringBuilder builder = new StringBuilder();
        builder.append("Resolution [ ");
        builder.append(featureName);
        builder.append(" : ");
        builder.append(candidates);
        builder.append(" ]: ");
        builder.append(resolutionPath);

        return builder.toString();
    }

    @Trivial
    private static String describeResolutionPath(Collection<String> chain) {
        StringBuilder builder = new StringBuilder();
        builder.append("Resolution [ ");

        boolean isFirst = true;
        for (String featureName : chain) {
            if (!isFirst) {
                builder.append(", ");
            } else {
                isFirst = false;
            }
            builder.append(featureName);
        }
        builder.append(" ]: ");

        return builder.toString();
    }
    //

    protected final Set<String> _missing;

    @Override
    public Set<String> getMissing() {
        return _missing;
    }

    // FeatureResolverImpl.checkRootsAreAccessibleAndSetFullName
    // -- A root feature does not exist.
    // -- Root feature name.
    // addMissingRoot(featureName);

    // FeatureResolverImpl.processIncluded
    // -- An included resource does not have a symbolic name.
    // -- Resolution chain.
    // -- Included resource.
    // addUnlabelledResource(includedResource, resolutionChain)

    // FeatureResolverImpl.processRoots
    // -- An public feature obtained by resolution does not exist.
    // -- Resolved feature name.
    // addMissingReference(featureName);

    // FeatureResolverImpl.SelectionContext.processCandidates
    // -- No candidate is available which matches the resolution process type.
    // -- Resolution chain.
    // -- Feature base name.
    // -- Tolerated versions.
    // addIncomplete(featureBaseName, toleratedVersions, resolutionChain);

    protected void addMissing(String missingFeature) {
        if (_missing.add(missingFeature)) {
            trace("Missing feature [ " + missingFeature + " ]");
        }
    }

    //

    protected final Set<String> _missingRoots;

    protected void addMissingRoot(String featureName) {
        if (_missingRoots.add(featureName)) {
            trace("Root feature [ " + featureName + " ] is missing.");
        }

        addMissing(featureName);
    }

    protected final Map<String, Set<List<String>>> _unlabelledResources;

    private static List<String> copy(Deque<String> chain) {
        List<String> copyChain = new ArrayList<>(chain.size());
        for (String featureName : chain) {
            copyChain.add(featureName);
        }
        return copyChain;
    }

    protected void addUnlabelledResource(FeatureResource resource, Deque<String> chain) {
        String location = resource.getLocation();
        Set<List<String>> chains = _unlabelledResources.get(location);
        if (chains == null) {
            chains = new HashSet<>(1);
            _unlabelledResources.put(location, chains);
        }
        chains.add(copy(chain));
        trace("Resource [ " + location + " ] has no symbolic name.");
        trace(describeResolutionPath(chain));

        addMissing(location);
    }

    protected final Set<String> _missingReferences;

    protected void addMissingReference(String featureName) {
        if (_missingReferences.add(featureName)) {
            trace("Missing referenced feature [ " + featureName + " ]");
        }

        addMissing(featureName);
    }

    public static class IncompleteResolution {
        public final String baseName;
        public final List<String> candidates;
        public final List<String> chain;

        public IncompleteResolution(String baseName, Collection<String> candidates, Collection<String> chain) {
            this.baseName = baseName;
            this.candidates = new ArrayList<>(candidates);
            this.chain = new ArrayList<>(chain);
        }
    }

    protected final Map<String, Set<IncompleteResolution>> _incompleteFeatures;

    protected void addIncomplete(String baseName, List<String> candidates, Collection<String> chain) {
        Set<IncompleteResolution> resolutions = _incompleteFeatures.get(baseName);
        if (resolutions == null) {
            resolutions = new HashSet<>(1);
            _incompleteFeatures.put(baseName, resolutions);
        }

        IncompleteResolution resolution = new IncompleteResolution(baseName, candidates, chain);
        resolutions.add(resolution);

        trace("Base feature [ " + baseName + " ] with candidates [ " + candidates + " ] has no resolutions for the process type.");
        trace(describeResolutionPath(chain));

        addMissing(baseName);
    }

    protected final Set<String> _nonPublicRoots;

    @Override
    public Set<String> getNonPublicRoots() {
        return _nonPublicRoots;
    }

    // FeatureResolverImpl.checkRootsAreAccessibleAndSetFullName
    // -- A non-public root feature was specified.
    // -- Root feature name.

    protected void addNonPublicRoot(String nonPublicRoot) {
        if (_nonPublicRoots.add(nonPublicRoot)) {
            trace("Root feature [ " + nonPublicRoot + " ] is not public.");
        }
    }

    protected final Map<String, Chain> _wrongProcessTypes;

    @Override
    public Map<String, Chain> getWrongProcessTypes() {
        return _wrongProcessTypes;
    }

    // FeatureResolverImpl.checkRootsAreAccessibleAndSetFullName
    // -- A root feature was specified that does not match the resolution process type.
    // -- Root feature name.
    // addWrongRootFeatureType(featureName, processType);

    // FeatureResolverImpl.SelectionContext.processCandidates
    // -- A candidate has the wrong process type.
    // -- Candidate feature symbolic name.
    // -- Candidate resolution chain.
    // addWrongResolvedFeatureType(featureName, resolutionChain)

    protected void addWrongProcessType(String symbolicName, String preferredVersion) {
        Chain chain = new Chain(symbolicName, preferredVersion);

        if (_wrongProcessTypes.put(symbolicName, chain) == null) {
            trace("Incompatible process type for feature [ " + symbolicName + " ]");
        }
    }

    protected void addWrongProcessType(String symbolicName, Chain chain) {
        if (_wrongProcessTypes.put(symbolicName, chain) == null) {
            trace("Incompatible process type for feature [" + symbolicName + " ].");
            trace(describeChain(chain));
        }
    }

    protected final Set<String> _wrongRootProcessTypes;

    protected void addWrongRootFeatureType(String featureName) {
        if (_wrongRootProcessTypes.add(featureName)) {
            trace("Root feature [ " + featureName + " ] does not match the process type.");
        }

        String preferredVersion = FeatureResolverImpl.parseVersion(featureName);
        addWrongProcessType(featureName, preferredVersion);
    }

    protected final Map<String, Set<Chain>> _wrongResolvedProcessTypes;

    protected void addWrongResolvedFeatureType(String featureName, Chain chain) {
        Set<Chain> chains = _wrongResolvedProcessTypes.get(featureName);
        if (chains == null) {
            chains = new HashSet<>(1);
            _wrongResolvedProcessTypes.put(featureName, chains);
        }
        chains.add(chain);
        trace("Resolved feature [ " + featureName + " ] does not match the process type.");
        trace(describeChain(chain));

        addWrongProcessType(featureName, chain);
    }

    //

    protected final Map<String, Collection<Chain>> _conflicts;

    @Override
    public Map<String, Collection<Chain>> getConflicts() {
        return _conflicts;
    }

    // FeatureResolverImpl.SelectionContext.addConflict

    // com.ibm.ws.kernel.feature.internal.FeatureResolverImpl.Chains.select
    // -- None of the chains available for a feature can be resolved.

    // com.ibm.ws.kernel.feature.internal.FeatureResolverImpl.SelectionContext.primeSelected
    // -- Resolution reached a feature at a version different than was previously selected for the feature.

    // com.ibm.ws.kernel.feature.internal.FeatureResolverImpl.SelectionContext.processCandidates
    // -- None of the candidates available for resolution can be selected.

    protected void addConflict(String baseFeatureName, Collection<Chain> conflicts) {
        trace("Resolution conflicts for feature [ " + baseFeatureName + " ]:");
        for (Chain conflictChain : conflicts) {
            trace(describeChain(conflictChain));
        }
        _conflicts.put(baseFeatureName, conflicts);
    }

    //

    // Remember the resolved in resolution order.
    // This used to be necessary, but is now less necessary
    // due to changes made to enable parallel bundle startup.

    protected final Set<String> _resolved;

    @Override
    public Set<String> getResolvedFeatures() {
        return _resolved;
    }

    // NOTE: This should replace any existing resolved.
    // When processing auto-features we start with
    // an already processed permutation with a result
    // we must replace that with this new set of resolved

    protected FeatureResolverResultImpl setResolvedFeatures(Collection<String> resolved) {
        _resolved.clear();
        _resolved.addAll(resolved);

        return this;
    }
}
