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

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.IEntityBBi;

@Entity
public class MOBiEntB_JC implements IEntityBBi {
    /**
     * Entity primary key, an integer id number.
     */
    @Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    @OneToMany(mappedBy = "overrideColumnNameRelationship")
    private Collection<MOBiEntA> entityA;

    public MOBiEntB_JC() {
        entityA = new ArrayList<MOBiEntA>();
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

    public Collection<MOBiEntA> getEntityA() {
        return entityA;
    }

    public void setEntityA(Collection<MOBiEntA> entityA) {
        this.entityA = entityA;
    }

    @Override
    public Collection getEntityACollection() {
        return getEntityA();
    }

    @Override
    public void insertEntityAField(IEntityA iEntityA) {
        MOBiEntA entity = (MOBiEntA) iEntityA;

        Collection<MOBiEntA> collection = getEntityA();
        collection.add(entity);
    }

    @Override
    public boolean isMemberOfEntityAField(IEntityA iEntityA) {
        MOBiEntA entity = (MOBiEntA) iEntityA;

        Collection<MOBiEntA> collection = getEntityA();

        return (collection.contains(entity));
    }

    @Override
    public void removeEntityAField(IEntityA iEntityA) {
        MOBiEntA entity = (MOBiEntA) iEntityA;
        Collection<MOBiEntA> collection = getEntityA();
        collection.remove(entity);

    }

    @Override
    public void setEntityACollectionField(Collection iEntityACollection) {
        Collection<MOBiEntA> collection = new ArrayList<MOBiEntA>();

        Iterator i = iEntityACollection.iterator();
        while (i.hasNext()) {
            MOBiEntA entity = (MOBiEntA) i.next();
            collection.add(entity);
        }

        setEntityA(collection);
    }

    @Override
    public String toString() {
        return "MOBiEntB_JC [id=" + id + ", name=" + name + "]";
    }
}
