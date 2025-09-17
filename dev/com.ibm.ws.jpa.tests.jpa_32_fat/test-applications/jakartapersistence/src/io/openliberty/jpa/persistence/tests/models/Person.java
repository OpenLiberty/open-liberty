/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.jpa.persistence.tests.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PersistencePerson")
public class Person {
    @Id
    public Long id;
    public String name;

    public static Person of(Long id, String name) {
        Person inst = new Person();
        inst.id = id;
        inst.name = name;
        return inst;
    }
}