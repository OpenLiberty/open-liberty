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
public class PKEntityString extends AbstractPKEntity {
    @Id
    private String pkey;
    private int intVal;

    public PKEntityString() {

    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public String getPkey() {
        return pkey;
    }

    public void setPkey(String pkey) {
        this.pkey = pkey;
    }

    /*
     * Support for IPKEntities with a String primary key
     */
    @Override
    public String getStringPK() {
        return getPkey();
    }

    @Override
    public void setStringPK(String pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "PKEntityString [pkey=" + pkey + "]";
    }
}
