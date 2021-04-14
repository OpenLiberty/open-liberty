/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.inheritance.entities.msc.ano;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;

import com.ibm.ws.jpa.fvt.inheritance.entities.msc.IMSCEntity;

@Entity
@AttributeOverride(name = "nameAO", column = @Column(name = "overridenNameAO"))
public class AnoAnoMSCEntity extends AnoMSC implements IMSCEntity {
    private String description;

    public AnoAnoMSCEntity() {
        super();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "AnoAnoMSCEntity [description=" + description + ", getId()=" + getId() + ", getName()=" + getName()
               + ", getNameAO()=" + getNameAO() + ", getParsedName()=" + getParsedName() + "]";
    }
}
