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

import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Implements the resource part of Requirement as well as equals and hashCode.
 */
public abstract class RequirementImpl implements Requirement, ResourceHolder {

    private Resource resource;

    /**
     * User friendly name for this requirement that can be used in exception messages, this is not used in the equality or hash code as that would break the contract defined by
     * {@link Requirement}
     */
    protected final String name;

    /**
     * Construct an instance of this requirement with the name set
     * 
     * @param name User friendly name for this requirement that can be used in exception messages
     */
    protected RequirementImpl(String name) {
        this.name = name;
    }

    /**
     * Returns a user friendly name for this requirement.
     * 
     * @return a user friendly name
     */
    public String getName() {
        return this.name;
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Capability#getResource()
     */
    @Override
    public Resource getResource() {
        return this.resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#hashCode()
     */
    @Override
    public int hashCode() {
        // This may look generated but it isn't: It calls methods on the interface instead of member variables to generate the hash code as per the spec guidelines
        final int prime = 31;
        int result = 1;
        Map<String, Object> attributes = this.getAttributes();
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        Map<String, String> directives = this.getDirectives();
        result = prime * result + ((directives == null) ? 0 : directives.hashCode());
        String namespace = this.getNamespace();
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        // Don't use resource when calculating the hashCode as it'll cause a stack overflow
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        /*
         * This method may look generated but it needed to support any requirement not just instances of RequirementImpl so it has been tweaked a bit to call the get methods on the
         * Requirement interface rather than directly accessing the member variables. Also, it needs to change the way the resource equality is checked as otherwise it will stack
         * overflow.
         * 
         * This means that sub types do not need to implement this method as it meets the spec requirements.
         */
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Requirement))
            return false;
        Requirement other = (Requirement) obj;
        Map<String, Object> thisAttributes = this.getAttributes();
        Map<String, Object> otherAttributes = other.getAttributes();
        if (thisAttributes == null) {
            if (otherAttributes != null)
                return false;
        } else if (!thisAttributes.equals(otherAttributes))
            return false;
        Map<String, String> thisDirectives = this.getDirectives();
        Map<String, String> otherDirectives = other.getDirectives();
        if (thisDirectives == null) {
            if (otherDirectives != null)
                return false;
        } else if (!thisDirectives.equals(otherDirectives))
            return false;
        String thisNamespace = this.getNamespace();
        String otherNamespace = other.getNamespace();
        if (thisNamespace == null) {
            if (otherNamespace != null)
                return false;
        } else if (!thisNamespace.equals(otherNamespace))
            return false;
        Resource thisResource = this.getResource();
        Resource otherResource = other.getResource();
        if (thisResource == null) {
            if (otherResource != null)
                return false;
        } else if (!thisResource.equals(otherResource))
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getClass().getName() + " [" + name + "]";
    }

}
