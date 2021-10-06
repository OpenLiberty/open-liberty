/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.xml;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IPKJoinEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IPKJoinEntityB;

public class XMLPKJoinOOEnA implements IPKJoinEntityA {
    private int id;

    private String strVal;

    private XMLPKJoinOOEnB entityB;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getStrVal() {
        return strVal;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setStrVal(String strVal) {
        this.strVal = strVal;
    }

    public XMLPKJoinOOEnB getEntityB() {
        return entityB;
    }

    public void setEntityB(XMLPKJoinOOEnB entityB) {
        this.entityB = entityB;
    }

    @Override
    public IPKJoinEntityB getIPKJoinEntityB() {
        return getEntityB();
    }

    @Override
    public void setIPKJoinEntityB(IPKJoinEntityB entityB) {
        setEntityB((XMLPKJoinOOEnB) entityB);
    }

    @Override
    public String toString() {
        return "XMLPKJoinOOEnA [id=" + id + ", strVal=" + strVal + "]";
    }
}
