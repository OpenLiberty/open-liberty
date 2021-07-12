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

public class XMLIDClassOMEntityA implements ICompoundPKOneXManyEntityA {
    private int id;

    private String userName;

    private String password;

    private Collection<XMLIDClassOMEntityB> identity;

    public XMLIDClassOMEntityA() {
        id = 0;
        userName = "";
        password = "";
        identity = new ArrayList<XMLIDClassOMEntityB>();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Collection<XMLIDClassOMEntityB> getIdentity() {
        return identity;
    }

    public void setIdentity(Collection<XMLIDClassOMEntityB> identity) {
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
        XMLIDClassOMEntityB entity = (XMLIDClassOMEntityB) identity;

        Collection<XMLIDClassOMEntityB> identityCollection = getIdentity();
        identityCollection.add(entity);
    }

    @Override
    public boolean isMemberOfIdentityField(ICompoundPKOneXManyEntityB identity) {
        XMLIDClassOMEntityB entity = (XMLIDClassOMEntityB) identity;

        Collection<XMLIDClassOMEntityB> collection = getIdentity();

        return (collection.contains(entity));
    }

    @Override
    public void removeIdentityField(ICompoundPKOneXManyEntityB identity) {
        XMLIDClassOMEntityB entity = (XMLIDClassOMEntityB) identity;
        Collection<XMLIDClassOMEntityB> identityCollection = getIdentity();
        identityCollection.remove(entity);
    }

    @Override
    public void setIdentityCollectionField(Collection identity) {
        Collection<XMLIDClassOMEntityB> identityCollection = new ArrayList<XMLIDClassOMEntityB>();

        Iterator i = identityCollection.iterator();
        while (i.hasNext()) {
            XMLIDClassOMEntityB entity = (XMLIDClassOMEntityB) i.next();
            identityCollection.add(entity);
        }

        setIdentity(identityCollection);
    }

    @Override
    public String toString() {
        return "XMLIDClassOMEntityA [id=" + id + ", userName=" + userName + ", password=" + password + "]";
    }
}