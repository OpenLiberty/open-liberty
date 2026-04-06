/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.orm;

import java.util.List;

/**
 * An entity with a multi layer embed with a collection
 */
public class WithEmbeddedMultilayerCollection {
    public int id;

    public Coordinate center;
    private Side side;

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public static record Coordinate(int x, int y) {
    }

    public static record Side(List<Coordinate> cords) {
    }
}
