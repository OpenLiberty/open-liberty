/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.xml;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICardinalEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICardinalEntityB;

/**
 * Simple entity to test Bidirectional OneToOne relationships.
 *
 * BiEntityA has a bidirectional relationship with BiEntityB, with A referencing B. BiEntityA is the owning part of the
 * relationship.
 *
 * Annotations are declared on the entity fields.
 *
 */
public class XMLOOCardEntA implements ICardinalEntityA {
    /**
     * Entity primary key, an integer id number.
     */
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    private XMLOOCardEntB b;

    public XMLOOCardEntB getB() {
        return b;
    }

    public void setB(XMLOOCardEntB b) {
        this.b = b;
    }

    @Override
    public ICardinalEntityB getBField() {
        return getB();
    }

    @Override
    public void setBField(ICardinalEntityB b) {
        setB((XMLOOCardEntB) b);
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
    public String toString() {
        return "XMLOOCardEntA [id=" + id + ", name=" + name + "]";
    }

}
