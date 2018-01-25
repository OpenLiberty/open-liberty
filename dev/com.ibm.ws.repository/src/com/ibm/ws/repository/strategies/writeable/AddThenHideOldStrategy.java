/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.strategies.writeable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.FilterPredicate;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryIllegalArgumentException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceValidationException;
import com.ibm.ws.repository.resources.ApplicableToProduct;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.AppliesToProcessor;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.writeable.ProductResourceWritable;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;
import com.ibm.ws.repository.resources.writeable.WebDisplayable;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;

/**
 * This class is used to ensure that when we add an asset only one asset remains visible on the website.
 * <p>
 * This is controlled by two concepts:
 * <ul>
 * <li>Matching - a matching resource refers to the same item. Only one of these should ever be in the repository, hidden or not.
 * <li>VanityURL - an item who's vanity URL is the same (but does not match) is a different version of the same resource.
 * </ul>
 * When we add an item with the same VanityURL we decide which is 'newer' (see below) and only the newer item should be visible.
 * Beta items are always hidden by non-beta items.
 * <p>
 * Determining which item is newer:
 * <ul>
 * <li> For Products
 * <ul>
 * <li> The product version is used to determine which product is newer
 * </ul>
 * <li> For non-Products
 * <ul>
 * <li> The appliesTo field is used to determine which resource is newer. The resource that applies to the highest
 * MINIMUM version is the newest. e.g. "8.5.5.5+" is is older than "8.5.5.6" even though "8.5.5.5+" would apply to "8.5.5.7".
 * <li> If we don't have a product version from the appliesTo information (or we have a tie) we look at the version field.
 * For this to work the resource version field needs to be an OSGI version e.g. "1.0.0" rather than "Version 1".
 * </ul>
 * </ul>
 */
public class AddThenHideOldStrategy extends AddThenDeleteStrategy {

    private static final Version4Digit MAX_VERSION = new Version4Digit(Integer.MAX_VALUE, 0, 0, "0");
    private static final Version4Digit MIN_VERSION = new Version4Digit(0, 0, 0, "0");

    /**
     * Delegate to super class for states
     */
    public AddThenHideOldStrategy() {}

    /**
     * Sets the desired state of the asset after uploading it
     *
     * @param desiredStateIfMatchingFound This is not used by this strategy but can be used by derived strategies
     * @param desiredStateIfNoMatchingFound Set the resource to this state after uploading. This behaviour can
     *            be changed by derived classes
     */
    public AddThenHideOldStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound) {
        super(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound, false);
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAsset(RepositoryResourceImpl newResource, List<RepositoryResourceImpl> matchingResources) throws RepositoryBackendException, RepositoryResourceException {

        // we haven't added the newResource yet so blank its id in case we have stale data
        newResource.resetId();

        State desired;
        if (matchingResources.size() != 0) {
            desired = calculateTargetState(matchingResources.get(0));
        } else {
            desired = _desiredStateIfNoMatchingFound;
        }

        boolean performHideOnOldResource = false;
        List<RepositoryResource> resourcesToHide = new ArrayList<RepositoryResource>();

        // Lock on the vanityURL
        String lockString = getVanityUrlLock(newResource.getVanityURL());
        synchronized (lockString) {

            // If the desired state is not published we will never hide any resources
            if (desired == State.PUBLISHED) {

                // If we adding a public beta feature make it visible (it will be hidden later
                // if there is a non-beta version before being uploaded)
                if (newResource instanceof EsaResourceImpl) {
                    EsaResourceImpl esa = (EsaResourceImpl) newResource;
                    if (isBeta(newResource) && Visibility.PUBLIC.equals(esa.getVisibility())) {
                        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
                    }
                }

                // get the resource(s) to hide (which may be the one we are adding)
                resourcesToHide = findResourcesToHide(newResource, matchingResources);

                // if the resource to hide is the resource being added make it hidden before upload.
                // If it is another resource set a flag so that it gets uploaded later as hidden.
                if (resourcesToHide.size() != 0) {

                    for (RepositoryResource loopResource : resourcesToHide) {

                        if (loopResource.getId() == null) {
                            // this is the resource we are adding (as it doesn't yet have an id) and we want it hidden
                            if (loopResource instanceof WebDisplayable) {
                                ((WebDisplayable) loopResource).setWebDisplayPolicy(DisplayPolicy.HIDDEN);
                            }
                        } else {
                            // there are resources other than the one we are adding to hide
                            performHideOnOldResource = true;
                        }
                    }
                }
            } // end if (desired == State.PUBLISHED)

            // upload the new resource (could be hidden or visible) ...
            super.uploadAsset(newResource, matchingResources);
            String newlyAddedAssetId = newResource.getId();

            // now hide any OTHER assets that need hiding as THIS one will have been hidden if necessary before upload.
            if (performHideOnOldResource) {
                for (RepositoryResource resourceToHide : resourcesToHide) {
                    if ((resourceToHide.getId()).equals(newlyAddedAssetId)) {
                        // leave the asset we have just added alone
                    } else {
                        // this is not the recently added resource so hide
                        if (resourceToHide instanceof WebDisplayable) {
                            ((WebDisplayable) resourceToHide).setWebDisplayPolicy(DisplayPolicy.HIDDEN);
                            RepositoryResourceWritable rrw = (RepositoryResourceWritable) resourceToHide;
                            // The desired stated is passed explicitly here, just in case a previous, failed,
                            // upload attempt has left a matching resource in the wrong state.  There should
                            // always be a matching resource, so the desiredStateIfNoMatchingFound (second parameter)
                            // should never be used.
                            rrw.uploadToMassive(new AddThenDeleteStrategy(rrw.getState(), State.DRAFT, true));
                        }
                    }

                }
            }
        }
    }

    /**
     * This method should return a resource for the caller to hide if it matches the new resource being added. this resource:
     * - has the same vanity URL as newResource
     * - is visible
     * - is published
     * - is not the resource that is or appliesTo a higher version
     * - when appliesTo versions are equal it returns one with the lower version (resource type dependent)
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    private List<RepositoryResource> findResourcesToHide(RepositoryResourceImpl newResource,
                                                         List<RepositoryResourceImpl> matchingResources) throws RepositoryBackendException, RepositoryResourceException {

        // build a list of matching ids
        List<String> matchingResourceIds = new ArrayList<String>();
        for (RepositoryResourceImpl r : matchingResources) {
            matchingResourceIds.add(r.getId());
        }

        String vanityURL = newResource.getVanityURL();
        RepositoryConnection repo = newResource.getRepositoryConnection();
        Collection<RepositoryResource> resourcesWithSameVanityURLs = repo.getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.VANITY_URL, vanityURL));

        List<RepositoryResource> resourcesToHide = new ArrayList<RepositoryResource>();
        RepositoryResource newestResource = newResource;

        // create list which excludes hidden and unpublished resources.  Also exclude matching resources as they will
        // be deleted so will not have to be hidden
        for (RepositoryResource resource : resourcesWithSameVanityURLs) {

            if (!isVisibleAndWebDisplayable(resource)) {
                continue;
            }
            if (!State.PUBLISHED.equals(((RepositoryResourceWritable) resource).getState())) {
                continue;
            }
            if (matchingResourceIds.contains(resource.getId())) {
                continue;
            }

            // newestResource is passed to method as parameter #1 as this the default return value
            // if there is no appliesTo / ProductVersion ie if we can't find which is the newer version we
            // default to leaving the most recently added visible.
            newestResource = getNewerResource(newestResource, resource);
            resourcesToHide.add(resource);
        }

        // remove the highest matching asset from the list so it won't get hidden
        // should only be one matching non-hidden resource.
        resourcesToHide.remove(newestResource);

        // add the newResource to the list if it wasn't the highest (ie eligible for hiding)
        if (newestResource != newResource) {
            resourcesToHide.add(newResource);
        }

        return resourcesToHide;
    }

    /**
     * Return the higher version of the resource or the first one if this cannot be determined. For Products
     * this is based off the ProductVersion and for everything else it is based of the appliesTo information.
     *
     * For resources that have the same appliesTo version the version field is used to determine which to return.
     *
     * If the higher version cannot be determined the first resource is returned.
     *
     * If one resource is beta and one non-beta the non-beta one is returned.
     *
     * @param res1 resource to compare
     * @param res2 resource to compare
     * @return RepositoryResource of the higher level, or res1 if the same level
     * @throws RepositoryResourceValidationException
     * @throws RepositoryIllegalArgumentException
     */
    private RepositoryResource getNewerResource(RepositoryResource res1, RepositoryResource res2) throws RepositoryResourceValidationException {

        // if one of the resources is beta and the other not, return the non-beta one
        RepositoryResource singleNonBetaResource = returnNonBetaResourceOrNull(res1, res2);
        if (singleNonBetaResource != null) {
            return singleNonBetaResource;
        }

        if (res1.getType() == ResourceType.INSTALL) {

            // have two String versions .. convert them into Version objects,checking that they are valid versions in the process
            Version4Digit res1Version = null;
            Version4Digit res2Version = null;

            try {
                res1Version = new Version4Digit(((ProductResourceWritable) res1).getProductVersion());
            } catch (IllegalArgumentException iae) {
                // the version was not a proper osgi version
                throw new RepositoryResourceValidationException("The product version was invalid: " + res1Version, res1.getId(), iae);
            }

            try {
                res2Version = new Version4Digit(((ProductResourceWritable) res2).getProductVersion());
            } catch (IllegalArgumentException iae) {
                // the version was not a proper osgi version
                throw new RepositoryResourceValidationException("The product version was invalid: " + res2Version, res2.getId(), iae);
            }

            if (res1Version.compareTo(res2Version) > 0) {
                return res1;
            } else {
                return res2;
            }

        } else if (res1.getType() == ResourceType.TOOL) {
            // tools don't have product versions or applies to so just return res1
            return res1;
        } else {
            return compareNonProductResourceAppliesTo(res1, res2);
        }
    }

    /**
     * Take in two resources. If one (only) is beta return the non beta one
     */
    private RepositoryResource returnNonBetaResourceOrNull(RepositoryResource res1, RepositoryResource res2) {
        if (isBeta(res1) && !isBeta(res2)) {
            return res2;
        } else if (!isBeta(res1) && isBeta(res2)) {
            return res1;
        } else {
            return null;
        }
    }

    private boolean isBeta(RepositoryResource res) {

        String version;
        String regex;
        if (res.getType() == ResourceType.INSTALL) {
            regex = AppliesToProcessor.BETA_REGEX;
            version = ((ProductResourceWritable) res).getProductVersion();
        } else if (res.getType() == ResourceType.TOOL) {
            return false; // no beta tools
        } else {
            version = ((ApplicableToProduct) res).getAppliesTo();
            regex = ".*productVersion=\"?" + AppliesToProcessor.BETA_REGEX;
        }

        if (version == null) {
            return false;
        } else {
            boolean matches = version.matches(regex);
            return matches;
        }
    }

    /**
     * This routine handles non product resources
     *
     * @param res1 - a non-product resource
     * @param res2 - a non-product resource
     * @return the newer resource
     */
    private RepositoryResource compareNonProductResourceAppliesTo(RepositoryResource res1, RepositoryResource res2) {

        // all types other than INSTALLS or TOOLS use appliesTo to determine which is the higher level
        String res1AppliesTo = ((ApplicableToProduct) res1).getAppliesTo();
        String res2AppliesTo = ((ApplicableToProduct) res2).getAppliesTo();

        // on the basis that we will work with the appliesTo of a resource, if we find one that has an applies to
        // and one that doesn't we will the one WITH the applies to is assumed to be newer (if both null we look
        // at the version field)
        if (res1AppliesTo == null && res2AppliesTo == null) {
            // if both appliesTo are null look at the versions
            return getNonProductResourceWithHigherVersion(res1, res2);

        } else if (res1AppliesTo == null || res2AppliesTo == null) {
            // if one of them is null we can't compare them so return res1
            return res1;
        }

        MinAndMaxVersion res1MinMax = getMinAndMaxAppliesToVersionFromAppliesTo(res1AppliesTo);
        MinAndMaxVersion res2MinMax = getMinAndMaxAppliesToVersionFromAppliesTo(res2AppliesTo);

        // compare the versions and return the resource that applies to the higher minimum version
        if (res1MinMax.min.compareTo(res2MinMax.min) > 0) {
            return res1;
        } else if (res1MinMax.min.compareTo(res2MinMax.min) == 0) {

            // if they apply to the same minimum version then select the one with the highest max versions
            if (res1MinMax.max.compareTo(res2MinMax.max) > 0) {
                return res1;
            } else if (res1MinMax.max.compareTo(res2MinMax.max) < 0) {
                return res2;
            } else {
                // if they are still the same decide on the version
                return getNonProductResourceWithHigherVersion(res1, res2);
            }

        } else {
            return res2;
        }
    }

    /**
     * Return the resource with the highest version for when the appliesTo versions are equal
     *
     * @param res1 resource to compare
     * @param res2 resource to compare
     * @return RepositoryResource with the higher version field
     */
    private RepositoryResource getNonProductResourceWithHigherVersion(RepositoryResource res1, RepositoryResource res2) {

        if (res1.getVersion() == null || res2.getVersion() == null) {
            return res1; // don't have two versions so can't compare
        }

        // have two String versions .. convert them into Version objects,checking that they are valid versions in the process
        Version4Digit res1Version = null;
        Version4Digit res2Version = null;
        try {
            res1Version = new Version4Digit(res1.getVersion());
            res2Version = new Version4Digit(res2.getVersion());
        } catch (IllegalArgumentException iae) {
            // at least one of the one or more of Versions is not a proper osgi
            // version so we cannot compare the version fields.  Just return res1.
            return res1;
        }

        if (res1Version.compareTo(res2Version) > 0) {
            return res1;
        } else {
            return res2;
        }
    }

    /**
     * Parse an appliesTo to get the lowest and highest version that this asset applies to and
     * return an object describing this.
     *
     * @param apliesTo the appliesTo String
     * @return MinAndMaxVersion object describing the range of levels supported
     */
    private MinAndMaxVersion getMinAndMaxAppliesToVersionFromAppliesTo(String appliesTo) {

        List<AppliesToFilterInfo> res1Filters = AppliesToProcessor.parseAppliesToHeader(appliesTo);
        Version4Digit highestVersion = null;
        Version4Digit lowestVersion = null;
        for (AppliesToFilterInfo f : res1Filters) {
            Version4Digit vHigh = (f.getMaxVersion() == null) ? MAX_VERSION : new Version4Digit(f.getMaxVersion().getValue());
            Version4Digit vLow = (f.getMinVersion() == null) ? MIN_VERSION : new Version4Digit(f.getMinVersion().getValue());
            if (highestVersion == null || vHigh.compareTo(highestVersion) > 0) {
                highestVersion = vHigh;
                lowestVersion = vLow;
            } else if (vHigh.compareTo(highestVersion) == 0) {
                if (lowestVersion == null || vLow.compareTo(lowestVersion) > 0) {
                    highestVersion = vHigh;
                    lowestVersion = vLow;
                }
            }
        }
        return new MinAndMaxVersion(lowestVersion, highestVersion);
    }

    /**
     * Returns <code>true</code> if the resource will be visible to the website.
     *
     * @param resource
     * @return
     */
    private boolean isVisibleAndWebDisplayable(RepositoryResource resource) {
        if (resource instanceof WebDisplayable) {
            DisplayPolicy displayPolicy = ((WebDisplayable) resource).getWebDisplayPolicy();
            return displayPolicy == DisplayPolicy.VISIBLE || displayPolicy == null;
        } else {
            return false;
        }
    }

    /**
     * Private class to return min and max versions
     */
    public static class MinAndMaxVersion {
        public Version4Digit min;
        public Version4Digit max;

        public MinAndMaxVersion(Version4Digit minVersion, Version4Digit maxVersion) {
            min = minVersion;
            max = maxVersion;
        }

        @Override
        public String toString() {
            return "(min=" + min + ", max=" + max + ")";

        }
    }
}
