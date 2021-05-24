/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.relationship.model;

import java.util.HashSet;

public class XMLBiO2MInverseEntity {

    protected int id;

    private HashSet<XMLJPAEmbeddableRelationshipEntity> owners;

    public XMLBiO2MInverseEntity() {
    }

    public XMLBiO2MInverseEntity(int id,
                                 HashSet<XMLJPAEmbeddableRelationshipEntity> owners) {
        this.id = id;
        this.owners = owners;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public HashSet<XMLJPAEmbeddableRelationshipEntity> getOwners() {
        return this.owners;
    }

    public void setOwners(HashSet<XMLJPAEmbeddableRelationshipEntity> owners) {
        this.owners = owners;
    }

    @Override
    public int hashCode() {
        int tmp = 37 * 17 + id;
        if (owners != null) {
            for (XMLJPAEmbeddableRelationshipEntity owner : owners)
                tmp = tmp * 37 + owner.getId();
        }
        return tmp;
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLBiO2MInverseEntity))
            return false;
        return (otherObject.hashCode() == hashCode());
        // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        if (owners != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("(id=" + id + " is inversed by [");
            for (XMLJPAEmbeddableRelationshipEntity owner : owners)
                sb.append(" " + owner.getId());
            sb.append("])");
            return sb.toString();
        }
        return "(id=" + id + " is not owned)";
    }

}
