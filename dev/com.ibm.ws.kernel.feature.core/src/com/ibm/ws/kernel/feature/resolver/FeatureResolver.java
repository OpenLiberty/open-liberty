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
package com.ibm.ws.kernel.feature.resolver;

import java.util.Collection;
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
         */
        Map<String, Collection<Chain>> getConflicts();

        /**
         * Tell how many conflicts this result contains.
         *
         * @return The count of conflicts in this result.
         */
        int getNumConflicts();

        /**
         * Not really used yet.
         */
        Map<String, Chain> getWrongProcessTypes();

        /**
         * A quick check to tell if there are any errors in the result.
         *
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
    public static interface Chain {
        List<String> getChain();
        List<String> getCandidates();
        boolean hasCandidate(String candidate);
        Version getPreferredVersion();
        String getFeatureRequirement();
        Chain select(String candidate);
    }

    Result resolveFeatures(Repository repository,
                           Collection<String> rootFeatures,
                           Set<String> preResolved,
                           boolean allowMultipleVersions);

    Result resolveFeatures(Repository repository,
                           Collection<ProvisioningFeatureDefinition> kernelFeatures,
                           Collection<String> rootFeatures, Set<String> preResolved,
                           boolean allowMultipleVersions);

    Result resolveFeatures(Repository repository,
                           Collection<ProvisioningFeatureDefinition> kernelFeatures,
                           Collection<String> rootFeatures, Set<String> preResolved,
                           boolean allowMultipleVersions,
                           EnumSet<ProcessType> supportedProcessTypes);

    /**
     * Resolves a collection of root features against a repository.
     *
     * @param repository              the feature repository to use
     * @param kernelFeatures          the set of kernel features to use for auto-feature processing
     * @param rootFeatures            the root features to resolve
     * @param preResolved             the set of already resolved features to base the resolution delta off of
     * @param allowedMultipleVersions the set that includes features that effectively ignore singletons (null means none, empty set means all)
     * @param supportedProcessTypes   the supported process types to allow to be resolved
     *
     * @return the resolution result
     */
    Result resolveFeatures(Repository repository,
                           Collection<ProvisioningFeatureDefinition> kernelFeatures,
                           Collection<String> rootFeatures,
                           Set<String> preResolved,
                           Set<String> allowedMultipleVersions,
                           EnumSet<ProcessType> supportedProcessTypes);
}
