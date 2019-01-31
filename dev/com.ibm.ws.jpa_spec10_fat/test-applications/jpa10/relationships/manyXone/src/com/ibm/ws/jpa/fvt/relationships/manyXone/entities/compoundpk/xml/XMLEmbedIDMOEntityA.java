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

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.xml;

import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.ICompoundPKManyXOneEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.ICompoundPKManyXOneEntityB;

public class XMLEmbedIDMOEntityA implements ICompoundPKManyXOneEntityA {
    private int id;
    private String userName;
    private String password;

    XMLEmbedIDMOEntityB identity;

    public XMLEmbedIDMOEntityA() {
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

    public XMLEmbedIDMOEntityB getIdentity() {
        return identity;
    }

    public void setIdentity(XMLEmbedIDMOEntityB identity) {
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
        setIdentity((XMLEmbedIDMOEntityB) identity);
    }

    @Override
    public String toString() {
        return "XMLEmbedIDMOEntityA [id=" + id + ", userName=" + userName + ", password=" + password + ", identity="
               + identity + "]";
    }
}
