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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.ICompoundPKOneXManyEntityB;

public class XMLEmbedIDOMEntityB implements ICompoundPKOneXManyEntityB {
    private XMLEmbeddableID id;

    private String name;

    private int salary;

    public XMLEmbedIDOMEntityB() {
        id = new XMLEmbeddableID();
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
    public int getIdField() {
        return getId();
    }

    @Override
    public void setIdField(int id) {
        setId(id);
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
        return "XMLEmbedIDOMEntityB [id=" + id + ", name=" + name + ", salary=" + salary + "]";
    }
}
