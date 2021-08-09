/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.ResourceRef;

/**
 *
 */
public class ResourceRefImpl extends AbstractResourceGroup implements ResourceRef {

    private int authValue;

    private int sharingScopeValue;

    private String type;

    private List<Description> descriptions;

    public ResourceRefImpl(ResourceRef resourceRef) {
        super(resourceRef);
        this.authValue = resourceRef.getAuthValue();
        this.sharingScopeValue = resourceRef.getSharingScopeValue();
        this.type = resourceRef.getType();
        this.descriptions = new ArrayList<Description>(resourceRef.getDescriptions());
    }

    /** {@inheritDoc} */
    @Override
    public List<Description> getDescriptions() {
        return Collections.unmodifiableList(descriptions);
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public int getAuthValue() {
        return authValue;
    }

    /** {@inheritDoc} */
    @Override
    public int getSharingScopeValue() {
        return sharingScopeValue;
    }
}
