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

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICompoundPKOneXOneEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICompoundPKOneXOneEntityB;

public class XMLEmbedIDOOEntA implements ICompoundPKOneXOneEntityA {
    private int id;
    private String userName;
    private String password;

    XMLEmbedIDOOEntB identity;

    public XMLEmbedIDOOEntA() {
        id = 0;
        userName = "";
        password = "";
        identity = null;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public XMLEmbedIDOOEntB getIdentity() {
        return identity;
    }

    public void setIdentity(XMLEmbedIDOOEntB identity) {
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
    public ICompoundPKOneXOneEntityB getIdentityField() {
        return getIdentity();
    }

    @Override
    public void setIdentityField(ICompoundPKOneXOneEntityB identity) {
        setIdentity((XMLEmbedIDOOEntB) identity);
    }

    @Override
    public String toString() {
        return "XMLEmbedIDOOEntA [id=" + id + ", userName=" + userName + ", password=" + password + "]";
    }
}
