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
 * An entity representing a person or organization that receives a grant.
 * Used to test Fetching annotation and QueryOptions with entityGraph.
 */
@Entity
public class Grantee {

    public enum Focus {
        ARTS, EDUCATION, ENVIRONMENT, HEALTH, HOUSING, RESEARCH
    }

    @Column(nullable = false)
    public Focus focus;

    @Id
    public String name;

    public static Grantee of(String name,
                             Focus focus) {
        Grantee g = new Grantee();
        g.name = name;
        g.focus = focus;
        return g;
    }

    /**
     * Textual description. For example:
     *
     * Grantee:Rochester Community College EDUCATION
     */
    @Override
    public String toString() {
        return new StringBuilder("Grantee:")
                        .append(name)
                        .append(' ')
                        .append(focus)
                        .toString();
    }
}
