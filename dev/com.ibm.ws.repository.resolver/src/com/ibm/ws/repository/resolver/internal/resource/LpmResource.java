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

import java.util.Collections;
import java.util.List;

import org.osgi.resource.Requirement;

import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants.NameAttributes;

/**
 * This class represents a resource that we want to install through the Liberty Package Manager. It is not a "real" resource, i.e. it is just an entity that is created to hold a
 * requirement and the requirement should resolve to a real resource inside Massive. There is no public constructor for this class instead {@link #createInstance(String, boolean)}
 * should be
 * used.
 */
public class LpmResource extends ResourceImpl {

    private final String resource;

    /**
     * <p>Create a new instance of a resource containing the requirement from LPM. The <code>resourceString</code> should be in the form:</p>
     * <code>{name}/{version}</code>
     * <p>Where the <code>{name}</code> can be either the symbolic name or short name of the resource and <code>/{version}</code> is optional</p>
     * 
     * @param resourceString The string to parse to work out what to install
     * @param attributeToMatch The attribute to try to match against
     * @param type The type of resource to look for
     * @return The {@link LpmResource} with the requirement on the resource in the resourceString
     * @throws IllegalArgumentException if the <code>resourceString</code> contains more than one / symbol
     */
    public static LpmResource createInstance(String resourceString, NameAttributes attributeToMatch, String type) {
        String[] parts = resourceString.split("/");
        if (parts.length > 2) {
            throw new IllegalArgumentException("Only one \"/\" symbol is allowed in the resourceString but it was " + resourceString);
        }

        String version = null;
        if (parts.length == 2) {
            version = parts[1];
        }

        return new LpmResource(Collections.singletonList((Requirement) new InstallableEntityRequirement(attributeToMatch, parts[0], version, type)), LOCATION_INSTALL, resourceString);
    }

    /**
     * Creates an instance of this class
     * 
     * @param requirements The requirements
     * @param location The location
     * @param resourceString The original string to the resource being required
     */
    public LpmResource(List<Requirement> requirements, String location, String resourceString) {
        super(null, requirements, location, null);
        this.resource = resourceString;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "LpmResource [resource=" + resource + "]";
    }

}
