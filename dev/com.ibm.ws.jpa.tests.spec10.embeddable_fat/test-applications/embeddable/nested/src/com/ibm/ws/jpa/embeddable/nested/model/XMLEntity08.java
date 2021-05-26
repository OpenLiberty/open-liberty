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

import java.util.HashSet;
import java.util.Set;

public class XMLEntity08 implements IEntity08 {

    private int id;
    private String ent08_str01;
    private String ent08_str02;
    private String ent08_str03;
    private Set<String> ent08_set01;
    private Set<Integer> ent08_set02;
    private Set<Embeddable01> ent08_set03;

    public XMLEntity08() {
        ent08_set01 = new HashSet<String>();
        ent08_set02 = new HashSet<Integer>();
        ent08_set03 = new HashSet<Embeddable01>();
    }

    public XMLEntity08(String ent08_str01,
                       String ent08_str02,
                       String ent08_str03,
                       Set<String> ent08_set01,
                       Set<Integer> ent08_set02,
                       Set<Embeddable01> ent08_set03) {
        this.ent08_str01 = ent08_str01;
        this.ent08_str02 = ent08_str02;
        this.ent08_str03 = ent08_str03;
        this.ent08_set01 = ent08_set01;
        this.ent08_set02 = ent08_set02;
        this.ent08_set03 = ent08_set03;
    }

    @Override
    public String toString() {
        return ("XMLEntity08: id: " + getId() +
                " ent08_str01: " + getEnt08_str01() +
                " ent08_str02: " + getEnt08_str02() +
                " ent08_str03: " + getEnt08_str03() +
                " ent08_set01: " + getEnt08_set01() +
                " ent08_set02: " + getEnt08_set02() +
                " ent08_set03: " + getEnt08_set03());
    }

    //----------------------------------------------------------------------------------------------
    // XMLEntity08 fields
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
    public String getEnt08_str01() {
        return ent08_str01;
    }

    @Override
    public void setEnt08_str01(String str) {
        this.ent08_str01 = str;
    }

    @Override
    public String getEnt08_str02() {
        return ent08_str02;
    }

    @Override
    public void setEnt08_str02(String str) {
        this.ent08_str02 = str;
    }

    @Override
    public String getEnt08_str03() {
        return ent08_str03;
    }

    @Override
    public void setEnt08_str03(String str) {
        this.ent08_str03 = str;
    }

    @Override
    public Set<String> getEnt08_set01() {
        return ent08_set01;
    }

    @Override
    public void setEnt08_set01(Set<String> set) {
        this.ent08_set01 = set;
    }

    @Override
    public Set<Integer> getEnt08_set02() {
        return ent08_set02;
    }

    @Override
    public void setEnt08_set02(Set<Integer> set) {
        this.ent08_set02 = set;
    }

    @Override
    public Set<Embeddable01> getEnt08_set03() {
        return ent08_set03;
    }

    @Override
    public void setEnt08_set03(Set<Embeddable01> set) {
        this.ent08_set03 = set;
    }
}
