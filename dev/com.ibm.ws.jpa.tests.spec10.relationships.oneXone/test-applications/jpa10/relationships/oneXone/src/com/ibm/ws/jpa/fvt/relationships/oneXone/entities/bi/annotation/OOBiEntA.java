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

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IEntityB;

/**
 * Simple entity to test Bidirectional OneToOne relationships.
 *
 * BiEntityA has a bidirectional relationship with BiEntityB, with A referencing B. BiEntityA is the owning part of the
 * relationship.
 *
 * Annotations are declared on the entity fields.
 *
 */
@Entity
public class OOBiEntA implements IEntityA {
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
     * One to one mapping to an BiEntityB-type entity. Overriding the name of the join column to "BIENT_B1".
     *
     * OneToOne Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "BIENT_B1".
     */
    @OneToOne
    @JoinColumn(name = "BIENT_B1")
    private OOBiEntB_B1 b1;

    /**
     * One to one mapping to an BiEntityB-type entity. No override of the foreign key column name, which should default
     * to "B2_ID" (Name of the field of the referencing entity + " " + the name of the referenced primary key column).
     *
     * OneToOne Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: The foreign key column name is set to
     * "B2_ID".
     */
    @OneToOne
    private OOBiEntB_B2 b2;

    /**
     * One to one mapping to an BiEntityB-type entity. Overriding the name of the join column to "BIENT_B4". FetchType
     * has been set to LAZY.
     *
     * OneToOne Config Cascade: default no Fetch: LAZY Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "BIENT_B1".
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BIENT_B4")
    private OOBiEntB_B4 b4;

    /**
     * One to one mapping to an BiEntityB-type entity. Overriding the name of the join column to "BIENT_B5CA". This
     * relation field has the CascadeType of ALL.
     *
     * OneToOne Config Cascade: ALL Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "BIENT_B1".
     */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "BIENT_B1CA")
    private OOBiEntB_B5CA b5ca;

    /**
     * One to one mapping to an BiEntityB-type entity. Overriding the name of the join column to "BIENT_B5CM". This
     * relation field has the CascadeType of MERGE.
     *
     * OneToOne Config Cascade: MERGE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "BIENT_B1".
     */
    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "BIENT_B1CM")
    private OOBiEntB_B5CM b5cm;

    /**
     * One to one mapping to an BiEntityB-type entity. Overriding the name of the join column to "BIENT_B5CP". This
     * relation field has the CascadeType of PERSIST.
     *
     * OneToOne Config Cascade: PERSIST Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "BIENT_B1".
     */
    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "BIENT_B1CP")
    private OOBiEntB_B5CP b5cp;

    /**
     * One to one mapping to an BiEntityB-type entity. Overriding the name of the join column to "BIENT_B5RF". This
     * relation field has the CascadeType of REFRESH.
     *
     * OneToOne Config Cascade: REFRESH Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "BIENT_B1".
     */
    @OneToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "BIENT_B1RF")
    private OOBiEntB_B5RF b5rf;

    /**
     * One to one mapping to an BiEntityB-type entity. Overriding the name of the join column to "BIENT_B5RM". This
     * relation field has the CascadeType of REMOVE.
     *
     * OneToOne Config Cascade: REMOVE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "BIENT_B1".
     */
    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "BIENT_B1RM")
    private OOBiEntB_B5RM b5rm;

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

    public OOBiEntB_B1 getB1() {
        return b1;
    }

    public void setB1(OOBiEntB_B1 b1) {
        this.b1 = b1;
    }

    public OOBiEntB_B2 getB2() {
        return b2;
    }

    public void setB2(OOBiEntB_B2 b2) {
        this.b2 = b2;
    }

    public OOBiEntB_B4 getB4() {
        return b4;
    }

    public void setB4(OOBiEntB_B4 b4) {
        this.b4 = b4;
    }

    public OOBiEntB_B5CA getB5ca() {
        return b5ca;
    }

    public void setB5ca(OOBiEntB_B5CA b5ca) {
        this.b5ca = b5ca;
    }

    public OOBiEntB_B5CM getB5cm() {
        return b5cm;
    }

    public void setB5cm(OOBiEntB_B5CM b5cm) {
        this.b5cm = b5cm;
    }

    public OOBiEntB_B5CP getB5cp() {
        return b5cp;
    }

    public void setB5cp(OOBiEntB_B5CP b5cp) {
        this.b5cp = b5cp;
    }

    public OOBiEntB_B5RF getB5rf() {
        return b5rf;
    }

    public void setB5rf(OOBiEntB_B5RF b5rf) {
        this.b5rf = b5rf;
    }

    public OOBiEntB_B5RM getB5rm() {
        return b5rm;
    }

    public void setB5rm(OOBiEntB_B5RM b5rm) {
        this.b5rm = b5rm;
    }

    @Override
    public IEntityB getB1Field() {
        return getB1();
    }

    @Override
    public void setB1Field(IEntityB b1) {
        setB1((OOBiEntB_B1) b1);
    }

    @Override
    public IEntityB getB2Field() {
        return getB2();
    }

    @Override
    public void setB2Field(IEntityB b2) {
        setB2((OOBiEntB_B2) b2);
    }

    @Override
    public IEntityB getB4Field() {
        return getB4();
    }

    @Override
    public void setB4Field(IEntityB b4) {
        setB4((OOBiEntB_B4) b4);
    }

    @Override
    public IEntityB getB5caField() {
        return getB5ca();
    }

    @Override
    public void setB5caField(IEntityB b5ca) {
        setB5ca((OOBiEntB_B5CA) b5ca);
    }

    @Override
    public IEntityB getB5cmField() {
        return getB5cm();
    }

    @Override
    public void setB5cmField(IEntityB b5cm) {
        setB5cm((OOBiEntB_B5CM) b5cm);
    }

    @Override
    public IEntityB getB5cpField() {
        return getB5cp();
    }

    @Override
    public void setB5cpField(IEntityB b5cp) {
        setB5cp((OOBiEntB_B5CP) b5cp);
    }

    @Override
    public IEntityB getB5rfField() {
        return getB5rf();
    }

    @Override
    public void setB5rfField(IEntityB b5rf) {
        setB5rf((OOBiEntB_B5RF) b5rf);
    }

    @Override
    public IEntityB getB5rmField() {
        return getB5rm();
    }

    @Override
    public void setB5rmField(IEntityB b5rm) {
        setB5rm((OOBiEntB_B5RM) b5rm);
    }

    @Override
    public String toString() {
        return "OOBiEntA [id=" + id + ", name=" + name + "]";
    }
}
