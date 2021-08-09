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
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Embeddable
@SuppressWarnings("serial")
public class BiO2OOwnerAssociationOverridesEmbed implements java.io.Serializable {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "doesnotexist")
    private BiO2OInverseAssociationOverridesEntity biO2OInverseAssociationOverridesEntity;

    public BiO2OOwnerAssociationOverridesEmbed() {
    }

    public BiO2OOwnerAssociationOverridesEmbed(
                                               BiO2OInverseAssociationOverridesEntity biO2OInverseAssociationOverridesEntity) {
        this.biO2OInverseAssociationOverridesEntity = biO2OInverseAssociationOverridesEntity;
    }

    public BiO2OInverseAssociationOverridesEntity getBiO2OInverseAssociationOverridesEntity() {
        return this.biO2OInverseAssociationOverridesEntity;
    }

    public void setBiO2OInverseAssociationOverridesEntity(
                                                          BiO2OInverseAssociationOverridesEntity biO2OInverseAssociationOverridesEntity) {
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
        if (!(otherObject instanceof BiO2OOwnerAssociationOverridesEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biO2OInverseAssociationOverridesEntity=" + biO2OInverseAssociationOverridesEntity;
    }

}
