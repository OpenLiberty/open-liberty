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

@SuppressWarnings("serial")
public class XMLBiO2OOwnerAssociationOverridesEmbed implements java.io.Serializable {

    private XMLBiO2OInverseAssociationOverridesEntity biO2OInverseAssociationOverridesEntity;

    public XMLBiO2OOwnerAssociationOverridesEmbed() {
    }

    public XMLBiO2OOwnerAssociationOverridesEmbed(
                                                  XMLBiO2OInverseAssociationOverridesEntity biO2OInverseAssociationOverridesEntity) {
        this.biO2OInverseAssociationOverridesEntity = biO2OInverseAssociationOverridesEntity;
    }

    public XMLBiO2OInverseAssociationOverridesEntity getBiO2OInverseAssociationOverridesEntity() {
        return this.biO2OInverseAssociationOverridesEntity;
    }

    public void setBiO2OInverseAssociationOverridesEntity(
                                                          XMLBiO2OInverseAssociationOverridesEntity biO2OInverseAssociationOverridesEntity) {
        this.biO2OInverseAssociationOverridesEntity = biO2OInverseAssociationOverridesEntity;
    }

    @Override
    public int hashCode() {
        if (biO2OInverseAssociationOverridesEntity == null)
            return 37 * 17;
        return (37 * 17 + biO2OInverseAssociationOverridesEntity.hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLBiO2OOwnerAssociationOverridesEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biO2OInverseAssociationOverridesEntity=" + biO2OInverseAssociationOverridesEntity;
    }

}
