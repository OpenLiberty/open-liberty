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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.entities;

import java.util.Collection;

public interface ICompoundPKOneXManyEntityA {
    public int getId();

    public void setId(int id);

    public String getPassword();

    public void setPassword(String password);

    public String getUserName();

    public void setUserName(String userName);

    /*
     * Relationship Fields
     */

    /*
     * Field: identity
     *
     * One to many mapping with an ICompoundPKOneXManyEntityB-type entity.
     *
     * OneToMany Config Cascade: default no Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    public Collection getIdentityCollectionField();

    public void setIdentityCollectionField(Collection identity);

    public void insertIdentityField(ICompoundPKOneXManyEntityB identity);

    public void removeIdentityField(ICompoundPKOneXManyEntityB identity);

    public boolean isMemberOfIdentityField(ICompoundPKOneXManyEntityB identity);
}
