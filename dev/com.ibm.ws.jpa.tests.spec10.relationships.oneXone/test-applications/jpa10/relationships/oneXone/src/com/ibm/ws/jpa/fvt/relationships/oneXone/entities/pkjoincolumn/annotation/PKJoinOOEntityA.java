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

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IPKJoinEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IPKJoinEntityB;

@Entity
public class PKJoinOOEntityA implements IPKJoinEntityA {
    @Id
    private int id;

    private String strVal;

    @OneToOne
    @PrimaryKeyJoinColumn
    private PKJoinOOEntityB entityB;

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

    public void setEntityB(PKJoinOOEntityB entityB) {
        this.entityB = entityB;
    }

    public PKJoinOOEntityB getEntityB() {
        return entityB;
    }

    @Override
    public IPKJoinEntityB getIPKJoinEntityB() {
        return getEntityB();
    }

    @Override
    public void setIPKJoinEntityB(IPKJoinEntityB entityB) {
        setEntityB((PKJoinOOEntityB) entityB);
    }

    @Override
    public String toString() {
        return "PKJoinOOEntityA [id=" + id + ", strVal=" + strVal + "]";
    }
}
