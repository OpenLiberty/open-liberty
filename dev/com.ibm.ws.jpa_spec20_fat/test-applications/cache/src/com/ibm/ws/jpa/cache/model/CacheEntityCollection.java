/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.cache.model;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class CacheEntityCollection {
    @Id
    private int id;

    private String strVal;
    private int intVal;

    @OneToOne(cascade = CascadeType.ALL)
    private CacheEntitySimple2 entitySimple2;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "CECollection_CESimple1")
    private Collection<CacheEntitySimple1> entitySimple1;

    public CacheEntityCollection() {
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

    public CacheEntitySimple2 getEntitySimple2() {
        return entitySimple2;
    }

    public void setEntitySimple2(CacheEntitySimple2 entitySimple2) {
        this.entitySimple2 = entitySimple2;
    }

    public Collection<CacheEntitySimple1> getEntitySimple1() {
        return entitySimple1;
    }

    public void setEntitySimple1(Collection<CacheEntitySimple1> entitySimple1) {
        this.entitySimple1 = entitySimple1;
    }
}