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

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "CDM_Entity00")
public class Entity00 {

    @Id
    private int id;
    private String ent00_str01;
    private String ent00_str02;
    private String ent00_str03;

    public Entity00() {}

    public Entity00(String ent00_str01,
                    String ent00_str02,
                    String ent00_str03) {
        this.ent00_str01 = ent00_str01;
        this.ent00_str02 = ent00_str02;
        this.ent00_str02 = ent00_str03;
    }

    @Override
    public String toString() {
        return ("Entity00: id: " + getId() +
                " ent00_str01: " + getEnt00_str01() +
                " ent00_str02: " + getEnt00_str02() +
                " ent00_str03: " + getEnt00_str03());
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEnt00_str01() {
        return ent00_str01;
    }

    public void setEnt00_str01(String str) {
        this.ent00_str01 = str;
    }

    public String getEnt00_str02() {
        return ent00_str02;
    }

    public void setEnt00_str02(String str) {
        this.ent00_str02 = str;
    }

    public String getEnt00_str03() {
        return ent00_str03;
    }

    public void setEnt00_str03(String str) {
        this.ent00_str03 = str;
    }
}
