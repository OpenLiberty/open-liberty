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

package com.ibm.ws.jpa.fvt.entity.entities.multitable.xml;

import com.ibm.ws.jpa.fvt.entity.entities.IMultiTableEntity;

/**
 * Mapped Superclass defining fields located on a Secondary Table. The entity inheriting from this mapped superclass
 * will override the table locations of some of the fields to demonstrate
 *
 * @AttributeOverride functionality with @SecondaryTables.
 *
 * @author Jody Grassel (jgrassel@us.ibm.com)
 *
 */
// @MappedSuperclass
// @SecondaryTable(name="SEC_TABLE2MSC", pkJoinColumns=@PrimaryKeyJoinColumn(name="id"))
public abstract class XMLMSCMultiTable implements IMultiTableEntity {

    // Fields on the secondary database
    // @Column(table="SEC_TABLE2MSC")
    private String street;

    // @Column(table="SEC_TABLE2MSC")
    private String city;

    // @Column(table="SEC_TABLE2MSC")
    private String state;

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
