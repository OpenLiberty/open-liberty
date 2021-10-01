/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityBBi;

@Entity
public class MMBiEntB_CRM implements IEntityBBi {
    /**
     * Entity primary key, an integer id number.
     */
    @Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    @ManyToMany(mappedBy = "cascadeRemove")
    private Collection<MMBiEntA> entityA;

    public MMBiEntB_CRM() {
        entityA = new ArrayList<MMBiEntA>();
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

    public Collection<MMBiEntA> getEntityA() {
        return entityA;
    }

    public void setEntityA(Collection<MMBiEntA> entityA) {
        this.entityA = entityA;
    }

    @Override
    public Collection getEntityACollection() {
        return getEntityA();
    }

    @Override
    public void insertEntityAField(IEntityA iEntityA) {
        MMBiEntA entity = (MMBiEntA) iEntityA;

        Collection<MMBiEntA> collection = getEntityA();
        collection.add(entity);
    }

    @Override
    public boolean isMemberOfEntityAField(IEntityA iEntityA) {
        MMBiEntA entity = (MMBiEntA) iEntityA;

        Collection<MMBiEntA> collection = getEntityA();

        return (collection.contains(entity));
    }

    @Override
    public void removeEntityAField(IEntityA iEntityA) {
        MMBiEntA entity = (MMBiEntA) iEntityA;
        Collection<MMBiEntA> collection = getEntityA();
        collection.remove(entity);

    }

    @Override
    public void setEntityACollectionField(Collection iEntityACollection) {
        Collection<MMBiEntA> collection = new ArrayList<MMBiEntA>();

        Iterator i = iEntityACollection.iterator();
        while (i.hasNext()) {
            MMBiEntA entity = (MMBiEntA) i.next();
            collection.add(entity);
        }

        setEntityA(collection);
    }

    @Override
    public String toString() {
        return "MMBiEntB_CRM [id=" + id + ", name=" + name + "]";
    }

}