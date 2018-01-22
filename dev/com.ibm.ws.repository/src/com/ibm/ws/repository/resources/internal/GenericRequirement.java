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

package com.ibm.ws.repository.resources.internal;

import java.util.Map;

import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * A {@link Requirement} implementation backed by a {@link GenericMetadata} instance.
 */
public class GenericRequirement implements Requirement {

    private final GenericMetadata delegate;

    /**
     * @param delegate
     */
    public GenericRequirement(GenericMetadata delegate) {
        this.delegate = delegate;
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

    /** {@inheritDoc} */
    @Override
    public Resource getResource() {
        return null;
    }

}
