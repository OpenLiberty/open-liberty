/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entitylocking.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "EL10LockEntB")
public class LockEntityB {
    @Id
    @Column(name = "ID")
    private int id;

    @Basic
    @Column(name = "STRDATA")
    private String strData;

    @Version
    @Column(name = "VERSION")
    private int version;

    public LockEntityB() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStrData() {
        return strData;
    }

    public void setStrData(String strData) {
        this.strData = strData;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "LockEntityB [id=" + id + ", strData=" + strData + ", version="
               + version + "]";
    }
}
