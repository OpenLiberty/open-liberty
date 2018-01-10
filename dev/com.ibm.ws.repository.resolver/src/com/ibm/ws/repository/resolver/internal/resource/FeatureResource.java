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

package com.ibm.ws.repository.resolver.internal.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolveContext;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.VersionUtility;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.resolver.ProductRequirementInformation;
import com.ibm.ws.repository.resolver.internal.LibertyVersion;
import com.ibm.ws.repository.resolver.internal.LibertyVersionRange;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.namespace.ProductNamespace;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * <p>
 * This is a resource for a feature. It will give a {@link Capability} for the feature it provides and have a {@link Requirement} on the product that it applies to and each of the
 * APARs it requires.
 * </p>
 * <p>
 * There is no public constructor for this class, instead there are two factory method that can be called. If it is being constructed from an installed feature on the file system
 * then the feature manifest should be parsed into an {@link ProvisioningFeatureDefinition} object and an instance created through
 * {@link #createInstance(ProvisioningFeatureDefinition)}. If the feature is in massive then the {@link EsaResource} representation of it should be passed to
 * {@link #createInstance(EsaResource)} </p>
 * <p>The ordering of instances of these classes determined as to which would be the "best" resource to install being put first in a list. This is determined by the following
 * rules:</p>
 * <ol>
 * <li>Installed resources come before resources in the repository</li>
 * <li>Symbolic name</li>
 * <li>Higher versions of a feature come before lower ones</li>
 * <li>The applies to will then be processed using the rules:
 * <ol type="i">
 * <li>A higher fixed version of the applies to comes before a fixed lower version (8.5.5.6 is picked before 8.5.5.5)</li>
 * <li>A fixed version of the applies to for comes before a version range (8.5.5.6 is picked before 8.5.5.6+)</li>
 * <li>A version range with a higher minimum version comes before a version range with a lower minimum version (8.5.5.6+ is picked before 8.5.5.5+)</li>
 * </ol>
 * <p><b>Note:</b> These comparisons on the applies to version will only take the first applies to value defined in {@link ProductRequirement#getProductInformation()}. This is
 * because a) we only ever put one thing in there so it will work b) it's hard to do anything else. Lets consider a few scenarios to see why this is hard.</p>
 * <p>The obvious implementation choice is to compare the versions when the product ID is the same so if you had resource1 with applies to
 * <code>"a; productVersion=2.0.0.0, b; productVersion=2.0.0.0"</code> and resource2 with applies to <code>"a; productVersion=1.0.0.0"</code> then we could just compare the
 * versions on product "a" and say that <code>resource1.compareTo(resource2) &lt; 0</code>. Imagine though there was resource3 with applies to
 * <code>"b; productVersion=3.0.0.0"</code>, again using the above rules we'd compare the versions on "b" and see that <code>resource3.compareTo(resource1) &lt; 0</code>. The
 * transitive part of the comparable interface states that if <code>resource3.compareTo(resource1) &gt; 0 && resource1.compareTo(resource2) &gt; 0</code> then it implies that
 * <code>resource3.compareTo(resource2) &gt; 0</code> but there is no way to achieve this by just looking at the applies to on resource2 and resource3.</p>
 * <p>Of course you could just say we should only compare if the set of products that a resource applies to is the same but this doesn't work either. By doing this our 3 resources
 * used in the previous example would become: <code>resource1.compareTo(resource2) == resource1.compareTo(resource3) == 0</code>. Let's now introduce resource4 with applies to
 * <code>"a; productVersion=4.0.0.0"</code> as with the other resources <code>resource1.compareTo(resource4) == 0</code> which but as resource4 and resource2 have the same products
 * they apply to if we were to just to perform a comparison when the products are the same then <code>resource4.compareTo(resource2) &lt; 0</code> which is inconsistent with the
 * comparisons with resource1 which would have implied this should return 0.</p>
 * <p>Finally you would think that we could use the context of the resolution to get a better result where we know the product ID we are resolving against. However, just as an
 * applies to string may list more than one product it is also possible within Liberty to have more than one product installed. If this were the case then it is possible to
 * construct the above resources that would resolve against the various installed products and still end up with inconsistent results. For instance the Liberty for z/OS product
 * actually comprises of two product IDs that are both installed <code>com.ibm.websphere.appserver.zos</code> and <code>com.ibm.websphere.appserver</code>.</p>
 * <p>The easiest approach therefore is the one taken by the class which is to compare the version on the first entry in the applies to list, irrespective of whether or not it is
 * for the same product ID. This approach is fine for all of the resources currently in use in the repository and we can revisit if this ever changes.</p>
 * </li>
 * </ol>
 * <p>Note: this class has a natural ordering that is inconsistent with equals however this is only due to the fact that the resource is sorted by its location, version,
 * symbolic name and applies to in reality these three things should uniquely identify a resource and duplicates should not be added to the system with the same value for these
 * fields (although
 * testing equality purely on them would break the contract for {@link Resource#equals(Object)}. Comparing these objects will make the highest priority object for installation be
 * less than a lower priority object in order to be most useful when used in a {@link ResolveContext#findProviders(Requirement)} method invocation.</p>
 */
public class FeatureResource extends ResourceImpl implements Comparable<FeatureResource> {

    /** The symbolic name of the feature provided by this resource */
    private final String symbolicName;
    /** The version of the feature provided by this resource */
    private final Version version;
    private final boolean isAutoFeatureThatShouldBeInstalledWhenSatisfied;

    /**
     * Create an instance of this class for a feature that is installed in the file system.
     *
     * @param feature The feature installed
     * @return The instance
     */
    public static FeatureResource createInstance(ProvisioningFeatureDefinition feature) {
        // Use null as the requirement list as this feature is already installed so must be satisfied, we aren't trying to resolve the installed product
        String shortName = feature.getIbmShortName();
        String lowerCaseShortName = (shortName != null) ? shortName.toLowerCase() : null;
        String provisionCapability = feature.getHeader("IBM-Provision-Capability");
        boolean isAutoFeature = provisionCapability != null && !provisionCapability.isEmpty();
        boolean isAutoFeatureToInstallWhenSatisfied = false;
        if (isAutoFeature) {
            String installPolicy = feature.getHeader("IBM-Install-Policy");
            isAutoFeatureToInstallWhenSatisfied = "when-satisfied".equals(installPolicy);
        }
        return new FeatureResource(feature.getSymbolicName(), shortName, lowerCaseShortName, feature.getVersion(), null, LOCATION_INSTALL, null, isAutoFeatureToInstallWhenSatisfied);
    }

    /**
     * Create an instance of this class for a feature that is in Massive.
     *
     * @param featureAsset The feature in massive
     * @return The instance
     */
    public static FeatureResource createInstance(EsaResource esaResource) {
        List<Requirement> requirements = new ArrayList<Requirement>();

        /*
         * Put the requirement onto the product first, this way this is the first requirement we find is on the product so if this feature isn't applicable then we report that
         * rather than the dependent features. See defect 129171 for more details.
         */
        String appliesTo = esaResource.getAppliesTo();
        if (appliesTo != null && !appliesTo.isEmpty()) {
            requirements.add(new ProductRequirement(appliesTo));
        }

        // And also have one requirement for each of the required features and required fixes
        Collection<String> requiredFeatures = esaResource.getRequireFeature();
        if (requiredFeatures != null) {
            for (String requiredFeature : requiredFeatures) {
                requirements.add(new InstallableEntityRequirement(requiredFeature, InstallableEntityIdentityConstants.TYPE_FEATURE));
            }
        }
        Collection<String> requiredFixes = esaResource.getRequireFix();
        if (requiredFixes != null) {
            for (String requiredFix : requiredFixes) {
                requirements.add(new InstallableEntityRequirement(requiredFix, InstallableEntityIdentityConstants.TYPE_IFIX));
            }
        }

        // If this is an auto-feature it will also require it's provision-capability to be met
        String provisionCapability = esaResource.getProvisionCapability();
        boolean isAutoFeature = provisionCapability != null && !provisionCapability.isEmpty();
        if (isAutoFeature) {
            /*
             * The IBM-Provision-Capability header uses the same syntax as the OSGi require-capability syntax and can therefore be turned straight into a requirement using an Aries
             * util
             */
            List<GenericMetadata> requirementMetadata = ManifestHeaderProcessor.parseRequirementString(provisionCapability);
            for (GenericMetadata genericMetadata : requirementMetadata) {
                Requirement requirement = new GenericMetadataRequirement(genericMetadata);
                requirement.getDirectives().put(IdentityNamespace.REQUIREMENT_CLASSIFIER_DIRECTIVE, InstallableEntityIdentityConstants.CLASSIFIER_AUTO);
                requirements.add(requirement);
            }
        }

        boolean isAutoInstallable = isAutoFeature ? InstallPolicy.WHEN_SATISFIED.equals(esaResource.getInstallPolicy()) : false;

        return new FeatureResource(esaResource.getProvideFeature(), esaResource.getShortName(), esaResource.getLowerCaseShortName(), VersionUtility.stringToVersion(esaResource.getVersion()), requirements, LOCATION_REPOSITORY, esaResource, isAutoInstallable);
    }

    private FeatureResource(String symbolicName, String shortName, String lowerCaseShortName, Version version, List<Requirement> requirements, String location,
                            RepositoryResource massiveResource, boolean isAutoFeatureThatShouldBeInstalledWhenSatisfied) {
        super(createCapabilities(symbolicName, shortName, lowerCaseShortName, version), requirements, location, massiveResource);
        this.symbolicName = symbolicName;
        this.version = version;
        this.isAutoFeatureThatShouldBeInstalledWhenSatisfied = isAutoFeatureThatShouldBeInstalledWhenSatisfied;
    }

    /**
     * Creates a single capability for the feature with the supplied details
     *
     * @param symbolicName The symbolic name of the feature
     * @param shortName The short name of the feature, may be <code>null</code>
     * @param lowerCaseShortName The lower case version of the short name, may be <code>null</code>
     * @param version The version for the feature
     * @return The list of the single capability for this feature
     */
    private static List<Capability> createCapabilities(String symbolicName, String shortName, String lowerCaseShortName, Version version) {
        List<Capability> capabilities = new ArrayList<Capability>();

        // Just a single capability for the feature
        capabilities.add(new InstallableEntityCapability(symbolicName, shortName, lowerCaseShortName, version, InstallableEntityIdentityConstants.TYPE_FEATURE));
        return capabilities;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(FeatureResource o) {
        /*
         * The key thing we are trying to do here is order the elements as per the JavaDoc for this class. We want the best pick for the resolver to come first in a list so it is
         * lower, this means the return values are:
         *
         * -1 - this is lower than other so this will be picked first by the resolver
         * 0 - the two elements are the same and which one the resolver picks will be random
         * 1 - this is higher than other so other will be picked first by the resolver
         */

        // Installed resources always beat repo ones
        int locationCheck = compareLocation(o);
        if (locationCheck != 0) {
            return locationCheck;
        }

        // Now see if it is the same feature
        int featureCheck = this.symbolicName.compareTo(o.symbolicName);
        if (featureCheck != 0) {
            return featureCheck;
        }

        // Now do the version, we want the highest version to have the lowest index so compare the other way around
        int versionCheck = o.version.compareTo(this.version);
        if (versionCheck != 0) {
            return versionCheck;
        }

        // Have two features that are the same name, version and location - final check - what do they apply to?
        int appliesToCheck = compareAppliesTo(o);
        if (appliesToCheck != 0) {
            return appliesToCheck;
        }

        // They are the same!
        return 0;
    }

    /**
     * <p>This compares the applies to values on two feature resources.</p>
     * <p>We know for a FeatureResource that there will be zero or one requirements in the {@link ProductNamespace#PRODUCT_NAMESPACE} and we then compare the first
     * {@link ProductRequirement#getProductInformation()} instance.</p>
     *
     * @param o
     * @return
     */
    private int compareAppliesTo(FeatureResource o) {
        List<Requirement> thisRequirements = this.getRequirements(ProductNamespace.PRODUCT_NAMESPACE);
        List<Requirement> otherRequirements = o.getRequirements(ProductNamespace.PRODUCT_NAMESPACE);

        // Do we have requirements here?
        if (thisRequirements.size() == 0) {
            if (otherRequirements.size() > 0) {
                // The other element has a dependency on a product but we don't so want to pick that one first so say this one is higher
                return 1;
            } else {
                return 0;
            }
        }
        if (otherRequirements.size() == 0) {
            // This has requirements but other doesn't so pick this one
            return -1;
        }

        // They both have some product information. As discussed in the JavaDoc
        VersionInformation thisVersionInformation = getRequirementVersionInformation(thisRequirements);
        VersionInformation otherVersionInformation = getRequirementVersionInformation(otherRequirements);
        if (thisVersionInformation == null) {
            if (otherVersionInformation != null) {
                // This has no version but other does so put other first in the list
                return 1;
            } else {
                // Neither have version information so are the same
                return 0;
            }
        }

        if (otherVersionInformation == null) {
            // This has version information but other does not so put this first in the list
            return -1;
        }

        // Both have versions, which one is higher, ranges are always higher than exacts (i.e. picked last)
        // Remember - Liberty versions are either exact [8.5.5.6,8.5.5.6] or an unbounded range 8.5.5.6+
        if (thisVersionInformation.maximumVersion != null) {
            // This is exact
            if (otherVersionInformation.maximumVersion != null) {
                /*
                 * Both are exact so check which has a different minimum version - do reverse (i.e. other first) as we want this to be ordered according to what is best for the
                 * resolver
                 */
                return otherVersionInformation.minimumVersion.compareTo(thisVersionInformation.minimumVersion);
            } else {
                // This is a exact and the other is a range so put other later in the list
                return -1;
            }
        } else {
            // This is a range
            if (otherVersionInformation.maximumVersion != null) {
                // This is a range and and the other is exact so put this later in the list
                return 1;
            } else {
                /*
                 * This is a range and other is a range so work out which has the higher minimum - do reverse (i.e. other first) as we want this to be ordered according to what is
                 * best for the resolver
                 */
                return otherVersionInformation.minimumVersion.compareTo(thisVersionInformation.minimumVersion);
            }
        }
    }

    /**
     * Returns the information from the first {@link ProductRequirementInformation} in the {@link ProductRequirement#getProductInformation()} list.
     *
     * @param productRequirements List of {@link ProductRequirement}s, must have only one entry.
     * @return The version information for the first product requirement information or <code>null</code> if there isn't one
     */
    private VersionInformation getRequirementVersionInformation(List<Requirement> productRequirements) {
        ProductRequirement requirement = (ProductRequirement) productRequirements.get(0);
        List<ProductRequirementInformation> productInformations = requirement.getProductInformation();
        if (productInformations == null || productInformations.size() == 0) {
            // I don't think this can happen but be defensive
            return null;
        }
        ProductRequirementInformation productInformation = productInformations.get(0);
        LibertyVersionRange versionRange = productInformation.versionRange;
        LibertyVersion minimumVersion = null;
        LibertyVersion maximumVersion = null;
        if (versionRange != null) {
            minimumVersion = versionRange.getMinVersion();
            maximumVersion = versionRange.getMaxVersion();
            return new VersionInformation(minimumVersion, maximumVersion);
        } else {
            return null;
        }
    }

    /**
     * Simple data class for holding information about the version of a product in the applies to field.
     */
    private static class VersionInformation {
        private final LibertyVersion minimumVersion;
        private final LibertyVersion maximumVersion;

        /**
         * @param minimumVersion
         * @param maximumVersion
         */
        public VersionInformation(LibertyVersion minimumVersion, LibertyVersion maximumVersion) {
            super();
            this.minimumVersion = minimumVersion;
            this.maximumVersion = maximumVersion;
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "FeatureResource [symbolicName=" + symbolicName + ", version=" + version + ", location=" + location + "]";
    }

    public boolean isAutoFeatureThatShouldBeInstalledWhenSatisfied() {
        return isAutoFeatureThatShouldBeInstalledWhenSatisfied;
    }

}
