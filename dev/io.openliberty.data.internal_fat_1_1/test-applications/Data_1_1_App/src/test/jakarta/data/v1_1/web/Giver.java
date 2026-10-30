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
package test.jakarta.data.v1_1.web;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * An entity representing a person or organization that gives a grant.
 * Used to test Fetching annotation and QueryOptions with entityGraph.
 */
@Entity
public class Giver {

    public enum Category {
        FOUNDATION, GOVERNMENT, INDIVIDUAL
    }

    @Column(nullable = false)
    public Category category;

    @Id
    public String name;

    public static Giver of(String name,
                           Category category) {
        Giver g = new Giver();
        g.name = name;
        g.category = category;
        return g;
    }

    /**
     * Textual description. For example:
     *
     * Giver "National Science Foundation" (GOVERNMENT)
     */
    @Override
    public String toString() {
        return new StringBuilder("Giver:")
                        .append(name)
                        .append(" ")
                        .append(category)
                        .toString();
    }
}
