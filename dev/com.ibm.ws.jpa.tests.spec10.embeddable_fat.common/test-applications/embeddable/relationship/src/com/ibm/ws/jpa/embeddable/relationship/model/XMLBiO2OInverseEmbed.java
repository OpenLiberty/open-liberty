/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.relationship.model;

@SuppressWarnings("serial")
public class XMLBiO2OInverseEmbed implements java.io.Serializable {

    private XMLBiO2OOwnerEntity biO2OOwnerEntity;

    public XMLBiO2OInverseEmbed() {
    }

    public XMLBiO2OInverseEmbed(XMLBiO2OOwnerEntity biO2OOwnerEntity) {
        this.biO2OOwnerEntity = biO2OOwnerEntity;
    }

    public XMLBiO2OOwnerEntity getBiO2OOwnerEntity() {
        return this.biO2OOwnerEntity;
    }

    public void setBiO2OOwnerEntity(XMLBiO2OOwnerEntity biO2OOwnerEntity) {
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
        if (!(otherObject instanceof XMLBiO2OInverseEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biO2OOwnerEntity=" + biO2OOwnerEntity;
    }

}
