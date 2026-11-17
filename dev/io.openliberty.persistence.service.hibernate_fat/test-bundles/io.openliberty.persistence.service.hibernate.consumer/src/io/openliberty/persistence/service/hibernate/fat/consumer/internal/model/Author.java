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
package io.openliberty.persistence.service.hibernate.fat.consumer.internal.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Target of a lazy many-to-one association from {@link Book}. Hibernate stands in
 * for it with a runtime generated proxy subclass until it is first used.
 */
@Entity
public class Author {

    @Id
    private long id;

    private String name;

    public Author() {}

    public Author(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() { return id; }

    public String getName() { return name; }
}
