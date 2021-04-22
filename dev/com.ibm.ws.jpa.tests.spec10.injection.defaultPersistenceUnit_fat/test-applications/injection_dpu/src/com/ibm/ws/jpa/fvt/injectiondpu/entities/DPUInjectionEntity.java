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

package com.ibm.ws.jpa.fvt.injectiondpu.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class DPUInjectionEntity {
    @Id
    private long id;

    private String dataStr;
    private int dataInt;

    public DPUInjectionEntity() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDataStr() {
        return dataStr;
    }

    public void setDataStr(String dataStr) {
        this.dataStr = dataStr;
    }

    public int getDataInt() {
        return dataInt;
    }

    public void setDataInt(int dataInt) {
        this.dataInt = dataInt;
    }

    @Override
    public String toString() {
        return "DPUInjectionEntity [id=" + id + ", dataStr=" + dataStr + ", dataInt=" + dataInt + "]";
    }
}
