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

package com.ibm.ws.jpa.fvt.entity.entities.multitable.annotation;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

import com.ibm.ws.jpa.fvt.entity.entities.IMultiTableEntity;

@Entity
@SecondaryTable(name = "SEC_TABLEEMB", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id"))
public class AnnEmbedMultiTableEnt implements IMultiTableEntity {
    @Id
    private int id;

    private String name;

    @Embedded
    @AttributeOverrides({ @AttributeOverride(name = "street", column = @Column(table = "SEC_TABLEEMB")),
                          @AttributeOverride(name = "city", column = @Column(table = "SEC_TABLEEMB")),
                          @AttributeOverride(name = "state", column = @Column(table = "SEC_TABLEEMB")),
                          @AttributeOverride(name = "zip", column = @Column(table = "SEC_TABLEEMB")) })
    private MTEmbeddable embeddedObj;

    public AnnEmbedMultiTableEnt() {
        embeddedObj = new MTEmbeddable();
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

    public MTEmbeddable getEmbeddedObj() {
        return embeddedObj;
    }

    public void setEmbeddedObj(MTEmbeddable embeddedObj) {
        this.embeddedObj = embeddedObj;
    }

    @Override
    public String getCity() {
        return embeddedObj.getCity();
    }

    @Override
    public String getState() {
        return embeddedObj.getState();
    }

    @Override
    public String getStreet() {
        return embeddedObj.getStreet();
    }

    @Override
    public String getZip() {
        return embeddedObj.getZip();
    }

    @Override
    public void setCity(String city) {
        embeddedObj.setCity(city);
    }

    @Override
    public void setState(String state) {
        embeddedObj.setState(state);
    }

    @Override
    public void setStreet(String street) {
        embeddedObj.setStreet(street);
    }

    @Override
    public void setZip(String zip) {
        embeddedObj.setZip(zip);
    }

    @Override
    public String toString() {
        return "AnnEmbedMultiTableEnt [id=" + id + "]";
    }
}
