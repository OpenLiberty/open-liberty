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

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.IEntityBBi;

//@Entity
public class XMLMOBiEntB_CA implements IEntityBBi {
    /**
     * Entity primary key, an integer id number.
     */
    // @Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    // @OneToMany(mappedBy="cascadeAll")
    private Collection<XMLMOBiEntA> entityA;

    public XMLMOBiEntB_CA() {
        entityA = new ArrayList<XMLMOBiEntA>();
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

    public Collection<XMLMOBiEntA> getEntityA() {
        return entityA;
    }

    public void setEntityA(Collection<XMLMOBiEntA> entityA) {
        this.entityA = entityA;
    }

    @Override
    public Collection getEntityACollection() {
        return getEntityA();
    }

    @Override
    public void insertEntityAField(IEntityA iEntityA) {
        XMLMOBiEntA entity = (XMLMOBiEntA) iEntityA;

        Collection<XMLMOBiEntA> collection = getEntityA();
        collection.add(entity);
    }

    @Override
    public boolean isMemberOfEntityAField(IEntityA iEntityA) {
        XMLMOBiEntA entity = (XMLMOBiEntA) iEntityA;

        Collection<XMLMOBiEntA> collection = getEntityA();

        return (collection.contains(entity));
    }

    @Override
    public void removeEntityAField(IEntityA iEntityA) {
        XMLMOBiEntA entity = (XMLMOBiEntA) iEntityA;
        Collection<XMLMOBiEntA> collection = getEntityA();
        collection.remove(entity);

    }

    @Override
    public void setEntityACollectionField(Collection iEntityACollection) {
        Collection<XMLMOBiEntA> collection = new ArrayList<XMLMOBiEntA>();

        Iterator i = iEntityACollection.iterator();
        while (i.hasNext()) {
            XMLMOBiEntA entity = (XMLMOBiEntA) i.next();
            collection.add(entity);
        }

        setEntityA(collection);
    }

    @Override
    public String toString() {
        return "XMLMOBiEntB_CA [id=" + id + ", name=" + name + "]";
    }
}
