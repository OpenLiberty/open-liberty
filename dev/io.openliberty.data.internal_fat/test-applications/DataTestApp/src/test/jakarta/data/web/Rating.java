/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import java.util.Set;

/**
 * Entity that is a Java record with embeddable and collection attributes.
 */
public record Rating(
                int id,
                Item item,
                int numStars,
                Reviewer reviewer,
                Set<String> comments) {

    public static class Reviewer {
        public String firstName;
        public String lastName;
        public String email;

        public Reviewer() {
        }

        public Reviewer(String firstName, String lastName, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }
    }
    // TODO switch the above class to the following record after JPA 3.2 adds support for records as embeddables
    //public static record Reviewer(
    //                String firstName,
    //                String lastName,
    //                String email) {
    //}

    public static class Item {
        public String name;

        public float price;

        public Item() {
        }

        public Item(String name, float price) {
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return "Item: " + name + " $" + price;
        }
    }
}
