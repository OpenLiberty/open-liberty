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

package com.ibm.ws.jpa.fvt.entity.entities.pk.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.ibm.ws.jpa.fvt.entity.entities.pk.AbstractPKEntity;

@Entity
public class PKEntityByte extends AbstractPKEntity {
    @Id
    private byte pkey;
    private int intVal;

    public PKEntityByte() {
    }

    public byte getPkey() {
        return pkey;
    }

    public void setPkey(byte pkey) {
        this.pkey = pkey;
    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    /*
     * Support for IPKEntities with a byte/Byte primary key
     */
    @Override
    public byte getBytePK() {
        return getPkey();
    }

    @Override
    public void setBytePK(byte pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "PKEntityByte [pkey=" + pkey + "]";
    }
}
