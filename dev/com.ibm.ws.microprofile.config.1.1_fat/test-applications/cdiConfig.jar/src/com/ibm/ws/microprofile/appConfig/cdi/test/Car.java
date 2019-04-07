/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.test;

/**
 *
 */
public class Car<T extends Colour> {

    private final T colour;
    private final String name;

    public Car(String name, T colour) {
        this.name = name;
        this.colour = colour;
    }

    public String getName() {
        return name;
    }

    public T getColour() {
        return this.colour;
    }

    @Override
    public String toString() {
        return "Car: " + getName() + ", Colour: " + getColour().getName();
    }

}
