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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.osgi.framework.namespace.IdentityNamespace;

import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.namespace.ProductNamespace;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants.NameAttributes;

/**
 * <p>This is a requirement on an installed entity.</p>
 * <p>This could be either an APAR fix (iFix) or a feature.</p>
 * <p>The name returned from {@link #getName()} will be either the symbolic name, short name or lower case short name of the feature or fix ID of the iFix.</p>
 */
public class InstallableEntityRequirement extends RequirementImpl {

    /** These are the directives for this requirement */
    private final Map<String, String> directives;

    /** The type of this installable entity, can be {@link InstallableEntityIdentityConstants#TYPE_FEATURE} or {@link InstallableEntityIdentityConstants#TYPE_IFIX} */
    private final String type;

    /** The name attribute to match against */
    private final NameAttributes nameAttribute;

    /**
     * Constructs a new instance of this requirement.
     * 
     * @param symbolicName The symbolic name to require, must not be <code>null</code>
     * @param type The type of the entity to require, supported values are {@link InstallableEntityIdentityConstants#TYPE_FEATURE} or
     *            {@link InstallableEntityIdentityConstants#TYPE_IFIX}, must not
     *            be <code>null</code>
     * @throws NullPointerException if either of the parameters are <code>null</code>
     */
    public InstallableEntityRequirement(String symbolicName, String type) {
        super(symbolicName);
        if (symbolicName == null) {
            throw new NullPointerException("symbolicName must not be null");
        }
        if (type == null) {
            throw new NullPointerException("Type must not be null");
        }
        this.type = type;
        this.nameAttribute = NameAttributes.SYMBOLIC_NAME;
        Map<String, String> directives = new HashMap<String, String>();
        StringBuilder filterBuilder = new StringBuilder("(&");
        filterBuilder.append(createEqualityFilter(IdentityNamespace.IDENTITY_NAMESPACE, symbolicName));
        filterBuilder.append(createEqualityFilter(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, type));
        filterBuilder.append(")");
        directives.put(ProductNamespace.REQUIREMENT_FILTER_DIRECTIVE, filterBuilder.toString());
        this.directives = Collections.unmodifiableMap(directives);
    }

    /**
     * Creates a new requirement using the supplied symbolic name, short name and version, any of which can be null although at least one of symbolic name or short name must be set
     * or an {@link IllegalArgumentException} is thrown.
     * 
     * @param nameType The type of name to set on the requirement
     * @param nameValue The value of the name, must not be <code>null</code>
     * @param version The version to require
     * @param type The type of the entity to require, supported values are {@link InstallableEntityIdentityConstants#TYPE_FEATURE} or
     *            {@link InstallableEntityIdentityConstants#TYPE_IFIX}, must not
     *            be <code>null</code>
     */
    public InstallableEntityRequirement(NameAttributes nameType, String nameValue, String version, String type) {
        super(nameValue);
        if (nameValue == null || nameValue.isEmpty()) {
            throw new IllegalArgumentException("The name must be set");
        }
        if (type == null) {
            throw new NullPointerException("Type must not be null");
        }
        this.type = type;
        this.nameAttribute = nameType;
        // Not all of the properties may be set so only include the ones that are, the name is always set though
        Collection<String> filterParts = new HashSet<String>();
        nameValue = (nameType == NameAttributes.CASE_INSENSITIVE_SHORT_NAME) ? nameValue.toLowerCase() : nameValue;
        filterParts.add(createEqualityFilter(nameType.getFilterAttributeName(), nameValue));
        if (version != null && !version.isEmpty()) {
            filterParts.add(createEqualityFilter(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version));
        }
        filterParts.add(createEqualityFilter(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, type));

        // If there is more than one property we'll need to AND them in the filter 
        StringBuilder filterBuilder = new StringBuilder();
        if (filterParts.size() > 1) {
            filterBuilder.append("(&");
        }
        for (String filterPart : filterParts) {
            filterBuilder.append(filterPart);
        }
        if (filterParts.size() > 1) {
            filterBuilder.append(")");
        }

        // Phew, we're done, make the filter
        Map<String, String> directives = new HashMap<String, String>();
        directives.put(ProductNamespace.REQUIREMENT_FILTER_DIRECTIVE, filterBuilder.toString());
        this.directives = Collections.unmodifiableMap(directives);
    }

    /**
     * <p>Creates a filter string in the form:</p>
     * <code>(attributeName=attributeValue)</code>
     * 
     * @param attributeName The name for the attribute
     * @param attributeValue The value for the attribute
     * @return The filter string
     */
    private String createEqualityFilter(String attributeName, String attributeValue) {
        String filter = null;
        if (attributeValue != null && !attributeValue.isEmpty()) {
            filter = "(" + attributeName + "=" + attributeValue + ")";
        }
        return filter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#getNamespace()
     */
    @Override
    public String getNamespace() {
        return IdentityNamespace.IDENTITY_NAMESPACE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#getDirectives()
     */
    @Override
    public Map<String, String> getDirectives() {
        return this.directives;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#getAttributes()
     */
    @Override
    public Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }

    /**
     * Returns the type of this requirement, supported values are {@link InstallableEntityIdentityConstants#TYPE_FEATURE} or {@link InstallableEntityIdentityConstants#TYPE_IFIX}.
     * 
     * @return
     */
    public Object getType() {
        return this.type;
    }

    /**
     * Returns which name attribute is being used to do matching.
     * 
     * @return the nameAttribute
     */
    public NameAttributes getNameAttribute() {
        return nameAttribute;
    }

}
