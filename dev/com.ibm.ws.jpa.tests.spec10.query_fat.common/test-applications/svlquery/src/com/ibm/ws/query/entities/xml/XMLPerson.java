/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.query.entities.xml;

import java.util.Set;

import com.ibm.ws.query.entities.interfaces.IAddress;
import com.ibm.ws.query.entities.interfaces.IPerson;

public class XMLPerson implements IPerson {
    int id;
    int age;
    String first;
    String last;

    public Set<XMLAddress> residences;

    public XMLPerson() {
    }

    public XMLPerson(int id, int age, String first, String last) {
        super();
        // TODO Auto-generated constructor stub
        this.id = id;
        this.age = age;
        this.first = first;
        this.last = last;
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String getFirst() {
        return first;
    }

    @Override
    public void setFirst(String first) {
        this.first = first;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getLast() {
        return last;
    }

    @Override
    public void setLast(String last) {
        this.last = last;
    }

    @Override
    public Set<? extends IAddress> getResidences() {
        return residences;
    }

    @Override
    public void setResidences(Set<? extends IAddress> residences) {
        this.residences = (Set<XMLAddress>) residences;
    }

}
