/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.ibm.ws.jpa.fvt.entity.entities.IPKGeneratorEntity;

@Entity
public class PKGenSequenceType2Entity implements IPKGeneratorEntity {
    @Id
//    @SequenceGenerator(name = "AnnSeqGen1", sequenceName = "AnnSeqGen1_Seq")
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AnnSeqGen1")
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
        return "PKGenSequenceType2Entity [id=" + id + ", intVal=" + intVal + "]";
    }
}
