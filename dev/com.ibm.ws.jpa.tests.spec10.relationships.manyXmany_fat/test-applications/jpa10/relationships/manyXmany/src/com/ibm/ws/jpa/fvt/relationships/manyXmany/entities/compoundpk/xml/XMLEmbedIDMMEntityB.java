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
package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml;

import javax.persistence.Transient;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.ICompoundPKManyXManyEntityB;

public class XMLEmbedIDMMEntityB implements ICompoundPKManyXManyEntityB {
    private XMLEmbeddableID id;

    private String name;

    private int salary;

    public XMLEmbedIDMMEntityB() {
        id = new XMLEmbeddableID();
    }

    public XMLEmbedIDMMEntityB(int id, String country) {
        this.id = new XMLEmbeddableID(id, country);
    }

    public XMLEmbeddableID getId() {
        return id;
    }

    public void setId(XMLEmbeddableID id) {
        this.id = id;
    }

    @Override
    @Transient
    public int getIDField() {
        return getId().getId();
    }

    @Override
    @Transient
    public void setIdField(int id) {
        getId().setId(id);
    }

    @Override
    @Transient
    public String getCountryField() {
        return getId().getCountry();
    }

    @Override
    @Transient
    public void setCountryField(String country) {
        getId().setCountry(country);
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
        return "XMLEmbedIDMMEntityB [id=" + id + ", name=" + name + ", salary=" + salary + "]";
    }

}