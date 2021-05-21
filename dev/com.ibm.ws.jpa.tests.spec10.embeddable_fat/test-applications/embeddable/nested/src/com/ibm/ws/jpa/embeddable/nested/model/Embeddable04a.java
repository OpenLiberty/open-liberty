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
public class Embeddable04a {

    private int emb04a_int01;
    private int emb04a_int02;
    private int emb04a_int03;
    @Embedded
    private Embeddable04b embeddable04b;

    public Embeddable04a() {
        embeddable04b = new Embeddable04b();
    }

    public Embeddable04a(int emb04a_int01,
                         int emb04a_int02,
                         int emb04a_int03,
                         Embeddable04b embeddable04b) {
        this.emb04a_int01 = emb04a_int01;
        this.emb04a_int02 = emb04a_int02;
        this.emb04a_int03 = emb04a_int03;
        this.embeddable04b = embeddable04b;
    }

    @Override
    public String toString() {
        return ("Embeddable04a: " +
                " emb04a_int01: " + getEmb04a_int01() +
                " emb04a_int02: " + getEmb04a_int02() +
                " emb04a_int03: " + getEmb04a_int03() +
                " embeddable04b: " + getEmbeddable04b());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + emb04a_int01;
        hash = 31 * hash + emb04a_int02;
        hash = 31 * hash + emb04a_int03;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if ((object == null) || (object.getClass() != this.getClass()))
            return false;
        Embeddable04a other = (Embeddable04a) object;
        return (this.emb04a_int01 == other.emb04a_int01 &&
                this.emb04a_int02 == other.emb04a_int02 &&
                this.emb04a_int03 == other.emb04a_int03 &&
                this.embeddable04b.equals(other.embeddable04b));
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable04a fields
    //----------------------------------------------------------------------------------------------
    public int getEmb04a_int01() {
        return emb04a_int01;
    }

    public void setEmb04a_int01(int ii) {
        this.emb04a_int01 = ii;
    }

    public int getEmb04a_int02() {
        return emb04a_int02;
    }

    public void setEmb04a_int02(int ii) {
        this.emb04a_int02 = ii;
    }

    public int getEmb04a_int03() {
        return emb04a_int03;
    }

    public void setEmb04a_int03(int ii) {
        this.emb04a_int03 = ii;
    }

    public Embeddable04b getEmbeddable04b() {
        return embeddable04b;
    }

    public void setEmbeddable04b(Embeddable04b embeddable04b) {
        this.embeddable04b = embeddable04b;
    }
}
