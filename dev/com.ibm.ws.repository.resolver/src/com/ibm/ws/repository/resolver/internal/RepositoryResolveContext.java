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

package com.ibm.ws.repository.resolver.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.resource.FeatureResource;
import com.ibm.ws.repository.resolver.internal.resource.InstallableEntityRequirement;
import com.ibm.ws.repository.resolver.internal.resource.SampleResource;
import com.ibm.ws.repository.resources.EsaResource;

/**
 * <p>This implementation of {@link ResolveContext} is a fairly generic implementation that will match the capabilities of a given set of resources to a single mandatory resource.
 * It has a preferential order of matching capabilities to requirements so if a product matches are done first, then installed resources then repo resources, if a match is found in
 * any of the preceding resources subsequent checks are not done in order to make sure that a repo resource is <b>never</b> picked above an installed resource by the resolver when
 * finding matching capabilities. Although it is generic it does not fully implement the ResolveContext spec due to the following limitations:</p>
 * <ul>
 * <li>The {@link AbstractWiringNamespace#CAPABILITY_MANDATORY_DIRECTIVE} is not checked when looking for a match as this implementation is not designed to work with resources from
 * the osgi.wiring.* namespace</li>
 * <li>The {@link #insertHostedCapability(List, HostedCapability)} method will always just add the hosted capability to the end of the list</li>
 * <li>{@link #isEffective(Requirement)} always returns <code>true</code></li>
 * </ul>
 *
 */
public class RepositoryResolveContext extends ResolveContext {

    private final Collection<Resource> mandatoryResources;
    private final Collection<Resource> optionalResources;
    private final List<Resource> installedResources;
    private final static Map<Resource, Wiring> WIRINGS = Collections.emptyMap();
    private final Logger logger = Logger.getLogger(RepositoryResolveContext.class.getName());
    private final List<Resource> installedProducts;
    private final List<Resource> repoResources;
    private final Collection<Requirement> requirementsNotFound = new HashSet<Requirement>();
    private final RepositoryConnectionList loginInfo;
    private final Collection<ResolutionFilter> filters = new HashSet<ResolutionFilter>();

    /**
     * Create a new instance of this class. Note that the supplied {@link List}s must be sorted prior to calling this method, this class will not modify these lists in any way.
     * This means that the lowest indexed item in each list should be the resource that is most preferential to be matched to a given requirement.
     *
     * @param mandatoryResources The artifact(s) that are to be installed into WLP and must be wired by the resolution
     * @param optionalResources The artifact(s) that are to be installed if possible into WLP and will be wired by the resolution but will not cause the resolution to fail if they
     *            can't be
     * @param installedProducts A sorted list of products installed
     * @param installedResources A sorted list of resources installed into WLP
     * @param repoResources A sorted List of resources available in the WLP repository, this collection maybe added to if more resources need to be loaded from the repository due
     *            to a missing requirement. If this is a problem for the caller a clone of the list should be passed in.
     * @param loginInfo The login info for connecting to the repository. Maybe <code>null</code> which indicates no new resources should be loaded from the repository.
     */
    public RepositoryResolveContext(Collection<Resource> mandatoryResources, Collection<Resource> optionalResources, List<Resource> installedProducts,
                                    List<Resource> installedResources, List<Resource> repoResources, RepositoryConnectionList loginInfo) {
        this.mandatoryResources = mandatoryResources;
        this.optionalResources = optionalResources;
        this.installedProducts = installedProducts;
        this.installedResources = installedResources;
        this.repoResources = repoResources;
        this.loginInfo = loginInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.resolver.ResolveContext#findProviders(org.osgi.resource.Requirement)
     */
    @Override
    public List<Capability> findProviders(Requirement requirement) {
        /*
         * We have a hierarchy of where to look, if there is a resource installed the provides a capability always use that one. Only look in the repo once it isn't found installed
         * (this is because the user may of installed a lower level iFix so we don't want to use the rule that says use the latest one).
         */
        List<Capability> providers = this.findProviders(requirement, this.installedProducts);
        if (providers.isEmpty()) {
            providers = this.findProviders(requirement, this.installedResources);
        }
        if (providers.isEmpty()) {
            List<Capability> repoCapabilities = this.findProviders(requirement, this.repoResources);
            if (allowResolvingToRepoCapabilities(requirement, repoCapabilities)) {
                providers = repoCapabilities;
            }
        }
        if (providers.isEmpty()) {
            // Yikes, can't find it, if it is an ESA then see if we have one for another version so we can make a better error message
            if (loginInfo != null && requirement instanceof InstallableEntityRequirement && allowResolvingToRepoCapabilities(requirement, providers)) {
                InstallableEntityRequirement installableEntityRequirement = (InstallableEntityRequirement) requirement;
                if (InstallableEntityIdentityConstants.TYPE_FEATURE.equals(installableEntityRequirement.getType())) {
                    try {
                        Collection<EsaResource> esaMassiveResources = loginInfo.getMatchingEsas(installableEntityRequirement.getNameAttribute().getFeatureFilterAttribute(),
                                                                                                installableEntityRequirement.getName());
                        for (EsaResource esaMassiveResource : esaMassiveResources) {
                            FeatureResource featureResource = FeatureResource.createInstance(esaMassiveResource);
                            if (!repoResources.contains(featureResource)) {
                                // Don't worry about sorting this, if it is loaded here it can go on the end as it is from the wrong version so should only be tried in an emergency
                                repoResources.add(featureResource);
                            }
                        }
                    } catch (RepositoryBackendException e) {
                        // Swallow this, we're only doing it to get a better error message so if we fail to connect ignore it as we'll still supply a valid error back to the user
                    }
                    providers = this.findProviders(requirement, this.repoResources);
                } else if (InstallableEntityIdentityConstants.TYPE_SAMPLE.equals(installableEntityRequirement.getType())) {
                    try {
                        Collection<com.ibm.ws.repository.resources.SampleResource> sampleMassiveResources = loginInfo.getMatchingSamples(installableEntityRequirement.getNameAttribute().getFeatureFilterAttribute(),
                                                                                                                                         installableEntityRequirement.getName());
                        for (com.ibm.ws.repository.resources.SampleResource sampleMassiveResource : sampleMassiveResources) {
                            SampleResource sampleResource = SampleResource.createInstance(sampleMassiveResource);
                            if (!repoResources.contains(sampleResource)) {
                                // Don't worry about sorting this, if it is loaded here it can go on the end as it is from the wrong version so should only be tried in an emergency
                                repoResources.add(sampleResource);
                            }
                        }
                    } catch (RepositoryBackendException e) {
                        // Swallow this, we're only doing it to get a better error message so if we fail to connect ignore it as we'll still supply a valid error back to the user
                    }
                    providers = this.findProviders(requirement, this.repoResources);
                }
            }

            if (providers.isEmpty()) {
                // Still can't find it, not sure if this is fatal but record we couldn't find it and if the whole thing blows up the exception handler can use this in an error message
                requirementsNotFound.add(requirement);
            }
        }
        return providers;
    }

    private boolean allowResolvingToRepoCapabilities(Requirement requirement, List<Capability> repoCapabilities) {
        boolean allowResolvingToRepoCapabilities = true;
        for (ResolutionFilter filter : filters) {
            if (!filter.allowResolution(requirement, repoCapabilities)) {
                allowResolvingToRepoCapabilities = false;
                break;
            }
        }
        return allowResolvingToRepoCapabilities;
    }

    /**
     * Search for a provider of the requirement in the supplied list of resources.
     *
     * @param requirement
     * @param providers The providers of capabilities to look in
     * @return A list containing the providers or an empty list if none are found
     */
    private List<Capability> findProviders(Requirement requirement, List<Resource> providers) {
        /*
         * Look through all of the resources we know about looking for a
         * capability that matches the supplied requirement (this is a fairly
         * generic implementation based on the example in the spec)
         */
        List<Capability> result = new ArrayList<Capability>();
        for (Resource resource : providers) {
            for (Capability c : resource.getCapabilities(requirement.getNamespace())) {
                if (match(requirement, c)) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    /**
     * Match a given requirement to a capability. Just checks the namespaces match and the filter matches.
     *
     * @param requirement The requirement to test
     * @param capability The capability to test
     * @return <code>true</code> if the requirement matches the capability
     */
    private boolean match(Requirement requirement, Capability capability) {
        String requirementNamespace = requirement.getNamespace();
        String capabilityNamespace = capability.getNamespace();
        if (requirementNamespace != null) {
            if (!requirementNamespace.equals(capabilityNamespace)) {
                return false;
            }
        } else if (capabilityNamespace != null) {
            // Requirement namespace is null but the capability one isn't
            return false;
        }

        String filterString = requirement.getDirectives().get("filter");
        if (filterString != null) {
            Filter filter;
            try {
                filter = FrameworkUtil.createFilter(filterString);
                if (!filter.matches(capability.getAttributes())) {
                    return false;
                }
            } catch (InvalidSyntaxException e) {
                logger.log(Level.SEVERE, "Error in the filter string \"" + filterString + "\":" + e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.resolver.ResolveContext#getMandatoryResources()
     */
    @Override
    public Collection<Resource> getMandatoryResources() {
        return this.mandatoryResources;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.resolver.ResolveContext#getOptionalResources()
     */
    @Override
    public Collection<Resource> getOptionalResources() {
        return this.optionalResources;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.resolver.ResolveContext#getWirings()
     */
    @Override
    public Map<Resource, Wiring> getWirings() {
        /*
         * We don't use the uses constraint which is what the getWirings is used
         * for so although all the installed resources will have wirings between
         * them return an empty map. Note that there is a knock on effect that
         * the map returned from the resolve call on the Resolver will also
         * include the installed resources so we may need to revisit this...
         */
        return WIRINGS;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.resolver.ResolveContext#insertHostedCapability(java.util.List, org.osgi.service.resolver.HostedCapability)
     */
    @Override
    public int insertHostedCapability(List<Capability> caps, HostedCapability hc) {
        /*
         * Hosted capabilities come from fragments which don't really mean
         * anything to us, still have a valid implementation though but don't
         * stress about doing anything too arduous!
         */
        caps.add(hc);
        return caps.size() - 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.resolver.ResolveContext#isEffective(org.osgi.resource.Requirement)
     */
    @Override
    public boolean isEffective(Requirement requirement) {
        // All of our requirements are effective
        return true;
    }

    /**
     * Returns a collection of {@link Requirement}s that were not found in the {@link #findProviders(Requirement)} methods. Note that this object exists for the whole life of this
     * object so will contain requirements not found across all of the resolutions that this object provides the context for. It is the property of the caller so can be cleared in
     * between uses of this object if a fresh collection is required.
     *
     * @return The requirements that weren't found
     */
    public Collection<Requirement> getRequirementsNotFound() {
        return this.requirementsNotFound;
    }

    public void addFilter(ResolutionFilter filter) {
        this.filters.add(filter);
    }

}
