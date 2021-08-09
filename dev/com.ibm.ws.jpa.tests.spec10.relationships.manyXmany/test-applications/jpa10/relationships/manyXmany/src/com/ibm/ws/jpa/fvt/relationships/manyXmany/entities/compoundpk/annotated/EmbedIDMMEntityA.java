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
import javax.persistence.Table;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.ICompoundPKManyXManyEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.ICompoundPKManyXManyEntityB;

@Entity
@Table(name = "EmbIDManyXManyEntA")
public class EmbedIDMMEntityA implements ICompoundPKManyXManyEntityA {
    @Id
    private int id;

    private String userName;

    private String password;

    @ManyToMany
    @JoinTable(name = "EmbManyXManyJoinTable", inverseJoinColumns = {
                                                                      @JoinColumn(name = "identity_id", referencedColumnName = "id"),
                                                                      @JoinColumn(name = "identity_country", referencedColumnName = "country")
    })
    private Collection<EmbedIDMMEntityB> identity;

    public EmbedIDMMEntityA() {
        id = 0;
        userName = "";
        password = "";
        identity = new ArrayList<EmbedIDMMEntityB>();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Collection<EmbedIDMMEntityB> getIdentity() {
        return identity;
    }

    public void setIdentity(Collection<EmbedIDMMEntityB> identity) {
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
        EmbedIDMMEntityB entity = (EmbedIDMMEntityB) identity;

        Collection<EmbedIDMMEntityB> identityCollection = getIdentity();
        identityCollection.add(entity);
    }

    @Override
    public boolean isMemberOfIdentityField(ICompoundPKManyXManyEntityB identity) {
        EmbedIDMMEntityB entity = (EmbedIDMMEntityB) identity;

        Collection<EmbedIDMMEntityB> collection = getIdentity();

        return (collection.contains(entity));
    }

    @Override
    public void removeIdentityField(ICompoundPKManyXManyEntityB identity) {
        EmbedIDMMEntityB entity = (EmbedIDMMEntityB) identity;
        Collection<EmbedIDMMEntityB> identityCollection = getIdentity();
        identityCollection.remove(entity);
    }

    @Override
    public void setIdentityCollectionField(Collection identity) {
        Collection<EmbedIDMMEntityB> identityCollection = new ArrayList<EmbedIDMMEntityB>();

        Iterator i = identityCollection.iterator();
        while (i.hasNext()) {
            EmbedIDMMEntityB entity = (EmbedIDMMEntityB) i.next();
            identityCollection.add(entity);
        }

        setIdentity(identityCollection);
    }

    @Override
    public String toString() {
        return "EmbedIDMMEntityA [id=" + id + ", userName=" + userName + ", password=" + password + ", identity="
               + identity + "]";
    }
}