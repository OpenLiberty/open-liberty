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
public class Embeddable05d {

    private String emb05d_str07;
    private String emb05d_str08;
    private String emb05d_str09;
    @Embedded
    private Embeddable05e embeddable05e;

    public Embeddable05d() {
        embeddable05e = new Embeddable05e();
    }

    public Embeddable05d(String emb05d_str07,
                         String emb05d_str08,
                         String emb05d_str09,
                         Embeddable05e embeddable05e) {
        this.emb05d_str07 = emb05d_str07;
        this.emb05d_str08 = emb05d_str08;
        this.emb05d_str09 = emb05d_str09;
        this.embeddable05e = embeddable05e;
    }

    @Override
    public String toString() {
        return ("Embeddable05d: " + " emb05d_str07: " + getEmb05d_str07() +
                " emb05d_str08: " + getEmb05d_str08() +
                " emb05d_str09: " + getEmb05d_str09() +
                " embeddable05e: " + getEmbeddable05e());
    }

    @Override
    public int hashCode() {
        int ret = 0;
        if (emb05d_str07 != null)
            ret += emb05d_str07.hashCode();
        if (emb05d_str08 != null)
            ret = 31 * ret + emb05d_str08.hashCode();
        if (emb05d_str09 != null)
            ret = 31 * ret + emb05d_str09.hashCode();
        return ret;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof Embeddable05d))
            return false;
        Embeddable05d other = (Embeddable05d) object;
        return (this.emb05d_str07.equals(other.emb05d_str07) &&
                this.emb05d_str08.equals(other.emb05d_str08) &&
                this.emb05d_str09.equals(other.emb05d_str09) &&
                this.embeddable05e.equals(other.embeddable05e));
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable05d fields
    //----------------------------------------------------------------------------------------------
    public String getEmb05d_str07() {
        return emb05d_str07;
    }

    public void setEmb05d_str07(String str) {
        this.emb05d_str07 = str;
    }

    public String getEmb05d_str08() {
        return emb05d_str08;
    }

    public void setEmb05d_str08(String str) {
        this.emb05d_str08 = str;
    }

    public String getEmb05d_str09() {
        return emb05d_str09;
    }

    public void setEmb05d_str09(String str) {
        this.emb05d_str09 = str;
    }

    public Embeddable05e getEmbeddable05e() {
        return embeddable05e;
    }

    public void setEmbeddable05e(Embeddable05e embeddable05e) {
        this.embeddable05e = embeddable05e;
    }
}
