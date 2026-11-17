/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.persistence.service.hibernate.fat.consumer.internal.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Version
    private int version;

    private String firstName;
    private String lastName;
    private String data;

    public Person() {}

    public Person(String firstName, String lastName, String data) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.data      = data;
    }

    public long   getId()    { return id; }
    public String getFirst() { return firstName; }
    public String getLast()  { return lastName; }
    public String getData()  { return data; }

    public void setFirst(String f) { this.firstName = f; }
    public void setLast(String l)  { this.lastName  = l; }
}
