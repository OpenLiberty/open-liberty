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

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityBBi;

//@Entity
public class XMLMMBiEntB_DR implements IEntityBBi {
    /**
     * Entity primary key, an integer id number.
     */
    //@Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    //@ManyToMany(mappedBy="defaultRelationship")
    private Collection<XMLMMBiEntA> entityA;

    public XMLMMBiEntB_DR() {
        entityA = new ArrayList<XMLMMBiEntA>();
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

    public Collection<XMLMMBiEntA> getEntityA() {
        return entityA;
    }

    public void setEntityA(Collection<XMLMMBiEntA> entityA) {
        this.entityA = entityA;
    }

    @Override
    public Collection getEntityACollection() {
        return getEntityA();
    }

    @Override
    public void insertEntityAField(IEntityA iEntityA) {
        XMLMMBiEntA entity = (XMLMMBiEntA) iEntityA;

        Collection<XMLMMBiEntA> collection = getEntityA();
        collection.add(entity);
    }

    @Override
    public boolean isMemberOfEntityAField(IEntityA iEntityA) {
        XMLMMBiEntA entity = (XMLMMBiEntA) iEntityA;

        Collection<XMLMMBiEntA> collection = getEntityA();

        return (collection.contains(entity));
    }

    @Override
    public void removeEntityAField(IEntityA iEntityA) {
        XMLMMBiEntA entity = (XMLMMBiEntA) iEntityA;
        Collection<XMLMMBiEntA> collection = getEntityA();
        collection.remove(entity);

    }

    @Override
    public void setEntityACollectionField(Collection iEntityACollection) {
        Collection<XMLMMBiEntA> collection = new ArrayList<XMLMMBiEntA>();

        Iterator i = iEntityACollection.iterator();
        while (i.hasNext()) {
            XMLMMBiEntA entity = (XMLMMBiEntA) i.next();
            collection.add(entity);
        }

        setEntityA(collection);
    }

    @Override
    public String toString() {
        return "XMLMMBiEntB_DR [id=" + id + ", name=" + name + "]";
    }

}