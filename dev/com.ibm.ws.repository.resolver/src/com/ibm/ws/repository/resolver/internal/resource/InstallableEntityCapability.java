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
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;

import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;

/**
 * <p>This is a capability representing an installed entity.</p>
 * <p>This could be either an APAR fix (iFix) or a feature.</p>
 */
public class InstallableEntityCapability extends CapabilityImpl implements Capability {

    private final Map<String, Object> attributes;

    /**
     * Construct a new instance providing the symbolic name of the installable entity.
     * 
     * @param symbolicName The symbolic name of the installable entity.
     * @param type The type of the entity providing this capability, supported values are {@link InstallableEntityIdentityConstants#TYPE_FEATURE} or
     *            {@link InstallableEntityIdentityConstants#TYPE_IFIX}, must not be <code>null</code>
     */
    public InstallableEntityCapability(String symbolicName, String type) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(IdentityNamespace.IDENTITY_NAMESPACE, symbolicName);
        attributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, type);
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    /**
     * Construct a new instance providing all of the possible information about the installable entity.
     * 
     * @param symbolicName The symbolic name of the installable entity.
     * @param shortName The short name of the installable entity.
     * @param lowerCaseShortName The lower case name of the installable entity
     * @param version The version of the installable entity.
     * @param type The type of the entity providing this capability, supported values are {@link InstallableEntityIdentityConstants#TYPE_FEATURE} or
     *            {@link InstallableEntityIdentityConstants#TYPE_IFIX}, must not be <code>null</code>
     */
    public InstallableEntityCapability(String symbolicName, String shortName, String lowerCaseShortName, Version version, String type) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(IdentityNamespace.IDENTITY_NAMESPACE, symbolicName);
        attributes.put(InstallableEntityIdentityConstants.CAPABILITY_SHORT_NAME_ATTRIBUTE, shortName);
        attributes.put(InstallableEntityIdentityConstants.CAPABILITY_LOWER_CASE_SHORT_NAME_ATTRIBUTE, lowerCaseShortName);
        attributes.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
        attributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, type);
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Capability#getNamespace()
     */
    @Override
    public String getNamespace() {
        return IdentityNamespace.IDENTITY_NAMESPACE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Capability#getDirectives()
     */
    @Override
    public Map<String, String> getDirectives() {
        return Collections.emptyMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Capability#getAttributes()
     */
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

}
