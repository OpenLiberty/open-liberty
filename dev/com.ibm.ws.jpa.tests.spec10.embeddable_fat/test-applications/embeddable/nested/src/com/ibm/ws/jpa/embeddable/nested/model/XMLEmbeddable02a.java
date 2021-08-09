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

public class XMLEmbeddable02a {

    private int emb02a_int01;
    private int emb02a_int02;
    private int emb02a_int03;
    private XMLEmbeddable02b embeddable02b;

    public XMLEmbeddable02a() {
        embeddable02b = new XMLEmbeddable02b();
    }

    public XMLEmbeddable02a(int emb02a_int01,
                            int emb02a_int02,
                            int emb02a_int03,
                            XMLEmbeddable02b embeddable02b) {
        this.emb02a_int01 = emb02a_int01;
        this.emb02a_int02 = emb02a_int02;
        this.emb02a_int03 = emb02a_int03;
        this.embeddable02b = embeddable02b;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable02a: " + " emb02a_int01: " + getEmb02a_int01() +
                " emb02a_int02: " + getEmb02a_int02() +
                " emb02a_int03: " + getEmb02a_int03() +
                " embeddable02b: " + getEmbeddable02b());
    }

    @Override
    public int hashCode() {
        return (emb02a_int01 ^ emb02a_int02 ^ emb02a_int03) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof XMLEmbeddable02a))
            return false;
        XMLEmbeddable02a other = (XMLEmbeddable02a) object;
        return (this.emb02a_int01 == other.emb02a_int01 &&
                this.emb02a_int02 == other.emb02a_int02 &&
                this.emb02a_int03 == other.emb02a_int03 &&
                this.embeddable02b == other.embeddable02b);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable02a fields
    //----------------------------------------------------------------------------------------------
    public int getEmb02a_int01() {
        return emb02a_int01;
    }

    public void setEmb02a_int01(int ii) {
        this.emb02a_int01 = ii;
    }

    public int getEmb02a_int02() {
        return emb02a_int02;
    }

    public void setEmb02a_int02(int ii) {
        this.emb02a_int02 = ii;
    }

    public int getEmb02a_int03() {
        return emb02a_int03;
    }

    public void setEmb02a_int03(int ii) {
        this.emb02a_int03 = ii;
    }

    public XMLEmbeddable02b getEmbeddable02b() {
        return embeddable02b;
    }

    public void setXmlEmbeddable02b(XMLEmbeddable02b embeddable02b) {
        this.embeddable02b = embeddable02b;
    }
}
