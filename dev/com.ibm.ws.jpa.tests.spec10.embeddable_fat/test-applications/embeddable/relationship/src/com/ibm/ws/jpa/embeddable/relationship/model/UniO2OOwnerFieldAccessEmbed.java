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
public class UniO2OOwnerFieldAccessEmbed implements java.io.Serializable {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "UNIO2ODUMMYFA")
    private UniO2ODummyEntity uniO2ODummyEntity_FA;

    public UniO2OOwnerFieldAccessEmbed() {
    }

    public UniO2OOwnerFieldAccessEmbed(UniO2ODummyEntity uniO2ODummyEntity_FA) {
        this.uniO2ODummyEntity_FA = uniO2ODummyEntity_FA;
    }

    public UniO2ODummyEntity getUniO2ODummyEntity_FA() {
        return this.uniO2ODummyEntity_FA;
    }

    public void setUniO2ODummyEntity_FA(UniO2ODummyEntity uniO2ODummyEntity_FA) {
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
        if (!(otherObject instanceof UniO2OOwnerFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "uniO2ODummyEntity_FA=" + uniO2ODummyEntity_FA;
    }

}
