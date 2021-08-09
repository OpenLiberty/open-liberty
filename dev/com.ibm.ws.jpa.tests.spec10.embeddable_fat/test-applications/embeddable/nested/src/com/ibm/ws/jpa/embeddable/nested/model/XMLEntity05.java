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

public class XMLEntity05 implements IEntity05 {

    private int id;
    private String ent05_str01;
    private String ent05_str02;
    private String ent05_str03;
    private XMLEmbeddable05a embeddable05a;

    public XMLEntity05() {
        embeddable05a = new XMLEmbeddable05a();
    }

    public XMLEntity05(String ent05_str01,
                       String ent05_str02,
                       String ent05_str03,
                       XMLEmbeddable05a embeddable05a) {
        this.ent05_str01 = ent05_str01;
        this.ent05_str02 = ent05_str02;
        this.ent05_str03 = ent05_str03;
        this.embeddable05a = embeddable05a;
    }

    @Override
    public String toString() {
        return ("XMLEntity05: id: " + getId() +
                " ent05_str01: " + getEnt05_str01() +
                " ent05_str02: " + getEnt05_str02() +
                " ent05_str03: " + getEnt05_str03() +
                " embeddable05a: " + getEmbeddable05a());
    }

    //----------------------------------------------------------------------------------------------
    // XMLEntity05 fields
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
    public String getEnt05_str01() {
        return ent05_str01;
    }

    @Override
    public void setEnt05_str01(String str) {
        this.ent05_str01 = str;
    }

    @Override
    public String getEnt05_str02() {
        return ent05_str02;
    }

    @Override
    public void setEnt05_str02(String str) {
        this.ent05_str02 = str;
    }

    @Override
    public String getEnt05_str03() {
        return ent05_str03;
    }

    @Override
    public void setEnt05_str03(String str) {
        this.ent05_str03 = str;
    }

    public XMLEmbeddable05a getEmbeddable05a() {
        return embeddable05a;
    }

    public void setEmbeddable05a(XMLEmbeddable05a embeddable05a) {
        this.embeddable05a = embeddable05a;
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05a fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb05a_int01() {
        return embeddable05a.getEmb05a_int01();
    }

    @Override
    public void setEmb05a_int01(int ii) {
        embeddable05a.setEmb05a_int01(ii);
    }

    @Override
    public int getEmb05a_int02() {
        return embeddable05a.getEmb05a_int02();
    }

    @Override
    public void setEmb05a_int02(int ii) {
        embeddable05a.setEmb05a_int02(ii);
    }

    @Override
    public int getEmb05a_int03() {
        return embeddable05a.getEmb05a_int03();
    }

    @Override
    public void setEmb05a_int03(int ii) {
        embeddable05a.setEmb05a_int03(ii);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05b fields
    //----------------------------------------------------------------------------------------------
    @Override
    public String getEmb05b_str04() {
        return embeddable05a.getEmbeddable05b().getEmb05b_str04();
    }

    @Override
    public void setEmb05b_str04(String str) {
        embeddable05a.getEmbeddable05b().setEmb05b_str04(str);
    }

    @Override
    public String getEmb05b_str05() {
        return embeddable05a.getEmbeddable05b().getEmb05b_str05();
    }

    @Override
    public void setEmb05b_str05(String str) {
        embeddable05a.getEmbeddable05b().setEmb05b_str05(str);
    }

    @Override
    public String getEmb05b_str06() {
        return embeddable05a.getEmbeddable05b().getEmb05b_str06();
    }

    @Override
    public void setEmb05b_str06(String str) {
        embeddable05a.getEmbeddable05b().setEmb05b_str06(str);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05c fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb05c_int04() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmb05c_int04();
    }

    @Override
    public void setEmb05c_int04(int ii) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().setEmb05c_int04(ii);
    }

    @Override
    public int getEmb05c_int05() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmb05c_int05();
    }

    @Override
    public void setEmb05c_int05(int ii) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().setEmb05c_int05(ii);
    }

    @Override
    public int getEmb05c_int06() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmb05c_int06();
    }

    @Override
    public void setEmb05c_int06(int ii) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().setEmb05c_int06(ii);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05d fields
    //----------------------------------------------------------------------------------------------
    @Override
    public String getEmb05d_str07() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmb05d_str07();
    }

    @Override
    public void setEmb05d_str07(String str) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().setEmb05d_str07(str);
    }

    @Override
    public String getEmb05d_str08() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmb05d_str08();
    }

    @Override
    public void setEmb05d_str08(String str) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().setEmb05d_str08(str);
    }

    @Override
    public String getEmb05d_str09() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmb05d_str09();
    }

    @Override
    public void setEmb05d_str09(String str) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().setEmb05d_str09(str);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05e fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb05e_int07() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().getEmb05e_int07();
    }

    @Override
    public void setEmb05e_int07(int ii) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().setEmb05e_int07(ii);
    }

    @Override
    public int getEmb05e_int08() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().getEmb05e_int08();
    }

    @Override
    public void setEmb05e_int08(int ii) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().setEmb05e_int08(ii);
    }

    @Override
    public int getEmb05e_int09() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().getEmb05e_int09();
    }

    @Override
    public void setEmb05e_int09(int ii) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().setEmb05e_int09(ii);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05f fields
    //----------------------------------------------------------------------------------------------
    @Override
    public String getEmb05f_str10() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().getEmbeddable05f().getEmb05f_str10();
    }

    @Override
    public void setEmb05f_str10(String str) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().getEmbeddable05f().setEmb05f_str10(str);
    }

    @Override
    public String getEmb05f_str11() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().getEmbeddable05f().getEmb05f_str11();
    }

    @Override
    public void setEmb05f_str11(String str) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().getEmbeddable05f().setEmb05f_str11(str);
    }

    @Override
    public String getEmb05f_str12() {
        return embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().getEmbeddable05f().getEmb05f_str12();
    }

    @Override
    public void setEmb05f_str12(String str) {
        embeddable05a.getEmbeddable05b().getEmbeddable05c().getEmbeddable05d().getEmbeddable05e().getEmbeddable05f().setEmb05f_str12(str);
    }
}
