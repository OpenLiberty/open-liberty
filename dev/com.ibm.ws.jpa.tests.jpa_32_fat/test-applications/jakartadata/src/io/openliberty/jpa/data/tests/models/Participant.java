
/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "WLPParticipant")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    private Name name;

    // Static factory method for creating Participant instances
    public static Participant of(String firstName, String lastName, int id) {
        Participant p = new Participant();
        p.id = id;
        p.name = new Name(firstName, lastName);
        return p;
    }

    // Getters
    public Integer getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    // Name record with column mapping
    @Embeddable
    public static record Name(
                    @Column(name = "\"FIRST\"") String first,
                    @Column(name = "\"LAST\"") String last) {
        public String getFirst() {
            return first;
        }

        public String getLast() {
            return last;
        }
    }

}
