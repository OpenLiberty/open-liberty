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
public class PKEntityJavaSqlDate extends AbstractPKEntity {
    @Id
    private java.sql.Date pkey;
    private int intVal;

    public PKEntityJavaSqlDate() {

    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public java.sql.Date getPkey() {
        return pkey;
    }

    public void setPkey(java.sql.Date pkey) {
        this.pkey = pkey;
    }

    /*
     * Support for IPKEntities with a java.sql.Date primary key
     */
    @Override
    public java.sql.Date getJavaSqlDatePK() {
        return getPkey();
    }

    @Override
    public void setJavaSqlDatePK(java.sql.Date pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "PKEntityJavaSqlDate [pkey=" + pkey + "]";
    }
}
