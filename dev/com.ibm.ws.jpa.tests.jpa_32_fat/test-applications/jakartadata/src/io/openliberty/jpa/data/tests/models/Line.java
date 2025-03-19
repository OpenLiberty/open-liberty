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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Recreate from io.openliberty.data.internal_fat
 * Instead of using house, use this simpler Entity
 */
@Entity
public class Line {
    @GeneratedValue
    @Id
    public Long id;

    @Embedded
    @AttributeOverrides({
                          @AttributeOverride(name = "x", column = @Column(name = "x_A")),
                          @AttributeOverride(name = "y", column = @Column(name = "y_A"))
    })
    @Column
    public Point pointA;

    @Embedded
    @AttributeOverrides({
                          @AttributeOverride(name = "x", column = @Column(name = "x_B")),
                          @AttributeOverride(name = "y", column = @Column(name = "y_B"))
    })
    @Column

    public Point pointB;

    @Embeddable
    public static class Point {

        public int x;

        public int y;

        public static Point of(int x, int y) {
            Point inst = new Point();
            inst.x = x;
            inst.y = y;
            return inst;
        }

        @Override
        public String toString() {
            return "Point [x=" + x + ", y=" + y + "]";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Point other = (Point) obj;
            return x == other.x && y == other.y;
        }

    }

    public static Line of(int x1, int y1, int x2, int y2) {
        Line inst = new Line();
        inst.pointA = Point.of(x1, y1);
        inst.pointB = Point.of(x2, y2);
        return inst;
    }
}
