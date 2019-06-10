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

/**
 * This is an implementation class of the interface entity, Widget.
 */
public class Widget {
    
    @DefaultValue("SHOULD BE IGNORED")
    private String name;
    private int quantity = -1;
    private double weight = -1.0;
    private double length;
    private double height;
    private double depth;


    static Widget fromWidgetInput(WidgetInput input) {
        return new Widget(input.getName(),
                          input.getQuantity(),
                          input.getWeight(),
                          input.getLength(),
                          input.getHeight(),
                          input.getDepth());
    }

    static Widget fromString(String s) {
        if (!s.startsWith("Widget(") || !s.endsWith(")")) {
            throw new IllegalArgumentException();
        }
        s = s.substring("Widget(".length(), s.length()-1);
        String[] fields = s.split(",");
        Widget w = new Widget();
        w.setName(fields[0]);
        w.setQuantity(Integer.parseInt(fields[1]));
        w.setWeight(Double.parseDouble(fields[2]));
        w.setLength(Double.parseDouble(fields[3]));
        w.setHeight(Double.parseDouble(fields[4]));
        w.setDepth(Double.parseDouble(fields[5]));
        return w;
    }

    public Widget() {}

    public Widget(String name, int quantity, double weight, double length, double height, double depth) {
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

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getHeight() {
        return height;
    }

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
