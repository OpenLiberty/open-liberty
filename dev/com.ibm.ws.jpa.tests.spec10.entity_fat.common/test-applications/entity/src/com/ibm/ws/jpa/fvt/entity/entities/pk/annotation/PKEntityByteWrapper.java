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
public class PKEntityByteWrapper extends AbstractPKEntity {
    @Id
    private Byte pkey;
    private int intVal;

    public PKEntityByteWrapper() {

    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public Byte getPkey() {
        return pkey;
    }

    public void setPkey(Byte pkey) {
        this.pkey = pkey;
    }

    /*
     * Support for IPKEntities with a byte/Byte primary key
     */
    @Override
    public Byte getByteWrapperPK() {
        return getPkey();
    }

    @Override
    public void setByteWrapperPK(Byte pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "PKEntityByteWrapper [pkey=" + pkey + "]";
    }

}
