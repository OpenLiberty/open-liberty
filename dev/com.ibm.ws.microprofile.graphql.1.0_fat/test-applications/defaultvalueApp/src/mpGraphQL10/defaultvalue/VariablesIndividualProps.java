/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.defaultvalue;

public class VariablesIndividualProps implements Variables {

    public static VariablesIndividualProps newVars(String name, int quantity, double weight, double length, double height, double depth) {
        WidgetInput w = new WidgetInput(name, quantity, weight, length, height, depth);
        return new VariablesIndividualProps(w);
    }

    private WidgetInput widget;

    private VariablesIndividualProps(WidgetInput widget) {
        this.widget = widget;
    }

    public WidgetInput getWidget() {
        return widget;
    }

    public void setWidget(WidgetInput widget) {
        this.widget = widget;
    }
}
