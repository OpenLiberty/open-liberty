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

package com.ibm.ws.query.entities.ano;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

import com.ibm.ws.query.entities.interfaces.IPersonBean;

@SqlResultSetMapping(name = "PersonBeanSQLMapping",
                     entities = @EntityResult(entityClass = PersonBean.class))
@Entity
@Table(name = "JPAXYZ")
public class PersonBean implements IPersonBean {
    @Id
    int id;
    int age;
    @Column(name = "firstName", length = 20)
    String first;
    @Column(name = "lastName", length = 20)
    String last;

    public PersonBean() {
    }

    public PersonBean(int id, int age, String first, String last) {
        super();
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

}
