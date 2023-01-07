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

public class XMLPKEntityChar extends AbstractPKEntity {
    private char pkey;
    private int intVal;

    public XMLPKEntityChar() {

    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public char getPkey() {
        return pkey;
    }

    public void setPkey(char pkey) {
        this.pkey = pkey;
    }

    /*
     * Support for IPKEntities with a char/Char primary key
     */
    @Override
    public char getCharPK() {
        return getPkey();
    }

    @Override
    public void setCharPK(char pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "XMLPKEntityChar [pkey=" + pkey + "]";
    }
}
