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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.ICompoundPKManyXManyEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.ICompoundPKManyXManyEntityB;

@Entity
public class IDClassMMEntityA implements ICompoundPKManyXManyEntityA {
    @Id
    private int id;

    private String userName;

    private String password;

    @ManyToMany
    @JoinTable(name = "IDClassMMEntA_IDClassMMEntB", inverseJoinColumns = {
                                                                            @JoinColumn(name = "identity_id", referencedColumnName = "id"),
                                                                            @JoinColumn(name = "identity_country", referencedColumnName = "country")
    })
    private Collection<IDClassMMEntityB> identity;

    public IDClassMMEntityA() {
        id = 0;
        userName = "";
        password = "";
        identity = new ArrayList<IDClassMMEntityB>();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Collection<IDClassMMEntityB> getIdentity() {
        return identity;
    }

    public void setIdentity(Collection<IDClassMMEntityB> identity) {
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
    public Collection getIdentityCollectionField() {
        return getIdentity();
    }

    @Override
    public void insertIdentityField(ICompoundPKManyXManyEntityB identity) {
        IDClassMMEntityB entity = (IDClassMMEntityB) identity;

        Collection<IDClassMMEntityB> identityCollection = getIdentity();
        identityCollection.add(entity);
    }

    @Override
    public boolean isMemberOfIdentityField(ICompoundPKManyXManyEntityB identity) {
        IDClassMMEntityB entity = (IDClassMMEntityB) identity;

        Collection<IDClassMMEntityB> collection = getIdentity();

        return (collection.contains(entity));
    }

    @Override
    public void removeIdentityField(ICompoundPKManyXManyEntityB identity) {
        IDClassMMEntityB entity = (IDClassMMEntityB) identity;
        Collection<IDClassMMEntityB> identityCollection = getIdentity();
        identityCollection.remove(entity);
    }

    @Override
    public void setIdentityCollectionField(Collection identity) {
        Collection<IDClassMMEntityB> identityCollection = new ArrayList<IDClassMMEntityB>();

        Iterator i = identityCollection.iterator();
        while (i.hasNext()) {
            IDClassMMEntityB entity = (IDClassMMEntityB) i.next();
            identityCollection.add(entity);
        }

        setIdentity(identityCollection);
    }

    @Override
    public String toString() {
        return "IDClassMMEntityA [id=" + id + ", userName=" + userName + ", password=" + password + ", identity="
               + identity + "]";
    }
}