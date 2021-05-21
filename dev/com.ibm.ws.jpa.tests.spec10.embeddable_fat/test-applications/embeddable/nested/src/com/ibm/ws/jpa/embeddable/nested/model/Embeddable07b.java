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

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Embeddable07b {

    private int emb07b_int04;
    private int emb07b_int05;
    private int emb07b_int06;
    @AttributeOverrides({
                          @AttributeOverride(name = "emb04a_int01", column = @Column(name = "emb07b_emb04a_int01")),
                          @AttributeOverride(name = "emb04a_int02", column = @Column(name = "emb07b_emb04a_int02")),
                          @AttributeOverride(name = "emb04a_int03", column = @Column(name = "emb07b_emb04a_int03")),
                          @AttributeOverride(name = "embeddable04b.emb04b_str04", column = @Column(name = "emb07b_emb04b_str04")),
                          @AttributeOverride(name = "embeddable04b.emb04b_str05", column = @Column(name = "emb07b_emb04b_str05")),
                          @AttributeOverride(name = "embeddable04b.emb04b_str06", column = @Column(name = "emb07b_emb04b_str06")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.emb04c_int07", column = @Column(name = "emb07b_emb04c_int07")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.emb04c_int08", column = @Column(name = "emb07b_emb04c_int08")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.emb04c_int09", column = @Column(name = "emb07b_emb04c_int09")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.embeddable04d.emb04d_str10", column = @Column(name = "emb07b_emb04d_str10")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.embeddable04d.emb04d_str11", column = @Column(name = "emb07b_emb04d_str11")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.embeddable04d.emb04d_str12", column = @Column(name = "emb07b_emb04d_str12")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.embeddable04d.embeddable04e.emb04e_int13", column = @Column(name = "emb07b_emb04e_int13")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.embeddable04d.embeddable04e.emb04e_int14", column = @Column(name = "emb07b_emb04e_int14")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.embeddable04d.embeddable04e.emb04e_int15", column = @Column(name = "emb07b_emb04e_int15")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.embeddable04d.embeddable04e.embeddable04f.emb04f_str16",
                                             column = @Column(name = "emb07b_emb04f_str16")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.embeddable04d.embeddable04e.embeddable04f.emb04f_str17",
                                             column = @Column(name = "emb07b_emb04f_str17")),
                          @AttributeOverride(name = "embeddable04b.embeddable04c.embeddable04d.embeddable04e.embeddable04f.emb04f_str18",
                                             column = @Column(name = "emb07b_emb04f_str18"))
    })
    private Embeddable04a embeddable04a;

    public Embeddable07b() {
        embeddable04a = new Embeddable04a();
    }

    public Embeddable07b(int emb07b_int04,
                         int emb07b_int05,
                         int emb07b_int06,
                         Embeddable04a embeddable04a) {
        this.emb07b_int04 = emb07b_int04;
        this.emb07b_int05 = emb07b_int05;
        this.emb07b_int06 = emb07b_int06;
        this.embeddable04a = embeddable04a;
    }

    @Override
    public String toString() {
        return ("Embeddable07b: " +
                " emb07b_int04: " + getEmb07b_int04() +
                " emb07b_int05: " + getEmb07b_int05() +
                " emb07b_int06: " + getEmb07b_int06() +
                " embeddable04a: " + getEmbeddable04a());
    }

    @Override
    public int hashCode() {
        return (emb07b_int04 ^ emb07b_int05 ^ emb07b_int06) % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof Embeddable07b))
            return false;
        Embeddable07b other = (Embeddable07b) object;
        return (this.emb07b_int04 == other.emb07b_int04 &&
                this.emb07b_int05 == other.emb07b_int05 &&
                this.emb07b_int06 == other.emb07b_int06 && this.embeddable04a.equals(other.embeddable04a));
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable07b fields
    //----------------------------------------------------------------------------------------------
    public int getEmb07b_int04() {
        return emb07b_int04;
    }

    public void setEmb07b_int04(int ii) {
        this.emb07b_int04 = ii;
    }

    public int getEmb07b_int05() {
        return emb07b_int05;
    }

    public void setEmb07b_int05(int ii) {
        this.emb07b_int05 = ii;
    }

    public int getEmb07b_int06() {
        return emb07b_int06;
    }

    public void setEmb07b_int06(int ii) {
        this.emb07b_int06 = ii;
    }

    public Embeddable04a getEmbeddable04a() {
        return embeddable04a;
    }

    public void setEmbeddable04a(Embeddable04a embeddable04a) {
        this.embeddable04a = embeddable04a;
    }
}
