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
package mpGraphQL10.graphQLInterface;

/**
 * This is an implementation class of the interface entity, Widget.
 */
public class Widget implements IWidget {

    private String name;
    private int quantity = -1;
    private double weight = -1.0;

    static Widget fromWidgetInput(Widget input) {
        return new Widget(input.getName(),
                          input.getQuantity(),
                          input.getWeight());
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
        return w;
    }

    public Widget() {}

    public Widget(String name, int quantity, double weight) {
        this.name = name;
        this.quantity = quantity;
        this.weight = weight;
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
    
    @Override
    public String toString() {
        return "Widget(" + name + ", " + quantity + ", " + weight + ")";
    }
}
