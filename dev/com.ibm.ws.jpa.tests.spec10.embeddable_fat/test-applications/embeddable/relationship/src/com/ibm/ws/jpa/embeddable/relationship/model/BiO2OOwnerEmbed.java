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
public class BiO2OOwnerEmbed implements java.io.Serializable {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "BIO2OINVERSE")
    private BiO2OInverseEntity biO2OInverseEntity;

    public BiO2OOwnerEmbed() {
    }

    public BiO2OOwnerEmbed(BiO2OInverseEntity biO2OInverseEntity) {
        this.biO2OInverseEntity = biO2OInverseEntity;
    }

    public BiO2OInverseEntity getBiO2OInverseEntity() {
        return this.biO2OInverseEntity;
    }

    public void setBiO2OInverseEntity(BiO2OInverseEntity biO2OInverseEntity) {
        this.biO2OInverseEntity = biO2OInverseEntity;
    }

    @Override
    public int hashCode() {
        if (biO2OInverseEntity == null)
            return 37 * 17;
        return (37 * 17 + biO2OInverseEntity.hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof BiO2OOwnerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biO2OInverseEntity=" + biO2OInverseEntity;
    }

}
