/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.xml;

import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.INoOptEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.INoOptEntityB;

//@Entity
public class XMLMONoOptEntityA implements INoOptEntityA {
    /**
     * Entity primary key, an integer id number.
     */
    // @Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    /*
     * Field: noOptional
     *
     * Many to one mapping to an IEntityB-type entity. No override of the foreign key column name.
     *
     * ManyToOne Config Cascade: default no Fetch: default eager Optional: false (reference can not be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    // @ManyToOne(optional=false)
    private XMLMONoOptEntityB noOptional;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public XMLMONoOptEntityB getNoOptional() {
        return noOptional;
    }

    public void setNoOptional(XMLMONoOptEntityB noOptional) {
        this.noOptional = noOptional;
    }

    @Override
    public INoOptEntityB getNoOptionalField() {
        return getNoOptional();
    }

    @Override
    public void setNoOptionalField(INoOptEntityB b2) {
        setNoOptional((XMLMONoOptEntityB) b2);
    }

    @Override
    public String toString() {
        return "XMLMONoOptEntityA [id=" + id + ", name=" + name + ", noOptional=" + noOptional + "]";
    }

}
