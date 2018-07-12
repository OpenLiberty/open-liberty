/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resolver;

import java.util.Collection;
import java.util.HashSet;

import org.osgi.service.resolver.ResolutionException;

import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resolver.internal.LibertyVersion;
import com.ibm.ws.repository.resolver.internal.LibertyVersionRange;
import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * This exception is thrown when a resolution fails.
 */
public class RepositoryResolutionException extends RepositoryException {

    private static final long serialVersionUID = 4270023429850109361L;
    private final Collection<String> topLevelFeaturesNotResolved;
    private final Collection<String> allRequirementsNotFound;
    private final Collection<ProductRequirementInformation> missingProductInformation;
    private final Collection<MissingRequirement> allRequirementsResourcesNotFound;

    /**
     * @param cause
     * @param topLevelFeaturesNotResolved
     * @param allRequirementsNotFound
     * @param missingProductInformation all the product information requirements that could not be found. Can be empty but must not be <code>null</code>
     * @param allRequirementsResourcesNotFound The {@link MissingRequirement} objects that were not found. Must not be <code>null</code>.
     */
    public RepositoryResolutionException(ResolutionException cause, Collection<String> topLevelFeaturesNotResolved, Collection<String> allRequirementsNotFound,
                                         Collection<ProductRequirementInformation> missingProductInformation, Collection<MissingRequirement> allRequirementsResourcesNotFound) {
        super(cause);
        this.topLevelFeaturesNotResolved = topLevelFeaturesNotResolved;
        this.allRequirementsNotFound = allRequirementsNotFound;
        this.missingProductInformation = missingProductInformation;
        this.allRequirementsResourcesNotFound = allRequirementsResourcesNotFound;
    }

    /**
     * Returns a collection of top level feature names that were not resolved.
     *
     * @return
     */
    public Collection<String> getTopLevelFeaturesNotResolved() {
        return topLevelFeaturesNotResolved;
    }

    /**
     * Returns a collection of requirements that were not found during the resolution process.
     *
     * @return
     * @deprecated use {@link #getAllRequirementsResourcesNotFound()} instead as this includes information about the resource that is held the requirement
     */
    @Deprecated
    public Collection<String> getAllRequirementsNotFound() {
        return allRequirementsNotFound;
    }

    /**
     * Returns a collection of requirements that were not found during the resolution process including the resource that owns the requirement.
     *
     * @return the resulting collection
     */
    public Collection<MissingRequirement> getAllRequirementsResourcesNotFound() {
        return allRequirementsResourcesNotFound;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.repository.exceptions.RepositoryException#getCause()
     */
    @Override
    public ResolutionException getCause() {
        // You can't create one of these without having the cause being a ResolutionException so this should be a safe cast
        return (ResolutionException) super.getCause();
    }

    /**
     * This will iterate through the products that couldn't be found as supplied by {@link #getMissingProducts()} and look for the minimum version that was searched for. It will
     * limit it to products that match the supplied <code>productId</code>, major, minor and micro <code>version</code>, and <code>edition</code>. Note that if the minimum version
     * on a {@link ProductRequirementInformation} is not in the form digit.digit.digit.digit then it will be ignored.
     *
     * @param productId The product ID to find the minimum missing version for or <code>null</code> to match to all products
     * @param version The version to find the minimum missing version for by matching the first three parts so if you supply "9.0.0.0" and this item applies to version "8.5.5.3"
     *            and "9.0.0.1" then "9.0.0.1" will be returned. Supply <code>null</code> to match all versions
     * @param edition The edition to find the minimum missing version for or <code>null</code> to match to all products
     * @return The minimum missing version or <code>null</code> if there were no relevant matches
     */
    public String getMinimumVersionForMissingProduct(String productId, String version, String edition) {
        Collection<LibertyVersionRange> filteredRanges = filterVersionRanges(productId, edition);
        Collection<LibertyVersion> minimumVersions = new HashSet<LibertyVersion>();
        for (LibertyVersionRange range : filteredRanges) {
            minimumVersions.add(range.getMinVersion());
        }
        Collection<LibertyVersion> filteredVersions = filterVersions(minimumVersions, version);
        LibertyVersion minimumVersion = null;
        for (LibertyVersion potentialNewMinVersion : filteredVersions) {
            if (minimumVersion == null || potentialNewMinVersion.compareTo(minimumVersion) < 0) {
                minimumVersion = potentialNewMinVersion;
            }
        }
        return minimumVersion == null ? null : minimumVersion.toString();
    }

    /**
     * This will filter the supplied versions to make sure they all have the same major, minor and micro parts as the supplied version. This may return the original collection.
     *
     * @param minimumVersions
     * @param version
     * @return The filtered versions, may be empty but won't be <code>null</code>
     */
    private Collection<LibertyVersion> filterVersions(Collection<LibertyVersion> minimumVersions, String version) {
        LibertyVersion versionToMatch = LibertyVersion.valueOf(version);
        if (versionToMatch == null) {
            return minimumVersions;
        }
        Collection<LibertyVersion> filteredVersions = new HashSet<LibertyVersion>();
        for (LibertyVersion versionToTest : minimumVersions) {
            if (versionToTest.matchesToMicros(versionToMatch)) {
                filteredVersions.add(versionToTest);
            }
        }
        return filteredVersions;
    }

    /**
     * This method will iterate through the missingProductInformation and returned a filtered collection of all the {@link ProductRequirementInformation#versionRange}s.
     *
     * @param productId The product ID to find the version for or <code>null</code> to match to all products
     * @param edition The edition to find the version for or <code>null</code> to match to all editions
     *
     * @return
     */
    private Collection<LibertyVersionRange> filterVersionRanges(String productId, String edition) {
        Collection<LibertyVersionRange> filteredRanges = new HashSet<LibertyVersionRange>();
        if (this.missingProductInformation != null) {
            for (ProductRequirementInformation product : this.missingProductInformation) {
                if (product.versionRange != null
                    && (productId == null || productId.equals(product.productId))
                    && (edition == null || product.editions == null || product.editions.isEmpty() || product.editions.contains(edition))) {
                    filteredRanges.add(product.versionRange);
                }
            }
        }
        return filteredRanges;
    }

    /**
     * <p>This will iterate through the products that couldn't be found as supplied by {@link #getMissingProducts()} and look for the maximum version that was searched for. It will
     * limit it to products that match the supplied <code>productId</code>. Note that if the maximum version on a {@link ProductRequirementInformation} is not in the form
     * digit.digit.digit.digit then it will be ignored. Also, if the maximum version is unbounded then this method will return <code>null</code>, this means that
     * {@link #getMinimumVersionForMissingProduct(String)} may return a non-null value at the same time as this method returning <code>null</code>. It is possible for a strange
     * quirk in that if the repository had the following versions in it:</p>
     *
     * <ul><li>8.5.5.2</li>
     * <li>8.5.5.4+</li></ul>
     *
     * <p>The {@link #getMinimumVersionForMissingProduct(String)} would return "8.5.5.2" and this method would return <code>null</code> implying a range from 8.5.5.2 to infinity
     * even though 8.5.5.3 is not supported, therefore {@link #getMissingProducts()} should be relied on for the most accurate information although in reality this situation would
     * indicate a fairly odd repository setup.</p>
     *
     * @param productId The product ID to find the maximum missing version for or <code>null</code> to match to all products
     * @param version The version to find the maximum missing version for by matching the first three parts so if you supply "8.5.5.2" and this item applies to version "8.5.5.3"
     *            and "9.0.0.1" then "8.5.5.3" will be returned. Supply <code>null</code> to match all versions
     * @param edition The edition to find the maximum missing version for or <code>null</code> to match to all products
     * @return The maximum missing version or <code>null</code> if there were no relevant matches or the maximum version is unbounded
     */
    public String getMaximumVersionForMissingProduct(String productId, String version, String edition) {
        Collection<LibertyVersionRange> filteredRanges = filterVersionRanges(productId, edition);
        Collection<LibertyVersion> maximumVersions = new HashSet<LibertyVersion>();
        for (LibertyVersionRange range : filteredRanges) {
            LibertyVersion maxVersion = range.getMaxVersion();
            if (maxVersion == null) {
                // unbounded
                return null;
            }
            maximumVersions.add(maxVersion);
        }
        Collection<LibertyVersion> filteredVersions = filterVersions(maximumVersions, version);
        LibertyVersion maximumVersion = null;
        for (LibertyVersion potentialNewMaxVersion : filteredVersions) {
            if (maximumVersion == null || potentialNewMaxVersion.compareTo(maximumVersion) > 0) {
                maximumVersion = potentialNewMaxVersion;
            }
        }
        return maximumVersion == null ? null : maximumVersion.toString();
    }

    /**
     * <p>This method will return a collection of {@link ProductRequirementInformation} objects that contain all of the products that we attempted to resolve but couldn't. This can
     * be taken
     * as a list of all of the products for which we have an instance of a required feature/sample but could not resolve the dependency on the product. There are some
     * nuances in displaying this information to a user:</p>
     * <ul>
     * <li>The product version could contain a range in the form 8.5.5.4+. It may be possible to have overlapping ranges so for instance there could be one entry at version
     * 8.5.5.4+, one at 8.5.5.5+ and one at 8.5.5.6.</li>
     * <li>The collection may contain products that have different base versions with different editions so you could have:
     * <table>
     * <thead><th>Version</th><th>Editions</th></thead>
     * <tbody><tr><td>8.5.5.2</td><td>Base, Developers, Express, ND, z/OS</td></tr>
     * <tr><td>8.5.5.3</td><td>Base, Developers, Express, ND, z/OS</td></tr>
     * <tr><td>8.5.5.4</td><td>Base, Developers, Express, ND, z/OS</td></tr>
     * <tr><td>9.0.0.0</td><td>Base, Developers, Express, ND, z/OS</td></tr>
     * <tr><td>2014.2.0.0</td><td>EARLY_ACCESS</td></tr></tbody>
     * </table>
     * </li>
     * </ul>
     *
     * @return The collection of missing products, may be empty but will not be <code>null</code>
     */
    public Collection<ProductRequirementInformation> getMissingProducts() {
        return this.missingProductInformation;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        for (String missing : getTopLevelFeaturesNotResolved()) {
            sb.append("Requirement not met: resource=").append(missing).append("\n");
        }
        return sb.toString();
    }

    /**
     * Simple pair tuple object to hold the requirement name (either a symbolic name or applies to) and the resource that held the requirement.
     */
    public static class MissingRequirement {
        /** The name of the requirement that was not found (either a feature symbolic name or product applies to), will not be <code>null</code>. */
        public final String requirementName;
        /** The resource that owned this requirement. Maybe <code>null</code> if there is no MassiveResource that owns the requirement. */
        public final RepositoryResource owningResource;

        /**
         * @param requirementName
         * @param owningResource
         */
        /* package-protected */ MissingRequirement(String requirementName, RepositoryResource owningResource) {
            super();
            this.requirementName = requirementName;
            this.owningResource = owningResource;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "MissingRequirement [requirementName=" + requirementName + ", owningResource=" + owningResource + "]";
        }

        /**
         * Gets the requirement name
         *
         * @return the requirementName
         */
        public String getRequirementName() {
            return requirementName;
        }

        /**
         * Gets the owning resource
         *
         * @return the owningResource
         */
        public RepositoryResource getOwningResource() {
            return owningResource;
        }
    }

}
