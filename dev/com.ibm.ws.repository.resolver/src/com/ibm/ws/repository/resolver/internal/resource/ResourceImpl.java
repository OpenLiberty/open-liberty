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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * <p>This class implements the {@link Resource} interface.</p>
 */
public class ResourceImpl implements Resource {

    /**
     * A map of namespace to capability list, must not be <code>null</code>. There is also an entry keyed against <code>null</code> containing all the capabilities and each list is
     * immutable.
     */
    private final Map<String, List<Capability>> namespaceToCapabilities;
    /**
     * A map of namespace to requirement list, must not be <code>null</code>. There is also an entry keyed against <code>null</code> containing all the requirements and each list
     * is immutable.
     */
    private final Map<String, List<Requirement>> namespaceToRequirements;
    /** The location of this resource */
    protected final String location;
    /** Constant defining a repository location */
    public final static String LOCATION_REPOSITORY = "repo";
    /** Constant defining the location in an install */
    public final static String LOCATION_INSTALL = "install";
    /** The resource in massive for this OSGi resource, may be <code>null</code> if this resource is not from Massive */
    private final RepositoryResource resource;

    /**
     * Construct a new Resource with the supplied capabilities, requirements and location. If any of the <code>capabilities</code> are instances of {@link CapabilityImpl} or the
     * requirements instances of {@link RequirementImpl} then the resource will be set on it to this.
     * 
     * @param capabilities The capabilities that this resource possesses
     * @param requirements The requirements that this resource needs. The iteration order is maintained.
     * @param location The location of this resource used for equality comparison with other resources. Because a resource is unique in a given location it is sufficient for this
     *            to define the root location of the resource. Default supported values are supplied for resolving from a single repository ({@link #LOCATION_REPOSITORY}) or the
     *            installed product ({@link #LOCATION_INSTALL}).
     * @param massiveResource The resource in massive or <code>null</code> if this is not representing a resource from massive
     */
    public ResourceImpl(List<Capability> capabilities, List<Requirement> requirements, String location, RepositoryResource massiveResource) {
        this.resource = massiveResource;
        NamespaceMapBuilder<Capability> capabilityMapBuilder = new NamespaceMapBuilder<Capability>() {
            @Override
            public String getNamespace(Capability capability) {
                return capability.getNamespace();
            }
        };
        this.namespaceToCapabilities = capabilityMapBuilder.buildMap(capabilities, this);

        NamespaceMapBuilder<Requirement> requirementMapBuilder = new NamespaceMapBuilder<Requirement>() {
            @Override
            public String getNamespace(Requirement requirement) {
                return requirement.getNamespace();
            }
        };
        this.namespaceToRequirements = requirementMapBuilder.buildMap(requirements, this);

        this.location = location;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Resource#getCapabilities(java.lang.String)
     */
    @Override
    public List<Capability> getCapabilities(String namespace) {
        List<Capability> capabilities = this.namespaceToCapabilities.get(namespace);
        if (capabilities != null) {
            return capabilities;
        } else {
            return Collections.emptyList();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Resource#getRequirements(java.lang.String)
     */
    @Override
    public List<Requirement> getRequirements(String namespace) {
        List<Requirement> requirements = this.namespaceToRequirements.get(namespace);
        if (requirements != null) {
            return requirements;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns resource in massive for this OSGi resource, may be <code>null</code> if this resource is not from Massive
     * 
     * @return the resource
     */
    public RepositoryResource getResource() {
        return resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Resource#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((namespaceToCapabilities == null) ? 0 : namespaceToCapabilities.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((namespaceToRequirements == null) ? 0 : namespaceToRequirements.hashCode());
        return result;
    }

    /** A collection of resources that are in the current call stack on the {@link #equals(Object)} method */
    private List<ResourceImpl> equalityCheckingResources;

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Resource#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceImpl other = (ResourceImpl) obj;

        /*
         * As resources have relationships to requirements and capabilities and both need to check the other for equality we end up with a stack overflow when calling equals. To
         * get around this if we have already been called with the same obj then do not do any checks on its internal properties as we must be half way through an equality check at
         * the moment on this object so just return true to let that equality check complete.
         * 
         * Not that the equalityCheckingResources property must be cleared when we return from this method except for when we are skipping it.
         */
        if (this.equalityCheckingResources == null) {
            // Use a list so that it doesn't do any checking on the equality when you do an add
            this.equalityCheckingResources = new ArrayList<ResourceImpl>();
        }
        for (ResourceImpl alreadyChecked : this.equalityCheckingResources) {
            if (alreadyChecked == other) {
                return true;
            }
        }
        this.equalityCheckingResources.add(other);
        // We build the namespaceToCapabilities and namespaceToRequirements maps in the constructor so no need to do null checks
        if (!namespaceToCapabilities.equals(other.namespaceToCapabilities)) {
            this.equalityCheckingResources = null;
            return false;
        }
        if (!namespaceToRequirements.equals(other.namespaceToRequirements)) {
            this.equalityCheckingResources = null;
            return false;
        }
        if (location == null) {
            if (other.location != null) {
                this.equalityCheckingResources = null;
                return false;
            }
        } else if (!location.equals(other.location)) {
            this.equalityCheckingResources = null;
            return false;
        }
        this.equalityCheckingResources = null;
        return true;
    }

    /**
     * Compares the location of this resource with another. It will make {@link #LOCATION_INSTALL} be lower than {@link #LOCATION_REPOSITORY}.
     * 
     * @param other
     * @return
     */
    protected int compareLocation(ResourceImpl other) {
        if (LOCATION_INSTALL.equals(this.location) && !LOCATION_INSTALL.equals(other.location)) {
            return -1;
        } else if (!LOCATION_INSTALL.equals(this.location) && LOCATION_INSTALL.equals(other.location)) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * This will build a map between a namespace and a list of the objects for that namespace
     * 
     * @param <T> The type of the items in the list to sort, will be either {@link Capability} or {@link Requirement}
     */
    private abstract class NamespaceMapBuilder<T> {

        /**
         * Create a map grouping the items in the list by their namespace. A copy of the complete list of objects will also be added under the <code>null</code> key. Each grouped
         * list will be immutable. If an item in the list implements {@link ResourceHolder} then the resource on it will be set to the supplied <code>resource</code>.
         * 
         * @param list The list of objects to put into a map
         * @param resource T
         * @return
         */
        public Map<String, List<T>> buildMap(List<T> list, Resource resource) {
            Map<String, List<T>> map;
            if (list != null) {
                // As per the JavaDoc we need the map to only contain immutable lists so first build a temp map then make everything immutable 
                // Use a LinkedHashMap so that we maintain the iteration order (see defect 129271)
                Map<String, List<T>> tempMap = new LinkedHashMap<String, List<T>>();

                for (T item : list) {
                    String namespace = getNamespace(item);
                    List<T> namespaceItems = tempMap.get(namespace);
                    if (namespaceItems == null) {
                        namespaceItems = new ArrayList<T>();
                        tempMap.put(namespace, namespaceItems);
                    }
                    if (item instanceof ResourceHolder) {
                        ((ResourceHolder) item).setResource(resource);
                    }
                    namespaceItems.add(item);
                }

                /*
                 * When you do a getCapabilities(null) or getRequirements(null) call it should return a list with all the capabilities or requirements, to make this call easier
                 * store a list of all the capabilities and requirements under the null key - to make sure we stick to the immutability requirements in the Resource JavaDoc really
                 * store a copy
                 */
                tempMap.put(null, new ArrayList<T>(list));

                /*
                 * Have got all of the items sorted by namespace now, now make all the lists immutable to meet the spec requirement on getCapabilities and getRequirements
                 */
                map = new LinkedHashMap<String, List<T>>();
                for (Map.Entry<String, List<T>> namespaceAndItems : tempMap.entrySet()) {
                    map.put(namespaceAndItems.getKey(), Collections.unmodifiableList(namespaceAndItems.getValue()));
                }
            } else {
                map = Collections.emptyMap();
            }
            return map;
        }

        /**
         * Return the namespace for the item that was obtained from the list supplied to {@link #buildMap(List, Resource)}.
         * 
         * @param object
         * @return
         */
        // Boo that there isn't a common interface for Requirement and Capability!
        public abstract String getNamespace(T object);
    }

}
