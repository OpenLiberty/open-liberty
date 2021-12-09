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

public class XMLEmbeddable07b {

    private int emb07b_int04;
    private int emb07b_int05;
    private int emb07b_int06;
    private XMLEmbeddable04a embeddable04a;

    public XMLEmbeddable07b() {
        embeddable04a = new XMLEmbeddable04a();
    }

    public XMLEmbeddable07b(int emb07b_int04,
                            int emb07b_int05,
                            int emb07b_int06,
                            XMLEmbeddable04a embeddable04a) {
        this.emb07b_int04 = emb07b_int04;
        this.emb07b_int05 = emb07b_int05;
        this.emb07b_int06 = emb07b_int06;
        this.embeddable04a = embeddable04a;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable07b: " + " emb07b_int04: " + getEmb07b_int04() +
                " emb07b_int05: " + getEmb07b_int05() +
                " emb07b_int06: " + getEmb07b_int06() +
                " embeddable04a: " + getEmbeddable04a());
    }

    @Override
    public int hashCode() {
        return (emb07b_int04 ^ emb07b_int05 ^ emb07b_int06 ^ embeddable04a.hashCode()) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof XMLEmbeddable07b))
            return false;
        XMLEmbeddable07b other = (XMLEmbeddable07b) object;
        return (this.emb07b_int04 == other.emb07b_int04 &&
                this.emb07b_int05 == other.emb07b_int05 &&
                this.emb07b_int06 == other.emb07b_int06 &&
                this.embeddable04a.equals(other.embeddable04a));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable07b fields
    //----------------------------------------------------------------------------------------------
    public int getEmb07b_int04() {
        return emb07b_int04;
    }

    public void setEmb07b_int04(int ii) {
        this.emb07b_int04 = ii;
    }

    public int getEmb07b_int05() {
        return emb07b_int05;
    }

    public void setEmb07b_int05(int ii) {
        this.emb07b_int05 = ii;
    }

    public int getEmb07b_int06() {
        return emb07b_int06;
    }

    public void setEmb07b_int06(int ii) {
        this.emb07b_int06 = ii;
    }

    public XMLEmbeddable04a getEmbeddable04a() {
        return embeddable04a;
    }

    public void setEmbeddable04a(XMLEmbeddable04a embeddable04a) {
        this.embeddable04a = embeddable04a;
    }
}
