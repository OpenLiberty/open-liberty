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

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

/**
 * This abstract base class for capabilities holds the reference to the resource and a package protected setter for it as well as implementing equals and hashCode.
 */
public abstract class CapabilityImpl implements Capability, ResourceHolder {

    private Resource resource;

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
     * @see org.osgi.resource.Capability#hashCode()
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
     * @see org.osgi.resource.Capability#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        /*
         * This method may look generated but it needed to support any capability not just instances of CapabilityImpl so it has been tweaked a bit to call the get methods on the
         * Capability interface rather than directly accessing the member variables.
         * 
         * This means that sub types do not need to implement this method as it meets the spec requirements.
         */
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Capability))
            return false;
        Capability other = (Capability) obj;
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

}
