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

public class XMLEmbeddable07a {

    private int emb07a_int01;
    private int emb07a_int02;
    private int emb07a_int03;
    private XMLEmbeddable07b embeddable07b;
    private XMLEmbeddable01 embeddable01;

    public XMLEmbeddable07a() {
        embeddable07b = new XMLEmbeddable07b();
        embeddable01 = new XMLEmbeddable01();
    }

    public XMLEmbeddable07a(int emb07a_int01,
                            int emb07a_int02,
                            int emb07a_int03,
                            XMLEmbeddable07b embeddable07b,
                            XMLEmbeddable01 embeddable01) {
        this.emb07a_int01 = emb07a_int01;
        this.emb07a_int02 = emb07a_int02;
        this.emb07a_int03 = emb07a_int03;
        this.embeddable07b = embeddable07b;
        this.embeddable01 = embeddable01;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable07a: " + " emb07a_int01: " + getEmb07a_int01() +
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
        if (!(object instanceof XMLEmbeddable07a))
            return false;
        XMLEmbeddable07a other = (XMLEmbeddable07a) object;
        return (this.emb07a_int01 == other.emb07a_int01 &&
                this.emb07a_int02 == other.emb07a_int02 &&
                this.emb07a_int03 == other.emb07a_int03 &&
                this.embeddable07b.equals(other.embeddable07b) &&
                this.embeddable01.equals(other.embeddable01));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable07a fields
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

    public XMLEmbeddable07b getEmbeddable07b() {
        return embeddable07b;
    }

    public void setXmlEmbeddable07b(XMLEmbeddable07b embeddable07b) {
        this.embeddable07b = embeddable07b;
    }

    public XMLEmbeddable01 getEmbeddable01() {
        return embeddable01;
    }

    public void setXMLEmbeddable01(XMLEmbeddable01 embeddable01) {
        this.embeddable01 = embeddable01;
    }
}
