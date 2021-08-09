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

public class XMLEmbeddable02b {

    private int emb02b_int04;
    private int emb02b_int05;
    private int emb02b_int06;

    public XMLEmbeddable02b() {
    }

    public XMLEmbeddable02b(int emb02b_int04,
                            int emb02b_int05,
                            int emb02b_int06) {
        this.emb02b_int04 = emb02b_int04;
        this.emb02b_int05 = emb02b_int05;
        this.emb02b_int06 = emb02b_int06;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable02b: " + " emb02b_int04: " + getEmb02b_int04() +
                " emb02b_int05: " + getEmb02b_int05() +
                " emb02b_int06: " + getEmb02b_int06());
    }

    @Override
    public int hashCode() {
        return (emb02b_int04 ^ emb02b_int04 ^ emb02b_int04) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof XMLEmbeddable02b))
            return false;
        XMLEmbeddable02b other = (XMLEmbeddable02b) object;
        return (this.emb02b_int04 == other.emb02b_int04 &&
                this.emb02b_int05 == other.emb02b_int05 &&
                this.emb02b_int06 == other.emb02b_int06);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable02b fields
    //----------------------------------------------------------------------------------------------
    public int getEmb02b_int04() {
        return emb02b_int04;
    }

    public void setEmb02b_int04(int ii) {
        this.emb02b_int04 = ii;
    }

    public int getEmb02b_int05() {
        return emb02b_int05;
    }

    public void setEmb02b_int05(int ii) {
        this.emb02b_int05 = ii;
    }

    public int getEmb02b_int06() {
        return emb02b_int06;
    }

    public void setEmb02b_int06(int ii) {
        this.emb02b_int06 = ii;
    }
}
