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

public class XMLEmbeddable05e {

    private int emb05e_int07;
    private int emb05e_int08;
    private int emb05e_int09;
    private XMLEmbeddable05f embeddable05f;

    public XMLEmbeddable05e() {
        embeddable05f = new XMLEmbeddable05f();
    }

    public XMLEmbeddable05e(int emb05e_int07,
                            int emb05e_int08,
                            int emb05e_int09,
                            XMLEmbeddable05f embeddable05f) {
        this.emb05e_int07 = emb05e_int07;
        this.emb05e_int08 = emb05e_int08;
        this.emb05e_int09 = emb05e_int09;
        this.embeddable05f = embeddable05f;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable05e: " + " emb05e_int07: " + getEmb05e_int07() +
                " emb05e_int08: " + getEmb05e_int08() +
                " emb05e_int09: " + getEmb05e_int09() +
                " embeddable05f: " + getEmbeddable05f());
    }

    @Override
    public int hashCode() {
        return (emb05e_int07 ^ emb05e_int08 ^ emb05e_int09) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof XMLEmbeddable05e))
            return false;
        XMLEmbeddable05e other = (XMLEmbeddable05e) object;
        return (this.emb05e_int07 == other.emb05e_int07 &&
                this.emb05e_int08 == other.emb05e_int08 &&
                this.emb05e_int09 == other.emb05e_int09 &&
                this.embeddable05f.equals(other.embeddable05f));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05e fields
    //----------------------------------------------------------------------------------------------
    public int getEmb05e_int07() {
        return emb05e_int07;
    }

    public void setEmb05e_int07(int ii) {
        this.emb05e_int07 = ii;
    }

    public int getEmb05e_int08() {
        return emb05e_int08;
    }

    public void setEmb05e_int08(int ii) {
        this.emb05e_int08 = ii;
    }

    public int getEmb05e_int09() {
        return emb05e_int09;
    }

    public void setEmb05e_int09(int ii) {
        this.emb05e_int09 = ii;
    }

    public XMLEmbeddable05f getEmbeddable05f() {
        return embeddable05f;
    }

    public void setEmbeddable05f(XMLEmbeddable05f embeddable05f) {
        this.embeddable05f = embeddable05f;
    }
}
