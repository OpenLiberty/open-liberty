/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.interfaces;

public interface IPersonBean {
    public int getAge();

    public void setAge(int age);

    public String getFirst();

    public void setFirst(String first);

    public int getId();

    public void setId(int id);

    public String getLast();

    public void setLast(String last);
}
