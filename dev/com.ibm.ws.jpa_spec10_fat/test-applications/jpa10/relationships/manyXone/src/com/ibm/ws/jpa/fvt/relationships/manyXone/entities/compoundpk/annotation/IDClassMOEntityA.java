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

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.ICompoundPKManyXOneEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.ICompoundPKManyXOneEntityB;

@Entity
public class IDClassMOEntityA implements ICompoundPKManyXOneEntityA {
    @Id
    private int id;

    private String userName;

    private String password;

    @ManyToOne
    @JoinColumns({
                   @JoinColumn(name = "identity_id", referencedColumnName = "id"),
                   @JoinColumn(name = "identity_country", referencedColumnName = "country")
    })
    IDClassMOEntityB identity;

    public IDClassMOEntityA() {

    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public IDClassMOEntityB getIdentity() {
        return identity;
    }

    public void setIdentity(IDClassMOEntityB identity) {
        this.identity = identity;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public ICompoundPKManyXOneEntityB getIdentityField() {
        return getIdentity();
    }

    @Override
    public void setIdentityField(ICompoundPKManyXOneEntityB identity) {
        setIdentity((IDClassMOEntityB) identity);
    }

    @Override
    public String toString() {
        return "IDClassMOEntityA [id=" + id + ", userName=" + userName + ", password=" + password + ", identity="
               + identity + "]";
    }
}
