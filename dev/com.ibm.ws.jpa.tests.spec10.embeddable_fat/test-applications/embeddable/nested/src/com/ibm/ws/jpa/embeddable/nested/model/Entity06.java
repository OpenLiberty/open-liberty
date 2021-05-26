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
@Table(name = "EN_Entity06")
public class Entity06 implements IEntity06 {

    @Id
    private int id;
    private String ent06_str01;
    private String ent06_str02;
    private String ent06_str03;
    @Embedded
    private Embeddable01 embeddable01;
    @Embedded
    private Embeddable04a embeddable04a;

    public Entity06() {
        embeddable01 = new Embeddable01();
        embeddable04a = new Embeddable04a();
    }

    public Entity06(String ent06_str01,
                    String ent06_str02,
                    String ent06_str03,
                    Embeddable01 embeddable01,
                    Embeddable04a embeddable04a) {
        this.ent06_str01 = ent06_str01;
        this.ent06_str02 = ent06_str02;
        this.ent06_str03 = ent06_str03;
        this.embeddable01 = embeddable01;
        this.embeddable04a = embeddable04a;
    }

    @Override
    public String toString() {
        return ("Entity06: id: " + getId() +
                " ent06_str01: " + getEnt06_str01() +
                " ent06_str02: " + getEnt06_str02() +
                " ent06_str03: " + getEnt06_str03() +
                " embeddable01: " + getEmbeddable01() +
                " embeddable04a: " + getEmbeddable04a());
    }

    //----------------------------------------------------------------------------------------------
    // Entity06 fields
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
    public String getEnt06_str01() {
        return ent06_str01;
    }

    @Override
    public void setEnt06_str01(String str) {
        this.ent06_str01 = str;
    }

    @Override
    public String getEnt06_str02() {
        return ent06_str02;
    }

    @Override
    public void setEnt06_str02(String str) {
        this.ent06_str02 = str;
    }

    @Override
    public String getEnt06_str03() {
        return ent06_str03;
    }

    @Override
    public void setEnt06_str03(String str) {
        this.ent06_str03 = str;
    }

    public Embeddable01 getEmbeddable01() {
        return embeddable01;
    }

    public void setEmbeddable01(Embeddable01 embeddable01) {
        this.embeddable01 = embeddable01;
    }

    public Embeddable04a getEmbeddable04a() {
        return embeddable04a;
    }

    public void setEmbeddable04a(Embeddable04a embeddable04a) {
        this.embeddable04a = embeddable04a;
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

    //----------------------------------------------------------------------------------------------
    // Embeddable04a fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb04a_int01() {
        return embeddable04a.getEmb04a_int01();
    }

    @Override
    public void setEmb04a_int01(int ii) {
        embeddable04a.setEmb04a_int01(ii);
    }

    @Override
    public int getEmb04a_int02() {
        return embeddable04a.getEmb04a_int02();
    }

    @Override
    public void setEmb04a_int02(int ii) {
        embeddable04a.setEmb04a_int02(ii);
    }

    @Override
    public int getEmb04a_int03() {
        return embeddable04a.getEmb04a_int03();
    }

    @Override
    public void setEmb04a_int03(int ii) {
        embeddable04a.setEmb04a_int03(ii);
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable04b fields
    //----------------------------------------------------------------------------------------------
    @Override
    public String getEmb04b_str04() {
        return embeddable04a.getEmbeddable04b().getEmb04b_str04();
    }

    @Override
    public void setEmb04b_str04(String str) {
        embeddable04a.getEmbeddable04b().setEmb04b_str04(str);
    }

    @Override
    public String getEmb04b_str05() {
        return embeddable04a.getEmbeddable04b().getEmb04b_str05();
    }

    @Override
    public void setEmb04b_str05(String str) {
        embeddable04a.getEmbeddable04b().setEmb04b_str05(str);
    }

    @Override
    public String getEmb04b_str06() {
        return embeddable04a.getEmbeddable04b().getEmb04b_str06();
    }

    @Override
    public void setEmb04b_str06(String str) {
        embeddable04a.getEmbeddable04b().setEmb04b_str06(str);
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable04c fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb04c_int07() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int07();
    }

    @Override
    public void setEmb04c_int07(int ii) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(ii);
    }

    @Override
    public int getEmb04c_int08() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int08();
    }

    @Override
    public void setEmb04c_int08(int ii) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(ii);
    }

    @Override
    public int getEmb04c_int09() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int09();
    }

    @Override
    public void setEmb04c_int09(int ii) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(ii);
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable04d fields
    //----------------------------------------------------------------------------------------------
    @Override
    public String getEmb04d_str10() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str10();
    }

    @Override
    public void setEmb04d_str10(String str) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10(str);
    }

    @Override
    public String getEmb04d_str11() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str11();
    }

    @Override
    public void setEmb04d_str11(String str) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11(str);
    }

    @Override
    public String getEmb04d_str12() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str12();
    }

    @Override
    public void setEmb04d_str12(String str) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12(str);
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable04e fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb04e_int13() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int13();
    }

    @Override
    public void setEmb04e_int13(int ii) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(ii);
    }

    @Override
    public int getEmb04e_int14() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14();
    }

    @Override
    public void setEmb04e_int14(int ii) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(ii);
    }

    @Override
    public int getEmb04e_int15() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int15();
    }

    @Override
    public void setEmb04e_int15(int ii) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(ii);
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable04f fields
    //----------------------------------------------------------------------------------------------
    @Override
    public String getEmb04f_str16() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str16();
    }

    @Override
    public void setEmb04f_str16(String str) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16(str);
    }

    @Override
    public String getEmb04f_str17() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str17();
    }

    @Override
    public void setEmb04f_str17(String str) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17(str);
    }

    @Override
    public String getEmb04f_str18() {
        return embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str18();
    }

    @Override
    public void setEmb04f_str18(String str) {
        embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18(str);
    }
}
