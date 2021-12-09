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
public class XMLBiO2OOwnerEmbed implements java.io.Serializable {

    private XMLBiO2OInverseEntity biO2OInverseEntity;

    public XMLBiO2OOwnerEmbed() {
    }

    public XMLBiO2OOwnerEmbed(XMLBiO2OInverseEntity biO2OInverseEntity) {
        this.biO2OInverseEntity = biO2OInverseEntity;
    }

    public XMLBiO2OInverseEntity getBiO2OInverseEntity() {
        return this.biO2OInverseEntity;
    }

    public void setBiO2OInverseEntity(XMLBiO2OInverseEntity biO2OInverseEntity) {
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
        if (!(otherObject instanceof XMLBiO2OOwnerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biO2OInverseEntity=" + biO2OInverseEntity;
    }

}
