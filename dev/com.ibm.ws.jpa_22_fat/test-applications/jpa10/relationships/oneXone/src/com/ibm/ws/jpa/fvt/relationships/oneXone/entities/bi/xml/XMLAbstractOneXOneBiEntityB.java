/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml;

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
public abstract class XMLAbstractOneXOneBiEntityB implements IEntityBBi {
    private int id;

    private String name;

    public XMLAbstractOneXOneBiEntityB() {

    }

    public XMLAbstractOneXOneBiEntityB(int id, String name) {
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
        return "XMLAbstractOneXOneBiEntityB [id=" + id + ", name=" + name + "]";
    }
}
