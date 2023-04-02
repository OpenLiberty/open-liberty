/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities;

public interface ICompoundPKManyXManyEntityB {
    public int getIDField();

    public void setIdField(int id);

    public String getCountryField();

    public void setCountryField(String country);

    public String getName();

    public void setName(String name);

    public int getSalary();

    public void setSalary(int salary);
}
