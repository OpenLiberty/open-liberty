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

package com.ibm.ws.jpa.olgh17837.model;

import java.util.Collection;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

@Entity
public class OLGH17837Entity {
    @Id
    private long id;

    private String strVal1;
    private String strVal2;

    private Integer intVal1;
    private Integer intVal2;

    @ElementCollection
    @CollectionTable(name = "COLTABLE1", joinColumns = @JoinColumn(name = "ent_id"))
    private Collection<String> colVal1;

    public OLGH17837Entity() {
    }

    public OLGH17837Entity(Long id) {
        this.id = id;
    }

    public OLGH17837Entity(Long id, String strVal1) {
        this.id = id;
        this.strVal1 = strVal1;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStrVal1() {
        return strVal1;
    }

    public void setStrVal1(String strVal1) {
        this.strVal1 = strVal1;
    }

    public String getStrVal2() {
        return strVal2;
    }

    public void setStrVal2(String strVal2) {
        this.strVal2 = strVal2;
    }

    public Integer getIntVal1() {
        return intVal1;
    }

    public void setIntVal1(Integer intVal1) {
        this.intVal1 = intVal1;
    }

    public Integer getIntVal2() {
        return intVal2;
    }

    public void setIntVal2(Integer intVal2) {
        this.intVal2 = intVal2;
    }
}
