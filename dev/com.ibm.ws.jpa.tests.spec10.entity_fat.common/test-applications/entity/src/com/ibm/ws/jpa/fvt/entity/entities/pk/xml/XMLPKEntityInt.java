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

package com.ibm.ws.jpa.fvt.entity.entities.pk.xml;

import com.ibm.ws.jpa.fvt.entity.entities.pk.AbstractPKEntity;

public class XMLPKEntityInt extends AbstractPKEntity {
    private int pkey;
    private int intVal;

    public XMLPKEntityInt() {
    }

    public int getPkey() {
        return pkey;
    }

    public void setPkey(int pkey) {
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
     * Support for IPKEntities with an int primary key
     */
    @Override
    public int getIntPK() {
        return getPkey();
    }

    @Override
    public void setIntPK(int pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "XMLPKEntityInt [pkey=" + pkey + "]";
    }
}
