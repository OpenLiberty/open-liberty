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

public class XMLEmbeddable04c {

    private int emb04c_int07;
    private int emb04c_int08;
    private int emb04c_int09;
    private XMLEmbeddable04d embeddable04d;

    public XMLEmbeddable04c() {
        embeddable04d = new XMLEmbeddable04d();
    }

    public XMLEmbeddable04c(int emb04c_int07,
                            int emb04c_int08,
                            int emb04c_int09,
                            XMLEmbeddable04d embeddable04d) {
        this.emb04c_int07 = emb04c_int07;
        this.emb04c_int08 = emb04c_int08;
        this.emb04c_int09 = emb04c_int09;
        this.embeddable04d = embeddable04d;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable04c: " +
                " emb04c_int07: " + getEmb04c_int07() +
                " emb04c_int08: " + getEmb04c_int08() +
                " emb04c_int09: " + getEmb04c_int09() +
                " embeddable04d: " + getEmbeddable04d());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + emb04c_int07;
        hash = 31 * hash + emb04c_int08;
        hash = 31 * hash + emb04c_int09;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if ((object == null) || (object.getClass() != this.getClass()))
            return false;
        XMLEmbeddable04c other = (XMLEmbeddable04c) object;
        return (this.emb04c_int07 == other.emb04c_int07 &&
                this.emb04c_int08 == other.emb04c_int08 &&
                this.emb04c_int09 == other.emb04c_int09 &&
                this.embeddable04d.equals(other.embeddable04d));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable04c fields
    //----------------------------------------------------------------------------------------------
    public int getEmb04c_int07() {
        return emb04c_int07;
    }

    public void setEmb04c_int07(int ii) {
        this.emb04c_int07 = ii;
    }

    public int getEmb04c_int08() {
        return emb04c_int08;
    }

    public void setEmb04c_int08(int ii) {
        this.emb04c_int08 = ii;
    }

    public int getEmb04c_int09() {
        return emb04c_int09;
    }

    public void setEmb04c_int09(int ii) {
        this.emb04c_int09 = ii;
    }

    public XMLEmbeddable04d getEmbeddable04d() {
        return embeddable04d;
    }

    public void setEmbeddable04d(XMLEmbeddable04d embeddable04d) {
        this.embeddable04d = embeddable04d;
    }
}
