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
public class UniO2OOwnerPropertyAccessEmbed implements java.io.Serializable {

    private UniO2ODummyEntity uniO2ODummyEntity_PA;

    public UniO2OOwnerPropertyAccessEmbed() {
    }

    public UniO2OOwnerPropertyAccessEmbed(UniO2ODummyEntity uniO2ODummyEntity_PA) {
        this.uniO2ODummyEntity_PA = uniO2ODummyEntity_PA;
    }

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "UNIO2ODUMMYPA")
    public UniO2ODummyEntity getUniO2ODummyEntity_PA() {
        return this.uniO2ODummyEntity_PA;
    }

    public void setUniO2ODummyEntity_PA(UniO2ODummyEntity uniO2ODummyEntity_PA) {
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
        if (!(otherObject instanceof UniO2OOwnerPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "uniO2ODummyEntity_PA=" + getUniO2ODummyEntity_PA();
    }

}
