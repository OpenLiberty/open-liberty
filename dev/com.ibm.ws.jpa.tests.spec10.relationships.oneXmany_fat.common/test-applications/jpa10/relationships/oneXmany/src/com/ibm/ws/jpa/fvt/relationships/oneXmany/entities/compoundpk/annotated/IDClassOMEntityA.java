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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.ICompoundPKOneXManyEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.ICompoundPKOneXManyEntityB;

@Entity
public class IDClassOMEntityA implements ICompoundPKOneXManyEntityA {
    @Id
    private int id;

    private String userName;

    private String password;

    @OneToMany
    @JoinTable(name = "RELOM_IDCOMENTA",
               joinColumns = @JoinColumn(name = "ENT_A_ID", referencedColumnName = "id"),
               inverseJoinColumns = { @JoinColumn(name = "ENT_B_ID", referencedColumnName = "id"),
                                      @JoinColumn(name = "ENT_B_CTRY", referencedColumnName = "country") })
    private Collection<IDClassOMEntityB> identity;

    public IDClassOMEntityA() {
        id = 0;
        userName = "";
        password = "";
        identity = new ArrayList<IDClassOMEntityB>();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Collection<IDClassOMEntityB> getIdentity() {
        return identity;
    }

    public void setIdentity(Collection<IDClassOMEntityB> identity) {
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
    public void insertIdentityField(ICompoundPKOneXManyEntityB identity) {
        IDClassOMEntityB entity = (IDClassOMEntityB) identity;

        Collection<IDClassOMEntityB> identityCollection = getIdentity();
        identityCollection.add(entity);
    }

    @Override
    public boolean isMemberOfIdentityField(ICompoundPKOneXManyEntityB identity) {
        IDClassOMEntityB entity = (IDClassOMEntityB) identity;

        Collection<IDClassOMEntityB> collection = getIdentity();

        return (collection.contains(entity));
    }

    @Override
    public void removeIdentityField(ICompoundPKOneXManyEntityB identity) {
        IDClassOMEntityB entity = (IDClassOMEntityB) identity;
        Collection<IDClassOMEntityB> identityCollection = getIdentity();
        identityCollection.remove(entity);
    }

    @Override
    public void setIdentityCollectionField(Collection identity) {
        Collection<IDClassOMEntityB> identityCollection = new ArrayList<IDClassOMEntityB>();

        Iterator i = identityCollection.iterator();
        while (i.hasNext()) {
            IDClassOMEntityB entity = (IDClassOMEntityB) i.next();
            identityCollection.add(entity);
        }

        setIdentity(identityCollection);
    }

    @Override
    public String toString() {
        return "IDClassOMEntityA [id=" + id + ", userName=" + userName + ", password=" + password + "]";
    }
}