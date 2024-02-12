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
package com.ibm.ws.kernel.feature.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.osgi.framework.Version;

import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 * A resolver for liberty features. The resolver has two modes:
 *
 * First, the resolver accepts kernel and requested features and resolves
 * the constituents of these, selecting feature versions which mutually
 * satisfy the features. All <strong>auto</strong> features which are
 * satisfied as also selected. This first resolver mode is used when
 * starting the server, and is used when running minify on the server.
 *
 * Secondly, the resolver selects all constituents of requested features.
 * This second mode is used during installation.
 */
public interface FeatureResolver {
    /**
     * A collection of features which are available for feature resolution.
     */
    public interface Repository {
        /**
         * Answer all feature within this repository.
         *
         * @return All features of this repository.
         */
        Collection<ProvisioningFeatureDefinition> getFeatures();

        /**
         * Answer all auto-features of the repository.
         *
         * See {@link ProvisioningFeatureDefinition#isAutoFeature()}.
         *
         * @return The auto-features of this repository.
         */
        Collection<ProvisioningFeatureDefinition> getAutoFeatures();

        /**
         * Select feature definitions which match a specified predicate.
         *
         * @param selector A feature selector. If null, all features are selected.
         *
         * @return All feature definitions which match the predicate.
         */
        List<ProvisioningFeatureDefinition> select(Predicate<ProvisioningFeatureDefinition> selector);

        /**
         * Answer the feature which matches a specified feature name.
         *
         * The feature name may be the short name of a public feature,
         * or a feature symbolic name.
         *
         * @param featureName A feature short name or symbolic name.
         *
         * @return The named feature. Null if no matching feature is found.
         */
        ProvisioningFeatureDefinition getFeature(String featureName);

        /**
         * Returns a list of versions that should always be tolerated for the
         * specified base symbolic name. For example, a feature named
         * com.ibm.ws.something_1.0 has a base symbolic name of
         * &quot;com.ibm.ws.something&quot;. This method could return
         * a list [1.0, 1.1, 1.2] to indicate that all features that include a
         * version of com.ibm.ws.something also can tolerate versions
         * 1.0, 1.1 and 1.2. This is a way to override the tolerates.
         *
         * @param baseSymbolicName a feature base symbolic name to allow more tolerations for.
         * @return The additional versions of the feature that can be tolerated.
         */
        List<String> getConfiguredTolerates(String baseSymbolicName);
    }

    /**
     * A feature resolution result.
     */
    public interface Result {
        Set<String> getResolvedFeatures();

        //

        boolean hasErrors();

        Set<String> getMissing();

        Set<String> getNonPublicRoots();

        /**
         * Not really used yet
         */
        Map<String, Chain> getWrongProcessTypes();

        /**
         * The conflicting chains that resulted from a resolution operation. The key
         * is the base name of the feature that has conflicting versions required.
         * The value is a collection of dependency {@link Chain}s that transitively
         * lead to the conflicting versions of the feature.
         */
        Map<String, Collection<Chain>> getConflicts();
    }

    /**
     * A dependency chain of feature requirements that lead a singleton feature
     * and a list of candidates.
     */
    public static class Chain {
        private final List<String> _chain;
        private final List<String> _candidates;
        private final Version _preferredVersion;
        private final String _originalFeatureReq;

        /**
         * Creates a dependency chain.
         *
         * @param chain              The chain of feature names that have lead to a requirement on a singleton feature
         * @param candidates         The tolerated candidates that were found which may satisfy the feature requirement
         * @param preferredVersion   The preferred version
         * @param originalFeatureReq The full feature name that is required.
         */
        public Chain(Collection<String> chain, List<String> candidates, String preferredVersion, String originalFeatureReq) {
            _chain = chain.isEmpty() ? Collections.<String> emptyList() : new ArrayList<String>(chain);
            _candidates = candidates;
            Version v;
            try {
                v = Version.parseVersion(preferredVersion);
            } catch (IllegalArgumentException e) {
                v = Version.emptyVersion;
            }
            _preferredVersion = v;
            _originalFeatureReq = originalFeatureReq;
        }

        public Chain(String candidate, String preferredVersion, String originalFeatureReq) {
            _chain = Collections.<String> emptyList();
            _candidates = Collections.singletonList(candidate);
            Version v;
            try {
                v = Version.parseVersion(preferredVersion);
            } catch (IllegalArgumentException e) {
                v = Version.emptyVersion;
            }
            _preferredVersion = v;
            _originalFeatureReq = originalFeatureReq;
        }

        public List<String> getChain() {
            return _chain;
        }

        public List<String> getCandidates() {
            return _candidates;
        }

        public Version getPreferredVersion() {
            return _preferredVersion;
        }

        public String getFeatureRequirement() {
            return _originalFeatureReq;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ROOT->");
            for (String featureName : _chain) {
                builder.append(featureName).append("->");
            }
            builder.append(_candidates);
            builder.append(" ").append(_preferredVersion);
            return builder.toString();
        }
    }

    /**
     * Resolve with an empty collection of kernel features.
     */
    public Result resolveFeatures(Repository repository,
                                  Collection<String> rootFeatures,
                                  Set<String> preResolved,
                                  boolean allowMultipleVersions);

    /**
     * Resolve allowing all process types.
     */
    public Result resolveFeatures(Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                  Collection<String> rootFeatures,
                                  Set<String> preResolved,
                                  boolean allowMultipleVersions);

    /**
     * Main resolution API: Resolve features using the supplied parameters.
     */
    public Result resolveFeatures(Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                  Collection<String> rootFeatures,
                                  Set<String> preResolved,
                                  boolean allowMultipleVersions,
                                  EnumSet<ProcessType> supportedProcessTypes);

    /**
     * Main resolution API: Resolve features using the supplied parameters.
     *
     * Specify features which multiple-versions are allowed using a set.
     */
    public Result resolveFeatures(Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                  Collection<String> rootFeatures,
                                  Set<String> preResolved,
                                  Set<String> allowedMultipleVersions,
                                  EnumSet<ProcessType> supportedProcessTypes);
}
