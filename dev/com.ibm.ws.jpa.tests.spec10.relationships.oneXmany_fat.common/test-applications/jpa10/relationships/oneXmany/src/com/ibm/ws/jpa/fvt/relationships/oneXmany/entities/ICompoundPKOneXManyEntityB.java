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

public interface ICompoundPKOneXManyEntityB {
    public int getIdField();

    public void setIdField(int id);

    public String getCountryField();

    public void setCountryField(String country);

    public String getName();

    public void setName(String name);

    public int getSalary();

    public void setSalary(int salary);
}
