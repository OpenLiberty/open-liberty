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

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.ibm.ws.jpa.fvt.inheritance.entities.msc.IMSC;

@MappedSuperclass
public abstract class AnoMSC implements IMSC {
    @Id
    private int id;

    private String name;

    @Column(name = "originalNameAO")
    private String nameAO; // The entity class extending from this Mapped Superclass should use attributeOverride

    private transient String parsedName;

    public AnoMSC() {
        super();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getNameAO() {
        return nameAO;
    }

    public void setNameAO(String nameAO) {
        this.nameAO = nameAO;
    }

    public String getParsedName() {
        return parsedName;
    }

    public void setParsedName(String parsedName) {
        this.parsedName = parsedName;
    }

    @Override
    public String toString() {
        return "AnoMSC [id=" + id + ", name=" + name + ", nameAO=" + nameAO + "]";
    }
}
