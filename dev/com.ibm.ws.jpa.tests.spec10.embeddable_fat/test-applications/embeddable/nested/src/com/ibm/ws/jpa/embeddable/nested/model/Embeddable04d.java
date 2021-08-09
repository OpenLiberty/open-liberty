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

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class Embeddable04d {

    private String emb04d_str10;
    private String emb04d_str11;
    private String emb04d_str12;
    @Embedded
    private Embeddable04e embeddable04e;

    public Embeddable04d() {
        embeddable04e = new Embeddable04e();
    }

    public Embeddable04d(String emb04d_str10,
                         String emb04d_str11,
                         String emb04d_str12,
                         Embeddable04e embeddable04e) {
        this.emb04d_str10 = emb04d_str10;
        this.emb04d_str11 = emb04d_str11;
        this.emb04d_str12 = emb04d_str12;
        this.embeddable04e = embeddable04e;
    }

    @Override
    public String toString() {
        return ("Embeddable04d: " +
                " emb04d_str10: " + getEmb04d_str10() +
                " emb04d_str11: " + getEmb04d_str11() +
                " emb04d_str12: " + getEmb04d_str12() +
                " embeddable04e: " + getEmbeddable04e());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (emb04d_str10 == null ? 0 : emb04d_str10.hashCode());
        hash = 31 * hash + (emb04d_str11 == null ? 0 : emb04d_str11.hashCode());
        hash = 31 * hash + (emb04d_str12 == null ? 0 : emb04d_str12.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if ((object == null) || (object.getClass() != this.getClass()))
            return false;
        Embeddable04d other = (Embeddable04d) object;
        return ((this.emb04d_str10 == other.emb04d_str10 || (this.emb04d_str10 != null && this.emb04d_str10.equals(other.emb04d_str10))) &&
                (this.emb04d_str11 == other.emb04d_str11 || (this.emb04d_str11 != null && this.emb04d_str11.equals(other.emb04d_str11))) &&
                (this.emb04d_str12 == other.emb04d_str12 || (this.emb04d_str12 != null && this.emb04d_str12.equals(other.emb04d_str12))) &&
                this.embeddable04e.equals(other.embeddable04e));
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable04d fields
    //----------------------------------------------------------------------------------------------
    public String getEmb04d_str10() {
        return emb04d_str10;
    }

    public void setEmb04d_str10(String str) {
        this.emb04d_str10 = str;
    }

    public String getEmb04d_str11() {
        return emb04d_str11;
    }

    public void setEmb04d_str11(String str) {
        this.emb04d_str11 = str;
    }

    public String getEmb04d_str12() {
        return emb04d_str12;
    }

    public void setEmb04d_str12(String str) {
        this.emb04d_str12 = str;
    }

    public Embeddable04e getEmbeddable04e() {
        return embeddable04e;
    }

    public void setEmbeddable04e(Embeddable04e embeddable04e) {
        this.embeddable04e = embeddable04e;
    }
}
