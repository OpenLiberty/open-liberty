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

public class XMLEntity03 implements IEntity03 {

    private int id;
    private String ent03_str01;
    private String ent03_str02;
    private String ent03_str03;
    private XMLEmbeddable03a embeddable03a;

    public XMLEntity03() {
        embeddable03a = new XMLEmbeddable03a();
    }

    public XMLEntity03(String ent03_str01,
                       String ent03_str02,
                       String ent03_str03,
                       XMLEmbeddable03a embeddable03a) {
        this.ent03_str01 = ent03_str01;
        this.ent03_str02 = ent03_str02;
        this.ent03_str03 = ent03_str03;
        this.embeddable03a = embeddable03a;
    }

    @Override
    public String toString() {
        return ("XMLEntity03: id: " + getId() +
                " ent03_str01: " + getEnt03_str01() +
                " ent03_str02: " + getEnt03_str02() +
                " ent03_str03: " + getEnt03_str03() +
                " embeddable03a: " + getEmbeddable03a());
    }

    //----------------------------------------------------------------------------------------------
    // XMLEntity03 fields
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
    public String getEnt03_str01() {
        return ent03_str01;
    }

    @Override
    public void setEnt03_str01(String str) {
        this.ent03_str01 = str;
    }

    @Override
    public String getEnt03_str02() {
        return ent03_str02;
    }

    @Override
    public void setEnt03_str02(String str) {
        this.ent03_str02 = str;
    }

    @Override
    public String getEnt03_str03() {
        return ent03_str03;
    }

    @Override
    public void setEnt03_str03(String str) {
        this.ent03_str03 = str;
    }

    public XMLEmbeddable03a getEmbeddable03a() {
        return embeddable03a;
    }

    public void setEmbeddable03a(XMLEmbeddable03a embeddable03a) {
        this.embeddable03a = embeddable03a;
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable03a fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb03a_int01() {
        return embeddable03a.getEmb03a_int01();
    }

    @Override
    public void setEmb03a_int01(int ii) {
        embeddable03a.setEmb03a_int01(ii);
    }

    @Override
    public int getEmb03a_int02() {
        return embeddable03a.getEmb03a_int02();
    }

    @Override
    public void setEmb03a_int02(int ii) {
        embeddable03a.setEmb03a_int02(ii);
    }

    @Override
    public int getEmb03a_int03() {
        return embeddable03a.getEmb03a_int03();
    }

    @Override
    public void setEmb03a_int03(int ii) {
        embeddable03a.setEmb03a_int03(ii);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable03b fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getEmb03b_int04() {
        return embeddable03a.getEmbeddable03b().getEmb03b_int04();
    }

    @Override
    public void setEmb03b_int04(int ii) {
        embeddable03a.getEmbeddable03b().setEmb03b_int04(ii);
    }

    @Override
    public int getEmb03b_int05() {
        return embeddable03a.getEmbeddable03b().getEmb03b_int05();
    }

    @Override
    public void setEmb03b_int05(int ii) {
        embeddable03a.getEmbeddable03b().setEmb03b_int05(ii);
    }

    @Override
    public int getEmb03b_int06() {
        return embeddable03a.getEmbeddable03b().getEmb03b_int06();
    }

    @Override
    public void setEmb03b_int06(int ii) {
        embeddable03a.getEmbeddable03b().setEmb03b_int06(ii);
    }
}
