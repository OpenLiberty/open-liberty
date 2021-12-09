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

public class XMLEmbeddable05b {

    private String emb05b_str04;
    private String emb05b_str05;
    private String emb05b_str06;
    private XMLEmbeddable05c embeddable05c;

    public XMLEmbeddable05b() {
        embeddable05c = new XMLEmbeddable05c();
    }

    public XMLEmbeddable05b(String emb05b_str04,
                            String emb05b_str05,
                            String emb05b_str06,
                            XMLEmbeddable05c embeddable05c) {
        this.emb05b_str04 = emb05b_str04;
        this.emb05b_str05 = emb05b_str05;
        this.emb05b_str06 = emb05b_str06;
        this.embeddable05c = embeddable05c;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable05b: " + " emb05b_str04: " + getEmb05b_str04() +
                " emb05b_str05: " + getEmb05b_str05() +
                " emb05b_str06: " + getEmb05b_str06() +
                " embeddable05c: " + getEmbeddable05c());
    }

    @Override
    public int hashCode() {
        int ret = 0;
        if (emb05b_str04 != null)
            ret += emb05b_str04.hashCode();
        if (emb05b_str05 != null)
            ret = 31 * ret + emb05b_str05.hashCode();
        if (emb05b_str06 != null)
            ret = 31 * ret + emb05b_str06.hashCode();
        return ret;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof XMLEmbeddable05b))
            return false;
        XMLEmbeddable05b other = (XMLEmbeddable05b) object;
        return (this.emb05b_str04.equals(other.emb05b_str04) &&
                this.emb05b_str05.equals(other.emb05b_str05) &&
                this.emb05b_str06.equals(other.emb05b_str06) &&
                this.embeddable05c.equals(other.embeddable05c));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable05b fields
    //----------------------------------------------------------------------------------------------
    public String getEmb05b_str04() {
        return emb05b_str04;
    }

    public void setEmb05b_str04(String str) {
        this.emb05b_str04 = str;
    }

    public String getEmb05b_str05() {
        return emb05b_str05;
    }

    public void setEmb05b_str05(String str) {
        this.emb05b_str05 = str;
    }

    public String getEmb05b_str06() {
        return emb05b_str06;
    }

    public void setEmb05b_str06(String str) {
        this.emb05b_str06 = str;
    }

    public XMLEmbeddable05c getEmbeddable05c() {
        return embeddable05c;
    }

    public void setembeddable05c(XMLEmbeddable05c embeddable05c) {
        this.embeddable05c = embeddable05c;
    }
}
