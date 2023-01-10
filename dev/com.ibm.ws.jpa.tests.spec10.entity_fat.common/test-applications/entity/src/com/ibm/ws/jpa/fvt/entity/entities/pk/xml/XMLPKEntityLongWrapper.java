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

public class XMLPKEntityLongWrapper extends AbstractPKEntity {
    private Long pkey;
    private int intVal;

    public XMLPKEntityLongWrapper() {

    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public Long getPkey() {
        return pkey;
    }

    public void setPkey(Long pkey) {
        this.pkey = pkey;
    }

    /*
     * Support for IPKEntities with a long/Long primary key
     */
    @Override
    public Long getLongWrapperPK() {
        return getPkey();
    }

    @Override
    public void setLongWrapperPK(Long pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "XMLPKEntityLongWrapper [pkey=" + pkey + "]";
    }
}
