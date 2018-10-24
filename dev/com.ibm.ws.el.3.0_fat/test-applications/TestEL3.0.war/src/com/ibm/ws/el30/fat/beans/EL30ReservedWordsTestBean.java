/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el30.fat.beans;

/**
 * This is just a basic java bean which help testing all the EL 3.0 reserved words
 */
public class EL30ReservedWordsTestBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String and;
    private String or;
    private String not;
    private String eq;
    private String ne;
    private String lt;
    private String gt;
    private String le;
    private String ge;
    private String True;
    private String False;
    private String Null;
    private String Instanceof;
    private String empty;
    private String div;
    private String mod;
    private String cat;
    private String T;

    public EL30ReservedWordsTestBean() {}

    public void setAnd(String and) {
        this.and = and;
    }

    public String getAnd() {
        return and;
    }

    public void setOr(String or) {
        this.or = or;
    }

    public String getOr() {
        return or;
    }

    public void setNot(String not) {
        this.not = not;
    }

    public String getNot() {
        return not;
    }

    public void setEq(String eq) {
        this.eq = eq;
    }

    public String getEq() {
        return eq;
    }

    public void setNe(String ne) {
        this.ne = ne;
    }

    public String getNe() {
        return ne;
    }

    public void setLt(String lt) {
        this.lt = lt;
    }

    public String getLt() {
        return lt;
    }

    public void setGt(String gt) {
        this.gt = gt;
    }

    public String getGt() {
        return gt;
    }

    public void setLe(String le) {
        this.le = le;
    }

    public String getLe() {
        return le;
    }

    public void setGe(String ge) {
        this.ge = ge;
    }

    public String getGe() {
        return ge;
    }

    public void setTrue(String True) {
        this.True = True;
    }

    public String getTrue() {
        return True;
    }

    public void setFalse(String False) {
        this.False = False;
    }

    public String getFalse() {
        return False;
    }

    public void setNull(String Null) {
        this.Null = Null;
    }

    public String getNull() {
        return Null;
    }

    public void setInstanceof(String Instanceof) {
        this.Instanceof = Instanceof;
    }

    public String getInsanceof() {
        return Instanceof;
    }

    public void setEmpty(String empty) {
        this.empty = empty;
    }

    public String getEmpty() {
        return empty;
    }

    public void setDiv(String div) {
        this.div = div;
    }

    public String getDiv() {
        return div;
    }

    public void setMod(String mod) {
        this.mod = mod;
    }

    public String getMod() {
        return mod;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }

    public String getCat() {
        return cat;
    }

    public void setT(String T) {
        this.T = T;
    }

    public String getT() {
        return T;
    }

}
