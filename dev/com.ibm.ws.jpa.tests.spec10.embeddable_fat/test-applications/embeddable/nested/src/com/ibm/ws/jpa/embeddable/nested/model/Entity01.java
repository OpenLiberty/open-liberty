/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.nested.model;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "EN_Entity01")
public class Entity01 implements IEntity01 {

    @Id
    private int id;
    private String ent01_str01;
    private String ent01_str02;
    private String ent01_str03;
    @Embedded
    private Embeddable01 embeddable01;

    public Entity01() {
        embeddable01 = new Embeddable01();
    }

    public Entity01(String ent01_str01,
                    String ent01_str02,
                    String ent01_str03,
                    Embeddable01 embeddable01) {
        this.ent01_str01 = ent01_str01;
        this.ent01_str02 = ent01_str02;
        this.ent01_str02 = ent01_str03;
        this.embeddable01 = embeddable01;
    }

    @Override
    public String toString() {
        return ("Entity01: id: " + getId() +
                " ent01_str01: " + getEnt01_str01() +
                " ent01_str02: " + getEnt01_str02() +
                " ent01_str03: " + getEnt01_str03() +
                " embeddable01: " + getEmbeddable01());
    }

    //----------------------------------------------------------------------------------------------
    // Entity01 fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getEnt01_str01() {
        return ent01_str01;
    }

    @Override
    public void setEnt01_str01(String str) {
        this.ent01_str01 = str;
    }

    @Override
    public String getEnt01_str02() {
        return ent01_str02;
    }

    @Override
    public void setEnt01_str02(String str) {
        this.ent01_str02 = str;
    }

    @Override
    public String getEnt01_str03() {
        return ent01_str03;
    }

    @Override
    public void setEnt01_str03(String str) {
        this.ent01_str03 = str;
    }

    public Embeddable01 getEmbeddable01() {
        return embeddable01;
    }

    public void setEmbeddable01(Embeddable01 embeddable01) {
        this.embeddable01 = embeddable01;
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable01 fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb01_int01() {
        return embeddable01.getEmb01_int01();
    }

    @Override
    public void setEmb01_int01(int ii) {
        embeddable01.setEmb01_int01(ii);
    }

    @Override
    public int getEmb01_int02() {
        return embeddable01.getEmb01_int02();
    }

    @Override
    public void setEmb01_int02(int ii) {
        embeddable01.setEmb01_int02(ii);
    }

    @Override
    public int getEmb01_int03() {
        return embeddable01.getEmb01_int03();
    }

    @Override
    public void setEmb01_int03(int ii) {
        embeddable01.setEmb01_int03(ii);
    }
}
