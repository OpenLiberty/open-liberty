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

public class XMLBiO2OOwnerEntity {

    private int id;

    private XMLJPAEmbeddableRelationshipEntity inverse;

    public XMLBiO2OOwnerEntity() {
    }

    public XMLBiO2OOwnerEntity(int id, XMLJPAEmbeddableRelationshipEntity inverse) {
        this.id = id;
        this.inverse = inverse;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public XMLJPAEmbeddableRelationshipEntity getInverse() {
        return this.inverse;
    }

    public void setInverse(XMLJPAEmbeddableRelationshipEntity inverse) {
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
        if (!(otherObject instanceof XMLBiO2OOwnerEntity))
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
