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

package com.ibm.ws.jpa.fvt.entity.entities.versioning.xml;

import javax.persistence.Transient;

import com.ibm.ws.jpa.fvt.entity.entities.IVersionedEntity;

public class XMLVersionedSqlTimestampEntity implements IVersionedEntity {
    private int id;

    private java.sql.Timestamp version;

    private int intVal;

    private String stringVal;

    public XMLVersionedSqlTimestampEntity() {
        // version = new java.sql.Timestamp(System.currentTimeMillis());
        intVal = 0;
        stringVal = "";
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    @Override
    public String getStringVal() {
        return stringVal;
    }

    @Override
    public void setStringVal(String stringVal) {
        this.stringVal = stringVal;
    }

    public java.sql.Timestamp getVersion() {
        return version;
    }

    public void setVersion(java.sql.Timestamp version) {
        this.version = version;
    }

    @Override
    @Transient
    public Object getVersionObj() {
        return getVersion();
    }

    @Override
    public String toString() {
        return "XMLVersionedSqlTimestampEntity [id=" + id + ", version=" + version + ", intVal=" + intVal
               + ", stringVal=" + stringVal + "]";
    }
}
