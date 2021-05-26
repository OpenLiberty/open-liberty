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

public class XMLEntity04 implements IEntity04 {

    private int id;
    private String ent04_str01;
    private String ent04_str02;
    private String ent04_str03;
    private XMLEmbeddable04a embeddable04a;

    public XMLEntity04() {
        embeddable04a = new XMLEmbeddable04a();
    }

    public XMLEntity04(String ent04_str01,
                       String ent04_str02,
                       String ent04_str03,
                       XMLEmbeddable04a embeddable04a) {
        this.ent04_str01 = ent04_str01;
        this.ent04_str02 = ent04_str02;
        this.ent04_str03 = ent04_str03;
        this.embeddable04a = embeddable04a;
    }

    @Override
    public String toString() {
        return ("XMLEntity04: id: " + getId() +
                " ent04_str01: " + getEnt04_str01() +
                " ent04_str02: " + getEnt04_str02() +
                " ent04_str03: " + getEnt04_str03() +
                " embeddable04a: " + getEmbeddable04a());
    }

    //----------------------------------------------------------------------------------------------
    // XMLEntity04 fields
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
    public String getEnt04_str01() {
        return ent04_str01;
    }

    @Override
    public void setEnt04_str01(String str) {
        this.ent04_str01 = str;
    }

    @Override
    public String getEnt04_str02() {
        return ent04_str02;
    }

    @Override
    public void setEnt04_str02(String str) {
        this.ent04_str02 = str;
    }

    @Override
    public String getEnt04_str03() {
        return ent04_str03;
    }

    @Override
    public void setEnt04_str03(String str) {
        this.ent04_str03 = str;
    }

    public XMLEmbeddable04a getEmbeddable04a() {
        return embeddable04a;
    }

    public void setEmbeddable04a(XMLEmbeddable04a embeddable04a) {
        this.embeddable04a = embeddable04a;
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable04a fields
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
    // XMLEmbeddable04b fields
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
    // XMLEmbeddable04c fields
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
    // XMLEmbeddable04d fields
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
    // XMLEmbeddable04e fields
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
    // XMLEmbeddable04f fields
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
