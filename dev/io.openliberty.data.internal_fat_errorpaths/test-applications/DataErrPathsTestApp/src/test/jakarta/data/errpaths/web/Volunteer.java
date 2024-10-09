/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.errpaths.web;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * A valid entity. But used in a repository that uses a persistence unit
 * from which this entity is missing.
 */
@Entity
public class Volunteer {

    @Id
    @Column(nullable = false)
    public String name;

    public String address;

    @Column(nullable = false)
    public LocalDate birthday;

    public Volunteer() {
    }

    public Volunteer(String name, LocalDate birthday, String address) {
        this.name = name;
        this.address = address;
        this.birthday = birthday;
    }

    @Override
    public String toString() {
        return "Volunteer " + name + " (" + birthday + ") @" + address;
    }
}
