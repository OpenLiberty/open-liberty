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

@SuppressWarnings("serial")
public class XMLBiM2MOwnerEmbed implements java.io.Serializable {

    private HashSet<XMLBiM2MInverseEntity> biM2MInverseEntities;

    public XMLBiM2MOwnerEmbed() {
    }

    public XMLBiM2MOwnerEmbed(
                              HashSet<XMLBiM2MInverseEntity> biM2MInverseEntities) {
        this.biM2MInverseEntities = biM2MInverseEntities;
    }

    public HashSet<XMLBiM2MInverseEntity> getBiM2MInverseEntities() {
        return this.biM2MInverseEntities;
    }

    public void setBiM2MInverseEntities(
                                        HashSet<XMLBiM2MInverseEntity> biM2MInverseEntities) {
        this.biM2MInverseEntities = biM2MInverseEntities;
    }

    @Override
    public int hashCode() {
        int tmp = 37 * 17;
        if (biM2MInverseEntities != null) {
            for (XMLBiM2MInverseEntity biM2MInverseEntity : biM2MInverseEntities)
                tmp = tmp * 37 + biM2MInverseEntity.hashCode();
        }
        return tmp;
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLBiM2MOwnerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "biM2MInverseEntities=" + biM2MInverseEntities;
    }

}
