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

import java.util.Vector;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "EN_Entity10")
public class Entity10 implements IEntity10 {

    @Id
    private int id;
    private String ent10_str01;
    private String ent10_str02;
    private String ent10_str03;
    @ElementCollection
    @CollectionTable(name = "EN_Entity10_ent10_vector01", joinColumns = @JoinColumn(name = "Entity10_ID"))
    @OrderBy
    private Vector<String> ent10_vector01;
    @ElementCollection
    @CollectionTable(name = "EN_Entity10_ent10_vector02", joinColumns = @JoinColumn(name = "Entity10_ID"))
    @OrderBy
    private Vector<Integer> ent10_vector02;
    @ElementCollection
    @CollectionTable(name = "EN_Entity10_ent10_vector03", joinColumns = @JoinColumn(name = "Entity10_ID"))
    @OrderBy("embeddable04b.embeddable04c.embeddable04d.embeddable04e.emb04e_int14 DESC, " +
             "embeddable04b.embeddable04c.embeddable04d.emb04d_str10 DESC, " +
             "embeddable04b.embeddable04c.emb04c_int09 DESC, " +
             "embeddable04b.emb04b_str05 DESC, " +
             "emb04a_int01 DESC")
    private Vector<Embeddable04a> ent10_vector03;
    @Embedded
    private Embeddable04a embeddable04a;

    public Entity10() {
        ent10_vector01 = new Vector<String>();
        ent10_vector02 = new Vector<Integer>();
        ent10_vector03 = new Vector<Embeddable04a>();
        embeddable04a = new Embeddable04a();
    }

    public Entity10(String ent10_str01,
                    String ent10_str02,
                    String ent10_str03,
                    Vector<String> ent10_vector01,
                    Vector<Integer> ent10_vector02,
                    Vector<Embeddable04a> ent10_vector03,
                    Embeddable04a embeddable04a) {
        this.ent10_str01 = ent10_str01;
        this.ent10_str02 = ent10_str02;
        this.ent10_str02 = ent10_str03;
        this.ent10_vector01 = ent10_vector01;
        this.ent10_vector02 = ent10_vector02;
        this.ent10_vector03 = ent10_vector03;
        this.embeddable04a = embeddable04a;
    }

    @Override
    public String toString() {
        return ("Entity10: id: " + getId() +
                " ent10_str01: " + getEnt10_str01() +
                " ent10_str02: " + getEnt10_str02() +
                " ent10_str03: " + getEnt10_str03() +
                " ent10_vector01: " + getEnt10_vector01() +
                " ent10_vector02: " + getEnt10_vector02() +
                " ent10_vector03: " + getEnt10_vector03() +
                " embeddable04a: " + getEmbeddable04a());
    }

    //----------------------------------------------------------------------------------------------
    // Entity10 fields
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
    public String getEnt10_str01() {
        return ent10_str01;
    }

    @Override
    public void setEnt10_str01(String str) {
        this.ent10_str01 = str;
    }

    @Override
    public String getEnt10_str02() {
        return ent10_str02;
    }

    @Override
    public void setEnt10_str02(String str) {
        this.ent10_str02 = str;
    }

    @Override
    public String getEnt10_str03() {
        return ent10_str03;
    }

    @Override
    public void setEnt10_str03(String str) {
        this.ent10_str03 = str;
    }

    @Override
    public Vector<String> getEnt10_vector01() {
        return ent10_vector01;
    }

    @Override
    public void setEnt10_vector01(Vector<String> vector) {
        this.ent10_vector01 = vector;
    }

    @Override
    public Vector<Integer> getEnt10_vector02() {
        return ent10_vector02;
    }

    @Override
    public void setEnt10_vector02(Vector<Integer> vector) {
        this.ent10_vector02 = vector;
    }

    @Override
    public Vector<Embeddable04a> getEnt10_vector03() {
        return ent10_vector03;
    }

    @Override
    public void setEnt10_vector03(Vector<Embeddable04a> vector) {
        this.ent10_vector03 = vector;
    }

    public Embeddable04a getEmbeddable04a() {
        return embeddable04a;
    }

    public void setEmbeddable04a(Embeddable04a embeddable04a) {
        this.embeddable04a = embeddable04a;
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
