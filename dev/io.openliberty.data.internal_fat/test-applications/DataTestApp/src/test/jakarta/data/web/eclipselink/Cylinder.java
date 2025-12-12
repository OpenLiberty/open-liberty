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
package test.jakarta.data.web.eclipselink;

/**
 * Entity that is a Java record with multiple of the same type of embeddable
 * in its hierarchy.
 */
public record Cylinder(
                String cID,
                Coordinate center,
                Side side) {

    public static record Coordinate(
                    int x,
                    int y) {
    }

    // TODO switch to record once #33662 is fixed
    //public static record Side(
    //                Coordinate a,
    //                Coordinate b) {
    public static class Side {
        public Coordinate a, b;

        public Side() {
        }

        public Side(Coordinate a, Coordinate b) {
            this.a = a;
            this.b = b;
        }

        public Coordinate a() {
            return a;
        }

        public Coordinate b() {
            return b;
        }

        @Override
        public String toString() {
            return "Side {a=" + a + ", b=" + b + "}";
        }

    }

    public Cylinder(String cylinderID,
                    int aX, int aY,
                    int bX, int bY,
                    int centerX, int centerY) {
        this(cylinderID, //
             new Coordinate(centerX, centerY), //
             new Side(new Coordinate(aX, aY), new Coordinate(bX, bY)));
    }
}
