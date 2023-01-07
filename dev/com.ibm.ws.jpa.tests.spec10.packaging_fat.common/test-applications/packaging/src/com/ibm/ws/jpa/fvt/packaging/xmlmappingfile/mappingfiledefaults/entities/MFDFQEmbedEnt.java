/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities;

public class MFDFQEmbedEnt implements MappingFileEntity {
    private int id;
    private String name;

    private MFDFQEmbeddable embeddable;

    public MFDFQEmbedEnt() {
        embeddable = new MFDFQEmbeddable();
    }

    @Override
    public String getCity() {
        return embeddable.getCity();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getState() {
        return embeddable.getState();
    }

    @Override
    public String getStreet() {
        return embeddable.getStreet();
    }

    @Override
    public String getZip() {
        return embeddable.getZip();
    }

    @Override
    public void setCity(String city) {
        embeddable.setCity(city);
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setState(String state) {
        embeddable.setState(state);
    }

    @Override
    public void setStreet(String street) {
        embeddable.setStreet(street);
    }

    @Override
    public void setZip(String zip) {
        embeddable.setZip(zip);
    }

    public MFDFQEmbeddable getEmbeddable() {
        return embeddable;
    }

    public void setEmbeddable(MFDFQEmbeddable embeddable) {
        this.embeddable = embeddable;
    }

    @Override
    public String toString() {
        return "MFDFQEmbedEnt [id=" + id + ", name=" + name + ", embeddable=" + embeddable + "]";
    }

}
