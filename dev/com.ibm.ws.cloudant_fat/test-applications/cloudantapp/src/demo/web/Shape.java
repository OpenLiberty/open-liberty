/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package demo.web;

public class Shape {
    public String _id;
    public int sides;
    public float angle;

    public Shape() {
    }

    public Shape(String id, int sides, float angle) {
        this._id = id;
        this.sides = sides;
        this.angle = angle;
    }
}