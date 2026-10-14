/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.models;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.EntityListener;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;

/**
 * JPA 4.0 Entity Listener demonstrating:
 * 1. @EntityListener annotation
 * 2. Multiple callbacks of the same type (@PrePersist) with different parameter types
 *    as specified in JPA 4.0 Section 3.10.1
 */
@EntityListener
public class AuditListener {

    public static final AtomicInteger employeePrePersistCount = new AtomicInteger(0);
    public static final AtomicInteger productPrePersistCount = new AtomicInteger(0);
    public static final AtomicInteger postPersistCount = new AtomicInteger(0);

    public static void reset() {
        employeePrePersistCount.set(0);
        productPrePersistCount.set(0);
        postPersistCount.set(0);
    }

    /** @PrePersist callback specifically for SimpleEmployee */
    @PrePersist
    public void onPrePersistEmployee(SimpleEmployee emp) {
        employeePrePersistCount.incrementAndGet();
    }

    /** @PrePersist callback specifically for Product (same callback type, different parameter type) */
    @PrePersist
    public void onPrePersistProduct(Product product) {
        productPrePersistCount.incrementAndGet();
    }

    /** @PostPersist generic callback */
    @PostPersist
    public void onPostPersist(Object entity) {
        postPersistCount.incrementAndGet();
    }
}
