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
import javax.persistence.OneToOne;

@Embeddable
@SuppressWarnings("serial")
public class BiO2OInverseEmbed implements java.io.Serializable {

    @OneToOne(mappedBy = "inverse", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private BiO2OOwnerEntity biO2OOwnerEntity;

    public BiO2OInverseEmbed() {
    }

    public BiO2OInverseEmbed(BiO2OOwnerEntity biO2OOwnerEntity) {
        this.biO2OOwnerEntity = biO2OOwnerEntity;
    }

    public BiO2OOwnerEntity getBiO2OOwnerEntity() {
        return this.biO2OOwnerEntity;
    }

    public void setBiO2OOwnerEntity(BiO2OOwnerEntity biO2OOwnerEntity) {
        this.biO2OOwnerEntity = biO2OOwnerEntity;
    }

    @Override
    public int hashCode() {
        if (biO2OOwnerEntity == null)
            return 37 * 17;
        return (37 * 17 + biO2OOwnerEntity.hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof BiO2OInverseEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biO2OOwnerEntity=" + biO2OOwnerEntity;
    }

}
