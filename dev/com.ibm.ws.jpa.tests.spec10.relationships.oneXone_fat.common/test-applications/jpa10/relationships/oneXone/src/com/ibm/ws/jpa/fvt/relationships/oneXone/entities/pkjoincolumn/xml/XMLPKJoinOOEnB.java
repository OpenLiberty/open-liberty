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

public class XMLPKJoinOOEnB implements IPKJoinEntityB {
    private int id;

    private int intVal;

    XMLPKJoinOOEnA entityA;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public XMLPKJoinOOEnA getEntityA() {
        return entityA;
    }

    public void setEntityA(XMLPKJoinOOEnA entity) {
        this.entityA = entity;
    }

    @Override
    public IPKJoinEntityA getIPKJoinEntityA() {
        return getEntityA();
    }

    @Override
    public void setIPKJoinEntityA(IPKJoinEntityA entity) {
        setEntityA((XMLPKJoinOOEnA) entity);
    }

    @Override
    public String toString() {
        return "XMLPKJoinOOEnB [id=" + id + ", intVal=" + intVal + "]";
    }
}
