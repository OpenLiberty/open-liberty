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

@Entity
public class Organization {
    @Id
    public Long id;
    public String name;

    public static Organization of(Long id, String name) {
        Organization inst = new Organization();
        inst.name = name;
        inst.id = id;
        return inst;
    }
}