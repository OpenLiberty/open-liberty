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

package com.ibm.ws.jpa.fvt.entity.entities.pk.xml;

import com.ibm.ws.jpa.fvt.entity.entities.pk.AbstractPKEntity;

public class XMLPKEntityByteWrapper extends AbstractPKEntity {
    private Byte pkey;
    private int intVal;

    public XMLPKEntityByteWrapper() {

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
        return "XMLPKEntityByteWrapper [pkey=" + pkey + "]";
    }

}
