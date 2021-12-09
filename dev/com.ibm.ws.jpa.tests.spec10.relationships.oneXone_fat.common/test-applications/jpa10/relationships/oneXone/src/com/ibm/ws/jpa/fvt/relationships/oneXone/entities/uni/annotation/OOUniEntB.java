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

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.INoOptEntityB;

/**
 * Simple entity to test Unidirectional OneToOne relationships.
 *
 * UniEntityA has a unidirectional relationship with UniEntityB, with A referencing B. UniEntityA is the owning part of
 * the relationship.
 *
 * Annotations are declared on the entity fields.
 *
 */
@Entity
public class OOUniEntB implements IEntityB, INoOptEntityB {
    @Id
    private int id;

    private String name;

    public OOUniEntB() {

    }

    public OOUniEntB(int id, String name) {
        this.id = id;
        this.name = name;
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
        return "OOUniEntB [id=" + id + ", name=" + name + "]";
    }

}
