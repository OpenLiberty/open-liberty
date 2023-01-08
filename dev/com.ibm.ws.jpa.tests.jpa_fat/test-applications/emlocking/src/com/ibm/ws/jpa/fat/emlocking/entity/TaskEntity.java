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

package com.ibm.ws.jpa.fat.emlocking.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class TaskEntity {

    @Id
    private int id;

    @Version
    private int version;

    private int intData;

    private String strData;

    public TaskEntity() {

    }

    public TaskEntity(int id, int intData, String strData) {
        this.id = id;
        this.intData = intData;
        this.strData = strData;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getIntData() {
        return this.intData;
    }

    public void setIntData(int intData) {
        this.intData = intData;
    }

    public String getStrData() {
        return strData;
    }

    public void setStrData(String strData) {
        this.strData = strData;
    }

    @Override
    public String toString() {
        return "id=" + getId() + ", version=" + getVersion() + ", intData=" + getIntData() + ", strData=" + getStrData();
    }
}
