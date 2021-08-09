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
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;

/**
 *
 */
public class ResourceEnvRefImpl extends AbstractResourceGroup implements ResourceEnvRef {

    private List<Description> descriptions;

    private String typeName;

    public ResourceEnvRefImpl(ResourceEnvRef resourceEnvRef) {
        super(resourceEnvRef);
        this.typeName = resourceEnvRef.getTypeName();
        this.descriptions = new ArrayList<Description>(resourceEnvRef.getDescriptions());
    }

    /** {@inheritDoc} */
    @Override
    public List<Description> getDescriptions() {
        return Collections.unmodifiableList(descriptions);
    }

    /** {@inheritDoc} */
    @Override
    public String getTypeName() {
        return typeName;
    }

}
