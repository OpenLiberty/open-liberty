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
public class XMLUniO2OOwnerPropertyAccessEmbed implements java.io.Serializable {

    private XMLUniO2ODummyEntity uniO2ODummyEntity_PA;

    public XMLUniO2OOwnerPropertyAccessEmbed() {
    }

    public XMLUniO2OOwnerPropertyAccessEmbed(
                                             XMLUniO2ODummyEntity uniO2ODummyEntity_PA) {
        this.uniO2ODummyEntity_PA = uniO2ODummyEntity_PA;
    }

    public XMLUniO2ODummyEntity getUniO2ODummyEntity_PA() {
        return this.uniO2ODummyEntity_PA;
    }

    public void setUniO2ODummyEntity_PA(
                                        XMLUniO2ODummyEntity uniO2ODummyEntity_PA) {
        this.uniO2ODummyEntity_PA = uniO2ODummyEntity_PA;
    }

    @Override
    public int hashCode() {
        if (getUniO2ODummyEntity_PA() == null)
            return 37 * 17;
        return (37 * 17 + getUniO2ODummyEntity_PA().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLUniO2OOwnerPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "uniO2ODummyEntity_PA=" + getUniO2ODummyEntity_PA();
    }

}
