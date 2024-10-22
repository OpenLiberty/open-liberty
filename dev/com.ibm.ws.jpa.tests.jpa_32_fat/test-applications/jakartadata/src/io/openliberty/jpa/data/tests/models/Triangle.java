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

import java.util.Arrays;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Recreate from io.openliberty.data.internal_fat_jpa
 */
@Entity
public class Triangle {

    @Basic(optional = false)
    @GeneratedValue
    @Id
    public Integer distinctKey;

    @Basic(optional = true)
    public Byte hypotenuse;

    @Basic(optional = false)
    public short perimeter;

    @Basic(optional = false)
    public Short sameLengthSides; // 0, 2, or 3

    @Basic(optional = false)
    public byte[] sides;

    public static Triangle of(byte side1, byte side2, byte side3) {
        Triangle inst = new Triangle();
        inst.perimeter = (short) (side1 + side2 + side3);
        inst.sides = new byte[] { side1, side2, side3 };
        inst.sameLengthSides = side1 == side2 && side2 == side3 ? (short) 3 //
                        : side1 == side2 || side2 == side3 || side1 == side3 ? (short) 2 //
                                        : 0;
        byte longest = 0;
        byte[] others = new byte[2];
        int i = 0;

        for (byte s : inst.sides)
            if (s > longest) {
                if (longest > 0)
                    others[i++] = longest;
                longest = s;
            } else if (s <= 0) {
                throw new IllegalArgumentException("side " + s);
            } else {
                others[i++] = s;
            }

        inst.hypotenuse = longest * longest == others[0] * others[0] + others[1] * others[1] ? longest : null;

        return inst;
    }

    @Override
    public String toString() {
        return "Triangle#" + distinctKey + " sides " + Arrays.toString(sides) + ", "
               + sameLengthSides + "of same length, perimeter " + perimeter + ", hypotenuse " + hypotenuse;
    }
}
