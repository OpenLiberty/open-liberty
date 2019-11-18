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
package mpGraphQL10.outputFields;

import java.text.DecimalFormat;

import javax.json.bind.annotation.JsonbProperty;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Query;
/**
 * This is an implementation class of the interface entity, Widget.
 */
public class Widget {
    
    private String name;

    private int quantity = -1;
    private double weight = -1.0;

    @JsonbProperty("qty2")
    private int quantity2 = -1;
    private double weight2 = -1.0;

    private int quantity3 = -1;
    private double weight3 = -1.0;

    static Widget fromWidgetInput(Widget input) {
        return new Widget(input.getName(),
                          input.getQuantity(),
                          input.getWeight(),
                          input.getQuantity2(),
                          input.getWeight2());
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
        w.setQuantity2(Integer.parseInt(fields[3]));
        w.setWeight2(Double.parseDouble(fields[4]));
        return w;
    }

    public Widget() {}

    public Widget(String name, int quantity, double weight, int quantity2, double weight2) {
        this.name = name;
        this.quantity = quantity;
        this.weight = weight;
        this.quantity2 = quantity2;
        this.weight2 = weight2;
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

    @Query("shippingWeight")
    @Description("Total tonnage to be shipped")
    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getQuantity2() {
        return quantity2;
    }

    public void setQuantity2(int quantity2) {
        this.quantity2 = quantity2;
    }

    @JsonbProperty("shippingWeight2")
    public double getWeight2() {
        return weight2;
    }

    public void setWeight2(double weight2) {
        this.weight2 = weight2;
    }

    public int getQuantity3() {
        return quantity3;
    }

    public void setQuantity3(int quantity3) {
        this.quantity3 = quantity3;
    }

    @Query("shippingWeight3")
    @JsonbProperty("shippingWeightFromJsonbProperty")
    public double getWeight3() {
        return weight3;
    }

    public void setWeight3(double weight3) {
        this.weight3 = weight3;
    }

    @Override
    public String toString() {
        return "Widget(" + name + ", " + quantity + ", " + weight + ", " +
                        quantity2 + ", " + weight2 + ", " + quantity3 + ", " + 
                        weight3 + ")";
    }
}
