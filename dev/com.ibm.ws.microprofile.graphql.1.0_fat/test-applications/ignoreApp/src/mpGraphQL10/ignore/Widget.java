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
package mpGraphQL10.ignore;

import java.text.DecimalFormat;

import javax.json.bind.annotation.JsonbTransient;

import org.eclipse.microprofile.graphql.Ignore;
/**
 * This is an implementation class of the interface entity, Widget.
 */
public class Widget {
    
    private String name;
    private int quantity = -1;
    private double weight = -1.0;
    
    @Ignore // ignore from type and input type
    private String dimensions;
    private double length;
    private double height;
    private double depth;

    private int quantity2 = -1;
    private double weight2 = -1.0;
    
    @Ignore // ignore from type and input type
    private String dimensions2;
    private double length2;
    private double height2;
    private double depth2;


    static Widget fromWidgetInput(Widget input) {
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
        w.setDimensions(fields[6]);
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
        DecimalFormat df = new DecimalFormat("#.0");
        this.dimensions = String.format("%sx%sx%s", df.format(length), df.format(height), df.format(depth));
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

    @Ignore // ignored on input type
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Ignore // ignored on type
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

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public String toString() {
        return "Widget(" + name + ", " + quantity + ", " + weight + ", " +
               length + ", " + height + ", " + depth + ", " + dimensions + ")";
    }

    public String getDimensions2() {
        return dimensions2;
    }

    public void setDimensions2(String dimensions2) {
        this.dimensions2 = dimensions2;
    }

    public double getLength2() {
        return length2;
    }

    public void setLength2(double length2) {
        this.length2 = length2;
    }

    public double getHeight2() {
        return height2;
    }

    public void setHeight2(double height2) {
        this.height2 = height2;
    }

    public double getDepth2() {
        return depth2;
    }

    public void setDepth2(double depth2) {
        this.depth2 = depth2;
    }

    public int getQuantity2() {
        return quantity2;
    }

    @JsonbTransient
    public void setQuantity2(int quantity2) {
        this.quantity2 = quantity2;
    }

    @JsonbTransient
    public double getWeight2() {
        return weight2;
    }

    public void setWeight2(double weight2) {
        this.weight2 = weight2;
    }
}
