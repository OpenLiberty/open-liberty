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
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.Property;

/**
 *
 */
public class PersistenceContextRefImpl extends AbstractResourceBaseGroup implements PersistenceContextRef {

    private List<Description> descriptions;

    private String persistenceUnitName;

    private List<Property> properties;

    private int typeValue;

    private int synchronizationValue;

    public PersistenceContextRefImpl(PersistenceContextRef persistenceContextRef) {
        super(persistenceContextRef);
        this.persistenceUnitName = persistenceContextRef.getPersistenceUnitName();
        this.typeValue = persistenceContextRef.getTypeValue();
        this.synchronizationValue = persistenceContextRef.getSynchronizationValue();
        this.properties = new ArrayList<Property>(persistenceContextRef.getProperties());
        this.descriptions = new ArrayList<Description>(persistenceContextRef.getDescriptions());
    }

    /** {@inheritDoc} */
    @Override
    public List<Description> getDescriptions() {
        return Collections.unmodifiableList(descriptions);
    }

    /** {@inheritDoc} */
    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    /** {@inheritDoc} */
    @Override
    public List<Property> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    /** {@inheritDoc} */
    @Override
    public int getTypeValue() {
        return typeValue;
    }

    /** {@inheritDoc} */
    @Override
    public int getSynchronizationValue() {
        return synchronizationValue;
    }
}
