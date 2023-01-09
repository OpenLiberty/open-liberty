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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

import com.ibm.ws.jpa.fvt.entity.entities.IMultiTableEntity;

/**
 * Entity to demonstrate a simple use-case of @SecondaryTable, where most fields are stored in a table different from
 * the entity's table
 *
 * @author Joe Grassel (jgrassel@us.ibm.com)
 *
 */
@Entity
@SecondaryTable(name = "SEC_TABLE1", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id"))
public class AnnMultiTableEnt implements IMultiTableEntity {
    @Id
    private int id;

    private String name;

    // Fields on the secondary database
    @Column(table = "SEC_TABLE1")
    private String street;

    @Column(table = "SEC_TABLE1")
    private String city;

    @Column(table = "SEC_TABLE1")
    private String state;

    @Column(table = "SEC_TABLE1")
    private String zip;

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public void setCity(String city) {
        this.city = city;
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
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getStreet() {
        return street;
    }

    @Override
    public void setStreet(String street) {
        this.street = street;
    }

    @Override
    public String getZip() {
        return zip;
    }

    @Override
    public void setZip(String zip) {
        this.zip = zip;
    }

    @Override
    public String toString() {
        return "AnnMultiTableEnt [id=" + id + "]";
    }

}
