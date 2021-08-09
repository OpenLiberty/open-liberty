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

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICompoundPKOneXOneEntityB;

@Entity
public class EmbedIDOOEntB implements ICompoundPKOneXOneEntityB {
    @EmbeddedId
    private EmbeddableID id;

    private String name;

    private int salary;

    public EmbedIDOOEntB() {
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
        id.setCountry(country);
    }

    @Override
    public int getIdField() {
        return id.getId();
    }

    @Override
    public void setIdField(int id) {
        this.id.setId(id);
    }

    @Override
    public String getCountryField() {
        return id.getCountry();
    }

    @Override
    public void setCountryField(String country) {
        id.setCountry(country);
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
        return "EmbedIDOOEntB [id=" + id + ", name=" + name + ", salary=" + salary + "]";
    }
}
