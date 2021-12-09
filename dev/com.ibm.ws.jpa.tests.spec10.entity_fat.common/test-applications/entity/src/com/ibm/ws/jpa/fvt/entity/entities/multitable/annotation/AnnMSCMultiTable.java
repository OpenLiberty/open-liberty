/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.entities.multitable.annotation;

import javax.persistence.MappedSuperclass;

import com.ibm.ws.jpa.fvt.entity.entities.IMultiTableEntity;

/**
 * Mapped Superclass defining fields which will be overriden to a secondary field by the inheriting entity.
 *
 * @author Jody Grassel (jgrassel@us.ibm.com)
 *
 */
@MappedSuperclass
// @SecondaryTable(name="SEC_TABLE2MSC", pkJoinColumns=@PrimaryKeyJoinColumn(name="id"))
public abstract class AnnMSCMultiTable implements IMultiTableEntity {

    // Fields on the secondary database
    // @Column(table="SEC_TABLE2MSC")
    private String street;

    // @Column(table="SEC_TABLE2MSC")
    private String city;

    // @Column(table="SEC_TABLE2MSC")
    private String state;

    // @Column(table="SEC_TABLE2MSC")
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

}
