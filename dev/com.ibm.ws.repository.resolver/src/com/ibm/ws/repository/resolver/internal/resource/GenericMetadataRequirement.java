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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;

/**
 * A {@link RequirementImpl} backed by a {@link GenericMetadata} instance. It is assumed that the GenericMetadata will come from a syntax similar to the RequireCapability header
 * and use the osgi.identity namespace in the filter, if this is the case the display name of the requirement will be set to the identity of the required capability, otherwise the
 * name will be set to either the filter string or <code>null</code>.
 */
public class GenericMetadataRequirement extends RequirementImpl {

    private final GenericMetadata delegate;
    private static final Pattern OSGI_IDENTITY_PATTERN = Pattern.compile(".*\\(" + IdentityNamespace.IDENTITY_NAMESPACE + "=([^)]*).*");

    /**
     * @param delegate
     */
    public GenericMetadataRequirement(GenericMetadata delegate) {
        super(getNameFromDelegate(delegate));
        this.delegate = delegate;
    }

    /**
     * Returns the name set in the osgi.namespace of the filter. Note that this assumes that the generic metadata delegate you are using comes from a syntax similar to the
     * RequireCapability header in OSGi and that the filter you are using is filtering based on identity. If a filter is used not based on identity then the name will be set to the
     * filter string, in other cases it will be set to <code>null</code>.
     * 
     * @param delegate
     * @return
     */
    private static String getNameFromDelegate(GenericMetadata delegate) {
        String filterString = delegate.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        String name = null;
        if (filterString != null) {
            Matcher matcher = OSGI_IDENTITY_PATTERN.matcher(filterString);
            if (matcher.matches() && matcher.groupCount() == 1) {
                name = matcher.group(1);
            } else {
                name = filterString;
            }
        }
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#getNamespace()
     */
    @Override
    public String getNamespace() {
        return this.delegate.getNamespace();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#getDirectives()
     */
    @Override
    public Map<String, String> getDirectives() {
        return this.delegate.getDirectives();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#getAttributes()
     */
    @Override
    public Map<String, Object> getAttributes() {
        return this.delegate.getAttributes();
    }

}
