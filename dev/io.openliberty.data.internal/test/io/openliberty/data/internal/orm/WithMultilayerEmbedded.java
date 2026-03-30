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

/**
 * An entity with a multi layer embed
 */
public class WithMultilayerEmbedded {
    public int id;

    public Coordinate center;
    public Side side;

    public static record Coordinate(int x, int y) {
    }

    public static record Side(Coordinate a, Coordinate b) {
    }
}
