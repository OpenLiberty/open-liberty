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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "AnnBiO2OOwn")
public class BiO2OOwnerEntity {

    @Id
    private int id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "BIO2OOWNER")
    private JPAEmbeddableRelationshipEntity inverse;

    public BiO2OOwnerEntity() {
    }

    public BiO2OOwnerEntity(int id, JPAEmbeddableRelationshipEntity inverse) {
        this.id = id;
        this.inverse = inverse;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public JPAEmbeddableRelationshipEntity getInverse() {
        return this.inverse;
    }

    public void setInverse(JPAEmbeddableRelationshipEntity inverse) {
        this.inverse = inverse;
    }

    @Override
    public int hashCode() {
        return ((37 * 17 + id) * 37 + ((inverse != null) ? inverse.getId() : 0));
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof BiO2OOwnerEntity))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        if (inverse != null)
            return "(id=" + id + " is inversed by " + inverse.getId() + ")";
        return "(id=" + id + " is not inversed)";
    }
}
