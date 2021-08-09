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
public class Embeddable03a {

    private int emb03a_int01;
    private int emb03a_int02;
    private int emb03a_int03;
    @Embedded
    private Embeddable03b embeddable03b;

    public Embeddable03a() {
        embeddable03b = new Embeddable03b();
    }

    public Embeddable03a(int emb03a_int01,
                         int emb03a_int02,
                         int emb03a_int03,
                         Embeddable03b embeddable03b) {
        this.emb03a_int01 = emb03a_int01;
        this.emb03a_int02 = emb03a_int02;
        this.emb03a_int03 = emb03a_int03;
        this.embeddable03b = embeddable03b;
    }

    @Override
    public String toString() {
        return ("Embeddable03a: " + " emb03a_int01: " + getEmb03a_int01() +
                " emb03a_int02: " + getEmb03a_int02() +
                " emb03a_int03: " + getEmb03a_int03() +
                " embeddable03b: " + getEmbeddable03b());
    }

    @Override
    public int hashCode() {
        return (emb03a_int01 ^ emb03a_int02 ^ emb03a_int03) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof Embeddable03a))
            return false;
        Embeddable03a other = (Embeddable03a) object;
        return (this.emb03a_int01 == other.emb03a_int01 &&
                this.emb03a_int02 == other.emb03a_int02 &&
                this.emb03a_int03 == other.emb03a_int03 &&
                this.embeddable03b == other.embeddable03b);
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable03a fields
    //----------------------------------------------------------------------------------------------
    public int getEmb03a_int01() {
        return emb03a_int01;
    }

    public void setEmb03a_int01(int ii) {
        this.emb03a_int01 = ii;
    }

    public int getEmb03a_int02() {
        return emb03a_int02;
    }

    public void setEmb03a_int02(int ii) {
        this.emb03a_int02 = ii;
    }

    public int getEmb03a_int03() {
        return emb03a_int03;
    }

    public void setEmb03a_int03(int ii) {
        this.emb03a_int03 = ii;
    }

    public Embeddable03b getEmbeddable03b() {
        return embeddable03b;
    }

    public void setEmbeddable03b(Embeddable03b embeddable03b) {
        this.embeddable03b = embeddable03b;
    }
}
