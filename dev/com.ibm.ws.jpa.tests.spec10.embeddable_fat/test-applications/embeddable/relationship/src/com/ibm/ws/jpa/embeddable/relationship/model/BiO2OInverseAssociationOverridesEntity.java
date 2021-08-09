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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "AnnBiO2OInvAO")
public class BiO2OInverseAssociationOverridesEntity {

    @Id
    protected int id;

    @OneToOne(mappedBy = "biO2OOwnerAssociationOverridesEmbed.biO2OInverseAssociationOverridesEntity")
    private JPAEmbeddableRelationshipEntity owner;

    public BiO2OInverseAssociationOverridesEntity() {
    }

    public BiO2OInverseAssociationOverridesEntity(int id, JPAEmbeddableRelationshipEntity owner) {
        this.id = id;
        this.owner = owner;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public JPAEmbeddableRelationshipEntity getOwner() {
        return this.owner;
    }

    public void setOwner(JPAEmbeddableRelationshipEntity owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return ((37 * 17 + id) * 37 + ((owner != null) ? owner.getId() : 0));
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof BiO2OInverseAssociationOverridesEntity))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        if (owner != null)
            return "(id=" + id + " is inversed by " + owner.getId() + ")";
        return "(id=" + id + " is not owned)";
    }

}
