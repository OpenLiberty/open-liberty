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

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities;

public interface ICompoundPKManyXOneEntityA {
    public int getId();

    public void setId(int id);

    public String getPassword();

    public void setPassword(String password);

    public String getUserName();

    public void setUserName(String userName);

    public ICompoundPKManyXOneEntityB getIdentityField();

    public void setIdentityField(ICompoundPKManyXOneEntityB identity);
}
