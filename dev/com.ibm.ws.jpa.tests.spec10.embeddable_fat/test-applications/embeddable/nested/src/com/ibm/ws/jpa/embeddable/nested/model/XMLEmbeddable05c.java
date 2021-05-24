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

public class XMLEmbeddable05c {

    private int emb05c_int04;
    private int emb05c_int05;
    private int emb05c_int06;
    private XMLEmbeddable05d embeddable05d;

    public XMLEmbeddable05c() {
        embeddable05d = new XMLEmbeddable05d();
    }

    public XMLEmbeddable05c(int emb05c_int04,
                            int emb05c_int05,
                            int emb05c_int06,
                            XMLEmbeddable05d embeddable05d) {
        this.emb05c_int04 = emb05c_int04;
        this.emb05c_int05 = emb05c_int05;
        this.emb05c_int06 = emb05c_int06;
        this.embeddable05d = embeddable05d;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable05c: " + " emb05c_int04: " + getEmb05c_int04() +
                " emb05c_int05: " + getEmb05c_int05() +
                " emb05c_int06: " + getEmb05c_int06() +
                " embeddable05d: " + getEmbeddable05d());
    }

    @Override
    public int hashCode() {
        return (emb05c_int04 ^ emb05c_int05 ^ emb05c_int05) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof XMLEmbeddable05c))
            return false;
        XMLEmbeddable05c other = (XMLEmbeddable05c) object;
        return (this.emb05c_int04 == other.emb05c_int04 &&
                this.emb05c_int05 == other.emb05c_int05 &&
                this.emb05c_int06 == other.emb05c_int06 &&
                this.embeddable05d.equals(other.embeddable05d));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05c fields
    //----------------------------------------------------------------------------------------------
    public int getEmb05c_int04() {
        return emb05c_int04;
    }

    public void setEmb05c_int04(int ii) {
        this.emb05c_int04 = ii;
    }

    public int getEmb05c_int05() {
        return emb05c_int05;
    }

    public void setEmb05c_int05(int ii) {
        this.emb05c_int05 = ii;
    }

    public int getEmb05c_int06() {
        return emb05c_int06;
    }

    public void setEmb05c_int06(int ii) {
        this.emb05c_int06 = ii;
    }

    public XMLEmbeddable05d getEmbeddable05d() {
        return embeddable05d;
    }

    public void setEmbeddable05d(XMLEmbeddable05d embeddable05d) {
        this.embeddable05d = embeddable05d;
    }
}
