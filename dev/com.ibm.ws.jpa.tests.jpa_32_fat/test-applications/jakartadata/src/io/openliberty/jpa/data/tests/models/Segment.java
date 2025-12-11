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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Recreate from io.openliberty.data.internal_fat_jpa
 */
@Entity
public class Segment {

    @GeneratedValue
    @Id
    public Long id;

    @Embedded
    @Column(nullable = false)
    public Point pointA;

    @Embedded
    @Column(nullable = false)
    public Point pointB;

    @Embeddable
    public static record Point(int x, int y) {
       
    }

    public static Segment of(int x1, int y1, int x2, int y2) {
        Segment inst = new Segment();
        inst.pointA = new Point(x1, y1);
        inst.pointB = new Point(x2, y2);
        return inst;
    }
}
