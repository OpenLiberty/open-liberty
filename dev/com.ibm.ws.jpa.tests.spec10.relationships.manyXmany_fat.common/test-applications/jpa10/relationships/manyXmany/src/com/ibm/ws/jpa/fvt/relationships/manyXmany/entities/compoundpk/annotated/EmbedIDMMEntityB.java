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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.ICompoundPKManyXManyEntityB;

@Entity
@Table(name = "EmbIDManyXManyEntB")
public class EmbedIDMMEntityB implements ICompoundPKManyXManyEntityB {
    @EmbeddedId
    private EmbeddableID id;

    private String name;

    private int salary;

    public EmbedIDMMEntityB() {
        id = new EmbeddableID();
    }

    public int getId() {
        return id.getId();
    }

    public void setId(int id) {
        this.id.setId(id);
    }

    public String getCountry() {
        return id.getCountry();
    }

    public void setCountry(String country) {
        this.id.setCountry(country);
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
        return "EmbedIDMMEntityB [id=" + id + ", name=" + name + ", salary=" + salary + "]";
    }
}
