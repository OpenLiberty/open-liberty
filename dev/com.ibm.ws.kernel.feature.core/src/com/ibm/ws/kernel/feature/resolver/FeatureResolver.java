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
import java.util.LinkedHashMap;
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
         * Tell if any errors are recorded in the result.
         *
         * @see #getMissingRequested()
         * @see #getUnlabelledConstituents()
         * @see #getNonPublicRoots()
         * @see #getWrongProcessTypes()
         * @see #getConflicts()
         *
         * @return True or false telling if any errors are recorded.
         */
        boolean hasErrors();

        /**
         * Answer the names of requested features which are not
         * present in the feature repository.
         *
         * @return The names of missing requested features.
         */
        Set<String> getMissingRequested();

        /**
         * Answer a table of features which have missing constituents.
         * Keys are feature symbolic names. Values are sets of resource
         * locations.
         *
         * @return A table of missing feature constituents.
         */
        Map<String, Set<String>> getUnlabelledConstituents();

        /**
         * Answer a table of missing constituent versions.
         *
         * Keys are enclosing feature symbolic name, resource locations,
         * and constituent feature symbolic names.
         *
         * These do not cause failures: Single missing versions are ignored.
         *
         * @return A table of missing constituent versions.
         */
        Map<String, Map<String, Set<String>>> getMissingConstituentVersions();

        /**
         * Answer the names of any requested features which are not public.
         *
         * @return The names of any requested non-public features.
         */
        Set<String> getNonPublicRoots();

        /**
         * Answer information about any dependency feature, either requested
         * or a required dependency feature, which does not have a supported
         * process type.
         *
         * @return Information about features which have the wrong process
         *         type.
         */
        Map<String, ResolutionChain> getWrongProcessTypes();

        //

        /**
         * Tell how many new conflicts were recorded since these
         * results were created.
         *
         * @return The number of newly recorded conflicts.
         */
        int getNumNewConflicts();

        /**
         * Answer the base names of features which have conflicts and
         * which were recorded since these results were created.
         *
         * @return The base names of newly recorded features which have
         *         conflicts.
         */
        Set<String> getNewConflicts();

        /**
         * Tell if a specified base name has a conflict since the results
         * were created.
         *
         * @param baseName The base name of a feature which is to be tested.
         *
         * @return True or false telling if any new conflicts were recorded
         *         for the feature.
         */
        boolean isNewlyConflicted(String baseName);

        /**
         * The conflicting chains that resulted from a resolution operation. The key
         * is the base name of the feature that has conflicting versions required.
         * The value is a collection of dependency {@link ResolutionChain}s that transitively
         * lead to the conflicting versions of the feature.
         */
        Map<String, Collection<ResolutionChain>> getConflicts();

        /**
         * Tell how many conflicts this result contains.
         *
         * @return The count of conflicts in this result.
         */
        int getNumConflicts();

        /**
         * Tell if any conflicts are present for a specified base feature name.
         *
         * If a base name is present in conflict storage, at least one conflict
         * will be stored for that base name.
         *
         * @param baseName A base feature name.
         *
         * @return True or false telling if there are any conflicts for the base
         *         feature name.
         */
        boolean isConflicted(String baseName);

        //

        /**
         * Answer the names of resolved features.
         *
         * @return The names of resolved features.
         */
        Set<String> getResolvedFeatures();

        /**
         * Answer the resolved features.
         *
         * @return The resolved features.
         */
        OrderedFeatures getResolved();

        /**
         * @return
         */
        Map<String, Set<String>> getUnusableConstituents();
    }

    /**
     * A dependency chain of feature requirements that leads to
     * a singleton feature and a list of candidates.
     */
    public static interface ResolutionChain {
        List<String> getResolutionPath();

        List<String> getCandidates();

        String getPreferredCandidate();

        boolean isCandidate(String candidateSymbolicName);

        String getBaseName();

        Version getPreferredVersion();

        String getResolvedSymbolicName();

        ResolutionChain collapse(String candidate);
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
     * @param repository the feature repository to use
     * @param kernelFeatures the set of kernel features to use for auto-feature processing
     * @param rootFeatures the root features to resolve
     * @param preResolved the set of already resolved features to base the resolution delta off of
     * @param allowedMultipleVersions the set that includes features that effectively ignore singletons (null means none, empty set means all)
     * @param supportedProcessTypes the supported process types to allow to be resolved
     *
     * @return the resolution result
     */
    Result resolveFeatures(Repository repository,
                           Collection<ProvisioningFeatureDefinition> kernelFeatures,
                           Collection<String> rootFeatures,
                           Set<String> preResolved,
                           Set<String> allowedMultipleVersions,
                           EnumSet<ProcessType> supportedProcessTypes);

    // TODO: We should not need to use a LinkedHashMap,
    // however, auto-features appear to be very sensitive
    // to their installation order.

    /** Simple class synonym. This rather reduces code volume. */
    static class OrderedFeatures extends LinkedHashMap<String, ProvisioningFeatureDefinition> {
        private static final long serialVersionUID = 1L;

        public OrderedFeatures() {
            super();
        }

        public OrderedFeatures(int size) {
            super(size);
        }

        public OrderedFeatures(OrderedFeatures other) {
            this(other.size());
            putAll(other);
        }

        public OrderedFeatures copy() {
            return new OrderedFeatures(this);
        }
    }
}
