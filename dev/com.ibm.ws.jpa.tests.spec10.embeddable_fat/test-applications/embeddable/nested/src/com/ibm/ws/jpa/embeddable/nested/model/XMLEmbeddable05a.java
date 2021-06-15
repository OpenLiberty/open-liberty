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

public class XMLEmbeddable05a {

    private int emb05a_int01;
    private int emb05a_int02;
    private int emb05a_int03;
    private XMLEmbeddable05b embeddable05b;

    public XMLEmbeddable05a() {
        embeddable05b = new XMLEmbeddable05b();
    }

    public XMLEmbeddable05a(int emb05a_int01,
                            int emb05a_int02,
                            int emb05a_int03,
                            XMLEmbeddable05b embeddable05b) {
        this.emb05a_int01 = emb05a_int01;
        this.emb05a_int02 = emb05a_int02;
        this.emb05a_int03 = emb05a_int03;
        this.embeddable05b = embeddable05b;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable05a: " + " emb05a_int01: " + getEmb05a_int01() +
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
        if (!(object instanceof XMLEmbeddable05a))
            return false;
        XMLEmbeddable05a other = (XMLEmbeddable05a) object;
        return (this.emb05a_int01 == other.emb05a_int01 &&
                this.emb05a_int02 == other.emb05a_int02 &&
                this.emb05a_int03 == other.emb05a_int03 &&
                this.embeddable05b.equals(other.embeddable05b));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05a fields
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

    public XMLEmbeddable05b getEmbeddable05b() {
        return embeddable05b;
    }

    public void setEmbeddable05b(XMLEmbeddable05b embeddable05b) {
        this.embeddable05b = embeddable05b;
    }
}
