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

import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * Recreate from io.openliberty.data.internal_fat
 */
@Entity
public class Rating {

    @Id
    public int id;

    @Embedded
    public Item item;

    public int numStars;

    @Embedded
    public Reviewer reviewer;

    @ElementCollection(fetch = FetchType.EAGER)
    public Set<String> comments;

    // Basic constructor for builder method
    public Rating() {
    }

    // Test constructor to show that SELECT NEW works without comments field
    public Rating(int id, Item item, int stars, Reviewer reviewer) {
        this.id = id;
        this.item = item;
        this.numStars = stars;
        this.reviewer = reviewer;
        this.comments = null;
    }

    // The constructor SELECT NEW should be able to use
    public Rating(int id, Item item, int stars, Reviewer reviewer, Set<String> comments) {
        this.id = id;
        this.item = item;
        this.numStars = stars;
        this.reviewer = reviewer;
        this.comments = comments;
    }

    public static Rating of(int id, Item item, int stars, Reviewer reviewer, String... comments) {
        Rating inst = new Rating();
        inst.id = id;
        inst.item = item;
        inst.numStars = stars;
        inst.reviewer = reviewer;
        inst.comments = Set.of(comments);
        return inst;
    }

    @Embeddable
    public static class Reviewer {
        public String firstName;
        public String lastName;
        public String email;

        public static Reviewer of(String firstName, String lastName, String email) {
            Reviewer inst = new Reviewer();
            inst.firstName = firstName;
            inst.lastName = lastName;
            inst.email = email;
            return inst;
        }
    }

    @Embeddable
    public static class Item {
        public String name;
        public float price;

        public static Item of(String name, float price) {
            Item inst = new Item();
            inst.name = name;
            inst.price = price;
            return inst;
        }

        @Override
        public String toString() {
            return "Item: " + name + " $" + price;
        }
    }
}
