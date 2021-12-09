/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.annotation;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IEntityB;

/**
 * Simple entity to test Unidirectional OneToOne relationships.
 *
 * UniEntityA has a unidirectional relationship with UniEntityB, with A referencing B. UniEntityA is the owning part of
 * the relationship.
 *
 * Annotations are declared on the entity fields.
 *
 */
@Entity
public class OOUniEntA implements IEntityA {
    /**
     * Entity primary key, an integer id number.
     */
    @Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    /**
     * One to one mapping to an UniEntityB-type entity. Overriding the name of the join column to "UNIENT_B1".
     *
     * OneToOne Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "UNIENT_B1".
     */
    @OneToOne
    @JoinColumn(name = "UNIENT_B1")
    private OOUniEntB b1;

    /**
     * One to one mapping to an UniEntityB-type entity. No override of the foreign key column name, which should default
     * to "B2_ID" (Name of the field of the referencing entity + " " + the name of the referenced primary key column).
     *
     * OneToOne Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     */
    @OneToOne
    private OOUniEntB b2;

    /**
     * One to one mapping to an UniEntityB-type entity. FetchType has been set to LAZY.
     *
     * OneToOne Config Cascade: default no Fetch: LAZY Optional: default true (reference can be null).
     *
     */
    @OneToOne(fetch = FetchType.LAZY)
    private OOUniEntB b4;

    /**
     * One to one mapping to an UniEntityB-type entity. This relation field has the CascadeType of ALL.
     *
     * OneToOne Config Cascade: ALL Fetch: default eager Optional: default true (reference can be null).
     *
     */
    @OneToOne(cascade = CascadeType.ALL)
    private OOUniEntB b5ca;

    /**
     * One to one mapping to an UniEntityB-type entity. This relation field has the CascadeType of MERGE.
     *
     * OneToOne Config Cascade: MERGE Fetch: default eager Optional: default true (reference can be null).
     *
     */
    @OneToOne(cascade = CascadeType.MERGE)
    private OOUniEntB b5cm;

    /**
     * One to one mapping to an UniEntityB-type entity. This relation field has the CascadeType of PERSIST.
     *
     * OneToOne Config Cascade: PERSIST Fetch: default eager Optional: default true (reference can be null).
     *
     */
    @OneToOne(cascade = CascadeType.PERSIST)
    private OOUniEntB b5cp;

    /**
     * One to one mapping to an UniEntityB-type entity. This relation field has the CascadeType of REFRESH.
     *
     * OneToOne Config Cascade: REFRESH Fetch: default eager Optional: default true (reference can be null).
     *
     */
    @OneToOne(cascade = CascadeType.REFRESH)
    private OOUniEntB b5rf;

    /**
     * One to one mapping to an UniEntityB-type entity. This relation field has the CascadeType of REMOVE.
     *
     * OneToOne Config Cascade: REMOVE Fetch: default eager Optional: default true (reference can be null).
     *
     */
    @OneToOne(cascade = CascadeType.REMOVE)
    private OOUniEntB b5rm;

    public OOUniEntA() {

    }

    public OOUniEntB getB1() {
        return b1;
    }

    public void setB1(OOUniEntB b1) {
        this.b1 = b1;
    }

    public OOUniEntB getB2() {
        return b2;
    }

    public void setB2(OOUniEntB b2) {
        this.b2 = b2;
    }

    public OOUniEntB getB4() {
        return b4;
    }

    public void setB4(OOUniEntB b4) {
        this.b4 = b4;
    }

    public OOUniEntB getB5ca() {
        return b5ca;
    }

    public void setB5ca(OOUniEntB b5ca) {
        this.b5ca = b5ca;
    }

    public OOUniEntB getB5cm() {
        return b5cm;
    }

    public void setB5cm(OOUniEntB b5cm) {
        this.b5cm = b5cm;
    }

    public OOUniEntB getB5cp() {
        return b5cp;
    }

    public void setB5cp(OOUniEntB b5cp) {
        this.b5cp = b5cp;
    }

    public OOUniEntB getB5rf() {
        return b5rf;
    }

    public void setB5rf(OOUniEntB b5rf) {
        this.b5rf = b5rf;
    }

    public OOUniEntB getB5rm() {
        return b5rm;
    }

    public void setB5rm(OOUniEntB b5rm) {
        this.b5rm = b5rm;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public IEntityB getB1Field() {
        return getB1();
    }

    @Override
    public void setB1Field(IEntityB b1) {
        setB1((OOUniEntB) b1);
    }

    @Override
    public IEntityB getB2Field() {
        return getB2();
    }

    @Override
    public void setB2Field(IEntityB b2) {
        setB2((OOUniEntB) b2);
    }

    @Override
    public IEntityB getB4Field() {
        return getB4();
    }

    @Override
    public void setB4Field(IEntityB b4) {
        setB4((OOUniEntB) b4);
    }

    @Override
    public IEntityB getB5caField() {
        return getB5ca();
    }

    @Override
    public void setB5caField(IEntityB b5ca) {
        setB5ca((OOUniEntB) b5ca);
    }

    @Override
    public IEntityB getB5cmField() {
        return getB5cm();
    }

    @Override
    public void setB5cmField(IEntityB b5cm) {
        setB5cm((OOUniEntB) b5cm);
    }

    @Override
    public IEntityB getB5cpField() {
        return getB5cp();
    }

    @Override
    public void setB5cpField(IEntityB b5cp) {
        setB5cp((OOUniEntB) b5cp);
    }

    @Override
    public IEntityB getB5rfField() {
        return getB5rf();
    }

    @Override
    public void setB5rfField(IEntityB b5rf) {
        setB5rf((OOUniEntB) b5rf);
    }

    @Override
    public IEntityB getB5rmField() {
        return getB5rm();
    }

    @Override
    public void setB5rmField(IEntityB b5rm) {
        setB5rm((OOUniEntB) b5rm);
    }

    @Override
    public String toString() {
        return "OOUniEntA [id=" + id + ", name=" + name + "]";
    }

}
