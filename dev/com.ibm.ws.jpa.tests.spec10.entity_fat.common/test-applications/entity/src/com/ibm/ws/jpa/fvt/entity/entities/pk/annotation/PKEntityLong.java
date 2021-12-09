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

package com.ibm.ws.jpa.fvt.entity.entities.pk.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.ibm.ws.jpa.fvt.entity.entities.pk.AbstractPKEntity;

@Entity
public class PKEntityLong extends AbstractPKEntity {
    @Id
    private long pkey;
    private int intVal;

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public long getPkey() {
        return pkey;
    }

    public void setPkey(long pkey) {
        this.pkey = pkey;
    }

    /*
     * Support for IPKEntities with a long/Long primary key
     */
    @Override
    public long getLongPK() {
        return getPkey();
    }

    @Override
    public void setLongPK(long pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "PKEntityLong [pkey=" + pkey + "]";
    }
}