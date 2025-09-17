/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
                int ID, // All upper case is a bad practice, but the spec allows it
                Item item,
                int numStars,
                Reviewer reviewer,
                Set<String> comments) {

    public static record Reviewer(
                    Name name,
                    String email) {
        public static record Name(
                        String first,
                        String last) {
        }
    }

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
