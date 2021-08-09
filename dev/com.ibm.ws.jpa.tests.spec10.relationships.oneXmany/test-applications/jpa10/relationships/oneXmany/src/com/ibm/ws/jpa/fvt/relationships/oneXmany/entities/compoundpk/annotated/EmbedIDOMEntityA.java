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
public class EmbedIDOMEntityA implements ICompoundPKOneXManyEntityA {
    @Id
    private int id;

    private String userName;

    private String password;

    @OneToMany
    @JoinTable(name = "RELOM_EIDOMENTA",
               joinColumns = @JoinColumn(name = "ENT_A_ID", referencedColumnName = "id"),
               inverseJoinColumns = { @JoinColumn(name = "ENT_B_ID", referencedColumnName = "id"),
                                      @JoinColumn(name = "ENT_B_CTRY", referencedColumnName = "country") })
    private Collection<EmbedIDOMEntityB> identity;

    public EmbedIDOMEntityA() {
        id = 0;
        userName = "";
        password = "";
        identity = new ArrayList<EmbedIDOMEntityB>();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Collection<EmbedIDOMEntityB> getIdentity() {
        return identity;
    }

    public void setIdentity(Collection<EmbedIDOMEntityB> identity) {
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
    @SuppressWarnings("rawtypes")
    public Collection getIdentityCollectionField() {
        return getIdentity();
    }

    @Override
    public void insertIdentityField(ICompoundPKOneXManyEntityB identity) {
        EmbedIDOMEntityB entity = (EmbedIDOMEntityB) identity;

        Collection<EmbedIDOMEntityB> identityCollection = getIdentity();
        identityCollection.add(entity);
    }

    @Override
    public boolean isMemberOfIdentityField(ICompoundPKOneXManyEntityB identity) {
        EmbedIDOMEntityB entity = (EmbedIDOMEntityB) identity;

        Collection<EmbedIDOMEntityB> collection = getIdentity();

        return (collection.contains(entity));
    }

    @Override
    public void removeIdentityField(ICompoundPKOneXManyEntityB identity) {
        EmbedIDOMEntityB entity = (EmbedIDOMEntityB) identity;
        Collection<EmbedIDOMEntityB> identityCollection = getIdentity();
        identityCollection.remove(entity);
    }

    @Override
    public void setIdentityCollectionField(Collection identity) {
        Collection<EmbedIDOMEntityB> identityCollection = new ArrayList<EmbedIDOMEntityB>();

        Iterator i = identityCollection.iterator();
        while (i.hasNext()) {
            EmbedIDOMEntityB entity = (EmbedIDOMEntityB) i.next();
            identityCollection.add(entity);
        }

        setIdentity(identityCollection);
    }

    @Override
    public String toString() {
        return "EmbedIDOMEntityA [id=" + id + ", userName=" + userName + ", password=" + password + "]";
    }

}
