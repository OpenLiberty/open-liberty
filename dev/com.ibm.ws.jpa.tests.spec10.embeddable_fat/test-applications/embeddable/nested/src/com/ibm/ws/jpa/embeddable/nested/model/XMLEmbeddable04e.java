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

public class XMLEmbeddable04e {

    private int emb04e_int13;
    private int emb04e_int14;
    private int emb04e_int15;
    private XMLEmbeddable04f embeddable04f;

    public XMLEmbeddable04e() {
        embeddable04f = new XMLEmbeddable04f();
    }

    public XMLEmbeddable04e(int emb04e_int13,
                            int emb04e_int14,
                            int emb04e_int15,
                            XMLEmbeddable04f embeddable04f) {
        this.emb04e_int13 = emb04e_int13;
        this.emb04e_int14 = emb04e_int14;
        this.emb04e_int15 = emb04e_int15;
        this.embeddable04f = embeddable04f;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable04e: " +
                " emb04e_int13: " + getEmb04e_int13() +
                " emb04e_int14: " + getEmb04e_int14() +
                " emb04e_int15: " + getEmb04e_int15() +
                " embeddable04f: " + getEmbeddable04f());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + emb04e_int13;
        hash = 31 * hash + emb04e_int14;
        hash = 31 * hash + emb04e_int15;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if ((object == null) || (object.getClass() != this.getClass()))
            return false;
        XMLEmbeddable04e other = (XMLEmbeddable04e) object;
        return (this.emb04e_int13 == other.emb04e_int13 &&
                this.emb04e_int14 == other.emb04e_int14 &&
                this.emb04e_int15 == other.emb04e_int15 &&
                this.embeddable04f.equals(other.embeddable04f));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable04e fields
    //----------------------------------------------------------------------------------------------
    public int getEmb04e_int13() {
        return emb04e_int13;
    }

    public void setEmb04e_int13(int ii) {
        this.emb04e_int13 = ii;
    }

    public int getEmb04e_int14() {
        return emb04e_int14;
    }

    public void setEmb04e_int14(int ii) {
        this.emb04e_int14 = ii;
    }

    public int getEmb04e_int15() {
        return emb04e_int15;
    }

    public void setEmb04e_int15(int ii) {
        this.emb04e_int15 = ii;
    }

    public XMLEmbeddable04f getEmbeddable04f() {
        return embeddable04f;
    }

    public void setEmbeddable04f(XMLEmbeddable04f embeddable04f) {
        this.embeddable04f = embeddable04f;
    }
}
