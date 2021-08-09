/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jsonp.bundle;

/**
 * Simple Java object for an OSGi service component to marshall/unmarshall to/from JSON via JSON-B.
 */
public class Basket {
    public static enum Direction {
        N, NE, E, SE, S, SW, W, NW
    }

    public static enum Tee {
        CONCRETE, DIRT
    }

    public Tee tee;
    public int par;
    public int length;
    public Direction direction;
    public String terrain;

    public Basket() {}

    public Basket(Tee tee, int par, int length, Direction direction, String terrain) {
        this.tee = tee;
        this.par = par;
        this.length = length;
        this.direction = direction;
        this.terrain = terrain;
    }

    @Override
    public String toString() {
        return tee + " par " + par + ' ' + length + "ft " + direction + ' ' + terrain;
    }
}
