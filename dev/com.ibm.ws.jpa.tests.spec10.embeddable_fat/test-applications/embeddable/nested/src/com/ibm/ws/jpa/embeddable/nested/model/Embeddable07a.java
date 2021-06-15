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

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class Embeddable07a {

    private int emb07a_int01;
    private int emb07a_int02;
    private int emb07a_int03;
    @Embedded
    private Embeddable07b embeddable07b;
    @Embedded
    @AttributeOverrides({
                          @AttributeOverride(name = "emb01_int01", column = @Column(name = "emb07a_emb01_int01")),
                          @AttributeOverride(name = "emb01_int02", column = @Column(name = "emb07a_emb01_int02")),
                          @AttributeOverride(name = "emb01_int03", column = @Column(name = "emb07a_emb01_int03"))
    })
    private Embeddable01 embeddable01;

    public Embeddable07a() {
        embeddable07b = new Embeddable07b();
        embeddable01 = new Embeddable01();
    }

    public Embeddable07a(int emb07a_int01,
                         int emb07a_int02,
                         int emb07a_int03,
                         Embeddable07b embeddable07b,
                         Embeddable01 embeddable01) {
        this.emb07a_int01 = emb07a_int01;
        this.emb07a_int02 = emb07a_int02;
        this.emb07a_int03 = emb07a_int03;
        this.embeddable07b = embeddable07b;
        this.embeddable01 = embeddable01;
    }

    @Override
    public String toString() {
        return ("Embeddable07a: " + " emb07a_int01: " + getEmb07a_int01() +
                " emb07a_int02: " + getEmb07a_int02() +
                " emb07a_int03: " + getEmb07a_int03() +
                " embeddable07b: " + getEmbeddable07b() +
                " embeddable01: " + getEmbeddable01());
    }

    @Override
    public int hashCode() {
        return (emb07a_int01 ^ emb07a_int02 ^ emb07a_int03) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof Embeddable07a))
            return false;
        Embeddable07a other = (Embeddable07a) object;
        return (this.emb07a_int01 == other.emb07a_int01 &&
                this.emb07a_int02 == other.emb07a_int02 &&
                this.emb07a_int03 == other.emb07a_int03 &&
                this.embeddable07b.equals(other.embeddable07b) &&
                this.embeddable01.equals(other.embeddable01));
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable07a fields
    //----------------------------------------------------------------------------------------------
    public int getEmb07a_int01() {
        return emb07a_int01;
    }

    public void setEmb07a_int01(int ii) {
        this.emb07a_int01 = ii;
    }

    public int getEmb07a_int02() {
        return emb07a_int02;
    }

    public void setEmb07a_int02(int ii) {
        this.emb07a_int02 = ii;
    }

    public int getEmb07a_int03() {
        return emb07a_int03;
    }

    public void setEmb07a_int03(int ii) {
        this.emb07a_int03 = ii;
    }

    public Embeddable07b getEmbeddable07b() {
        return embeddable07b;
    }

    public void setEmbeddable07b(Embeddable07b embeddable07b) {
        this.embeddable07b = embeddable07b;
    }

    public Embeddable01 getEmbeddable01() {
        return embeddable01;
    }

    public void setEmbeddable01(Embeddable01 embeddable01) {
        this.embeddable01 = embeddable01;
    }
}
