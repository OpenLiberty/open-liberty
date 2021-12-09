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

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

@Embeddable
@SuppressWarnings("serial")
public class BiO2MInverseEmbed implements java.io.Serializable {

    @OneToMany(mappedBy = "inverse", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private HashSet<BiM2OOwnerEntity> biM2OOwnerEntities;

    public BiO2MInverseEmbed() {
    }

    public BiO2MInverseEmbed(HashSet<BiM2OOwnerEntity> biM2OOwnerEntities) {
        this.biM2OOwnerEntities = biM2OOwnerEntities;
    }

    public HashSet<BiM2OOwnerEntity> getBiM2OOwnerEntities() {
        return this.biM2OOwnerEntities;
    }

    public void setBiM2OOwnerEntities(HashSet<BiM2OOwnerEntity> biM2OOwnerEntities) {
        this.biM2OOwnerEntities = biM2OOwnerEntities;
    }

    @Override
    public int hashCode() {
        int tmp = 37 * 17;
        if (biM2OOwnerEntities != null) {
            for (BiM2OOwnerEntity biM2OOwnerEntity : biM2OOwnerEntities)
                tmp = tmp * 37 + biM2OOwnerEntity.hashCode();
        }
        return tmp;
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof BiO2MInverseEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biM2OOwnerEntities=" + biM2OOwnerEntities;
    }

}
