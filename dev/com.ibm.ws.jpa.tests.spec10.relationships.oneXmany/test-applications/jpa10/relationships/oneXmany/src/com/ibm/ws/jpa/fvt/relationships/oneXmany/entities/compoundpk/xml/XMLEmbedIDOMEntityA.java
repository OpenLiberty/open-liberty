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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.ICompoundPKOneXManyEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.ICompoundPKOneXManyEntityB;

public class XMLEmbedIDOMEntityA implements ICompoundPKOneXManyEntityA {
    private int id;

    private String userName;

    private String password;

    private Collection<XMLEmbedIDOMEntityB> identity;

    public XMLEmbedIDOMEntityA() {
        id = 0;
        userName = "";
        password = "";
        identity = new ArrayList<XMLEmbedIDOMEntityB>();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Collection<XMLEmbedIDOMEntityB> getIdentity() {
        return identity;
    }

    public void setIdentity(Collection<XMLEmbedIDOMEntityB> identity) {
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
        XMLEmbedIDOMEntityB entity = (XMLEmbedIDOMEntityB) identity;

        Collection<XMLEmbedIDOMEntityB> identityCollection = getIdentity();
        identityCollection.add(entity);
    }

    @Override
    public boolean isMemberOfIdentityField(ICompoundPKOneXManyEntityB identity) {
        XMLEmbedIDOMEntityB entity = (XMLEmbedIDOMEntityB) identity;

        Collection<XMLEmbedIDOMEntityB> collection = getIdentity();

        return (collection.contains(entity));
    }

    @Override
    public void removeIdentityField(ICompoundPKOneXManyEntityB identity) {
        XMLEmbedIDOMEntityB entity = (XMLEmbedIDOMEntityB) identity;
        Collection<XMLEmbedIDOMEntityB> identityCollection = getIdentity();
        identityCollection.remove(entity);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setIdentityCollectionField(Collection identity) {
        Collection<XMLEmbedIDOMEntityB> identityCollection = new ArrayList<XMLEmbedIDOMEntityB>();

        Iterator i = identityCollection.iterator();
        while (i.hasNext()) {
            XMLEmbedIDOMEntityB entity = (XMLEmbedIDOMEntityB) i.next();
            identityCollection.add(entity);
        }

        setIdentity(identityCollection);
    }

    @Override
    public String toString() {
        return "XMLEmbedIDOMEntityA [id=" + id + ", userName=" + userName + ", password=" + password + "]";
    }
}
