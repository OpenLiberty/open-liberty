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

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICardinalEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICardinalEntityBBi;

@Entity
public class OOBiCardEntB implements ICardinalEntityBBi {
    /**
     * Entity primary key, an integer id number.
     */
    @Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    @OneToOne(mappedBy = "b")
    private OOBiCardEntA a;

    @Override
    public OOBiCardEntA getA() {
        return a;
    }

    public void setA(OOBiCardEntA a) {
        this.a = a;
    }

    public ICardinalEntityA getAField() {
        return getA();
    }

    @Override
    public void setAField(ICardinalEntityA a) {
        setA((OOBiCardEntA) a);
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
        return "OOBiCardEntB [id=" + id + ", name=" + name + "]";
    }

}
