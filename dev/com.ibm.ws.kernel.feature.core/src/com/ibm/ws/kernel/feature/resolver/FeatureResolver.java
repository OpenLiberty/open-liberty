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
package com.ibm.ws.kernel.feature.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Version;

import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 * A resolver for liberty features. The resolver is responsible for determining a set of
 * features that must be installed in order to satisfy the feature requirements of an initial
 * set of root features.
 */
public interface FeatureResolver {
    /**
     * A repository that contains a set of known liberty features. The
     * repository is used by the resolver to lookup features based on feature
     * names.
     */
    public interface Repository {
        /**
         * Returns all known liberty features contained in this repository
         * which are considered to be auto features.
         *
         * @return a collection of auto features.
         */
        Collection<ProvisioningFeatureDefinition> getAutoFeatures();

        /**
         * Gets a feature based on the feature name. The feature name may
         * be the short name (if it is a public feature) or the full
         * symbolic name. The full symbolic name will include the version
         * (e.g. com.ibm.ws.something_1.0)
         *
         * @param featureName the feature name
         * @return the feature with the specified name or {@code null}.
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
        /**
         * The set of feature names that are required to resolve an initial set of root features.
         *
         * @return the feature names that are resolved
         */
        Set<String> getResolvedFeatures();

        /**
         * The required features that could not be found while trying to resolve root features.
         *
         * @return the missing features
         */
        Set<String> getMissing();

        /**
         * The set of root features must be public. This will return any that are not
         *
         * @return the non public root features
         */
        Set<String> getNonPublicRoots();

        /**
         * The conflicting chains that resulted from a resolution operation. The key
         * is the base name of the feature that has conflicting versions required.
         * The value is a collection of dependency {@link Chain}s that transitively
         * lead to the conflicting versions of the feature.
         *
         * @return
         */
        Map<String, Collection<Chain>> getConflicts();

        /**
         * Not really used yet
         *
         * @return
         */
        Map<String, Chain> getWrongProcessTypes();

        /**
         * A quick check to tell if there are any errors in the result.
         *
         * @return
         * @see #getMissing()
         * @see #getNonPublicRoots()
         * @see #getConflicts()
         * @see #getWrongProcessTypes()
         */
        boolean hasErrors();
    }

    /**
     * A dependency chain of feature requirements that lead a singleton feature and a list of candidates
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
            _chain = chain.isEmpty() ? Collections.<String> emptyList() : Collections.unmodifiableList(new ArrayList<String>(chain));
            _candidates = Collections.unmodifiableList(candidates);
            Version v;
            try {
                v = Version.parseVersion(preferredVersion);
            } catch (IllegalArgumentException e) {
                v = Version.emptyVersion;
            }
            _preferredVersion = v;
            _originalFeatureReq = originalFeatureReq;
        }

        /**
         * @return the dependency chain
         */
        public List<String> getChain() {
            return _chain;
        }

        /**
         * @return the tolerated candidates
         */
        public List<String> getCandidates() {
            return _candidates;
        }

        /**
         * @return the preferredVersion
         */
        public Version getPreferredVersion() {
            return _preferredVersion;
        }

        /**
         * @return the full feature requirement name
         */
        public String getFeatureRequirement() {
            return _originalFeatureReq;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            // print the chain first
            // always start with saying ROOT->
            builder.append("ROOT->");
            for (String featureName : _chain) {
                builder.append(featureName).append("->");
            }
            // print candidates
            builder.append(_candidates);
            // print preferred version
            builder.append(" ").append(_preferredVersion);
            return builder.toString();
        }
    }

    /**
     * Resolves a collection of root features against a repository.
     * This is a convenience method that effectively does the following:
     * <code>
     * resolveFeatures(repository, Collections.emptySet(), rootFeatures, preResolved, allowMultipleVersions);
     * </code>
     *
     * @param repository            the feature repository to use
     * @param rootFeatures          the root features to resolve
     * @param preResolved           the set of already resolved features to base the resolution delta off of
     * @param allowMultipleVersions a flag that allows multiple versions (this flag will effectively ignore singletons)
     * @return the resolution result
     */
    public Result resolveFeatures(Repository repository, Collection<String> rootFeatures, Set<String> preResolved, boolean allowMultipleVersions);

    /**
     * Resolves a collection of root features against a repository.
     * This is a convenience method that effectively does the following:
     * <code>
     * resolveFeatures(repository, kernelFeatures, rootFeatures, preResolved, allowMultipleVersions, EnumSet.allOf(ProcessType.class));
     * </code>
     *
     * @param repository            the feature repository to use
     * @param kernelFeatures        the set of kernel features to use for auto-feature processing
     * @param rootFeatures          the root features to resolve
     * @param preResolved           the set of already resolved features to base the resolution delta off of
     * @param allowMultipleVersions a flag that allows multiple versions (this flag will effectively ignore singletons)
     * @return the resolution result
     */
    public Result resolveFeatures(Repository repository, Collection<ProvisioningFeatureDefinition> kernelFeatures, Collection<String> rootFeatures, Set<String> preResolved,
                                  boolean allowMultipleVersions);

    /**
     * Resolves a collection of root features against a repository.
     *
     * @param repository            the feature repository to use
     * @param kernelFeatures        the set of kernel features to use for auto-feature processing
     * @param rootFeatures          the root features to resolve
     * @param preResolved           the set of already resolved features to base the resolution delta off of
     * @param allowMultipleVersions a flag that allows multiple versions (this flag will effectively ignore singletons)
     * @param supportedProcessTypes the supported process types to allow to be resolved
     * @return the resolution result
     */
    public Result resolveFeatures(Repository repository, Collection<ProvisioningFeatureDefinition> kernelFeatures, Collection<String> rootFeatures, Set<String> preResolved,
                                  boolean allowMultipleVersions,
                                  EnumSet<ProcessType> supportedProcessTypes);
}
