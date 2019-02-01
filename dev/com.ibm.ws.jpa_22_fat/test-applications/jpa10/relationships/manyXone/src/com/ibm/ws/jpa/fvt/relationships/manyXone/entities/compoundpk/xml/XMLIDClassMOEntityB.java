/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.xml;

import javax.persistence.Transient;

import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.ICompoundPKManyXOneEntityB;

public class XMLIDClassMOEntityB implements ICompoundPKManyXOneEntityB {
    private int id;
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
    @Transient
    public int getIdField() {
        return getId();
    }

    @Override
    @Transient
    public void setIdField(int id) {
        setId(id);
    }

    @Override
    @Transient
    public String getCountryField() {
        return getCountry();
    }

    @Override
    @Transient
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
        return "XMLIDClassMOEntityB [id=" + id + ", country=" + country + ", name=" + name + ", salary=" + salary + "]";
    }
}
