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

public class XMLPKEntityJavaUtilDate extends AbstractPKEntity {
    private java.util.Date pkey;
    private int intVal;

    public XMLPKEntityJavaUtilDate() {

    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public java.util.Date getPkey() {
        return pkey;
    }

    public void setPkey(java.util.Date pkey) {
        this.pkey = pkey;
    }

    /*
     * Support for IPKEntities with a java.util.Date primary key
     */
    @Override
    public java.util.Date getJavaUtilDatePK() {
        return getPkey();
    }

    @Override
    public void setJavaUtilDatePK(java.util.Date pkey) {
        setPkey(pkey);
    }

    @Override
    public String toString() {
        return "XMLPKEntityJavaUtilDate [pkey=" + pkey + "]";
    }

}
