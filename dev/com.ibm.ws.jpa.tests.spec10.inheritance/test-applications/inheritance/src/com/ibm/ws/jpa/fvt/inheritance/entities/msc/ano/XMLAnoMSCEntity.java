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

import com.ibm.ws.jpa.fvt.inheritance.entities.msc.IMSCEntity;

public class XMLAnoMSCEntity extends AnoMSC implements IMSCEntity {
    private String description;

    public XMLAnoMSCEntity() {
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
        return "XMLAnoMSCEntity [description=" + description + ", getId()=" + getId() + ", getName()=" + getName()
               + ", getNameAO()=" + getNameAO() + ", getParsedName()=" + getParsedName() + "]";
    }
}
