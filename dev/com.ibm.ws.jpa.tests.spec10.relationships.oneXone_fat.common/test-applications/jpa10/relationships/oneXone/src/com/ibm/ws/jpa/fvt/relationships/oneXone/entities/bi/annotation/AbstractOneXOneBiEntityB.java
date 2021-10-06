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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IEntityBBi;

/**
 * Abstract entity definition for entities on the inverse side of a bidirectional OneToOne relationship.
 *
 * BiEntityA has a bidirectional relationship with BiEntityB, with A referencing B. BiEntityA is the owning part of the
 * relationship.
 *
 * Annotations are declared on the entity fields.
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractOneXOneBiEntityB implements IEntityBBi {
    @Id
    private int id;

    private String name;

    public AbstractOneXOneBiEntityB() {

    }

    public AbstractOneXOneBiEntityB(int id, String name) {
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
        return "AbstractOneXOneBiEntityB [id=" + id + ", name=" + name + "]";
    }
}
