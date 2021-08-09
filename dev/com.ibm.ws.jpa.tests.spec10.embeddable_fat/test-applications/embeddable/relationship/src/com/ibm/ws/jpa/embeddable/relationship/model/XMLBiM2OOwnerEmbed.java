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
public class XMLBiM2OOwnerEmbed implements java.io.Serializable {

    private XMLBiO2MInverseEntity biO2MInverseEntity;

    public XMLBiM2OOwnerEmbed() {
    }

    public XMLBiM2OOwnerEmbed(XMLBiO2MInverseEntity biO2MInverseEntity) {
        this.biO2MInverseEntity = biO2MInverseEntity;
    }

    public XMLBiO2MInverseEntity getBiO2MInverseEntity() {
        return this.biO2MInverseEntity;
    }

    public void setBiO2MInverseEntity(XMLBiO2MInverseEntity biO2MInverseEntity) {
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
        if (!(otherObject instanceof XMLBiM2OOwnerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biO2MInverseEntity=" + biO2MInverseEntity;
    }

}
