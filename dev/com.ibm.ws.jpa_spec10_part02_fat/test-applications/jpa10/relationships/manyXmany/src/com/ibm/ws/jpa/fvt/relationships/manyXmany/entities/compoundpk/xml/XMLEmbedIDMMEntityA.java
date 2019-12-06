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
package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.ICompoundPKManyXManyEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.ICompoundPKManyXManyEntityB;

public class XMLEmbedIDMMEntityA implements ICompoundPKManyXManyEntityA {
    private int id;

    private String userName;

    private String password;

    private Collection<XMLEmbedIDMMEntityB> identity;

    public XMLEmbedIDMMEntityA() {
        id = 0;
        userName = "";
        password = "";
        identity = new ArrayList<XMLEmbedIDMMEntityB>();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Collection<XMLEmbedIDMMEntityB> getIdentity() {
        return identity;
    }

    public void setIdentity(Collection<XMLEmbedIDMMEntityB> identity) {
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
        XMLEmbedIDMMEntityB entity = (XMLEmbedIDMMEntityB) identity;

        Collection<XMLEmbedIDMMEntityB> identityCollection = getIdentity();
        identityCollection.add(entity);
    }

    @Override
    public boolean isMemberOfIdentityField(ICompoundPKManyXManyEntityB identity) {
        XMLEmbedIDMMEntityB entity = (XMLEmbedIDMMEntityB) identity;

        Collection<XMLEmbedIDMMEntityB> collection = getIdentity();

        return (collection.contains(entity));
    }

    @Override
    public void removeIdentityField(ICompoundPKManyXManyEntityB identity) {
        XMLEmbedIDMMEntityB entity = (XMLEmbedIDMMEntityB) identity;
        Collection<XMLEmbedIDMMEntityB> identityCollection = getIdentity();
        identityCollection.remove(entity);
    }

    @Override
    public void setIdentityCollectionField(Collection identity) {
        Collection<XMLEmbedIDMMEntityB> identityCollection = new ArrayList<XMLEmbedIDMMEntityB>();

        Iterator i = identityCollection.iterator();
        while (i.hasNext()) {
            XMLEmbedIDMMEntityB entity = (XMLEmbedIDMMEntityB) i.next();
            identityCollection.add(entity);
        }

        setIdentity(identityCollection);
    }

    @Override
    public String toString() {
        return "XMLEmbedIDMMEntityA [id=" + id + ", userName=" + userName + ", password=" + password + ", identity="
               + identity + "]";
    }

}