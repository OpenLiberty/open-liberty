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

import java.util.ArrayList;
import java.util.List;

public class XMLEntity09 implements IEntity09 {

    private int id;
    private String ent09_str01;
    private String ent09_str02;
    private String ent09_str03;
    private List<String> ent09_list01;
    private List<Integer> ent09_list02;
    private List<Embeddable01> ent09_list03;

    public XMLEntity09() {
        ent09_list01 = new ArrayList<String>();
        ent09_list02 = new ArrayList<Integer>();
        ent09_list03 = new ArrayList<Embeddable01>();
    }

    public XMLEntity09(String ent09_str01,
                       String ent09_str02,
                       String ent09_str03,
                       List<String> ent09_list01,
                       List<Integer> ent09_list02,
                       List<Embeddable01> ent09_list03) {
        this.ent09_str01 = ent09_str01;
        this.ent09_str02 = ent09_str02;
        this.ent09_str03 = ent09_str03;
        this.ent09_list01 = ent09_list01;
        this.ent09_list02 = ent09_list02;
        this.ent09_list03 = ent09_list03;
    }

    @Override
    public String toString() {
        return ("XMLEntity09: id: " + getId() +
                " ent09_str01: " + getEnt09_str01() +
                " ent09_str02: " + getEnt09_str02() +
                " ent09_str03: " + getEnt09_str03() +
                " ent09_list01: " + getEnt09_list01() +
                " ent09_list02: " + getEnt09_list02() +
                " ent09_list03: " + getEnt09_list03());
    }

    //----------------------------------------------------------------------------------------------
    // XMLEntity09 fields
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
    public String getEnt09_str01() {
        return ent09_str01;
    }

    @Override
    public void setEnt09_str01(String str) {
        this.ent09_str01 = str;
    }

    @Override
    public String getEnt09_str02() {
        return ent09_str02;
    }

    @Override
    public void setEnt09_str02(String str) {
        this.ent09_str02 = str;
    }

    @Override
    public String getEnt09_str03() {
        return ent09_str03;
    }

    @Override
    public void setEnt09_str03(String str) {
        this.ent09_str03 = str;
    }

    @Override
    public List<String> getEnt09_list01() {
        return ent09_list01;
    }

    @Override
    public void setEnt09_list01(List<String> list) {
        this.ent09_list01 = list;
    }

    @Override
    public List<Integer> getEnt09_list02() {
        return ent09_list02;
    }

    @Override
    public void setEnt09_list02(List<Integer> list) {
        this.ent09_list02 = list;
    }

    @Override
    public List<Embeddable01> getEnt09_list03() {
        return ent09_list03;
    }

    @Override
    public void setEnt09_list03(List<Embeddable01> list) {
        this.ent09_list03 = list;
    }
}
