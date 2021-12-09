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

public class XMLEmbeddable01 {

    private int emb01_int01;
    private int emb01_int02;
    private int emb01_int03;

    public XMLEmbeddable01() {
    }

    public XMLEmbeddable01(int emb01_int01,
                           int emb01_int02,
                           int emb01_int03) {
        this.emb01_int01 = emb01_int01;
        this.emb01_int02 = emb01_int02;
        this.emb01_int03 = emb01_int03;
    }

    @Override
    public String toString() {
        return ("XMLEmbeddable01: " +
                " emb01_int01: " + getEmb01_int01() +
                " emb01_int02: " + getEmb01_int02() +
                " emb01_int03: " + getEmb01_int03());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + emb01_int01;
        hash = 31 * hash + emb01_int02;
        hash = 31 * hash + emb01_int03;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if ((object == null) || (object.getClass() != this.getClass()))
            return false;
        XMLEmbeddable01 other = (XMLEmbeddable01) object;
        return (this.emb01_int01 == other.emb01_int01 &&
                this.emb01_int02 == other.emb01_int02 &&
                this.emb01_int03 == other.emb01_int03);
    }

    //----------------------------------------------------------------------------------------------
    // XMLEmbeddable01 fields
    //----------------------------------------------------------------------------------------------
    public int getEmb01_int01() {
        return emb01_int01;
    }

    public void setEmb01_int01(int ii) {
        this.emb01_int01 = ii;
    }

    public int getEmb01_int02() {
        return emb01_int02;
    }

    public void setEmb01_int02(int ii) {
        this.emb01_int02 = ii;
    }

    public int getEmb01_int03() {
        return emb01_int03;
    }

    public void setEmb01_int03(int ii) {
        this.emb01_int03 = ii;
    }
}
