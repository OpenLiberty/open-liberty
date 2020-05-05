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
package mpGraphQL10.inputFields;

import java.text.DecimalFormat;

import javax.json.bind.annotation.JsonbProperty;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Name;
/**
 * This is an implementation class of the interface entity, Widget.
 */
public class WidgetClientObject {

    private String name;
    @JsonbProperty("qty")
    @Description("Number of units to ship")
    private int quantity = -1;
    private double weight = -1.0;

    @JsonbProperty("qty2")
    private int quantity2 = -1;
    private double weight2 = -1.0;


    static WidgetClientObject fromWidgetInput(WidgetClientObject input) {
        return new WidgetClientObject(input.getName(),
                          input.getQuantity(),
                          input.getWeight(),
                          input.getQuantity2(),
                          input.getWeight2());
    }

    static WidgetClientObject fromString(String s) {
        if (!s.startsWith("Widget(") || !s.endsWith(")")) {
            throw new IllegalArgumentException();
        }
        s = s.substring("Widget(".length(), s.length()-1);
        String[] fields = s.split(",");
        WidgetClientObject w = new WidgetClientObject();
        w.setName(fields[0]);
        w.setQuantity(Integer.parseInt(fields[1]));
        w.setWeight(Double.parseDouble(fields[2]));
        w.setQuantity2(Integer.parseInt(fields[3]));
        w.setWeight2(Double.parseDouble(fields[4]));
        return w;
    }

    public WidgetClientObject() {}

    public WidgetClientObject(String name, int quantity, double weight, int quantity2, double weight2) {
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

    public double getWeight() {
        return weight;
    }

//    @Name(value = "shippingWeight", description = "Total tonnage to be shipped")
    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getQuantity2() {
        return quantity2;
    }

    public void setQuantity2(int quantity2) {
        this.quantity2 = quantity2;
    }

    public double getWeight2() {
        return weight2;
    }

    //@JsonbProperty("shippingWeight2") // cannot use JsonbProperty here since we're getting "weight2" back
    public void setWeight2(double weight2) {
        this.weight2 = weight2;
    }

    @Override
    public String toString() {
        return "Widget(" + name + ", " + quantity + ", " + weight + ", " +
                        quantity2 + ", " + weight2 + ")";
    }
}
