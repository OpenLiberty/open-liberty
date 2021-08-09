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
import com.ibm.ws.javaee.dd.common.EnvEntry;

/**
 *
 */
public class EnvEntryImpl extends AbstractResourceGroup implements EnvEntry {

    private String typeName;

    private String value;

    private List<Description> descriptions;

    public EnvEntryImpl(EnvEntry envEntry) {
        super(envEntry);
        this.typeName = envEntry.getTypeName();
        this.value = envEntry.getValue();
        this.descriptions = new ArrayList<Description>(envEntry.getDescriptions());
    }

    /** {@inheritDoc} */
    @Override
    public String getTypeName() {
        return typeName;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public List<Description> getDescriptions() {
        return Collections.unmodifiableList(descriptions);
    }

}
