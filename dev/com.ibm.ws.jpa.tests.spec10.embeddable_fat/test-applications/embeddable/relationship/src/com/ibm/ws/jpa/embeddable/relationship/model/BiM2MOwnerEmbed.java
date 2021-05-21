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
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

@Embeddable
@SuppressWarnings("serial")
public class BiM2MOwnerEmbed implements java.io.Serializable {

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "AnnBiM2MOwnEm", joinColumns = @JoinColumn(name = "BIM2MINVERSE"), inverseJoinColumns = @JoinColumn(name = "BIM2MOWNER"))
    private HashSet<BiM2MInverseEntity> biM2MInverseEntities;

    public BiM2MOwnerEmbed() {
    }

    public BiM2MOwnerEmbed(HashSet<BiM2MInverseEntity> biM2MInverseEntities) {
        this.biM2MInverseEntities = biM2MInverseEntities;
    }

    public HashSet<BiM2MInverseEntity> getBiM2MInverseEntities() {
        return this.biM2MInverseEntities;
    }

    public void setBiM2MInverseEntities(
                                        HashSet<BiM2MInverseEntity> biM2MInverseEntities) {
        this.biM2MInverseEntities = biM2MInverseEntities;
    }

    @Override
    public int hashCode() {
        int tmp = 37 * 17;
        if (biM2MInverseEntities != null) {
            for (BiM2MInverseEntity biM2MInverseEntity : biM2MInverseEntities)
                tmp = tmp * 37 + biM2MInverseEntity.hashCode();
        }
        return tmp;
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof BiM2MOwnerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biM2MInverseEntities=" + biM2MInverseEntities;
    }

}
