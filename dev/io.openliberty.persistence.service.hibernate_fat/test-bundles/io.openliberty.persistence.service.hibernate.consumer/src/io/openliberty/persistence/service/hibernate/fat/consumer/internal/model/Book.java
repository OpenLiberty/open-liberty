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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Entity with a lazy many-to-one association, like Grant in the Jakarta Data tests.
 * Loading a Book must not load its Author until the Author is used.
 */
@Entity
public class Book {

    @Id
    private long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    private Author author;

    public Book() {}

    public Book(long id, String title, Author author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public long getId() { return id; }

    public String getTitle() { return title; }

    public Author getAuthor() { return author; }
}
