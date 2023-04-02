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
public class XMLUniO2OOwnerFieldAccessEmbed implements java.io.Serializable {

    private XMLUniO2ODummyEntity uniO2ODummyEntity_FA;

    public XMLUniO2OOwnerFieldAccessEmbed() {
    }

    public XMLUniO2OOwnerFieldAccessEmbed(
                                          XMLUniO2ODummyEntity uniO2ODummyEntity_FA) {
        this.uniO2ODummyEntity_FA = uniO2ODummyEntity_FA;
    }

    public XMLUniO2ODummyEntity getUniO2ODummyEntity_FA() {
        return this.uniO2ODummyEntity_FA;
    }

    public void setUniO2ODummyEntity_FA(
                                        XMLUniO2ODummyEntity uniO2ODummyEntity_FA) {
        this.uniO2ODummyEntity_FA = uniO2ODummyEntity_FA;
    }

    @Override
    public int hashCode() {
        if (uniO2ODummyEntity_FA == null)
            return 37 * 17;
        return (37 * 17 + uniO2ODummyEntity_FA.hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLUniO2OOwnerFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "uniO2ODummyEntity_FA=" + uniO2ODummyEntity_FA;
    }

}
