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

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

/**
 * Root of a SINGLE_TABLE hierarchy, like the entity inheritance that Jakarta Data
 * supports. Jakarta Data supplies a table name for every entity class, including
 * subclasses such as {@link CommercialAd}.
 */
@Entity
@Inheritance
@DiscriminatorColumn(name = "MEDIA")
public abstract class Ad {

    @Id
    private long id;

    private String title;

    protected Ad() {}

    protected Ad(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public long getId() { return id; }

    public String getTitle() { return title; }
}
