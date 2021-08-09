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

public class XMLEmbeddable04b {

    private String emb04b_str04;
    private String emb04b_str05;
    private String emb04b_str06;
    private XMLEmbeddable04c embeddable04c;

    public XMLEmbeddable04b() {
        embeddable04c = new XMLEmbeddable04c();
    }

    public XMLEmbeddable04b(String emb04b_str04,
                            String emb04b_str05,
                            String emb04b_str06,
                            XMLEmbeddable04c embeddable04c) {
        this.emb04b_str04 = emb04b_str04;
        this.emb04b_str05 = emb04b_str05;
        this.emb04b_str06 = emb04b_str06;
        this.embeddable04c = embeddable04c;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable04b: " +
                " emb04b_str04: " + getEmb04b_str04() +
                " emb04b_str05: " + getEmb04b_str05() +
                " emb04b_str06: " + getEmb04b_str06() +
                " embeddable04c: " + getEmbeddable04c());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (emb04b_str04 == null ? 0 : emb04b_str04.hashCode());
        hash = 31 * hash + (emb04b_str05 == null ? 0 : emb04b_str05.hashCode());
        hash = 31 * hash + (emb04b_str06 == null ? 0 : emb04b_str06.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if ((object == null) || (object.getClass() != this.getClass()))
            return false;
        XMLEmbeddable04b other = (XMLEmbeddable04b) object;
        return ((this.emb04b_str04 == other.emb04b_str04 || (this.emb04b_str04 != null && this.emb04b_str04.equals(other.emb04b_str04))) &&
                (this.emb04b_str05 == other.emb04b_str05 || (this.emb04b_str05 != null && this.emb04b_str05.equals(other.emb04b_str05))) &&
                (this.emb04b_str06 == other.emb04b_str06 || (this.emb04b_str06 != null && this.emb04b_str06.equals(other.emb04b_str06))) &&
                this.embeddable04c.equals(other.embeddable04c));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable04b fields
    //----------------------------------------------------------------------------------------------
    public String getEmb04b_str04() {
        return emb04b_str04;
    }

    public void setEmb04b_str04(String str) {
        this.emb04b_str04 = str;
    }

    public String getEmb04b_str05() {
        return emb04b_str05;
    }

    public void setEmb04b_str05(String str) {
        this.emb04b_str05 = str;
    }

    public String getEmb04b_str06() {
        return emb04b_str06;
    }

    public void setEmb04b_str06(String str) {
        this.emb04b_str06 = str;
    }

    public XMLEmbeddable04c getEmbeddable04c() {
        return embeddable04c;
    }

    public void setembeddable04c(XMLEmbeddable04c embeddable04c) {
        this.embeddable04c = embeddable04c;
    }
}
