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
public class PKEntityShort extends AbstractPKEntity {
    @Id
    private short pkey;
    private int intVal;

    public PKEntityShort() {

    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public short getPkey() {
        return pkey;
    }

    public void setPkey(short pkey) {
        this.pkey = pkey;
    }

    /*
     * Support for IPKEntities with a short/Short primary key
     */
    @Override
    public short getShortPK() {
        return getPkey();
    }

    @Override
    public void setShortPK(short pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "PKEntityShort [pkey=" + pkey + "]";
    }
}
