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

public class XMLEmbeddable04f {

    private String emb04f_str16;
    private String emb04f_str17;
    private String emb04f_str18;

    public XMLEmbeddable04f() {
    }

    public XMLEmbeddable04f(String emb04f_str16,
                            String emb04f_str17,
                            String emb04f_str18) {
        this.emb04f_str16 = emb04f_str16;
        this.emb04f_str17 = emb04f_str17;
        this.emb04f_str18 = emb04f_str18;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable04f: " +
                " emb04f_str16: " + getEmb04f_str16() +
                " emb04f_str17: " + getEmb04f_str17() +
                " emb04f_str18: " + getEmb04f_str18());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (emb04f_str16 == null ? 0 : emb04f_str16.hashCode());
        hash = 31 * hash + (emb04f_str17 == null ? 0 : emb04f_str17.hashCode());
        hash = 31 * hash + (emb04f_str18 == null ? 0 : emb04f_str18.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if ((object == null) || (object.getClass() != this.getClass()))
            return false;
        XMLEmbeddable04f other = (XMLEmbeddable04f) object;
        return ((this.emb04f_str16 == other.emb04f_str16 || (this.emb04f_str16 != null && this.emb04f_str16.equals(other.emb04f_str16))) &&
                (this.emb04f_str17 == other.emb04f_str17 || (this.emb04f_str17 != null && this.emb04f_str17.equals(other.emb04f_str17))) &&
                (this.emb04f_str18 == other.emb04f_str18 || (this.emb04f_str18 != null && this.emb04f_str18.equals(other.emb04f_str18))));
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable04f fields
    //----------------------------------------------------------------------------------------------
    public String getEmb04f_str16() {
        return emb04f_str16;
    }

    public void setEmb04f_str16(String str) {
        this.emb04f_str16 = str;
    }

    public String getEmb04f_str17() {
        return emb04f_str17;
    }

    public void setEmb04f_str17(String str) {
        this.emb04f_str17 = str;
    }

    public String getEmb04f_str18() {
        return emb04f_str18;
    }

    public void setEmb04f_str18(String str) {
        this.emb04f_str18 = str;
    }
}
