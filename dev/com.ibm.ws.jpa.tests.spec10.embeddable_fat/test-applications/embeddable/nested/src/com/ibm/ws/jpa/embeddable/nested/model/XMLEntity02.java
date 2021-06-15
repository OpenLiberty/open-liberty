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

public class XMLEntity02 implements IEntity02 {

    private int id;
    private String ent02_str01;
    private String ent02_str02;
    private String ent02_str03;
    private XMLEmbeddable02a embeddable02a;

    public XMLEntity02() {
        embeddable02a = new XMLEmbeddable02a();
    }

    public XMLEntity02(String ent02_str01,
                       String ent02_str02,
                       String ent02_str03,
                       XMLEmbeddable02a embeddable02a) {
        this.ent02_str01 = ent02_str01;
        this.ent02_str02 = ent02_str02;
        this.ent02_str03 = ent02_str03;
        this.embeddable02a = embeddable02a;
    }

    @Override
    public String toString() {
        return ("XMLEntity02: id: " + getId() +
                " ent02_str01: " + getEnt02_str01() +
                " ent02_str02: " + getEnt02_str02() +
                " ent02_str03: " + getEnt02_str03() +
                " embeddable02a: " + getEmbeddable02a());
    }

    //----------------------------------------------------------------------------------------------
    // XMLEntity02 fields
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
    public String getEnt02_str01() {
        return ent02_str01;
    }

    @Override
    public void setEnt02_str01(String str) {
        this.ent02_str01 = str;
    }

    @Override
    public String getEnt02_str02() {
        return ent02_str02;
    }

    @Override
    public void setEnt02_str02(String str) {
        this.ent02_str02 = str;
    }

    @Override
    public String getEnt02_str03() {
        return ent02_str03;
    }

    @Override
    public void setEnt02_str03(String str) {
        this.ent02_str03 = str;
    }

    public XMLEmbeddable02a getEmbeddable02a() {
        return embeddable02a;
    }

    public void setEmbeddable02a(XMLEmbeddable02a embeddable02a) {
        this.embeddable02a = embeddable02a;
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable02a fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb02a_int01() {
        return embeddable02a.getEmb02a_int01();
    }

    @Override
    public void setEmb02a_int01(int ii) {
        embeddable02a.setEmb02a_int01(ii);
    }

    @Override
    public int getEmb02a_int02() {
        return embeddable02a.getEmb02a_int02();
    }

    @Override
    public void setEmb02a_int02(int ii) {
        embeddable02a.setEmb02a_int02(ii);
    }

    @Override
    public int getEmb02a_int03() {
        return embeddable02a.getEmb02a_int03();
    }

    @Override
    public void setEmb02a_int03(int ii) {
        embeddable02a.setEmb02a_int03(ii);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable02b fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb02b_int04() {
        return embeddable02a.getEmbeddable02b().getEmb02b_int04();
    }

    @Override
    public void setEmb02b_int04(int ii) {
        embeddable02a.getEmbeddable02b().setEmb02b_int04(ii);
    }

    @Override
    public int getEmb02b_int05() {
        return embeddable02a.getEmbeddable02b().getEmb02b_int05();
    }

    @Override
    public void setEmb02b_int05(int ii) {
        embeddable02a.getEmbeddable02b().setEmb02b_int05(ii);
    }

    @Override
    public int getEmb02b_int06() {
        return embeddable02a.getEmbeddable02b().getEmb02b_int06();
    }

    @Override
    public void setEmb02b_int06(int ii) {
        embeddable02a.getEmbeddable02b().setEmb02b_int06(ii);
    }
}
