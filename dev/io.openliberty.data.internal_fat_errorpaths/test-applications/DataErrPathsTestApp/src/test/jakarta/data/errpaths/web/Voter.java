/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * A valid entity.
 */
@Entity
public class Voter {

    @Column(nullable = false)
    public String name;

    public String address;

    @Column(nullable = false)
    public LocalDate birthday;

    public String description;

    @ElementCollection(fetch = FetchType.EAGER)
    public Set<String> emailAddresses = new HashSet<>();

    @Id
    @Column(nullable = false)
    public int ssn;

    public Voter() {
    }

    public Voter(int ssn,
                 String name,
                 LocalDate birthday,
                 String address,
                 String... emailAddresses) {
        this.ssn = ssn;
        this.name = name;
        this.birthday = birthday;
        this.address = address;
        this.description = name + " born on " + birthday + " and living at " + address;
        for (String email : emailAddresses)
            this.emailAddresses.add(email);
    }

    @Override
    public int hashCode() {
        return ssn;
    }

    @Override
    public String toString() {
        return "Voter#" + ssn + " " + birthday + " " + name +
               " @" + address + " " + emailAddresses;
    }
}
