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
import javax.persistence.ManyToOne;

@Embeddable
@SuppressWarnings("serial")
public class BiM2OOwnerEmbed implements java.io.Serializable {

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "BIM2OINVERSE")
    private BiO2MInverseEntity biO2MInverseEntity;

    public BiM2OOwnerEmbed() {
    }

    public BiM2OOwnerEmbed(BiO2MInverseEntity biO2MInverseEntity) {
        this.biO2MInverseEntity = biO2MInverseEntity;
    }

    public BiO2MInverseEntity getBiO2MInverseEntity() {
        return this.biO2MInverseEntity;
    }

    public void setBiO2MInverseEntity(BiO2MInverseEntity biO2MInverseEntity) {
        this.biO2MInverseEntity = biO2MInverseEntity;
    }

    @Override
    public int hashCode() {
        if (biO2MInverseEntity == null)
            return 37 * 17;
        return (37 * 17 + biO2MInverseEntity.hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof BiM2OOwnerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biO2MInverseEntity=" + biO2MInverseEntity;
    }

}
