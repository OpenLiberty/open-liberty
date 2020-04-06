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

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.annotation;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.INoOptEntityB;

@Entity
public class MONoOptBiEntityB implements INoOptEntityB {
    /**
     * Entity primary key, an integer id number.
     */
    @Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    @OneToMany(mappedBy = "noOptional")
    private Collection<MONoOptBiEntityA> entityA;

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

    public Collection<MONoOptBiEntityA> getEntityA() {
        return entityA;
    }

    public void setEntityA(Collection<MONoOptBiEntityA> entityA) {
        this.entityA = entityA;
    }

    @Override
    public String toString() {
        return "MONoOptBiEntityB [id=" + id + ", name=" + name + "]";
    }

}
