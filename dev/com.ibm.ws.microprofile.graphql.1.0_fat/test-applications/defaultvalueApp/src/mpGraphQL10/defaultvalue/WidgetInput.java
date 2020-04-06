/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.defaultvalue;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input("WidgetInput")
public class WidgetInput {

    @DefaultValue("Crockpot")
    private String name;
    @DefaultValue("5")
    private int quantity;
    private double weight;
    private double length;
    private double height;
    @DefaultValue("10.0")
    private double depth;

    public WidgetInput() {}

    public WidgetInput(String name, int quantity, double weight, double length, double height, double depth) {
        this.name = name;
        this.quantity = quantity;
        this.weight = weight;
        this.length = length;
        this.height = height;
        this.depth = depth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @DefaultValue("20.4")
    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getHeight() {
        return height;
    }

    @DefaultValue("30.1")
    public void setHeight(double height) {
        this.height = height;
    }

    public double getDepth() {
        return depth;
    }

    public void setDepth(double depth) {
        this.depth = depth;
    }

    public String toString() {
        return "Widget(" + name + ", " + quantity + ", " + weight + ", " +
               length + ", " + height + ", " + depth + ")";
    }
}
