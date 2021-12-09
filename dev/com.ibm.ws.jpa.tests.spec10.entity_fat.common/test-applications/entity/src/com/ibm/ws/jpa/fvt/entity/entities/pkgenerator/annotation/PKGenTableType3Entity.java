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

package com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TableGenerator;

import com.ibm.ws.jpa.fvt.entity.entities.IPKGeneratorEntity;

@Entity
public class PKGenTableType3Entity implements IPKGeneratorEntity {
    @Id
    @TableGenerator(name = "TableType3Generator", table = "TableIDGenTable", pkColumnName = "GEN_NAME", valueColumnName = "GEN_VAL")
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "TableType3Generator")
    private int id;

    private int intVal;

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    @Override
    public String toString() {
        return "PKGenTableType3Entity [id=" + id + ", intVal=" + intVal + "]";
    }
}
