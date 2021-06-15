/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.ICompoundPKManyXManyEntityB;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.CompoundPK;

/**
 * Entity on the inverse side of a One to Many Relationship. This entity has a compound primary key represented by a
 * IdClass.
 *
 * Annotations are declared on the entity fields.
 *
 */
@Entity
@IdClass(CompoundPK.class)
public class IDClassMMEntityB implements ICompoundPKManyXManyEntityB {
    @Id
    private int id;

    @Id
    private String country;

    private String name;

    int salary;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public int getIDField() {
        return getId();
    }

    @Override
    public void setIdField(int id) {
        setId(id);
    }

    @Override
    public String getCountryField() {
        return getCountry();
    }

    @Override
    public void setCountryField(String country) {
        setCountry(country);
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
    public int getSalary() {
        return salary;
    }

    @Override
    public void setSalary(int salary) {
        this.salary = salary;
    }

    @Override
    public String toString() {
        return "IDClassMMEntityB [id=" + id + ", country=" + country + ", name=" + name + ", salary=" + salary + "]";
    }
}