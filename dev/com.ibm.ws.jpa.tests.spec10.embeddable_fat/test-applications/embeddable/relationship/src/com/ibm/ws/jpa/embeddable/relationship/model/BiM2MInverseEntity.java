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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name = "AnnBiM2MInv")
public class BiM2MInverseEntity {

    @Id
    protected int id;

    @ManyToMany(mappedBy = "biM2MOwnerEmbed.biM2MInverseEntities")
    private HashSet<JPAEmbeddableRelationshipEntity> owners;

    public BiM2MInverseEntity() {
    }

    public BiM2MInverseEntity(int id,
                              HashSet<JPAEmbeddableRelationshipEntity> owners) {
        this.id = id;
        this.owners = owners;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public HashSet<JPAEmbeddableRelationshipEntity> getOwners() {
        return this.owners;
    }

    public void setOwners(HashSet<JPAEmbeddableRelationshipEntity> owners) {
        this.owners = owners;
    }

    @Override
    public int hashCode() {
        int tmp = 37 * 17 + id;
        if (owners != null) {
            for (JPAEmbeddableRelationshipEntity owner : owners)
                tmp = tmp * 37 + owner.getId();
        }
        return tmp;
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof BiM2MInverseEntity))
            return false;
        return (otherObject.hashCode() == hashCode());
        // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        if (owners != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("(id=" + id + " is owned by [");
            for (JPAEmbeddableRelationshipEntity owner : owners)
                sb.append(" " + owner.getId());
            sb.append("])");
            return sb.toString();
        }
        return "(id=" + id + " is not owned)";
    }

}
