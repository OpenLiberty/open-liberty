/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resources.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.common.enums.FilterPredicate;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.exceptions.RepositoryResourceNoConnectionException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.JavaSEVersionRequirements;
import com.ibm.ws.repository.transport.model.RequireFeatureWithTolerates;
import com.ibm.ws.repository.transport.model.WlpInformation;

public class EsaResourceImpl extends RepositoryResourceImpl implements EsaResourceWritable {

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    /**
     * Constructor - requires connection info
     *
     */
    public EsaResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    public EsaResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        super(repoConnection, ass);

        if (ass == null) {
            setType(ResourceType.FEATURE);
            setDownloadPolicy(DownloadPolicy.INSTALLER);
            setInstallPolicy(InstallPolicy.MANUAL);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setProvideFeature(String feature) {
        if (_asset.getWlpInformation().getProvideFeature() != null) {
            _asset.getWlpInformation().setProvideFeature(null);
        }
        _asset.getWlpInformation().addProvideFeature(feature);
    }

    /** {@inheritDoc} */
    @Override
    public String getProvideFeature() {
        Collection<String> provideFeatures = _asset.getWlpInformation().getProvideFeature();
        if (provideFeatures == null || provideFeatures.isEmpty()) {
            return null;
        } else {
            return provideFeatures.iterator().next();
        }
    }

    /*
     * Uses the required features information to calculate a list of queries
     * stored as Strings that can be searched upon later
     * Input the list of required features information to convert into the query
     * Returns the list of queries (Strings)
     */
    private Collection<String> createEnablesQuery() {
        Collection<String> query = null;
        Collection<String> requiredFeatures = getRequireFeature();
        if (requiredFeatures != null) {
            query = new ArrayList<String>();
            for (String required : requiredFeatures) {
                String temp = "wlpInformation.provideFeature=" + required;

                String version = findVersion();
                if (version != null) {
                    temp += "&wlpInformation.appliesToFilterInfo.minVersion.value=";
                    temp += version;
                }

                temp += "&type=";
                temp += getType().getValue(); // get the long name of the Type
                query.add(temp);
            }
        }
        return query;
    }

    /**
     * Uses the filter information to return the first version number
     *
     * @return the first version number
     */
    private String findVersion() {
        WlpInformation wlp = _asset.getWlpInformation();
        if (wlp == null) {
            return null;
        }

        Collection<AppliesToFilterInfo> filterInfo = wlp.getAppliesToFilterInfo();
        if (filterInfo == null) {
            return null;
        }

        for (AppliesToFilterInfo filter : filterInfo) {
            if (filter.getMinVersion() != null) {
                return filter.getMinVersion().getValue();
            }
        }
        return null;
    }

    /*
     * Calculates the Enabled By query (required by features)
     */
    private Collection<String> createEnabledByQuery() {
        Collection<String> query = new ArrayList<String>();
        String temp = "";

        //generate queries
        temp = "wlpInformation.requireFeature=";
        temp += getProvideFeature();
        String version = findVersion();
        if (version != null) {
            temp += "&wlpInformation.appliesToFilterInfo.minVersion.value=";
            temp += version;
        }

        temp += "&type=";
        temp += getType().getValue();

        query.add(temp);
        return query;
    }

    /**
     * @return the query for Superseded By or null if this feature
     *         does not declare itself to be superseded by anything.
     */
    private Collection<String> createSupersededByQuery() {
        Collection<String> supersededBy = _asset.getWlpInformation().getSupersededBy();

        Collection<String> query = null;
        if (supersededBy != null) { //if there are no queries to add
            query = new ArrayList<String>();
            for (String feature : supersededBy) {
                StringBuilder b = new StringBuilder();
                b.append("wlpInformation.shortName=");
                b.append(feature);
                b.append("&wlpInformation.appliesToFilterInfo.minVersion.value=");
                String version = findVersion();
                if (version != null) {
                    b.append(version);
                    b.append("&type=com.ibm.websphere.Feature");
                }
                query.add(b.toString());
            }
        }

        return query;
    }

    /**
     * @return the Supersedes query. Note that this query will always be
     *         set to something because the website can't tell if this features
     *         supersedes anything without running the query. So this method
     *         won't ever return null.
     */
    private Collection<String> createSupersedesQuery() {
        String shortName = _asset.getWlpInformation().getShortName();
        if (shortName != null) {
            StringBuilder b = new StringBuilder();
            b.append("wlpInformation.supersededBy=");
            b.append(shortName);
            String version = findVersion();
            if (version != null) {
                b.append("&wlpInformation.appliesToFilterInfo.minVersion.value=");
                b.append(version);
            }
            return Arrays.asList(new String[] { b.toString() });
        } else {
            // if we get here then our shortname is null so we can't create a
            // query that refers to it.
            return null;
        }
    }

    /**
     * This generates the string that should be displayed on the website to indicate
     * the supported Java versions. The requirements come from the bundle manifests.
     * The mapping between the two is non-obvious, as it is the intersection between
     * the Java EE requirement and the versions of Java that Liberty supports.
     */
    private void addVersionDisplayString() {
        WlpInformation wlp = _asset.getWlpInformation();
        JavaSEVersionRequirements reqs = wlp.getJavaSEVersionRequirements();
        if (reqs == null) {
            return;
        }

        String minVersion = reqs.getMinVersion();

        // Null means no requirements specified which is fine
        if (minVersion == null) {
            return;
        }

        String minJava17 = "Java SE 17";
        String minJava11 = "Java SE 11, Java SE 17";
        String minJava8 = "Java SE 8, Java SE 11, Java SE 17";

        // The min version should have been validated when the ESA was constructed
        // so checking for the version string should be safe
        if (minVersion.equals("1.6.0") || minVersion.equals("1.7.0") || minVersion.equals("1.8.0")) {
            reqs.setVersionDisplayString(minJava8);
            return;
        }
        if (minVersion.startsWith("9.") ||
            minVersion.startsWith("10.") ||
            minVersion.startsWith("11.")) {
            // If a feature requires a min of Java 9/10/11, state Java 11 is required because
            // Liberty does not officially support Java 9 or 10
            reqs.setVersionDisplayString(minJava11);
            return;
        }

        if (minVersion.startsWith("12.") ||
            minVersion.startsWith("13.") ||
            minVersion.startsWith("14.") ||
            minVersion.startsWith("15.") ||
            minVersion.startsWith("16.") ||
            minVersion.startsWith("17.")) {
            // If a feature requires a min of Java 12/13/14/15/16/17, state Java 17 is required because
            // Liberty does not officially support Java 12-16
            reqs.setVersionDisplayString(minJava17);
            return;
        }

        // The min version string has been generated/validated incorrectly
        // Can't recover from this, it is a bug in EsaUploader
        throw new AssertionError("Unrecognized java version: " + minVersion);

    }

    /**
     * @return the query for Superseded By (optional) or null if this feature
     *         does not declare itself to be optionally superseded by anything.
     */
    private Collection<String> createSupersededByOptionalQuery() {
        Collection<String> supersededByOptional = _asset.getWlpInformation().getSupersededByOptional();

        Collection<String> query = null;
        if (supersededByOptional != null) { //if there are no queries to add
            query = new ArrayList<String>();

            for (String feature : supersededByOptional) {
                StringBuilder b = new StringBuilder();
                b.append("wlpInformation.shortName=");
                b.append(feature);
                b.append("&wlpInformation.appliesToFilterInfo.minVersion.value=");
                String version = findVersion();
                if (version != null) {
                    b.append(version);
                    b.append("&type=com.ibm.websphere.Feature");
                }
                query.add(b.toString());
            }
        }

        return query;
    }

    private Link makeLink(String label, String linkLabelProperty, Collection<String> query, String linkLabelPrefix, String linkLabelSuffix) {
        Link link = makeLink(label, linkLabelProperty, query);
        link.setLinkLabelPrefix(linkLabelPrefix);
        link.setLinkLabelSuffix(linkLabelSuffix);
        return link;
    }

    private Link makeLink(String label, String linkLabelProperty, Collection<String> query) {
        Link link = new Link();
        link.setLabel(label);
        link.setLinkLabelProperty(linkLabelProperty);
        link.setQuery(query);
        return link;
    }

    /**
     * Creates the links to enables/enabled-by/supersedes/superseded-by sections. At present,
     * link labels are hardcoded in this function. We may need to move them in the future if
     * we need to translate them or if we just don't like the idea of having hardcoded message
     * strings in here.
     */
    private Collection<Link> createLinks() {
        ArrayList<Link> links = new ArrayList<Link>();

        Collection<String> enablesQuery = createEnablesQuery();
        links.add(makeLink("Features that this feature enables", "name", enablesQuery));

        Collection<String> enabledByQuery = createEnabledByQuery();
        links.add(makeLink("Features that enable this feature", "name", enabledByQuery));

        Collection<String> supersedesQuery = createSupersedesQuery();
        links.add(makeLink("Features that this feature supersedes", "name", supersedesQuery));

        Collection<String> supersededByQuery = createSupersededByQuery();
        links.add(makeLink("Features that supersede this feature", "name", supersededByQuery));

        // Note: by giving this the same link title as superseded-by, the links appear in the same
        // link section on the website (but with the suffix that we add here).
        Collection<String> supersededByOptionalQuery = createSupersededByOptionalQuery();
        links.add(makeLink("Features that supersede this feature", "name", supersededByOptionalQuery, null, " (optional)"));

        return links;
    }

    @Override
    public void updateGeneratedFields(boolean performEditionChecking) throws RepositoryResourceCreationException {
        super.updateGeneratedFields(performEditionChecking);

        setLinks(createLinks());

        // add the string the website will use for displaying java verison compatibility
        addVersionDisplayString();
    }

    protected Collection<AppliesToFilterInfo> getAppliesToFilterInfo() {
        return _asset.getWlpInformation().getAppliesToFilterInfo();
    }

    @Override
    public RepositoryResourceMatchingData createMatchingData() {
        ExtendedMatchingData matchingData = new ExtendedMatchingData();
        matchingData.setType(getType());

        // Regen the appliesToFilterInfo as the level of code that generated each resource may
        // be different and give us different results so regen it now.
        List<AppliesToFilterInfo> atfi;
        try {
            atfi = generateAppliesToFilterInfoList(false);
            matchingData.setAtfi(atfi);
        } catch (RepositoryResourceCreationException e) {
            // This should only be thrown if validate editions is set to true, for us its set to false
        }
        matchingData.setVersion(getVersion());
        matchingData.setProvideFeature(getProvideFeature());
        return matchingData;
    }

    @Override
    protected Collection<? extends RepositoryResource> getPotentiallyMatchingResources() throws RepositoryBackendException, RepositoryResourceNoConnectionException {
        Collection<RepositoryResource> resources;

        if (getProvideFeature() != null) {
            resources = getAndCheckRepositoryConnection().getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.TYPE, getType()),
                                                                               FilterPredicate.areEqual(FilterableAttribute.SYMBOLIC_NAME, getProvideFeature()));
        } else {
            resources = getAndCheckRepositoryConnection().getMatchingResources(FilterPredicate.areEqual(FilterableAttribute.TYPE, getType()));
        }

        return resources;
    }

    @Override
    protected void copyFieldsFrom(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        EsaResourceImpl esaRes = (EsaResourceImpl) fromResource;
        setAppliesTo(esaRes.getAppliesTo());
        setWebDisplayPolicy(esaRes.getWebDisplayPolicy());
        setInstallPolicy(esaRes.getInstallPolicy());
        setLinks(esaRes.getLinks());
        setProvideFeature(esaRes.getProvideFeature());
        setProvisionCapability(esaRes.getProvisionCapability());
        // No need to call setRequireFeature, as setRequireFeatureWithTolerates will set both fields
        setRequireFeatureWithTolerates(esaRes.getRequireFeatureWithTolerates());
        setVisibility(esaRes.getVisibility());
        setShortName(esaRes.getShortName());
        setVanityURL(esaRes.getVanityURL());
        setSingleton(esaRes.getSingleton());
        setIBMInstallTo(esaRes.getIBMInstallTo());
    }

    @Override
    protected String getNameForVanityUrl() {
        return getProvideFeature();
    }

    /**
     * Returns the Enables {@link Links} for this feature
     *
     * @return
     */
    public void setLinks(Collection<Link> links) {
        Collection<com.ibm.ws.repository.transport.model.Link> attachmentLinks = new ArrayList<com.ibm.ws.repository.transport.model.Link>();
        for (Link link : links) {
            attachmentLinks.add(link.getLink());
        }

        _asset.getWlpInformation().setLinks(attachmentLinks);
    }

    /**
     * Set the Enables {@link Links} for this feature
     *
     * @return
     */
    public Collection<Link> getLinks() {
        Collection<com.ibm.ws.repository.transport.model.Link> attachmentLinks = _asset.getWlpInformation().getLinks();
        Collection<Link> links = new ArrayList<Link>();
        for (com.ibm.ws.repository.transport.model.Link link : attachmentLinks) {
            links.add(new Link(link));
        }
        return links;
    }

    /** {@inheritDoc} */
    @Override
    public void addRequireFeature(String requiredFeatureSymbolicName) {
        copyRequireFeatureToRequireFeatureWithTolerates();
        // Add to the old field without tolerates info
        _asset.getWlpInformation().addRequireFeature(requiredFeatureSymbolicName);

        // Add to requireFeatureWithTolerates field, but with empty tolerates info
        // Need to ensure that if there is an existing object with the same feature name
        // it is removed first, so that there is only one entry per feature.
        removeRequireFeatureWithToleratesIfExists(requiredFeatureSymbolicName);

        RequireFeatureWithTolerates newFeature = new RequireFeatureWithTolerates();
        newFeature.setFeature(requiredFeatureSymbolicName);
        newFeature.setTolerates(Collections.<String> emptySet());
        _asset.getWlpInformation().addRequireFeatureWithTolerates(newFeature);
    }

    /** {@inheritDoc} */
    @Override
    public void addRequireFeatureWithTolerates(String feature, Collection<String> tolerates) {
        WlpInformation wlp = _asset.getWlpInformation();
        copyRequireFeatureToRequireFeatureWithTolerates();
        // add just the feature info to the old field
        wlp.addRequireFeature(feature);

        // Add to the new field including tolerates info.
        // The set of RequireFeatureWithTolerates should be unique based on the feature
        // name, so need to check if there is an existing entry to overwrite.
        removeRequireFeatureWithToleratesIfExists(feature);

        // Previous entry removed (if it existed), now add the new entry
        RequireFeatureWithTolerates newFeature = new RequireFeatureWithTolerates();
        newFeature.setFeature(feature);
        newFeature.setTolerates(tolerates);
        wlp.addRequireFeatureWithTolerates(newFeature);
    }

    /**
     * Looks in the underlying asset to see if there is a requireFeatureWithTolerates entry for
     * the supplied feature, and if there is, removes it.
     */
    private void removeRequireFeatureWithToleratesIfExists(String feature) {
        Collection<RequireFeatureWithTolerates> rfwt = _asset.getWlpInformation().getRequireFeatureWithTolerates();
        if (rfwt != null) {
            for (RequireFeatureWithTolerates toCheck : rfwt) {
                if (toCheck.getFeature().equals(feature)) {
                    rfwt.remove(toCheck);
                    return;
                }
            }
        }
    }

    /**
     * requireFeature was the old field in the asset which didn't contain tolerates information.
     * The new field is requireFeatureWithTolerates, and for the moment, both fields are being
     * maintained, as older assets in the repository will only have the older field. When older assets
     * are being written to, the data from the older field needs to be copied to the new field, to ensure
     * both are consistent.
     * The write will then write to both fields
     */
    private void copyRequireFeatureToRequireFeatureWithTolerates() {
        Collection<RequireFeatureWithTolerates> rfwt = _asset.getWlpInformation().getRequireFeatureWithTolerates();
        if (rfwt != null) {
            // Both fields (with and without tolerates) should exist, as
            // rfwt should not be created unless the other field is created first.
            // No need to copy, as the two fields should always be in sync
            return;
        }

        Collection<String> requireFeature = _asset.getWlpInformation().getRequireFeature();
        if (requireFeature == null) {
            // Neither field exists, no need to copy
            return;
        }

        // We have the requireFeature field but not rfwt, so copy info into
        // the new field (rfwt).
        Collection<RequireFeatureWithTolerates> newOne = new HashSet<RequireFeatureWithTolerates>();
        for (String feature : requireFeature) {
            RequireFeatureWithTolerates newFeature = new RequireFeatureWithTolerates();
            newFeature.setFeature(feature);
            newFeature.setTolerates(Collections.<String> emptyList());
            newOne.add(newFeature);
        }
        _asset.getWlpInformation().setRequireFeatureWithTolerates(newOne);
    }

    /** {@inheritDoc} */
    @Override
    public void addRequireFix(String fix) {
        _asset.getWlpInformation().addRequireFix(fix);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getRequireFix() {
        return _asset.getWlpInformation().getRequireFix();
    }

    /** {@inheritDoc} */
    @Override
    public void setRequireFeature(Collection<String> feats) {
        // No need to copy (like we do in addRequireFeatureWithTolerates)
        // as we are overwriting anyway
        // It would be nice if this delegated to setRequireFeatureWithTolerates, but that
        // would require an awful lot of data munging with little benefit, and this method
        // is deprecated and going away anyway.
        // Need to allow for feats being null, for legacy reasons
        Collection<RequireFeatureWithTolerates> set = null;
        if (feats != null) {
            set = new HashSet<RequireFeatureWithTolerates>();
            for (String feat : feats) {
                RequireFeatureWithTolerates feature = new RequireFeatureWithTolerates();
                feature.setFeature(feat);
                feature.setTolerates(Collections.<String> emptySet());
                set.add(feature);
            }
        }
        _asset.getWlpInformation().setRequireFeatureWithTolerates(set);
        _asset.getWlpInformation().setRequireFeature(feats);
    }

    /** {@inheritDoc} */
    @Override
    public void setRequireFeatureWithTolerates(Map<String, Collection<String>> features) {
        // No need to copy (like we do in addRequireFeatureWithTolerates)
        // as we are overwriting anyway
        // need to allow for features being null, for legacy reasons
        Collection<RequireFeatureWithTolerates> set = null;
        Collection<String> collection = null;
        if (features != null) {
            set = new HashSet<RequireFeatureWithTolerates>();
            collection = new HashSet<String>();
            for (Map.Entry<String, Collection<String>> foo : features.entrySet()) {
                RequireFeatureWithTolerates feature = new RequireFeatureWithTolerates();
                feature.setFeature(foo.getKey());
                feature.setTolerates(foo.getValue());
                set.add(feature);
                collection.add(foo.getKey());
            }
        }
        _asset.getWlpInformation().setRequireFeatureWithTolerates(set);
        _asset.getWlpInformation().setRequireFeature(collection);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getRequireFeature() {
        return _asset.getWlpInformation().getRequireFeature();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Collection<String>> getRequireFeatureWithTolerates() {
        // The feature may be an older feature which never had the tolerates information
        // stored, in which case, look in the older requireFeature field and massage
        // that info into the required format.
        // Or there may just not be any required features at all.
        Collection<RequireFeatureWithTolerates> rfwt = _asset.getWlpInformation().getRequireFeatureWithTolerates();
        if (rfwt != null) {
            Map<String, Collection<String>> rv = new HashMap<String, Collection<String>>();
            for (RequireFeatureWithTolerates feature : rfwt) {
                rv.put(feature.getFeature(), feature.getTolerates());
            }
            return rv;
        }

        // Newer field not present, check the older field
        Collection<String> rf = _asset.getWlpInformation().getRequireFeature();
        if (rf != null) {
            Map<String, Collection<String>> rv = new HashMap<String, Collection<String>>();
            for (String feature : rf) {
                rv.put(feature, Collections.<String> emptyList());
            }
            return rv;
        }

        // No required features at all
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void addSupersededBy(String feature) {
        _asset.getWlpInformation().addSupersededBy(feature);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getSupersededBy() {
        return _asset.getWlpInformation().getSupersededBy();
    }

    /** {@inheritDoc} */
    @Override
    public void addSupersededByOptional(String feature) {
        _asset.getWlpInformation().addSupersededByOptional(feature);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getSupersededByOptional() {
        return _asset.getWlpInformation().getSupersededByOptional();
    }

    /**
     * Returns the {@link Visibility} for this feature
     *
     * @return
     */
    @Override
    public Visibility getVisibility() {
        return _asset.getWlpInformation().getVisibility();
    }

    /** {@inheritDoc} */
    @Override
    public void setVisibility(Visibility vis) {
        _asset.getWlpInformation().setVisibility(vis);
    }

    /** {@inheritDoc} */
    @Override
    public void setShortName(String shortName) {
        _asset.getWlpInformation().setShortName(shortName);
    }

    /** {@inheritDoc} */
    @Override
    public String getShortName() {
        return _asset.getWlpInformation().getShortName();
    }

    /** {@inheritDoc} */
    @Override
    public String getLowerCaseShortName() {
        return _asset.getWlpInformation().getLowerCaseShortName();
    }

    /** {@inheritDoc} */
    @Override
    public void setAppliesTo(String appliesTo) {
        _asset.getWlpInformation().setAppliesTo(appliesTo);
    }

    /** {@inheritDoc} */
    @Override
    public String getAppliesTo() {
        return _asset.getWlpInformation().getAppliesTo();
    }

//    @Override
//    protected String getVersionForVanityUrl() {
//        String version = "";
//        WlpInformation wlp = _asset.getWlpInformation();
//        if (wlp != null) {
//            Collection<AppliesToFilterInfo> atfis = wlp.getAppliesToFilterInfo();
//            if (atfis != null && !atfis.isEmpty()) {
//                AppliesToFilterInfo atfi = atfis.iterator().next();
//                if (atfi != null) {
//                    FilterVersion ver = atfi.getMinVersion();
//                    if (ver != null) {
//                        version = ver.getLabel();
//                    }
//                }
//            }
//        }
//        return version;
//    }

    /** {@inheritDoc} */
    @Override
    public void setWebDisplayPolicy(DisplayPolicy policy) {
        _asset.getWlpInformation().setWebDisplayPolicy(policy);
    }

    /**
     * Get the {@link DisplayPolicy}
     *
     * @return {@link DisplayPolicy} in use
     */
    @Override
    public DisplayPolicy getWebDisplayPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return _asset.getWlpInformation().getWebDisplayPolicy();
    }

    /** {@inheritDoc} */
    @Override
    public String getProvisionCapability() {
        return _asset.getWlpInformation().getProvisionCapability();
    }

    /** {@inheritDoc} */
    @Override
    public void setProvisionCapability(String provisionCapability) {
        _asset.getWlpInformation().setProvisionCapability(provisionCapability);
    }

    /** {@inheritDoc} */
    @Override
    public InstallPolicy getInstallPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return _asset.getWlpInformation().getInstallPolicy();
    }

    /** {@inheritDoc} */
    @Override
    public void setInstallPolicy(InstallPolicy policy) {
        _asset.getWlpInformation().setInstallPolicy(policy);
    }

    /** {@inheritDoc} */
    @Override
    public void setJavaSEVersionRequirements(String minimum, String maximum, Collection<String> rawBundleRequirements) {
        JavaSEVersionRequirements reqs = new JavaSEVersionRequirements();
        reqs.setMinVersion(minimum);
        reqs.setMaxVersion(maximum);
        reqs.setRawRequirements(rawBundleRequirements);
        _asset.getWlpInformation().setJavaSEVersionRequirements(reqs);
    }

    /**
     * An ESA may require a minimum or maximum Java version. This is an aggregate min/max,
     * calculated from the individual requirements of the contained bundles, as specified
     * by the bundles' Require-Capability header in the bundle manifest. The
     * <code>JavaSEVersionRequirements</code> contains the set of the Require-Capability
     * headers, i.e. one from each bundle which specifies the header.
     * All fields in the version object may be null, if no requirement was specified in the bundles.
     *
     * @return
     */
    public JavaSEVersionRequirements getJavaSEVersionRequirements() {
        return _asset.getWlpInformation().getJavaSEVersionRequirements();
    }

    /** {@inheritDoc} */
    @Override
    public String getSingleton() {
        return _asset.getWlpInformation().getSingleton();
    }

    @Override
    public boolean isSingleton() {
        return Boolean.valueOf(getSingleton());
    }

    /** {@inheritDoc} */
    @Override
    public void setSingleton(String singleton) {
        _asset.getWlpInformation().setSingleton(singleton);
    }

    /** {@inheritDoc} */
    @Override
    public String getIBMInstallTo() {
        return _asset.getWlpInformation().getIbmInstallTo();
    }

    /** {@inheritDoc} */
    @Override
    public void setIBMInstallTo(String ibmInstallTo) {
        _asset.getWlpInformation().setIbmInstallTo(ibmInstallTo);
    }

}
