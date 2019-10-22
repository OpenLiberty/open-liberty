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

import java.util.List;

public class AllWidgetData {

    private List<WidgetClientObject> allWidgets;
    private WidgetClientObject createdWidget;

    public List<WidgetClientObject> getAllWidgets() {
        return allWidgets;
    }

    public void setAllWidgets(List<WidgetClientObject> allWidgets) {
        this.allWidgets = allWidgets;
    }
    
    public WidgetClientObject getCreateWidget() {
        return createdWidget;
    }
    
    public void setCreateWidget(WidgetClientObject widget) {
        createdWidget = widget;
    }

    public WidgetClientObject getCreateWidgetByHand() {
        return createdWidget;
    }
    
    public void setCreateWidgetByHand(WidgetClientObject widget) {
        createdWidget = widget;
    }
}
