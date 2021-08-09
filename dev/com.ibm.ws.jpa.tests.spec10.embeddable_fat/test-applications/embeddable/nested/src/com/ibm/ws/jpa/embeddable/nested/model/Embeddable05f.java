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

@Embeddable
public class Embeddable05f {

    private String emb05f_str10;
    private String emb05f_str11;
    private String emb05f_str12;

//  @Embedded
//  private Embeddable05a embeddable05a;                    // OPENJPA-\038

    public Embeddable05f() {
//      embeddable05a = new Embeddable05a();
    }

    public Embeddable05f(String emb05f_str10,
                         String emb05f_str11,
                         String emb05f_str12) {
//                       Embeddable05f embeddable05f) {
        this.emb05f_str10 = emb05f_str10;
        this.emb05f_str11 = emb05f_str11;
        this.emb05f_str12 = emb05f_str12;
//      this.embeddable05a = embeddable05a;
    }

    @Override
    public String toString() {
        return ("Embeddable05f: " + " emb05f_str10: " + getEmb05f_str10() +
                " emb05f_str11: " + getEmb05f_str11() +
                " emb05f_str12: " + getEmb05f_str12());
    }

    @Override
    public int hashCode() {
        int ret = 0;
        if (emb05f_str10 != null)
            ret += emb05f_str10.hashCode();
        if (emb05f_str11 != null)
            ret = 31 * ret + emb05f_str11.hashCode();
        if (emb05f_str12 != null)
            ret = 31 * ret + emb05f_str12.hashCode();
        return ret;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof Embeddable05f))
            return false;
        Embeddable05f other = (Embeddable05f) object;
        return (this.emb05f_str10.equals(other.emb05f_str10) &&
                this.emb05f_str11.equals(other.emb05f_str11) && this.emb05f_str12.equals(other.emb05f_str12));
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable05f fields
    //----------------------------------------------------------------------------------------------
    public String getEmb05f_str10() {
        return emb05f_str10;
    }

    public void setEmb05f_str10(String str) {
        this.emb05f_str10 = str;
    }

    public String getEmb05f_str11() {
        return emb05f_str11;
    }

    public void setEmb05f_str11(String str) {
        this.emb05f_str11 = str;
    }

    public String getEmb05f_str12() {
        return emb05f_str12;
    }

    public void setEmb05f_str12(String str) {
        this.emb05f_str12 = str;
    }
//  public Embeddable05a getEmbeddable05a() {
//      return embeddable05a;
//  }
//  public void setEmbeddable05a(Embeddable05a embeddable05a) {
//      this.embeddable05a = embeddable05a;
//  }
}
