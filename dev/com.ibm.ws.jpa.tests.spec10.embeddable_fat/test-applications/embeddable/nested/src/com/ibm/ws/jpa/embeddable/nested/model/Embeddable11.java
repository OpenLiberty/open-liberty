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

import javax.persistence.Embeddable;

@Embeddable
public class Embeddable11 {

    private String emb11_str01;
    private String emb11_str02;
    private String emb11_str03;
    private Integer emb11_int01;
    private Integer emb11_int02;
    private Integer emb11_int03;

    public Embeddable11() {
    }

    public Embeddable11(String emb11_str01,
                        String emb11_str02,
                        String emb11_str03,
                        Integer emb11_int01,
                        Integer emb11_int02,
                        Integer emb11_int03) {
        this.emb11_str01 = emb11_str01;
        this.emb11_str02 = emb11_str02;
        this.emb11_str03 = emb11_str03;
        this.emb11_int01 = emb11_int01;
        this.emb11_int02 = emb11_int02;
        this.emb11_int03 = emb11_int03;
    }

    @Override
    public String toString() {
        return ("Embeddable11: " +
                " emb11_str01: " + getEmb11_str01() +
                " emb11_str02: " + getEmb11_str02() +
                " emb11_str03: " + getEmb11_str03() +
                " emb11_int01: " + getEmb11_int01() +
                " emb11_int02: " + getEmb11_int02() +
                " emb11_int03: " + getEmb11_int03());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (emb11_str01 == null ? 0 : emb11_str01.hashCode());
        hash = 31 * hash + (emb11_str02 == null ? 0 : emb11_str02.hashCode());
        hash = 31 * hash + (emb11_str03 == null ? 0 : emb11_str03.hashCode());
        hash = 31 * hash + (emb11_int01 == null ? 0 : emb11_int01.hashCode());
        hash = 31 * hash + (emb11_int02 == null ? 0 : emb11_int02.hashCode());
        hash = 31 * hash + (emb11_int03 == null ? 0 : emb11_int03.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if ((object == null) || (object.getClass() != this.getClass()))
            return false;
        Embeddable11 other = (Embeddable11) object;
        return ((this.emb11_str01 == other.emb11_str01 || (this.emb11_str01 != null && this.emb11_str01.equals(other.emb11_str01))) &&
                (this.emb11_str02 == other.emb11_str02 || (this.emb11_str02 != null && this.emb11_str02.equals(other.emb11_str02))) &&
                (this.emb11_str03 == other.emb11_str03 || (this.emb11_str03 != null && this.emb11_str03.equals(other.emb11_str03))) &&
                (this.emb11_int01 == other.emb11_int01 || (this.emb11_int01 != null && this.emb11_int01.equals(other.emb11_int01))) &&
                (this.emb11_int02 == other.emb11_int02 || (this.emb11_int02 != null && this.emb11_int02.equals(other.emb11_int02)))
                && (this.emb11_int03 == other.emb11_int03 || (this.emb11_int03 != null && this.emb11_int03.equals(other.emb11_int03))));
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable11 fields
    //----------------------------------------------------------------------------------------------
    public String getEmb11_str01() {
        return emb11_str01;
    }

    public void setEmb11_str01(String str) {
        this.emb11_str01 = str;
    }

    public String getEmb11_str02() {
        return emb11_str02;
    }

    public void setEmb11_str02(String str) {
        this.emb11_str02 = str;
    }

    public String getEmb11_str03() {
        return emb11_str03;
    }

    public void setEmb11_str03(String str) {
        this.emb11_str03 = str;
    }

    public Integer getEmb11_int01() {
        return emb11_int01;
    }

    public void setEmb11_int01(Integer ii) {
        this.emb11_int01 = ii;
    }

    public Integer getEmb11_int02() {
        return emb11_int02;
    }

    public void setEmb11_int02(Integer ii) {
        this.emb11_int02 = ii;
    }

    public Integer getEmb11_int03() {
        return emb11_int03;
    }

    public void setEmb11_int03(Integer ii) {
        this.emb11_int03 = ii;
    }
}
