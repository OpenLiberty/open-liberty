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
public class XMLUniM2OOwnerEmbed implements java.io.Serializable {

    private XMLUniO2ODummyEntity uniO2MDummyEntity;

    public XMLUniM2OOwnerEmbed() {
    }

    public XMLUniM2OOwnerEmbed(XMLUniO2ODummyEntity uniO2MDummyEntity) {
        this.uniO2MDummyEntity = uniO2MDummyEntity;
    }

    public XMLUniO2ODummyEntity getUniO2MDummyEntity() {
        return this.uniO2MDummyEntity;
    }

    public void setUniO2MDummyEntity(XMLUniO2ODummyEntity uniO2MDummyEntity) {
        this.uniO2MDummyEntity = uniO2MDummyEntity;
    }

    @Override
    public int hashCode() {
        if (uniO2MDummyEntity == null)
            return 37 * 17;
        return (37 * 17 + uniO2MDummyEntity.hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLUniM2OOwnerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "uniO2MDummyEntity=" + uniO2MDummyEntity;
    }

}
