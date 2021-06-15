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

public class XMLEmbeddable03b {

    private int emb03b_int04;
    private int emb03b_int05;
    private int emb03b_int06;
//  private XMLEmbeddable03a embeddable03a;                 // OPENJPA-1038

    public XMLEmbeddable03b() {
//      embeddable03a = new XMLEmbeddable03a();
    }

    public XMLEmbeddable03b(int emb03b_int04,
                            int emb03b_int05,
                            int emb03b_int06) {
//                          XMLEmbeddable03a embeddable03a) {
        this.emb03b_int04 = emb03b_int04;
        this.emb03b_int05 = emb03b_int05;
        this.emb03b_int06 = emb03b_int06;
//      this.embeddable03a = embeddable03a;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable03b: " + " emb03b_int04: " + getEmb03b_int04() +
                " emb03b_int05: " + getEmb03b_int05() +
                " emb03b_int06: " + getEmb03b_int06());
    }

    @Override
    public int hashCode() {
        return (emb03b_int04 ^ emb03b_int05 ^ emb03b_int06) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof XMLEmbeddable03b))
            return false;
        XMLEmbeddable03b other = (XMLEmbeddable03b) object;
        return (this.emb03b_int04 == other.emb03b_int04 &&
                this.emb03b_int05 == other.emb03b_int05 &&
                this.emb03b_int06 == other.emb03b_int06);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable03b fields
    //----------------------------------------------------------------------------------------------
    public int getEmb03b_int04() {
        return emb03b_int04;
    }

    public void setEmb03b_int04(int ii) {
        this.emb03b_int04 = ii;
    }

    public int getEmb03b_int05() {
        return emb03b_int05;
    }

    public void setEmb03b_int05(int ii) {
        this.emb03b_int05 = ii;
    }

    public int getEmb03b_int06() {
        return emb03b_int06;
    }

    public void setEmb03b_int06(int ii) {
        this.emb03b_int06 = ii;
    }
//  public XMLEmbeddable03a getEmbeddable03a() {
//      return embeddable03a;
//  }
//  public void setEmbeddable03a(XMLEmbeddable03a embeddable03a) {
//      this.embeddable03a = embeddable03a;
//  }
}
