/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.cache.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class CacheEntitySimple2 {
    @Id
    private int id;

    private String strVal;
    private int intVal;

    public CacheEntitySimple2() {

    }

    public CacheEntitySimple2(int id, String strVal, int intVal) {
        this.id = id;
        this.strVal = strVal;
        this.intVal = intVal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIntVal() {
        return intVal;
    }

    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public String getStrVal() {
        return strVal;
    }

    public void setStrVal(String strVal) {
        this.strVal = strVal;
    }
}
