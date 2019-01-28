/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.xml;

import javax.persistence.Transient;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.INoOptEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.INoOptEntityB;

/**
 * Simple entity to test non-optional Unidirectional OneToOne relationships.
 *
 * UniEntityA has a bidirectional, non-optional relationship with UniEntityB, with A referencing B. UniEntityA is the
 * owning part of the relationship.
 *
 * Annotations are declared on the entity fields.
 *
 */

public class XMLOONoOptBiEntityA implements INoOptEntityA {
    /**
     * Entity primary key, an integer id number.
     */
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    /**
     * One to One Relationship, non optional, bidirectional.
     */
    private XMLOONoOptBiEntityB b;

    public XMLOONoOptBiEntityA() {

    }

    public XMLOONoOptBiEntityB getB() {
        return b;
    }

    public void setB(XMLOONoOptBiEntityB b) {
        this.b = b;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Transient
    public void setBField(INoOptEntityB b) {
        setB((XMLOONoOptBiEntityB) b);
    }

    @Override
    @Transient
    public INoOptEntityB getBField() {
        return getB();
    }

    @Override
    public String toString() {
        return "XMLOONoOptBiEntityA [id=" + id + ", name=" + name + "]";
    }
}
