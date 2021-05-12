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

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IEntityA;

/**
 * Implementation entity for the inverse side of the B5RM bidirectional relationship defined on the entity BiEntityA.
 *
 */
public class XMLOOBiEntB_B5RM extends XMLAbstractOneXOneBiEntityB {
    private XMLOOBiEntA entityA;

    public XMLOOBiEntB_B5RM() {
        super();
    }

    public XMLOOBiEntB_B5RM(int id, String name) {
        super(id, name);
    }

    public void setEntityA(XMLOOBiEntA entityA) {
        this.entityA = entityA;
    }

    public XMLOOBiEntA getEntityA() {
        return entityA;
    }

    @Override
    public IEntityA getEntityAField() {
        return getEntityA();
    }

    @Override
    public void setEntityAField(IEntityA entity) {
        setEntityA((XMLOOBiEntA) entity);
    }

    @Override
    public String toString() {
        return "XMLOOBiEntB_B5RM [getId()=" + getId() + ", getName()=" + getName() + "]";
    }
}