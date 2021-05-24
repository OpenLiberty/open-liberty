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
import javax.persistence.Embedded;

@Embeddable
public class Embeddable05a {

    private int emb05a_int01;
    private int emb05a_int02;
    private int emb05a_int03;
    @Embedded
    private Embeddable05b embeddable05b;

    public Embeddable05a() {
        embeddable05b = new Embeddable05b();
    }

    public Embeddable05a(int emb05a_int01,
                         int emb05a_int02,
                         int emb05a_int03,
                         Embeddable05b embeddable05b) {
        this.emb05a_int01 = emb05a_int01;
        this.emb05a_int02 = emb05a_int02;
        this.emb05a_int03 = emb05a_int03;
        this.embeddable05b = embeddable05b;
    }

    @Override
    public String toString() {
        return ("Embeddable05a: " + " emb05a_int01: " + getEmb05a_int01() +
                " emb05a_int02: " + getEmb05a_int02() +
                " emb05a_int03: " + getEmb05a_int03() +
                " embeddable05b: " + getEmbeddable05b());
    }

    @Override
    public int hashCode() {
        return (emb05a_int01 ^ emb05a_int02 ^ emb05a_int03) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof Embeddable05a))
            return false;
        Embeddable05a other = (Embeddable05a) object;
        return (this.emb05a_int01 == other.emb05a_int01 &&
                this.emb05a_int02 == other.emb05a_int02 &&
                this.emb05a_int03 == other.emb05a_int03 &&
                this.embeddable05b.equals(other.embeddable05b));
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable05a fields
    //----------------------------------------------------------------------------------------------
    public int getEmb05a_int01() {
        return emb05a_int01;
    }

    public void setEmb05a_int01(int ii) {
        this.emb05a_int01 = ii;
    }

    public int getEmb05a_int02() {
        return emb05a_int02;
    }

    public void setEmb05a_int02(int ii) {
        this.emb05a_int02 = ii;
    }

    public int getEmb05a_int03() {
        return emb05a_int03;
    }

    public void setEmb05a_int03(int ii) {
        this.emb05a_int03 = ii;
    }

    public Embeddable05b getEmbeddable05b() {
        return embeddable05b;
    }

    public void setEmbeddable05b(Embeddable05b embeddable05b) {
        this.embeddable05b = embeddable05b;
    }
}
