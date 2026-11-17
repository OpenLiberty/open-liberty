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
import jakarta.persistence.Table;

/**
 * An annotated entity, like   Jakarta Data passes to the persistence
 * service as entity class names plus a table name (the
 * io.openliberty.persistence.internal.tableNames property).
 */
@Entity
@Table(name = "DataAnnotated")
public class AnnotatedItem {

    @Id
    private long id;

    private String name;

    public AnnotatedItem() {}

    public AnnotatedItem(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() { return id; }

    public String getName() { return name; }
}
